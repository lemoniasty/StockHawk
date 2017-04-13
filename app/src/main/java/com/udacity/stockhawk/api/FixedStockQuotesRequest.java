package com.udacity.stockhawk.api;

import java.util.Arrays;

import yahoofinance.quotes.stock.StockQuotesData;
import yahoofinance.quotes.stock.StockQuotesRequest;

/**
 *  This class has been created to fix problem with empty lines which is returned by the API
 *  when stock symbol has contain invalid/special characters.
 */
class FixedStockQuotesRequest extends StockQuotesRequest {

    FixedStockQuotesRequest(String query) {
        super(query);
    }

    @Override
    protected StockQuotesData parseCSVLine(String line) {
        if (line.isEmpty()) {
            String[] emptyQuote = new String[57];
            Arrays.fill(emptyQuote, "");
            return new StockQuotesData(emptyQuote);
        }

        return super.parseCSVLine(line);
    }
}
