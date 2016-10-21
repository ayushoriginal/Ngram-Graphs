/*
 * summaryGenericEvaluator.java
 *
 * Created on June 14, 2007, 1:04 PM
 *
 */

package gr.demokritos.iit.jinsect.console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.documentModel.ILoadableTextPrint;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;
import gr.demokritos.iit.jinsect.threading.ThreadList;

/** A generic class for summary evaluation, which can host many different
 * methods, through a single interface.
 * @author ggianna
 */
public class summaryGenericEvaluator {
    // Variables
    protected Integer NMin, NMax, Dist, Threads;
    protected String OutFile, SummaryDir, ModelDir, DocumentClass, 
            ComparatorClass;
    protected boolean Silent, Progress;
    protected Semaphore OutputSemaphore;
    protected Hashtable hModelCache = new Hashtable();
    protected Hashtable hNModelCache = new Hashtable();
    
    /** 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // doRougeLikeEval();
        summaryGenericEvaluator sCur = new summaryGenericEvaluator(args);        
        sCur.run();
    }
    
    /** Provides command-line syntax information for the main function. */
    private static void printUsage() {
            System.err.println("Syntax:\nsummaryEvaluator [-summaryDir=summaries/] [-modelDir=models/]"+
                    "[-nMin=#] [-nMax=#] [-dist=#] [-t=#]" + 
                    "[-s] [-use=o|d] [-progress] [-docClass=...] [-compClass=...]");
            System.err.println("nMin=#\tMin n-gram size.\nnMax=#\tMax n-gram size.\n" +
                    "dist=#\tN-gram window.\n" +
                    "-o=outFile\tThe file to output data. Default is stdout.\n" +
                    "-t=#\tNumber of threads. Defaults to number of available processors declared by system.\n" +
                    "-s\tFor non-verbose output (silent).\n" +
                    "-progress\tFor progress indication (even in silent mode).\n" +
                    "-docClass=...\tA java class identifier to use as Document class. " +
                        "Defaults to jinsect.documentModel.NGramDocument \n" +
                    "-compClass=...\tA java class identifier to use as Comparator class. " +
                        "Defaults to jinsect.documentModel.StandardDocumentComparator \n" +
                    "-?\tShow this screen.");
    }

    /** Creates a new instance of summaryEvaluator, given an array of 
     * command-line like parameters.
     */
    public summaryGenericEvaluator(String[] args) {
        OutputSemaphore = new Semaphore(1);
        
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        if (gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"?", "").length() > 0) {
            printUsage();
            System.exit(0);
        }
            
