package org.poo.visitors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

public final class DeleteCommandVisitor implements CommandVisitorInterface {

    /**
     * Method used to implement visitor interface
     * @param bank Bank data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        return null;
    }

    /**
     * Method used to implement visitor interface
     * @param user User data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        return null;
    }

    /**
     * Deletes account from bank data
     * @param account Account details
     * @param handler Command data
     * @return Error details in Json format if account balance is not 0, null otherwise
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {

        //Json output
        if (account.getBalance() != 0) {
            handler.addTransactions(account.getOwner(),
                    account,
                    account.deleteErrorTransactionJson(handler.getCommand().getTimestamp()));

            return account.deleteErrorJson(handler);
        }

        ObjectNode output = account.deleteSuccessJson(handler);
        account.getOwner().getAccounts().remove(account);
        return output;
    }

    /**
     * Deletes card from bank data
     * @param card Card details
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Account account = card.getAccount();
        User user = account.getOwner();

        if (account.hasAuthorityToDeleteCard
                (handler.getBank().searchUser(handler.getCommand().getEmail()), card)) {

            if (account.getBalance() == 0) {
                account.getCards().remove(card);
                handler.addTransactions(user,
                        account,
                        card.cardDeletedJson(handler.getCommand().getTimestamp()));
            }
        }

        return null;
    }


}
