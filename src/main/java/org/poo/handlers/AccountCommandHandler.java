package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.fileio.CommandInput;
import org.poo.main.Account;
import org.poo.main.Bank;

public final class AccountCommandHandler extends CommandHandler {

    public AccountCommandHandler(final CommandInput command, final Bank bank) {
        super(command, bank);
    }

    /**
     * Calls accept method in account class if account is not null
     * @param visitor Corresponding visitor class for given command
     * @return Accept method call in account class
     */
    public ObjectNode runCommand(final CommandVisitorInterface visitor) {
        Account account = getBank().searchAccount(getCommand().getAccount());

        if (account == null) {
            return errorAccountNotFound();
        }

        return account.accept(visitor, this);
    }

}
