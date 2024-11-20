package org.example;

import java.io.*;

public class Main {

    private static String configFile = "";
    private static String baseCurr = "";
    private static String apiName = "OXR";
    private static String Username = "";
    private static String errorEmail = "ops@exchange-data.com";
    private static String DBURL = "";
    private static String Password = "";
    private static String APIID = "";
    private static String serverName = "";
    private static String labelName = "";
    private MainFrame mainFrame;
    EmailSender SendEmail = new EmailSender();

    public void argsChecker(String[] args) {
        for (int f = 0; f < args.length; f++) {
            if (args[f].equalsIgnoreCase("-cfg")) {
                configFile = args[f + 1];
                isFileDir(configFile);
                getDBDetails(configFile);
            } else if (args[f].equalsIgnoreCase("-cur")) {
                baseCurr = args[f + 1];
            } else if (args[f].equalsIgnoreCase("-API")) {
                apiName = args[f + 1];
            } else if (args[f].equalsIgnoreCase("-SVR")) {
                labelName = args[f + 1];
            }
        }
    }


    private boolean isFileDir(String path) {
        File file = new File(path);
        if (file.isFile() || file.isDirectory()) {
            return true;
        } else {
            onfail("Path - " + path + " - does not exist");
            return false;
        }
    }

    private void onfail(String error) {
        System.out.println(error);
        SendEmail.sendEmail(errorEmail, "Xrates error", String.valueOf(error));
    }


    private void getDBDetails(String filePath) {
        File sourcefile = new File(filePath);
        if (!sourcefile.exists()) {
            System.out.println("Configuration file does not exist: " + filePath);
            return;
        }

        try (BufferedReader in = new BufferedReader(new FileReader(sourcefile))) {
            String str;
            // Read the file line by line
            while ((str = in.readLine()) != null) {
                String[] DBdetails = str.split(" ");


                if (DBdetails.length < 7) {

                    continue;
                }

                if (DBdetails[0].equals(labelName)) {
                     if(DBdetails[6].equals(apiName)) {
                    labelName = DBdetails[0].trim();
                    serverName = DBdetails[1].trim();
                    DBURL = DBdetails[2].trim();
                    Username = DBdetails[3].trim();
                    Password = DBdetails[4].trim();
                    APIID = DBdetails[5].trim();
                    apiName = DBdetails[6].trim();

                }
               }



            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String setCurrency() {
        return baseCurr;
    }

    public String setApi() {
        return apiName;
    }

    public String setFilePath() {
        return configFile;
    }

    public String setAPIID() {
        return APIID;
    }

    public String setDBUrl() {
        return DBURL;
    }

    public String setPassword() {
        return Password;
    }

    public String setUsername() {
        return Username;
    }

    public String setErrorEmail() {
        return errorEmail;
    }

    public String setServerName() {
        return serverName;
    }

    public String setLabelName(){
        return labelName;
    }

    public void SomeOtherClass(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
}


