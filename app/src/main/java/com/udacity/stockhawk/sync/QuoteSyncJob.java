package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.udacity.stockhawk.Utility;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.MainActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();
                Stock stock = quotes.get(symbol);

                // Determine if stock symbol is exist.
                StockQuote quote = stock.getQuote();
                if (quote.getPrice() != null) {
                    float price = quote.getPrice().floatValue();
                    float change = quote.getChange().floatValue();
                    float percentChange = quote.getChangeInPercent().floatValue();

                    // WARNING! Don't request historical data for a stock that doesn't exist!
                    // The request will hang forever X_x
                    List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                    StringBuilder historyBuilder = new StringBuilder();
                    for (HistoricalQuote it : history) {
                        historyBuilder.append(it.getDate().getTimeInMillis());
                        historyBuilder.append(", ");
                        historyBuilder.append(it.getClose());
                        historyBuilder.append("\n");
                    }

                    ContentValues quoteCV = new ContentValues();
                    quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                    quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                    quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);

                    quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                    quoteCVs.add(quoteCV);
                } else {
                    // If stock symbol does not exists... remove it from preferences.
                    // We don't need to cleanup database, because we did not put there
                    // any data when it is not exist ;)
                    PrefUtils.removeStock(context, symbol);

                    // Send broadcast about invalid quote.
                    Intent invalidData = new Intent(MainActivity.ACTION_QUOTE_INVALID);
                    invalidData.putExtra(MainActivity.ACTION_QUOTE_SYMBOL, symbol);
                    context.sendBroadcast(invalidData);
                }
            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            // Save last synchronisation time
            PrefUtils.updateSyncTime(context);

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);
        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }


    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");
        JobInfo.Builder builder = new JobInfo.Builder(
                PERIODIC_ID, new ComponentName(context, QuoteJobService.class));

        // If SDK version is N and above, we have to use different method to set periodic tasks
        // Unfortunately on N version recurring time will be approx. 25 min :(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Recurring job in N version should recur with the provided interval and flex.
            // The job can execute at any time in a window of flex length at the end of the period.
            long flexMillis = JobInfo.getMinFlexMillis();
            long minFlexTime = (long) (PERIOD * 0.05);
            if (minFlexTime > flexMillis) {
                flexMillis = minFlexTime;
            }

            // flexMillis is millisecond flex for this job. Flex is clamped to be at least
            // getMinFlexMillis() or 5% of the period, whichever is higher.
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(PERIOD, flexMillis)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        } else {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(PERIOD)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        }

        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }

    public static synchronized void initialize(final Context context) {
        schedulePeriodic(context);
    }

    public static synchronized void syncImmediately(Context context) {
        if (Utility.isNetworkAvailable(context)) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(
                    ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
            JobScheduler scheduler =
                    (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }
}
