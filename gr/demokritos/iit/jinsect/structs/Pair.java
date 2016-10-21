/*
 * Pair.java
 *
 * Created on May 6, 2008, 1:48 PM
 *
 */

package gr.demokritos.iit.jinsect.structs;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/** Represents a pair of elements of any type (as a templated class).
 *
 * @author ggianna
 */
public class Pair<ObjTypeFirst, ObjTypeSecond> implements Serializable {
    protected ObjTypeFirst first;
    protected ObjTypeSecond second;
    
    /** Creates a new instance of Pair, given two objects. 
     *@param oFirst The first object.
     *@param oSecond The second object.
     */
    public Pair(ObjTypeFirst oFirst, ObjTypeSecond oSecond) {
        first = oFirst;
        second = oSecond;
    }

    @Override
    public int hashCode() {
        return (String.valueOf(first.hashCode()) + "_" +
                String.valueOf(second.hashCode())).hashCode();
    }

    @Override
    public String toString() {
        return first.toString() + ", " + second.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Pair) && (first.equals(((Pair)obj).getFirst()) &&
                second.equals(((Pair)obj).getSecond()));
    }



    /** Returns the first object of the pair. 
     *@return The first object. 
     */
    public ObjTypeFirst getFirst() {
        return first;
    }

    /** Returns the second object of the pair. 
     *@return The second object. 
     */
    public ObjTypeSecond getSecond() {
        return second;
    }

    private void writeObject(java.io.ObjectOutputStream out)
         throws IOException {
        out.writeObject(first);
        out.writeObject(second);
    }

    private void readObject(java.io.ObjectInputStream in)
         throws IOException, ClassNotFoundException {
         first = (ObjTypeFirst)in.readObject();
         second = (ObjTypeSecond)in.readObject();
    }

    private void readObjectNoData() throws ObjectStreamException {
        first = null;
        second = null;
    }
}
