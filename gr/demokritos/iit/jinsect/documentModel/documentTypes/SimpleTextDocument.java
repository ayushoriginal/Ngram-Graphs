/*
 * INSECTWordDocument.java
 *
 * Created on 31 Ιανουάριος 2006, 4:35 μμ
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ITextPrint;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordHistogram;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.utils;

/**
 *
 * @author ggianna
 */
public class SimpleTextDocument extends NGramDocument {
    protected DocumentWordGraph Graph;
    protected DocumentWordHistogram Histogram;
    protected String DataString;
    
    /** Creates a new instance of INSECTWordDocument */
    public SimpleTextDocument() {
        Graph = new DocumentWordGraph();
        Histogram = new DocumentWordHistogram();
    }
    
    public SimpleTextDocument(int iMinNGram, int iMaxNGram, int iDistance) {
        Graph = new DocumentWordGraph(iMinNGram, iMaxNGram, iDistance);
        Histogram = new DocumentWordHistogram(iMinNGram, iMaxNGram);
    }
    
    @Override
    public DocumentNGramHistogram getDocumentHistogram() {
        return Histogram;
    }
    public void setDocumentHistogram(DocumentWordHistogram idnNew) {
        Histogram = idnNew;
    }    

    @Override
    public DocumentNGramGraph getDocumentGraph() {
        return Graph;
    }
    
    public void setDocumentGraph(DocumentWordGraph idgNew) {
        Graph = idgNew;
    }

    /***
     *Returns the size of the full Document Object, by summing the Graph and
     *Histogram sizes of the document.
     ***/
    @Override
    public int length() {
        return Histogram.length() + Graph.length();
    }
    
    // Temporary datastring
    public void setTempDataString(String sDataString) {
        DataString = sDataString;
    }
    public void applyTempDataString() {
        if (DataString != null)
            setDataString(DataString);
    }
    
    public String getTempDataString() {
        return DataString;
    }
    
    public void loadTempDataStringFromFile(String sFilename) {
//            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
//            FileInputStream fiIn = new FileInputStream(sFilename);
//            int iData = 0;
//            while ((iData = fiIn.read()) > -1)
//                bsOut.write(iData);
//            String sDataString = bsOut.toString();
//            fiIn.close();
//            DataString = sDataString; // Actually update temp datastring
        DataString = utils.loadFileToStringWithNewlines(sFilename);
    }
    
    // Actual datastring
    @Override
    public void loadDataStringFromFile(String sFilename) {
        try {
            Histogram.loadDataStringFromFile(sFilename);
            Graph.loadDataStringFromFile(sFilename);
        }
        catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            Histogram.setDataString("");
            Graph.setDataString("");
        }
    }
    
    @Override
    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        Graph.setDataString(sDataString);        
    }
    
    @Override
    public String getDataString() {
        return Histogram.getDataString();
    }
    
    @Override
    public void mergeWith(ITextPrint tpData,double fLearningRate) {
        Histogram.mergeHistogram(tpData.getDocumentHistogram(), fLearningRate);
        Graph.mergeGraph(tpData.getDocumentGraph(), fLearningRate);
    }
    
    @Override
    public void prune(double dMinCoexistenceImportance) {
        // Prune graph
        Graph.prune(dMinCoexistenceImportance);
    }
    
    public void prune(double dMinCoexistenceImportance, NotificationListener nlDeletionListener) {
        // Prune graph
        Graph.DeletionNotificationListener = nlDeletionListener;
        Graph.prune(dMinCoexistenceImportance);
        Graph.DeletionNotificationListener = null;
    }
}
