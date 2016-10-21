/*
 * GraphSimilarity.java
 *
 * Created on 24 Ιανουάριος 2006, 10:36 μμ
 *
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.jinsect.events.CalculatorListener;
/**
 * Class describing three aspects of similarity between graphs (mainly):
 *<ul>
 *        <li><code>Value</code>
 *        specifying the similarity by means of co-existing edges, as well as their values.
 *        Range of possible values [0, 1]. 0 means no edges of A exists in B. 1 means all edges of
 *        A exist in B and their weights are identical.
 *        <li><code>Containment</code> 
 *        Range of possible values [0, 1]. 0 Means A is not at all contained in B,
 *        whereas 1 means A is fully contained in B (as far as edges are concerned)
 *        <li><code>SizeSimilarity</code>
 *        Range of possible values [0, 1]. Expresses the size ratio between A and B as 
 *        min(size(A) / size(B), size(B) / size(A)).		 
 *</ul>
 * @author PCKid
**/

public class GraphSimilarity implements ISimilarity {
    /** Specifies the similarity by means of co-existing nodes, as well as their values.
    * Range of possible values [0, 1].0 means no node of A exists in B. 1 means all edges of
    * A exist in B and their weights are identical.
     */
    public double ValueSimilarity;
    
    /** Specifies the similarity by means of co-existing nodes.
    * Range of possible values [0, 1].0 means no edge of A exists in B. 1 means all edges of
    * A exist in B.
     */
    public double ContainmentSimilarity;
    
    /** Specifies the similarity by means of size.
    * Range of possible values [0, 1]. 1 means A has the same number of edges as B.
    */
    public double SizeSimilarity;
    private CalculatorListener clCalculator = null;
    /**
     * Creates a new instance of GraphSimilarity
     */
    public GraphSimilarity() {
        this.ValueSimilarity = 0.0;
        this.ContainmentSimilarity = 0.0;
        this.SizeSimilarity = 0.0;
    }
    
    /** Return a string representation of this object, describing all aspects of similarity.
     *@return The detailed representation.
     */
    public String toString() {
        return "Value: " + String.format("%5.2f%%", ValueSimilarity * 100.0) + 
                " Containment: " + String.format("%5.2f%%", ContainmentSimilarity * 100.0) + 
                " Size: " + String.format("%5.2f%%", SizeSimilarity * 100.0);
    }
    
    /** Assigns a calculator of type {@link CalculatorListener} to be called when 
     * <code>getOverallSimilarity()</code> is called.
     *@param clCalc The CalculatorListener.
     *@see CalculatorListener
     */
    public void setCalculator(CalculatorListener clCalc) {
        this.clCalculator = clCalc;
    }
    
    /**
     * Calculates the overall similarity this object describes, returned as a double number. If
     * a calculator of type {@link CalculatorListener} has been assigned using the 
     * <code>setCalculator</code> method, then that calculator is used. Otherwise, the product of
     * all aspects of GraphSimilarity is returned.
     * 
     * @return The overall similarity.
     */
    public double getOverallSimilarity() {
        if (clCalculator == null)
            return ValueSimilarity * ContainmentSimilarity * SizeSimilarity;
        else
            return clCalculator.Calculate(this, this);
    }
    
    /**
     * Calculates an overall distance as a function of the overall similarity. This method uses
     * the <code>getOverallSimilarity</code> method and returns its inverse if it has a non-zero value.
     * Otherwise the return value is positive infinity.
     * 
     * @return The overall distance, ranging from zero to Double.POSITIVE_INFINITY.
     */
    public double asDistance() {
        double dS = getOverallSimilarity();
        if (dS == 0)
            return Double.POSITIVE_INFINITY;
        else
            return 1.0 / dS;
    }
    
    /**
     * Returns a 3-element double array corresponding to the aspects of GraphSimilarity.
     * 
     * @return An array where
     * <ul>
     *  <li>element 0 is the ValueGraphSimilarity aspect.
     *  <li>element 1 is the ContainmentGraphSimilarity aspect.
     *  <li>element 2 is the SizeGraphSimilarity aspect.
     * </ul>
     */
    public double[] toArray() {
        double[] daRes = new double[3];
        daRes[0] = ValueSimilarity;
        daRes[1] = ContainmentSimilarity;
        daRes[2] = SizeSimilarity;
        
        return daRes;
    }
    
    /**
     * Returns a 3-element float array corresponding to the aspects of GraphSimilarity.
     * 
     * @return An array where
     * <ul>
     *  <li>element 0 is the ValueGraphSimilarityy aspect.
     *  <li>element 1 is the ContainmentGraphSimilarityy aspect.
     *  <li>element 2 is the SizeGraphSimilarityy aspect.
     * </ul>
     */
    public float[] toFloatArray() {
        float[] faRes = new float[3];
        faRes[0] = (float)ValueSimilarity;
        faRes[1] = (float)ContainmentSimilarity;
        faRes[2] = (float)SizeSimilarity;
        
        return faRes;        
    }
}
