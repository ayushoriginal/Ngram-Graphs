/*
 * AverageLinkClusterer.java
 *
 * Created on April 12, 2007, 5:53 PM
 *
 */

package gr.demokritos.iit.jinsect.algorithms.clustering;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.InvalidClassException;
import java.util.Iterator;
import java.util.Set;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.structs.ISimilarity;

/** A generic use class that performs AverageLinkClustering. More in 
 *<pre>
 * @book{theodoridis2003pr,
 * title={{Pattern Recognition}},
 * author={Theodoridis, S. and Koutroumbas, K.},
 * year={2003},
 * publisher={Academic Press}
 * }
 *</pre>
 * @author ggianna
 */
public class AverageLinkClusterer  extends SingleLinkClusterer {
    
    /** Creates a new instance of AverageLinkClusterer. */
    public AverageLinkClusterer() {
    }
    
    /** Calculates the similarity between two clusters. In this algorithm the 
     * average similarity between all pairs of the two clusters is used.
     *@param sClusterOne The first cluster.
     *@param sClusterTwo The second cluster.
     *@param clDistanceCalculator The calculator of distance between set elements.
     *@return The similarity between the clusters.
     */
    protected double getSimilarityBetweenClusters(Set sClusterOne, Set sClusterTwo, 
            SimilarityComparatorListener clDistanceCalculator) {
        Distribution dDistances = new Distribution();
        
        // For every object in cluster one
        Iterator iFirstCluster = sClusterOne.iterator();
        int iCnt = 0;
        while (iFirstCluster.hasNext()) {
            Object oFirst = iFirstCluster.next();

            // For every object in cluster two
            Iterator iSecondCluster = sClusterTwo.iterator();
            while (iSecondCluster.hasNext()) {
                Object oSecond = iSecondCluster.next();
                ISimilarity sSimil;
                // Compare the objects
                try {
                    synchronized (oFirst) {
                        synchronized (oSecond) {
                            sSimil = clDistanceCalculator.getSimilarityBetween(oFirst, 
                                    oSecond);
                        }
                    }
                } catch (InvalidClassException ex) {
                    System.err.println("Cannot compare " + oFirst.toString() + " to " + 
                            oSecond.toString() + ". Cause:");
                    ex.printStackTrace(System.err);
                    continue;
                }
                // Put id of pair and their similarity in distance distribution
                dDistances.setValue(iCnt++, sSimil.getOverallSimilarity());                    
            }
        }
        
        // Return the maximum similarity, which is actually the average of distance between elements.
        return dDistances.average(true);
    }}
