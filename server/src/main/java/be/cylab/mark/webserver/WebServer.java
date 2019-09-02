/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
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
package be.cylab.mark.webserver;

import be.cylab.mark.client.Client;
import be.cylab.mark.core.InvalidProfileException;
import static spark.Spark.*;
import com.google.inject.Inject;
import be.cylab.mark.server.Config;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.TemplateViewRoute;
import spark.template.pebble.PebbleTemplateEngine;

/**
 *
 * @author Thibault Debatty
 */
public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            WebServer.class);

    private final Config config;
    private final Client client;

    /**
     * Instantiate a web server with provided config.
     *
     * @param config
     */
    @Inject
    public WebServer(final Config config)
            throws MalformedURLException, InvalidProfileException {
        this.config = config;

        this.client = new Client(
                new URL("http://127.0.0.1:8080"), config.getSubjectAdapter());
                //config.getDatastoreUrl(), config.getSubjectAdapter());
    }

    /**
     * Start the web server (non-blocking).
     *
     * @throws java.lang.Exception if jetty fails to start
     */
    public final void start() throws Exception {
        if (!config.start_webserver) {
            return;
        }

        LOGGER.info("Starting web interface at port 8000");


        PebbleTemplateEngine pebble = new PebbleTemplateEngine(
                new ClasspathLoader());

        staticFiles.location("/static");
        port(8000);

        get("/", new HomeRoute(client), pebble);
        get("/status", new StatusRoute(client), pebble);
    }

    /**
     *
     * @throws Exception if an error happens while stopping the server
     */
    public final void stop() throws Exception {
        spark.Spark.stop();
    }

    class StatusRoute implements TemplateViewRoute {

        private final Client client;

        public StatusRoute(Client client) {
            this.client = client;
        }

        @Override
        public ModelAndView handle(Request rqst, Response rspns)
                throws Exception {

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("mark", this.client);
            return new ModelAndView(attributes, "status.html");
        }
    }
}