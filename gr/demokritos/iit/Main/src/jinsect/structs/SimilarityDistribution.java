/*
 * SimilarityDistribution.java
 *
 * Created on 9 Φεβρουάριος 2007, 5:23 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.TreeMap;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;

/** A distribution of similarities.
 *
 * @author ggianna
 */
public class SimilarityDistribution {
    private Distribution dContain = new Distribution();
    private Distribution dValue = new Distribution(); 
    private Distribution dSize = new Distribution(); 
    private TreeMap hmSimils = new TreeMap();
    
    public void setValue(Object oKey, GraphSimilarity sSimil) {
        hmSimils.put(oKey, sSimil);
        dContain.setValue(oKey, sSimil.ContainmentSimilarity);
        dValue.setValue(oKey, sSimil.ValueSimilarity);
        dSize.setValue(oKey, sSimil.SizeSimilarity);
    }
    
    public GraphSimilarity getValue(Object oKey) {
        if (hmSimils.containsKey(oKey))
            return (GraphSimilarity)hmSimils.get(oKey);
        else
            return new GraphSimilarity();
    }
    
    public GraphSimilarity average() {
        GraphSimilarity isRes = new GraphSimilarity();
        isRes.ContainmentSimilarity = dContain.average(true);
        isRes.ValueSimilarity = dValue.average(true);
        isRes.SizeSimilarity = dSize.average(true);
        
        return isRes;
    }
    
    public TreeMap asTreeMap() {
        return hmSimils;
    }
}
