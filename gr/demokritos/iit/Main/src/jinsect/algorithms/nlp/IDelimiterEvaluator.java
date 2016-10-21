/*
 * IDelimiterEvaluator.java
 *
 * Created on 8 Ιανουάριος 2007, 12:08 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.algorithms.nlp;

/** Describes an evaluator that can determine delimiter characters in a given string.
 *
 * @author ggianna
 */
public interface IDelimiterEvaluator {
    /** Checks whether the character in a given position of a given string is a delimiter.
     *@param iPos The position to check.
     *@param sStr The used string.
     *@return True if the given character is a delimiter, else false.
     */
    public boolean isDelimiter(int iPos, String sStr); 
}
