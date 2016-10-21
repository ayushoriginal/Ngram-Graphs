/*
 * DistributionSet.java
 *
 * Created on March 30, 2007, 10:59 AM
 *
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author ggianna
 */
public class VectorSet extends HashSet {
    /**
     * Creates a new instance of VectorSet, expected to contain a set of {@link Distribution}
     * objects, viewed as Vectors.
     */
    public VectorSet() {
    }
    
    /** Calculates the centroid of all distributions contained. 
     *@return The centroid 
     */
    public Distribution centroid() {
        if (isEmpty())
            return null;
        
        Distribution dRes = new Distribution();
        Iterator iVectorIter = iterator();
        // For all vectors
        while (iVectorIter.hasNext()) {
            Distribution dCurVector = (Distribution)iVectorIter.next();
            // For all features
            Iterator iFeatures = dCurVector.asTreeMap().keySet().iterator();
            while (iFeatures.hasNext()) {
                Object oFeatureKey = iFeatures.next();
                
            }
        }
        // TODO: Implement
        return null;
    }
}
