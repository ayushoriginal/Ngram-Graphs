/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.summarization.analysis;

import java.util.Arrays;
import java.util.List;

/** Splits a text into sentences, by using simple delimiter characters.
 *
 * @author pckid
 */
public class SentenceSplitter implements ISplitter<String> {
    protected String Delimiters;
    
    public SentenceSplitter() {
        Delimiters = "[\\/-<>,.:;\"'?!@#$%^&*()]";
    }
    
    public SentenceSplitter(String sDelimiters) {
        Delimiters = sDelimiters;
    }
    
    @Override
    public final List<String> split(String sToTokenize) {
        return Arrays.asList(sToTokenize.split(Delimiters));
    }

}
