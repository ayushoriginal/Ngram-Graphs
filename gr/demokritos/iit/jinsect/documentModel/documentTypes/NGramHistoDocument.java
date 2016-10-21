/*
 * NGramHistoDocument.java
 *
 * Created on September 13, 2007, 1:02 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import java.io.IOException;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;

/** This class is identical to the {@link NGramDocument} class, except that the graph aspect of the document
 * is ignored: no data is loaded, and the <code>setDataString</code> method does not affect the graph.
 *
 * @author ggianna
 */
public class NGramHistoDocument extends NGramDocument {
    
    /** Creates a new instance of NGramHistoDocument */
    public NGramHistoDocument() {
        Graph = new DocumentNGramGraph();
        Histogram = new DocumentNGramHistogram();
    }
    
    /** Creates a new instance of NGramGaussNormSymWinDocument, given the parameters for the underlying
     * histogram and graph representations.
     *@param iMinGraphSize The minimum n-gram rank (size) in the document graph (Ignored).
     *@param iMaxGraphSize The maximum n-gram rank (size) in the document graph (Ignored).
     *@param iGraphCorrelationWindow The neighbourhood distance used in the document graph (Ignored).
     *@param iMinHistogramSize The minimum n-gram rank (size) in the document histogram.
     *@param iMaxHistogramSize The maximum n-gram rank (size) in the document histogram.
     *@see DocumentNGramGraph
     *@see DocumentNGramHistogram
     */
    public NGramHistoDocument(int iMinGraphSize, int iMaxGraphSize, int iGraphCorrelationWindow,
            int iMinHistogramSize, int iMaxHistogramSize) {
        Graph = new DocumentNGramGraph(iMinGraphSize, iMaxGraphSize, iGraphCorrelationWindow);
        Histogram = new DocumentNGramHistogram(iMinHistogramSize, iMaxHistogramSize);
    }
    
    /** Uses the given file to load the initial text to represent. Ignores the document graph. 
     *@param sFilename The file to use.
     */
    public void loadDataStringFromFile(String sFilename) {
        try {
            Histogram.loadDataStringFromFile(sFilename);
            // Graph.loadDataStringFromFile(sFilename);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            Histogram.setDataString("");
            Graph.setDataString("");
        }
    }
    
    /** Sets the initial text to represent. Ignores the document graph. 
     *@param sDataString The text to represent.
     */
    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        // Graph.setDataString(sDataString);        
    }
    
}
