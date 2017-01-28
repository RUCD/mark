package mark.detection;

import java.util.Random;
import mark.activation.DetectionAgentProfile;
import mark.core.Evidence;
import mark.core.RawData;
import mark.core.ServerInterface;
import mark.core.Subject;

/**
 * Dummy detection agent that reads some raw data from datastore and writes
 * two evidences. Requires a running server.
 * @author Thibault Debatty
 */
public class ReadWrite<T extends Subject> extends AbstractDetectionAgent<T> {


    @Override
    public void analyze(
            T subject,
            String actual_trigger_label,
            DetectionAgentProfile profile,
            ServerInterface<T> datastore) throws Throwable {

        RawData[] data = datastore.findRawData(actual_trigger_label, subject);

        // Process data
        Random rand = new Random();

        // Add evidences to datastore
        Evidence<T> evidence = new Evidence<T>();
        evidence.label = profile.label;
        evidence.subject = subject;
        evidence.report = "Some report...";
        evidence.score = rand.nextDouble();
        evidence.time = data[0].time;
        datastore.addEvidence(evidence);

        evidence.score = rand.nextDouble();
        datastore.addEvidence(evidence);
    }
}
