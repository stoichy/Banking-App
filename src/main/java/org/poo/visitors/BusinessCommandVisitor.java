package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

import java.util.*;

public final class BusinessCommandVisitor implements CommandVisitorInterface {
    //Business report
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account = bank.searchAccount(handler.getCommand().getAccount());

        if (account == null) {
            return null;
        }

        if (!account.getType().equals("business")) {
            return null;
        }

        if (handler.getCommand().getType().equals("commerciant")) {
            ArrayList<Object[]> commerciantsExpenses = new ArrayList<>();
            boolean found;
            for (Object[] pair : account.getExpenses()) {
                String commerciant = (String) pair[2];
                if (commerciant != null) {
                    found = false;
                    for (int i = 0; i < commerciantsExpenses.size(); i++) {
                        if (commerciant.equals(commerciantsExpenses.get(i)[0])) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        commerciantsExpenses.add(new Object[]{commerciant, 0.0, null, null});
                        ArrayNode managers = new ArrayNode(new ObjectMapper().getNodeFactory());
                        ArrayNode employees = new ArrayNode(new ObjectMapper().getNodeFactory());
                        for (Object[] pair2 : account.getExpenses()) {
                            if (commerciant.equals(pair2[2])) {
                                double amount = (double) pair2[1];
                                commerciantsExpenses.getLast()[1] = (double) commerciantsExpenses.getLast()[1] + amount;
                                String email = (String) pair2[0];
                                User user = bank.searchUser(email);
                                if (account.getUserPosition(email).equals("manager")) {
                                    ObjectNode managerNode = new ObjectMapper().createObjectNode();
                                    managerNode.put("username", user.getUserInformation().getLastName() + " " +
                                            user.getUserInformation().getFirstName());
                                    managers.add(managerNode);
                                } else {
                                    ObjectNode employeeNode = new ObjectMapper().createObjectNode();
                                    employeeNode.put("username", user.getUserInformation().getLastName() + " " +
                                            user.getUserInformation().getFirstName());
                                    employees.add(employeeNode);
                                }
                            }
                        }
                        commerciantsExpenses.getLast()[2] = managers;
                        commerciantsExpenses.getLast()[3] = employees;
                    }
                }
            }
            return bankCommerciantReportJson(commerciantsExpenses, account, handler);
        }
        else {
            double totalSpent = 0, totalDeposited = 0, depositedPerUser = 0, spentPerUser = 0;
            ArrayNode managers = new ObjectMapper().createArrayNode();
            ArrayNode employees = new ObjectMapper().createArrayNode();
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
                managerNode.put("username", manager.getUserInformation().getLastName() + " " +
                        manager.getUserInformation().getFirstName());
                managerNode.put("deposited", depositedPerUser);
                managerNode.put("spent", spentPerUser);
                managers.add(managerNode);
            }
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
                employeeNode.put("username", employee.getUserInformation().getLastName() + " " +
                        employee.getUserInformation().getFirstName());
                employeeNode.put("deposited", depositedPerUser);
                employeeNode.put("spent", spentPerUser);
                employees.add(employeeNode);
            }
            for (Object[] pair : account.getExpenses()) {
                double amount = (double) pair[1];
                totalSpent += amount;
            }
            for (Object[] pair : account.getDeposits()) {
                double amount = (double) pair[1];
                totalDeposited += amount;
            }
            return bankTransactionsReportJson(handler, account, managers, employees, totalDeposited, totalSpent);
        }
    }

    //Add business associate
    public ObjectNode visit(final User user, final CommandHandler handler) {
        Account account = handler.getBank().searchAccount(handler.getCommand().getAccount());
        if (account == null) {
            return null;
        }
        if (handler.getCommand().getRole().equals("employee")) {
            account.addEmployee(user);
        } else {
            account.addManager(user);
        }
        return null;
    }

    //Change limit
    public ObjectNode visit(final Account account, final CommandHandler handler) {
        if (!handler.getCommand().getEmail().equals
                (account.getOwner().getUserInformation().getEmail())) {
            return userIsNotOwnerJson(handler);
        }
        if (handler.getCommand().getCommand().equals("changeSpendingLimit")) {
            account.setSpendingLimit(handler.getCommand().getAmount());
        } else {
            account.setDepositLimit(handler.getCommand().getAmount());
        }
        return null;
    }

    public ObjectNode visit(final Card card, final CommandHandler handler) {
        return null;
    }

    public ObjectNode bankCommerciantReportJson(ArrayList<Object[]> commerciantsExpenses, Account account, CommandHandler handler) {
        ObjectNode commandJson = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("balance", account.getBalance());
        ArrayNode commerciantsExpensesJson = new ObjectMapper().createArrayNode();
        for (Object[] pair : commerciantsExpenses) {
            ObjectNode commerciantNode = new ObjectMapper().createObjectNode();
            commerciantNode.put("commerciant", (String) pair[0]);
            commerciantNode.put("total spent", (double) pair[1]);
            commerciantNode.set("managers", (ArrayNode) pair[2]);
            commerciantNode.set("employees", (ArrayNode) pair[3]);
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

    public ObjectNode bankTransactionsReportJson
            (CommandHandler handler, Account account, ArrayNode managers, ArrayNode employees, double totalDeposited, double totalSpent) {
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

    public ObjectNode userIsNotOwnerJson(CommandHandler handler) {
        ObjectNode commandJson = handler.getJsonWithHeader(handler.getCommand());
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "You must be owner in order to change spending limit.");
        output.put("timestamp", handler.getCommand().getTimestamp());
        commandJson.set("output", output);
        commandJson.put("timestamp", handler.getCommand().getTimestamp());
        return commandJson;
    }
}
