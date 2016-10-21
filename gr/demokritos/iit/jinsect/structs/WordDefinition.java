/*
 * WordDefinition.java
 *
 * Created on 25 Ιανουάριος 2006, 12:02 πμ
 *
 */

package gr.demokritos.iit.jinsect.structs;

import java.io.Serializable;

/** A serializable word definition.
 *
 * @author PCKid
 */
public class WordDefinition implements Serializable {
    /** The word described by the definition.
     */
    public String Word;
    /** A hash corresponding to the word described by the definition.
     */
    public int Hash;
    
    /**
     * Creates a new instance of WordDefinition, given a word.
     *@param sWord The word.
     */
    public WordDefinition(String sWord) {
        Word = new String(sWord.toLowerCase()); // Use lowercase
        //Word = sWord;
         Hash = hashWord(sWord);
    }
    
    /** Calculates a hash for the word: (size * 8192) + ascii code sum.
     *@param sWord The word to hash.
     */
    public static int hashWord(String sWord) {
        int iCnt = 0, iRes = 0, iLen = sWord.length();
        for (iCnt = 0; iCnt < iLen; iCnt++)
            iRes += (int)sWord.charAt(iCnt);            
        return (sWord.length() * 8192 + iRes);
    }
    
    /** Returns the hashcode of this word.
     *@return The hash code.
     */
    public final int hashCode() {
        return Hash;
    }
    
    /** Compares two word definitions.
     *@param oObj The word definition to compare this definition with.
     *@return True if this object and the given object refer to the same word.
     */
    public final boolean equals(Object oObj) {
        // return hashCode() == ((WordDefinition)oObj).hashCode();
        return Word.equals(((WordDefinition) oObj).Word);
    }
}
