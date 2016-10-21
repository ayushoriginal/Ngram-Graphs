/*
 * SimpleTextHistoDocument.java
 *
 * Created on September 18, 2007, 4:50 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordHistogram;

/**
 *
 * @author ggianna
 */
public class SimpleTextHistoDocument extends SimpleTextDocument {
    
    /** Creates a new instance of SimpleTextHistoDocument */
    public SimpleTextHistoDocument() {
        Graph = new DocumentWordGraph();
        Histogram = new DocumentWordHistogram();
    }
    
    public SimpleTextHistoDocument(int iMinNGram, int iMaxNGram, int iDistance) {
        Graph = new DocumentWordGraph(iMinNGram, iMaxNGram, iDistance);
        Histogram = new DocumentWordHistogram(iMinNGram, iMaxNGram);
    }
    
    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        // Graph.setDataString(sDataString);        
    }
}
