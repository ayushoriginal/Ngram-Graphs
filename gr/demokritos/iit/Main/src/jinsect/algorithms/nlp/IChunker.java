/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.jinsect.algorithms.nlp;

import java.util.List;

/** An interface describing a class that can chunk a string into substrings.
 *
 * @author pckid
 */
public interface IChunker {
    /** The method that performs the chunking of a string.
     * 
     * @param sToChunk The string to chunk.
     * @return A {@link List} of substrings from the original string.
     */
    public List<String> chunkString(String sToChunk);
}
