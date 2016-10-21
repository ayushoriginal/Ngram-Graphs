/*
 * IntegerPair.java
 *
 * Created on July 2, 2007, 11:37 AM
 *
 */

package gr.demokritos.iit.jinsect.structs;

/** Implements a pair of integers. The pair is {@link Comparable}, by doing a string comparison between the
 * string representation of the pair, as: value1 + "_" + value2
 *
 * @author ggianna
 */
public class IntegerPair implements Comparable {
    protected int[] aPair = new int[2];
    
    public IntegerPair(int []nums) {
        aPair[0] = nums[0];
        aPair[1] = nums[1]; 
    }
    
    public IntegerPair(int num1, int num2) {
        aPair[0] = num1;
        aPair[1] = num2; 
    }
    
    public int compareTo(Object oOther) {
        IntegerPair o = (IntegerPair)oOther;
        // Actually string difference
        return toString().compareTo(o.toString());
    }
    
    public String toString() {
        return new String(aPair[0] + "_" + aPair[1]);
    }
    
    public int[] toArray() {
        return aPair;
    }
    
    public int first() {
        return aPair[0];
    }

    public int second() {
        return aPair[0];
    }
}