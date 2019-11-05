package com.resonance.cashdisplay.shopping_list;


public class ItemProductList {
    private int indexPosition = -1;
    private int npp = 0;
    private String codTovara = "";
    private String nameTovara = "";
    private int divisible = 0;
    private long count = 0;
    private long price = 0;
    private float sumWithoutDiscount = 0;   // calculated value
    private float discount = 0;              // calculated value
    private long summ = 0;      // value from command from COM port

    public ItemProductList() {
        this.indexPosition = -1;
    }

    public ItemProductList(int indexPosition, String codTovara, String nameTovara, int divisible,
                           long count, long price, long summ, long discount, long sumWithoutDiscount) {
        this.indexPosition = indexPosition;
        this.codTovara = codTovara;
        this.nameTovara = nameTovara;
        this.divisible = divisible;
        this.count = count;
        this.price = price;
        this.sumWithoutDiscount = sumWithoutDiscount;
        this.discount = discount;
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

    public float getDiscount() {
        float amount = ((divisible == 1) ? ((float) count / 1000) : count);
        discount = (amount * price) - summ;
        return discount;
    }

    public float getSumWithoutDiscount() {
        float amount = ((divisible == 1) ? ((float) count / 1000) : count);
        sumWithoutDiscount = amount * price;
        return sumWithoutDiscount;
    }
}
