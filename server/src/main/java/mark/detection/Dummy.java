package mark.detection;

import mark.activation.DetectionAgentProfile;
import mark.core.ServerInterface;
import mark.core.Subject;

/**
 * Dummy detection agent, which does not try to read or write to the datastore.
 * Can be used to test activation, without starting a complete server.
 * @author Thibault Debatty
 */
public class Dummy extends AbstractDetectionAgent {

    private static final int SLEEP_TIME = 500;


    @Override
    public void analyze(
            final Subject subject,
            final String actual_trigger_label,
            final DetectionAgentProfile profile,
            final ServerInterface datastore) throws Throwable {

        Thread.sleep(SLEEP_TIME);
    }
}
