package com.resonance.cashdisplay.shopping_list;


public class ItemShoppingList {
    public String action = "";
    private int indexPosition = -1;
    private int npp = 0;
    private String codTovara = "";
    private String nameTovara = "";
    private int divisible = 0;
    private long count = 0;
    private long price = 0;
    private long summ = 0;

    private long discount = 0;
    private long summToPay = 0;

    public ItemShoppingList() {
        this.indexPosition = -1;
    }

    public ItemShoppingList(int indexPosition, String codTovara, String nameTovara, int divisible,
                            long count, long price, long summ) {
        this.indexPosition = indexPosition;
        this.codTovara = codTovara;
        this.nameTovara = nameTovara;
        this.divisible = divisible;
        this.count = count;
        this.price = price;
        this.summ = summ;
    }

    public int getIndexPosition() {
        return indexPosition;
    }

    public void setIndexPosition(int indexPosition) {
        this.indexPosition = indexPosition;
    }

    public int getNpp() {
        return npp;
    }

    public void setNpp(int npp) {
        this.npp = npp;
    }

    public String getCodTovara() {
        return codTovara;
    }

    public void setCodTovara(String codTovara) {
        this.codTovara = codTovara;
    }

    public String getNameTovara() {
        return nameTovara;
    }

    public void setNameTovara(String nameTovara) {
        this.nameTovara = nameTovara;
    }

    public int getDivisible() {
        return divisible;
    }

    public void setDivisible(int divisible) {
        this.divisible = divisible;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getSumm() {
        return summ;
    }

    public void setSumm(long summ) {
        this.summ = summ;
    }

    public long getDiscount() {
        return discount;
    }

    public void setDiscount(long discount) {
        this.discount = discount;
    }

    public long getSummToPay() {
        return summToPay;
    }

    public void setSummToPay(long summToPay) {
        this.summToPay = summToPay;
    }
}
