package org.example;

import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

public class MainFrame extends JFrame {
    private JTextArea textArea;
    private static TextAreaOutputStream textAreaOutputStream;

    public MainFrame() {
        setTitle("Exchange Rate Client");
        setSize(600, 180);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Initialize the TextAreaOutputStream
        textAreaOutputStream = new TextAreaOutputStream(textArea);

        // Redirect System.out and System.err to the text area
        PrintStream printStream = new PrintStream(textAreaOutputStream);
        System.setOut(printStream);
        System.setErr(printStream);
    }

    // Public method to write directly to the text area
    public void writeToTextArea(String message) {
        try {
            textAreaOutputStream.write(message.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TextAreaOutputStream extends OutputStream {
        private final JTextArea textArea;

        public TextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            // Start the Main class logic in a separate thread
            new Thread(() -> {
                Main loader = new Main();
                loader.argsChecker(args);
                String APIsource = loader.setApi();
                String appId = loader.setAPIID();
                String currency = loader.setCurrency();
                String DBURL = loader.setDBUrl();
                String Username = loader.setUsername();
                String FilePath = loader.setFilePath();
                String Password = loader.setPassword();
                String errorEmail = loader.setErrorEmail();
                String serverName = loader.setServerName();
                String labelName = loader.setLabelName();

                ExchangeRateClient client = new ExchangeRateClient(appId, currency, APIsource, DBURL, Username, Password, FilePath, errorEmail, serverName, labelName);
                System.out.println(labelName);
                System.out.println(serverName);
                System.out.println(Username);
                System.out.println(Password);
                System.out.println(DBURL);
                System.out.println(appId);

                System.out.println("Getting Exchange rate");
                JsonObject newRatesJson = client.getLatestExchangeRates();
                System.out.println("======");
                System.out.println("|| Getting previousRates ... ||");
                Map<String, Double> previousRates = client.fetchPreviousRates();
                System.out.println("======");
                System.out.println("|| Getting BaseCurrency ...  ||");
                String prevBaseCurrency = client.getPreviousBaseCurrency();
                client.updateDatabase(newRatesJson);

                if (newRatesJson != null) {
                    JsonObject newRates = newRatesJson;

                    client.compareRates(previousRates, newRates, prevBaseCurrency);

                    
                } else {
                    System.out.println("Failed to fetch new rates.");
                }

                // Start the scheduled task to periodically update rates
                client.startScheduledTask();
            }).start();
        });
    }
}
