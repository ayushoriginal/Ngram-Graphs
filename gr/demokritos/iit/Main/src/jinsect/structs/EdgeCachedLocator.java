/*
 * EdgeCachedLocator.java
 *
 * Created on June 1, 2007, 10:38 AM
 *
 */

package gr.demokritos.iit.jinsect.structs;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;

/**
 *
 * @author ggianna
 */
public class EdgeCachedLocator implements Serializable {
    protected int CacheMaxSize;
    protected HashMap Cache;
    protected TreeMap CacheAccess;
    protected HashMap ElementAccessTime;
    protected long TimeCnt = Long.MIN_VALUE;
    protected long lHits = 0, lMisses = 0;
    
    /** The locator should NOT hold any cache data and thus this method is 
     * overriden by an empty method.
     */
    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    }
    
    /** The locator should NOT hold any cache data and thus this method is 
     * overriden by an empty method.
     */
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    }
    
    /** Creates a new instance of EdgeCachedLocator, concerning a specific graph.
     *@param iCacheMaxSize The maximum number of edges to cache.
     */
    public EdgeCachedLocator(int iCacheMaxSize) {
        CacheMaxSize = iCacheMaxSize;
        
        Cache = new HashMap();
        CacheAccess = new TreeMap();
        ElementAccessTime = new HashMap();
    }
    
    /** Looks up a given directed edge in a selected graph. 
     * The edge is described based on the label of its
     *vertices.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head of the edge.
     *@param vTail A vertex with the desired label for the tail of the edge.
     *@return The edge, if found, otherwise null.
     */
    public final Edge locateDirectedEdgeInGraph(UniqueVertexGraph gGraph, Vertex vHead, Vertex vTail) {
        Edge eRes = null;
        try {
            vHead = locateVertexInGraph(gGraph, vHead);
            if (vHead == null)
                return null;
            vTail = locateVertexInGraph(gGraph, vTail);
            if (vTail == null)
                return null;
            
            // Check cache
            TreeMap hOutVertices = (TreeMap)Cache.get(vHead.getLabel());
            List lEdges;
            if (hOutVertices == null) { // If not found
                lMisses++;
                lEdges = getOutgoingEdgesUncached(gGraph, vHead);
                // TODO: REMOVE
                // lEdges = getOutgoingEdgesUncached(gGraph, vHead);
                ///////////////
                // Check if time has reached max value
                if (TimeCnt == Long.MAX_VALUE) {
                    // if so, cache must be reset
                    resetCache();
                }
                
                hOutVertices = new TreeMap();
                for (Object elem : lEdges) {
                    hOutVertices.put(((Edge)elem).getVertexB().getLabel(), elem);
                    if (((Edge)elem).getVertexB().getLabel().equals(vTail.getLabel()))
                        eRes = (Edge)elem;
                }
                
                // Update cache
                Cache.put(vHead.getLabel(), hOutVertices);
                ElementAccessTime.put(vHead.getLabel(), ++TimeCnt);
            }
            else {
                lHits++;
                ElementAccessTime.put(vHead.getLabel(), ++TimeCnt);
            }
            
            // Update Access time
            CacheAccess.put(TimeCnt, vHead.getLabel());
            
            // Remove oldest to keep max size
            if (Cache.size() > CacheMaxSize) {
                // Keep doing the following
                while (true) {
                    // Check if the oldest element has been reused
                    String sVertexLabel = (String)CacheAccess.get(CacheAccess.firstKey());
                    if ((Long)ElementAccessTime.get(sVertexLabel) > (Long)CacheAccess.firstKey())
                    {
                        // If it has, remove the older time reference
                        CacheAccess.remove(CacheAccess.firstKey());
                    }
                    else
                    {
                        // else remove the object from cache
                        Cache.remove(sVertexLabel);
                        CacheAccess.remove(CacheAccess.firstKey());
                        ElementAccessTime.remove(sVertexLabel);
                        // and break
                        break;
                    }
                }
            }
            else // If already cached
            {
                return (Edge)(hOutVertices.get(vTail.getLabel()));
            }
            
            return eRes;
        }
        catch (NullPointerException e) {
            return null;
        }        
    }

    /** Looks up a vertex in a given graph.
     *@param gGraph The graph to use.
     *@param vToFind The vertex to locate.
     *@return The vertex, if found, otherwise null.
     */
    public final Vertex locateVertexInGraph(UniqueVertexGraph gGraph, Vertex vToFind) {
        return gGraph.locateVertex(vToFind);
    }
    
    /** Looks up a given (undirected) edge in a selected graph. 
     * The edge is described based on the label of its
     *vertices.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head or tail of the edge.
     *@param vTail A vertex with the desired label for the tail or tail of the edge.
     *@return The edge, if found, otherwise null.
     */
    public final Edge locateEdgeInGraph(UniqueVertexGraph gGraph, Vertex vHead, Vertex vTail) {
        Edge eRes = locateDirectedEdgeInGraph(gGraph, vHead, vTail);
        return eRes == null ? locateDirectedEdgeInGraph(gGraph, vTail, vHead) : eRes;
    }
    
    /** Gets the outgoing edges of a given vertex in a directed graph.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head of the edge.
     *@return A list of outgoing edges from <code>vHead</code>. If no such edges exist returns an
     *empty list.
     */
    public final List getOutgoingEdges(UniqueVertexGraph gGraph, Vertex vHead) {
        Vertex vNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(gGraph, vHead.toString());
        ArrayList lRes = new ArrayList();
        if (vNode != null) {
            List neighbours = gGraph.getAdjacentVertices(vNode);
            Iterator iIter = neighbours.iterator();
            while (iIter.hasNext()) {
                Vertex vCandidateParent = (Vertex)iIter.next();
                // Add only child neighbours
                Edge eCur = locateDirectedEdgeInGraph(gGraph, vNode, vCandidateParent);
                if (eCur != null)
                    lRes.add(eCur);
            }
            
            return lRes;
        }
        
        return new ArrayList();
    }    
    
    /** Clears the cache. */
    public void resetCache() {
        Cache.clear();
        ElementAccessTime.clear();
        CacheAccess.clear();

        TimeCnt = Long.MIN_VALUE;
    }
    
    /**Updates cache as needed, if the edges of any vertex already contained within the cache are changed.
     *@param e The new edge.
     */
    public void addedEdge(Edge e) {
            // Check cache
            TreeMap hOutVertices = (TreeMap)Cache.get(e.getVertexA().getLabel());
            if (hOutVertices == null)
                return; // Not cached
            else
                hOutVertices.put(e.getVertexB().getLabel(), e); // Update cache
    }
    
    /** Returns the success ratio of the cache.
     *@return The ratio of hits (number of hits / number of total cache accesses) of the cache.
     */
    public double getSuccessRatio() {
        return (double)lHits / (lHits + lMisses);
    }
    
    /** Uncached outgoing edge lookup.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head of the edge.
     *@return A list of outgoing edges from <code>vHead</code>. If no such edges exist returns an
     *empty list. Returns null if the vertex does not exist in the graph.
     */
    protected List getOutgoingEdgesUncached(UniqueVertexGraph gGraph, Vertex vHead) {
        Vertex vNode = gGraph.locateVertex(vHead);
        if (vNode == null)
            return null;
        ArrayList lRes = new ArrayList();
        
        for (Edge lCand : (List<Edge>)gGraph.getEdges(vHead)) {
            if (lCand.getVertexA().equals(vHead))
                lRes.add(lCand);
        }
        return lRes;
    }    
}
