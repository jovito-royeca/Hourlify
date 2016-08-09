/*
 * Writes data in Google Sheets.
 */
package com.hourlify.dataloader;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;

import java.io.*;
import java.util.*;

public class SheetsQuickstart {
    /** Application name. */
    private static final String APPLICATION_NAME =
        "Google Sheets API Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart.json");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/sheets.googleapis.com-java-quickstart.json
     */
    private static final List<String> SCOPES =
        Arrays.asList(SheetsScopes.SPREADSHEETS);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
            SheetsQuickstart.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() throws IOException {
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws Exception {
        createHourlifyEmployees();
    }

    public static void readSampleSpreadsheet() throws IOException {
        // Build a new authorized API client service.
        Sheets service = getSheetsService();

        // Prints the names and majors of students in a sample spreadsheet:
        // https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
        String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
        String range = "Class Data!A2:E";
        ValueRange response = service.spreadsheets().values()
            .get(spreadsheetId, range)
            .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
        } else {
          System.out.println("Name, Major");
          for (List row : values) {
            // Print columns A and E, which correspond to indices 0 and 4.
            System.out.printf("%s, %s\n", row.get(0), row.get(4));
          }
        }
    }

    public static void createHourlifyEmployees() throws Exception {
        String spreadsheetId = "14oUVpOkT_mcwOIvOn643g7Q_OktdCkdru4NpURMlUhU";
        int employeeCount = 200;
        ArrayList<ValueRange> values = new ArrayList<>();

        List lastNames = arrayFromFile("lastname.csv");
        List firstNames = arrayFromFile("firstname.csv");
        for (int i=0; i<employeeCount; i++) {
            Random rand1 = new Random();
            Random rand2 = new Random();

            ValueRange value = new ValueRange();
            
            ArrayList<Object> arrayList = new ArrayList();
            ArrayList<Object> row = new ArrayList();
            row.add(i+1000);
            row.add(lastNames.get(rand1.nextInt(lastNames.size()-1)));
            row.add(firstNames.get(rand2.nextInt(firstNames.size()-1)));
            arrayList.add(row);
            
            value.set("values", arrayList);
            value.setRange("Employees!A"+(i+2)+":C"+(i+2));
            values.add(value);
        }
        
        BatchUpdateValuesRequest request = new BatchUpdateValuesRequest();
        request.setValueInputOption("RAW");
        request.setData(values);
        Sheets service = getSheetsService();
        service.spreadsheets().values().batchUpdate(spreadsheetId, request).execute();
    }
    
    public static List<Map<String,String>> generateNames() {
//        List lastNames = arrayFromFile("lastname.csv");
//        List firstNames = arrayFromFile("firstname.csv");
        ArrayList<Map<String,String>> array = new ArrayList();
        int number = 150;
        
        for (int i=0; i<number; i++) {
           
        }
        
        return array;
    }
    
    public static List<String> arrayFromFile(String fileName) throws Exception {
        ArrayList<String> array = new ArrayList();
        InputStream in = SheetsQuickstart.class.getClassLoader().getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = br.readLine()) != null) {
            array.add(line);
        }
        
        return array;
    }
}