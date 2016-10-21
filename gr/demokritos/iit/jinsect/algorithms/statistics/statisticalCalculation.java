/*
 * statisticalCalculation.java
 *
 * Created on 12 Δεκέμβριος 2006, 2:54 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.algorithms.statistics;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.Iterator;

/** Helper class implementing statistical calculation functions. Under review.
 *
 * @author ggianna
 */
public class statisticalCalculation {
    
    /** Calculates the entropy given a {@link Distribution}.
     *@param dDist The give distribution.
     *@return The entropy as bits.
     */
    public static final double entropy(Distribution dDist) {
        double dRes = 0.0;
        Iterator iIter = dDist.asTreeMap().values().iterator();
        while (iIter.hasNext()) {
            Double dNext = (Double)iIter.next();
            if (dNext == 0)
                dNext = Double.MIN_VALUE;
            dRes += dNext * (Math.log(dNext) / Math.log(2));
        }
        return -dRes;
    }
    
    /** Returns the number of groups of k members chosen from n elements.
     *@param n The number of items.
     *@param k The size of group for every combination.
     *@return The number of combinations of n by k.
     */
    public static final double combinationsOfNByK(int n, int k) {
        return gr.demokritos.iit.jinsect.utils.factorial(n, k);
    }
    
    /** Returns the probability of a given number of successful results after a given number of Bernoulli trials.
     *@param iNumberOfTrials The number of Bernoulli trials.
     *@param iNumberOfSuccesses The number of successful results.
     *@param dSuccessChance The probability of a successful result in a signle trial.
     *@return The probability of the given number of successful results.
     */
    public static final double binomialSuccessProbability(int iNumberOfTrials, int iNumberOfSuccesses, 
            double dSuccessChance) {
        return gr.demokritos.iit.jinsect.utils.factorial(iNumberOfTrials, iNumberOfSuccesses) * 
                Math.pow(dSuccessChance, iNumberOfSuccesses) * 
                Math.pow(1.0 - dSuccessChance, iNumberOfTrials - iNumberOfSuccesses);
    }
    
    /** Returns a poisson distributed random number. Based on the following algorithm:
     *<pre>
     *algorithm poisson random number (Knuth):
     * init:
     *  Let L ← pow(e,−λ), k ← 0 and p ← 1.
     * do:
     *  k ← k + 1.
     *  Generate uniform random number u and let p ← p × u.
     * while p ≥ L
     * return k − 1.
     *</pre>
     */
    public static final double getPoissonNumber(double dMean) {
        double L=Math.exp(-dMean);
        double k=0,p=1;
        do {
            k++;
            p = p * Math.random();
        } while (p >= L);
        return k-1;
    }
    
    /** Test function. Not to be used.
     */
    public static void main(String[] args) {
        Distribution dDist = new Distribution();
        dDist.setValue(1, 0.9);
        dDist.setValue(2, 0.1);
        //dDist.setValue(3, 0.25);
        //dDist.setValue(4, 0.25);
        
        System.out.println("Entropy: " + entropy(dDist));
    }
}
