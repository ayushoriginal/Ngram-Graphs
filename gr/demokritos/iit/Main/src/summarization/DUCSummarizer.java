/*
 * Under LGPL
 */

package gr.demokritos.iit.summarization;

import gr.demokritos.iit.conceptualIndex.LocalWordNetMeaningExtractor;
import gr.demokritos.iit.conceptualIndex.documentModel.SemanticIndex;
import gr.demokritos.iit.conceptualIndex.documentModel.SymbolicGraph;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.console.grammaticalityEstimator;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.events.IObjectFilter;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.summarization.analysis.ConceptExtractor;
import gr.demokritos.iit.summarization.analysis.EntropyChunker;
import gr.demokritos.iit.summarization.selection.NoveltyBasedSelector;
import gr.demokritos.iit.summarization.selection.RedundancyBasedSelector;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import edu.nus.comp.nlp.tool.PlainText;
import gr.demokritos.iit.tacTools.DUC2006TopicFileSet;

/**
 *
 * @author pckid
 */
public class DUCSummarizer {

    public static final String CONTENT_FILE_NAME = "Content.dat";

    public static void printSyntax() {
        System.out.println("Syntax: "  + DUCSummarizer.class.getName() +
                " -topicID=AAAA [-queryExpansion]" +
                " [-grammarDir=grammar/] [-inputDir=docs/] [-outputSize=#] " +
                "[-trainDataDir=dir/] " +
//                "[-useInputDirData] " +
                "[-maxSentencesSelected=#] [-chunkScoring] [-penalizeGrammarChunks] " +
                "[-eliminateNonContent] [-useAverageChunkScore] [-noGrammar] " +
                "[-topicXMLFile=topics.xml] [-chunkExpansion] [-allowRedundancy]" +
                " [-minNGram=3] [-maxNGram=3] [-dist=4]\n" +
                "-topicID=AAAA \tRequired parameters indicating the ID of the " +
                "topic to summarize.\n" +
//                "-readTopicID=AAAA \tIf supplied, indicates which topic ID is " +
//                "supposed to have been read by the user (implying which info " +
//                "will be considered redundant.)" +
                "-queryExpansion\tIf selected, wordnet overview of senses is used" +
                " to expand given query." +
                "-grammarDir=grammar/  \tThe directory containing a corpus of texts from the same language, categorized in" +
                " subdirectories by subject. (Including the dir " +
                "slash character).\n" +
                "-inputDir=docs/ \tThe directory of input documents (including " +
                "the slash character). It should follow the TAC2008 structure.\n" +
                "-outputSize=100 \tThe maximum number of words to use in the " +
                "summary.\n" +
                "-trainDataDir=dir/ \tA directory containing previous run " +
                "data used to avoid retraining (grammar, chunker, etc.). " +
                " If no such data exist, then" +
                " normal training is performed and the new data are saved in " +
                "the directory. If the option is not selected no previous " +
                " data are used and extracted data are not saved for future use.\n" +
//                "-useInputDirData \tIf indicated, then data from the analysis " +
//                "of the input directory are stored, or reused if they" +
//                " already exist. The data files are saved in the input directory.\n" +
                "-sentenceSplitting=(high|low|smart) \tThe granularity of sentence" +
                " splitting. High or low granularity based on very simple " +
                "patterns, or smart for intelligent sentence splitting.\n" +
                "-maxSentencesSelected=# \tThe maximum number of sentences " +
                "to use. Default value is [outputSize/5].\n" +
                "-chunkScoring \tIf indicated, then sentences are scored " +
                "depending on their chunks and not as a whole.\n" +
                "-chunkExpansion \t(Only works with chunkScoring, queryExpansion " +
                "enabled) If indicated, then sentences are first expanded into " +
                "concept descriptions and then used.\n" +
                "-penalizeGrammarChunks \tIf selected, then sentences that " +
                "contain grammar chunks are penalized.\n" +
                "-eliminateNonContent \tIf selected, all non-content, " +
                "grammatical words are eliminated in the output summary." +
                "-useAverageChunkScore \tIf selected, then the average of " +
                "the chunks' content score of a sentence is used as the" +
                "sentence score. Otherwise, the sum of the chunks' content " +
                "score is considered the sentence score.\n" +
                "-noGrammar \tIf selected, no grammar is used in the process." +
                "-topicXMLFile=topics.xml \tA TAC2007 topic set file, defining topics.\n" +
                "-allowRedundancy \tIgnores redundancy when selecting summary sentences.\n" +
                "-useNovelty \tUses novelty estimation instead of redundancy over " +
                    "all content.\n" +
                "-minNGram=3 -maxNGram=3 -dist=4 \tThe n-gram analysis values.\n");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Parse command line
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);
        String sInputDir = null;
        String sGrammarDir = null;
        int iOutputSize = -1;
        String sTrainDataDir = null;
        boolean bUseInputDirData = false;
        String sQuery = null;
        String sSentenceSplitting = "smart";
        int iMaxSentencesSelected = -1;
        boolean bChunkScoring = false;
        boolean bPenalizeGrammarChunks = false;
        boolean bEliminateNonContent = false;
        boolean bUseAverageChunkScore = false;
        boolean bNoGrammar = false;
        String sTopicID = null;
        // String sReadTopicID = null;
        String sTopicXMLFile = null;
        boolean bQueryExpansion = false;
        boolean bChunkExpansion = false;
        boolean bAllowRedundancy = false;
        boolean bUseNovelty = false;
        int iMinNGram = 3;
        int iMaxNGram =3;
        int iDist = 4;

