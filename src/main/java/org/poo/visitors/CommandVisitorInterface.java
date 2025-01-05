package org.poo.visitors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

public interface CommandVisitorInterface {
    /**
     * Abstract method for handling commands that require bank data
     * @param bank Bank data
     * @param bankCommandHandler Command data
     */
    ObjectNode visit(Bank bank, CommandHandler bankCommandHandler);
    /**
     * Abstract method for handling commands that require user data
     * @param user User data
     * @param userCommandHandler Command data
     */
    ObjectNode visit(User user, CommandHandler userCommandHandler);
    /**
     * Abstract method for handling commands that require account data
     * @param account Account data
     * @param accountCommandHandler Command data
     */
    ObjectNode visit(Account account, CommandHandler accountCommandHandler);
    /**
     * Abstract method for handling commands that require card data
     * @param card Card data
     * @param cardCommandHandler Command data
     */
    ObjectNode visit(Card card, CommandHandler cardCommandHandler);
}
