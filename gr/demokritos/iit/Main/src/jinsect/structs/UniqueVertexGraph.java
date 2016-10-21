/*
 * UniqueVertexGraph.java
 *
 * Created on 24 Ιανουάριος 2006, 10:35 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.jinsect.utils;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;
import salvo.jesus.graph.WeightedEdgeImpl;
import salvo.jesus.graph.WeightedGraphImpl;

/** A weighted graph, where the vertices have unique labels.
 *
 * @author PCKid
 */
public class UniqueVertexGraph extends WeightedGraphImpl implements Serializable {
    //TODO: REMOVE
    private static final long serialVersionUID = 880380742772630619L;
    /////////////
    /** The map of vertices and labels.
     */
    public HashMap<String,Vertex> UniqueVertices;
    protected EdgeCachedLocator eclLocator = null;

    /**
     * Set a locator to optimize the edge lookup.
     * @param eNewLocator The locator to use.
     */
    public void setLocator(EdgeCachedLocator eNewLocator) {
        eclLocator = eNewLocator;
    }
    
   
    /** Initializes the graph.
     */
    public UniqueVertexGraph() {
        UniqueVertices = new HashMap<String, Vertex>();
        // DuplicateChecker d = new DuplicateChecker(this);
        // this.addListener(d);
    }
    
    /** Checks whether a given vertex exists in this graph.
     *@param v The vertex, the label of which will be used for the lookup.
     *@return True if the vertex is contained in this graph. Otherwise false.
     */
    public boolean contains(Vertex v) {
        return UniqueVertices.containsKey(v.getLabel());
    }
    
    /** Looks up a given vertex in this graph.
     *@param v The vertex, the label of which will be used for the lookup.
     *@return The vertex if it is contained in this graph. Otherwise null.
     */
    public synchronized Vertex locateVertex(Vertex v) {
        return (Vertex)UniqueVertices.get(v.getLabel());
    }

    /** Looks up a given vertex label in this graph.
     *@param sVertexLabel  The label which will be used for the lookup.
     *@return The vertex if it is contained in this graph. Otherwise null.
     */
    public synchronized Vertex locateVertex(String sVertexLabel) {
        return (Vertex)UniqueVertices.get(sVertexLabel);
    }
    
    
    /** Adds a new vertex to the graph, checking for duplicate labels.
     *@param v The vertex to add.
     *@throws Exception If a vertex with the same label already exists.
     */
    @Override
    public synchronized void add(Vertex v) throws Exception {
        if (UniqueVertices.containsKey(v.getLabel()))
            // If already exists
            return;
        else
            super.add(v);
        // Append
        UniqueVertices.put(v.getLabel(), v);
    }
    
    /** Adds a new edge to the graph, checking for duplicate labels of its vertices.
     *@param vHead The edge head to add.
     *@param vTail The edge tail to add.
     *@return The newly added edge
     *@throws Exception If the edge cannot be added.
     */
    @Override
    public synchronized Edge addEdge(Vertex vHead, Vertex vTail) throws Exception {
        Vertex vH = null, vT = null;
        boolean bVertexMissed = false;
        
        if ((vH = locateVertex(vHead)) == null) {
            add(vH = vHead);
            bVertexMissed = true;
        }
        
        if ((vT = locateVertex(vTail)) == null) {
            add(vT = vTail);
            bVertexMissed = true;
        }
        
        Edge e = null;
        if (eclLocator == null) {
            // Locate only if both vertices have been found
            if (!bVertexMissed)
                e = utils.locateDirectedEdgeInGraph(this, vH, vT);
            // ONLY DIRECTED
//            if (e == null) {
//                e = utils.locateEdgeInGraph(this, vH, vT);
//                if (e instanceof DirectedEdge) {
//                    if (((DirectedEdge)e).getSink() != vT)
//                        e=null;
//                }
//
//            }
        }
        else
        {
            // Locate only if both vertices have been found
            if (!bVertexMissed)
                e = eclLocator.locateDirectedEdgeInGraph(this, vH, vT);
            // ONLY DIRECTED
//            if (e == null) {
//                e = eclLocator.locateEdgeInGraph(this, vH, vT);
//                if (e instanceof DirectedEdge) {
//                    if (((DirectedEdge)e).getSink() != vT)
//                        e=null;
//                }
//
//            }

        }
        
        if (e == null) {
            e = super.addEdge(vH, vT);
            if (eclLocator != null)
                eclLocator.addedEdge(e);
        }
        
        // Return added edge, or null.
        return e;
    }

    @Override
    /** Adds a new edge to the graph, checking for duplicate labels of its vertices.
     *@param vHead The edge head to add.
     *@param vTail The edge tail to add.
     *@param dWeight The weight of the edge.
     *@return The newly added weighted edge.
     *@throws Exception If the edge cannot be added.
     */
    public synchronized WeightedEdge addEdge(Vertex vHead, Vertex vTail, double dWeight)
        throws Exception {
        WeightedEdge e = super.addEdge(vHead, vTail, dWeight);
        // Return added edge, or null.
        return ((WeightedEdge)e);
    }

