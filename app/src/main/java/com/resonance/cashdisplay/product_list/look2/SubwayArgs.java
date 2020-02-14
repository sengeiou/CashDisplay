package com.resonance.cashdisplay.product_list.look2;

import com.resonance.cashdisplay.settings.PrefWorker;

/**
 * Class encapsulates arguments, received in PRLS command for {@link PrefWorker#LOOK_SUBWAY}.
 *
 * @see com.resonance.cashdisplay.CommandParser for PRLS command
 */
public class SubwayArgs {

    public final static String SUBWAY_PRLS_CARD_BALANCE = "card_balance";
    public final static String SUBWAY_PRLS_CARD_PAYMENT = "card_payment";
    public final static String SUBWAY_PRLS_QR_TICKET = "qr_ticket";

    public boolean isCard = true;              // is operation doing with card or qr-ticket
    public int itemsAmount = 0;                // items amount for this receipt
}
