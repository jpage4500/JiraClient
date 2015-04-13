import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.CustomFieldOption;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * JIRA Command-line client
 */
public class JiraCommandLine {

    private static final String DEFAULT_JIRA_URL = "https://mobeam.atlassian.net";

    private static final String JIRA_URL = "jiraUrl";
    private static final String JIRA_USERNAME = "jiraUsername";
    private static final String JIRA_PASSWORD = "jiraPassword";

    public static String jiraUrl;
    public static String jiraUsername;
    public static String jiraPassword;

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage(args);
            return;
        }

        if (args[0].equalsIgnoreCase("config")) {
            setCredentials();
        } else if (args[0].equalsIgnoreCase("comment")) {
            if (args.length < 2) {
                System.out.println("not enough arguments!\n");
                printUsage(args);
                return;
            }
            addComment(args[1], args[2]);
        } else {
            System.out.println("unknown option: " + args[0] + "\n");
            printUsage(args);
            return;
        }
    }

    private static void printUsage(String[] args) {
        System.out.println("JIRA command-line client");
        System.out.println("usage:");
        System.out.println("JiraCommandLine comment <issue> <comments to add>");
        System.out.println("    - add a comment to issue <issue>");
        System.out.println("JiraCommandLine config");
        System.out.println("    - configure JIRA URL / username / password (encrypted)");
        System.exit(0);
    }

    private static void setCredentials() {
        // get previous stored credentails (if any)
        getCredentials();

        // default to known JIRA URL (if any)
        if (StringUtils.isEmpty(jiraUrl)) {
            jiraUrl = DEFAULT_JIRA_URL;
        }

        // prompt for new credentials / URL
        String url = readLine("Enter JIRA URL", jiraUrl);
        String username = readLine("Enter JIRA Username", jiraUsername);
        String password = readLine("Enter JIRA Password", null);

        System.out.println("testing credentials..");

        // test credentials
        BasicCredentials creds = new BasicCredentials(username, password);
        JiraClient jira = new JiraClient(url, creds);
        try {
            jira.getProjects();
        } catch (JiraException ex) {
            System.out.println("failed to login: " + ex.getMessage());
            // try again
            setCredentials();
            return;
        }

        System.out.println("login success! credentials saved.");

        // login success - save credentials
        Preferences pref = Preferences.userNodeForPackage(JiraCommandLine.class);
        pref.put(JIRA_URL, url);
        pref.put(JIRA_USERNAME, username);

        String base64Password = Base64.encodeBase64String(password.getBytes());
        pref.put(JIRA_PASSWORD, base64Password);

        try {
            pref.flush();
        } catch (BackingStoreException e) {
            System.out.println("failed to save preferences!: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static JiraClient getClient() {
        if (getCredentials() == false) {
            System.out.println("no saved credentials! run with 'config' option first!");
            return null;
        }
        BasicCredentials creds = new BasicCredentials(jiraUsername, jiraPassword);
        JiraClient jira = new JiraClient(jiraUrl, creds);
        return jira;
    }

    private static String readLine(String prompt, String defaultValue) {
        System.out.print(prompt + ":");
        if (StringUtils.isNotEmpty(defaultValue)) {
            System.out.print(" (" + defaultValue + ")");
        }
        // newline and :
        System.out.print("\n:");

        String value = null;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            value = br.readLine();
        } catch (IOException ioe) {
            System.out.println("IOException!");
        }

        if (StringUtils.isEmpty(value) && StringUtils.isNotEmpty(defaultValue)) {
            System.out.println("using: " + defaultValue);
            value = defaultValue;
        }

        if (StringUtils.isEmpty(value)) {
            return readLine(prompt, null);
        }

        return value;
    }

    private static boolean getCredentials() {
        Preferences pref = Preferences.userNodeForPackage(JiraCommandLine.class);
        jiraUrl = pref.get(JIRA_URL, null);
        jiraUsername = pref.get(JIRA_USERNAME, null);

        String encodedPassword = pref.get(JIRA_PASSWORD, null);
        if (encodedPassword != null) {
            jiraPassword = new String(Base64.decodeBase64(encodedPassword));
        }

        return (StringUtils.isNotEmpty(jiraUrl) && StringUtils.isNotEmpty(jiraUsername) && StringUtils.isNotEmpty(jiraPassword));
    }

    private static void addComment(String issueNumber, String comment) {
        JiraClient client = getClient();
        if (client == null) {
            return;
        }

        System.out.println("adding comment to issue: " + issueNumber);

        try {
            final Issue issue = client.getIssue(issueNumber);
            issue.addComment(comment);
        } catch (JiraException ex) {
            System.out.println("failed to add comment: " + ex.getMessage());
            return;
        }
        System.out.println("DONE!");
    }

    private static void testActions() {
        BasicCredentials creds = new BasicCredentials("batman", "pow! pow!");
        JiraClient jira = new JiraClient("https://jira.example.com/jira", creds);

        try {
            /* Retrieve issue TEST-123 from JIRA. We'll get an exception if this fails. */
            final Issue issue = jira.getIssue("TEST-123");

            /* Print the issue key. */
            System.out.println(issue);

            /* You can also do it like this: */
            System.out.println(issue.getKey());

            /* Vote for the issue. */
            issue.vote();

            /* And also watch it. Add Robin too. */
            issue.addWatcher(jira.getSelf());
            issue.addWatcher("robin");

            /* Open the issue and assign it to batman. */
            issue.transition().field(Field.ASSIGNEE, "batman").execute("Open");

            /* Add two comments, with one limited to the developer role. */
            issue.addComment("No problem. We'll get right on it!");
            issue.addComment("He tried to send a whole Internet!", "role", "Developers");

            /* Print the reporter's username and then the display name */
            System.out.println("Reporter: " + issue.getReporter());
            System.out.println("Reporter's Name: " + issue.getReporter().getDisplayName());

            /* Print existing labels (if any). */
            for (String l : issue.getLabels()) {
                System.out.println("Label: " + l);
            }

            /*
             * Change the summary and add two labels to the issue. The double-brace initialiser
             * isn't required, but it helps with readability.
             */
            issue.update().field(Field.SUMMARY, "tubes are clogged").field(Field.LABELS, new ArrayList() {

                {
                    addAll(issue.getLabels());
                    add("foo");
                    add("bar");
                }
            }).field(Field.PRIORITY, Field.valueById("1")) /* you can also set the value by ID */
            .execute();

            /* You can also update values with field operations. */
            issue.update().fieldAdd(Field.LABELS, "baz").fieldRemove(Field.LABELS, "foo").execute();

            /* Print the summary. We have to refresh first to pickup the new value. */
            issue.refresh();
            System.out.println("New Summary: " + issue.getSummary());

            /* Now let's start progress on this issue. */
            issue.transition().execute("Start Progress");

            /* Pretend customfield_1234 is a text field. Get the raw field value... */
            Object cfvalue = issue.getField("customfield_1234");

            /* ... Convert it to a string and then print the value. */
            String cfstring = Field.getString(cfvalue);
            System.out.println(cfstring);

            /* And finally, change the value. */
            issue.update().field("customfield_1234", "new value!").execute();

            /* Pretend customfield_5678 is a multi-select box. Print out the selected values. */
            List<CustomFieldOption> cfselect = Field.getResourceArray(CustomFieldOption.class, issue.getField("customfield_5678"), jira.getRestClient());
            for (CustomFieldOption cfo : cfselect) {
                System.out.println("Custom Field Select: " + cfo.getValue());
            }

            /* Print out allowed values for the custom multi-select box. */
            List<CustomFieldOption> allowedValues = jira.getCustomFieldAllowedValues("customfield_5678", "TEST", "Task");
            for (CustomFieldOption customFieldOption : allowedValues) {
                System.out.println(customFieldOption.getValue());
            }

            /* Set two new values for customfield_5678. */
            issue.update().field("customfield_5678", new ArrayList() {

                {
                    add("foo");
                    add("bar");
                    add(Field.valueById("1234")); /* you can also update using the value ID */
                }
            }).execute();

            /* Add an attachment */
            File file = new File("C:\\Users\\John\\Desktop\\screenshot.jpg");
            issue.addAttachment(file);

            /* And finally let's resolve it as incomplete. */
            issue.transition().field(Field.RESOLUTION, "Incomplete").execute("Resolve Issue");

            /* Create a new issue. */
            Issue newIssue = jira
                    .createIssue("TEST", "Bug")
                    .field(Field.SUMMARY, "Bat signal is broken")
                    .field(Field.DESCRIPTION, "Commissioner Gordon reports the Bat signal is broken.")
                    .field(Field.REPORTER, "batman")
                    .field(Field.ASSIGNEE, "robin")
                    .execute();
            System.out.println(newIssue);

            /* Link to the old issue */
            newIssue.link("TEST-123", "Dependency");

            /* Create sub-task */
            Issue subtask = newIssue.createSubtask().field(Field.SUMMARY, "replace lightbulb").execute();

            /* Search for issues */
            Issue.SearchResult sr = jira.searchIssues("assignee=batman");
            System.out.println("Total: " + sr.total);
            for (Issue i : sr.issues) {
                System.out.println("Result: " + i);
            }

        } catch (JiraException ex) {
            System.err.println(ex.getMessage());

            if (ex.getCause() != null) {
                System.err.println(ex.getCause().getMessage());
            }
        }
    }

}