        try {
            if (Boolean.valueOf(utils.getSwitch(hSwitches, "?",
                    Boolean.FALSE.toString())) ||
                    Boolean.valueOf(utils.getSwitch(hSwitches, "help",
                    Boolean.FALSE.toString()))) {
                printSyntax();
                return;
            }

            sInputDir = utils.getSwitch(hSwitches, "inputDir", "docs/");
            sGrammarDir = utils.getSwitch(hSwitches, "grammarDir", "grammar/");
            iOutputSize = Integer.valueOf(utils.getSwitch(hSwitches, "outputSize",
                    String.valueOf(100))).intValue();
            iMaxSentencesSelected = Integer.valueOf(utils.getSwitch(hSwitches,
                    "maxSentencesSelected",
                    String.valueOf(iOutputSize / 5))).intValue();
            iMinNGram = Integer.valueOf(utils.getSwitch(hSwitches,
                    "minNGram",
                    String.valueOf(3))).intValue();
            iMaxNGram = Integer.valueOf(utils.getSwitch(hSwitches,
                    "minNGram",
                    String.valueOf(3))).intValue();
            iDist = Integer.valueOf(utils.getSwitch(hSwitches,
                    "dist",
                    String.valueOf(3))).intValue();
            sTrainDataDir = utils.getSwitch(hSwitches, "trainDataDir", "");
//            bUseInputDirData = Boolean.valueOf(utils.getSwitch(hSwitches,
//                    "useInputDirData", Boolean.FALSE.toString()));
            //sQuery = utils.getSwitch(hSwitches, "query", "");
            bChunkScoring = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "chunkScoring", Boolean.FALSE.toString()));
            bChunkExpansion = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "chunkExpansion", Boolean.FALSE.toString()));
            bQueryExpansion = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "queryExpansion", Boolean.FALSE.toString()));
            bPenalizeGrammarChunks = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "penalizeGrammarChunks", Boolean.FALSE.toString()));
            bEliminateNonContent = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "eliminateNonContent", Boolean.FALSE.toString()));
            bUseAverageChunkScore = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "useAverageChunkScore", Boolean.FALSE.toString()));
            bNoGrammar = Boolean.valueOf(utils.getSwitch(hSwitches, "noGrammar",
                    Boolean.FALSE.toString()));
            bAllowRedundancy = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "allowRedundancy", Boolean.FALSE.toString()));
            bUseNovelty = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "useNovelty", Boolean.FALSE.toString()));
            sTopicID = utils.getSwitch(hSwitches, "topicID",
                    "");
            if (sTopicID.length() == 0)
                throw new Exception("no topic ID was supplied.");
            //sReadTopicID = utils.getSwitch(hSwitches, "readTopicID",
            //        "");
            sTopicXMLFile = utils.getSwitch(hSwitches, "topicXMLFile",
                    "");
            sSentenceSplitting = utils.getSwitch(hSwitches, "sentenceSplitting",
                    "smart");

        }
        catch (Exception e) {
            System.err.println("Cannot parse command line (" + e.getMessage()
                    + "). Aborting.");
            printSyntax();
            return;
        }

        // FIXED PARAMETERS
        // final int iTopChunksKeptNum = 1000;
        // final int FoldCount = 10;

        // Create input document set
        System.err.println("Creating document sets...");
        DUC2006TopicFileSet dsFiles;
        try {
            dsFiles = new DUC2006TopicFileSet(sTopicXMLFile, sInputDir);
        } catch (Exception ex) {
            Logger.getLogger(TACSummarizer.class.getName()).log(Level.SEVERE,
                    "Could not create document sets", ex);
            return;
        }

        dsFiles.createSets();
        Set<String> sFiles;
