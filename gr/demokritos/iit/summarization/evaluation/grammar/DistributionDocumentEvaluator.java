/*
 * DistributionDocumentEvaluator.java
 *
 * Created on October 26, 2007, 12:44 PM
 *
 */

package gr.demokritos.iit.summarization.evaluation.grammar;

import gr.demokritos.iit.conceptualIndex.documentModel.DistributionDocument;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import gr.demokritos.iit.jinsect.console.grammaticalityEstimator;

/**
 *
 * @author ggianna
 */
public class DistributionDocumentEvaluator implements IGrammaticallityEvaluator {
    Map<Integer,String> WordMap;
    grammaticalityEstimator Estimator;
    
    /**
     * Creates a new instance of DistributionDocumentEvaluator, given an integer-to-term map and a distribution document.
     * 
     * @param mIdxToWord A {@link Map} connecting indices to (string) terms.
     * @param geEstimator A {@linkgrammaticalityEstimatorr} indicating the accepted distributions.
     */
    public DistributionDocumentEvaluator(Map<Integer,String> mIdxToWord, grammaticalityEstimator geEstimator) {
        WordMap = mIdxToWord;
        Estimator = geEstimator;
    }

    /** Returns a double value, indicative of grammaticallity given underlying distributions. Higher values indicate higher 
     * grammaticallity.
     *@param vText The {@link Vector} of term indices, representing the text.
     */
    public double getGrammaticallity(Vector vText) {
        Iterator<Integer> iIter = vText.iterator();
        StringBuffer sbText = new StringBuffer();
        while (iIter.hasNext()) {
            Integer iCurWord = iIter.next();
            sbText.append(WordMap.get(iCurWord));
        }
        
        return Estimator.getNormality(sbText.toString());
    }
    
}
