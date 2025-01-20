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
    private static final int MINIMUM_AGE_FOR_SAVINGS_WITHDRAW = 21;
    private static final int GOLD_FROM_SILVER_PLAN_UPGRADE_PRICE = 250;
    private static final int GOLD_FROM_STANDARD_PLAN_UPGRADE_PRICE = 350;
    private static final int SILVER_PLAN_UPGRADE_PRICE = 100;

    /**
     * Method used to implement visitor interface
     * @param bank Bard data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        return null;
    }

    /**
     * Withdraw user savings to classic account if possible, otherwise store given error
     * @param user User data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Bank bankData = handler.getBank();
        CommandInput commandData = handler.getCommand();

        Account senderAccount = bankData.searchAccount(commandData.getAccount());
        Account receiverAccount = searchFirstAccountByCurrency(commandData.getCurrency(), user);
        double amount;
        if (user.getAge() < MINIMUM_AGE_FOR_SAVINGS_WITHDRAW) {
            user.addTransaction(minimumAgeErrorJson(handler));
            return null;
        }

        if (receiverAccount == null) {
            user.addTransaction(noClassicAccountJson(handler));
            senderAccount.addTransaction(noClassicAccountJson(handler));
            return null;
        }


        if (senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            amount = commandData.getAmount();
        } else {
            amount = bankData.exchange(senderAccount.getCurrency(), receiverAccount.getCurrency(),
                    commandData.getAmount());
        }

        if (senderAccount.getBalance() < amount) {
            return null;
        }

        receiverAccount.setBalance(receiverAccount.getBalance() + commandData.getAmount());
        senderAccount.setBalance(senderAccount.getBalance() - amount);
        user.addTransaction(savingsWithdrawalSuccessJson(
                commandData,  senderAccount.getNumber(), receiverAccount.getNumber()));
        user.addTransaction(savingsWithdrawalSuccessJson(
                commandData,  senderAccount.getNumber(), receiverAccount.getNumber()));

        return null;
    }

    /**
     * Upgrades user plan if possible, otherwise store given error
     * @param account Account data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        CommandInput commandData = handler.getCommand();
        User user = handler.getBank().searchUserByAccount(commandData.getAccount());
        String upgradePlan = commandData.getNewPlanType();
        String currentPlan = user.getPlan();
        double exchangedPrice;

        if (currentPlan.equals(upgradePlan)) {
            user.addTransaction(userAlreadyHasPlanErrorJson(handler));
            account.addTransaction(userAlreadyHasPlanErrorJson(handler));
            return null;
        }

        if (currentPlan.equals("silver") && (upgradePlan.equals("student")
                || upgradePlan.equals("standard"))) {
            return null;
        }

        if (currentPlan.equals("gold") && (upgradePlan.equals("student")
                || upgradePlan.equals("standard") || upgradePlan.equals("silver"))) {
            return null;
        }

        if (upgradePlan.equals("gold") && (currentPlan.equals("standard")
                || currentPlan.equals("student"))) {
            exchangedPrice = handler.getBank().exchange(account.getCurrency(),
                    "RON", GOLD_FROM_STANDARD_PLAN_UPGRADE_PRICE);
            if (account.getBalance() >= exchangedPrice) {
                user.setPlan("gold");
                account.setBalance(account.getBalance() - exchangedPrice);
            } else {
                handler.addTransactions(user, account,
                        account.insufficientFundsJson(commandData.getTimestamp()));
                return null;
            }
        }

        if (upgradePlan.equals("gold") && currentPlan.equals("silver")) {
            exchangedPrice = handler.getBank().exchange(account.getCurrency(),
                    "RON", GOLD_FROM_SILVER_PLAN_UPGRADE_PRICE);
            if (account.getBalance() >= exchangedPrice) {
                user.setPlan("gold");
                account.setBalance(account.getBalance() - exchangedPrice);
            } else {
                handler.addTransactions(user, account,
                        account.insufficientFundsJson(commandData.getTimestamp()));
                return null;
            }
        }

        if (upgradePlan.equals("silver")) {
            exchangedPrice = handler.getBank().exchange(account.getCurrency(),
                    "RON", SILVER_PLAN_UPGRADE_PRICE);
            if (account.getBalance() >= exchangedPrice) {
                user.setPlan("silver");
                account.setBalance(account.getBalance() - exchangedPrice);
            } else {
                handler.addTransactions(user, account,
                        account.insufficientFundsJson(commandData.getTimestamp()));
                return null;
            }
        }

        account.addTransaction(handler.planUpgradeSuccessfulJson(
                upgradePlan, account.getNumber(), commandData.getTimestamp()));
        user.addTransaction(handler.planUpgradeSuccessfulJson(
                upgradePlan, account.getNumber(), commandData.getTimestamp()));
        return null;
    }

    /** Withdraws cash from account if possible, otherwise store given error
     * @param card Card data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Account account = handler.getBank().searchAccountByCard(card.getNumber());
        Bank bankData = handler.getBank();
        CommandInput commandData = handler.getCommand();
        double comissionPlusAmount;

        User user = handler.getBank().searchUserByAccount(account.getNumber());

        if (bankData.searchUser(commandData.getEmail()) == null) {
            return handler.userNotFoundJson();
        }

        comissionPlusAmount = bankData.exchange(account.getCurrency(), "RON",
                bankData.getCommission(account, commandData.getAmount()));

        if (!account.getOwner().getUserInformation().getEmail().equals(commandData.getEmail())) {
                return handler.cardNotFoundJson();
        }

        //Check if account has sufficient funds
        if (account.getBalance() < comissionPlusAmount) {
            handler.addTransactions(
                    user, account, account.insufficientFundsJson(commandData.getTimestamp()));
            return null;
        }

        account.setBalance(account.getBalance() - comissionPlusAmount);

        handler.addTransactions(
                user, account, cashWithdrawalSuccessfulJson(handler, commandData.getAmount()));

        if (card.isOneTime() && account.getBalance() <= 0) {
            account.generateNewOneTimeCard(card, handler);
        }

        return null;
    }

    /** Searches from the given user account list the first account with the given currency
     * @param currency Currency of account to be searched
     * @param user User data
     * @return The first account with the given currency, null if none is found
     */
    public Account searchFirstAccountByCurrency(final String currency, final User user) {
        for (Account account : user.getAccounts()) {
            if (account.getCurrency().equals(currency) && account.getType().equals("classic")) {
                return account;
            }
        }
        return null;
    }

    /** Generates Object Node storing successful plan upgrade transaction
     * @param handler Command data
     * @param amount Amount of cash withdrawn
     * @return Json format output for successful plan upgrade
     */
    public ObjectNode cashWithdrawalSuccessfulJson(
            final CommandHandler handler, final double amount) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("timestamp", handler.getCommand().getTimestamp());
        output.put("description", "Cash withdrawal of " + amount);
        output.put("amount", amount);
        return output;
    }

    /** Generates Object Node for failing to upgrade plan
     * @param handler Command data
     * @return Json format output for upgrade plan error
     */
    public ObjectNode userAlreadyHasPlanErrorJson(final CommandHandler handler) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "The user already has the "
                + handler.getCommand().getNewPlanType() + " plan.");
        output.put("timestamp", handler.getCommand().getTimestamp());
        return output;
    }

    /** Generates Object Node for successful cash withdrawal
     * @param command Command data
     * @param senderAccount Sender account data
     * @param receiverAccount Receiver account data
     * @return Json format output for successful cash withdrawal
     */
    public ObjectNode savingsWithdrawalSuccessJson(
            final CommandInput command, final String senderAccount, final String receiverAccount) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("amount", command.getAmount());
        output.put("classicAccountIBAN", receiverAccount);
        output.put("description", "Savings withdrawal");
        output.put("savingsAccountIBAN", senderAccount);
        output.put("timestamp", command.getTimestamp());
        return output;
    }

    /** Generates Object Node for failing to withdraw savings from account,
     * because user does not have the required minimum age
     * @param handler Command data
     * @return Json format output for cash withdrawal error
     */
    public ObjectNode minimumAgeErrorJson(final CommandHandler handler) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "You don't have the minimum age required.");
        output.put("timestamp", handler.getCommand().getTimestamp());
        return output;
    }

    /** Generates Object Node for failing to withdraw savings from account,
     * because user has no classic account
     * @param handler Command data
     * @return Json format output for cash withdrawal error
     */
    public ObjectNode noClassicAccountJson(final CommandHandler handler) {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("description", "You do not have a classic account.");
        json.put("timestamp", handler.getCommand().getTimestamp());
        return json;
    }

}
