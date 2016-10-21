/*
 * SimpleSimilarity.java
 *
 * Created on April 12, 2007, 12:15 PM
 *
 */

package gr.demokritos.iit.jinsect.structs;

/** A class returning a simple similarity representation.
 *
 * @author ggianna
 */
public class SimpleSimilarity implements ISimilarity {
    /** The value of similarity this object represents.
     */
    protected Double Simil = 0.0;

    /** Creates a simple similarity object, with default value zero.
     */
    public SimpleSimilarity() {
    }
    
    /** Creates a simple similarity object, with a default value given.
     *@param dInit A double default similarity value.
     */
    public SimpleSimilarity(double dInit) {
        Simil = Double.valueOf(dInit);
    }
    
    
    public double getOverallSimilarity() {
        return Simil.doubleValue();
    }

    public synchronized void setSimilarity(double dNewVal) {
        Simil = Double.valueOf(dNewVal);
    }
    
    public double asDistance() {
        return (Simil == 0.0) ? Double.POSITIVE_INFINITY : 1.0 / Simil;
    }
    
}
