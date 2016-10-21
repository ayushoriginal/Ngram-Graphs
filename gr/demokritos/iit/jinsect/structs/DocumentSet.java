/*
 * DocumentSet.java
 *
 * Created on 28 ???????????????????? 2006, 7:21 ????
 *
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.util.Set;

/** A set of documents, that can be split to training and test sets.
 *
 * @author PCKid
 */
public class DocumentSet implements IDocumentSet {
    protected double TrainingPercent;
    protected String BaseDir;
    protected ArrayList TrainingFiles;
    protected ArrayList TestFiles;
    protected ArrayList Categories;
    
    /** Constant indicating tirage from the training set.
     */
    public static final int FROM_TRAINING_SET = 1;
    /** Constant indicating tirage from the test set.
     */
    public static final int FROM_TEST_SET = 2;
    /** Constant indicating tirage from the whole (training plus test) set.
     */
    public static final int FROM_WHOLE_SET = 3;
    
    /** An evaluator of files to add to this document set. If null, no criteria are applied.
     */
    public FileFilter FileEvaluator = null;
       
    /**
     * Creates a new instance of DocumentSet with a training set portion.
     * 
     * @param sBaseDir The root of the corpus directory. Each document is supposed
     * to be contained in a subdir of this dir, corresponding to the name of its
     * category.
     * @param dTrainingPercent Percent of trainining set as part of the whole document
     * set.
     */
    public DocumentSet(String sBaseDir, double dTrainingPercent) {
        TrainingPercent = dTrainingPercent;
        BaseDir = sBaseDir;
        
        TrainingFiles = new ArrayList();
        TestFiles = new ArrayList();
        Categories = new ArrayList();
    }
    
    /** Returns a list of the categories appearing in the document set.
     *@return The list of categories.
     */
    public List getCategories() {
        return Categories;
    }
   
    /** Initializes the document sets with all files of the base directory subtree used.
     */
    public void createSets() {
        createSets(true, 1.0, false);
    }
    
    /** Initializes the document sets with all files of the base directory subtree used.
     *@param bNoCategories Indicates whether there are no subcategories to take 
     * into account. If so, a flat directory full of files is expected.
     */
    public void createSets(boolean bNoCategories) {
        createSets(true, 1.0, bNoCategories);
    }
    
    /** Initializes the document sets using a portion of the files of the base directory subtree,
     *either in a stratified or not stratified manner. Assumes non-flat structure.
     *@param bEvenly Attempt stratification of instances.
     *@param dPartOfTheCorpus Percentage of the corpus to use. Values should be between 0.0 and 1.0.
     */
    public void createSets(boolean bEvenly, double dPartOfTheCorpus) {
        createSets(bEvenly, dPartOfTheCorpus, false);
    }
    
