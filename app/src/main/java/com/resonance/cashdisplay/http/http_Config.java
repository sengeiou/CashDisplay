package com.resonance.cashdisplay.http;

public class http_Config {
    private transient static final String TAG = "http_Config";

    public final int port = 8182;

    public boolean useAuth = true;
    public String username = "admin";
    public String password = "admin";

    private transient static http_Config instance;

    private http_Config() { }

    public static http_Config get() {
        if (instance == null) {
            final http_Config config = new http_Config();
            instance = config;
        }
        return instance;
    }

    public http_Config save() {
        return this;
    }

}

