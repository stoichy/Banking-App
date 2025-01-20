package org.poo.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.fileio.UserInput;
import org.poo.handlers.CommandHandler;
import org.poo.visitors.CommandVisitorInterface;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public final class User {
    private UserInput userInformation;
    private List<Account> accounts;
    private ArrayNode transactions;
    private String plan;
    private int transactionsOver300RON;

    public User(final UserInput userInformation) {
        setUserInformation(userInformation);
        setAccounts(new ArrayList<>());
        setTransactions(new ObjectMapper().createArrayNode());
        if (userInformation.getOccupation().equals("student")) {
            setPlan("student");
        } else {
            setPlan("standard");
        }
    }

    public int getAge() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate birthDate = LocalDate.parse(getUserInformation().getBirthDate(), formatter);
        LocalDate today = LocalDate.now();
        return Period.between(birthDate, today).getYears();
    }

    /**
     * Accepts a visitor and a command handler to process the current command
     * @param visitor The visitor class that implements the command processing logic
     * @param userCommandHandler The handler containing the command data
     * @return The result of the visit method call, encapsulated in an ObjectNode
     */
    public ObjectNode accept(final CommandVisitorInterface visitor,
                             final CommandHandler userCommandHandler) {
        return visitor.visit(this, userCommandHandler);
    }

    /**
     * Adds transaction to user's list of transaction
     * @param transaction ObjectNode that contains transaction information in Json format
     */
    public void addTransaction(final ObjectNode transaction) {
        getTransactions().add(transaction);
    }

    /**
     * Converts user information to JSON format.
     * @return An ObjectNode containing user data in JSON format.
     */
    public ObjectNode toJson() {

        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("firstName", this.getUserInformation().getFirstName());
        objectNode.put("lastName", this.getUserInformation().getLastName());
        objectNode.put("email", this.getUserInformation().getEmail());
        objectNode.set("accounts", accountsToJson());
        return objectNode;
    }

    /**
     * Converts user's accounts to JSON format.
     *
     * @return An ArrayNode containing user's accounts in JSON format.
     */
    public ArrayNode accountsToJson() {
        ArrayNode accountsArrayNode = new ObjectMapper().createArrayNode();
        for (Account account : getAccounts()) {
            accountsArrayNode.add(account.toJson());
        }
        return accountsArrayNode;
    }

    /**
     * Adds a new account to the user's account list.
     * @param account The account to be added to the list.
     */
    public void addAccount(final Account account) {
        this.accounts.add(account);
    }
}
