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
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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

import static com.resonance.cashdisplay.CommandParser.SYMBOL_SEPARATOR;
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

    private Context context;
    // animations for products images
    private Animation fadeOut;
    private Animation fadeIn;

    private AdapterProductList adapterProductList;
    private ArrayList<ItemProductList> arrayProductList = new ArrayList<ItemProductList>();

    private static Timer clockTimer;    // static link to timer to prevent creating multiple uncancelled timers

    private HandlerLook2 handlerLook2;  // object of special class for handling special look interface

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
                handlerLook2 = new HandlerLook2();
                handlerLook2.init(mainActivity);
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
                handlerLook2.onGetPRLSCommand(args);
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
                handlerLook2.setDefaultState();
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
            Bitmap productImage = adapterProductList.getImage(selectedItem.getCode());

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
                if (KievSubwayArgs.isPRLSworked
                        || KievSubwayArgs.itemsAmount != arrayProductList.size())
                    return;
                KievSubwayArgs.isPRLSworked = true;
                new Handler().postDelayed(handlerLook2, 100);     // delay provides more smooth animation
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

    /**
     * Provides clock for Product List screen.
     * Every 500ms specified TextView will set text, provided in arguments to this method:
     * arg1 - arg2 - arg1 - arg2 ....
     * If second argument == null, then only 1-st argument will change specified TextView:
     * arg1 - arg1 - arg1 - arg1 ....
     *
     * @param dateFormat0 main format, must be present necessarily;
     * @param dateFormat1 is optional, and may be equal {@code null}.
     */
    private void startClock(DateFormat dateFormat0, @Nullable DateFormat dateFormat1) {
        if (clockTimer == null) {
            clockTimer = new Timer();
            TextView textViewDateTime = ((Activity) context).findViewById(R.id.textview_date_time);
            Date date = new Date(System.currentTimeMillis());
            clockTimer.scheduleAtFixedRate(new TimerTask() {
                boolean flip = true;

                @Override
                public void run() {
                    date.setTime(System.currentTimeMillis());
                    if (flip) {
                        textViewDateTime.post(() ->
                                textViewDateTime.setText(dateFormat0.format(date)));
                        if (dateFormat1 == null)
                            return;
                    } else {
                        textViewDateTime.post(() ->
                                textViewDateTime.setText(dateFormat1.format(date)));
                    }
                    flip = !flip;
                }
            }, 0, 500);
        }
    }

    public void showToast(String message) {
        Toast myToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        myToast.show();
    }


    /**********************************************************************************************
     * Class handles behavior of Product List if interface {@link PrefWorker#LOOK_SUBWAY} is chosen.
     * ********************************************************************************************
     */
    private class HandlerLook2 implements Runnable {

        private ConstraintLayout layoutCardInfo;
        private TextView textViewCardNumber;

        private TextView textViewBalance;           // word "Баланс"
        private ImageView imageViewBalanceUnderline;// line below word "Баланс"

        private LinearLayout layoutItem12Block;     // layout of 1-2 items mode
        private TextView textViewItem1Name;         // name of 1-st product for 1-2 items mode
        private TextView textViewItem1Count;        // count of 1-st product for 1-2 items mode
        private TextView textViewCostLabel1;        // word "Вартiсть"
        private LinearLayout layoutItem1Cost;       // layout for 1-st "value ₴" for 1-2 items mode
        private TextView textViewItem1Cost;         // price of 1-st item in 1-2 items mode

        private LinearLayout layoutItem2;           // second item for 1-2 items mode
        private ImageView imageViewItem12Delimiter;
        private TextView textViewItem2Name;         // name of 2-nd product for 1-2 items mode
        private TextView textViewItem2Count;        // count of 2-nd product for 1-2 items mode
        private TextView textViewCostLabel2;        // word "Вартiсть"
        private LinearLayout layoutItem2Cost;       // layout for 2-nd "value ₴" for 1-2 items mode
        private TextView textViewItem2Cost;         // price of 2-nd item in 1-2 items mode
        private ImageView imageViewItem2Underline;  // bottom line to delimit second product from to pay words

        private ConstraintLayout layoutToPay12Items;// "До сплати" for 1-2 items mode
        private ConstraintSet setLayoutToPay12Items = new ConstraintSet();

        private TextView textViewListHeaderSum;     // text in header of list for "sum" column (total price for item)
        private LinearLayout layoutList;            // layout of list mode (more than 2 items)

        private void init(Activity activity) {
            layoutCardInfo = activity.findViewById(R.id.layout_card_info);
            textViewCardNumber = activity.findViewById(R.id.textview_card_number);

            textViewBalance = activity.findViewById(R.id.textview_balance);
            imageViewBalanceUnderline = activity.findViewById(R.id.imageview_balance_underline);

            layoutItem12Block = activity.findViewById(R.id.layout_item_1_2_block);
            textViewItem1Name = activity.findViewById(R.id.textview_item1_name);
            textViewItem1Count = activity.findViewById(R.id.textview_item1_count);
            textViewCostLabel1 = activity.findViewById(R.id.textview_cost_label1);
            layoutItem1Cost = activity.findViewById(R.id.layout_item1_cost);
            textViewItem1Cost = activity.findViewById(R.id.textview_item1_cost);

            layoutItem2 = activity.findViewById(R.id.layout_item2);
            imageViewItem12Delimiter = activity.findViewById(R.id.imageview_item_1_2_delimiter);
            textViewItem2Name = activity.findViewById(R.id.textview_item2_name);
            textViewItem2Count = activity.findViewById(R.id.textview_item2_count);
            textViewCostLabel2 = activity.findViewById(R.id.textview_cost_label2);
            layoutItem2Cost = activity.findViewById(R.id.layout_item2_cost);
            textViewItem2Cost = activity.findViewById(R.id.textview_item2_cost);
            imageViewItem2Underline = activity.findViewById(R.id.imageview_item2_underline);

            layoutToPay12Items = activity.findViewById(R.id.layout_to_pay_1_2_items);
            setLayoutToPay12Items.clone(layoutToPay12Items);

            textViewListHeaderSum = activity.findViewById(R.id.textview_list_header_sum);
            layoutList = activity.findViewById(R.id.layout_list);
        }

        /**
         * Gets all arguments from PRLS command and starts clock timer.
         * Must be called when Product List is displayed.
         *
         * @param args arguments of PRLS command in one line
         * @see com.resonance.cashdisplay.CommandParser for PRLS command
         */
        private void onGetPRLSCommand(@Nullable String args) {
            if (args != null) {
                Log.d("PLW", "show=" + args);

                KievSubwayArgs.isQR = false;
                KievSubwayArgs.isCharging = false;
                KievSubwayArgs.itemsAmount = 0;

                String[] argList = args.split(Character.toString((char) SYMBOL_SEPARATOR));
                if (argList.length > 0)
                    switch (argList[0]) {
                        case SUBWAY_PRLS_CARD_BALANCE:
                            KievSubwayArgs.isCharging = false;
                            break;
                        case SUBWAY_PRLS_QR_TICKET:
                            KievSubwayArgs.isQR = true;
                        case SUBWAY_PRLS_CARD_PAYMENT:
                            KievSubwayArgs.isCharging = true;
                            break;
                        default:
                            break;
                    }
                if (argList.length > 1) {
                    KievSubwayArgs.cardNumber = new StringBuilder(context.getString(R.string.card_num))
                            .append(argList[1])
                            .toString();
                }
                if (argList.length > 2)
                    KievSubwayArgs.itemsAmount = strToInt(argList[2]);
            }

            DateFormat dateFormat0 = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("uk"));
            DateFormat dateFormat1 = new SimpleDateFormat("dd.MM.yyyy HH mm", new Locale("uk"));
            startClock(dateFormat0, dateFormat1);
        }

        @Override
        public void run() {
            int totalSum = 0;
            for (int i = 0; i < arrayProductList.size(); i++) {
                ItemProductList selectedItem = arrayProductList.get(i);
                totalSum += selectedItem.getSum();
            }
            String sumTotalToPay = String.format(Locale.FRENCH, "%.2f", (double) totalSum / 100);

            Transition transition = new Fade();
            transition.setDuration(150);
            transition.addTarget(layoutItem12Block);
            transition.addTarget(layoutList);
            TransitionManager.beginDelayedTransition(relativeLayout[2], transition);    // scene root = product list

            switch (KievSubwayArgs.itemsAmount) {               // current amount of products that are in list right now
                case 0:
                    return;
                case 2:
                    ItemProductList item = arrayProductList.get(1);
                    textViewItem2Name.setText(item.getName());
                    textViewItem2Count.setText((item.getCount() == -1) ? (context.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));

                    if (KievSubwayArgs.isCharging) {
                        textViewItem2Cost.setText(String.format(Locale.FRENCH, "%.02f", (double) item.getSum() / 100));
                        textViewCostLabel1.setVisibility(View.VISIBLE);
                        layoutItem1Cost.setVisibility(View.VISIBLE);
                        textViewCostLabel2.setVisibility(View.VISIBLE);
                        layoutItem2Cost.setVisibility(View.VISIBLE);

                        imageViewItem2Underline.setVisibility(View.VISIBLE);
                        setLayoutToPay12Items.setMargin(R.id.textview_to_pay_1_2_items, ConstraintSet.START, 540);      // shift from start of "До сплати" words
                        setLayoutToPay12Items.applyTo(layoutToPay12Items);
                    } else {
                        imageViewBalanceUnderline.setBackgroundResource(R.drawable.gradient_horizontal_left_white);
                        imageViewItem12Delimiter.setBackgroundResource(R.drawable.gradient_horizontal_left_white);
                        imageViewBalanceUnderline.setVisibility(View.VISIBLE);
                    }

                    layoutItem2.setVisibility(View.VISIBLE);
                case 1:
                    item = arrayProductList.get(0);
                    textViewItem1Name.setText(item.getName());
                    textViewItem1Count.setText((item.getCount() == -1) ? (context.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));

                    if (KievSubwayArgs.isCharging) {
                        textViewItem1Cost.setText(String.format(Locale.FRENCH, "%.02f", (double) item.getSum() / 100));

                        TextView textViewItem12ToPaySum = ((Activity) context).findViewById(R.id.textview_items_to_pay_sum);
                        textViewItem12ToPaySum.setText(sumTotalToPay);
                        layoutToPay12Items.setVisibility(View.VISIBLE);            // 1-2 items mode
                    }

                    if (KievSubwayArgs.isQR) {
                        imageViewProduct.setBackgroundResource(R.drawable.qr_dummy_w300);
                    } else {
                        imageViewProduct.setBackgroundResource(R.drawable.kyiv_smart_card_w300);
                    }

                    layoutItem12Block.setVisibility(View.VISIBLE);                  // 1-2 items mode
                    break;
                default:
                    ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 100;
                    if (KievSubwayArgs.isQR)
                        imageViewProduct.setBackgroundResource(R.drawable.qr_dummy_w233);
                    else
                        imageViewProduct.setBackgroundResource(R.drawable.kyiv_smart_card_w233);

                    if (KievSubwayArgs.isCharging) {
                        textViewListHeaderSum.setVisibility(View.VISIBLE);
                        textViewTotalSum.setText(sumTotalToPay);
                        layoutTotal.setVisibility(View.VISIBLE); // list mode
                    } else {
                        textViewBalance.setGravity(Gravity.CENTER);
                        imageViewBalanceUnderline.setVisibility(View.VISIBLE);
                    }

                    layoutList.setVisibility(View.VISIBLE);                        // list mode
                    break;
            }

            imageViewProduct.setVisibility(View.VISIBLE);

            if (!KievSubwayArgs.isQR) {
                textViewCardNumber.setText(KievSubwayArgs.cardNumber);
                textViewCardNumber.setVisibility(View.VISIBLE);
            }

            if (!KievSubwayArgs.isCharging)
                textViewBalance.setVisibility(View.VISIBLE);
        }

        /**
         * Return all components to their default state and stops clock timer.
         * Must be called when Product List is hiding.
         */
        private void setDefaultState() {
            ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 150;
            imageViewProduct.setVisibility(View.INVISIBLE);
            imageViewProduct.setBackground(null);
            textViewCardNumber.setVisibility(View.GONE);
            textViewCardNumber.setText(context.getString(R.string.card_default));

            textViewBalance.setVisibility(View.GONE);
            textViewBalance.setGravity(Gravity.CENTER_VERTICAL);
            imageViewBalanceUnderline.setVisibility(View.GONE);

            layoutItem12Block.setVisibility(View.GONE);
            textViewCostLabel1.setVisibility(View.GONE);
            layoutItem1Cost.setVisibility(View.GONE);
            imageViewBalanceUnderline.setBackgroundResource(R.drawable.gradient_horizontal_center_white);
            imageViewItem12Delimiter.setBackgroundResource(R.drawable.gradient_horizontal_center_white);
            layoutItem2.setVisibility(View.GONE);
            textViewCostLabel2.setVisibility(View.GONE);
            layoutItem2Cost.setVisibility(View.GONE);
            imageViewItem2Underline.setVisibility(View.GONE);

            layoutToPay12Items.setVisibility(View.GONE);
            setLayoutToPay12Items.setMargin(R.id.textview_to_pay_1_2_items, ConstraintSet.START, 0);      // shift from start of "До сплати" words
            setLayoutToPay12Items.applyTo(layoutToPay12Items);

            layoutList.setVisibility(View.GONE);
            layoutTotal.setVisibility(View.GONE);
            textViewListHeaderSum.setVisibility(View.GONE);

            KievSubwayArgs.isPRLSworked = false;

            if (clockTimer != null) {
                clockTimer.cancel();
                clockTimer = null;
            }
        }
    }
}
