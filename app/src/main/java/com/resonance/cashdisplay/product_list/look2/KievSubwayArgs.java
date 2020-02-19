package com.resonance.cashdisplay.product_list.look2;

import com.resonance.cashdisplay.settings.PrefWorker;

/**
 * Class encapsulates arguments, received in PRLS command for {@link PrefWorker#LOOK_SUBWAY}.
 *
 * @see com.resonance.cashdisplay.CommandParser for PRLS command
 */
public class KievSubwayArgs {

    public final static String SUBWAY_PRLS_CARD_BALANCE = "card_balance";
    public final static String SUBWAY_PRLS_CARD_PAYMENT = "card_charging";
    public final static String SUBWAY_PRLS_QR_TICKET = "qr_ticket";

    public static boolean isQR = false;               // is current transaction is doing with qr-ticket (card in another case)
    public static boolean isCharging = false;         // by default transaction is not payment, but balance view (actual only for card)
    public static int itemsAmount = 0;                // items amount for this receipt
}