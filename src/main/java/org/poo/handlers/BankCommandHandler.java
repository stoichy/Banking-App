package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.fileio.CommandInput;
import org.poo.main.Bank;

public final class BankCommandHandler extends CommandHandler {

    public BankCommandHandler(final CommandInput command, final Bank bank) {
        super(command, bank);
    }

    /**
     * Calls accept method in bank class
     * @param visitor Corresponding visitor class for given command
     * @return Accept method call in bank class
     */
    public ObjectNode runCommand(final CommandVisitorInterface visitor) {
        return getBank().accept(visitor, this);
    }
}
