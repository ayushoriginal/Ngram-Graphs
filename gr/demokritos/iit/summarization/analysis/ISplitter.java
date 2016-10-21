/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.summarization.analysis;

import java.util.List;

/** Describes classes that can tokenize sentences into tokens.
 *
 * @author pckid
 */
public interface ISplitter<TokenType> {
    /**
     * Splits a given string into a list of tokens.
     * @param sToTokenize The string to tokenize.
     * @return 
     */
    public List<TokenType> split(String sToTokenize);
}
