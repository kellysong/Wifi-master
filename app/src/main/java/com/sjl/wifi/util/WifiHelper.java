package com.sjl.wifi.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * wifi操作辅助类
 *
 * @author Kelly
 * @version 1.0.0
 * @filename WifiHelper.java
 * @time 2019/3/13 13:48
 * @copyright(C) 2019 xxx有限公司
 */
public class WifiHelper {
    private static final String TAG = "WifiHelper";
    private Context context;

    /**
     * 定义一个WifiManager对象,提供Wifi管理的各种主要API，主要包含wifi的扫描、建立连接、配置信息等
     */
    private WifiManager mWifiManager;


    public WifiHelper(Context context) {
        this.context = context;
        // 获得WifiManager对象
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
    }


    /**
     * 打开wifi
     */
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    /**
     * 关闭wifi
     */
    public void closeWifi() {
        mWifiManager.setWifiEnabled(false);
    }

    /**
     * 判断wifi是否打开
     *
     * @return
     */
    public boolean isWifiEnabled() {
        WifiManager wifimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifimanager.isWifiEnabled();
    }

    /**
     * wifi扫描
     */
    public void startScan() {
        boolean scanResult = mWifiManager.startScan(); //最好检查下返回值，因为这个方法可能会调用失败
        Log.i(TAG, "scanResult: " + scanResult);
    }


