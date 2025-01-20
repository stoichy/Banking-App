package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.fileio.CommerciantInput;
import org.poo.handlers.CommandHandler;
import org.poo.main.Bank;
import org.poo.main.User;
import org.poo.main.Account;
import org.poo.main.Card;
import org.poo.main.Commerciant;

import java.util.ArrayList;

public final class AddCommandVisitor implements CommandVisitorInterface {
    private static final double DEFAULT_BUSINESS_ACCOUNT_LIMIT = 500;
    /**
     * Adds interest to account based on current interest rate
     * @param bank Bank data for accessing account details
     * @param handler Command details for interest rate and account details
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account = bank.searchAccount(handler.getCommand().getAccount());
        User user = bank.searchUserByAccount(handler.getCommand().getAccount());

        if (account != null && !account.getType().equals("savings")) {
            return handler.notSavingsAccountErrorJson();
        }

        double amount = account.getInterestRate() * account.getBalance();
        account.setBalance(account.getBalance() + amount);

        handler.addTransactions(user, account, interestIncomeJson(handler, account, amount));

        return null;
    }

    /**
     * Creates a new account for given user
     * @param user User details
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Account account;
        if (handler.getCommand().getAccountType().equals("savings")) {
            account = new Account.Builder(handler.getCommand(), user).
                    type("savings").interestRate(handler.getCommand().getInterestRate()).build();
        } else {
            if (handler.getCommand().getAccountType().equals("classic")) {
                account = new Account.Builder(handler.getCommand(), user).
                        type("classic").build();
            } else {
                account = new Account.Builder(handler.getCommand(), user).
                        type("business").build();
                account.setEmployees(new ArrayList<>());
                account.setManagers(new ArrayList<>());
                double limit;
                if (!account.getCurrency().equals("RON")) {
                    limit = handler.getBank().exchange(account.getCurrency(),
                            "RON", DEFAULT_BUSINESS_ACCOUNT_LIMIT);
                } else {
                    limit = DEFAULT_BUSINESS_ACCOUNT_LIMIT;
                }
                account.setDepositLimit(limit);
                account.setSpendingLimit(limit);
            }
        }

        for (CommerciantInput commerciantInput : handler.getBank().getInput().getCommerciants()) {
            Commerciant commerciant = new Commerciant();
            commerciant.setCommerciant(commerciantInput);
            commerciant.setTransactions(0);
            account.getCommerciants().add(commerciant);
        }
        user.addAccount(account);
        handler.addTransactions(user, account,
                account.accountCreatedTransactionJson(handler.getCommand().getTimestamp()));

        return null;
    }

    /**
     * Adds fixed amount of funds to account balance
     * @param account Account details
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        CommandInput commandData = handler.getCommand();
        if (account.getType().equals("business")) {
            String userPosition = account.getUserPosition(commandData.getEmail());
            if (userPosition.isEmpty()) {
                return null;
            }
            if (userPosition.equals("employee")
                    && commandData.getAmount() > account.getDepositLimit()) {
                return null;
            }
            if (!userPosition.equals("owner")) {
                account.getDeposits().add(new Object[]{commandData.getEmail(),
                        commandData.getAmount()});
            }
        }
        account.setBalance(account.getBalance() + commandData.getAmount());

        return null;
    }

    /**
     * Method used for implementing visitor interface
     * @param card Card data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        return null;
    }

    /**
     * Creates a JSON format transaction for adding interest to account
     * @param handler Command data
     * @param account Account details
     * @param amount Amount of interest to be added
     * @return ObjectNode output that represents the transaction
     */
    public ObjectNode interestIncomeJson(
            final CommandHandler handler, final Account account, final double amount) {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("amount", amount);
        json.put("currency", account.getCurrency());
        json.put("description", "Interest rate income");
        json.put("timestamp", handler.getCommand().getTimestamp());
        return json;
    }

}
