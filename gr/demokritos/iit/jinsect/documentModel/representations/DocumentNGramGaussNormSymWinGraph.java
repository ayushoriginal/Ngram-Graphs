/*
 * DocumentNGramGaussNormSymWinGraph.java
 *
 * Created on June 13, 2007, 6:37 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.representations;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/**
 *
 * @author ggianna
 */
public class DocumentNGramGaussNormSymWinGraph extends DocumentNGramGaussNormGraph {

    static final double Visibility = 3.0; // The width of the actual window, as a factor over the
                                          // given window
    /** Creates a new instance of INSECTDocumentGraph */
    public DocumentNGramGaussNormSymWinGraph() {
        InitGraphs();
    }
    
    /***
     * Creates a new instance of INSECTDocumentGraph 
     * @param iMinSize The minimum n-gram size
     * @param iMaxSize The maximum n-gram size
     * @param iCorrelationWindow The standard deviation of the Gaussian scaling function to use when
     * determining neighbouring weights.
     */
    public DocumentNGramGaussNormSymWinGraph(int iMinSize, int iMaxSize, int iCorrelationWindow) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        CorrelationWindow = iCorrelationWindow;
        
        InitGraphs();
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
                int iFrom = (iListSize - (int)(CorrelationWindow * Visibility) - 1) >= 0 ? 
                    iListSize - (int)(CorrelationWindow * Visibility) - 1 : 0;
                createSymEdgesConnecting(gGraph, sCurNGram, 
                        gr.demokritos.iit.jinsect.utils.reverseList(lNGramSequence.subList(iFrom, iTo)), hTokenAppearence);
                
            }
        }
        
        // 2nd pass. Create graph. OBSOLETE
        ///////////////////////////////
        // For all sizes create corresponding levels
