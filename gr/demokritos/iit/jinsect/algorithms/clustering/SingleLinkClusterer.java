/*
 * SingleLinkClusterer.java
 *
 * Created on April 12, 2007, 11:42 AM
 *
 */

package gr.demokritos.iit.jinsect.algorithms.clustering;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.structs.INamed;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import gr.demokritos.iit.jinsect.algorithms.statistics.CombinationGenerator;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import gr.demokritos.iit.jinsect.structs.IntegerPair;
import gr.demokritos.iit.jinsect.structs.SimpleSimilarity;
import gr.demokritos.iit.jinsect.threading.ThreadList;
import gr.demokritos.iit.jinsect.utils;
import java.util.Arrays;
import salvo.jesus.graph.DirectedEdgeImpl;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;

/** A single link clustering algorithm implementation class.
 *
 * @author ggianna
 */
public class SingleLinkClusterer implements IClusterer {
    /** The separator string between names of clusters in super-cluster labels. 
     */
    public static final String CLUSTER_NAME_SEPARATOR = " ";
    
    ArrayList<ArrayList<Set>> ClusteringsInTime;
    UniqueVertexGraph Hierarchy;
    
    /** Creates a new instance of SingleLinkClusterer. No parameters required.*/
    public SingleLinkClusterer() {
        // Clusterings in time (0 => current state, 1 => next state)
        ClusteringsInTime = new ArrayList();
        Hierarchy = new UniqueVertexGraph();
    }

