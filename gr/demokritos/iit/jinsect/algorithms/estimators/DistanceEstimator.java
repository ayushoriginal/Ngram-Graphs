/*
 * DistanceEstimator.java
 *
 * Created on July 2, 2007, 9:55 AM
 *
 */

package gr.demokritos.iit.jinsect.algorithms.estimators;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;

/**
 *
 * @author ggianna
 */
public class DistanceEstimator {
    protected Distribution SymbolsPerRank;
    protected Distribution NonSymbolsPerRank;
    protected int MinRank, MaxRank;
    protected NGramSizeEstimator Estimator;
    
    /** Creates a new instance of DistanceEstimator, given two distribution of symbols and non-symbols,
     * by getting a copy of the distributions.
     *@param tmSymbolsPerRank The distribution of symbols per n-gram rank.
     *@param tmNonSymbolsPerRank The distribution of non-symbols per n-gram rank.
     */
    public DistanceEstimator(Distribution tmSymbolsPerRank, Distribution tmNonSymbolsPerRank) {
        SymbolsPerRank = new Distribution();
        SymbolsPerRank.asTreeMap().putAll(tmSymbolsPerRank.asTreeMap());
        NonSymbolsPerRank = new Distribution();
        NonSymbolsPerRank.asTreeMap().putAll(tmNonSymbolsPerRank.asTreeMap());
        
        MinRank = Math.min((Integer)tmSymbolsPerRank.asTreeMap().firstKey(), 
                (Integer)tmNonSymbolsPerRank.asTreeMap().firstKey());
        MaxRank = Math.max((Integer)tmSymbolsPerRank.asTreeMap().lastKey(), 
                (Integer)tmNonSymbolsPerRank.asTreeMap().lastKey());
        Estimator = new NGramSizeEstimator(SymbolsPerRank, NonSymbolsPerRank);
    }
    
    /** Creates a new instance of DistanceEstimator, given two distribution of symbols and non-symbols,
     * by getting a copy of the distributions.
     *@param tmSymbolsPerRank The distribution of symbols per n-gram rank.
     *@param tmNonSymbolsPerRank The distribution of non-symbols per n-gram rank.
     *@param nseEstimator An estimator for various n-gram rank cardinalities.
     */
    public DistanceEstimator(Distribution tmSymbolsPerRank, Distribution tmNonSymbolsPerRank,
            NGramSizeEstimator nseEstimator) {
        SymbolsPerRank = new Distribution();
        SymbolsPerRank.asTreeMap().putAll(tmSymbolsPerRank.asTreeMap());
        NonSymbolsPerRank = new Distribution();
        NonSymbolsPerRank.asTreeMap().putAll(tmNonSymbolsPerRank.asTreeMap());
        
        MinRank = Math.min((Integer)tmSymbolsPerRank.asTreeMap().firstKey(), 
                (Integer)tmNonSymbolsPerRank.asTreeMap().firstKey());
        MaxRank = Math.max((Integer)tmSymbolsPerRank.asTreeMap().lastKey(), 
                (Integer)tmNonSymbolsPerRank.asTreeMap().lastKey());
        Estimator = nseEstimator;
    }
    
    /** Returns the symbol to non-symbol percentage given a range of n-gram ranks.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The percentage of symbols to non-symbols.
     */
    public double getSymbolToNonSymbolPercentage(int iMinRank, int iMaxRank) {
        double dWNSymbols = 0, dNonSymbols = 0;
        
        for (int iCnt=iMinRank; iCnt<=iMaxRank; iCnt++) {
            dWNSymbols += Estimator.getNormWeightedSymbols(iCnt);
            dNonSymbols += Estimator.getNumberOfNonSymbols(iCnt, iCnt);
        }
        
        return dWNSymbols / dNonSymbols;
    }
    
