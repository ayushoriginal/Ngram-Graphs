/*
 * IMeaningExtractor.java
 *
 * Created on 10 Ιανουάριος 2007, 1:40 μμ
 *
 */

package gr.demokritos.iit.conceptualIndex;

import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;

/** Interface for classes that can return a {@link WordDefinition} given a string word.
 *
 * @author ggianna
 */
public interface IMeaningExtractor {
    /** Should return a WordDefinition object, according to the input string.
     *@param sString The string to lookup.
     *@return The meaning of the word being looked up.
     *@see WordDefinition
     */
    public WordDefinition getMeaning(String sString);
}
