/*
 * IMergeable.java
 *
 * Created on May 7, 2008, 2:59 PM
 *
 */

package gr.demokritos.iit.jinsect.structs;

/** Describes a class that can be merged with (updated by) another object.
 *
 * @author ggianna
 */
public interface IMergeable<TType> {
    /**
     *Merges the object with a new one, given a weight parameter used in the 
     * update.
     *@param dgOtherObject The second object used for the merging.
     *@param fWeightPercent The convergence tendency parameter. Typically, 
     * a value of 0.0 means no change to existing object, 
     * 1.0 means updated object is the same as  the new object. 
     * A value of 0.5 means new object is equally similar to the two source 
     * objects (averaging effect).
    ***/
    public void merge(TType dgOtherObject, double fWeightPercent);
}
