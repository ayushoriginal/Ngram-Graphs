/*
 * HLDATextGenerator.java
 *
 * Created on October 26, 2007, 11:30 AM
 *
 */

package gr.demokritos.iit.summarization.generation;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import gr.demokritos.iit.jinsect.console.ConsoleNotificationListener;
import gr.demokritos.iit.jinsect.console.grammaticalityEstimator;
import gr.demokritos.iit.jinsect.interoperability.HierLDACaller;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import probabilisticmodels.HierLDAGibbs;
import gr.demokritos.iit.summarization.evaluation.grammar.DistributionDocumentEvaluator;
import gr.demokritos.iit.summarization.evaluation.grammar.IGrammaticallityEvaluator;

/** A generator of texts, based on the HLDA model of a text corpus and a grammar evaluator.
 *
 * @author ggianna
 */
public class HLDATextGenerator {
    HierLDAGibbs Model;
    IGrammaticallityEvaluator Evaluator;
    Map<Integer,String> WordMap;
    public Date dStart = new Date();
    HashMap<Integer,Distribution> hmDistroPerTopic = new HashMap(); // Holds topic - distro pairs for caching.
    
    /** Creates a new instance of HLDATextGenerator, given and HLDA model and a grammaticality evaluator. */
    public HLDATextGenerator(HierLDAGibbs hlgModel, IGrammaticallityEvaluator igeEval,
            Map<Integer, String> mWordMap) {
        Model = hlgModel;
        Evaluator = igeEval;
        WordMap = mWordMap;
    }
    
    /** Randomly generates a word (index) based on the overall distribution of words over topics.
     * The probability of generation of a candidate word is directly proportional to the 
     * grammaticality of the given preceding text, followed by the candidate word, and also proportional
     * to the probability of appearence of the given word, within the whole corpus.
     *@param vCurrentText The preceding text.
     *@return The index of the generated word.
     */
    public int generateNextWord(Vector vCurrentText) {
        // Get distribution of words, according to FULL model.
        
        // Select next topic
        int iTopic = Model.generateNextLeafTopic();
        
        Distribution dWordsAtSelectedTopic;
        // Check for cached distro
        if (hmDistroPerTopic.containsKey(iTopic)) {
            dWordsAtSelectedTopic =  hmDistroPerTopic.get(iTopic);
        }
        else
        {
            // Get distribution
            dWordsAtSelectedTopic =  Model.getTopicTermDistro(Model.getNumOfLevels(), 
                iTopic);
            // Update cache
            hmDistroPerTopic.put(iTopic, dWordsAtSelectedTopic);
        }
        
        // For every word
        Distribution dOutputDistro = new Distribution();
        Iterator iWords = dWordsAtSelectedTopic.asTreeMap().keySet().iterator();
        while (iWords.hasNext()) {
            Object oWord = iWords.next();

            // Create a candidate text using current word
            vCurrentText.add(oWord);
            // Get the grammaticality of the text
            double dGram = Evaluator.getGrammaticallity(vCurrentText);
            // Reset text to previous state
            vCurrentText.remove(vCurrentText.size() - 1);
            // TODO: CHECK
            // Ignore if grammaticality is zero
            if (dGram != 0)
                dOutputDistro.setValue(oWord, dWordsAtSelectedTopic.getValue(oWord) * dGram);
            //////////////
        }
            
        // If no match has been found, return a random word.
        if (dOutputDistro.asTreeMap().size() == 0)
            return ((Integer)dWordsAtSelectedTopic.getNextResult()).intValue();
        // Actually select word
        return ((Integer)dOutputDistro.getNextResult()).intValue();
        //return ((Integer)dWordsAtSelectedTopic.getKeyOfMaxValue()).intValue();
    }
    
    /** Generates a normal (string) text , based on the model, given the mean text length.
     *@param iMeanSize The mean text length in terms.
     *@param iGrammarVincinity The distance upon which to calculate grammaticality.
     *@return A list of term indices.
     */
    public Vector<Integer> generateNormalText(int iMeanSize, int iGrammarVincinity) {
        
        // Sample text size based on a poisson calculation.
        int iTextSize = (int)(gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation.getPoissonNumber(iMeanSize));
        Vector<Integer> vText = new Vector<Integer>(iTextSize);
        
        while (iTextSize-- > 0) {
            Vector<Integer> vCur = new Vector();
            vCur.addAll(vText.subList(Math.max(vText.size() - iGrammarVincinity, 0),
                    vText.size()));
            vText.add(generateNextWord(vCur));
            // DEBUG LINES
            System.err.print(".");
            //////////////
        }
        return vText;
    }
    
