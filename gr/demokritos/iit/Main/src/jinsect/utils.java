/*
 * utils.java
 *
 */

package gr.demokritos.iit.jinsect;

import gr.demokritos.iit.conceptualIndex.structs.Concatenation;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.conceptualIndex.structs.DistributionGraph;
import gr.demokritos.iit.conceptualIndex.structs.Union;
import gr.demokritos.iit.jinsect.storage.IFileLoader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.swing.JFrame;
import gr.demokritos.iit.jinsect.algorithms.statistics.CombinationGenerator;
import gr.demokritos.iit.jinsect.algorithms.nlp.PorterStemmer;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import java.io.IOException;
import java.io.StringReader;
import java.util.TreeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/** A class including a set of useful, general purpose functions.
 *
 * @author ggianna
 */
public final class utils {
    public static long Count = 0;
    public static long Sum = 0;
    
    /** Math.max reimplemented.
     */
    public final static double max(double Num1, double Num2)
    {
        return (Num1 > Num2) ? Num1 : Num2;
    }
    
    /** Math.min reimplemented.
     */
    public final static double min(double Num1, double Num2)
    {
        return -max(-Num1, -Num2);
    }
    
    /** Math.abs reimplemented.
     */
    public final static double abs(double dNum) {
        return (dNum > 0)?dNum:-dNum;
    }

    /** Looks up a given (undirected) edge in a selected graph. 
     * The edge is described based on the label of its
     *vertices.
     *@param gGraph The graph to use.
     *@param sHead The label of the head or tail vertex of the edge.
     *@param sTail The label of the tail or tail vertex of the edge.
     *@return The edge, if found, otherwise null.
     */
    public static final Edge locateEdgeInGraph(UniqueVertexGraph gGraph, String sHead, String sTail) {
        VertexImpl vHead = new VertexImpl();
        vHead.setLabel(sHead);
        VertexImpl vTail = new VertexImpl();
        vTail.setLabel(sTail);
        return locateEdgeInGraph(gGraph, vHead, vTail);
    }
    
    /** Looks up a vertex, based on its label, within a given graph.
     *@param gGraph The graph to use.
     *@param sToFind The label of the desired vertex.
     *@return The vertex, if found, otherwise null.
     */
    public static final Vertex locateVertexInGraph(UniqueVertexGraph gGraph, String sToFind) {
        return gGraph.locateVertex(new VertexImpl(sToFind));
    }
    
