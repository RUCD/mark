package mark.agent.detection.dummy;

import java.util.logging.Level;
import java.util.logging.Logger;
import mark.activation.AbstractDetectionAgent;
import mark.core.Evidence;
import mark.core.RawData;
import mark.core.ServerInterface;

/**
 * Dummy detection agent that reads some raw data from datastore and writes
 * two evidences. Requires a running server.
 * @author Thibault Debatty
 */
public class ReadWrite extends AbstractDetectionAgent {

    /**
     * {@inheritDoc}
     */
    public final void run() {

        // Read data from datastore
        ServerInterface datastore;
        RawData[] data;
        try {
            datastore = getDatastore();
            data = datastore.findRawData(getLabel(), getSubject());
        } catch (Throwable ex) {
            System.err.println("Could not connect to server!");
            System.err.println(ex.getMessage());
            return;
        }

        System.out.println("Found " + data.length + " elements");
        System.out.println(data[data.length - 1]);

        // Process data

        // Add evidences to datastore
        Evidence evidence = createEvidenceTemplate();
        evidence.report = "Some report...";
        evidence.score = 0.6;
        evidence.time = data[0].time;

        try {
            datastore.addEvidence(evidence);
        } catch (Throwable ex) {
            Logger.getLogger(ReadWrite.class.getName()).log(Level.SEVERE, null, ex);
        }

        evidence.score = 0.3;
        try {
            datastore.addEvidence(evidence);
        } catch (Throwable ex) {
            Logger.getLogger(ReadWrite.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