    /** Gets a vector of indices representing a text and returns the actual text representation, based on the
     * integer to string map of the text generator.
     *@param vText A {@link Vector} of integers, representing indices of strings in a given map.
     */
    public String getVectorToText(Vector<Integer> vText) {
        Iterator<Integer> iIter = vText.iterator();
        StringBuffer sbText = new StringBuffer();
        while (iIter.hasNext()) {
            Integer iCurWord = iIter.next();
            sbText.append(WordMap.get(iCurWord));
        }
        
        return sbText.toString();
    }
    
    /** Utility method that outputs syntax information for calling the main class.
     */
    public static void printSyntax() {
        System.out.println("Syntax:\n" + HLDATextGenerator.class.getName() + " [-inputDir=corpusPath] [-inputDirFlat] [-model=modelFile]" +
                " [-docMatrix=docMatrixFile] [-levels=#] [-iters=#] [-burnIn=#] [-alpha=#.##] [-beta=#.##] [-threads=#]" +
                " [-recalc] [-textMeanSize=#]\n" +
                "-inputDir=corpusPath\tThe directory with the input documents. Can contain subdirectories for categories or not " +
                "(see -inputDirFlat option).\n" +
                "-inputDirFlat\tIf supplied expects that the input document directory contains the documents in itself, and" +
                " not in subdirectories.\n" +
                "-model=modelFile\t The modelfile that holds or should hold HLDA model data. If no such file exists, it is created.\n" +
                "-docMatrix=docMatrixFile\t The modelfile that holds or should hold document word matric data.\n" +
                "If no such file exists, it is created.\n" +
                "-levels=#\tThe levels of the hierarchy in the model.\n" +
                "-iters=#\tThe iterations for the HLDA inference.\n" +
                "-burnIn=#\tThe burn-in iterations for the HLDA inference.\n" +
                "-alpha=#\tThe alpha parameter for the HLDA inference.\n" +
                "-beta=#\tThe beta parameter for the HLDA inference.\n" +
                "-threads=#\tThe number of threads to use for the inference.\n" +
                "-recalc\tIf supplied, will ignore model and document matrix data and recalculate them.\n" +
                "-textMeanSize=#\tThe mean number of words for the supplied texts.");
        
    }
    
