/*
 * INSECTDocumentGraph.java
 *
 * Created on 24 ?????????? 2006, 10:33 ??
 *
 */

package gr.demokritos.iit.jinsect.documentModel.representations;
import gr.demokritos.iit.jinsect.structs.IMergeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import gr.demokritos.iit.jinsect.events.NormalizerListener;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.jinsect.events.TextPreprocessorListener;
import java.util.Arrays;
import java.util.List;
import salvo.jesus.graph.*;

/** Represents the graph of a document, with vertices n-grams of the document and edges the number
 * of the n-grams' co-occurences within a given window.
 *
 * @author PCKid
 */
public class DocumentNGramGraph implements Serializable, Cloneable, IMergeable<DocumentNGramGraph> {
    /** The minimum and maximum n-gram size, and the cooccurence window.
     * Default values are 3, 3, 3 correspondingly.
     */
    protected int MinSize = 3, MaxSize = 3, CorrelationWindow = 3;
    protected String DataString = "";
    protected HashMap DegradedEdges;
    
    public NormalizerListener Normalizer = null;
    public WordEvaluatorListener WordEvaluator = null;
    public TextPreprocessorListener TextPreprocessor = null;
    
    protected UniqueVertexGraph[] NGramGraphArray;
    protected EdgeCachedLocator eclLocator = null;

    /** Creates a new instance of INSECTDocumentGraph */
    public DocumentNGramGraph() {
        InitGraphs();
    }
    
    /***
     * Creates a new instance of INSECTDocumentGraph 
     * @param iMinSize The minimum n-gram size
     * @param iMaxSize The maximum n-gram size
     * @param iCorrelationWindow The maximum distance of terms to be considered
     * as correlated.
     */
    public DocumentNGramGraph(int iMinSize, int iMaxSize, int iCorrelationWindow) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        CorrelationWindow = iCorrelationWindow;
        
