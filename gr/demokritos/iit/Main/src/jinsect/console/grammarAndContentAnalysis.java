/*
 * grammarAndContentAnalysis.java
 *
 * Created on December 6, 2007, 7:00 PM
 *
 */
package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramDistroGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;
import static gr.demokritos.iit.jinsect.utils.getSwitch;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A command-line facility class to perform grammar and content analysis of texts,
 * based on n-gram graphs.
 *
 * @author ggianna
 */
public class grammarAndContentAnalysis {
        // Constants
        final static protected String GRAMMAR_NAME = "OverallGrammar";
        final static protected String GRAMMAR_TYPE = "grammarGraph";
        final static protected String GRAMMAR_DOCS_PARAM = "GrammarDocsParam";
        final static protected String PARAM_TYPE = "params";
        final static protected String CATEGORY_MODEL_TYPE = "categoryGraph";

    // Cache
    static HashMap <String,DocumentNGramGraph> hCategoryGraphs = null;

    /** Loads a set of documents into a common {@link DocumentNGramDistroGraph}.
     *@param gGraph1 The graph to hold the representation of the set of documents.
     *@param iDocuments An {@link Iterable} object, that can iterate over the set of input
     * documents.
     */
    private static void loadTopicIntoGraph(DocumentNGramSymWinGraph gInGraph,
            Iterable iDocuments) {
        DocumentNGramGraph gGraph1 = (DocumentNGramGraph) gInGraph.clone();
        double dDocCnt = 0.0;

        // For every file in the set, for topic 1
        for (Object oCatFileEntry : iDocuments) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry) oCatFileEntry;
            // Init new graph
            DocumentNGramGraph gGraph2 = new DocumentNGramSymWinGraph(gGraph1.getMinSize(),
                    gGraph1.getMaxSize(), gGraph1.getWindowSize());
            try {
                // Load graph from file
                gGraph2.loadDataStringFromFile(cfeCur.getFileName());
            } catch (IOException ex) {
                System.err.println("Could not load file " + cfeCur.getFileName());
                ex.printStackTrace(System.err);
            }
            // Merge new graph to existing graph
            gGraph1.merge(gGraph2, dDocCnt == 0 ? 1.0 : dDocCnt / (dDocCnt + 1));
            dDocCnt++;

