package org.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExchangeRateClient {

    private static final String API_URL = "https://openexchangerates.org/api/latest.json";
    private static final String API_URL_FXR = "http://data.fixer.io/api/latest";
    private String appId = "57699a5e19cb391bab33a1b367b2eac5";
    private String curr = "";
    private String source = "";
    private  String Username= "";
    private  String DBURL= "";
    private  String Password= "";
    private  String Filepath= "";
    private String errorEmail = "";
    private String serverName = "";
    private String labelName;
    EmailSender SendEmail = new EmailSender();
    private static final int RETRY_INTERVAL = 5; // in minutes
    private static final int MAX_RETRIES = 4; // maximum number of retries
    private int retryCount = 0;
    private String previousBaseCurrency;
//    private static final String SNOWFLAKE_URL  = "jdbc:snowflake://kf93486.europe-west2.gcp.snowflakecomputing.com";
//    private static final String SFUSER = "username";
//    private static final String SFPASSWORD =  "password!";
private static final String WAREHOUSE = "TESTWAREHOUSE";
    private static final String DATABASE = "DEVOPS";
    private static final String SCHEMA = "DEV";

    public ExchangeRateClient(String appId, String curr, String source, String DBURL, String Username, String Password,String Filepath, String errorMail, String serverName,
    String labelName) {
        this.appId = appId;
        this.curr = curr;
        this.source = source;
        this.DBURL = DBURL;
        this.Username = Username;
        this.Password = Password;
        this.Filepath = Filepath;
        this.errorEmail = errorMail;
        this.serverName = serverName;
        this.labelName = labelName;
    }


    public JsonObject getLatestExchangeRates() {
        JsonObject jsonObject = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request;
            if (curr.isEmpty() && source.equals("OXR")) {
                request = new HttpGet(API_URL + "?app_id=" + appId);
            } else if(!curr.isEmpty() && source.equals("OXR") ) {

                request = new HttpGet(API_URL + "?app_id=" + appId + "&base=" + curr);
            } else if (curr.isEmpty() && source.equals("FXR")) {
                request = new HttpGet(API_URL_FXR + "?access_key=" + appId);
            } else if(!curr.isEmpty() && source.equals("FXR") ) {
                request = new HttpGet(API_URL_FXR + "?access_key=" + appId + "&base=" + curr);
            } else {
                request = new HttpGet(API_URL + "?app_id=" + appId + "&base=" + curr);
            }
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    jsonObject = JsonParser.parseString(result).getAsJsonObject();
                }
            } catch (Exception e) {
                e.printStackTrace();
                SendEmail.sendEmail(errorEmail, "Xrates error", String.valueOf(e));
            }
        } catch (Exception e) {
            e.printStackTrace();
            SendEmail.sendEmail(errorEmail, "Xrates error", String.valueOf(e));
        }
        return jsonObject;
    }

    public void updateDatabase(JsonObject jsonObject) {
        if (jsonObject == null) {
            System.out.println("No data to update the database.");
            SendEmail.sendEmail(errorEmail, "Xrates error", "No data to update the database.");
            return;
        }

        String sql = "INSERT INTO rates (curr, rate, base, feeddate, acttime, src, hour_loaded) VALUES (?, ?, ?, ?, ?, ?, ?)";

        JsonObject rates = jsonObject.getAsJsonObject("rates");
        String baseCurrency = jsonObject.get("base").getAsString();
        String apiSource = source;
        LocalDateTime feedDate = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime actTime = LocalDateTime.now(ZoneOffset.UTC);
        int hourLoaded = Date.from(feedDate.toInstant(ZoneOffset.UTC)).getHours();

        try {
            if (serverName.equalsIgnoreCase("MYSQL")) {
                updateMySQLDatabase(sql, rates, baseCurrency, apiSource, feedDate, actTime, hourLoaded);
            } else if (serverName.equalsIgnoreCase("SnowFlake")) {
                updateSnowflakeDatabase(sql, rates, baseCurrency, apiSource, feedDate, actTime, hourLoaded);
            } else {
                System.out.println("Unsupported server type: " + serverName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            SendEmail.sendEmail(errorEmail, "Xrates error", e.toString());
        }
    }

    private void updateMySQLDatabase(String sql, JsonObject rates, String baseCurrency, String apiSource,
                                     LocalDateTime feedDate, LocalDateTime actTime, int hourLoaded) throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + DBURL + ":3306/exchange_rates", Username, Password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            for (Map.Entry<String, JsonElement> entry : rates.entrySet()) {
                String currency = entry.getKey();
                double rate = entry.getValue().getAsNumber().doubleValue();

                preparedStatement.setString(1, currency);
                preparedStatement.setDouble(2, rate);
                preparedStatement.setString(3, baseCurrency);
                preparedStatement.setObject(4, Date.from(feedDate.toInstant(ZoneOffset.UTC)));
                preparedStatement.setObject(5, Date.from(actTime.toInstant(ZoneOffset.UTC)));
                preparedStatement.setString(6, apiSource);
                preparedStatement.setInt(7, hourLoaded);

                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            System.out.println("Exchange rates updated successfully in MySQL.");
        }
    }

    private void updateSnowflakeDatabase(String sql, JsonObject rates, String baseCurrency, String apiSource,
                                         LocalDateTime feedDate, LocalDateTime actTime, int hourLoaded) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBURL, Username, Password)) {
            // Set the appropriate warehouse, database, and schema
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("USE WAREHOUSE " + WAREHOUSE);
                stmt.execute("USE DATABASE " + DATABASE);
                stmt.execute("USE SCHEMA " + SCHEMA);
            }
            String modifiedSQL = sql.replace("!", "CONVERT_TIMEZONE('UTC', !)");

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


                Timestamp feedDateTimestamp = Timestamp.valueOf(feedDate.atZone(ZoneOffset.UTC).toLocalDateTime());
                Timestamp actTimeTimestamp = Timestamp.valueOf(actTime.atZone(ZoneOffset.UTC).toLocalDateTime());



                for (Map.Entry<String, JsonElement> entry : rates.entrySet()) {
                    String currency = entry.getKey();
                    double rate = entry.getValue().getAsNumber().doubleValue();

                    preparedStatement.setString(1, currency);
                    preparedStatement.setDouble(2, rate);
                    preparedStatement.setString(3, baseCurrency);
                    preparedStatement.setTimestamp(4, feedDateTimestamp);
                    preparedStatement.setTimestamp(5, actTimeTimestamp);
                    preparedStatement.setString(6, apiSource);
                    preparedStatement.setInt(7, hourLoaded);

                    preparedStatement.addBatch();

                }

                // Execute the batch insert
                preparedStatement.executeBatch();
                System.out.println("Exchange rates updated successfully in Snowflake.");
            }
        }
    }


    public void startScheduledTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            retryCount++;
            JsonObject rates = getLatestExchangeRates();
            if (rates == null || rates.has("error") ) {
                if (retryCount < MAX_RETRIES) {
                    System.out.println(rates);
                    System.out.println("Retrying in " + RETRY_INTERVAL + " minutes... Attempt " + retryCount + " of " + MAX_RETRIES);

                    SendEmail.sendEmail(errorEmail, "Xrates error", String.valueOf(rates));
                } else {
                    System.out.println("Max retries reached. Stopping retries.");
                    scheduler.shutdown(); // Stop further retries
                }
            } else {
                retryCount =0;
                System.out.println("Successfully fetched exchange rates: " + rates.toString());
                updateDatabase(rates);
                scheduler.shutdown();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                System.exit(0);

            }
        };
        scheduler.scheduleAtFixedRate(task, 0, RETRY_INTERVAL, TimeUnit.MINUTES);
    }

    public Map<String, Double> fetchPreviousRates() {
        Map<String, Double> previousRates = new HashMap<>();
        previousBaseCurrency = null;

        String sql = "SELECT curr, rate, base FROM rates WHERE acttime = " +
                "(SELECT MAX(acttime) FROM rates) ORDER BY curr ASC";

        try {
            if (serverName.equalsIgnoreCase("MYSQL")) {
                fetchRatesFromMySQL(sql, previousRates);
            } else if (serverName.equalsIgnoreCase("SnowFlake")) {
                fetchRatesFromSnowflake(sql, previousRates);
            } else {
                System.out.println("Unsupported server type: " + serverName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return previousRates;
    }

    private void fetchRatesFromMySQL(String sql, Map<String, Double> previousRates) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + DBURL + ":3306/exchange_rates", Username, Password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String currency = resultSet.getString("curr");
                double rate = resultSet.getDouble("rate");
                previousRates.put(currency, rate);

                if (previousBaseCurrency == null) {
                    previousBaseCurrency = resultSet.getString("base");
                }
            }
        }
    }

    private void fetchRatesFromSnowflake(String sql, Map<String, Double> previousRates) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBURL, Username, Password)) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("USE WAREHOUSE " + WAREHOUSE);
                stmt.execute("USE DATABASE " + DATABASE);
                stmt.execute("USE SCHEMA " + SCHEMA);
            }

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String currency = resultSet.getString("CURR");
                    double rate = resultSet.getDouble("RATE");
                    previousRates.put(currency, rate);

                    if (previousBaseCurrency == null) {
                        previousBaseCurrency = resultSet.getString("BASE");
                    }
                }
            }
        }
    }


    public String getPreviousBaseCurrency() {
        return previousBaseCurrency;
    }


    public void compareRates(Map<String, Double> previousRates, JsonObject newRates, String previousBaseCurrency) {

        if (newRates == null) {
            System.out.println("New rates data is null.");
            SendEmail.sendEmail(errorEmail, "Xrates error", "New rates data is null.");
            return;
        }

        JsonObject ratesObject = newRates.getAsJsonObject("rates");
        if (ratesObject == null) {
            System.out.println("New rates data does not contain 'rates' key.");
            SendEmail.sendEmail(errorEmail, "Xrates error", "New rates data does not contain 'rates' key.");
            return;
        }

        // Retrieve the base currency from the previous rates map
        if (previousBaseCurrency == null) {
            System.out.println("Previous base currency is missing.");
            SendEmail.sendEmail(errorEmail, "Xrates error", "Previous base currency is missing.");
            return;
        }

        String newBaseCurrency = newRates.get("base").getAsString();
        if (!previousBaseCurrency.equals(newBaseCurrency)) {
            System.out.println("====");
            System.out.println("|| " + previousBaseCurrency + " ||");
            System.out.println("====");
            System.out.println("|| " +newBaseCurrency + " ||");
            System.out.println("====");
            System.out.println("Base currencies do not match. Skipping validation comparison.");
            return;
        }

        Set<String> previousCurrencies = previousRates.keySet();
        Set<String> newCurrencies = new HashSet<>();
        List<String> fluctuationRates = new ArrayList<>();
        List<String> staleRates = new ArrayList<>();
        Set<String> missingRates = new HashSet<>();

        LocalDateTime feedDate = LocalDateTime.now(ZoneOffset.UTC);

        // Extract currencies from newRates
        for (Map.Entry<String, JsonElement> entry : ratesObject.entrySet()) {
            String currency = entry.getKey();
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                newCurrencies.add(currency);
            }
        }

        // Check for fluctuations and stale rates
        for (String currency : newCurrencies) {
            if (!currency.equals("BASE_CURRENCY")) {
                double newRate = ratesObject.get(currency).getAsDouble();
                if (previousRates.containsKey(currency)) {
                    double oldRate = previousRates.get(currency);
                    if (Math.abs(newRate - oldRate) / oldRate > 0.05) {
                        fluctuationRates.add(currency);
                    }
                }

                // Example check for stale rates (1 hour threshold)
                if (feedDate.minusHours(1).isAfter(feedDate)) {
                    staleRates.add(currency);
                }
            }
        }

        missingRates.clear();
        for (String currency : previousCurrencies) {
            if (!newCurrencies.contains(currency) && !currency.equals("BASE_CURRENCY")) {
                missingRates.add(currency);
            }
        }

        if (!missingRates.isEmpty() || !fluctuationRates.isEmpty() || !staleRates.isEmpty()) {
            System.out.println("Missing Rates: " + missingRates);
            System.out.println("Fluctuation Rates: " + fluctuationRates);
            System.out.println("Stale Rates: " + staleRates);
            SendEmail.sendEmail(errorEmail, "Xrates Report",
                    "Stale Rates: " + staleRates + "\n" +
                            "Fluctuation Rates: " + fluctuationRates + "\n" +
                            "Missing Rates: " + missingRates);
        }
    }


}