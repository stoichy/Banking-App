package org.poo.handlers;

import org.poo.fileio.CommandInput;
import org.poo.main.Bank;

public class HandlerFactory {
    private static HandlerFactory instance = null;

    public static HandlerFactory getInstance() {
        if (instance == null) {
            instance = new HandlerFactory();
        }
        return instance;
    }

    public CommandHandler getHandlerType(CommandInput commandHandlerType, Bank bank) {
        return switch (commandHandlerType.getCommand()) {
            case "addAccount", "setMinBalance", "printTransactions", "withdrawSavings", "addNewBusinessAssociate" ->
                new UserCommandHandler(commandHandlerType, bank);
            case "printUsers", "addInterest", "splitPayment", "changeInterestRate",
                 "rejectSplitPayment", "acceptSplitPayment", "businessReport" ->
                new BankCommandHandler(commandHandlerType, bank);
            case "createCard", "createOneTimeCard", "report", "spendingsReport", "setAlias",
                 "sendMoney", "deleteAccount", "addFunds", "upgradePlan", "changeSpendingLimit", "changeDepositLimit" ->
                new AccountCommandHandler(commandHandlerType, bank);
            case "deleteCard", "checkCardStatus", "payOnline", "cashWithdrawal" ->
                new CardCommandHandler(commandHandlerType, bank);
            default -> null;
        };
    }
}
