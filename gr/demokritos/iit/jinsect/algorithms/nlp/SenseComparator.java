/*
 * SenseComparator.java
 *
 * Created on 15 Ιανουάριος 2007, 2:46 μμ
 *
 */

package gr.demokritos.iit.jinsect.algorithms.nlp;

import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;

/** Attempts to compare two senses, based on the similarity of their descriptions given a 
 *semantic resource (e.g. WordNet). Test class.
 *
 * @author ggianna
 */
public class SenseComparator {
    
    /** Creates a new instance of SenseComparator */
    public SenseComparator() {
    }
    
    /** Used for testing purposes.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DocumentSet dsSet = new DocumentSet("/home/ggianna/JInsect/summaries", 1.0);
        dsSet.createSets();
        if (dsSet.getTrainingSet().size() == 0)
        {
            System.err.println("Empty document set...");
            System.exit(-1);
        }
        // Simple text
        System.out.println("@relation jinsect\n");
        System.out.println("@attribute GraphCooccurenceSimilarity real");
        System.out.println("@attribute GraphValueSimilarity real");
        System.out.println("@attribute GraphSizeSimilarity real");
        System.out.println("@attribute HistogramContainmentSimilarity real");
        System.out.println("@attribute HistogramValueSimilarity real");
        System.out.println("@attribute HistogramSizeSimilarity real");
        System.out.println("@attribute OverallSimilarity real");
        // N-grams
        System.out.println("@attribute CharGraphCooccurenceSimilarity real");
        System.out.println("@attribute CharGraphValueSimilarity real");
        System.out.println("@attribute CharGraphSizeSimilarity real");
        System.out.println("@attribute NHistogramContainmentSimilarity real");
        System.out.println("@attribute NHistogramValueSimilarity real");
        System.out.println("@attribute NHistogramSizeSimilarity real");
        System.out.println("@attribute NOverallSimilarity real");
        System.out.println("@attribute IsSame {TRUE,FALSE}\n");

        System.out.println("@data");
        
        HashMap hmCache = new HashMap();
        HashMap hmNCache = new HashMap();
        
        int iTotal = dsSet.getTrainingSet().size();
        int iCur = 0;
        
        StandardDocumentComparator sdcComparator = new StandardDocumentComparator();
        Iterator iIter = dsSet.getTrainingSet().iterator();
        while (iIter.hasNext()) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
            SimpleTextDocument ndDoc1 = new SimpleTextDocument();
            NGramDocument ndNDoc1 = new NGramDocument();
            if (hmCache.containsKey(cfeCur.getFileName())) {
                ndDoc1 = (SimpleTextDocument)hmCache.get(cfeCur.getFileName());
                ndNDoc1 = (NGramDocument)hmNCache.get(cfeCur.getFileName());
            }
                
            else {
                ndDoc1.loadDataStringFromFile(cfeCur.getFileName());
                ndNDoc1.loadDataStringFromFile(cfeCur.getFileName());
                // hmCache.put(cfeCur.getFileName(), ndDoc1);
                // hmNCache.put(cfeCur.getFileName(), ndNDoc1);
            }
            
            Iterator iOtherIter = dsSet.getTrainingSet().subList(dsSet.getTrainingSet().indexOf(cfeCur), 
                    dsSet.getTrainingSet().size()).iterator();
            while (iOtherIter.hasNext()) {
                CategorizedFileEntry cfeOther = (CategorizedFileEntry)iOtherIter.next();
                SimpleTextDocument ndDoc2 = new SimpleTextDocument();
                NGramDocument ndNDoc2 = new NGramDocument();
                if (hmCache.containsKey(cfeOther.getFileName())) {
                    ndDoc2 = (SimpleTextDocument)hmCache.get(cfeOther.getFileName());
                    ndNDoc2 = (SimpleTextDocument)hmCache.get(cfeOther.getFileName());
                }
                else {
                    ndDoc2.loadDataStringFromFile(cfeOther.getFileName());
                    ndNDoc2.loadDataStringFromFile(cfeOther.getFileName());
                    // hmCache.put(cfeOther.getFileName(), ndDoc2);
                    // hmNCache.put(cfeOther.getFileName(), ndNDoc2);
                }
                try {
                        GraphSimilarity sSimil;
                        sSimil = sdcComparator.getSimilarityBetween(ndDoc1, ndDoc2);
                        
                        /* sSimil.setCalculator(new CalculatorAdapter() {
                           public double Calculate(Object oCaller, Object oCalculationParams) {
                               fouble fCont, fVal;
                               GraphSimilarity sLocalSimil = (GraphSimilarity)oCaller;
                               fCont = 0.5 + (0.4 * );
                               fVal = 0.9 * (sLocalSimil.SizeSimilarity);
                               
                               return fVal * sLocalSimil.ValueSimilarity + 
                                       fCont * sLocalSimil.ContainmentSimilarity;
                           }
                         }); */
                    
                        GraphSimilarity sSimil2;
                        sSimil2 = sdcComparator.getSimilarityBetween(ndNDoc1, ndNDoc2);
                        
                        /* sSimil2.setCalculator(new CalculatorAdapter() {
                           public double Calculate(Object oCaller, Object oCalculationParams) {
                               GraphSimilarity sLocalSimil = (GraphSimilarity)oCaller;
                               return sLocalSimil.ValueSimilarity * sLocalSimil.ContainmentSimilarity;
                           }
                         }); */
                         
                    System.out.print(sdcComparator.getGraphSimilarity().ContainmentSimilarity + "," +
                            sdcComparator.getGraphSimilarity().ValueSimilarity + "," +
                            sdcComparator.getGraphSimilarity().SizeSimilarity + "," +
                            sdcComparator.getHistogramSimilarity().ContainmentSimilarity + "," +
                            sdcComparator.getHistogramSimilarity().ValueSimilarity + "," +
                            sdcComparator.getHistogramSimilarity().SizeSimilarity + "," + 
                            sSimil.getOverallSimilarity() + ",");
                    System.out.println(sdcComparator.getGraphSimilarity().ContainmentSimilarity + "," +
                            sdcComparator.getGraphSimilarity().ValueSimilarity + "," +
                            sdcComparator.getGraphSimilarity().SizeSimilarity + "," +
                            sdcComparator.getHistogramSimilarity().ContainmentSimilarity + "," +
                            sdcComparator.getHistogramSimilarity().ValueSimilarity + "," +
                            sdcComparator.getHistogramSimilarity().SizeSimilarity + "," + 
                            sSimil.getOverallSimilarity() + "," +
                            (((cfeOther.getCategory() == cfeCur.getCategory()) || 
                                (cfeCur.getFileName().substring(cfeCur.getFileName().lastIndexOf("/")) == 
                                cfeCur.getFileName().substring(cfeCur.getFileName().lastIndexOf("/")))) ?  "TRUE" : "FALSE"));
                }
                catch (InvalidClassException iceE) {
                    System.err.println("Cannot compare...");
                }
                
            }
            System.err.println("Completed " + (double)iCur++ / iTotal * 100 + "%");
        }
    }
    
}
 