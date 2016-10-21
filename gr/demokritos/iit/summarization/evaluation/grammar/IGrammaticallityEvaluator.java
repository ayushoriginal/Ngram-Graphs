/*
 * IGrammaticallityEvaluator.java
 *
 * Created on October 26, 2007, 11:32 AM
 *
 */

package gr.demokritos.iit.summarization.evaluation.grammar;

import java.util.Vector;

/** Represents a bearer of grammaticality estimation.
 *
 * @author ggianna
 */
public interface IGrammaticallityEvaluator {
    /** Should determine the grammaticality of a given text (vector of terms). 
     *@param vText A {@link Vector} of terms (probably strings or string indices).
     */
    public double getGrammaticallity(Vector vText);
}
