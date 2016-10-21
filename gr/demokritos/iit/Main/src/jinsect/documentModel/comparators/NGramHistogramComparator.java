/*
 * NGramHistogramComparator.java
 *
 * Created on 25 Ιανουάριος 2006, 2:13 πμ
 *
 */

package gr.demokritos.iit.jinsect.documentModel.comparators;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.utils;
/** An n-gram histogram comparison performing class, the compares DocumentNGramHistogram objects.
 *
 * @author PCKid
 */
public class NGramHistogramComparator implements SimilarityComparatorListener{
    
    /**
     * Creates a new instance of NGramHistogramComparator.
     */
    public NGramHistogramComparator() {
    }
    
    /***
     *Returns the similarity between two document n-gram histograms.
     *@param oFirst The first document n-gram histogram.
     *@param oSecond The second document n-gram histogram.
     *@return A {@link GraphSimilarity} object, giving a measure of similarity between the 
     * two graphs.
     *@see DocumentNGramHistogram
     ***/
    public GraphSimilarity getSimilarityBetween(Object oFirst, Object oSecond) {
        GraphSimilarity sSimil = new GraphSimilarity();
        DocumentNGramHistogram dnFirst = (DocumentNGramHistogram)oFirst;
        DocumentNGramHistogram dnSecond = (DocumentNGramHistogram)oSecond;
        
        int iTotalNodes = dnFirst.length();
        int iOtherTotalNodes = dnSecond.length();
        int iValidCnt = 0;
        
        java.util.Iterator iIter = dnFirst.NGramHistogram.keySet().iterator();
        while (iIter.hasNext())
        {
            double fSimil = 0.0;
            String iItm = (String)(iIter.next());
            if (dnSecond.NGramHistogram.containsKey(iItm))
            {
                sSimil.ContainmentSimilarity += 1.0 / utils.max(iTotalNodes, iOtherTotalNodes);
                double iFirstItem = ((Double)dnFirst.NGramHistogram.get(iItm)).doubleValue();
                double iSecondItem = ((Double)dnSecond.NGramHistogram.get(iItm)).doubleValue();

                if ((iFirstItem + iSecondItem) > 0)
                {
                    // Value difference metrics
                    fSimil = utils.min(iFirstItem, iSecondItem) /
                            utils.max(iFirstItem, iSecondItem);
                    iValidCnt += 1;
                    sSimil.ValueSimilarity += fSimil;
                }
            }
        }
            
                
        if (iValidCnt > 0)
            sSimil.ValueSimilarity /= iValidCnt;
        else
            sSimil.ValueSimilarity = 0;
        
        // Get node count similarity. Always <= 1        
        sSimil.SizeSimilarity = utils.min(iTotalNodes, iOtherTotalNodes) / 
                utils.max(iTotalNodes, utils.max(iOtherTotalNodes, 1));
        
        return sSimil;
    }
}
