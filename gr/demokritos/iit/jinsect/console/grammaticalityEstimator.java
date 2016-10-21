/*
 * grammaticalityEstimator.java
 *
 * Created on October 19, 2007, 11:22 AM
 *
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.conceptualIndex.documentModel.DistributionDocument;
import gr.demokritos.iit.conceptualIndex.documentModel.DistributionWordDocument;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.ducTools.DUCDocumentInfo;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import java.io.File;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** The grammaticality estimator uses the probability of finding a given token (character)
 * after a given n-gram (string), extracted from a text corpus, in order to determine normality of
 * other (new) strings. 
 * @see DistributionDocument
 *
 * @author ggianna
 */
public class grammaticalityEstimator implements Serializable {
    
    /** Map between level and distribution documents.
     */
    protected TreeMap<Integer,DistributionDocument> DistroDocs;
    /** Map between level and word distribution documents.
     */
    protected TreeMap<Integer,DistributionWordDocument> DistroWordDocs;
    
    /** The minimum and maximum n-gram sizes to take into account.
     */
    protected int iMinCharNGram, iMaxCharNGram, iMinWordNGram, iMaxWordNGram;
    
    /** The word and character n-gram neighbourhood sizes.
     */
    protected int iWordDist, iCharDist;
    
    /** The concatenation of all corpus texts.
     */
    protected String FullTextDataString;
    
    private void writeObject(java.io.ObjectOutputStream out)
     throws IOException 
    {
        // Char params
        out.writeInt(iMinCharNGram);
        out.writeInt(iMaxCharNGram);
        out.writeInt(iCharDist);
        // Word params
        out.writeInt(iMinCharNGram);
        out.writeInt(iMaxCharNGram);
        out.writeInt(iCharDist);
        // Tree maps
        out.writeObject(DistroDocs);
        out.writeObject(DistroWordDocs);
        // Full data string
        out.writeObject(FullTextDataString);
    }

    private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException 
    {
        // Char params
        iMinCharNGram = in.readInt();
        iMaxCharNGram = in.readInt();
        iCharDist = in.readInt();
        // Word params
        iMinCharNGram = in.readInt();
        iMaxCharNGram = in.readInt();
        iCharDist = in.readInt();
        // Tree maps
        DistroDocs = (TreeMap<Integer,DistributionDocument>)in.readObject();
        DistroWordDocs = (TreeMap<Integer,DistributionWordDocument>)in.readObject();
        // Full data string
        FullTextDataString = (String)in.readObject();
    }

/**
 * Creates a new instance of grammaticalityEstimator, using a given text
 * for training.
 *
 *
 * @param sText The full text for training.
 * @param iMinChar The minimum character n-gram size to take into account.
 * @param iMaxChar The maximum character n-gram size to take into account.
 * @param iCharWindow The neighbourhood window to use for the calculation of
 * n-gram - token neighbourhood of characters.
 * @param iMinWord The minimum word n-gram size to take into account.
 * @param iMaxWord The maximum word n-gram size to take into account.
 * @param iWordWindow The neighbourhood window to use for the calculation of
 * n-gram - token neighbourhood of words.
 */
    public grammaticalityEstimator(String sText, int iMinChar, int iMaxChar, int iCharWindow,
            int iMinWord, int iMaxWord, int iWordWindow) {
        iMinCharNGram = iMinChar;
        iMaxCharNGram = iMaxChar;
        iMinWordNGram = iMinWord;
        iMaxWordNGram = iMaxWord;
        iWordDist = iWordWindow;
        iCharDist = iCharWindow;

        DistroDocs = new TreeMap<Integer,DistributionDocument>();
        DistroWordDocs = new TreeMap<Integer,DistributionWordDocument>();
        // Save to full text data string
        FullTextDataString = sText;

        // Init distro docs
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistroDocs.put(iCnt, new DistributionDocument(iCharDist, iCnt));
        }

        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            DistroWordDocs.put(iCnt, new DistributionWordDocument(iWordDist, iCnt));
        }

    }