        InitGraphs();
    }
    
    protected void InitGraphs() {
        // Create array of graphs
        NGramGraphArray = new UniqueVertexGraph[MaxSize - MinSize + 1];
        // Init array
        for (int iCnt=MinSize; iCnt <= MaxSize; iCnt++)
            NGramGraphArray[iCnt - MinSize] = new UniqueVertexGraph();
        // Create degraded edge list
        DegradedEdges = new HashMap();        
    }
    
    /** Measures an indication of the size of a document n-gram graph based on 
     * the edge count of its contained graphs.
     * 
     * @return The sum of the count of the edges of the various level graphs in
     * the document n-gram graph.
     */
    public int length() {
        java.util.Iterator iIter = java.util.Arrays.asList(NGramGraphArray).iterator();
        int iCnt = 0;
        while (iIter.hasNext())
            iCnt += ((UniqueVertexGraph)iIter.next()).getEdgesCount();
        return  iCnt;
    }
    
    public boolean isEmpty() {
        return NGramGraphArray[0].getEdgesCount()== 0;
    }
    
    /** Creates the graph based on a data string loaded from a given file.
     *@param sFilename The filename of the file containing the data string.
     */
    public void loadDataStringFromFile(String sFilename) throws java.io.IOException,
            java.io.FileNotFoundException{        
        String sDataString = utils.loadFileToStringWithNewlines(sFilename);
        setDataString(sDataString); // Actually update
    }
    
    /***
     *Returns graph with M-based index
     *@param iIndex The index of the graph. Zero (0) equals to the graph for 
     * level MinSize n-grams.
     *@return The {@link UniqueVertexGraph} of the corresponding level.
     ***/
    public UniqueVertexGraph getGraphLevel(int iIndex) {
        return NGramGraphArray[iIndex];
    }

    /***
     *Returns graph with n-gram-size-based index
     *@param iNGramSize The n-gram size of the graph. 
     *@return The {@link UniqueVertexGraph} of the corresponding level.
     ***/
    public UniqueVertexGraph getGraphLevelByNGramSize(int iNGramSize) {
        // Check bounds
        if ((iNGramSize < MinSize) || (iNGramSize > MaxSize))
            return null;
        
        return NGramGraphArray[iNGramSize - MinSize];
    }

    public HashSet getAllNodes() {
        HashSet hRes = new HashSet(length() / (MaxSize - MinSize)); // Init set
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++)
        {
            java.util.Iterator iIter = NGramGraphArray[iCurLvl - 
                    MinSize].getEdgeSet().iterator();
            
            while (iIter.hasNext())
                hRes.add(iIter.next());
        }
        
        return hRes;
    }
    
    /**
     * Set a locator to optimize the edge lookup.
     * @param eNewLocator The locator to use.
     */
    public void setLocator(EdgeCachedLocator eNewLocator) {
        eclLocator = eNewLocator;
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
        Vertex vOldA = gGraph.locateVertex(new VertexImpl(sStartNode));
        // DEPRECATED: Vertex vOldA = utils.locateVertexInGraph(gGraph, sStartNode);
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
            
        
        EdgeCachedLocator ecl;
        if (eclLocator == null)
            ecl = new EdgeCachedLocator(100);
        else
            ecl = eclLocator;
        
        // For every edge
        while (iIter.hasNext())
        {
            VertexImpl vB = new VertexImpl();
            vB.setLabel((String)iIter.next());
            
            double dOldWeight = 0;
            double dNewWeight = 0;
            //dStartWeight = 2.0 / (((Double)hAppearenceHistogram.get(vA.getLabel())).doubleValue() +
                    //((Double)hAppearenceHistogram.get(vB.getLabel())).doubleValue());
            dStartWeight = 1.0;
            dIncreaseWeight = dStartWeight;
            WeightedEdge weCorrectEdge = (WeightedEdge)ecl.locateDirectedEdgeInGraph(
                    gGraph, vA, vB);
            
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
                    ecl.addedEdge(gGraph.addEdge(vA, vB, dNewWeight));
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
     *@param dStartWeight The initial weight for first-occuring nodes
     *@param dNewWeight The new weight
     *@param dDataImportance The tendency towards the new value. 0.0 means no change
     *to the current value. 1.0 means the old value is completely replaced by the
     *new. 0.5 means the final value is the average of the old and the new.
    ***/
    public void createWeightedEdgesConnecting(UniqueVertexGraph gGraph,
            String sStartNode, List lOtherNodes,
            double dStartWeight, double dNewWeight, double dDataImportance) {
        
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
                
            }
        
        // Locate or create source node
        Vertex vA = gGraph.locateVertex(sStartNode);
        if (vA == null) {
            vA = new VertexImpl();
            vA.setLabel(sStartNode);
            try {
                gGraph.add(vA);
            }
            catch (Exception e) {
                // Add failed. Ignore
            }
        }

        EdgeCachedLocator ecl;
        if (eclLocator == null)
            ecl = new EdgeCachedLocator(100);
        else
            ecl = eclLocator;
        
        // Otherwise for every neighbour add edge
        java.util.Iterator iIter = lOtherNodes.iterator();
        // For every edge
        while (iIter.hasNext())
        {
            VertexImpl vB = new VertexImpl();
            vB.setLabel(new String((String)iIter.next()));
            
            double dOldWeight = 0;
            double dFinalWeight = 0;
            WeightedEdge weCorrectEdge = null;
            
            // Get old weight
            WeightedEdge weEdge = null;
            // Look for SAME ORIENTATION OF EDGE
            boolean bFound = (weEdge =
                    (WeightedEdge)ecl.locateDirectedEdgeInGraph(gGraph, vA, vB))
                    != null;
            if (bFound)
            {
                dOldWeight = weEdge.getWeight();
                // Found edge should break to avoid redundancy
                weCorrectEdge = weEdge;
                dFinalWeight = dOldWeight + (dNewWeight - dOldWeight)
                        * dDataImportance; // Increase as required
                weCorrectEdge.setWeight(dFinalWeight);
            }
            else
            {
                // Not found. New edge.
                dFinalWeight = dStartWeight;
                try {
                    gGraph.addEdge(vA, vB, dFinalWeight);
                    ecl.resetCache();
                }
                catch (Exception e) {
                    // Insert failed. Ignoring...
                    // TODO: Check if it needs to be removed
                    e.printStackTrace();
                }
            }
            // DEBUG LINES
            // if (dFinalWeight < 0)
            //    System.err.println("Negative weight.");
            //////////////
        }
    }

    /***
     * Creates the graph of n-grams, for all the levels specified in the MinSize, MaxSize range.
    ***/

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
                
            }
        }
        
        // 2nd pass. Create graph.
        ///////////////////////////////
        // For all sizes create corresponding levels
        for (int iNGramSize = MinSize; iNGramSize <= MaxSize; iNGramSize++)
        {
            // If n-gram bigger than text
            if (iLen < iNGramSize)
                // then Ignore
                continue;
            
            Vector PrecedingNeighbours = new Vector();
            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iNGramSize);
            
            // The String has a size of at least [iNGramSize]
            String sCurNGram = "";
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
                String[] aFinalNeighbours;
                // Normalize
                if (Normalizer != null)
                    aFinalNeighbours = (String[])Normalizer.normalize(null, PrecedingNeighbours.toArray());
                else
                {
                    aFinalNeighbours = new String[PrecedingNeighbours.size()];
                    PrecedingNeighbours.toArray(aFinalNeighbours);
                }
                createEdgesConnecting(gGraph, sCurNGram, java.util.Arrays.asList(aFinalNeighbours), 
                        hTokenAppearence);
                
                PrecedingNeighbours.add(sCurNGram);
                if (PrecedingNeighbours.size() > CorrelationWindow)
                    PrecedingNeighbours.removeElementAt(0);// Remove first element
            }
        }        
    }
    
