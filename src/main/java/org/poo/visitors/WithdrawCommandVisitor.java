package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

public final class WithdrawCommandVisitor implements CommandVisitorInterface {

    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        return null;
    }

    //Withdraw savings
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Bank bankData = handler.getBank();
        CommandInput commandData = handler.getCommand();

        Account senderAccount = bankData.searchAccount(commandData.getAccount());
        Account receiverAccount = searchFirstAccountByCurrency(commandData.getCurrency(), user);
        double amount;
        if (user.getAge() < 21) {
            user.addTransaction(minimumAgeErrorJson(handler));
            return null;
        }

        if (senderAccount == null) {
            //TODO account not found error
            return null;
        }

        if (receiverAccount == null) {
            user.addTransaction(noClassicAccountJson(handler));
            senderAccount.addTransaction(noClassicAccountJson(handler));
            return null;
        }

        if (!senderAccount.getType().equals("savings")) {
            //TODO account is not savings account error
            return null;
        }
        if (senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            amount = commandData.getAmount();
        } else {
            amount = bankData.exchange(senderAccount.getCurrency(), receiverAccount.getCurrency(),
                    commandData.getAmount());
        }

        if (senderAccount.getBalance() < amount) {
            //TODO funds not sufficient error
            return null;
        }

        receiverAccount.setBalance(receiverAccount.getBalance() + commandData.getAmount());
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        user.addTransaction(savingsWithdrawalSuccessJson
                (commandData,  senderAccount.getNumber(), receiverAccount.getNumber()));
        user.addTransaction(savingsWithdrawalSuccessJson
                (commandData,  senderAccount.getNumber(), receiverAccount.getNumber()));

        return null;
    }

    //Upgrade plan
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        User user = handler.getBank().searchUserByAccount(handler.getCommand().getAccount());
        String upgradePlan = handler.getCommand().getNewPlanType();

        if (user.getPlan().equals(upgradePlan)) {
            user.addTransaction(userAlreadyHasPlanErrorJson(handler));
            account.addTransaction(userAlreadyHasPlanErrorJson(handler));
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
                    account.setBalance(account.getBalance() - amountToDeduct);
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
                    account.setBalance(account.getBalance() - amountToDeduct);
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
                    account.addTransaction(handler.planUpgradeSuccessfulJson(upgradePlan, account.getNumber(), handler.getCommand().getTimestamp()));
                    user.setPlan("silver");
                    account.setBalance(account.getBalance() - 100);
                } else {
                    handler.addTransactions(user, account, account.insufficientFundsJson(handler.getCommand().getTimestamp()));
                    return null;
                }
            } else {
                if (account.getBalance() >= handler.getBank().exchange(account.getCurrency(), "RON", 100)) {
                    user.setPlan("silver");
                    double amountToDeduct = handler.getBank().exchange(account.getCurrency(), "RON", 100);
                    account.setBalance(account.getBalance() - amountToDeduct);
                } else {
                    handler.addTransactions(user, account, account.insufficientFundsJson(handler.getCommand().getTimestamp()));
                    return null;
                }
            }
        }

        user.addTransaction(handler.planUpgradeSuccessfulJson(upgradePlan, account.getNumber(), handler.getCommand().getTimestamp()));
        return null;
    }

    //Cash withdrawal
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Account account = handler.getBank().searchAccountByCard(card.getNumber());
        Bank bankData = handler.getBank();
        CommandInput commandData = handler.getCommand();
        double comissionPlusAmount;

        User user = handler.getBank().searchUserByAccount(account.getNumber());

        if (bankData.searchUser(handler.getCommand().getEmail()) == null) {
            return handler.userNotFoundJson();
        }

        if (card.getFrozen()) {
            handler.addTransactions(user, account, card.isFrozenJson(commandData.getTimestamp()));
            return null;
        }

        if (account.getCurrency().equals("RON")) {
            comissionPlusAmount = bankData.getCommission(account, commandData.getAmount());
        } else {
            comissionPlusAmount = bankData.exchange(account.getCurrency(),
                    "RON", bankData.getCommission(account, commandData.getAmount()));
        }

        if (account.getType().equals("business")) {
            String email = user.getUserInformation().getEmail();
            if (account.getUserPosition(email).isEmpty()) {
                return handler.cardNotFoundJson();
            }
            if (account.getUserPosition(email).equals("employee")) {
                if (comissionPlusAmount > account.getSpendingLimit()) {
                    return null;
                }
            }
            if (!account.getUserPosition(email).equals("owner")) {
                account.getExpenses().add
                        (new Object[]{email, handler.getCommand().getAmount(),
                                commandData.getCommerciant()});
            }
        } else {
            if (!account.getOwner().getUserInformation().getEmail().equals(handler.getCommand().getEmail())) {
                return handler.cardNotFoundJson();
            }
        }

        //Check if account has sufficient funds
        if (account.getBalance() < comissionPlusAmount) {
            handler.addTransactions(user, account, account.insufficientFundsJson(commandData.getTimestamp()));
            return null;
        }

//        if (commandData.getTimestamp() >= 284 && commandData.getTimestamp() <= 293) {
//            System.out.println("Timestamp: " + commandData.getTimestamp() +
//                    account.getType() + " " + account.getNumber());
//        }
        account.setBalance(account.getBalance() - comissionPlusAmount);

        handler.addTransactions(user, account, cashWithdrawalSuccessfulJson(handler, commandData.getAmount()));

        if (card.isOneTime() && account.getBalance() <= 0) {
            Card newCard = new Card.Builder(account).oneTime().build();
            account.addCard(newCard);

            handler.addTransactions(user, account, card.cardDestroyedJson(handler));
            handler.addTransactions(user, account, newCard.cardCreatedJson(handler));
            account.getCards().remove(card);
        }

        return null;
    }

    public Account searchFirstAccountByCurrency(final String currency, final User user) {
        for (Account account : user.getAccounts()) {
            if (account.getCurrency().equals(currency) && account.getType().equals("classic")) {
                return account;
            }
        }
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

    public ObjectNode userAlreadyHasPlanErrorJson (final CommandHandler handler) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "The user already has the " + handler.getCommand().getNewPlanType() + " plan.");
        output.put("timestamp", handler.getCommand().getTimestamp());
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

    public ObjectNode minimumAgeErrorJson(final CommandHandler handler) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "You don't have the minimum age required.");
        output.put("timestamp", handler.getCommand().getTimestamp());
        return output;
    }

    public ObjectNode noClassicAccountJson(final CommandHandler handler) {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("description", "You do not have a classic account.");
        json.put("timestamp", handler.getCommand().getTimestamp());
        return json;
    }

}
