package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

/**
 * Widget remote views service for collection widget which contains all selected
 * stock symbols and their values.
 */
public class QuotesWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new QuotesViewsFactory();
    }

    /**
     * Widget views factory for collection widget which contains all selected
     * stock symbols and their values.
     */
    private class QuotesViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Cursor mData = null;

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            if (mData != null) {
                mData.close();
            }

            final long identityToken = Binder.clearCallingIdentity();

            mData = getContentResolver().query(
                    Contract.Quote.URI,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                    null,
                    null,
                    Contract.Quote.COLUMN_SYMBOL + " ASC");

            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public void onDestroy() {
            if (mData != null) {
                mData.close();
                mData = null;
            }
        }

        @Override
        public int getCount() {
            return mData == null ? 0 : mData.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position == AdapterView.INVALID_POSITION || mData == null
                    || !mData.moveToPosition(position)) {
                return null;
            }

            // Fetch data from cursor
            String symbol = mData.getString(Contract.Quote.POSITION_SYMBOL);
            float price = mData.getFloat(Contract.Quote.POSITION_PRICE);
            float absoluteChange = mData.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = mData.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            // Show price change based on selected price change type.
            Context context = QuotesWidgetRemoteViewsService.this;
            String formattedPriceChange;
            if (PrefUtils.getDisplayMode(context).equals(
                    context.getString(R.string.pref_display_mode_absolute_key))) {
                formattedPriceChange = Utility.formatAbsoluteChange(absoluteChange);
            } else {
                formattedPriceChange = Utility.formatPercentageChange(absoluteChange);
            }

            // Bind data to the view
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item);
            views.setTextColor(R.id.change, Utility.determineColor(
                    QuotesWidgetRemoteViewsService.this, percentageChange));

            views.setTextViewText(R.id.symbol, symbol);
            views.setTextViewText(R.id.price, Utility.formatPrice(price));
            views.setTextViewText(R.id.change, formattedPriceChange);

            // Set fill-intent which will be used to fill in the pending intent template.
            Intent intent = new Intent();
            intent.setData(Contract.Quote.makeUriForStock(symbol));
            views.setOnClickFillInIntent(R.id.widget_item, intent);

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            if (mData.moveToPosition(i)) {
                return mData.getLong(Contract.Quote.POSITION_ID);
            }

            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
