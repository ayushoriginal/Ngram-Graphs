/*
 * EntropyChunker.java
 *
 * Created on March 31, 2008, 2:43 PM
 *
 */

package gr.demokritos.iit.summarization.analysis;

import gr.demokritos.iit.conceptualIndex.documentModel.SymbolicGraph;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.algorithms.nlp.IChunker;
import gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.utils;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/** This class can separate a token sequence into chunks, based on the entropy of the
 * following symbol.
 *
 * @author ggianna
 * @licence LGPL
 */
public class EntropyChunker implements Serializable, IChunker {
    
    /** The graph containing symbol sequence information.
     */
    SymbolicGraph sgOverallGraph;
    EdgeCachedLocator clLocator;
    SortedMap smDelims;
    
    /** {@link Serializable} interface implementer. */
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
        out.writeObject(sgOverallGraph); // Store graph
        out.writeObject(smDelims); // Store delims
    }

    /** {@link Serializable} interface implementer. */
    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
        // Load graph
        sgOverallGraph = (SymbolicGraph)in.readObject();
        // Load delims
        smDelims = (SortedMap)in.readObject();
        // Re-init cache
        clLocator = new EdgeCachedLocator(100);
    }
    
    /** {@link Serializable} interface implementer. */
    private void readObjectNoData()
      throws ObjectStreamException {
        return; // Do nothing
    }
    
    /** Creates a new instance of EntropyChunker. */
    public EntropyChunker() {
        sgOverallGraph = new SymbolicGraph(1,1);
        // Re-init cache
        clLocator = new EdgeCachedLocator(100);
        smDelims = null;
    }
    
    /** Train the statistics of the chunker from a given file set.
     *@param sFiles The set of {@link CategorizedFileEntry} objects to use for
     *training.
     */
    public void train(Set<String> sFileNames) {
        Iterator<String> iFile = sFileNames.iterator();
        while (iFile.hasNext()) {
            String sText = utils.loadFileToString(iFile.next());
            train(sText);
        }
    }
    
    /** Train the statistics of the chunker from a given text.
     *@param sTrainingText The text that defines the statistics used by the
     * chunker.
     */
    public void train(String sTrainingText) {
        // Update graph
        sgOverallGraph.setDataString(sTrainingText + (new StringBuffer().append((char)StreamTokenizer.TT_EOF)).toString());
        // Reset cache
        clLocator.resetCache();
        // Calculate delimiters
        getDelimiters();
    }
    
    /** Clears list of delimiters determined. */
    public void clearDelimiters() {
        smDelims = null;
    }
    
    /** Returns a sorted map of delimiters, based on their entropy of next character measure.
     *@return The {@link SortedMap} of Delimiters, where each delimiter is matched to its entropy measure.
     */
    public SortedMap getDelimiters() {
        // If extracted then return a copy
        if (smDelims != null)
            return new TreeMap(smDelims);
        
        // Else extract
        smDelims = identifyCandidateDelimiters(sgOverallGraph.getDataString(), 1);
        int iImportant = determineImportantDelimiters(smDelims);
        Iterator iIter = smDelims.keySet().iterator();
        int iCnt = 0;
        while (iIter.hasNext() && (iCnt++ < smDelims.size() - iImportant))
            iIter.next();
        
        smDelims = smDelims.tailMap(iIter.next());
        if (!smDelims.containsValue(StreamTokenizer.TT_EOF)) {
            smDelims.put((Double)smDelims.lastKey() + 0.1, new StringBuffer().append((char)StreamTokenizer.TT_EOF).toString()); // Add EOF char
        }
        
        // Return copy of delims
        return new TreeMap(smDelims);
    }
    
    /** Returns a list of string chunks, derived from a given string.
     *@param sToChunk The string to chunk.
     *@return A {@link List} of strings that are the chunks of the given string.
     */
    @Override
    public List chunkString(String sToChunk) {
        Integer[] iRes = splitPointsByDelimiterList(sToChunk, getDelimiters());
        String[] sRes = splitStringByDelimiterPoints(sToChunk, iRes);
        return Arrays.asList(sRes);
    }
    
    /* Returns a list of indices concerning possible split points.
     *@param sStr The string to split.
     *@param lDelimiters A {@link SortedMap} of delimiter strings.
     *@return An array of integers, indicating the split points for the given
     * string.
     */
    protected Integer[] splitPointsByDelimiterList(String sStr, SortedMap lDelimiters) {
        ArrayList alRes = new ArrayList();
        TreeMap lLocal = new TreeMap();
        lLocal.putAll(lDelimiters);
        
        // For every candidate delimiter
        while (lLocal.size() > 0) {
            Object oNext = lLocal.lastKey();
            // Get all split points
            int iNextSplit = 0;
            int iLastSplit = 0;
            while ((iNextSplit = sStr.indexOf((String)lDelimiters.get(oNext), iLastSplit)) > -1) {
                // TODO : Check
                alRes.add(new Integer(iNextSplit + ((String)lDelimiters.get(oNext)).length()));
                iLastSplit = iNextSplit + 1;
            }
            
            lLocal.remove(oNext);
        }
        Integer [] iaRes = new Integer[alRes.size()];
        alRes.toArray(iaRes);
        gr.demokritos.iit.jinsect.utils.bubbleSortArray(iaRes);
        
        return iaRes;
    }
    
    /** Returns the substrings defined by a string and a set of split points.
     *@param sStr The string to split.
     *@param iRes An array of integers, indicating the points at which the string
     * is to be split.
     *@return An array of sub-strings of the given string.
     */
    protected static String[] splitStringByDelimiterPoints(String sStr, Integer[] iRes) {
        ArrayList alRes = new ArrayList();
        
        // For every split point get substring
        for (int iCnt=0; iCnt < iRes.length; iCnt++) {
            if (iCnt == 0)
                alRes.add(sStr.substring(0, iRes[iCnt]));
            else
                alRes.add(sStr.substring(iRes[iCnt - 1], iRes[iCnt]));
        }
        // Add last part
        if (iRes.length > 0)
            alRes.add(sStr.substring(iRes[iRes.length - 1]));
        else
            alRes.add(sStr); // No splitting
        
        String[] sRes = new String[alRes.size()]; // n split points => n+1 string parts
        alRes.toArray(sRes);
        
        return sRes;
    }
    
    /** Returns a list of indices concerning possible split points.
     *@param sStr The string to analyse.
     *@param lDelimiters An array of delimiting characters.
     *@return An array of integers, indicating split points in the given string.
     */
    private Integer[] splitPointsByDelimiterList(String sStr, char[] lDelimiters) {
        TreeMap tmDels = new TreeMap();
        for (int iCnt=0; iCnt < lDelimiters.length; iCnt++)
            tmDels.put(iCnt, new String() + lDelimiters[iCnt]);
        
        return splitPointsByDelimiterList(sStr, tmDels);
    }
    
    /** Returns the entropy of the next character, given a head string.
     *@param sStr The head string.
     *@return A double indicating the entropy of the next character.
     */
    private double getEntropyOfNextChar(String sStr) {
        return getEntropyOfNextChar(sStr, false);
    }
    
    /** Returns the entropy of the next character, given a head string. Normalizes
     *the value if required.
     *@param sStr The head string.
     *@return A double indicating the entropy of the next character.
     */
    private final double getEntropyOfNextChar(String sStr, boolean bNormalized) {
        double dRes = 0.0;
        
        // Look-up current n-gram
        Vertex vStrNode = clLocator.locateVertexInGraph(sgOverallGraph, new VertexImpl(sStr));
        if (vStrNode == null)
            return dRes; // Ignore inexistent symbols
        
        // else get outgoing edges
        List lEdges = gr.demokritos.iit.jinsect.utils.getOutgoingEdges(sgOverallGraph, vStrNode);
        Iterator iEdgeIter = lEdges.iterator();
        Distribution dDist = new Distribution();
        if (lEdges.size() > 0) {
            while (iEdgeIter.hasNext()) {
                WeightedEdge weCur = (WeightedEdge)iEdgeIter.next();
                if ( Double.isNaN(weCur.getWeight()))
                    System.err.println("WARNING: Not a number edge weight for edge:" + weCur.toString());
                dDist.setValue(weCur.toString(), weCur.getWeight());
            }
            
            dDist.normalizeToSum();
            if (bNormalized) {
                // Calc NORMALIZED entropy - entropy to the number of appearences
                double dLogOccurences = (Math.log(dDist.calcTotalValues()) / Math.log(2));
                dRes = statisticalCalculation.entropy(dDist) / dLogOccurences;
            } else
                // Calc entropy
                dRes = statisticalCalculation.entropy(dDist);
        }
        if ( Double.isNaN(dRes))
            System.err.println("WARNING: Not a number entropy for symbol:" + vStrNode);
        return dRes;
    }
    
    protected int determineImportantDelimiters(SortedMap smMap) {
        Iterator iIter = smMap.keySet().iterator();
        // Distribution dEntropyDist = new Distribution();
        // Distribution dEntropyDeltaDist = new Distribution();
        Distribution dDist = new Distribution();
        Distribution dReverse = new Distribution();
        
        // Get first number
        Double dPrv = Double.NEGATIVE_INFINITY;
        Double dTwoPrv = Double.NEGATIVE_INFINITY;
        
        // Create corresponding distribution
        while (iIter.hasNext()) {
            Double oNext = (Double)iIter.next();
            if ((dPrv != Double.NEGATIVE_INFINITY) && (dTwoPrv != Double.NEGATIVE_INFINITY)) {
                if (oNext.isNaN())
                    System.err.println("WARNING: Encountered NaN. Ignoring...");
                // dEntropyDeltaDist.asTreeMap().put(smMap.get(oNext), (oNext - dPrv)); // Get distance from previous data point
                // dEntropyDist.asTreeMap().put(smMap.get(oNext), oNext); // Get position of current data point
                
                dDist.setValue(dPrv, dPrv * Math.abs(dPrv-dTwoPrv-oNext+dPrv)); // Detect peaks
                dReverse.setValue(dPrv * Math.abs(dPrv-dTwoPrv-oNext+dPrv), dPrv);
            }
            
            dTwoPrv = dPrv;
            dPrv = oNext;
        }
        
        // DEBUG LINES
//        System.err.println("Symbol\tEntropy");
//        for (Iterator iEntropies = smMap.keySet().iterator();
//            iEntropies.hasNext();) {
//            Object o = iEntropies.next();
//            String sSymbol = (String)smMap.get(o);
//            try {
//                sSymbol = URLEncoder.encode(sSymbol, "utf8");
//            } catch (UnsupportedEncodingException ex) {
//                sSymbol = "(NotPrintable)";
//                ex.printStackTrace(System.err);
//            }
//            System.err.println(o.toString() + "\t" + sSymbol);
//        }
        //////////////
        
        double dVar = dDist.variance(true);
        double dMean = dDist.average(true);
        
        // return getDelimiterIndexByThreshold(smMap, Math.min(dMean + Math.abs(dVar), dDist.maxValue()));
        return getDelimiterIndexByThreshold(smMap, dReverse.getValue(dDist.maxValue()));
    }
    
    private final int getDelimiterIndexByThreshold(SortedMap smMap, double dThreshold) {
        // Locate delim in map
        Iterator iIter = smMap.keySet().iterator();
        int iCnt = 0;
        while (iIter.hasNext()) {
            if ((Double)iIter.next() > dThreshold)
                break;
            iCnt++;
        }
        // Indicate index
        return smMap.size() - iCnt + 1;
        
    }
    
    /** Returns a sorted map of candidate delimiters for a given string and a given
     * n-gram size.
     *@param sStr The string to analyse to identify the candidate delimiters.
     *@param iNGramSize The n-gram size of the delimiters to extract.
     *@return The sorted map of delimiters, sorted by their entropy of next character.
     */
    private final SortedMap identifyCandidateDelimiters(String sStr, int iNGramSize) {
        String sSubStr = null;
        Integer[] iRes = null;
        ArrayList alRes = new ArrayList();
        TreeMap tmRes = new TreeMap();
        
        for (int iCnt = 0; iCnt <= sStr.length() - iNGramSize; iCnt++) {
            if (iCnt + iNGramSize > sStr.length())
                continue;
            // Get n-gram
            sSubStr = sStr.substring(iCnt, iCnt + iNGramSize);
            if (tmRes.containsValue(sSubStr))   // Ignore duplicates
                continue;
            
            // Look-up current n-gram
            Vertex vStrNode = clLocator.locateVertexInGraph(sgOverallGraph, new VertexImpl(sSubStr));
            if (vStrNode == null)
                continue; // Ignore inexistent symbols
            // double dNormEntropy = getEntropyOfNextChar(sSubStr, true);
            double dEntropy = getEntropyOfNextChar(sSubStr, false);
            // tmRes.put(dNormEntropy, sSubStr);
            tmRes.put(dEntropy, sSubStr);
            
        }
        
        return tmRes;
    }
    
    /** Utility method. Used for testing purposes. */
    public static void main(String[] sArgs) {
        String sText = "this is a test text. Indeed, this previous text is nothing but a test. " +
                "What do you think you should do? I would try it once more by testing...";
        EntropyChunker ec = new EntropyChunker();
        ec.train(sText);
        Iterator iIter = ec.chunkString("OK. Now where do I do the splitting? Here, or here? We shall see.").iterator();
        while (iIter.hasNext()) {
            System.out.println(iIter.next().toString());
        }
        
    }
}
