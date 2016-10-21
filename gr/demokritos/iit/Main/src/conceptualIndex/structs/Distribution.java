/*
 * Distribution.java
 *
 * Created on 24 ?????????????? 2006, 5:41 ????
 *
 */

package gr.demokritos.iit.conceptualIndex.structs;

import gr.demokritos.iit.conceptualIndex.events.IDistributionComparisonListener;
import gr.demokritos.iit.jinsect.algorithms.statistics.ChiSquareDistributionBase;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Represents a ditribution of values, either in terms of a simple histogram, or in terms
 *of a probability distribution. Also provides methods for distribution normalization , comparison
 *and other useful functions.
 *
 * @author ggianna
 */
public class Distribution<TKeyType> implements Serializable, IDistributionComparisonListener {
    /** The underlying {@link TreeMap} representation of the Distribution
     */
    private NavigableMap<TKeyType, Double> hDistro;
    
    /** Creates a new instance of Distribution */
    public Distribution() {
        hDistro = new TreeMap();
    }
    
    /** Creates a new instance of Distribution, given a corresponding {@link TreeMap}. The TreeMap should contain
     *<Object, Double> entries.
     *@param tm The TreeMap.
     */
    public Distribution(NavigableMap<TKeyType, Double> tm){
        hDistro = tm;
    }
    
    /**Looks up the value of a selected key, or zero if the key has no value.
     *@param oKey The key to look up.
     *@return The value mapped to the selected key.
     */
    public double getValue(TKeyType oKey) {
        if (hDistro.containsKey(oKey))
            return ((Double)hDistro.get(oKey)).doubleValue();
        else
            return 0.0; // Could be null
    }
    
    /**Sets the value of a selected key.
     *@param oXValue The key to use. The key can either be a double number or an object.
     *@param dYValue The value to apply to the selected key.
     */
    public void setValue(TKeyType oXValue, double dYValue) {
        hDistro.put(oXValue, dYValue);
    }
    
    /**Increases the value of a selected key by a quantity.
     *@param oXValue The key to use. The key can either be a double number or an object.
     *@param dYValue The value by which to increase the selected key value.
     */
    public void increaseValue(TKeyType oXValue, double dYValue) {
        hDistro.put(oXValue, getValue(oXValue) + dYValue);
    }
    
    /**Composes a string representation of this distribution, referring only to non-zero elements.
     *@return The string representation of this distribution.
     */
    @Override
    public String toString() {
        String sRes = "";
        Iterator iIter = hDistro.entrySet().iterator();
        while (iIter.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry)iIter.next();
            sRes = sRes.concat(String.valueOf(e.getKey()) + "=>" + String.valueOf(e.getValue()));
            if (iIter.hasNext())
                sRes += "\t";
        }
        
