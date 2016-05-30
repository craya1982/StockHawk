package com.sam_chordas.android.stockhawk.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Displays graph information
 */
public class GraphActivity extends AppCompatActivity implements Callback {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.linechart)
    LineChart mLineChart;

    private String mSymbol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        ButterKnife.bind(this);
        mSymbol = getIntent().getStringExtra(Constants.SYMBOL);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mSymbol);

        OkHttpClient client = new OkHttpClient();
        // create Request object

        /// Yahoo historical data stock query is based on https://github.com/swatiag1101/LineGraph
        String query = "select * from yahoo.finance.historicaldata where symbol ='" + mSymbol
                + "' and startDate = '" + lastMonth() + "' and endDate = '" + yesterday() + "'";
        Uri uri = Uri.parse(Constants.BASE_STOCK_URL).buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("env", "store://datatables.org/alltableswithkeys")
                .build();
        String url = uri.toString();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String yesterday() {
        return getDate(Calendar.DATE);
    }

    private String lastMonth() {
        return getDate(Calendar.MONTH);
    }

    private String getDate(int field) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(field, -1);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return format.format(calendar.getTime());
    }

    @Override
    public void onFailure(Call call, IOException e) {
        e.printStackTrace();
    }

    @Override
    public void onResponse(Call call, final Response response) throws IOException {
        if (!response.isSuccessful()) {
            return;
        }
        if (isDestroyed()) {
            return;
        }
        //Can't be run on UI Thread.
        final String json = response.body().string();
        //Now join ui thread due to library requirements
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    /// MPChart usage is based on
                    /// https://www.numetriclabz.com/android-line-chart-using-mpandroidchart-tutorial/
                    // list of y-values
                    List<Entry> entries = new ArrayList<>();
                    // x-axis labels
                    List<String> labels = new ArrayList<>();
                    JSONArray jsonArray =
                            new JSONObject(json).getJSONObject("query").getJSONObject("results").getJSONArray("quote");
                    // the jsonArray is in reverse chronological order
                    int j = 0;
                    for (int i = jsonArray.length() - 1; i >= 0; i--) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        // JSON field "Adj_Close" (adjusted close) is chosen instead of field "Close"
                        float adjustedClose = Float.parseFloat(jsonObject.getString("Adj_Close"));
                        Entry entry = new Entry(adjustedClose, j);
                        entries.add(entry);
                        String date = jsonObject.getString("Date");
                        labels.add(date);
                        j++;
                    }
                    LineDataSet dataSet = new LineDataSet(entries, getString(R.string.time_label, mSymbol));
                    LineData lineData = new LineData(labels, dataSet);
                    mLineChart.setData(lineData);
                    mLineChart.animateY(1000);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
