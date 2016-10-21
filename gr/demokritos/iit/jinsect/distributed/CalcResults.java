/*
 * CalcResults.java
 *
 * Created on 15 ?????????????????????? 2007, 2:47 ????
 *
 */

package gr.demokritos.iit.jinsect.distributed;

import java.io.Serializable;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;

/** Represents the result of a comparison, with a unique ID and a similarity array of
 * results.
 *@see SimilarityArray
 */
public class CalcResults implements Serializable {
    String ID; 
    SimilarityArray Simil;
}