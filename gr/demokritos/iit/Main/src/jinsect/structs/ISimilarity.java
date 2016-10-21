/*
 * ISimilarity.java
 *
 * Created on April 12, 2007, 11:29 AM
 *
 */

package gr.demokritos.iit.jinsect.structs;

/** Represents a similarity measure.
 *
 * @author ggianna
 */
public interface ISimilarity {
    /** Computes the overall similarity, given any (descendent defined) parameters.
     *@return The overall similarity, as a double value.
     */
    public double getOverallSimilarity();
    
    /** Computes a distance as a function of the overall similarity.
     *@return The overall distance, as a double value.
     */
    public double asDistance();
}
