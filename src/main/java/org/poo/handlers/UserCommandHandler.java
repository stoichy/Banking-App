package org.poo.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.main.User;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.fileio.CommandInput;
import org.poo.main.Bank;

public final class UserCommandHandler extends CommandHandler {

    public UserCommandHandler(final CommandInput command, final Bank bank) {
        super(command, bank);
    }

    /**
     * Calls accept method in user class if user is not null
     * @param visitor Corresponding visitor class for given command
     * @return Accept method call in user class
     */
    public ObjectNode runCommand(final CommandVisitorInterface visitor) {
        User user;
        if (getCommand().getEmail() != null) {
            user = getBank().searchUser(getCommand().getEmail());
            if (user == null) {
                return null;
            }
        } else {
            if (getBank().searchAccount(getCommand().getAccount()) == null) {
                return errorAccountNotFound();
            } else {
                user = getBank().searchUserByAccount(getCommand().getAccount());
            }
        }

        return user.accept(visitor, this);
    }
}
