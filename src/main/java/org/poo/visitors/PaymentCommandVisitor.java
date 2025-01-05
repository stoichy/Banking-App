package org.poo.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.poo.fileio.CommandInput;
import org.poo.handlers.CommandHandler;
import org.poo.main.Account;
import org.poo.main.Bank;
import org.poo.main.Card;
import org.poo.main.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PaymentCommandVisitor implements CommandVisitorInterface {

    /**
     * Handles split payment process between accounts
     * @param bank Bank data for access to accounts
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Bank bank, final CommandHandler handler) {
        Account account;
        double amount;
        int numberOfAccounts = handler.getCommand().getAccounts().size();

        //Check if each account has enough funds for split payment
        for (int i = numberOfAccounts - 1; i >= 0; i--) {
            String accountNumber = handler.getCommand().getAccounts().get(i);
            account = bank.searchAccount(accountNumber);
            amount = handler.getCommand().getAmount();

            if (account == null) {
                return null;
            }

            if (!account.getCurrency().equals(handler.getCommand().getCurrency())) {
                amount = handler.getBank().exchange(account.getCurrency(),
                        handler.getCommand().getCurrency(), handler.getCommand().getAmount());
            }

            amount /= handler.getCommand().getAccounts().size();

            if (account.getBalance() < amount) {
                for (String accountNumberCopy : handler.getCommand().getAccounts()) {
                    Account involvedAccount = bank.searchAccount(accountNumberCopy);
                    if (involvedAccount != null) {
                        User user = involvedAccount.getOwner();
                        ObjectNode transaction = insufficientFundsForSplitJson(
                                handler.getCommand(), account.getNumber());
                        handler.addTransactions(user, involvedAccount, transaction);
                    }
                }
                return null;
            }
        }

        //Add transaction to each user and account
        for (String accountNumber : handler.getCommand().getAccounts()) {
            account = bank.searchAccount(accountNumber);
            User user = account.getOwner();
            amount = handler.getCommand().getAmount();

            if (!account.getCurrency().equals(handler.getCommand().getCurrency())) {
                amount = handler.getBank().exchange(account.getCurrency(),
                        handler.getCommand().getCurrency(), handler.getCommand().getAmount());
            }

            account.setBalance(account.getBalance() - amount / numberOfAccounts);

            ObjectNode transaction = splitPaymentJson(handler.getCommand());
            handler.addTransactions(user, account, transaction);
        }

        return null;
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
        CommandInput command = handler.getCommand();
        Account receiverAccount = handler.getBank().searchAccount(command.getReceiver());
        User senderUser = senderAccount.getOwner();
        double amount = command.getAmount();

        if (receiverAccount == null) {
            return handler.userNotFoundJson();
        }

        //Check if sender has alias and return null if true (error)
        if (handler.getCommand().getAccount().equals(senderAccount.getAlias())) {
            return null;
        }

        //Account doesn't have enough money to send
        if (senderAccount.getBalance() < amount) {
            handler.addTransactions(senderUser,
                                    senderAccount,
                                    senderAccount.insufficientFundsJson(
                                            handler.getCommand().getTimestamp()));
            return null;
        }

        User receiverUser = receiverAccount.getOwner();

        //Check if currencies of accounts are different and exchange if true
        if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            handler.getCommand().setCurrency(senderAccount.getCurrency());
            amount = handler.getBank().exchange(receiverAccount.getCurrency(),
                    handler.getCommand().getCurrency(), handler.getCommand().getAmount());
        }

        handler.addTransactions(senderUser, senderAccount,
                                transferSenderJson(senderAccount, receiverAccount, handler));

        handler.addTransactions(receiverUser, receiverAccount,
                                transferReceiverJson
                                        (senderAccount, receiverAccount, amount, handler));

        receiverAccount.setBalance(receiverAccount.getBalance() + amount);

        senderAccount.setBalance(senderAccount.getBalance() -
                handler.getBank().commission
                        (handler.getBank().searchUserByAccount(senderAccount.getNumber()),
                                handler.getCommand().getAmount()));
        senderAccount.setBalance
                (senderAccount.getBalance() + handler.getBank().getCashback(senderAccount));

        return null;
    }

    /**
     * Handles card payment process
     * @param card Card data
     * @param handler Command data
     * @return Null, no output is needed
     */
    public ObjectNode visit(final Card card, final CommandHandler handler) {
        Account account = card.getAccount();
        User user = handler.getBank().
                searchUser(handler.getCommand().getEmail());
        double amount = handler.getCommand().getAmount();
        double amountPlusCommission;

        if (user == null){
            return null;
        }

        //Check if card is frozen
        if (card.getFrozen()) {
            handler.addTransactions(
                    user, account, card.isFrozenJson(handler.getCommand().getTimestamp()));
            return null;
        }

        //Exchange money if currencies of account and payment are different
        if (!handler.getCommand().getCurrency().equals(account.getCurrency())) {
            amount = handler.getBank().exchange(account.getCurrency(),
                    handler.getCommand().getCurrency(), handler.getCommand().getAmount());
        }

        //Check if account doesn't have enough funds to pay
        if (account.getBalance() < amount) {
            handler.addTransactions(
                    user,
                    account,
                    account.insufficientFundsJson(handler.getCommand().getTimestamp()));
            return null;
        }

        BigDecimal roundedAmount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        handler.addTransactions(user, account, card.paymentJson(roundedAmount.doubleValue(),
                handler.getCommand().getCommerciant(),
                handler));

        amountPlusCommission = handler.getBank().commission(user, amount);
        if (handler.getCommand().getTimestamp() == 13) {
            System.out.println(amountPlusCommission);
        }
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

    private ObjectNode transferTemplateJson(
            final Account senderAccount,
            final Account receiverAccount,
            final String result,
            final CommandHandler handler) {
        ObjectNode transaction = new ObjectMapper().createObjectNode();
        transaction.put("timestamp", handler.getCommand().getTimestamp());
        transaction.put("description", handler.getCommand().getDescription());
        transaction.put("senderIBAN", senderAccount.getNumber());
        transaction.put("receiverIBAN", receiverAccount.getNumber());
        transaction.put("amount", result);
        return transaction;
    }

    private ObjectNode transferSenderJson(final Account senderAccount,
                                          final Account receiverAccount,
                                          final CommandHandler handler) {
        String result = handler.getCommand().getAmount() + " " + senderAccount.getCurrency();
        ObjectNode transaction =
                transferTemplateJson(senderAccount, receiverAccount, result, handler);
        transaction.put("transferType", "sent");
        return transaction;
    }

    private ObjectNode transferReceiverJson(
            final Account senderAccount,
            final Account receiverAccount,
            final double amount,
            final CommandHandler handler) {
        String result = amount + " " + receiverAccount.getCurrency();
        ObjectNode transaction =
                transferTemplateJson(senderAccount, receiverAccount, result, handler);
        transaction.put("transferType", "received");
        return transaction;
    }

    private ObjectNode paymentTemplateJson(
            final CommandInput handler, final String insufficientFundsAccount) {
        double amount = handler.getAmount();
        String currency = handler.getCurrency();
        List<String> accounts = handler.getAccounts();

        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("amount",
                amount / accounts.size());
        output.put("currency", currency);
        output.put("description",
                "Split payment of " + String.format("%.2f", amount) + " " + currency);
        ArrayNode accountsJson = new ObjectMapper().createArrayNode();
        for (String account : accounts) {
            accountsJson.add(account);
        }
        if (insufficientFundsAccount != null) {
            output.put("error", "Account " + insufficientFundsAccount
                    + " has insufficient funds for a split payment.");
        }
        output.set("involvedAccounts", accountsJson);
        output.put("timestamp", handler.getTimestamp());
        return output;
    }

    private ObjectNode splitPaymentJson(final CommandInput handler) {
        return paymentTemplateJson(handler, null);
    }

    private ObjectNode insufficientFundsForSplitJson(
            final CommandInput splitInformation, final String account) {
        return paymentTemplateJson(splitInformation, account);

    }
}
