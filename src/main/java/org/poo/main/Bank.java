package org.poo.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.fileio.*;
import org.poo.handlers.*;
import org.poo.visitors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Setter
@Getter
public final class Bank {
    private static Bank instance = null;
    private ObjectInput input;
    private int timestamp;
    private List<User> users;

    private Bank(final ObjectInput input) {
        setInput(input);
        setTimestamp(0);
        setUsers(new ArrayList<>());
        for (UserInput userInput : getInput().getUsers()) {
            User user = new User(userInput);
            addUser(user);
            if (user.getUserInformation().getOccupation().equals("student")) {
                user.setPlan("student");
            } else {
                user.setPlan("standard");
            }
        }
    }

    public static Bank getInstance(final ObjectInput input) {
        if (instance == null) {
            instance = new Bank(input);
        }
        return instance;
    }

    public static void delete() {
        instance = null;
    }

    /**
     * Runs each command type and stores its output in Json format
     * @return ArrayNode that encapsulates all commands output
     */
    public ArrayNode runCommands() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode commandsOutput = mapper.createArrayNode();
        ObjectNode commandResult;

        CommandHandler handler;
        HandlerFactory handlerFactory = HandlerFactory.getInstance();
        CommandVisitorInterface visitor;
        CommandVisitorFactory visitorFactory = CommandVisitorFactory.getInstance();

