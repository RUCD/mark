/*
 * The MIT License
 *
 * Copyright 2018 georgi.
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

import java.io.FileInputStream;
import java.io.IOException;
import mark.core.DataAgentInterface;
import mark.core.DataAgentProfile;
import mark.core.ServerInterface;
import java.util.zip.GZIPInputStream;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import mark.core.RawData;

/**
 *
 * A generic data agent that reads an archive of files, extracts it and parses
 * rthe files (in JSON format).
 * This data agent is usually used for testing detection agents, or to run a
 * demo.
 * @author Georgi Nikolov
 */
public class ArchiveSource implements DataAgentInterface {

    /**
     * Use this key in the parameters map to set the speedup for reading the
     * file.
     */
    public static final String SPEEDUP_KEY = "speedup";

    private static final double DEFAULT_SPEEDUP = Double.MAX_VALUE;

    /**
     *
     * @param profile
     * @param datastore
     * @throws Throwable if something went wrong...
     */
    public final void run(
            final DataAgentProfile profile, final ServerInterface datastore)
            throws Throwable {

        double speedup = DEFAULT_SPEEDUP;
        String speedup_string = profile.parameters.get(SPEEDUP_KEY);
        if (speedup_string != null) {
            try {
                speedup = Double.valueOf(speedup_string);
            } catch (NumberFormatException ex) {
                speedup = DEFAULT_SPEEDUP;
            }
        }

        long start_time = 0;
        long first_data_time = 0;

        byte[] buffer = new byte[1024];

        try {
            File data_file = profile.getPath(profile.parameters.get("file"));
            FileInputStream file_in =
                    new FileInputStream(data_file);

            GZIPInputStream gzip_input_stream = new GZIPInputStream(file_in);

            StringBuilder str = new StringBuilder();
            int bytes_read;
            int nmb_lines = 0;
            while ((bytes_read = gzip_input_stream.read(buffer)) > 0) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }

                String s = new String(buffer, 0, bytes_read);
                //System.out.println("DEBUG: " + s);
                str.append(s);
                String result = str.toString();
                String[] result_lines = result.split("\\r?\\n");
                String to_keep = result_lines[0];

                RawData rd = parseLine(to_keep);
                rd.label = profile.label;

                if (start_time == 0) {
                    start_time = System.currentTimeMillis();
                    first_data_time = rd.time;
                }

                // Simulated time for this new data
                rd.time = rd.time - first_data_time + start_time;

                long wait_time = (long) ((rd.time - start_time) / speedup);
                Thread.sleep(wait_time);

                datastore.addRawData(rd);
                nmb_lines++;

                if (result_lines.length > 1) {
                    String temp = result_lines[1];
                    str.setLength(0);
                    str.append(temp);

                }
            }

            gzip_input_stream.close();

            System.out.println("The file was decompressed"
                    + " and parced successfully!");
            // Print some stats
            System.out.println("----");
            System.out.println("Number of lines: " + nmb_lines);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private RawData parseLine(final String line) throws Exception {
        RawData rd = new RawData();
        JsonParser parser = new JsonParser();
        JsonObject json_obj = (JsonObject) parser.parse(line);
        try {
            SimpleDateFormat sdf
                    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(json_obj.get("@timestamp").getAsString());
            Long timestamp = date.getTime();

            String client = "unkown";
            if (json_obj.has("tk_client_ip")) {
                client = json_obj.get("tk_client_ip").getAsString();
            }
            String method = "unkown";
            if (json_obj.has("tk_operation")) {
                method = json_obj.get("tk_operation").getAsString();
            }
            int bytes = 0;
            if (json_obj.has("tk_size")) {
                bytes = json_obj.get("tk_size").getAsInt();
            }
            String url = "unkown";
            if (json_obj.has("tk_url")) {
                url = json_obj.get("tk_url").getAsString();
            }
            String peerhost = "unkown";
            if (json_obj.has("tk_server_ip")) {
                peerhost = json_obj.get("tk_server_ip").getAsString();
            }
            String type = "unkown";
            if (json_obj.has("tk_mime_content")) {
                type = json_obj.get("tk_mime_content").getAsString();
            }

            String raw_data = timestamp
                    + " " + 0 //info elapsed time not available
                    + " " + client //client IP
                    + " " + "unknown" //code status not available
                    + " " + bytes //bytes sent/recieved
                    + " " + method //method of connection
                    + " " + url //destination url
                    + " " + "-"
                    + " " + "DIRECT/" + peerhost //server IP
                    + " " + type; //mime type of connection

            rd.time = timestamp;
            rd.data = raw_data;
            rd.subject = new Link(client, peerhost);
        } catch (ParseException ex) {
            System.out.println("Error Parsing JSON: " + ex);
        }
        return rd;
    }
}