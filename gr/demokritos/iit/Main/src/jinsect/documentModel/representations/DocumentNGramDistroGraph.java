/*
 * DocumentNGramDistroGraph.java
 *
 * Created on 12 Φεβρουάριος 2007, 2:29 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.representations;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedDistroGraphComparator;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import java.util.Iterator;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;
import static gr.demokritos.iit.jinsect.utils.graphToDot;

/**
 *
 * @author ggianna
 */
public class DocumentNGramDistroGraph extends DocumentNGramGraph {
    
    protected HashMap EdgeDistros = null;
    
    /**
     * Creates a new instance of DocumentNGramDistroGraph
     */
    public DocumentNGramDistroGraph() {
        InitGraphs();
    }
    
    public DocumentNGramDistroGraph(int iMinSize, int iMaxSize, int iCorrelationWindow) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        CorrelationWindow = iCorrelationWindow;
        
        InitGraphs();
    }
    
    protected void InitGraphs() {
        super.InitGraphs();
        // Init edge distributions
        EdgeDistros = new HashMap();        
    }

    @Override
    public void setDataString(String sDataString) {
        super.setDataString(sDataString);
        // Also update weights
        updateWeights();        
    }

    
    /***
     *Creates an edge in a graph connecting a selected node to each node in
     *a list of other nodes. The method also uses a histogram of appearences of 
     *the terms in the graph vertices as background knowledge.
     *@param gGraph The graph to use
     *@param sStartNode The node from which all edges begin
     *@param lOtherNodes The list of nodes to which sBaseNode is connected
     *@param hAppearenceHistogram The histogram of appearences of the terms
    ***/
    @Override
    public void createEdgesConnecting(UniqueVertexGraph gGraph, String sStartNode, List lOtherNodes,
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
                    e.printStackTrace();
                }
                return;
            }
        
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
                    
        java.util.Iterator iIter = lOtherNodes.iterator();
        
        Double dDist = 0.0;
        // For every edge
        while (iIter.hasNext())
        {
            dDist++; // Get distance
            
            VertexImpl vB = new VertexImpl();
            vB.setLabel((String)iIter.next());
            
            // Locate edge
            WeightedEdge weCorrectEdge = (WeightedEdge)gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(gGraph, vA, vB);
            
            try
            {
                Distribution dTemp = null;
                if (weCorrectEdge == null)
                {
                    // Not found. Init distribution.
                    dTemp = new Distribution();
                    dTemp.increaseValue(dDist, 1.0); // Initialize distance occurences to 1.
                    
                    // Add edge with initialized (zero) weight
                    weCorrectEdge = gGraph.addEdge(vA, vB, 0.0);
                    
                }
                else {
                    dTemp = (Distribution)EdgeDistros.get(weCorrectEdge);
                    // If no distro assigned yet
                    if (dTemp == null)
                        dTemp = new Distribution();
                    dTemp.increaseValue(dDist, 1.0); // Initialize distance occurences to 1.                    
                }
                // Update distros' hashmap
                EdgeDistros.put(weCorrectEdge, dTemp);
                
                if (EdgeDistros.get(weCorrectEdge) == null)
                    throw new NullPointerException("Added null edge distro...");
                
                Distribution dProb = dTemp.getProbabilityDistribution();
                
                // Update using average - VERY CPU CONSUMING - SHOULD BE PUT 
                // ELSEWHERE
                // weCorrectEdge.setWeight(dProb.average(false));
            }
            catch (Exception e)
            {
                // Unknown error
                e.printStackTrace();
            }
        }
    }

    /** DONE: Checked
     * Updates all weights, based on a function of the corresponding distro.
     */
    protected void updateWeights() {
        Iterator<WeightedEdge> iCurEdge = EdgeDistros.keySet().iterator();
        while (iCurEdge.hasNext()) {
            WeightedEdge eCur = iCurEdge.next();
            eCur.setWeight(((Distribution)EdgeDistros.get(eCur)).average(false));
        }
        
    }
    
    /** TODO: Correct
     *Merges the data of [dgOtherGraph] document graph to the data of this graph, 
     *by adding all existing edges and summing the histogram values of the edge distros of both graphs.
     *The current graph is modified, becoming the merged graph.
     *
     *@param dgOtherGraph The second graph used for the merging
    ***/
    public DocumentNGramDistroGraph mergeDistroGraph(DocumentNGramDistroGraph dgOtherGraph) {
        EdgeCachedLocator ecLocator = new EdgeCachedLocator(10);
        // Clone this object
        DocumentNGramDistroGraph res = (DocumentNGramDistroGraph)clone();
        
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gGraph = res.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gOtherGraph = dgOtherGraph.getGraphLevelByNGramSize(iCurLvl);
            
            // TODO: Check if that's what should be done
            // Check if other graph has corresponding level
            if (gOtherGraph == null)
                // If not, ignore level 
                continue;
            ////////////////////////////////////////////
            
            // For every edge on other graph
            java.util.Iterator iIter = gOtherGraph.getEdgeSet().iterator();
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                // Get vertices
                String sHead = weCurItem.getVertexA().getLabel();
                String sTail = weCurItem.getVertexB().getLabel();
                WeightedEdge weMyCurItem = (WeightedEdge)ecLocator.locateDirectedEdgeInGraph(gGraph, 
                        weCurItem.getVertexA(), weCurItem.getVertexB());
                
                // If edge was not found, add it.
                if (weMyCurItem == null) {
                    try {
                        weMyCurItem = gGraph.addEdge(weCurItem.getVertexA(), weCurItem.getVertexB(), 
                                weCurItem.getWeight());
                        ecLocator.addedEdge(weMyCurItem);
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                        continue;
                    }
                }
                // Get distros and add their corresponding histograms
                Distribution d1 = (Distribution)res.EdgeDistros.get(weMyCurItem);
                if (d1 == null)
                    d1 = new Distribution();
                Distribution d2 = (Distribution)dgOtherGraph.EdgeDistros.get(weCurItem);
                if (d2 == null)
                    d2 = new Distribution();
                // Sum them up and update edge distros
                res.EdgeDistros.put(weMyCurItem, d2.addTo(d1));
                
                // Also update edge weight to the sum
                if (weMyCurItem != null) {
                    double dWeight = weCurItem.getWeight() + weMyCurItem.getWeight();                
                    weMyCurItem.setWeight(dWeight);
                }
            }
        }
        return res;
    }

    
    /** 
     * Returns the intersection of two graphs as a new graph, containing only common nodes and their 
     * corresponding edges. The values of the edges are updated by summing the histogram distro values of the edges.
     *@param dgOtherGraph The second graph used for the intersection.
     *@return A {@link DocumentNGramDistroGraph} indicating the maximum common subgraph of the given graph the
     * second graph.
     */
    public DocumentNGramDistroGraph intersectDistroGraph(DocumentNGramDistroGraph dgOtherGraph) {
        // Init res graph
        DocumentNGramDistroGraph gRes = new DocumentNGramDistroGraph(MinSize, MaxSize, CorrelationWindow);
        EdgeCachedLocator ecLocator = new EdgeCachedLocator(10);
        EdgeCachedLocator ecNewLocator = new EdgeCachedLocator(10);
        
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
                WeightedEdge eEdge = (WeightedEdge)ecLocator.locateDirectedEdgeInGraph(gGraph, 
                        weCurItem.getVertexA(), weCurItem.getVertexB());
                WeightedEdge weNewEdge = null;
                
                String sHead = weCurItem.getVertexA().getLabel();
                String sTail = weCurItem.getVertexB().getLabel();
                
                if (eEdge != null) {
                    try
                    {
                        List l = new ArrayList();
                        l.add(sTail);
                        createWeightedEdgesConnecting(gNewGraph, sHead, l, eEdge.getWeight() + weCurItem.getWeight(), 
                                eEdge.getWeight() + weCurItem.getWeight(), 
                                1.0);
                        // Locate newly added edge
                        weNewEdge = (WeightedEdge)gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(gNewGraph, 
                                weCurItem.getVertexA(), weCurItem.getVertexB());
                    }
                    catch (Exception e)
                    {
                        // Non fatal error occured. Continue.
                        e.printStackTrace();                        
                    }
                    // Get distros and add their corresponding histograms
                    Distribution d1 = (Distribution)this.EdgeDistros.get(eEdge);
                    if (d1 == null)
                        d1 = new Distribution();
                    Distribution d2 = (Distribution)dgOtherGraph.EdgeDistros.get(weCurItem);
                    if (d2 == null)
                        d2 = new Distribution();
                    // Sum them up and update edge distros
                    if (weNewEdge != null)
                        gRes.EdgeDistros.put(weNewEdge, d2.addTo(d1));
                }
            }
        }                
        return gRes;
    }
    
    /** Returns the difference (inverse of the intersection) graph between the current graph 
     * and a given graph.
     *@param dgOtherGraph The graph to compare to.
     *@return A DocumentNGramDistroGraph that is the difference between the current graph 
     * and the given graph. The edge distributions are kept from the original graphs.
     */
    public DocumentNGramDistroGraph inverseIntersectDistroGraph(DocumentNGramDistroGraph dgOtherGraph) {
        
        // Get the union (merged) graph
        DocumentNGramDistroGraph dgUnion = (DocumentNGramDistroGraph)clone();
        dgUnion.mergeDistroGraph(dgOtherGraph);
        
        // Get the intersection graph
        DocumentNGramDistroGraph dgIntersection = intersectDistroGraph(dgOtherGraph);
        
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
                        dgUnion.getEdgesToDistros().remove(eEdge);
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
    public DocumentNGramDistroGraph[] intersectAndDeltaDistroGraph(DocumentNGramDistroGraph dgOtherGraph) {
        
        // Get the union (merged) graph
        DocumentNGramDistroGraph dgUnion = (DocumentNGramDistroGraph)clone();
        dgUnion.mergeDistroGraph(dgOtherGraph);
        
        DocumentNGramDistroGraph[] res = new DocumentNGramDistroGraph[2];
        
        // Get the intersection graph
        DocumentNGramDistroGraph dgIntersection = intersectDistroGraph(dgOtherGraph);
        res[0] = dgIntersection;
        
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
                        dgUnion.getEdgesToDistros().remove(eEdge);
                    } catch (Exception ex) {
                        // Non-lethal exception. Continue.
                        ex.printStackTrace();
                    }
            }
        }
        
        res[1] = dgUnion;
        return res;
    }
    
    /** Returns all edges not existent in another graph. 
     *@param dgOtherGraph The graph to use for intersection and difference.
     *@return A DocumentNGramDistroGraph containing all edges from this graph not existing in the
     * other given graph (edge distros are not used).
     * The edge distributions are kept from this graphs.
     */
    public DocumentNGramDistroGraph allNotIn(DocumentNGramDistroGraph dgOtherGraph) {
        // TODO: Order by edge count for optimization
        EdgeCachedLocator eclLocator = new EdgeCachedLocator(100);
        // Clone this graph
        DocumentNGramDistroGraph dgClone = (DocumentNGramDistroGraph)clone();
        for (int iCurLvl = MinSize; iCurLvl <= MaxSize; iCurLvl++) {
            UniqueVertexGraph gCloneLevel = dgClone.getGraphLevelByNGramSize(iCurLvl);
            UniqueVertexGraph gOtherGraphLevel = dgOtherGraph.getGraphLevelByNGramSize(iCurLvl);
            // If this level does not exist in other graph, then keep it and continue.
            if (gOtherGraphLevel == null)
                continue;
            
            // For every edge of the cloned graph
            java.util.Iterator iIter = gCloneLevel.getEdgeSet().iterator();
            // For every level
            while (iIter.hasNext())
            {
                WeightedEdge weCurItem = (WeightedEdge)iIter.next();
                // If the edge is contained in the merged graph
                Edge eEdge = eclLocator.locateDirectedEdgeInGraph(gOtherGraphLevel, weCurItem.getVertexA(), 
                        weCurItem.getVertexB());
                if (eEdge != null)
                    try {
                        gCloneLevel.removeEdge(weCurItem);
                        dgClone.getEdgesToDistros().remove(weCurItem);
                        // Refresh edge iterator
                        iIter = gCloneLevel.getEdgeSet().iterator();
                    } catch (Exception ex) {
                        // Non-lethal exception. Continue.
                        ex.printStackTrace();
                    }
            }
        }
        
        return dgClone;
    }
    
    /** Returns a mapping of edges to corresponding distributions.
     *@return The Map of edges to distributions.
     */
    public Map getEdgesToDistros() {
        return EdgeDistros;
    }
    
    /** TODO: CHECK
     */
    public Object clone() {
        System.err.println(this.getClass().getName() + ": The clone() method should be checked...");
        DocumentNGramDistroGraph gRes = new DocumentNGramDistroGraph(MinSize, MaxSize, CorrelationWindow);
        gRes.DataString = DataString;
        gRes.DegradedEdges.putAll(this.DegradedEdges);
        gRes.NGramGraphArray = this.NGramGraphArray.clone();
        gRes.Normalizer = this.Normalizer;
        gRes.TextPreprocessor = this.TextPreprocessor;
        gRes.WordEvaluator = this.WordEvaluator;
        gRes.EdgeDistros = (HashMap)this.EdgeDistros.clone();
        
        return gRes;
    }
    
    
    // Serialization
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    // Write Fields
    out.writeObject(EdgeDistros);
    }

    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
      EdgeDistros = (HashMap)in.readObject();
    }
  
    public static void main(String[] sArgs) {
        String s = "112233";
        DocumentNGramDistroGraph g = new DocumentNGramDistroGraph(1,1,2);
        g.setDataString(s);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(g.getGraphLevel(0), true, g.getEdgesToDistros()));
        String s2 = "1122";
        DocumentNGramDistroGraph g2 = new DocumentNGramDistroGraph(1,1,2);
        g2.setDataString(s2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(g2.getGraphLevel(0), true, g2.getEdgesToDistros()));
        
        DocumentNGramDistroGraph gAllNotIn = g.allNotIn(g2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(gAllNotIn.getGraphLevel(0), true, gAllNotIn.getEdgesToDistros()));
                
        DocumentNGramDistroGraph[] gAll = g.intersectAndDeltaDistroGraph(g2);
        System.out.println("\n" + graphToDot(gAll[0].getGraphLevel(0), true, gAll[0].getEdgesToDistros()));
        System.out.println("\n" + graphToDot(gAll[1].getGraphLevel(0), true, gAll[1].getEdgesToDistros()));
        
        NGramCachedDistroGraphComparator comp = new NGramCachedDistroGraphComparator();
        System.out.println("\ng, Intersect Similarity:\n" + comp.getSimilarityBetween(g, gAll[0]));
        System.out.println("\ng, Delta Similarity:\n" + comp.getSimilarityBetween(g, gAll[1]));
        System.out.println("\nIntersect and Delta Similarity:\n" + comp.getSimilarityBetween(gAll[0], gAll[1]));
        System.exit(0); ////////////////////////////
        
        System.out.println("\nG1, G2 Similarity:\n" + comp.getSimilarityBetween(g, g2));
        
        // Intersect
        DocumentNGramDistroGraph gInter = g.intersectDistroGraph(g2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(gInter.getGraphLevel(0), true,
                gInter.getEdgesToDistros()));
        System.out.println("\nG1, Intersect Similarity:\n" + comp.getSimilarityBetween(g, gInter));
        System.out.println("\nIntersect, G1 Similarity:\n" + comp.getSimilarityBetween(gInter, g));
        System.out.println("\nG2, Intersect Similarity:\n" + comp.getSimilarityBetween(g2, gInter));
        DocumentNGramDistroGraph gMerged = null;
        
        // Attempt cloning
        gMerged = gMerged.mergeDistroGraph(g2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(gMerged.getGraphLevel(0), true,
                gMerged.getEdgesToDistros()));
        System.out.println("G1, Merge Similarity:\n" + comp.getSimilarityBetween(gMerged, g));
        System.out.println("G2, Merge Similarity:\n" + comp.getSimilarityBetween(gMerged, g2));
        
        
        // Calculate Delta
        DocumentNGramDistroGraph gDelta = g.inverseIntersectDistroGraph(g2);
        System.out.println(gr.demokritos.iit.jinsect.utils.graphToDot(gDelta.getGraphLevel(0), true, 
                gDelta.getEdgesToDistros()));
    }
    
}
