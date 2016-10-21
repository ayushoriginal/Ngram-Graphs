/*
 * summarySingleFileEvaluator.java
 *
 * Created on April 11, 2008, 10:11 AM
 *
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.ILoadableTextPrint;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramSymWinDocument;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/** This class provides a facility to compare a text with a set of model texts
 * returning the set of results of the individual comparisons and an overall 
 * similarity measure.
 * @author ggianna
 */
public class summarySingleFileEvaluator {
    String DocumentModelClassName;
    String ComparatorClassName;
    int MinNGramRank, MaxNGramRank, NGramDist;
    boolean AvoidSelfComparison, Jackknifing, Silent;
    /** Creates a new instance of summarySingleFileEvaluator, given a single 
     *summary text and a set of model texts.
     *@param sDocumentModelClassName The document model class name.
     *@param sComparatorClassName The comparator class name.
     *@param iMinNGramRank The min n-gram to take into account.
     *@param iMaxNGramRank The max n-gram to take into account.
     *@param iNGramDist The max distance to take into account for neighbourhood.
     */
    public summarySingleFileEvaluator(String sDocumentModelClassName, 
            String sComparatorClassName, 
            int iMinNGramRank, int iMaxNGramRank, int iNGramDist) 
    {
        DocumentModelClassName = sDocumentModelClassName;
        ComparatorClassName = sComparatorClassName;
        MinNGramRank = iMinNGramRank;
        MaxNGramRank = iMaxNGramRank;
        NGramDist = iNGramDist;
    }
    
    public summarySingleFileEvaluator() {
        DocumentModelClassName = NGramSymWinDocument.class.getName();
        ComparatorClassName = NGramCachedGraphComparator.class.getName();
        MinNGramRank = 3;
        MaxNGramRank = 4;
        NGramDist = 4;
        
    }
    
