package org.poo.visitors;

public class CommandVisitorFactory {
    private static CommandVisitorFactory instance = null;

    public static CommandVisitorFactory getInstance() {
        if (instance == null) {
            instance = new CommandVisitorFactory();
        }
        return instance;
    }

    public CommandVisitorInterface getVisitor(String accountType) {
        return switch (accountType) {
            case "addAccount", "addFunds", "addInterest" -> new AddCommandVisitor();
            case "printUsers", "printTransactions", "checkCardStatus", "report", "spendingsReport"
                    -> new PrintCommandVisitor();
            case "createCard", "createOneTimeCard" -> new CreateCommandVisitor();
            case "deleteAccount", "deleteCard" -> new DeleteCommandVisitor();
            case "payOnline", "sendMoney", "splitPayment" -> new PaymentCommandVisitor();
            case "setAlias", "setMinBalance", "changeInterestRate" -> new SetterCommandVisitor();
            case "withdrawSavings", "upgradePlan", "cashWithdrawal" -> new WithdrawCommandVisitor();
            default -> null;
        };
    }
}
