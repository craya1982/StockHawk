package com.sam_chordas.android.stockhawk.service;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Constants;

/**
 * Created by Chris on 5/30/2016.
 */
public class StockWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        // return remote view factory
        return new RemoteViewsFactory() {
            Cursor mCursor = null;

            @Override
            public void onCreate() {
                // nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (mCursor != null)
                    mCursor.close();
                long identityToken = Binder.clearCallingIdentity();
                mCursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                     new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                                             QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                                                     QuoteColumns.ISCURRENT + " = ?",
                                                     new String[]{"1"},
                                                     null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (mCursor != null) {
                    mCursor.close();
                    mCursor = null;
                }
            }

            @Override
            public int getCount() {
                return mCursor == null ? 0 : mCursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.item_widget);
                if (mCursor != null && mCursor.moveToPosition(position)) {
                    String symbol = mCursor.getString(mCursor.getColumnIndex("symbol"));
                    remoteViews.setTextViewText(R.id.stock_symbol, symbol);
                    remoteViews.setTextViewText(R.id.bid_price, mCursor.getString(mCursor.getColumnIndex("bid_price")));
                    remoteViews.setTextViewText(R.id.change, mCursor.getString(mCursor.getColumnIndex("change")));

                    Intent fillInIntent = new Intent();
                    fillInIntent.putExtra(Constants.SYMBOL, symbol);
                    remoteViews.setOnClickFillInIntent(R.id.list_row, fillInIntent);
                }
                return remoteViews;
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
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
