/*
 * StandardDocumentDistroComparator.java
 *
 * Created on 12 Φεβρουάριος 2007, 6:03 μμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;

import java.io.InvalidClassException;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDistroDocument;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;

/** This class implements the SimilarityComparatorListener interface, describing objects that
 * can perform comparison between {@link NGramDistroDocument} objects, than contain n-gram
 * graph edge information represented as distributions. 
 *
 * @author PCKid
 */
public class StandardDocumentDistroComparator implements SimilarityComparatorListener {    
    private double GraphImportance = 0.5;   // Graph importance
    private GraphSimilarity sGraph;
    private GraphSimilarity sHistogram;
    
    /** Creates a new instance of StandardComparator. */
    public StandardDocumentDistroComparator() {
        sGraph = new GraphSimilarity();
        sHistogram = new GraphSimilarity();
    }
    
    /** Creates a new instance of StandardComparator, given the importance of the n-gram graph similarity in the
     * calculation of overall similarity.
     *@param dGraphImportance A value between 0.0 and 1.0 indicating how important is the graph in the calculation.
     * If zero, then only histogram information is used. If 1.0, then only graph information is used.
     */
    public StandardDocumentDistroComparator(double dGraphImportance) {
        GraphImportance = dGraphImportance;
    }
    
    /** Returns the similarity of two given {@link NGramDistroDocument}s.
     *@param oFirst The first object participating in the similarity calculation. If not an NGramDistroDocument then an {@link InvalidClassException}
     * is thrown.
     *@param oSecond The first object participating in the similarity calculation. If not an NGramDistroDocument then an {@link InvalidClassException}
     * is thrown.
     *@return A {@link GraphSimilarity} object indicating the similarity between the two given objects.
     */
    public GraphSimilarity getSimilarityBetween(Object oFirst, Object oSecond) throws InvalidClassException {
        if (!((oFirst instanceof NGramDistroDocument) && (oSecond instanceof NGramDistroDocument)))
            throw new InvalidClassException("Both operands should be Documents (NGramDistroDocument class)");
        NGramDistroDocument dFirst = (NGramDistroDocument)oFirst;
        NGramDistroDocument dSecond = (NGramDistroDocument)oSecond;
        NGramCachedDistroGraphComparator gcComparator = new NGramCachedDistroGraphComparator();
        NGramHistogramComparator hcComparator = new NGramHistogramComparator();
        
        GraphSimilarity[] saSimil = new GraphSimilarity[2];
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
    
    /** Returns the graph similarity part of the overall similality measurement, performed for the last comparison. 
     *@return The graph similarity of the objects last evaluated.
     */
    public GraphSimilarity getGraphSimilarity() {
        return sGraph;
    }
    
    /** Returns the graph similarity part of the overall similality measurement, performed for the last comparison. 
     *@return The histogram similarity of the objects last evaluated.
     */
    public GraphSimilarity getHistogramSimilarity() {
        return sHistogram;
    }
}