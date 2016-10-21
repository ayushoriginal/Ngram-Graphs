/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.tacTools;

import gr.demokritos.iit.jinsect.storage.IFileLoader;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.IDocumentSet;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** A class that takes a TAC2008 topic structure directory and can return groupA
 * or groupB documents given an xml topic file and a topic ID. The directory 
 * structure contains a top directory for every topic ID. Each topic ID 
 * directory in turns contains two directories, one for each group of documents.
 *
 * @author ggianna
 */
public class TAC2008UpdateSummarizationFileSet implements IDocumentSet, IFileLoader<String> {
    /** Constant indicating tirage from the training set.
     */
    public static final int FROM_TRAINING_SET = 1;
    /** Constant indicating tirage from the test set.
     */
    public static final int FROM_TEST_SET = 2;
    /** Constant indicating tirage from the whole (training plus test) set.
     */
    public static final int FROM_WHOLE_SET = 3;
        
    /** The top directory of the TAC2008 topic structure */
    protected String CorpusDir;
    
    /** The set of category (topic ID) names. */
    protected HashSet<String> Categories;
    
    /** The list of training files (actually group A files).*/
    protected ArrayList<CategorizedFileEntry> TrainingFiles;
    /** The list of test files (actually group B files).*/
    protected ArrayList<CategorizedFileEntry> TestFiles;
    
    /** Initialize the set using as corpus dir a given dir.
     * 
     * @param sCorpusDir The top directory of the corpus structure.
     */
    public TAC2008UpdateSummarizationFileSet(String sCorpusDir) {
        CorpusDir = sCorpusDir;
        Categories = null;
        TrainingFiles = new ArrayList<CategorizedFileEntry>();
        TestFiles = new ArrayList<CategorizedFileEntry>();
    }

