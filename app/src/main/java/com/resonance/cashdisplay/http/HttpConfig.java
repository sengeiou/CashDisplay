package com.resonance.cashdisplay.http;

public class HttpConfig {
    private transient static final String TAG = "HttpConfig";

    public final int port = 8182;

    public boolean useAuth = true;
    public String userName = "admin";
    public String password = "admin";

    public String userNameTestMode = "otk";

    public String superUser = "OOPS!!! LOOKS LIKE I FORGOT MY LOGIN FOREVER (((";
    public String superPassword = "AND PASSWORD I FORGOT TOO...";

    private transient static HttpConfig instance;

    private HttpConfig() { }

    public static HttpConfig get() {
        if (instance == null) {
            final HttpConfig config = new HttpConfig();
            instance = config;
        }
        return instance;
    }

    public void setAuthData(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
}

