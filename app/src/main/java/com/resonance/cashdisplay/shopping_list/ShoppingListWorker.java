package com.resonance.cashdisplay.shopping_list;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;

import java.util.ArrayList;
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

    private static long totalSumm = 0;
    private static Context mContext;

    private static BlockingQueue<Integer> needScrollQueue;//обеспечивает передачу событий для скроллинга списка товаров

    public static AdapterShoppingList adapterShoppingList;
    public static ArrayList<ItemShoppingList> arrayShoppingList;


    /**
     * @param context
     */
    public ShoppingListWorker(Context context) {
        mContext = context;
        needScrollQueue = new ArrayBlockingQueue<Integer>(50);
        needScrollQueue.clear();

        arrayShoppingList = new ArrayList<ItemShoppingList>();
        adapterShoppingList = new AdapterShoppingList(arrayShoppingList, mContext);

        runUpdateThread();

        Log.d(TAG, "ShoppingListWorker");
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
        arrayShoppingList.clear();
        adapterShoppingList.notifyDataSetChanged();
    }

    public void updateScreen(final int position) {
        adapterShoppingList.notifyDataSetChanged();
        scrollToPosition(position);
        updateTotalSumm();
        updateTotalCount();
    }


    /*обработчик команд для экран "Список покупок"*/

    /**
     * Добавляет товар в список
     *
     * @param param строка "сырых" данных
     */
    public void addTovarList(String param) {

        //Log.d(TAG, "addTovarList :"+param);
        final ItemShoppingList item = ParseData(param);
        if (item.getIndexPosition() < 0) return;

        if (item.getIndexPosition() <= arrayShoppingList.size()) {
            addProductDebug(param);

            arrayShoppingList.add(item.getIndexPosition(), item);

            updateScreen(item.getIndexPosition());
        } else {
            showToast("Невiрнi параметри при внесеннi товару, необхідно очистити чек!");
            Log.d(TAG, " ОШИБКА, количество товаров в списке: " + arrayShoppingList.size() + ", добавляется товар на позицию:" + item.getIndexPosition());
        }

    }

    /**
     * Очистка списка товаров
     *
     * @param param строка "сырых" данных
     */
    public void clearTovarList(String param) {
        arrayShoppingList.clear();
        MainActivity.imageViewTovar.setImageBitmap(null);
        updateScreen((arrayShoppingList.size() > 0) ? arrayShoppingList.size() - 1 : -1);
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
        if (arrayShoppingList.size() > 0) {
            final ItemShoppingList item = ParseData(param);

            if (item.getIndexPosition() < 0) return;

            if (item.getIndexPosition() < arrayShoppingList.size()) {

                ItemShoppingList dummy_item = arrayShoppingList.get(item.getIndexPosition());
                if ((dummy_item.getCount() != item.getCount()) || (dummy_item.getSumm() != item.getSumm()) || (!dummy_item.getCodTovara().equals(item.getCodTovara()))) {

                    arrayShoppingList.set(item.getIndexPosition(), item);
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

        if (arrayShoppingList.size() > 0) {
            if ((arrayShoppingList.size() - 1) >= indexPosition) {
                arrayShoppingList.remove(indexPosition);
            }
            updateScreen((arrayShoppingList.size() > 0) ? arrayShoppingList.size() - 1 : 0);
        }
        Log.d(TAG, "deletePositionTovarList :" + indexPosition);
    }

    /**
     * Пересчет суммы по списку товаров
     */
    private void updateTotalSumm() {
        totalSumm = 0;

        for (int i = 0; i < arrayShoppingList.size(); i++) {
            ItemShoppingList selectedItem = arrayShoppingList.get(i);
            totalSumm += selectedItem.getSumm();
        }

        MainActivity.tv_TotalSumm.post(new Runnable() {
            public void run() {
                MainActivity.tv_TotalSumm.setText(String.format("%.2f", (float) (((float) totalSumm) / 100)).replace(",", "."));
            }
        });
        MainActivity.textViewDEBUG.post(new Runnable() {
            public void run() {
                MainActivity.textViewDEBUG.append("MSG_TS: " + totalSumm + "\n");
            }
        });
    }

    /**
     * Пересчет количества товаров в списке
     */
    private void updateTotalCount() {
        MainActivity.tv_TotalCount.post(new Runnable() {
            public void run() {
                MainActivity.tv_TotalCount.setText(("" + arrayShoppingList.size()).replace(",", "."));
            }
        });
        MainActivity.textViewDEBUG.post(new Runnable() {
            public void run() {
                MainActivity.textViewDEBUG.append("MSG_TC: " + arrayShoppingList.size() + "\n");
            }
        });


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
    private ItemShoppingList ParseData(String param) {
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

                            if (ShoppingListWorker.adapterShoppingList.getCount() > indexScroll) {

                                final ItemShoppingList selectedItem = ShoppingListWorker.adapterShoppingList.getItem(indexScroll);
                                try {
                                    MainActivity.imageViewTovar.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.imageViewTovar.setImageBitmap(AdapterShoppingList.getImage(selectedItem.getCodTovara()));
                                        }
                                    });

                                } catch (Exception e) {
                                    Log.e(TAG, "Exception QueWorker: " + e);
                                }

                            }

                        } else {
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