    public void calculateClusters(Set sObjects, SimilarityComparatorListener clDistanceCalculator) {       
        // Clear history
        ClusteringsInTime.clear();
        Hierarchy = new UniqueVertexGraph();
        // Init clustering to single item clusters
        ArrayList<Set> R0 = new ArrayList();
        Iterator iObjects = sObjects.iterator();
        while (iObjects.hasNext()) {
            HashSet hsCurCluster = new HashSet();
            hsCurCluster.add(iObjects.next());
            R0.add(hsCurCluster);
        }
        ClusteringsInTime.add(0, R0); // Init current state
        // Use all processors
        ThreadList tThreads = new ThreadList(Runtime.getRuntime().availableProcessors());
        
        // While more than a single cluster.
        while (ClusteringsInTime.get(0).size() > 1) {
            // Get all possible pairs
            CombinationGenerator cgCur = new CombinationGenerator(ClusteringsInTime.get(0).size(), 2);
            final Distribution dDistances = new Distribution();
            
            while (cgCur.hasMore())
            {
                int[] iaCurObjects = cgCur.getNext();
                /////////////////////////////
                final Set s1 = ClusteringsInTime.get(0).get(iaCurObjects[0]);
                final Set s2 = ClusteringsInTime.get(0).get(iaCurObjects[1]);
                final int[] iaCurObjectsArg = new int[2];
                iaCurObjectsArg[0] = iaCurObjects[0];
                iaCurObjectsArg[1] = iaCurObjects[1];
                final SimilarityComparatorListener fc = clDistanceCalculator;
                // Multi-threading
                while (!tThreads.addThreadFor(new Runnable() {
                    public void run() {
                        double dSimil = getSimilarityBetweenClusters(s1, s2, fc);
                        IntegerPair p = new IntegerPair(iaCurObjectsArg);
                        synchronized (dDistances) {
                            dDistances.setValue(p, dSimil);
                        }
                    }
                }))
                    Thread.yield();
                
                /////////////////////////////
            }
            
            try {
                tThreads.waitUntilCompletion();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
                return;
            }
            
            // Init new Clustering
            ArrayList<Set> RCur = new ArrayList(ClusteringsInTime.get(0));
            // Get closest clusters
            int[] iaClosestObjects = ((IntegerPair)dDistances.getKeyOfMaxValue()).toArray();
            // Merge closest
            HashSet hsMerged = new HashSet();
            hsMerged.addAll(ClusteringsInTime.get(0).get(iaClosestObjects[0]));
            hsMerged.addAll(ClusteringsInTime.get(0).get(iaClosestObjects[1]));
            
            RCur.remove(ClusteringsInTime.get(0).get(iaClosestObjects[0]));
            RCur.remove(ClusteringsInTime.get(0).get(iaClosestObjects[1]));
            RCur.add(hsMerged);
            // Show merging in hierarchy
            try {
                Vertex v1 = new VertexImpl(ClusteringsInTime.get(0).get(iaClosestObjects[0]));
                
                String sName1 = utils.printSortIterable(ClusteringsInTime.get(0).get(iaClosestObjects[0]), 
                        CLUSTER_NAME_SEPARATOR);
                v1.setLabel(sName1);
                Vertex v2 = new VertexImpl(ClusteringsInTime.get(0).get(iaClosestObjects[1]));
                String sName2 = utils.printSortIterable(ClusteringsInTime.get(0).get(iaClosestObjects[1]), 
                        CLUSTER_NAME_SEPARATOR);
                v2.setLabel(sName2);
                Vertex vPar = new VertexImpl(hsMerged);
                
                // Extract names from clusters, back into their parts
                // and add all into a single set
                Set<String> sNames = new HashSet<String>();
                sNames.addAll(Arrays.asList(sName1.split(CLUSTER_NAME_SEPARATOR)));
                sNames.addAll(Arrays.asList(sName2.split(CLUSTER_NAME_SEPARATOR)));
                
                // Create name of new cluster, based on all parts
                String sNameParent = utils.printSortIterable(sNames, 
                        CLUSTER_NAME_SEPARATOR);
                vPar.setLabel(sNameParent);
                // DEBUG LINES
                // System.err.println("Added cluster " + sNameParent);
                //////////////
                
                try {
                    if (Hierarchy.contains(v1))
                        v1 = Hierarchy.locateVertex(v1);
                    if (Hierarchy.contains(vPar))
                        vPar = Hierarchy.locateVertex(vPar);
                    
                    Hierarchy.addEdge(new DirectedEdgeImpl(v1, vPar));
                }
                catch (Exception e) {
                    // Ignore. Edge already exists.
                }
                
                try {
                    if (Hierarchy.contains(v2))
                        v2 = Hierarchy.locateVertex(v2);
                    if (Hierarchy.contains(vPar))
                        vPar = Hierarchy.locateVertex(vPar);
                    Hierarchy.addEdge(new DirectedEdgeImpl(v2, vPar));
                }
                catch (Exception e) {
                    // Ignore. Edge already exists.
                }
                    
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
            // Add last time to ClusteringsInTime
            ClusteringsInTime.add(0, RCur);
        }
        
    }
    
    public UniqueVertexGraph getHierarchy() {
        return Hierarchy;
    }
    
    /** Calculates the similarity between two clusters. In this algorithm the 
     * maximum similarity over all pairs of the two clusters is used.
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
        
        // Return the maximum similarity
        return dDistances.maxValue();
    }

    // Testing function
    public static void main(String[] args) {
        SingleLinkClusterer s = new SingleLinkClusterer();
        SimilarityComparatorListener c = new SimilarityComparatorListener() {
            public ISimilarity getSimilarityBetween(Object oFirst, Object oSecond) throws InvalidClassException {
                ISimilarity res = new SimpleSimilarity(
                        org.apache.commons.lang.StringUtils.getLevenshteinDistance(
                        (String)oFirst, (String)oSecond));
                return res;
            }
        };
        HashSet hElements = new HashSet();
        hElements.add("p1");
        hElements.add("p10");
        hElements.add("p11");
        hElements.add("p21");
        hElements.add("p311");
        hElements.add("p4111");
        hElements.add("p4112");
        hElements.add("p4115");
        s.calculateClusters(hElements,c);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(s.getHierarchy(), true));
    }
}

