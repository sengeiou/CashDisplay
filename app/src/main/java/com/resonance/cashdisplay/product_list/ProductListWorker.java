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
import androidx.constraintlayout.widget.ConstraintLayout;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.product_list.look2.KievSubwayArgs;
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
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_CARD_BALANCE;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_CARD_PAYMENT;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_QR_TICKET;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.isCharging;
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
    private ConstraintLayout layoutCardInfo;
    private TextView textViewCardNumber;
    private LinearLayout layoutItemsBlock;      // layout of 1-2 items mode
    private TextView textViewItemName;          // name of 1-st product for 1-2 items mode
    private TextView textViewItemCount;         // count of 1-st product for 1-2 items mode
    private TextView textViewCostLabel1;        // word "Вартiсть"
    private TextView textViewCostLabel2;        // word "Вартiсть"
    private LinearLayout layoutItemCost1;       // layout for 1-st "value ₴" for 1-2 items mode
    private LinearLayout layoutItemCost2;       // layout for 2-nd "value ₴" for 1-2 items mode
    private TextView textViewItemCost;          // price of 1-st item in 1-2 items mode
    private TextView textViewItemExtraCost;     // price of 2-nd item in 1-2 items mode
    private LinearLayout layoutItemExtra;       // second item for 1-2 items mode
    private TextView textViewItemExtraName;     // name of 2-nd product for 1-2 items mode
    private TextView textViewItemExtraCount;    // count of 2-nd product for 1-2 items mode
    private ImageView imageViewItemExtraBottomLine; // bottom line to delimit second product from to pay words
    private LinearLayout layoutToPay;           // "До сплати" for 1-2 items mode
    private LinearLayout layoutList;            // layout of list mode (more than 2 items)
    private TextView textviewListHeaderSum;     // text in header of list for "sum" column

    public static Timer clockTimer;

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
                layoutCardInfo = mainActivity.findViewById(R.id.layout_card_info);
                textViewCardNumber = mainActivity.findViewById(R.id.textview_card_number);
                layoutItemsBlock = mainActivity.findViewById(R.id.layout_items_block);
                textViewItemName = mainActivity.findViewById(R.id.textview_item_name);
                textViewItemCount = mainActivity.findViewById(R.id.textview_item_count);
                textViewCostLabel1 = mainActivity.findViewById(R.id.textview_cost_label1);
                textViewCostLabel2 = mainActivity.findViewById(R.id.textview_cost_label2);
                layoutItemCost1 = mainActivity.findViewById(R.id.layout_item_cost1);
                layoutItemCost2 = mainActivity.findViewById(R.id.layout_item_cost2);
                textViewItemCost = mainActivity.findViewById(R.id.textview_item_cost);
                textViewItemExtraCost = mainActivity.findViewById(R.id.textview_item_extra_cost);
                layoutItemExtra = mainActivity.findViewById(R.id.layout_item_extra);
                textViewItemExtraName = mainActivity.findViewById(R.id.textview_item_extra_name);
                textViewItemExtraCount = mainActivity.findViewById(R.id.textview_item_extra_count);
                imageViewItemExtraBottomLine = mainActivity.findViewById(R.id.imageview_item_extra_bottom_line);
                layoutToPay = mainActivity.findViewById(R.id.layout_to_pay);
                layoutList = mainActivity.findViewById(R.id.layout_list);
                textviewListHeaderSum = mainActivity.findViewById(R.id.textview_list_header_sum);
                break;
            default:
                break;
        }
    }

    /**
     * Called when product list screen becomes visible.
     * *
     *
     * @param args contains String list of additional arguments various for different looks.
     *             For different looks there may be 0 or more arguments, that are divided from each
     *             other with 0x03 symbols.
     *             <p>
     *             For LOOK_SUBWAY - args amount = 3:
     *             1 - sell mode, one of
     *             {@value KievSubwayArgs#SUBWAY_PRLS_CARD_BALANCE},
     *             {@value KievSubwayArgs#SUBWAY_PRLS_CARD_PAYMENT} or
     *             {@value KievSubwayArgs#SUBWAY_PRLS_QR_TICKET};
     *             2 - card number - any string, like "000000000000";
     *             3 - items amount - integer number of items in current receipt;
     */
    public void onProductListShow(@Nullable String args) {
        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                if (args != null) {
                    Log.d("PLW", "show=" + args);

                    imageViewProduct.setVisibility(View.INVISIBLE);
                    textViewCardNumber.setVisibility(View.GONE);
                    layoutToPay.setVisibility(View.GONE);             // for 1-2 items mode
                    layoutTotal.setVisibility(View.GONE);             // for list mode
                    imageViewItemExtraBottomLine.setVisibility(View.GONE);
                    textviewListHeaderSum.setVisibility(View.GONE);
                    KievSubwayArgs.isQR = false;
                    KievSubwayArgs.isCharging = false;
                    KievSubwayArgs.itemsAmount = 0;

                    String[] argList = args.split(Character.toString((char) SYMBOL_SEPARATOR));
                    if (argList.length > 0) {
                        switch (argList[0]) {
                            case SUBWAY_PRLS_CARD_BALANCE:
                                textViewCardNumber.setText(R.string.card_balance_num);
                                if (argList.length > 1)
                                    textViewCardNumber.append(argList[1]);        // card number argument
                                break;
                            case SUBWAY_PRLS_QR_TICKET:
                                KievSubwayArgs.isQR = true;
                            case SUBWAY_PRLS_CARD_PAYMENT:
                                if (argList[0].equals(SUBWAY_PRLS_CARD_PAYMENT)) {
                                    textViewCardNumber.setText(R.string.card_num);
                                    if (argList.length > 1) {
                                        textViewCardNumber.append(argList[1]);    // card number argument
                                    }
                                }
                                layoutToPay.setVisibility(View.VISIBLE);          // for 1-2 items mode
                                layoutTotal.setVisibility(View.VISIBLE);          // for list mode
                                imageViewItemExtraBottomLine.setVisibility(View.VISIBLE);
                                textviewListHeaderSum.setVisibility(View.VISIBLE);
                                isCharging = true;
                                break;
                            default:
                                break;
                        }
                    }
                    if (argList.length > 2)
                        KievSubwayArgs.itemsAmount = strToInt(argList[2]);
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
                textViewCardNumber.setVisibility(View.GONE);
                textViewCardNumber.setText(context.getString(R.string.card_default));
                imageViewProduct.setVisibility(View.INVISIBLE);
                imageViewProduct.setBackground(null);
                layoutItemsBlock.setVisibility(View.INVISIBLE);
                textViewCostLabel1.setVisibility(View.GONE);
                textViewCostLabel2.setVisibility(View.GONE);
                layoutItemCost1.setVisibility(View.GONE);
                layoutItemCost2.setVisibility(View.GONE);
                layoutItemExtra.setVisibility(View.GONE);
                layoutToPay.setVisibility(View.GONE);
                layoutList.setVisibility(View.INVISIBLE);
                layoutTotal.setVisibility(View.GONE);
                textviewListHeaderSum.setVisibility(View.GONE);

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

                if (arrayProductList.size() != KievSubwayArgs.itemsAmount)
                    return;

                new Handler().postDelayed(() -> {
                    Transition transition = new Fade();
                    transition.setDuration(150);        // 200
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
                            if (isCharging) {
                                textViewItemCost.setText(String.format(Locale.FRENCH, "%.02f", (double) item.getSum() / 100));
                                textViewCostLabel1.setVisibility(View.VISIBLE);
                                layoutItemCost1.setVisibility(View.VISIBLE);
                            }
                            layoutItemsBlock.setVisibility(View.VISIBLE);   // 1-2 items mode
                            layoutItemExtra.setVisibility(View.VISIBLE);
                            layoutList.setVisibility(View.INVISIBLE);       // list mode
                        case 1:
                            ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 150;
                            if (KievSubwayArgs.isQR)
                                imageViewProduct.setBackgroundResource(R.drawable.qr_dummy_w300);
                            else
                                imageViewProduct.setBackgroundResource(R.drawable.kyiv_smart_card_w300);
                            item = arrayProductList.get(0);
                            textViewItemName.setText(item.getName());
                            textViewItemCount.setText((item.getCount() == -1) ? (context.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));
                            if (isCharging) {
                                textViewItemExtraCost.setText(String.format(Locale.FRENCH, "%.02f", (double) item.getSum() / 100));
                                textViewCostLabel2.setVisibility(View.VISIBLE);
                                layoutItemCost2.setVisibility(View.VISIBLE);
                            }
                            layoutItemsBlock.setVisibility(View.VISIBLE);   // 1-2 items mode
                            if (arrayProductList.size() == 1)
                                layoutItemExtra.post(() -> {                // corrects flaw when specified view is going away
                                    layoutItemExtra.setVisibility(View.GONE);
                                });
                            layoutList.setVisibility(View.INVISIBLE);       // list mode
                            break;
                        default:
                            ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 100;
                            if (KievSubwayArgs.isQR)
                                imageViewProduct.setBackgroundResource(R.drawable.qr_dummy_w233);
                            else
                                imageViewProduct.setBackgroundResource(R.drawable.kyiv_smart_card_w233);
                            layoutItemsBlock.setVisibility(View.INVISIBLE); // 1-2 items mode
                            layoutList.setVisibility(View.VISIBLE);         // list mode
                            break;
                    }

                    imageViewProduct.setVisibility(View.VISIBLE);
                    if (!KievSubwayArgs.isQR)
                        textViewCardNumber.setVisibility(View.VISIBLE);

                    int totalSum = 0;
                    for (int i = 0; i < arrayProductList.size(); i++) {
                        ItemProductList selectedItem = arrayProductList.get(i);
                        totalSum += selectedItem.getSum();
                    }

                    String sumTotalToPay = String.format(Locale.FRENCH, "%.2f", (double) totalSum / 100);
                    TextView textViewItemsToPaySum = ((Activity) context).findViewById(R.id.textview_items_to_pay_sum);
                    textViewItemsToPaySum.setText(sumTotalToPay);
                    textViewTotalSum.setText(sumTotalToPay);

                }, 100);            // here delay provides smooth animation
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
