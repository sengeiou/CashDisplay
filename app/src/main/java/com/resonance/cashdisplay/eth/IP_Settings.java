package com.resonance.cashdisplay.eth;

public class IP_Settings {

    private String ip, netmask, dns, gateway;

    public IP_Settings() {
        ip = "";
        netmask = "";
        gateway = "";
        dns = "";
    }

    public void setIp(String ipAddress) {
        ip = ipAddress;
    }

    public void setNetmask(String Netmask) {
        netmask = Netmask;
    }

    public void setGateway(String Gateway) {
        gateway = Gateway;
    }


    public String getIp() {
        return this.ip;
    }

    public String getNetmask() {
        return this.netmask;
    }

    public String getGateway() {
        return this.gateway;
    }
}
