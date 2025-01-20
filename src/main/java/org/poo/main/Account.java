package org.poo.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.poo.fileio.CommandInput;
import org.poo.handlers.CommandHandler;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
public class Account {
    private double balance;
    private double minimumBalance;
    private double interestRate;
    private double cashbackForClothes;
    private double cashbackForFood;
    private double cashbackForTech;
    private double spendingThreshold;
    private double spendingLimit;
    private double depositLimit;
    private String number;
    private String currency;
    private String type;
    private String alias;
    private List<User> employees;
    private List<User> managers;
    private List<Card> cards;
    private List<Object[]> deposits;
    private List<Object[]> expenses;
    private ArrayNode transactions;
    private User owner;
    private ArrayList<Commerciant> commerciants;

    private Account(final Builder builder) {
        setNumber(builder.number);
        setCurrency(builder.currency);
        setType(builder.type);
        setBalance(builder.balance);
        setCards(builder.cards);
        setOwner(builder.owner);
        setTransactions(builder.transactions);
        setAlias(builder.alias);
        setMinimumBalance(builder.minimumBalance);
        setInterestRate(builder.interestRate);
        setDeposits(builder.deposits);
        setExpenses(builder.expenses);
        setCommerciants(builder.commerciants);
    }

    public static class Builder {
        private final String number;
        private final double balance;
        private final String currency;
        private final String alias;
        private final List<Card> cards;
        private final ArrayNode transactions;
        private final double minimumBalance;
        private final User owner;
        private final ArrayList<Commerciant> commerciants;
        private final List<Object[]> deposits;
        private final List<Object[]> expenses;
        private double interestRate = 0;
        private String type = "";

        public Builder(final CommandInput information, final User owner) {
            this.number = Utils.generateIBAN();
            this.balance = 0;
            this.currency = information.getCurrency();
            this.alias = "";
            this.cards = new ArrayList<>();
            this.transactions = new ObjectMapper().createArrayNode();
            this.minimumBalance = 0;
            this.owner = owner;
            this.deposits = new ArrayList<>();
            this.expenses = new ArrayList<>();
            this.commerciants = new ArrayList<>();
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public Builder interestRate(final double interestRate) {
            this.interestRate = interestRate;
            return this;
        }

        public Account build() {
            return new Account(this);
        }
    }

    /**
     * Accepts visitor class for running command and command handler class to store command data
     * @param visitor Visitor class that implements the command processing logic
     * @param handler The handler containing bank and command data
     * @return Result of the visit method call, encapsulated in an ObjectNode
     */
    public ObjectNode accept(final CommandVisitorInterface visitor, final CommandHandler handler) {
        return visitor.visit(this, handler);
    }

    /**
     * Adds card to account cards list
     *
     * @param card Card to be added
     */
    public void addCard(final Card card) {
        getCards().add(card);
    }

    /**
     * Adds transaction to account transactions list
     *
     * @param transaction Transaction to be added
     */
    public void addTransaction(final ObjectNode transaction) {
        getTransactions().add(transaction);
    }

    /**
     * Convert account details to Json format
     *
     * @return ObjectNode that encapsulates account data
     */
    public ObjectNode toJson() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("IBAN", getNumber());
        objectNode.put("balance", getBalance());
        objectNode.put("currency", getCurrency());
        objectNode.put("type", getType());
        objectNode.set("cards", cardsToJson());
        return objectNode;
    }

    /**
     * Converts all account's cards details to Json format
     *
     * @return ObjectNode that encapsulates cards data
     */
    public ArrayNode cardsToJson() {
        ArrayNode cardsArrayNode = new ObjectMapper().createArrayNode();
        for (Card card : getCards()) {
            cardsArrayNode.add(card.toJson());
        }
        return cardsArrayNode;
    }

    /**
     * Creates error in Json format for not having enough funds in account balance
     *
     * @param timestamp Command execution timestamp
     * @return ObjectNode that encapsulates error details
     */
    public ObjectNode insufficientFundsJson(final int timestamp) {
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.put("timestamp", timestamp);
        result.put("description", "Insufficient funds");
        return result;
    }

    /**
     * Account creation transaction in Json format
     *
     * @param timestamp Command execution timestamp
     * @return ObjectNode that encapsulates transaction details
     */
    public ObjectNode accountCreatedTransactionJson(final int timestamp) {
        ObjectNode transaction = new ObjectMapper().createObjectNode();
        transaction.put("timestamp", timestamp);
        transaction.put("description", "New account created");
        return transaction;
    }

    /**
     * Creates error for failing account deletion in Json format
     *
     * @param handler Command data
     * @return ObjectNode that encapsulates error details
     */
    public ObjectNode deleteErrorJson(final CommandHandler handler) {
        ObjectNode resultNode = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode successNode = new ObjectMapper().createObjectNode();
        successNode.put("error",
                "Account couldn't be deleted - see org.poo.transactions for details");
        successNode.put("timestamp", handler.getCommand().getTimestamp());
        resultNode.set("output", successNode);
        resultNode.put("timestamp", handler.getCommand().getTimestamp());

        return resultNode;
    }

    /**
     * Creates transaction for account deletion success in Json format
     *
     * @param handler Command data
     * @return ObjectNode that encapsulates transaction details
     */
    public ObjectNode deleteSuccessJson(final CommandHandler handler) {
        ObjectNode resultNode = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode successNode = new ObjectMapper().createObjectNode();
        successNode.put("success", "Account deleted");
        successNode.put("timestamp", handler.getCommand().getTimestamp());
        resultNode.set("output", successNode);
        resultNode.put("timestamp", handler.getCommand().getTimestamp());

        return resultNode;
    }

