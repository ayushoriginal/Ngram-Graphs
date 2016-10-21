/*
 * summarizationPerformer.java
 *
 * Created on March 31, 2008, 11:28 AM
 *
 */

package gr.demokritos.iit.summarization;

import edu.nus.comp.nlp.tool.PlainText;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.console.grammaticalityEstimator;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.events.IObjectFilter;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.summarization.analysis.EntropyChunker;
import gr.demokritos.iit.summarization.selection.CombinedNoveltyBasedSelector;
import gr.demokritos.iit.tacTools.ACQUAINT2DocumentSet;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author ggianna
 */
public class summarizationPerformer {
    
    public static final String CONTENT_FILE_NAME = "Content.dat";
    
    public static void printSyntax() {
        System.out.println("Syntax: "  + summarizationPerformer.class.getName() + " [-grammarDir=grammar/] [-inputDir=docs/] [-outputSize=#] " +
                "[-trainData=file.dat] [-useInputDirData] [-maxSentencesSelected=#] [-chunkScoring] [-penalizeGrammarChunks] " +
                "[-eliminateNonContent] [-useAverageChunkScore] [-noGrammar]\n" +
                "-grammarDir=grammar/  \tThe directory containing a corpus of texts from the same language, categorized in" +
                " subdirectories by subject. (Including the dir " +
                "slash character).\n" +
                "-inputDir=docs/ \tThe directory of input documents (including the slash character).\n" +
                "-outputSize=100 \tThe maximum number of words to use in the summary.\n" +
                "-trainDataDir=dir/ \tA directory containing previous run data used to avoid retraining (grammar, chunker, etc.). " +
                " If no such data exist, then" +
                " normal training is performed and the new data are saved in the directory. If the option is not selected no previous " +
                " data are used and extracted data are not saved for future use.\n" +
                "-useInputDirData \tIf indicated, then data from the analysis of the input directory are stored, or reused if they" +
                " already exist. The data files are saved in the input directory.\n" +
                "-maxSentencesSelected=# \tThe maximum number of sentences to use. Default value is [outputSize/5].\n" +
                "-chunkScoring \tIf indicated, then sentences are scored depending on their chunks and not as a whole.\n" +
                "-penalizeGrammarChunks \tIf selected, then sentences that contain grammar chunks are penalized.\n" +
                "-eliminateNonContent \tIf selected, all non-content, grammatical words are eliminated in the output summary." +
                "-useAverageChunkScore \tIf selected, then the average of the chunks' content score of a sentence is used as the" +
                "sentence score. Otherwise, the sum of the chunks' content score is considered the sentence score.\n" +
                "-nogrammar \tIf selected, no grammar is used in the process.\n" +
                "-ignoreContentInRedundancy \tIf selected, redundancy will not "
                + "penalize redundant content-related chunks.\n" +
                "-aquaint \tIf documents in DUC/TAC AQUAINT format.");
    }
    