    /** Looks up a vertex in a given graph.
     *@param gGraph The graph to use.
     *@param vToFind The vertex to locate.
     *@return The vertex, if found, otherwise null.
     */
    public static final Vertex locateVertexInGraph(UniqueVertexGraph gGraph, Vertex vToFind) {
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
    public static final Edge locateEdgeInGraph(UniqueVertexGraph gGraph, Vertex vHead, Vertex vTail) {
        /*
        try {
            vHead = locateVertexInGraph(gGraph, vHead);
            if (vHead == null)
                return null;
            vTail = locateVertexInGraph(gGraph, vTail);
            if (vTail == null)
                return null;
            
            List lEdges =  gGraph.getEdges(vHead);
            java.util.Iterator iIter = lEdges.iterator();
            String sTailLbl = vTail.getLabel();
            while (iIter.hasNext())
            {
                Edge eCurrent = (Edge)iIter.next();
                if (vHead != vTail) {
                    if ((eCurrent.getVertexA().getLabel().compareTo(sTailLbl) == 0) || 
                            (eCurrent.getVertexB().getLabel().compareTo(sTailLbl) == 0))
                        return eCurrent; // Found. Return edge.
                }
                else 
                {
                    if ((eCurrent.getVertexA().getLabel().equals(sTailLbl)) && 
                            (eCurrent.getVertexB().getLabel().equals(sTailLbl)))
                        return eCurrent; // Found. Return edge.
                }
            }
            return null;    // Not found
        }
        catch (NullPointerException e) {
            return null;
        }
        */
        Edge eRes = locateDirectedEdgeInGraph(gGraph, vHead, vTail);
        return eRes == null ? locateDirectedEdgeInGraph(gGraph, vTail, vHead) : eRes;
    }

    /** Looks up a given directed edge in a selected graph. 
     * The edge is described based on the label of its
     *vertices.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head of the edge.
     *@param vTail A vertex with the desired label for the tail of the edge.
     *@return The edge, if found, otherwise null.
     */
    public static final Edge locateDirectedEdgeInGraph(UniqueVertexGraph gGraph, Vertex vHead, Vertex vTail) {
        try {
            vHead = locateVertexInGraph(gGraph, vHead);
            if (vHead == null)
                return null;
            vTail = locateVertexInGraph(gGraph, vTail);
            if (vTail == null)
                return null;
            
            List lEdges =  gGraph.getEdges(vHead);
            java.util.Iterator iIter = lEdges.iterator();
            String sTailLbl = vTail.getLabel();
            while (iIter.hasNext())
            {
                Edge eCurrent = (Edge)iIter.next();
                if (vHead != vTail) {
                    if (eCurrent.getVertexB().getLabel().compareTo(sTailLbl) == 0)
                        return eCurrent; // Found. Return edge.
                }
                else 
                {
                    if ((eCurrent.getVertexA().getLabel().equals(sTailLbl)) && 
                            (eCurrent.getVertexB().getLabel().equals(sTailLbl)))
                        return eCurrent; // Found. Return edge.
                }
            }
            return null;    // Not found
        }
        catch (NullPointerException e) {
            return null;
        }
    }
    
    /** Gets the outgoing edges of a given vertex in a graph.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head of the edge.
     *@return A list of outgoing edges from <code>vHead</code>. If no such edges exist returns an
     *empty list.
     */
    public static final List getOutgoingEdges(UniqueVertexGraph gGraph, Vertex vHead) {
        Vertex vNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(gGraph, vHead.toString());
        ArrayList lRes = new ArrayList();
        if (vNode != null) {
            List neighbours = gGraph.getAdjacentVertices(vNode);
            Iterator iIter = neighbours.iterator();
            while (iIter.hasNext()) {
                Vertex vCandidateParent = (Vertex)iIter.next();
                // Add only child neighbours
                Edge eCur = gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(gGraph, vNode, vCandidateParent);
                if (eCur != null)
                    lRes.add(eCur);
            }
            
            return lRes;
        }
        
        return new ArrayList();
    }
    
    /** Gets the adjacent vertices of outgoing edges from a given vertex in a 
     * graph.
     *@param gGraph The graph to use.
     *@param vHead A vertex with the desired label for the head of the edge.
     *@return A list of vertices of outgoing edges from the given head. 
     * If no such edges exist returns an empty list.
     */
    public static final List getOutgoingAdjacentVertices(UniqueVertexGraph gGraph, Vertex vHead) {
        Vertex vNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(gGraph, vHead.toString());
        ArrayList<Vertex> lRes = new ArrayList<Vertex>();
        if (vNode != null) {
            List neighbours = gGraph.getAdjacentVertices(vNode);
            Iterator iIter = neighbours.iterator();
            while (iIter.hasNext()) {
                Vertex vCandidateParent = (Vertex)iIter.next();
                // Add only child neighbours
                Edge eCur = gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(gGraph, vNode, vCandidateParent);
                if (eCur != null)
                    lRes.add(vCandidateParent);
            }
            
            return lRes;
        }
        
        return new ArrayList();
    }
    
    /** Gets the incoming edges to a given vertex in a directed graph.
     *@param gGraph The graph to use.
     *@param vTail A vertex with the desired label for the tail of the edge.
     *@return A list of incoming edges to <code>vTail</code>. If no such edges exist returns an
     *empty list.
     */
    public static final List getIncomingEdges(UniqueVertexGraph gGraph, Vertex vTail) {
        Vertex vNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(gGraph, vTail.toString());
        ArrayList lRes = new ArrayList();
        if (vNode != null) {
            List neighbours = gGraph.getAdjacentVertices(vNode);
            Iterator iIter = neighbours.iterator();
            while (iIter.hasNext()) {
                Vertex vCandidateParent = (Vertex)iIter.next();
                // Remove non-parent neighbours
                // Add only poarent neighbours
                Edge eCur = gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(gGraph, vCandidateParent, vNode);
                if (eCur != null)
                    lRes.add(eCur);
            }
            
            return lRes;
        }
        
        return new ArrayList();
    }
    
    /** Gets the adjacent vertices of incoming edges of a given vertex in a 
     * directed graph.
     *@param gGraph The graph to use.
     *@param vTail A vertex with the desired label for the tail of the edge.
     *@return A list of incoming vertices to <code>vTail</code>. If no such edges exist returns an
     *empty list.
     */
    public static final List getAdjacentIncomingVertices(UniqueVertexGraph gGraph, Vertex vTail) {
        Vertex vNode = utils.locateVertexInGraph(gGraph, vTail.toString());
        ArrayList<Vertex> lRes = new ArrayList();
        if (vNode != null) {
            List neighbours = gGraph.getAdjacentVertices(vNode);
            Iterator iIter = neighbours.iterator();
            while (iIter.hasNext()) {
                Vertex vCandidateParent = (Vertex)iIter.next();
                // Remove non-parent neighbours
                // Add only poarent neighbours
                Edge eCur = gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(gGraph, vCandidateParent, vNode);
                if (eCur != null)
                    lRes.add(vCandidateParent);
            }
            
            return lRes;
        }
        
        return new ArrayList();
    }
    
    
    /** Testbench function. Not to be used.
     */
    public static void main(String[] args) throws Exception {
        String[] s = "This is a test. 1 2 3 4-5.".split("(\\s|\\p{Punct})+");
        List<String> alTest = Arrays.asList(s);
        shuffleList(alTest);
        System.out.println(alTest.toString());
    }    
    
    /** Converts milliseconds to a string representation of x hours, y min, z sec.
     *@param lMillis The long number of milliseconds.
     *@return The formated string.
     */
    public static final String millisToMinSecString(long lMillis) {
        return String.format("%d hours %d min %d sec", lMillis / (1000 * 60 * 60), (lMillis / (1000 * 60)) % 60, 
                (lMillis / 1000) % 60);
    }

    /** Repeatedly randomizes a given list.
     *
     * @param l The input list to randomize.
     * @param repeat The times to perform randomization. Higher values allow
     * more shuffling.
     */
    public static final void shuffleList(List l, int repeat) {
        for (int iCnt = 0 ; iCnt < repeat; iCnt++)
            shuffleList(l);
    }

    /** Randomizes the order of items in a given list.
     *@param l The input list that will be modified.
     */
    public static final void shuffleList(List l) {
        // DEBUG LINES
        // int iSwapCount = 0;
        // double dSwapDistance = 0;
        //////////////
        
        Random d = new Random();
        
        for (int iCnt = 0; iCnt < l.size() - 1; iCnt++) {
            for (int iSwapPos = iCnt + 1; iSwapPos < l.size(); iSwapPos++) {
                // Randomly
                if (d.nextBoolean())
                {
                    // Swap files
                    Object oTemp = l.get(iSwapPos);
                    l.set(iSwapPos, l.get(iCnt));
                    l.set(iCnt, oTemp);
                    continue;
                    
                    // DEBUG LINES
                    // iSwapCount++;
                    // dSwapDistance = (dSwapDistance + Math.abs(iSwapPos - iCnt)) / 2;
                    //////////////
                }            
            }
        }
        
        // Reverse
        for (int iCnt = l.size() - 1; iCnt > 0 ; iCnt--) {
            for (int iSwapPos = iCnt - 1; iSwapPos > 0; iSwapPos--) {
                // Randomly
                if (d.nextBoolean())
                {
                    // Swap files
                    Object oTemp = l.get(iSwapPos);
                    l.set(iSwapPos, l.get(iCnt));
                    l.set(iCnt, oTemp);
                    
                    // DEBUG LINES
                    // iSwapCount++;
                    // dSwapDistance = (dSwapDistance + Math.abs(iSwapPos - iCnt)) / 2;
                    //////////////
                }            
            }
        }
        
        // DEBUG LINES
        // System.out.println(String.format("Position Distance:%2.2f\tSwap Count:%d", dSwapDistance, iSwapCount));
        /////////////
    }
    
    /** Splits a given string to its words, without stemming. Words are considered to be everything, 
     * but sequences of whitespace and punctuation.
     *@param sStr The input string.
     *@return An array of String containing the words of the given string.
     */
    public static final String[] splitToWords(String sStr) {
        return splitToWords(sStr, false); // Do NOT stem
    }
    
    /** Splits a given string to its words, without stemming. Words are considered to be everything, 
     * but sequences of whitespace and punctuation.
     *@param sStr The input string.
     *@param bStem True if stemming should be performed to the input words, otherwise false.
     *@return An array of String containing the (possibly stemmed) words of the given string.
     */
    public static final String[] splitToWords(String sStr, boolean bStem) {
        PorterStemmer sStem = new PorterStemmer();
        // Removed conversion to lowercase
//        String [] sRes = sStr.toLowerCase().split("(\\s|\\p{Punct})+");
        String [] sRes = sStr.split("(\\s|\\p{Punct})+");
        if (bStem)
            for (int iCnt=0; iCnt < sRes.length; iCnt++)
                if (!sRes[iCnt].trim().equals(""))
                    try {
                        sRes[iCnt] = sStem.stem(sRes[iCnt]);
                    }
                    catch (Exception e)
                    {
                        // Stem failed. Ignore.
                        // System.out.println("Word '" + sRes[iCnt] + "' could not be stemmed.");
                    }
                
        return sRes;
    }

    /** Calculates the logarithm of a number using a given base.
     *@param dNumber The number whose logarithm is meant to be calculated.
     *@param dBase The base of the logarithm.
     *@return The logarithm base <code>dBase</code> of <code>dNumber</code>.
     */
    public static final double logX(double dNumber, double dBase) {
        return  Math.log(dNumber)/Math.log(dBase);
    }
    
    /** Creates a formatted string representation of an iterable object, sorting
     * the string representation of its parts at the output.
     *@param iIterable The iterable object.
     *@param sSeparator The separator to use for elements.
     *@return The ordered string representation of the iterable object.
     */
    public static final String printSortIterable(Iterable iIterable, 
            String sSeparator) {
        // Init buffer
        StringBuffer sOut = new StringBuffer();
        Iterator iIter = iIterable.iterator();
        // Use treeset to sort
        TreeSet<String> tsItems = new TreeSet<String>();
        while (iIter.hasNext()) {
            tsItems.add(iIter.next().toString());
        }
        
        // Add all string representations to buffer, using separator.
        for (Iterator<String> isCur = tsItems.iterator(); isCur.hasNext();) {
            sOut.append(isCur.next());
            if (isCur.hasNext())
                sOut.append(sSeparator);
        }
        return sOut.toString();
    }
    
    /** Creates a formatted string representation of an iterable
     * object using a given separator.
     *@param iIterable The (possibly nested) iterable object.
     *@param sSeparator The separator to use for elements of the same level.
     *@return The string representation of the (possibly nested) iterable object.
     */
    public static final String printIterable(Iterable iIterable, String sSeparator) {
        StringBuffer sbRes = new StringBuffer();
        
        Iterator iIter = iIterable.iterator();
        while (iIter.hasNext()) {
            Object oNext = iIter.next();
                sbRes.append(oNext.toString());
                if (iIter.hasNext())
                    sbRes.append(sSeparator);
        }
        
        return sbRes.toString();
    }
    
    /** Creates a formatted string representation of a (possibly nested) list, 
     * taking into account {@link Union} and 
     *{@link Concatenation} objects and using a given separator.
     *@param lToPrint The (possibly nested) list.
     *@param sSeparator The separator to use for elements of the same level.
     *@return The string representation of the (possibly nested) list.
     */
    public static final String printList(List lToPrint, String sSeparator) {
        String sRes = new String();
        
        Iterator iIter = lToPrint.iterator();
        while (iIter.hasNext()) {
            Object oNext = iIter.next();
            if ((oNext instanceof Union) || (oNext instanceof Concatenation))
            {
                sRes += oNext.toString();
                if (iIter.hasNext())
                    sRes += sSeparator;
            }
            else
                if (oNext instanceof List)
                sRes += "(" + printList((List)oNext, sSeparator) + ")";
            else
                sRes += sSeparator + (oNext.toString()) + sSeparator;
                
        }
        
        return sRes;
        
    }
    
    /** Creates a formatted string representation of a (possibly nested) list, 
     * taking into account {@link Union} and 
     *{@link Concatenation} objects.
     *@param lToPrint The (possibly nested) list.
     *@return The string representation of the (possibly nested) list.
     */
    public static final String printList(List lToPrint) {
        String sSeparator;
        if (lToPrint instanceof Union)
            sSeparator = "|";
        else
            sSeparator = ",";
        
        return printList(lToPrint, sSeparator);
    }
               
    /** Creates a {@link Union} of combinations of elements taken from a given list for a given number
     * of elements per combined set. 
     *@param oObj The input list.
     *@param iBySize The size of elements to use in every combination returned.
     *@return The {@link Union} of combination alternatives.
     */
    public static final Union getCombinationsBy(Object oObj, int iBySize) {
        Union uRes = new Union();
        
        List lList;
        // If unary, wrap in list.
        if (!(oObj instanceof List)) {
            lList = new ArrayList();
            lList.add(oObj);
        }
        else
            lList = (List)oObj;
        
        int[] indices;
        CombinationGenerator cgGen = new CombinationGenerator (lList.size(), iBySize);
        while (cgGen.hasMore()) {
          Concatenation cComb = new Concatenation();
          indices = cgGen.getNext ();
          for (int i = 0; i < indices.length; i++) {
            cComb.add(lList.get(indices[i]));
          }
          uRes.add(cComb);
        }        
        return uRes;
    }    
    
    /** Calculates the substrings matching particular requirements in a given string,
     * given a maximum substring size. If a string of a particular size is not matched, then it
     * is broken into its substrings, which in turn are attempted to be matched. Every matched 
     * substring is not analysed further.
     *@param sStr The input string.
     *@param iMaxSubStringSize The maximum substring size to take into account.
     *@param isMatcher  A matcher of type {@link IMatching} to use, in order to take into account 
     * a given substring or not.
     *@return A {@link Union} of the matched substrings within the given string.
     */
    public static final Union getSubStrings(String sStr, int iMaxSubStringSize, 
            IMatching isMatcher) {
        return getSubStrings(sStr, iMaxSubStringSize, 
            isMatcher, Integer.MAX_VALUE, 0);
    }
    
    /** Calculates the substrings matching particular requirements in a given string,
     * given a maximum substring size. If a string of a particular size is not matched, then it
     * is broken into its substrings, which in turn are attempted to be matched. Every matched 
     * substring is not analysed further. The string will also not be analyzed further from a 
     * given depth of analysis (break-downs).
     *@param sStr The input string.
     *@param iMaxSubStringSize The maximum substring size to take into account.
     *@param isMatcher  A matcher of type {@link IMatching} to use, in order to take into account 
     * a given substring or not.
     *@param iMaxDepth The maximum depth of analysis to use for the substring analysis.
     *@return A {@link Union} of the matched substrings within the given string.
     */
    public static final Union getSubStrings(String sStr, int iMaxSubStringSize, 
            IMatching isMatcher, int iMaxDepth) {
        return getSubStrings(sStr, iMaxSubStringSize, 
            isMatcher, iMaxDepth, 0);
    }
    
    /** Helper function. Calculates the substrings matching particular requirements in a given string,
     * given a maximum substring size. If a string of a particular size is not matched, then it
     * is broken into its substrings, which in turn are attempted to be matched. Every matched 
     * substring is not analysed further. The string will also not be analyzed further from a 
     * given depth of analysis (break-downs).
     *@param sStr The input string.
     *@param iMaxSubStringSize The maximum substring size to take into account.
     *@param isMatcher  A matcher of type {@link IMatching} to use, in order to take into account 
     * a given substring or not.
     *@param iMaxDepth The maximum depth of analysis to use for the substring analysis.
     *@param iCurDepth The current reached depth.
     *@return A {@link Union} of the matched substrings within the given string.
     */
    public static final Union getSubStrings(String sStr, int iMaxSubStringSize, 
            IMatching isMatcher, int iMaxDepth, int iCurDepth) {
        // Check max depth
        if (iCurDepth > iMaxDepth)
            return new Union();
        
        Union aRes = new Union();
        if (sStr.length() == 0)
            return aRes;
        
        // For each window of size iMaxSubStringSize within the string
        boolean bFoundMainMatch = false;
        for (int iCnt=0; iCnt <= sStr.length() - iMaxSubStringSize; iCnt++) {
            List aTemp = new ArrayList();
            // Break the word in three pieces (prefix, window, suffix)
            String sPrefix = sStr.substring(0, iCnt);
            String sMain = sStr.substring(iCnt, iMaxSubStringSize + iCnt);
            String sSuffix = sStr.substring(iMaxSubStringSize + iCnt);
            // If main part matches
            if (isMatcher.match(sMain)) {
                // apply the lookup process for the rest of the word parts (prefix, suffix)
                // and create return set
                Union u = getSubStrings(sPrefix, sPrefix.length(), isMatcher, iMaxDepth, iCurDepth + 1);
                if (!(u.isEmpty()))
                    aTemp = getListProduct(u, sMain);
                else
                {
                    ArrayList lList = new ArrayList();
                    lList.add(sMain);
                    aTemp.add(lList);
                }
                    
                
                u = getSubStrings(sSuffix, sSuffix.length(), isMatcher, iMaxDepth, iCurDepth + 1);
                ArrayList lTmp = new ArrayList();
                if (!(u.isEmpty()))
                    aTemp = getListProduct(aTemp, u);
                
                bFoundMainMatch = true;
                
                aRes.addAll(aTemp);
            }            
            // else continue
        }
        if (!bFoundMainMatch) // if no match was found at this size
            // attempt smaller size
            if (sStr.length() < 2) {
                aRes.add(sStr);
            }
            else
                if (iMaxSubStringSize > 1)
                    return getSubStrings(sStr, iMaxSubStringSize - 1, isMatcher, iMaxDepth, iCurDepth + 1);
        
            return aRes;
    }
    
    /** Returns the system encoding String.
     *@return A String indicating the System default encoding.
     */
    public static String getSystemEncoding() {
        String defaultEncoding = new InputStreamReader(
              new ByteArrayInputStream(new byte[0])).getEncoding();
        
        return defaultEncoding;
        
    }
    
    /** Converts a string to UTF-8 encoding.
     *@param sStr The input string.
     *@return A UTF-8 encoded version of the input string.
     */
    public static String toUTF8(String sStr) {
        byte[] baBytes = sStr.getBytes();
        try {
            return new String(baBytes, "UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
            return new String(baBytes);
        }
        
    }
    
    /** The sign function.
     *@param dNum The input number.
     *@return 1 if the input number is positive, -1 if negative and zero otherwise.
     */
    public static double sign(double dNum) {
        return dNum == 0.0 ? dNum : dNum / Math.abs(dNum);

    }
    
    /*
    public static List getFlattenedList(Object oNestedLists) {
        if (oNestedLists instanceof Union)
            return getFlattenedUnion(oNestedLists, new ArrayList());
        if (oNestedLists instanceof Concatenation)
            return getFlattenedConcatenation(oNestedLists, new ArrayList());
        
        ArrayList alTemp = new ArrayList();        
        alTemp.add(oNestedLists);
        
        return alTemp;
    }
    
    private static final List getFlattenedUnion(Object oNestedLists, List lFlatList) {
        List lFirstList = new ArrayList();
        List lSecondList;
        
        if (oNestedLists instanceof Union) {
            // Get list
            List lNestedLists = (List)oNestedLists;
            Iterator iCurObj = lNestedLists.iterator();
            
            // For every object
            while (iCurObj.hasNext()) {
                Object oCur = iCurObj.next();
                // If it is a list
                if (oCur instanceof Concatenation)
                    lFirstList.addAll(getFlattenedConcatenation(oCur, lFlatList));
                else
                    if (oCur instanceof Union)
                        lFirstList.addAll(getFlattenedUnion(oCur, lFlatList));
                    else
                        lFirstList.add(oCur);
            }
        }
        else // otherwise
        {
            // If not a list
            lFirstList.add(oNestedLists); // Just add to flat list
        }
        
        lSecondList = lFlatList;
        lFlatList = getPermutations(lFirstList, lSecondList);
        
        return lFlatList;
    }
    
    private static final List getFlattenedConcatenation(Object oNestedLists, List lFlatList) {
        if (oNestedLists instanceof Concatenation) {
            // Get list
            List lNestedLists = (List)oNestedLists;
            Iterator iCurObj = lNestedLists.iterator();
            
            // For every object
            while (iCurObj.hasNext()) {
                Object oCur = iCurObj.next();
                // If it is a list
                if (oCur instanceof Concatenation)
                    getFlattenedConcatenation(oCur, lFlatList);
                else
                    if (oCur instanceof Union)
                        getFlattenedUnion(oCur, lFlatList);
                    else
                        lFlatList.add(oCur);
            }
        }
        else // otherwise
        {
            // If not a list
            lFlatList.add(oNestedLists); // Just add to flat list
        }
        
        return lFlatList;
    }
    */
    
    /** Calculates the product of two lists.
     *@param oA The first list.
     *@param oB The second list.
     *@return The product of the elements of the two lists as a new list of lists.
     */
    private static final List getListProduct(Object oA, Object oB) {
        // Join list of lists
        ArrayList aRes = new ArrayList();
        List lAList, lBList;
        
        // If unary, create unary list, else use existing list
        if (!(oA instanceof List)) {
            lAList = new ArrayList();
            lAList.add(oA);
        }
        else
            lAList = (List)oA;
        if (!(oB instanceof List)) {
            lBList = new ArrayList();
            lBList.add(oB);
        }
        else
            lBList = (List)oB;
        
        // For every item in A
        Iterator iA = lAList.iterator();        
        while (iA.hasNext()) {
            Object oANext = iA.next();
            
            // For every item in B
            Iterator iB = lBList.iterator();
            while (iB.hasNext()) {
                Object oBNext = iB.next();
                
                ArrayList lTemp = new ArrayList();
                if (oANext instanceof List)
                    lTemp.addAll((List)oANext);
                else
                    lTemp.add(oANext);
                if (oBNext instanceof List)
                    lTemp.addAll((List)oBNext);
                else
                    lTemp.add(oBNext);
                
                aRes.add(lTemp);
            }
            
        }
        
        return (List)aRes;
    }
    
    /** Bubble-sorts an array of comparable items.
     *@param aArr An array of {@link Comparable} objects.
     */
    public static final void bubbleSortArray(Comparable[] aArr) {
        boolean bChanged = true;
        Comparable a, b;
        while (bChanged) {
            bChanged = false;
            for (int iCnt = 0; iCnt < aArr.length - 1; iCnt++) {
                a = aArr[iCnt];
                b = aArr[iCnt + 1];
                if (a.compareTo(b) > 0) {
                    aArr[iCnt] = b;
                    aArr[iCnt + 1] = a;
                    bChanged = true;
                }
            }
        }
    }
    
    /**
     * Bubble sorts the strings in a given String list, where the longest string is the first checked.
     *@param l The input list.
     *@return The sorted list.
     */
    public static final List bubbleSortVerticesByStringLength(List l) {
        boolean bChanged = true;
        while (bChanged) {
            bChanged = false;
            for (int iCnt = 0; iCnt < l.size() - 1; iCnt++) {
                if ((l.get(iCnt + 1).toString()).length() > (l.get(iCnt).toString()).length()) {
                    VertexImpl vTmp = (VertexImpl)(l.get(iCnt + 1));
                    l.set(iCnt + 1, l.get(iCnt));
                    l.set(iCnt, vTmp);
                    bChanged = true;
                }
            }
        }
        
        return l;
    }
    
    /**
     *Parses the command line expecting values of either
     *`-switch` or
     *`-key=value`
     *and returns corresponding {@link Hashtable}, with switches as keys
     *and `TRUE` as value, or `key` as keys and `value` as values
     *@param sCommands The command line array of Strings.
     *@return The described hashtable.
     */
    public static Hashtable parseCommandLineSwitches(String[] sCommands) {
        Hashtable hRes = new Hashtable();
        Iterator iStr = Arrays.asList(sCommands).iterator();
        while (iStr.hasNext()) {
            String sToken = (String)iStr.next();
            String sType, sVal;
            if (sToken.indexOf("-")==0) {
                // Switch
                if (sToken.contains("=")) {
                    // Parameter
                    sType = (sToken.split("=")[0]).substring(1); // Take part before '=' as key, omitting dash.
                    sVal = sToken.split("=")[1]; // Take part after '=' as value
                }
                else
                {
                    // Simple switch
                    sType = sToken.substring(1); // Omit dash
                    sVal = "TRUE";
                }
                
                hRes.put(sType, sVal);
            }
        }
        
        return hRes;
    }
    
    /** Given a {@link Hashtable} and a given option string, this function returns either the
     *option set in the hashtable, or a given default if the option has not been set.
     *@param hSwitches The hashtable of switches (see also <code>parseCommandLineSwitches</code>).
     *@param sOption The name of the option of interest.
     *@param sDefault The default value to be used if the option has not been set.
     *@return The value of the switch, or the default value if no value has been set.
     */
    public static String getSwitch(Hashtable hSwitches, String sOption, String sDefault) {
        Iterator iIter = hSwitches.keySet().iterator();
        while (iIter.hasNext()) {
            String sCurSwitch = (String)iIter.next();
            if (sCurSwitch.equals(sOption))
                return (String)hSwitches.get(sCurSwitch);
        }
        return sDefault;
    }
    
    /***
     *Returns the sum of a sequence of numbers in a specified range
     *@param iStart The minimum term of the sequence
     *@param iEnd The maximum term of the sequence
     ***/
    public static int sumFromTo(int iStart, int iEnd) {
        int iRes = 0;
        for (int iCnt = iStart; iCnt <= iEnd; iRes += iCnt++);
        return iRes;
    }
    
    public double getHistogramTotal(HashMap hHist) {
        Iterator iIter = hHist.values().iterator();
        double dSum = 0.0;
        while (iIter.hasNext()) {
            dSum += ((Double)iIter.next()).doubleValue();
        }
        return dSum;
    }    
    
    /** Renders a graph to its DOT representation (See GraphViz for more info on the format).
     *@param gTree The input graph.
     *@param bDirected Indicate whether the graph should be described as a directed graph or not.
     *@return The DOT formatted string representation of the graph.
     */
    public static String graphToDot(UniqueVertexGraph gTree, boolean bDirected) {
        StringBuffer sb = new StringBuffer();
        String sConnector;
        boolean bDistroGraph = gTree instanceof DistributionGraph;
        
        // Render graph
        if (!bDirected) {
            sb.append("graph {\n");
            sConnector = "--";
        }
        else {
            sb.append("digraph {\n");
            sConnector = "->";
        }
        
        Iterator iIter = gTree.getEdgeSet().iterator();
        while (iIter.hasNext()) {
            Edge e = (Edge)iIter.next();
            String sA = "_" + e.getVertexA().toString().replaceAll("\\W", "_");
            String sB = "_" + e.getVertexB().toString().replaceAll("\\W", "_");
            String sLabel = "";
            if (e instanceof WeightedEdge) {
                sLabel += String.format("%4.2f", ((WeightedEdge)e).getWeight());
            }
            if (bDistroGraph) {
                Distribution dTmp;
                if ((dTmp = (Distribution)((DistributionGraph)gTree).EdgeDistros.get(e)) != null)
                    sLabel += " - Distro: " + dTmp.toString();
            }
            if (e instanceof WeightedEdge)
                sb.append("\t" + sA + " " + sConnector + " " + sB + 
                    " [label=\"" + sLabel.replaceAll("\\s+", " ") + "\"]\n");
            else
                sb.append("\t" + sA + " " + sConnector + " " + sB + "\n");
            sb.append("\t" + sA + " [label=\"" + sA + "\"] " + "\n");
        }
        sb.append("}");
        
        return sb.toString();
    }
    
    /** Renders a graph to its DOT representation (See GraphViz for more info on the format).
     *@param gTree The input graph.
     *@param bDirected Indicate whether the graph should be described as a directed graph or not.
     *@param hEdgeDistros The map between edges and their distributions
     *@return The DOT formatted string representation of the graph.
     */
    public static String graphToDot(UniqueVertexGraph gTree, boolean bDirected, Map hEdgeDistros) {
        StringBuffer sb = new StringBuffer();
        String sConnector;
        boolean bDistroGraph = (hEdgeDistros != null);
        
        // Render graph
        if (!bDirected) {
            sb.append("graph {\n");
            sConnector = "--";
        }
        else {
            sb.append("digraph {\n");
            sConnector = "->";
        }
        
        Iterator iIter = gTree.getEdgeSet().iterator();
        while (iIter.hasNext()) {
            Edge e = (Edge)iIter.next();
            // Always use the underscore prefix
            String sA = "_" + e.getVertexA().toString().replaceAll("\\W", "_");
            String sB = "_" + e.getVertexB().toString().replaceAll("\\W", "_");
            String sLabel = "";
            if (e instanceof WeightedEdge) {
                sLabel += String.format("%4.2f", ((WeightedEdge)e).getWeight());
            }
            if (bDistroGraph) {
                Distribution dTmp;
                if ((dTmp = (Distribution)(hEdgeDistros.get(e))) != null)
                    sLabel += " - Distro: " + dTmp.toString();
            }
            if (e instanceof WeightedEdge)
                sb.append("\t" + sA + " " + sConnector + " " + sB + 
                    " [label=\"" + sLabel.replaceAll("\\s+", " ") + "\"]\n");
            else
                sb.append("\t" + sA + " " + sConnector + " " + sB + "\n");
            sb.append("\t" + sA + " [label=\"" + sA + "\"] " + "\n");
        }
        sb.append("}");
        
        return sb.toString();
    }
    
    /** Loads the contents of a file into a string, <i>without preserving newlines</i>. 
     *@param sFilename The filename of the file to load.
     *@return A String containing the contents of the given file.
     */
    public static String loadFileToString(String sFilename) {
        StringBuffer sb = new StringBuffer();
        try {
           BufferedReader in = new BufferedReader(new FileReader(sFilename));
           String line;
           while ((line = in.readLine()) != null) {
            sb.append(line);
           }
           in.close();
        } catch (Exception e) {
            System.err.println("Coult not load file:" + sFilename);
            e.printStackTrace(System.err);
        }
        
        return sb.toString();
    }

    /** Loads the contents of a file into a string, <i>without preserving newlines</i>. 
     *@param sFilename The filename of the file to load.
     *@return A String containing the contents of the given file.
     */
    public static String loadFileToString(String sFilename, int iMaxLen) {
        StringBuffer sb = new StringBuffer();
        try {
           BufferedReader in = new BufferedReader(new FileReader(sFilename));
           String line;
           while (((line = in.readLine()) != null) && 
                   (sb.length() + line.length() < iMaxLen)) {
            sb.append(line);
           }
           in.close();
        } catch (Exception e) {
            System.err.println("Coult not load file:" + sFilename);
            e.printStackTrace(System.err);
        }
        
        return sb.toString();
    }
    
    /** Loads the contents of a file into a string, preserving newlines. 
     *@param sFilename The filename of the file to load.
     *@return A String containing the contents of the given file.
     */
    public static String loadFileToStringWithNewlines(String sFilename) {
        StringBuffer sb = new StringBuffer();
        try {
           BufferedReader in = new BufferedReader(new FileReader(sFilename));
           String line;
           while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
           }
           in.close();
        } catch (Exception e) {
            System.err.println("Coult not load file:" + sFilename);
            e.printStackTrace(System.err);
        }
        
        return sb.toString();
    }
    
    /** Loads the contents of a set of files into a string, by calling repeatedly
     * <code>loadFileToString</code>. Each file is separated from another by a 
     * zero character (char(0)).
     *@param ssFiles The set of string filenames to load.
     *@return A String containing the concatenation of the contents of the 
     * given files.
     */
    public static String loadFileSetToString(Set<String> ssFiles) {
        StringBuffer sbRes = new StringBuffer();
        for (String sCurFile : ssFiles) {
            sbRes.append(loadFileToString(sCurFile)).append((char)0);
        }
        
        return sbRes.toString();
    }
    
    /** Loads the contents of a set of files into a string, by calling repeatedly
     * the <code>loadFile</code> function of a {@link IFileLoader}. 
     * Each file is separated from another by a zero character (char(0)).
     *@param ssFiles The set of string filenames to load.
     *@param lLoader The loader to use for loading the files
     *@return A String containing the concatenation of the contents of the 
     * given files.
     */
    public static String loadFileSetToString(Set<String> ssFiles, 
            IFileLoader<String> lLoader) {
        StringBuffer sbRes = new StringBuffer();
        for (String sCurFile : ssFiles) {
            sbRes.append(lLoader.loadFile(sCurFile)).append((char)0);
        }
        
        return sbRes.toString();
    }
    
    /**Repeats a given string a specified number of times.
     *@param sStr The string to repeat.
     *@param iTimes The times to repeat the string.
     *@return A string containing the given string concatenated the specified number of times.
     */
    public static final String repeatString(String sStr, int iTimes) {
       StringBuffer sb = new StringBuffer();
       for (int iCnt=0; iCnt < iTimes; iCnt++)
           sb.append(sStr);
       
       return sb.toString();
    }
    
    /** Returns the factorial <pre>1*2*...*(n-1)*n</pre>.
     *@param n The highest number of the factorial.
     *@return The factorial.
     */
    public static final double factorial(int n) {
        return factorial(1, n);
    }
    
    /** Returns the factorial <pre>m*(m+1)*...*(n-1)*n</pre>.
     *@param m The lowest number of the factorial.
     *@param n The highest number of the factorial.
     *@return The factorial.
     */
    public static final double factorial(int m, int n) {
        if ((m < 0) || (n < 0))
            return Double.NaN;

        if (n == 0)
            return 1.0;
        
        double dRes = 1.0;
        for (int iCnt = m; iCnt <= n; iCnt++) {
            dRes *= iCnt;
        }
        
        return dRes;
    }
    
    public static String reverseString(String source) {
        int i, len = source.length();
        StringBuffer dest = new StringBuffer(len);

        for (i = (len - 1); i >= 0; i--)
          dest.append(source.charAt(i));
        return dest.toString();
    }

    /** Returns a reversed (by means of item index) version of a given list.
     *@param l The list to reverse.
     *@return The reversed list.
     */
    public static List reverseList(List l) {
        LinkedList lRes = new LinkedList();
        int iListSize = l.size();
        for (int iCnt=0; iCnt < iListSize; iCnt++) {
            lRes.add(l.get(iListSize - iCnt - 1));
        }
        
        return lRes;
    }

    /** Returns the portion of filename after the last directory separator. If there is no file name there, an empty string
     * is returned.
     *@param sFilepath The path to the file.
     *@return The filename stripped of directories.
     */
    public static final String getFilenameOnly(String sFilepath) {
        return new File(sFilepath).getName();
    }
    
    /** Returns the index of the constructor of a class, given its 
     * parameter count.
     *@param sClassName The class name of interest.
     *@param iParams The parameter count to look for.
     *@return The index of the constructor in the 
     *Class.forName(sClassName).getConstructors() array.
     */
    public static final int getConstructor(String sClassName, int iParams) {
        int iCnt = -1;
        try {
            for (iCnt=0; iCnt < Class.forName(sClassName).getConstructors().length; iCnt++)
                if (Class.forName(sClassName).getConstructors()[iCnt].getParameterTypes().length == iParams)
                    return iCnt;
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
        return -1;
    }

    public static String extractText(String sWholeString) throws IOException{
    final ArrayList<String> list = new ArrayList<String>();
    StringReader reader = new StringReader(sWholeString);
    
    ParserDelegator parserDelegator = new ParserDelegator();
    ParserCallback parserCallback = new ParserCallback() {
      @Override
      public void handleText(final char[] data, final int pos) {
        list.add(new String(data));
      }
      @Override
      public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos) { }
      @Override
      public void handleEndTag(Tag t, final int pos) {  }
      @Override
      public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) { }
      @Override
      public void handleComment(final char[] data, final int pos) { }
      @Override
      public void handleError(final java.lang.String errMsg, final int pos) { }
    };
    parserDelegator.parse(reader, parserCallback, true);

    StringBuffer sbRes = new StringBuffer();
    for (String sLine:list)
        sbRes.append(sLine+"\n");
    return sbRes.toString();
  }

