/*
 * DistributionGraph.java
 *
 * Created on 24 Ιούλιος 2006, 5:41 μμ
 *
 */

package gr.demokritos.iit.conceptualIndex.structs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import java.io.IOException;
import java.io.Serializable;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.WeightedEdge;
import salvo.jesus.graph.WeightedEdgeImpl;

/** A graph with weighted edges mapped to distributions.
 *
 * @author ggianna
 */
public class DistributionGraph extends UniqueVertexGraph implements Serializable {
    /** The mapping between edges and distributions.
     */
    public HashMap EdgeDistros;
  
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
        // Write edge info
        out.writeInt(this.getEdgeSet().size()); // Output edge count
        Iterator iIter = this.getEdgeSet().iterator();
        while (iIter.hasNext()) {
            WeightedEdge eCur = (WeightedEdge)iIter.next();
            out.writeObject(eCur.getVertexA());
            out.writeObject(eCur.getVertexB());
            out.writeDouble(eCur.getWeight());
        }
        // Output edge distros
        out.writeObject(EdgeDistros);
    }

    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
        // Get edge count
        int iEdgeCnt = in.readInt();
        // Get edge info
        for (int iCnt=0; iCnt<iEdgeCnt; iCnt++) {
            Vertex vA = (Vertex)in.readObject();
            Vertex vB = (Vertex)in.readObject();
            double dWeight = in.readDouble();
            // Create edge
            WeightedEdgeImpl weCur = new WeightedEdgeImpl(vA, vB, dWeight);
            try {
                // Add it to graph
                this.addEdge(weCur);
            } catch (Exception ex) {
                throw new IOException("Could not add input edge to graph." + weCur.toString());
            }
        }
        // Get edge distros
        EdgeDistros = (HashMap)in.readObject();
    }
  
    /** Creates a new instance of DistributionGraph. */
    public DistributionGraph() {        
        super();
        EdgeDistros = new HashMap();
    }
    
    /** Maps an edge to a distribution. 
     *@param e The edge to use as the map key.
     *@param d The distribution to use as the map value.
     */
    public void setEdgeDistro(Edge e, Distribution d) {
        EdgeDistros.put(e,d);
    }
    
    /** Returns the distribution mapped to a selected edge.
     *@param e The key edge.
     *@return The corresponding distribution.
     *@see Distribution
     */
    public Distribution getEdgeDistro(Edge e) {
        return (Distribution)EdgeDistros.get(e);
    }
    
    /** Removes the selected edge from the graph.
     *@param edge The edge to remove.
     */
    public void removeEdge(Edge edge) throws Exception {
        if (EdgeDistros.containsKey(edge))
            EdgeDistros.remove(edge);
        super.removeEdge(edge);
    }
    
    /**Returns a string representation of the distribution graph.
     *@return The string representation, including the distribution information.
     */
    public String toString() {
        String sRes = "";
        Iterator iEdgeIter = Arrays.asList(getEdgeSet().toArray()).listIterator();
        while (iEdgeIter.hasNext()) {
            // Get next edge
            Edge e = (Edge)iEdgeIter.next();
            
            // Get 1st vertex
            String sA = "";
            char[] cTmp = e.getVertexA().getLabel().toCharArray();
            
            int iCnt=0;
            while (iCnt<cTmp.length)
            {
                char c = cTmp[iCnt];
                sA += (Character.isISOControl(c)) ? "_" : (char)c;
                iCnt++;
            }
            
            // Get 2nd vertex
            String sB = "";
            cTmp = e.getVertexB().getLabel().toCharArray();
            
            iCnt=0;
            while (iCnt<cTmp.length)
            {
                char c = cTmp[iCnt];
                sB += (Character.isISOControl(c)) ? "_" : (char)c;
                iCnt++;
            }
            
            sRes = sRes.concat(sA + "->" + sB + "(Distro: " + getEdgeDistro(e).toString() + ")\n");
        }
        
        return sRes;
    }
    
}
