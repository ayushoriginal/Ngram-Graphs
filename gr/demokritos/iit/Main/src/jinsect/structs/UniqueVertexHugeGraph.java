/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.GraphFactory;
import salvo.jesus.graph.GraphListener;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.WeightedEdge;
import salvo.jesus.graph.WeightedGraph;
import salvo.jesus.graph.algorithm.GraphTraversal;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 *
 * @author ggianna
 */
public class UniqueVertexHugeGraph extends UniqueVertexGraph {

    public static final char VERTEX_LABEL_SEP = (char)26;
    protected UniqueVertexGraph[] UnderlyingGraphs;
//    ThreadQueue tRunner = new ThreadQueue();
    transient ExecutorService tRunner = Executors.newCachedThreadPool();

    public UniqueVertexHugeGraph(int iSegments) {
        UnderlyingGraphs = new UniqueVertexGraph[iSegments];
        for (int iCnt = 0; iCnt < iSegments; iCnt++) {
           UnderlyingGraphs[iCnt] = new UniqueVertexGraph();
        }
        UniqueVertices= new HashMap<String, Vertex>(1000000);
    }

    public final int getHash(String s) {
        int iRes = Math.abs(s.hashCode()) % UnderlyingGraphs.length;
        return iRes;
    }

    @Override
    public synchronized void add(Vertex v) throws Exception {
        for (int iCnt = 0; iCnt < UnderlyingGraphs.length; iCnt++) {
            UnderlyingGraphs[iCnt].add(v);
        }
        //super.add(v);
        
//        // Add to graphs asynchronously
//        for (int iCnt = 0; iCnt < UnderlyingGraphs.length; iCnt++) {
//           final UniqueVertexGraph uArg = UnderlyingGraphs[iCnt];
//           final int iCntArg = iCnt;
//           final Vertex vArg = v;
//           tRunner.execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        uArg.add(vArg);
//                    } catch (Exception ex) {
//                        // Could not add. Not handled.
//                        Logger.getAnonymousLogger().log(Level.INFO,
//                                ex.getLocalizedMessage());
//                    }
//                }
//            });
//        }
    }

    @Override
    public synchronized Edge addEdge(Vertex vHead, Vertex vTail) throws Exception {
        String sHashKey = vHead.getLabel() + VERTEX_LABEL_SEP + vTail.getLabel();
        if (!contains(vHead))
            add(vHead);
        if (!contains(vTail))
            add(vTail);
        return UnderlyingGraphs[getHash(sHashKey)].addEdge(vHead, vTail);
    }

    @Override
    public synchronized WeightedEdge addEdge(Vertex vHead, Vertex vTail, double dWeight) throws Exception {
        String sHashKey = vHead.getLabel() + VERTEX_LABEL_SEP + vTail.getLabel();
        if (!contains(vHead))
            add(vHead);
        else
            vHead = locateVertex(vHead);
        if (!contains(vTail))
            add(vTail);
        else
            vTail = locateVertex(vTail);
        return UnderlyingGraphs[getHash(sHashKey)].addEdge(vHead, vTail, dWeight);
    }

    @Override
    public synchronized void addEdge(Edge edge) throws Exception {
        String sHashKey = edge.getVertexA().getLabel() + VERTEX_LABEL_SEP +
                edge.getVertexB().getLabel();
        if (!contains(edge.getVertexA()))
            add(edge.getVertexA());

        if (!contains(edge.getVertexB()))
            add(edge.getVertexB());
        UnderlyingGraphs[getHash(sHashKey)].addEdge(edge);
    }

    @Override
    public void addListener(GraphListener listener) {
        for (int iCnt = 0; iCnt < UnderlyingGraphs.length; iCnt++) {
            super.addListener(listener);
        }
    }

    @Override
    public boolean contains(Vertex v) {
        for (int iCnt = 0; iCnt < UnderlyingGraphs.length; iCnt++)
            if (UnderlyingGraphs[iCnt].contains(v))
                    return true;
        return false;
    }

    @Override
    public boolean containsEdge(Edge edge) {
        String sHashKey = edge.getVertexA().getLabel() + VERTEX_LABEL_SEP +
            edge.getVertexB().getLabel();
        return UnderlyingGraphs[getHash(sHashKey)].containsEdge(edge);

    }

    @Override
    public boolean containsVertex(Vertex v) {
        for (int iCnt = 0; iCnt < UnderlyingGraphs.length; iCnt++)
            if (UnderlyingGraphs[iCnt].contains(v))
                    return true;
        return false;
    }

    @Override
    public List getAdjacentVertices(Vertex v) {
        Iterator  iterator;
        Edge      edge;
        Vertex    oppositeVertex;

        ArrayList<Vertex> lRes = new ArrayList<Vertex>();
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            if (UnderlyingGraphs[iCnt].contains(v)) {
                try {
                    List<Edge> incidentEdges = UnderlyingGraphs[iCnt].getEdges(v);
                    if( incidentEdges != null ) {
                        iterator = incidentEdges.iterator();
                        while( iterator.hasNext() ) {
                            edge = (Edge) iterator.next();
                            oppositeVertex = edge.getOppositeVertex( v );
                            if( oppositeVertex != null )
                                lRes.add( oppositeVertex );
                        }
                    }
                }
                catch (NullPointerException ne) {
                    // No info for edge in this segment-graph
                    // Continue
                }
            }
        }


