/*
 * KLDivergenceCalculator.java
 *
 * Created on March 29, 2007, 4:15 PM
 *
 */

package gr.demokritos.iit.jinsect.algorithms.statistics;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.util.Iterator;

/**
 * Computes the entropy based KL Divergence between two distributions
 * @author ilias
 * @author ggianna
 */
public class KLDivergenceCalculator {
    
    /**
     * The static method where the computation of KL_asymmetric is performed.
     * This measure is asymmetric and non-negative.
     *
     * @param p the first {@link Distribution}.
     * @param q the second {@link Distribution}.
     * @return  zero if q and p are equal
     */
    static public double KL_asymmetric(Distribution p, Distribution q) {
        double sum=0;
        if(p.asTreeMap().size()==q.asTreeMap().size()) {
            Iterator iIter = p.asTreeMap().keySet().iterator();
            while (iIter.hasNext()) {
                Object i = iIter.next();
                sum+=p.getValue(i)*Math.log10(p.getValue(i)/q.getValue(i))/Math.log10(2);                
            }
        } else {
            return 0;
        }
        return sum;
    }
    
    /**
     * The static method where the computation of symmetric KL is performed.
     * This measure is symmetric and non-negative.
     *
     * @param p the first {@link Distribution}.
     * @param q the second {@link Distribution}.
     * @return  zero if q and p are equal
     */
    static public double KL_symmetric(Distribution p, Distribution q) {
        if(p.asTreeMap().size()==q.asTreeMap().size()) {
            return(0.5*(KL_asymmetric(p,q)+KL_asymmetric(q,p)));
        } else {
            return 0;
        }
    }
    
}
