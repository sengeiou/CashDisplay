package com.resonance.cashdisplay.shopping_list;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Класс обрабатывает экран "Список товаров"
 */
public class ProductListWorker {

    private final static String TAG = "ProductListWorker";

    private final byte SYMBOL_SEPARATOR = (byte) 0x03;

    private Context context;

    private AdapterProductList adapterProductList;
    private ArrayList<ItemProductList> arrayProductList = new ArrayList<ItemProductList>();

    public ProductListWorker(Context context) {
        this.context = context;
        Log.d(TAG, "New ShoppingListWorker created.");
    }

    /**
     * @param lookCode // value of {@link PreferencesValues#productListLookCode}
     */
    public AdapterProductList createAdapterProductList(int lookCode) {
        int resource;
        switch (lookCode) {
            case 0:
                resource = R.layout.list_item_look_0;
                break;
            case 1:
                resource = R.layout.list_item_look_1;
                break;
            default:
                resource = R.layout.list_item_look_0;
                break;
        }
        adapterProductList = new AdapterProductList(context, resource, arrayProductList);

        return adapterProductList;
    }

    /**
     * Добавлен вывод на экран отладочной информации
     */
    public void addProductDebug(final String msg) {

        final int start = MainActivity.textViewDEBUG.getText().length();
        MainActivity.textViewDEBUG.append(msg + "\n");
        int end = MainActivity.textViewDEBUG.getText().length();
        Spannable spannableText = (Spannable) MainActivity.textViewDEBUG.getText();
        spannableText.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, 0);
        MainActivity.mScrollView.fullScroll(View.FOCUS_DOWN);
    }

    /**
     * Добавляет товар в список
     *
     * @param param строка "сырых" данных
     */
    public void addTovarList(String param) {

        final ItemProductList item = parseData(param);
        if (item.getIndexPosition() < 0)
            return;
        if (item.getIndexPosition() <= arrayProductList.size()) {
            addProductDebug(param);
            arrayProductList.add(item.getIndexPosition(), item);
            updateScreen(item.getIndexPosition());
        } else {
            showToast("Невiрнi параметри при внесеннi товару, необхідно очистити чек!");
            Log.d(TAG, " ОШИБКА, количество товаров в списке: " + arrayProductList.size() + ", добавляется товар на позицию:" + item.getIndexPosition());
        }
    }

    /**
     * Вставляет товар в указанную позицию списка
     *
     * @param param строка "сырых" данных
     */
    public void setPositionTovarList(String param) {

        Log.d(TAG, "setPositionTovarList :" + param);
        addProductDebug(param);
        if (arrayProductList.size() > 0) {
            final ItemProductList item = parseData(param);
            if (item.getIndexPosition() < 0)
                return;
            if (item.getIndexPosition() < arrayProductList.size()) {
                ItemProductList dummyItem = arrayProductList.get(item.getIndexPosition());
                if ((dummyItem.getCount() != item.getCount()) || (dummyItem.getSumm() != item.getSumm()) || (!dummyItem.getCodTovara().equals(item.getCodTovara()))) {
                    arrayProductList.set(item.getIndexPosition(), item);
                    updateScreen(item.getIndexPosition());
                }
            } else {
                addTovarList(param);
            }
        } else {
            addTovarList(param);
        }
    }

    /**
     * Удаление товара в указанной позиции
     *
     * @param param строка "сырых" данных
     */
    public void deletePositionTovarList(String param) {
        addProductDebug(param);
        int indexPosition = Integer.valueOf(param.substring(0, 2));  // 2
        if (arrayProductList.size() > 0) {
            if ((arrayProductList.size() - 1) >= indexPosition) {
                arrayProductList.remove(indexPosition);
            }
            updateScreen((arrayProductList.size() > 0) ? (arrayProductList.size() - 1) : 0);
        }
        Log.d(TAG, "deletePositionTovarList :" + indexPosition);
    }

    /**
     * Очистка списка товаров
     *
     * @param param строка "сырых" данных
     */
    public void clearTovarList(String param) {
        addProductDebug(param);
        arrayProductList.clear();
        updateScreen(0);
        Log.d(TAG, "clearTovarList: " + param);
    }

    private void updateScreen(int position) {
        adapterProductList.notifyDataSetChanged();
        scrollToPosition(position);
        setProductImage(position);
        updateTotalSumm();
        updateTotalCount();
    }

    private void scrollToPosition(int index) {
        MainActivity.listView.setSelection(index);
        MainActivity.listView.smoothScrollToPositionFromTop(index, 0);
        Log.d(TAG, "ScrollToPosition: " + index);
    }

    private void setProductImage(int index){
        if (adapterProductList.getCount() > 0) {
            ItemProductList selectedItem = adapterProductList.getItem(index);
            MainActivity.imageViewTovar.setImageBitmap(AdapterProductList.getImage(selectedItem.getCodTovara()));
            Log.d(TAG, "selectedItem.getCodTovara() " + selectedItem.getCodTovara());
        } else
            MainActivity.imageViewTovar.setImageBitmap(null);
    }

    /**
     * Пересчет суммы по списку товаров
     */
    private void updateTotalSumm() {
        int totalSumWithoutDiscount = 0;
        int totalDiscount = 0;
        int totalSumm = 0;

        for (int i = 0; i < arrayProductList.size(); i++) {
            ItemProductList selectedItem = arrayProductList.get(i);
            totalSumWithoutDiscount += selectedItem.getSumWithoutDiscount();
            totalDiscount += selectedItem.getDiscount();
            totalSumm += selectedItem.getSumm();
        }

        MainActivity.textViewTotalSummWithDiscount.setText(String.format(Locale.ROOT, "%.2f", (float) (((float) totalSumWithoutDiscount) / 100)));
        MainActivity.textViewTotalDiscount.setText(String.format(Locale.ROOT, "%.2f", (float) (((float) totalDiscount) / 100)));
        MainActivity.tv_TotalSumm.setText(String.format("%.2f", (float) (((float) totalSumm) / 100)).replace(",", "."));
        MainActivity.textViewDEBUG.append("MSG_totalSumWithoutDiscount: " + totalSumWithoutDiscount + "\n");
        MainActivity.textViewDEBUG.append("MSG_totalDiscount: " + totalDiscount + "\n");
        MainActivity.textViewDEBUG.append("MSG_totalSum: " + totalSumm + "\n");
    }

    /**
     * Пересчет количества товаров в списке
     */
    private void updateTotalCount() {
        MainActivity.tv_TotalCount.setText(("" + arrayProductList.size()).replace(",", "."));
        MainActivity.textViewDEBUG.append("MSG_TotalCount: " + arrayProductList.size() + "\n");
    }

    /**
     * Парсер данных с ResPos
     *
     * @param param строка "сырых" данных
     * @return ItemShoppingList
     */
    private ItemProductList parseData(String param) {
        ItemProductList item = new ItemProductList();
        try {
            int index = 0;
            int nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }

            item.setIndexPosition(Integer.valueOf(param.substring(index, nextSeparator)));
            index = nextSeparator + 1;

            //код товара
            nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }
            item.setCodTovara(param.substring(index, nextSeparator));
            index = nextSeparator + 1;

            //делимость
            nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }
            item.setDivisible(Integer.valueOf(param.substring(index, nextSeparator)));
            index = nextSeparator + 1;

            //количество
            nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }
            item.setCount(Long.valueOf(param.substring(index, nextSeparator)));
            index = nextSeparator + 1;

            //цена
            nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }
            item.setPrice(Long.valueOf(param.substring(index, nextSeparator)));
            index = nextSeparator + 1;

            //Сумма
            nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }
            item.setSumm(Long.valueOf(param.substring(index, nextSeparator)));
            index = nextSeparator + 1;

            //Наименование
            nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
            if (nextSeparator < 0) {
                item.setIndexPosition(-1);
                return item;
            }
            item.setNameTovara(param.substring(index, nextSeparator));

        } catch (Exception e) {
            Log.e(TAG, "ERROR ParseData :" + e);
        }
        return item;
    }

    public void showToast(String message) {
        Toast myToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        myToast.show();
    }
}
