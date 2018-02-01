package com.lyy.showwifipass;

/**
 * Created by lyy on 2018/2/1.
 */

public class WifiInfo {
    private String ssid;

    private String psk;

    private String priority;

    private String key_mgmt;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPsk() {
        return psk;
    }

    public void setPsk(String psk) {
        this.psk = psk;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getKey_mgmt() {
        return key_mgmt;
    }

    public void setKey_mgmt(String key_mgmt) {
        this.key_mgmt = key_mgmt;
    }
}
