package org.poo.main;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SplitPayment {
    private User user;
    private Account account;
    private double totalAmount;
    private double paymentAmount;
    private List<String> accounts;
    private List<Double> amounts;
    private String currency;
    private String splitPaymentCurrency;
    private String type;
    private boolean accepted;
    private int timestamp;
    public SplitPayment(final User user, final Account account, final double totalAmount,
             final double paymentAmount, final List<String> accounts, final List<Double> amounts,
             final String currency, final String splitPaymentCurrency, final String type,
             final int timestamp) {
        this.user = user;
        this.account = account;
        this.totalAmount = totalAmount;
        this.paymentAmount = paymentAmount;
        this.accounts = accounts;
        this.amounts = amounts;
        this.accepted = false;
        this.currency = currency;
        this.splitPaymentCurrency = splitPaymentCurrency;
        this.type = type;
        this.timestamp = timestamp;
    }

}