    /**
     * Returns a string based on a constant change between
     * vowels and consonants and blanks. Considered "normal".
     * @return The random "normal" string.
     */
    public static String getNormalString() {
            // Both the set of vowels and consonants also include
            // black characters to allow for space in the string.
            String sVowels = "aeiuoy ";
            String sConsonants = "qwrtpsdf jklhzxcvbnm ";
            StringBuffer sbRes = new StringBuffer();
            int iLen = (int)(7.0 +
                            (3.0 * new Random().nextGaussian()));

            // Randomly initialize to vowel or consonant
            boolean bVowel = new Random().nextBoolean();
            for (int iCharCnt=0; iCharCnt<iLen; iCharCnt++) {
                    int iStart;
                    if (bVowel) {
                            iStart = Math.abs(new Random().nextInt()) % (sVowels.length() - 1);
                            sbRes.append(sVowels.substring(iStart, iStart + 1));
                    }
                    else {
                            iStart = Math.abs(new Random().nextInt()) % (sConsonants.length() - 1);
                            sbRes.append(sConsonants.substring(iStart, iStart + 1));
                    }
                    bVowel = !bVowel;
            }
            return sbRes.toString();
    }

    /**
     * Returns a random string, based on random selection from
     * an alphabet of characters and symbols.
     * @return The random string.
     */
    public static String getAbnormalString() {
            String sAlphabet = "aeiuoy qwrtpsdfjklhzxcvbnm1234567890!@#";
            StringBuffer sbRes = new StringBuffer();
            int iLen = (int)(12.0 +
                            (11.0 * new Random().nextGaussian()));

            // Randomly generate from alphabet
            for (int iCharCnt=0; iCharCnt<iLen; iCharCnt++) {
                    int iStart;
                    iStart = Math.abs(new Random().nextInt()) % (sAlphabet.length() - 1);
                    sbRes.append(sAlphabet.substring(iStart, iStart + 1));
            }
            return sbRes.toString();
    }

}

/** A default {@link WindowAdapter} class, terminating the application according to EXIT_ON_CLOSE.
 */
class WindowDefaultAdapter extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
        ((JFrame)e.getComponent()).setVisible(false);
        ((JFrame)e.getComponent()).dispose();
    }
    
    @Override
    public void windowClosed(WindowEvent e) {
        if (JFrame.EXIT_ON_CLOSE > 0)
            System.exit(0);
    }

}
