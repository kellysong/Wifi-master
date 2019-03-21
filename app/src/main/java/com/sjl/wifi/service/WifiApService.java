package com.sjl.wifi.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * 8.0版本开启热点。这种方法不用我们去设置账号密码，系统会随机产生，并且在回调方法中可以直接拿到。
 * 这里需要注意的是要在远程服务中去开启，否则会出现退出到后台热点关闭的情况
 *
 * @author Kelly
 * @version 1.0.0
 * @filename WifiApService.java
 * @time 2019/3/21 9:50
 * @copyright(C) 2019 song
 */
public class WifiApService extends Service {
    private static final String TAG = "WifiApService";
    private WifiManager.LocalOnlyHotspotReservation reservation;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        openAp();
    }

    private void openAp() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    WifiApService.this.reservation = reservation;
                    String ssid = reservation.getWifiConfiguration().SSID;
                    String pwd = reservation.getWifiConfiguration().preSharedKey;
                    Log.i(TAG, "app开启热点：" + "ssid:" + ssid + "pwd:" + pwd);
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.w(TAG, "app开启热点：热点已关闭");
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    Log.e(TAG, "app开启热点：热点开启失败reason:" + reason);
                }

            }, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (reservation != null){
            reservation.close();
        }
    }
}
