package com.resonance.cashdisplay.product_list;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;
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

    private AdapterProductList adapterProductList;
    private ArrayList<ItemProductList> arrayProductList = new ArrayList<ItemProductList>();

    public ProductListWorker(Context context) {
        this.context = context;
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
    public void addProductDebug(final String msg) {

        final int start = MainActivity.textViewDebug.getText().length();
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
    public void setProductToList(String param) {

        Log.d(TAG, "setProductToList :" + param);
        addProductDebug(param);
        if (arrayProductList.size() > 0) {
            ItemProductList item = parseData(param);
            if (item.getIndexPosition() < 0)
                return;

            if (item.getIndexPosition() < arrayProductList.size()) {
                ItemProductList presentItem = arrayProductList.get(item.getIndexPosition());
                if ((presentItem.getCount() != item.getCount())
                        || (presentItem.getSum() != item.getSum())
                        || (!presentItem.getCode().equals(item.getCode())))
                    arrayProductList.set(item.getIndexPosition(), item);
                updateScreen(item.getIndexPosition());
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
        addProductDebug(param);
        int indexPosition = Integer.valueOf(param.substring(0, 2));  // 2
        if (arrayProductList.size() > 0) {
            if ((arrayProductList.size() - 1) >= indexPosition) {
                arrayProductList.remove(indexPosition);
            }
            updateScreen((arrayProductList.size() > 0) ? (arrayProductList.size() - 1) : 0);
        }
        Log.d(TAG, "deleteProductFromList :" + indexPosition);
    }

    /**
     * Очистка списка товаров
     *
     * @param param строка "сырых" данных
     */
    public void clearProductList(String param) {
        addProductDebug(param);
        arrayProductList.clear();
        updateScreen(0);
        Log.d(TAG, "clearProductList: " + param);
    }

    private void updateScreen(int position) {
        adapterProductList.notifyDataSetChanged();
        // need some time to update list view with new data
        new Handler(Looper.getMainLooper()).post(() -> {
            scrollToPosition(position);
        });
        setProductImage(position);
        updateTotalValues();
    }

    private void scrollToPosition(final int position) {
        listViewProducts.smoothScrollToPositionFromTop(position, 0, 100);
        Log.d(TAG, "scrollToPosition: " + position);

        listViewProducts.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    int totalItemsCount = listViewProducts.getCount();
                    int visibleItemsCount = (listViewProducts.getLastVisiblePosition() - listViewProducts.getFirstVisiblePosition()) + 1;
                    int positionViewPort = position;
                    if (adapterProductList.getCount() > visibleItemsCount) {
                        positionViewPort = visibleItemsCount - (totalItemsCount - position);
                        if (positionViewPort < 0)
                            positionViewPort = 0;
                    }

                    View listItem = listViewProducts.getChildAt(positionViewPort);

                    Log.d(TAG, "listItem = " + ((TextView) listItem.findViewById(R.id.textview_product)).getText());


                    AnimationDrawable animDrawable = (AnimationDrawable) listItem.getBackground();
                    Log.d(TAG, "animDrawable = " + animDrawable);
                    // Enter Fade duration is part of duration in xml (starts when xml frame starts, but no longer then xml duration)
                    // Exit Fade is added to duration in xml (xml plays, then this fade starts with next xml item - intersection).
                    animDrawable.setEnterFadeDuration(0);    // duration of item in xml has priority (if in xml 0, fade in = 0; if in xml 100, fade in = 100)
                    animDrawable.setExitFadeDuration(1200);  // this duration plays always (if in xml 400, then 400 + fade out)
                    animDrawable.start();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                Log.d(TAG, "firstVisibleItem " + firstVisibleItem);
                Log.d(TAG, "visibleItemCount " + visibleItemCount);
                Log.d(TAG, "totalItemCount " + totalItemCount);
            }
        });

        Log.d(TAG, "listViewProducts.getCount() = " + listViewProducts.getCount());
    }

    private void setProductImage(int index) {
        if (adapterProductList.getCount() > 0) {
            ItemProductList selectedItem = adapterProductList.getItem(index);
            MainActivity.imageViewProduct.setImageBitmap(AdapterProductList.getImage(selectedItem.getCode()));
            Log.d(TAG, "selectedItem.getCodTovara() " + selectedItem.getCode());
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
        MainActivity.textViewDebug.append("MSG_TotalCount: " + arrayProductList.size() + "\n");
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