    /** Performs summarization. Call by using the <b>-?</b> switch to get help on the syntax.
     * @param args The command line arguments.
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
        int iMaxSentencesSelected = -1;
        boolean bChunkScoring = false;
        boolean bPenalizeGrammarChunks = false;
        boolean bEliminateNonContent = false;
        boolean bUseAverageChunkScore = false;
        boolean bIgnoreContentInRedundancy = false;
        boolean bNoGrammar = false;
        boolean bAquaint = false;
        
        try {
            sInputDir = utils.getSwitch(hSwitches, "inputDir", "docs/");
            sGrammarDir = utils.getSwitch(hSwitches, "grammarDir", "grammar/");
            iOutputSize = Integer.valueOf(utils.getSwitch(hSwitches, "outputSize", 
                    String.valueOf(100))).intValue();
            iMaxSentencesSelected = Integer.valueOf(utils.getSwitch(hSwitches, 
                    "maxSentencesSelected", String.valueOf(iOutputSize / 5))).intValue();
            sTrainDataDir = utils.getSwitch(hSwitches, "trainDataDir", "");
            bUseInputDirData = Boolean.valueOf(utils.getSwitch(hSwitches, 
                    "useInputDirData", Boolean.FALSE.toString()));
            sQuery = utils.getSwitch(hSwitches, "query", "");
            bChunkScoring = Boolean.valueOf(utils.getSwitch(hSwitches, "chunkScoring", Boolean.FALSE.toString()));
            bPenalizeGrammarChunks = Boolean.valueOf(utils.getSwitch(hSwitches, 
                    "penalizeGrammarChunks", Boolean.FALSE.toString()));
            bEliminateNonContent = Boolean.valueOf(utils.getSwitch(hSwitches, 
                    "eliminateNonContent", Boolean.FALSE.toString()));
            bUseAverageChunkScore = Boolean.valueOf(utils.getSwitch(hSwitches, 
                    "useAverageChunkScore", Boolean.FALSE.toString()));
            bIgnoreContentInRedundancy = Boolean.valueOf(utils.getSwitch(hSwitches,
                    "ignoreContentInRedundancy", Boolean.FALSE.toString()));
            bNoGrammar = Boolean.valueOf(utils.getSwitch(hSwitches, "noGrammar", 
                    Boolean.FALSE.toString()));
            bAquaint = Boolean.valueOf(utils.getSwitch(hSwitches, "aquaint",
                    Boolean.FALSE.toString()));
        }
        catch (Exception e) {
            System.err.println("Cannot parse command line. Aborting.");
            printSyntax();
            return;
        }
        
        // FIXED PARAMETERS
        // final int iTopChunksKeptNum = 1000;
        final int FoldCount = 10;
        final int ActualFolds = FoldCount;
                    
        // Create input document set
        System.err.println("Creating document sets...");
        DocumentSet dsFiles = new DocumentSet(sInputDir, 1.0);
        // Ingore data files
        dsFiles.FileEvaluator = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.getName().endsWith(".dat");
            }
        };
        dsFiles.createSets(true);
        Set<String> sFiles = dsFiles.toFilenameSet(DocumentSet.FROM_WHOLE_SET);
        
        // Init vars to null
        grammaticalityEstimator geEstimator = null;
        DocumentNGramGraph Grammar = null;
        
        boolean bLoadedOK = false;
        // Constants
        final String CHUNKER_MODEL_NAME = "ChunkerModel";
        final String GRAMMAR_MODEL_NAME = "GrammarModel";
        final String NORMALITY_MODEL_NAME = "NormalityModel";
        final String CONTENT_MODEL_NAME = "ContentModel";
        final String DATA_MODELS_CATEGORY = "SumData";
        
        // Attempt loading previous data if requested
        if (sTrainDataDir.length() > 0) {
            try {
                INSECTDB dbTrainingData = new INSECTFileDB("", sTrainDataDir);
//                if (!dbTrainingData.existsObject(NORMALITY_MODEL_NAME, DATA_MODELS_CATEGORY))
//                    throw new FileNotFoundException("Could not find " + NORMALITY_MODEL_NAME + "...");
//                System.err.print("Loading normality estimator...");
//                geEstimator = (grammaticalityEstimator)dbTrainingData.loadObject(NORMALITY_MODEL_NAME, 
//                        DATA_MODELS_CATEGORY);
//                if (geEstimator == null)
//                    throw new IOException("Could not load " + NORMALITY_MODEL_NAME + "...");
//                System.err.println("Done.");
                if (!bNoGrammar) {
                    if (!dbTrainingData.existsObject(GRAMMAR_MODEL_NAME, DATA_MODELS_CATEGORY))
                        throw new FileNotFoundException("Could not find " + GRAMMAR_MODEL_NAME + "...");
                    System.err.print("Loading grammar model...");                
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
                dsGrammarFiles.createSets(false);

                // Analyse documents in input dir for statistical normality. 
    //            System.err.print("Training statistical normality estimator...");
    //            geEstimator = new grammaticalityEstimator(dsGrammarFiles.toFilenameSet(DocumentSet.FROM_TRAINING_SET), 
    //                    1, 3, 1, -1, 1);
    //            geEstimator.train();
    //            System.err.println("Done.");

                // Extract grammar graph from grammar dir
                System.err.println("Extracting grammar...");

                /* DEPRECATED
                Set sAllGrammarDocs = dsGrammarFiles.toFilenameSet(DocumentSet.FROM_TRAINING_SET);
                int iFoldSize=sAllGrammarDocs.size() / FoldCount; // Ten-fold
                for (int iFirstCnt=0; iFirstCnt < FoldCount; iFirstCnt++) {
                    for (int iSecondCnt=0; iSecondCnt < FoldCount; iSecondCnt++) {
                        int iCountNum = iFirstCnt * FoldCount + iSecondCnt;

                        if (iCountNum > ActualFolds) break; // Stop before all folds

                        System.err.print(String.format("Pass %d...", iCountNum));
                        DocumentNGramGraph GrammarPrv = Grammar;
                        List alCurFiles = new ArrayList(sAllGrammarDocs).subList(iFirstCnt * iFoldSize, (iFirstCnt * iFoldSize) + (iFoldSize - 1));
                        String sGrammarText = utils.loadFileSetToString(new HashSet(alCurFiles));
                        DocumentNGramSymWinGraph ngdTmp1 = new DocumentNGramSymWinGraph(3,3,4);               
                        ngdTmp1.setDataString(sGrammarText);

                        DocumentNGramSymWinGraph ngdTmp2 = new DocumentNGramSymWinGraph(3,3,4);
                        alCurFiles = new ArrayList(sAllGrammarDocs).subList(iSecondCnt * iFoldSize, (iSecondCnt * iFoldSize) + (iFoldSize - 1));
                        ngdTmp2.setDataString(utils.loadFileSetToString(new HashSet(alCurFiles)));
                        Grammar = ngdTmp1.intersectGraph(ngdTmp2);
                        // Combine with previous results
                        if (GrammarPrv != null) {
                            Grammar = Grammar.intersectGraph(GrammarPrv);
                        }
                        System.err.println("Done.");
                    }
                }
                */ 
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
                    DocumentNGramSymWinGraph ngdTmp1 = new DocumentNGramSymWinGraph(3,3,4);
                    ngdTmp1.setDataString(sGrammarText);
                    System.err.print("Done. Updating Grammar...");
                    if (Grammar == null) 
                        Grammar = ngdTmp1;
                    else
                        Grammar = Grammar.intersectGraph(ngdTmp1);

