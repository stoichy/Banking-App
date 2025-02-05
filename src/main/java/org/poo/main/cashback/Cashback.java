package org.poo.main.cashback;

import org.poo.fileio.CommerciantInput;
import org.poo.main.Account;
import org.poo.main.Bank;

public abstract class Cashback {
    protected final Account account;
    protected CommerciantInput lastCommerciant;
    protected String amount;
    protected Bank bank;
    protected static final int[] MINIMUM_AMOUNTS_FOR_SPENDING_THRESHOLD_CASHBACK = {500, 300, 100};
    protected static final double[] SPENDING_THRESHOLD_CASHBACK_FOR_STANDARD_PLAN =
            {0.0025, 0.002, 0.001};
    protected static final double[] SPENDING_THRESHOLD_CASHBACK_FOR_SILVER_PLAN =
            {0.005, 0.004, 0.003};
    protected static final double[] SPENDING_THRESHOLD_CASHBACK_FOR_GOLD_PLAN =
            {0.007, 0.0055, 0.005};
    protected static final double[] NUMBER_OF_TRANSACTIONS_CASHBACK_FOR_EACH_TYPE =
            {0.02, 0.05, 0.1};
    protected static final double[] NUMBER_OF_TRANSACTIONS_FOR_CASHBACK = {2, 5, 10};

    public Cashback(final Account account, final CommerciantInput lastCommerciant,
                                     final String amount, final Bank bank) {
        this.account = account;
        this.lastCommerciant = lastCommerciant;
        this.amount = amount;
        this.bank = bank;
    }

    /**
     * Checks if cashback for a type of commerciant was already applied. If it wasn't, return true
     * @param transactionAccount Account involved in the transaction
     * @param type Type of commerciant
     * @return True if cashback wasn't applied, false otherwise
     */
    public final boolean applyCashbackForType(final Account transactionAccount, final String type) {
        if ("Food".equals(type) && transactionAccount.getCashbackForFood() > 0) {
            account.setCashbackForFood(-1);
            return true;
        }
        if ("Clothes".equals(type) && transactionAccount.getCashbackForClothes() > 0) {
            account.setCashbackForClothes(-1);
            return true;
        }
        if ("Tech".equals(type) && transactionAccount.getCashbackForTech() > 0) {
            account.setCashbackForTech(-1);
            return true;
        }
        return false;
    }

    /**
     * Calculate cashback for last transaction involving nrOfTransaction strategy
     * @return Amount of cashback given for last transaction
     */
    public abstract double getCashbackNrOfTransactions();

    /**
     * Calculate cashback for last transaction involving spendingThreshold strategy
     * @return Amount of cashback given for last transaction
     */
    public abstract double getCashbackSpendingThreshold();
}
