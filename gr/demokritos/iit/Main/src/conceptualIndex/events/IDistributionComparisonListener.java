/*
 * IDistributionComparisonListener.java
 *
 * Created on 13 Μάρτιος 2007, 2:57 μμ
 *
 */

package gr.demokritos.iit.conceptualIndex.events;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;

/** Interface for classes that can compare distributions.
 *@see Distribution
 *
 * @author ggianna
 */
public interface IDistributionComparisonListener {
    public double compareDistributions(Distribution d1, Distribution d2);   
}
