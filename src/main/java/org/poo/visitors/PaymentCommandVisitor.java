package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.poo.fileio.CommandInput;
import org.poo.handlers.CommandHandler;
import org.poo.main.Bank;
import org.poo.main.SplitPayment;
import org.poo.main.User;
import org.poo.main.Account;
import org.poo.main.Card;

import java.util.ArrayList;
import java.util.List;

public final class PaymentCommandVisitor implements CommandVisitorInterface {
    private static final int MINIMUM_TRANSACTION_AMOUNT_FOR_GOLD_UPGRADE = 300;
    private static final int MINIMUM_TRANSACTIONS_NUMBER_FOR_GOLD_UPGRADE = 5;

    /**
     * Handles split payment process between accounts
     * @param bank Bank data for access to accounts
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        CommandInput commandData = handler.getCommand();

        switch (commandData.getCommand()) {
            case "acceptSplitPayment":
                if (bank.searchUser(commandData.getEmail()) == null) {
                    return handler.userNotFoundJson();
                }
                customSplitPayment(bank, commandData.getEmail(), commandData.getSplitPaymentType(),
                        "accept");
                return null;
            case "rejectSplitPayment":
                if (bank.searchUser(commandData.getEmail()) == null) {
                    return handler.userNotFoundJson();
                }
                customSplitPayment(bank, commandData.getEmail(), commandData.getSplitPaymentType(),
                        "reject");
                return null;
            default: break;
        }

        List<String> accounts = commandData.getAccounts();
        int numberOfAccounts = accounts.size();
        List<Double> amountForUsers = commandData.getAmountForUsers();
        double totalAmount = commandData.getAmount();
        String commandCurrency = commandData.getCurrency();
        String splitPaymentType = commandData.getSplitPaymentType();

        if (splitPaymentType.equals("equal") && amountForUsers == null) {
            double equalAmount = totalAmount / numberOfAccounts;
            amountForUsers = new ArrayList<>();
            for (int i = 0; i < numberOfAccounts; i++) {
                amountForUsers.add(equalAmount);
            }
            commandData.setAmountForUsers(amountForUsers);
        }

        bank.addListOfSplitPayments();

        for (int i = 0; i < numberOfAccounts; i++) {
            Account account = bank.searchAccount(accounts.get(i));
            if (account == null) {
                return null;
            }

            double paymentAmount = splitPaymentType.equals("equal")
                    ? amountForUsers.get(i) : commandData.getAmountForUsers().get(i);

            if (!account.getCurrency().equals(commandCurrency)) {
                paymentAmount = bank.exchange(account.getCurrency(), commandCurrency,
                        paymentAmount);
            }

            bank.addSplitPaymentToList(new SplitPayment(account.getOwner(), account, totalAmount,
                    paymentAmount, accounts, amountForUsers, account.getCurrency(), commandCurrency,
                    splitPaymentType, commandData.getTimestamp()));
        }

        return null;
    }

    private void customSplitPayment(final Bank bank, final String email, final String paymentType,
                                    final String commandType) {
        List<SplitPayment> splitPaymentList =
                bank.searchSplitPaymentsListByUser(email, paymentType);
        if (splitPaymentList == null) {
            return;
        }

        if (commandType.equals("accept")) {
            if (checkSplitPaymentsStatus(splitPaymentList)) {
                for (SplitPayment payment : splitPaymentList) {
                    if (payment.getAccount().getBalance() < payment.getPaymentAmount()) {
                        rejectSplitPayments(
                                bank, splitPaymentList, payment.getAccount().getNumber());
                        return;
                    }
                }
                acceptSplitPayments(bank, splitPaymentList);
            }
        } else {
            rejectSplitPayments(bank, splitPaymentList, "reject");
        }
    }

    private void acceptSplitPayments(final Bank bank, final List<SplitPayment> splitPaymentList) {
        splitPaymentList.forEach(payment -> {
            payment.getAccount().setBalance(payment.getAccount().getBalance()
                    - payment.getPaymentAmount());
            payment.getUser().addTransaction(acceptSplitPaymentJson(splitPaymentList));
        });
        bank.getListsOfSplitPayments().remove(splitPaymentList);
    }

    private void rejectSplitPayments(
            final Bank bank, final List<SplitPayment> splitPaymentList, final String error) {
        for (SplitPayment sp : splitPaymentList) {
            sp.getUser().addTransaction(rejectSplitPaymentJson(splitPaymentList, error));
        }
        bank.getListsOfSplitPayments().remove(splitPaymentList);
    }

    private boolean checkSplitPaymentsStatus(final List<SplitPayment> splitPayments) {
        for (SplitPayment splitPayment : splitPayments) {
            if (!splitPayment.isAccepted()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method used to implement visitor interface
     * @param user User data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final User user, final CommandHandler handler) {
        return null;
    }

    /**
     * Handles money transfer between 2 accounts
     * @param senderAccount Sender account data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Account senderAccount, final CommandHandler handler) {
        Bank bankData = handler.getBank();
        CommandInput commandData = handler.getCommand();

        Account receiverAccount = bankData.searchAccount(commandData.getReceiver());
        User senderUser = senderAccount.getOwner();
        double amount = commandData.getAmount();
        double amountPlusCommission = bankData.getCommission(senderAccount, amount);

        if (receiverAccount == null) {
            if (bankData.searchCommerciantAccount(commandData.getReceiver()) == null) {
                return handler.userNotFoundJson();
            }
            processTransaction(senderAccount, amountPlusCommission, handler, senderUser,
                    commandData.getReceiver());
            return null;
        }

        if (commandData.getAccount().equals(senderAccount.getAlias())
                || senderAccount.getBalance() < amountPlusCommission) {
            handler.addTransactions(senderUser, senderAccount,
                    senderAccount.insufficientFundsJson(commandData.getTimestamp()));
            return null;
        }

        User receiverUser = receiverAccount.getOwner();

        if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            amount = bankData.exchange(
                    receiverAccount.getCurrency(), senderAccount.getCurrency(), amount);
        }

        handler.addTransactions(senderUser, senderAccount,
                transferSenderJson(senderAccount.getNumber(), receiverAccount.getNumber(),
                        senderAccount.getCurrency(), handler));
        handler.addTransactions(receiverUser, receiverAccount,
                transferReceiverJson(senderAccount.getNumber(), receiverAccount.getNumber(),
                        receiverAccount.getCurrency(), amount, handler));

        receiverAccount.setBalance(receiverAccount.getBalance() + amount);
        senderAccount.setBalance(senderAccount.getBalance()
                + bankData.getCashback(senderAccount) - amountPlusCommission);

        return null;
    }

    private void processTransaction(
            final Account senderAccount, final double amountPlusCommission,
            final CommandHandler handler, final User senderUser, final String receiver) {
        if (senderAccount.getBalance() <= amountPlusCommission) {
            return;
        }
        handler.addTransactions(senderUser, senderAccount, transferSenderJson(
                senderAccount.getNumber(), receiver, senderAccount.getCurrency(), handler));
        senderAccount.setBalance(senderAccount.getBalance() - amountPlusCommission
                + handler.getBank().getCashback(senderAccount));
    }

    /**
     * Handles card payment process
     * @param card Card data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Bank bankData = handler.getBank();
        CommandInput commandData = handler.getCommand();

        Account account = handler.getBank().searchAccountByCard(card.getNumber());
        User user = bankData.searchUser(commandData.getEmail());

        double amount = commandData.getAmount();
        double amountPlusCommission;

        if (amount == 0) {
            return null;
        }

        //Exchange money if currencies of account and payment are different
        if (!commandData.getCurrency().equals(account.getCurrency())) {
            amount = bankData.exchange(
                    account.getCurrency(), commandData.getCurrency(), commandData.getAmount());
        }

        amountPlusCommission = bankData.getCommission(account, amount);
        String email = user.getUserInformation().getEmail();

        if (account.getType().equals("business")) {
            if (account.getUserPosition(email).isEmpty()) {
                return handler.cardNotFoundJson();
            }
            if (account.getUserPosition(email).equals("employee")) {
                if (amountPlusCommission > account.getSpendingLimit()) {
                    return null;
                }
            }
        } else if (!account.getOwner().getUserInformation().getEmail().equals(
                handler.getCommand().getEmail())) {
                return handler.cardNotFoundJson();
        }

        //Check if account doesn't have enough funds to pay
        if (account.getBalance() < amountPlusCommission) {
            handler.addTransactions(user, account,
                    account.insufficientFundsJson(commandData.getTimestamp()));
            return null;
        }

        if (account.getType().equals("business") && !account.getOwner().getUserInformation().
                getEmail().equals(handler.getCommand().getEmail())) {
            account.getExpenses().add(new Object[]{email, amount, commandData.getCommerciant()});
        }

        handler.addTransactions(user, account, card.paymentJson(amount,
                commandData.getCommerciant(), handler));

        double transactionAmount = bankData.exchange(
                "RON", account.getCurrency(), amountPlusCommission);
        if (transactionAmount >= MINIMUM_TRANSACTION_AMOUNT_FOR_GOLD_UPGRADE) {
            account.getOwner().setTransactionsOver300RON(user.getTransactionsOver300RON() + 1);
        }
        if (account.getOwner().getTransactionsOver300RON()
                >= MINIMUM_TRANSACTIONS_NUMBER_FOR_GOLD_UPGRADE
                && account.getOwner().getPlan().equals("silver")) {
            account.getOwner().setPlan("gold");
            account.getOwner().addTransaction(handler.planUpgradeSuccessfulJson(
                    "gold", account.getNumber(), commandData.getTimestamp()));
        }

        double cashback = bankData.getCashback(account);

        account.setBalance(account.getBalance() + cashback);
        account.setBalance(account.getBalance() - amountPlusCommission);

        if (card.isOneTime()) {

            Card newCard = new Card.Builder(account).oneTime().build();
            account.addCard(newCard);

            handler.addTransactions(user, account, card.cardDestroyedJson(handler));
            handler.addTransactions(user, account, newCard.cardCreatedJson(handler));
            account.getCards().remove(card);
        }

        return null;
    }

    private ObjectNode transferTemplateJson(final String senderAccount,
                                            final String receiverAccount, final String result,
                                            final CommandHandler handler) {
        ObjectNode transaction = new ObjectMapper().createObjectNode();
        transaction.put("timestamp", handler.getCommand().getTimestamp());
        transaction.put("description", handler.getCommand().getDescription());
        transaction.put("senderIBAN", senderAccount);
        transaction.put("receiverIBAN", receiverAccount);
        transaction.put("amount", result);
        return transaction;
    }

    private ObjectNode transferSenderJson(final String senderAccount, final String receiverAccount,
                                          final String senderCurrency,
                                          final CommandHandler handler) {
        String result = handler.getCommand().getAmount() + " " + senderCurrency;
        ObjectNode transaction =
                transferTemplateJson(senderAccount, receiverAccount, result, handler);
        transaction.put("transferType", "sent");
        return transaction;
    }

    private ObjectNode transferReceiverJson(final String senderAccount,
                                            final String receiverAccount,
                                            final String receiverCurrency,
                                            final double amount, final CommandHandler handler) {
        String result = amount + " " + receiverCurrency;
        ObjectNode transaction =
                transferTemplateJson(senderAccount, receiverAccount, result, handler);
        transaction.put("transferType", "received");
        return transaction;
    }

    private ObjectNode createCommonJson(final SplitPayment splitPayment) {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("currency", splitPayment.getSplitPaymentCurrency());
        json.put("description", "Split payment of " + String.format(
                "%.2f", splitPayment.getTotalAmount()) + " "
                + splitPayment.getSplitPaymentCurrency());
        json.set("involvedAccounts", createArrayNode(splitPayment.getAccounts()));
        json.put("splitPaymentType", splitPayment.getType());
        json.put("timestamp", splitPayment.getTimestamp());
        return json;
    }

    private ArrayNode createArrayNode(final List<String> list) {
        ArrayNode arrayNode = new ObjectMapper().createArrayNode();
        for (String item : list) {
            arrayNode.add(item);
        }
        return arrayNode;
    }

    private ArrayNode createArrayNodeFromDoubles(final List<Double> list) {
        ArrayNode arrayNode = new ObjectMapper().createArrayNode();
        for (Double item : list) {
            arrayNode.add(item);
        }
        return arrayNode;
    }

    /**
     * Creates JSON format output for split payment success
     * @param splitPayments List of split payments data for all involved accounts
     * @return Json ObjectNode for split payment success message output
     */
    public ObjectNode acceptSplitPaymentJson(final List<SplitPayment> splitPayments) {
        SplitPayment splitPayment = splitPayments.getFirst();
        if (splitPayment.getAmounts() == null) {
            return null;
        }

        ObjectNode json = createCommonJson(splitPayment);
        if (splitPayment.getType().equals("custom")) {
            json.set("amountForUsers", createArrayNodeFromDoubles(splitPayment.getAmounts()));
        } else {
            json.put("amount", splitPayment.getAmounts().getFirst());
        }
        return json;
    }

    /**
     * Creates JSON format output for split payment failure
     * @param splitPayments List of split payments data for all involved accounts
     * @param error Type of error that cause split payment failure
     * @return Json ObjectNode for split payment failure message output
     */
    public ObjectNode rejectSplitPaymentJson(final List<SplitPayment> splitPayments,
                                             final String error) {
        SplitPayment splitPayment = splitPayments.getFirst();
        if (splitPayment.getAmounts() == null) {
            return null;
        }

        ObjectNode json = createCommonJson(splitPayment);
        if (splitPayment.getType().equals("custom")) {
            json.set("amountForUsers", createArrayNodeFromDoubles(splitPayment.getAmounts()));
        } else {
            json.put("amount", splitPayment.getAmounts().getFirst());
        }

        if (error.equals("reject")) {
            json.put("error", "One user rejected the payment.");
        } else {
            json.put("error", "Account " + error + " has insufficient funds for a split payment.");
        }

        return json;
    }
}