            // DEBUG LINES
            System.err.print("." + String.valueOf(dDocCnt));
        //////////////
        }
        // DEBUG LINES
        System.err.println(" Merging Complete");
    //////////////
    }

    /** Creates or reads a category graph and returns it.
     *
     * @param sName Category name.
     * @param sType The repository entry type.
     * @param db The {@link INSECTDB} repository object.
     * @param lCurrentDocs The set of documents for the category.
     * @param iMinNGram The minimum n-gram of the graph.
     * @param iMaxNGram The maximum n-gram of the graph.
     * @param iWindowSize The neighbourhood window size.
     * @param bLoad If true the graph will be attempted to be loaded.
     * @param bSave If true the graph will be saved after its creation.
     * @return The graph of the category.
     */
    public static DocumentNGramGraph getGraphFor(String sName, String sType,
            INSECTDB<DocumentNGramGraph> db, List<String> lCurrentDocs,
            int iMinNGram, int iMaxNGram, int iWindowSize,
            boolean bLoad, boolean bSave) {

        // Read selected document category into graph
        DocumentNGramGraph gCurGraph = null;

        boolean bLoadedOK = false;
        if (bLoad) {
            System.err.print("Loading graph for category " + sName +
                    "...");
            gCurGraph = db.loadObject(sName, sType);
            bLoadedOK = gCurGraph != null;
        }
        if (!bLoadedOK) {
            System.err.println("Failed to load graph. Creating...");
            gCurGraph = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram,
                    iWindowSize);
            loadTopicIntoGraph((DocumentNGramSymWinGraph) gCurGraph,
                    lCurrentDocs);
            if (bSave) {
                System.err.println("Saving content graph...");
                db.saveObject(gCurGraph, sName, sType);
            }
        }

        return gCurGraph;
    }

    /** Provides command-line syntax information for calling the class. */
    public static void printUsage() {
        System.out.println("Usage: " + grammarAndContentAnalysis.class.getName() +
                "[-corpusDir=dir" + System.getProperty("file.separator") + "] The base directory" +
                " of the corpus including the directory separator character.\n" +
//                "[-topic1Dir=dirName] [-topic2Dir=dirName] The names of the topics used in the " +
//                "comparison." +
                //                " -topic1Dir=dirName \t The subdirectory name of the 1st topic. Default is topic1.\n" +
                //                " -topic2Dir=dirName \t The subdirectory name of the 1st topic. Default is topic2.\n" +
                "[-categoriesForGrammar=#] \t The number of categories to use for grammar extraction. Default is 2.\n" +
                " -trainPercent=#.## \t The training percent to use for the grammar. Default is 0.66.\n" +
                " -minNGram=# \t The min n-gram rank. Default is 3.\n" +
                " -maxNGram=# \t The max n-gram rank. Default is 8.\n" +
                " -partOfCorpus=#.## \t The percent of corpus to use for all " +
                " experiments.\n" +
                " -windowSize=# \t The neighbourhood window. Default is max(2*minNGram, maxNGram+1).\n" +
                " -cache \t If selected will cache category graphs in memory. NOTE: Very memory consuming.");
    }

    /** Formats a {@link GraphSimilarity} object for output.
     *@param g The {@link GraphSimilarity} object.
     *@return A formatted string representation of the given {@link GraphSimilarity} object.
     */
    private static final String formatSimilarity(GraphSimilarity g) {
        double[] daSimils = g.toArray();
        return String.format("%8.6f\t%8.6f\t%8.6f", daSimils[0], daSimils[1], daSimils[2]);
    }

    /** Performs a grammar and content comparison between sets of documents. Grammar is defined
     * as the conjunction of the n-gram graphs of a set of topic graphs. Content is defined as the
     * delta of the n-gram graph of each document from the grammar.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        // Check if help is needed.
        if (getSwitch(hSwitches, "help", "").length() > 0) {
            printUsage();
            System.exit(0);
        }

        // Select grammar corpus
        String sCorpusDir = getSwitch(hSwitches, "corpusDir", "." +
                System.getProperty("file.separator"));
        // Select topic 1
        //String sTopic1Dir = getSwitch(hSwitches, "topic1", "topic1");
        // Select subject 2
        //String sTopic2Dir = getSwitch(hSwitches, "topic2", "topic2");

        int iMinNGram = Integer.valueOf(getSwitch(hSwitches, "minNGram", "3")).intValue();
        int iMaxNGram = Integer.valueOf(getSwitch(hSwitches, "maxNGram", "8")).intValue();
        //int iComparisonsPerCategory = Integer.valueOf(getSwitch(hSwitches, "compPerCat", "1")).intValue();
        int iCategoriesForGrammar = Integer.valueOf(getSwitch(hSwitches,
                "categoriesForGrammar", "2")).intValue();
        // Window size is at least [max n-gram size] + 1.
        int iWindowSize = Integer.valueOf(getSwitch(hSwitches, "windowSize",
                String.valueOf(2 * iMinNGram <= iMaxNGram ? iMaxNGram + 1 : 2 * iMinNGram))).intValue();
        double dTrainPercent = Double.valueOf(getSwitch(hSwitches, "trainPercent", "0.20")).doubleValue();
        double dPartOfCorpus = Double.valueOf(getSwitch(hSwitches, "partOfCorpus", "1.00")).doubleValue();
        boolean bSave = Boolean.valueOf(getSwitch(hSwitches, "save",
                String.valueOf(false))).booleanValue();
        boolean bLoad = Boolean.valueOf(getSwitch(hSwitches, "load",
                String.valueOf(false))).booleanValue();
        boolean bCache = Boolean.valueOf(getSwitch(hSwitches, "cache",
                String.valueOf(false))).booleanValue();
        if (bCache)
            hCategoryGraphs = new HashMap <String,DocumentNGramGraph>();
        String sModelDir = getSwitch(hSwitches, "modelDir", "./models/");

        // Determine output
        String sOutFile = getSwitch(hSwitches, "o", "");
        if (sOutFile.length() > 0) {
            try {
                PrintStream pOut = new PrintStream(new FileOutputStream(
                        new File(sOutFile)));
                System.setOut(pOut);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(grammarAndContentAnalysis.class.getName()).log(
                        Level.SEVERE, null, ex);
                System.err.println("Could not redirect to output file. Using " +
                        "standard output.");
            }
        }

        // Create doc set
        DocumentSet dsCorpus = new DocumentSet(sCorpusDir, dTrainPercent);
        dsCorpus.createSets(true, dPartOfCorpus);

        // Init file repository
        INSECTFileDB<DocumentNGramGraph> db = new INSECTFileDB("", sModelDir);
        INSECTFileDB<HashSet<String>> dbParams = new INSECTFileDB("params",
                sModelDir);
        Set<String> sGrammarCategories = null;

        // Extract/load grammar
        DocumentNGramGraph categoryGraph = null;
        DocumentNGramGraph gGrammar = null;

        // Attempt grammar load
        boolean bLoadedOK = false;
        if (bLoad) {
            System.err.print("Loading grammar...");
            gGrammar = db.loadObject(GRAMMAR_NAME, GRAMMAR_TYPE);
            bLoadedOK = gGrammar != null;
            sGrammarCategories = dbParams.loadObject(GRAMMAR_DOCS_PARAM,
                    PARAM_TYPE);
            bLoadedOK = bLoadedOK && (sGrammarCategories != null);
            if (bLoadedOK) {
                if ((gGrammar.getMinSize() == iMinNGram) && (gGrammar.getMaxSize() == iMaxNGram) && (gGrammar.getWindowSize() == iWindowSize)) {
                    System.err.println("Loaded OK  with a size of " +
                            gGrammar.length());
                } else {
                    // Incompatible grammar parameters
                    bLoadedOK = false;
                    System.err.println("Graph parameters differ from the parameters" +
                            " given. Recreating grammar.");
                }
            } else {
                System.err.println("Load failed. Continuing...");
            }
        }

        if (!bLoadedOK) {
            // Init grammar categories' log.
            sGrammarCategories = new HashSet<String>();
            // For every category
            for (Object elem : dsCorpus.getCategories()) {
                String sCurCategory = (String) elem;
                // Init graph
                categoryGraph = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram,
                        iWindowSize);
                System.err.println("Extracting graph from category " + sCurCategory);
                // Load files
                categoryGraph = getGraphFor(sCurCategory, CATEGORY_MODEL_TYPE, db,
                        dsCorpus.getFilesFromCategory(sCurCategory,
                        DocumentSet.FROM_TRAINING_SET),
                        iMinNGram, iMaxNGram, iWindowSize, bLoad, bSave);

                if (sGrammarCategories.size() > 0) {
                    gGrammar.intersectGraph(categoryGraph);
                } else {
                    gGrammar = categoryGraph;
                }

                // Add category to used categories
                sGrammarCategories.add(sCurCategory);

                if (sGrammarCategories.size() == iCategoriesForGrammar) {
                    break;
                }
            }
            System.err.println("Grammar created with a size of " + gGrammar.length() + ".");

            if (bSave) {
                System.err.print("Saving grammar...");
                db.saveObject(gGrammar, GRAMMAR_NAME, GRAMMAR_TYPE);
                dbParams.saveObject((HashSet<String>) sGrammarCategories, GRAMMAR_DOCS_PARAM,
                        PARAM_TYPE);
                System.err.println("Done.");
            }
        }



        /* TODO: Remove
        NGramCachedGraphComparator dgcComparator = new NGramCachedGraphComparator();

        HashSet<String> sCheckedCategories = new HashSet<String>();
        // For every category
        for (Object oCategory : dsCorpus.getCategories()) {
            String sCurCategory = (String) oCategory;
            System.err.println("\nProcessing documents from category " + sCurCategory + "...");

            // Select current document set from category
            List lCurrentDocs = dsCorpus.getFilesFromCategory(sCurCategory,
                    DocumentSet.FROM_TRAINING_SET);

            DocumentNGramGraph gCurGraph = getGraphFor(sCurCategory,
                    CATEGORY_MODEL_TYPE, db, lCurrentDocs, iMinNGram, iMaxNGram,
                    iWindowSize, bLoad, bSave);

            System.out.println("NORMAL_SIM\t" +
                    sCurCategory + "\t" +
                    "GRAMMAR\t" +
                    formatSimilarity(dgcComparator.getSimilarityBetween(
                    gGrammar, gCurGraph)) +
                    "\t" + sGrammarCategories.contains(sCurCategory));
            DocumentNGramGraph gGraphs1 =
                    gCurGraph.allNotIn(gGrammar);

            for (String sOtherCategory : sCheckedCategories) {
                // Split graphs
                DocumentNGramGraph gGraphs2 =
                        getGraphFor(sOtherCategory, CATEGORY_MODEL_TYPE, db,
                        dsCorpus.getFilesFromCategory(sOtherCategory,
                        DocumentSet.FROM_TEST_SET),
                        iMinNGram, iMaxNGram, iWindowSize, bLoad,
                        bSave).allNotIn(gGrammar);

                // Compare peer document to current document by content
                System.out.println("NONGRAMMAR_SIM\t" +
                        sCurCategory + "\t" +
                        sOtherCategory + "\t" +
                        formatSimilarity(dgcComparator.getSimilarityBetween(gGraphs1,
                        gGraphs2)) +
                        "\t" + sCurCategory.equals(sOtherCategory));
                // Compare selected document to current document by grammar
                System.err.print(".");
            }
            // Update checked categories
            sCheckedCategories.add(sCurCategory);
            System.err.print(".");
        }
*/
        // Now check for classification purposes
        // TODO: Improve
        
        Distribution<String> dPerCategoryOverallCount = new Distribution<String>();
        Distribution<String> dPerCategoryCorrectCount = new Distribution<String>();

            // For each testing document
        for (CategorizedFileEntry cfeCur : (List<CategorizedFileEntry>) 
                dsCorpus.getTestSet()) {
            // For each category
            //for (String sCurCategory : (List<String>) dsCorpus.getCategories()) {
            //    // Extract graph
                DocumentNGramGraph dgCurDoc = new DocumentNGramSymWinGraph(iMinNGram,
                        iMaxNGram, iWindowSize);
                // DEBUG LINES
                System.err.println("Loading file " + cfeCur.getFileName());

                try {
                    dgCurDoc.loadDataStringFromFile(cfeCur.getFileName());
                } catch (IOException ex) {
                    Logger.getLogger(
                            grammarAndContentAnalysis.class.getName()).log(
                            Level.SEVERE, "Cannot load file.", ex);
                    continue;
                }
                // Remove grammar
                if (gGrammar != null) {
                    System.err.println("Removing grammar...");
                    dgCurDoc = dgCurDoc.allNotIn(gGrammar);
                }

                // Compare to all classes
                String sRes = determineCategory(dgCurDoc, cfeCur.getFileName(),
                        cfeCur.getCategory(),
                        dsCorpus.getCategories(), db, dsCorpus,
                        iMinNGram, iMaxNGram, iWindowSize, bLoad, bSave,
                        gGrammar);

                if (sRes.equals(cfeCur.getCategory())) {
                    dPerCategoryCorrectCount.increaseValue(cfeCur.getCategory(),
                            1.0);
                }
                dPerCategoryOverallCount.increaseValue(cfeCur.getCategory(), 1.0);
            //}
        }
        outputResults("Results ", dPerCategoryCorrectCount,
                dPerCategoryOverallCount);

        /*
        // Check for grammar convergence
        for (int iFoldCnt=0; iFoldCnt < 10; iFoldCnt++) {
            DocumentNGramGraph gCurGrammar = null;
            
            System.out.println("Step#\t GrammarGraphLength");
            int iStepCnt =0;
            List<String> lShuffledCategories = (List<String>) dsCorpus.getCategories();
            utils.shuffleList(lShuffledCategories);
            
            System.out.println("Fold #" + iFoldCnt);
            System.out.println("Categories' order: " + utils.printIterable(lShuffledCategories, ","));

            for (String sCurCategory : lShuffledCategories) {
                DocumentNGramGraph gCurGraph =
                        getGraphFor(sCurCategory, CATEGORY_MODEL_TYPE, db,
                        dsCorpus.getFilesFromCategory(sCurCategory,
                        DocumentSet.FROM_TEST_SET),
                        iMinNGram, iMaxNGram, iWindowSize, bLoad,
                        bSave);
                if (gCurGrammar == null)
                    gCurGrammar = gCurGraph;
                else
                    gCurGrammar = gCurGrammar.intersectGraph(gCurGraph);
                System.out.println(String.format("%d\t%d", ++iStepCnt,
                    gCurGrammar.length()));


            }
            System.out.println("Final grammar graph length: " +
                    String.valueOf(gCurGrammar.length()));

        }
         */
    }

    protected static void outputResults(String sTitle,
            Distribution<String> dCorrect, Distribution<String> dOverall) {

        // Output results
        System.out.println(sTitle);
        System.out.println("Category ClassCorrect    ClassTotal ClassRecall");
        for (String sCurCategory : dOverall.asTreeMap().keySet()) {
            System.out.println(String.format("%s\t%6.4f\t%6.4f\t%6.4f", sCurCategory,
                    dCorrect.getValue(sCurCategory),
                    dOverall.getValue(sCurCategory),
                    dCorrect.getValue(sCurCategory) /
                    dOverall.getValue(sCurCategory)));
        }
    }

    protected static String determineCategory(DocumentNGramGraph dg,
            String sFilename, String sCorrectCategory,
            List<String> sCategories, INSECTDB<DocumentNGramGraph> db,
            DocumentSet dsCorpus,
            int iMinNGram, int iMaxNGram, int iWindowSize,
            boolean bLoad, boolean bSave, DocumentNGramGraph gGrammar) {
        Distribution<String> dResults = new Distribution<String>();
        // For each category
        for (String sCurCategory : sCategories) {
            // Get category graph
            DocumentNGramGraph gCatGraph = null;
            if (hCategoryGraphs != null)
                if (hCategoryGraphs.containsKey(sCurCategory))
                    gCatGraph = hCategoryGraphs.get(sCurCategory);
            // If not found in cache, then
            if (gCatGraph == null) {
                // Get it
                gCatGraph = getGraphFor(sCurCategory, CATEGORY_MODEL_TYPE, db,
                    dsCorpus.getFilesFromCategory(sCurCategory,
                    DocumentSet.FROM_TRAINING_SET),
                    iMinNGram, iMaxNGram, iWindowSize, bLoad,
                    bSave);
                // NO NEED TO REMOVE GRAMMAR if removed from the document
//                // Remove grammar if given
//                if (gGrammar != null)
//                    gCatGraph = gCatGraph.allNotIn(gGrammar);
                
                // And save it, if cache is being used
                if (hCategoryGraphs != null)
                    hCategoryGraphs.put(sCurCategory, gCatGraph);
            }
            
            NGramCachedGraphComparator dgcComparator = new
                    NGramCachedGraphComparator();
            GraphSimilarity gs = dgcComparator.getSimilarityBetween(dg,
                    gCatGraph);
            dResults.setValue(sCurCategory,
                    gs.getOverallSimilarity());

        }

        // DEBUG LINES
        System.err.println(sFilename + " (" + sCorrectCategory + "):" +
            dResults.toString());
        //////////////

        return dResults.getKeyOfMaxValue();
    }
}