package com.resonance.cashdisplay.product_list.look2;

import android.app.Activity;
import android.content.Context;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.product_list.ItemProductList;
import com.resonance.cashdisplay.product_list.ProductListWorker;
import com.resonance.cashdisplay.settings.PrefWorker;

import java.util.List;
import java.util.Locale;

import static com.resonance.cashdisplay.MainActivity.imageViewProduct;
import static com.resonance.cashdisplay.MainActivity.layoutTotal;
import static com.resonance.cashdisplay.MainActivity.relativeLayout;
import static com.resonance.cashdisplay.MainActivity.textViewTotalSum;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_CARD_BALANCE;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_CARD_CHARGING;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_CARD_PURCHASE;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_OTHER_GOODS;
import static com.resonance.cashdisplay.product_list.look2.KievSubwayArgs.SUBWAY_PRLS_QR_TICKET;

/**
 * Class handles behavior of Product List if interface {@link PrefWorker#LOOK_SUBWAY} is chosen.
 */

public class HandlerLook2 extends ProductListWorker implements Runnable {

    private Context mContext;
    private List<ItemProductList> arrayProductList;

    private ConstraintLayout layoutCardInfo;
    private TextView textViewCardNumber;

    private TextView textViewBalance;           // word "Баланс"
    private ImageView imageViewBalanceUnderline;// line below word "Баланс"

    private LinearLayout layoutItem12Block;     // layout of 1-2 items mode
    private TextView textViewItem1Name;         // name of 1-st product for 1-2 items mode
    private TableLayout layoutItem1Data;        // count and price of 1-st product for 1-2 items mode
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

    public HandlerLook2(Context context, List<ItemProductList> arrayList) {
        super(context);

        mContext = context;
        arrayProductList = arrayList;

        initUI();
    }

    public void initUI() {
        Activity activity = (Activity) mContext;

        layoutCardInfo = activity.findViewById(R.id.layout_card_info);
        textViewCardNumber = activity.findViewById(R.id.textview_card_number);

        textViewBalance = activity.findViewById(R.id.textview_balance);
        imageViewBalanceUnderline = activity.findViewById(R.id.imageview_balance_underline);

        layoutItem12Block = activity.findViewById(R.id.layout_item_1_2_block);
        textViewItem1Name = activity.findViewById(R.id.textview_item1_name);
        layoutItem1Data = activity.findViewById(R.id.layout_item1_data);
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

        Log.d("PLW", "HandlerLook2.init() successfully");
    }

    /**
     * Gets all arguments from PRLS command and starts clock timer.
     * Must be called when Product List is displayed.
     *
     * @param args arguments of PRLS command in one line
     * @see com.resonance.cashdisplay.CommandParser for PRLS command
     */
    public void onGetPRLSCommand(@Nullable String args) {
        if (args != null) {
            Log.d("PLW", "show=" + args);

            KievSubwayArgs.isCardPurchase = false;
            KievSubwayArgs.isPayment = true;
            KievSubwayArgs.isQR = false;
            KievSubwayArgs.isOtherGoods = false;
            KievSubwayArgs.itemsAmount = -1;                     // set to its default value

            String[] argList = args.split(Character.toString((char) SYMBOL_SEPARATOR));
            if (argList.length > 0)
                switch (argList[0]) {
                    case SUBWAY_PRLS_CARD_PURCHASE:
                        KievSubwayArgs.isCardPurchase = true;
                        break;
                    case SUBWAY_PRLS_CARD_BALANCE:
                        KievSubwayArgs.isPayment = false;
                        break;
                    case SUBWAY_PRLS_CARD_CHARGING:
                        KievSubwayArgs.isPayment = true;
                        break;
                    case SUBWAY_PRLS_QR_TICKET:
                        KievSubwayArgs.isQR = true;
                        break;
                    case SUBWAY_PRLS_OTHER_GOODS:
                        KievSubwayArgs.isOtherGoods = true;
                        break;
                    default:
                        break;
                }
            if (argList.length > 1)
                KievSubwayArgs.cardNumber = new StringBuilder(mContext.getString(R.string.card_num))
                        .append(argList[1])
                        .toString();
            if (argList.length > 2)
                KievSubwayArgs.itemsAmount = strToInt(argList[2]);
        }
    }

