package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.chart.DetailsMarkerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import yahoofinance.histquotes.HistoricalQuote;

public class DetailsActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, DetailsMarkerView.ChartTickerClickHandler {

    /**
     * View elements
     */
    @BindView(R.id.stock_chart)
    LineChart chart;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.tv_price)
    TextView mCurrentPrice;
    @BindView(R.id.tv_change_absolute)
    TextView mChangeAbsolute;
    @BindView(R.id.tv_change_percentage)
    TextView mChangePercentage;
    @BindView(R.id.error_message)
    TextView mErrorMessage;
    @BindView(R.id.details_data)
    View mDetailsData;
    @BindView(R.id.last_synchronization)
    TextView mLastSynchronization;
    @BindView(R.id.rv_quote_history)
    RecyclerView mHistoryList;
    private ActionBar mActionBar;

    /**
     * Selected stock symbol URI address.
     */
    private Uri stockUri;

    /**
     * Adapter which contains all historical data of the stock quote.
     */
    private QuoteHistoryAdapter mHistoryAdapter;

    /**
     * Loader ID for fetch selected stock quote.
     */
    private static final int SINGLE_STOCK_LOADER_ID = 2135;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        supportPostponeEnterTransition();

        // Gets selected stock quote URI address.
        Intent intent = getIntent();
        stockUri = intent.getData();
        if (stockUri == null) {
            throw new IllegalArgumentException("Intent should contain an URI address.");
        }

        // Setup an action bar.
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Setup a recycler view
        mHistoryAdapter = new QuoteHistoryAdapter();
        mHistoryList.setHasFixedSize(true);
        mHistoryList.setLayoutManager(new LinearLayoutManager(this));
        mHistoryList.setAdapter(mHistoryAdapter);

        // Initialize loader.
        Timber.d("Stock URI: %s", stockUri.toString());
        getSupportLoaderManager().initLoader(SINGLE_STOCK_LOADER_ID, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case SINGLE_STOCK_LOADER_ID:
                return new CursorLoader(this,
                        stockUri,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                        null, null, null);

            default:
                throw new RuntimeException("Loader under provided ID is not implemented.");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case SINGLE_STOCK_LOADER_ID:
                if (data != null && data.moveToFirst()) {
                    // Get data from the cursor
                    float price =
                            data.getFloat(Contract.Quote.POSITION_PRICE);
                    String priceDescription = getString(
                            R.string.price_content_description, Utility.formatPrice(price));
                    String history =
                            data.getString(Contract.Quote.POSITION_HISTORY);
                    String quoteSymbol =
                            data.getString(Contract.Quote.POSITION_SYMBOL);
                    float absoluteChange =
                            data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                    String absoluteChangeDescription = getString(
                            R.string.absolute_price_change_content_description,
                            Utility.formatAbsoluteChange(absoluteChange));
                    float percentageChange =
                            data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);
                    String percentageChangeDescription = getString(
                            R.string.percentage_price_change_content_description,
                            Utility.formatPercentageChange(percentageChange));
                    ArrayList<HistoricalQuote> quotesHistory
                            = Utility.parseHistoryString(history);

                    // Change color of price whether current quote show profit or loss.
                    int valueColor = Utility.determineColor(this, absoluteChange);

                    // Prepare history chart
                    LineData lineData = prepareChart(new ArrayList<>(quotesHistory));

                    // Show data on the screen
                    mActionBar.setTitle(quoteSymbol);
                    mCurrentPrice.setTextColor(valueColor);
                    mCurrentPrice.setText(Utility.formatPrice(price));
                    mCurrentPrice.setContentDescription(priceDescription);
                    mChangeAbsolute.setTextColor(valueColor);
                    mChangeAbsolute.setText(Utility.formatAbsoluteChange(absoluteChange));
                    mChangeAbsolute.setContentDescription(absoluteChangeDescription);
                    mChangePercentage.setTextColor(valueColor);
                    mChangePercentage.setText(Utility.formatPercentageChange(percentageChange));
                    mChangePercentage.setContentDescription(percentageChangeDescription);

                    // Set data and refresh chart with animation
                    chart.setData(lineData);
                    chart.animateX(1000);

                    // Show history data in recycler view.
                    mHistoryAdapter.setData(quotesHistory);

                    // Show last synchronization info.
                    manageLastSynchronizationInfo();
                } else {
                    showError(getString(R.string.error_message_invalid_quote));
                }

                supportStartPostponedEnterTransition();
                break;

            default:
                throw new RuntimeException("Loader under provided ID is not implemented.");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Click handler for selected point on graph.
     *
     * @param position of selection
     */
    @Override
    public void onSelectedOnChart(int position) {
        // As list is ordered in opposite direction to data in chart
        // sow we need to recalculate position.
        position = mHistoryAdapter.getItemCount() - position - 1;
        if (position >= 0) {
            mHistoryAdapter.setSelectedPosition(position);
            mHistoryList.smoothScrollToPosition(position);
        }
    }

    /**
     * Prepare chart for displaying data.
     *
     * @param quotesHistory which we want to show on the graph.
     * @return Configured line data for the chart.
     */
    private LineData prepareChart(ArrayList<HistoricalQuote> quotesHistory) {
        if (quotesHistory == null) {
            return null;
        }

        // When we are downloading data from the the Yahoo finance, we receive a data ordered
        // from newest to older. As we want to draw a chart which starts from the older data
        // and ends on the most recent data we need to reverse data array.
        Collections.reverse(quotesHistory);

        // Set allowed interactions with the chart.
        chart.setPinchZoom(false);
        chart.setDescription(null);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setViewPortOffsets(0, 0f, 0f, 0f);

        // Remove label and grid lines
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        // Configure X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        xAxis.setLabelCount(5, true);
        xAxis.setAvoidFirstLastClipping(true);

        // Prepare chart data abd labels
        List<Entry> entries = new ArrayList<>();
        final ArrayList<String> xLabels = new ArrayList<>();
        for (float index = 0; index < quotesHistory.size(); index++) {
            HistoricalQuote historicalQuote = quotesHistory.get((int) index);

            // Set price for the quote.
            float historicalPrice = 0;
            if (historicalQuote.getClose() != null) {
                historicalPrice = historicalQuote.getClose().floatValue();
            }

            // For chart purpose we need create entry object and fill its data with
            // proper X and Y values. As a values must be a floats, there was a problem
            // when we passing out a timestamp data. In chart library was provided
            // a workaround for this problem, but it was a much dirtier than setting
            // a string labels as I did.
            entries.add(new Entry(index, historicalPrice));
            xLabels.add(Utility.formatDate(historicalQuote.getDate(), DateFormat.SHORT));
        }

        // Show labels on x axis.
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // A date on the right edge of X axis is little bit clipped.
                // It is known bug of the chart library :( Solution for this problem was supposed
                // to be a setting setAvoidFirstLastClipping for X axis...
                // and it solves a problem... partially.
                // Left label is now fine but label on right side is still clipped.
                return xLabels.get((int) value);
            }
        });

        // Create marker on the chart.
        // This marker will pass an index of selected point on the graph
        // to activity, so we are able to show selected data on RecyclerView.
        DetailsMarkerView markerView = new DetailsMarkerView(this, this);
        chart.setMarker(markerView);

        LineDataSet dataSet = new LineDataSet(entries, null);
        dataSet.setColors(new int[]{R.color.material_blue_500}, this);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_gradient));
        return new LineData(dataSet);
    }

    /**
     * Show an error message.
     *
     * @param message which we want to display
     */
    private void showError(String message) {
        mErrorMessage.setText(message);
        mDetailsData.setVisibility(View.INVISIBLE);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Determine if we should show last synchronization time or not.
     * <p>
     * This view will be shown after specified time pass since last
     * synchronization - which will means our data might be outdated.
     */
    private void manageLastSynchronizationInfo() {
        if (PrefUtils.showLastSynchronizationInfo(DetailsActivity.this)) {
            mLastSynchronization.setText(PrefUtils.getLastSyncTime(this));
            mLastSynchronization.setVisibility(View.VISIBLE);
        } else {
            mLastSynchronization.setVisibility(View.GONE);
        }
    }
}