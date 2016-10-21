/*
 * DistributionWordDocument.java
 *
 * Created on October 22, 2007, 5:04 PM
 *
 */

package gr.demokritos.iit.conceptualIndex.documentModel;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import java.util.Iterator;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.VertexImpl;

/**
 *
 * @author ggianna
 */
public class DistributionWordDocument extends DistributionDocument {
    
    /** Creates a new instance of DistributionWordDocument. The source n-gram size is set to the default
     * value of 1.
     *@param iNeighbourhoodWindow The size of the window indicative of neighbourhood between a
     *source n-gram and a given token.
     */
    public DistributionWordDocument(int iNeighbourhoodWindow) {
        super(iNeighbourhoodWindow);
    }
    
    /** Creates a new instance of DistributionWordDocument.
     *@param iNeighbourhoodWindow The size of the window indicative of neighbourhood between a
     *source n-gram and a given token.
     *@param iSourceNGramSize The size of the source n-grams in character length.
     */
    public DistributionWordDocument(int iNeighbourhoodWindow, int iSourceNGramSize) {
        super(iNeighbourhoodWindow, iSourceNGramSize);
    }
    
    /** Creates and saves the graph representation of a string, using <i>word n-grams</i> of selected size 
     * as source nodes and <i>word n-grams</i> of size 1 (words) as destination nodes.
     *@param sDataString The data string to analyse and represent as a distribution graph.
     *@param iNGramSize The size of the n-grams used as source nodes.
     *@param clearCurrentData Indicates whether the new data replace existing data. If this parameter
     *is set to false, then the new data is appended to existing data.
     */
    public void setDataString(String sDataString, int iNGramSize, boolean clearCurrentData) {
        // Clear data if required
        if (clearCurrentData)
        {
            clearDocumentGraph();
            //hNGrams.clear();
        }
        
        
        // TODO: Use text preprocessor
        String sUsableString = new String(sDataString);
        String[]  saWords = gr.demokritos.iit.jinsect.utils.splitToWords(sDataString);
        int iLen = saWords.length;
        
        // If n-gram not bigger than text
        if (iNGramSize < iLen)
        {
            // The String has a size of at least [iNGramSize]
            String sCurNGram = "";
            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
            {
                // Do not exceed string length
                if (iCurStart + iNGramSize > iLen)
                    continue;
                
                // Get n-gram
                sCurNGram = getSubstringFromStringArray(saWords, iCurStart, iCurStart + iNGramSize - 1);
                
                for (int iCurNeighbour = 0; 
                    (iCurNeighbour < NeighbourhoodWindow) && (iCurStart + iNGramSize + iCurNeighbour < iLen);
                    iCurNeighbour++)
                {
                    String sNeighbour = getSubstringFromStringArray(saWords, iCurStart + iNGramSize + iCurNeighbour,
                            iCurStart + iNGramSize + iCurNeighbour);
                    try {
                        Edge e = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(Graph, sCurNGram, sNeighbour);
                        if (e == null)
                            e = Graph.addEdge(new VertexImpl(sCurNGram), new VertexImpl(sNeighbour));
                        Distribution d = (Graph.getEdgeDistro(e) == null) ? 
                            new Distribution() : Graph.getEdgeDistro(e);
                        d.setValue(iCurNeighbour + 1, d.getValue(iCurNeighbour + 1) + 1.0);
                        
                        // Add word
                        //hNGrams.put(sCurNGram + sNeighbour, 1);
                        
                        Graph.setEdgeDistro(e, d);
                    }
                    catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }            
        }
        
        if (clearCurrentData)
            DataString = sDataString;
        else
            DataString += sDataString;
    }
       
    private final String getSubstringFromStringArray(String[] sSrc, int iFrom, int iToIncluding) {
        StringBuffer sbCur = new StringBuffer();
        for (int iNGramPartCnt=iFrom; iNGramPartCnt <= iToIncluding; iNGramPartCnt++) {
            sbCur.append(sSrc[iNGramPartCnt]);
        }
        return sbCur.toString();
    }

    /** Calculates a degree of normality, indicating whether a given string appears in a form
     * similar to text in the document. The process actually compares distributions. These 
     * distributions appear in same edges of the graph representations of the DistributionDocument
     * object, and another DistributionDocument, created by use of the given string.
     * If the public variable <code>OnCompare</code> has been set it is used to compare the distributions.
     *@see Distribution
     */
    public double normality(String s) {
        DistributionWordDocument dDoc = new DistributionWordDocument(NeighbourhoodWindow,SourceNGramSize);
        dDoc.setDataString(s, SourceNGramSize, true);
        EdgeCachedLocator eclLocator = new EdgeCachedLocator(20);
        
        double dRes = 0.0; // Normality
        int iCnt = 0;
        try {
            Iterator iIter = dDoc.Graph.getEdgeSet().iterator();
            while (iIter.hasNext()){
                Edge eCur = (Edge)iIter.next();
                Distribution dCur = dDoc.Graph.getEdgeDistro(eCur);
                
                Edge e = eclLocator.locateEdgeInGraph(Graph, eCur.getVertexA(), eCur.getVertexB());
                if (e != null) // If the edge was found then
                {
                    Distribution d = (Graph.getEdgeDistro(e) == null) ? 
                        new Distribution() : Graph.getEdgeDistro(e);
                    
                    if (OnCompare != null)
                        dRes += OnCompare.compareDistributions(dCur, d);
                    else
                        dRes += dCur.similarityTo(d);                    
                }
                //else ignore
                
                iCnt ++;
            }
        }
        catch (Exception exc) {
            exc.printStackTrace();
            return 0.0; // Error
        }
        return dRes / (iCnt == 0 ? 1 : iCnt); // Return average
    }
    
    public static void main(String[] sArgs) {
        DistributionWordDocument dTest = new DistributionWordDocument(2, 2);
        dTest.setDataString("A big big test.", 2, true);
        System.out.println(dTest.toString());
        System.out.println(dTest.normality("This is a big test. Is it not. A big big test..."));
        System.out.println(dTest.normality("This is. A test. A big big test"));
        System.out.println(dTest.normality("This is. A test. A big big test..."));
        
    }
}
