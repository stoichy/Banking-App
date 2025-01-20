package org.poo.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import org.poo.fileio.ExchangeInput;
import org.poo.fileio.ObjectInput;
import org.poo.fileio.UserInput;
import org.poo.fileio.CommandInput;
import org.poo.fileio.CommerciantInput;

import org.poo.handlers.CommandHandler;
import org.poo.handlers.HandlerFactory;

import org.poo.visitors.CommandVisitorInterface;
import org.poo.visitors.CommandVisitorFactory;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public final class Bank {
    private static Bank instance = null;
    private ObjectInput input;
    private int timestamp;
    private List<User> users;
    private List<List<SplitPayment>> listsOfSplitPayments;
    private static final double SILVER_PLAN_COMMISSION = 1.001;
    private static final double STANDARD_PLAN_COMMISSION = 1.002;
    private static final int MINIMUM_AMOUNT_FOR_SILVER_PLAN_COMMISSION = 500;

    private Bank(final ObjectInput input) {
        setInput(input);
        setTimestamp(0);
        setUsers(new ArrayList<>());
        for (UserInput userInput : getInput().getUsers()) {
            User user = new User.Builder(userInput).plan("business").build();
            addUser(user);
            if (user.getUserInformation().getOccupation().equals("student")) {
                user.setPlan("student");
            } else {
                user.setPlan("standard");
            }
        }
        this.listsOfSplitPayments = new ArrayList<>();
    }

    /**
     * Get instance if class is already initialized, otherwise create a new one
     * @param input Input data needed for instantiating Bank
     * @return Instance of the Bank class
     */
    public static Bank getInstance(final ObjectInput input) {
        if (instance == null) {
            instance = new Bank(input);
        }
        return instance;
    }

    /**
     * Deletes bank instance
     */
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

    /**
     * Calculate total transaction amount plus commission according to account plan
     * @param account Account data
     * @param amount Amount of money needed for transaction
     * @return Total transaction amount with commission included
     */
    public double getCommission(final Account account, final double amount) {
        User user = searchUserByAccount(account.getNumber());

        double exchangedAmount = exchange("RON", account.getCurrency(), amount);
        if (user.getPlan().equals("standard")) {
            return amount * STANDARD_PLAN_COMMISSION;
        }
        if (user.getPlan().equals("silver")
                && exchangedAmount >= MINIMUM_AMOUNT_FOR_SILVER_PLAN_COMMISSION) {
            return amount * SILVER_PLAN_COMMISSION;
        }
        return amount;
    }

    /**
     * Calculate cashback for last transaction of given account
     * depending on commerciant cashback strategy
     * @param account Account involved in last transaction
     * @return Amount of cashback given for last transaction
     */
    public double getCashback(final Account account) {
        ArrayNode transactions = account.getTransactions();
        JsonNode lastTransaction = transactions.get(transactions.size() - 1);
        String amount = lastTransaction.get("amount").asText();

        CommerciantInput lastCommerciant = null;
        if (lastTransaction.get("commerciant") != null) {
            lastCommerciant = searchCommerciant(
                    lastTransaction.get("commerciant").asText());
        } else if (lastTransaction.get("receiverIBAN") != null) {
            lastCommerciant = searchCommerciantAccount(
                    lastTransaction.get("receiverIBAN").asText());
        }
        if (lastCommerciant == null) {
            return 0;
        }

        Cashback cashback;
        if (!"spendingThreshold".equals(lastCommerciant.getCashbackStrategy())) {
            cashback = new NrOfTransactionsCashback(account, lastCommerciant, amount, this);
            return cashback.getCashbackNrOfTransactions();
        } else {
             cashback = new SpendingThresholdCashback(account, lastCommerciant, amount, this);
            return cashback.getCashbackSpendingThreshold();
        }
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
     * Searches commerciant by name from input
     * @param commerciant Commerciant name
     * @return If found, returns Json format object for storing commerciant details, otherwise
     * returns null
     */
    public CommerciantInput searchCommerciant(final String commerciant) {
        for (CommerciantInput commerciantJson : getInput().getCommerciants()) {
            if (commerciantJson.getCommerciant().equals(commerciant)) {
                return commerciantJson;
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
                if (account.getNumber().equals(number)
                        || (!alias.isEmpty() && alias.equals(number))) {
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

    /**
     * Searches account that stores given card
     * @param number Card number
     * @return If found, return Account class instance, otherwise null
     */
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
     * Searches for account owned by commerciant
     * @param number Account IBAN
     * @return If found, return class holding commerciant details, otherwise null
     */
    public CommerciantInput searchCommerciantAccount(final String number) {
        for (CommerciantInput commerciant : getInput().getCommerciants()) {
            if (commerciant.getAccount().equals(number)) {
                return commerciant;
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

    /**
     * Searches list of payments for a split payment
     * (waiting to be accepted or rejected) by user and payment type
     * @param email User email
     * @param type Split payment type
     * @return If split payment is found, returns its list of payments, otherwise null
     */
    public List<SplitPayment> searchSplitPaymentsListByUser(final String email, final String type) {
        List<SplitPayment> splitPaymentFoundList = null;
        for (List<SplitPayment> splitPaymentList : getListsOfSplitPayments()) {
            for (SplitPayment splitPayment : splitPaymentList) {
                if (splitPayment.getType().equals(type)) {
                    if (splitPayment.getUser().getUserInformation().getEmail().equals(email)) {
                        splitPayment.setAccepted(true);
                        splitPaymentFoundList = splitPaymentList;
                    }
                }
            }
        }
        return splitPaymentFoundList;
    }

    /**
     * Adds a list of payments corresponding to a split payment
     */
    public void addListOfSplitPayments() {
        this.listsOfSplitPayments.add(new ArrayList<>());
    }

    /**
     * Adds a payment to the list of payments corresponding to a split payment
     */
    public void addSplitPaymentToList(final SplitPayment splitPayment) {
        this.listsOfSplitPayments.getLast().add(splitPayment);
    }
}
