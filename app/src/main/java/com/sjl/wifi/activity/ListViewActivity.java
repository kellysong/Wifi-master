package com.sjl.wifi.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.sjl.wifi.R;
import com.sjl.wifi.adapter.ListViewAdapter;
import com.sjl.wifi.widget.MyListView;

import java.util.Arrays;
import java.util.List;

/**
 * listview焦点控制
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ListViewActivity.java
 * @time 2019/3/19 15:22
 * @copyright(C) 2019 song
 */
public class ListViewActivity extends AppCompatActivity {
    private List<String> data = Arrays.asList("aa", "bb", "cc", "dd", "aa", "bb", "cc", "dd", "aa", "bb", "cc", "dd", "aa", "bb", "cc", "dd");
    MyListView listView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_activity);
         listView = (MyListView) findViewById(R.id.listView);
        ListViewAdapter adapter = new ListViewAdapter(this, data);
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(true);


    }


}