        for (CommandInput command : getInput().getCommands()) {
            setTimestamp(getTimestamp() + 1);
            visitor = visitorFactory.getVisitor(command.getCommand());
            handler = handlerFactory.getHandlerType(command, this);
            commandResult = null;
            if (handler != null && visitor != null) {
                commandResult = handler.runCommand(visitor);
            }

            if (commandResult != null) {
                commandsOutput.add(commandResult);
            }
        }
        return commandsOutput;
    }

    /**
     * Accepts visitor for running command and command handler to store command data
     * @param visitor Visitor class that implements the command processing logic
     * @param bankCommandHandler The handler containing command data
     * @return Result of the visit method call, encapsulated in an ObjectNode
     */
    public ObjectNode accept(final CommandVisitorInterface visitor,
                             final CommandHandler bankCommandHandler) {
        return visitor.visit(this, bankCommandHandler);
    }

    //Commission calculator
    public double commission(User user, double amount) {
        if (user.getPlan().equals("standard")) {
            amount = amount * 1.002;
        }
        if (user.getPlan().equals("silver") && amount >= 500) {
            amount = amount * 1.001;
        }
        return amount;
    }

    public double getCashback(final Account account) {
        ArrayNode transactions = account.getTransactions();
        JsonNode lastTransaction = transactions.
                get(transactions.size() - 1);

        if (lastTransaction.get("commerciant") != null) {
            CommerciantInput lastCommerciant = searchCommerciant
                    (lastTransaction.get("commerciant").asText());
            if (lastCommerciant == null) {
                return 0;
            }
            return getCashbackNrOfTransactions(account, lastCommerciant, lastTransaction.get("amount").asText()) +
                    getCashbackSpendingThreshold(account, lastCommerciant, lastTransaction.get("amount").asText());
        }
        return 0;
    }

    public double getCashbackSpendingThreshold
            (final Account account, final CommerciantInput lastCommerciant, final String amount) {
        User user = searchUserByAccount(account.getNumber());

        if (!lastCommerciant.getCashbackStrategy().equals("spendingThreshold")) {
            return 0;
        }

        if (user == null) {
            return 0;
        }

        account.setSpendingThreshold100(account.getSpendingThreshold100() + Double.parseDouble(amount));
        account.setSpendingThreshold300(account.getSpendingThreshold300() + Double.parseDouble(amount));
        account.setSpendingThreshold500(account.getSpendingThreshold500() + Double.parseDouble(amount));

        if (user.getPlan().equals("standard") || user.getPlan().equals("student")) {
            if (account.getSpendingThreshold500() >= 500) {
                return Double.parseDouble(amount) * 0.0025;
            }
            if (account.getSpendingThreshold300() >= 300) {
                return Double.parseDouble(amount) * 0.002;
            }
            if (account.getSpendingThreshold100() >= 100) {
                return Double.parseDouble(amount) * 0.001;
            }
        }
        if (user.getPlan().equals("silver")) {
            if (account.getSpendingThreshold500() >= 500) {
                return Double.parseDouble(amount) * 0.005;
            }
            if (account.getSpendingThreshold300() >= 300) {
                return Double.parseDouble(amount) * 0.004;
            }
            if (account.getSpendingThreshold100() >= 100) {
                return Double.parseDouble(amount) * 0.003;
            }
        }
        if (user.getPlan().equals("gold")) {
            if (account.getSpendingThreshold500() >= 500) {
                return Double.parseDouble(amount) * 0.007;
            }
            if (account.getSpendingThreshold300() >= 300) {
                return Double.parseDouble(amount) * 0.0055;
            }
            if (account.getSpendingThreshold100() >= 100) {
                return Double.parseDouble(amount) * 0.005;
            }
        }

        return 0;
    }

    public double getCashbackNrOfTransactions
            (final Account account, final CommerciantInput lastCommerciant, final String amount) {

        if (!lastCommerciant.getCashbackStrategy().equals("nrOfTransactions")) {
            return 0;
        }

        if (lastCommerciant.getType().equals("Food") && account.getCashbackForFood() > 0) {
            account.setCashbackForFood(-1);
            return Double.parseDouble(amount) * 0.02;
        }

        if (lastCommerciant.getType().equals("Clothes") && account.getCashbackForClothes() > 0) {
            account.setCashbackForClothes(-1);
            return Double.parseDouble(amount) * 0.05;
        }

        if (lastCommerciant.getType().equals("Tech") && account.getCashbackForTech() > 0) {
            account.setCashbackForTech(-1);
            return Double.parseDouble(amount) * 0.1;
        }

        for (JsonNode transaction : account.getTransactions()) {
            if (transaction.get("commerciant") != null) {
                CommerciantInput commerciant =
                        searchCommerciant(transaction.get("commerciant").asText());
                if (commerciant != null && "nrOfTransactions".equals(commerciant.getCashbackStrategy())) {
                    long numberOfCommerciants =
                            StreamSupport.stream
                                            (account.getTransactions().spliterator(), false)
                            .filter(t -> t.get("commerciant") != null)
                            .count();
                    if (numberOfCommerciants == 2) account.setCashbackForFood(1);
                    if (numberOfCommerciants == 5) account.setCashbackForClothes(1);
                    if (numberOfCommerciants == 10) account.setCashbackForTech(1);
                }
            }
        }
        return 0;
    }

    public CommerciantInput searchCommerciant(final String commerciant) {
        for (CommerciantInput commerciantJson : getInput().getCommerciants()) {
            if (commerciantJson.getCommerciant().equals(commerciant)) {
                return commerciantJson;
            }
        }
        return null;
    }

    /**
     * Converts money to another currency
     * @param firstCurrency Currency to be converted
     * @param secondCurrency Currency to convert to
     * @param amount Amount to exchange
     * @return Double integer representing converted amount of money
     */
    public double exchange(
            final String firstCurrency, final String secondCurrency, final double amount) {
        double exchangedAmount = 0;

        for (ExchangeInput exchange : getInput().getExchangeRates()) {
            if ((firstCurrency.equals(exchange.getFrom())
                    && secondCurrency.equals(exchange.getTo()))) {
                exchangedAmount = amount * (1 / exchange.getRate());
                return exchangedAmount;
            }
            if ((firstCurrency.equals(exchange.getTo())
                    && secondCurrency.equals(exchange.getFrom()))) {
                exchangedAmount = amount * exchange.getRate();
                return exchangedAmount;
            }
        }

        for (ExchangeInput intermediateExchange : getInput().getExchangeRates()) {
            String intermediateCurrency = intermediateExchange.getFrom();
            if (!intermediateCurrency.equals(firstCurrency)
                && !intermediateCurrency.equals(secondCurrency)) {
                Double firstRate = findRate(firstCurrency, intermediateCurrency);
                Double secondRate = findRate(intermediateCurrency, secondCurrency);

                if (firstRate != null && secondRate != null) {
                    exchangedAmount = amount * (1 / firstRate) * (1 / secondRate);
                    return exchangedAmount;
                }
            }
        }
        return exchangedAmount;
    }

    /**
     * Helper function for finding exchange rate between 2 given currencies
     * @param fromCurrency Currency that needs to be converted
     * @param toCurrency Converted Currency
     * @return Double integer representing exchange rate
     */
    private Double findRate(final String fromCurrency, final String toCurrency) {
        for (ExchangeInput exchange : getInput().getExchangeRates()) {
            if (exchange.getFrom().equals(fromCurrency) && exchange.getTo().equals(toCurrency)) {
                return exchange.getRate();
            }
            if (exchange.getTo().equals(fromCurrency) && exchange.getFrom().equals(toCurrency)) {
                return 1 / exchange.getRate();
            }
        }
        return null;
    }

    /**
     * Searches user by email in bank data
     * @param email User corresponding email, stored as a string
     * @return User class representing found user
     */
    public User searchUser(final String email) {
        for (User user : getUsers()) {
            if (user.getUserInformation().getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Searches account by IBAN in bank data
     * @param number Account corresponding IBAN, stored as a string
     * @return Account class representing found account
     */
    public Account searchAccount(final String number) {
        for (User user : getUsers()) {
            for (Account account : user.getAccounts()) {
                String alias = account.getAlias();
                if (account.getNumber().equals(number) || (!alias.isEmpty() && alias.equals(number))) {
                    return account;
                }
            }
        }
        return null;
    }

    /**
     * Searches card by number in bank data
     * @param number Card corresponding number, stored as a string
     * @return Card class representing found card
     */
    public Card searchCard(final String number) {
        for (User user : getUsers()) {
            for (Account account : user.getAccounts()) {
                for (Card card : account.getCards()) {
                    if (card.getNumber().equals(number)) {
                        return card;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Searches user by owned account in bank data
     * @param number Account corresponding IBAN, stored as a string
     * @return User class representing owner of account
     */
    public User searchUserByAccount(final String number) {
        for (User user : getUsers()) {
            for (Account account : user.getAccounts()) {
                if (account.getNumber().equals(number)) {
                    return user;
                }
            }
        }
        return null;
    }

    public Account searchAccountByCard(final String number) {
        for (User user : getUsers()) {
            for (Account account : user.getAccounts()) {
                for (Card card : account.getCards()) {
                    if (card.getNumber().equals(number)) {
                        return account;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds user to bank users list
     * @param user User class that needs to be added to user bank list
     */
    public void addUser(final User user) {
        getUsers().add(user);
    }

}