/**
     * Creates a new instance of grammaticalityEstimator, using a given set of documents for training.
     * 
     * 
     * @param FileNames A set of filenames to be used as input training set.
     * @param iMinChar The minimum character n-gram size to take into account.
     * @param iMaxChar The maximum character n-gram size to take into account.
     * @param iCharWindow The neighbourhood window to use for the calculation of 
     * n-gram - token neighbourhood of characters.
     * @param iMinWord The minimum word n-gram size to take into account.
     * @param iMaxWord The maximum word n-gram size to take into account.
     * @param iWordWindow The neighbourhood window to use for the calculation of 
     * n-gram - token neighbourhood of words.
     */
    public grammaticalityEstimator(Set FileNames, int iMinChar, int iMaxChar, int iCharWindow,
            int iMinWord, int iMaxWord, int iWordWindow) {
        iMinCharNGram = iMinChar;
        iMaxCharNGram = iMaxChar;
        iMinWordNGram = iMinWord;
        iMaxWordNGram = iMaxWord;
        iWordDist = iWordWindow;
        iCharDist = iCharWindow;
        
        DistroDocs = new TreeMap<Integer,DistributionDocument>();
        DistroWordDocs = new TreeMap<Integer,DistributionWordDocument>();
        StringBuffer sb = new StringBuffer();
        
        Iterator iTexts = FileNames.iterator();
        while (iTexts.hasNext()) {
            String sFile = (String)iTexts.next();
            try {
                BufferedReader bf = new BufferedReader(new FileReader(sFile));
                String sTmp;
                while ((sTmp = bf.readLine()) != null)
                    sb.append(sTmp);
                bf.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            // Indicate text separator
            sb.append((char)0);
        }
        // Save to full text data string
        FullTextDataString = sb.toString();
        
        // Init distro docs
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistroDocs.put(iCnt, new DistributionDocument(iCharDist, iCnt));
        }
        
        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            DistroWordDocs.put(iCnt, new DistributionWordDocument(iWordDist, iCnt));
        }
    }
    
    /**
     * Creates a new instance of grammaticalityEstimator, using a given set of documents for training.
     * 
     * 
     * @param FileNames A set of filenames to be used as input training set.
     * @param iMinChar The minimum character n-gram size to take into account.
     * @param iMaxChar The maximum character n-gram size to take into account.
     * @param iMinWord The minimum word n-gram size to take into account.
     * @param iMaxWord The maximum word n-gram size to take into account.
     * @param iNeighbourhoodWindow The neighbourhood window to use for the calculation of 
     * n-gram - token neighbourhood.
     */
    public grammaticalityEstimator(Set FileNames, int iMinChar, int iMaxChar, 
            int iMinWord, int iMaxWord, int iNeighbourhoodWindow) {
        iMinCharNGram = iMinChar;
        iMaxCharNGram = iMaxChar;
        iMinWordNGram = iMinWord;
        iMaxWordNGram = iMaxWord;
        
        DistroDocs = new TreeMap<Integer,DistributionDocument>();
        DistroWordDocs = new TreeMap<Integer,DistributionWordDocument>();
        StringBuffer sb = new StringBuffer();
        
        Iterator iTexts = FileNames.iterator();
        while (iTexts.hasNext()) {
            String sFile = (String)iTexts.next();
            try {
                BufferedReader bf = new BufferedReader(new FileReader(sFile));
                String sTmp;
                while ((sTmp = bf.readLine()) != null)
                    sb.append(sTmp);
                bf.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            // Indicate text separator
            sb.append((char)0);
        }
        // Save to full text data string
        FullTextDataString = sb.toString();
        
        // Init distro docs
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistroDocs.put(iCnt, new DistributionDocument(iNeighbourhoodWindow, iCnt));
        }
        
        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            DistroWordDocs.put(iCnt, new DistributionWordDocument(iNeighbourhoodWindow, iCnt));
        }
    }
    
    /**
     * Creates a new instance of grammaticalityEstimator.
     * 
     * 
     * @param sCorpusDir The path to the directory containing the training corpus.
     * @param iMinChar The minimum character n-gram size to take into account.
     * @param iMaxChar The maximum character n-gram size to take into account.
     * @param iMinWord The minimum word n-gram size to take into account.
     * @param iMaxWord The maximum word n-gram size to take into account.
     * @param iNeighbourhoodWindow The neighbourhood window to use for the calculation of 
     * n-gram - token neighbourhood.
     * @param bFlatDir If true, then the corpus is supposed to be a set of texts in
     */    
    public grammaticalityEstimator(String sCorpusDir, int iMinChar, int iMaxChar,
            int iMinWord, int iMaxWord, int iNeighbourhoodWindow,
            boolean bFlatDir) {
        iMinCharNGram = iMinChar;
        iMaxCharNGram = iMaxChar;
        iMinWordNGram = iMinWord;
        iMaxWordNGram = iMaxWord;
        
        DistroDocs = new TreeMap<Integer,DistributionDocument>();
        DistroWordDocs = new TreeMap<Integer,DistributionWordDocument>();
        StringBuffer sb = new StringBuffer();
        
        DocumentSet ds = new DocumentSet(sCorpusDir,1.0);
        ds.createSets(bFlatDir); // Create
        
        Iterator iTexts = ds.getTrainingSet().iterator();
        while (iTexts.hasNext()) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)iTexts.next();
            try {
                BufferedReader bf = new BufferedReader(new FileReader(cfeCur.getFileName()));
                String sTmp;
                while ((sTmp = bf.readLine()) != null)
                    sb.append(sTmp);
                bf.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            // Indicate text separator
            sb.append((char)0);
        }
        // Save to full text data string
        FullTextDataString = sb.toString();
        
        // Init distro docs
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistroDocs.put(iCnt, new DistributionDocument(iNeighbourhoodWindow, iCnt));
        }
        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            DistroWordDocs.put(iCnt, new DistributionWordDocument(iNeighbourhoodWindow, iCnt));
        }
    }
       
    /** Performs the training of the distribution model.
     * @param bResetExisting If true, then existing data is cleared, before
     * training. If false, update is performed.
     */
    public void train(String sUpdateText, boolean bResetExisting) {
        // Reset if needed
        if (!bResetExisting)
            FullTextDataString = FullTextDataString + sUpdateText;
        else
            FullTextDataString = sUpdateText;
        
        // Train distro docs

        // For every distro document
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistributionDocument dCur =  DistroDocs.get(iCnt);
            // Actually train
            if (dCur != null)
                if (!bResetExisting)
                    dCur.setDataString(sUpdateText, iCnt, bResetExisting);
                else
                    dCur.setDataString(FullTextDataString, iCnt, bResetExisting);

        }

        // For every distro document
        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            // Same for word distros
            DistributionWordDocument dWordCur =  DistroWordDocs.get(iCnt);
            // Actually train
            if (dWordCur != null)
                if (!bResetExisting)
                    dWordCur.setDataString(sUpdateText, iCnt, 
                            bResetExisting);
                else
                    dWordCur.setDataString(FullTextDataString, iCnt,
                            bResetExisting);
        }
    }

    /** Performs the training of the distribution model.
     */
    public void train() {
        // Train distro docs
        
        // For every distro document
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistributionDocument dCur =  DistroDocs.get(iCnt);
            // Actually train
            if (dCur != null)
                dCur.setDataString(FullTextDataString, iCnt, true);
            
        }
        
        // For every distro document
        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            // Same for word distros
            DistributionWordDocument dWordCur =  DistroWordDocs.get(iCnt);
            // Actually train
            if (dWordCur != null)
                dWordCur.setDataString(FullTextDataString, iCnt, true);
        }
    }
    
    /** Calculates a degree of normality, indicating whether a given string appears in a form
     * similar to text in the training corpus. The normality is the mean value of a distribution of
     * normalities for all n-gram sizes.
     *@param sStr The string to test.
     *@return A measure of normality as a double.
     *@see DistributionDocument
     */    
    public double getNormality(String sStr) {
        Distribution dDist = new Distribution();
        if (iMinCharNGram > 0)
            for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
                DistributionDocument dCur =  DistroDocs.get(iCnt);
                dDist.setValue(Double.valueOf(iCnt), dCur.normality(sStr));
            }
        
        if (iMinWordNGram > 0)
            for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
                DistributionWordDocument dCur =  DistroWordDocs.get(iCnt);
                dDist.setValue(Double.valueOf(iCnt), (dDist.getValue(Double.valueOf(iCnt)) + 
                        dCur.normality(sStr)) / 2.0);
            }
        
        if (dDist.sumOfValues() > 0.0)
            return dDist.average(false);
        else
            return 0.0; // Everything is zero
    }


    /** Calculates degrees of normality, indicating whether a given string appears in a form
     * similar to text in the training corpus. The distribution of
     * normalities for all <i>character</i> n-gram sizes is the result.
     *@param sStr The string to test.
     *@return A distribution mapping n-gram sizes to a double normality estimation.
     */
    public Distribution<Double> getCharNormalityDistro(String sStr) {
        Distribution<Double> dDist = new Distribution<Double>();
        for (int iCnt=iMinCharNGram; iCnt<=iMaxCharNGram; iCnt++) {
            DistributionDocument dCur =  DistroDocs.get(iCnt);
            if (dCur != null)
                dDist.setValue(Double.valueOf(iCnt), dCur.normality(sStr));
        }

        return dDist;
    }

    /** Calculates a degree of normality, indicating whether a given string appears in a form
     * similar to text in the training corpus. The normality is the mean value of a distribution of
     * normalities for all <i>character</i> n-gram sizes.
     *@param sStr The string to test.
     *@return A measure of character normality as a double.
     *@see DistributionDocument
     */    
    public double getCharNormality(String sStr) {
        Distribution dDist = getCharNormalityDistro(sStr);
        
        // Check for zero value
        if (dDist.sumOfValues() == 0)
            return 0.0;
            
        if (dDist.asTreeMap().size() == 1)
            return (Double)dDist.asTreeMap().get(dDist.asTreeMap().firstKey()); // Single item distro. Do NOT Average.
        else
            return dDist.average(false); // Multi size distro. Return average.
    }
    
    /** Calculates degrees of normality, indicating whether a given string appears in a form
     * similar to text in the training corpus. The distribution of
     * normalities for all <i>word</i> n-gram sizes is the result.
     *@param sStr The string to test.
     *@return A distribution mapping n-gram sizes to a double normality estimation.
     */
    public Distribution<Double> getWordNormalityDistro(String sStr) {
        Distribution<Double> dDist = new Distribution<Double>();
        for (int iCnt=iMinWordNGram; iCnt<=iMaxWordNGram; iCnt++) {
            DistributionWordDocument dCur =  DistroWordDocs.get(iCnt);
            if (dCur != null)
                dDist.setValue(Double.valueOf(iCnt), dCur.normality(sStr));
        }

        return dDist;
    }
    
    /** Calculates a degree of normality, indicating whether a given string appears in a form
     * similar to text in the training corpus. The normality is the mean value of a distribution of
     * normalities for all <i>word</i> n-gram sizes.
     *@param sStr The string to test.
     *@return A measure of normality as a double.
     *@see DistributionDocument
     */    
    public double getWordNormality(String sStr) {
        Distribution dDist = getWordNormalityDistro(sStr);
         
        // Check for zero value
        if (dDist.sumOfValues() == 0)
            return 0.0;
            
        if (dDist.asTreeMap().size() == 1)
            return (Double)dDist.asTreeMap().get(dDist.asTreeMap().firstKey()); // Single item distro. Do NOT Average.
        else
            return dDist.average(false); // Multi size distro. Return average.
    }
    
    public boolean saveToStream(OutputStream os) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(this);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return false; // Failed
    }
    
    public static grammaticalityEstimator loadFromStream(InputStream is) {
        grammaticalityEstimator res;
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            res = (grammaticalityEstimator) ois.readObject();
            return res;

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
        
        return null;
    }

    /** Provides command-line syntax information for the execution of the class's main function. */
    public static void printSyntax() {
        System.out.println("Usage: " + grammarAndContentAnalysis.class.getName() + 
                "-corpusDir=xxxx The corpus base directory.\n" +
                "-peerDir=xxxx The peer document set base directory.\n" +
                "-modelDir=xxxx The model document set base directory.\n" +
                "[-minChar=#] The min character n-gram rank." +
                "[-maxChar=#] The max character n-gram rank." +
                "[-charDist=#] The neighbourhood window for characters." +
                "[-minWord=#] The min word n-gram rank." +
                "[-maxWord=#] The max word n-gram rank." +
                "[-wordDist=#] The neighbourhood window for words." +
                "[-flatCorpusDir] If provided, indicates that the corpus directory does not have" +
                " a subdirectory for each category / theme of texts. Thus, it is all contained " +
                "in a single directory." +
                "[-categoryLimit=#] The maximum number of categories to examine.\n" +
                "[-modelFile=filename] The model file to use either as input or as output for the grammar" +
                " model." +
                "[-perCategoryModel] If provided, then a different model is created for every iteration. Otherwise," +
                " the model from the first iteration is used in all following ones.\n" +
                "NOTE: The files should preferrably be named after the DUC format:\n" +
                "TopicID.M.wordLength.Assessor.PeerSystemID");
    }

    /** A utility main method that performs grammaticality estimation, given a corpus, a peer 
     * document set and a model document set.
     */
    public static void main(String[] args) {
        // Read options
        Hashtable hOptions = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        
        String sCorpusDir = gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "corpusDir", 
                "./corpus/");
                //"/downloads/Torrents/Data/DUC2006/duc2006_docs/");
        
        String sPeerDir = gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "peerDir", 
                "./peers/");
                //"/home/ggianna/JInsect/DUC/peers2006/");
        
        String sModelDir = gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "modelDir", 
                "./models/");
                //"/home/ggianna/JInsect/DUC/models2006/");
        
        String sModelFile = gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "modelFile", 
                "");
        
        boolean bFlatCorpusDir = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(
                hOptions, "flatCorpusDir", String.valueOf(false))).booleanValue();
        boolean bPerCategoryModel = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(
                hOptions, "perCategoryModel", String.valueOf(false))).booleanValue();
        
