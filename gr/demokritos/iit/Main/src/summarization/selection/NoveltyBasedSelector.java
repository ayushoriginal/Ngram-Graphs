/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.summarization.selection;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramGraphComparator;
import java.io.InvalidClassException;
import java.util.List;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.events.IObjectFilter;
import gr.demokritos.iit.jinsect.events.IdentityObjectFilter;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Chooses sentences based on the novelty of the provided information, 
 * given an existing information document graph.
 *
 * @author pckid
 */
public class NoveltyBasedSelector<TSentenceType,TTokenType> implements 
        ISentenceSelector<TSentenceType,TTokenType> {

    /** The representation of pre-existing information */
    // protected DocumentNGramGraph PreexistingInfo;
    
    /** Pre-existing information text. */
    protected String PreexistingText;

    /**
     * N-gram parameters.
     */
    protected int MinNGram, MaxNGram, Dist;

    /** Comparator for the representations. */
    public SimilarityComparatorListener Comparator;
    
    /** Representation filter. If not null, then the graph representation of
     * each sentence passes through the filter before being used.
     */
    public IObjectFilter<DocumentNGramGraph> SentenceRepresentationFilter;
    
    /** The maximum number of sentences to select. */
    public int MaxSentencesSelected = Integer.MAX_VALUE;
    
    public NoveltyBasedSelector() {
        // PreexistingInfo = new DocumentNGramSymWinGraph();
        Comparator = new NGramCachedGraphComparator();
        SentenceRepresentationFilter = new 
                IdentityObjectFilter<DocumentNGramGraph>();
        PreexistingText = "";
        MinNGram = 3;
        MaxNGram = 3;
        Dist = 3;
    }
    
    public NoveltyBasedSelector(int iMinNGram, int iMaxNGram, int iDist) {
        // PreexistingInfo = new DocumentNGramSymWinGraph();
        Comparator = new NGramCachedGraphComparator();
        SentenceRepresentationFilter = new
                IdentityObjectFilter<DocumentNGramGraph>();
        PreexistingText = "";
        MinNGram = iMinNGram;
        MaxNGram = iMaxNGram;
        Dist = iDist;
    }

    public NoveltyBasedSelector(String sPreexistingText) {
        // PreexistingInfo = new DocumentNGramSymWinGraph();
        initComparator();
        SentenceRepresentationFilter = new 
                IdentityObjectFilter<DocumentNGramGraph>();
        // Update default text
        PreexistingText = sPreexistingText;
    }
    
    public NoveltyBasedSelector(String sPreexistingText, int iMinNGram,
            int iMaxNGram, int iDist) {
        // PreexistingInfo = new DocumentNGramSymWinGraph();
        initComparator();
        SentenceRepresentationFilter = new
                IdentityObjectFilter<DocumentNGramGraph>();
        // Update default text
        PreexistingText = sPreexistingText;
        MinNGram = iMinNGram;
        MaxNGram = iMaxNGram;
        Dist = iDist;
    }

    /** Makes sure the comparator is not null. */
    protected void initComparator() {
        if (Comparator  == null)
            Comparator = new NGramCachedGraphComparator();
    }
    
    /** Makes sure the representation filter is not null. */
    protected void initSentenceRepresentationFilter() {
        if (SentenceRepresentationFilter  == null)
            SentenceRepresentationFilter = new IdentityObjectFilter<DocumentNGramGraph>();
    }
    
    /** Replaces pre-existing info with a new set of information.
     * @param dngNewInfo The new information.
     */
    public void setPreexistingText(String sNewPreexistingText) {
        PreexistingText = sNewPreexistingText;
    }
    
/*    @Override
    public List<TSentenceType> selectFromSentences(List<TSentenceType> sSentences) {
        ArrayList<TSentenceType> Sentences = new ArrayList<TSentenceType>(sSentences);
        // Init current graph
        DocumentNGramGraph CurrentInfo = new DocumentNGramSymWinGraph();      
        
        ArrayList alRes = new ArrayList();
        // Check for empty sentence set
        if (Sentences.size() == 0)
            return alRes;
        
        // Verify filter is used
        initSentenceRepresentationFilter();
        // Verify comparator is used
        initComparator();
        
        // Get 1st sentence
        alRes.add(Sentences.get(0));
        // DEBUG LINES
        System.err.print("Updating existing info...");
        //////////////
        // Update preexisting info
        CurrentInfo.setDataString(PreexistingText +
                Sentences.get(0));
        // Filter representation if required
        CurrentInfo = SentenceRepresentationFilter.filter(CurrentInfo);
        Sentences.remove(0);
        // DEBUG LINES
        System.err.println(String.format("Done. %d sentences remaining.", 
                (MaxSentencesSelected != Integer.MAX_VALUE) ? 
                    MaxSentencesSelected - alRes.size() : Sentences.size()));
        //////////////
        
        HashMap<TSentenceType,DocumentNGramGraph> filteredRepCache = new
                HashMap<TSentenceType,DocumentNGramGraph>();
        
        // While not all sentences have been processed
        while ((!Sentences.isEmpty()) && (alRes.size() < MaxSentencesSelected)){
            double dMinSim = Double.MAX_VALUE;
            // DocumentNGramGraph gBest = null;
            TSentenceType sBest = null;
            
            // For every sentence left in the set
            for (TSentenceType s : Sentences) {
                // lookup or extract its graph
                DocumentNGramGraph gTemp = null;
                if (!filteredRepCache.containsKey(s)) {
                    gTemp = new DocumentNGramGraph();
                    gTemp.setDataString(s.toString());
                    gTemp = SentenceRepresentationFilter.filter(gTemp);
                    filteredRepCache.put(s, gTemp);
                }
                else
                    gTemp = filteredRepCache.get(s);
                // DEBUG LINES
                System.err.print(".");
                //////////////
                
                // compare to pre-existing info
                double dSim = Double.MAX_VALUE;
                try {
                    dSim = Comparator.getSimilarityBetween(CurrentInfo, 
                            gTemp).getOverallSimilarity();
                } catch (InvalidClassException ex) {
                    Logger.getLogger(NoveltyBasedSelector.class.getName()).log(Level.SEVERE, 
                            null, ex);
                }
                // Select, if minimum similarity sentence
                if (dSim < dMinSim) {
                    dMinSim = dSim;
                    //gBest = gTemp;
                    sBest = s;
                }
            }
            // Add sentence to list
            alRes.add(sBest);
            // Remove from hash and cache, as processed
            Sentences.remove(sBest);
            filteredRepCache.remove(sBest);
            // DEBUG LINES
            System.err.print("Updating existing info...");
            //////////////
            // Update preexisting info
            CurrentInfo.setDataString(utils.printIterable(alRes, ""));
            // Filter representation if required
            CurrentInfo = SentenceRepresentationFilter.filter(CurrentInfo);
            
            // DEBUG LINES
            System.err.println(String.format("Done. %d sentences remaining.", 
                    (MaxSentencesSelected != Integer.MAX_VALUE) ? 
                        MaxSentencesSelected - alRes.size() : Sentences.size()));
            //////////////
        }
        
        // Return resulting list
        return alRes;
    }
*/
    @Override
    public List<TSentenceType> selectFromSentences(List<TSentenceType> sSentences) {
        // Init copy list
        List<TSentenceType> Sentences = new ArrayList<TSentenceType>(sSentences);
        
        // Init empty list
        List<TSentenceType> alRes = new ArrayList<TSentenceType>();
        // Init current graph
        DocumentNGramGraph CurrentInfo = new DocumentNGramSymWinGraph(MinNGram,
                MaxNGram, Dist);
        // Check for empty sentence set
        if (Sentences.size() == 0)
            return alRes;
        // Verify filter is used
        initSentenceRepresentationFilter();
        // Verify comparator is used
        initComparator();
        

        // Rank sentences according to similarity to existing current graph
        // Get 1st sentence
        alRes.add(Sentences.get(0));
        // DEBUG LINES
        System.err.print("Updating existing info...");
        //////////////
        // Update preexisting info
        CurrentInfo.setDataString(PreexistingText +
                Sentences.get(0));
        // Filter representation if required
        CurrentInfo = SentenceRepresentationFilter.filter(CurrentInfo);
        Sentences.remove(0);
        // DEBUG LINES
        System.err.println(String.format("Done. %d sentences remaining.", 
                (MaxSentencesSelected != Integer.MAX_VALUE) ? 
                    MaxSentencesSelected - alRes.size() : Sentences.size()));
        //////////////
        
        HashMap<TSentenceType,DocumentNGramGraph> filteredRepCache = new
                HashMap<TSentenceType,DocumentNGramGraph>();
        // While not all sentences have been processed
        while ((!Sentences.isEmpty()) && (alRes.size() < MaxSentencesSelected)){
            
            // Sentences score distribution
            Distribution<TSentenceType> dSentences = new 
                    Distribution<TSentenceType>();
            // Initialize score values giving highest score to higher 
            // rank in list
            double dOriginalRank = Sentences.size();
            for (TSentenceType s : Sentences) {
                dOriginalRank--;
                dSentences.setValue(s, dOriginalRank);
            }
        
            // Redundancy distribution
            Distribution<TSentenceType> redundancy = new 
                    Distribution<TSentenceType>();
            
            // Every sentence left
            for (TSentenceType s : Sentences) {
                // lookup or extract its graph
                DocumentNGramGraph gTemp = null;
                if (!filteredRepCache.containsKey(s)) {
                    gTemp = new DocumentNGramSymWinGraph(MinNGram,
                        MaxNGram, Dist);
                    gTemp.setDataString(s.toString());
                    gTemp = SentenceRepresentationFilter.filter(gTemp);
                    filteredRepCache.put(s, gTemp);
                }
                else
                    gTemp = filteredRepCache.get(s);
                // DEBUG LINES
                System.err.print(".");
                //////////////
                // compare to pre-existing info
                double dSim = Double.MAX_VALUE;
                try {
                    if (!(Comparator instanceof NGramGraphComparator))
                        dSim = Comparator.getSimilarityBetween(CurrentInfo, 
                            gTemp).getOverallSimilarity();
                    else {
                        GraphSimilarity gTmp = (GraphSimilarity)
                            Comparator.getSimilarityBetween(CurrentInfo, 
                            gTemp);
                        dSim = gTmp.ValueSimilarity / gTmp.SizeSimilarity;
                    }
                } catch (InvalidClassException ex) {
                    Logger.getLogger(NoveltyBasedSelector.class.getName()).log(Level.SEVERE, 
                            null, ex);
                }
                redundancy.setValue(s, dSim);
            }
            
            // Update sentence ranks, using redundancy
            // Higher redundancy is offered minimum score
            while (!redundancy.asTreeMap().isEmpty()) {
                double dRedundancyRank = redundancy.asTreeMap().size();

                // Find next sentence of maximum redundancy
                TSentenceType s = redundancy.getKeyOfMaxValue();
                dSentences.setValue(s, -dRedundancyRank);
                // Remove maximum value from set
                redundancy.asTreeMap().remove(s);
            }

            // DEBUG LINES
            System.err.println("\n---Novelty detection:\n");
            for (TSentenceType s: dSentences.asTreeMap().keySet())
                System.err.println(String.format("%6.4f\t%s",
                    dSentences.getValue(s), s.toString()));
            //////////////
                    
            // Get best scoring sentence
            TSentenceType sBest = (TSentenceType)dSentences.getKeyOfMaxValue();
            // Add sentence to list
            alRes.add(sBest);
            // Remove from hash and cache, as processed
            Sentences.remove(sBest);
            filteredRepCache.remove(sBest);
            // DEBUG LINES
            System.err.print("Updating existing info...");
            //////////////
            // Update preexisting info
            CurrentInfo.setDataString(PreexistingText + 
                    utils.printIterable(alRes, ""));
            // Filter representation if required
            CurrentInfo = SentenceRepresentationFilter.filter(CurrentInfo);
            // DEBUG LINES
            System.err.println(String.format("Done. %d sentences remaining.", 
                    (MaxSentencesSelected != Integer.MAX_VALUE) ? 
                        MaxSentencesSelected - alRes.size() : Sentences.size()));
            //////////////
        }        
        
        System.err.println("Sentence novelty enhanced ordering");
        return alRes;
    }   
    
    @Override
    public boolean selectSentence(List<TTokenType> tokens) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double sentenceSelectionConfidence(List<TTokenType> tokens) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    

}