//        for (int iNGramSize = MinSize; iNGramSize <= MaxSize; iNGramSize++)
//        {
//            // If n-gram bigger than text
//            if (iLen < iNGramSize)
//                // then Ignore
//                continue;
//            
//            Vector PrecedingNeighbours = new Vector();
//            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iNGramSize);
//            
//            // The String has a size of at least [iNGramSize]
//            String sCurNGram = "";
//            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
//            {
//                // If reached end
//                if (iLen < iCurStart + iNGramSize)
//                    // then break
//                    break;
//                
//                // Get n-gram                
//                sCurNGram = sUsableString.substring(iCurStart, iCurStart + iNGramSize);
//                // Evaluate word
//                if (WordEvaluator != null)
//                    if (!WordEvaluator.evaluateWord(sCurNGram))
//                        // and ignore if it does not evaluate
//                        continue;
//                String[] aFinalNeighbours;
//                // Normalize
//                if (Normalizer != null)
//                    aFinalNeighbours = (String[])Normalizer.normalize(null, PrecedingNeighbours.toArray());
//                else
//                {
//                    aFinalNeighbours = new String[PrecedingNeighbours.size()];
//                    PrecedingNeighbours.toArray(aFinalNeighbours);
//                }
//                // Select the middle element as basic vertex
//                if (PrecedingNeighbours.size() == 2 * CorrelationWindow * Visibility + 1) {
//                    LinkedList lTmp = new LinkedList(java.util.Arrays.asList(aFinalNeighbours));
//                    lTmp.remove((int)(CorrelationWindow * Visibility));
//                    createEdgesConnecting(gGraph, aFinalNeighbours[(int)(CorrelationWindow * Visibility)], 
//                            lTmp, hTokenAppearence);
//                };
//                
//                PrecedingNeighbours.add(sCurNGram);
//                // Take neighbours into account up to Visibility times the stdev
//                if (PrecedingNeighbours.size() > 2 * CorrelationWindow * Visibility + 1)
//                    PrecedingNeighbours.removeElementAt(0);// Remove first element
//            }
//            
//        }        
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
        double dStartWeight = 0;
        double dIncreaseWeight = 0;
        
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
        List lOldEdges;
        lOldEdges = gGraph.getEdges(vA);
        
        //////////!!!!!!!!!!!!/////////
        // TODO: MAKE SURE the order of neighbouring vertices corresponds to their distance.
        //////////!!!!!!!!!!!!/////////
        
        int iCnt=0;
        // For every edge
        while (iIter.hasNext())
        {
            VertexImpl vB = new VertexImpl();
            vB.setLabel((String)iIter.next());
            
            double dOldWeight = 0;
            double dNewWeight = 0;
            //dStartWeight = 2.0 / (((Double)hAppearenceHistogram.get(vA.getLabel())).doubleValue() +
                    //((Double)hAppearenceHistogram.get(vB.getLabel())).doubleValue());
            dStartWeight = ScalingFunction(++iCnt);
            dIncreaseWeight = dStartWeight;
            //WeightedEdge weCorrectEdge = (WeightedEdge)jinsect.utils.locateDirectedEdgeInGraph(gGraph, vA, vB);
            
            if (eclLocator == null)
                eclLocator = new EdgeCachedLocator(10);
            // Add one-way edge
            WeightedEdge weCorrectEdge = (WeightedEdge)eclLocator.locateDirectedEdgeInGraph(gGraph, vA, vB);
            
            if (weCorrectEdge == null)
                // Not found. Using Start weight
                dNewWeight = dStartWeight;
            else {
                dOldWeight = weCorrectEdge.getWeight();
                dNewWeight = dOldWeight + dIncreaseWeight; // Increase as required
            }
            
            try
            {
                if (weCorrectEdge == null) {
                    WeightedEdge e = gGraph.addEdge(vA, vB, dNewWeight);
                    eclLocator.addedEdge(e);
                }
                else
                    weCorrectEdge.setWeight(dNewWeight);
            }
            catch (Exception e)
            {
                // Unknown error
                e.printStackTrace();
            }
            
            // Add reverse edge
            weCorrectEdge = (WeightedEdge)eclLocator.locateDirectedEdgeInGraph(gGraph, vB, vA);
            
            if (weCorrectEdge == null)
                // Not found. Using Start weight
                dNewWeight = dStartWeight;
            else {
                dOldWeight = weCorrectEdge.getWeight();
                dNewWeight = dOldWeight + dIncreaseWeight; // Increase as required
            }
            
            try
            {
                if (weCorrectEdge == null) {
                    WeightedEdge e = gGraph.addEdge(vB, vA, dNewWeight);
                    eclLocator.addedEdge(e);
                }
                else
                    weCorrectEdge.setWeight(dNewWeight);
            }
            catch (Exception e)
            {
                // Unknown error
                e.printStackTrace();
            }

        }
    }
    
    /***
     * Creates an edge in [gGraph] connecting [sBaseNode] to each node in the
     *[lOtherNodes] list of nodes. If an edge exists, its weight is increased by [iIncreaseWeight],
     *else its weight is set to [iStartWeight]
     *@param gGraph The graph to use
     *@param sStartNode The node from which all edges begin
     *@param lOtherNodes The list of nodes to which sBaseNode is connected
     *@param hAppearenceHistogram The histogram of appearences of the terms
    ***/
    public void createEdgesConnecting(UniqueVertexGraph gGraph, String sStartNode, List lOtherNodes,
            HashMap hAppearenceHistogram) {
        double dStartWeight = 0;
        double dIncreaseWeight = 0;
        
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
                    e.printStackTrace();
                }
                return;
            }
        
        // Otherwise for every neighbour add edge
        java.util.Iterator iIter = lOtherNodes.iterator();
        
        // Locate source node
        Vertex vOldA = utils.locateVertexInGraph(gGraph, sStartNode);
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
        List lOldEdges;
        lOldEdges = gGraph.getEdges(vA);
        
        //////////!!!!!!!!!!!!/////////
        // TODO: MAKE SURE the order of neighbouring vertices corresponds to their distance.
        //////////!!!!!!!!!!!!/////////
        
        int iCnt=0;
        // For every edge
        while (iIter.hasNext())
        {
            VertexImpl vB = new VertexImpl();
            vB.setLabel((String)iIter.next());
            
            double dOldWeight = 0;
            double dNewWeight = 0;
            //dStartWeight = 2.0 / (((Double)hAppearenceHistogram.get(vA.getLabel())).doubleValue() +
                    //((Double)hAppearenceHistogram.get(vB.getLabel())).doubleValue());
            dStartWeight = ScalingFunction(Math.abs(++iCnt - (lOtherNodes.size() / 2)));
            dIncreaseWeight = dStartWeight;
            //WeightedEdge weCorrectEdge = (WeightedEdge)jinsect.utils.locateDirectedEdgeInGraph(gGraph, vA, vB);
            if (eclLocator == null)
                eclLocator = new EdgeCachedLocator(10);
            WeightedEdge weCorrectEdge = (WeightedEdge)eclLocator.locateDirectedEdgeInGraph(gGraph, vA, vB);
            
            if (weCorrectEdge == null)
                // Not found. Using Start weight
                dNewWeight = dStartWeight;
            else {
                dOldWeight = weCorrectEdge.getWeight();
                dNewWeight = dOldWeight + dIncreaseWeight; // Increase as required
            }
            
            try
            {
                if (weCorrectEdge == null) {
                    WeightedEdge e = gGraph.addEdge(vA, vB, dNewWeight);
                    eclLocator.addedEdge(e);
                }
                else
                    weCorrectEdge.setWeight(dNewWeight);
            }
            catch (Exception e)
            {
                // Unknown error
                e.printStackTrace();
            }
        }

    }
    
    public static void main(String args[]) {
        DocumentNGramGaussNormSymWinGraph ngs = new DocumentNGramGaussNormSymWinGraph(3,3,2);
        //ngs.setDataString("This is a great test, isn't it?");
        ngs.setDataString("This is a test");
        
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(ngs.getGraphLevel(0), true));

    }

}
