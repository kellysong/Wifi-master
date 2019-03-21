package com.sjl.wifi.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.sjl.wifi.R;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MainActivity.java
 * @time 2019/3/21 13:58
 * @copyright(C) 2019 song
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn1, btn2, btn3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        setContentView(R.layout.main_activity);
    }

    private void initData() {
        btn1 = findViewById(R.id.btn_1);
        btn2 = findViewById(R.id.btn_2);
        btn3 = findViewById(R.id.btn_3);
    }

    private void initListener() {
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1:
                openActivity(WifiListActivity.class);
                break;
            case R.id.btn_2:
                openActivity(WifiApActivity.class);
                break;
            case R.id.btn_3:
                openActivity(ListViewActivity.class);
                break;
            default:
                break;
        }
    }

    private void openActivity(Class<?> clz) {
        startActivity(new Intent(this,clz));
    }
}
