package happy.birthday.bot.service;

import com.vdurmont.emoji.EmojiParser;
import happy.birthday.bot.model.JsonObject;
import happy.birthday.bot.model.SharedData;
import happy.birthday.bot.model.Signal;
import happy.birthday.bot.model.Swap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramBot extends TelegramLongPollingBot {
    private String name;
    final Long botOwner;
    static final String HELP_BODY = """
            Бот отслеживает котировки в режиме реального времени и пришлет оповещение, если вы заранее его добавили.\s
                Посмотреть видео-инструкцию с примерами запросов можно на ютюбе.
                Я могу прислать примеры запросов здесь в чате, для этого в ответ на это сообщение напишите /examples
            """;
    static final String HELP_INFO = "shows help message";
    static final String SET_SIGNAL_INFO = """
            example: BTC/USDT 43000 message(optional)
            """;
    static final String CONFIRM_BUTTON = "CONFIRM_BUTTON";
    static final String DENY_BUTTON = "DENY_BUTTON";
    static final String ERROR_OCCURRED = "Error occurred: ";
    static final long ONE_ORDER = 1;
    static final long ALl_ORDERS = -1;
    static final String SWAP = "SWAP";
    static final String CONFIRMATION_INSTRUCTION = "instruction";
    static final String USER_STRING = "user";
    public static Map<String, Long> instructionsForConfirmationButtons;
    public static Signal signalToCancel;
    public static String defaultStableCoin = "USDT";
    public static String defaultFiat = "USD";
    public static String userTickerPart = "";
    public static String cryptoTicker = userTickerPart + defaultStableCoin;
    private final EventListener eventListener;
    private final Lock sharedLock;
    private final SharedData sharedData;
    @Getter
    public static List<Signal> matchedSignals;

    @Getter
    public static int counter = 0;

    // TODO настраиваем settings/edit
    @Autowired
    public TelegramBot(String token, String name, Long botOwner, EventListener eventListener, Lock sharedLock, SharedData sharedData) {
        super(token);
        this.name = name;
        this.botOwner = botOwner;
        this.eventListener = eventListener;
        this.sharedLock = sharedLock;
        this.sharedData = sharedData;
        instructionsForConfirmationButtons = new HashMap<>();
        List<BotCommand> botCommandsList = new ArrayList<>();
        matchedSignals = new ArrayList<>();
        botCommandsList.add(new BotCommand("start", "get a welcome message"));
        /*botCommandsList.add(new BotCommand("mydata", "get my stored data"));
        botCommandsList.add(new BotCommand("help", HELP_INFO));
        botCommandsList.add(new BotCommand("edit", "edit"));*/
        try {
            execute(new SetMyCommands(botCommandsList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        String messageText;
        if (update.hasMessage() && update.getMessage().hasText()) {
            messageText = update.getMessage().getText().toLowerCase();
            Long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                String name = update.getMessage().getChat().getFirstName();
                startMessageReceived(chatId, name);
                System.out.println("Owner " + botOwner + "bot name " + this.name);
            } else if (messageText.equals("help")) {
                configureAndSendMessage(chatId, HELP_BODY);
            } else if (messageText.equals("mydata")) {
                String answer = getUserDataString(update);
                sendMessage(chatId, answer);
            } else if (messageText.startsWith("sig")) {
                Signal userSignal = processTextSignal(chatId, messageText);
                System.out.println("********************" + userSignal);
                log.info("SIGNAL CREATED DETAILS: {}", userSignal);
                if (!userSignal.ticker().equalsIgnoreCase("error")) {
                    try {
                        sharedLock.lock();
                        //eventListener.getUserSignals().add(userSignal);
                        sharedData.addUserSignalsWithLock(userSignal);
                    } finally {
                        sharedLock.unlock();
                    }
                }
                setSignalReceived(chatId, userSignal);
            } else if (messageText.startsWith("cancel_one")) {
                signalToCancel = processTextSignal(chatId, messageText);
                setUpConfirmationBeforeCancellation(chatId, signalToCancel, instructionsForConfirmationButtons, ONE_ORDER);
            } else if (messageText.startsWith("cancel_pos")) {
                int index = Integer.parseInt(extractLastWord(messageText)) - 1;
                    if (index < sharedData.getUserSignalsSizeWithLock()) {
                        signalToCancel = sharedData.getUserSignalsIndexWithLock(index);
                        setUpConfirmationBeforeCancellation(chatId, signalToCancel, instructionsForConfirmationButtons, ONE_ORDER);
                    } else sendMessage(chatId, "There is no such signal");
            } else if (messageText.equals("cancel_all")) {
                setUpConfirmationBeforeCancellation(chatId, new Signal("s", 12.2, 0.0,"", 1, counter++), instructionsForConfirmationButtons, ALl_ORDERS);
                System.out.println("********** cancel all **********");
            } else if (messageText.startsWith("set default stable")) {
                defaultStableCoin = extractLastWord(messageText).toUpperCase();
                System.out.println(defaultStableCoin);
            } else if (messageText.equals("default coin")) {
                configureAndSendMessage(chatId, "Default stable coin is: " + defaultStableCoin);
            } else if (messageText.equals("default fiat")) {
                configureAndSendMessage(chatId, "Default fiat currency is: " + defaultFiat);
            } else if (messageText.equals("edit")) {
                    sharedLock.lock();
                    String message = getListOrdersString(sharedData.getUserSignalsCopyWithLock());
                    configureAndSendMessage(chatId, message);
            } else if (messageText.equals("test")) {
                String message = sharedData.getUserSignalsIndexWithLock(0).toString();
                configureAndSendMessage(chatId, message);
            } else {
                configureAndSendMessage(chatId, "Sorry, this command is not recognized");
            }


        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();


            if (data.equals("CONFIRM_BUTTON")) {
                Long l = instructionsForConfirmationButtons.get(CONFIRMATION_INSTRUCTION);
                String text = "";
                if (l == ONE_ORDER) {
                    sharedData.removeFromUserSignalsWithLock(signalToCancel);
                    text = "The order has been cancelled";
                    signalToCancel = null;
                } else if (l == ALl_ORDERS) {
                    sharedData.cancelAllOrders();
                    text = "All orders have been cancelled";
                }
                executeEditMessageText(chatId, text, messageId);

            } else if (data.equals("DENY_BUTTON")) {
                String text = "Cancellation denied";
                executeEditMessageText(chatId, text, messageId);

            }
        }
    }

    private static String getUserDataString(Update update) {
        User user = update.getMessage().getFrom();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Id: ").append(user.getId()).append("\n")
                .append("Username: ").append(user.getUserName()).append("\n")
                .append("Firstname: ").append(user.getFirstName()).append("\n")
                .append("Lastname: ").append(user.getLastName()).append("\n");
        return stringBuilder.toString();
    }

    private String getListOrdersString(List<Signal> signals) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("There are ")
                .append(signals.size())
                .append(" currently active signals.\n");
        for (int i = 0; i < signals.size(); i++) {
            Signal signal = signals.get(i);
            stringBuilder
                    .append(i + 1)
                    .append(". ")
                    .append(signal.ticker())
                    .append(" ")
                    .append(signal.price())
                    .append(" ")
                    .append(signal.message())
                    .append("\n");
        }
        stringBuilder.append("To cancel a signal type: cancel_pos <your_number> \n");
        return stringBuilder.toString();
    }

    private static String extractLastWord(String messageText) {
        String[] tokens = messageText.split("\\s+");
        return tokens[tokens.length - 1];
    }

    private void setSignalReceived(Long chatId, Signal signal) {
        String answer;
        if (signal.ticker().equalsIgnoreCase("error")) {
            answer = signal.message();
        } else
            answer = "Signal: " + signal.ticker().toUpperCase() + " " + signal.price() + " has been created successfully.";
        sendMessage(chatId, answer);
        log.info("replied on /sig to user: " + name);
    }

    private Signal processTextSignal(Long chatId, String messageText) {
        log.info("METHOD: processTextSignal started");
        boolean isValid = false;
        String[] array = messageText.split("\\s+");
        ArrayList<String> params = new ArrayList<>();
        for (String p : array) {
            if (p.toUpperCase().equals(defaultStableCoin)) continue;
            params.add(p);
            System.out.println(p);
        }
        String ticker = "";
        double price = -1;
        String message = "";

        double current = 0;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < params.size(); i++) {

                switch (i) {
                    case 1:
                        ticker = sb
                                .append(params.get(i).toUpperCase())
                                .append("-")
                                .append(defaultStableCoin)
                                .append("-")
                                .append(SWAP)
                                .toString();
                        sb.setLength(0);
                        break;
                    case 2:
                        price = Double.parseDouble(params.get(i));
                        break;
                }
                if (i > 2) {
                    sb.append(" ").append(params.get(i));
                }
            }
            message = sb.toString();
            try {
                sharedLock.lock();
                log.info("LOCK for current TAKEN in {}", Thread.currentThread().getName());
                List<JsonObject> extractedObjects = eventListener.getExtractedObjects();
                log.info("EXTRACTED SIZE: {}", extractedObjects.size());
                for (JsonObject o : extractedObjects
                ) {
                    Swap swap = (Swap) o;
                    if (swap.instId().contains(ticker)) {
                        current = swap.last();
                        log.info("Current value is {}", current);
                        isValid = true;
                        log.info("SWAP last = {}, isValid = {}", current, isValid);
                    }
                }
            } finally {
                sharedLock.unlock();
                log.info("LOCK for current RELEASED");
            }

// todo here!!!!!
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            return getErrorSignal();
        }
        if (ticker.isEmpty() || price < 0 || current < 0 || !isValid) {
            log.error("METHOD: processTextSignal. Error signal was created  ticker:{}, price:{}, current:{}, isValid:{}", ticker, price, current, isValid);
            return getErrorSignal();
        }
        if (counter > 1000) counter = 0;
        return new Signal(ticker, price, current, message, chatId, ++counter);
    }

    private static Signal getErrorSignal() {
        String error = "You have entered incorrect data. Please try again.";
        return new Signal("Error", 0.0, 0.0, error, 0, 123);
    }

    private void setUpConfirmationBeforeCancellation(Long chatId, Signal signal, Map<String, Long> instructionsForConfirmationButtons, Long numberOrders) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (numberOrders == 1) {
            instructionsForConfirmationButtons.put(CONFIRMATION_INSTRUCTION, numberOrders);
            instructionsForConfirmationButtons.put(USER_STRING, chatId);
            message.setText("Are you sure you want to cancel order " + signal.ticker() + " " + signal.price() + "?");
        } else if (numberOrders == -1) {
            instructionsForConfirmationButtons.put(CONFIRMATION_INSTRUCTION, numberOrders);
            instructionsForConfirmationButtons.put(USER_STRING, chatId);
            message.setText("Are you sure you want to cancel all orders?");
        }
        InlineKeyboardMarkup inlineMarkup = getInlineKeyboardMarkup();
        message.setReplyMarkup(inlineMarkup);
        executeMessage(message);


    }

    private static InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("Confirm");
        confirmButton.setCallbackData(CONFIRM_BUTTON);

        InlineKeyboardButton denyButton = new InlineKeyboardButton();
        denyButton.setText("Deny");
        denyButton.setCallbackData(DENY_BUTTON);

        buttons.add(confirmButton);
        buttons.add(denyButton);

        keyboard.add(buttons);

        inlineMarkup.setKeyboard(keyboard);
        return inlineMarkup;
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }


    /*private void registerUser(Message message) {
        if(userRepository.findById(message.getChatId()).isEmpty()) {
            Chat chat = message.getChat();

            User user = new User();
            user.setId(message.getChatId());
            user.setName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setNick(chat.getUserName());
            user.setRegistered(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            log.info(user + " has been registered");
        }
    }*/

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return name;
    }


    @Override
    public void onRegister() {
        super.onRegister();
    }

    private void startMessageReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hello " + name + ", sport is good!" + ":muscle:");
        sendMessage(chatId, answer);
        log.info("replied on /start to user: " + name);

    }

    private void configureAndSendMessage(long chatId, String messageToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageToSend);
        executeMessage(message);

    }

    public void sendMessage(long chatId, String messageToSend) {
        SendMessage message = new SendMessage(String.valueOf(chatId), messageToSend);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add(new KeyboardButton("cancel_all"));
        row.add(new KeyboardButton("help"));
        row.add(new KeyboardButton("mydata"));
        rowList.add(row);

        row = new KeyboardRow();

        row.add(new KeyboardButton("default coin"));
        row.add(new KeyboardButton("default fiat"));
        row.add(new KeyboardButton("edit"));
        rowList.add(row);

        /*row = new KeyboardRow();

        row.add(new KeyboardButton("param"));
        row.add(new KeyboardButton("empty"));
        row.add(new KeyboardButton("empty"));
        rowList.add(row);*/

        markup.setKeyboard(rowList);

        message.setReplyMarkup(markup);


        executeMessage(message);

    }

    private void executeEditMessageText(long chatId, String text, int messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId(messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }

    public void monitorSignalMatchFlag() {
        while (true) {
            if (!matchedSignals.isEmpty()) {
                try {
                    sharedLock.lock();
                    Signal signal = matchedSignals.remove(0);
                    StringBuilder sb = new StringBuilder();
                    sb
                            .append("SIGNAL MATCH!!!\n")
                            .append(signal.ticker()).append("\n")
                            .append(signal.price()).append("\n")
                            .append(signal.message());
                    sendMessage(signal.user(), sb.toString());

                } finally {
                    sharedLock.unlock();
                }
            }
            try {
                Thread.sleep(500);
                //log.info("While woke up matched size: {}", matchedSignals.size());
                log.info("monitor flag in {} is working", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
