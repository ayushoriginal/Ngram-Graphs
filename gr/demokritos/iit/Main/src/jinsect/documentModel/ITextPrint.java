/*
 * ITextPrint.java
 *
 * Created on 25 Ιανουάριος 2006, 1:48 πμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;

/** Describes a text representation that is based on a DocumentNGramHistogram and a DocumentNGramGraph.
 *
 * @author PCKid
 */
public interface ITextPrint {
    /** Should return the histogram corresponding to the text representation. 
     *@return An n-gram histogram representing a text.
     */   
    public DocumentNGramHistogram getDocumentHistogram();
    
    /** Should set the histogram corresponding to the text representation. 
     *@param idnNew The n-gram histogram to replace the existing one.
     */
    public void setDocumentHistogram(DocumentNGramHistogram idnNew);

    /** Should return the graph corresponding to the text representation. 
     *@return A document n-gram graph representing a text.
     */   
    public DocumentNGramGraph getDocumentGraph();
    /** Should set the histogram corresponding to the text representation. 
     *@param idgNew The n-gram graph to replace the existing one.
     */
    public void setDocumentGraph(DocumentNGramGraph idgNew);
}