    /**
     * Creates error for failing account deletion for transaction output
     *
     * @param timestamp Command execution timestamp
     * @return ObjectNode that encapsulates transaction details
     */
    public ObjectNode deleteErrorTransactionJson(final int timestamp) {
        ObjectNode resultNode = new ObjectMapper().createObjectNode();
        resultNode.put("description",
                "Account couldn't be deleted - there are funds remaining");
        resultNode.put("timestamp", timestamp);
        return resultNode;
    }

    /**
     * Creates report for all account transfers, payments and errors in transactions
     *
     * @param handler Command data
     * @return ObjectNode that encapsulates all account transactions
     */
    public ObjectNode reportJson(final CommandHandler handler) {
        ObjectNode report = handler.getJsonWithHeader(handler.getCommand());

        ObjectNode outputJson = new ObjectMapper().createObjectNode();
        outputJson.put("IBAN", getNumber());
        outputJson.put("balance", getBalance());
        outputJson.put("currency", getCurrency());

        ArrayNode filteredTransactions = new ObjectMapper().createArrayNode();

        for (JsonNode transaction : getTransactions()) {
            int transactionTimestamp = transaction.get("timestamp").asInt();
            if (transactionTimestamp >= handler.getCommand().getStartTimestamp()
                    && transactionTimestamp <= handler.getCommand().getEndTimestamp()) {
                filteredTransactions.add(transaction);
            }
        }

        outputJson.set("transactions", filteredTransactions);
        report.set("output", outputJson);
        report.put("timestamp", handler.getCommand().getTimestamp());
        return report;
    }

    /**
     * Creates report for all account payments to commerciants
     *
     * @param handler Command data
     * @return ObjectNode that encapsulates all account payments
     */
    public ObjectNode spendingReportJson(final CommandHandler handler) {
        ObjectNode report = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode outputJson = new ObjectMapper().createObjectNode();
        outputJson.put("balance", getBalance());

        ArrayNode filteredTransactions = new ObjectMapper().createArrayNode();

        for (JsonNode transaction : getTransactions()) {
            int transactionTimestamp = transaction.get("timestamp").asInt();
            if (transactionTimestamp >= handler.getCommand().getStartTimestamp()
                    && transactionTimestamp <= handler.getCommand().getEndTimestamp()
                    && transaction.get("commerciant") != null) {
                filteredTransactions.add(transaction);
            }
        }

        Map<String, Double> merchantSpending = new HashMap<>();
        for (JsonNode transaction : filteredTransactions) {
            if (transaction.get("commerciant") != null) {
                String merchant = transaction.get("commerciant").asText();
                double amount = transaction.get("amount").asDouble();
                merchantSpending.put(
                        merchant,
                        merchantSpending.getOrDefault(merchant, 0.0) + amount);
            }
        }
        List<Map.Entry<String, Double>> sortedMerchants =
                new ArrayList<>(merchantSpending.entrySet());
        sortedMerchants.sort(Map.Entry.comparingByKey());


        ArrayNode commerciantsJson = new ObjectMapper().createArrayNode();
        for (Map.Entry<String, Double> entry : sortedMerchants) {
            ObjectNode merchantJson = new ObjectMapper().createObjectNode();
            merchantJson.put("commerciant", entry.getKey());
            merchantJson.put("total", entry.getValue());
            commerciantsJson.add(merchantJson);
        }

        outputJson.set("commerciants", commerciantsJson);
        outputJson.put("currency", getCurrency());
        outputJson.put("IBAN", getNumber());
        outputJson.set("transactions", filteredTransactions);
        report.set("output", outputJson);
        report.put("timestamp", handler.getCommand().getTimestamp());
        return report;
    }

    public void addEmployee(final User user) {
        getEmployees().add(user);
    }

    public void addManager(final User user) {
        getManagers().add(user);
    }

    public String getUserPosition(final String email) {
        if (getOwner().getUserInformation().getEmail().equals(email)) {
            return "owner";
        }
        for (User user : getEmployees()) {
            if (user.getUserInformation().getEmail().equals(email)) {
                return "employee";
            }
        }
        for (User user : getManagers()) {
            if (user.getUserInformation().getEmail().equals(email)) {
                return "manager";
            }
        }
        return "";
    }

    public Commerciant searchCommerciant(final String commerciantName) {
        for (Commerciant commerciant : getCommerciants()) {
            if (commerciant.getCommerciant().getCommerciant().equals(commerciantName)) {
                return commerciant;
            }
        }
        return null;
    }

    public boolean foundUserInBusinessAccount(final User user) {
        for (User manager : getManagers()) {
            if (manager.getUserInformation().getEmail().equals(user.getUserInformation().getEmail())) {
                return true;
            }
        }
        for (User employee : getEmployees()) {
            if (employee.getUserInformation().getEmail().equals(user.getUserInformation().getEmail())) {
                return true;
            }
        }
        return getOwner().getUserInformation().getEmail().equals(user.getUserInformation().getEmail());
    }

    public boolean hasAuthorityToDeleteCard(final User user, final Card card) {
        if (getType().equals("business")) {
            return foundUserInBusinessAccount(user);
        }
        //Classic account
        return getOwner().getUserInformation().getEmail().equals(user.getUserInformation().getEmail());
    }
}
