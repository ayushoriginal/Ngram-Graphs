/*
 * SimilarityComparatorListener.java
 *
 * Created on 25 Ιανουάριος 2006, 2:09 πμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.events;

import java.io.InvalidClassException;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import java.io.Serializable;

/** A class representing a similarity comparator, listening to similarity comparison request events.
 *
 * @author PCKid
 */
public interface SimilarityComparatorListener extends Serializable {
    /** Calculates the similarity between two objects.
     *@param oFirst The first object.
     *@param oSecond The second object.
     *@return The similarity between the two objects.
     *@throws InvalidClassException Throws this exception when the objects are not comparable due to class
     * restrictions.
     *@see ISimilarity
     */
    public ISimilarity getSimilarityBetween(Object oFirst, Object oSecond) throws InvalidClassException;
}
