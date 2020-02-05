package com.resonance.cashdisplay.product_list;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.settings.PrefValues;
import com.resonance.cashdisplay.settings.PrefWorker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.resonance.cashdisplay.MainActivity.listViewProducts;
import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_BASKET;
import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_DMART;
import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_SUBWAY;

/**
 * Handles screen "Список покупок".
 * Controls operations under list of goods (using appropriate adapter for list).
 * Works with different looks and is recreated when new look is chosen.
 *
 * @see PrefValues#productListLookCode to understand looks
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

    // used in all views
    private LinearLayout layoutTotal;

    // used for LOOK_SUBWAY only
    private TextView textViewCardNumber;
    private LinearLayout layoutItemsBlock;
    private LinearLayout layoutItemExtra;
    private LinearLayout layoutToPay;
    private LinearLayout layoutList;

    public static Timer clockTimer;

    public ProductListWorker(Context context) {
        this.context = context;

        fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(100);
        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(300);

        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                Activity mainActivity = (Activity) context;
                textViewCardNumber = mainActivity.findViewById(R.id.textview_card_number);
                layoutItemsBlock = mainActivity.findViewById(R.id.layout_items_block);    // layout of 1-2 items mode
                layoutItemExtra = mainActivity.findViewById(R.id.layout_item_extra);      // second item for 1-2 items mode
                layoutToPay = mainActivity.findViewById(R.id.layout_to_pay);              // "До сплати" for 1-2 items mode
                layoutList = mainActivity.findViewById(R.id.layout_list);                 // layout of list mode (more than 2 items)
                layoutTotal = mainActivity.findViewById(R.id.layout_total);               // layout of total sum for list mode
                break;
            default:
                break;
        }

        if (clockTimer != null) {
            clockTimer.cancel();
            clockTimer = null;
        }

        Log.d(TAG, "New ProductListWorker created.");
    }

    /**
     * @param lookCode // value of {@link PrefValues#productListLookCode}
     */
    public AdapterProductList createAdapterProductList(int lookCode) {
        int resource;
        switch (lookCode) {
            case LOOK_BASKET:
                resource = R.layout.list_item_look_0;
                break;
            case LOOK_DMART:
                resource = R.layout.list_item_look_1;
                break;
            case LOOK_SUBWAY:
                resource = R.layout.list_item_look_2;
                break;
            default:
                resource = R.layout.list_item_look_0;
                break;
        }
        adapterProductList = new AdapterProductList(context, resource, arrayProductList);

        return adapterProductList;
    }

    /**
     * Called when product list screen becomes visible.
     *
     * @param args contains String list of additional arguments various for different looks.
     *             For different looks there may be 0 or more arguments, that are divided from each
     *             other with 0x03 symbols.
     */
    public void onProductListShow(@Nullable String args) {
        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                if (args != null) {
                    Log.d("PLW", "show=" + args);
                    int argAmount = 3;                   // according "inner" protocol for this look

                    String[] argList = args.split(Character.toString((char) SYMBOL_SEPARATOR), argAmount);
                    Log.d("PLW", "argList size = " + argList.length);
                    for (String str : argList) {
                        Log.d("PLW", "argList element = " + str);
                    }

                    if (argList.length == 3) {
                        switch (strToInt(argList[0])) {                       // amount of products argument
                            case 1:
                                layoutItemsBlock.setVisibility(View.VISIBLE);
                                break;
                            case 2:
                                layoutItemsBlock.setVisibility(View.VISIBLE);
                                layoutItemExtra.setVisibility(View.VISIBLE);
                                break;
                            default:
                                layoutList.setVisibility(View.VISIBLE);       // list mode
                                break;
                        }

                        if ("payment".equals(argList[1])) {                   // "payment" or "balance" argument
                            textViewCardNumber.setText(R.string.card_num);
                            layoutToPay.setVisibility(View.VISIBLE);          // for 1-2 items mode
                            layoutTotal.setVisibility(View.VISIBLE);          // for list mode
                        } else
                            textViewCardNumber.setText(R.string.card_balance_num);

                        textViewCardNumber.append(argList[2]);                // card number argument
                    }
                }

                if (clockTimer == null) {
                    clockTimer = new Timer();
                    TextView textViewDateTime = ((Activity) context).findViewById(R.id.textview_date_time);
                    DateFormat dateFormat0 = new SimpleDateFormat("dd.MM.yyyy HH mm", new Locale("uk"));
                    DateFormat dateFormat1 = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("uk"));
                    Date date = new Date(System.currentTimeMillis());
                    clockTimer.scheduleAtFixedRate(new TimerTask() {
                        boolean flip = false;

                        @Override
                        public void run() {
                            date.setTime(System.currentTimeMillis());
                            if (flip) {
                                textViewDateTime.post(() ->
                                        textViewDateTime.setText(dateFormat0.format(date)));
                            } else {
                                textViewDateTime.post(() ->
                                        textViewDateTime.setText(dateFormat1.format(date)));
                            }
                            flip = !flip;
                        }
                    }, 0, 50000);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Called when product list screen becomes invisible.
     */
    public void onProductListHide() {
        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                textViewCardNumber.setText(context.getString(R.string.card_default));
                layoutItemsBlock.setVisibility(View.INVISIBLE);
                layoutItemExtra.setVisibility(View.GONE);
                layoutToPay.setVisibility(View.GONE);
                layoutList.setVisibility(View.INVISIBLE);
                layoutTotal.setVisibility(View.GONE);

                if (clockTimer != null) {
                    clockTimer.cancel();
                    clockTimer = null;
                }
                break;
            default:
                break;
        }
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
     * @param args строка "сырых" данных
     */
    public void addProductToList(String args) {
        ItemProductList item = parseData(args);
        if (item.getIndexPosition() < 0)
            return;
        if (item.getIndexPosition() <= arrayProductList.size()) {
            arrayProductList.add(item.getIndexPosition(), item);
            Log.d("PLW", "add " + item.getIndexPosition());
            updateScreen(item.getIndexPosition(), true, true);
        } else {
            showToast("Невiрнi параметри при внесеннi товару, необхідно очистити чек!");
            Log.d(TAG, " ОШИБКА, количество товаров в списке: " + arrayProductList.size() + ", добавляется товар на позицию:" + item.getIndexPosition());
        }
    }

    /**
     * Вставляет товар в указанную позицию списка
     *
     * @param args строка "сырых" данных
     */
    public void setProductToList(String args) {
        if (arrayProductList.size() > 0) {
            ItemProductList item = parseData(args);
            if (item.getIndexPosition() < 0)
                return;

            if (item.getIndexPosition() < arrayProductList.size()) {
                ItemProductList presentItem = arrayProductList.get(item.getIndexPosition());
                if ((presentItem.getCount() != item.getCount())
                        || (presentItem.getSum() != item.getSum())
                        || (!presentItem.getCode().equals(item.getCode()))) {
                    arrayProductList.set(item.getIndexPosition(), item);
                    Log.d("PLW", "set " + item.getIndexPosition());
                    updateScreen(item.getIndexPosition(), true, true);   // updates only changed position
                }                                                               // !!! important for ResPOS
            } else {
                addProductToList(args);
            }

        } else {
            addProductToList(args);
        }
    }

    /**
     * Удаление товара в указанной позиции
     *
     * @param args строка "сырых" данных
     */
    public void deleteProductFromList(String args) {
        int indexPosition = Integer.MAX_VALUE;
        try {
            indexPosition = Integer.valueOf(args.substring(0, args.indexOf(SYMBOL_SEPARATOR)));  // 2
        } catch (Exception e) {
            Log.e(TAG, "ERROR parseData: " + e);
        }

        if (arrayProductList.size() > 0) {
            if ((arrayProductList.size() - 1) >= indexPosition) {
                arrayProductList.remove(indexPosition);
                Log.d("PLW", "delete " + indexPosition);
                updateScreen((arrayProductList.size() > 0) ? (arrayProductList.size() - 1) : 0, false, false);
            }
        }
    }

    /**
     * Очистка списка товаров
     */
    public void clearProductList() {
        arrayProductList.clear();
        Log.d("PLW", "clear");
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


        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:

                switch (arrayProductList.size()) {
                    case 0:

                        break;
                    case 1:

                        break;
                    case 2:

                        break;
                    default:

                        break;
                }

                int totalSum = 0;
                for (int i = 0; i < arrayProductList.size(); i++) {
                    ItemProductList selectedItem = arrayProductList.get(i);
                    totalSum += selectedItem.getSum();
                }

                String sumTotalToPay = String.format(Locale.FRENCH, "%.2f", (double) totalSum / 100);
                TextView textViewItemsToPaySum = ((Activity) context).findViewById(R.id.textview_items_to_pay_sum);
                textViewItemsToPaySum.setText(sumTotalToPay);
                MainActivity.textViewTotalSum.setText(sumTotalToPay);
                break;
            default:
                break;
        }

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
     * @param args raw data from ADDL and SETi commands
     * @return ItemProductList
     */
    private ItemProductList parseData(String args) {
        ItemProductList item = new ItemProductList();
        int argAmount = 7;
        // + 1 is tail string after last delimiter (cases, when last arg string will empty - "")
        String[] argList = args.split(Character.toString((char) SYMBOL_SEPARATOR), argAmount + 1);
        int argListLength = argList.length;

        if (argListLength != (argAmount + 1)) {
            item.setIndexPosition(-1);
            return item;
        }

        for (int argNum = 0; argNum < argAmount; argNum++) {
            switch (argNum) {
                case 0:
                    item.setIndexPosition(strToInt(argList[argNum]));
                    break;
                case 1:
                    item.setCode(argList[argNum]);
                    break;
                case 2:
                    item.setDivisible(strToInt(argList[argNum]));
                    break;
                case 3:
                    item.setCount(strToInt(argList[argNum]));
                    break;
                case 4:
                    item.setPrice(strToLong(argList[argNum]));
                    break;
                case 5:
                    item.setSum(strToLong(argList[argNum]));
                    break;
                case 6:
                    item.setName(argList[argNum]);
                    break;
                default:
                    break;
            }
        }
        return item;
    }

    private Integer strToInt(String str) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            Log.e(TAG, "ERROR strToInt: " + e);
        }
        return 0;
    }

    private Long strToLong(String str) {
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            Log.e(TAG, "ERROR strToLong: " + e);
        }
        return 0L;
    }

    public void showToast(String message) {
        Toast myToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        myToast.show();
    }
}
