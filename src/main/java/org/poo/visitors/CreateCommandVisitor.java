package org.poo.visitors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

public final class CreateCommandVisitor implements CommandVisitorInterface {

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
     * Creates card of given type
     * @param account Account where card should be created
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        User accountOwner = handler.getBank().searchUserByAccount(account.getNumber());

        //Given user doesn't own account
        if (!handler.getCommand().getEmail().equals(accountOwner.getUserInformation().getEmail())) {
            return null;
        }

        Card newCard;

        if (handler.getCommand().getCommand().equals("createOneTimeCard")) {
            newCard = new Card.Builder(account).oneTime().build();
        } else {
            newCard = new Card.Builder(account).build();
        }

        account.addCard(newCard);

        handler.addTransactions(accountOwner, account,
                newCard.addCardTransactionJson(newCard, handler.getCommand().getTimestamp()));

        return null;
    }

    /**
     * Method used to implement visitor interface
     * @param card Card data
     * @param handler Command data
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        return null;
    }

}
