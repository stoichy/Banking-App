package org.poo.main.cashback;

import lombok.Getter;
import lombok.Setter;
import org.poo.fileio.CommerciantInput;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Commerciant;

@Getter
@Setter
public final class NrOfTransactionsCashback extends Cashback {

    public NrOfTransactionsCashback(final Account account, final CommerciantInput lastCommerciant,
                                    final String amount, final Bank bank) {
        super(account, lastCommerciant, amount, bank);
    }

    /**
     * Calculate cashback for last transaction involving number of transactions commerciant strategy
     * @return Amount of cashback given for last transaction
     */
    public double getCashbackNrOfTransactions() {
        double parsedAmount = Double.parseDouble(amount.split(" ")[0]);
        String commerciantType = lastCommerciant.getType();

        Commerciant lastCommerciantOfAccount =
                account.searchCommerciant(lastCommerciant.getCommerciant());
        lastCommerciantOfAccount.setTransactions(lastCommerciantOfAccount.getTransactions() + 1);

        if (applyCashbackForType(account, commerciantType)) {
            return cashbackAmount(commerciantType, parsedAmount);
        }
        updateCashbackBasedOnTransactions(account, lastCommerciantOfAccount.getTransactions());

        return 0;
    }

    public double getCashbackSpendingThreshold() {
        return 0;
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

    //Helper method for getCashbackNrOfTransactions
    private void updateCashbackBasedOnTransactions(final Account account, final int transactions) {
        if (transactions == NUMBER_OF_TRANSACTIONS_FOR_CASHBACK[0]) {
            account.setCashbackForFood(1);
        }
        if (transactions == NUMBER_OF_TRANSACTIONS_FOR_CASHBACK[1]) {
            account.setCashbackForClothes(1);
        }
        if (transactions == NUMBER_OF_TRANSACTIONS_FOR_CASHBACK[2]) {
            account.setCashbackForTech(1);
        }
    }

}
