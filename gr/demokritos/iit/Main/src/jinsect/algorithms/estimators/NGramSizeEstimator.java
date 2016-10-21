/*
 * NGramSizeEstimator.java
 *
 * Created on July 2, 2007, 9:57 AM
 *
 */

package gr.demokritos.iit.jinsect.algorithms.estimators;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.structs.IntegerPair;

/** A class for the estimation of symbols and non-symbols, as defined in [Giannakopoulos,2008], for given
 * distribution of n-gram ranks in a set of n-grams. The class is mainly used for its ability to determine 
 * a signal-to-noise ratio (see <code>getSignalToNoise</code>).
 * @author ggianna
 */
public class NGramSizeEstimator {
    
    /** The distribution of symbols per n-gram rank. */
    protected Distribution SymbolsPerRank;
    /** The distribution of non-symbols per n-gram rank. */
    protected Distribution NonSymbolsPerRank;
    /** The minimum and maximum n-gram rank used. */
    protected int MinRank, MaxRank;
    
    /** Creates a new instance of NGramSizeEstimator, given two distribution of symbols and non-symbols,
     * by getting a copy of the distributions.
     *@param tmSymbolsPerRank The distribution of symbols per n-gram rank.
     *@param tmNonSymbolsPerRank The distribution of non-symbols per n-gram rank.
     */
    public NGramSizeEstimator(final Distribution tmSymbolsPerRank, 
            final Distribution tmNonSymbolsPerRank) {
        SymbolsPerRank = new Distribution();
        SymbolsPerRank.asTreeMap().putAll(tmSymbolsPerRank.asTreeMap());
        NonSymbolsPerRank = new Distribution();
        NonSymbolsPerRank.asTreeMap().putAll(tmNonSymbolsPerRank.asTreeMap());
        
        MinRank = Math.min((Integer)tmSymbolsPerRank.asTreeMap().firstKey(), 
                (Integer)tmNonSymbolsPerRank.asTreeMap().firstKey());
        MaxRank = Math.max((Integer)tmSymbolsPerRank.asTreeMap().lastKey(), 
                (Integer)tmNonSymbolsPerRank.asTreeMap().lastKey());
    }
    
    
    /** Returns the weight factor of a given rank.
     *@param iRank The rank of interest
     *@return The weight factor of the rank of interest.
     */
    public double getRankWeight(int iRank) {
        return (iRank == 1) ? (SymbolsPerRank.getValue(iRank) + NonSymbolsPerRank.getValue(iRank)) : 
            (SymbolsPerRank.getValue(iRank - 1) + NonSymbolsPerRank.getValue(iRank - 1)) * 
                (SymbolsPerRank.getValue(1) + NonSymbolsPerRank.getValue(1));
    }
    
    /** Returns the weight factor of a given rank, divided by the weight of the first rank.
     *@param iRank The rank of interest.
     *@return The normalized weight factor of the rank of interest.
     */
    public double getRankNormalizedWeight(int iRank) {
        return getRankWeight(iRank) / getRankWeight(1);
    }
    
    /** Returns a weighted number of symbols for a specified rank.
     *@param iRank The rank of interest.
     *@return The weighted number of symbols.
     */
    public double getWeightedSymbols(int iRank) {
        return SymbolsPerRank.getValue(iRank) * getRankWeight(iRank);
    }
    
    /** Returns the number of weighted symbols, summed over a specific range of ranks.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The total of weighted symbols.
     */
    protected double getNumberOfWeightedSymbols(int iMinRank, int iMaxRank) {
        double dSum = 0;
        for (int iCnt=iMinRank; iCnt <= iMaxRank; iCnt++) {
            dSum += getWeightedSymbols(iCnt);
        }
        return dSum;
    }
    
    /** Returns the number of symbols, summed over a specific range of ranks.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The total of symbols
     */
    protected double getNumberOfSymbols(int iMinRank, int iMaxRank) {
        double dSum = 0;
        for (int iCnt=iMinRank; iCnt <= iMaxRank; iCnt++) {
            dSum += SymbolsPerRank.getValue(iCnt);
        }
        return dSum;
    }
    
