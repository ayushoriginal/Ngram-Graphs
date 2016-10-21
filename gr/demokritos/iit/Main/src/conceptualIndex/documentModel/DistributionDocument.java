/*
 * ConceptualDocument.java
 *
 * Created on 25 ?????????????? 2006, 3:20 pm
 *
 */

package gr.demokritos.iit.conceptualIndex.documentModel;

import gr.demokritos.iit.conceptualIndex.events.IDistributionComparisonListener;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.conceptualIndex.structs.DistributionGraph;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.utils;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.VertexImpl;

/** Represents a document, described as a graph of distributions. Each distribution indicates the
 * probability of a token (character) to appear after a given n-gram, indicated as <i>source</i>.
 * Allows input and output operations
 * and can function as grammar indicator, to determine normality of other texts.
 *
 * @author ggianna
 */
public class DistributionDocument  implements Serializable {
    
    /* Instance Variables */
    
    /** The Graph representing the document
     *@see DistributionGraph
     */
    protected DistributionGraph Graph;
    /** The string corresponding to input texts directly.
     */
    protected String DataString = "";
    
    /** The size of the window indicating neighbourhood. I.e. if two tokens are positioned more than
     * <code>NeighbourhoodWindow</code>, then they are not considered neighbours.
     */
    int NeighbourhoodWindow = 4;
    /** The size of the source n-grams (i.e. n-grams preceding a given token).
     */
    int SourceNGramSize = 1;
    
