package com.sjl.wifi.activity;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sjl.wifi.R;
import com.sjl.wifi.app.AppConstants;
import com.sjl.wifi.bean.WifiBean;
import com.sjl.wifi.util.WifiHelper;


/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename WifiDetailActivity.java
 * @time 2019/3/13 14:48
 * @copyright(C) 2019 xxx有限公司
 */
public class WifiDetailActivity extends AppCompatActivity {
    private static final String TAG = "WifiDetailActivity";

    TextView wifi_name;
    TextView ip;
    TextView connect_state;
    TextView signal;
    TextView connnect_rate;
    TextView frequency;
    TextView safe;
    Button deleteNet;
    WifiInfo wifiInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }


    private void initView() {
        setContentView(R.layout.wifi_detail_activity);
        wifi_name = findViewById(R.id.tv_wifi_name);
        ip = findViewById(R.id.tv_ip);
        connect_state = findViewById(R.id.tv_connect_state);
        signal = findViewById(R.id.tv_signal);
        connnect_rate = findViewById(R.id.tv_connnect_rate);
        frequency = findViewById(R.id.tv_frequency);
        safe = findViewById(R.id.tv_safe);
        deleteNet = findViewById(R.id.btn_delete_net);
    }

    private void initListener() {
        deleteNet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    WifiHelper wifiHelper = new WifiHelper(WifiDetailActivity.this);
                    wifiHelper.removeWifiBySsid(wifiInfo.getSSID());
                    wifiHelper.startScan();
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void initData() {

        Intent intent = getIntent();
        WifiBean wifiBean = intent.getParcelableExtra(AppConstants.EXTRA_WIFI_BEAN);
        WifiHelper wifiHelper = new WifiHelper(this);

        wifiInfo = wifiHelper.getConnectionWifiInfo();
        Log.i(TAG, wifiBean.toString() + "\n\nwifi信息" + wifiInfo.toString());
        //wifiBean包含所有信息，看需求显示
        String ipAddress = wifiHelper.intToIp(wifiInfo.getIpAddress());

        wifi_name.setText(wifiHelper.trimSSID(wifiInfo.getSSID()));

        ip.setText(ipAddress);
        connect_state.setText("已连接");
        int rssi = wifiInfo.getRssi();//单位dBm
        String rssiText;
        if (rssi <= -70) {
            rssiText = "弱";
        } else if (rssi > -70 && rssi <= -50) {
            rssiText = "中";
        } else {
            rssiText = "强";
        }
        signal.setText(rssiText);//信号信号强度
        connnect_rate.setText(wifiInfo.getLinkSpeed() + "Mbps");//连接速率
        frequency.setText(wifiInfo.getFrequency() + "MHz");//频率
        safe.setText(wifiBean.getCapabilities());//可以化简显示
    }
}
