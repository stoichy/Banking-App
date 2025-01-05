package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class WithdrawCommandVisitor implements CommandVisitorInterface {

    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        return null;
    }

    //Withdraw savings
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Account senderAccount = handler.getBank().searchAccount(handler.getCommand().getAccount());
        Account receiverAccount =
                searchFirstAccountByCurrency(handler.getCommand().getCurrency(), user);
        double amount;

        if (user.getAge() < 21) {
            user.addTransaction(minimumAgeErrorJson(handler));
            return null;
        }

        if (receiverAccount== null) {
            //TODO no classic account found error
            return null;
        }

        if (senderAccount == null) {
            //TODO account not found error
            return null;
        }

        amount = handler.getBank().exchange
                        (receiverAccount.getCurrency(),
                        senderAccount.getCurrency(),
                        handler.getCommand().getAmount());

        if (!senderAccount.getType().equals("savings")) {
            //TODO account is not savings account error
            return null;
        }
        if (senderAccount.getBalance() <
                handler.getBank().exchange
                                (receiverAccount.getCurrency(),
                                senderAccount.getCurrency(),
                                handler.getCommand().getAmount())) {
            //TODO funds not sufficient error
            return null;
        }

        receiverAccount.setBalance(receiverAccount.getBalance() - handler.getCommand().getAmount());
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        user.addTransaction(savingsWithdrawalSuccessJson(handler.getCommand(),  senderAccount.getNumber(), receiverAccount.getNumber()));

        return null;
    }

    //Upgrade plan
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        User user = handler.getBank().searchUserByAccount(handler.getCommand().getAccount());
        String upgradePlan = handler.getCommand().getNewPlanType();

        if (user.getPlan().equals(upgradePlan)) {
            //TODO user already has plan error
            return null;
        }

        if (user.getPlan().equals("silver") &&
                (upgradePlan.equals("student") || upgradePlan.equals("standard"))) {
            //TODO can't downgrade plan error
            return null;
        }

        if (user.getPlan().equals("gold") &&
                (upgradePlan.equals("student") ||
                upgradePlan.equals("standard") ||
                upgradePlan.equals("silver"))) {
            //TODO can t downgrade plan error
            return null;
        }

        if (handler.getCommand().getNewPlanType().equals("gold") &&
                (user.getPlan().equals("standard") || user.getPlan().equals("student"))) {
            if (account.getCurrency().equals("RON")) {
                if (account.getBalance() >= 350) {
                    //TODO add transaction to user and take fee from account balance
                    user.setPlan("gold");
                    account.setBalance(account.getBalance() - 350);
                } else {
                    //TODO insufficient funds error
                    return null;
                }
            } else {
                if (account.getBalance() >= handler.getBank().exchange(account.getCurrency(), "RON", 350)) {
                    user.setPlan("gold");
                    double amountToDeduct = handler.getBank().exchange(account.getCurrency(), "RON", 350);
                    BigDecimal roundedAmount = new BigDecimal(amountToDeduct).setScale(2, RoundingMode.HALF_UP);
                    account.setBalance(account.getBalance() - roundedAmount.doubleValue());
                } else {
                    //TODO insufficient funds error
                    return null;
                }
            }
        }

        if (handler.getCommand().getNewPlanType().equals("gold") && user.getPlan().equals("silver")) {
            if (account.getCurrency().equals("RON")) {
                if (account.getBalance() >= 250) {
                    //TODO add transaction to user and take fee from account balance
                    user.setPlan("gold");
                    account.setBalance(account.getBalance() - 250);
                } else {
                    //TODO insufficient funds error
                    return null;
                }
            } else {
                if (account.getBalance() >= handler.getBank().exchange(account.getCurrency(), "RON", 250)) {
                    user.setPlan("gold");
                    double amountToDeduct = handler.getBank().exchange(account.getCurrency(), "RON", 250);
                    BigDecimal roundedAmount = new BigDecimal(amountToDeduct).setScale(2, RoundingMode.HALF_UP);
                    account.setBalance(account.getBalance() - roundedAmount.doubleValue());
                } else {
                    //TODO insufficient funds error
                    return null;
                }
            }
        }

        //Insufficient funds error
        if (handler.getCommand().getNewPlanType().equals("silver")) {
            if (account.getCurrency().equals("RON")) {
                if (account.getBalance() >= 100) {
                    //TODO add transaction to user and take fee from account balance
                    user.setPlan("silver");
                    account.setBalance(account.getBalance() - 100);
                } else {
                    //TODO insufficient funds error
                    return null;
                }
            } else {
                if (account.getBalance() >= handler.getBank().exchange(account.getCurrency(), "RON", 100)) {
                    user.setPlan("silver");
                    double amountToDeduct = handler.getBank().exchange(account.getCurrency(), "RON", 100);
                    BigDecimal roundedAmount = new BigDecimal(amountToDeduct).setScale(2, RoundingMode.HALF_UP);
                    account.setBalance(account.getBalance() - roundedAmount.doubleValue());
                } else {
                    //TODO insufficient funds error
                    return null;
                }
            }
        }

        user.addTransaction(planUpgradeSuccessfulJson(upgradePlan, account.getNumber(), handler.getCommand().getTimestamp()));
        return null;
    }

    //Cash withdrawal
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Account account = handler.getBank().searchAccountByCard(card.getNumber());
        double amount;

        if (account == null) {
            //TODO account not found json error
            return null;
        }

        User user = handler.getBank().searchUserByAccount(account.getNumber());

        if (user == null) {
            return handler.userNotFoundJson();
        }

        if (account.getCurrency().equals("RON")) {
            amount = handler.getBank().commission(user, handler.getCommand().getAmount());
        } else {
            amount = handler.getBank().commission(user, handler.getBank().exchange
                            (account.getCurrency(),
                            "RON",
                            handler.getCommand().getAmount()));
        }

        //Check if account has sufficient funds
        if (account.getBalance() < handler.getCommand().getAmount()) {
            return null;
        }

        account.setBalance(account.getBalance() - amount);
        user.addTransaction
                (cashWithdrawalSuccessfulJson(handler, handler.getCommand().getAmount()));
        account.addTransaction
                (cashWithdrawalSuccessfulJson(handler, handler.getCommand().getAmount()));
        return null;
    }

    public Account searchFirstAccountByCurrency(final String currency, final User user) {
        return null;
    }

    public ObjectNode cashWithdrawalSuccessfulJson
            (final CommandHandler handler, final double amount) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("timestamp", handler.getCommand().getTimestamp());
        output.put("description", "Cash withdrawal of " + amount);
        output.put("amount", amount);
        return output;
    }

    public ObjectNode planUpgradeSuccessfulJson
            (String newPlan, String accountNumber, int timestamp) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "Upgrade plan");
        output.put("accountIBAN", accountNumber);
        output.put("newPlanType", newPlan);
        output.put("timestamp", timestamp);
        return output;
    }

    public ObjectNode savingsWithdrawalSuccessJson(final CommandInput command, final String senderAccount, final String receiverAccount) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("amount", command.getAmount());
        output.put("classicAccountIBAN", receiverAccount);
        output.put("description", "Savings withdrawal");
        output.put("savingsAccountIBAN", senderAccount);
        output.put("timestamp", command.getTimestamp());
        return output;
    }

    public ObjectNode minimumAgeErrorJson(CommandHandler handler) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "You don't have the minimum age required.");
        output.put("timestamp", handler.getCommand().getTimestamp());
        return output;
    }

}