    /** Initializes the document sets using a portion of the files of the base directory subtree,
     *either in a stratified or not stratified manner.
     *@param bEvenly Attempt stratification of instances.
     *@param dPartOfTheCorpus Percentage of the corpus to use. Values should be between 0.0 and 1.0.
     *@param bNoCategories Indicates whether there are no subcategories to take 
     * into account. If so, a flat directory full of files is expected.
     */
    public void createSets(boolean bEvenly, double dPartOfTheCorpus, boolean bNoCategories) {
        // Init category list
        Categories.clear();
        // Get category list
        File sBaseDir = new File(BaseDir);
        // DEBUG LINES
        // System.out.println("Examining directory " + sBaseDir.getAbsolutePath());
        //////////////
        if (bNoCategories)
            Categories.add("."); // Only current dir
        else
            try {
                String[] sDirs = sBaseDir.list();
                List lDirs = Arrays.asList(sDirs);
                Iterator iIter = lDirs.iterator();
                // Run through files to find dirs (categories)
                while (iIter.hasNext())
                {
                    String sCurCategory = (String)iIter.next();
                    String sCurFile = BaseDir +  System.getProperty("file.separator") + sCurCategory;
                    // DEBUG LINES
                    // System.out.println("Examining directory entry " + sCurFile);
                    //////////////
                    File f = new File(sCurFile);
                    if (f.isDirectory())
                    {
                        // Add as category
                        Categories.add(sCurCategory);
                        // DEBUG LINES
                        // System.out.println("Added category " + sCurCategory);
                        //////////////
                    }                
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
        
        // If no category is found
        if (Categories.size() == 0)
            return;
        Iterator iIter = Categories.iterator();        
        // For all categories
        while (iIter.hasNext())
        {
            String sCategory = (String)iIter.next();
            File fCatDir = new File(BaseDir + System.getProperty("file.separator") + sCategory);
            
            // Get file list
            File [] sFileList = fCatDir.listFiles(new FileFilter() {
                public boolean accept(File inputFile) {
                    return !(inputFile.isDirectory());
                }
            });
            if (sFileList == null) {
                // No files in category
                sFileList = new File[0];
                // DEBUG LINES
                // System.out.println("No files in category " + sCategory);
                //////////////
            }
            // Keep only some files based on (dPartOfTheCorpus)
            int iAllFiles = (int)((double)sFileList.length * dPartOfTheCorpus);
            // DEBUG LINES
            //System.err.println("Using " + String.valueOf(iAllFiles) + " out of " + 
                    //String.valueOf(sFileList.length));
            //////////////
            int iInTrainingSet = (int)(iAllFiles * TrainingPercent);
            //if (iInTrainingSet == 0)
            //    iInTrainingSet = 1; // At least one trainer
            int iRemainingFiles = iAllFiles;
            // Category train and test sets
            ArrayList lTempTrain = new ArrayList(iInTrainingSet);
            ArrayList lTempTest = new ArrayList(iAllFiles - iInTrainingSet);
            
            // Randomize file selection if part of corpus selected
            List lFileList = Arrays.asList(sFileList);
            LinkedList lList = new LinkedList(lFileList);
            Iterator iFile = lList.listIterator();
            while (iFile.hasNext()) {
                File fCurFile = (File)iFile.next();
                if (FileEvaluator != null) {
                    if (!FileEvaluator.accept(fCurFile))
                        iFile.remove();
                }
            }
            
            if (dPartOfTheCorpus < 1.0)
            {
                // DEBUG LINES
                // System.out.println(lList.toString());
                gr.demokritos.iit.jinsect.utils.shuffleList(lList);
                // System.out.println(lList.toString());
            }

            Iterator iFileIter = lList.iterator();
            // For every file
            Random r = new Random(); // Init random generator
            while (iFileIter.hasNext() && (iRemainingFiles > 0)) {
                String sCurFile = ((File)iFileIter.next()).getAbsolutePath();
                // DEBUG LINES
                // System.out.println("Evaluating file " + sCurFile);
                //////////////
                
                // If 
                // - randomly the file should be added and the training set is 
                // not full already or
                // - the files remaining are not adequate to complete the training
                // set
                if (((r.nextDouble() < TrainingPercent) && (iInTrainingSet > lTempTrain.size()))
                    || (iRemainingFiles + lTempTrain.size() <= iInTrainingSet))
                    // Then add the file to the training set
                    lTempTrain.add(new CategorizedFileEntry(sCurFile, sCategory));
                else
                    // else add it to the test set.
                    lTempTest.add(new CategorizedFileEntry(sCurFile, sCategory));
                iRemainingFiles--;
            }
            // Append to actual sets
            TestFiles.addAll(lTempTest);
            TrainingFiles.addAll(lTempTrain);
        }
        
        if (!bEvenly)
            shuffleTestAndTrainingSetTogether();
    }
    
    /** Shuffles (randomizes the order of) the files appearing in the training set.
     */
    public void shuffleTrainingSet() {
        utils.shuffleList(TrainingFiles);
    }
    
    /** Shuffles (randomizes the order of) the files appearing in the test set.
     */
    public void shuffleTestSet() {
        utils.shuffleList(TestFiles);
    }
    
    /** Creates a list containing shuffled test and training instances and then recreates the
     * training and test sets based on this list.
     */
    protected void shuffleTestAndTrainingSetTogether() {
        // Create overall list
        ArrayList lOverall = new ArrayList(TestFiles.size() + TrainingFiles.size());
        lOverall.addAll(TrainingFiles);
        lOverall.addAll(TestFiles);
        
        utils.shuffleList(lOverall);
        // Reassign sets
        TestFiles.clear();
        TrainingFiles.clear();
        TestFiles.addAll(lOverall.subList(0, TestFiles.size()));
        TrainingFiles.addAll(lOverall.subList(TestFiles.size(), lOverall.size()));
    }
    
    /**Returns the training set of this document set.
     *@return The training set as an {@link ArrayList}.
     */
    public ArrayList getTrainingSet() {
        return TrainingFiles;
    }
    
    /**Returns the test set of this document set.
     *@return The test set as an {@link ArrayList}.
     */
    public ArrayList getTestSet() {
        return TestFiles;
    }
    
    
    /**Returns the training and test set files that belong to a given category.
     *@return The set of files.
     *@see ArrayList
     */
    public ArrayList getFilesFromCategory(String sCategoryName) {
        return getFilesFromCategory(sCategoryName, FROM_WHOLE_SET);
    }
    
    /**Returns files either contained to the training and/or test set and belong to a given category.
     *@param sCategoryName The name of the category of interest.
     *@param iFromWhichSet A value indicating from which subset the files should be drawn:
     *<ol>
     * <li>FROM_TRAINING_SET indicates the training set as the source.
     * <li>FROM_TEST_SET indicates the testing set as the source.
     * <li>FROM_WHOLE_SET indicates the whole document set as the source.
     *</ol>
     *@return The set of files that match the conditions set by the parameters.
     *@see ArrayList
     */
    public ArrayList getFilesFromCategory(
            String sCategoryName, int iFromWhichSet) {
        ArrayList alRes = new ArrayList();
        Iterator iIter = TrainingFiles.iterator();
        if ((iFromWhichSet & FROM_TRAINING_SET) > 0)
            while (iIter.hasNext()) {
                CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
                if (cfeCur.getCategory().equals(sCategoryName)) {
                    alRes.add(cfeCur);
                }
            }

        iIter = TestFiles.iterator();
        if ((iFromWhichSet & FROM_TEST_SET) > 0)
            while (iIter.hasNext()) {
                CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
                if (cfeCur.getCategory().equals(sCategoryName)) {
                    alRes.add(cfeCur);
                }
            }
        
        return alRes;
    }
    
    /** Get a string list of all file names in the set or its training / test 
     * subsets. 
     *@param iSubset A value of either FROM_TRAINING_SET, FROM_TEST_SET, 
     * FROM_WHOLE_SET indicating the subset used to extract filenames.
     *@return A {@link Set} of strings, that are the filenames of the files in the
     *set.
     */
    public Set<String> toFilenameSet(int iSubset) {
        // Init set
        HashSet s = new HashSet();
        if ((iSubset & FROM_TRAINING_SET) > 0)
            // For all training files
            for (Object elem : getTrainingSet()) {
                CategorizedFileEntry cfeCur = (CategorizedFileEntry)elem;
                // Get name
                s.add(cfeCur.getFileName());
            }
        
        if ((iSubset & FROM_TEST_SET) > 0)
        // For all test files
        for (Object elem : getTestSet()) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)elem;
            // Get name
            s.add(cfeCur.getFileName());
        }
        
        return s;
    }

    public static List<String> categorizedFileEntriesToStrings(List<CategorizedFileEntry>
            lEntries) {
        ArrayList<String> lRes = new ArrayList<String>(lEntries.size());
        for (CategorizedFileEntry cfe : lEntries) {
            lRes.add(cfe.getFileName());
        }
        
        return lRes;
    }
}

