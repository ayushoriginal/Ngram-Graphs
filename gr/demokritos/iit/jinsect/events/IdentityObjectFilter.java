/* Under LGPL licence.
 */

package gr.demokritos.iit.jinsect.events;

/** A filter the applies no change to an object. It returns the object itself.
 *
 * @author pckid
 */
public class IdentityObjectFilter<TObjectType> 
        implements IObjectFilter<TObjectType> {

    /** Returns the object itself.
     * 
     * @param obj The input object.
     * @return The input object, ubchanged.
     */
    public final TObjectType filter(TObjectType obj) {
        return obj;
    }
    
}
