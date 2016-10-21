/*
 * summaryEvaluator.java
 *
 * Created on 17 ?????????? 2007, 5:16 ??
 *
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.ducTools.DUCDocumentInfo;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramSymWinDocument;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDistroDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDistroDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentDistroComparator;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;
import gr.demokritos.iit.jinsect.threading.ThreadList;

/** A class of objects that can evaluate a set of summaries, using n-gram graph representation.
 *
 * @author ggianna
 */
public class summaryEvaluator implements Runnable {
    // Constants
    /** Constant to use distribution average as edge weight in the n-gram graph. */
    public static final int USE_DISTRO_AVERAGE_AS_WEIGHT = 0;
    /** Constant to use co-occurence cardinality as edge weight in the n-gram graph. */
    public static final int USE_OCCURENCES_AS_WEIGHT = 1;
    /** Constant defining word n-gram method. */
    public static final String DO_WORDS = "word";
    /** Constant defining char n-gram method. */
    public static final String DO_CHARS = "char";
    /** Constant defining union (char <i>and</i> word) n-gram method. */
    public static final String DO_ALL = "all";
    
    // Variables
    protected Integer WordMin, WordMax, WordDist, CharMin, CharMax, CharDist, Threads, WeightMethod;
    protected String OutFile, SummaryDir, ModelDir, Do;
    boolean Silent, Progress;
    protected Semaphore OutputSemaphore;
    /** Word n-gram graph representation cache.*/
    protected Hashtable hModelCache = new Hashtable();
    /** Character n-gram graph representation cache.*/
    protected Hashtable hNModelCache = new Hashtable();
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // doRougeLikeEval();
        summaryEvaluator sCur = new summaryEvaluator(args);        
        sCur.run();
    }
    
    /** Provides command-line syntax information for the execution of the main class function. */
    private static void printUsage() {
            System.err.println("Syntax:\nsummaryEvaluator -do=(char|word|all) [-summaryDir=summaries/] [-modelDir=models/]"+
                    "[-wordMin=#] [-wordMax=#] [-wordDist=#] [-charMin=#] [-charMax=#] [-charDist=#] [-o=outFile] [-t=#]" + 
                    "[-s] [-use=o|d] [-progress]");
            System.err.println("wordMin=#\tMin word n-gram size.\nwordMax=#\tMax word n-gram size.\nwordDist=#\tWord n-gram window\n" +
                    "charMin=#\tMin char n-gram size.\ncharMax=#\tMax char n-gram size.\ncharDist=#\tChar n-gram window.\n" +
                    "o=outFile\tThe file to output data. Default is stdout.\n-t=#\tNumber of threads. Defaults to 2.\n" +
                    "s\tFor non-verbose output (silent).\n" +
                    "progress\tFor progress indication (even in silent mode).\n" +
                    "use=o|d\tUse [o]ccurences or average [d]istance to assign weights to the graph. Defaults to o.\n" +
                    "-?\tShow this screen.");
    }

    /** Creates a summaryEvaluator object.
     *@param sOutputSemaphore A semaphore that ensures that the output is provided consistently.
     *@param sDo The method of evaluation (see <code>DO_WORDS, DO_CHAR, DO_ALL</code>).
     *@param iWordMin The min word n-gram rank to take into account, if applicable to the method.
     *@param iWordMax The max word n-gram rank to take into account, if applicable to the method.
     *@param iWordDist The word n-gram neighbourhood distance to use, if applicable to the method.
     *@param iCharMin The min char n-gram rank to take into account, if applicable to the method.
     *@param iCharMax The max char n-gram rank to take into account, if applicable to the method.
     *@param iCharDist The char n-gram neighbourhood distance to use, if applicable to the method.
     *@param iThreads The number of threads to use, for multi-threaded processing.
     *@param sOutFile The file to output results.
     *@param sSummaryDir The peer summary base directory.
     *@param sModelDir The model summaries base directory.
     *@param bSilent If true, no debug messages are output.
     *@param iWeightMethod The method to use for weighting edges in the n-gram graph. See 
     * <code>USE_DISTRO_AVERAGE_AS_WEIGHT, USE_OCCURENCES_AS_WEIGHT</code>.
     *@param bProgress If true, indicates that progress indication should be output, even in silent
     * mode.
     */
    public summaryEvaluator(Semaphore sOutputSemaphore, String sDo,
            int iWordMin, int iWordMax, int iWordDist, int iCharMin, int iCharMax, int iCharDist, int iThreads,
        String sOutFile, String sSummaryDir, String sModelDir, boolean bSilent, int iWeightMethod,
            boolean bProgress)
    {
        Do = sDo;
        OutputSemaphore = sOutputSemaphore;
        WordMin = iWordMin;
        WordMax = iWordMax;
        WordDist = iWordDist;
        CharMin = iCharMin;
        CharMax = iCharMax;
        CharDist = iCharDist;
        Threads = iThreads;
        OutFile = sOutFile;
        SummaryDir = sSummaryDir;
        ModelDir = sModelDir;
        Silent = bSilent;
        WeightMethod = iWeightMethod;
        Progress = bProgress;
    }
    
    /** Creates a summaryEvaluator object, given a command-line like string.
     *@param args An array of strings, corresponding to command-line parsed parameters.
     */
    public summaryEvaluator(String[] args) {
        OutputSemaphore = new Semaphore(1);
        
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        if (gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"?", "").length() > 0) {
            printUsage();
            System.exit(0);
        }
            
        // Parse commandline
        try {
            WordMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordMin", "1"));
            WordMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordMax", "2"));
            WordDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordDist", "3"));
            CharMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charMin", "3"));
            CharMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charMax", "5"));
            CharDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charDist", "3"));
            Threads = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"t", 
                    "" + Runtime.getRuntime().availableProcessors()));
            String sWeightMethod = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"use","o");
            if (sWeightMethod.equals("o"))
                WeightMethod = USE_OCCURENCES_AS_WEIGHT;
            else
                if (sWeightMethod.equals("d"))
                    WeightMethod = USE_DISTRO_AVERAGE_AS_WEIGHT;
                else {
                    printUsage();
                    System.exit(1);
                }
                    
            
            // Define method
            Do = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"do", "all");
            if ((Do.length() == 0) || ("char_word_all__".indexOf(Do) % 5 != 0))
            {
                // Invalid or undefined method
                printUsage();
                System.exit(0);
            }
            // Define output file
            OutFile = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "o", "");
            // Get summary and model dir
            SummaryDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "summaryDir", "summaries" +
                    System.getProperty("file.separator"));
            ModelDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "modelDir", "models" + 
                    System.getProperty("file.separator"));
            // Determine if silent
            Silent=gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "s", "FALSE").equals("TRUE");
            // Determine if progress indication should be shown
            Progress=gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "progress", "FALSE").equals("TRUE");
            
            if (!Silent)
                System.err.println("Using parameters:\n" + hSwitches);
            
            
        }
        catch (ClassCastException cce) {
            System.err.println("Malformed switch:" + cce.getMessage() + ". Aborting...");
            printUsage();
        }
    }
    
    /** Performs the evaluation step in a thread-safe way. */
    public void run() {
        PrintStream pOut = null;
        if (OutFile.length() != 0) {
            try {
                pOut = new PrintStream(OutFile);
            }
            catch (FileNotFoundException fnfe) {
                System.err.println("Cannot output to selected file:\n" + fnfe.getMessage());
                //System.exit(1);
                return;
            }
        }
        try {

            //doNormalEval(OutputSemaphore, pOut, WordMin, WordMax, WordDist, CharMin, CharMax, CharDist,
                    //Do.equals("char") || Do.equals("all"), Do.equals("word") || Do.equals("all"), SummaryDir, ModelDir, 
                    //Threads, Silent);
            doOptimizedEval(OutputSemaphore, pOut, WordMin, WordMax, WordDist, CharMin, CharMax, CharDist,
                Do.equals("char") || Do.equals("all"), Do.equals("word") || Do.equals("all"), SummaryDir, ModelDir, 
                Threads, Silent, Progress);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        
        if (pOut!=null)
            pOut.close();
    }
    
    /** Performs similarity measurement of a {@link CategorizedFileEntry}, given a model set. It
     * uses default values for the n-gram graphs' parameters, performing a test for both word and
     * character n-grams.
     *@param cfeCur The current file to compare to models.
     *@param dsModelSet The input model set.
     *@param bOutput If true, output is verbose.
     *@param sSem The semaphore to use to ascertain that output is consistent and thread-safe.
     *@return A {@link SimilarityArray} containing similarity values for the given file.
     */
    protected SimilarityArray calcSimilarityMeasures(CategorizedFileEntry cfeCur, List dsModelSet, 
            boolean bOutput, Semaphore sSem) {
        return calcSimilarityMeasures(cfeCur, dsModelSet, bOutput, null, sSem, 1, 2, 3, 3, 5, 3, 
                true, true, true);
    }
    
    /** Performs similarity measurement of a {@link CategorizedFileEntry}, given a model set.
     *@param cfeCur The current file to compare to models.
     *@param dsModelSet The input model set.
     *@param bOutput If true, output is verbose.
     *@param pOut The {@link PrintStream} to use for output.
     *@param sSem The semaphore to use to ascertain that output is consistent and thread-safe.
     *@param WordNGramSize_Min The min word n-gram rank to use in the representation.
     *@param WordNGramSize_Max The max word n-gram rank to use in the representation.
     *@param Word_Dmax The max neighbourhood distance to use in the word n-gram graph 
     * representation.
     *@param CharacterNGramSize_Min The min character n-gram rank to use in the representation.
     *@param CharacterNGramSize_Max The max character n-gram rank to use in the representation.
     *@param Character_Dmax The max neighbourhood distance to use in the character n-gram graph 
     * representation.
     *@param bDoCharNGrams If true performs character n-gram comparison. Can be used together with 
     * <code>bDoWordNGrams</code>.
     *@param bDoWordNGrams If true performs word n-gram comparison. Can be used together with 
     * <code>bDoCharNGrams</code>.
     *@param bSilent If true, no debugging information is displayed.
     *@return A {@link SimilarityArray} containing similarity values for the given file.
     */
    protected SimilarityArray calcSimilarityMeasures(CategorizedFileEntry cfeCur, List dsModelSet, 
            boolean bOutput, PrintStream pOut, Semaphore sSem, 
            int WordNGramSize_Min, int WordNGramSize_Max, int Word_Dmax,
            int CharacterNGramSize_Min, int CharacterNGramSize_Max, int Character_Dmax, 
            boolean bDoCharNGrams, boolean bDoWordNGrams, boolean bSilent) {
        
        if (pOut == null)
            pOut = System.out;
        
        // Init return struct
        SimilarityArray saRes = new SimilarityArray();
        
        // Read first file
        SimpleTextDocument ndDoc1 = new SimpleTextDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
        NGramSymWinDocument ndNDoc1 = new NGramSymWinDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, CharacterNGramSize_Min, CharacterNGramSize_Max);
        
        if (bDoWordNGrams)
            ndDoc1.loadDataStringFromFile(cfeCur.getFileName());
        if (bDoCharNGrams)
            ndNDoc1.loadDataStringFromFile(cfeCur.getFileName());
        
        StandardDocumentComparator sdcComparator = new StandardDocumentComparator();
        StandardDocumentComparator sdcNComparator = new StandardDocumentComparator();
        
        Iterator iOtherIter = dsModelSet.iterator();
        while (iOtherIter.hasNext()) {
            CategorizedFileEntry cfeOther = (CategorizedFileEntry)iOtherIter.next();

            String sSumName = new File(cfeOther.getFileName()).getName();
            String sModelName = new File(cfeCur.getFileName()).getName();
            if (sSumName.equals(sModelName)) {
                if (!bSilent)
                    synchronized (System.err) {
                        System.err.println("Ignoring identically named files:" + cfeOther.getFileName() +
                                " , " + cfeCur.getFileName());
                    }
                continue; // Ignore docs with exactly same names (i.e. same files)
            }
            
            if (!bSilent)
                synchronized (System.err) {
                    System.err.println("Comparing files:" + cfeOther.getFileName() +
                                " , " + cfeCur.getFileName());
                }

            // Load model data
            SimpleTextDocument ndDoc2 = null;
            NGramSymWinDocument ndNDoc2 = null;
                    
            if (bDoWordNGrams)
            {
                synchronized (hModelCache) {
                    // Look up cache
                    if (hModelCache.containsKey(cfeOther.getFileName()))
                        ndDoc2 = (SimpleTextDocument)hModelCache.get(cfeOther.getFileName());
                    else {
                        ndDoc2 = new SimpleTextDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
                        ndDoc2.loadDataStringFromFile(cfeOther.getFileName());
                        hModelCache.put(cfeOther.getFileName(), ndDoc2);
                    }
                }
            }
                
            // Yield
            Thread.yield();
            
            if (bDoCharNGrams) {
                synchronized (hNModelCache) {
                    if (hNModelCache.containsKey(cfeOther.getFileName()))
                        ndNDoc2 = (NGramSymWinDocument)hNModelCache.get(cfeOther.getFileName());
                    else {
                        ndNDoc2 = new NGramSymWinDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, 
                                  CharacterNGramSize_Min, CharacterNGramSize_Max);
                        ndNDoc2.loadDataStringFromFile(cfeOther.getFileName());
                        hNModelCache.put(cfeOther.getFileName(), ndNDoc2);
                    }
                }
            }
                
            // Yield
            Thread.yield();
                        
            // Save and Output results
            try {
                GraphSimilarity sSimil = null;
                if (bDoWordNGrams) {
                    // Get simple text similarities
                    sSimil = sdcComparator.getSimilarityBetween(ndDoc1, ndDoc2);
                    saRes.SimpleTextOverallSimil = sSimil;
                    saRes.SimpleTextGraphSimil = sdcComparator.getGraphSimilarity();
                    saRes.SimpleTextHistoSimil = sdcComparator.getHistogramSimilarity();
                }
                
                GraphSimilarity sSimil2 = null;
                if (bDoCharNGrams) {
                    sSimil2 = sdcNComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);
                    // Get n-gram document similarities
                    saRes.NGramOverallSimil = sSimil2;
                    saRes.NGramGraphSimil = sdcNComparator.getGraphSimilarity();
                    saRes.NGramHistoSimil = sdcNComparator.getHistogramSimilarity();
                }
                

                // Init name
                // OBSOLETE
                // String[] sFileNameData = new File(cfeCur.getFileName()).getName().split("\\.");
                // String sID = sFileNameData[0] + "\t" + sFileNameData[4]; // Only print Theme and SystemID
                ///////////
                
                DUCDocumentInfo d = new DUCDocumentInfo(cfeCur.getFileName());
                String sID = d.Topic + "\t" + d.Summarizer; // Only print Theme and SystemID;

                if (bOutput) {
                    
                    if (sSem != null)
                        try {
                            sSem.acquire();
                        }
                        catch (InterruptedException ie) {
                            return null;
                        }
                    try {
                        pOut.print(sID + "\t");
                        if (bDoWordNGrams)
                            pOut.print(saRes.SimpleTextGraphSimil.ContainmentSimilarity + "\t" +
                                saRes.SimpleTextGraphSimil.ValueSimilarity + "\t" +
                                saRes.SimpleTextGraphSimil.SizeSimilarity + "\t" +
                                saRes.SimpleTextHistoSimil.ContainmentSimilarity + "\t" +
                                saRes.SimpleTextHistoSimil.ValueSimilarity + "\t" +
                                saRes.SimpleTextHistoSimil.SizeSimilarity + "\t" + 
                                saRes.SimpleTextOverallSimil.getOverallSimilarity() + "\t");
                        if (bDoCharNGrams)
                            pOut.print(saRes.NGramGraphSimil.ContainmentSimilarity + "\t" +
                                saRes.NGramGraphSimil.ValueSimilarity + "\t" +
                                saRes.NGramGraphSimil.SizeSimilarity + "\t" +
                                saRes.NGramHistoSimil.ContainmentSimilarity + "\t" +
                                saRes.NGramHistoSimil.ValueSimilarity + "\t" +
                                saRes.NGramHistoSimil.SizeSimilarity + "\t" + 
                                saRes.NGramOverallSimil.getOverallSimilarity());                    
                        pOut.println();
                        pOut.flush();
                    }
                    finally {
                        if (sSem != null)
                            sSem.release();
                    }
                }
            }
            catch (InvalidClassException iceE) {
                System.err.println("Cannot compare...");
            }
            
            // Let updates happen
            Thread.yield();

        }
        return saRes;
    }

    /** Performs similarity measurement of a {@link CategorizedFileEntry}, given a model set.
     *@param cfeCur The current file to compare to models.
     *@param dsModelSet The input model set.
     *@param bOutput If true, output is verbose.
     *@param pOut The {@link PrintStream} to use for output.
     *@param sSem The semaphore to use to ascertain that output is consistent and thread-safe.
     *@param WordNGramSize_Min The min word n-gram rank to use in the representation.
     *@param WordNGramSize_Max The max word n-gram rank to use in the representation.
     *@param Word_Dmax The max neighbourhood distance to use in the word n-gram graph 
     * representation.
     *@param CharacterNGramSize_Min The min character n-gram rank to use in the representation.
     *@param CharacterNGramSize_Max The max character n-gram rank to use in the representation.
     *@param Character_Dmax The max neighbourhood distance to use in the character n-gram graph 
     * representation.
     *@param bDoCharNGrams If true performs character n-gram comparison. Can be used together with 
     * <code>bDoWordNGrams</code>.
     *@param bDoWordNGrams If true performs word n-gram comparison. Can be used together with 
     * <code>bDoCharNGrams</code>.
     *@param bSilent If true, no debugging information is displayed.
     *@return A {@link SimilarityArray} containing similarity values for the given file.
     */
    protected SimilarityArray calcDistroSimilarityMeasures(CategorizedFileEntry cfeCur, List dsModelSet, 
            boolean bOutput, PrintStream pOut, Semaphore sSem, 
            int WordNGramSize_Min, int WordNGramSize_Max, int Word_Dmax,
            int CharacterNGramSize_Min, int CharacterNGramSize_Max, int Character_Dmax, 
            boolean bDoCharNGrams, boolean bDoWordNGrams, boolean bSilent) {
        
        if (pOut == null)
            pOut = System.out;
        
        // Init return struct
        SimilarityArray saRes = new SimilarityArray();
        
        // Read first file
        SimpleTextDistroDocument ndDoc1 = new SimpleTextDistroDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
        NGramDistroDocument ndNDoc1 = new NGramDistroDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, CharacterNGramSize_Min, CharacterNGramSize_Max);
        
        if (bDoWordNGrams)
            ndDoc1.loadDataStringFromFile(cfeCur.getFileName());
        if (bDoCharNGrams)
            ndNDoc1.loadDataStringFromFile(cfeCur.getFileName());
        
        StandardDocumentDistroComparator sdcComparator = new StandardDocumentDistroComparator();
        StandardDocumentDistroComparator sdcNComparator = new StandardDocumentDistroComparator();
        
        Iterator iOtherIter = dsModelSet.iterator();
        while (iOtherIter.hasNext()) {
            CategorizedFileEntry cfeOther = (CategorizedFileEntry)iOtherIter.next();

            String sSumName = new File(cfeOther.getFileName()).getName();
            String sModelName = new File(cfeCur.getFileName()).getName();
            if (sSumName.equals(sModelName))
                continue; // Ignore docs with exactly same names (i.e. same files)

            // Load model data
            SimpleTextDistroDocument ndDoc2 = null;
            NGramDistroDocument ndNDoc2 = null;
                    
            if (bDoWordNGrams)
            {
                synchronized (hModelCache) {
                    // Look up cache
                    if (hModelCache.containsKey(cfeOther.getFileName()))
                        ndDoc2 = (SimpleTextDistroDocument)hModelCache.get(cfeOther.getFileName());
                    else {
                        ndDoc2 = new SimpleTextDistroDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
                        ndDoc2.loadDataStringFromFile(cfeOther.getFileName());
                        hModelCache.put(cfeOther.getFileName(), ndDoc2);
                    }
                }
            }
                
            // Yield
            Thread.yield();
            
            if (bDoCharNGrams) {
                synchronized (hModelCache) {
                    if (hNModelCache.containsKey(cfeOther.getFileName()))
                        ndNDoc2 = (NGramDistroDocument)hNModelCache.get(cfeOther.getFileName());
                    else {
                        ndNDoc2 = new NGramDistroDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, 
                                CharacterNGramSize_Min, CharacterNGramSize_Max);
                        ndNDoc2.loadDataStringFromFile(cfeOther.getFileName());
                        hNModelCache.put(cfeOther.getFileName(), ndNDoc2);
                    }
                }
            }
                
            
            // Yield
            Thread.yield();
            
            // Save and Output results
            try {
                GraphSimilarity sSimil = null;
                if (bDoWordNGrams) {
                    // Get simple text similarities
                    sSimil = sdcComparator.getSimilarityBetween(ndDoc1, ndDoc2);
                    saRes.SimpleTextOverallSimil = sSimil;
                    saRes.SimpleTextGraphSimil = sdcComparator.getGraphSimilarity();
                    saRes.SimpleTextHistoSimil = sdcComparator.getHistogramSimilarity();
                }
                
                GraphSimilarity sSimil2 = null;
                if (bDoCharNGrams) {
                    sSimil2 = sdcNComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);
                    // Get n-gram document similarities
                    saRes.NGramOverallSimil = sSimil2;
                    saRes.NGramGraphSimil = sdcNComparator.getGraphSimilarity();
                    saRes.NGramHistoSimil = sdcNComparator.getHistogramSimilarity();
                }
                

                // Init name
                String[] sFileNameData = new File(cfeCur.getFileName()).getName().split("\\.");
                String sID = sFileNameData[0] + "\t" + sFileNameData[4]; // Only print Theme and SystemID

                if (bOutput) {
                    
                    if (sSem != null)
                        try {
                            sSem.acquire();
                        }
                        catch (InterruptedException ie) {
                            return null;
                        }
                    try {
                        pOut.print(sID + "\t");
                        if (bDoWordNGrams)
                            pOut.print(saRes.SimpleTextGraphSimil.ContainmentSimilarity + "\t" +
                                saRes.SimpleTextGraphSimil.ValueSimilarity + "\t" +
                                saRes.SimpleTextGraphSimil.SizeSimilarity + "\t" +
                                saRes.SimpleTextHistoSimil.ContainmentSimilarity + "\t" +
                                saRes.SimpleTextHistoSimil.ValueSimilarity + "\t" +
                                saRes.SimpleTextHistoSimil.SizeSimilarity + "\t" + 
                                saRes.SimpleTextOverallSimil.getOverallSimilarity() + "\t");
                        if (bDoCharNGrams)
                            pOut.print(saRes.NGramGraphSimil.ContainmentSimilarity + "\t" +
                                saRes.NGramGraphSimil.ValueSimilarity + "\t" +
                                saRes.NGramGraphSimil.SizeSimilarity + "\t" +
                                saRes.NGramHistoSimil.ContainmentSimilarity + "\t" +
                                saRes.NGramHistoSimil.ValueSimilarity + "\t" +
                                saRes.NGramHistoSimil.SizeSimilarity + "\t" + 
                                saRes.NGramOverallSimil.getOverallSimilarity());                    
                        pOut.println();
                        pOut.flush();
                    }
                    finally {
                        if (sSem != null)
                            sSem.release();
                    }
                }
            }
            catch (InvalidClassException iceE) {
                System.err.println("Cannot compare...");
            }

        }
        return saRes;
    }
    
    /** Performs optimized evaluation of a given set of summaries, given a model directory. 
     *@param sSem A semaphore that ensures that the output is provided consistently.
     *@param pOverallResultsOutStream The output stream for results.
     *@param WordNGramSize_Min The min word n-gram rank to take into account, if applicable to the method.
     *@param WordNGramSize_Max The max word n-gram rank to take into account, if applicable to the method.
     *@param Word_Dmax The word n-gram neighbourhood distance to use, if applicable to the method.
     *@param CharacterNGramSize_Min The min char n-gram rank to take into account, if applicable to the method.
     *@param CharacterNGramSize_Max The max char n-gram rank to take into account, if applicable to the method.
     *@param Character_Dmax The char n-gram neighbourhood distance to use, if applicable to the method.
     *@param bDoCharNGrams If true, char n-grams evaluation is performed.
     *@param bDoWordNGrams If true, word n-grams evaluation is performed.
     *@param sSummaryDir The peer summary base directory.
     *@param sModelDir The model summaries base directory.
     *@param bSilent If true, no debug messages are output.
     *@param bProgress If true, indicates that progress indication should be output, even in silent
     * mode.
     */
    protected void doOptimizedEval(final Semaphore sSem, PrintStream pOverallResultsOutStream,
            int WordNGramSize_Min, int WordNGramSize_Max, int Word_Dmax,
            int CharacterNGramSize_Min, int CharacterNGramSize_Max, int Character_Dmax,
            boolean bDoCharNGrams, boolean bDoWordNGrams, String sSummaryDir, String sModelDir, int iThreads,
            boolean bSilent, boolean bProgress) throws Exception {
                // Defaults to standard output
        if (pOverallResultsOutStream == null)
            pOverallResultsOutStream = System.out;
        
        ThreadList tqRobin = new ThreadList(iThreads);
        
        DocumentSet dsSummarySet = new DocumentSet(sSummaryDir, 1.0);
        final DocumentSet dsModelSet = new DocumentSet(sModelDir, 1.0);
        dsSummarySet.createSets();
        dsModelSet.createSets();
        HashMap hmCategoryResults = new HashMap();        
        
        if (dsSummarySet.getTrainingSet().size() *  dsModelSet.getTrainingSet().size()== 0)
        {
            System.err.println("Empty document set...");
            throw new Exception("Empty document set...");
        }

        // Simple text
        pOverallResultsOutStream.print("Theme\t");
        pOverallResultsOutStream.print("SystemID\t");
        if (bDoWordNGrams) {
            pOverallResultsOutStream.print("GraphCooccurence\t");
            pOverallResultsOutStream.print("GraphValue\t");
            pOverallResultsOutStream.print("GraphSize\t");
            pOverallResultsOutStream.print("HistoContainmentSimilarity\t");
            pOverallResultsOutStream.print("HistoValue\t");
            pOverallResultsOutStream.print("HistoSize\t");
            pOverallResultsOutStream.print("OverallSimil\t");
        }
        // N-grams
        if (bDoCharNGrams) {
            pOverallResultsOutStream.print("CharGraphCooccurence\t");
            pOverallResultsOutStream.print("CharGraphValue\t");
            pOverallResultsOutStream.print("CharGraphSize\t");
            pOverallResultsOutStream.print("NHistoContainmentSimilarity\t");
            pOverallResultsOutStream.print("NHistoValue\t");
            pOverallResultsOutStream.print("NHistoSize\t");
            pOverallResultsOutStream.print("NOverallSimil\t");
        }
        pOverallResultsOutStream.println();
        
        int iTotal = dsSummarySet.getTrainingSet().size();
        int iCur = 0;
        
        // For every category
        Iterator iCatIter = dsModelSet.getCategories().iterator();
        // Init start date
        Date dStart = new Date();
        
        while (iCatIter.hasNext()) {
            String sCurCategory = (String)iCatIter.next();
            if (!bSilent)
                System.err.println("Processing category:" + sCurCategory);
            
            // Clear cache to avoid memory redundancy
            hModelCache.clear();
            hNModelCache.clear();
            
            // Load model file list
            List lModelFiles = dsModelSet.getFilesFromCategory(sCurCategory);            
            // For every file in the same category as the models
            Iterator iIter = dsSummarySet.getFilesFromCategory(sCurCategory, 
                    dsSummarySet.FROM_TRAINING_SET).iterator();            
            while (iIter.hasNext()) {
                final CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
                
                Runnable r = new CalcSimilRunner(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax,
                        CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, cfeCur,
                        lModelFiles, sSem, 
                        bDoCharNGrams, bDoWordNGrams,
                        pOverallResultsOutStream, bSilent, this, WeightMethod, Progress);
                while (!tqRobin.addThreadFor(r))
                    Thread.yield();

                Date dCurTime = new Date();            
                long lRemaining = (iTotal - iCur + 1) * (long)((double)(dCurTime.getTime() - dStart.getTime()) / iCur);
                if (!bSilent || (bProgress))
                    System.err.print("Completed " + String.format("%7.4f", (double)iCur++ / iTotal * 100) + "%"+
                        String.format(" - Remaining %50s\r", gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining)));

            }

        }
        try {
            tqRobin.waitUntilCompletion();
        } catch (InterruptedException ex) {
            System.err.println("Could not complete execution of all tasks.");
            ex.printStackTrace(System.err);
        }
        // Finished
        System.err.println("Completed 100%. A total of " + iTotal + " comparisons were " +
                "performed.");
    }
    
    /** TODO */
    protected void doNormalEval(final Semaphore sSem, PrintStream pOverallResultsOutStream,
            int WordNGramSize_Min, int WordNGramSize_Max, int Word_Dmax,
            int CharacterNGramSize_Min, int CharacterNGramSize_Max, int Character_Dmax,
            boolean bDoCharNGrams, boolean bDoWordNGrams, String sSummaryDir, String sModelDir, int iThreads,
            boolean bSilent, boolean bProgress) {
        // Defaults to standard output
        if (pOverallResultsOutStream == null)
            pOverallResultsOutStream = System.out;
        
        ThreadList tqRobin = new ThreadList(iThreads);
        
        DocumentSet dsSummarySet = new DocumentSet(sSummaryDir, 1.0);
        final DocumentSet dsModelSet = new DocumentSet(sModelDir, 1.0);
        dsSummarySet.createSets();
        dsModelSet.createSets();
        HashMap hmCategoryResults = new HashMap();        
        
        if (dsSummarySet.getTrainingSet().size() *  dsModelSet.getTrainingSet().size()== 0)
        {
            System.err.println("Empty document set...");
            System.exit(-1);
        }

        // Simple text
        pOverallResultsOutStream.print("Theme\t");
        pOverallResultsOutStream.print("SystemID\t");
        if (bDoWordNGrams) {
            pOverallResultsOutStream.print("GraphCooccurence\t");
            pOverallResultsOutStream.print("GraphValue\t");
            pOverallResultsOutStream.print("GraphSize\t");
            pOverallResultsOutStream.print("HistoContainmentSimilarity\t");
            pOverallResultsOutStream.print("HistoValue\t");
            pOverallResultsOutStream.print("HistoSize\t");
            pOverallResultsOutStream.print("OverallSimil\t");
        }
        // N-grams
        if (bDoCharNGrams) {
            pOverallResultsOutStream.print("CharGraphCooccurence\t");
            pOverallResultsOutStream.print("CharGraphValue\t");
            pOverallResultsOutStream.print("CharGraphSize\t");
            pOverallResultsOutStream.print("NHistoContainmentSimilarity\t");
            pOverallResultsOutStream.print("NHistoValue\t");
            pOverallResultsOutStream.print("NHistoSize\t");
            pOverallResultsOutStream.print("NOverallSimil\t");
        }
        pOverallResultsOutStream.println();
        
        int iTotal = dsSummarySet.getTrainingSet().size();
        int iCur = 0;
        
        Iterator iIter = dsSummarySet.getTrainingSet().iterator();
        Date dStart = new Date();
        while (iIter.hasNext()) {
            final CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
            final String sCategory = cfeCur.getFileName().substring(cfeCur.getFileName().lastIndexOf(".") + 1);
            Runnable r = new CalcSimilRunner(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax,
                    CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, cfeCur,
                    dsModelSet.getFilesFromCategory(cfeCur.getCategory()), sSem, 
                    bDoCharNGrams, bDoWordNGrams,
                    pOverallResultsOutStream, bSilent, this, WeightMethod, Progress);
            while (!tqRobin.addThreadFor(r))
                try {
                    Thread.sleep(500 / iThreads);
                }
                catch (InterruptedException ie) {
                    // Actually ignore
                    ie.printStackTrace(System.err);
                }
                
                
            Date dCurTime = new Date();            
            long lRemaining = (iTotal - iCur + 1) * (long)((double)(dCurTime.getTime() - dStart.getTime()) / iCur);
            if (!bSilent || (bProgress))
                System.err.print("Completed " + String.format("%7.4f", (double)iCur++ / iTotal * 100) + "%"+
                    String.format(" - Remaining %s\r", gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining)));

        }
        try {
            tqRobin.waitUntilCompletion();
        } catch (InterruptedException ex) {
            System.err.println("Could not complete execution of all tasks.");
            ex.printStackTrace(System.err);
        }
        System.err.println();
    }
    