        return Collections.unmodifiableList(lRes);
    }

    @Override
    public int getDegree(Vertex v) {
        int iDegree = 0;
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            if (UnderlyingGraphs[iCnt].contains(v))
                iDegree += UnderlyingGraphs[iCnt].getDegree(v);
        }
        return iDegree;
    }

    @Override
    public Set getEdgeSet() {
        HashSet<Edge> hRes = new HashSet<Edge>();
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            hRes.addAll(UnderlyingGraphs[iCnt].getEdgeSet());
        }
        return Collections.unmodifiableSet(hRes);
    }

    @Override
    public List getEdges(Vertex v) {
        ArrayList<Edge> lRes = new ArrayList<Edge>();
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            if (UnderlyingGraphs[iCnt].contains(v))
                try {
                    lRes.addAll(UnderlyingGraphs[iCnt].getEdges(v));
                }
                catch (NullPointerException ne) {
                    // This segment-graph contains no edge info on vertex
                    // Continue
                }
        }
        return lRes;
    }

    @Override
    public int getEdgesCount() {
        int iEdgeCount = 0;
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            iEdgeCount += UnderlyingGraphs[iCnt].getEdgesCount();
        }
        return iEdgeCount;
    }

    public Distribution<Double> getEdgeCountDistro() {
        Distribution<Double> dRes = new Distribution();
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            dRes.setValue((double)iCnt, UnderlyingGraphs[iCnt].getEdgesCount());
        }

        return dRes;
    }
    @Override
    public Set getVertexSet() {
        // Return one of the duplicate verex sets
        HashSet<Vertex> hRes = new HashSet<Vertex>(UnderlyingGraphs[0].getVertexSet());
        return Collections.unmodifiableSet(hRes);
    }

    @Override
    public int getVerticesCount() {
        int iVerticesCount = 0;
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            iVerticesCount += UnderlyingGraphs[iCnt].getVerticesCount();
        }
        return iVerticesCount;
    }

    @Override
    public Iterator getVerticesIterator() {
        //return UniqueVertices.values().iterator();
        HashSet<Vertex> alRes = new HashSet<Vertex>();
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            alRes.addAll(UnderlyingGraphs[iCnt].getVertexSet());
        }
        return alRes.iterator();
    }

    @Override
    public synchronized Vertex locateVertex(String sVertexLabel) {
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            if (UnderlyingGraphs[iCnt].UniqueVertices.containsKey(sVertexLabel))
                return UnderlyingGraphs[iCnt].locateVertex(sVertexLabel);
        }
        return null;

    }

    @Override
    public synchronized Vertex locateVertex(Vertex v) {
        return locateVertex(v.getLabel());
    }

    @Override
    public void remove(Vertex v) throws Exception {
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            UnderlyingGraphs[iCnt].remove(v);
        }
        super.remove(v);
    }

    @Override
    public void removeEdge(Edge edge) throws Exception {
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            if (UnderlyingGraphs[iCnt].containsEdge(edge))
                UnderlyingGraphs[iCnt].removeEdge(edge);
        }
    }

    @Override
    public void removeEdges(Vertex v) throws Exception {
        for (int iCnt=0; iCnt < UnderlyingGraphs.length; iCnt++) {
            if (UnderlyingGraphs[iCnt].contains(v))
                UnderlyingGraphs[iCnt].removeEdges(v);
        }
        
    }

    ///////////////////////////////////////////////////
    // Not implemented.
    // TODO: Implement?
    ///////////////////////////////////////////////////
    @Override
    public void forgetConnectedSets() {
        throw new NotImplementedException();
    }

    @Override
    public Vertex getClosest(Vertex v) {
        throw new NotImplementedException();
    }

    @Override
    public int getDegree() {
        throw new NotImplementedException();
    }

    @Override
    public Collection getConnectedSet() {
        throw new NotImplementedException();
    }

    @Override
    public Set getConnectedSet(Vertex v) {
        throw new NotImplementedException();
    }

    @Override
    public GraphFactory getGraphFactory() {
        throw new NotImplementedException();
    }

    @Override
    public WeightedGraph shortestPath(Vertex vertex) {
        throw new NotImplementedException();
    }

    @Override
    public List traverse(Vertex startat) {
        throw new NotImplementedException();
    }


    @Override
    public List cloneVertices() {
        throw new NotImplementedException();
    }

    @Override
    public Set getAdjacentVertices(List vertices) {
        throw new NotImplementedException();
    }

    @Override
    public GraphTraversal getTraversal() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isConnected(Vertex v1, Vertex v2) {
        throw new NotImplementedException();
    }
}
