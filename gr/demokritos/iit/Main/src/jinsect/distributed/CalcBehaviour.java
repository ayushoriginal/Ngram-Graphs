/*
 * CalcBehaviour.java
 *
 * Created on 15 ?????????????????????? 2007, 1:17 ????
 *
 */

package gr.demokritos.iit.jinsect.distributed;

import jade.core.behaviours.OneShotBehaviour;
import java.io.InvalidClassException;
import gr.demokritos.iit.jinsect.documentModel.comparators.CachedDocumentComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.NGramGaussNormDocument;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;

/** Implements an {@link OneShotBehaviour} that calculates the similarity between two
 * documents, using words and char n-grams. The character n-gram part is performed 
 * using the Gauss-normalized methodology.
 */
public class CalcBehaviour extends OneShotBehaviour {
    SimpleTextDocument ndDoc1 = null;
    SimpleTextDocument ndDoc2 = null;
    NGramDocument ndNDoc1 = null;
    NGramDocument ndNDoc2 = null;
    
    String Doc1, Doc2, ID;
    int WordNGramSize_Min, WordNGramSize_Max, Word_Dmax,
            CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax;
    boolean DoCharNGrams, DoWordNGrams, Silent;

    /** Initializes a CalcBehaviour, given two texts to compare and the parameters of
     * the comparison.
     *@param sDoc1 The first document.
     *@param sDoc2 The second document.
     *@param iWordNGramSize_Min The minimum word n-gram to use.
     *@param iWordNGramSize_Max The maximum word n-gram to use.
     *@param iWord_Dmax The maximum neighbourhood word distance to use.
     *@param iCharacterNGramSize_Min The minimum character n-gram to use.
     *@param iCharacterNGramSize_Max The maximum character n-gram to use.
     *@param iCharacter_Dmax The maximum neighbourhood character distance to use.
     *@param bDoCharNGrams If true, character n-gram comparison is performed.
     *@param bDoWordNGrams If true, word n-gram comparison is performed.
     *@param bSilent If true, no debugging messages appear.
     *@param sID A unique identifier for the comparison.
     */
    public CalcBehaviour(String sDoc1, String sDoc2, 
            int iWordNGramSize_Min, int iWordNGramSize_Max, int iWord_Dmax,
            int iCharacterNGramSize_Min, int iCharacterNGramSize_Max, int iCharacter_Dmax, 
            boolean bDoCharNGrams, boolean bDoWordNGrams, boolean bSilent, String sID) {
        
        Doc1 = sDoc1; 
        Doc2 = sDoc2;
        ID = sID;
        WordNGramSize_Min = iWordNGramSize_Min;
        WordNGramSize_Max = iWordNGramSize_Max;
        Word_Dmax = iWord_Dmax;
        CharacterNGramSize_Min = iCharacterNGramSize_Min;
        CharacterNGramSize_Max = iCharacterNGramSize_Max;
        Character_Dmax = iCharacter_Dmax;
        
        DoCharNGrams = bDoCharNGrams;
        DoWordNGrams = bDoWordNGrams;
        Silent = bSilent;
    }
    
    /** Performs the comparisonm and sends the saves the result. */
    public void action() {
        SimilarityArray saRes = null;
        try 
        {
            // Read first file
            ndDoc1 = new SimpleTextDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
            ndNDoc1 = new NGramGaussNormDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, CharacterNGramSize_Min, CharacterNGramSize_Max);
            if (DoWordNGrams)
                ndDoc1.setDataString(Doc1);
            if (DoCharNGrams)
                ndNDoc1.setDataString(Doc1);

            StandardDocumentComparator sdcComparator = new StandardDocumentComparator();
            CachedDocumentComparator sdcNComparator = new CachedDocumentComparator();

            SimpleTextDocument ndDoc2 = null;
            NGramDocument ndNDoc2 = null;

            // Read second file
            if (DoWordNGrams)
            {
                // Look up cache
                ndDoc2 = new SimpleTextDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
                ndDoc2.setDataString(Doc2);
            }

            if (DoCharNGrams) {
                ndNDoc2 = new NGramGaussNormDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, 
                        CharacterNGramSize_Min, CharacterNGramSize_Max);
                ndNDoc2.setDataString(Doc2);
            }

            // Execute comparison
            saRes = new SimilarityArray();
            GraphSimilarity sSimil = null;
            if (DoWordNGrams) {
                try {
                    // Get simple text similarities
                    sSimil = sdcComparator.getSimilarityBetween(ndDoc1, ndDoc2);
                } catch (InvalidClassException ex) {
                    synchronized(System.err) {                    
                        ex.printStackTrace(System.err);
                        return;
                    }
                }
                saRes.SimpleTextOverallSimil = sSimil;
                saRes.SimpleTextGraphSimil = sdcComparator.getGraphSimilarity();
                saRes.SimpleTextHistoSimil = sdcComparator.getHistogramSimilarity();
            }

            GraphSimilarity sSimil2 = null;
            if (DoCharNGrams) {
                try {
                    sSimil2 = sdcNComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);
                } catch (InvalidClassException ex) {
                    synchronized(System.err) {
                        ex.printStackTrace(System.err);
                        return;
                    }
                }
                // Get n-gram document similarities
                saRes.NGramOverallSimil = sSimil2;
                saRes.NGramGraphSimil = sdcNComparator.getGraphSimilarity();
                saRes.NGramHistoSimil = sdcNComparator.getHistogramSimilarity();
            }

            // Set result
            synchronized (myAgent) {
                ((NGramDocumentComparatorAgent)myAgent).setResult(saRes, ID);
            }

            if (!Silent)
                synchronized(System.err) {
                    // DEBUG LINES
                    System.err.println(myAgent.getName() + ":" + "Calculation complete.");
                    //////////////
                }
        }
        finally 
        {
            synchronized (myAgent) {
                ((NGramDocumentComparatorAgent)myAgent).setResult(saRes, ID);
            }            
        }
    }
    
}

