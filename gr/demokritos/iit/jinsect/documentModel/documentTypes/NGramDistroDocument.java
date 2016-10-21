/*
 * NGramDistroDocument.java
 *
 * Created on 12 Φεβρουάριος 2007, 3:07 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ITextPrint;
import java.io.IOException;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramDistroGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;

/** An {@link NGramDocument} the n-gram graph representation of which includes 
 * distributional information for n-gram neighbourhoods (instead of the single numbers of the
 * NGramDocument class). This type of representation offers more information and is, thus, more
 * expressive.
 *
 * @author ggianna
 */
public class NGramDistroDocument extends NGramDocument {    
    
    private DocumentNGramDistroGraph Graph;
    private DocumentNGramHistogram Histogram;
    
    /** Creates a new instance of NGramDistroDocument. */
    public NGramDistroDocument() {
        Graph = new DocumentNGramDistroGraph();
        Histogram = new DocumentNGramHistogram();
    }
    
    /** Creates a new instance of NGramDistroDocument, given the parameters for the underlying
     * histogram and graph representation. 
     *@param iMinGraphSize The minimum n-gram rank (size) in the document graph.
     *@param iMaxGraphSize The maximum n-gram rank (size) in the document graph.
     *@param iGraphCorrelationWindow The neighbourhood distance used in the document graph.
     *@param iMinHistogramSize The minimum n-gram rank (size) in the document histogram.
     *@param iMaxHistogramSize The maximum n-gram rank (size) in the document histogram.
     */
    public NGramDistroDocument(int iMinGraphSize, int iMaxGraphSize, int iGraphCorrelationWindow,
            int iMinHistogramSize, int iMaxHistogramSize) {
        Graph = new DocumentNGramDistroGraph(iMinGraphSize, iMaxGraphSize, iGraphCorrelationWindow);
        Histogram = new DocumentNGramHistogram(iMinHistogramSize, iMaxHistogramSize);
    }
    
    
    public DocumentNGramHistogram getDocumentHistogram() {
        return Histogram;
    }
    
    public void setDocumentHistogram(DocumentNGramHistogram idnNew) {
        Histogram = idnNew;
    }    

    public DocumentNGramDistroGraph getDocumentGraph() {
        return Graph;
    }
    
    public void setDocumentGraph(DocumentNGramDistroGraph idgNew) {
        Graph = idgNew;
    }

    /***
     *Returns the size of the full Document Object, by summing the Graph and
     *Histogram sizes of the document.
     *@return The requested size.
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
    
    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        Graph.setDataString(sDataString);        
    }
    
    public String getDataString() {
        return Histogram.getDataString();
    }
    
    public void mergeWith(ITextPrint tpData, double fLearningRate) {
        Histogram.mergeHistogram(tpData.getDocumentHistogram(), fLearningRate);
        Graph.mergeGraph(tpData.getDocumentGraph(), fLearningRate);
    }
    
    public void prune(double dMinCoexistenceImportance) {
        // Prune graph
        Graph.prune(dMinCoexistenceImportance);
    }
    
}