                    System.err.println("Done. Current grammar size " + Grammar.length() + ".");

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
                
        // DEBUG LINES
        ////////////////////////////////////////////////
//        // Tokenize documents in input dir 
//        System.err.println("Extracting chunks...");
//        HashSet hChunks = new HashSet();
//        // Use local block to free entropy chunker
//        {
//            EntropyChunker ec = new EntropyChunker();
//            ec.train(sFiles);
//            Iterator<String> iFile = sFiles.iterator();
//            while (iFile.hasNext()) {
//                String sCurFile = iFile.next();
//                System.err.print("Chunking file " + sCurFile + "...");
//
//                String sText = utils.loadFileToStringWithNewlines(sCurFile);
//
//                hChunks.addAll(ec.chunkString(sText));
//                System.err.println("Done.");
//            }
//        }
//        System.err.println("Extracting chunks...Done.");
//        // DEBUG LINES
//        System.out.println("CHUNKS:" + utils.printIterable(new ArrayList(hChunks), "\n"));
//        //////////////
//        //////////////////////////////////////////////

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

            DocumentNGramGraph ContentPrv = null;
            int iCurFileCnt = 0;
            for (String sCurFile : sFiles) {
                System.err.print("Extracting content from " + sCurFile + "...");
                DocumentNGramGraph curContent = new DocumentNGramSymWinGraph(3,3,4); // Init content graph
                curContent.setDataString(utils.loadFileToString(sCurFile));
                if (!bNoGrammar)
                    curContent = curContent.allNotIn(Grammar);
                
                /* DEPRECATED
                // Every 5 intersect, otherwise merge.
                if (Content == null) {
                    // Init content graph
                    Content = curContent;
                    System.err.print("Initial content graph size: " + Content.getGraphLevel(0).getVerticesCount() + " edges.");
                }
                else {
                    if (iCurFileCnt % 5 > 0) {
                        Content.mergeGraph(curContent, 0.5 - (0.5 / (iCurFileCnt % 5)));
                    }
                    else {
                        if (ContentPrv == null)
                            ContentPrv = Content;
                        else
                            ContentPrv = ContentPrv.intersectGraph(Content);
                        System.err.print("Current content graph size: " + ContentPrv.length() + " edges.");
                        if (!bNoGrammar)
                            Content = curContent.allNotIn(Grammar);
                    }
                }
                */ 
                if (Content == null) {
                    // Init content graph
                    Content = curContent;
                    System.err.print("Initial content graph size: " +
                            Content.getGraphLevel(0).getVerticesCount() + " edges.");
                }
                else {
                    Content.mergeGraph(curContent, 0.5 - (0.5 / iCurFileCnt));
                }
                
                System.err.println("Done.");
                iCurFileCnt++;
            }
            
