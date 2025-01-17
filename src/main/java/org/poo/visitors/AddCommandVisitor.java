package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.User;
import org.poo.main.Card;

import java.util.ArrayList;

public final class AddCommandVisitor implements CommandVisitorInterface {

    /**
     * Adds interest to account based on current interest rate
     * @param bank Bank data for accessing account details
     * @param handler Command details for interest rate and account details
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account = bank.searchAccount(handler.getCommand().getAccount());
        User user = bank.searchUserByAccount(handler.getCommand().getAccount());

        if (!account.getType().equals("savings")) {
            return handler.notSavingsAccountErrorJson();
        }

        double amount = account.getInterestRate() * account.getBalance();
        account.setBalance(account.getBalance()
                + amount);

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
                            "RON", 500);
                } else {
                    limit = 500;
                }
                account.setDepositLimit(limit);
                account.setSpendingLimit(limit);
            }
        }

        user.addAccount(account);
        handler.addTransactions(user,
                account,
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
        if (account.getType().equals("business")) {
            String userPosition = account.getUserPosition(handler.getCommand().getEmail());
            if (userPosition.equals("employee") && handler.getCommand().getAmount() > account.getDepositLimit()) {
                return null;
            }
            if (!userPosition.equals("owner")) {
                account.getDeposits().add(new Object[]{handler.getCommand().getEmail(),
                        handler.getCommand().getAmount()});
            }
        }
        account.setBalance(account.getBalance() + handler.getCommand().getAmount());

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

    public ObjectNode interestIncomeJson
            (final CommandHandler handler, final Account account, final double amount) {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("amount", amount);
        json.put("currency", account.getCurrency());
        json.put("description", "Interest rate income");
        json.put("timestamp", handler.getCommand().getTimestamp());
        return json;
    }

}
