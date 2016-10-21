/*
 * INSECTWordCategory.java
 *
 * Created on 31 Ιανουάριος 2006, 11:56 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ITextPrint;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.events.NotificationAdapter;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.structs.Dictionary;

/**
 *
 * @author PCKid
 */
public class SimpleTextCategory extends SimpleTextDocument implements WordEvaluatorListener {
    public Dictionary Dictionary;
    public String Name = "";
    protected int DocumentCount;
    /** Creates a new instance of INSECTWordCategory */
    public SimpleTextCategory(String sName) {
        super();
        // Init dictionary
        Dictionary = new Dictionary(sName, 30); // Max word size set to 30
        Dictionary.AddSubStrings = false;
        Dictionary.RemoveSubStrings = false;
        DocumentCount = 0;
        
        Name = sName;
    }
    
    /***Modifies the category so as to reject documents as [oOperand].
     ***/
    public void rejectDocument(NGramDocument dDoc) {
        // Affect structs
        DocumentNGramGraph gGraph = 
                dDoc.getDocumentGraph().intersectGraph(getDocumentGraph());
        DocumentNGramHistogram hHistogram = 
                dDoc.getDocumentHistogram().intersectHistogram(getDocumentHistogram());
        
        getDocumentGraph().degrade(gGraph);
    }
    
    @Override
    public void setDataString(String sDataString) {
        super.setDataString(sDataString);
        
        // Reset and update dictionary
        Dictionary.clear();
        for (Iterator iIter = getDocumentHistogram().NGramHistogram.keySet().iterator(); iIter.hasNext();) {
            String sWord = (String)iIter.next();
            Dictionary.addWord(sWord);
        }        
        DocumentCount = 1;
    }
    
    public void mergeWith(ITextPrint tpData) {
        mergeWith(tpData, DocumentCount == 0 ? 1.0 :
            (DocumentCount / DocumentCount + 1));
    }
    
    @Override
    public void mergeWith(ITextPrint tpData, double fLearningRate) {
        super.mergeWith(tpData, fLearningRate);
        
        // Reset and update dictionary
        Dictionary.clear();
        for (Iterator iIter = getDocumentHistogram().NGramHistogram.keySet().iterator(); iIter.hasNext();) {
            String sWord = (String)iIter.next();
            Dictionary.addWord(sWord);
        }        
        DocumentCount++;
    }
    
    @Override
    public final boolean evaluateWord(String sWord) {
        return Dictionary.contains(sWord.trim());
    }
    
    public String evaluateText(String[] asWords) {
        ListIterator liIter = Arrays.asList(asWords).listIterator();
        String sRes = "";
        while (liIter.hasNext()) {
            String sCur = (String)liIter.next();
            if (Dictionary.contains(sCur.trim()))
                sRes += " " + sCur;
        }
        return sRes.trim();
    }
    
    @Override
    public void prune(double dMinCoexistenceImportance) {
        super.prune(dMinCoexistenceImportance, new NotificationAdapter() {
            @Override
            public void Notify(Object sSender, Object sParams) {
                Dictionary.removeWord((String)sParams);
            }
        });
    }
    
    @Override
    public int length() {
        return super.length() + Dictionary.length();
    }
}

