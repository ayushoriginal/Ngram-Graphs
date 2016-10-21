/*
 * NGramCachedDistroGraphComparator.java
 *
 * Created on December 7, 2007, 3:45 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import gr.demokritos.iit.conceptualIndex.events.IDistributionComparisonListener;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.Iterator;
import java.util.Map;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramDistroGraph;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import salvo.jesus.graph.WeightedEdge;

/** A class that can compare graphs with distributions on their edges (like {@link DocumentNGramDistroGraph}).
 *@see DocumentNGramDistroGraph
 * @author ggianna
 */
public class NGramCachedDistroGraphComparator extends NGramCachedGraphComparator {
    protected IDistributionComparisonListener distroComparator = null;
            
    public NGramCachedDistroGraphComparator() {
        super();
        
        distroComparator = new Distribution(); // Also acts as a calculator.
    }
    
    public NGramCachedDistroGraphComparator(IDistributionComparisonListener cl) {
        distroComparator = cl;
    }
    
    /***
     *Returns the similarity of the document n-gram graph oFirst as opposed
     * to oSecond.
     *@param oFirst The first document n-gram graph.
     *@param oSecond The second document n-gram graph.
     *@return A {@link GraphSimilarity} object indicative of the similarity between the two graphs.
     ***/
    public GraphSimilarity getSimilarityBetween(Object oFirst, Object oSecond) {        
        // Initialize variables
        GraphSimilarity sSimil = new GraphSimilarity();
        DocumentNGramDistroGraph dgFirst = (DocumentNGramDistroGraph)oFirst;
        DocumentNGramDistroGraph dgSecond = (DocumentNGramDistroGraph)oSecond;
        
        // Use a weight for every level. Larger n-grams have higher weight.
        int iOverallImportance = 0;
        for (int iCnt = dgFirst.getMinSize(); iCnt <= dgFirst.getMaxSize(); iCnt++ )
            iOverallImportance += gr.demokritos.iit.jinsect.utils.sumFromTo(dgFirst.getMinSize(), iCnt);
        
        for (int iCurLvl = dgFirst.getMinSize(); iCurLvl <= dgFirst.getMaxSize(); iCurLvl++) {
            // Calc level weight
            int iLevelImportance = gr.demokritos.iit.jinsect.utils.sumFromTo(dgFirst.getMinSize(), iCurLvl);
            GraphSimilarity sSimilLevel = new GraphSimilarity();
            UniqueVertexGraph ngFirstGraph = dgFirst.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph ngSecondGraph = dgSecond.getGraphLevelByNGramSize(iCurLvl);
            // If there is no corresponding level in dgOtherGraph
            if (ngSecondGraph == null)
                // Ignore it
                continue;
            
            // Get edge count
            int iFirstTotalEdges = ngFirstGraph.getEdgesCount();
            int iSecondTotalEdges = ngSecondGraph.getEdgesCount();
            
            Map firstEdges = dgFirst.getEdgesToDistros();
            Map secondEdges = dgSecond.getEdgesToDistros();
            
            // Use minimum-edged as main graph for speed
            if (iFirstTotalEdges > iSecondTotalEdges) {
                // Swap graphs
                UniqueVertexGraph ngIntermediate = ngSecondGraph;
                ngSecondGraph = ngFirstGraph;
                ngFirstGraph = ngIntermediate;                
                
                firstEdges = dgSecond.getEdgesToDistros();
                secondEdges = dgFirst.getEdgesToDistros();                
            
            }
            
            // Get min and max edge count
            int iMinEdges = ngFirstGraph.getEdgesCount();
            int iMaxEdges = ngSecondGraph.getEdgesCount();
            eclLocator = new EdgeCachedLocator(iMinEdges);
            
            int iProgress = 0;
            
            
            // For every edge of the first graph
            Iterator iIter = ngFirstGraph.getEdgeSet().iterator();
            iProgress = 0;
            while (iIter.hasNext())
            {
                // Get edge
                WeightedEdge weEdge = (WeightedEdge)iIter.next();
                
                iProgress++;
                
                // If edge does not exist in other graph
                WeightedEdge weFound = (WeightedEdge)eclLocator.locateDirectedEdgeInGraph(ngSecondGraph, 
                        weEdge.getVertexA(), weEdge.getVertexB());
                if (weFound == null)
                    // Ignore
                    continue;
                // else update similarity parameters
                // Take degredation into account
                double dFinalDegredation = Math.min(dgSecond.degredationDegree(weFound),
                		dgFirst.degredationDegree(weEdge));
                sSimilLevel.ContainmentSimilarity += 1.0 / (iMinEdges *
                        Math.max(1.0, dFinalDegredation));
                
                // Value difference metrics, using DISTROS
                Distribution d1 = (Distribution)firstEdges.get(weEdge);
                Distribution d2 = (Distribution)secondEdges.get(weFound);
                
//                double dFirstData = weEdge.getWeight();
//                double dOtherData = weFound.getWeight();
                
                // Take degredation into account
                sSimilLevel.ValueSimilarity += distroComparator.compareDistributions(d1, d2) / (iMinEdges *
                        Math.max(1.0, 
                            (dgSecond.degredationDegree(weFound) + dgFirst.degredationDegree(weEdge))));
                
                // TODO : Use progress indication
                if (iProgress % 500 == 0)
                    if (Listener != null) {
                        Listener.Notify(this, new Double(100.0 * ((double)iProgress / iMinEdges)));
                    }
            }
            sSimilLevel.SizeSimilarity = (double)iMinEdges / gr.demokritos.iit.jinsect.utils.max(iMaxEdges, 1.0);
            
            // Summarize
            sSimil.ValueSimilarity += sSimilLevel.ValueSimilarity * iLevelImportance / iOverallImportance;
            sSimil.ContainmentSimilarity += sSimilLevel.ContainmentSimilarity * iLevelImportance / iOverallImportance;
            sSimil.SizeSimilarity += sSimilLevel.SizeSimilarity * iLevelImportance / iOverallImportance;            
            ////////////
            
        }
        
        return sSimil;
    }    
    
}
