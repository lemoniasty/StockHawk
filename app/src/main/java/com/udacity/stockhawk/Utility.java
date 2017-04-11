package com.udacity.stockhawk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import yahoofinance.histquotes.HistoricalQuote;

/**
 * Utility class.
 */
public class Utility {

    // Price and changes formatters.
    private static final DecimalFormat priceFormat =
            (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

    private static final DecimalFormat absoluteChangeFormat =
            (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

    private static final DecimalFormat percentageChangeFormat =
            (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());

    /**
     * Parse a string which contains historical stock quotes (date and closing value).
     * This method is used for parsing a data which we storing in history column of quotes table.
     *
     * @param history string
     * @return list of the history quotes
     */
    public static ArrayList<HistoricalQuote> parseHistoryString(String history) {
        ArrayList<HistoricalQuote> historicalQuotes = new ArrayList<>();

        if (history.isEmpty())
            return historicalQuotes;

        String[] lines = history.split("\\n");
        for (String line : lines) {
            String[] historyChunks = line.split(", ");

            if (historyChunks.length == 2) {
                // Create a calendar from timestamp.
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Long.valueOf(historyChunks[0]));

                // Create price in big decimal value
                BigDecimal price = new BigDecimal(historyChunks[1]);

                // Create historicalQuote object
                HistoricalQuote historicalQuote = new HistoricalQuote();
                historicalQuote.setDate(calendar);
                historicalQuote.setClose(price);

                historicalQuotes.add(historicalQuote);
            }
        }

        return historicalQuotes;
    }

    /**
     * Format price.
     *
     * @param price in numeric value
     * @return formatted price value
     */
    public static String formatPrice(Float price) {
        if (price == null) {
            price = 0f;
        }
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setMaximumFractionDigits(2);

        return priceFormat.format(price);
    }

    /**
     * Format absolute change of the price.
     *
     * @param value which express a price change.
     * @return formatted absolute change
     */
    public static String formatAbsoluteChange(float value) {
        absoluteChangeFormat.setPositivePrefix("+$");
        absoluteChangeFormat.setMinimumFractionDigits(2);
        absoluteChangeFormat.setMaximumFractionDigits(2);

        return absoluteChangeFormat.format(value);
    }

    /**
     * Format a percentage change of the price.
     *
     * @param value which express a price change.
     * @return formatted percentage change
     */
    public static String formatPercentageChange(float value) {
        value /= 100;
        if (value > 0) {
            percentageChangeFormat.setPositivePrefix("+");
        }

        percentageChangeFormat.setMinimumFractionDigits(2);
        percentageChangeFormat.setMaximumFractionDigits(2);

        return percentageChangeFormat.format(value);
    }

    /**
     * Format date into string value.
     *
     * @param calendar which contains a date
     * @return string representation of the date
     */
    public static String formatDate(Calendar calendar) {
        return formatDate(calendar, DateFormat.DEFAULT);
    }

    /**
     * Format date into string value in specified format.
     *
     * @param calendar   which contains a date
     * @param dateFormat which we want to receive
     * @return string representation of the date
     */
    public static String formatDate(Calendar calendar, int dateFormat) {
        if (calendar == null) {
            return "";
        }

        DateFormat formatter = SimpleDateFormat.getDateInstance(dateFormat);
        formatter.setTimeZone(calendar.getTimeZone());
        return formatter.format(calendar.getTimeInMillis());
    }

    /**
     * Format date into string value in specified format.
     *
     * @param calendar   which contains a date
     * @param dateFormat which we want to receive
     * @return string representation of the date
     */
    public static String formatDate(Calendar calendar, String dateFormat) {
        if (calendar == null) {
            return "";
        }

        DateFormat formatter = new SimpleDateFormat(dateFormat, Locale.getDefault());
        formatter.setTimeZone(calendar.getTimeZone());
        return formatter.format(calendar.getTimeInMillis());
    }

    /**
     * Determine right color for value.
     * This method will select proper color which will express value state.
     * Red color - Value loss.
     * Blue color - Value hasn't changed.
     * Green color - Value gain.
     *
     * @param context of the call
     * @param value   to express
     * @return color to proper value expression.
     */
    public static int determineColor(Context context, float value) {
        int color = R.color.material_blue_500;
        if (value < 0) {
            color = R.color.material_red_700;
        } else if (value > 0) {
            color = R.color.material_green_700;
        }

        return ContextCompat.getColor(context, color);
    }

    /**
     * Method which determine if device is connected to the internet or not.
     *
     * @param context of call
     * @return internet connection availability
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}