    /** Returns the number of non-symbols, summed over a specific range of ranks.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The total of non-symbols
     */
    protected double getNumberOfNonSymbols(int iMinRank, int iMaxRank) {
        double dSum = 0;
        for (int iCnt=iMinRank; iCnt <= iMaxRank; iCnt++) {
            dSum += NonSymbolsPerRank.getValue(iCnt);
        }
        return dSum;
    }
    
    /** Returns the number of symbols that would exist in a given rank, if the 
     * total number of weighted symbols and symbols over all Ranks were equal.
     *@param iRank The rank of interest.
     *@return The number of normalized weighted symbols.
     */
    public double getNormWeightedSymbols(int iRank) {
       return getWeightedSymbols(iRank) * getNumberOfSymbols(MinRank, MaxRank) / 
               getNumberOfWeightedSymbols(MinRank, MaxRank);
    }
    
    
    /** Returns the number of symbols that would exist in a given range of ranks, if the 
     * total number of weighted symbols and symbols over all Ranks were equal.
     *@param iMinRank The minimum rank of interest.
     *@param iMaxRank The maximum rank of interest.
     *@return The number of normalized weighted symbols.
     */
    public double getNumberOfNormWeightedSymbols(int iMinRank, int iMaxRank) {
        double dSum = 0;
        for (int iCnt=iMinRank; iCnt <= iMaxRank; iCnt++) {
            dSum += getNormWeightedSymbols(iCnt);
        }
        return dSum;
    }
    
    /** Returns the signal to noise ratio, given as:
     * <P>
     * 10.0 * Math.log10((getNumberOfWeightedSymbols(iMinRank, iMaxRank) + 10e-5) / 
     * (getNumberOfNonSymbols(iMinRank, iMaxRank)))
     * </P>
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The signal to noise ratio.
     */
    public double getSignalToNoise(int iMinRank, int iMaxRank) {
        double dRes = 0;
        double dWeightedSymbols, dNonSymbols;
        dWeightedSymbols = getNumberOfNormWeightedSymbols(iMinRank, iMaxRank);
        dNonSymbols = getNumberOfNonSymbols(iMinRank, iMaxRank);
        
        if (dWeightedSymbols == 0) // No signal
            return Double.NEGATIVE_INFINITY;
        
        dRes = 10.0 * Math.log10((dWeightedSymbols + 10e-5) / dNonSymbols);
        return dRes;
    }
    
    
    /** Returns the minimum rank of n-grams found in the distributions.
     *@return The minimum rank.
     */
    public int getMinRank() {
        return MinRank;
    }
    
    /** Returns the maximum rank of n-grams found in the distributions.
     *@return The maximum rank.
     */
    public int getMaxRank() {
        return MaxRank;
    }
    
    /**
     * Returns the optimal range, as viewed by the signal to noise ratio.
     * 
     * @return A {@linkIntegerPairr} indicating the best minimum and maximum ranks.
     */
    public IntegerPair getOptimalRange() {
        double dBestEval = Double.NEGATIVE_INFINITY;
        int iBestMin = 0, iBestMax = 0;
        System.err.println("MinNGram MaxNGram Performance");
        
        for (int iMinRank=MinRank; iMinRank <= MaxRank; iMinRank++) {
            for (int iMaxRank=iMinRank; iMaxRank <= MaxRank; iMaxRank++) {
                double dEval = getSignalToNoise(iMinRank, iMaxRank);
                
                if (dEval >= dBestEval) {
                    System.err.println(String.format("New optimal: "
                            + "%d %d %10.8f", iMinRank, iMaxRank, dEval));
                    if (dEval == dBestEval)
                    {
                        // Prefer shorter intervals
                        if (iBestMax - iBestMin > iMaxRank - iMinRank) {
                            iBestMin = iMinRank;
                            iBestMax = iMaxRank;
                        }
                    }
                    else
                    {
                        iBestMin = iMinRank;
                        iBestMax = iMaxRank;                        
                        dBestEval = dEval;
                    }
                }
            }            
        }
        
        return new IntegerPair(iBestMin, iBestMax);
    }
}
