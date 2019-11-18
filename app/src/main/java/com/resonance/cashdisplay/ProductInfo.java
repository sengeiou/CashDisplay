package com.resonance.cashdisplay;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.widget.ImageView;


/**
 * Created by Святослав on 06.05.2016.
 */

public class ProductInfo extends BaseObservable {

    private String productName;
    private String weight;
    private String price;
    private String sum;
    private String action;
    private String imageUrl;
    private String line1;
    private String line2;
    private String qrInfo;
    private String zagvart;//загальна вартисть
    private String toPay;//к оплате
    private String insertCash;//внесено
    private String reshta;//решта
    private String znijka;
    private String bonusi;
    private String info;

    private String statusConnection;
    private String statusConnection2;
    ImageView imageViewQR;
    ImageView imageViewQRDiscount;
    ImageView imageViewProstor;
    ImageView imageViewProstor1;

    Activity activity;

    public ProductInfo(Activity activity) {
        this.activity = activity;
        this.productName = "";//mainLayer
        this.weight = "";//mainLayer
        this.price = "";//mainLayer
        this.sum = "";//mainLayer
        this.action = "";//mainLayer
        this.zagvart = "";//mainLayer
        this.toPay = "";//
        this.insertCash = "";//
        this.reshta = "";//
        this.znijka = "";
        this.bonusi = "";//
        this.qrInfo = "";
        this.info = "Будь ласка, не забудьте забрати чек. Перевiряйте решту не вiдходячи вiд каси";
        this.statusConnection = "";
        this.statusConnection2 = statusConnection;

    }

    @Bindable
    public String getInfo() {
        return this.info;
    }

    @Bindable
    public void setInfo(String str) {
        this.info = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.info);
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
    public String getBonusi() {
        return this.bonusi;
    }

    @Bindable
    public void setBonusi(String str) {
        this.bonusi = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.bonusi);
    }

    @Bindable
    public String getZnijka() {
        return this.znijka;
    }

    @Bindable
    public void setZnijka(String str) {
        this.znijka = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.znijka);
    }

    @Bindable
    public String getReshta() {
        return this.reshta;
    }

    @Bindable
    public void setReshta(String str) {
        this.reshta = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.reshta);
    }

    @Bindable
    public String getInsertCash() {
        return this.insertCash;
    }

    @Bindable
    public void setInsertCash(String str) {
        this.insertCash = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.insertCash);
    }

    @Bindable
    public String getToPay() {
        return this.toPay;
    }

    @Bindable
    public void setToPay(String str) {
        this.toPay = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.toPay);
    }

    @Bindable
    public String getZagvart() {
        return this.zagvart;
    }

    @Bindable
    public void setZagvart(String vart) {
        this.zagvart = vart;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.zagvart);
    }

    @Bindable
    public String getQrInfo() {
        return this.qrInfo;
    }

    public void setQrInfo(String info) {
        this.qrInfo = info;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.qrInfo);
    }

    @Bindable
    public String getLine1() {
        return this.line1;
    }

    @Bindable
    public String getLine2() {
        return this.line2;
    }

    @Bindable
    public String getProductName() {
        return this.productName;
    }

    @Bindable
    public String getPrice() {
        return this.price;
    }

    @Bindable
    public String getWeight() {
        return this.weight;
    }

    @Bindable
    public String getSum() {
        return this.sum;
    }

    @Bindable
    public String getAction() {
        return this.action;
    }

    @Bindable
    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setProductName(String productName) {
        this.productName = productName;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.productName);
    }

    public void setPrice(String price) {
        this.price = price;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.price);
    }

    public void setWeight(String weight) {
        this.weight = weight;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.weight);
    }

    public void setAction(String action) {
        this.action = action;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.action);
    }

    public void setSum(String sum) {
        this.sum = sum;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.sum);
    }

    public void setLine1(String line1) {
        this.line1 = line1;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.line1);
    }

    public void setLine2(String line2) {
        this.line2 = line2;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.line2);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.imageUrl);
    }
}

