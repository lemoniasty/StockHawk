package com.udacity.stockhawk.ui.chart;

import android.content.Context;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.udacity.stockhawk.R;

import timber.log.Timber;

/**
 * Details marker view.
 * It is used when user selects any point on the line chart.
 * <p>
 * Instead of drawing data on the chart i'm fetching a click and showing
 * a selected point as a item of RecyclerView.
 * <p>
 * Unfortunately I have to build dummy layout {@R.layout.chart_marker_id} for purpose
 * of the constructor fo this class.
 */
public class DetailsMarkerView extends MarkerView {

    /**
     * Click handler.
     */
    private final ChartTickerClickHandler mClickHandler;

    /**
     * Click handler interface.
     */
    public interface ChartTickerClickHandler {
        void onSelectedOnChart(int position);
    }

    /**
     * Constructor. Sets up the MarkerView with a custom layout resource.
     *
     * @param context
     */
    public DetailsMarkerView(Context context, ChartTickerClickHandler clickHandler) {
        super(context, R.layout.chart_marker_ui);
        mClickHandler = clickHandler;
        Timber.i("Create details marker view.");
    }

    /**
     * Callback which is called every time the MarkerView is redrawn.
     * Lets use it to update a content.
     *
     * @param e         data
     * @param highlight data
     */
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        Timber.d("Refresh content: " + e);
        mClickHandler.onSelectedOnChart((int) e.getX());
    }
}
