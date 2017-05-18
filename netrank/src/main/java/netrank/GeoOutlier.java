/*
 * The MIT License
 *
 * Copyright 2017 Georgi Nikolov.
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

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mark.core.DetectionAgentInterface;
import mark.core.DetectionAgentProfile;
import mark.core.Evidence;
import mark.core.RawData;
import mark.core.ServerInterface;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.bson.Document;

/**
 *
 * @author Georgi Nikolov Agent class for determining servers whose coordinates
 * are too far away from the normal servers the clients has been connected to.
 * They are outliers and may be malicious.
 */
public class GeoOutlier implements DetectionAgentInterface<Link> {

    //maximum radius of the neighbourhood to be considered by clustering,
    //metric dependant on the DistanceMeasure used for the clustering algorithm
    private final int max_radius = 500;
    //minimum number of points needed for a cluster
    private final int min_points = 0;
    //minimum accepted quantity of points in a cluster
    private final int min_cluster_size = 3;

    private ArrayList<LocationWrapper> getLocations(
            final LookupService cl, final RawData[] raw_data) {

        ArrayList<LocationWrapper> locations = new ArrayList<>();
        //regex pattern for extracting the server IP the client connected to
        Pattern pattern = Pattern.compile("DIRECT/(\\b(?:(?:25[0-5]|2[0-4]"
                + "[0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]"
                + "|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b)");
        for (RawData line : raw_data) {

            Matcher matcher = pattern.matcher(line.data);
            if (!matcher.find()) {
                continue;
            }

            String server_ip = matcher.group(1);
            Location location = cl.getLocation(server_ip);
            if (location == null) {
                continue;
            }

            //create a wrapper for the different locations
            LocationWrapper locwrapper = new LocationWrapper(
                    server_ip, location);
            //check if its already a saved server location
            if (locations.contains(locwrapper)) {
                continue;
            }

            locations.add(locwrapper);
        }
        return locations;
    }

    // Analyze function inherited from the DetectionAgentInterface
    // accepts the subject to analyze
    // trigger of the agent
    // the profile used to load the agent
    // the database to which to connect to gather RawData
    @Override
    public final void analyze(
            final Link subject,
            final String actual_trigger_label,
            final DetectionAgentProfile profile,
            final ServerInterface datastore) throws Throwable {

        Document query = new Document(LinkAdapter.CLIENT, subject.getClient())
                .append("LABEL", actual_trigger_label);
        RawData[] data = datastore.findData(query);

        LookupService cl = loadGeoIP();


        //get the filtered locations already wrapped without duplicates
        ArrayList<LocationWrapper> locations = getLocations(cl, data);
        //Initialize a new cluster algorithm.
        //We use DBSCANCluster to determine locations close to each other
        //and outliers that don't belong to any cluster.
        DBSCANClusterer dbscan = new DBSCANClusterer(
                max_radius, min_points, new EarthDistance());
        List<Cluster<LocationWrapper>> clusters = dbscan.cluster(locations);

        for (Cluster cluster : clusters) {
            if (cluster.getPoints().size() < min_cluster_size) {
                Evidence evidence = new Evidence();
                evidence.score = 1;
                evidence.subject = subject;
                evidence.label = profile.label;
                evidence.time = data[data.length - 1].time;
                evidence.report = "Found"
                        + " outliers in the connections with"
                        + " distance between the servers bigger than the"
                        + " expected distance between servers"
                        + "\n";
                datastore.addEvidence(evidence);
            }
        }
    }

    /**
     * Load GeoIP database file from JAR.
     * @return
     * @throws IOException if we cannot load the DB
     */
    public final LookupService loadGeoIP() throws IOException {
        //Code for Accessing the local GeoLocation File consisting of the
        //information per IP Address.
        ClassLoader class_loader = getClass().getClassLoader();
        File geo_file = new File(class_loader
                .getResource("GeoLiteCity.dat").getFile());
        LookupService cl = new LookupService(geo_file,
                LookupService.GEOIP_MEMORY_CACHE
                | LookupService.GEOIP_CHECK_CACHE);

        return cl;
    }
}

/**
 *
 * @author Georgi Nikolov Helper class for wrapping the locations to be passed
 * as Clusterables to the DBSCANClusterer algorithm.
 */
class LocationWrapper implements Clusterable {

    private final double[] points;
    private final Location location;
    private final String server_ip;

    public LocationWrapper(final String serverip, final Location location) {
        this.server_ip = serverip;
        this.location = location;
        this.points = new double[]{location.latitude, location.longitude};
    }

    public Location getLocation() {
        return this.location;
    }

    @Override
    public double[] getPoint() {
        return this.points;
    }

    public String getServerIp() {
        return this.server_ip;
    }

    @Override
    public boolean equals(final Object object) {
        boolean same = false;
        if (object != null && object instanceof LocationWrapper) {
            same = this.server_ip.equals(
                    ((LocationWrapper) object).server_ip);
        }
        return same;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.server_ip);
        return hash;
    }

}

/**
 * Calculates the Earth distance between two GPS points. Returns the distance in
 * kilometers
 */
class EarthDistance implements DistanceMeasure {

    /* function for computing the distance */
    @Override
    public final double compute(final double[] point1, final double[] point2)
            throws DimensionMismatchException {
        double earth_radius = 6371; // in kilometer

        double d_lat = Math.toRadians(point2[0] - point1[0]);
        double d_lng = Math.toRadians(point2[1] - point1[1]);

        double sind_lat = Math.sin(d_lat / 2);
        double sind_lng = Math.sin(d_lng / 2);

        double a = Math.pow(sind_lat, 2) + Math.pow(sind_lng, 2)
                * Math.cos(Math.toRadians(point1[0]))
                * Math.cos(Math.toRadians(point2[0]));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = earth_radius * c;

        return dist; // output distance in kilometer
    }
}
