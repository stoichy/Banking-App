package org.poo.handlers;

import org.poo.fileio.CommandInput;
import org.poo.main.Bank;

public final class HandlerFactory {
    private static HandlerFactory instance = null;

    /**
     * Singleton pattern implementation for the usage of only one instance of class
     */
    public static HandlerFactory getInstance() {
        if (instance == null) {
            instance = new HandlerFactory();
        }
        return instance;
    }

    /**
     * Gets the required handler based on the given command
     * @param commandHandlerType Command name
     * @param bank Bank data
     * @return Required command handler for running given command
     */
    public CommandHandler getHandlerType(final CommandInput commandHandlerType, final Bank bank) {
        return switch (commandHandlerType.getCommand()) {
            case "addAccount", "setMinBalance", "printTransactions", "withdrawSavings",
                 "addNewBusinessAssociate" -> new UserCommandHandler(commandHandlerType, bank);
            case "printUsers", "addInterest", "splitPayment", "changeInterestRate",
                 "rejectSplitPayment", "acceptSplitPayment", "businessReport" ->
                new BankCommandHandler(commandHandlerType, bank);
            case "createCard", "createOneTimeCard", "report", "spendingsReport", "setAlias",
                 "sendMoney", "deleteAccount", "addFunds", "upgradePlan", "changeSpendingLimit",
                 "changeDepositLimit" -> new AccountCommandHandler(commandHandlerType, bank);
            case "deleteCard", "checkCardStatus", "payOnline", "cashWithdrawal" ->
                new CardCommandHandler(commandHandlerType, bank);
            default -> null;
        };
    }
}
