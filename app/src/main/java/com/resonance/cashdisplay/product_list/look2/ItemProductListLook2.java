package com.resonance.cashdisplay.product_list.look2;

/**
 * NOT USED. W
 * Special variant of {@link ItemProductList}, where the name of specific product is parsed.
 */

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.product_list.ItemProductList;

@Deprecated
public class ItemProductListLook2 extends ItemProductList {

    public static final String TAG = ItemProductList.class.getSimpleName();

    public ItemProductListLook2() {
        super();
    }

    /**
     * Real samples:
     * ПРОЇЗНИЙ НА ЧЕРВЕНЬ (46 ПОЇЗДОК)
     * ПРОЇЗНИЙ НА ТРАВЕНЬ (46 ПОЇЗДОК)
     * ПРОЇЗНИЙ НА  ТРАВЕНЬ(124 ПОЇЗДКИ)
     * ПРОЇЗНИЙ НА ДРУГУ ПОЛОВИНУ МIСЯЦЯ
     * Possible:
     * ПРОЇЗНИЙ НА ЧЕРВЕНЬ (1 ПОЇЗДОК)
     * ПРОЇЗНИЙ НА МАЙ (57 ПОЇЗДКИ)
     * ПРОЇЗНИЙ НА ЛИСТОПАД (124 ПОЇЗДКА)
     *
     * @param name
     */
    public void setName(String name) {
        super.setName(name);

        String regex = "ПРОЇЗНИЙ НА[^\\(]+\\(\\d{1,4} ПОЇЗД[КО][АИК]\\)";
        if (name.matches(regex)) {
            String parsedName;
            int parsedCount;
            try {
                parsedName = name.substring(0, name.indexOf("(")).trim();
            } catch (Exception e) {
                Log.e(TAG, "setName(): parsedName, name=" + name + ", " + e);
                return;
            }
            try {
                String count = name.substring(name.indexOf("(") + 1, name.indexOf("ПОЇЗД")).trim();
                parsedCount = Integer.parseInt(count);
            } catch (Exception e) {
                Log.e(TAG, "setName(): parsedCount, name=" + name + ", " + e);
                return;
            }
            super.setName(parsedName);
            super.setCount(parsedCount);
        }
    }
}
