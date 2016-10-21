/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.summarization.analysis;

import gr.demokritos.iit.conceptualIndex.documentModel.DistributionDocument;
import gr.demokritos.iit.conceptualIndex.documentModel.SymbolicGraph;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.console.StatusConsole;
import gr.demokritos.iit.jinsect.gui.IStatusDisplayer;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.utils;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ggianna
 */
public class SymbolExtractor {
    protected SymbolicGraph sgOverallGraph;
    protected Set<String> Symbols = null;

    public SymbolExtractor() {
    }

    public void setSymbols(Set<String> sSymbols) {
        this.Symbols = sSymbols;
    }
    public Set<String> getSymbols() {
        return Symbols;
    }

    public Set<String> getSymbols(List<String> sStrings, final IStatusDisplayer fStatus) {
        if (Symbols != null)
            return Symbols;
        else
            Symbols = calcSymbols(sStrings, fStatus);

        return Symbols;
    }

    public final static String SYMBOLS_CATEGORY = "SymbolFile";
    public final static String DISTROS_CATEGORY = "DistroFile";

    /** Returns the probability of generation of an exact given string, based on the alphabet of the
     * Symbolic Graph, and given that there the generation process is random.
     *@param sString The string for which the probability of appearence is to be calculated.
     *@return The probability of appearence of the given string.
     */
    private double getProbabilityOfStringInRandomText(String sPrefix, String sSuffix) {
        double dRes = 0.0;

        // Look directly in the data string of the symbolic graph
        int iPrefixCount = 0;
        int iLastOccurence = -1;

        if (sPrefix.length() == 0) // Check for empty string
            return 1.0;

        while ((iLastOccurence = sgOverallGraph.getDataString().indexOf(sPrefix, iLastOccurence + 1))
            > -1)
            iPrefixCount++;

        // Consider that the occurence of a random suffix is a proportion of the prefix occurences,
        // as indicated by the probability of the given suffix, as a random selection process of n
        // symbols from the symbol set.
        int iFullStringCount = (int)Math.ceil((double)iPrefixCount *
                Math.pow(1.0 / sgOverallGraph.getAlphabet().size(), sSuffix.length()));
        // Consider p(sPrefix) = N(sPrefix) / (length(Text) / length(sPrefix))
        double pPrefix = (double) iPrefixCount /
                (sgOverallGraph.getDataString().length() / sPrefix.length());

        String sFullString = sPrefix + sSuffix;
        double pJoined = (double) iFullStringCount /
                (sgOverallGraph.getDataString().length() / sFullString.length());
        // Return p(sPrefix) * p(sPrefix, sSuffix)
        dRes = (iPrefixCount == 0) ? 0.0 : pPrefix * pJoined;
        return dRes;
    }

    /** Returns the probability of occurence of a given suffix, given a prefix, within the data string
     *of the Symbolic Graph.
     *@param sPrefix The prefix required.
     *@param sSuffix The suffix for which the probability of occurence is to be calculated, given the
     * prefix.
     *@return The probability of occurence of the suffix, given the prefix.
     */
    private double getProbabilityOfStringInText(String sPrefix, String sSuffix) {
        double dRes = 0.0;

        // Look directly in the data string of the symbolic graph
        int iPrefixCount = 0;
        int iLastOccurence = -1;

        if (sPrefix.length() == 0) // Check for empty string
            return 1.0;

        while ((iLastOccurence = sgOverallGraph.getDataString().indexOf(sPrefix, iLastOccurence + 1))
            > -1)
            iPrefixCount++;

        String sFullString = sPrefix + sSuffix;
        int iFullStringCount = 0;
        iLastOccurence = -1;
        while ((iLastOccurence = sgOverallGraph.getDataString().indexOf(sFullString, iLastOccurence + 1))
            > -1)
            iFullStringCount++;

        // Consider p(sPrefix) = N(sPrefix) / (length(Text) / length(sPrefix))
        double pPrefix = (double) iPrefixCount /
                (sgOverallGraph.getDataString().length() / sPrefix.length());
        double pJoined = (double) iFullStringCount /
                (sgOverallGraph.getDataString().length() / sFullString.length());
        // Return p(sPrefix) * p(sPrefix, sSuffix)
        dRes = (iPrefixCount == 0) ? 0.0 : pPrefix * pJoined;
        return dRes;

    }
    
