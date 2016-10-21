/*
 * Suggester.java
 *
 * Created on 11 Φεβρουάριος 2006, 8:11 μμ
 *
 */

package gr.demokritos.iit.jinsect.classification;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import gr.demokritos.iit.jinsect.structs.Decision;
import gr.demokritos.iit.jinsect.utils;

/** This class describes objects that can suggest a category, given previous decisions. 
 * This class is a meta-classifier, in that it decides based on previous decisions
 * and corrections, in order to determine the final proposal. Suggester gives an answer if the similarity
 * between the confidence of the current decision and previous ones is within a given threshold.
 *
 * @author PCKid
 */
public class Suggester {
    /** List of previous decisions. */
    private Vector vPreviousDecisions;
    /** The distance threshold from previous decisions within which a decision can be made. */
    private double LookupThreshold;
    
    /** Creates a new instance of Suggester, with a threshold of 0.8. */
    public Suggester() {
        this(0.80);
    }
    
    /** Creates a new instance of Suggester, given a threshold.
     *@param dLookupThreshold The threshold to use for the decision process.
     */
    public Suggester(double dLookupThreshold) {
        vPreviousDecisions = new Vector();
        LookupThreshold = dLookupThreshold;
    }
    
    /** Clear existing decisions cache. */
    public void clear() {
        vPreviousDecisions.clear();
    }
    
    /** Trains the suggester with a new instance, given the estimation values for each category,
     * the original category decided and the actual category.
     *@param CategoryValues A {@link Map} containing category-estimation pairs, indicating how close
     *a given instance was to the possible categories.
     *@param sSuggestedCategory The category originally suggested (by some decider) for the instance.
     *@param sFinalCategory The final (corrected) category for the given instance.
     */
    public void train(Map CategoryValues, String sSuggestedCategory, String sFinalCategory) {
        if (CategoryValues == null)
            CategoryValues = new HashMap();
        if (CategoryValues.size() > 0)
            vPreviousDecisions.add(new DecisionSupport(CategoryValues, sSuggestedCategory, sFinalCategory));
    }
    
    /** Decide upon an instance, given a decider's estimation of this instance's similarity to a set of categories.
     *@param CategoryValues A {@link Map} containing category-estimation pairs, indicating how close
     *a given instance was to the possible categories.
     *@return A {@link Decision} indicating updated estimations.
     */
    public Decision suggest(Map CategoryValues) {
        String sSuggestion = "";
        // Use similarity.
        double dMaxSimil = -1.0; // Init simil
        if (sSuggestion.equals("")) {
            Iterator iIter = CategoryValues.keySet().iterator();
            while (iIter.hasNext()) {
                String sCurCategory = (String)iIter.next();
                double dCurSimilarity = ((Double)CategoryValues.get(sCurCategory)).doubleValue();
                if (dCurSimilarity > dMaxSimil) {
                    sSuggestion = sCurCategory;
                    dMaxSimil = dCurSimilarity;
                }
            }
        }
        
        // If uncertainty is high
        double dUncertainty = UncertaintyCalculator.computeUncertainty(CategoryValues, dMaxSimil, sSuggestion);        
        if (dUncertainty > LookupThreshold) {
            // DEBUG LINES
            System.out.println("High uncertainty. Looking up for similar decision.");
            //////////////
            double dMinDistance = Double.MAX_VALUE;
            Iterator iIter = vPreviousDecisions.iterator();
            DecisionSupport dpSecondarySuggestion = null;
            while (iIter.hasNext()) {
                DecisionSupport dp = (DecisionSupport)iIter.next();
                double dCurDist = dp.distanceFrom(CategoryValues, sSuggestion);
                if ((dMinDistance > dCurDist) && (dCurDist < Double.MAX_VALUE)) {
                    dpSecondarySuggestion = dp;
                    dMinDistance = dCurDist;
                }
            }
            // If a decision has been found
            double dFoundUncertainty = 0;
            if (dpSecondarySuggestion != null) {
                // If the noted uncertainty before the error
                // was at most as high as this
                dFoundUncertainty = EntropyUncertaintyCalculator.computeUncertainty(dpSecondarySuggestion.CategoryEstimations, 
                    ((Double)dpSecondarySuggestion.CategoryEstimations.get(dpSecondarySuggestion.SuggestedCategory)).doubleValue() ,
                    dpSecondarySuggestion.SuggestedCategory);
                if (dFoundUncertainty <= dUncertainty) {
                    // Then USE IT
                    sSuggestion = dpSecondarySuggestion.CorrectCategory;
                    dUncertainty = dUncertainty / dFoundUncertainty;    // Uncertainty reduced
                    // DEBUG LINES
                    System.out.println("Found decision " + dpSecondarySuggestion.CorrectCategory+ " for " + 
                            CategoryValues.toString());
                    //////////////
                }
            }
            
        }
        return new Decision(null, sSuggestion, 1 - dUncertainty, CategoryValues);
    }
    
}