        // Parse commandline
        try {
            NMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"nMin", "3"));
            NMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"nMax", "5"));
            Dist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"dist", "3"));
            Threads = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"t", 
                    "" + Runtime.getRuntime().availableProcessors()));
            DocumentClass = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"docClass", NGramDocument.class.getName()); 
            ComparatorClass = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"compClass", 
                    StandardDocumentComparator.class.getName()); 
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
    
    /**
     * Creates a summaryGenericEvaluator, given a document class name, 
     * a comparator class name
     * and a set of parameters indicating how to perform the evaluation. The document class
     * can be any class that inherits from {@link ILoadableTextPrint}, while the
     * comparator can be any offspring of {@link SimilarityComparatorListener}.
     *@see ILoadableTextPrint
     *@see SimilarityComparatorListener
     */
    public summaryGenericEvaluator(Semaphore sOutputSemaphore, int iNMin, int iNMax, int iDist,
        int iThreads, String sOutFile, String sSummaryDir, String sModelDir, 
            String sDocumentClass, String sComparatorClass, boolean bSilent, 
            boolean bProgress)
    {
        OutputSemaphore = sOutputSemaphore;
        NMin = iNMin;
        NMax = iNMax;
        Dist = iDist;
        Threads = iThreads;
        OutFile = sOutFile;
        SummaryDir = sSummaryDir;
        ModelDir = sModelDir;
        DocumentClass = sDocumentClass;
        ComparatorClass = sComparatorClass;
        Silent = bSilent;
        Progress = bProgress;
    }
    
    /** Performs optimized evaluation of the documents in given dirs, given
     *  a document and a comparator class name, as well as a set of evaluation
     *  parameters.
     *@param sSem A semaphore that ensures that the output is provided consistently.
     *@param pOverallResultsOutStream The output stream for results.
     *@param NGramSize_Min The min n-gram rank to take into account, if applicable to the method.
     *@param NGramSize_Max The max n-gram rank to take into account, if applicable to the method.
     *@param Dmax The n-gram neighbourhood distance to use, if applicable to the method.
     *@param sSummaryDir The peer summary base directory.
     *@param sModelDir The model summaries base directory.
     *@param iThreads The umber of threads to use for parallel computation.
     *@param DocumentClass The dodcment class by which to represent texts.
     *@param ComparatorClass The comparator class to use for the evaluation.
     *@param bSilent If true, no debug messages are output.
     *@param bProgress If true, indicates that progress indication should be output, even in silent
     * mode.
     */
    protected void doOptimizedEval(final Semaphore sSem, PrintStream pOverallResultsOutStream,
            int NGramSize_Min, int NGramSize_Max, int Dmax,
            String sSummaryDir, String sModelDir, int iThreads,
            String DocumentClass, String ComparatorClass,
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
        pOverallResultsOutStream.print("GraphCooccurence\t");
        pOverallResultsOutStream.print("GraphValue\t");
        pOverallResultsOutStream.print("GraphSize\t");
        pOverallResultsOutStream.print("HistoContainmentSimilarity\t");
        pOverallResultsOutStream.print("HistoValue\t");
        pOverallResultsOutStream.print("HistoSize\t");
        pOverallResultsOutStream.print("OverallSimil\t");
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
                
                Runnable r = new GenericCalcSimilRunner(NGramSize_Min, NGramSize_Max, Dmax, cfeCur,
                        lModelFiles, sSem, pOverallResultsOutStream, bSilent, this, 
                        DocumentClass, ComparatorClass,
                        Progress);
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
    
    /** Executor of the evaluation. */
    public void run() {
        PrintStream pOut = null;
        if (OutFile.length() != 0) {
            try {
                pOut = new PrintStream(OutFile);
            }
            catch (FileNotFoundException fnfe) {
                System.err.println("Cannot output to selected file:\n" + fnfe.getMessage());
                System.exit(1);
            }
        }

        //doNormalEval(OutputSemaphore, pOut, WordMin, WordMax, WordDist, CharMin, CharMax, CharDist,
                //Do.equals("char") || Do.equals("all"), Do.equals("word") || Do.equals("all"), SummaryDir, ModelDir, 
                //Threads, Silent);
        doOptimizedEval(OutputSemaphore, pOut, NMin, NMax, Dist,
            SummaryDir, ModelDir, Threads, DocumentClass, ComparatorClass,
            Silent, Progress);
        
        if (pOut!=null)
            pOut.close();
    }
    
    /** Calculates similarity measures between a file entry and a set of 
     *  model documents, using default representation ({@link NGramDocument})
     * and default comparator ({@link StandardDocumentComparator}) classes.
     */
    protected SimilarityArray calcSimilarityMeasures(CategorizedFileEntry cfeCur, List dsModelSet, 
            boolean bOutput, Semaphore sSem) {
        return calcSimilarityMeasures(cfeCur, dsModelSet, bOutput, null, sSem, 
                NGramDocument.class.getName(), StandardDocumentComparator.class.getName(), 
                1, 2, 3, true);
    }
    
    /** Calculates similarity measures between a file entry and a set of 
     *  model documents.
     *@param cfeCur The file entry to compare to models.
     *@param dsModelSet The set of models to compare against.
     *@param bOutput If true verbose output is selected.
     *@param pOut The output stream for results.
     *@param sSem The semaphore that assures consistent output.
     *@param sDocumentClass The name of the document class to represent
     *  documents by. Should inherit from {@link ILoadableTextPrint}.
     *@param sEvaluatorClass The name of the evaluator class to use. 
     *Should inherit from SimilarityComparatorListener.
     *@param NGramSize_Min The min n-gram rank to use.
     *@param NGramSize_Max The max n-gram rank to use.
     *@param bSilent If true, minimal debug output is given.
     *@return The result similarity array.
     */
    protected SimilarityArray calcSimilarityMeasures(CategorizedFileEntry cfeCur, List dsModelSet, 
            boolean bOutput, PrintStream pOut, Semaphore sSem,  
            String sDocumentClass, String sEvaluatorClass,
            int NGramSize_Min, int NGramSize_Max, int Dmax, boolean bSilent) {
        
        if (pOut == null)
            pOut = System.out;
        
        // Init return struct
        SimilarityArray saRes = new SimilarityArray();
        ILoadableTextPrint ndNDoc1 = null;
        try {
            int iIdx = getConstructor(DocumentClass,3);
            if (iIdx > -1)
                ndNDoc1 = (ILoadableTextPrint)Class.forName(DocumentClass).getConstructors()
                    [iIdx].newInstance(NGramSize_Min, NGramSize_Max, Dmax);
            else {
                iIdx = getConstructor(DocumentClass,5);
                ndNDoc1 = (ILoadableTextPrint)Class.forName(DocumentClass).getConstructors()
                    [iIdx].newInstance(NGramSize_Min, NGramSize_Max, Dmax, NGramSize_Min,
                        NGramSize_Max);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace(System.err);
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        } catch (InstantiationException ex) {
            ex.printStackTrace(System.err);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace(System.err);
        } catch (InvocationTargetException ex) {
            ex.printStackTrace(System.err);
        }
        if (ndNDoc1 == null)
            return new SimilarityArray();
        
        // Read first file        
        ndNDoc1.loadDataStringFromFile(cfeCur.getFileName());
        
        // Init Comparator Class        
        SimilarityComparatorListener sdcNComparator = null;
        try {
            int iIdx = getConstructor(ComparatorClass,1);
            if (iIdx > -1)
                sdcNComparator = (SimilarityComparatorListener)
                    Class.forName(ComparatorClass).getConstructors()
                    [iIdx].newInstance(1.0); // Graph only
            else
                sdcNComparator = 
                    (SimilarityComparatorListener)Class.forName(ComparatorClass).newInstance();
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace(System.err);
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        } catch (InstantiationException ex) {
            ex.printStackTrace(System.err);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace(System.err);
        } catch (InvocationTargetException ex) {
            ex.printStackTrace(System.err);
        }        
        if (sdcNComparator == null)
            return new SimilarityArray();
        
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
            // Init document class
            ILoadableTextPrint ndNDoc2 = null;
                    
            synchronized (hModelCache) {
                // Look up cache
                if (hModelCache.containsKey(cfeOther.getFileName()))
                    ndNDoc2 = (ILoadableTextPrint)hModelCache.get(cfeOther.getFileName());
                else {
                    try {
                        int iIdx = getConstructor(DocumentClass,3);
                        if (iIdx > -1)
                            ndNDoc2 = (ILoadableTextPrint)Class.forName(DocumentClass).getConstructors()
                                [iIdx].newInstance(NGramSize_Min, NGramSize_Max, Dmax);
                        else {
                            iIdx = getConstructor(DocumentClass,5);
                            ndNDoc2 = (ILoadableTextPrint)Class.forName(DocumentClass).getConstructors()
                                [iIdx].newInstance(NGramSize_Min, NGramSize_Max, Dmax, NGramSize_Min,
                                    NGramSize_Max);
                        }
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace(System.err);
                    } catch (SecurityException ex) {
                        ex.printStackTrace(System.err);
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace(System.err);
                    } catch (InstantiationException ex) {
                        ex.printStackTrace(System.err);
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace(System.err);
                    } catch (InvocationTargetException ex) {
                        ex.printStackTrace(System.err);
                    }
                    if (ndNDoc2 == null)
                        return new SimilarityArray();
                    ndNDoc2.loadDataStringFromFile(cfeOther.getFileName());
                    hModelCache.put(cfeOther.getFileName(), ndNDoc2);
                }
            }
                
            // Yield
            Thread.yield();
                                    
            // Save and Output results
            try {
                GraphSimilarity sSimil = null;
                // Get simple text similarities
                sSimil = (GraphSimilarity)sdcNComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);

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
                            pOut.print(sSimil.ContainmentSimilarity + "\t" +
                                sSimil.ValueSimilarity + "\t" +
                                sSimil.SizeSimilarity + "\t" +
                                "0.0\t" + //sSimil.ContainmentSimilarity + "\t" +
                                "0.0\t" + //sSimil.ValueSimilarity + "\t" +
                                "0.0\t" + //sSimil.SizeSimilarity + "\t" + 
                                sSimil.getOverallSimilarity());                    
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

    /** Returns the index of the constructor of a class, given its 
     * parameter count.
     *@param sClassName The class name of interest.
     *@param iParams The parameter count to look for.
     *@return The index of the constructor in the 
     *Class.forName(sClassName).getConstructors() array.
     */
    protected int getConstructor(String sClassName, int iParams) {
        int iCnt = -1;
        try {
            for (iCnt=0; iCnt < Class.forName(sClassName).getConstructors().length; iCnt++)
                if (Class.forName(sClassName).getConstructors()[iCnt].getParameterTypes().length == iParams)
                    return iCnt;
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
        return -1;
    }
}


class GenericCalcSimilRunner implements Runnable {
    
    // Variables
    int NGramSize_Min, NGramSize_Max, Dmax;
    CategorizedFileEntry CurEntry;
    Semaphore Sem;
    List CompareAgainst;
    PrintStream OutStream;
    boolean Silent, Progress;
    summaryGenericEvaluator Caller;
    String DocumentClass = NGramDocument.class.getName();
    String ComparatorClass = StandardDocumentComparator.class.getName();
    
    public GenericCalcSimilRunner(int iNGramSize_Min, int iNGramSize_Max, int iDmax,
            CategorizedFileEntry cfeCurEntry, List lCompareAgainst, Semaphore sSem, 
            PrintStream psOutStream, boolean bSilent, summaryGenericEvaluator seCaller, 
            String sDocumentClass, String sComparatorClass,
            boolean bProgress) {
        NGramSize_Min = iNGramSize_Min;
        NGramSize_Max = iNGramSize_Max;
        Dmax = iDmax;        
        CurEntry = cfeCurEntry;
        CompareAgainst = lCompareAgainst;
        Sem = sSem;
        OutStream = psOutStream;
        Silent = bSilent;
        Caller = seCaller;
        Progress = bProgress;
        DocumentClass = sDocumentClass;
        ComparatorClass = sComparatorClass;
    }
    public void run() {
        SimilarityArray sa;
        // Just compare, ignoring identical summary from models
        sa = Caller.calcSimilarityMeasures(CurEntry, CompareAgainst, true, OutStream, Sem, 
            DocumentClass, ComparatorClass, NGramSize_Min, NGramSize_Max, Dmax,                
            Silent);
    }
    
}