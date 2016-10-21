/*
 * DocumentNGramSymWinDistroGraph.java
 *
 * Created on December 4, 2007, 2:36 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.representations;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedDistroGraphComparator;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/**
 *
 * @author ggianna
 */
public class DocumentNGramSymWinDistroGraph extends DocumentNGramDistroGraph {
    EdgeCachedLocator eclLocator = null;
    
    /**
     * Creates a new instance of DocumentNGramSymWinDistroGraph
     */
    public DocumentNGramSymWinDistroGraph() {
        InitGraphs();
    }
    
    public DocumentNGramSymWinDistroGraph(int iMinSize, int iMaxSize, int iCorrelationWindow) {
        super(iMinSize, iMaxSize, iCorrelationWindow);
    }
    
    public void createGraphs() {       
        String sUsableString = new StringBuilder().append(DataString).toString();
        
        // Use preprocessor if available
        if (TextPreprocessor != null)
            sUsableString = TextPreprocessor.preprocess(sUsableString);
        // else
            // sUsableString = new String(sUsableString);
        
        int iLen = DataString.length();
        // Create token histogram.
        HashMap hTokenAppearence = new HashMap();
        // 1st pass. Populate histogram.
        ///////////////////////////////
        // For all sizes create corresponding levels
        for (int iNGramSize = MinSize; iNGramSize <= MaxSize; iNGramSize++)
        {
            // If n-gram bigger than text
            if (iLen < iNGramSize)
                // then Ignore
                continue;
            
            // The String has a size of at least [iNGramSize]
            String sCurNGram = null;
            LinkedList lNGramSequence = new LinkedList();
            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iNGramSize);
            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
            {
                // If reached end
                if (iLen < iCurStart + iNGramSize)
                    // then break
                    break;
                
                // Get n-gram                
                sCurNGram = sUsableString.substring(iCurStart, iCurStart + iNGramSize);
                // Evaluate word
                if (WordEvaluator != null)
                    if (!WordEvaluator.evaluateWord(sCurNGram))
                        // and ignore if it does not evaluate
                        continue;
                
                // Update Histogram
                if (hTokenAppearence.containsKey(sCurNGram))
                    hTokenAppearence.put(sCurNGram, ((Double)hTokenAppearence.get(sCurNGram)).doubleValue() + 1.0);
                else
                    hTokenAppearence.put(sCurNGram, 1.0);
                
                // Update list of n-grams
                lNGramSequence.add(sCurNGram);
                // Update graph
                int iListSize = lNGramSequence.size();
                int iTo = (iListSize - 1) >= 0 ? iListSize - 1 : 0;
                int iFrom = (iListSize - CorrelationWindow - 1) >= 0 ? 
                    iListSize - CorrelationWindow - 1 : 0;
                createSymEdgesConnecting(gGraph, sCurNGram, 
                        gr.demokritos.iit.jinsect.utils.reverseList(lNGramSequence.subList(iFrom, iTo)), hTokenAppearence);
            }
        }
        
    }
    
    /***
     * Creates an edge in [gGraph] connecting [sStartNode] to each node in the
     *[lOtherNodes] list of nodes, as well as other nodes to [sBaseNode]. 
     *If an edge exists, its weight is increased by [iIncreaseWeight],
     *else its weight is set to [iStartWeight]
     *@param gGraph The graph to use
     *@param sStartNode The node from which all edges begin
     *@param lOtherNodes The list of nodes to which sBaseNode is connected. The list MUST BE ORDERED ASCENDINGLY 
     * based on distance from the <code>sStartNode</code>.
     *@param hAppearenceHistogram The histogram of appearences of the terms
    ***/
    public void createSymEdgesConnecting(UniqueVertexGraph gGraph, String sStartNode, List lOtherNodes,
            HashMap hAppearenceHistogram) {
        
        // If no neightbours
        if (lOtherNodes != null)
            if (lOtherNodes.size() == 0)
            {
                // Attempt to add solitary node [sStartNode]
                VertexImpl v = new VertexImpl();
                v.setLabel(sStartNode);
                try {
                    gGraph.add(v);    
                }
                catch (Exception e) {
                    // Probably exists already
                    e.printStackTrace(System.err);
                }
                return;
            }
        
        // Otherwise for every neighbour add edge
        java.util.Iterator iIter = lOtherNodes.iterator();
        
        // Locate source node
        Vertex vOldA = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(gGraph, sStartNode);
        Vertex vA;
        if (vOldA != null)
            vA = vOldA;
        else {
            // else create it
            vA = new VertexImpl();
            vA.setLabel(sStartNode);
            // Add to graph
            try {
                gGraph.add(vA);
            }
            catch (Exception e) {
                // Not added. Ignore.
            }
            
        }
            
        
        // Get old edges including vA as a vertex
        // List lOldEdges;
        // lOldEdges = gGraph.getEdges(vA);
        
        //////////!!!!!!!!!!!!/////////
        // TODO: MAKE SURE the order of neighbouring vertices corresponds to their distance.
        //////////!!!!!!!!!!!!/////////
        
        int iCnt=0;
        // For every edge
        while (iIter.hasNext())
        {
            VertexImpl vB = new VertexImpl();
            vB.setLabel((String)iIter.next());
            
//            double dOldWeight = 0;
//            double dNewWeight = 0;
            //dStartWeight = 2.0 / (((Double)hAppearenceHistogram.get(vA.getLabel())).doubleValue() +
                    //((Double)hAppearenceHistogram.get(vB.getLabel())).doubleValue());
            //WeightedEdge weCorrectEdge = (WeightedEdge)jinsect.utils.locateDirectedEdgeInGraph(gGraph, vA, vB);
            
            if (eclLocator == null)
                eclLocator = new EdgeCachedLocator(10);
            // Add one-way edge
            WeightedEdge weCorrectEdge = (WeightedEdge)eclLocator.locateDirectedEdgeInGraph(gGraph, vA, vB);
            
            Distribution dDist = null;
            if (weCorrectEdge == null) {
                // Not found. Using Start weight
//                dNewWeight = dStartWeight;
                
                dDist = new Distribution();
            }
            else {
//                dOldWeight = weCorrectEdge.getWeight();
//                dNewWeight = dOldWeight + dIncreaseWeight; // Increase as required
                
                // Get existing distribution, if it exists
                dDist = (Distribution)EdgeDistros.get(weCorrectEdge);
                // else add new.
                if (dDist == null) {
                    dDist = new Distribution();
                }
            }
            dDist.increaseValue(Double.valueOf(++iCnt), 1);
            
            try
            {
                if (weCorrectEdge == null) {
                    WeightedEdge e = gGraph.addEdge(vA, vB, 0.0);
                    eclLocator.addedEdge(e);
                    EdgeDistros.put(e, dDist);
                }
                else {
//                    weCorrectEdge.setWeight(dNewWeight);
                    EdgeDistros.put(weCorrectEdge, dDist);
                }
            }
            catch (Exception e)
            {
                // Unknown error
                e.printStackTrace();
            }
            
            // Add reverse edge
            weCorrectEdge = (WeightedEdge)eclLocator.locateDirectedEdgeInGraph(gGraph, vB, vA);
            
            if (weCorrectEdge == null) {
                // Not found. Using Start weight
//                dNewWeight = dStartWeight;
                dDist = new Distribution();
            }
//            else {
//                dOldWeight = weCorrectEdge.getWeight();
//                dNewWeight = dOldWeight + dIncreaseWeight; // Increase as required
//                dNewWeight = 0.0;
//            }
            
            dDist.increaseValue(Double.valueOf(iCnt), 1);
            
            try
            {
                if (weCorrectEdge == null) {
                    WeightedEdge e = gGraph.addEdge(vB, vA, 0.0);
                    eclLocator.addedEdge(e);
                    EdgeDistros.put(e, dDist);
                }
                else {
                    // Get existing distribution, if it exists
                    dDist = (Distribution)EdgeDistros.get(weCorrectEdge);
                    // else add new.
                    if (dDist == null) {
                        dDist = new Distribution();
                    }                    
                    weCorrectEdge.setWeight(0.0);
                    EdgeDistros.put(weCorrectEdge, dDist);                    
                }
            }
            catch (Exception e)
            {
                // Unknown error
                e.printStackTrace();
            }

                        
        }
    }
    
    
    public static void main(String[] sArgs) {
        String s = "112233";
        DocumentNGramSymWinDistroGraph g = new DocumentNGramSymWinDistroGraph(1,1,2);
        g.setDataString(s);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(g.getGraphLevel(0), true, g.getEdgesToDistros()));
        String s2 = "1122";
        DocumentNGramDistroGraph g2 = new DocumentNGramSymWinDistroGraph(1,1,2);
        g2.setDataString(s2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(g2.getGraphLevel(0), true, g2.getEdgesToDistros()));
        // Intersect
        DocumentNGramDistroGraph[] gAll = g.intersectAndDeltaDistroGraph(g2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(gAll[0].getGraphLevel(0), true,
                gAll[0].getEdgesToDistros()));
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(gAll[1].getGraphLevel(0), true,
                gAll[1].getEdgesToDistros()));
        
        NGramCachedDistroGraphComparator comp = new NGramCachedDistroGraphComparator();
        System.out.println("\nIntersect and Delta Similarity:\n" + comp.getSimilarityBetween(gAll[0], gAll[1]));
    }
}
