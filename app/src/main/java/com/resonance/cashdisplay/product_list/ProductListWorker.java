package com.resonance.cashdisplay.product_list;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.resonance.cashdisplay.Log;
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

import static com.resonance.cashdisplay.MainActivity.imageViewProduct;
import static com.resonance.cashdisplay.MainActivity.layoutTotal;
import static com.resonance.cashdisplay.MainActivity.listViewProducts;
import static com.resonance.cashdisplay.MainActivity.relativeLayout;
import static com.resonance.cashdisplay.MainActivity.scrollViewDebug;
import static com.resonance.cashdisplay.MainActivity.textViewDebug;
import static com.resonance.cashdisplay.MainActivity.textViewTotalCount;
import static com.resonance.cashdisplay.MainActivity.textViewTotalDiscount;
import static com.resonance.cashdisplay.MainActivity.textViewTotalSum;
import static com.resonance.cashdisplay.MainActivity.textViewTotalSumWithoutDiscount;
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

    // used for LOOK_SUBWAY only
    private TextView textViewCardNumber;
    private LinearLayout layoutItemsBlock;      // layout of 1-2 items mode
    private LinearLayout layoutItemExtra;       // second item for 1-2 items mode
    private LinearLayout layoutToPay;           // "До сплати" for 1-2 items mode
    private LinearLayout layoutList;            // layout of list mode (more than 2 items)
    private TextView textViewItemName;          // name of 1-st product for 1-2 items mode
    private TextView textViewItemCount;         // count of 1-st product for 1-2 items mode
    private TextView textViewItemExtraName;     // name of 2-nd product for 1-2 items mode
    private TextView textViewItemExtraCount;    // count of 2-nd product for 1-2 items mode
    private ImageView imageViewItemExtraBottomLine; // bottom line to delimit second product from to pay words

    public static Timer clockTimer;

    private boolean doInstantly = false;        // service field, see in code for more details

    public ProductListWorker(Context context) {
        this.context = context;

        fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(100);
        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(300);

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
     * Must be called after specified look set up, because make initialization of components,
     * that are absent in another looks or that are .
     *
     * @see PrefValues#productListLookCode for more information about looks
     */
    public void initUniqueComponents() {
        Activity mainActivity = (Activity) context;
        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                textViewCardNumber = mainActivity.findViewById(R.id.textview_card_number);
                layoutItemsBlock = mainActivity.findViewById(R.id.layout_items_block);
                textViewItemName = mainActivity.findViewById(R.id.textview_item_name);
                textViewItemCount = mainActivity.findViewById(R.id.textview_item_count);
                layoutItemExtra = mainActivity.findViewById(R.id.layout_item_extra);
                textViewItemExtraName = mainActivity.findViewById(R.id.textview_item_extra_name);
                textViewItemExtraCount = mainActivity.findViewById(R.id.textview_item_extra_count);
                imageViewItemExtraBottomLine = mainActivity.findViewById(R.id.imageview_item_extra_bottom_line);
                layoutToPay = mainActivity.findViewById(R.id.layout_to_pay);
                layoutList = mainActivity.findViewById(R.id.layout_list);
                break;
            default:
                break;
        }
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
                    int argAmount = 2;                   // according "inner" protocol for this look

                    String[] argList = args.split(Character.toString((char) SYMBOL_SEPARATOR));

                    // little animation when setting view (view group) visible or invisible
                    Transition transition = new Fade();
                    transition.setDuration(120);
                    transition.addTarget(textViewCardNumber);
                    TransitionManager.beginDelayedTransition(relativeLayout[2], transition);    // scene root = product list
                    textViewCardNumber.setVisibility(View.VISIBLE);

                    if (argList.length == argAmount) {
                        if (argList[0].equals("balance")) {                   // "payment" or "balance" argument
                            textViewCardNumber.setText(R.string.card_balance_num);
                            layoutToPay.setVisibility(View.GONE);             // for 1-2 items mode
                            layoutTotal.setVisibility(View.GONE);             // for list mode
                            imageViewItemExtraBottomLine.setVisibility(View.GONE);
                        } else {
                            textViewCardNumber.setText(R.string.card_num);
                            layoutToPay.setVisibility(View.VISIBLE);          // for 1-2 items mode
                            layoutTotal.setVisibility(View.VISIBLE);          // for list mode
                            imageViewItemExtraBottomLine.setVisibility(View.VISIBLE);
                        }
                        textViewCardNumber.append(argList[1]);                // card number argument
                    } else
                        textViewCardNumber.setText(R.string.card_default);    // for insurance (wrong args for some reason)
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
                    }, 0, 500);
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
                textViewCardNumber.setVisibility(View.INVISIBLE);
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
        int start = textViewDebug.getText().length();
        textViewDebug.append(msg + "\n");
        int end = textViewDebug.getText().length();
        Spannable spannableText = (Spannable) textViewDebug.getText();
        spannableText.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, 0);
        scrollViewDebug.fullScroll(View.FOCUS_DOWN);
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
        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_DMART:
                scrollToPosition(position, highlightItem);
                updateTotalValues();
                break;
            case LOOK_SUBWAY:
                scrollToPosition(position, false);
                break;
            default:
                scrollToPosition(position, highlightItem);
                setProductImage(position, animProductImage);
                updateTotalValues();
                break;
        }
        handleUniqueLooks();
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
                    imageViewProduct.setImageBitmap(productImage);
                    imageViewProduct.startAnimation(fadeIn);
                }
            });

            if (animProductImage)
                imageViewProduct.startAnimation(fadeOut);
            else
                imageViewProduct.setImageBitmap(productImage);
        } else
            imageViewProduct.setImageBitmap(null);
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

        textViewTotalSumWithoutDiscount.setText(String.format(Locale.ROOT, "%.2f", (double) totalSumWithoutDiscount / 100));
        textViewTotalDiscount.setText(String.format(Locale.ROOT, "%.2f", (double) totalDiscount / 100));
        textViewTotalSum.setText(String.format(Locale.ROOT, "%.2f", (double) totalSum / 100));
        textViewTotalCount.setText(String.valueOf(arrayProductList.size()));
        textViewDebug.append("MSG_totalSumWithoutDiscount: " + totalSumWithoutDiscount + "\n");
        textViewDebug.append("MSG_totalDiscount: " + totalDiscount + "\n");
        textViewDebug.append("MSG_totalSum: " + totalSum + "\n");
        textViewDebug.append("MSG_totalCount: " + arrayProductList.size() + "\n");
        new Handler().post(() -> scrollViewDebug.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Works with looks, that have unique components, which are absent in another looks.
     *
     * @see PrefValues#productListLookCode for more information about looks
     */
    private void handleUniqueLooks() {
        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                // provides executing of this method every time with delay
                if (!doInstantly) {
                    new Handler().postDelayed(() -> {
                        doInstantly = true;
                        handleUniqueLooks();
                    }, 50);
                    return;
                }
                doInstantly = false;

                // little animation when setting view (view group) visible or invisible
                Transition transition = new Fade();
                transition.setDuration(200);
                transition.addTarget(layoutItemsBlock);
                transition.addTarget(layoutItemExtra);
                transition.addTarget(layoutList);
                TransitionManager.beginDelayedTransition(relativeLayout[2], transition);    // scene root = product list

                switch (arrayProductList.size()) {     // current amount of products that are in list right now
                    case 0:
                        layoutItemsBlock.setVisibility(View.INVISIBLE);
                        layoutItemExtra.setVisibility(View.GONE);
                        layoutList.setVisibility(View.INVISIBLE);
                        break;
                    case 2:
                        ItemProductList item = arrayProductList.get(1);
                        textViewItemExtraName.setText(item.getName());
                        textViewItemExtraCount.setText((item.getCount() == -1) ? (context.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));
                        layoutItemsBlock.setVisibility(View.VISIBLE);   // 1-2 items mode
                        layoutItemExtra.setVisibility(View.VISIBLE);
                        layoutList.setVisibility(View.INVISIBLE);       // list mode
                    case 1:
                        item = arrayProductList.get(0);
                        textViewItemName.setText(item.getName());
                        textViewItemCount.setText((item.getCount() == -1) ? (context.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));
                        layoutItemsBlock.setVisibility(View.VISIBLE);   // 1-2 items mode
                        if (arrayProductList.size() == 1)
                            layoutItemExtra.post(() -> {                // corrects flaw when specified view is going away
                                layoutItemExtra.setVisibility(View.GONE);
                            });
                        layoutList.setVisibility(View.INVISIBLE);       // list mode
                        break;
                    default:
                        layoutItemsBlock.setVisibility(View.INVISIBLE); // 1-2 items mode
                        layoutList.setVisibility(View.VISIBLE);         // list mode
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
                textViewTotalSum.setText(sumTotalToPay);
                break;
            default:
                break;
        }
    }

    /**
     * Парсер данных с ResPos (for ADDL and SETi commands)
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
