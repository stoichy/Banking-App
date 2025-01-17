package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.handlers.CommandHandler;
import org.poo.main.Bank;
import org.poo.main.Account;
import org.poo.main.User;
import org.poo.main.Card;

public final class SetterCommandVisitor implements CommandVisitorInterface {

    /**
     * Changes interest rate of account
     * @param bank Account details
     * @param handler Command data
     * @return Error in Json format encapsulated in ObjectNode
     * if account has wrong type, null otherwise
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account = bank.searchAccount(handler.getCommand().getAccount());
        User user = account.getOwner();

        if (account.getType().equals("savings")) {
            account.setInterestRate(handler.getCommand().getInterestRate());
        } else {
            return handler.notSavingsAccountErrorJson();
        }

        handler.addTransactions(user, account, changedInterestTransactionJson(handler.getCommand()));

        return null;
    }

    /**
     * Sets account minimum balance limit
     * @param user Parameter used to implement visitor interface
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Account account = handler.getBank().searchAccount(handler.getCommand().getAccount());

        account.setMinimumBalance(handler.getCommand().getAmount());
        return null;
    }

    /**
     * Sets account alias
     * @param account Parameter used to implement visitor interface
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        account.setAlias(handler.getCommand().getAlias());
        return null;
    }

    /**
     * Method used to implement visitor interface
     * @param card Card details
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        return null; }

    /**
     * Prints change of interest rate
     * @param command Command data
     * @return Transaction details in Json format encapsulated in ObjectNode
     */
    public ObjectNode changedInterestTransactionJson(final CommandInput command) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "Interest rate of the account changed to " + command.getInterestRate());
        output.put("timestamp", command.getTimestamp());
        return output;
    }
}
