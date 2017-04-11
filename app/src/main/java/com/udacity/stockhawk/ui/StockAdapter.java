package com.udacity.stockhawk.ui;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private final Context context;
    private Cursor cursor;
    private final StockAdapterOnClickHandler clickHandler;

    StockAdapter(Context context, StockAdapterOnClickHandler clickHandler) {
        this.context = context;
        this.clickHandler = clickHandler;
    }

    void setCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    String getSymbolAtPosition(int position) {

        cursor.moveToPosition(position);
        return cursor.getString(Contract.Quote.POSITION_SYMBOL);
    }

    @Override
    public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.list_item_quote, parent, false);

        return new StockViewHolder(item);
    }

    @Override
    public void onBindViewHolder(StockViewHolder holder, int position) {
        cursor.moveToPosition(position);

        String symbol = cursor.getString(Contract.Quote.POSITION_SYMBOL);
        String symbolDescription = context.getString(R.string.symbol_content_description, symbol);
        String price = Utility.formatPrice(cursor.getFloat(Contract.Quote.POSITION_PRICE));
        String priceDescription = context.getString(R.string.price_content_description, price);

        holder.symbol.setText(symbol);
        holder.symbol.setContentDescription(symbolDescription);
        holder.price.setText(price);
        holder.price.setContentDescription(priceDescription);

        float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        if (rawAbsoluteChange > 0) {
            holder.change.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            holder.change.setBackgroundResource(R.drawable.percent_change_pill_red);
        }

        String change = Utility.formatPercentageChange(percentageChange);
        if (PrefUtils.getDisplayMode(context)
                .equals(context.getString(R.string.pref_display_mode_absolute_key))) {
            change = Utility.formatAbsoluteChange(rawAbsoluteChange);
        }

        String changeDescription =
                context.getString(R.string.price_change_content_description, change);

        holder.change.setText(change);
        holder.change.setContentDescription(changeDescription);
        // Shared item animation
        ViewCompat.setTransitionName(holder.price, "priceView" + position);
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
        }
        return count;
    }


    interface StockAdapterOnClickHandler {
        void onClick(String symbol, StockViewHolder viewHolder);
    }

    class StockViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.symbol)
        TextView symbol;

        @BindView(R.id.price)
        TextView price;

        @BindView(R.id.change)
        TextView change;

        StockViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            cursor.moveToPosition(adapterPosition);
            int symbolColumn = cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL);
            clickHandler.onClick(cursor.getString(symbolColumn), this);
        }
    }
}