            // Perform final update, based on size of graphs (for a ratio of more than 50% merge, else intersect)
            /* DEPRECATED
             if ((double)Content.length() / (ContentPrv.length() + 1) > 0.5) {
                Content = ContentPrv;
                Content.mergeGraph(Content, 0.5);
            }
            else
                Content = ContentPrv.intersectGraph(Content);
             */ 
            
            System.err.print("Final content graph size: " + Content.length() + " edges.");
            // Save content graph if needed
            if (bUseInputDirData) {
                String sContentFileName = CONTENT_MODEL_NAME + sInputDir;
                INSECTDB dbTrainingData = new INSECTFileDB("", sTrainDataDir);

                System.err.print("Saving content graph...");
                dbTrainingData.saveObject(Content, sContentFileName, DATA_MODELS_CATEGORY);
                System.err.println("Done.");
            }
        }
        
        //System.err.println("Outputting topic content graph...");
        //System.out.println(utils.graphToDot(Content.getGraphLevel(0), false, Content.getEdgesToDistros()));
        //System.err.println("Outputting topic content graph...Done");
        
        NGramCachedGraphComparator dgcComparator = new NGramCachedGraphComparator();
        
        // TODO: Restore
        ////////////////////////////////////////
//        System.err.println("Extracting content similarity for tokens...");
//        
//        // Init token ordered map
//        TreeMap<Double, String> tmTokensValue = new TreeMap<Double, String>();
//        TreeMap<Double, String> tmTokensCooccurence = new TreeMap<Double, String>();
//        
//        // Order tokens by content
//        for (Object oChunk: hChunks) {
//            String sChunk = (String)oChunk;
//            if (sChunk.length() == 0)
//                continue;
//            DocumentNGramSymWinDistroGraph dChunk = new DocumentNGramSymWinDistroGraph(3,3,4);
//            dChunk.setDataString(sChunk);
//            DocumentNGramDistroGraph dChunkContent = dChunk.allNotIn(Grammar);
//            
//            try {
//                GraphSimilarity gsCur = dgcComparator.getSimilarityBetween(Content, dChunkContent);
//                
//                tmTokensValue.put(gsCur.ValueSimilarity, sChunk);
//                tmTokensCooccurence.put(gsCur.ContainmentSimilarity, sChunk);
//                
//                if (tmTokensValue.size() > iTopChunksKeptNum) { // Keep top entries only
//                        tmTokensValue.remove(tmTokensValue.firstKey()); // Remove lesser element
//                        tmTokensCooccurence.remove(tmTokensCooccurence.firstKey()); // Remove lesser element
//                }
//                System.err.print(".");
//            }
//            catch(Exception e) {
//                System.err.println("\nCould not check: ''" + sChunk + "''. Ignoring...");
//            }
//        }
//        
//        System.err.println("\nExtracting content similarity for tokens...Done.");
        ////////////////////////////////////////
        
