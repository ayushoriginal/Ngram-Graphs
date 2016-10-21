/*
 * Under LGPL
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import salvo.jesus.graph.WeightedEdge;

/** A class that uses Eucledian distance, considering each edge-weight to be 
 * a vector value for the specific dimension-edge.
 * 
 * @author ggianna
 */
public class NGramGraphEuclidianComparator extends NGramCachedGraphComparator {
    /***
     *Returns the similarity of the document n-gram graph oFirst as opposed
     * to oSecond, using the inverse of the Eucledian distance. 
     * Each unique edge is supposed to correspond to a dimension and its weight 
     * is the vector value in that dimension. 
     *@param oFirst The first document n-gram graph.
     *@param oSecond The second document n-gram graph.
     *@param bUseLevelWeighting If true, uses level weighting for dimensions,
     *  based on the rank of the given edge n-grams.
     *@return A {@link GraphSimilarity} object indicative of the similarity between the two graphs.
     ***/
    public ISimilarity getEuclidianSimilarityBetween(Object oFirst, Object oSecond,
            boolean bUseLevelWeighting) {
        // Initialize variables
        GraphSimilarity sSimil = new GraphSimilarity();
        DocumentNGramGraph dgFirst = (DocumentNGramGraph)oFirst;
        DocumentNGramGraph dgSecond = (DocumentNGramGraph)oSecond;
        // Init vector representation with the distribution object, which can
        // be used as a sparse vector.
        Distribution<String> dFirst = new Distribution<String>();
        Distribution<String> dSecond = new Distribution<String>();
        
        // Use a weight for every level. Larger n-grams have higher weight.
        int iOverallImportance = 1;
        if (bUseLevelWeighting) 
            for (int iCnt = dgFirst.getMinSize(); iCnt <= dgFirst.getMaxSize(); iCnt++ )
                iOverallImportance += utils.sumFromTo(dgFirst.getMinSize(), iCnt);
        
        double dDistance = 0.0;
        for (int iCurLvl = dgFirst.getMinSize(); iCurLvl <= dgFirst.getMaxSize(); iCurLvl++) {
            // Calc level weight
            int iLevelImportance = 1;
            if (bUseLevelWeighting) 
                iLevelImportance = utils.sumFromTo(dgFirst.getMinSize(), iCurLvl);
            
            UniqueVertexGraph ngFirstGraph = dgFirst.getGraphLevelByNGramSize(iCurLvl);
            graphToSparseVector(ngFirstGraph, dFirst);
            UniqueVertexGraph ngSecondGraph = dgSecond.getGraphLevelByNGramSize(iCurLvl);
            graphToSparseVector(ngSecondGraph, dSecond);
            
            dDistance += calcEuclidianDistanceBetween(dFirst, dSecond, 
                    iLevelImportance);
        }
        
        dDistance = Math.sqrt(dDistance);
        
        final double dDistanceArg = dDistance;
        final double dSimilarityArg = 1.0 / dDistance;
        
        // Return similarity object
        return new ISimilarity() {
            @Override
            public double getOverallSimilarity() {
                return dSimilarityArg;
            }

            @Override
            public double asDistance() {
                return dDistanceArg;
            }
        };
    }    

    private void graphToSparseVector(UniqueVertexGraph gCur, 
            Distribution dSparseVector) {
        // If there is no corresponding level in dgOtherGraph
        if (gCur == null)
            // Ignore it
            return;

        // Get edge count
        int iTotalEdges = gCur.getEdgesCount();
        eclLocator = new EdgeCachedLocator(iTotalEdges);

        int iProgress = 0;

        // For every edge of the first graph
        Iterator iIter = gCur.getEdgeSet().iterator();
        iProgress = 0;
        while (iIter.hasNext())
        {
            // Get edge
            WeightedEdge weEdge = (WeightedEdge)iIter.next();
            iProgress++;

            dSparseVector.setValue(weEdge.toString(), weEdge.getWeight());
            // TODO : Use progress indication
            if (iProgress % 500 == 0)
                if (Listener != null) {
                    Listener.Notify(this, new Double(100.0 * 
                            ((double)iProgress / iTotalEdges)));
                }
        }
        
    }
    
    private double calcEuclidianDistanceBetween(Distribution<String> d1, 
            Distribution<String> d2, double dImportance) {
        double dDist = 0.0;
        // Get all edges
        Set<String> dAll = new TreeSet(d1.asTreeMap().keySet());
        dAll.addAll(d2.asTreeMap().keySet());
        for (String sDim : dAll) {
            dDist += dImportance * Math.sqrt(Math.pow(
                d1.getValue(sDim) - d2.getValue(sDim) ,2));
        }
        
        return dDist;
    }
    
    /** Utility method used for testing purposes. **/
    public static void main(String sArgs[]) {
        DocumentNGramGraph g1 = new DocumentNGramGraph();
        g1.setDataString("This is a serious test...");
        DocumentNGramGraph g2 = new DocumentNGramGraph(1, 3, 3);
        g2.setDataString("This is a test...");
        DocumentNGramGraph g3 = new DocumentNGramGraph(1, 3, 3);
        g3.setDataString("This is serious for a test.");
        
        NGramGraphEuclidianComparator c = new NGramGraphEuclidianComparator();
        System.err.println("Value Similarity of g1, g2 to g3:");
        System.err.println(c.getSimilarityBetween(g1, g3).ValueSimilarity);
        System.err.println(c.getSimilarityBetween(g2, g3).ValueSimilarity);
        
        System.err.println("Euclidian Similarity of g1, g2 to g3:");
        System.err.println(c.getEuclidianSimilarityBetween(g1, g3, false).getOverallSimilarity());
        System.err.println(c.getEuclidianSimilarityBetween(g2, g3, false).getOverallSimilarity());
        
        System.err.println("Euclidian Distance of g1, g2 to g3:");
        System.err.println(c.getEuclidianSimilarityBetween(g1, g3, false).asDistance());
        System.err.println(c.getEuclidianSimilarityBetween(g2, g3, false).asDistance());
        
        System.err.println("Self-Euclidian Similarity of g1:");
        System.err.println(c.getEuclidianSimilarityBetween(g1, g1, false).getOverallSimilarity());
        System.err.println(c.getEuclidianSimilarityBetween(g1, g1, false).asDistance());
        
    }
}
