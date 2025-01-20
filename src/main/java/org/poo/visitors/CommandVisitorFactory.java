package org.poo.visitors;

public final class CommandVisitorFactory {
    private static CommandVisitorFactory instance = null;

    /**
     * Singleton pattern implementation for the usage of only one instance of class
     */
    public static CommandVisitorFactory getInstance() {
        if (instance == null) {
            instance = new CommandVisitorFactory();
        }
        return instance;
    }

    /**
     * Gets the required visitor based on the given command
     * @param commandName Name of command
     * @return Required visitor for running given command
     */
    public CommandVisitorInterface getVisitor(final String commandName) {
        return switch (commandName) {
            case "addAccount", "addFunds", "addInterest" -> new AddCommandVisitor();
            case "printUsers", "printTransactions", "checkCardStatus", "report", "spendingsReport"
                    -> new PrintCommandVisitor();
            case "createCard", "createOneTimeCard" -> new CreateCommandVisitor();
            case "deleteAccount", "deleteCard" -> new DeleteCommandVisitor();
            case "payOnline", "sendMoney", "splitPayment", "acceptSplitPayment",
                 "rejectSplitPayment" -> new PaymentCommandVisitor();
            case "setAlias", "setMinBalance", "changeInterestRate" -> new SetterCommandVisitor();
            case "withdrawSavings", "upgradePlan", "cashWithdrawal" -> new WithdrawCommandVisitor();
            case "addNewBusinessAssociate", "businessReport", "changeSpendingLimit",
                 "changeDepositLimit"  -> new BusinessCommandVisitor();
            default -> null;
        };
    }
}
