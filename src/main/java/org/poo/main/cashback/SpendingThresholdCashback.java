package org.poo.main.cashback;

import lombok.Getter;
import lombok.Setter;
import org.poo.fileio.CommerciantInput;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Commerciant;
import org.poo.main.User;

@Getter
@Setter
public final class SpendingThresholdCashback extends Cashback {

    public SpendingThresholdCashback(final Account account, final CommerciantInput lastCommerciant,
                                     final String amount, final Bank bank) {
        super(account, lastCommerciant, amount, bank);
    }

    /**
     * Calculate cashback for last transaction involving spending threshold commerciant strategy
     * @return Amount of cashback given for last transaction
     */
    public double getCashbackSpendingThreshold() {
        User user = bank.searchUserByAccount(account.getNumber());
        double doubleAmount = Double.parseDouble(amount.split(" ")[0]);
        double exchangedAmount = account.getCurrency().equals("RON") ? doubleAmount
                : bank.exchange("RON", account.getCurrency(), doubleAmount);
        account.setSpendingThreshold(account.getSpendingThreshold() + exchangedAmount);

        double cashback = switch (user.getPlan()) {
            case "standard", "student" ->
                    getStandardOrStudentCashback(account.getSpendingThreshold(), doubleAmount);
            case "silver" -> getSilverCashback(account.getSpendingThreshold(), doubleAmount);
            case "gold" -> getGoldCashback(account.getSpendingThreshold(), doubleAmount);
            default -> 0;
        };

        double parsedAmount = Double.parseDouble(amount.split(" ")[0]);
        String commerciantType = lastCommerciant.getType();
        Commerciant lastCommerciantOfAccount =
                account.searchCommerciant(lastCommerciant.getCommerciant());
        lastCommerciantOfAccount.setTransactions(lastCommerciantOfAccount.getTransactions() + 1);

        if (applyCashbackForType(account, commerciantType)) {
            cashback += cashbackAmount(commerciantType, parsedAmount);
        }
        return cashback;
    }

    public double getCashbackNrOfTransactions() {
        return 0;
    }

    private double getCashbackOnThreshold(
            final double spendingThreshold,
            final double transactionAmount,
            final double[] cashbackRates) {
        if (spendingThreshold >= MINIMUM_AMOUNTS_FOR_SPENDING_THRESHOLD_CASHBACK[0]) {
            return transactionAmount * cashbackRates[0];
        } else if (spendingThreshold >= MINIMUM_AMOUNTS_FOR_SPENDING_THRESHOLD_CASHBACK[1]) {
            return transactionAmount * cashbackRates[1];
        } else if (spendingThreshold >= MINIMUM_AMOUNTS_FOR_SPENDING_THRESHOLD_CASHBACK[2]) {
            return transactionAmount * cashbackRates[2];
        }
        return 0;
    }

    private double getStandardOrStudentCashback(
            final double spendingThreshold, final double transactionAmount) {
        return getCashbackOnThreshold(spendingThreshold, transactionAmount,
                SPENDING_THRESHOLD_CASHBACK_FOR_STANDARD_PLAN);
    }

    private double getSilverCashback(
            final double spendingThreshold, final double transactionAmount) {
        return getCashbackOnThreshold(
                spendingThreshold, transactionAmount, SPENDING_THRESHOLD_CASHBACK_FOR_SILVER_PLAN);
    }

    private double getGoldCashback(final double spendingThreshold, final double transactionAmount) {
        return getCashbackOnThreshold(
                spendingThreshold, transactionAmount, SPENDING_THRESHOLD_CASHBACK_FOR_GOLD_PLAN);
    }

    //Helper method for getCashbackNrOfTransactions
    private double cashbackAmount(final String type, final double amount) {
        return switch (type) {
            case "Food" -> amount * NUMBER_OF_TRANSACTIONS_CASHBACK_FOR_EACH_TYPE[0];
            case "Clothes" -> amount * NUMBER_OF_TRANSACTIONS_CASHBACK_FOR_EACH_TYPE[1];
            case "Tech" -> amount * NUMBER_OF_TRANSACTIONS_CASHBACK_FOR_EACH_TYPE[2];
            default -> 0;
        };
    }

}