    private SortedSet<String> getSymbolsByProbabilities(String sText, IStatusDisplayer fStatus) {
        StringBuffer sbSubStr = new StringBuffer();
        TreeSet tsRes = new TreeSet();
        Date dStartTime = new Date();

        // For every character
        for (int iCnt = 0; iCnt < sText.length(); iCnt++) {
            String sNextChar = sText.substring(iCnt, iCnt+1);
            // If the probability of a suffix char given a prefix is higher than
            // random, the suffix is considered part of the prefix.
            if ((sbSubStr.length() == 0) ||
                    (getProbabilityOfStringInText(sbSubStr.toString(), sNextChar) >
                    getProbabilityOfStringInRandomText(sbSubStr.toString(), sNextChar)))
                sbSubStr.append(sText.charAt(iCnt));
            else
                // else end the existing symbol, adding it to the returned set and start a new one
            {
                tsRes.add(sbSubStr.toString());
                // DEBUG LINES
                // System.err.println("Found symbol:" + sbSubStr.toString());
                // appendToLog("Found symbol:" + sbSubStr.toString());
                //////////////
                sbSubStr = new StringBuffer(sNextChar);
            }

            Date dCurTime = new Date();
            long lRemaining = (sText.length() - iCnt + 1) *
                    (long)((double)(dCurTime.getTime() - dStartTime.getTime()) / iCnt);
            String sRemaining = String.format(" - Remaining: %40s\r",
                    gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining));
            if (iCnt % 50 == 0)
                fStatus.setStatus("Determining corpus symbols..." + sRemaining + "\n",
                    (double)iCnt / sText.length());
        }
        // Add the final symbol, if not empty.
        if (sbSubStr.length() > 0)
            tsRes.add(sbSubStr.toString());
        fStatus.setStatus("Determining corpus symbols... Done.\r", 1.0);
        return tsRes;
    }

    private Set<String> calcSymbols(List<String> sStrings, final IStatusDisplayer fStatus) {
        // Initialize SymbolicGraph
        // TODO: Add param
        int Levels=1;
        sgOverallGraph = new SymbolicGraph(1, Levels); // Init graph with a min of 2

        // Init distribution document
        // TODO: Add param
        int MinLevel = 2;
        // Create overall documents
        DistributionDocument[] ddaDoc = new DistributionDocument[Levels];
        for (int iCnt=0; iCnt < Levels; iCnt++)
        {
            ddaDoc[iCnt] = new DistributionDocument(1, MinLevel + iCnt); // For all windows
        }

        for (int iCnt=0; iCnt < Levels; iCnt++) {
            for (String sCurText: sStrings) {
                ddaDoc[iCnt].setDataString(sCurText, false);
            }
        }

        // Get symbol set
        Set<String> sSymbols = new HashSet<String>();

        for (String sCurText: sStrings) {
            // train symbolic graph
            sgOverallGraph.setDataString(((new StringBuffer().append(
                    (char)StreamTokenizer.TT_EOF))).toString());
            sgOverallGraph.setDataString(sCurText);
        }

        ExecutorService es = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        final Set<String> sSymbolsArg = sSymbols;
        final Distribution<Double> dProg = new Distribution();

        final List<String> sStringsArg = sStrings;
        for (final String sCurText: sStrings) {

            es.submit(new Runnable() {

                public void run() {
                    Set<String> sTmp = getSymbolsByProbabilities(sCurText, new IStatusDisplayer() {
                        public void setStatus(String sText, double dValue) {
                        }
                        public String getStatusText() {
                            return "";
                        }
                        public void setVisible(boolean bShow) {
                            return;
                        }
                        public boolean getVisible() {
                            return false;
                        }
                    });
                    synchronized (sSymbolsArg)
                    {
                        sSymbolsArg.addAll(sTmp);
                    }
                    synchronized (dProg) {
                        dProg.increaseValue(1.0, 1.0);
                        fStatus.setStatus("Extracting symbols...",
                                dProg.getValue(1.0) / sStringsArg.size());
                    }
                }
            });
            
        }

        es.shutdown();
        try {
            es.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(SymbolExtractor.class.getName()).log(
                    Level.SEVERE, null, ex);
            return null;
        }

        return sSymbols;
    }

    public static void main(String[] saArgs) {
        SymbolExtractor se = new SymbolExtractor();
        DocumentSet dsTest = new DocumentSet("/home/ggianna/Documents/JApplications/"
                + "RecordLinkage/data/training/", 1.0);
        dsTest.createSets(true);
        
        ArrayList<String> lsTexts = new ArrayList<String>(
                dsTest.getTrainingSet().size());
        for (CategorizedFileEntry cfeCur : (List<CategorizedFileEntry>)dsTest.getTrainingSet()) {
            String sCurText = utils.loadFileToStringWithNewlines(cfeCur.getFileName());
            lsTexts.add(sCurText);
        }

        Set<String> sSymbols = se.getSymbols(lsTexts,
                new StatusConsole(120));

        System.out.println(utils.printIterable(sSymbols, "\n"));
    }
}
