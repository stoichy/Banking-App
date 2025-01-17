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
    private List<List<SplitPayment>> listsOfSplitPayments;

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
        this.listsOfSplitPayments = new ArrayList<>();
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
    public double getCommission(Account account, double amount) {
        User user = searchUserByAccount(account.getNumber());
        double exchangedAmount = amount;
        if (user == null) {
            return amount;
        }

        if (!account.getCurrency().equals("RON")) {
            exchangedAmount = exchange("RON", account.getCurrency(), amount);
        }
        if (user.getPlan().equals("standard")) {
            amount = amount * 1.002;
        }
        if (user.getPlan().equals("silver") && exchangedAmount >= 500) {
            amount = amount * 1.001;
        }
        return amount;
    }

    public double getCashback(final Account account) {
        ArrayNode transactions = account.getTransactions();
        JsonNode lastTransaction = transactions.get(transactions.size() - 1);
        String amount = lastTransaction.get("amount").asText();

        CommerciantInput lastCommerciant = null;
        if (lastTransaction.get("commerciant") != null) {
            lastCommerciant = searchCommerciant(lastTransaction.get("commerciant").asText());
        } else if (lastTransaction.get("receiverIBAN") != null) {
            lastCommerciant = searchCommerciantAccount(lastTransaction.get("receiverIBAN").asText());
        }
        if (lastCommerciant == null) {
            return 0;
        }

        if (!"spendingThreshold".equals(lastCommerciant.getCashbackStrategy())) {
            return getCashbackNrOfTransactions(account, lastCommerciant, amount);
        } else {
            return getCashbackSpendingThreshold(account, lastCommerciant, amount);
        }
    }

    public double getCashbackSpendingThreshold(Account account, CommerciantInput lastCommerciant, String amount) {
        User user = searchUserByAccount(account.getNumber());
        if (user == null) {
            return 0;
        }
        double doubleAmount = Double.parseDouble(amount.split(" ")[0]);
        double exchangedAmount = exchange("RON", account.getCurrency(), Double.parseDouble(amount.split(" ")[0]));
        account.setSpendingThreshold(account.getSpendingThreshold() + exchangedAmount);

        double spendingThreshold = account.getSpendingThreshold();
        String plan = user.getPlan();
        double cashback = 0;

        if ("standard".equals(plan) || "student".equals(plan)) {
            cashback = getStandardOrStudentCashback(spendingThreshold, doubleAmount);
        } else if ("silver".equals(plan)) {
            cashback = getSilverCashback(spendingThreshold, doubleAmount);
        } else if ("gold".equals(plan)) {
            cashback = getGoldCashback(spendingThreshold, doubleAmount);
        }

        return cashback;
    }

    //Helper method for getCashbackSpendingThreshold
    private double getStandardOrStudentCashback(double spendingThreshold, final double amount) {
        if (spendingThreshold >= 500) {
            return amount * 0.0025;
        } else if (spendingThreshold >= 300) {
            return amount * 0.002;
        } else if (spendingThreshold >= 100) {
            return amount * 0.001;
        }
        return 0;
    }

    //Helper method for getCashbackSpendingThreshold
    private double getSilverCashback(double spendingThreshold, final double amount) {
        if (spendingThreshold >= 500) {
            return amount * 0.005;
        } else if (spendingThreshold >= 300) {
            return amount * 0.004;
        } else if (spendingThreshold >= 100) {
            return amount * 0.003;
        }
        return 0;
    }

    //Helper method for getCashbackSpendingThreshold
    private double getGoldCashback(double spendingThreshold, final double amount) {
        if (spendingThreshold >= 500) {
            return amount * 0.007;
        } else if (spendingThreshold >= 300) {
            return amount * 0.0055;
        } else if (spendingThreshold >= 100) {
            return amount * 0.005;
        }
        return 0;
    }

    public double getCashbackNrOfTransactions(final Account account, final CommerciantInput lastCommerciant, final String amount) {
        double parsedAmount = Double.parseDouble(amount.split(" ")[0]);
        String commerciantType = lastCommerciant.getType();

        if (applyCashbackForType(account, commerciantType, parsedAmount)) {
            return cashbackAmount(commerciantType, parsedAmount);
        }

        updateCashbackBasedOnTransactions(account);

        return 0;
    }

    //Helper method for getCashbackNrOfTransactions
    private boolean applyCashbackForType(Account account, String type, double amount) {
        if ("Food".equals(type) && account.getCashbackForFood() > 0) {
            account.setCashbackForFood(-1);
            return true;
        }
        if ("Clothes".equals(type) && account.getCashbackForClothes() > 0) {
            account.setCashbackForClothes(-1);
            return true;
        }
        if ("Tech".equals(type) && account.getCashbackForTech() > 0) {
            account.setCashbackForTech(-1);
            return true;
        }
        return false;
    }

    //Helper method for getCashbackNrOfTransactions
    private double cashbackAmount(String type, double amount) {
        return switch (type) {
            case "Food" -> amount * 0.02;
            case "Clothes" -> amount * 0.05;
            case "Tech" -> amount * 0.1;
            default -> 0;
        };
    }

    //Helper method for getCashbackNrOfTransactions
    private void updateCashbackBasedOnTransactions(Account account) {
        long numberOfCommerciants = StreamSupport.stream(account.getTransactions().spliterator(), false)
                .filter(t -> t.get("commerciant") != null)
                .count();

        if (numberOfCommerciants == 2) account.setCashbackForFood(1);
        if (numberOfCommerciants == 5) account.setCashbackForClothes(1);
        if (numberOfCommerciants == 10) account.setCashbackForTech(1);
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
                if (account.getNumber().equals(number) || (!alias.isEmpty() && alias.equals(number))) {
                    return account;
                }
            }
        }
        return null;
    }

    public CommerciantInput searchCommerciantAccount (final String number) {
        for (CommerciantInput commerciant : getInput().getCommerciants()) {
            if (commerciant.getAccount().equals(number)) {
                return commerciant;
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

    public void addListOfSplitPayments() {
        this.listsOfSplitPayments.add(new ArrayList<>());
    }

    public void addSplitPaymentToList(final SplitPayment splitPayment) {
        this.listsOfSplitPayments.getLast().add(splitPayment);
    }
}