    /**
     * 断开指定ID的网络
     *
     * @param netId
     */
    public void disConnectionWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }


    /**
     * 判断当前是否已经连接
     *
     * @param ssid
     * @return
     */
    public boolean isGivenWifiConnect(String ssid) {
        return isWifiConnected() && getCurrentWifiSSID().equals(ssid);
    }

    /**
     * 得到当前连接的WiFi  SSID
     *
     * @return
     */
    public String getCurrentWifiSSID() {
        String ssid = mWifiManager.getConnectionInfo().getSSID();
        if (ssid.substring(0, 1).equals("\"")
                && ssid.substring(ssid.length() - 1).equals("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return trimSSID(ssid);
    }

    /**
     * 获取到的wifi名称前后可能带有引号
     *
     * @param ssid
     * @return
     */
    public String trimSSID(String ssid) {
        if (ssid.substring(0, 1).equals("\"")
                && ssid.substring(ssid.length() - 1).equals("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }


    /**
     * 是否处于wifi连接的状态
     */
    public boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        } else if (wifiNetworkInfo.isAvailable()) {
            return true;
        }
        return false;
    }


    /**
     * 连接wifi
     *
     * @param ssid         服务标志
     * @param password     密码
     * @param capabilities 安全性
     * @return
     */
    public boolean connectWifi(String ssid, String password, String capabilities) {
        WifiConfiguration mWifiConfiguration;
        //检测指定SSID的WifiConfiguration 是否存在
        WifiConfiguration tempConfig = isExsits(ssid);
        boolean enabled;
        if (tempConfig == null) {
            //创建一个新的WifiConfiguration ，CreateWifiInfo()需要自己实现
            mWifiConfiguration = createWifiInfo(ssid, password, getWifiCipherWay(capabilities));
            int wcgID = mWifiManager.addNetwork(mWifiConfiguration);
            enabled = mWifiManager.enableNetwork(wcgID, true);
        } else {
            //发现指定WiFi，并且这个WiFi以前连接成功过
            mWifiConfiguration = tempConfig;
            enabled = mWifiManager.enableNetwork(mWifiConfiguration.networkId, true);

        }
        Log.i(TAG, "enableNetwork:" + enabled);
        if (enabled) { //若失败，则连接之前成功过的网络
            boolean reconnect = mWifiManager.reconnect();
            Log.i(TAG, "reconnect:" + reconnect);
        }
        return enabled;
    }

    /**
     * 添加一个网络并连接
     *
     * @param ssid           服务标志
     * @param password       密码
     * @param wifiCipherType 安全性枚举
     */
    public void addNetwork(String ssid, String password, WifiCipherType wifiCipherType) {
        switch (wifiCipherType) {
            case WIFICIPHER_NOPASS:
                connectWifi(ssid, password, "NONE");
                break;
            case WIFICIPHER_WPA:
                connectWifi(ssid, password, "WPA");
                break;
            case WIFICIPHER_WEP:
                connectWifi(ssid, password, "WEP");
                break;
            default:
                break;
        }
    }


    /**
     * 移除某一个网络
     *
     * @param netId
     */
    private boolean removeNetwork(int netId) {
        boolean removeNetwork = mWifiManager.removeNetwork(netId);
        mWifiManager.saveConfiguration();
        Log.i(TAG, "removeNetwork: " + removeNetwork);
        return removeNetwork;
    }

    /**
     * 忘记密码,并断开网路连接
     *
     * @param targetSsid
     */
    public void removeWifiBySsid(String targetSsid) {
        Log.d(TAG, "try to removeWifiBySsid, targetSsid=" + targetSsid);
        //返回已经配置的WifiConfiguration对象列表
        List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfig : wifiConfigs) {
            String ssid = wifiConfig.SSID;
            if (ssid.equals(targetSsid)) {
                Log.d(TAG, "removeWifiBySsid success, SSID = " + wifiConfig.SSID + " netId = " + String.valueOf(wifiConfig.networkId));
                disConnectionWifi(wifiConfig.networkId);
                removeNetwork(wifiConfig.networkId);
            }
        }
    }


    /**
     * 创建一个wifi连接配置
     *
     * @param SSID
     * @param Password
     * @param Type
     * @return
     */
    public WifiConfiguration createWifiInfo(String SSID, String Password,
                                            WifiCipherType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        WifiConfiguration tempConfig = this.isExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == WifiCipherType.WIFICIPHER_NOPASS) // WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WifiCipherType.WIFICIPHER_WEP) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WifiCipherType.WIFICIPHER_WPA) // WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }


    /**
     * 查看以前是否也配置过这个网络
     *
     * @param ssid
     * @return
     */
    public WifiConfiguration isExsits(String ssid) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * 加密方式枚举
     */
    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }


    /**
     * 判断wifi支持的加密方式
     *
     * @param capabilities
     */
    public WifiCipherType getWifiCipherWay(String capabilities) {
        if (TextUtils.isEmpty(capabilities)) {
            return WifiCipherType.WIFICIPHER_INVALID;//无效
        } else if (capabilities.contains("WEP")) {
            return WifiCipherType.WIFICIPHER_WEP;
        } else if (capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPS")) {
            return WifiCipherType.WIFICIPHER_WPA;
        } else {
            return WifiCipherType.WIFICIPHER_NOPASS;
        }
    }


    /**
     * 获得手机扫描到的所有wifi的信息
     */
    public List<ScanResult> getScanResults() {
        return mWifiManager.getScanResults();
    }


    /**
     * 以 SSID 为关键字，过滤掉信号弱的选项
     *
     * @param scanResults
     * @return
     */
    public List<ScanResult> filterScanResult(List<ScanResult> scanResults) {
        Map<String, ScanResult> linkedMap = new LinkedHashMap<>(scanResults.size());
        for (ScanResult rst : scanResults) {
            if (!TextUtils.isEmpty(rst.SSID)) {
                if (linkedMap.containsKey(rst.SSID)) {
                    if (rst.level > linkedMap.get(rst.SSID).level) {
                        linkedMap.put(rst.SSID, rst);
                    }
                    continue;
                }
                linkedMap.put(rst.SSID, rst);
            } else {
                continue;
            }
        }
        scanResults.clear();
        scanResults.addAll(linkedMap.values());
        return scanResults;
    }

    /**
     * 获取过滤的wifi扫描列表（过滤重复项）
     *
     * @return
     */
    public List<ScanResult> getFilterScanResult() {
        return filterScanResult(getScanResults());
    }


    /**
     * 获取当前已连接上的wifi的信息
     *
     * @return
     */
    public WifiInfo getConnectionWifiInfo() {
        // 取得WifiInfo对象
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 获取已经保存的网络列表
     */
    public List<WifiConfiguration> getConfiguredNetworks() {
        return mWifiManager.getConfiguredNetworks();
    }

    /**
     * wifi ip地址转换
     *
     * @param i
     * @return
     */
    public String intToIp(int i) {

        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    /**
     * 返回wifi level 等级
     */
    public int getLevel(int level) {
        if (Math.abs(level) < 50) {
            return 1;
        } else if (Math.abs(level) < 75) {
            return 2;
        } else if (Math.abs(level) < 90) {
            return 3;
        } else {
            return 4;
        }
    }


}
