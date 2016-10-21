/*
 * NGramSymWinDocument.java
 *
 * Created on June 13, 2007, 5:29 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import java.io.IOException;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;

/**
 *
 * @author ggianna
 */
public class NGramSymWinDocument extends NGramDocument {
    
    public NGramSymWinDocument() {
        Graph = new DocumentNGramSymWinGraph();
        Histogram = new DocumentNGramHistogram();
    }
    
    public NGramSymWinDocument(int iMinGraphSize, int iMaxGraphSize, int iGraphCorrelationWindow,
            int iMinHistogramSize, int iMaxHistogramSize) {
        Graph = new DocumentNGramSymWinGraph(iMinGraphSize, iMaxGraphSize, iGraphCorrelationWindow);
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
