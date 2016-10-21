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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Uses an TAC 2008 XML topic definition file to create the set.
 *
 * @author ggianna
 */
public class TAC2008TopicFileSet implements IDocumentSet, 
        IFileLoader<String> {
    /** Topic tag string in XML file. */
    protected static String TOPIC_TAG = "topic";
    /** Title tag string in XML file. */
    protected static String TITLE_TAG = "title";
    /** Docset A tag string in XML file. */
    protected static String DOCSET_A_TAG = "docsetA";
    /** Docset B tag string in XML file. */
    protected static String DOCSET_B_TAG = "docsetB";
    /** Docset B tag string in XML file. */
    protected static String NARRATIVE_TAG = "narrative";
    /** Document tag string in XML file. */
    protected static String DOCUMENT_TAG = "doc";
    
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
    /** The XML topic file filename */
    private String TopicFile;
    /** The underlying XML document itself. */
    Document TopicXMLDoc;
    
    /** Initializes the document set, given a TAC2008 topic XML file.
     * 
     * @param sXMLFile The filename of the topic file.
     * @param sCorpusRootDir The base directory of the TAC2008 test corpus 
     * directory structure.
     */
    public TAC2008TopicFileSet(String sTopicXMLFile, String sCorpusRootDir) 
            throws ParserConfigurationException, SAXException, IOException {
        TopicFile = sTopicXMLFile;
        CorpusDir = sCorpusRootDir;
        // Init XML doc file
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        TopicXMLDoc = docBuilder.parse(new File(sTopicXMLFile));
        
        Categories = null;
        TrainingFiles = null;
        TestFiles = null;
    }
    
    @Override
    public void createSets() {
        // Init categories
        List<String> lCats = getCategories();
        // If no categories, return
        if (lCats.size() == 0)
            return;
        
        // Init sets
        TrainingFiles = new ArrayList<CategorizedFileEntry>();
        TestFiles = new ArrayList<CategorizedFileEntry>();
        // For each category
        Iterator<String> iCats = lCats.iterator();
        while (iCats.hasNext()) {
            String sCurCategory = iCats.next();
            
            // Get topic node
            Node nTopic = getTopicNode(sCurCategory);
            // If not found, then alert and continue.
            if (nTopic == null) {
                System.err.println("Could not find topic " + sCurCategory);
                continue;
            }
            
            
            // Training files
            List<String> lsFiles = getFilesFromTopic(sCurCategory, 
                    FROM_TRAINING_SET);
            for (String sFile : lsFiles)
            {
                // Attempt to find file
                File fFile = new File(CorpusDir + sFile);
                if (!fFile.exists()) {
                    System.err.println("File " + fFile.toString() + 
                            " not found.");
                    continue;
                }
                // If found add entry
                TrainingFiles.add(new CategorizedFileEntry(
                        fFile.getAbsolutePath(), sCurCategory));
            }
            
            // Test files
            lsFiles = getFilesFromTopic(sCurCategory, 
                    FROM_TEST_SET);
            for (String sFile : lsFiles)
            {
                // Attempt to find file
                File fFile = new File(CorpusDir + sFile);
                if (!fFile.exists()) {
                    System.err.println("File " + fFile.toString() + 
                            " not found.");
                    continue;
                }
                // If found add entry
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
                sRes = nDoc.getTextContent() + ". " + sRes;
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

    /** Actually returns the list of topics from the file. */
    public List getCategories() {
        ArrayList alRes = new ArrayList<String>();
        // Get topic nodes
        NodeList nlTopics = TopicXMLDoc.getElementsByTagName(TOPIC_TAG);
        // Return if failed
        if (nlTopics == null)
            return alRes;
        
        // For every topic node
        for (int iNodeCnt = 0; iNodeCnt < nlTopics.getLength(); iNodeCnt++) {
            Node nCur = nlTopics.item(iNodeCnt);
            // Add ID attribute value as topic name
            // alRes.add(getAttribute(nCur, "id"));
             alRes.add(nCur.getAttributes().getNamedItem("id").getTextContent());
        }
        return alRes;
    }
    
    /** Return the node of a given topic in the XML document.
     * 
     * @param sTopicID The topic ID of interest.
     * @return Null if the topic was not found, else the corresponding node.
     */
    protected final Node getTopicNode(String sTopicID) {
        ArrayList alRes = new ArrayList<String>();
        // Get topic nodes
        NodeList nlTopics = TopicXMLDoc.getElementsByTagName(TOPIC_TAG);
        // Return if failed
        if (nlTopics == null)
            return null;
        
        // For every topic node
        for (int iNodeCnt = 0; iNodeCnt < nlTopics.getLength(); iNodeCnt++) {
            Node nCur = nlTopics.item(iNodeCnt);
            // Check whether we have reached the topic
            if (nCur.getAttributes().getNamedItem(
                    "id").getTextContent().equals(sTopicID))
                return nCur;
        }
        
        // Nothing found
        return null;
    }
    
    /** Returns the text of the topic title field, given a topic. 
     * 
     * @param sTopicID The topic of interest.
     * @return The topic title field text or null if topic was not found.
     */
    protected String getTopicTitle(String sTopicID) {
        Node nTopic = getTopicNode(sTopicID);
        if (nTopic == null)
            return null;
        
        NodeList TopicData = nTopic.getChildNodes();
        for (int iNodeCnt = 0; iNodeCnt < TopicData.getLength(); iNodeCnt++) {
            Node nCur = TopicData.item(iNodeCnt);
            // Check if field was found
            if (nCur.getNodeName().equals(TITLE_TAG))
                return nCur.getTextContent();
        }
        
        return null;
    }
    
    /** Returns the text of the narrative field, given a topic. 
     * @param sTopicID The topic of interest.
     * @return The narrative field text or null if topic was not found.
     */
    protected String getTopicNarrative(String sTopicID) {
        Node nTopic = getTopicNode(sTopicID);
        if (nTopic == null)
            return null;
        
        NodeList TopicData = nTopic.getChildNodes();
        for (int iNodeCnt = 0; iNodeCnt < TopicData.getLength(); iNodeCnt++) {
            Node nCur = TopicData.item(iNodeCnt);
            // Check if field was found
            if (nCur.getNodeName().equals(NARRATIVE_TAG))
                return nCur.getTextContent();
        }
        
        return null;
        
    }
    
    /** Returns a list of filenames the meet certain criteria: a given topic ID, 
     * and a docset.
     * 
     * @param sTopicID The topic of interest.
     * @param iFromWhichSet An integer value, being one of the following class
     *  statics: FROM_TRAINING_SET, FROM_TEST_SET, FROM_WHOLE_SET, indicating
     *  docset A, docset B or both correspondingly.
     * @return A {@link String} {@link List} of the files meeting the criteria.
     */
    protected List getFilesFromTopic(String sTopicID, int iFromWhichSet) {
        ArrayList alFiles = new ArrayList();
        // Get topic node
        Node nTopic = getTopicNode(sTopicID);
        if (nTopic == null)
            return new ArrayList();
        
        // Look for docset tags for docset A
        if ((iFromWhichSet & FROM_TRAINING_SET) != 0) {
            NodeList TopicData = nTopic.getChildNodes();
            for (int iNodeCnt = 0; iNodeCnt < TopicData.getLength(); iNodeCnt++) {
                Node nCur = TopicData.item(iNodeCnt);
                // Check if docset was found
                if (nCur.getNodeName().equals(DOCSET_A_TAG))
                {
                    // Get docsetID
                    String sDocSetID = nCur.getAttributes().getNamedItem(
                            "id").getTextContent();
                    
                    // For every file in docset
                    NodeList nDocs = nCur.getChildNodes();
                    for (int iDocCnt = 0; iDocCnt < nDocs.getLength(); 
                        iDocCnt++) {
                        Node nDoc = nDocs.item(iDocCnt);
                        // Add file to list
                        if (nDoc.getNodeName().equals(DOCUMENT_TAG))
                            alFiles.add(sTopicID + "/" + sDocSetID + "/" +
                                    nDoc.getAttributes().getNamedItem(
                                    "id").getTextContent());
                    }
                }
            }
        }
        
        // Look for docset tags for docset B
        if ((iFromWhichSet & FROM_TEST_SET) != 0) {
            NodeList TopicData = nTopic.getChildNodes();
            for (int iNodeCnt = 0; iNodeCnt < TopicData.getLength(); iNodeCnt++) {
                Node nCur = TopicData.item(iNodeCnt);
                // Check if docset was found
                if (nCur.getNodeName().equals(DOCSET_B_TAG))
                {
                    // Get docsetID
                    String sDocSetID = nCur.getAttributes().getNamedItem(
                            "id").getTextContent();
                    
                    // For every file in docset
                    NodeList nDocs = nCur.getChildNodes();
                    for (int iDocCnt = 0; iDocCnt < nDocs.getLength(); 
                        iDocCnt++) {
                        Node nDoc = nDocs.item(iDocCnt);
                        // Add file to list
                        if (nDoc.getNodeName().equals(DOCUMENT_TAG))
                            alFiles.add(sTopicID + "/" + sDocSetID + "/" +
                                    nDoc.getAttributes().getNamedItem(
                                    "id").getTextContent());
                    }
                }
            }
        }
        
        
        return alFiles;
    }
    
    /** Used for testing purposes only.
     * 
     * @param sArgs Unused.
     */
    public static void main(String[] sArgs) {
        TAC2008TopicFileSet t  = null;
        try {
            t = new TAC2008TopicFileSet("/home/ggianna/JInsect/TAC2008/UpdateSumm08_test_topics.xml",
                    "/home/ggianna/JInsect/TAC2008/UpdateSumm08_test_docs_files/");
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(TAC2008TopicFileSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(TAC2008TopicFileSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TAC2008TopicFileSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (t == null)
            return;
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
        System.err.flush();
        
        String sLastCat = (String)t.getCategories().get(
                t.getCategories().size() - 1);
        String sFile = ((CategorizedFileEntry)t.getFilesFromCategory(
                sLastCat).get(0)).getFileName();
        
        System.out.println(t.loadFile(sFile));
    }
    
    
    /** Returns the narrative question of a given topic.
     * 
     * @param sTopicID The ID of the topic of interest.
     * @return Null if the topic is not found; otherwise a String
     *  containing the narrative of the given topic.
     */
    public String getTopicDefinition(String sTopicID) {
        if (getTopicNode(sTopicID) != null)
            return getTopicTitle(sTopicID) + " " + getTopicNarrative(sTopicID);
        return null;
    }
}
