package com.resonance.cashdisplay.shopping_list;



public class ItemShoppingList {
    public String Action = "";
    private int indexPosition = -1;
    private int Npp = 0;
    private String codTovara = "";
    private String nameTovara = "";
    private int divisible = 0;
    private long count = 0;
    private long price = 0;
    private long summ = 0;

    public int getNpp() {
        return Npp;
    }

    public void setNpp(int npp) {
        Npp = npp;
    }

    public int getIndexPosition() {
        return indexPosition;
    }

    public void setIndexPosition(int indexPosition) {
        this.indexPosition = indexPosition;
    }

    public void setCodTovara(String codTovara) {
        this.codTovara = codTovara;
    }

    public void setNameTovara(String nameTovara) {
        this.nameTovara = nameTovara;
    }

    public void setDivisible(int divisible) {
        this.divisible = divisible;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public void setSumm(long summ) {
        this.summ = summ;
    }

    public String getCodTovara() {
        return codTovara;
    }

    public String getNameTovara() {
        return nameTovara;
    }

    public int getDivisible() {
        return divisible;
    }

    public long getCount() {
        return count;
    }

    public long getPrice() {
        return price;
    }

    public long getSumm() {
        return summ;
    }

    public ItemShoppingList()
    {
        indexPosition = -1;
    }
    public ItemShoppingList(int index_Position, String cod_Tovara, String name_Tovara, int _divisible, long _count, long _price, long _summ){
        indexPosition = index_Position;
        codTovara = cod_Tovara;
        nameTovara = name_Tovara;
        divisible = _divisible;
        count = _count;
        price = _price;
        summ = _summ;
    }
}
