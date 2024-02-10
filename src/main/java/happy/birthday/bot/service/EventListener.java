package happy.birthday.bot.service;

;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import happy.birthday.bot.model.JsonObject;
import happy.birthday.bot.model.SharedData;
import happy.birthday.bot.model.Signal;
import happy.birthday.bot.model.Swap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Service
@Getter
@Slf4j
public class EventListener {
    @Value("${okx.secret.key}")
    private String OKX_SECRET_KEY;
    @Value("${okx.api.key}")
    private String OKX_API_KEY;
    // private static final String API_GENERAL_INFO = "/api/v5/public/instruments?instType=SPOT";
    private static final String DOMAIN_NAME = "https://okx.com";
    private static final String API_GENERAL_INFO = "/api/v5/market/tickers?instType=SWAP";
    @Value("${okx.passphrase}")
    private String PASSPHRASE;
    private static final String GET = "GET";
    private static final String OK_ACCESS_KEY = "OK-ACCESS-KEY";
    private static final String OK_ACCESS_SIGN = "OK-ACCESS-SIGN";
    private static final String OK_ACCESS_TIMESTAMP = "OK-ACCESS-TIMESTAMP";
    private static final String OK_ACCESS_PASSPHRASE = "OK-ACCESS-PASSPHRASE";
    private List<Signal> userSignals = new ArrayList<>();
    private volatile boolean isOn = true;
    private final ObjectMapper objectMapper;
    private final SharedData sharedData;

    public EventListener(ObjectMapper objectMapper, SharedData sharedData) {
        this.objectMapper = objectMapper;
        this.sharedData = sharedData;
    }

    public HttpURLConnection setupConnection() {
        try {
            URL url = new URL(DOMAIN_NAME + API_GENERAL_INFO);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String utcTimestamp = getUtcTimestamp();
            String signature = utcTimestamp +
                    GET +
                    API_GENERAL_INFO +
                    OKX_SECRET_KEY;
            connection.setRequestMethod(GET);
            connection.setRequestProperty(OK_ACCESS_KEY, OKX_API_KEY);
            connection.setRequestProperty(OK_ACCESS_SIGN, signature);
            connection.setRequestProperty(OK_ACCESS_TIMESTAMP, utcTimestamp);
            connection.setRequestProperty(OK_ACCESS_PASSPHRASE, PASSPHRASE);
            return connection;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static String getUtcTimestamp() {
        Instant instant = Instant.now();
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public void monitorApiData() throws IOException {
          //  log.info("METHOD: monitorApiData started");
            HttpURLConnection connection = setupConnection();
          //  log.info("Connection created code {}", connection.getResponseCode());
         //   log.info("USERSIGNALS size: {}", sharedData.getUserSignalsSizeWithLock());

        sharedData.setExtractedObjectsWithLock(getJsonObjectListFromApi(connection));
        List<JsonObject> extractedObjectsCopy = sharedData.getExtractedObjectsCopyWithLock();
        if (!sharedData.getUserSignals().isEmpty()) {
            extractedObjectsCopy.forEach(jsonObject -> {
                Swap swap = (Swap) jsonObject;          //todo что если у нас не только SWAP?
                Signal matchedSignal = null;
                List<Signal> userSignalsCopy = sharedData.getUserSignalsCopyWithLock();
                for (Signal s : userSignalsCopy
                ) {
                    if (s.isLong() && swap.instId().equals(s.ticker()) && swap.last() >= s.price()) {
                            matchedSignal = s;
                            sharedData.addMatchedSignalsWithLock(s);
                            log.info("Signal removed IN LONG userSignals size: {}", userSignalsCopy.size());
                    } else if ((!s.isLong()) && swap.instId().equals(s.ticker()) && swap.last() <= s.price()) {
                            matchedSignal = s;
                            sharedData.addMatchedSignalsWithLock(s);
                            log.info("Signal removed in SHORT userSignals size: {}", userSignals.size());
                    }
                }
                sharedData.removeFromUserSignalsWithLock(matchedSignal);
            });
            try {

               // log.info("{} WAKES UP", Thread.currentThread().getName());
            } catch (Exception e) {
                log.error("Wake up error", e);
                throw new RuntimeException(e);
            }
        }


    }


    public List<JsonObject> getJsonObjectListFromApi(HttpURLConnection connection) throws IOException {
      //  log.info("METHOD: getJsonObjectListFromApi starts.");
        String line = "";

        int respCode = connection.getResponseCode();
        if (respCode != 200) {
            throw new RuntimeException("ERROR: httpResponse: " + respCode);
        } else {
            try (InputStream is = connection.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

                StringBuilder sb = new StringBuilder();
                while (br.ready()) {
                    sb.append(br.readLine());
                    line = sb.toString();
                   // log.info("STRING from stream: {}", line);
                }
            }catch (Exception e) {
                log.error("Error reading from stream {}", e.getMessage());
            }
         //   System.out.println("LINE: " + line);

            ArrayList<JsonObject> extractedObjects = new ArrayList<>();
            try {
                JsonNode rootNode = objectMapper.readTree(line);
                JsonNode dataArrayNode = rootNode.path("data");
             //   System.out.println(dataArrayNode.asText());
                if (dataArrayNode.isArray()) {
                    for (JsonNode node : dataArrayNode
                    ) {
                        Swap swap = objectMapper.treeToValue(node, Swap.class);
                     //   if(swap.instId().contains("BTC-USDT")) System.out.println(swap); // TODO delete later
                        extractedObjects.add(swap);
                    }
                } else {
                    String string = dataArrayNode.elements().toString();
                    System.out.println("THIS " + string);
                    System.out.println("'data' is not a JSON array.");

                }
               // log.info("EXTRACTED FROM API size: {}", extractedObjects.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

          //  log.info("METHOD getJsonObjectListFromApi ends.");
            return extractedObjects;
        }
    }




}
