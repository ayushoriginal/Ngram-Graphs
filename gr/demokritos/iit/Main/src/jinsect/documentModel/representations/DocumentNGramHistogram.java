/*
 * INSECTDocumentNGrams.java
 *
 * Created on 24 Ιανουάριος 2006, 10:34 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.documentModel.representations;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.CharBuffer;
import java.util.HashMap;
import gr.demokritos.iit.jinsect.events.TextPreprocessorListener;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.utils;
/**
 *
 * @author PCKid
 * Implements a structure holding the n-grams of a document as a histogram
 */
public class DocumentNGramHistogram implements Serializable {
    protected int MinSize;
    protected int MaxSize;
    protected  String DataString;
    public HashMap NGramHistogram;
    public WordEvaluatorListener WordEvaluator = null;
    public TextPreprocessorListener TextPreprocessor = null;
    
    /** The number of total n-grams of the analyzed text.
     */
    private int iTotalNGrams = -1; 
    /*** 
     * Creates a new instance of INSECTDocumentNGrams 
     *@param iMinSize The minimum n-gram size
     *@param iMaxSize The maximum n-gram size
     */
    public DocumentNGramHistogram(int iMinSize, int iMaxSize) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        DataString = "";
        NGramHistogram = new HashMap();                
    }
    
    /***
     * Creates a new instance of INSECTDocumentNGrams with n-gram sizes from
     * 3 to 5.*/
    public DocumentNGramHistogram() {
        MinSize = 3;
        MaxSize = 5;
        DataString = "";
        NGramHistogram = new HashMap();        
    }
    
    /***
     *Returns the size of the histogram (unique element count).
     */
    public int length() {
        return NGramHistogram.size();
    }
    
    /** Returns the number of total n-grams in the analyzed data string.
     *@return The number of total n-grams.
     */
    public int numberOfTotalNGrams() {
        if (iTotalNGrams != -1)
            return iTotalNGrams;
        
        createHistogram();
        return iTotalNGrams;
    }
    
    /***
     *Opens a text file and sets its contents as data string
     *@param sFilename The filename of the file to open.
     */
    public void loadDataStringFromFile(String sFilename) throws java.io.IOException,
            java.io.FileNotFoundException{
        String sDataString = utils.loadFileToStringWithNewlines(sFilename);
        setDataString(sDataString); // Actually update
    }
    
    /***
     *Creates the histogram of n-grams in the data string.
     *The WordEvaluatorListener (if present) is called for every n-gram
     *before the latter is added.
     */
    public void createHistogram(){
        String sDataString;
        iTotalNGrams = 0;
        // Do preprocessing
        if (TextPreprocessor == null)
            sDataString = this.DataString;
        else
            sDataString = TextPreprocessor.preprocess(DataString);
        
        int iLen = sDataString.length();
        
        // Set current n-gram size
        for (int iNGramSize=MinSize; iNGramSize <= MaxSize; iNGramSize++)
        {
            // Traverse the whole data string
            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
            {
                // If not enough letters, break
                if (iLen < iCurStart + iNGramSize)
                    break;
                
                // else attempt to add n-gram to histogram
                String sCurNGram = DataString.substring(iCurStart, iCurStart + iNGramSize);
                // Check for evaluator
                if (WordEvaluator != null)
                    // Evaluate
                    if (!WordEvaluator.evaluateWord(sCurNGram))
                        // If it dit not evaluate ignore
                        continue;
                
                if (this.NGramHistogram.containsKey(sCurNGram))
                {
                    double dPrev = ((Double)NGramHistogram.get(sCurNGram)).doubleValue();
                    // If it already exists, increase count
                    //NGramHistogram.put(sCurNGram, dPrev + 1.0 / iLen);
                    NGramHistogram.put(sCurNGram, dPrev + 1.0);
                }
                else
                    // else init
                    //NGramHistogram.put(sCurNGram, 1.0 / iLen);
                    NGramHistogram.put(sCurNGram, 1.0);
                
                iTotalNGrams++; // Increase number of encountered n-grams
            }
        }
        
    }
    
    /***
     * Merges the data of another histogram [dnOtherDocumentNGram] with this histogram data.
    * If an n-gram exists its weight is increased (modified) by 
    * [fNewDataImportance] * ([iNewWeight] - ExistingWeight)
    * else it is set to the new data value.        
    * @param fNewDataImportance Value of 1.0 means immediate change to new data value. 
    * 0.0 means no change. 0.5 means normal change towards new data.
     ***/
    public void mergeHistogram(DocumentNGramHistogram dnOtherDocumentNGram, 
            double fNewDataImportance) {
        java.util.Iterator iIter = dnOtherDocumentNGram.NGramHistogram.keySet().iterator();
        while (iIter.hasNext())
        {
            String sCurNGram = (String)iIter.next();
            if (this.NGramHistogram.containsKey(sCurNGram))
            {
                double dPrev = ((Double)NGramHistogram.get(sCurNGram)).doubleValue();
                double dNew = ((Double)dnOtherDocumentNGram.NGramHistogram.get(sCurNGram)).doubleValue();
                // Modify count
                NGramHistogram.put(sCurNGram, dPrev + (dNew - dPrev) * fNewDataImportance);
            }
            else
            {
                double dNew = ((Double)dnOtherDocumentNGram.NGramHistogram.get(sCurNGram)).doubleValue();
                // Add new n-gram
                NGramHistogram.put(sCurNGram, dNew);                
            }
        }
    }

    public DocumentNGramHistogram intersectHistogram(DocumentNGramHistogram dgOtherHistogram) {
        java.util.Iterator iIter = dgOtherHistogram.NGramHistogram.keySet().iterator();        
        DocumentNGramHistogram hRes = new DocumentNGramHistogram(MinSize, MaxSize);
        
        while (iIter.hasNext())
        {
            String sCurNGram = (String)iIter.next();
            if (this.NGramHistogram.containsKey(sCurNGram))
            {
                hRes.NGramHistogram.put(sCurNGram, Math.min(
                        ((Double)dgOtherHistogram.NGramHistogram.get(sCurNGram)).doubleValue()
                    , ((Double)NGramHistogram.get(sCurNGram)).doubleValue())); // Get min value
            }
        }
        return hRes;
    }
    
    public void inverseIntersectHistogram(DocumentNGramHistogram dgOtherHistogram, boolean bAffectOtherHistogram) {
        java.util.Iterator iIter = dgOtherHistogram.NGramHistogram.keySet().iterator();
        while (iIter.hasNext())
        {
            String sCurNGram = (String)iIter.next();
            if (this.NGramHistogram.containsKey(sCurNGram))
            {
                // Delete item
                NGramHistogram.remove(sCurNGram);
                if (bAffectOtherHistogram)
                    dgOtherHistogram.NGramHistogram.remove(sCurNGram);
            }
        }        
    }
    
    public void deleteItem(String sItem) {
        NGramHistogram.remove(sItem);
    }
    
    /***
     * Sets item value to zero, without removing it.
     *@param sItem The item to nullify
     ***/
    public void nullifyItem(String sItem) {
        NGramHistogram.put(sItem, 0.0);
    }
        
    public void nullify() {
        java.util.Iterator iIter = NGramHistogram.keySet().iterator();
        while (iIter.hasNext())
            NGramHistogram.put(iIter.next(), 0.0);
    }
    public String toString() {
        return NGramHistogram.toString();
    }
    
    public String getDataString() {
        return DataString;
    }
    
    public void setDataString(String sDataString) {
        DataString = sDataString;
        NGramHistogram = new HashMap(); // Clear histogram
        createHistogram();
    }
}
