package com.udacity.stockhawk.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.histquotes.HistoricalQuote;

/**
 * Adapter which contains historical quotes for selected stock symbol.
 */
class QuoteHistoryAdapter extends
        RecyclerView.Adapter<QuoteHistoryAdapter.QuoteHistoryViewHolder> {

    private ArrayList<HistoricalQuote> mData;
    private int mSelectedPosition = AbsListView.INVALID_POSITION;

    @Override
    public QuoteHistoryAdapter.QuoteHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View item = LayoutInflater.from(context)
                .inflate(R.layout.list_item_quote_history, parent, false);

        return new QuoteHistoryViewHolder(item);
    }

    @Override
    public void onBindViewHolder(QuoteHistoryAdapter.QuoteHistoryViewHolder holder, int position) {
        // Get data
        HistoricalQuote quote = mData.get(position);

        // Stock quotes history are given for week number which points to first day of the week.
        // To avoid any confusion we need to set the date to friday...
        // it is a real end of the week.
        Calendar quoteDate = quote.getDate();
        while (quoteDate.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
            quoteDate.add(Calendar.DATE, 1);
        }

        String formattedDate = Utility.formatDate(quote.getDate());
        String formattedPrice = Utility.formatPrice(quote.getClose().floatValue());

        // Bind data to the view
        holder.mQuoteDate.setText(formattedDate);
        holder.mQuoteValue.setText(formattedPrice);

        // Set proper background if it is selected item.
        if (position == mSelectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.chart_gradient);
        } else {
            holder.itemView.setBackgroundResource(0);
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public void setData(ArrayList<HistoricalQuote> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        notifyItemChanged(mSelectedPosition);
        mSelectedPosition = position;
        notifyItemChanged(mSelectedPosition);
    }

    class QuoteHistoryViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_quote_date)
        TextView mQuoteDate;
        @BindView(R.id.tv_quote_value)
        TextView mQuoteValue;

        QuoteHistoryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