//        String sPeerDir = jinsect.utils.getSwitch(hOptions, "peerDir", 
//                "");
//        String sModelDir = jinsect.utils.getSwitch(hOptions, "modelDir", 
//                "");
        int iMinChar = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "minChar", 
                "1"));
        int iMaxChar = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "maxChar", 
                "5"));
        int iCharDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "charDist", 
                "5"));
        int iMinWord = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "minWord", 
                "1"));
        int iMaxWord = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "maxWord", 
                "5"));
        int iWordDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "wordDist", 
                "5"));
        int iCategoryLimit = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hOptions, "categoryLimit", 
                "-1"));
        iCategoryLimit = (iCategoryLimit <= 0) ? Integer.MAX_VALUE : iCategoryLimit;
        
        // Init document model
        DocumentSet dsModel = 
                new DocumentSet(sCorpusDir, 1.0);
        dsModel.createSets(bFlatCorpusDir);
        
        
        // For every topic (i.e. category)
        Iterator iCatIter = dsModel.getCategories().iterator();
        
        // Init result distribution per peer
        TreeMap<String,Distribution> tmResultsPerPeer = new TreeMap<String,Distribution>(); 
        // Init result distribution per model
        TreeMap<String,Distribution> tmResultsPerModel = new TreeMap<String,Distribution>(); 
        
        // Output header line
        System.out.println("SystemID\tSystemType\tTopicID\tNormality\tCharNormality\tWordNormality");
        
        int iIterCnt = 0;
        grammaticalityEstimator g = null; // Init estimator variable
        
        while (iCatIter.hasNext() && (iIterCnt++ < iCategoryLimit)) {
            String sCurrentCategory = (String)iCatIter.next();
            // Get file names of category files
            Iterator iIter = dsModel.getFilesFromCategory(sCurrentCategory, 
                    dsModel.FROM_TRAINING_SET).iterator();
            HashSet alDocFileNames = new HashSet();
            while (iIter.hasNext()) {
                alDocFileNames.add(((CategorizedFileEntry)iIter.next()).getFileName());
            }
        
            String sIterModelFile = sModelFile;
            if (bPerCategoryModel)
                sIterModelFile += String.valueOf(iIterCnt);
            
            // Initialize model of grammaticality estimator
            if ((g == null) || bPerCategoryModel) {
                System.err.println("Loading corpus... (Iteration " + (iIterCnt) + ")");
                boolean bLoaded = false;
                if (sModelFile.length() > 0)
                {
                    if (new File(sIterModelFile).exists())
                    // Try to load file
                    try {
                        System.err.println("Loading from model file (" + sIterModelFile + ")");
                        // Use compression
                        g = grammaticalityEstimator.loadFromStream(
                                new GZIPInputStream(new FileInputStream(sIterModelFile)));
                        bLoaded = g!=null;

                    } catch (FileNotFoundException ex) {
                        System.err.println("Failed to load model.");
                        ex.printStackTrace(System.err);
                        g = null; // Failed to load.
                    } catch (IOException ioe) {
                        System.err.println("Failed to load model, due to decompression problem.");
                        ioe.printStackTrace(System.err);
                        g = null; // Failed to load.                        
                    } catch (Exception e) {
                        System.err.println("Failed to load model, due to problem.");
                        e.printStackTrace(System.err);
                        g = null; // Failed to load.                        
                    }
                    else
                        System.err.println("Model file not found. Using file name for model output.");
                }

                // Initialize grammaticality estimator, if not already loaded.
                if (g == null) {
                    g = new grammaticalityEstimator(alDocFileNames, iMinChar,
                        iMaxChar, iMinWord, iMaxWord, Math.max(iMaxChar, iMaxWord));
                    System.err.println("Training on corpus...");
                    // Call the garbage collector EXPLICITLY
                    System.gc();
                    // Train the estimator
                    g.train();
                }

                // Save if needed
                if ((!bLoaded) && (g != null)) {
                    System.err.println("Saving to model file (" + sIterModelFile + ")");
                    FileOutputStream fsModelOut;
                    try {
                        fsModelOut = new FileOutputStream(sIterModelFile);
                        GZIPOutputStream gosTmp = new GZIPOutputStream(fsModelOut, 16384);
                        g.saveToStream(gosTmp);
                        gosTmp.finish();
                        fsModelOut.close();
                    } catch (FileNotFoundException ex) {
                        System.err.println("Could not save to file " + sIterModelFile);
                        ex.printStackTrace(System.err);
                    } catch (IOException ex) {
                        System.err.println("Could not save to file " + sIterModelFile);
                        ex.printStackTrace(System.err);
                    }
                }
            }

            // Check on peers
            System.err.println("\nAnalysing peers...");
            DocumentSet dsPeers = null;
            if (bPerCategoryModel) {
                dsPeers = new DocumentSet(sPeerDir + sCurrentCategory.substring(0, sCurrentCategory.length() - 1),
                    1.0);
                // No categories
                dsPeers.createSets(true);
            }
            else {
                dsPeers = new DocumentSet(sPeerDir, 1.0);
                // Categories as normal
                dsPeers.createSets(false);
            }
            
            System.err.println("Found " + String.valueOf(dsPeers.getTrainingSet().size()) + " peer documents...");
            Iterator iDocs = dsPeers.getTrainingSet().iterator();
            while (iDocs.hasNext()) {
                StringBuffer sbCurLine = new StringBuffer();
                CategorizedFileEntry cfeCur = (CategorizedFileEntry)iDocs.next();
                String sFilename = cfeCur.getFileName();
                String sText = gr.demokritos.iit.jinsect.utils.loadFileToString(sFilename);
                double dNorm = g.getNormality(sText);
                double dCharNorm = g.getCharNormality(sText);
                double dWordNorm = g.getWordNormality(sText);
                // Get doc info
                DUCDocumentInfo ddiCur = new DUCDocumentInfo(sFilename);
                sbCurLine.append(ddiCur.Summarizer + "\t");
                //System.out.println(sFilename + " \t" + dNorm);
                sbCurLine.append("Peer\t");
                sbCurLine.append(ddiCur.Topic + "\t");
                sbCurLine.append(dNorm + "\t");
                sbCurLine.append(dCharNorm + "\t");
                sbCurLine.append(dWordNorm);
                
                Distribution dCur = null;
                // If distribution of results for selected summarizer exists
                if (tmResultsPerPeer.containsKey(ddiCur.Summarizer))
                    // Use it
                    dCur = tmResultsPerPeer.get(ddiCur.Summarizer);
                else
                {
                    // Else create a new one
                    dCur = new Distribution();
                    tmResultsPerPeer.put(ddiCur.Summarizer, dCur);
                }
                dCur.setValue(dCur.asTreeMap().size(), dNorm);
                // Output line
                System.out.println(sbCurLine.toString());
            }

            // Check on models
            System.err.println("\nAnalysing models...");
            DocumentSet dsModels = new DocumentSet(sModelDir  + sCurrentCategory.substring(0, sCurrentCategory.length() - 1), 1.0);
            dsModels.createSets(true);
            System.err.println("Found " + String.valueOf(dsModels.getTrainingSet().size()) + " model documents...");
            iDocs = dsModels.getTrainingSet().iterator();
            while (iDocs.hasNext()) {
                StringBuffer sbCurLine = new StringBuffer();
                CategorizedFileEntry cfeCur = (CategorizedFileEntry)iDocs.next();
                String sFilename = cfeCur.getFileName();
                String sText = gr.demokritos.iit.jinsect.utils.loadFileToString(sFilename);
                double dNorm = g.getNormality(sText);
                double dCharNorm = g.getCharNormality(sText);
                double dWordNorm = g.getWordNormality(sText);
                // Get doc info
                DUCDocumentInfo ddiCur = new DUCDocumentInfo(sFilename);
                sbCurLine.append(ddiCur.Summarizer + "\t");
                //System.out.println(sFilename + " \t" + dNorm);
                sbCurLine.append("Model\t");
                sbCurLine.append(ddiCur.Topic + "\t");
                sbCurLine.append(dNorm + "\t");
                sbCurLine.append(dCharNorm + "\t");
                sbCurLine.append(dWordNorm + "\t");
                //System.out.println(sFilename + " :\t" + dNorm);
                Distribution dCur = null;
                // If distribution of results for selected summarized exists
                if (tmResultsPerModel.containsKey(ddiCur.Summarizer))
                    // Use it
                    dCur = tmResultsPerModel.get(ddiCur.Summarizer);
                else
                {
                    // Else create a new one
                    dCur = new Distribution();
                    tmResultsPerModel.put(ddiCur.Summarizer, dCur);
                }
                dCur.setValue(dCur.asTreeMap().size(), dNorm);
                
                // Output line
                System.out.println(sbCurLine.toString());
            }
        }

        
        System.err.println("OVERALL RESULTS:\n");
        for (Iterator<String> it = tmResultsPerPeer.keySet().iterator(); it.hasNext();) {
            String sPeer = it.next();
            System.err.println(sPeer + ": " + tmResultsPerPeer.get(sPeer).average(true) + 
                    " (" + tmResultsPerPeer.get(sPeer).standardDeviation(true) + ")");
        }
    }

    public TreeMap<Integer, DistributionDocument> getDistroDocs() {
        return DistroDocs;
    }
    
}