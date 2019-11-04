package com.resonance.cashdisplay.shopping_list;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.R;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Класс обрабатывает экран "Список товаров"
 */
public class ShoppingListWorker {


    private final static String TAG = "ShoppingListWorker";

    private final byte SYMBOL_SEPARATOR = (byte) 0x03;

    public static final String FINISH_SHOPPING_LIST_ALERT = "finish_shopping_list_alert";
    public static final String DEBUG_ALERT = "Debug_Alert";
    public static final String TOTAL_SUMM_SHOPPING_LIST_ALERT = "TotalSumm_TovarList";
    public static final String TOTAL_COUNT_SHOPPING_LIST_ALERT = "TotalCount_TovarList";
    public static final String SCROLL_SHOPPING_LIST_ALERT = "Scroll_TovarList";

    private int totalSumWithoutDiscount = 0;
    private int totalDiscount = 0;
    private int totalSumm = 0;
    private static Context mContext;

    private BlockingQueue<Integer> needScrollQueue;//обеспечивает передачу событий для скроллинга списка товаров

    private AdapterProductList adapterProductList;
    private ArrayList<ItemShoppingList> arrayProductList;

    /**
     * @param context
     */
    public ShoppingListWorker(Context context) {
        mContext = context;
        needScrollQueue = new ArrayBlockingQueue<Integer>(50);
        needScrollQueue.clear();

        arrayProductList = new ArrayList<ItemShoppingList>();
        runUpdateThread();
        Log.d(TAG, "ShoppingListWorker");
    }

    /**
     * @param lookCode value of {@link PreferencesValues#productListLookCode}
     */
    public void createAdapterProductList(int lookCode) {
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
        adapterProductList = new AdapterProductList(mContext, resource, arrayProductList);
    }

    public AdapterProductList getAdapterProductList() {
        return adapterProductList;
    }

    /**
     * Добавлен вывод на экран потоковой информации
     *
     * @param msg
     */
    public void addProductDebug(final String msg) {

        final int start = MainActivity.textViewDEBUG.getText().length();

        MainActivity.textViewDEBUG.post(new Runnable() {
            public void run() {
                MainActivity.textViewDEBUG.append(msg + "\n");
                int end = MainActivity.textViewDEBUG.getText().length();

                Spannable spannableText = (Spannable) MainActivity.textViewDEBUG.getText();
                spannableText.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, 0);
            }
        });

