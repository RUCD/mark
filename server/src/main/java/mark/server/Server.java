package mark.server;

import com.google.inject.Inject;
import mark.core.InvalidProfileException;
import mark.datastore.Datastore;
import mark.core.DataAgentProfile;
import mark.webserver.WebServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import mark.activation.ActivationController;
import mark.core.DetectionAgentProfile;
import mark.data.DataAgentContainer;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

/**
 * Represents a MARK server. It is composed of: - a webserver - an activation
 * controller - a datastore (json-rpc server) - optionally: some data agents
 *
 * @author Thibault Debatty
 */
public class Server {

    public interface ServerFactory {

        Server create(Config config);
    }

    private static final org.slf4j.Logger LOGGER
            = LoggerFactory.getLogger(Server.class);

    private final Config config;
    private final Datastore datastore;
    private final WebServer web_server;
    private final LinkedList<DataAgentContainer> data_agents;
    private final ActivationController activation_controller;

    /**
     * Initialize a server with default configuration, dummy subject adapter, no
     * data agents and no detection agents.
     *
     * @param config
     * @throws java.lang.Throwable
     */
    @Inject
    public Server(final Config config, final WebServer web_server,
            final ActivationController activation_controller,
            final Datastore datastore) throws Throwable {
        this.config = config;

        startLogging();
        parseConfig();

        LOGGER.info("Instantiate web server...");
        this.web_server = web_server;

        LOGGER.info("Instantiate activation controller "
                + "and Apache Ignite cluster");
        this.activation_controller = activation_controller;

        LOGGER.info("Instantiate Datastore...");
        this.datastore = datastore;

        LOGGER.info("Instantiate data agents...");
        this.data_agents = new LinkedList<>();

        File modules_dir;
        try {
            modules_dir = config.getModulesDirectory();
        } catch (FileNotFoundException ex) {
            LOGGER.warn(ex.getMessage());
            return;
        }

        LOGGER.info("Parsing modules directory "
                + modules_dir.getAbsolutePath());

        // Parse *.data.yml files
        File[] data_agent_files = modules_dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".data.yml");
            }
        });

        for (File file : data_agent_files) {
            data_agents.add(
                    new DataAgentContainer(
                            DataAgentProfile.fromFile(file),
                            config));
        }

        // Parse *.detection.yml files
        File[] detection_agent_files
                = modules_dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(final File dir, final String name) {
                        return name.endsWith(".detection.yml");
                    }
                });

        for (File file : detection_agent_files) {
            activation_controller.addAgent(
                    DetectionAgentProfile.fromFile(file));
        }
    }

    /**
     * Non-blocking start the datastore and data agents (sources) in separate
     * threads. This method returns when the server and agents are started. You
     * can use server.stop()
     *
     * @throws java.net.MalformedURLException if the URL specified by config is
     * invalid
     * @throws Exception if Jetty caused an exception
     */
    public final void start()
            throws MalformedURLException, Exception {

        LOGGER.info("Starting server...");

        // Start the web server...
        web_server.start();

        // Start the activation controller...
        activation_controller.testProfiles();
        activation_controller.start();

        // Start the datastore...
        datastore.start();

        // Start data agents...
        for (DataAgentContainer agent : data_agents) {
            agent.start();
        }

        LOGGER.info("Server started!");
    }

    /**
     * Stop the data agents, wait for all detection agents to complete and
     * eventually stop the datastore.
     */
    public final void stop() throws Exception {
        LOGGER.info("Stopping server...");
        LOGGER.info("Ask data agents to stop...");
        for (DataAgentContainer agent : data_agents) {
            agent.interrupt();
        }

        awaitTermination();

        LOGGER.info("Ask activation controller to stop...");
        activation_controller.interrupt();
        activation_controller.join();

        LOGGER.info("Ask datastore to stop...");
        datastore.stop();

        LOGGER.info("Ask webserver to stop...");
        web_server.stop();

        LOGGER.info("Server stopped!");
    }

    public final void awaitTermination() throws InterruptedException {
        LOGGER.info("Wait for data agents to finish...");
        for (DataAgentContainer agent : data_agents) {
            agent.join();
        }

        LOGGER.info("Wait for activation controller to finish running tasks...");
        activation_controller.awaitTermination();
    }

    /**
     * Analyze the module folder. - modify the class path - parse data agent
     * profiles - parse detection agent profiles
     *
     * @throws MalformedURLException
     */
    private void parseConfig()
            throws MalformedURLException, FileNotFoundException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException,
            InvalidProfileException {

        LOGGER.info("Parse configuration...");
        File modules_dir;
        try {
            modules_dir = config.getModulesDirectory();
        } catch (FileNotFoundException ex) {
            LOGGER.warn(ex.getMessage());
            return;
        }

        // List *.jar and update the class path
        // this is a hack that allows to modify the global (system) class
        // loader.
        URLClassLoader class_loader
                = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod(
                "addURL", URL.class);
        method.setAccessible(true);

        File[] jar_files = modules_dir.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".jar");
            }
        });

        for (File jar_file : jar_files) {
            method.invoke(class_loader, jar_file.toURI().toURL());
        }
    }

    /**
     * Add the profile for a detection agent.
     *
     * @param profile
     */
    public final void addDetectionAgent(final DetectionAgentProfile profile) {
        activation_controller.addAgent(profile);
    }

    /**
     *
     * @param profile
     * @throws InvalidProfileException
     * @throws MalformedURLException
     */
    public final void addDataAgentProfile(final DataAgentProfile profile)
            throws InvalidProfileException, MalformedURLException {
        data_agents.add(new DataAgentContainer(profile, config));
    }

    private void startLogging() {

        Logger.getRootLogger().getLoggerRepository().resetConfiguration();

        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d [%p] [%t] %c %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.FATAL);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        console = new ConsoleAppender();
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getLogger("mark").addAppender(console);

        try {
            Logger.getRootLogger().addAppender(
                    getFileAppender("mark-server.log", Level.INFO));
            Logger.getLogger("org.apache.ignite").addAppender(
                    getFileAppender("mark-ignite.log", Level.INFO));
            Logger.getLogger("org.eclipse.jetty").addAppender(
                    getFileAppender("mark-jetty.log", Level.INFO));
            Logger.getLogger("mark.activation.ActivationController").addAppender(
                    getFileAppender("mark-activationctonroller.log", Level.DEBUG));

        } catch (FileNotFoundException ex) {
            System.err.println(
                    "Logs will not be written to files: " + ex.getMessage());
        }

    }

    private FileAppender getFileAppender(String filename, Level level)
            throws FileNotFoundException {
        FileAppender fa = new FileAppender();
        fa.setName(filename);
        fa.setFile(
                config.getLogDiretory().getPath() + File.separator + filename);
        fa.setLayout(new PatternLayout("%d [%p] [%t] %c %m%n"));
        fa.setThreshold(level);
        fa.setAppend(true);
        fa.activateOptions();

        return fa;
    }
}
