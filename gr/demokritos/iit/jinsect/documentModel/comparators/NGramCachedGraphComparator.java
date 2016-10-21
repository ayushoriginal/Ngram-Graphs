/*
 * NGramCachedGraphComparator.java
 *
 * Created on June 4, 2007, 3:39 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import java.util.Iterator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.WeightedEdge;

/** An n-gram graph comparator class, which uses caching of edge info 
 * to optimize the comparison speed.
 *
 * @author ggianna
 */
public class NGramCachedGraphComparator extends NGramGraphComparator {
    EdgeCachedLocator eclLocator = null;
    
    /***
     *Returns the similarity of the document n-gram graph oFirst as opposed
     * to oSecond.
     *@param oFirst The first document n-gram graph.
     *@param oSecond The second document n-gram graph.
     *@return A {@link GraphSimilarity} object indicative of the similarity between the two graphs.
     ***/
    @Override
    public GraphSimilarity getSimilarityBetween(Object oFirst, Object oSecond) {
        // Initialize variables
        GraphSimilarity sSimil = new GraphSimilarity();
        DocumentNGramGraph dgFirst = (DocumentNGramGraph)oFirst;
        DocumentNGramGraph dgSecond = (DocumentNGramGraph)oSecond;
        
        // Use a weight for every level. Larger n-grams have higher weight.
        int iOverallImportance = 0;
        for (int iCnt = dgFirst.getMinSize(); iCnt <= dgFirst.getMaxSize(); iCnt++ )
            iOverallImportance += utils.sumFromTo(dgFirst.getMinSize(), iCnt);
        
        for (int iCurLvl = dgFirst.getMinSize(); iCurLvl <= dgFirst.getMaxSize(); iCurLvl++) {
            // Calc level weight
            int iLevelImportance = utils.sumFromTo(dgFirst.getMinSize(), iCurLvl);
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
            // Use minimum-edged as main graph for speed
            if (iFirstTotalEdges > iSecondTotalEdges) {
                // Swap graphs
                UniqueVertexGraph ngIntermediate = ngSecondGraph;
                ngSecondGraph = ngFirstGraph;
                ngFirstGraph = ngIntermediate;
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
                WeightedEdge weFound = (WeightedEdge)eclLocator.locateEdgeInGraph(ngSecondGraph, weEdge.getVertexA(), 
                        weEdge.getVertexB());
                if (weFound == null)
                    // Ignore
                    continue;
                // else update similarity parameters
                // Take degredation into account
                double dFinalDegredation = Math.min(dgSecond.degredationDegree(weFound),
                		dgFirst.degredationDegree(weEdge));
                sSimilLevel.ContainmentSimilarity += 1.0 / (iMinEdges *
                        Math.max(1.0, dFinalDegredation));
                
                // Value difference metrics
                double dFirstData = weEdge.getWeight();
                double dOtherData = weFound.getWeight();

                if ((dFirstData < 0) || (dOtherData < 0))
                        System.err.println("Negative weights found...");
                
                // Take degredation into account
                sSimilLevel.ValueSimilarity += (Math.min(dFirstData, dOtherData) /
                        Math.max(dFirstData, dOtherData)) / (iMaxEdges *
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
 
/** A utility class that acts as a {@link Comparable} object that holds edges.
 */
class ComparableEdgeHolder implements Comparable {
    /** The edge contained in the holder. */
    protected Edge eHeld;
    public ComparableEdgeHolder(Edge e) {
        eHeld = e;
    }
    
    /**Implementation of the <code>compareTo</code> function of {@link Comparable} 
     * that uses a simple string representation of the edge's node lables to perform comparison.
     * That means that edges with identically labelled nodes, are considered identical.
     *@param o The (ComparableEdgeHolder) object to compare this object to.
     *@return Returns the results of the {@link String} <code>compareTo</code> method, when applied 
     * to the string representation of the edge contained in the two ComparableEdgeHolder objects.
     */
    @Override
    public int compareTo(Object o) {
        StringBuffer sbThis = 
                new StringBuffer(eHeld.getVertexA().getLabel()).append("==>").append(eHeld.getVertexB().getLabel());
        Edge oOther = ((ComparableEdgeHolder)o).eHeld;
        StringBuffer sbOther = 
                new StringBuffer(oOther.getVertexA().getLabel()).append("==>").append(oOther.getVertexB().getLabel());
        return sbThis.toString().compareTo(sbOther.toString());
    }
}