    /** Returns the probability of occurence of a non-symbol given a range of n-gram ranks.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The probability of occurence of a non-symbol.
     */
    public double getNonSymbolProbability(int iMinRank, int iMaxRank, int iDistance) {
        double dNoSymbolOverallProb = 0.0; // Init
        double dSymbols = 0;
        double dNonSymbols = 0;
        
        int iRank = iMinRank;
        while ((iDistance < iRank) && (iRank <= iMaxRank))
            iRank++;
        
            dSymbols = Estimator.getNumberOfNormWeightedSymbols(iRank, iRank);
            dNonSymbols = Estimator.getNumberOfNonSymbols(iRank, iRank);
            // Get all symbol prob
            // ((iDistance - iRank) / iRank) is the maximum number of symbols that can be found
            // given rank iRank, and the distance iDistance
        while (iRank <= iMaxRank) {
            dSymbols = Estimator.getNumberOfNormWeightedSymbols(iRank, iMaxRank);
            dNonSymbols = Estimator.getNumberOfNonSymbols(iRank, iMaxRank);
            // Get no symbol prob for rank
            dNoSymbolOverallProb += ((iDistance - iRank) / iRank) * dNonSymbols / 
                    (dSymbols + dNonSymbols);
            iRank++;
        }
        dNoSymbolOverallProb /= (iMaxRank - iMinRank + 1); // Average
        
        return dNoSymbolOverallProb;
    }
    
    /** Returns the probability that, for a given distance, all n-grams in it will be symbols, given
     * a rank range.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@param iDistance The distance (character range) within which we expect n-grams to be found.
     *@return The above described probability.
     */
    public double getAllSymbolProbability(int iMinRank, int iMaxRank, int iDistance) {
        double dAllSymbolOverallProb = 0.0; // Init
        double dSymbols = 0;
        double dNonSymbols = 0;
        
        int iRank = iMinRank;
        while ((iDistance < iRank) && (iRank <= iMaxRank))
            iRank++;
        
        while (iRank <= iMaxRank) {
            dSymbols = Estimator.getNumberOfNormWeightedSymbols(iRank, iRank);
            dNonSymbols = Estimator.getNumberOfNonSymbols(iRank, iRank);
            // Get all symbol prob
            // ((iDistance - iRank) / iRank) is the maximum number of symbols that can be found
            // given rank iRank, and the distance iDistance
            // x2 if symmetric window
            dAllSymbolOverallProb += ((iDistance - iRank) / iRank) * dSymbols / (dSymbols + dNonSymbols);
            iRank++;
        }
        dAllSymbolOverallProb /= (iMaxRank - iMinRank + 1); // Average
        
        return dAllSymbolOverallProb;
    }
    
    /** Returns the expected number of symbols that exist, having a given n-gram rank, within a
     * specified distance (character range).
     *@param iNGramSize The rank of the symbols.
     *@param iDistance The distance (character range) within which we expect n-grams to be found.
     *@return The expected number of symbols.
     */
    private final double getExpectedNumberOfSymbols(int iNGramSize, int iDistance) {
        double dSymbols = Estimator.getNumberOfNormWeightedSymbols(iNGramSize, iNGramSize);
        double dNonSymbols = Estimator.getNumberOfNonSymbols(iNGramSize, iNGramSize) + 10e-5;
        Distribution diNumberOfSymbols = new Distribution();
        for (int iSymbolCnt=0; iSymbolCnt <= iDistance/iNGramSize; iSymbolCnt++) {
            // Should I take into account the current n-gram length?
            double dPsd = 
                    gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation.binomialSuccessProbability(iDistance, iSymbolCnt, dSymbols / (dSymbols + dNonSymbols));
            diNumberOfSymbols.setValue(new Double(iSymbolCnt), dPsd);
        }
        
        // Return the mean number of symbols that can be found.
        return (diNumberOfSymbols.asTreeMap().size() == 0) ? 0 : diNumberOfSymbols.average(false);
    }
    
