package com.udacity.stockhawk.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yahoofinance.Stock;
import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuotesData;

/**
 * I've decided to resolve problem with special characters by overriding {@YahooFinance.getQuotes}
 * and call API by the {@FixedStockQuotesRequest} instead {@StockQuotesRequest}.
 *
 * It seems that author of this class does not predicted using a by the user a special characters,
 * or did not supposed that user will send symbol name which only contains a special chars.
 *
 * As I didn't want to only catch exception, without knowing which symbol has caused a problem -
 * because I want to delete it from preferences. I override {@YahooFinance.getQuotes} and
 * I'm sending request via {@FixedStockQuotesRequest}. In {@FixedStockQoutesRequest}
 * I'm overriding a parseCSVLine method, where I added an additional check if line is empty.
 * If line is empty then I'm creating a dummy object, if not then I'm calling this method from
 * super class. In this way I'm able to catch every invalid stock symbol and delete
 * it from preferences with proper notification.
 */
public class FinanceAPI extends YahooFinance {

    public static Map<String, Stock> get(String[] symbols) throws IOException {
        return FinanceAPI.getQuotes(Utils.join(symbols, ","), false);
    }

    private static Map<String, Stock> getQuotes(
            String query, boolean includeHistorical) throws IOException {

        FixedStockQuotesRequest request = new FixedStockQuotesRequest(query);
        List<StockQuotesData> quotes = request.getResult();
        Map<String, Stock> result = new HashMap<>();

        for (StockQuotesData data : quotes) {
            Stock s = data.getStock();
            result.put(s.getSymbol(), s);
        }

        if (includeHistorical) {
            for (Stock s : result.values()) {
                s.getHistory();
            }
        }

        return result;
    }
}