        return sRes;
    }
    
    /** Returns the underlying {@link TreeMap} structure.
     *@return The underlying tree map.
     */
    public NavigableMap<TKeyType, Double> asTreeMap() {
        return hDistro;
    }
    
    /**Calculates the degree of similarity between two distributions. The similarity is calculated in
     *the following steps:
     *<ol> 
     * <li>The sum of the absolute difference of the values of each distribution over all points is
     * calculated. Let this be SD.
     * <li> The values of both compared distributions are summed. Let this be SV.
     * <li> The returned value is 1.0 - (SD / SV).
     *</ol> 
     * Derived classes could override this function to use different similarity metrics.
     *@param dOther The distribution to compare to.
     *@return A value between 1.0 (identity) and 0.0 (no similarity) indicative of similarity
     * between this distribution and the given one.
     */
    public double similarityTo(Distribution dOther) {
        TreeMap tOverAll = new TreeMap();
        
        Iterator iThisIter = hDistro.entrySet().iterator();
        Iterator iOtherIter = dOther.asTreeMap().entrySet().iterator();
        
        // For this distribution, add all points to dOverAll
        while (iThisIter.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry)iThisIter.next();
            Double[] dVals = new Double[2];
            dVals[0] = (Double)e.getValue();
            dVals[1] = 0.0;
            tOverAll.put(e.getKey(),dVals);
        }
        
        // For the other distribution, add all points to dOverAll
        while (iOtherIter.hasNext()) {
            Double[] dVals = new Double[2];
            java.util.Map.Entry e = (java.util.Map.Entry)iOtherIter.next();
            if (tOverAll.containsKey(e.getKey()))
            {
                dVals = (Double [])tOverAll.get(e.getKey());
                dVals[1] = (Double)e.getValue();
            }
            else
            {
                dVals = new Double[2];
                dVals[0] = 0.0;
                dVals[1] = (Double)e.getValue();
            }
            tOverAll.put(e.getKey(), dVals);
        }
        
        Iterator iIter = tOverAll.entrySet().iterator();
        double dDiff = 0.0;
        // For all x-values of the overall distribution
        while (iIter.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry)iIter.next();
            // Calc the y-value distance
            Double []dVals = (Double[])e.getValue();
            dDiff += Math.abs(dVals[1] - dVals[0]);
        }
        
        return 1.0 - (dDiff / (calcTotalValues() + dOther.calcTotalValues()));
    }
    
    /**Calculates the sum of all the values in the distribution.
     *@return The sum of values over all points.
     */
    public double calcTotalValues() {
        Iterator iIter = hDistro.entrySet().iterator();
        double dSum = 0.0;
        while (iIter.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry)iIter.next();
            dSum += (Double)(e.getValue());
        }
        return dSum;
    }
    
    /**Normalizes the values of the distribution to a range of numbers between zero (0) 
     * and a selected value.
     *@param dNewMax The new maximum value after the transformation.
     */
    public void normalizeTo(double dNewMax) {
        double dMax = 0;
        // 1st pass - Find max
        Iterator iValIter = hDistro.values().iterator();
        while (iValIter.hasNext()) {
            dMax = Math.max(dMax, (Double)iValIter.next());
        }
        double dFactor = dNewMax / dMax;
        
        Iterator<TKeyType> iKeyIter = hDistro.keySet().iterator();
        while (iKeyIter.hasNext()) {
            TKeyType oKey = iKeyIter.next();
            Double dVal = (Double)hDistro.get(oKey);
            dVal *= dFactor; // Normalize
            hDistro.put(oKey, dVal);
        }
    }
    
    /**Normalizes the values of the distribution to the range [0,1] 
     */
    public void normalize() {
        normalizeTo(1.0);
    }
    
    /**Normalizes the values of the distribution to the sum of values, resolving to a probability
     *distribution, as the sum of the new distribution will amount to 1.
     */
    public void normalizeToSum() {
        double dMax = 0;
        // 1st pass - Find sum
        Iterator iValIter = hDistro.values().iterator();
        while (iValIter.hasNext()) {
            dMax += (Double)iValIter.next();
        }
        
        Iterator<TKeyType> iKeyIter = hDistro.keySet().iterator();
        while (iKeyIter.hasNext()) {
            TKeyType oKey = iKeyIter.next();
            Double dVal = (Double)hDistro.get(oKey);
            dVal /= dMax; // Normalize
            hDistro.put(oKey, dVal);
        }
    }
    
    /**Calculates the sum of values over all distribution points.
     *@return The sum of values.
     */
    public double sumOfValues() {        
        double dSum = 0;
        // 1st pass - Find sum
        Iterator iValIter = hDistro.values().iterator();
        while (iValIter.hasNext()) {
            dSum += (Double)iValIter.next();
        }
        
        return dSum;
    }

    /** Sums over all the values, taking into account the keys as weights.
     * If keys are not double keys, then only the weights are considered to be
     * equal to 1.
     * @return The sum of all (key*value) pairs.
     */
    public double sumWithWeights() {
        double dSum = 0;
        // 1st pass - Find sum
        for (TKeyType dKey : hDistro.keySet()) {
            if (dKey instanceof Double)
                dSum += ((Double)dKey * getValue(dKey));
            else
                dSum += getValue(dKey);
        }

        return dSum;
    }
    
    /** Creates a new probability distribution, corresponding to the occurences
     * of the features appearing in this distribution. If all appearences are equal to zero,
     * returns a uniform distribution.
     *@return The derived probability distribution.
     */
    public Distribution getProbabilityDistribution() {
        Distribution dNew = new Distribution();
        dNew.asTreeMap().putAll(hDistro);
        if (dNew.sumOfValues() == 0.0) // Cannot normalize
        {
            Iterator iIter = dNew.asTreeMap().keySet().iterator();
            while (iIter.hasNext())
                dNew.setValue(iIter.next(), 1.0); // Equiprobable
        }
        dNew.normalizeToSum();
        
        return dNew;
    }
    
    /** Calculates the average value of the distribution. For a distribution where the keys are double
     *values, this function can calculate the expected value (mean), taking into account the keys. In
     *other cases the result is the average of values.
     *@param bOnlyValueAverage If true, then only the value average is computed, otherwise 
     * the expected value is calculated, taking into account the keys (which are supposed to be 
     * {@link Double} objects)
     *@return The expected (mean / average) value of the distribution.
     */
    public double average(boolean bOnlyValueAverage) {
        double dRes = 0.0;
        // Clone self
        Distribution dTemp = new Distribution();
        dTemp.asTreeMap().putAll(hDistro);
        // If keys should be taken into account
        if (!bOnlyValueAverage)
            // Normalize to sum, so as to create a probability mass function
            dTemp = dTemp.getProbabilityDistribution();
        
        // Calc average
        Iterator iKeyIter = dTemp.asTreeMap().keySet().iterator();
        while (iKeyIter.hasNext()) {
            Object oNextKey = iKeyIter.next();
            if (bOnlyValueAverage) // If only values are of interest
                dRes += (Double)dTemp.getValue(oNextKey) / dTemp.asTreeMap().size(); // Value average only
            else
                dRes += (Double)oNextKey * (Double)dTemp.getValue(oNextKey); // Determine actual expectation
        }
        
        return dRes;
    }
    
    /** Looks up the maximum value appearing in the distribution.
     *@return The maximum value of the distribution (considering non-zero elements).
     */
    public double maxValue() {
        Iterator iIter = asTreeMap().values().iterator();
        Double dMax = Double.NEGATIVE_INFINITY;
        while (iIter.hasNext()) {
            Double dVal = (Double)iIter.next();
            dMax = (dVal > dMax) ? dVal : dMax;
        }
        return dMax;
    }
    
    /** Looks up the minimum value appearing in the distribution.
     *@return The minimum value of the distribution (considering non-zero elements).
     */
    public double minValue() {
        Iterator iIter = asTreeMap().values().iterator();
        Double dMin = Double.POSITIVE_INFINITY;
        while (iIter.hasNext()) {
            Double dVal = (Double)iIter.next();
            dMin = (dVal < dMin) ? dVal : dMin;
        }
        return dMin;
    }
    
    
    /** Calculates the variance of the distribution, either for the values only, or taking into 
     * account the keys.
     *@param bOnlyValue If true, then only the value variance is computed, otherwise 
     * the variance is calculated taking into account the keys (which are supposed to be 
     * {@link Double} objects).
     *@return The variance of the distribution.
     */
    public double variance(boolean bOnlyValue) {
        double dExpectation = average(bOnlyValue);
        double dRes = 0.0;
        // Calc average
        Iterator iKeyIter = hDistro.keySet().iterator();
        double dObservationCount = 0;
        while (iKeyIter.hasNext()) {
            Object oNextKey = iKeyIter.next();
            if (bOnlyValue)  {// If only values are of interest
                dRes += Math.pow((Double)hDistro.get(oNextKey) - dExpectation, 2); // Value variance only
                dObservationCount++;
            }
            else {
                dRes += (Double)hDistro.get(oNextKey) * Math.pow((Double)oNextKey - dExpectation, 2); // Determine actual expectation
                dObservationCount += (Double)hDistro.get(oNextKey);
            }
        }

        return (dRes / dObservationCount);
    }

    
    /** Calculates the standard deviation (square root of the variance) of the distribution, 
     * either for the values only, or taking into account the keys.
     *@param bOnlyValue If true, then only the value standard deviation is computed, otherwise 
     * the standard deviation is calculated taking into account the keys (which are supposed to be 
     * {@link Double} objects).
     *@return The standard deviation of the distribution.
     */
    public double standardDeviation(boolean bOnlyValue) {
        return Math.sqrt(variance(bOnlyValue));
    }

    /**
     * Performs a normality test (JB-test) for the given distribution.
     * @param bOnlyValue If true, then only the value normality test is
     * performed, otherwise the test is calculated taking into account the
     * keys (which are supposed to be {@link Double} objects).
     * @param dPValueForRejection The statistical leve of confidence as the
     * p-value value for the rejection of the normality hypothesis. Usually,
     * this value is 5% (i.e. 0.05).
     * @return True if distribution cannot be rejected as normal, within the
     * given statistical confidence.
     */
    public boolean isNormal(boolean bOnlyValue, double dPValueForRejection) {
        double dPRes = 0.0;
        double dSD = standardDeviation(bOnlyValue);
        double dSkewness = getCentralMoment(bOnlyValue, 3) /
                Math.pow(dSD, 3.0);
        double dKurtosis = getCentralMoment(bOnlyValue, 4) /
                Math.pow(dSD, 4.0);

        double dJB = (observationCount(bOnlyValue) / 6.0) * (Math.pow(dSkewness, 2.0) +
                (Math.pow(dKurtosis - 3, 2.0) / 4));
        dPRes = ChiSquareDistributionBase.getPValue(dJB);

        return dPRes > dPValueForRejection;
    }

    /** Returns the distribution value at a given percentage of the population.
     * Uses the ordering of keys.
     * 
     * @param bOnlyValue If true, then each key is considered to be mapped to a
     * single observation, otherwise the frequency of each key is taken into 
     * account (keys are supposed to be {@link Double} objects).
     * @param dPopulPoint Indicates the observation of interest. The observation
     * is the [dPopulPoint * (number of observations)]th observation in the
     * distribution.
     * @return The key value describing for the selected observation.
     */
    public TKeyType getValueAtPoint(boolean bOnlyValue, double dPopulPoint) {
        // Get the 95 percent threshold otherwise
        TKeyType dRes = null;
        if (dPopulPoint == 0.0)
            return asTreeMap().firstKey();

        if (dPopulPoint == 1.0)
            return asTreeMap().lastKey();
        
        double dNumOfObsvs = observationCount(bOnlyValue);
        double dCnt = 0;
        Iterator iSentLengthIter = asTreeMap().keySet().iterator();
        while (dCnt < dPopulPoint * dNumOfObsvs) {
            if (!iSentLengthIter.hasNext())
                break;
            TKeyType dNextObs = (TKeyType)iSentLengthIter.next();
            dCnt += getValue(dNextObs);
            dRes = dNextObs;
        }
        return dRes;
    }

    /** Returns the number of observations within the distribution.
     *
     * @param bOnlyValue If true, then every key value is considered to be
     * mapped to a single observation. Otherwise, the result sums the values over
     * all keys (which are supposed to be {@link Double} objects).
     * @return The number of observations.
     */
    public double observationCount(boolean bOnlyValue) {
        if (bOnlyValue)
            return hDistro.keySet().size();
        else
        {
            int iCnt = 0;

            Iterator iKeyIter = hDistro.keySet().iterator();
            while (iKeyIter.hasNext()) {
                Object oNextKey = iKeyIter.next();
                iCnt += ((Double)hDistro.get(oNextKey));
            }
            return iCnt;
        }
    }

    /** Returns the n-th central moment around the mean for the distribution.
     *
     * @param bOnlyValue If true, then only the value normality test is
     * performed, otherwise the test is calculated taking into account the
     * keys (which are supposed to be {@link Double} objects).
     * @param iOrder The order of the moment.
     * @return The value of the n-th moment around the mean.
     */
    public double getCentralMoment(boolean bOnlyValue, int iOrder) {
        double dRes = 0.0;
        double dExpectation = average(bOnlyValue);
        
        // Calc average
        Iterator iKeyIter = hDistro.keySet().iterator();
        while (iKeyIter.hasNext()) {
            Object oNextKey = iKeyIter.next();
            if (bOnlyValue) // If only values are of interest
                dRes += Math.pow((Double)hDistro.get(oNextKey) - dExpectation, 
                        iOrder); // Value variance only
            else
                dRes += (Double)hDistro.get(oNextKey) * Math.pow((Double)oNextKey 
                        - dExpectation, iOrder);
        }

        return dRes / observationCount(bOnlyValue);
    }
    
    /** Looks up the key corresponding to the maximum value of the distribution.
     *@return The key of the maximum value.
     */
    public TKeyType getKeyOfMaxValue() {
        Iterator<TKeyType> iKeyIter = hDistro.keySet().iterator();
        TKeyType oRes = null;
        Object oMax = null;
        
        while (iKeyIter.hasNext()) {
            TKeyType oNextKey = iKeyIter.next();
            if (oMax != null)
            {
                // Check if current element is max
                if (((Comparable)hDistro.get(oNextKey)).compareTo((Comparable)oMax) > 0)
                {
                    oRes = oNextKey;
                    oMax = hDistro.get(oNextKey);
                }                
            }
            else
            {
                // First element. Set as max.
                oRes = oNextKey;
                oMax = hDistro.get(oNextKey);
            }                
                
        }
        
        return oRes;
    }

    /** Inverts the probabilities of this distribution.
     */
    public void invertProbability() {
        normalizeToSum();
        double dRemaining = 1.0;
        Iterator<TKeyType> iIter = hDistro.keySet().iterator();
        while (iIter.hasNext()) {
            TKeyType oNext = iIter.next();
            setValue(oNext, dRemaining - getValue(oNext));
            dRemaining += getValue(oNext);
        }
    }
    
    /** Returns a random key using this probability distribution. For example if the distribution
     * represents the prbabilities of dice rolls in a six-sided dice, then this function will return
     * the next roll result.
     *@return The key of a random selection using this distribution.
     */
    public Object getNextResult() {
        // Clone distro
        Distribution d = new Distribution();
        d.asTreeMap().putAll(hDistro);
        
        // Get random roll within [0, sumOfValues())
        double dRoll = Math.random() * d.sumOfValues();
        // For each key
        Iterator iIter = d.asTreeMap().keySet().iterator();
        double dCurProbMassVal = 0.0;
        while (iIter.hasNext()) {
            Object oNext = iIter.next();
            // If probability mass function value for current key
            // is grater that the roll, then return the current key.
            dCurProbMassVal += (Double)d.getValue(oNext);
            if (dCurProbMassVal > dRoll)
                return oNext;
        }
        
        return d.asTreeMap().lastKey();
    }
    
    /** Testing function.
     */
    public static void main(String[] args) {
        Distribution d = new Distribution();
//        d.setValue(1.0, 1.0/6);
//        d.setValue(2.0, 1.0/36);
//        d.setValue(3.0, 1.0/18);
//        d.setValue(4.0, 1.0/12);
//        d.setValue(5.0, 1.0/9);
//        d.setValue(6.0, 5.0/36);
//        d.setValue(7.0, 1.0/6);
//        d.setValue(8.0, 5.0/36);
//        d.setValue(9.0, 1.0/9);
//        d.setValue(10.0, 1.0/12);
//        d.setValue(11.0, 1.0/18);
//        d.setValue(12.0, 1.0/36);
        
//        d.setValue(1.0, 7.0);
//        d.setValue(2.0, 14.0);
//        d.setValue(3.0, 7.0);
//        d.setValue(5.0, 7.0);
//        d.setValue(6.0, 14.0);
//        d.setValue(7.0, 7.0);
        
//        Distribution dTmp = new Distribution();
//        dTmp.setValue(1.0, 1.0);
//        dTmp.setValue(2.0, 3.0);
//        dTmp.setValue(3.0, 5.0);
//        dTmp.setValue(5.0, 7.0);
//        dTmp.setValue(6.0, 5.0);
//        dTmp.setValue(7.0, 3.0);
//        dTmp.setValue(8.0, 1.0);
        d.setValue(1.0, 1.0);
        d.setValue(2.0, 1.0);
        d.setValue(3.0, 1.0);
        d.setValue(4.0, 1.0);
        d.setValue(5.0, 1.0);
        System.out.println(gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation.entropy(d));
//        System.out.println(gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation.entropy(dTmp));

        /* 
        d.normalizeToSum();
        
        d.normalize();
        
        Distribution dCur = new Distribution();
        double dMinErr = Double.POSITIVE_INFINITY;
        for (int iCnt = 0; iCnt < Math.pow(2.0, 20); iCnt++) {
            // Create distro
            Distribution d2 = new Distribution();
            for (int iDigitCnt = 0; iDigitCnt < 20; iDigitCnt++) {
                d2.setValue((double)iDigitCnt, 
                        (double)(iCnt & (int)(Math.pow(2.0, iDigitCnt))) > 0 ? 1 : 0);
            }
            
            double dCurErr = d.calcSumSquaredErrorFrom(d2);
            if (dCurErr < dMinErr)
            {
                dMinErr = dCurErr;
                dCur = d2;
            }
            
            if (iCnt % 10000 == 0)
                System.out.println(((double)iCnt / Math.pow(2.0, 20)) + " completed...");
        }
        System.out.println("Min Error:" + dMinErr);
        System.out.println("Distribution:" + dCur.toString());
        */
        
        System.out.println("Only values:\n===========");
        System.out.println("Mean: " + d.average(true));
        System.out.println("Variance: " + d.variance(true));
        System.out.println("Std deviation :" + d.standardDeviation(true));
        System.out.println("\nIs normal: " + d.isNormal(true, 0.05));
        System.out.println("\nUsing keys:\n===========");
        System.out.println("Mean: " + d.average(false));
        System.out.println("Variance: " + d.variance(false));
        System.out.println("Std deviation :" + d.standardDeviation(false));
        System.out.println("\nKey of max value: " + d.getKeyOfMaxValue());
        System.out.println("\nIs normal: " + d.isNormal(false, 0.05));
        System.out.println("\nValue at 5% of population: " + d.getValueAtPoint(false, 0.05));
        System.out.println("\nValue at 95% of population: " + d.getValueAtPoint(false, 0.95));

    }
    
    public double calcSumSquaredErrorFrom(Distribution dOther) {
        double dErr = 0.0;
        Iterator<TKeyType> iThisIter = asTreeMap().keySet().iterator();
        while (iThisIter.hasNext()) {
            TKeyType oVal = iThisIter.next();
            dErr += Math.pow(this.getValue(oVal) - dOther.getValue(oVal), 2.0);
        }
        
        // Check keys not existing in this distro
        Iterator<TKeyType> iOtherIter = dOther.asTreeMap().keySet().iterator();
        while (iOtherIter.hasNext()) {
            TKeyType oVal = iOtherIter.next();
            if (asTreeMap().containsKey(oVal))
                continue; // Ignore common values. They have been taken into account already.
            dErr += Math.pow(this.getValue(oVal) - dOther.getValue(oVal), 2.0);
        }
        
        return dErr;
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(asTreeMap());
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        hDistro = (TreeMap)in.readObject();
    }
    
    /** Creates a new distribution containing the sum of values of the given distribution and a second distribution.
     *@param dOther The other distribution.
     *@return A new Distribution which contains values corresponding to the sum of values of the original distributions.
     */
    public Distribution addTo(Distribution dOther) {
        double dSimilarity = 0.0;
        TreeMap<Object, Double> tOverAll = new TreeMap<Object, Double>();
        
        Iterator iThisIter = hDistro.entrySet().iterator();
        Iterator iOtherIter = dOther.asTreeMap().entrySet().iterator();
        
        // For this distribution, add all points to dOverAll
        while (iThisIter.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry)iThisIter.next();
            tOverAll.put(e.getKey(),(Double)e.getValue());
        }
        
        // For the other distribution, add all points to dOverAll
        while (iOtherIter.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry)iOtherIter.next();
            double dVal;
            if (tOverAll.containsKey(e.getKey()))
            {
                dVal = (Double)tOverAll.get(e.getKey()).doubleValue();
                dVal += ((Double)e.getValue()).doubleValue();
            }
            else
            {
                dVal = (Double)e.getValue();
            }
            tOverAll.put(e.getKey(), dVal);
        }
        
        return new Distribution(tOverAll);
        
    }

    /** Use default comparison between d1 and d2. */
    public double compareDistributions(Distribution d1, Distribution d2) {
        return d1.similarityTo(d2);
    }
}