/*    protected static void doRougeLikeEval(final Semaphore sSem) {
        DocumentSet dsSummarySet = new DocumentSet("/home/ggianna/JInsect/summaries/", 1.0);
        DocumentSet dsModelSet = new DocumentSet("/home/ggianna/JInsect/models/", 1.0);
        dsSummarySet.createSets();
        dsModelSet.createSets();
        HashMap hmCategoryResults = new HashMap();        
        
        if (dsSummarySet.getTrainingSet().size() *  dsModelSet.getTrainingSet().size()== 0)
        {
            System.err.println("Empty document set...");
            System.exit(-1);
        }

        // Simple text
        System.out.print("Theme\t");
        System.out.print("SystemID\t");
        System.out.print("GraphCooccurence\t");
        System.out.print("GraphValue\t");
        System.out.print("GraphSize\t");
        System.out.print("HistoContainmentSimilarity\t");
        System.out.print("HistoValue\t");
        System.out.print("HistoSize\t");
        System.out.print("OverallSimil\t");
        // N-grams
        System.out.print("CharGraphCooccurence\t");
        System.out.print("CharGraphValue\t");
        System.out.print("CharGraphSize\t");
        System.out.print("NHistoContainmentSimilarity\t");
        System.out.print("NHistoValue\t");
        System.out.print("NHistoSize\t");
        System.out.println("NOverallSimil\t");

        // System.out.println("@data");
        
        int iTotal = dsSummarySet.getTrainingSet().size();
        int iCur = 0;
        
        Iterator iIter = dsSummarySet.getTrainingSet().iterator();
        while (iIter.hasNext()) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
            String sCategory = cfeCur.getFileName().substring(cfeCur.getFileName().lastIndexOf(".") + 1);
            
            if ("ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(sCategory) > -1) // If human summary
            {
                // Just compare, ignoring identical summary from models
                SimilarityArray sa = calcSimilarityMeasures(cfeCur, 
                        dsModelSet.getFilesFromCategory(cfeCur.getCategory()), true, null);
            }
            else
            {
                // Get training set
                Union uTrainingSet = new Union();
                uTrainingSet.addAll(dsModelSet.getFilesFromCategory(cfeCur.getCategory()));
                // Get permutations of (relevant) training set
                Union uPermutations = jinsect.utils.getCombinationsBy(uTrainingSet, uTrainingSet.size() - 1);
                // Init result distributions
                SimilarityDistribution sdGraphResults = new SimilarityDistribution();
                SimilarityDistribution sdHistoResults = new SimilarityDistribution();
                SimilarityDistribution sdCharGraphResults = new SimilarityDistribution();
                SimilarityDistribution sdNHistoResults = new SimilarityDistribution();
                SimilarityDistribution sdOverallResults = new SimilarityDistribution();
                SimilarityDistribution sdNOverallResults = new SimilarityDistribution();
                
                // For every permutation
                Iterator iPermIter = uPermutations.iterator();
                while (iPermIter.hasNext()) {
                    List lCurDocSet = (List)iPermIter.next();
                    // Compare normally
                    SimilarityArray saPartialRes =  calcSimilarityMeasures(cfeCur, 
                        lCurDocSet, false, sSem);
                    // Add results to distributions
                    sdGraphResults.setValue(sdGraphResults.asTreeMap().size(), saPartialRes.SimpleTextGraphSimil);
                    sdHistoResults.setValue(sdHistoResults.asTreeMap().size(), saPartialRes.SimpleTextHistoSimil);
                    sdCharGraphResults.setValue(sdCharGraphResults.asTreeMap().size(), saPartialRes.NGramGraphSimil);
                    sdNHistoResults.setValue(sdNHistoResults.asTreeMap().size(), saPartialRes.NGramHistoSimil);
                    sdOverallResults.setValue(sdOverallResults.asTreeMap().size(), 
                            saPartialRes.SimpleTextOverallSimil);
                    sdNOverallResults.setValue(sdNOverallResults.asTreeMap().size(), 
                            saPartialRes.NGramOverallSimil);
                }
                // Get averages
                SimilarityArray saRes = new SimilarityArray();
                saRes.SimpleTextGraphSimil = sdGraphResults.average();
                saRes.SimpleTextHistoSimil = sdHistoResults.average();
                saRes.SimpleTextOverallSimil = sdOverallResults.average();
                
                saRes.NGramGraphSimil = sdCharGraphResults.average();
                saRes.NGramHistoSimil = sdNHistoResults.average();
                saRes.NGramOverallSimil = sdNOverallResults.average();
                
                // Init name
                String[] sFileNameData = cfeCur.getFileName().substring(
                        cfeCur.getFileName().lastIndexOf("/") + 1).split("\\.");
                String sID = sFileNameData[0] + "\t" + sFileNameData[4] + "\t"; // Only print Theme and SystemID
                // Output results
                System.out.print(sID + saRes.SimpleTextGraphSimil.ContainmentSimilarity + "\t" +
                        saRes.SimpleTextGraphSimil.ValueSimilarity + "\t" +
                        saRes.SimpleTextGraphSimil.SizeSimilarity + "\t" +
                        saRes.SimpleTextHistoSimil.ContainmentSimilarity + "\t" +
                        saRes.SimpleTextHistoSimil.ValueSimilarity + "\t" +
                        saRes.SimpleTextHistoSimil.SizeSimilarity + "\t" + 
                        saRes.SimpleTextOverallSimil.getOverallSimilarity() + "\t");
                System.out.println(saRes.NGramGraphSimil.ContainmentSimilarity + "\t" +
                        saRes.NGramGraphSimil.ValueSimilarity + "\t" +
                        saRes.NGramGraphSimil.SizeSimilarity + "\t" +
                        saRes.NGramHistoSimil.ContainmentSimilarity + "\t" +
                        saRes.NGramHistoSimil.ValueSimilarity + "\t" +
                        saRes.NGramHistoSimil.SizeSimilarity + "\t" + 
                        saRes.NGramOverallSimil.getOverallSimilarity());                    
            }
                
            System.err.println("Completed " + String.format("%7.4f", (double)iCur++ / iTotal * 100) + "%");
        }
        
    }
 */
}

