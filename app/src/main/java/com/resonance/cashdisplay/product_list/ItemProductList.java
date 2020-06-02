package com.resonance.cashdisplay.product_list;

public class ItemProductList {
    private int indexPosition = -1;
    private int n = 0;
    private String code = "";
    private int divisible = 0;
    private int count = 0;
    private long price = 0;
    private long sum = 0;                  // value from command from COM port
    private String name = "";
    private long sumWithoutDiscount = 0;   // calculated value
    private long discount = 0;             // calculated value

    public ItemProductList() {
        this.indexPosition = -1;
    }

    public ItemProductList(int indexPosition, String code, int divisible, int count,
                           long price, long sum, String name, long sumWithoutDiscount, long discount) {
        this.indexPosition = indexPosition;
        this.code = code;
        this.divisible = divisible;
        this.count = count;
        this.price = price;
        this.sum = sum;
        this.name = name;
        this.sumWithoutDiscount = sumWithoutDiscount;
        this.discount = discount;
    }

    public int getIndexPosition() {
        return indexPosition;
    }

    public void setIndexPosition(int indexPosition) {
        this.indexPosition = indexPosition;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getDivisible() {
        return divisible;
    }

    public void setDivisible(int divisible) {
        this.divisible = divisible;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getSum() {
        return sum;
    }

    public void setSum(long sum) {
        this.sum = sum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSumWithoutDiscount() {
        double amount = ((divisible == 1) ? (((double) count) / 1000) : count);
        sumWithoutDiscount = Math.round(amount * price);
        return sumWithoutDiscount;
    }

    public long getDiscount() {
        discount = getSumWithoutDiscount() - sum;
        return discount;
    }
}
