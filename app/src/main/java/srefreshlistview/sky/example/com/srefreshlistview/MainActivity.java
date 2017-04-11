package srefreshlistview.sky.example.com.srefreshlistview;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private List<String> textList;
    private RefreshListView mRefreshListView;
    private ArrayAdapter<String> mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRefreshListView = (RefreshListView) findViewById(R.id.refresh_list);
        textList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            textList.add("这是一条ListView的数据" + i);
        }
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textList);
        mRefreshListView.setAdapter(mArrayAdapter);
        mRefreshListView.setOnRefreshListener(new RefreshListView.OnRefreshListener() {
            @Override
            public void loadMore() {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        SystemClock.sleep(2000);
                        for (int i = 0; i < 2; i++) {
                            textList.add("新加载的数据" + i);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mArrayAdapter.notifyDataSetChanged();
                        mRefreshListView.hiddenFooterView();
                    }
                }.execute();
            }

            @Override
            public void refresh() {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        SystemClock.sleep(2000);
                        for (int i = 0; i < 2; i++) {
                            textList.add(0, "刷新的数据" + i);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mArrayAdapter.notifyDataSetChanged();
                        mRefreshListView.hiddenHeaderView();
                    }
                }.execute();
            }
        });
    }

}
