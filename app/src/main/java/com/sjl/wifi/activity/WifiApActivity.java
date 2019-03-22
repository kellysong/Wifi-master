package com.sjl.wifi.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.sjl.wifi.R;
import com.sjl.wifi.util.WifiApUtils;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename WifiApActivity.java
 * @time 2019/3/20 10:48
 * @copyright(C) 2019 song
 */
public class WifiApActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * 权限请求码
     */
    private static final int PERMISSION_REQUEST_CODE = 0;
    /**
     * 系统设置修改请求码
     */
    private static final int SETTING_REQUEST_CODE = 1;

    /**
     * 两个危险权限需要动态申请，需要定位
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private boolean mHasPermission;

    private Switch mSwitch;
    private EditText mSsid;
    private EditText mPwd;

    private Button mSave;
    private Button mRecover;
    private HotspotReceiver hotspotReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();

    }


    private void initView() {
        setContentView(R.layout.wifi_ap_activity);
        mSwitch = findViewById(R.id.sw_ap);
        mSsid = findViewById(R.id.et_ssid);
        mPwd = findViewById(R.id.et_pwd);
        mSave = findViewById(R.id.btn_save);
        mRecover = findViewById(R.id.btn_recover);
    }


    private void initData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()){//有权限
                if (!Settings.System.canWrite(this)) { // 修改系统设置权限
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, SETTING_REQUEST_CODE);
                } else {
                    init();
                }
            }else {
                requestPermission();
            }
        } else {
            init();
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

    private void init() {
        mHasPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1){
            hotspotReceiver = new HotspotReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
            registerReceiver(hotspotReceiver, intentFilter);
        }else {
            if (WifiApUtils.isApOn(this)) {
                mSwitch.setChecked(true);
            }
        }
        String ssid = WifiApUtils.getSsid(this);
        String pwd = WifiApUtils.getPwd(this);
        mSsid.setText(ssid);
        mPwd.setText(pwd);

    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                NEEDED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    private class HotspotReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {//便携式热点的状态为：10---正在关闭；11---已关闭；12---正在开启；13---已开启
                int state = intent.getIntExtra("wifi_state", 0);
                if (state == 10) {
                } else if (state == 11) {
                    mSwitch.setChecked(false);
                } else if (state == 12) {
                } else if (state == 13) {
                    mSwitch.setChecked(true);
                }
            }
        }
    }


    private void initListener() {

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mHasPermission){
                    Toast.makeText(WifiApActivity.this, "权限不足", Toast.LENGTH_LONG).show();
                    mSwitch.setChecked(false);
                    return;
                }
                if (isChecked) {
                    openAp(true);
                } else {
                    WifiApUtils.closeWifiAp(WifiApActivity.this);
                }
            }
        });
        mSave.setOnClickListener(this);
        mRecover.setOnClickListener(this);
    }

    private void openAp(boolean flag) {
        if (!mHasPermission){
            Toast.makeText(WifiApActivity.this, "权限不足", Toast.LENGTH_LONG).show();
            return;
        }
        String ssid = mSsid.getText().toString().trim();
        String pwd = mPwd.getText().toString().trim();
        if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(pwd)) {
            Toast.makeText(WifiApActivity.this, "SSID和密码不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        if (pwd.length() < 8) {
            Toast.makeText(WifiApActivity.this, "密码至少8位", Toast.LENGTH_LONG).show();
            return;
        }
        if (flag) {
            boolean openWifiAp = WifiApUtils.openWifiAp(this, ssid, pwd);
            Log.i("SIMPLE_LOGGER","热点开启："+openWifiAp);
        } else {
            if (mSwitch.isChecked()){//处于热点开启状态，直接生效
                boolean openWifiAp = WifiApUtils.openWifiAp(this, ssid, pwd);
                Log.i("SIMPLE_LOGGER","热点开启："+openWifiAp);
            }else {//保存账号密码
                WifiApUtils.saveApInfo(this, ssid, pwd);
            }

        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save:
                //保存设置
                openAp(false);
                break;
            case R.id.btn_recover:
                //恢复默认
                mSsid.setText(WifiApUtils.DEFAULT_SSID);
                mPwd.setText(WifiApUtils.DEFAULT_PWD);
                break;
            default:
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hotspotReceiver != null) {
            unregisterReceiver(hotspotReceiver);
            hotspotReceiver = null;
        }
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(this)) { // 修改系统设置权限
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, SETTING_REQUEST_CODE);
                    } else {
                        init();
                    }
                }
            } else {  //用户不同意权限
                Toast.makeText(WifiApActivity.this, "未开启定位权限,请手动到设置去开启权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 判断是否有WRITE_SETTINGS权限
                if (Settings.System.canWrite(this)) {
                    //创建热点
                    init();
                } else {
                    //没有权限
                    Toast.makeText(WifiApActivity.this, "权限不足，请允许修改系统设置", Toast.LENGTH_LONG).show();

                }
            }
        }

    }
}
