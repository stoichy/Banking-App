package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.poo.main.User;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.fileio.CommandInput;
import org.poo.main.Account;
import org.poo.main.Bank;

@Setter
@Getter
public abstract class CommandHandler {
    protected CommandInput command;
    protected Bank bank;

    public CommandHandler(final CommandInput command, final Bank bank) {
        setCommand(command);
        setBank(bank);
    }

    /**
     * Abstract class that serves as an interface for
     * all handlers corresponding to each class in the bank hierarchy
     * @param visitor Corresponding visitor class for given command
     */
    public abstract ObjectNode runCommand(CommandVisitorInterface visitor);

    protected final ObjectNode errorAccountNotFound() {
        ObjectNode actionResult = getJsonWithHeader(getCommand());
        ObjectNode error = new ObjectMapper().createObjectNode();
        error.put("description", "Account not found");
        error.put("timestamp", getCommand().getTimestamp());
        actionResult.set("output", error);
        actionResult.put("timestamp", getCommand().getTimestamp());
        return actionResult;
    }

    /**
     * Header for each command output in Json format
     * @param commandData Command data for creating header
     * @return ObjectNode that contains command name in Json format
     */
    public final ObjectNode getJsonWithHeader(final CommandInput commandData) {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("command", commandData.getCommand());

        return objectNode;
    }

    /**
     * Adds transaction to given user and account
     * @param user Used to access transactions list of given user
     * @param account Used to access transactions list of given account
     * @param transaction Transaction that needs to be added in Json format
     */
    public final void addTransactions(final User user, final Account account,
                                final ObjectNode transaction) {
        user.addTransaction(transaction);
        account.addTransaction(transaction);
    }

    /**
     * Creates error corresponding to account state
     * @return ObjectNode that contains error in Json format
     */
    public final ObjectNode notSavingsAccountErrorJson() {
        ObjectNode error = getJsonWithHeader(getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "This is not a savings account");
        output.put("timestamp", getCommand().getTimestamp());
        error.set("output", output);
        error.put("timestamp", getBank().getTimestamp());
        return error;
    }

    public ObjectNode userNotFoundJson() {
        ObjectNode error = getJsonWithHeader(getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        error.set("output", output);
        output.put("timestamp", getCommand().getTimestamp());
        output.put("description", "User not found");
        error.put("timestamp", getCommand().getTimestamp());
        return error;
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

    public ObjectNode planUpgradeSuccessfulJson
            (String newPlan, String accountNumber, int timestamp) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "Upgrade plan");
        output.put("accountIBAN", accountNumber);
        output.put("newPlanType", newPlan);
        output.put("timestamp", timestamp);
        return output;
    }

}
