/*
 * summaryFuzzyEvaluator.java
 *
 * Created on 27 Φεβρουάριος 2007, 4:44 μμ
 *
 */

package gr.demokritos.iit.jinsect.console;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.events.TextSpectralSpellPreprocessor;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;


/** A class that performs summary evaluation like the {@link summaryEvaluator} super-class, but
 * with fuzzy string matching between n-grams of different texts. Uses SpectralSpell application
 * for fuzziness matching of words.
 *
 * @author ggianna
 */
public class summaryFuzzyEvaluator extends summaryEvaluator {
    protected String sspellParams; // Spectral Spell params
    
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
     *@param sSspellParams Custom parameters to pass to SpectralSpell.
     */
    public summaryFuzzyEvaluator(Semaphore sOutputSemaphore, String sDo,
            int iWordMin, int iWordMax, int iWordDist, int iCharMin, int iCharMax, int iCharDist, int iThreads,
        String sOutFile, String sSummaryDir, String sModelDir, boolean bSilent, int iWeightMethod,
            boolean bProgress, String sSspellParams)
    {
        super(sOutputSemaphore, sDo,
                iWordMin, iWordMax, iWordDist, iCharMin, iCharMax, iCharDist, 
                iThreads, sOutFile, 
                sSummaryDir, sModelDir, bSilent, iWeightMethod, bProgress);
        
        sspellParams = sSspellParams;
    }

    public summaryFuzzyEvaluator(String[] args) {
        super(args);
        
        // Get SpectralSpell params
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        String sSspellParams = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"ssParams","");
        sspellParams = sSspellParams;
    }
    
    private static void printUsage() {
            System.err.println("Syntax:\nsummaryFuzzyEvaluator -do=(char|word|all) [-summaryDir=summaries/] [-modelDir=models/]"+
                    "[-wordMin=#] [-wordMax=#] [-wordDist=#] [-charMin=#] [-charMax=#] [-charDist=#] [-o=outFile] [-t=#]" + 
                    "[-s] [-use=o|d] [-progress]");
            System.err.println("wordMin=#\tMin word n-gram size.\nwordMax=#\tMax word n-gram size.\nwordDist=#\tWord n-gram window" +
                    "charMin=#\tMin char n-gram size.\ncharMax=#\tMax char n-gram size.\ncharDist=#\tChar n-gram window.\n" +
                    "-o=outFile\tThe file to output data. Default is stdout.\n-t=#\tNumber of threads. Defaults to 2.\n" +
                    "-s\tFor no progress report (silent).\n" +
                    "-progress\tFor progress indication (even in silent mode).\n" +
                    "-use=o|d\tUse [o]ccurences or average [d]istance to assign weights to the graph. Defaults to o.\n" +
                    "-ssParams=\"string\"\tThe parameters for spectral spell.");
    }

    protected SimilarityArray calcSimilarityMeasures(CategorizedFileEntry cfeCur, List dsModelSet, boolean bOutput, PrintStream pOut, Semaphore sSem, int WordNGramSize_Min, int WordNGramSize_Max, int Word_Dmax, int CharacterNGramSize_Min, int CharacterNGramSize_Max, int Character_Dmax, boolean bDoCharNGrams, boolean bDoWordNGrams, boolean bSilent) {
        // Init preprocessor
        TextSpectralSpellPreprocessor tSpectral = new TextSpectralSpellPreprocessor(sspellParams);
        initSpectralPreprocessor(cfeCur, dsModelSet, tSpectral);
        
        if (pOut == null)
            pOut = System.out;
        
        // Init return struct
        SimilarityArray saRes = new SimilarityArray();
        
        // Read first file
        SimpleTextDocument ndDoc1 = new SimpleTextDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
        // Set preprocessor
        ndDoc1.getDocumentGraph().TextPreprocessor = tSpectral;
        ndDoc1.getDocumentHistogram().TextPreprocessor = tSpectral;
        NGramDocument ndNDoc1 = new NGramDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, CharacterNGramSize_Min, CharacterNGramSize_Max);
        // Set preprocessor
        ndNDoc1.getDocumentGraph().TextPreprocessor = tSpectral;
        ndNDoc1.getDocumentHistogram().TextPreprocessor = tSpectral;
        
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
            NGramDocument ndNDoc2 = null;
                    
            if (bDoWordNGrams)
            {
                synchronized (hModelCache) {
                    // Look up cache
                    if (hModelCache.containsKey(cfeOther.getFileName()))
                        ndDoc2 = (SimpleTextDocument)hModelCache.get(cfeOther.getFileName());
                    else {
                        ndDoc2 = new SimpleTextDocument(WordNGramSize_Min, WordNGramSize_Max, Word_Dmax);
                        // Set preprocessor
                        ndDoc2.getDocumentGraph().TextPreprocessor = tSpectral;
                        ndDoc2.getDocumentHistogram().TextPreprocessor = tSpectral;
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
                        ndNDoc2 = (NGramDocument)hNModelCache.get(cfeOther.getFileName());
                    else {
                        ndNDoc2 = new NGramDocument(CharacterNGramSize_Min, CharacterNGramSize_Max, Character_Dmax, 
                                  CharacterNGramSize_Min, CharacterNGramSize_Max);
                        // Set preprocessor
                        ndNDoc2.getDocumentGraph().TextPreprocessor = tSpectral;
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
    
    /*
     *Simply loads a file into a string
     *@param sFilename The filename of the source file.
     */
    private String loadFile(String sFilename) {
         try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(sFilename);
            int iData = 0;
            while ((iData = fiIn.read()) > -1)
                bsOut.write(iData);
            String sDataString = bsOut.toString();
            fiIn.close();
            return sDataString; // Actually update temp datastring
        }
        catch (IOException e) {
            return "";
        }
       
    }
    
    private void initSpectralPreprocessor(CategorizedFileEntry cfeCur, List dsModelSet,
            TextSpectralSpellPreprocessor tSpectral) {
        // Add Summary
        tSpectral.addDocument(loadFile(cfeCur.getFileName()));
        
        // Add models
        Iterator iOtherIter = dsModelSet.iterator();
        while (iOtherIter.hasNext()) {
            CategorizedFileEntry cfeModel = (CategorizedFileEntry)iOtherIter.next();
            tSpectral.addDocument(loadFile(cfeModel.getFileName()));
        }
    }
    
    public static void main(String[] args) {
        // doRougeLikeEval();
        summaryFuzzyEvaluator sCur = new summaryFuzzyEvaluator(args);
        sCur.run();
    }
    
    
}