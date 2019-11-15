package com.resonance.cashdisplay.http;

public class HttpConfig {
    private transient static final String TAG = "HttpConfig";

    public final int port = 8182;

    public boolean useAuth = true;
    public String userName = "admin";
    public String password = "admin";

    public String userNameTestMode = "otk";

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