    /** An event, used to attach a comparator of distributions to this class. The comparator is 
     * used in the <code>normality</code> function.
     */
    public IDistributionComparisonListener OnCompare = null;
    
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
        out.writeObject(Graph);
        out.writeObject(DataString);
        out.writeInt(NeighbourhoodWindow);
        out.writeInt(SourceNGramSize);
    }

    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
        Graph = (DistributionGraph)in.readObject();
        DataString = (String)in.readObject();
        NeighbourhoodWindow = in.readInt();
        SourceNGramSize = in.readInt();
    }
    
    /** Creates a new instance of DistributionDocument. The source n-gram size is set to the default
     * value of 1.
     *@param iNeighbourhoodWindow The size of the window indicative of neighbourhood between a
     *source n-gram and a given token.
     */
    public DistributionDocument(int iNeighbourhoodWindow) {
        Graph = new DistributionGraph();
        NeighbourhoodWindow = iNeighbourhoodWindow;
        //hNGrams = new HashMap();
    }
    
    /** Creates a new instance of DistributionDocument.
     *@param iNeighbourhoodWindow The size of the window indicative of neighbourhood between a
     *source n-gram and a given token.
     *@param iSourceNGramSize The size of the source n-grams in character length.
     */
    public DistributionDocument(int iNeighbourhoodWindow, int iSourceNGramSize) {
        SourceNGramSize = iSourceNGramSize;
        Graph = new DistributionGraph();
        NeighbourhoodWindow = iNeighbourhoodWindow;
        //hNGrams = new HashMap();
    }
    
    /** Clears the document graph, resetting the representation. */
    public void clearDocumentGraph() {
        Graph = new DistributionGraph();
    }
    
    /** Sets the document graph to a selected existing graph.
     *@param dgNew The distribution graph to replace the existing one.
     *@see DistributionGraph
     */
    public void setDocumentGraph(DistributionGraph dgNew) {
        Graph = dgNew;
    }

    /**Calculates the size of the full document object, by getting the edge count of the
     * corresponding graph and <i>not</i> the datastring (i.e. text) size.
     *@return The size of the document object, based on edge count.
     ***/
    public int length() {
        return Graph.getEdgesCount();
    }
    
    /** Loads the contents of a file as the datastring.
     *@param sFilename The filename of the input file.
     *@param clearCurrentData Indicates whether the new file replaces existing text. If this parameter
     *is set to false, then the new file is appended to existing text.
     */
    public void loadDataStringFromFile(String sFilename, boolean clearCurrentData) {
        loadDataStringFromFile(sFilename, clearCurrentData, utils.getSystemEncoding());
    }
    
    /** Loads the contents of a file as the datastring.
     *@param sFilename The filename of the input file.
     *@param clearCurrentData Indicates whether the new file replaces existing text. If this parameter
     *is set to false, then the new file is appended to existing text.
     *@param sEncoding The encoding of the input file.
     */
    public void loadDataStringFromFile(String sFilename, boolean clearCurrentData, String sEncoding) {
        try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(sFilename);
            //InputStreamReader isrIn = new InputStreamReader(fiIn, sEncoding);
            int iData = 0;
            while ((iData = fiIn.read()) > -1)
                bsOut.write(iData);
            String sDataString = bsOut.toString();
            //fiIn.close();
            setDataString(sDataString, SourceNGramSize, clearCurrentData); // Actually update
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            setDataString("",1, false);
        }
    }
    
    /** Creates and saves the graph representation of a string, using substrings of selected size
     * as source nodes and substrings of size 1 (letters) as destination nodes.
     *@param sDataString The data string to analyse and represent as a distribution graph.
     *@param iNGramSize The size of the n-grams used as source nodes.
     *@param clearCurrentData Indicates whether the new data replace existing data. If this parameter
     *is set to false, then the new data is appended to existing data.
     */
    public void setDataString(String sDataString, boolean clearCurrentData) {
        setDataString(sDataString, SourceNGramSize, clearCurrentData);
    }
    /** Creates and saves the graph representation of a string, using substrings of selected size 
     * as source nodes and substrings of size 1 (letters) as destination nodes.
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
        
        int iLen = sDataString.length();
        
        // If n-gram not bigger than text
        if (iNGramSize < iLen)
        {
            // TODO: Use text preprocessor
            String sUsableString = new String(sDataString);

            // The String has a size of at least [iNGramSize]
            String sCurNGram = "";
            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
            {
                // Do not exceed string length
                if (iCurStart + iNGramSize > iLen)
                    continue;
                
                // Get n-gram                
                sCurNGram = sUsableString.substring(iCurStart, iCurStart + iNGramSize);
                for (int iCurNeighbour = 0; 
                    (iCurNeighbour < NeighbourhoodWindow) && (iCurStart + iNGramSize + iCurNeighbour < iLen);
                    iCurNeighbour++)
                {
                    String sNeighbour = sUsableString.substring(iCurStart + iNGramSize + iCurNeighbour,
                            iCurStart + iNGramSize + iCurNeighbour + 1);
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
    
    /** Returns the current data string (i.e. text representation) of the document.
     *@return The data string.
     */
    public String getDataString() {
        return DataString;
    }
    
    /** TODO: Document
     */
    public void mergeWith(DistributionDocument tpData, double fLearningRate) {
        // TODO: IMPLEMENT
        //Graph.mergeGraph(tpData.getDocumentGraph(), fLearningRate);
    }
    
    /** TODO: Document
     */
    public void prune(double dMinCoexistenceImportance) {
        // TODO: IMPLEMENT
        // Prune graph
        // Graph.prune(dMinCoexistenceImportance);
    }

    /** Returns a string representation of the document graph.
     */
    public String toString() {
        return Graph.toString();
    }
    
    /** Calculates a degree of normality, indicating whether a given string appears in a form
     * similar to text in the document. The process actually compares distributions. These 
     * distributions appear in same edges of the graph representations of the DistributionDocument
     * object, and another DistributionDocument, created by use of the given string.
     * If the public variable <code>OnCompare</code> has been set it is used to compare the distributions.
     *@see Distribution
     */
    public double normality(String s) {
        DistributionDocument dDoc = new DistributionDocument(NeighbourhoodWindow,SourceNGramSize);
        dDoc.setDataString(s, SourceNGramSize, true);
        
        double dRes = 0.0; // Normality
        int iCnt = 0;
        try {
            Iterator iIter = dDoc.Graph.getEdgeSet().iterator();
            while (iIter.hasNext()){
                Edge eCur = (Edge)iIter.next();
                Distribution dCur = dDoc.Graph.getEdgeDistro(eCur);
                
                Edge e = gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(Graph, eCur.getVertexA(), eCur.getVertexB());
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
        DistributionDocument dTest = new DistributionDocument(1, 1);
        dTest.setDataString("A big big test.", 1, true);
        System.out.println(dTest.toString());
        System.out.println(dTest.normality("test."));
        System.out.println(dTest.normality("This is a test."));

        dTest = new DistributionDocument(2, 2);
        dTest.setDataString("A small test.", 2, true);
        System.out.println(dTest.toString());
        System.out.println(dTest.normality("This is a small test. Indeed."));        
    }
}
