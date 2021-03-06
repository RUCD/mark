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
package be.cylab.mark.activation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import be.cylab.mark.server.Config;
import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.collision.fifoqueue.FifoQueueCollisionSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

/**
 * Allows to run our detection jobs using an Apache Ignite cluster.
 * @author tibo
 */
@Singleton
public final class IgniteExecutor implements ExecutorInterface {

    private final Ignite ignite;
    private final Config config;

    private static final String LOCALHOST = "127.0.0.1";

    /**
     *
     * @param config
     */
    @Inject
    public IgniteExecutor(final Config config) {

        this.config = config;

        if (Ignition.state() == IgniteState.STARTED) {
            ignite = Ignition.ignite();
            return;
        }

        IgniteConfiguration ignite_config = new IgniteConfiguration();
        ignite_config.setPeerClassLoadingEnabled(true);
        ignite_config.setClientMode(!config.isIgniteStartServer());

        ignite_config.setCollisionSpi(new FifoQueueCollisionSpi());

        ignite_config.setMetricsUpdateFrequency(500);

        // Changing total RAM size to be used by Ignite Node.
        DataStorageConfiguration storage_config =
                new DataStorageConfiguration();
        storage_config.getDefaultDataRegionConfiguration().setMaxSize(
            12L * 1024 * 1024 * 1024);
        ignite_config.setDataStorageConfiguration(storage_config);

        if (!config.isIgniteAutodiscovery()) {
            // Disable autodiscovery
            TcpDiscoverySpi spi = new TcpDiscoverySpi();
            TcpDiscoveryVmIpFinder ip_finder = new TcpDiscoveryVmIpFinder();
            ip_finder.setAddresses(Arrays.asList(LOCALHOST));
            spi.setIpFinder(ip_finder);
            ignite_config.setDiscoverySpi(spi);
        }

        // Start Ignite framework..
        ignite = Ignition.start(ignite_config);

    }


    @Override
    public void submit(final Runnable job) {
        this.ignite.executorService().submit(job);
    }

    @Override
    public boolean shutdown() throws InterruptedException {
        Thread.sleep(2 * 1000 * config.getUpdateInterval());
        this.ignite.executorService().shutdown();
        return this.ignite.executorService().awaitTermination(1, TimeUnit.DAYS);
    }

    @Override
    public Map<String, Object> getStatus() {
        HashMap<String, Object> map = new HashMap<>();
        ClusterMetrics metrics = ignite.cluster().metrics();

        map.put("executor.job.executed", metrics.getTotalExecutedJobs());
        map.put("executor.job.running", metrics.getCurrentActiveJobs());
        map.put("executor.job.waiting", metrics.getCurrentWaitingJobs());

        map.put("executor.job.waittime", metrics.getAverageJobWaitTime());
        map.put("executor.job.executetime", metrics.getAverageJobExecuteTime());

        map.put("executor.nodes", metrics.getTotalNodes());
        map.put("executor.cpus", metrics.getTotalCpus());


        return map;
    }



}
