package com.udacity.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        StockAdapter.StockAdapterOnClickHandler {

    /**
     * ID used for receiving broadcast about if is provided stock symbol is valid of.
     * ACTION_QUOTE_SYMBOL is used to pass symbol in intent.
     * ACTION_QUOTE_INVALID is the intent filter for this broadcast receiver.
     */
    public static final String ACTION_QUOTE_SYMBOL = "ACTION_QUOTE_SYMBOL";
    public static final String ACTION_QUOTE_INVALID = "com.udacity.stockhawk.ACTION_QUOTE_INVALID";

    /**
     * Loader ID.
     */
    private static final int STOCK_LOADER = 1256;

    /**
     * RecyclerView adapter.
     */
    private StockAdapter adapter;

    // View members.
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    @BindView(R.id.last_synchronization)
    TextView mLastSynchronization;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    /**
     * Broadcast receiver for checking if provided stock symbol is valid or not.
     * As we check validity of stock symbol during an API call, we need to pass a result form
     * background service to this activity.
     */
    private final BroadcastReceiver stockBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String symbol = intent.getStringExtra(ACTION_QUOTE_SYMBOL);
            if (symbol.isEmpty()) {
                throw new IllegalArgumentException("Symbol should not be empty!");
            }

            // If data is invalid, show information in Toast.
            String message = getString(R.string.error_stock_symbol_invalid, symbol);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onClick(String symbol, StockAdapter.StockViewHolder viewHolder) {
        Timber.d("Symbol clicked: %s", symbol);

        // Create an URI address for selected stock and start a new activity.
        Uri selectedStockUri = Contract.Quote.makeUriForStock(symbol);
        Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
        intent.setData(selectedStockUri);
        Pair pair = new Pair<View, String>(viewHolder.price,
                getString(R.string.transition_name_price));

        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, pair);
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup swipe refresh layout
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setOnRefreshListener(this);

        supportPostponeEnterTransition();
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());

                int removedItems = getContentResolver().delete(
                        Contract.Quote.makeUriForStock(symbol), null, null);
                if (removedItems > 0) {
                    PrefUtils.removeStock(MainActivity.this, symbol);
                }
            }
        }).attachToRecyclerView(stockRecyclerView);

        // Initialize periodic synchronization job and refresh a data
        QuoteSyncJob.initialize(this);
        onRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register broadcast receiver
        IntentFilter intentFilter = new IntentFilter(ACTION_QUOTE_INVALID);
        registerReceiver(stockBroadcastReceiver, intentFilter);

        // Register shared preferences listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister broadcast receiver
        unregisterReceiver(stockBroadcastReceiver);
    }

    @Override
    public void onRefresh() {
        // Let's check if we have any stocks added to the list.
        if (PrefUtils.getStocks(this).size() == 0) {
            showError(getString(R.string.error_no_stocks));
            return;
        }

        // Let's check if we have internet connection.
        if (!Utility.isNetworkAvailable(this)) {
            // Attach information about last synchronisation when we have no access to the internet.
            manageLastSynchronizationInfo();

            // Lets check if we have internet connection.
            if (adapter.getItemCount() == 0) {
                showError(getString(R.string.error_no_network));
            }

            // We don't have internet connection... show error in Toast.
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
            return;
        }

        // We good to go... start syncing.
        hideError();
        swipeRefreshLayout.setRefreshing(true);
        QuoteSyncJob.syncImmediately(this);
    }

    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            if (Utility.isNetworkAvailable(this)) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            hideError();
            stockRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (stockRecyclerView.getChildCount() > 0) {
                        supportStartPostponedEnterTransition();
                        return true;
                    }

                    return false;
                }
            });
        } else {
            // Check if we have any stock on the list. Otherwise show an error.
            if (PrefUtils.getStocks(MainActivity.this).size() == 0) {
                showError(getString(R.string.error_no_stocks));
            }
        }

        adapter.setCursor(data);
        manageLastSynchronizationInfo();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
            item.setTitle(R.string.action_button_change_units_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
            item.setTitle(R.string.action_button_change_units_absolute);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hide an error message.
     */
    private void hideError() {
        error.setVisibility(View.GONE);
    }

    /**
     * Show an error message.
     *
     * @param message which we want to show.
     */
    private void showError(String message) {
        swipeRefreshLayout.setRefreshing(false);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    /**
     * Determine if we should show last synchronization time or not.
     * <p>
     * This view will be shown after specified time pass since last
     * synchronization - which will means our data might be outdated.
     */
    private void manageLastSynchronizationInfo() {
        if (PrefUtils.showLastSynchronizationInfo(MainActivity.this)) {
            mLastSynchronization.setText(PrefUtils.getLastSyncTime(this));
            mLastSynchronization.setVisibility(View.VISIBLE);
        } else {
            mLastSynchronization.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_stocks_key))) {
            if (PrefUtils.getStocks(MainActivity.this).size() == 0) {
                showError(getString(R.string.error_no_stocks));
            }
        }
    }
}
