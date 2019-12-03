package com.resonance.cashdisplay.product_list;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.R;

import java.util.ArrayList;
import java.util.Locale;

import static com.resonance.cashdisplay.MainActivity.listViewProducts;

/**
 * Класс обрабатывает экран "Список товаров"
 */
public class ProductListWorker {

    private final static String TAG = "ProductListWorker";

    private final byte SYMBOL_SEPARATOR = (byte) 0x03;

    private Context context;
    // animations for products images
    private Animation fadeOut;
    private Animation fadeIn;

    private AdapterProductList adapterProductList;
    private ArrayList<ItemProductList> arrayProductList = new ArrayList<ItemProductList>();

    public ProductListWorker(Context context) {
        this.context = context;

        fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(100);
        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(300);

        Log.d(TAG, "New ProductListWorker created.");
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
    public void addProductDebug(String msg) {
        int start = MainActivity.textViewDebug.getText().length();
        MainActivity.textViewDebug.append(msg + "\n");
        int end = MainActivity.textViewDebug.getText().length();
        Spannable spannableText = (Spannable) MainActivity.textViewDebug.getText();
        spannableText.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, 0);
        MainActivity.scrollView.fullScroll(View.FOCUS_DOWN);
    }

    /**
     * Добавляет товар в список
     *
     * @param param строка "сырых" данных
     */
    public void addProductToList(String param) {
        ItemProductList item = parseData(param);
        if (item.getIndexPosition() < 0)
            return;
        if (item.getIndexPosition() <= arrayProductList.size()) {
            arrayProductList.add(item.getIndexPosition(), item);
            Log.d(TAG, "addProductToList: " + param);
            updateScreen(item.getIndexPosition(), true, true);
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
    public void setProductToList(String param) {
        if (arrayProductList.size() > 0) {
            ItemProductList item = parseData(param);
            if (item.getIndexPosition() < 0)
                return;

            if (item.getIndexPosition() < arrayProductList.size()) {
                ItemProductList presentItem = arrayProductList.get(item.getIndexPosition());
                if ((presentItem.getCount() != item.getCount())
                        || (presentItem.getSum() != item.getSum())
                        || (!presentItem.getCode().equals(item.getCode()))) {
                    arrayProductList.set(item.getIndexPosition(), item);
                    Log.d(TAG, "setProductToList :" + param);
                    updateScreen(item.getIndexPosition(), true, true);   // updates only changed position
                }                                                               // !!! important for ResPOS
            } else {
                addProductToList(param);
            }

        } else {
            addProductToList(param);
        }
    }

    /**
     * Удаление товара в указанной позиции
     *
     * @param param строка "сырых" данных
     */
    public void deleteProductFromList(String param) {
        int indexPosition = Integer.valueOf(param.substring(0, 2));  // 2
        if (arrayProductList.size() > 0) {
            if ((arrayProductList.size() - 1) >= indexPosition) {
                arrayProductList.remove(indexPosition);
                Log.d(TAG, "deleteProductFromList :" + indexPosition);
            }
            updateScreen((arrayProductList.size() > 0) ? (arrayProductList.size() - 1) : 0, false, false);
        }
    }

    /**
     * Очистка списка товаров
     *
     * @param param строка "сырых" данных
     */
    public void clearProductList(String param) {
        arrayProductList.clear();
        Log.d(TAG, "clearProductList: " + param);
        updateScreen(0, false, false);
    }

    /**
     * Update list view with new data.
     *
     * @param position         absolute serial number of item in list view
     * @param highlightItem    indicates if selected item must be highlighted or not
     * @param animProductImage changes product images with animation fade out and fade in
     */
    private void updateScreen(int position, boolean highlightItem, boolean animProductImage) {
        adapterProductList.notifyDataSetChanged();
        scrollToPosition(position, highlightItem);
        setProductImage(position, animProductImage);
        updateTotalValues();
    }

    /**
     * Scrolls list view to specified absolute position. Optionally highlights selected item.
     *
     * @param position      absolute serial number of item in list view
     * @param highlightItem indicates if selected item must be highlighted or not
     */
    private void scrollToPosition(int position, boolean highlightItem) {
        new Handler().post(() -> {
            listViewProducts.setSelection(position);
            if (highlightItem)
                listViewProducts.performItemClick(listViewProducts, position, listViewProducts.getItemIdAtPosition(position));
        });
        Log.d(TAG, "scrollToPosition: " + position);
    }

    private void setProductImage(int position, boolean animProductImage) {
        if (adapterProductList.getCount() > 0) {
            ItemProductList selectedItem = adapterProductList.getItem(position);
            Bitmap productImage = AdapterProductList.getImage(selectedItem.getCode());

            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    MainActivity.imageViewProduct.setImageBitmap(productImage);
                    MainActivity.imageViewProduct.startAnimation(fadeIn);
                }
            });

            if (animProductImage)
                MainActivity.imageViewProduct.startAnimation(fadeOut);
            else
                MainActivity.imageViewProduct.setImageBitmap(productImage);

            Log.d(TAG, "selectedItem.getCode() " + selectedItem.getCode());
        } else
            MainActivity.imageViewProduct.setImageBitmap(null);
    }

    /**
     * Calculation of total values for all product list and exposing results
     */
    private void updateTotalValues() {
        long totalSumWithoutDiscount = 0;
        long totalDiscount = 0;
        long totalSum = 0;

        for (int i = 0; i < arrayProductList.size(); i++) {
            ItemProductList selectedItem = arrayProductList.get(i);
            totalSumWithoutDiscount += selectedItem.getSumWithoutDiscount();
            totalDiscount += selectedItem.getDiscount();
            totalSum += selectedItem.getSum();
        }

        MainActivity.textViewTotalSumWithoutDiscount.setText(String.format(Locale.ROOT, "%.2f", (double) totalSumWithoutDiscount / 100));
        MainActivity.textViewTotalDiscount.setText(String.format(Locale.ROOT, "%.2f", (double) totalDiscount / 100));
        MainActivity.textViewTotalSum.setText(String.format(Locale.ROOT, "%.2f", (double) totalSum / 100));
        MainActivity.textViewTotalCount.setText(String.valueOf(arrayProductList.size()));
        MainActivity.textViewDebug.append("MSG_totalSumWithoutDiscount: " + totalSumWithoutDiscount + "\n");
        MainActivity.textViewDebug.append("MSG_totalDiscount: " + totalDiscount + "\n");
        MainActivity.textViewDebug.append("MSG_totalSum: " + totalSum + "\n");
        MainActivity.textViewDebug.append("MSG_totalCount: " + arrayProductList.size() + "\n");
        new Handler().post(() -> MainActivity.scrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Парсер данных с ResPos
     *
     * @param param raw data from ADDL and SETi commands
     * @return ItemProductList
     */
    private ItemProductList parseData(String param) {
        ItemProductList item = new ItemProductList();
        try {
            int index = 0;
            int nextSeparator;
            int parametersAmount = 7;

            for (int parNum = 0; parNum < parametersAmount; parNum++) {
                nextSeparator = param.indexOf(SYMBOL_SEPARATOR, index);
                if (nextSeparator < 0) {
                    item.setIndexPosition(-1);
                    return item;
                }
                switch (parNum) {
                    case 0:
                        item.setIndexPosition(Integer.valueOf(param.substring(index, nextSeparator)));
                        break;
                    case 1:
                        item.setCode(param.substring(index, nextSeparator));
                        break;
                    case 2:
                        item.setDivisible(Integer.valueOf(param.substring(index, nextSeparator)));
                        break;
                    case 3:
                        item.setCount(Integer.valueOf(param.substring(index, nextSeparator)));
                        break;
                    case 4:
                        item.setPrice(Long.valueOf(param.substring(index, nextSeparator)));
                        break;
                    case 5:
                        item.setSum(Long.valueOf(param.substring(index, nextSeparator)));
                        break;
                    case 6:
                        item.setName(param.substring(index, nextSeparator));
                        break;
                    default:
                        break;
                }
                index = nextSeparator + 1;
            }
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
