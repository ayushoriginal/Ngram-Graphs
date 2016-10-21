/*
 * DocumentHistoComparator.java
 *
 * Created on September 13, 2007, 1:06 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

/** A comparator class that only uses the histogram for comparison.
 * This class actually uses the {@link StandardDocumentComparator} to perform the similarity
 * calculation, but given the graph a zero importance.
 * @author ggianna
 */
public class DocumentHistoComparator extends StandardDocumentComparator {
    
    /** Creates a new instance of DocumentHistoComparator. */
    public DocumentHistoComparator() {
        super(0.0);
    }
    
}
