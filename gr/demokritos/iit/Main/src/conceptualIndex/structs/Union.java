/*
 * Union.java
 *
 * Created on 11 Οκτώβριος 2006, 5:11 μμ
 *
 */

package gr.demokritos.iit.conceptualIndex.structs;

import java.util.ArrayList;
import java.util.Iterator;

/** A list subclass indicative of union between elements.
 *
 * @author ggianna
 */
public class Union  extends ArrayList {
    
    /** Returns a string representation of this union, using distinctive notation.
     */
     public String toString() {
        String sRes = "[";
        Iterator iIter = iterator();
        while (iIter.hasNext()) {
            Object oNext = iIter.next();
            sRes += oNext.toString();
            if (iIter.hasNext())
                sRes += "|";
        }
        sRes += "]";
        return sRes;
    }
    
}
