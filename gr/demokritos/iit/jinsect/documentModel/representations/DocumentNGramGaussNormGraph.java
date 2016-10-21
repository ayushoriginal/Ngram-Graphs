/*
 * DocumentNGramGaussNormGraph.java
 *
 * Created on May 29, 2007, 6:15 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.representations;

import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import gr.demokritos.iit.jinsect.utils;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/** A Document N-gram UniqueVertexGraph that uses a Gaussian bell scaling faction to determine weights applied to various distances
 * of neighbouring n-grams.
 * @author ggianna
 */
public class DocumentNGramGaussNormGraph extends DocumentNGramGraph {
    public EdgeCachedLocator eclLocator = null;
    
    /** Creates a new instance of INSECTDocumentGraph */
    public DocumentNGramGaussNormGraph() {
        InitGraphs();
    }
    
    /***
     * Creates a new instance of INSECTDocumentGraph 
     * @param iMinSize The minimum n-gram size
     * @param iMaxSize The maximum n-gram size
     * @param iCorrelationWindow The standard deviation of the Gaussian scaling function to use when
     * determining neighbouring weights.
     */
    public DocumentNGramGaussNormGraph(int iMinSize, int iMaxSize, int iCorrelationWindow) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        CorrelationWindow = iCorrelationWindow;
        
        InitGraphs();
    }
    
    /***
     * Creates the graph of n-grams, for all the levels specified in the MinSize, MaxSize range.
     * The whole document is taken into account for neighbouring, even though the distance affects neighbouring
     * importance, by scaling the neighbouring weight by a Gaussian function of distance.
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
                // Take neighbours into account up to 3 times the stdev
                if (PrecedingNeighbours.size() > CorrelationWindow * 3)
                    PrecedingNeighbours.removeElementAt(0);// Remove first element
            }
            int iNeighboursLen = PrecedingNeighbours.size();
            if (iNeighboursLen > 0) {
                createEdgesConnecting(gGraph, sCurNGram, (List)PrecedingNeighbours, hTokenAppearence);
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
            dStartWeight = ScalingFunction(++iCnt);
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
    
    
    /** A function providing a scaling factor according to the distance between any two n-grams.
     *@param iDistance The distance between the two n-grams.
     *@return A double scaling factor.
     */
    protected double ScalingFunction(int iDistance) {
        return Math.exp(-Math.pow((iDistance),2.0) / (2.0*Math.pow(CorrelationWindow,2.0)));
    }

    protected void InitGraphs() {
        super.InitGraphs();
        if (eclLocator != null)
            eclLocator.resetCache();
    }
}
