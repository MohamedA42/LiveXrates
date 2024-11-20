Xrates

Overview
Xrates is a robust Java-based application designed to provide real-time currency exchange rates. In addition to fetching current exchange rates, Xrates includes comprehensive data validation features. It compares the retrieved data against previous data points to identify any missing, stale, or significantly fluctuating currency rates. If such inconsistencies are detected, the system automatically sends an alert email to `ops@exchange-data.com`, detailing the detected issues.

Key Features
- Real-time Exchange Rates: Fetches up-to-date exchange rate data from selected APIs.
- Data Validation: Automatically checks for missing, stale, or unusually fluctuating currency rates.
- Automated Alerts: Sends email notifications to operational teams when data anomalies are detected, ensuring prompt action.
- Customizable Base Currency: Users can specify their preferred base currency when retrieving rates.
- API Flexibility: Supports multiple APIs, including Open Exchange Rates (OXR) and Fixer (FXR).

Getting Started

Prerequisites
Before running the Xrates application, ensure you have the following:
- Java Development Kit (JDK): Version 8 or later.
- API Key(s): Valid API keys for the exchange rate services (OXR or FXR).
- Configuration File: A properly configured file specifying necessary parameters.

 Building and Running the Application

To build and run the project from the command line:

1. Navigate to the Distribution Folder:

   cd /path/to/your/project/out/xrates_jar
   ```

2. Run the Application:
   The application requires specific command-line parameters to function correctly:
	
   Xrate.jar -SVR [server tag] -API [API_NAME] -cfg [CONFIG_FILE_PATH] -cur [BASE_CURRENCY]

   - -SVR: the name of the server you would like to run.
   - -API: Specifies the API service to use. Options are `OXR` (Open Exchange Rates) or `FXR` (Fixer).
   - -cfg: The file path to your configuration file, which should contain the necessary API keys and other settings.
   - -cur *(optional)*: Specifies the base currency for the exchange rates. If omitted, a default base currency will be used.

   Example:

   Xrate.jar -SVR testServer -API OXR -cfg /your/config/file -cur GBP


 Configuration File

The configuration file should include:
- API Key: Your API key for the selected service.
- Email Settings: SMTP settings for sending alert emails.
- Validation Parameters: Thresholds and conditions for triggering data validation alerts.

 Notes

- Order of Parameters: Ensure that the command-line parameters are provided in the order specified above to avoid errors.
- API Documentation: 
- [Open Exchange Rates Documentation](https://docs.openexchangerates.org/reference/api-introduction)
- [Fixer API Documentation](https://fixer.io/documentation)

These links provide detailed information about each API, including endpoints, usage limits, and data formats.

Error Handling and Alerts
Xrates is designed to maintain high data integrity. If any of the following conditions are detected during data retrieval or validation, an alert email will be sent:
- Missing Data: No exchange rate data is available for a required currency.
- Stale Data: The retrieved data is older than a specified threshold.
- Influx/Fluctuations: Significant fluctuations in exchange rates that may indicate errors or market volatility.
- Failed API: if the API call failed the application is programmed to retry 4 times every 5 minutes for a total of 20 minutes.
- File path to config file is invalid


The alert email sent to `ops@exchange-data.com` will contain a detailed report of the issue, allowing the operations team to take corrective action promptly.