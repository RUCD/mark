/*
 * The MIT License
 *
 * Copyright 2017 georgi.
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
package netrank;

import java.io.IOException;
import mark.core.DetectionAgentInterface;
import mark.core.DetectionAgentProfile;
import mark.core.Evidence;
import mark.core.RawData;
import mark.core.ServerInterface;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author georgi
 * The Reputation agent scraps two websites and composes a Reputation Index
 * based on the scores given by those two websites. If the Reputation Index
 * is below a predetermined threshold, evidence is created.
 */
public class Reputation implements DetectionAgentInterface<Link> {

    private static final String WOT_URL = "https://www.mywot.com";
    private static final String SEARCH_AGENT = "Mozilla/5.0 "
            + "(Windows NT 6.2; WOW64) AppleWebKit/537.15 "
            + "(KHTML, like Gecko) Chrome/24.0.1295.0 Safari/537.15";
    private static final int REPUTATION_THRESHOLD = 50;

/**
 *
 * @param word parameters is the domain we are passing to the WebOfTrust search.
 * @return returns an estimation if the domain we pass has malicious code
 * attached to it. The "www.mywot.com" website checks the reputation of all IP
 * addresses related to a domain. This reputation is computed by users that
 * submit their information about the domain so its a crowdfunded website.
 * @throws IOException
 */
    private int connectToWOT(final String word) throws IOException {
        String search_url = WOT_URL + "/en/scorecard/" + word;
        Document doc = Jsoup.connect(search_url).timeout(5000)
                .userAgent(SEARCH_AGENT).get();

        //search for the span DOM element that holds the # of results
        Elements result_element = doc
                .select("div.score-board-ratings__index.r1");
        return parseWOTdata(result_element.html());
    }

/**
 *
 * @param data parameters is the result retrieved from the WebOfTrust search.
 * @return returns the reputation that WOT gives the domain in integer
 * WOT has two parameters: "Trustworthiness" and "Child Safety". For this agent
 * we just consider the trustworthiness of the domain.
 */
    private int parseWOTdata(final String data) {
        int reputation = 0;
        if (data != null && !data.isEmpty()) {
            String[] lines = data.split("\\r?\\n");
            String trustworthiness = lines[0];
            reputation = Integer.parseInt(trustworthiness);
        }
        return reputation;
    }

    @Override
    public final void analyze(
            final Link subject,
            final String actual_trigger_label,
            final DetectionAgentProfile profile,
            final ServerInterface datastore) throws Throwable {

        RawData[] raw_data = datastore.findRawData(
            actual_trigger_label, subject);

        String domain_name = subject.getServer();
        int reputation = 0;
        try {
            reputation = connectToWOT(domain_name);
        } catch (IOException ex) {
            System.out.println("Could not establish connection to server");
            return;
        }

        if (reputation < REPUTATION_THRESHOLD) {
            Evidence evidence = new Evidence();
            evidence.score = 1;
            evidence.subject = subject;
            evidence.label = profile.label;
            evidence.time = raw_data[raw_data.length - 1].time;
            evidence.report = "Found a domain:"
                    + " " + domain_name
                    + " " + "with suspiciously low Reputation of "
                    + reputation;
            datastore.addEvidence(evidence);
        }
    }
}
