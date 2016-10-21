/*
 * TextPrintImpl.java
 *
 * Created on 25 Ιανουάριος 2006, 1:50 πμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;

/** This class implements the {@link ITextPrint} interface, with default implementations.
 *
 * @author PCKid
 */
public class TextPrintImpl implements ITextPrint {
    private DocumentNGramHistogram idn;
    private DocumentNGramGraph idg;
    
    /**
     * Creates a new instance of TextPrintImpl.
     */
    public TextPrintImpl() {
    }
    
    public DocumentNGramHistogram getDocumentHistogram() {
        return idn;
    }
    
    public void setDocumentHistogram(DocumentNGramHistogram idnNew) {
        idn = idnNew;
    }

    public DocumentNGramGraph getDocumentGraph() {
        return idg;
    }
    
    public void setDocumentGraph(DocumentNGramGraph idgNew) {
        idg = idgNew;
    }

}
