package org.poo.visitors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.User;
import org.poo.main.Card;

public final class AddCommandVisitor implements CommandVisitorInterface {

    /**
     * Adds interest to account based on current interest rate
     * @param bank Bank data for accessing account details
     * @param handler Command details for interest rate and account details
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account = bank.searchAccount(handler.getCommand().getAccount());

        if (!account.getType().equals("savings")) {
            return handler.notSavingsAccountErrorJson();
        }

        account.setBalance(account.getBalance()
                + handler.getCommand().getInterestRate() * account.getBalance());

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
            account = new Account.Builder(handler.getCommand(), user).
                    type("classic").build();
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

}