/** Class of objects representing the facts for a given decision concerning an instance, as well as the 
 * correctness of the decision.*/
class DecisionSupport {
    /** A set of category membership estimations (for the given sample). */
    public TreeMap CategoryEstimations;
    /** The original classification result. */
    public String SuggestedCategory;
    /** The final (corrected) classification result. */
    public String CorrectCategory;
    
    /** Create a DecisionSupport object for a set of membership estimations, an originally suggested category and a final
     * category.
     *@param hCategoryEstimations The membership estimations for a set of categories.
     *@param sSuggestedCategory The originally suggested category.
     *@param sCorrectCategory The final category.
     */
    public DecisionSupport(Map hCategoryEstimations, String sSuggestedCategory, String sCorrectCategory) {
        CategoryEstimations = new TreeMap();
        
        CategoryEstimations.putAll(hCategoryEstimations);
        SuggestedCategory = new String(sSuggestedCategory);
        CorrectCategory = new String(sCorrectCategory);
    }
    
    /** Returns whether there is an estimation for a given category in the set of estimations.
     *@param sCategory The category for which we seek an estimation.
     *@return A boolean indicating whether there is an estimation for the given category.
     */
    public boolean contains(String sCategory) {
        return CategoryEstimations.containsKey(sCategory);
    }

    /** Returns the Euclidean distance, in the space of membership estimations for a set of categories, between a given estimation
     *map and the object estimation map. The function only looks for estimations that returned the same original estimation.
     *
     *@param hNewEstimations The estimations to find the distance from.
     *@param sCategory The originally suggested category.
     *@return The distance measure.
     */
    public double distanceFrom(Map hNewEstimations, String sCategory) {
        // Only use if same category is to be picked
        if (!SuggestedCategory.equals(sCategory))
            return Double.MAX_VALUE;
        
        double dRes = 0;
        Iterator iIter = hNewEstimations.keySet().iterator();
        while (iIter.hasNext()) {
            String sCategoryName = (String)iIter.next();
            double dCategoryEstimation = ((Double)hNewEstimations.get(sCategoryName)).doubleValue();
            double dLocal = 0.0;
            if (CategoryEstimations.containsKey(sCategoryName))
                dLocal = ((Double)CategoryEstimations.get(sCategoryName)).doubleValue();
            else
                return Double.MAX_VALUE;    // Not a match
            
            dRes += Math.pow((dLocal - dCategoryEstimation), 2); // Euclidean distance
        }
        return dRes;
    }
}

/** A utility class to calculate uncertainty in decision. */
class UncertaintyCalculator {
    public static double computeUncertainty(Map CategoryValues, double dMaxSimilarity, String sSelectedCategory) {
        Iterator iIter = CategoryValues.keySet().iterator();
        double dMaxUncertainty = 0;
        // Get 1st higher value

        // Calc sum
        while (iIter.hasNext()) {
            String sCur = (String)iIter.next();
            // Ignore selected category
            if (!sSelectedCategory.equals(sCur)) {
                // Ignore lower similarity categories
                if (dMaxSimilarity < ((Double)CategoryValues.get(sCur)).doubleValue())
                    // Find maximum uncertainty
                    dMaxUncertainty = Math.max(dMaxUncertainty, 
                            ((Double)CategoryValues.get(sCur)).doubleValue() / dMaxSimilarity);
            }
        }
        return dMaxUncertainty;
    }
}

/** A utility class to calculate entropy-based uncertainty in decision. */
class EntropyUncertaintyCalculator extends UncertaintyCalculator {
    public static double computeUncertainty(Map CategoryValues, double dMaxSimilarity, String sSelectedCategory) {
        Iterator iIter = CategoryValues.keySet().iterator();

        double dOverallEntropy = 0;    
        while (iIter.hasNext()) {
            String sCur = (String)iIter.next();
            dOverallEntropy += -((Double)CategoryValues.get(sCur)).doubleValue() * 
                    utils.logX(((Double)CategoryValues.get(sCur)).doubleValue(), 2); // Augment entropy
        }
        
        return dOverallEntropy;
    }
}
