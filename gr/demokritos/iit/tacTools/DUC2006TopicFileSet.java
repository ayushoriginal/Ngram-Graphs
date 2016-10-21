/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.demokritos.iit.tacTools;

import gr.demokritos.iit.jinsect.storage.IFileLoader;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;

/** Uses an DUC 2006 SGML topic definition file to create the set.
 *
 * @author ggianna
 */
public class DUC2006TopicFileSet extends DocumentSet implements
        IFileLoader<String> {
    /** Topic tag string in XML file. */
    protected static String TOPIC_TAG = "topic";
    /** Title tag string in XML file. */
    protected static String TITLE_TAG = "title";
    /** Docset A tag string in XML file. */
    protected static String DOCSET_TAG = "num";
    /** Narrative tag string in XML file. */
    protected static String NARRATIVE_TAG = "narr";
    /** Text tag string in document file. */
    protected static String TEXT_TAG = "TEXT";
    /** Text tag string in document file. */
    protected static String HEADLINE_TAG = "HEADLINE";

    /** The SGML topic file filename */
    protected String TopicFile;
    /** The underlying XML document itself. */
    Document TopicXMLDoc;

    /** The content of the topic file. */
    String TopicFileText;

    /** Initializes the document set, given a TAC2008 topic XML file.
     *
     * @param sTopicSGMLFile The filename of the topic file.
     * @param sCorpusRootDir The base directory of the TAC2008 test corpus
     * directory structure.
     */
    public DUC2006TopicFileSet(String sTopicSGMLFile, String sCorpusRootDir) {
        super(sCorpusRootDir, 1.0);
        
        TopicFile = sTopicSGMLFile;
        // Init SGML topic file
        TopicFileText = utils.loadFileToStringWithNewlines(sTopicSGMLFile);
        
    }

    /** Returns the text of the topic title field, given a topic.
     *
     * @param sTopicID The topic of interest.
     * @return The topic title field text or null if topic was not found.
     */
    protected String getTopicTitle(String sTopicID) {
        int iTopicIdx, iTitleIdx;
        // Look up topic
        String sLookUp = String.format("<%s> %s </%s>", DOCSET_TAG, sTopicID,
                DOCSET_TAG);
        if ((iTopicIdx = TopicFileText.indexOf(sLookUp)) < 0)
            return null; // Could not find topic

        // Look up title
        iTopicIdx += sLookUp.length(); // Skip definition
        sLookUp = String.format("<%s>", TITLE_TAG);
        if ((iTitleIdx = TopicFileText.indexOf(sLookUp, iTopicIdx)) < 0)
                return null; // Could not find title
        iTitleIdx += sLookUp.length();

        sLookUp = String.format("</%s>", TITLE_TAG);
        // Find the end of the title and return the title
        return TopicFileText.substring(iTitleIdx, 
                TopicFileText.indexOf(sLookUp, iTitleIdx));
    }

    /** Returns the text of the narrative field, given a topic.
     * @param sTopicID The topic of interest.
     * @return The narrative field text or null if topic was not found.
     */
    protected String getTopicNarrative(String sTopicID) {
        int iTopicIdx, iNarrIdx;
        // Look up topic
        String sLookUp = String.format("<%s> %s </%s>", DOCSET_TAG, sTopicID,
                DOCSET_TAG);
        if ((iTopicIdx = TopicFileText.indexOf(sLookUp)) < 0)
            return null; // Could not find topic

        // Look up title
        iTopicIdx += sLookUp.length(); // Skip definition
        sLookUp = String.format("<%s>", NARRATIVE_TAG);
        if ((iNarrIdx = TopicFileText.indexOf(sLookUp, iTopicIdx)) < 0)
                return null; // Could not find title
        iNarrIdx += sLookUp.length();

        sLookUp = String.format("</%s>", NARRATIVE_TAG);
        // Find the end of the title and return the title
        return TopicFileText.substring(iNarrIdx,
                TopicFileText.indexOf(sLookUp, iNarrIdx));
    }

    /** Used for testing purposes only.
     *
     * @param sArgs Unused.
     */
    public static void main(String[] sArgs) {
        DUC2006TopicFileSet t  = null;
        t = new DUC2006TopicFileSet("/usr/misc/Corpora/DUC2006/duc2006_topics.sgml",
                "/usr/misc/Corpora/DUC2006/duc2006_docs/");

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
        return getTopicTitle(sTopicID) + " " + getTopicNarrative(sTopicID);
    }

    @Override
    public String loadFile(String sID) {
        String sRes = null;
        String sTmp = utils.loadFileToStringWithNewlines(sID);
        String sLookUp = "<" + HEADLINE_TAG + ">";
        if (sTmp.indexOf(sLookUp) > 0)
            sRes = sTmp.substring(sTmp.indexOf(sLookUp) + sLookUp.length(),
                sTmp.indexOf("</" + HEADLINE_TAG));
        else
            sRes = "";

        sLookUp = "<" + TEXT_TAG + ">";
        sRes += " " + sTmp.substring(sTmp.indexOf(sLookUp) + sLookUp.length(),
                sTmp.indexOf("</" + TEXT_TAG));

        return sRes.replaceAll("<P>|</P>", "");
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
}
