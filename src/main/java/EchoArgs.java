import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

class EchoArgs {

    // Variables:
    private static final String credPath = "/credentials.json";
    private static final String tokenPath = System.getenv("HOME") + "/.config/polybar/Gmail-Polybar/tokens";
    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPE = Collections.singletonList(GmailScopes.GMAIL_LABELS);
    private static final String appName = "Polybar Gmail Unread";
    
    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException {
        if (args.length < 1) {
            printHelp();
        } else {
            switch (args[0]) {
                case "-h": //Help
                    printHelp();
                    break;

                case "-l":
                    listLables();
                    break;

                case "-s":
                    String id;
                    if (args.length > 1) {
                        id = args[1];
                    } else {
                        id = "INBOX"; 
                    }
                    if (!start(id)) {
                        System.out.println("Label not found: " + id);
                    }
                    break;

                default:
                    System.out.println("Argument not recognized: " + args[0]);
                    printHelp();
                    break;
            }
        }
    }

    static void printHelp() {
        System.out.println("Help: \n"
        + "     -h          Print this help message\n"
        + "     -l          List all posable labels\n"
        + "     -s [label]  Start program with spicific label");
    }

    static Credential getCreds(final HttpTransport httpTransport) throws IOException {
        InputStream in = EchoArgs.class.getResourceAsStream(credPath);
        if (in == null) {
            throw new FileNotFoundException("Reasource not found " + credPath);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, SCOPE)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokenPath)))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    static Gmail startGmail(HttpTransport httpTransport) throws IOException {
        return new Gmail.Builder(httpTransport, jsonFactory, getCreds(httpTransport))
            .setApplicationName(appName)
            .build();
    }

    static void listLables() throws IOException, GeneralSecurityException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = startGmail(httpTransport);
        ListLabelsResponse listResponce = service.users().labels().list("me").execute();
        List<Label> labels = listResponce.getLabels();
        Label unreadLabel;
        for (Label label : labels) {
            System.out.println("- " + label.getName() + " (" + label.getId() + ")");
            unreadLabel = service.users().labels().get("me", label.getId()).execute();
            System.out.println("Unread Messages: " + unreadLabel.getMessagesUnread());
            System.out.println();
        }
    }

    static boolean start(String labelId) throws IOException, GeneralSecurityException, InterruptedException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = startGmail(httpTransport);
        Label unreadLabel;
        while (true) {
            try {
                unreadLabel = service.users().labels().get("me", labelId).execute();
            } catch (GoogleJsonResponseException e) {
                return false;
            }
            System.out.println(unreadLabel.getMessagesUnread());
            Thread.sleep(60000);
        }
    }

}