/***
     *Merges the data of [dgOtherGraph] document graph to the data of this graph, 
     *by adding all existing edges and moving the values of those existing in both graphs
     *towards the new graph values based on a tendency modifier. 
     *The convergence tendency towards the starting value or the new value is determined 
     *by [fWeightPercent]. 
     *@param dgOtherGraph The second graph used for the merging
     *@param fWeightPercent The convergence tendency parameter. A value of 0.0 
     * means no change to existing value, 1.0 means new value is the same as 
     * that of the new graph. A value of 0.5 means new value is exactly between 
     * the old and new value (average).
    ***/
    public void mergeGraph(DocumentNGramGraph dgOtherGraph, double fWeightPercent) {
        // If both graphs are the same, ignore merging.
        if (dgOtherGraph == this)
            return;
        
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gOtherGraph = dgOtherGraph.getGraphLevelByNGramSize(iCurLvl);
            // Check if other graph has corresponding level
            if (gOtherGraph == null)
                // If not, ignore level
                continue;

            // For every edge on other graph
            java.util.Iterator iIter = gOtherGraph.getEdgeSet().iterator();
            ArrayList<String> lOtherNodes = new ArrayList<String>();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                String sHead = weCurItem.getVertexA().getLabel();
                String sTail = weCurItem.getVertexB().getLabel();
                double dWeight = weCurItem.getWeight();
                lOtherNodes.clear();
                lOtherNodes.add(sTail);
                // TODO: Check this
                createWeightedEdgesConnecting(gGraph, sHead,
                 lOtherNodes, dWeight, dWeight, fWeightPercent);
            }

            // DONE: Remove multi-threading