    /** Returns a list of the topic IDs, as category names.
     * 
     * @return A {@link String} {@link List} containing category names, which
     * actually correspond to topic IDs.
     */
    public List getCategories() {
        if (Categories != null)
            return new ArrayList(Categories);
        // Init category list
        Categories = new HashSet<String>();
        // Get category list
        File sBaseDir = new File(CorpusDir);
        // DEBUG LINES
        // System.out.println("Examining directory " + sBaseDir.getAbsolutePath());
        //////////////
        try {
            String[] sDirs = sBaseDir.list();
            List lDirs = Arrays.asList(sDirs);
            Iterator<String> iIter = lDirs.iterator();
            // Run through files to find dirs (categories)
            while (iIter.hasNext())
            {
                String sCurCategory = iIter.next();
                String sCurFile = CorpusDir +  
                        System.getProperty("file.separator") + sCurCategory;
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
            return null;
        }
        
        return new ArrayList(Categories);
    }

    public void createSets() {
        // Init categories
        List<String> lCats = getCategories();
        // If no categories, return
        if (lCats.size() == 0)
            return;
        
        // For each category
        Iterator<String> iCats = lCats.iterator();
        while (iCats.hasNext()) {
            String sCurCategory = iCats.next();
            
            // Training files
            List<File> lfFiles = Arrays.asList(new File(CorpusDir +  
                    System.getProperty("file.separator") + sCurCategory +
                    System.getProperty("file.separator") + sCurCategory + "-A"
                    ).listFiles());
            for (File fFile : lfFiles)
            {
                TrainingFiles.add(new CategorizedFileEntry(
                        fFile.getAbsolutePath(), sCurCategory));
            }
            
            // Test files
            lfFiles = Arrays.asList(new File(CorpusDir +  
                    System.getProperty("file.separator") + sCurCategory +
                    System.getProperty("file.separator") + sCurCategory + "-B"
                    ).listFiles());
            for (File fFile : lfFiles)
            {
                TestFiles.add(new CategorizedFileEntry(
                        fFile.getAbsolutePath(), sCurCategory));
            }            
        }
    }

    public final ArrayList getFilesFromCategory(String sCategoryName) {
        ArrayList<CategorizedFileEntry> alRes = new 
                ArrayList<CategorizedFileEntry>();
        // Add all corresponding training files to result list
        Iterator<CategorizedFileEntry> iFiles = TrainingFiles.iterator();
        while (iFiles.hasNext()) {
            CategorizedFileEntry cfeCur = iFiles.next();
            if (cfeCur.getCategory().equals(sCategoryName))
                alRes.add(cfeCur);
        }
        
        // Add all corresponding test files to result list
        iFiles = TestFiles.iterator();
        while (iFiles.hasNext()) {
            CategorizedFileEntry cfeCur = iFiles.next();
            if (cfeCur.getCategory().equals(sCategoryName))
                alRes.add(cfeCur);
        }
        
        return alRes;
    }

    /**
     * Returns all files belonging to a specified category, and belonging to 
     * a specified subset of the set.
     * @param sCategoryName The name of the category the files should belong to.
     * @param iFromWhatPart One of the FROM_TRAINING_SET, FROM_TEST_SET,
     *  FROM_WHOLE_SET values to indicate which subset should be used.
     * @return A list of filenames corresponding to the selected files.
     */
    public final List<String> getFilenamesFromCategory(String sCategoryName, int 
            iFromWhatPart) {
        ArrayList<String> lsRes = new 
                ArrayList<String>();
        // Add all corresponding training files to result list
        Iterator<CategorizedFileEntry> iFiles = TrainingFiles.iterator();
        if ((iFromWhatPart & FROM_TRAINING_SET) != 0) {
            while (iFiles.hasNext()) {
                CategorizedFileEntry cfeCur = iFiles.next();
                if (cfeCur.getCategory().equals(sCategoryName))
                    lsRes.add(cfeCur.getFileName());
            }
        }
        
        // Add all corresponding test files to result list
        if ((iFromWhatPart & FROM_TEST_SET) != 0) {
            iFiles = TestFiles.iterator();
            while (iFiles.hasNext()) {
                CategorizedFileEntry cfeCur = iFiles.next();
                if (cfeCur.getCategory().equals(sCategoryName))
                    lsRes.add(cfeCur.getFileName());
            }
        }
        
        return lsRes;
    }

    /** Returns group A files, described as training set. */
    public final ArrayList getTrainingSet() {
        return TrainingFiles;
    }

    /** Returns group B files, described as training set. */
    public final ArrayList getTestSet() {
        return TestFiles;
    }

    /** Loads the text of a given file, given its filename.
     * 
     * @param sID The filename of the file to load.
     * @return A {@link String} representing the title and text of the given 
     * file.
     */
    public String loadFile(String sID) {
        return getDocumentText(sID, true);
    }
    
    /** Used for testing purposes only.
     * 
     * @param sArgs Unused.
     */
    public static void main(String[] sArgs) {
        TAC2008UpdateSummarizationFileSet t = new TAC2008UpdateSummarizationFileSet(
                "/home/ggianna/JInsect/TAC2008/UpdateSumm08_test_docs_files/");
        
        t.createSets();
        System.err.println("Training set:");
        System.err.println(utils.printIterable(t.getTrainingSet(), "\n"));
        System.err.println("Training set:");
        System.err.println(utils.printIterable(t.getTestSet(), "\n"));
        
        System.err.println("Per category:");
        for (String sCategory : (List<String>)t.getCategories()) {
            System.err.println(utils.printIterable(t.getFilesFromCategory(
                    sCategory), "\n"));
        }
        
        String sLastCat = (String)t.getCategories().get(
                t.getCategories().size() - 1);
        String sFile = ((CategorizedFileEntry)t.getFilesFromCategory(
                sLastCat).get(0)).getFileName();
        
        System.out.println(t.loadFile(sFile));
    }

    /** Returns a given element of a given document as a String, if the
     * element exists.
     *@param sDocID The document ID.
     *@param bIncludeTitle If true, title is leading the return text. Otherwise,
     *  it is omitted.
     *@return Null if document is not found, a zero length String if the specified
     * element was not found, otherwise the document's element text.
     */
    protected final String getDocumentText(String sDocID, boolean bIncludeTitle) {
        // Init XML doc file
        Document XMLDoc = null;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            XMLDoc = docBuilder.parse(new File(sDocID));
        } catch (ParserConfigurationException ex) {
            System.err.println("Invalid XML file. Details:");
            ex.printStackTrace(System.err);
        } catch (SAXException ex) {
            System.err.println("Invalid XML file. Details:");
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println("Could not read XML file. Cause:");
            ex.printStackTrace(System.err);
        }
        if (XMLDoc == null)
            return null;

        String sRes = "";
        // Get text
        Node nDoc = XMLDoc.getElementsByTagName("TEXT").item(0);
        if (nDoc != null)
            sRes = nDoc.getTextContent();
        
        if (bIncludeTitle) {
            // Get title
            nDoc = XMLDoc.getElementsByTagName("HEADLINE").item(0);
            if (nDoc != null) {        
                sRes = nDoc.getTextContent() + sRes;
            }
        }
        
        return sRes;
        
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
}
