package com.resonance.cashdisplay;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

/**
 * Created by Святослав on 06.05.2016.
 */

public class ViewModel extends BaseObservable {

    private String line1;
    private String line2;
    private String statusConnection;
    private String statusConnection2;

    public ViewModel() {
        this.statusConnection = "";
        this.statusConnection2 = statusConnection;

    }

    @Bindable
    public String getStatusConnection() {
        return this.statusConnection;
    }

    @Bindable
    public void setStatusConnection(String str) {
        this.statusConnection = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.statusConnection);
    }

    @Bindable
    public String getStatusConnection2() {
        return this.statusConnection2;
    }

    @Bindable
    public void setStatusConnection2(String str) {
        this.statusConnection2 = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.statusConnection2);
    }

    @Bindable
    public String getLine1() {
        return this.line1;
    }

    @Bindable
    public void setLine1(String line1) {
        this.line1 = line1;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.line1);
    }

    @Bindable
    public String getLine2() {
        return this.line2;
    }

    @Bindable
    public void setLine2(String line2) {
        this.line2 = line2;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.line2);
    }
}

