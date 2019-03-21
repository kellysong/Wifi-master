package com.sjl.wifi.bean;

import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;


public class WifiBean implements Comparable<WifiBean>, Parcelable {
    private static final long serialVersionUID = -7060210544600464481L;

    private String wifiName;
    private String level;
    private String state;  //已连接  正在连接  未连接 三种状态
    private String capabilities;//加密方式

    private ScanResult scanResult;//原生的对象ScanResult

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public int compareTo(WifiBean o) {
        int level1 = Integer.parseInt(getLevel());
        int level2 = Integer.parseInt(o.getLevel());
        if (level1 > level2) {//降序
            return -1;
        } else if (level1 == level2) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "自定义WifiBean{" +
                "wifiName='" + wifiName + '\'' +
                ", level='" + level + '\'' +
                ", state='" + state + '\'' +
                ", capabilities='" + capabilities + '\'' +
                ",\n 扫描点scanResult=" + scanResult +
                '}';
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(wifiName);
        dest.writeString(level);
        dest.writeString(state);
        dest.writeString(capabilities);
        dest.writeValue(scanResult);

    }

    public static final Parcelable.Creator<WifiBean> CREATOR = new Parcelable.Creator<WifiBean>() {

        @Override
        public WifiBean createFromParcel(Parcel source) {

            WifiBean model = new WifiBean();
            model.wifiName = source.readString();
            model.level = source.readString();
            model.state = source.readString();
            model.capabilities = source.readString();
            model.scanResult = (ScanResult) source.readValue(ScanResult.class.getClassLoader());
            return model;
        }

        @Override
        public WifiBean[] newArray(int size) {
            return new WifiBean[size];
        }
    };
}
