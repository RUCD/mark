/*
 * The MIT License
 *
 * Copyright 2019 tibo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package be.cylab.mark.integration;

import be.cylab.mark.client.Client;
import be.cylab.mark.core.DetectionAgentProfile;
import be.cylab.mark.core.Evidence;
import be.cylab.mark.detection.TimeAverage;
import java.net.URL;

/**
 *
 * @author tibo
 */
public class TimeAverageIT extends MarkCase {

    public final void testTimeAverage() throws Throwable {

        DetectionAgentProfile agent = new DetectionAgentProfile();
        agent.setClassName(TimeAverage.class.getName());
        agent.setLabel("detection.timeaverage");
        agent.setTriggerLabel("data");

        getActivationController().setAgentProfile(agent);

        Client datastore = new Client(new URL("http://127.0.0.1:8080"));

        Link link = new Link("1.2.3.4", "my.server.com");

        Evidence ev = new Evidence();
        ev.setLabel("data");
        ev.setScore(1);
        ev.setSubject(link);
        datastore.addEvidence(ev);

        ev.setScore(2);
        datastore.addEvidence(ev);
        ev.setScore(3);
        datastore.addEvidence(ev);

        Thread.sleep(3000);
        Evidence[] ta_evidences =
                datastore.findEvidence("detection.timeaverage", link);

        assertTrue(ta_evidences.length > 0);
        assertEquals(2.0, ta_evidences[ta_evidences.length - 1].getScore());


    }

}
