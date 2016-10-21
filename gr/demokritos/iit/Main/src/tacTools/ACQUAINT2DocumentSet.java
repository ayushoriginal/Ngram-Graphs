/*
 * TACDocumentSet.java
 *
 * Created on May 5, 2008, 10:18 AM
 *
 */

package gr.demokritos.iit.tacTools;

import gr.demokritos.iit.jinsect.storage.IFileLoader;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.IDocumentSet;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** This class implements the {@link IDocumentSet} interface, also implementing
 * a number of methods to make the retrieval of TAC document sets (also called 
 * DOCSTREAMs) easier.
 *
 * @author ggianna
 */
public class ACQUAINT2DocumentSet implements IDocumentSet, IFileLoader<String> {
    /** Constant indicating tirage from the training set.
     */
    public static final int FROM_TRAINING_SET = 1;
    /** Constant indicating tirage from the test set.
     */
    public static final int FROM_TEST_SET = 2;
    /** Constant indicating tirage from the whole (training plus test) set.
     */
    public static final int FROM_WHOLE_SET = 3;
    
    /** The tag name of the Dateline tag. */
    public static final String DATELINE_TAG = "DATELINE";
    /** The tag name of the Text tag. */
    public static final String TEXT_TAG = "TEXT";
    
    /** The XMLDocument holding the DOCSTREAM. */
    Document XMLDoc;
    /** The categories of documents, derived from the <i>type</i> attribute of 
     * the <i>DOC</i> tag. */
    ArrayList Categories;
    /** The set of document identifiers.
     */
    HashSet<String> hsDocs;
    /** The mapping of files to categories. */
    HashMap<String, String> hmDocsToCategories;
    
    /** Creates a new instance of TACDocumentSet, given a corresponding TAC08
     * formatted file.
     *@param sTACXMLFile The XML file containing the DOCSTREAM.
     */
    public ACQUAINT2DocumentSet(String sTACXMLFile) {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        XMLDoc = null;
        Categories = null;
        hsDocs = null;
        hmDocsToCategories = null;
        
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            XMLDoc = docBuilder.parse(new File(sTACXMLFile));
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
            return;
        
        // Init mapping to categories
        hmDocsToCategories = new HashMap<String, String>();
        
        // normalize text representation
        XMLDoc.getDocumentElement().normalize();
    }

    public List getCategories() {
        // Check for already calculated categories
        if (Categories != null)
            // and return, if found.
            return Categories;
        
        
        HashMap<String,Integer> hmCategories = new HashMap();
        // Init categories.
        // Get all docs
        NodeList docList = XMLDoc.getElementsByTagName("DOC");
        // and extract type as category.
        for (int iNodeCnt = 0; iNodeCnt < docList.getLength(); iNodeCnt++) {
            // Update Categories
            hmCategories.put(docList.item(iNodeCnt).getAttributes().getNamedItem("type").getNodeValue(), 1);
        }
        Categories = new ArrayList(hmCategories.keySet());
        
        return Categories;
    }

    public void createSets() {
        // Clear existing set of files
        hsDocs = new HashSet<String>();
        // Get all docs
        NodeList docList = XMLDoc.getElementsByTagName("DOC");
        for (int iNodeCnt = 0; iNodeCnt < docList.getLength(); iNodeCnt++) {
            // Add file ID to set
            hsDocs.add(docList.item(iNodeCnt).getAttributes().getNamedItem("id").getNodeValue());
            // Update category for file
            hmDocsToCategories.put(docList.item(iNodeCnt).getAttributes().getNamedItem("id").getNodeValue(),
                    docList.item(iNodeCnt).getAttributes().getNamedItem("type").getNodeValue());
        }
        
    }

    public ArrayList getFilesFromCategory(String sCategoryName) {
        ArrayList sRes = new ArrayList();
        for (String sFile : hmDocsToCategories.keySet()) {
            if (hmDocsToCategories.get(sFile).equals(sCategoryName))
                sRes.add(sFile);
        }
        
        return sRes;
    }

    /** Returns whole set. TODO: Implement as should be. */
    public ArrayList getTrainingSet() {
        ArrayList<CategorizedFileEntry> alRes = new ArrayList<CategorizedFileEntry>();
        for (String sFile : hmDocsToCategories.keySet()) {
            alRes.add(new CategorizedFileEntry(sFile, hmDocsToCategories.get(sFile)));
        }
        
        return alRes;
    }

    /** Returns whole set. TODO: Implement as should be. */
    public ArrayList getTestSet() {
        ArrayList<CategorizedFileEntry> alRes = new ArrayList<CategorizedFileEntry>();
        for (String sFile : hmDocsToCategories.keySet()) {
            alRes.add(new CategorizedFileEntry(sFile, hmDocsToCategories.get(sFile)));
        }
        
        return alRes;
    }
    
    public String loadFile(String sID) {
        return loadDocumentTextToString(sID);
    }
    
    /** Returns the text portion of a given document as a String.
     *@param sDocID The document ID.
     *@return Null if document is not found, otherwise its text portion (all
     * text found within <i>TEXT</i> tags.
     */
    public String loadDocumentTextToString(String sDocID) {
        return loadDocumentElement(sDocID, TEXT_TAG);
    }

