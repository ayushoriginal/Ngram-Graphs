/*
 * INSECTCategory.java
 *
 * Created on 24 Ιανουάριος 2006, 10:33 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ITextPrint;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.structs.Dictionary;

/** Describes objects that represent sets of documents, in an overall n-gram document representation.
 * It can be used to merge documents and also to be differentiated from given documents. Most usable
 * for classification purposes.
 *
 * @author PCKid
 */
public class NGramCategory extends NGramDocument  implements WordEvaluatorListener {
    public Dictionary Dictionary;
    public String Name = "";
    protected int DocumentCount;
    
    /** Creates a new instance of INSECTCategory, given a category name. It presupposes a maximum n-gram size of 12 
     * TODO: Max n-gram rank should be changed to a parameter.
     */
    public NGramCategory(String sName) {
        super();
        // Init dictionary
        Dictionary = new Dictionary(sName, 12); // Max word size set to 12
        Dictionary.AddSubStrings = true;
        Dictionary.RemoveSubStrings = false;
        DocumentCount = 0;
        
        Name = sName;
    }
    
    /** Modifies the category so as to merge documents as negative examples. The category converges in rejecting
     * the given type of documents, with a speed described by a learning rate parameter.
     *@param dDoc The document, the nodes of which will be marked as negative in the category document representation.
     *@param fLearningRate A number between 0.0 and 1.0 the effect of merging the given text to the category 
     * representation. A number of 1.0 means that every node of the given document will be directly considered to be
     * a negative example, and all previous learning will be rejected. 
     * A value of zero means that the example will not be taken into account. A value of 0.5 indicates that the 
     * sample should be taken as a negative example and modify existing learnt values in an averaging manner.
     */
    public void rejectDocument(NGramDocument dDoc, double fLearningRate) {    
        
        dDoc.getDocumentGraph().nullify();
        dDoc.getDocumentHistogram().nullify();
        
        if (this.getDocumentHistogram().length() > 0)
            // Calculate new histogram
            getDocumentHistogram().mergeHistogram(dDoc.getDocumentHistogram(), fLearningRate);
            
        if (!this.getDocumentGraph().isEmpty())
            // Calculate new graph
            this.getDocumentGraph().mergeGraph(dDoc.getDocumentGraph(), fLearningRate);
    }

    /** Creates the representation of a given string (text) and uses it to describe the category.
     *@param sDataString The given text.
     */
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
    
    /** Merges a given document ({@link ITextPrint} descendant) into the category.
     *@param tpData The document to merge.
     *@param fLearningRate A number between 0.0 and 1.0 the effect of merging the given text to the category 
     */
    public void mergeWith(ITextPrint tpData) {
        mergeWith(tpData, 1.0 / (Math.log10(DocumentCount + 20)));
    }
    
    /** Merges a given document ({@link ITextPrint} descendant) into the category, given a 
     * learning rate..
     *@param tpData The document to merge.
     *@param fLearningRate A number between 0.0 and 1.0 the effect of merging the given text to the category 
     * representation. A number of 1.0 means that every node of the given document will be directly considered to be
     * a negative example, and all previous learning will be rejected. 
     * A value of zero means that the example will not be taken into account. A value of 0.5 indicates that the 
     * sample should be taken as a negative example and modify existing learnt values in an averaging manner.
     */
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

    /** Performs evaluation of a word, using the dictionary of the category. Words that have not appeared in the
     * category, return a false value.
     *@param sWord The word to evaluate.
     *@return True if the word is found in the dictionary, else false.
     */
    public boolean evaluateWord(String sWord) {
        return Dictionary.contains(sWord);
    }
}
