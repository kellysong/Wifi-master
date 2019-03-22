package com.sjl.wifi.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback;
import android.os.Build;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.sjl.wifi.service.WifiApService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * wifi热点工具类
 * 前面7.0之前的方式是可以自由配置热点的ssid和密码的，
 * 8.0这种方式就只能是打开系统默认那个ssid和密码的热点了。
 *
 * @author Kelly
 * @version 1.0.0
 * @filename WifiApService.java
 * @time 2019/3/20 10:11
 * @copyright(C) 2019 song
 */
public class WifiApUtils {
    private static final String TAG = "WifiApUtils";

    public static final String config_ap = "config_ap";


    /**
     * ssid
     */
    public static final String DEFAULT_SSID = "docoy";
    /**
     * 密码至少8位
     */
    public static final String DEFAULT_PWD = "12345678";

    /**
     * 开启热点
     *
     * @param context  上下文
     * @param ssid     ssid
     * @param password 密码
     * @return
     */
    public static boolean openWifiAp(Context context, String ssid, String password) {
        return setWifiApEnabled(context, ssid, password, true);
    }

    /**
     * 关闭热点
     *
     * @param context 上下文
     * @return
     */
    public static boolean closeWifiAp(Context context) {
        return setWifiApEnabled(context, null, null, false);
    }

    /**
     * 开启/关闭热点
     *
     * @param context  上下文
     * @param ssid     ssid
     * @param password 密码
     * @param enabled  true打开，false关闭
     * @return
     */
    private static boolean setWifiApEnabled(final Context context, String ssid, String password, final boolean enabled) {
        //8.0这种方式就只能是打开系统默认那个ssid和密码的热点了，不支持设置ssid和密码
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            return setWifiApEnabledForAndroidO(context, enabled);
        }
        //适合7.1-8.1之间的安卓版本,不过这种方式打开的热点ssid是用UUID随机生成的，
        //类似:AndroidShare_7640,其中AndroidShare_是固定的，而后面的数字则是随机的,与系统设置里面的ssid和密码无关
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT <= 27) {
            if (enabled) {
                Intent startApService = new Intent(context, WifiApService.class);
                startApService.setAction("com.ap.hotspot");
                context.startService(startApService);
            } else {
                Intent startApService = new Intent(context, WifiApService.class);
                startApService.setAction("com.ap.hotspot");
                context.stopService(startApService);
            }
            return true;
        }
        //处理低版本，只适用于安卓7.0或7.0以下版本且版本>=4.0
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            // 因为6.0及其以下版本，在开启热点之前要先手动关闭wifi。以后版本就不需要了会自动关闭，热点关闭后也会自动打开
            wifiManager.setWifiEnabled(false);
            closeAp(context);
            WifiConfiguration apConfig = null;
            if (enabled) {
                if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
                    return false;
                }
                // 热点的配置类
                apConfig = getApConfig(ssid, password, 2);
            }
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            return (Boolean) method.invoke(wifiManager, apConfig, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 8.0 开启热点方法
     * 注意：这个方法开启的热点名称和密码是手机系统里面默认的那个
     * 权限： android.permission.OVERRIDE_WIFI_CONFIG
     *
     * @param context
     */
    private static boolean setWifiApEnabledForAndroidO(Context context, boolean isEnable) {
        ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Field iConnMgrField = null;
        try {
            iConnMgrField = connManager.getClass().getDeclaredField("mService");
            iConnMgrField.setAccessible(true);
            Object iConnMgr = iConnMgrField.get(connManager);
            Class<?> iConnMgrClass = Class.forName(iConnMgr.getClass().getName());

            if (isEnable) {
                Method startTethering = iConnMgrClass.getMethod("startTethering", int.class, ResultReceiver.class, boolean.class);
                startTethering.invoke(iConnMgr, 0, null, true);
            } else {
                Method startTethering = iConnMgrClass.getMethod("stopTethering", int.class);
                startTethering.invoke(iConnMgr, 0);
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, "8.0开启热点异常", e);
            return false;
        }
    }


    /**
     * 判断热点是否开启
     *
     * @param context
     * @return
     */
    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable e) {
            Log.e(TAG, "判断热点是否开启", e);
        }
        return false;
    }


    /**
     * 关闭热点
     *
     * @param context
     */
    public static void closeAp(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取开启热点后的IP地址
     *
     * @param context
     * @return
     */
    public static String getHotspotLocalIpAddress(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wifimanager.startLocalOnlyHotspot(new LocalOnlyHotspotCallback() {

            }, null);
        }
        DhcpInfo dhcpInfo = wifimanager.getDhcpInfo();
        if (dhcpInfo != null) {
            int address = dhcpInfo.serverAddress;
            return ((address & 0xFF)
                    + "." + ((address >> 8) & 0xFF)
                    + "." + ((address >> 16) & 0xFF)
                    + "." + ((address >> 24) & 0xFF));
        }
        return null;
    }

    /**
     * 设置热点
     *
     * @param ssid     热点名称
     * @param password 热点密码
     * @param type     加密类型
     * @return
     */
    public static WifiConfiguration getApConfig(String ssid, String password, int type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = ssid;
        if (type == 0) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (type == 1) {
            config.wepKeys[0] = password;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == 2) {//   WPA/WPA2 PSK的加密方式都可以通过此方法连上热点  也就是说我们连接热点只用分为有密码和无密码情况
            config.preSharedKey = password;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    /**
     * 保存ssid信息
     *
     * @param context
     * @param ssid
     * @param pwd
     */
    public static void saveApInfo(Context context, String ssid, String pwd) {
        SharedPreferences preferences = context.getSharedPreferences(config_ap, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("ssid", ssid);
        editor.putString("pwd", pwd);
        editor.commit();

    }

    /**
     * 获取ssid
     *
     * @param context
     */
    public static String getSsid(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(config_ap, Context.MODE_PRIVATE);
        return preferences.getString("ssid", DEFAULT_SSID);
    }

    /**
     * 获取pwd
     *
     * @param context
     */
    public static String getPwd(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(config_ap, Context.MODE_PRIVATE);
        return preferences.getString("pwd", DEFAULT_PWD);
    }
}
