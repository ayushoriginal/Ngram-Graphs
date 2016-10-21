/*
 * CASCGraph.java
 *
 * Created on April 3, 2007, 1:32 PM
 *
 */

package gr.demokritos.iit.jinsect.casc.structs;

import gr.demokritos.iit.conceptualIndex.documentModel.SymbolicGraph;
import java.util.HashMap;
import salvo.jesus.graph.Vertex;

/** Represents a graph of named documents, allowing intermediate unnamed (latent) vertices.
 *
 * @author ggianna
 */
public class CASCGraph extends SymbolicGraph {
    /** Constant indicative of the Normal vertex type. */
    public static final String NORMAL = "Normal";
    /** Constant indicative of the Latent vertex type. */
    public static final String LATENT = "Latent";
    
    /** A HashMap of the Type of each Vertex in the graph.
     */
    protected HashMap hVertexTypes;
    
    /** Creates a new instance of CASCGraph. */
    public CASCGraph() {
        super(1,1);
        hVertexTypes = new HashMap();
    }
    
    /** Sets the vertex type of a vertex.
     *@param v The vertex to define.
     *@param sType A string indicating the type of the vertex. The constants NORMAL, LATENT have been
     *predefined in the class to help the use of this method.
     *@return The vertex changed.
     */
    public Vertex setVertexType(Vertex v, String sType) {
        hVertexTypes.put(v, sType);
        
        return v;
    }
    
    /** Gets the vertex type of a given vertex.
     *@param v The vertex to define.
     *@return A string indicating the type of the vertex. The constants NORMAL, LATENT have been
     *predefined in the class to help the use of this method. Any vertex the type of which has not been
     *defined, is considered to be of type NORMAL.
     */
    public String getVertexType(Vertex v) {
        if (hVertexTypes.containsKey(v))
            return (String)hVertexTypes.get(v);
        else
            // Default to normal
            return NORMAL;
    }
    
    /** Removes a given vertex from the graph, also erasing vertex type information.
     *@param v The vertex to remove.
     *@throws Exception An exception is thrown, if the vertex cannot be removed.
     */
    public void remove(Vertex v) throws Exception {
        if (hVertexTypes.containsKey(v))
            hVertexTypes.remove(v);
        
        super.remove(v);
    }
}
