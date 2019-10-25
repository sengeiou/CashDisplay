package com.resonance.cashdisplay.http;

public class HttpConfig {
    private transient static final String TAG = "http_Config";

    public final int port = 8182;

    public boolean useAuth = true;
    public String username = "admin";
    public String password = "admin";

    private transient static HttpConfig instance;

    private HttpConfig() { }

    public static HttpConfig get() {
        if (instance == null) {
            final HttpConfig config = new HttpConfig();
            instance = config;
        }
        return instance;
    }

    public HttpConfig save() {
        return this;
    }
}

