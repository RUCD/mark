package mark.server;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.net.MalformedURLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import mark.activation.ActivationController;
import mark.activation.DetectionAgentProfile;
import mark.core.Subject;
import mark.core.SubjectAdapter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author Thibault Debatty
 */
public class Datastore<T extends Subject> implements Runnable {

    private Config config;

    // isStarted() will be called from another thread => must be volatile
    private volatile Server http_server;
    private volatile ActivationController activation_controller;
    private SubjectAdapter<T> adapter;

    /**
     * Instatiate a datastore with default config and empty activation profiles.
     */
    public Datastore(SubjectAdapter<T> adapter) {
        this.adapter = adapter;

        this.activation_controller = new ActivationController<T>(adapter);

    }

    /**
     * Set the activation profiles.
     * @param profiles
     * @throws Exception if the profiles are corrupted (misspelled class name?)
     */
    public final void setActivationProfiles(
            final Iterable<DetectionAgentProfile> profiles)
            throws Exception {

        activation_controller.setProfiles(profiles);
    }

    /**
     * Set the configuration before starting the server.
     * @param config
     */
    public final void setConfiguration(final Config config) {
        this.config = config;
    }

    /**
     * Run the datastore server, blocking.
     * This method will only return if the server crashes...
     *
     */
    public final void run() {

        if (config == null) {
            this.config = new Config();
        }



        try {
            activation_controller.setServerAddress(
                    "http://" + config.server_host + ":" + config.server_port);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Datastore.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Connect to mongodb
        MongoClient mongodb = new MongoClient(
                config.mongo_host, config.mongo_port);
        MongoDatabase mongodb_database = mongodb.getDatabase(config.mongo_db);

        // Start Activation controller
        activation_controller.start();

        // Create and run HTTP / JSON-RPC server
        RequestHandler<T> datastore_handler = new RequestHandler<T>(
                mongodb_database,
                activation_controller,
                adapter);

        ObjectMapper object_mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        //module.addSerializer(AnalysisUnit.class, new LinkSerializer());
        module.addDeserializer(Subject.class, adapter);
        object_mapper.registerModule(module);

        JsonRpcServer jsonrpc_server = new JsonRpcServer(object_mapper, datastore_handler);

        QueuedThreadPool thread_pool = new QueuedThreadPool(
                config.max_threads,
                config.min_threads,
                config.idle_timeout,
                new ArrayBlockingQueue<Runnable>(config.max_pending_requests));

        http_server = new Server(thread_pool);

        ServerConnector http_connector = new ServerConnector(http_server);
        http_connector.setHost(config.server_host);
        http_connector.setPort(config.server_port);

        http_server.setConnectors(new Connector[]{http_connector});
        http_server.setHandler(new JettyHandler(jsonrpc_server));

        try {
            http_server.start();
        } catch (Exception ex) {
            System.err.println("Failed to start datastore: " + ex.getMessage());
        }
    }

    /**
     * Returns true if the datastore is completely started (http server).
     * @return
     */
    public final boolean isStarted() {
        return http_server != null && http_server.isStarted();
    }

    /**
     * Wait for current tasks to finish and stop the datastore server.
     */
    public final void stop() {
        activation_controller.awaitTermination();

        try {
            http_server.stop();

        } catch (Exception ex) {
            System.err.println(
                    "HTTP server failed to stop: " + ex.getMessage());
        }
    }


}