    /** Adds a new edge to the graph, checking for duplicate labels of its vertices.
     *@param eEdge The edge to add.
     *@throws Exception If the edge cannot be added.
     */
    @Override
    public synchronized void addEdge(Edge edge) throws Exception {
        Vertex vH, vT;
        Vertex vHead = edge.getVertexA();
        Vertex vTail = edge.getVertexB();
        boolean bVertexMissed = false;
        
        if ((vH = locateVertex(vHead)) == null) {
            add(vH = vHead);
            bVertexMissed = true;
        }
        // DEBUG LINES
        //else
        //    System.out.println("Vertex " + vHead.getLabel() + " already added.");
        ///////////////
        
        if ((vT = locateVertex(vTail)) == null) {
            add(vT = vTail);
            bVertexMissed = true;
        }
            
        // DEBUG LINES
        //else
        //{            
        //    System.out.println("Vertex " + vTail.getLabel() + " already added.");
        ///////////////
        //}
        
        
        //Edge e = jinsect.utils.locateDirectedEdgeInGraph(this, vH, vT);
        Edge e = null;
        // If any vertex of the two was missing, the edge SURELY does not exist
        // so omit lookup
        if (eclLocator == null) {
            if (!bVertexMissed)
                e = utils.locateDirectedEdgeInGraph(this, vH, vT);
            if (e == null) {
                super.addEdge(edge);
                return;
            }
            else
                return;
        }
        else {
            if (!bVertexMissed)
                e = eclLocator.locateDirectedEdgeInGraph(this, vH, vT);
            if (e == null) {
                super.addEdge(edge);
                eclLocator.addedEdge(edge);
                return;
            }
            else
                return;
        }
    }
    
    // Serialization
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
      out.writeObject(UniqueVertices);
    }
  
    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
     UniqueVertices = (HashMap)in.readObject();
    }

    @Override
    public Object clone() {
//        INSECTCompressedMemoryDB<UniqueVertexGraph> m = new
//                INSECTCompressedMemoryDB<UniqueVertexGraph>();
//        m.saveObject(this, "tmp", "tmp");
//        Object res = m.loadObject("tmp", "tmp");
//        // Clear reference
//        m.deleteObject("tmp", "tmp");
        UniqueVertexGraph res = new UniqueVertexGraph();
        res.UniqueVertices = (HashMap<String, Vertex>) this.UniqueVertices.clone();
        
        for (WeightedEdgeImpl eCur: (Set<WeightedEdgeImpl>)this.getEdgeSet())
            try {
                res.addEdge(eCur.getVertexA(), eCur.getVertexB(), 
                        eCur.getWeight());
            } catch (Exception ex) {
                return null;
            }
        return res;
    }

    public static void main(String[] args) {
        UniqueVertexGraph uvg = new UniqueVertexGraph();
        Vertex vA = new VertexImpl();
        vA.setLabel("A");
        Vertex vB = new VertexImpl();
        vB.setLabel("B");
        try {
            uvg.addEdge(new WeightedEdgeImpl(vB, vA, 2.0));
            uvg.addEdge(new WeightedEdgeImpl(vA, vB, 2.0));
            uvg.addEdge(new WeightedEdgeImpl(vA, vB, 1.0));
        } catch (Exception ex) {
            Logger.getLogger(UniqueVertexGraph.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
}

/** DEPRECATED
class DuplicateChecker implements GraphListener {
    // The graph to check
    UniqueVertexGraph GraphToListen;
    public DuplicateChecker(UniqueVertexGraph g) {
        GraphToListen = g;
    }
    
    public void beforeVertexAdded(GraphAddVertexEvent event) throws Exception {
        if (GraphToListen.contains(event.getVertex())) {
            // DEBUG LINES
            //System.err.println("Will not add vertex " + 
                    //event.getVertex().getLabel());
            //////////////
            throw new Exception("Vertex already exists.");
        }
    }

    public void afterVertexAdded(GraphAddVertexEvent event) {
        GraphToListen.UniqueVertices.put(event.getVertex().getLabel(),
                event.getVertex());
    }

    public void beforeVertexRemoved(GraphRemoveVertexEvent event) throws Exception {
    }

    public void afterVertexRemoved(GraphRemoveVertexEvent event) {
        GraphToListen.UniqueVertices.remove(event.getVertex().getLabel());
    }

    public void beforeEdgeAdded(GraphAddEdgeEvent event) throws Exception {
        Vertex vH, vT;
        boolean bVertexMissed = false;
        Vertex vHead = event.getEdge().getVertexA();
        Vertex vTail = event.getEdge().getVertexB();
        
        if ((vH = GraphToListen.locateVertex(vHead)) == null) {
            GraphToListen.add(vH = vHead);
            bVertexMissed = true;
        }
        
        if ((vT = GraphToListen.locateVertex(vTail)) == null) {
            GraphToListen.add(vT = vTail);
            bVertexMissed = true;
        }
        
        Edge e = null;
        // Locate only if both vertices have been found
        if (!bVertexMissed)
            e = utils.locateDirectedEdgeInGraph(
                    GraphToListen, vH, vT);
        if (e != null) {
            // DEBUG LINES
            System.err.println("Will not add edge " + 
                    event.getEdge().toString());
            //////////////
            throw new Exception("Edge already exists.");
        }
        
    }

    public void afterEdgeAdded(GraphAddEdgeEvent event) {
    }

    public void beforeEdgeRemoved(GraphRemoveEdgeEvent event) throws Exception {
    }

    public void afterEdgeRemoved(GraphRemoveEdgeEvent event) {
    }
    
}
 */