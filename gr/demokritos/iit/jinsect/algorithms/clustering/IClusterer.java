/*
 * IClusterer.java
 *
 * Created on April 12, 2007, 11:22 AM
 *
 */

package gr.demokritos.iit.jinsect.algorithms.clustering;

import java.util.Set;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;

/** An interface describing a cluster facility. The clusterer gets a set of Objects and a distance calculator
 * and returns a (possibly nested) set of clusters, each containing a number of the original Objects.
 * @author ggianna
 */
public interface IClusterer {
    /** A method clustering a set of objects, given a distance calculator upon pairs of these objects.
     *@param Objects The set of objects to cluster.
     *@param DistanceCalculator The similarity comparison event listener object, that actually performs the 
     * similarity calculation.
     */
    public void calculateClusters(Set Objects, SimilarityComparatorListener DistanceCalculator);
    
    /** Returns the (calculated) hierarchy of the clusters. For non-hierarchical algorithms, there will be
     *no edges in the graph. Every edge in the graph points from the child to the parent cluster.
     *@return The hierarchy as a {@link UniqueVertexGraph}.
     */
    public UniqueVertexGraph getHierarchy();
    
}
