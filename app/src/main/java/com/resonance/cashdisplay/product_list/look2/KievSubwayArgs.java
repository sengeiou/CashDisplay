package com.resonance.cashdisplay.product_list.look2;

import com.resonance.cashdisplay.settings.PrefWorker;

/**
 * Class encapsulates arguments, received in PRLS command for {@link PrefWorker#LOOK_SUBWAY}.
 * This arguments are represented in constants of this class.
 * <p>
 * There are 5 different kinds of displaying information, and arguments determine which one to use.
 *
 * @see com.resonance.cashdisplay.CommandParser for PRLS command
 */
public class KievSubwayArgs {

    public final static String SUBWAY_PRLS_CARD_PURCHASE = "card_purchase";
    public final static String SUBWAY_PRLS_CARD_BALANCE = "card_balance";
    public final static String SUBWAY_PRLS_CARD_CHARGING = "card_charging";
    public final static String SUBWAY_PRLS_QR_TICKET = "qr_ticket";
    public final static String SUBWAY_PRLS_OTHER_GOODS = "other_goods";

    // May be 4 different kinds of receipts, and every receipt is separate (for example, qr-ticket will not be sold with any other items or goods).
    // Also there is 1 kind of receipt with card - balance view for smart card. Totally = 5 items.
    public static boolean isCardPurchase = false;     // smart card purchase or not (only 1 item in receipt - the same smart card, and without "Кiлькiсть поiздок")
    public static boolean isPayment = true;           // determines if "До сплати" must be displayed (otherwise "Баланс" for smart card displayed)
    public static boolean isQR = false;               // qr-ticket purchase or not (smart card in another case by default)
    public static boolean isOtherGoods = false;       // special receipt with other goods (mineral water, cigarettes)

    public static int itemsAmount = -1;               // integer number of items in current receipt; default = -1
    public static String cardNumber;                  // for smart card only (any value for qr-ticket)
}