/** A runnable class that performs the actual comparison of a text to all model texts. */
class CalcSimilRunner implements Runnable {
    
    // Variables
    private int WordNGramSize_Min, WordNGramSize_Max, Word_Dmax;
    private int CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax;
    private CategorizedFileEntry CurEntry;
    private Semaphore Sem;
    private List CompareAgainst;
    private boolean DoCharNGrams, DoWordNGrams;
    private PrintStream OutStream;
    private boolean Silent, Progress;
    private summaryEvaluator Caller;
    private int WeightingMethod = -1;
    
    /** TODO */
    public CalcSimilRunner(int iWordNGramSize_Min, int iWordNGramSize_Max, int iWord_Dmax,
            int iCharacterNGramSize_Min, int iCharacterNGramSize_Max, int iCharacter_Dmax,
            CategorizedFileEntry cfeCurEntry, List lCompareAgainst, Semaphore sSem, 
            boolean dDoCharNGrams, boolean dDoWordNGrams, PrintStream psOutStream,
            boolean bSilent, summaryEvaluator seCaller, int iWeightingMethod,
            boolean bProgress) {
        WordNGramSize_Min = iWordNGramSize_Min;
        WordNGramSize_Max = iWordNGramSize_Max;
        Word_Dmax = iWord_Dmax;
        CharacterNGramSize_Min = iCharacterNGramSize_Min;
        CharacterNGramSize_Max = iCharacterNGramSize_Max;
        Character_Dmax = iCharacter_Dmax;        
        CurEntry = cfeCurEntry;
        CompareAgainst = lCompareAgainst;
        Sem = sSem;
        DoCharNGrams = dDoCharNGrams;
        DoWordNGrams = dDoWordNGrams;
        OutStream = psOutStream;
        Silent = bSilent;
        Caller = seCaller;
        WeightingMethod = iWeightingMethod;
        Progress = bProgress;
    }
    
    /** The executor function of the comparison. */
    public void run() {
        SimilarityArray sa;
        switch (WeightingMethod) {
            case summaryEvaluator.USE_DISTRO_AVERAGE_AS_WEIGHT:
                // Just compare, ignoring identical summary from models
               sa = Caller.calcDistroSimilarityMeasures(CurEntry,
                    CompareAgainst, true, OutStream, Sem, WordNGramSize_Min, WordNGramSize_Max, Word_Dmax,
                    CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, DoCharNGrams, DoWordNGrams, 
                    Silent);
                break;
            case summaryEvaluator.USE_OCCURENCES_AS_WEIGHT:
                // Just compare, ignoring identical summary from models
                sa = Caller.calcSimilarityMeasures(CurEntry,
                    CompareAgainst, true, OutStream, Sem, WordNGramSize_Min, WordNGramSize_Max, Word_Dmax,
                    CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, DoCharNGrams, DoWordNGrams, 
                    Silent);
                break;
            default:
              sa = new SimilarityArray();  
        }
    }
}