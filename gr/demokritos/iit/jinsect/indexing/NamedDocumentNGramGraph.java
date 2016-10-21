/*
 * NamedDocumentNGramGraph.java
 *
 * Created on May 6, 2008, 2:03 PM
 *
 */

package gr.demokritos.iit.jinsect.indexing;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.INamed;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.WeightedEdge;

/** A named graph.
 *
 * @author ggianna
 */
public class NamedDocumentNGramGraph extends DocumentNGramSymWinGraph implements INamed {
    protected String Name = new String();
    
    @Override
    public String getName() {
        return Name;
    }
    
    public void setName(String sNewName) {
        Name = sNewName;
    }
    
    /** The hash code of the object is the hashcode of its class name, followed
     *  by the object's name. */
    @Override
    public int hashCode() {
        return (getClass().getName() + Name).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NamedDocumentNGramGraph other = (NamedDocumentNGramGraph) obj;
        if ((this.Name == null) ? (other.Name != null) : !this.Name.equals(other.Name)) {
            return false;
        }
        return true;
    }

    @Override
    public DocumentNGramGraph allNotIn(DocumentNGramGraph dgOtherGraph) {
        // TODO: Order by edge count for optimization
        EdgeCachedLocator eclNewLocator = new EdgeCachedLocator(100);
        // Clone this graph
        NamedDocumentNGramGraph dgClone = (NamedDocumentNGramGraph)clone();
        
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
                Edge eEdge = eclNewLocator.locateEdgeInGraph(gOtherGraphLevel, 
                        weCurItem.getVertexA(), 
                        weCurItem.getVertexB());
                if (eEdge != null)
                    try {
                        gCloneLevel.removeEdge(weCurItem);
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

    @Override
    public Object clone() {
        NamedDocumentNGramGraph gRes = new NamedDocumentNGramGraph();
        gRes.DataString = DataString;
        gRes.DegradedEdges.putAll(this.DegradedEdges);
        gRes.NGramGraphArray = this.NGramGraphArray.clone();
        gRes.Normalizer = this.Normalizer;
        gRes.TextPreprocessor = this.TextPreprocessor;
        gRes.WordEvaluator = this.WordEvaluator;
        gRes.Name = Name;
        return gRes;
    }
    
    @Override
    public String toString() {
        return Name;
    }
}