    /** Returns the expected number of non-symbols that exist, having a given n-gram rank, within a
     * specified distance (character range).
     *@param iNGramSize The rank of the non-symbols.
     *@param iDistance The distance (character range) within which we expect n-grams to be found.
     *@return The expected number of non-symbols.
     */
    private final double getExpectedNumberOfNonSymbols(int iNGramSize, int iDistance) {
        return Math.max(iDistance - getExpectedNumberOfSymbols(iNGramSize, iDistance), 0.0);
    }
    
    /** Returns the signal to noise ratio for a given n-gram rank range.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@param iDistance The distance (character range) within which we expect n-grams to be found.
     *@return The signal to noise.
     */
    public final double getSignalToNoise(int iMinRank, int iMaxRank, int iDistance) {
        return getSignalToNoise(iMinRank, iMaxRank, iDistance, iDistance);
    }
    
    /** Returns the signal to noise ratio for a given n-gram rank range.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@param iDistance The distance (character range) within which we expect n-grams to be found.
     *@return The signal to noise.
     */
    public final double getSignalToNoise(int iMinRank, int iMaxRank, int iDistance, int iCurNGramSize) {
        
        double dSP = 0, dNonSP = 0;
        for (int iCnt = iMinRank; iCnt <= iMaxRank; iCnt++) {
            dSP += getExpectedNumberOfSymbols(iCnt, iDistance);
            dNonSP += getExpectedNumberOfNonSymbols(iCnt, iDistance);
        }
        
        // Also take into account the probability of having chosen a symbol as current n-gram
        //double dSymbols = Estimator.getNumberOfNormWeightedSymbols(iCurNGramSize, iCurNGramSize);
        //double dNonSymbols = Estimator.getNumberOfNonSymbols(iCurNGramSize, iCurNGramSize);
        //double dPCurSymbol = dSymbols / dNonSymbols;
        //dSP *= dPCurSymbol;
        
        if (dSP == 0) // No signal
            return Double.NEGATIVE_INFINITY;
        
        return 10 * Math.log10(dSP / (dNonSP + 10e-5));
    }
    
    /** Returns the distance corresponding to the highest signal to noise ratio for a 
     * given n-gram rank range, and a given distance range to examine. The distance range is 
     * examined exhaustively to find the best distance.
     *@param iMinDist The minimum distance to examine.
     *@param iMaxDist The maximum distance to examine.
     *@param iMinRank The minimum rank to take into account.
     *@param iMaxRank The maximum rank to take into account.
     *@return The optimal distance.
     */
    public int getOptimalDistance(int iMinDist, int iMaxDist, int iMinRank, int iMaxRank) {
        double dBestPerformance = Double.NEGATIVE_INFINITY;
        int iBestDist = -1;
        // DEBUG LINES
        // System.err.println("MinRank MaxRank Dist Performance");
        //////////////
        for (int iCnt=iMinDist; iCnt <= iMaxDist; iCnt++) {
            double dPerf = getSignalToNoise(iMinRank, iMaxRank, iCnt);
            
            // DEBUG LINES
            // System.err.println("-->" + iMinRank + " " + iMaxRank + " " + iCnt + " " + dPerf);
            //////////////
            
            if (dPerf > dBestPerformance) {
                iBestDist = iCnt;
                dBestPerformance = dPerf;
            }
        }
        
        return iBestDist;
    }
    
    /** Returns the distance corresponding to the highest signal to noise ratio for a 
     * given distance range to examine, with respects to ranks identified by the rank estimator.
     * The distance range is examined exhaustively to find the best distance.
     *@param iMinDist The minimum distance to examine.
     *@param iMaxDist The maximum distance to examine.
     *@return The optimal distance.
     */
    public int getOptimalDistance(int iMinDist, int iMaxDist) {
        return getOptimalDistance(iMinDist, iMaxDist, Estimator.getMinRank(), Estimator.getMaxRank());
    }
}
