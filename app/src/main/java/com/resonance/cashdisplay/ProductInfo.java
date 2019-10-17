package com.resonance.cashdisplay;

import android.app.Activity;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.resonance.cashdisplay.databinding.ActivityMainBinding;



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
    private String ToPay;//к оплате
    private String InsertCash;//внесено
    private String Reshta;//решта
    private String Znijka;
    private String Bonusi;
    private String Info;




    private String statusConnection;
    private String statusConnection2;
    ImageView imageViewQR;
    ImageView imageViewQRDiscount;
    ImageView imageViewProstor;
    ImageView imageViewProstor1;


    Activity activity;
    public ProductInfo(Activity activity){
        this.activity = activity;
        this.productName = "";//mainLayer
        this.weight = "";//mainLayer
        this.price = "";//mainLayer
        this.sum = "";//mainLayer
        this.action = "";//mainLayer
        this.zagvart = "";//mainLayer
        this.ToPay = "";//
        this.InsertCash = "";//
        this.Reshta = "";//
        this.Znijka = "";
        this.Bonusi = "";//
        this.qrInfo = "";
        this.Info = "Будь ласка, не забудьте забрати чек. Перевiряйте решту не вiдходячи вiд каси";
        this.statusConnection = "";
        this.statusConnection2 = statusConnection;

    }

    @Bindable
    public String getInfo(){return this.Info;}
    @Bindable
    public void setInfo(String str)
    {
        this.Info = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.info);
    }
    @Bindable
    public String getStatusConnection(){return this.statusConnection;}

    @Bindable
    public void setStatusConnection(String str)
    {
        this.statusConnection = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.statusConnection);
    }


    @Bindable
    public String getStatusConnection2(){return this.statusConnection2;}
    @Bindable
    public void setStatusConnection2(String str)
    {
        this.statusConnection2 = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.statusConnection2);
    }


    @Bindable
    public String getBonusi(){return this.Bonusi;}
    @Bindable
    public void setBonusi(String str)
    {
        this.Bonusi = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.bonusi);
    }

    @Bindable
    public String getZnijka(){return this.Znijka;}
    @Bindable
    public void setZnijka(String str){
        this.Znijka = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.znijka);
    }
    @Bindable
    public String getReshta(){return this.Reshta;}
    @Bindable
    public void setReshta(String str){
        this.Reshta = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.reshta);
    }
    @Bindable
    public String getInsertCash(){return this.InsertCash;}
    @Bindable
    public void setInsertCash(String str){
        this.InsertCash = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.insertCash);
    }

    @Bindable
    public String getToPay(){return  this.ToPay;}
    @Bindable
    public void setToPay(String str){
        this.ToPay = str;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.toPay);
    }

    @Bindable
    public String getZagvart(){return this.zagvart;}

    @Bindable
    public void setZagvart(String vart){
        this.zagvart = vart;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.zagvart);
    }

    @Bindable
    public String getQrInfo(){
        return this.qrInfo ;
    }
    public void setQrInfo(String info){
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
    public String getAction(){
        return this.action;
    }

    @Bindable
    public String getImageUrl() {return this.imageUrl;}

    public void  setProductName(String productName) {
        this.productName = productName;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.productName);
    }
    public void setPrice(String price) {
        this.price = price;
        notifyPropertyChanged( com.resonance.cashdisplay.BR.price);
    }
    public void setWeight(String weight)
    {
        this.weight = weight;
        notifyPropertyChanged( com.resonance.cashdisplay.BR.weight);
    }
    public void setAction(String action)
    {
        this.action = action;
        notifyPropertyChanged( com.resonance.cashdisplay.BR.action);
    }
    public void setSum(String sum)
    {
        this.sum = sum;
        notifyPropertyChanged( com.resonance.cashdisplay.BR.sum);
    }
    public void  setLine1(String Line1) {
        this.line1 = Line1;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.line1);
    }

    public void  setLine2(String Line2) {
        this.line2 = Line2;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.line2);
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
        notifyPropertyChanged(com.resonance.cashdisplay.BR.imageUrl);

    }




}