//        // Show
//        System.err.println("Outputting results for Value");
//        System.out.println("Value\tToken");
//        for (Object elem : tmTokensValue.keySet()) {
//            System.out.println(String.format("%f \t %s", elem, tmTokensValue.get(elem).replaceAll("\n", " ")));
//        }
//        System.err.println("Outputting results for Cooccurence");
//        
//        System.out.println("Cooccurence\tToken");
//        for (Object elem : tmTokensValue.keySet()) {
//            System.out.println(String.format("%f \t %s", elem, tmTokensCooccurence.get(elem).replaceAll("\n", " ")));
//        }
        
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
            
            // Create query string representation, to use in sentence selection.
            DocumentNGramSymWinGraph gQuery = null;
            NGramCachedGraphComparator ncgcComparator = new 
                    NGramCachedGraphComparator();
            if (sQuery.length() > 0) {
                gQuery = new DocumentNGramSymWinGraph();
                gQuery.setDataString(sQuery);
            }
            
            Iterator<String> iFile = sFiles.iterator();
            while (iFile.hasNext()) {
                String sCurFile = iFile.next();
                System.err.println("Extracting and assigning importance to sentences from " + sCurFile + "...");
                String sText = utils.loadFileToStringWithNewlines(sCurFile);
                //String sText = utils.loadFileToStringWithNewlines(sCurFile).replaceAll("\n", " ");
                // Extract TEXT ONLY
                if (bAquaint) {
                    sText = sText.substring(sText.indexOf(ACQUAINT2DocumentSet.TEXT_TAG) + ACQUAINT2DocumentSet.TEXT_TAG.length() + 1);
                    sText = sText.substring(0, sText.indexOf(ACQUAINT2DocumentSet.TEXT_TAG));
                }
                else if (sText.contains("HTML")) {
                    try {
                        sText = utils.extractText(sText);
                    }
                    catch (Exception e) {
                        System.err.println("Not an HTML file. Keeping whole.");
                    }
                }

                // For each sentence
                double dSentenceScore = 0.0;
                double dSentenceGrammarScore = 0.0;
                // TODO: Implement good chunking
                // HIGH Granularity chunking with: "([.,;-]+:*[ ]+)|(<P>)|(</P>)"
                // LOW Granularity chunking with "[.]|(<P>)|(</P>)"


                // Using smart sentence splitter
                // TODO: Split normally, or PROVIDE SPLIT: 1 SENTENCE PER LINE
                String[] sCandSentences = sText.split("\n+");
                

                for (String sSentence : Arrays.asList(sCandSentences)) {
//                for (String sSentence : Arrays.asList(sText.split("([.,;-]+:*[ ]+)|(<P>)|(</P>)"))) {
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
                                
                            DocumentNGramSymWinGraph dChunk = new DocumentNGramSymWinGraph(3,3,4);
                            dChunk.setDataString(sCurChunk);
                            
                            // Penalize grammar, if selected
                            if (!bNoGrammar)
                                if (bPenalizeGrammarChunks) {
                                    double dGrammarChunkScore = 
                                            dgcComparator.getSimilarityBetween(dChunk, Grammar).ValueSimilarity;
                                    dSentenceGrammarScore += dGrammarChunkScore;
                                }
                            DocumentNGramGraph dChunkContent;
                            if (!bNoGrammar) 
                                dChunkContent = dChunk.allNotIn(Grammar);
                            else
                                dChunkContent = dChunk;
                            
                            double dChunkScore = dgcComparator.getSimilarityBetween(Content, 
                                    dChunkContent).ValueSimilarity;
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
                        DocumentNGramSymWinGraph gSentence = new DocumentNGramSymWinGraph(3,3,4);
                        gSentence.setDataString(sSentence);
                        DocumentNGramGraph gSentenceContent = null;
                        if (!bNoGrammar)
                            gSentenceContent = gSentence.allNotIn(Grammar);
                        else
                            gSentenceContent = gSentence;
                        
                        dSentenceScore = dgcComparator.getSimilarityBetween(Content, gSentenceContent).ValueSimilarity;
                        if (!bNoGrammar)
                            dSentenceGrammarScore = dgcComparator.getSimilarityBetween(Grammar, gSentence).ValueSimilarity;
                    }
                    // Deprecated: Get SENTENCE grammar similarity
                    // DocumentNGramSymWinDistroGraph dSentenceGrammar = new DocumentNGramSymWinDistroGraph(3,3,4);
                    // dSentenceGrammar.setDataString(sSentence);
                    // dSentenceGrammarScore = dgcComparator.getSimilarityBetween(Grammar, dSentenceGrammar).ValueSimilarity;
                    //////////////////////////////////////////////
                    
                    // Use query similarity to complete sentence
                    DocumentNGramSymWinGraph gSentence = new DocumentNGramSymWinGraph();
                    gSentence.setDataString(sSentence);
                    // If query exists
                    if (gQuery != null)
                        // Multiply sentence score by similarity to query (plus a small number)
                        dSentenceScore *= 1.0 + (ncgcComparator.getSimilarityBetween(gQuery, 
                                gSentence).ContainmentSimilarity);
                    // Add sentence to ordering, using log importance
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

        
        // Select sentences based on novelty
        //NoveltyBasedSelector<String,String> nbsNovelty = new NoveltyBasedSelector<String,String>();
        // USE original importance ranking
        CombinedNoveltyBasedSelector<String,String> nbsNovelty = new CombinedNoveltyBasedSelector<String,String>();

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
        final boolean argBIgnoreContent = bIgnoreContentInRedundancy;
        
        // Filter removes grammar and pure content words, leaving differences
        nbsNovelty.SentenceRepresentationFilter = new IObjectFilter<DocumentNGramGraph>() {
            @Override
            public DocumentNGramGraph filter(DocumentNGramGraph obj) {
                DocumentNGramGraph gRes = obj;
                if (!argBNoGrammar)
                    gRes = obj.allNotIn(argGrammar);
                if (argBIgnoreContent)
                    gRes = gRes.allNotIn(argContent);
                return gRes; // Compare non-common part
            }
        };
        
        List<String> lNoveltyBasedSentences = nbsNovelty.selectFromSentences(alImportantSentences);
        int iCurSummaryWordCount = 0;
        Iterator<String> iSentenceIterator = lNoveltyBasedSentences.iterator();
        ArrayList<String> alSummarySentences = new ArrayList<String>();
        
        // DEBUG LINES
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
                    DocumentNGramSymWinGraph dCurChunk = new 
                            DocumentNGramSymWinGraph(3,3,4);
                    dCurChunk.setDataString(sChunk);
                    if (!bNoGrammar)
                        // Omit grammatical tokens
                        if (dgcComparator.getSimilarityBetween(dCurChunk, Grammar).ValueSimilarity <= 
                                dgcComparator.getSimilarityBetween(dCurChunk, Content).ValueSimilarity)
                            continue;
                    
                    sbFilteredSentence.append(sChunk);
                }
                else
                    // else, add chunk without applying filter
                    sbFilteredSentence.append(sChunk);
                
                if (sChunk.trim().length() > 0)
                    iCurSentenceSize++;
            }
                            
            if (iCurSummaryWordCount +  iCurSentenceSize > iOutputSize)
                continue; // Would exceed max size
            // If all is OK, add sentence to output summary
            alSummarySentences.add(sbFilteredSentence.toString());
            iCurSummaryWordCount += iCurSentenceSize ; // Update word count
            System.err.println("Added \n" + sCur + "\n to summary.");
        }
        
        // Output summary
        System.out.println(String.format("SUMMARY using %d sentences:", alSummarySentences.size()));
        // Remove double fullstops
        System.out.println(utils.printIterable(alSummarySentences, ".\n").replaceAll("[.]{2}", "."));
    }
    
}
