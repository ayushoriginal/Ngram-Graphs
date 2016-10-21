/*
 * OverallGraphSimilarity.java
 *
 * Created on 24 Ιανουάριος 2006, 11:40 μμ
 *
 */

package gr.demokritos.iit.jinsect.structs;

/**
 * A GraphSimilarity-descended class that calculates a weighted overall similarity measure.
 * 
 * @author PCKid
 */
public class OverallGraphSimilarity extends GraphSimilarity {
    /** Calculates the overall similarity, giving different weights to Value, Containment 
     * and Size similarities in descending order.
     *@return The overall similarity.
     */
    public double getOverallSimilarity() {
        return ((ValueSimilarity * 13) + (ContainmentSimilarity * 6) + SizeSimilarity) / 20;
        // TODO: Finetune
    }    
}
