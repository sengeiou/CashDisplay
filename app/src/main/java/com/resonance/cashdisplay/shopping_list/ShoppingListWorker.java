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

    private final byte SYMBOL_SEPARATOR = (byte)0x03;

    public static final String FINISH_SHOPPING_LIST_ALERT = "finish_shopping_list_alert";
    public static final String DEBUG_ALERT = "Debug_Alert";
    public static final String TOTAL_SUMM_SHOPPING_LIST_ALERT = "TotalSumm_TovarList";
    public static final String TOTAL_COUNT_SHOPPING_LIST_ALERT = "TotalCount_TovarList";
    public static final String SCROLL_SHOPPING_LIST_ALERT = "Scroll_TovarList";

    private static long TotalSumm = 0;
    private static Context mContext;

    private static BlockingQueue<Integer> needScroll_Queue ;//обеспечивает передачу событий для скроллинга списка товаров

    public static AdapterShoppingList adapterShoppingList;
    public static ArrayList<ItemShoppingList> ArrayShoppingList;


    /**
     *
     * @param context
     */
    public ShoppingListWorker(Context context){
        mContext = context;
        needScroll_Queue = new ArrayBlockingQueue<Integer>(50);
        needScroll_Queue.clear();

        ArrayShoppingList = new ArrayList<ItemShoppingList>();
        adapterShoppingList = new AdapterShoppingList(ArrayShoppingList,mContext);

        RunUpdateThread();

        Log.d(TAG, "ShoppingListWorker");
    }

    /**
     * Добавлен вывод на экран потоковой информации
     * @param msg
     */
    public void ADD_DEBUG(final String msg){

        final int start = MainActivity.textViewDEBUG.getText().length();

        MainActivity.textViewDEBUG.post(new Runnable()
        {
            public void run()
            {
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

    public void CloseDisplayShoppingList()
    {
        ArrayShoppingList.clear();
        adapterShoppingList.notifyDataSetChanged();
    }

    public void UpdateScreen(final int position)
    {
        adapterShoppingList.notifyDataSetChanged();
        ScrollToPosition(position);//
        Update_TotalSumm();
        Update_TotalCount();
    }


    /*обработчик команд для экран "Список покупок"*/

    /**
     * Добавляет товар в список
     * @param param строка "сырых" данных
     */
    public void Add_TovarList(String param)
    {

        //Log.d(TAG, "Add_TovarList :"+param);
        final ItemShoppingList item = ParseData(param);
        if (item.getIndexPosition()<0) return;

        if (item.getIndexPosition() <= ArrayShoppingList.size())
        {
            ADD_DEBUG(param);

            ArrayShoppingList.add(item.getIndexPosition(), item);

            UpdateScreen(item.getIndexPosition());
        }else {
            showToast("Невiрнi параметри при внесеннi товару, необхідно очистити чек!");
            Log.d(TAG, " ОШИБКА, количество товаров в списке: " + ArrayShoppingList.size()+", добавляется товар на позицию:"+item.getIndexPosition());
        }

    }

    /**
     * Очистка списка товаров
     * @param param  строка "сырых" данных
     */
    public void Clear_TovarList(String param)
    {
        ArrayShoppingList.clear();
        MainActivity.imageViewTovar.setImageBitmap(null);
        UpdateScreen((ArrayShoppingList.size()>0)?ArrayShoppingList.size()-1:-1);
        Log.d(TAG, "Clear_TovarList :"+param);
        ADD_DEBUG(param);

    }

    /**
     * Вставляет товар в указанную позицию списка
     * @param param строка "сырых" данных
     */
    public void SetPosition_TovarList(String param)
    {

        Log.d(TAG, "SetPosition_TovarList :"+param);
        ADD_DEBUG(param);
        if (ArrayShoppingList.size() >0)
        {
            final  ItemShoppingList item = ParseData(param);

            if (item.getIndexPosition()<0) return;

            if (item.getIndexPosition()<ArrayShoppingList.size()) {

                ItemShoppingList dummy_item = ArrayShoppingList.get(item.getIndexPosition());
                if ((dummy_item.getCount()!=item.getCount())||(dummy_item.getSumm()!=item.getSumm())||(!dummy_item.getCodTovara().equals(item.getCodTovara())))
                {

                    ArrayShoppingList.set(item.getIndexPosition(), item);
                    UpdateScreen(item.getIndexPosition());
                }
            }else{
                Add_TovarList(param);
            }
        }else {
            Add_TovarList(param);
        }
    }

    /**
     * Удаление товара в указанной позиции
     * @param param строка "сырых" данных
     */
    public void DeletePosition_TovarList(String param){
        ADD_DEBUG(param);

        int indexPosition = Integer.valueOf(param.substring(0, 2));//2

        if (ArrayShoppingList.size()>0)
        {
            if((ArrayShoppingList.size()-1) >=indexPosition)
            {
                ArrayShoppingList.remove(indexPosition);
            }
            UpdateScreen((ArrayShoppingList.size()>0)?ArrayShoppingList.size()-1:0);
        }
        Log.d(TAG, "DeletePosition_TovarList :"+indexPosition);
    }

    /**
     * Пересчет суммы по списку товаров
     */
    private void Update_TotalSumm()
    {
        TotalSumm = 0;

        for (int i = 0; i < ArrayShoppingList.size(); i++) {
            ItemShoppingList selectedItem = ArrayShoppingList.get(i);
            TotalSumm += selectedItem.getSumm();
        }

        MainActivity.tv_TotalSumm.post(new Runnable() {
            public void run() {
                MainActivity.tv_TotalSumm.setText(String.format("%.2f",(float)(((float)TotalSumm)/100)).replace(",","."));
            }
        });
        MainActivity.textViewDEBUG.post(new Runnable() {
            public void run() {
                MainActivity.textViewDEBUG.append("MSG_TS: "+TotalSumm + "\n");
            }
        });
    }

    /**
     * Пересчет количества товаров в списке
     */
    private void Update_TotalCount()
    {
       MainActivity.tv_TotalCount.post(new Runnable() {
            public void run() {
                MainActivity.tv_TotalCount.setText((""+ArrayShoppingList.size()).replace(",","."));
            }
        });
        MainActivity.textViewDEBUG.post(new Runnable() {
            public void run() {
                MainActivity.textViewDEBUG.append("MSG_TC: "+ArrayShoppingList.size() + "\n");
            }
        });


        if (MainActivity.listView.getCount()==0){
            MainActivity.imageViewTovar.setVisibility(View.INVISIBLE);
        }else
        {
            MainActivity.imageViewTovar.setVisibility(View.VISIBLE);
        }
    }

    private void ScrollToPosition(final int index){
        if (needScroll_Queue.remainingCapacity()>0){
            needScroll_Queue.add(index);
        }
    }


    /**
     * Парсер данных с ResPos
     * @param param  строка "сырых" данных
     * @return  ItemShoppingList
     */
    private ItemShoppingList ParseData(String param)
    {
        ItemShoppingList item = new ItemShoppingList();
        try
        {
            int index = 0;
            int next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setIndexPosition(Integer.valueOf(param.substring(index, next_separator)));
            index = next_separator+1;

            //код товара
            next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setCodTovara(param.substring(index, next_separator));
            index = next_separator+1;

            //делимость
            next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setDivisible(Integer.valueOf(param.substring(index, next_separator)));
            index = next_separator+1;

            //количество
            next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setCount(Long.valueOf(param.substring(index, next_separator)));
            index = next_separator+1;

            //цена
            next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setPrice(Long.valueOf(param.substring(index, next_separator)));
            index = next_separator+1;

            //Сумма
            next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setSumm(Long.valueOf(param.substring(index, next_separator)));
            index = next_separator+1;

            //Наименование
            next_separator = param.indexOf(SYMBOL_SEPARATOR,index);
            if(next_separator<0) {item.setIndexPosition(-1); return item;}

            item.setNameTovara(param.substring(index, next_separator));

        }catch (Exception e)
        {
            Log.e(TAG, "ERROR ParseData :"+e);
        }
        return item;
    }

    /**
     * Поток принимает события для скроллирования списка товара и установки изображения
     */
    public void RunUpdateThread() {

        Thread update_thread = new Thread(new Runnable() {
            int indexScroll = 0;
            @Override
            public void run()
            {

                while(true)
                {
                    try
                    {
                        if (!needScroll_Queue.isEmpty())
                        {
                            indexScroll = needScroll_Queue.take();

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

                            }else
                        {
                            if (MainActivity.listView.getSelectedItemPosition()!=indexScroll){
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

                        Log.e(TAG, "Exception:"+e);

                    }
                }



            }
        });
        update_thread.start();
    }




    public void showToast(String message) {
        Toast myToast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        myToast.show();
    }

}
