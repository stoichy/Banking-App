package org.poo.visitors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

import java.util.ArrayList;
import java.util.List;

public final class PrintCommandVisitor implements CommandVisitorInterface {

    /**
     * Prints all users data
     * @param bank Bank data for accessing all users details
     * @param handler Command data
     * @return Users data in Json format encapsulated in ObjectNode
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        ObjectNode actionResult =
                handler.getJsonWithHeader(handler.getCommand());
        ArrayNode usersArrayNode = new ObjectMapper().createArrayNode();

        for (User user : bank.getUsers()) {
            usersArrayNode.add(user.toJson());
        }
        actionResult.set("output", usersArrayNode);
        actionResult.put("timestamp", handler.getCommand().getTimestamp());
        return actionResult;
    }

    /**
     * Prints user's transactions details
     * @param user User details
     * @param handler Command data
     * @return User's transactions details in Json format encapsulated in ObjectNode
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        ObjectNode actionResult = handler.getJsonWithHeader(handler.getCommand());
        List<JsonNode> jsonNodes = new ArrayList<>();
        user.getTransactions().forEach(jsonNodes::add);
        jsonNodes.sort((node1, node2) -> {
            JsonNode timestampNode1 = node1.get("timestamp");
            JsonNode timestampNode2 = node2.get("timestamp");

            // Handle null values for "timestamp"
            int timestamp1 = (timestampNode1 != null) ? timestampNode1.asInt() : Integer.MAX_VALUE;
            int timestamp2 = (timestampNode2 != null) ? timestampNode2.asInt() : Integer.MAX_VALUE;

            return Integer.compare(timestamp1, timestamp2);
        });
        ArrayNode sortedArrayNode = new ObjectMapper().createArrayNode();
        jsonNodes.forEach(sortedArrayNode::add);
        actionResult.set("output", sortedArrayNode);
        actionResult.put("timestamp", handler.getCommand().getTimestamp());
        return actionResult;
    }

    /**
     * Prints account's transactions details
     * @param account Account details
     * @param handler Command data
     * @return Account's transactions details or error in Json format encapsulated in ObjectNode
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        if (handler.getCommand().getCommand().equals("report")) {
            return account.reportJson(handler);
        } else {
            if (account.getType().equals("savings")) {
                return reportNotAvailableJson(handler);
            }
            return account.spendingReportJson(handler);
        }
    }

    /**
     * Handles account balance status
     * @param card Card details
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Account account = card.getAccount();
        User user = account.getOwner();

        if (card.getFrozen()) {
            return null;
        }

        if (account.getBalance() <= account.getMinimumBalance()) {
            card.setFrozen(true);
            handler.addTransactions(user, account, minimumAmountReachedJson(handler));
        }
        return null;
    }

    /**
     * Creates error regarding account report
     * @param handler Command data
     * @return Error details in Json format encapsulated in ObjectNode
     */
    public ObjectNode reportNotAvailableJson(final CommandHandler handler) {
        ObjectNode reportError = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("error",
                        "This kind of report is not supported for a saving account");
        reportError.set("output", output);
        reportError.put("timestamp", handler.getCommand().getTimestamp());
        return reportError;
    }

    /**
     * Creates card status change
     * @param handler Command data
     * @return Card status change in Json format encapsulated in ObjectNode
     */
    public ObjectNode minimumAmountReachedJson(final CommandHandler handler) {
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("timestamp", handler.getCommand().getTimestamp());
        output.put("description",
                "You have reached the minimum amount of funds, the card will be frozen");
        return output;
    }

}