    /** Performs comparison between a (summary) text file and a set of model (summary)
     * text files. The comparison result is the average similarity of the given 
     * text to the individuals of the text set.
     * @param sSummaryTextFile The filename of the text file to use.
     * @param ssModelFiles A set of strings, containing the filenames of the model
     *  texts.
     * @return A double value indicating the average <b>value</b> similarity 
     * between the given text and the model texts.
     */
    public double doCompare(String sSummaryTextFile, Set<String> ssModelFiles) {
        ILoadableTextPrint ndNDoc1 = null;
        try {
            int iIdx = utils.getConstructor(DocumentModelClassName,3);
            if (iIdx > -1)
                ndNDoc1 = (ILoadableTextPrint)Class.forName(DocumentModelClassName).getConstructors()
                    [iIdx].newInstance(MinNGramRank, MaxNGramRank, NGramDist);
            else {
                iIdx = utils.getConstructor(DocumentModelClassName,5);
                ndNDoc1 = (ILoadableTextPrint)Class.forName(DocumentModelClassName).getConstructors()
                    [iIdx].newInstance(MinNGramRank, MaxNGramRank, NGramDist, 
                        MinNGramRank, MaxNGramRank);
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
            return Double.NEGATIVE_INFINITY;
        
        // Read first file        
        ndNDoc1.loadDataStringFromFile(sSummaryTextFile);

        File fSummaryFile = new File(sSummaryTextFile);
        
        // Init Comparator Class        
        SimilarityComparatorListener sdcNComparator = null;
        try {
            int iIdx = utils.getConstructor(ComparatorClassName,1);
            if (iIdx > -1)
                sdcNComparator = (SimilarityComparatorListener)
                    Class.forName(ComparatorClassName).getConstructors()
                    [iIdx].newInstance(1.0); // Graph only
            else
                sdcNComparator = 
                    (SimilarityComparatorListener)Class.forName(ComparatorClassName).newInstance();
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
            return Double.NEGATIVE_INFINITY;
        
        Iterator<String> iOtherIter = ssModelFiles.iterator();

        if (Jackknifing) {
            Distribution<Double> dJackResults = new Distribution<Double>();

            for (int iLeaveOneOutCnt = 0; iLeaveOneOutCnt < ssModelFiles.size();
             iLeaveOneOutCnt++) {
                // Init return struct
                Distribution dFoldSims = new Distribution(); // Distro of results

                int iCurIdx = 0;
                iOtherIter = ssModelFiles.iterator();
                while (iOtherIter.hasNext()) {
                    String sModelFile = iOtherIter.next();
                    // Skip self-comparison if asked
                    if ((new File(sModelFile).getName().equals(fSummaryFile.getName()) &&
                            AvoidSelfComparison) ||
                            (iCurIdx++ == iLeaveOneOutCnt))
                    {
                        if (!Silent)
                            System.err.println(String.format("Skipping '%s' to '%s' comparison",
                                sModelFile, fSummaryFile));
                        continue;
                    }
                    // Load model data
                    // Init document class
                    ILoadableTextPrint ndNDoc2 = null;

                    try
                    {
                        int iIdx = utils.getConstructor(DocumentModelClassName,3);
                        if (iIdx > -1)
                            ndNDoc2 = (ILoadableTextPrint)Class.forName(DocumentModelClassName).getConstructors()
                                [iIdx].newInstance(MinNGramRank, MaxNGramRank, NGramDist);
                        else {
                            iIdx = utils.getConstructor(DocumentModelClassName,5);
                            ndNDoc2 = (ILoadableTextPrint)Class.forName(DocumentModelClassName).getConstructors()
                                [iIdx].newInstance(MinNGramRank, MaxNGramRank, NGramDist,
                                    MinNGramRank, MaxNGramRank);
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
                        return Double.NEGATIVE_INFINITY;
                    ndNDoc2.loadDataStringFromFile(sModelFile);

                    // Save and Output results
                    try {
                        GraphSimilarity sSimil = null;
                        // Get simple text similarities
                        sSimil = (GraphSimilarity)sdcNComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);
                        dFoldSims.increaseValue(sSimil.ValueSimilarity, 1.0);
                    }
                    catch (InvalidClassException iceE) {
                        System.err.println("Cannot compare..." +
                                iceE.getMessage());
                    }

                }
                // Update jackknifing results
                dJackResults.increaseValue(dFoldSims.average(false), 1.0);
            }
            
            return dJackResults.average(false);
        }

        Distribution<Double> dRes = new Distribution<Double>();
        while (iOtherIter.hasNext()) {
            String sModelFile = iOtherIter.next();
            // Skip self-comparison if asked
            if (new File(sModelFile).getName().equals(fSummaryFile.getName()) &&
                    AvoidSelfComparison)
            {
                if (!Silent)
                    System.err.print(String.format("Skipping '%s' to '%s' comparison",
                        sModelFile, fSummaryFile));
                continue;
            }
            // Load model data
            // Init document class
            ILoadableTextPrint ndNDoc2 = null;

            try
            {
                int iIdx = utils.getConstructor(DocumentModelClassName,3);
                if (iIdx > -1)
                    ndNDoc2 = (ILoadableTextPrint)Class.forName(DocumentModelClassName).getConstructors()
                        [iIdx].newInstance(MinNGramRank, MaxNGramRank, NGramDist);
                else {
                    iIdx = utils.getConstructor(DocumentModelClassName,5);
                    ndNDoc2 = (ILoadableTextPrint)Class.forName(DocumentModelClassName).getConstructors()
                        [iIdx].newInstance(MinNGramRank, MaxNGramRank, NGramDist,
                            MinNGramRank, MaxNGramRank);
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
                return Double.NEGATIVE_INFINITY;
            ndNDoc2.loadDataStringFromFile(sModelFile);

            // Save and Output results
            try {
                GraphSimilarity sSimil = null;
                // Get simple text similarities
                sSimil = (GraphSimilarity)sdcNComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);
                dRes.increaseValue(sSimil.ValueSimilarity, 1.0);
            }
            catch (InvalidClassException iceE) {
                System.err.println("Cannot compare...");
            }

        }

        return dRes.average(false);
    }
    
    /** Performs comparison between the graph representation of a (summary) 
     * text file and a set of (model summary) text files. 
     * The comparison result is the similarity of the given 
     * text to the union of the representation of the texts in the text set.
     * @param sSummaryTextFile The filename of the text file to use.
     * @param ssModelFiles A set of strings, containing the filenames of the model
     *  texts.
     * @return A double value indicating the <b>normalized value</b> similarity 
     * between the given text representation and the model texts set representation.
     */
    public double doGraphCompareToSet(String sSummaryTextFile, 
            Set<String> ssModelFiles, String sGraphModelClassName, 
            String sComparatorClassName, int iMinNGramRank, int iMaxNGramRank, 
            int iNGramDist) {
        DocumentNGramGraph ndNDoc1 = null;
        try {
            int iIdx = utils.getConstructor(sGraphModelClassName,3);
            if (iIdx > -1)
                ndNDoc1 = (DocumentNGramGraph)Class.forName(sGraphModelClassName).getConstructors()
                    [iIdx].newInstance(iMinNGramRank, iMaxNGramRank, iNGramDist);
            else {
                iIdx = utils.getConstructor(sGraphModelClassName,5);
                ndNDoc1 = (DocumentNGramGraph)Class.forName(sGraphModelClassName).getConstructors()
                    [iIdx].newInstance(iMinNGramRank, iMaxNGramRank, iNGramDist, 
                        iMinNGramRank, iMaxNGramRank);
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
            return Double.NEGATIVE_INFINITY;
        try {
            // Read first file
            ndNDoc1.loadDataStringFromFile(sSummaryTextFile);
        } catch (IOException ex) {
            Logger.getLogger(summarySingleFileEvaluator.class.getName()
                    ).log(Level.SEVERE, null, ex);
            return Double.NEGATIVE_INFINITY;
        } 
        
        // Init Comparator Class        
        SimilarityComparatorListener sdcNComparator = null;
        try {
            int iIdx = utils.getConstructor(sComparatorClassName,1);
            if (iIdx > -1)
                sdcNComparator = (SimilarityComparatorListener)
                    Class.forName(sComparatorClassName).getConstructors()
                    [iIdx].newInstance(1.0); // Graph only
            else
                sdcNComparator = 
                    (SimilarityComparatorListener)Class.forName(
                    sComparatorClassName).newInstance();
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
            return Double.NEGATIVE_INFINITY;

        if (!Jackknifing) {
            DocumentNGramGraph ndNModel = null;
            Iterator<String> iOtherIter = ssModelFiles.iterator();
            int iDocCnt = 0;
            while (iOtherIter.hasNext())
            {
                String sModelFile = iOtherIter.next();
                // Load model data
                // Init document class
                DocumentNGramGraph ndNDoc2 = null;

                try
                {
                    int iIdx = utils.getConstructor(sGraphModelClassName,3);
                    if (iIdx > -1)
                        ndNDoc2 = (DocumentNGramGraph)Class.forName(
                                sGraphModelClassName).getConstructors()
                            [iIdx].newInstance(iMinNGramRank, iMaxNGramRank,
                            iNGramDist);
                    else {
                        iIdx = utils.getConstructor(sGraphModelClassName,5);
                        ndNDoc2 = (DocumentNGramGraph)Class.forName(
                                sGraphModelClassName).getConstructors()
                            [iIdx].newInstance(iMinNGramRank, iMaxNGramRank,
                            iNGramDist, iMinNGramRank, iMaxNGramRank);
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
                    return Double.NEGATIVE_INFINITY;

                try {
                    ndNDoc2.loadDataStringFromFile(sModelFile);
                } catch (IOException ex) {
                    Logger.getLogger(summarySingleFileEvaluator.class.getName()
                            ).log(Level.SEVERE, null, ex);
                    return Double.NEGATIVE_INFINITY;
                }

                ++iDocCnt;
                if (ndNModel == null)
                    ndNModel = ndNDoc2;
                else
                    ndNModel.merge(ndNDoc2, 1.0 - (iDocCnt / ssModelFiles.size()));
            }

            // Save and Output results
            GraphSimilarity sSimil = null;
            try {
                // Get simple text similarities
                sSimil = (GraphSimilarity)sdcNComparator.getSimilarityBetween(ndNDoc1,
                        ndNModel);
            }
            catch (InvalidClassException iceE) {
                System.err.println("Cannot compare...");
                return Double.NEGATIVE_INFINITY;
            }

            // Return the normalized value similarity
            return (sSimil.SizeSimilarity == 0) ? 0 :
                (sSimil.ValueSimilarity / sSimil.SizeSimilarity);
        }
        else // if jackknifing
        {
            Distribution<Double> dJackknifingResults = new Distribution<Double>();
            DocumentNGramGraph ndNModel = null;
            // For each combination
            for (int iLeaveOutCnt=0; iLeaveOutCnt < ssModelFiles.size();
                iLeaveOutCnt++)
            {
                Distribution<Double> dFoldRes = new Distribution<Double>();
                Iterator<String> iOtherIter = ssModelFiles.iterator();
                int iDocCnt = 0;
                int iCurModelCnt=0;

                // For every model in combination
                while (iOtherIter.hasNext())
                {
                    String sModelFile = iOtherIter.next();
                    // Omit leave-out model
                    if (iCurModelCnt++ == iLeaveOutCnt) {
                        if (!Silent)
                            System.err.println(String.format("Skipping '%s' to " +
                              "'%s' comparison", sModelFile, sSummaryTextFile));
                        continue;
                    }
                    
                    // Load model data
                    // Init document class
                    DocumentNGramGraph ndNDoc2 = null;

                    try
                    {
                        int iIdx = utils.getConstructor(sGraphModelClassName,3);
                        if (iIdx > -1)
                            ndNDoc2 = (DocumentNGramGraph)Class.forName(
                                    sGraphModelClassName).getConstructors()
                                [iIdx].newInstance(iMinNGramRank, iMaxNGramRank,
                                iNGramDist);
                        else {
                            iIdx = utils.getConstructor(sGraphModelClassName,5);
                            ndNDoc2 = (DocumentNGramGraph)Class.forName(
                                    sGraphModelClassName).getConstructors()
                                [iIdx].newInstance(iMinNGramRank, iMaxNGramRank,
                                iNGramDist, iMinNGramRank, iMaxNGramRank);
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
                        return Double.NEGATIVE_INFINITY;

                    try {
                        ndNDoc2.loadDataStringFromFile(sModelFile);
                    } catch (IOException ex) {
                        Logger.getLogger(summarySingleFileEvaluator.class.getName()
                                ).log(Level.SEVERE, null, ex);
                        return Double.NEGATIVE_INFINITY;
                    }

                    ++iDocCnt;
                    if (ndNModel == null)
                        ndNModel = ndNDoc2;
                    else
                        ndNModel.merge(ndNDoc2, 1.0 - (iDocCnt / ssModelFiles.size()));
                }
                // Compare model to summary
                // Save and Output results
                GraphSimilarity sSimil = null;
                try {
                    // Get simple text similarities
                    sSimil = (GraphSimilarity)sdcNComparator.getSimilarityBetween(ndNDoc1,
                            ndNModel);
                }
                catch (InvalidClassException iceE) {
                    System.err.println("Cannot compare...");
                    return Double.NEGATIVE_INFINITY;
                }

                // Return the normalized value similarity
                double dTmpRes = (sSimil.SizeSimilarity == 0) ? 0.0 :
                    (sSimil.ValueSimilarity / sSimil.SizeSimilarity);
                // Add result to fold results
                dJackknifingResults.increaseValue(dTmpRes, 1.0);
            }
            return dJackknifingResults.average(false);
        }
        
    }
    
    /** Provides command-line syntax information for the main function. */
    private static void printUsage() {
            System.err.println("Syntax:\nsummaryEvaluator [-summary=summary.txt] [-modelDir=models/]"+
                    "[-nMin=#] [-nMax=#] [-dist=#]" + 
                    "[-s] [-docClass=...] [-compClass=...] [-merge]");
            System.err.println("nMin=#\tMin n-gram size.\nnMax=#\tMax n-gram size.\n" +
                    "dist=#\tN-gram window.\n" +
                    "-s\tFor non-verbose output (silent).\n" +
                    "-docClass=...\tA java class identifier to use as Document class. " +
                        "Defaults to jinsect.documentModel.NGramDocument \n" +
                    "-compClass=...\tA java class identifier to use as Comparator class. " +
                        "Defaults to jinsect.documentModel.NGramCachedGraphComparator \n" +
                    "-merge\tIf indicated then the model files' representation is merged" +
                    " to provide an overall model graph. Then comparison is performed" +
                    " with respect to the overall graph." +
                    "-jack\tUse jackknifing in comparison (round robin comparison with leave-one-out)" +
                    "to the model summaries. The resulting score is the mean of the scores." +
                    "-?\tShow this screen.");
    }
    
    /** Main function for usage from the command line.
     * @param args The command line arguments
     */
    public static void main(String[] args) {
       // Parse commandline
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);
        if (utils.getSwitch(hSwitches,"?", "").length() > 0) {
            printUsage();
            System.exit(0);
        }
        
        // Vars
        int NMin, NMax, Dist;
        String DocumentClass, ComparatorClass, SummaryFile, ModelDir, ModelFiles,
                PrependPerLine;
        boolean Silent, Merge, bAvoidSelfComparison, bJack;
        
        try {
            // Determine if silent
            Silent=utils.getSwitch(hSwitches, "s", "FALSE").equals("TRUE");

            NMin = Integer.valueOf(utils.getSwitch(hSwitches,"nMin", "3"));
            NMax = Integer.valueOf(utils.getSwitch(hSwitches,"nMax", "3"));
            Dist = Integer.valueOf(utils.getSwitch(hSwitches,"dist", "3"));
            DocumentClass = utils.getSwitch(hSwitches,"docClass",
                    NGramSymWinDocument.class.getName());
            ComparatorClass = utils.getSwitch(hSwitches,"compClass", 
                    StandardDocumentComparator.class.getName()); 
            // Get summary and model dir
            SummaryFile = utils.getSwitch(hSwitches, "summary", "summary.txt");
            ModelDir = utils.getSwitch(hSwitches, "modelDir", "models" + 
                    System.getProperty("file.separator"));
            ModelFiles = utils.getSwitch(hSwitches, "models", "");
            if (ModelFiles.length() > 0) {
                if (!Silent)
                System.err.println("Model files explicitly declared. Ignoring" +
                        " model dir.");
            }
            Merge=utils.getSwitch(hSwitches, "merge", "FALSE").equals("TRUE");
            bAvoidSelfComparison = utils.getSwitch(hSwitches,
                    "avoidSelfComparison", "FALSE").equals("TRUE");
            bJack = utils.getSwitch(hSwitches,
                    "jack", "FALSE").equals("TRUE");
            PrependPerLine = utils.getSwitch(hSwitches, "prepend", "");

            if (bJack && bAvoidSelfComparison)
                System.err.println("WARNING: Both jackknifing and self-comparison " +
                        "avoidance have been requested.");
            
            if (!Silent)
                System.err.println("Using parameters:\n" + hSwitches);
            
        }
        catch (ClassCastException cce) {
            System.err.println("Malformed switch:" + cce.getMessage() + ". Aborting...");
            printUsage();
            return;
        }
        summarySingleFileEvaluator ssfeEval = new summarySingleFileEvaluator(
                DocumentClass, ComparatorClass, NMin, NMax, Dist);
        ssfeEval.AvoidSelfComparison = bAvoidSelfComparison;
        ssfeEval.Jackknifing = bJack;
        ssfeEval.Silent = Silent;
        
        Set<String> ssModels;
        double dRes = Double.NaN;

        // If model dir used
        if (ModelFiles.length() == 0) {
            DocumentSet dsModels = new DocumentSet(ModelDir, 1.0);
            dsModels.createSets(true);

            // Use ordered set. Needed for Jackknifing
            ssModels = new TreeSet<String>(dsModels.toFilenameSet(
                DocumentSet.FROM_WHOLE_SET));
        }
        else // if explicit files were given
        {
            ssModels = new TreeSet<String>(Arrays.asList(ModelFiles.split(";")));
            Iterator<String> iModels = ssModels.iterator();
            // Remove empty strings
            while (iModels.hasNext()) {
                String sCandModel = iModels.next();
                if (sCandModel.trim().length() == 0) {
                    ssModels.remove(sCandModel);
                    iModels = ssModels.iterator();
                }
            }
        }

        if (!Merge)
            dRes = ssfeEval.doCompare(SummaryFile, ssModels);
        else
            dRes = ssfeEval.doGraphCompareToSet(SummaryFile,
                ssModels, DocumentClass, ComparatorClass, NMin, NMax, Dist);

        synchronized (System.out) {
            System.out.println(String.format("%s\t%10.8f", PrependPerLine +
                new File(SummaryFile).getName(),
                dRes));
        }
    }
    
    
    
}
