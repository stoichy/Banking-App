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
    private final UserInput userInformation;
    private List<Account> accounts;
    private ArrayNode transactions;
    private String plan;
    private int transactionsOver300RON;

    private User(final User.Builder builder) {
        this.userInformation = builder.userInformation;
        this.accounts = builder.accounts;
        this.transactions = builder.transactions;
        this.plan = builder.plan;
        this.transactionsOver300RON = builder.transactionsOver300RON;
    }

    public static final class Builder {
        private final UserInput userInformation;
        private final List<Account> accounts;
        private final ArrayNode transactions;
        private final int transactionsOver300RON;
        private String plan;

        public Builder(final UserInput userInformation) {
            this.userInformation = userInformation;
            this.accounts = new ArrayList<>();
            this.transactions = new ObjectMapper().createArrayNode();
            this.transactionsOver300RON = 0;
        }

        /**
         * Sets initial plan of user and returns class build
         * @param userPlan Plan of user
         * @return All Builder variables
         */
        public User.Builder plan(final String userPlan) {
            this.plan = userPlan;
            return this;
        }

        /**
         * Creates new User class instance with given parameters
         * @return New User instance
         */
        public User build() {
            return new User(this);
        }
    }

    /**
     * Returns age of user based on his birthdate from its information field
     * @return Age of user as an integer
     */
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

    public ArrayNode getCopyTransactions() {
        return transactions.deepCopy();
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
