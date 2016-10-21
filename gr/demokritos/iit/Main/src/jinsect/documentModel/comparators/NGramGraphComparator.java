/*
 * NGramGraphComparator.java
 *
 * Created on 25 Ιανουάριος 2006, 3:23 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.events.Notifier;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import salvo.jesus.graph.WeightedEdge;

/** An n-gram graph comparison performing class, the compares DocumentNGramGraph objects.
 *
 * @author PCKid
 */
public class NGramGraphComparator implements SimilarityComparatorListener, Notifier {
    protected NotificationListener Listener = null;
    /**
     * Creates a new instance of NGramGraphComparator.
     */
    public NGramGraphComparator() {
    }
    
    /***
     *Returns the similarity between two document n-gram graphs.
     *@param oFirst The first document n-gram graph.
     *@param oSecond The second document n-gram graph.
     *@return A {@link GraphSimilarity} object, giving a measure of similarity between the 
     * two graphs.
     *@see DocumentNGramGraph
     ***/
    public GraphSimilarity getSimilarityBetween(Object oFirst, Object oSecond) {
        // Initialize variables
        GraphSimilarity sSimil = new GraphSimilarity();
        DocumentNGramGraph dgFirst = (DocumentNGramGraph)oFirst;
        DocumentNGramGraph dgSecond = (DocumentNGramGraph)oSecond;
        
        // Use a weight for every level. Larger n-gram have higher weight.
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
            
            // For every edge of the first graph
            java.util.Iterator iIter = ngFirstGraph.getEdgeSet().iterator();
            int iProgress = 0;
            while (iIter.hasNext())
            {
                // Get edge
                WeightedEdge weEdge = (WeightedEdge)iIter.next();
                iProgress++;
                
                // If edge does not exist in other graph
                WeightedEdge weFound = (WeightedEdge)gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(ngSecondGraph, weEdge.getVertexA(), 
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
                
                // Take degredation into account
                sSimilLevel.ValueSimilarity += (gr.demokritos.iit.jinsect.utils.min(dFirstData, dOtherData) /
                        gr.demokritos.iit.jinsect.utils.max(dFirstData, dOtherData)) / (iMaxEdges *
                        Math.max(1.0, 
                            (dgSecond.degredationDegree(weFound) + dgFirst.degredationDegree(weEdge))));
                
                if (Listener != null) {
                    Listener.Notify(this, new Double(((double)iProgress / iMinEdges)));
                }
            }
            sSimilLevel.SizeSimilarity = iMinEdges / gr.demokritos.iit.jinsect.utils.max(iMaxEdges, 1.0);
            
            // Summarize
            sSimil.ValueSimilarity += sSimilLevel.ValueSimilarity * iLevelImportance / iOverallImportance;
            sSimil.ContainmentSimilarity += sSimilLevel.ContainmentSimilarity * iLevelImportance / iOverallImportance;
            sSimil.SizeSimilarity += sSimilLevel.SizeSimilarity * iLevelImportance / iOverallImportance;            
            ////////////
        }
        return sSimil;
    }    

    /** Sets the notification listener, that accepts progress report info.
     *@param nlListener The listener object.
     *@see NotificationListener
     */
    public void setNotificationListener(NotificationListener nlListener) {
        Listener = nlListener;
    }

    /** Clears the notification listener, that accepts progress report info, so that
     *  no reporting is made.
     *@see NotificationListener
     */
    public void removeNotificationListener() {
        Listener = null;
    }

    /** Returns the notification listener used for progress report info.
     *@return The notification listener, or null value if none is assigned.
     *@see NotificationListener
     */
    public NotificationListener getNotificationListener() {
        return Listener;
    }
    
}
