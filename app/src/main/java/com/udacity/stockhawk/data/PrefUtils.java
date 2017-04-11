package com.udacity.stockhawk.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utility;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class PrefUtils {

    private PrefUtils() {
    }

    public static Set<String> getStocks(Context context) {
        String stocksKey = context.getString(R.string.pref_stocks_key);
        String initializedKey = context.getString(R.string.pref_stocks_initialized_key);
        String[] defaultStocksList = context.getResources().getStringArray(R.array.default_stocks);

        HashSet<String> defaultStocks = new HashSet<>(Arrays.asList(defaultStocksList));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }
        return prefs.getStringSet(stocksKey, new HashSet<String>());

    }

    private static void editStockPref(Context context, String symbol, Boolean add) {
        String key = context.getString(R.string.pref_stocks_key);
        Set<String> stocks = new HashSet<>(getStocks(context));

        if (add) {
            stocks.add(symbol);
        } else {
            stocks.remove(symbol);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(key, stocks);
        editor.apply();
    }

    public static void addStock(Context context, String symbol) {
        editStockPref(context, symbol, true);
    }

    public static void removeStock(Context context, String symbol) {
        editStockPref(context, symbol, false);
    }

    public static String getDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String defaultValue = context.getString(R.string.pref_display_mode_default);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static void toggleDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayMode = getDisplayMode(context);

        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey)) {
            editor.putString(key, percentageKey);
        } else {
            editor.putString(key, absoluteKey);
        }

        editor.apply();
    }

    /**
     * Update a value of last sync time.
     * This value is updated after each successful synchronization.
     * By having this value, we can determine if data is fresh or not.
     *
     * @param context of the application
     */
    public static void updateSyncTime(Context context) {
        String key = context.getString(R.string.pref_last_sync_time_key);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(key, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Fetch a formatted last synchronization time.
     *
     * @param context of the application
     * @return formatted string which contain last synchronization time.
     */
    public static String getLastSyncTime(Context context) {
        String key = context.getString(R.string.pref_last_sync_time_key);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long timestamp = sp.getLong(key, 0);

        String lastSyncDate = context.getString(R.string.last_synchronization_never);
        if (timestamp != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            lastSyncDate = Utility.formatDate(calendar, "yyyy-MM-dd HH:mm:ss");
        }

        return context.getString(R.string.last_synchronization_info, lastSyncDate);
    }

    /**
     * Determine if data stored in database are fresh or not.
     * Fresh data is considered as a fresh when they was collected no later than one hour ago.
     *
     * @param context of the application
     * @return flag whether data is fresh or not
     */
    public static boolean showLastSynchronizationInfo(Context context) {
        // Well if we don't have any added stocks, data can't be outdated ;)
        if (PrefUtils.getStocks(context).size() == 0) {
            return false;
        }

        String key = context.getString(R.string.pref_last_sync_time_key);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long timestamp = sp.getLong(key, 0);

        if (timestamp == 0) {
            return true;
        }

        long timeDiff = System.currentTimeMillis() - timestamp;
        return timeDiff > TimeUnit.HOURS.toMillis(1);
    }

}
