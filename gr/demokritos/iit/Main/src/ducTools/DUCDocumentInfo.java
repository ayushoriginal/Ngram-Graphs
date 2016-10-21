/*
 * DUCDocumentInfo.java
 *
 * Created on October 19, 2007, 4:12 PM
 *
 */

package gr.demokritos.iit.ducTools;

import java.io.File;

/** Parser of DUC doc names. Returns a parsed structure of document info.
 *
 * @author ggianna
 */
public class DUCDocumentInfo {
    public String Topic;
    public int Length;
    public String Selector;
    public String Assessor;
    public String Summarizer;
    
    /** Creates a new instance of DUCDocumentInfo. Can also be used for non-DUC
     * documents.
     */
    public DUCDocumentInfo(String sDocName) {
        // Get name only
        sDocName = new File(sDocName).getName();
        // If DUC doc
        if (isDUCDoc(sDocName)) {
            // Get data
            String[] sArr = sDocName.split("[.]");
            Topic = sArr[0];
            Length = Integer.valueOf(sArr[2]).intValue();
            Selector = sArr[3];
            Assessor = sArr[3];
            Summarizer = sArr[4];
        }
        else
        {
            Topic = "NA";
            Length = -1; // Unknown
            Selector = "NA";
            Assessor = "NA";
            Summarizer = "NA";
        }
    }
    
    
    /** Returns true if the document is a DUC named document. 
     *@param sDocName The name of the document to check.
     *@return True if document name has 5 parts, as in DUC.
     */
    public static boolean isDUCDoc(String sDocName) {
        return (new File(sDocName).getName().split("[.]").length == 5);
    }
}