//            // Multi-threading
//            ThreadQueue tq = new ThreadQueue();
//            // For every edge on other graph
//            java.util.Iterator iIter = gOtherGraph.getEdgeSet().iterator();
//            while (iIter.hasNext())
//            {
//                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
//                final String sHead = weCurItem.getVertexA().getLabel();
//                final String sTail = weCurItem.getVertexB().getLabel();
//                final double dWeight = weCurItem.getWeight();
//                final String[] lOtherNodes = new String[1];
//                lOtherNodes[0] = sTail;
//                final UniqueVertexGraph graphArg = gGraph;
//                final double dWeightPercentArg = fWeightPercent;
//
//                while (!tq.addThreadFor(new Runnable() {
//                    @Override
//                    public void run() {
//                        synchronized (graphArg) {
//                            createWeightedEdgesConnecting(graphArg, sHead,
//                             java.util.Arrays.asList(lOtherNodes), 1.0, dWeight,
//                             dWeightPercentArg);
//                        }
//                    }
//                }))
//                    Thread.yield();
//            }
//
//            try {
//                tq.waitUntilCompletion();
//            }
//            catch (InterruptedException ie) {
//                // Do nothing
//            }
        }
    }
    
    
    public DocumentNGramGraph intersectGraph(DocumentNGramGraph dgOtherGraph) {
        // Init res graph
        DocumentNGramGraph gRes = new DocumentNGramGraph(MinSize, MaxSize, CorrelationWindow);
        
        // Use cached edge locator
        EdgeCachedLocator ecl = new EdgeCachedLocator(1000);
        
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gOtherGraph = dgOtherGraph.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gNewGraph = gRes.getGraphLevelByNGramSize(iCurLvl);
            
            // Check if other graph has corresponding level
            if (gOtherGraph == null)
                // If not, ignore level
                continue;
            
            // For every edge on other graph
            java.util.Iterator iIter = gOtherGraph.getEdgeSet().iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                String sHead = weCurItem.getVertexA().getLabel();
                String sTail = weCurItem.getVertexB().getLabel();
                
                // TODO: Check if should be directed or not
                //WeightedEdge eEdge = (WeightedEdge)gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(gGraph, sHead, sTail);
                WeightedEdge eEdge = (WeightedEdge)ecl.locateEdgeInGraph(gGraph, 
                        weCurItem.getVertexA(),weCurItem.getVertexB());
                
                if (eEdge != null)
                    try
                    {
                        List l = new ArrayList();
                        l.add(sTail);
                        double dTargetWeight = 0.5 * (eEdge.getWeight() + weCurItem.getWeight());

                        // Initialize with mean weight
                        createWeightedEdgesConnecting(gNewGraph, sHead, l, dTargetWeight, dTargetWeight, 1.0);
                        // Used to be
                        //createWeightedEdgesConnecting(gNewGraph, sHead, l,1, eEdge.getWeight(), 1.0);
                    }
                    catch (Exception e)
                    {
                        // Non fatal error occured. Continue.
                        e.printStackTrace();                        
                    }
            }
        }                
        return gRes;
    }

    /** Returns the difference (inverse of the intersection) graph between the current graph 
     * and a given graph.
     *@param dgOtherGraph The graph to compare to.
     *@return A DocumentNGramGraph that is the difference between the current graph and the given graph.
     */
    public DocumentNGramGraph inverseIntersectGraph(DocumentNGramGraph dgOtherGraph) {
        
        // Get the union (merged) graph
        DocumentNGramGraph dgUnion = (DocumentNGramGraph)clone();
        dgUnion.mergeGraph(dgOtherGraph, 0);
        
        // Get the intersection graph
        DocumentNGramGraph dgIntersection = intersectGraph(dgOtherGraph);
        
        // For every level
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gUnion = dgUnion.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gIntersection = dgIntersection.getGraphLevelByNGramSize(iCurLvl);
            // TODO: Order by edge count for optimization
            EdgeCachedLocator eclLocator = new EdgeCachedLocator(10);
            
            // Check if other graph has corresponding level
            if (gIntersection == null)
                // If not, ignore level
                continue;            
            
            // For every edge of intersection
            java.util.Iterator iIter = gIntersection.getEdgeSet().iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                // If the edge is contained in the merged graph
                Edge eEdge = eclLocator.locateDirectedEdgeInGraph(gUnion, weCurItem.getVertexA(), 
                        weCurItem.getVertexB());
                if (eEdge != null)
                    
                    try {
                        gUnion.removeEdge(eEdge);
                    } catch (Exception ex) {
                        // Non-lethal exception. Continue.
                        ex.printStackTrace();
                    }
            }
        }
        
        return dgUnion;
        
    }
    
    /** Returns both the intersection and the difference (inverse of the intersection)
     * graph between the current graph and a given graph.
     *@param dgOtherGraph The graph to use for intersection and difference.
     *@return A DocumentNGramDistroGraph array of two elements. The first is the intersection between
     * the current graph and the given graph and the second is the difference of the graphs.
     * The edge distributions are kept from the original graphs.
     */
    public DocumentNGramGraph[] intersectAndDeltaGraph(DocumentNGramGraph dgOtherGraph) {

        DocumentNGramGraph dgUnion = null;
        // Initialize union using the biggest graph
        // and get the union (merged) graph
        if (dgOtherGraph.length() > length()) {
            dgUnion = (DocumentNGramGraph)dgOtherGraph.clone();
            dgUnion.merge(this, 0);
        }
        else {
            dgUnion = (DocumentNGramGraph)clone();
            dgUnion.merge(dgOtherGraph, 0);
        }

        
        
        DocumentNGramGraph[] res = new DocumentNGramGraph[2];

        // Get the intersection graph
        DocumentNGramGraph dgIntersection = intersectGraph(dgOtherGraph);
        res[0] = dgIntersection;

        // For every level
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gUnion = dgUnion.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gIntersection =
                    dgIntersection.getGraphLevelByNGramSize(iCurLvl);
            // TODO: Order by edge count for optimization
            EdgeCachedLocator eclLocator = new EdgeCachedLocator(100);

            // Check if other graph has corresponding level
            if (gIntersection == null)
                // If not, ignore level
                continue;

            // For every edge of intersection
            java.util.Iterator iIter = gIntersection.getEdgeSet().iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                // If the edge is contained in the merged graph
                Edge eEdge = eclLocator.locateDirectedEdgeInGraph(gUnion,
                        weCurItem.getVertexA(), weCurItem.getVertexB());
                if (eEdge != null)

                    try {
                        gUnion.removeEdge(eEdge);
                    } catch (Exception ex) {
                        // Non-lethal exception. Continue.
                        ex.printStackTrace();
                    }
            }
        }

        res[1] = dgUnion;
        return res;
    }

    public int getMinSize() {
        return MinSize;
    }

    public int getMaxSize() {
        return MaxSize;
    }
    
    public int getWindowSize() {
        return CorrelationWindow;
    }
    
    /***
     * Returns a functions of [element graph edges max],[number of neighbours], where
     * [element graph edges max] refers to the maximum weight of the edges including [sNode],
     * and [number of neightbours] is its number of neighbours in the graph.
     *@param sNode The node object the Coexistence Importance of which we calculate
     ***/
    public double calcCoexistenceImportance(String sNode) {
        VertexImpl v = new VertexImpl();
        v.setLabel(sNode);
        
        return calcCoexistenceImportance(v);
    }
    
    public double calcCoexistenceImportance(Vertex vNode) {
        double dRes = 0.0;
        
        int iNoOfNeighbours = 0;
        double dMaxEdgeWeight = 0;
        // Search all levels
        for (int iNGramSize=MinSize; iNGramSize <= MaxSize; iNGramSize++) {
            UniqueVertexGraph gCurLevel = getGraphLevelByNGramSize(iNGramSize);
            if (gCurLevel.containsVertex(vNode))                
            {
                // Keep max neighbours number
                List lEdges = gCurLevel.getEdges(vNode);
                int iTempNeighbours = lEdges.size();
                iNoOfNeighbours = (iTempNeighbours > iNoOfNeighbours) ? iTempNeighbours : iNoOfNeighbours;
                
                java.util.Iterator iIter = lEdges.iterator();
                while (iIter.hasNext())
                {
                    // Keep max edge weight
                    WeightedEdge weEdge = (WeightedEdge)iIter.next();
                    dMaxEdgeWeight = (weEdge.getWeight() > dMaxEdgeWeight) ? weEdge.getWeight() : dMaxEdgeWeight;
                }
            }
        }
        
        // Final calculation
        dRes = -200000.0; // Very low value
        if (dMaxEdgeWeight > 0) {
            if (iNoOfNeighbours > 0)
                dRes = Math.log10(Math.pow(2 * dMaxEdgeWeight, 2.5) / Math.max(1.0, Math.pow(iNoOfNeighbours / 2, 2)));
            else
                dRes = Math.log10(Math.pow(2 * dMaxEdgeWeight, 2.5));                
        }
        
        return dRes;
    }
    
    public void prune(double dMinCoexistenceImportance) {
        for (int iNGramSize=MinSize; iNGramSize <= MaxSize; iNGramSize++) {
            UniqueVertexGraph gCurLevel = getGraphLevelByNGramSize(iNGramSize);
            Vector vToRemove = new Vector();
            
            Iterator iIter = gCurLevel.getVerticesIterator();
            while (iIter.hasNext()) {
                Vertex vCur = (Vertex)iIter.next();
                if (calcCoexistenceImportance(vCur) < dMinCoexistenceImportance) {
                    vToRemove.add(vCur);
                }
            }
            
            // Actually remove
            iIter = vToRemove.iterator();
            while (iIter.hasNext())
            try {
                gCurLevel.remove((Vertex)iIter.next());
            }
            catch (Exception e) {
                // Ignore
            }
        }        
    }
    
    /***
     *Removes an item (node) from all graphs.
     *@param sItem The item to remove.
     ***/
    public void deleteItem(String sItem) {
        // From all levels
        for (int iNGramSize=MinSize; iNGramSize <= MaxSize; iNGramSize++) {
            UniqueVertexGraph gCurLevel = getGraphLevelByNGramSize(iNGramSize);
            Vertex v = utils.locateVertexInGraph(gCurLevel, sItem);
            if (v == null)
                return;
            try {
                gCurLevel.remove(v);
            }
            catch (Exception e) {
                e.printStackTrace(); // Probably node did not exist
            }
        }        
    }
    
    /***
     *Sets all weights in all graphs to zero
     ***/
    public void nullify() {
        // From all levels
        for (int iNGramSize=MinSize; iNGramSize <= MaxSize; iNGramSize++) {
            UniqueVertexGraph gCurLevel = getGraphLevelByNGramSize(iNGramSize);
            // Get all edges
            java.util.Iterator iIter = gCurLevel.getEdgeSet().iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weEdge = (WeightedEdge)iIter.next();
                // Set weight to zero
                weEdge.setWeight(0.0);
            }
        }                
    }
    
    public void setDataString(String sDataString) {
        DataString = new StringBuilder().append(sDataString).toString();
        InitGraphs();   // Clear graphs
        createGraphs(); // Update graphs        
    }
    
    public String getDataString() {
        return DataString;
    }

    // Serialization
  private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    // Write Fields
    out.writeInt(MinSize);
    out.writeInt(MaxSize);
    out.writeInt(CorrelationWindow);
    out.writeObject(DataString);

    // Save all graphs
    // For each graph
    for (int iCnt=MinSize; iCnt <= MaxSize; iCnt++) {

        UniqueVertexGraph g = getGraphLevelByNGramSize(iCnt);
        // Serialize
        out.writeObject(g);
    }
    // Update degredation
    out.writeObject(DegradedEdges);
   }
  
  private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
        // Read Fields
    try {
        MinSize = in.readInt();
        MaxSize = in.readInt();
        CorrelationWindow = in.readInt();
        DataString = (String)in.readObject();
//        DataString = "";

////        // DEBUG LINES
//        if (utils.Sum == 0) {
//            UniqueVertexGraph uT = new UniqueVertexGraph();
//            Runtime.getRuntime().gc();
//            utils.Sum = Runtime.getRuntime().freeMemory();
//            System.out.println("Starting free (MB):" +
//                Runtime.getRuntime().freeMemory() / (1024*1024));
//        }
//        //////////////
        // Create array of graphs
        NGramGraphArray = new UniqueVertexGraph[MaxSize - MinSize + 1];
        // For each graph
        for (int iCnt=MinSize; iCnt <= MaxSize; iCnt++) {
            // TODO: Restore
            UniqueVertexGraph g = (UniqueVertexGraph)in.readObject();
//            in.readObject();
//            UniqueVertexGraph g = null;
            
            this.NGramGraphArray[iCnt - MinSize] = g;

////            // DEBUG LINES
//            if (++utils.Count % 200 == 0) {
//                System.out.println("So far " + utils.Count);
//                Runtime.getRuntime().runFinalization();
//                Runtime.getRuntime().gc();
//                System.out.println("Current free (MB):" +
//                    Runtime.getRuntime().freeMemory() / (1024*1024));
//                System.out.println("Average size (Kb): " + (double)
//                        (utils.Sum - Runtime.getRuntime().freeMemory())
//                        / (utils.Count * 1024));
//            }
//            //////////////
        }
        // Load degredation
        DegradedEdges = (HashMap)in.readObject();
//        if (DegradedEdges.size() > 500)
//            System.out.println(DegradedEdges.size() + " degraded");
    } catch (Exception e) {
        throw new IOException(e.getMessage());
    }
  }
        
  public void degrade(DocumentNGramGraph dgOtherGraph) {
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gOtherGraph = dgOtherGraph.getGraphLevelByNGramSize(iCurLvl);
            // Check if other graph has corresponding level
            if (gOtherGraph == null)
                // If not, ignore level
                continue;
            
            // For every edge on other graph
            java.util.Iterator iIter = gOtherGraph.getEdgeSet().iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                String sHead = weCurItem.getVertexA().getLabel();
                String sTail = weCurItem.getVertexB().getLabel();
                Edge eEdge = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(gGraph, sHead, sTail);
                if (eEdge != null)
                    try
                    {
                        if (DegradedEdges.containsKey(eEdge))
                            DegradedEdges.put(eEdge,
                                    ((Double)DegradedEdges.get(eEdge)).doubleValue() + 1);
                        else
                            DegradedEdges.put(eEdge, (double)1.0);
                    }
                    catch (Exception e)
                    {
                        // Non fatal error occured. Continue.
                        e.printStackTrace();                        
                    }
            }
        }        
  }
  
  public double degredationDegree(Edge e) {
      if (DegradedEdges.containsKey(e))
          return ((Double)DegradedEdges.get(e)).doubleValue();
      else
          return 0;
  }
  
  public String toCooccurenceText(Map mCooccurenceMap) {
    StringBuffer sb = new StringBuffer();
    // For every graph level
    for (int iCnt=MinSize; iCnt <= MaxSize; iCnt++) {
        UniqueVertexGraph g = getGraphLevelByNGramSize(iCnt);
        // For all edges
        Iterator iIter = g.getEdgeSet().iterator();
        while (iIter.hasNext()) {
            // Get edge
            WeightedEdge eCur = (WeightedEdge)iIter.next();
            String sCooccurenceID;
            // If the edge is already in the map
            if (mCooccurenceMap.containsKey(eCur.toString()))
                // Get its ID
                sCooccurenceID = (String)mCooccurenceMap.get(((Edge)eCur).toString());
            else {
                // else create a new ID based on current time and put it in the map.
                sCooccurenceID = String.valueOf(mCooccurenceMap.size() + 1);
                mCooccurenceMap.put(((Edge)eCur).toString(), sCooccurenceID);
            }
            
            // Add the ID as many times as the co-occurences
            for (int iTimes=0; iTimes < (int)eCur.getWeight(); iTimes++) {
                sb.append(sCooccurenceID + " ");
            }
        }
    }
      
    return sb.toString();
  }
  
    public static void main(String args[]) {
        DocumentNGramGraph ngs = new DocumentNGramGraph(3,3,2);
        ngs.setDataString("abcdef");
        //ngs.setDataString("This is");

        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(ngs.getGraphLevel(0), true));
    }
    
    @Override
    public Object clone() {
        DocumentNGramGraph gRes = new DocumentNGramGraph(MinSize, MaxSize, CorrelationWindow);
        gRes.DataString = DataString;
        gRes.DegradedEdges.putAll((HashMap)this.DegradedEdges.clone());
        gRes.NGramGraphArray = new UniqueVertexGraph[this.NGramGraphArray.length];
        int iCnt=0;
        for (UniqueVertexGraph uCur : this.NGramGraphArray)
            gRes.NGramGraphArray[iCnt++] = (UniqueVertexGraph)uCur.clone();
        gRes.Normalizer = this.Normalizer;
        gRes.TextPreprocessor = this.TextPreprocessor;
        gRes.WordEvaluator = this.WordEvaluator;
        
        return gRes;
    }

    /** See the <i>mergeGraph</i> member for details. Implements the merge interface. */
    public void merge(DocumentNGramGraph dgOtherObject, double fWeightPercent) {
        mergeGraph(dgOtherObject, fWeightPercent);
    }
    
    /** Returns all edges not existent in another graph. 
     *@param dgOtherGraph The graph to use for intersection and difference.
     *@return A DocumentNGramGraph containing all edges from this graph not existing in the
     * other given graph (edge distros are not used).
     * The edge distributions are kept from this graphs.
     */
    public DocumentNGramGraph allNotIn(DocumentNGramGraph dgOtherGraph) {
        // TODO: Order by edge count for optimization
        EdgeCachedLocator eclLocator = new EdgeCachedLocator(Math.max(length(),
                dgOtherGraph.length()));
        // Clone this graph
        DocumentNGramGraph dgClone = (DocumentNGramGraph)clone();
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gCloneLevel = dgClone.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gOtherGraphLevel = dgOtherGraph.getGraphLevelByNGramSize(iCurLvl);
            // If this level does not exist in other graph, then keep it and continue.
            if (gOtherGraphLevel == null)
                continue;
            
            // For every edge of the cloned graph (using a new list of edges)
            java.util.Iterator iIter = Arrays.asList(gCloneLevel.getEdgeSet().toArray()).iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                // If the edge is contained in the merged graph
                Edge eEdge = eclLocator.locateDirectedEdgeInGraph(gOtherGraphLevel, weCurItem.getVertexA(), 
                        weCurItem.getVertexB());
                if (eEdge != null)
                    try {
                        gCloneLevel.removeEdge(weCurItem);
                        eclLocator.resetCache();
                        // Refresh edge iterator
                        // iIter = gCloneLevel.getEdgeSet().iterator();
                    } catch (Exception ex) {
                        // Non-lethal exception. Continue.
                        ex.printStackTrace();
                    }
            }
        }
        
        // DEBUG LINES
        //System.err.println(String.format("(%s) Cache success: %4.3f", 
        //        this.getClass().getName(), eclLocator.getSuccessRatio()));
        //////////////
        return dgClone;
    }
    
}

