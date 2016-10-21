/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.summarization.selection;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedNonSymmGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramGraphComparator;
import java.io.InvalidClassException;
import java.util.List;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.events.IdentityObjectFilter;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author pckid
 */
public class RedundancyBasedSelector<TSentenceType,TTokenType> extends 
        NoveltyBasedSelector<TSentenceType,TTokenType> {

    /** The threshold value over which sentences are considered redundant
     */
    public double RedundancyThreshold;
    
    /** Makes sure the comparator is not null. */
    @Override
    protected void initComparator() {
        if (Comparator  == null)
            Comparator = new NGramCachedNonSymmGraphComparator();
    }

    /**
     * Creates a RedundancyBasedSelector, with a given text string as
     * preexisting text. Uses a predefined redundancy threshold of 0.4 for the
     * removal of redundant sentences.
     * @param sPreexistingText
     * @param iMinNGram The minimum n-gram to take into account for graphs.
     * @param iMaxNGram The maximum n-gram to take into account for graphs.
     * @param iDist The neighbourhood distance to use in graphs.
     */
    public RedundancyBasedSelector(String sPreexistingText, int iMinNGram,
            int iMaxNGram, int iDist) {
        // PreexistingInfo = new DocumentNGramSymWinGraph();
        Comparator = new NGramCachedNonSymmGraphComparator();
        SentenceRepresentationFilter = new 
                IdentityObjectFilter<DocumentNGramGraph>();
        // Update default text
        PreexistingText = sPreexistingText;
        RedundancyThreshold = 0.4;

        MinNGram = iMinNGram;
        MaxNGram = iMaxNGram;
        Dist = iDist;
    }

    /**
     * Creates a RedundancyBasedSelector, with a given text string as
     * preexisting text. Also defines the redundancy threshold for removal
     * of redundant sentences.
     * @param sPreexistingText The text to use as preexisting information.
     * @param iMinNGram The minimum n-gram to take into account for graphs.
     * @param iMaxNGram The maximum n-gram to take into account for graphs.
     * @param iDist The neighbourhood distance to use in graphs.
     * @param dRedundancyThreshold The threshold value over which sentences
     * are considered redundant and are removed.
     */
    public RedundancyBasedSelector(String sPreexistingText, int iMinNGram,
            int iMaxNGram, int iDist, double dRedundancyThreshold) {
        // PreexistingInfo = new DocumentNGramSymWinGraph();
        Comparator = new NGramCachedNonSymmGraphComparator();
        SentenceRepresentationFilter = new
                IdentityObjectFilter<DocumentNGramGraph>();
        // Update default text
        PreexistingText = sPreexistingText;
        RedundancyThreshold = dRedundancyThreshold;
        
        MinNGram = iMinNGram;
        MaxNGram = iMaxNGram;
        Dist = iDist;
    }

    @Override
    public List<TSentenceType> selectFromSentences(List<TSentenceType> sSentences) {
        // Init copy list
        List<TSentenceType> Sentences = new ArrayList<TSentenceType>(sSentences);
        
        // Init empty list
        List<TSentenceType> alRes = new ArrayList<TSentenceType>();
        // Init current graph
        DocumentNGramGraph CurrentInfo = new DocumentNGramSymWinGraph(MinNGram,
                MaxNGram, Dist);
        // Update preexisting info
        CurrentInfo.setDataString(PreexistingText);
        // Filter representation if required
        CurrentInfo = SentenceRepresentationFilter.filter(CurrentInfo);
        
        // Check for empty sentence set
        if (Sentences.size() == 0)
            return alRes;
        // Verify filter is used
        initSentenceRepresentationFilter();
        // Verify comparator is used
        initComparator();
        

        HashMap<TSentenceType,DocumentNGramGraph> filteredRepCache = new
                HashMap<TSentenceType,DocumentNGramGraph>();
        
        // DEBUG LINES
        System.err.print("Detecting redundancy...");
        
        // For every sentence
        Distribution<Double> redundancy = new Distribution<Double>();
        List<TSentenceType> lSentences = new ArrayList(Sentences);
        ListIterator<TSentenceType> iSentence = lSentences.listIterator(
                lSentences.size());
        while (iSentence.hasPrevious()) {
            TSentenceType sSentence = iSentence.previous();
            
            // lookup or extract its graph
            DocumentNGramGraph gTemp = null;
            if (!filteredRepCache.containsKey(sSentence)) {
                gTemp = new DocumentNGramSymWinGraph(MinNGram,
                    MaxNGram, Dist);
                gTemp.setDataString(sSentence.toString());
                gTemp = SentenceRepresentationFilter.filter(gTemp);
                filteredRepCache.put(sSentence, gTemp);
            }
            else
                gTemp = filteredRepCache.get(sSentence);
            // DEBUG LINES
            System.err.print(".");
            //////////////
            
            double dMaxSim = Double.NEGATIVE_INFINITY;
            // Take into account pre-existing info, if existent
            if (PreexistingText.length() > 0)
            try {
                double dSim = Double.MAX_VALUE;
                if (!(Comparator instanceof NGramGraphComparator))
                    dSim = Comparator.getSimilarityBetween(CurrentInfo,
                        gTemp).getOverallSimilarity();
                else {
                    GraphSimilarity gTmp = (GraphSimilarity)
                        Comparator.getSimilarityBetween(CurrentInfo,
                        gTemp);
                    dSim = gTmp.ContainmentSimilarity;
                }

                // Indicate removal status, if needed
                if (dSim > RedundancyThreshold) {
                    dMaxSim = dSim;
                    break;
                }
            } catch (InvalidClassException ex) {
                Logger.getLogger(RedundancyBasedSelector.class.getName()).log(Level.SEVERE,
                        null, ex);
            }

            
            // If not marked for removal
                        // Compare to all other sentences
            ListIterator<TSentenceType> iSecSentence =
                    lSentences.listIterator();
            if (dMaxSim <= RedundancyThreshold) {
                while (iSecSentence.hasNext()) {
                    TSentenceType sSecSentence = iSecSentence.next();
                    if (sSecSentence.equals(sSentence))
                        continue;

                    DocumentNGramGraph gSecTemp = null;
                    if (!filteredRepCache.containsKey(sSecSentence)) {
                        gSecTemp = new DocumentNGramSymWinGraph(MinNGram,
                            MaxNGram, Dist);
                        gSecTemp.setDataString(sSecSentence.toString());
                        gSecTemp = SentenceRepresentationFilter.filter(gSecTemp);
                        filteredRepCache.put(sSecSentence, gSecTemp);
                    }
                    else
                        gSecTemp = filteredRepCache.get(sSecSentence);

                    double dSim = Double.NEGATIVE_INFINITY;
                    try {
                        if (!(Comparator instanceof NGramGraphComparator))
                            dSim = Comparator.getSimilarityBetween(gSecTemp,
                                gTemp).getOverallSimilarity();
                        else {
                            GraphSimilarity gTmp = (GraphSimilarity)
                                Comparator.getSimilarityBetween(gSecTemp,
                                gTemp);
                            dSim = gTmp.ContainmentSimilarity;
                        }
                    } catch (InvalidClassException ex) {
                        Logger.getLogger(NoveltyBasedSelector.class.getName()).log(Level.SEVERE,
                                null, ex);
                    }

                    redundancy.increaseValue(dSim, 1.0);
                    // Indicate removal status, if needed
                    if (dSim > RedundancyThreshold) {
                        dMaxSim = dSim;
                        break;
                    }
                }
            }

            // Remove sentence if redundant
            if (dMaxSim > RedundancyThreshold) {
                iSentence.remove();
                // DEBUG LINES
                System.err.print("D");
                //////////////
            }

        }

        System.err.println("Done.");
        
        System.err.println(String.format("Redundancy mean %8.2f and sd %8.2f." +
                "Distribution seems %snormal.",
                redundancy.average(false), redundancy.standardDeviation(false),
                redundancy.isNormal(false, 0.10) ? "" : "not "));
        
        // Return remaining sentences
        alRes = lSentences;
        
        System.err.println("Sentence redundancy enhanced ordering");
        return alRes;
    }   

}
