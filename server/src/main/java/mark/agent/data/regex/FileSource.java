package mark.agent.data.regex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mark.core.RawData;
import mark.core.ServerInterface;
import mark.server.DataAgentInterface;
import mark.server.DataAgentProfile;

/**
 *
 * A generic data agent that reads a file as fast as possible, and parse it
 * line by line using a regular expression.
 * This data agent is usually used for testing detection agents or a log file.
 * Hence it does simply print some stats at the end of execution...
 * @author Thibault Debatty
 */
public class FileSource implements DataAgentInterface {

    private ServerInterface datastore;
    private final String regex =
            "^(\\d{10})\\..*\\s(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s"
            + "(\\S+)\\s(\\S+)\\s(\\S+)\\s(\\S+)\\s.*$";
    private final String type = "http";

    private InputStream stream;
    private int line_count;
    private int error_count;

    private volatile boolean run = true;

    /**
     * Return the number of lines in the file that caused an exception while
     * parsing or saving to the datastore.
     * @return
     */
    public final int getErrorCount() {
        return error_count;
    }

    /**
     *
     */
    public final void run() {

        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line = null;

        try {
            while ((line = in.readLine()) != null) {
                if (!run) {
                    break;
                }

                line_count++;
                try {
                    RawData rd = parse(line);
                    datastore.addRawData(rd);

                } catch (Throwable ex) {
                    error_count++;
                    System.err.println(ex.getMessage());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(
                    FileSource.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Print some stats
        System.out.println("----");
        System.out.println("Number of lines: " + line_count);
        System.out.println("Lines with error: " + error_count);
    }

    /**
     * Set the input stream (usually a file) to be used by this data agent.
     * @param stream
     */
    public final void setInputStream(final InputStream stream) {
        this.stream = stream;
    }

    /**
     *
     * @param line
     * @return
     * @throws Exception
     */
    protected final RawData parse(final String line) throws Exception {
        Pattern p = Pattern.compile(regex);
        Matcher match = p.matcher(line);

        if (!match.matches()) {
            throw new Exception("Regex did not match line " + line);
        }

        RawData rd = new RawData();
        rd.type = type;
        rd.time = Integer.valueOf(match.group(1));
        rd.client = match.group(2);

        URI uri = new URI(match.group(6));
        String domain = uri.getHost();
        rd.server = domain;
        rd.data = line;
        return rd;
    }


    /**
     * {@inheritDoc}
     */
    public final void stop() {
        // Nothing to do here, just wait for the end of the file.
    }


    /**
     * {@inheritDoc}
     */
    public final void kill() {
        run = false;
    }

    public void setDatastore(ServerInterface datastore) {
        this.datastore = datastore;
    }

    public void setProfile(DataAgentProfile profile) throws Exception {
        File profile_file = new File(profile.path);
        File data_file = new File(profile_file.toURI().resolve((String) profile.parameters.get("file")));
        this.setInputStream(new FileInputStream(data_file));
    }

}
