/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import java.util.Iterator;
import salvo.jesus.graph.WeightedEdge;

/**
 * @author pckid
 */
public class NGramCachedNonSymmGraphComparator extends NGramCachedGraphComparator {
    /***
     *Returns the similarity of the document n-gram graph oFirst when compared
     * to oSecond. This comparator is <b>not symmetric</b>.
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
                sSimilLevel.ContainmentSimilarity += 1.0 / (iFirstTotalEdges *
                        Math.max(1.0, dFinalDegredation));
                
                // Value difference metrics
                double dFirstData = weEdge.getWeight();
                double dOtherData = weFound.getWeight();
                
                // Take degredation into account
                sSimilLevel.ValueSimilarity += (gr.demokritos.iit.jinsect.utils.min(dFirstData, dOtherData) /
                        gr.demokritos.iit.jinsect.utils.max(dFirstData, dOtherData)) / (iFirstTotalEdges *
                        Math.max(1.0, 
                            (dgSecond.degredationDegree(weFound) + dgFirst.degredationDegree(weEdge))));
                
                // TODO : Use progress indication
                if (iProgress % 500 == 0)
                    if (Listener != null) {
                        Listener.Notify(this, new Double(100.0 * ((double)iProgress / iMinEdges)));
                    }
            }
            sSimilLevel.SizeSimilarity = (double)iMinEdges / gr.demokritos.iit.jinsect.utils.max(iFirstTotalEdges, 1.0);
            
            // Summarize
            sSimil.ValueSimilarity += sSimilLevel.ValueSimilarity * iLevelImportance / iOverallImportance;
            sSimil.ContainmentSimilarity += sSimilLevel.ContainmentSimilarity * iLevelImportance / iOverallImportance;
            sSimil.SizeSimilarity += sSimilLevel.SizeSimilarity * iLevelImportance / iOverallImportance;            
            ////////////
            
        }
        
        return sSimil;
    }    
    
}
    
