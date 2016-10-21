/*
 * ISentenceSelector.java
 *
 * Created on October 18, 2007, 4:11 PM
 *
 */

package gr.demokritos.iit.summarization.selection;

import java.util.List;
import java.util.Set;

/** Performs sentence selection in various ways. A sentence is considered to be a list of tokens.
 * The subclasses should implement different selection methodologies.
 *
 * @author ggianna
 */
public interface ISentenceSelector<TSentenceType,TTokenType> {
   /** Selects a sentence subset of a given sentence set.
    */
   List<TSentenceType> selectFromSentences(List<TSentenceType> Sentences);  
   
   /** Returns whether the given sentence (token list) should be selected.
    */
   boolean selectSentence(List<TTokenType> tokens);
   
   /** Returns a confidence measure of whether the given sentence (token list) should be selected.
    */
   double sentenceSelectionConfidence(List<TTokenType> tokens);
}
