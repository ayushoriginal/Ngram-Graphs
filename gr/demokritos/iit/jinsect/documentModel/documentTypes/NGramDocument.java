/*
 * INSECTDocument.java
 *
 * Created on 24 Ιανουάριος 2006, 10:34 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ILoadableTextPrint;
import gr.demokritos.iit.jinsect.documentModel.ITextPrint;
import java.io.IOException;
import java.io.Serializable;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;

/** A dual representation of a document, using both a histogram and an n-gram graph representation
 * for a given text.
 *
 * @author PCKid
 */
public class NGramDocument implements ILoadableTextPrint, Serializable {
    protected DocumentNGramGraph Graph;
    protected DocumentNGramHistogram Histogram;
    
    /** Creates a new instance of INSECTDocument. */
    public NGramDocument() {
        Graph = new DocumentNGramGraph();
        Histogram = new DocumentNGramHistogram();
    }
    
    /** Creates a new instance of INSECTDocument, with specific parameters for the histogram and
     * n-gram graph representation. 
     *@param iMinGraphSize The minimum n-gram rank (size) in the document graph.
     *@param iMaxGraphSize The maximum n-gram rank (size) in the document graph.
     *@param iGraphCorrelationWindow The neighbourhood distance used in the document graph.
     *@param iMinHistogramSize The minimum n-gram rank (size) in the document histogram.
     *@param iMaxHistogramSize The maximum n-gram rank (size) in the document histogram.
     *@see DocumentNGramGraph
     *@see DocumentNGramHistogram
     */
    public NGramDocument(int iMinGraphSize, int iMaxGraphSize, int iGraphCorrelationWindow,
            int iMinHistogramSize, int iMaxHistogramSize) {
        Graph = new DocumentNGramGraph(iMinGraphSize, iMaxGraphSize, iGraphCorrelationWindow);
        Histogram = new DocumentNGramHistogram(iMinHistogramSize, iMaxHistogramSize);
    }
    
    
    public DocumentNGramHistogram getDocumentHistogram() {
        return Histogram;
    }
    public void setDocumentHistogram(DocumentNGramHistogram idnNew) {
        Histogram = idnNew;
    }    

    public DocumentNGramGraph getDocumentGraph() {
        return Graph;
    }
    
    public void setDocumentGraph(DocumentNGramGraph idgNew) {
        Graph = idgNew;
    }

    /***
     *Returns the size of the full Document Object, by summing the Graph and
     *Histogram sizes of the document.
     ***/
    public int length() {
        return Histogram.length() + Graph.length();
    }
    
    public void loadDataStringFromFile(String sFilename) {
        try {
            Histogram.loadDataStringFromFile(sFilename);
            Graph.loadDataStringFromFile(sFilename);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            Histogram.setDataString("");
            Graph.setDataString("");
        }
    }
    
    /** Creates the representation of a given string (text) and uses it to describe the document.
     *@param sDataString The given text.
     */
    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        Graph.setDataString(sDataString);        
    }
    
    /** Returns the initial text, the representation of which is described by this object.
     *@return The initial text.
     */
    public String getDataString() {
        return Histogram.getDataString();
    }
    
    /** Merges the histogram and the graph of the object with the corresponding
     * representations of another {@link ITextPrint} object, given a learning rate.
     *@param tpData A text representation object.
     *@param fLearningRate A number between 0.0 and 1.0 the effect of merging the given text to the current object
     * representation. A number of 1.0 means that every node of the given document will be directly considered to be
     * a negative example, and all previous learning will be rejected. 
     * A value of zero means that the example will not be taken into account. A value of 0.5 indicates that the 
     * sample should be taken as a negative example and modify existing learnt values in an averaging manner.
     */
    public void mergeWith(ITextPrint tpData, double fLearningRate) {
        Histogram.mergeHistogram(tpData.getDocumentHistogram(), fLearningRate);
        Graph.mergeGraph(tpData.getDocumentGraph(), fLearningRate);
    }
    
    /** Prunes the n-gram representation, given a minimum co-existence importance.
     *@param dMinCoexistenceImportance The minimum importance of nodes to be kept.
     */
    public void prune(double dMinCoexistenceImportance) {
        // Prune graph
        Graph.prune(dMinCoexistenceImportance);
    }
}
