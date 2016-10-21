/*
 * IMatching.java
 *
 */

package gr.demokritos.iit.jinsect;

/** Interface indicating if an object matches another (given) object.
 *
 * @author ggianna
 */
public interface IMatching<TypeToMatch> {
    /** Returns true if the implementer of the interfaces matches a given object.
     *@param o1 The given object.
     *@return True if matched, else false.
     */
    public boolean match(TypeToMatch o1);
}