        MainActivity.mScrollView.post(new Runnable() {
            public void run() {
                MainActivity.mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void closeDisplayShoppingList() {
        clearTovarList("on product list display closed");
        arrayProductList.clear();
        adapterProductList.notifyDataSetChanged();
    }

    public void updateScreen(final int position) {
        scrollToPosition(position);
        updateTotalSumm();
        updateTotalCount();
        adapterProductList.notifyDataSetChanged();
    }

    /*обработчик команд для экран "Список покупок"*/

    /**
     * Добавляет товар в список
     *
     * @param param строка "сырых" данных
     */
    public void addTovarList(String param) {

        //Log.d(TAG, "addTovarList :"+param);
        final ItemShoppingList item = parseData(param);
        if (item.getIndexPosition() < 0) return;

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
     * Очистка списка товаров
     *
     * @param param строка "сырых" данных
     */
    public void clearTovarList(String param) {
        arrayProductList.clear();
        MainActivity.imageViewTovar.setImageBitmap(null);
        updateScreen((arrayProductList.size() > 0) ? arrayProductList.size() - 1 : -1);
        Log.d(TAG, "clearTovarList :" + param);
        addProductDebug(param);
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
            final ItemShoppingList item = parseData(param);

            if (item.getIndexPosition() < 0) return;

            if (item.getIndexPosition() < arrayProductList.size()) {
                ItemShoppingList dummyItem = arrayProductList.get(item.getIndexPosition());
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

        int indexPosition = Integer.valueOf(param.substring(0, 2));//2

        if (arrayProductList.size() > 0) {
            if ((arrayProductList.size() - 1) >= indexPosition) {
                arrayProductList.remove(indexPosition);
            }
            updateScreen((arrayProductList.size() > 0) ? arrayProductList.size() - 1 : 0);
        }
        Log.d(TAG, "deletePositionTovarList :" + indexPosition);
    }

    /**
     * Пересчет суммы по списку товаров
     */
    private void updateTotalSumm() {
        totalSumWithoutDiscount = 0;
        totalDiscount = 0;
        totalSumm = 0;

        for (int i = 0; i < arrayProductList.size(); i++) {
            ItemShoppingList selectedItem = arrayProductList.get(i);
            totalSumWithoutDiscount += selectedItem.getSumWithoutDiscount();
            totalDiscount += selectedItem.getDiscount();
            totalSumm += selectedItem.getSumm();
        }

        MainActivity.tv_TotalSumm.post(new Runnable() {
            public void run() {
                MainActivity.textViewTotalSummWithDiscount.setText(String.format(Locale.ROOT, "%.2f", (float) (((float) totalSumWithoutDiscount) / 100)));
                MainActivity.textViewTotalDiscount.setText(String.format(Locale.ROOT, "%.2f", (float) (((float) totalDiscount) / 100)));
                MainActivity.tv_TotalSumm.setText(String.format("%.2f", (float) (((float) totalSumm) / 100)).replace(",", "."));
                MainActivity.textViewDEBUG.append("MSG_totalSumWithoutDiscount: " + totalSumWithoutDiscount + "\n");
                MainActivity.textViewDEBUG.append("MSG_totalDiscount: " + totalDiscount + "\n");
                MainActivity.textViewDEBUG.append("MSG_totalSum: " + totalSumm + "\n");
            }
        });
    }

    /**
     * Пересчет количества товаров в списке
     */
    private void updateTotalCount() {
        MainActivity.tv_TotalCount.post(new Runnable() {
            public void run() {
                MainActivity.tv_TotalCount.setText(("" + arrayProductList.size()).replace(",", "."));
            }
        });
        MainActivity.textViewDEBUG.post(new Runnable() {
            public void run() {
                MainActivity.textViewDEBUG.append("MSG_TC: " + arrayProductList.size() + "\n");
            }
        });

        if (MainActivity.preferenceParams.productListLookCode == PreferenceParams.LOOK_AMERICAN) {
            MainActivity.imageViewTovar.setVisibility(View.GONE);
            return;
        }

        if (MainActivity.listView.getCount() == 0) {
            MainActivity.imageViewTovar.setVisibility(View.INVISIBLE);
        } else {
            MainActivity.imageViewTovar.setVisibility(View.VISIBLE);
        }
    }

    private void scrollToPosition(final int index) {
        if (needScrollQueue.remainingCapacity() > 0) {
            needScrollQueue.add(index);
        }
    }

    /**
     * Парсер данных с ResPos
     *
     * @param param строка "сырых" данных
     * @return ItemShoppingList
     */
    private ItemShoppingList parseData(String param) {
        ItemShoppingList item = new ItemShoppingList();
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

    /**
     * Поток принимает события для скроллирования списка товара и установки изображения
     */
    public void runUpdateThread() {

        Thread updateThread = new Thread(new Runnable() {
            int indexScroll = 0;

            @Override
            public void run() {

                while (true) {
                    try {
                        if (!needScrollQueue.isEmpty()) {
                            indexScroll = needScrollQueue.take();

                            MainActivity.listView.post(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.listView.setSelection(indexScroll);
                                    MainActivity.listView.smoothScrollToPositionFromTop(indexScroll, 0);
                                    Log.d(TAG, "5 ScrollToPosition :" + indexScroll);
                                }
                            });

                            if (adapterProductList.getCount() > indexScroll) {

                                final ItemShoppingList selectedItem = adapterProductList.getItem(indexScroll);
                                try {
                                    MainActivity.imageViewTovar.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.imageViewTovar.setImageBitmap(AdapterProductList.getImage(selectedItem.getCodTovara()));
                                        }
                                    });
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception QueWorker: " + e);
                                }
                            }
                        } else {
                            Log.d(TAG, "MainActivity.listView.getSelectedItemPosition() = " + Integer.toString(MainActivity.listView.getSelectedItemPosition()));
                            Log.d(TAG, "indexScroll = " + indexScroll);
                            if (MainActivity.listView.getSelectedItemPosition() != indexScroll) {
                                MainActivity.listView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.listView.setSelection(indexScroll);
                                        MainActivity.listView.smoothScrollToPositionFromTop(indexScroll, 0);
                                    }
                                });
                            }
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Exception:" + e);
                    }
                }
            }
        });
        updateThread.start();
    }

    public void showToast(String message) {
        Toast myToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        myToast.show();
    }
}
