/*
 * NGramGaussNormSymWinDocument.java
 *
 * Created on June 14, 2007, 5:50 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import java.io.IOException;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGaussNormSymWinGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;

/** An {@link NGramDocument}, which uses the Gauss-normalized approach on n-gram neighbourhood
 * calculations.
 *
 * @author ggianna
 */
public class NGramGaussNormSymWinDocument extends NGramDocument {
    /** Creates a new instance of NGramGaussNormSymWinDocument. */
    public NGramGaussNormSymWinDocument() {
        Graph = new DocumentNGramSymWinGraph();
        Histogram = new DocumentNGramHistogram();
    }
    
    /** Creates a new instance of NGramGaussNormSymWinDocument, given the parameters for the underlying
     * histogram and graph representations.
     *@param iMinGraphSize The minimum n-gram rank (size) in the document graph.
     *@param iMaxGraphSize The maximum n-gram rank (size) in the document graph.
     *@param iGraphCorrelationWindow The neighbourhood distance used in the document graph.
     *@param iMinHistogramSize The minimum n-gram rank (size) in the document histogram.
     *@param iMaxHistogramSize The maximum n-gram rank (size) in the document histogram.
     *@see DocumentNGramGraph
     *@see DocumentNGramHistogram
     */
    public NGramGaussNormSymWinDocument(int iMinGraphSize, int iMaxGraphSize, int iGraphCorrelationWindow,
            int iMinHistogramSize, int iMaxHistogramSize) {
        Graph = new DocumentNGramGaussNormSymWinGraph(iMinGraphSize, iMaxGraphSize, iGraphCorrelationWindow);
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
    
    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        Graph.setDataString(sDataString);        
    }
}
