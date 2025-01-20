package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

import java.util.ArrayList;
import java.util.Comparator;

public final class BusinessCommandVisitor implements CommandVisitorInterface {
    private static final int MANAGERS_INDEX_FOR_EXPENSES = 2;
    private static final int EMPLOYEES_INDEX_FOR_EXPENSES = 3;

    /**
     * Generates business account commerciants or transactions report
     * @param bank Bank data
     * @param handler Command data
     * @return Json format output that represents report details
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account = bank.searchAccount(handler.getCommand().getAccount());

        if (handler.getCommand().getType().equals("commerciant")) {
            return commerciantBusinessReport(account, handler);
        } else {
            return transactionsBusinessReport(account, handler);
        }
    }

    private ObjectNode commerciantBusinessReport(
            final Account account, final CommandHandler handler) {
        ArrayList<Object[]> commerciantsExpenses = new ArrayList<>();
        boolean found;
        // Iterate through all expenses and store them by commerciant
        for (Object[] pair1 : account.getExpenses()) {
            String commerciant = (String) pair1[2];
            if (commerciant != null) {
                found = false;
                // Make sure that commerciant is not in ArrayList
                for (Object[] commerciantsExpense : commerciantsExpenses) {
                    if (commerciant.equals(commerciantsExpense[0])) {
                        found = true;
                        break;
                    }
                }
                // Add all account's expenses related to commerciant to ArrayList
                if (!found) {
                    commerciantsExpenses.add(new Object[]{commerciant, 0.0, null, null});
                    ArrayList<String> managers = new ArrayList<>();
                    ArrayList<String> employees = new ArrayList<>();
                    for (Object[] pair2 : account.getExpenses()) {
                        if (commerciant.equals(pair2[2])) {
                            double amount = (double) pair2[1];
                            commerciantsExpenses.getLast()[1] =
                                    (double) commerciantsExpenses.getLast()[1] + amount;
                            String email = (String) pair2[0];
                            User user = handler.getBank().searchUser(email);
                            // Add all managers that had transactions with commerciant
                            if (account.getUserPosition(email).equals("manager")) {
                                managers.add(user.getUserInformation().getLastName() + " "
                                        + user.getUserInformation().getFirstName());
                            } else {
                                // Add all employees that had transactions with commerciant
                                employees.add(user.getUserInformation().getLastName() + " "
                                        + user.getUserInformation().getFirstName());
                            }
                        }
                    }
                    // Add all managers and employees to ArrayList
                    commerciantsExpenses.getLast()[MANAGERS_INDEX_FOR_EXPENSES] = managers;
                    commerciantsExpenses.getLast()[EMPLOYEES_INDEX_FOR_EXPENSES] = employees;
                }
            }
        }
        return bankCommerciantsReportJson(commerciantsExpenses, account, handler);
    }

    private ObjectNode transactionsBusinessReport(
            final Account account, final CommandHandler handler) {
        double totalSpent = 0, totalDeposited = 0, depositedPerUser = 0, spentPerUser = 0;
        ArrayNode managers = new ObjectMapper().createArrayNode();
        ArrayNode employees = new ObjectMapper().createArrayNode();
        // Iterate through all business account's managers and store
        // how much each one of them deposited and spent
        for (User manager : account.getManagers()) {
            depositedPerUser = 0;
            spentPerUser = 0;
            for (Object[] pair : account.getDeposits()) {
                String email = (String) pair[0];
                double amount = (double) pair[1];
                if (email.equals(manager.getUserInformation().getEmail())) {
                    depositedPerUser += amount;
                }
            }
            for (Object[] pair : account.getExpenses()) {
                String email = (String) pair[0];
                double amount = (double) pair[1];
                if (email.equals(manager.getUserInformation().getEmail())) {
                    spentPerUser += amount;
                }
            }
            ObjectNode managerNode = new ObjectMapper().createObjectNode();
            managerNode.put("username",
                        manager.getUserInformation().getLastName() + " "
                                + manager.getUserInformation().getFirstName());
            managerNode.put("deposited", depositedPerUser);
            managerNode.put("spent", spentPerUser);
            managers.add(managerNode);
        }
        // Iterate through all business account's employees and store
        // how much each one of them deposited and spent
        for (User employee : account.getEmployees()) {
            depositedPerUser = 0;
            spentPerUser = 0;
            for (Object[] pair : account.getExpenses()) {
                String email = (String) pair[0];
                double amount = (double) pair[1];
                if (email.equals(employee.getUserInformation().getEmail())) {
                    spentPerUser += amount;
                }
            }
            for (Object[] pair : account.getDeposits()) {
                String email = (String) pair[0];
                double amount = (double) pair[1];
                if (email.equals(employee.getUserInformation().getEmail())) {
                    depositedPerUser += amount;
                }
            }
            ObjectNode employeeNode = new ObjectMapper().createObjectNode();
            employeeNode.put("username", employee.getUserInformation().getLastName()
                    + " " + employee.getUserInformation().getFirstName());
            employeeNode.put("deposited", depositedPerUser);
            employeeNode.put("spent", spentPerUser);
            employees.add(employeeNode);
        }
        // Calculate total amount of account's expenses
        for (Object[] pair : account.getExpenses()) {
            double amount = (double) pair[1];
            totalSpent += amount;
        }
        // Calculate total amount of account's deposits
        for (Object[] pair : account.getDeposits()) {
            double amount = (double) pair[1];
            totalDeposited += amount;
        }
        return bankTransactionsReportJson(
                handler, account, managers, employees, totalDeposited, totalSpent);
    }

    /**
     * Addds business associate to business account
     * @param user Business associate to be added
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Account account = handler.getBank().searchAccount(handler.getCommand().getAccount());

        if (account != null
                && !account.getUserPosition(user.getUserInformation().getEmail()).isEmpty()) {
            return null;
        }

        if (handler.getCommand().getRole().equals("employee")) {
            account.addEmployee(user);
        } else {
            account.addManager(user);
        }
        return null;
    }

    /**
     * Changes business account's spending or deposit limit
     * @param account Business account DATA
     * @param handler Command data
     * @return Null if change was successful, otherwise Json format error output
     */
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        if (!handler.getCommand().getEmail().equals(
                account.getOwner().getUserInformation().getEmail())) {
            return userIsNotOwnerJson(handler);
        }
        if (!account.getType().equals("business")) {
            return notBusinessAccountJson(handler);
        }
        if (handler.getCommand().getCommand().equals("changeSpendingLimit")) {
            account.setSpendingLimit(handler.getCommand().getAmount());
        } else {
            account.setDepositLimit(handler.getCommand().getAmount());
        }
        return null;
    }

    /**
     * Method used to implement visitor interface
     * @param card Card data
     * @param handler Command data
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        return null;
    }

    /**
     * Generates Object Node containing business account's commerciants expenses report
     * @param commerciantsExpenses List containing all associates expenses for each commerciant
     * @param account Business account data
     * @param handler Command data
     * @return Json format output that contains report details
     */
    public ObjectNode bankCommerciantsReportJson(final ArrayList<Object[]> commerciantsExpenses,
                                                 final Account account,
                                                 final CommandHandler handler) {
        ObjectNode commandJson = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("balance", account.getBalance());
        ArrayNode commerciantsExpensesJson = new ObjectMapper().createArrayNode();
        commerciantsExpenses.sort(Comparator.comparing(pair -> (String) pair[0]));
        for (Object[] pair : commerciantsExpenses) {
            ObjectNode commerciantNode = new ObjectMapper().createObjectNode();
            commerciantNode.put("commerciant", (String) pair[0]);
            commerciantNode.put("total received", (double) pair[1]);
            commerciantNode.set(
                    "managers", new ObjectMapper().valueToTree(pair[MANAGERS_INDEX_FOR_EXPENSES]));
            commerciantNode.set(
                    "employees", new ObjectMapper().valueToTree(
                            pair[EMPLOYEES_INDEX_FOR_EXPENSES]));
            commerciantsExpensesJson.add(commerciantNode);
        }
        output.set("commerciants",  commerciantsExpensesJson);
        output.put("currency", account.getCurrency());
        output.put("deposit limit", account.getDepositLimit());
        output.put("IBAN", account.getNumber());
        output.put("spending limit", account.getSpendingLimit());
        output.put("statistics type", "commerciant");
        commandJson.set("output", output);
        commandJson.put("timestamp", handler.getCommand().getTimestamp());
        return commandJson;
    }

    /**
     * Generates Object Node containing all business account's transactions report
     * @param handler Command data
     * @param account Business account data
     * @param managers List containing all account's managers and their transactions
     * @param employees List containing all account's employees and their transactions
     * @param totalDeposited Total amount of money deposited in account
     * @param totalSpent Total amount of money spent from account
     * @return Json format output that contains report details
     */
    public ObjectNode bankTransactionsReportJson(final CommandHandler handler,
                                                 final Account account, final ArrayNode managers,
                                                 final ArrayNode employees,
                                                 final double totalDeposited,
                                                 final double totalSpent) {
        ObjectNode commandJson = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("IBAN", account.getNumber());
        output.put("balance", account.getBalance());
        output.put("currency", account.getCurrency());
        output.put("spending limit", account.getSpendingLimit());
        output.put("deposit limit", account.getDepositLimit());
        output.put("statistics type", "transaction");
        output.set("managers", managers);
        output.set("employees", employees);
        output.put("total spent", totalSpent);
        output.put("total deposited", totalDeposited);
        commandJson.set("output", output);
        commandJson.put("timestamp", handler.getCommand().getTimestamp());
        return commandJson;
    }

    /**
     * Generates Object Node containing error message for business account command
     * @param handler Command data
     * @return Json format output that contains business account command error
     */
    public ObjectNode userIsNotOwnerJson(final CommandHandler handler) {
        ObjectNode commandJson = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();

        if (handler.getCommand().getCommand().equals("changeSpendingLimit")) {
            output.put("description", "You must be owner in order to change spending limit.");
        } else if (handler.getCommand().getCommand().equals("changeDepositLimit")) {
            output.put("description", "You must be owner in order to change deposit limit.");
        }
        output.put("timestamp", handler.getCommand().getTimestamp());
        commandJson.set("output", output);
        commandJson.put("timestamp", handler.getCommand().getTimestamp());
        return commandJson;
    }

    /**
     * Generates Object Node containing error message for business account command
     * @param handler Command data
     * @return Json format output that contains business account command error
     */
    public ObjectNode notBusinessAccountJson(final CommandHandler handler) {
        ObjectNode commandJson = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "This is not a business account");
        output.put("timestamp", handler.getCommand().getTimestamp());
        commandJson.set("output", output);
        commandJson.put("timestamp", handler.getCommand().getTimestamp());
        return commandJson;
    }
}