    @Override
    public void run() {
        int arrayProductListSize = arrayProductList.size();
        if (KievSubwayArgs.itemsAmount != arrayProductListSize) {   // control check right before UI operations
            setDefaultState();
            return;
        }
        KievSubwayArgs.itemsAmount = -1;

        Log.d("PLW", "dispItems = " + arrayProductListSize);

        Transition transition = new Fade();
        transition.setDuration(150);
        transition.addTarget(layoutItem12Block);
        transition.addTarget(layoutList);
        TransitionManager.beginDelayedTransition(relativeLayout[2], transition);    // scene root = product list

        try {
            int totalSum = 0;
            for (int i = 0; i < arrayProductListSize; i++) {
                if (arrayProductList.size() == arrayProductListSize) {      // protection if size of List accidentally grew
                    ItemProductList selectedItem = arrayProductList.get(i);
                    totalSum += selectedItem.getSum();
                } else
                    throw new IndexOutOfBoundsException();
            }
            String sumTotalToPay = String.format(Locale.FRENCH, "%.2f", (double) totalSum / 100);

            switch (arrayProductListSize) {
                case 0:
                    setDefaultState();
                    return;
                case 2:
                    ItemProductList item;
                    if (arrayProductList.size() == arrayProductListSize)    // protection if size of List accidentally grew
                        item = arrayProductList.get(1);
                    else
                        throw new IndexOutOfBoundsException();

                    textViewItem1Name.setSingleLine(true);

                    textViewItem2Name.setText(item.getName());
                    textViewItem2Count.setText((item.getCount() == -1) ? (mContext.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));

                    if (KievSubwayArgs.isPayment) {
                        textViewItem2Cost.setText(String.format(Locale.FRENCH, "%.02f", (double) item.getSum() / 100));
                        textViewCostLabel1.setVisibility(View.VISIBLE);
                        layoutItem1Cost.setVisibility(View.VISIBLE);
                        textViewCostLabel2.setVisibility(View.VISIBLE);
                        layoutItem2Cost.setVisibility(View.VISIBLE);

                        imageViewBalanceUnderline.setVisibility(View.GONE);
                        imageViewItem12Delimiter.setBackgroundResource(R.drawable.gradient_horizontal_center_white);
                        imageViewItem2Underline.setVisibility(View.VISIBLE);
                        setLayoutToPay12Items.setMargin(R.id.textview_to_pay_1_2_items, ConstraintSet.START, 540);      // shift from start of "До сплати" words
                        setLayoutToPay12Items.applyTo(layoutToPay12Items);
                    } else {
                        textViewCostLabel1.setVisibility(View.GONE);
                        layoutItem1Cost.setVisibility(View.GONE);
                        textViewCostLabel2.setVisibility(View.GONE);
                        layoutItem2Cost.setVisibility(View.GONE);

                        imageViewBalanceUnderline.setBackgroundResource(R.drawable.gradient_horizontal_left_white);
                        imageViewBalanceUnderline.setVisibility(View.VISIBLE);
                        imageViewItem12Delimiter.setBackgroundResource(R.drawable.gradient_horizontal_left_white);
                        imageViewItem2Underline.setVisibility(View.GONE);
                    }

                    layoutItem2.setVisibility(View.VISIBLE);
                case 1:
                    if (arrayProductListSize == 1)
                        layoutItem2.setVisibility(View.GONE);

                    if (arrayProductList.size() == arrayProductListSize) // protection if size of List accidentally grew
                        item = arrayProductList.get(0);
                    else
                        throw new IndexOutOfBoundsException();

                    textViewBalance.setGravity(Gravity.CENTER_VERTICAL);

                    if (arrayProductListSize == 1)
                        textViewItem1Name.setSingleLine(false);
                    textViewItem1Name.setText(item.getName());
                    textViewItem1Count.setText((item.getCount() == -1) ? (mContext.getString(R.string.unlimited)) : (String.valueOf(item.getCount())));

                    if (arrayProductListSize == 1) {
                        textViewCostLabel1.setVisibility(View.GONE);
                        layoutItem1Cost.setVisibility(View.GONE);
                        imageViewBalanceUnderline.setVisibility(View.GONE);
                    }

                    if (KievSubwayArgs.isCardPurchase)
                        layoutItem1Data.setVisibility(View.GONE);
                    else
                        layoutItem1Data.setVisibility(View.VISIBLE);

                    if (KievSubwayArgs.isPayment) {
                        textViewItem1Cost.setText(String.format(Locale.FRENCH, "%.02f", (double) item.getSum() / 100));
                        TextView textViewItem12ToPaySum = ((Activity) mContext).findViewById(R.id.textview_items_to_pay_sum);
                        textViewItem12ToPaySum.setText(sumTotalToPay);
                        if (arrayProductListSize == 1) {
                            setLayoutToPay12Items.setMargin(R.id.textview_to_pay_1_2_items, ConstraintSet.START, 0);        // shift from start of "До сплати" words
                            setLayoutToPay12Items.applyTo(layoutToPay12Items);
                        }
                        layoutToPay12Items.setVisibility(View.VISIBLE);        // 1-2 items mode
                    } else {
                        layoutToPay12Items.setVisibility(View.GONE);           // 1-2 items mode
                    }

                    ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 150;
                    if (KievSubwayArgs.isQR) {
                        imageViewProduct.setBackgroundResource(R.drawable.qr_dummy_w300);
                    } else {
                        imageViewProduct.setBackgroundResource(R.drawable.kyiv_smart_card_w300);
                    }

                    layoutList.setVisibility(View.GONE);                       // list mode
                    layoutItem12Block.setVisibility(View.VISIBLE);             // 1-2 items mode
                    break;
                default:
                    ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 100;
                    if (KievSubwayArgs.isQR)
                        imageViewProduct.setBackgroundResource(R.drawable.qr_dummy_w233);
                    else
                        imageViewProduct.setBackgroundResource(R.drawable.kyiv_smart_card_w233);

                    if (KievSubwayArgs.isPayment) {
                        textViewListHeaderSum.setVisibility(View.VISIBLE);
                        textViewTotalSum.setText(sumTotalToPay);
                        layoutTotal.setVisibility(View.VISIBLE);               // list mode
                        imageViewBalanceUnderline.setVisibility(View.GONE);
                    } else {
                        textViewListHeaderSum.setVisibility(View.GONE);
                        layoutTotal.setVisibility(View.GONE);                  // list mode
                        textViewBalance.setGravity(Gravity.CENTER);
                        imageViewBalanceUnderline.setBackgroundResource(R.drawable.gradient_horizontal_center_white);
                        imageViewBalanceUnderline.setVisibility(View.VISIBLE);
                    }

                    layoutItem12Block.setVisibility(View.GONE);                // 1-2 items mode
                    layoutList.setVisibility(View.VISIBLE);                    // list mode
                    break;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("PLW", e.toString());
            setDefaultState();
            return;
        }

        imageViewProduct.setVisibility(View.VISIBLE);

        if (KievSubwayArgs.isQR) {
            textViewCardNumber.setVisibility(View.GONE);
        } else {
            textViewCardNumber.setText(KievSubwayArgs.cardNumber);
            textViewCardNumber.setVisibility(View.VISIBLE);
        }

        if (KievSubwayArgs.isPayment)
            textViewBalance.setVisibility(View.GONE);
        else
            textViewBalance.setVisibility(View.VISIBLE);
    }

    /**
     * Return all components to their default state and stops clock timer.
     * Must be called when Product List is hiding.
     */
    public void setDefaultState() {
        imageViewProduct.setVisibility(View.INVISIBLE);
        imageViewProduct.setBackground(null);
        textViewCardNumber.setVisibility(View.GONE);
        textViewCardNumber.setText(mContext.getString(R.string.card_default));

        textViewBalance.setVisibility(View.GONE);
        textViewBalance.setGravity(Gravity.CENTER_VERTICAL);
        imageViewBalanceUnderline.setVisibility(View.GONE);
        imageViewBalanceUnderline.setBackgroundResource(R.drawable.gradient_horizontal_center_white);

        layoutItem12Block.setVisibility(View.GONE);
        textViewItem1Name.setSingleLine(false);
        layoutItem1Data.setVisibility(View.VISIBLE);
        textViewCostLabel1.setVisibility(View.GONE);
        layoutItem1Cost.setVisibility(View.GONE);
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

        ((LinearLayout.LayoutParams) layoutCardInfo.getLayoutParams()).weight = 150;

        KievSubwayArgs.itemsAmount = -1;
    }
}