/*
 * CachedDocumentComparator.java
 *
 * Created on June 4, 2007, 3:37 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import java.io.InvalidClassException;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;

/** An optimized document comparator that uses cached edge information (via {@link NGramCachedGraphComparator}) for the graph comparison process.
 *
 * @author ggianna
 */
public class CachedDocumentComparator extends StandardDocumentComparator implements
    NotificationListener {
    
    /** Creates an instance of CachedDocumentComparator, using default importance for graph and histogram (0.5).*/
    public CachedDocumentComparator() {
        GraphImportance = 0.5;
    }
    
    public CachedDocumentComparator(double dGraphImportance) {
        GraphImportance = dGraphImportance;
    }
    
    public GraphSimilarity getSimilarityBetween(Object oFirst, Object oSecond) throws InvalidClassException {
        if (!((oFirst instanceof NGramDocument) && (oSecond instanceof NGramDocument)))
            throw new InvalidClassException("Both operands should be Documents (" + NGramDocument.class.getName() + 
                    " class)");
        NGramDocument dFirst = (NGramDocument)oFirst;
        NGramDocument dSecond = (NGramDocument)oSecond;
        NGramCachedGraphComparator gcComparator = new NGramCachedGraphComparator();
        NGramHistogramComparator hcComparator = new NGramHistogramComparator();
        
        GraphSimilarity[] saSimil = new GraphSimilarity[2];
        //gcComparator.setNotificationListener(this); // Set this to listener
        // Graph GraphSimilarity
        saSimil[0] = gcComparator.getSimilarityBetween(dFirst.getDocumentGraph(), dSecond.getDocumentGraph());
        sGraph = saSimil[0];
        // Histogram GraphSimilarity
        saSimil[1] = hcComparator.getSimilarityBetween(dFirst.getDocumentHistogram(), dSecond.getDocumentHistogram());
        sHistogram = saSimil[1];
        
        GraphSimilarity sSimil = new GraphSimilarity();
        sSimil.ContainmentSimilarity = saSimil[0].ContainmentSimilarity * GraphImportance +
                saSimil[1].ContainmentSimilarity * (1 - GraphImportance);
        sSimil.ValueSimilarity = saSimil[0].ValueSimilarity * GraphImportance +
                saSimil[1].ValueSimilarity * (1 - GraphImportance);
        sSimil.SizeSimilarity = saSimil[0].SizeSimilarity * GraphImportance +
                saSimil[1].SizeSimilarity * (1 - GraphImportance);
                
        return sSimil;
    }

    /** Acts as a notifier, outputting debug information to the standard error, indicative of the
     *  comparison progress. 
     *@param oSender The sender of the notification.
     *@param oParams The parameter object containing a Double number (0.0 to 1.0), that indicated the percentile
     * progress of the process.
     */
    public void Notify(Object oSender, Object oParams) {
        double dProgress =  ((Double)oParams).doubleValue();
        System.err.print("Comparison " + String.format("%4.2f%%", dProgress) + "\r");
    }
    
    
}
