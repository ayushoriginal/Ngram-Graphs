/*
 * GraphSimilarityComparatorAdapter.java
 *
 * Created on 25 Ιανουάριος 2006, 2:11 πμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.events;
import java.io.InvalidClassException;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
/**
 *
 * @author PCKid
 */
public class GraphSimilarityComparatorAdapter implements SimilarityComparatorListener {
    
    /**
     * Creates a new instance of GraphSimilarityComparatorAdapter
     */
    public GraphSimilarityComparatorAdapter() {
    }

    public ISimilarity getSimilarityBetween(Object oFirst, Object oSecond)  throws InvalidClassException {
        return new GraphSimilarity();
    }
}
