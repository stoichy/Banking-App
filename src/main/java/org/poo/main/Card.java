package org.poo.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.handlers.CommandHandler;
import org.poo.visitors.CommandVisitorInterface;
import org.poo.utils.Utils;

@Getter
@Setter
public final class Card {
    private boolean oneTime;
    private boolean frozen;
    private String number;
    private Account account;

    private Card(final Builder builder) {
        setOneTime(builder.oneTime);
        setFrozen(builder.frozen);
        setNumber(builder.number);
        setAccount(builder.account);
    }

    public static class Builder {
        private boolean oneTime = false;
        private final boolean frozen;
        private final String number;
        private final Account account;


        public Builder(final Account account) {
            this.frozen = false;
            this.number = Utils.generateCardNumber();
            this.account = account;
        }


        public Builder oneTime () {
            this.oneTime = true;
            return this;
        }

        public Card build() {
            return new Card(this);
        }
    }

    /**
     * Calls visitor visit method for running command
     * @param visitor The visitor class that implements the command processing logic
     * @param cardCommandHandler The command handler corresponding class containing the command data
     * @return Result of the visit method call, encapsulated in an ObjectNode
     */
    public ObjectNode accept(final CommandVisitorInterface visitor,
                             final CommandHandler cardCommandHandler) {
        return visitor.visit(this, cardCommandHandler);
    }

    public boolean getFrozen() {
        return frozen;
    }

    /**
     * Converts card information to JSON format
     * @return ObjectNode containing card data in JSON format
     */
    public ObjectNode toJson() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("cardNumber", number);
        objectNode.put("status", frozen ? "frozen" : "active");
        return objectNode;
    }

    /**
     * Creates JSON object representing a payment transaction
     * @param amount Payment amount stored as a double integer
     * @param commerciant The name of the merchant involved in the transaction
     * @param handler Command data that provides payment
     *                context stored as command handler corresponding class
     * @return ObjectNode containing the payment details in JSON format
     */
    public ObjectNode paymentJson(final double amount,
                                  final String commerciant,
                                  final CommandHandler handler) {
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.put("timestamp", handler.getCommand().getTimestamp());
        result.put("description", "Card payment");
        result.put("amount", amount);
        result.put("commerciant", commerciant);
        return result;
    }

    private ObjectNode cardMutationTemplateJson(final CommandHandler handler,
                                                final String description) {
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.put("account", getAccount().getNumber());
        result.put("card", getNumber());
        result.put("cardHolder", getAccount().getOwner().getUserInformation().getEmail());
        result.put("description", description);
        result.put("timestamp", handler.getCommand().getTimestamp());
        return result;
    }

    /**
     * Creates JSON object representing deletion of card transaction
     * @param handler Command data for card deletion context stored
     *                as a command handler specific class
     * @return ObjectNode containing the card deletion details in JSON format
     */
    public ObjectNode cardDestroyedJson(final CommandHandler handler) {
        return cardMutationTemplateJson(handler, "The card has been destroyed");
    }

    /**
     * Creates JSON object representing creation of card transaction
     * @param handler Command data for card creation context stored
     *                as a command handler specific class
     * @return ObjectNode containing the card creation details in JSON format
     */
    public ObjectNode cardCreatedJson(final CommandHandler handler) {
        return cardMutationTemplateJson(handler, "New card created");
    }

    /**
     * Creates JSON object representing card frozen status
     * @param timestamp Timestamp of command execution
     * @return An ObjectNode containing the card frozen status
     */
    public ObjectNode isFrozenJson(final int timestamp) {
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.put("timestamp", timestamp);
        result.put("description", "The card is frozen");
        return result;
    }

    /**
     * Creates JSON object representing new card creation transaction
     * @param card Card class representing card data
     * @param timestamp Timestamp of command execution
     * @return ObjectNode containing the transaction details
     */
    public ObjectNode addCardTransactionJson(final Card card, final int timestamp) {
        ObjectNode transaction = new ObjectMapper().createObjectNode();
        transaction.put("timestamp", timestamp);
        transaction.put("description", "New card created");
        transaction.put("card", card.getNumber());
        transaction.put("cardHolder", getAccount().getOwner().getUserInformation().getEmail());
        transaction.put("account", account.getNumber());
        return transaction;
    }

    /**
     * Creates JSON object representing deletion of card
     * @param timestamp Timestamp of command execution
     * @return ObjectNode containing command details
     */
    public ObjectNode cardDeletedJson(final int timestamp) {
        ObjectNode transactionJson = new ObjectMapper().createObjectNode();
        transactionJson.put("timestamp", timestamp);
        transactionJson.put("description", "The card has been destroyed");
        transactionJson.put("card", getNumber());
        transactionJson.put("cardHolder", getAccount().getOwner().getUserInformation().getEmail());
        transactionJson.put("account", account.getNumber());

        return transactionJson;
    }

}