    /** Returns the full text of a given document, including dateline and other 
     * elements as a String. Tags are removed.
     *@param sDocID The document ID.
     *@return Null if document is not found, otherwise its full text.
     */
    public String loadFullDocumentTextToString(String sDocID) {
        Node nDoc = XMLDoc.getElementById(sDocID);
        if (nDoc == null)
            return null;
        
        Element eDoc = (Element)nDoc;
        String sRes = eDoc.getTextContent();
        
        return sRes;
    }

    /** Returns the dateline portion of a given document as a String, if the
     * dateline exists.
     *@param sDocID The document ID.
     *@return Null if document is not found, a zero length String if no dateline
     * was found, otherwise the document's dateline field (all
     * text found within <i>DATELINE</i> tags.
     */
    public String loadDocumentDatelineToString(String sDocID) {
        return loadDocumentElement(sDocID, DATELINE_TAG);
    }
    
    /** Returns a given element of a given document as a String, if the
     * element exists.
     *@param sDocID The document ID.
     *@param sElement The element name (e.g. TEXT or DATELINE).
     *@return Null if document is not found, a zero length String if the specified
     * element was not found, otherwise the document's element text.
     */
    public final String loadDocumentElement(String sDocID, String sElement) {
        Node nDoc = XMLDoc.getElementById(sDocID);
        if (nDoc == null)
            return null;
        
        Element eDoc = (Element)nDoc;
        NodeList nDocElements = nDoc.getChildNodes();
        Node n = null;
        // Find text elements
        for (int iCnt = 0; iCnt < nDocElements.getLength(); iCnt++) {
            if (nDocElements.item(iCnt).getNodeName().equalsIgnoreCase(sElement)) {
                n = nDocElements.item(iCnt);
                break;                
            }
        }
        
        String sRes;
        if (n != null)
            sRes = n.getTextContent();
        else
            sRes = ""; // Not found
        
        return sRes;
        
    }
    
    public Date getDocDate(String sDocID) {
        // Get dateline
        String sDate = loadDocumentDatelineToString(sDocID);
        
        // Init date based on ID.
        // DocID Format: XXX_XXX_YYYYMMdd.####
        // Get year by backtracking 8 digits from the dot.
        int iDateStart = sDocID.indexOf(".");
        String sYear = sDocID.substring(iDateStart - 8, iDateStart - 4);
        String sMonthNum = sDocID.substring(sDocID.indexOf(".") - 4, 
                sDocID.indexOf(".") - 2);
        String sMonth = null;
        String sMonthDay = sDocID.substring(sDocID.indexOf(".") - 4, 
                sDocID.indexOf(".") - 2);
        
        String sFinalDate = "";
        
        // Use dateline
        Pattern pDate = Pattern.compile(
                "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)");
        // If a month match has been found
        Matcher m = pDate.matcher(sDate);
        
        if (m.find()) {
            // Get month substring
            sMonth = sDate.substring(m.start(), m.end());
            pDate = Pattern.compile("\\d+");
            
            // If monthdate has been found
            m = pDate.matcher(sDate);
            if (m.find()) {
                // read it
                sMonthDay = sDate.substring(m.start(), m.end());
            }
        }
            
        // Compose date in medium form
        sFinalDate = sYear + " " + ((sMonth == null) ? sMonthNum : sMonth) + 
                " " + sMonthDay;
        try {
            String sFormat = (sMonth == null) ? "YYYY MM dd" : "YYYY MMM dd";
            return new SimpleDateFormat(sFormat, new Locale("eng")
                    ).parse(sFinalDate);
        } catch (ParseException ex) {
            Logger.getLogger(ACQUAINT2DocumentSet.class.getName()).log(Level.WARNING, null, ex);
        }
        
        return null;
    }
            
    /** Testing main function. */
    public static void main(String[] sArgs) {
        System.err.print("Parsing XML file...");
        ACQUAINT2DocumentSet tds = new ACQUAINT2DocumentSet(
                "/home/pckid/Documents/JApplications/JInsect/TAC2008/data/cna_eng/cna_eng_200410");
        System.err.println("Done.");
        
        System.err.print("Creating sets.");
        tds.createSets();
        System.err.println("Done.");

        System.err.println("Determining categories...");
        System.out.println(utils.printList(tds.getCategories(), " | "));
        System.err.println("Determining categories...Done.");
        
        System.err.println("File count per category...");
        for (String sCategory : (List<String>)tds.getCategories())
            System.out.println(String.format("%s : #%d", sCategory, tds.getFilesFromCategory(sCategory).size()));
        System.err.println("File count per category...Done.");
        
        System.out.println("First text per category...");
        for (String sCategory : (List<String>)tds.getCategories()) {
            System.out.println("\n===" + sCategory + "===");
            System.out.println(tds.loadDocumentTextToString((String)tds.getFilesFromCategory(sCategory).get(0)));
            String sDateline = tds.loadDocumentDatelineToString(
                    (String)tds.getFilesFromCategory(sCategory).get(0));
            System.out.println(sDateline.length() == 0 ? "No dateline..." : "Dateline:\t" + sDateline);
        }        
        System.out.println("File count per category...Done.");
        
        System.err.println("Extracting dates...");
        for (String sDocID : tds.toFilenameSet(ACQUAINT2DocumentSet.FROM_WHOLE_SET)) {
            Date d = tds.getDocDate(sDocID);
            System.out.println(String.format("%s : %s", sDocID, d != null ? d.toString() :
                "No date found"));
        }
        System.err.println("Extracting dates...Done.");
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