//        if (sReadTopicID.length() > 0) {
//            // DEBUG LINES
//            System.err.println("Using previously read group A of topic " +
//                    sReadTopicID);
//            //////////////
//            sFiles = new HashSet(dsFiles.getFilenamesFromCategory(sReadTopicID,
//                TAC2008TopicFileSet.FROM_TRAINING_SET));
//        }
//        else
        // All files are included in the training set
        sFiles = new HashSet(dsFiles.getFilenamesFromCategory(sTopicID,
            DUC2006TopicFileSet.FROM_TRAINING_SET));

        // Init vars to null
        grammaticalityEstimator geEstimator = null;
        DocumentNGramGraph Grammar = null;

        boolean bLoadedOK = false;
        // Constants
        final String GRAMMAR_MODEL_NAME = "GrammarModel";
        final String NORMALITY_MODEL_NAME = "NormalityModel";
        final String CONTENT_MODEL_NAME = "ContentModel";
        final String DATA_MODELS_CATEGORY = "SumData";

        // Attempt loading previous data if requested
        if (sTrainDataDir.length() > 0) {
            try {
                INSECTDB dbTrainingData = new INSECTFileDB("", sTrainDataDir);
                if (!bNoGrammar) {
                    if (!dbTrainingData.existsObject(GRAMMAR_MODEL_NAME, DATA_MODELS_CATEGORY))
                        throw new FileNotFoundException("Could not find " + GRAMMAR_MODEL_NAME + "...");
                    System.err.print("Loading grammar model...");
                    //Grammar = (DocumentNGramGraph)dbTrainingData.loadObject(GRAMMAR_MODEL_NAME,
                    //        DATA_MODELS_CATEGORY);
                    Grammar = (DocumentNGramGraph)dbTrainingData.loadObject(GRAMMAR_MODEL_NAME,
                            DATA_MODELS_CATEGORY);
                    if (Grammar == null)
                        throw new IOException("Could not load " + GRAMMAR_MODEL_NAME + "...");
                }
                else
                    System.err.print("Grammar use skipped...");

                System.err.println("Done.");
                bLoadedOK = true;
            } catch (Exception e) {
                bLoadedOK = false;
            }

            if (!bLoadedOK)
                System.err.println("Loading failed. Continuing normally...");
        }

        // If loading failed, proceed by normal initialization
        if (!bLoadedOK)
        {
            // Init grammar document set
            DocumentSet dsGrammarFiles = new DocumentSet(sGrammarDir, 1.0);
            // Filter out Content file
            dsGrammarFiles.FileEvaluator = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !pathname.getName().endsWith(".dat");
                }
            };

            if (!bNoGrammar)
            {
                // Grammar induction using data from DIFFERENT TOPICS
                dsGrammarFiles.createSets();

                // Extract grammar graph from grammar dir
                System.err.println("Extracting grammar...");

                // For each category
                for (String sCategory : (List<String>)dsGrammarFiles.getCategories())
                {
                    System.err.print("Processing category " + sCategory + "...");
                    List<CategorizedFileEntry> cfeCategoryFiles =
                            (List<CategorizedFileEntry>)dsGrammarFiles.getFilesFromCategory(sCategory);
                    HashSet<String> sTopicFiles = new HashSet();
                    for (CategorizedFileEntry cCurFile : cfeCategoryFiles) {
                        sTopicFiles.add(cCurFile.getFileName());

                    }

                    String sGrammarText = utils.loadFileSetToString(sTopicFiles);
                    DocumentNGramGraph ngdTmp1 = new DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram,iDist);
                    //DocumentWordGraph ngdTmp1 = new DocumentWordGraph(1,2,3);

                    ngdTmp1.setDataString(sGrammarText);
                    System.err.print("Done. Updating Grammar...");
                    if (Grammar == null)
                        Grammar = ngdTmp1;
                    else
                        Grammar = (DocumentNGramGraph)Grammar.intersectGraph(ngdTmp1);

                    System.err.println("Done. Current grammar size " +
                            Grammar.length() + " vertices and edges.");

                }

                System.err.println("Extracting grammar...Done.");

                // Save to file
                if (sTrainDataDir.length() > 0) {
                    INSECTDB dbTrainingData = new INSECTFileDB("", sTrainDataDir);

                    // Saving normality estimator
                    System.err.print("Saving normality estimator...");
                    dbTrainingData.saveObject(geEstimator, NORMALITY_MODEL_NAME,
                            DATA_MODELS_CATEGORY);
                    System.err.println("Done.");
                    System.err.print("Saving grammar model...");
                    dbTrainingData.saveObject(Grammar, GRAMMAR_MODEL_NAME,
                            DATA_MODELS_CATEGORY);
                    System.err.println("Done.");
                }
            }
        }
        if (!bNoGrammar)
            System.err.println("Final grammar size " + Grammar.length() + " edges.");

        // For each topic

        // Extract content
        DocumentNGramGraph Content = null; // Init content graph

        if (bUseInputDirData) {
            try {
                INSECTDB dbTrainingData = new INSECTFileDB("", sTrainDataDir);
                String sContentFileName = CONTENT_MODEL_NAME + sInputDir;
                if (!dbTrainingData.existsObject(sContentFileName, DATA_MODELS_CATEGORY))
                    throw new FileNotFoundException("Could not find " + sContentFileName + "...");
                System.err.print("Loading content graph...");
                Content = (DocumentNGramGraph)dbTrainingData.loadObject(sContentFileName,
                        DATA_MODELS_CATEGORY);
                if (Content == null)
                    throw new IOException("Could not load " + sContentFileName + "...");
                System.err.println("Done.");

                bLoadedOK = true;
            } catch (FileNotFoundException ex) {
                // Ignore
                // ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            if (Content==null)
                System.err.println("Loading failed. Continuing normally...");
        }

        // If content was not loaded
        if (Content==null)
        { // GC Helper block
            System.err.println("Extracting topic content...");

            int iCurFileCnt = 0;
            for (String sCurFile : dsFiles.getFilenamesFromCategory(sTopicID,
                    DUC2006TopicFileSet.FROM_TRAINING_SET)) {
                System.err.print("Extracting content from " + sCurFile + "...");
                DocumentNGramGraph curContent = new DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram,iDist); // Init content graph
                //DocumentNGramGraph  curContent = new DocumentWordGraph(1,2,3); // Init content graph

                // Use document set loadFile method
                curContent.setDataString(dsFiles.loadFile(sCurFile));

                if (!bNoGrammar)
                    curContent = curContent.allNotIn(Grammar);

                if (Content == null) {
                    // Init content graph
                    Content = curContent;
                    System.err.print("Initial content graph size: " +
                            Content.length() + " vertices and edges.");
                }
                else {
                    // Content.mergeGraph(curContent, 0.5 - (0.5 / iCurFileCnt)); // For words
                    Content = Content.intersectGraph(curContent); // For chars
                }

                System.err.println("Done.");
                iCurFileCnt++;
            }

            // Perform final update, based on size of graphs (for a ratio of more than 50% merge, else intersect)
            System.err.println("\nFinal content graph size: " +
                    Content.length() + " vertices and edges.");
            // Save content graph if needed
            if (bUseInputDirData) {
                String sContentFileName = CONTENT_MODEL_NAME + sInputDir;
                INSECTDB dbTrainingData = new INSECTFileDB("", sTrainDataDir);

                System.err.print("Saving content graph...");
                dbTrainingData.saveObject(Content, sContentFileName, DATA_MODELS_CATEGORY);
                System.err.println("Done.");
            }
        }

        // Create query string representation, to use in sentence selection.
        NGramCachedGraphComparator ncgcComparator = new
                NGramCachedGraphComparator();

        sQuery = dsFiles.getTopicDefinition(sTopicID);

        // Init global concept extractor structs
        ConceptExtractor ce = null;
        SymbolicGraph sg = null;
        SemanticIndex si = null;
        List<String> slCandidateExpansion = null;
        if (bQueryExpansion) {
            StringBuffer sbExpandedQuery = new StringBuffer();
            // Query maximization
            System.err.println("Expanding query...");
            // Create symbolic graph
            System.err.print("Loading symbolic graph...");
            sg = new SymbolicGraph(1, 10);
            for (String sFile : sFiles) {
                sg.setDataString(dsFiles.loadFile(sFile));
            }
            System.err.println("Done.");
            // Init semantic index
            si = new SemanticIndex(sg);
            try {
                si.MeaningExtractor = new LocalWordNetMeaningExtractor();
            } catch (IOException ex) {
                Logger.getLogger(TACSummarizer.class.getName()).log(Level.SEVERE,
                        "Could not find local instance of wordnet.", ex);

            }
            // Init concept extractor to use
            ce = new ConceptExtractor(si);
            ce.setNotificationListener(new NotificationListener() {
                @Override
                public void Notify(Object oSender, Object oParams) {
                    System.err.println("Concept extractor working..." +
                            oParams);
                }
            });


            System.err.print("Replacing query...");
            // Break query into chunks
            List<String> sQueryChunks = Arrays.asList(sQuery.split("\\s"));
            for (String sCurQChunk : sQueryChunks) {
                // Extract concept descriptions
                slCandidateExpansion =
                        ce.extractConceptDescriptions(sCurQChunk);
                Iterator<String> iIter = slCandidateExpansion.iterator();
                while (iIter.hasNext()) {
                    String sCandidate = iIter.next();
                    DocumentNGramGraph gTmp = new
                            DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
                    //DocumentNGramGraph gTmp = new
                    //        DocumentWordGraph(1,2,3);
                    gTmp.setDataString(sCandidate);
                    NGramCachedGraphComparator ngcCand =
                            new NGramCachedGraphComparator();
                    double dContentRelatedness = getOverallSimilarity(
                            ngcCand.getSimilarityBetween(gTmp, Content));
                    if (dContentRelatedness < 0.20) {
                       slCandidateExpansion.remove(sCandidate);
                       iIter = slCandidateExpansion.iterator(); // Reset iter
                    }
                }

                sbExpandedQuery.append(utils.printIterable(
                        slCandidateExpansion , " ").replaceAll(
                        "(\\(.+\\))|([-]+)", ""));
            }
            // Update expanded query
            System.err.println("Query expansion:" + sbExpandedQuery.toString());
            slCandidateExpansion = Arrays.asList(
                    sbExpandedQuery.toString().split(",|\\n"));
            System.err.print("Done.");
        }
        // DEBUG LINES
        System.err.println("Title - Narrative Query: \n" + sQuery);
        //////////////

        NGramCachedGraphComparator dgcComparator = new NGramCachedGraphComparator();

        // Extract query graph
        DocumentNGramGraph gFinalQuery = null;

        if (sQuery.length() > 0) {

            DocumentNGramGraph gQuery = null;
            gQuery = new DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
            // gQuery = new DocumentWordGraph(1, 2, 3);

            gQuery.setDataString(sQuery);
            // Remove grammar, if required
            if (!bNoGrammar)
                gFinalQuery = gQuery.allNotIn(Grammar);
            else
                gFinalQuery = gQuery;
        }

        // Update content with query
        Content.merge(gFinalQuery, 0.5);
        // Update content with query expansion
        if (bQueryExpansion) {
            int iCnt = 2;
            // Set to avoid duplicate insertions of same words from expansion
            HashSet<String> sAdded = new HashSet<String>();
            for (String sQueryExp : slCandidateExpansion) {
                if ((sQueryExp.length() == 0) ||
                        (sAdded.contains(sQueryExp))) continue;
                sAdded.add(sQueryExp);
                DocumentNGramGraph gQuery = null;
                gQuery = new DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
                // gQuery = new DocumentWordGraph(1, 2, 3);

                gQuery.setDataString(sQueryExp);
                Content.merge(gFinalQuery, 1.0 / ++iCnt);
            }
        }

        // Order input sentences based on content graph matching of their tokens.
        TreeMap<Double,String> tmSentences = new TreeMap<Double,String>();
        System.err.println("Ordering sentences by content similarity...");
        EntropyChunker ec = null;
        // Use local block to free chunker
        {
            // Train chunker if needed
            if (bUseInputDirData) {
                try {
                    FileInputStream fisIn = new FileInputStream(sInputDir + "Chunker.dat");
                    GZIPInputStream gsIn = new GZIPInputStream(fisIn);
                    ObjectInputStream oisIn = new ObjectInputStream(gsIn);

                    System.err.print("Loading chunker...");
                    ec = (EntropyChunker)oisIn.readObject();
                    System.err.println("Done.");
                    oisIn.close();
                    gsIn.close();
                    fisIn.close();
                    bLoadedOK = true;
                } catch (FileNotFoundException ex) {
                    // Ignore
                    // ex.printStackTrace(System.err);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
                if (Content==null)
                    System.err.println("Loading failed. Continuing normally...");
            }

            // Chunker
            if (ec == null) {
                System.err.print("Training chunker...");
                ec = new EntropyChunker();
                ec.train(sFiles);
                System.err.println("Done.");

                if (bUseInputDirData)
                try {
                    FileOutputStream fosOut = new FileOutputStream(sInputDir + "Chunker.dat");
                    GZIPOutputStream gsOut = new GZIPOutputStream(fosOut);
                    ObjectOutputStream oosOut = new ObjectOutputStream(gsOut);

                    System.err.print("Saving chunker data...");
                    oosOut.writeObject(ec);
                    oosOut.close();
                    gsOut.close();
                    fosOut.close();
                    System.err.println("Done.");
                } catch (FileNotFoundException ex) {
                    // Ignore
                    // ex.printStackTrace(System.err);
                } catch (IOException ex) {
                    System.err.println("Saving failed. Cause:");
                    ex.printStackTrace(System.err);
                }

            }

            Iterator<String> iFile = sFiles.iterator();
            while (iFile.hasNext()) {
                String sCurFile = iFile.next();
                System.err.println("Extracting and assigning importance to sentences from " + sCurFile + "...");
                String sText = dsFiles.loadFile(sCurFile).replaceAll("\n", " ");
                // Extract TEXT ONLY
                // sText = sText.substring(sText.indexOf(TACDocumentSet.TEXT_TAG) + TACDocumentSet.TEXT_TAG.length() + 1);
                // sText = sText.substring(0, sText.indexOf(TACDocumentSet.TEXT_TAG));

                // For each sentence
                double dSentenceScore = 0.0;
                double dSentenceGrammarScore = 0.0;
                // TODO: Implement good chunking
                // HIGH Granularity chunking with: "([.,;-]+:*[ ]+)|(<P>)|(</P>)"
                // LOW Granularity chunking with "[.]|(<P>)|(</P>)"
                String[] sCandSentences;
                if (sSentenceSplitting.equalsIgnoreCase("high"))
                    sCandSentences = sText.split("[.?!](\\p{Space}+)|(\\\")");
                else
                    if (sSentenceSplitting.equalsIgnoreCase("low"))
                        sCandSentences = sText.split("[.]|(<P>)|(</P>)");
                    else
                        // DEFAULT IS SMART
                        sCandSentences = PlainText.splitSentences(sText,
                                "(<P>)|(</P>)");

                for (String sSentence : Arrays.asList(sCandSentences)) {
                    dSentenceScore = 1.0;
                    if (bChunkScoring) {
                        List<String> lSentenceChunks = ec.chunkString(sSentence);
                        Distribution dSentenceChunkScores = new Distribution();
                        for (String sCurChunk : lSentenceChunks) {
                            // Ignore empty strings or strings with too low a size
                            if (sCurChunk.trim().length() == 0)
                                continue;
                            if (!bNoGrammar && (sCurChunk.length() < Grammar.getMinSize()))
                                continue;

                            DocumentNGramGraph dChunk = new
                                    DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
                            // Use chunk expansion, if requested
                            if (bChunkExpansion)
                            {
                                System.err.print("Updating chunk...");
                                // Break query into chunks
                                StringBuffer sbExpandedChunk = new StringBuffer();
                                // TODO: Implement correct way of doing this
                                // using selected senses and merging
                                sbExpandedChunk.append(utils.printIterable(
                                        ce.extractConceptDescriptions(sCurChunk),
                                        " "));

                                dChunk.setDataString(sCurChunk + " " +
                                        sbExpandedChunk.toString());
                            }
                            else
                                dChunk.setDataString(sCurChunk);

                            // Penalize grammar, if selected
                            if (!bNoGrammar)
                                if (bPenalizeGrammarChunks) {
                                    double dGrammarChunkScore =
                                            getOverallSimilarity(dgcComparator.getSimilarityBetween(dChunk,
                                            Grammar));
                                    dSentenceGrammarScore += dGrammarChunkScore;
                                }
                            DocumentNGramGraph dChunkContent;
                            if (!bNoGrammar)
                                dChunkContent = dChunk.allNotIn(Grammar);
                            else
                                dChunkContent = dChunk;

                            GraphSimilarity gTmp = dgcComparator.getSimilarityBetween(Content,
                                    dChunkContent);
                            double dChunkScore = getOverallSimilarity(gTmp);
                            dSentenceChunkScores.increaseValue(dChunkScore, 1.0);
                            dSentenceScore += dChunkScore;

                            //DEBUG LINES
                            // System.err.println("--" + sCurChunk + " \t-> \t" + String.format("%10.8f",Math.log10(dChunkScore + 10.0e-20)));
                            /////////////
                        }
                        // Use average instead of sentence sum score if requested.
                        if (bUseAverageChunkScore)
                            dSentenceScore = dSentenceChunkScores.average(false);
                    }
                    else {
                        DocumentNGramGraph gSentence = new
                                DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
                        //DocumentNGramGraph gSentence = new
                        //        DocumentWordGraph(1,2,3);

                        gSentence.setDataString(sSentence);
                        DocumentNGramGraph gSentenceContent = null;
                        if (!bNoGrammar)
                            gSentenceContent = gSentence.allNotIn(Grammar);
                        else
                            gSentenceContent = gSentence;

                        GraphSimilarity gTmp = dgcComparator.getSimilarityBetween(Content, gSentenceContent);
                        dSentenceScore = getOverallSimilarity(gTmp);
                        if (!bNoGrammar) {
                            gTmp = dgcComparator.getSimilarityBetween(Grammar, gSentence);
                            dSentenceGrammarScore = getOverallSimilarity(gTmp);
                        }
                    }
                    // Deprecated: Get SENTENCE grammar similarity
                    // DocumentNGramSymWinDistroGraph dSentenceGrammar = new DocumentNGramSymWinDistroGraph(3,3,4);
                    // dSentenceGrammar.setDataString(sSentence);
                    // dSentenceGrammarScore = dgcComparator.getSimilarityBetween(Grammar, dSentenceGrammar).ValueSimilarity;
                    //////////////////////////////////////////////

                    // Use query similarity to complete sentence
                    DocumentNGramGraph gSentence = new DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
                    // DocumentNGramGraph gSentence = new DocumentWordGraph(1,2,3);

                    gSentence.setDataString(sSentence);
                    // If query exists
                    if (gFinalQuery != null) {
                        // Calc similarity
                        GraphSimilarity gTmp = ncgcComparator.getSimilarityBetween(gFinalQuery,
                                gSentence);
                        // Multiply sentence score by similarity to query (plus a small number)
                        dSentenceScore *= 1.0 + getOverallSimilarity(gTmp);
                    }
                    // Add sentence to ordering, using importance
                    tmSentences.put(dSentenceScore - dSentenceGrammarScore, sSentence);
                    // DEBUG LINES
                    // System.out.println(String.format("LogImportance %f (Avg: %f) : %s", dSentenceScore, dSentenceChunkScores.average(false), sSentence));
                    //////////////
                }

                System.err.println("Extracting and assigning importance to sentences from " + sCurFile + "...Done.");
            }
        }
        System.err.println("Ordering sentences by content similarity...Done.");

        // Display ordered sentences
        // Add important sentences to ordered list
        System.err.println("Ordered sentences (descending)");
        ArrayList<String> alImportantSentences = new ArrayList<String>();
        System.err.println("Value\tSentence");
        for (Object elem : tmSentences.descendingKeySet()) {
            String sCurSentence = tmSentences.get(elem).replaceAll("\n", " ");
            System.err.println(String.format("%f \t %s", elem, sCurSentence));
            alImportantSentences.add(sCurSentence);
        }


        // If readTopicID has been indicated, use it.
        StringBuffer sbReadTexts = new StringBuffer();
//        if (sReadTopicID.length() > 0) {
//            // For every training text (Groupd A) in topic
//            List<String> lsReadFiles = dsFiles.getFilenamesFromCategory(
//                    sReadTopicID, TAC2008TopicFileSet.FROM_TRAINING_SET);
//            for (String sFile : lsReadFiles) {
//                // Extract text and add it to pre-existing text
//                sbReadTexts.append(dsFiles.loadFile(sFile));
//            }
//        }

        // Select sentences based on novelty

        NoveltyBasedSelector<String,String> nbsNovelty;
        if (bUseNovelty)
            nbsNovelty = new NoveltyBasedSelector<String,String>(
                sbReadTexts.toString(), iMinNGram, iMaxNGram, iDist);
        else
            nbsNovelty = new RedundancyBasedSelector<String, String>(
                sbReadTexts.toString(), iMinNGram, iMaxNGram, iDist,
                0.20);

        // Use max sentences setting
        nbsNovelty.MaxSentencesSelected = iMaxSentencesSelected;
//        // Use custom comparator to take length ratio into account
//        nbsNovelty.Comparator = new SimilarityComparatorListener() {
//            @Override
//            public ISimilarity getSimilarityBetween(Object oFirst, Object oSecond) throws InvalidClassException {
//                NGramCachedGraphComparator nc = new NGramCachedGraphComparator();
//                final GraphSimilarity gTmpArg = nc.getSimilarityBetween(oFirst, oSecond);
//                return new ISimilarity() {
//
//                    @Override
//                    public final double getOverallSimilarity() {
//                        return (1.0 - gTmpArg.SizeSimilarity) * gTmpArg.ValueSimilarity;
//                    }
//
//                    @Override
//                    public final double asDistance() {
//                        return 1.0 / getOverallSimilarity();
//                    }
//                };
//            }
//        };

        // Prepare filter args
        final DocumentNGramGraph argContent = Content;
        final DocumentNGramGraph argGrammar = Grammar;
        final boolean argBNoGrammar = bNoGrammar;

        // Filter removes grammar and pure content words, leaving differences
        nbsNovelty.SentenceRepresentationFilter = new IObjectFilter<DocumentNGramGraph>() {
            @Override
            public DocumentNGramGraph filter(DocumentNGramGraph obj) {
                return (argBNoGrammar) ?  obj.allNotIn(argContent) :
                    obj.allNotIn(argGrammar).allNotIn(argContent); // Compare non-common part
            }
        };

        // Use novelty selector, only if requested

        List<String> lNoveltyBasedSentences = null;
        if (!bAllowRedundancy) {
            lNoveltyBasedSentences =
                    nbsNovelty.selectFromSentences(alImportantSentences);
        }
        else
        {
            lNoveltyBasedSentences = new ArrayList<String>();
            lNoveltyBasedSentences.addAll(alImportantSentences);
        }
        int iCurSummaryWordCount = 0;
        Iterator<String> iSentenceIterator = lNoveltyBasedSentences.iterator();
        ArrayList<String> alSummarySentences = new ArrayList<String>();

        // Omit too short sentences
        double dLenThreshold = 0.0;
        Distribution<Double> dSentenceLength = new Distribution<Double>();
        for (String sSentence : lNoveltyBasedSentences)
            dSentenceLength.increaseValue((double)sentenceLength(sSentence), 1.0);

        // DEBUG LINES
        System.err.println(String.format("Sentence length Mean (SD) : %6.4f " +
                "(%6.4f). Distribution looks %snormal.",
                dSentenceLength.average(false),
                dSentenceLength.standardDeviation(false),
                dSentenceLength.isNormal(false, 0.10) ? "" : "not "));

        // If lengths are normal
        if (dSentenceLength.isNormal(false, 0.10))
            // Get the 95 percent threshold
            dLenThreshold = dSentenceLength.average(false) -
                2 * dSentenceLength.standardDeviation(false);

        // If an abnormal threshold
        if (dLenThreshold <= 0.0)
        {
            dLenThreshold = dSentenceLength.getValueAtPoint(false, 0.05);
        }
        //////////////

        // DEBUG LINES
        System.err.println(String.format("Threshold size set to %3.1f words.",
                dLenThreshold));
        // System.err.println(utils.printIterable(alSummarySentences, "\n"));
        //////////////
        while (iCurSummaryWordCount < iOutputSize) {
            if (!iSentenceIterator.hasNext())
                break; // Reached end of sentence set
            // Get next sentence
            String sCur = iSentenceIterator.next();
            StringBuffer sbFilteredSentence = new StringBuffer();
            // Count tokens
            int iCurSentenceSize = 0;
            List<String> sSentenceChunks = ec.chunkString(sCur);
            for (String sChunk : sSentenceChunks) {
                // If grammatical tokens should be eliminated
                if (bEliminateNonContent) {
                    DocumentNGramGraph dCurChunk = new
                           DocumentNGramSymWinGraph(iMinNGram,
                            iMaxNGram, iDist);
                    //DocumentNGramGraph dCurChunk = new
                    //        DocumentWordGraph(3,3,4);

                    dCurChunk.setDataString(sChunk);
                    if (!bNoGrammar)
                        // Omit grammatical tokens
                        if (getOverallSimilarity(dgcComparator.getSimilarityBetween(dCurChunk, Grammar)) <=
                                getOverallSimilarity(dgcComparator.getSimilarityBetween(dCurChunk, Content)))
                            continue;

                    sbFilteredSentence.append(sChunk);
                }
                else
                    // else, add chunk without applying filter
                    sbFilteredSentence.append(sChunk);

                //if (sChunk.trim().length() > 0)
                //    iCurSentenceSize++;
            }
            // Size is determined as unique whitespace separated tokens in TAC
            iCurSentenceSize = sentenceLength(sbFilteredSentence.toString());
            // Also ignore too short sentences
            if ((iCurSummaryWordCount + iCurSentenceSize > iOutputSize) ||
                    (iCurSentenceSize <= dLenThreshold))
                continue; // Would exceed max size
            // If all is OK, add sentence to output summary
            alSummarySentences.add(sbFilteredSentence.toString());
            iCurSummaryWordCount += iCurSentenceSize ; // Update word count
            System.err.println("Added \n" + sCur + "\n to summary.");
        }

        // Output summary
        String sSummary = utils.printIterable(alSummarySentences, " ");
        System.err.println(String.format("Summary created using %d sentences. " +
                "Length is %d words.", alSummarySentences.size(),
                sentenceLength(sSummary)));
        System.out.println(sSummary);
    }

    private static final int sentenceLength(String sSentence) {
        return sSentence.split("\\s").length;
    }

    private static final double getOverallSimilarity(GraphSimilarity gSim) {
        if (gSim.SizeSimilarity == 0)
            return 0.0;
        else
            return gSim.ValueSimilarity / gSim.SizeSimilarity;
    }

}