    /** Utility main method that creates a random text, based on a model corpus. 
     */
    public static void main(String[] sArgs) {
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(sArgs);
        String sInputDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "inputDir", ".");
        boolean bInputDirFlat = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "inputDirFlat", 
                Boolean.FALSE.toString()));
        String sModel = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "model", "");
        String sDocumentMatrix = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "docMatrix", "");
        int iLevels = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "levels", "5")).intValue();
        int iIterations = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "iters", "10000")).intValue();
        int iBurnInIterations = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "burnIn", "1000")).intValue();
        double dAlpha = Double.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "alpha", "2.0")).doubleValue();
        double dBeta = Double.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "beta", "0.5")).doubleValue();
        int iThreads = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "threads", String.valueOf(Runtime.getRuntime().availableProcessors()))).intValue();
        boolean bRecalc = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "recalc", 
                "FALSE")).booleanValue();
        int iTextMeanSize = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "textMeanSize", 
                "250")).intValue();
        if (Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "help", 
                "FALSE")).booleanValue()) 
        {
            printSyntax();
            return;
        }
        
        ConsoleNotificationListener cnlReporting = new ConsoleNotificationListener();
        
        TreeMap<Integer, String> tmIdxToStr = new TreeMap<Integer, String>();
        // Init HLDA
        System.err.println("Initializing HLDA...");
        
        // Read term document matrix
        int[][] dtm = null;
        boolean bMatrixLoadedOK = false;
        if ((sDocumentMatrix.length() != 0) && !bRecalc) {
            try {
                System.err.println("Loading document-term matrix...");
                FileInputStream fis = new FileInputStream(sDocumentMatrix);
                ObjectInputStream ois = new ObjectInputStream(fis);
                dtm = (int[][])ois.readObject(); // Read model from file
                tmIdxToStr = (TreeMap<Integer,String>)ois.readObject(); // Read map from file
                ois.close();
                fis.close();
                bMatrixLoadedOK = true;
            }
            catch (Exception e) {
                System.err.println("Could not load document term matrix.");
            }
        };
        
        if (!bMatrixLoadedOK)
        {
            DocumentSet dm = new DocumentSet(sInputDir,1.0);
            dm.createSets(false, 1.0, bInputDirFlat);
            System.err.print("Calculating document term matrix...");
            dtm = HierLDACaller.getDocumentTermMatrix(dm.getTrainingSet(), tmIdxToStr);
            System.err.println("Done.");

            // Save to file
            System.err.println("Saving document-term matrix...");
            if (sDocumentMatrix.length() != 0) {
                try {
                    FileOutputStream fos = new FileOutputStream(sDocumentMatrix);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(dtm); // Write document term matrix to file
                    oos.writeObject(tmIdxToStr); // Write map to file
                    oos.close();
                    fos.close();
                }
                catch (Exception e) {
                    System.err.println("Could not save document term matrix.");
                    e.printStackTrace(System.err);
                }
            }

        }
        HierLDAGibbs hierLDA = null;
        boolean bHLDALoadedOK = false;
        if ((sModel.length() != 0) && !bRecalc) {
            File fLDAModel = new File(sModel);
            try {
                System.err.println("Loading HLDA model...");
                FileInputStream fis = new FileInputStream(fLDAModel);
                ObjectInputStream ois = new ObjectInputStream(fis);
                hierLDA = (HierLDAGibbs)ois.readObject(); // Read model from file
                ois.close();
                fis.close();
                bHLDALoadedOK = true;
            }
            catch (Exception e) {
                System.err.println("Could not load HLDA model...");
                hierLDA = new HierLDAGibbs(iLevels,dtm, dAlpha, dBeta);
                System.err.println("Performing HLDA sampling...");
                hierLDA.setProgressIndicator(cnlReporting); // Set notification listener
                hierLDA.performGibbs(iIterations, iBurnInIterations, Runtime.getRuntime().availableProcessors());
            }
        }
        else {
            hierLDA = new HierLDAGibbs(iLevels,dtm, dAlpha, dBeta);
            hierLDA.setProgressIndicator(cnlReporting); // Set notification listener
            System.err.println("Performing HLDA sampling...");
            hierLDA.performGibbs(iIterations, iBurnInIterations, Runtime.getRuntime().availableProcessors());
        }
        // Save to file, if not loaded
        if ((sModel.length() != 0) && (!bHLDALoadedOK)) {
            try {
                System.err.println("Saving HLDA model...");
                FileOutputStream fos = new FileOutputStream(sModel);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(hierLDA); // Write document term matrix to file
                oos.close();
                fos.close();
            }
            catch (Exception e) {
                System.err.println("Could not save document term matrix.");
                e.printStackTrace(System.err);
            }
        }
        
        
        System.err.println("Initializing and training grammaticality estimator...");
        grammaticalityEstimator ge = new grammaticalityEstimator(sInputDir, 1, -1, 1, 3, 2, bInputDirFlat);
        ge.train();
        
        System.err.println("Initializing text generator...");
        HLDATextGenerator tg = new HLDATextGenerator(hierLDA,new DistributionDocumentEvaluator(tmIdxToStr, ge),
                tmIdxToStr);
        
        // TODO: Add vicinity as a parameter
        for (int iGramVinc=1; iGramVinc < 2; iGramVinc++) {
            System.err.println("Creating text... Grammar rank " + iGramVinc);
            Vector<Integer> vText = tg.generateNormalText(iTextMeanSize, iGramVinc);
            String sText = tg.getVectorToText(vText);
            // Restore blanks
            sText = sText.replaceAll("_/", " ");
            sText = sText.replaceAll("\\\\_", " ");
            System.out.println(sText);
            System.err.println("Grammaticality:" + ge.getNormality(sText));
        }
        System.err.println("Complete.");
        System.exit(0);
    }

}
