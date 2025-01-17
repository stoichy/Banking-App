package org.poo.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.fileio.CommandInput;
import org.poo.main.Bank;
import org.poo.main.Card;

public final class CardCommandHandler extends CommandHandler {

    public CardCommandHandler(final CommandInput command, final Bank bank) {
        super(command, bank);
    }

    /**
     * Calls accept method in card class if card is not null
     * @param visitor Corresponding visitor class for given command
     * @return Accept method call in card class
     */
    public ObjectNode runCommand(final CommandVisitorInterface visitor) {
        Card card = getBank().searchCard(getCommand().getCardNumber());
        if (card == null && !getCommand().getCommand().equals("deleteCard")) {
            return cardNotFoundJson();
        }
        if (card == null) {
            return null;
        }

        return card.accept(visitor, this);
    }

    /**
     * Creates error regarding card search in Json format
     * @return ObjectNode with command parameters and error output
     */
    public ObjectNode cardNotFoundJson() {
        ObjectNode error = getJsonWithHeader(getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        error.set("output", output);
        output.put("timestamp", getCommand().getTimestamp());
        output.put("description", "Card not found");
        error.put("timestamp", getCommand().getTimestamp());
        return error;
    }

}
