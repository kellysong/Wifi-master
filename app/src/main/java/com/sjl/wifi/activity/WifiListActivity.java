package com.sjl.wifi.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.sjl.wifi.R;
import com.sjl.wifi.adapter.WifiListAdapter;
import com.sjl.wifi.app.AppConstants;
import com.sjl.wifi.bean.WifiBean;
import com.sjl.wifi.util.CollectionUtils;
import com.sjl.wifi.util.WifiHelper;
import com.sjl.wifi.widget.WifiLinkDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WifiListActivity extends AppCompatActivity {

    private static final String TAG = "WifiListActivity";
    //权限请求码
    private static final int PERMISSION_REQUEST_CODE = 0;
    //两个危险权限需要动态申请
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private boolean mHasPermission;

    ProgressBar pbWifiLoading;

    List<WifiBean> realWifiList = new ArrayList<>();

    private WifiListAdapter adapter;

    private RecyclerView recyWifiList;

    private WifiBroadcastReceiver wifiReceiver;

    private int connectType = 0;//1：连接成功？ 2 正在连接（如果wifi热点列表发生变需要该字段）

    private Switch mSwitch;
    private TextView mWifiSwitchMsg;
    private Button mRefresh;
    private WifiHelper mWifiHelper;
    private boolean is_password_error;
    private static final List<String> ignoreSsid = Arrays.asList("0x", "<unknown ssid>");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        setContentView(R.layout.wifi_list_activity);
        pbWifiLoading = (ProgressBar) this.findViewById(R.id.pb_wifi_loading);
        mWifiSwitchMsg = findViewById(R.id.tv_wifi_switch);
        mSwitch = findViewById(R.id.sw_wifi);
        mRefresh = findViewById(R.id.btn_refresh);
        recyWifiList = (RecyclerView) this.findViewById(R.id.recy_list_wifi);
        hidingProgressBar();
    }

    private void initListener() {
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mWifiHelper.openWifi();
                    mWifiSwitchMsg.setText("开启");
                    mRefresh.setEnabled(true);
                    mWifiHelper.startScan();//扫描
                } else {
                    mWifiHelper.closeWifi();
                    mWifiSwitchMsg.setText("关闭");
                    realWifiList.clear();
                    adapter.notifyDataSetChanged();
                    mRefresh.setEnabled(false);
                }
            }
        });

        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiHelper.startScan();//扫描

            }
        });

        adapter.setOnItemClickListener(new WifiListAdapter.onItemClickListener() {
            @Override
            public void onItemClick(View view, int postion, Object o) {
                WifiBean wifiBean = realWifiList.get(postion);
                if (wifiBean.getState().equals(AppConstants.WIFI_STATE_CONNECT)) {//已经连接，查看详情
                    Intent intent = new Intent(WifiListActivity.this, WifiDetailActivity.class);
                    intent.putExtra(AppConstants.EXTRA_WIFI_BEAN, wifiBean);
                    startActivity(intent);
                } else {//未连接
                    String capabilities = realWifiList.get(postion).getCapabilities();
                    if (mWifiHelper.getWifiCipherWay(capabilities) == WifiHelper.WifiCipherType.WIFICIPHER_NOPASS) {//无需密码
                        mWifiHelper.connectWifi(wifiBean.getWifiName(), null, capabilities);//不需要弹窗
                    } else {  //需要密码，弹出输入密码dialog
                        noConfigurationWifi(postion);
                    }
                }

            }
        });
    }


    private void initData() {
        mWifiHelper = new WifiHelper(this);
        adapter = new WifiListAdapter(this, realWifiList);
        recyWifiList.setLayoutManager(new LinearLayoutManager(this));
        recyWifiList.setAdapter(adapter);
        mHasPermission = checkPermission();
        if (!mHasPermission && mWifiHelper.isWifiEnabled()) {  //未获取权限，申请权限
            requestPermission();
        } else if (mHasPermission && mWifiHelper.isWifiEnabled()) {  //已经获取权限,且wifi打开
            mSwitch.setChecked(true);
            mWifiSwitchMsg.setText("开启");
            mRefresh.setEnabled(true);
            mWifiHelper.startScan();//扫描，3秒左右返回数据
        } else {
            Toast.makeText(WifiListActivity.this, "WIFI处于关闭状态", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 之前没有配置wifi， 弹出输入密码界面
     *
     * @param position
     */
    private void noConfigurationWifi(int position) {
        WifiLinkDialog linkDialog = new WifiLinkDialog(this, realWifiList.get(position).getWifiName(), realWifiList.get(position).getCapabilities());
        if (!linkDialog.isShowing()) {
            linkDialog.show();
        }
    }

    /**
     * wifi连接失败，弹出输入框输入密码
     *
     * @param ssid
     */
    private void wifiErrorConnect(String ssid) {
        if (is_password_error) {
            return;
        }
        is_password_error = true;//只弹出一次
        if (CollectionUtils.isNullOrEmpty(realWifiList)) {
            return;
        }
        WifiBean wifiBean = null;
        for (int i = 0; i < realWifiList.size(); i++) {
            wifiBean = realWifiList.get(i);
            if (("\"" + wifiBean.getWifiName() + "\"").equals(ssid)) {
                Log.i(TAG, "列表存在改wifi：" + ssid);
                break;
            }
        }
        if (wifiBean != null) {
            WifiLinkDialog linkDialog = new WifiLinkDialog(this, wifiBean.getWifiName(), wifiBean.getCapabilities());
            if (!linkDialog.isShowing()) {
                linkDialog.show();
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        //注册广播
        wifiReceiver = new WifiBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//监听wifi是开关变化的状态
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//监听wifi连接状态广播,是否连接了一个有效路由
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//监听wifi列表变化（开启一个热点或者关闭一个热点）
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        this.registerReceiver(wifiReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(wifiReceiver);
    }

    /**
     * 监听wifi状态
     * 当都没有可用连接，系统会连接已经保存的网路
     */
    public class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            if (!mWifiManage.isWifiEnabled()) {
//                Log.d(TAG, "wifi已经禁用");
//                return;
//            }
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {//wifi开关状态
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    /**
                     * WIFI_STATE_DISABLED    WLAN已经关闭
                     * WIFI_STATE_DISABLING   WLAN正在关闭
                     * WIFI_STATE_ENABLED     WLAN已经打开
                     * WIFI_STATE_ENABLING    WLAN正在打开
                     * WIFI_STATE_UNKNOWN     未知
                     */
                    case WifiManager.WIFI_STATE_DISABLED: {
                        Log.d(TAG, "已经关闭");
                        mSwitch.setChecked(false);
                        mWifiSwitchMsg.setText("关闭");
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLING: {
                        Log.d(TAG, "正在关闭");
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLED: {
                        Log.d(TAG, "已经打开");
                        mSwitch.setChecked(true);
                        mWifiSwitchMsg.setText("开启");
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLING: {
                        Log.d(TAG, "正在打开");
                        break;
                    }
                    case WifiManager.WIFI_STATE_UNKNOWN: {
                        Log.d(TAG, "未知状态");
                        break;
                    }
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {//wifi连接网络状态变化

                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "wifi连接网络状态变化：" + info.toString());
                if (NetworkInfo.State.DISCONNECTED == info.getState()) {//wifi断开连接
                    WifiInfo connectedWifiInfo = mWifiHelper.getConnectionWifiInfo();

                    Log.i(TAG, "wifi断开连接：" + connectedWifiInfo.getSSID());
                    if (ignoreSsid.contains(connectedWifiInfo.getSSID())) {
                        return;
                    }
                    hidingProgressBar();
                    for (int i = 0; i < realWifiList.size(); i++) {//没连接上将 所有的连接状态都置为“未连接”
                        realWifiList.get(i).setState(AppConstants.WIFI_STATE_UNCONNECT);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    wifiErrorConnect(connectedWifiInfo.getSSID());
                } else if (NetworkInfo.State.CONNECTED == info.getState()) {//wifi连接上了
                    Log.i(TAG, "wifi已连接");
                    hidingProgressBar();
                    WifiInfo connectedWifiInfo = mWifiHelper.getConnectionWifiInfo();
                    is_password_error = false;
                    connectType = 1;
                    wifiListSet(connectedWifiInfo.getSSID(), connectType);
                } else if (NetworkInfo.State.CONNECTING == info.getState()) {//正在连接
                    Log.i(TAG, "wifi正在连接");
                    WifiInfo connectedWifiInfo = mWifiHelper.getConnectionWifiInfo();
                    if (ignoreSsid.contains(connectedWifiInfo.getSSID())) {
                        return;
                    }
                    showProgressBar();
                    connectType = 2;
                    wifiListSet(connectedWifiInfo.getSSID(), connectType);
                }

            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {  //扫描结果
                Log.i(TAG, "网络列表变化了");//几秒回调一次
                wifiListChange();
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {//密码错误
                int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                Log.e(TAG, "linkWifiResult：" + linkWifiResult);
                if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                    Toast.makeText(WifiListActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }


    /**
     * wifi列表改变
     */
    public void wifiListChange() {
        convertScanResult();
        WifiInfo connectedWifiInfo = mWifiHelper.getConnectionWifiInfo();
        wifiListSet(connectedWifiInfo.getSSID(), connectType);
    }

    /**
     * 获取wifi列表然后将bean转成自己定义的WifiBean
     */
    public void convertScanResult() {
        List<ScanResult> scanResults = mWifiHelper.getFilterScanResult();
        realWifiList.clear();
        if (!CollectionUtils.isNullOrEmpty(scanResults)) {
            for (int i = 0; i < scanResults.size(); i++) {
                WifiBean wifiBean = new WifiBean();
                ScanResult scanResult = scanResults.get(i);
                wifiBean.setWifiName(scanResult.SSID);
                wifiBean.setState(AppConstants.WIFI_STATE_UNCONNECT);   //只要获取都假设设置成未连接，真正的状态都通过广播来确定
                wifiBean.setCapabilities(scanResult.capabilities);
                wifiBean.setLevel(String.valueOf(mWifiHelper.getLevel(scanResult.level)));
                wifiBean.setScanResult(scanResult);
                realWifiList.add(wifiBean);
            }
        }
    }


    /**
     * 将"已连接"或者"正在连接"的wifi热点放置在第一个位置
     *
     * @param wifiName
     * @param type
     */
    public void wifiListSet(String wifiName, int type) {
        int index = -1;
        WifiBean wifiInfo = new WifiBean();
        if (CollectionUtils.isNullOrEmpty(realWifiList)) {
            return;
        }
        Collections.sort(realWifiList);//根据信号强度排序
        for (int i = 0; i < realWifiList.size(); i++) {
            WifiBean wifiBean = realWifiList.get(i);
            if (index == -1 && ("\"" + wifiBean.getWifiName() + "\"").equals(wifiName)) {
                index = i;
                wifiInfo.setLevel(wifiBean.getLevel());
                wifiInfo.setWifiName(wifiBean.getWifiName());
                wifiInfo.setCapabilities(wifiBean.getCapabilities());
                wifiInfo.setScanResult(wifiBean.getScanResult());
                if (type == 1) {
                    wifiInfo.setState(AppConstants.WIFI_STATE_CONNECT);
                } else {
                    wifiInfo.setState(AppConstants.WIFI_STATE_ON_CONNECTING);
                }
            }
        }
        if (index != -1) {
            realWifiList.remove(index);
            realWifiList.add(0, wifiInfo);
            adapter.notifyDataSetChanged();
        } else {
            adapter.notifyDataSetChanged();
        }
    }


    /**
     * 检查是否已经授予权限
     *
     * @return
     */
    private boolean checkPermission() {
        for (String permission : NEEDED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 申请权限
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                NEEDED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasAllPermission = true;
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    hasAllPermission = false;   //判断用户是否同意获取权限
                    break;
                }
            }

            //如果同意权限
            if (hasAllPermission) {
                mHasPermission = true;
                if (mWifiHelper.isWifiEnabled() && mHasPermission) {  //如果wifi开关是开 并且 已经获取权限
                    mWifiHelper.startScan();
                } else {
                    Toast.makeText(WifiListActivity.this, "WIFI处于关闭状态或权限获取失败", Toast.LENGTH_SHORT).show();
                }

            } else {  //用户不同意权限
                mHasPermission = false;
                Toast.makeText(WifiListActivity.this, "获取权限失败", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void showProgressBar() {
        pbWifiLoading.setVisibility(View.VISIBLE);
    }

    public void hidingProgressBar() {
        pbWifiLoading.setVisibility(View.GONE);
    }
}
