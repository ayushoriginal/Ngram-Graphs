/*
 * Decider.java
 *
 * Created on 12 Φεβρουάριος 2006, 12:01 μμ
 *
 */

package gr.demokritos.iit.jinsect.classification;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.algorithms.statistics.statisticalCalculation;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
//import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextCategory;
//import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.events.CalculatorAdapter;
import gr.demokritos.iit.jinsect.indexing.NamedDocumentNGramGraph;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.structs.Decision;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import java.util.logging.Level;
import java.util.logging.Logger;

/** This class gathers data for a set of categories and can then perform classification over documents.
 * The category data can be updated at any point in time.
 * @author PCKid
 */
public class Decider {
    /** The number of documents per category so far. **/
    protected Distribution<String> CategoryEvidenceCount;

    //protected List Categories;
    /** The {@link INSECTDB} repository of data used. */
    protected INSECTDB Repository;
    
    /** Creates a new instance of Decider, given a repository of data.
     *@param dbRepository The repository to use.
     */
    public Decider(INSECTDB dbRepository) {
        //Categories = lCategories;
        Repository = dbRepository;
        CategoryEvidenceCount = new Distribution<String>();
    }
    
    /** Sets the repository of the Decider data 
     *@param dbRepository The repository to use.
     **/
    public void setRepository(INSECTDB dbRepository) {
        Repository = dbRepository;
    }
    
    /*
     *
     * Sets the category list of the Decider
     *@param lCategories The category list.
     *
    public void setCategories(List lCategories) {
        Categories = lCategories;
    }
     */
    
    /** Suggests a category for a given file.
     *@param sFilename The path to the file under review.
     *@return A {@link Decision} indicating the suggestion of category for the given file.
     **/
    public Decision suggestCategory(String sFilename) {
        // Load file
        DocumentNGramGraph dDoc = new DocumentNGramSymWinGraph();
        try {
            dDoc.loadDataStringFromFile(sFilename);
        } catch (IOException ex) {
            Logger.getLogger(Decider.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        // Suggest
        return suggestCategory(dDoc);
    }
    
    /** Suggests a category for the given document. Returns a Decision with the info
     *concerning the decision.
     *@param dDoc The document the category of which is to be determined.
     *@return A {@link Decision} indicating the suggestion of category for the given file.
     **/
    protected Decision suggestCategory(DocumentNGramGraph dDoc) {
        HashMap hResults = new HashMap();
        // For each category
        Iterator iIter = Arrays.asList(getAvailableCategories()).iterator();
        int iCnt = 0;
        String sSelectedCategory = null;
        double dMaxSimilarity = -1.0;
        Distribution<String> dEvidence = new Distribution<String>();
        
        while (iIter.hasNext())
        {
            // Load category
            NamedDocumentNGramGraph ic = (NamedDocumentNGramGraph)Repository.loadObject((String)iIter.next(),
                    INSECTDB.CATEGORY_TYPE);
            // If loaded OK
            if (ic != null) {
                // Create doc using the category dictionary
                // UpdateSecondaryStatus("Comparing document of size " + String.valueOf(dDoc.getTempDataString().length()) + " bytes.", (double)iCnt / Categories.length);
                
                NamedDocumentNGramGraph stdTemp = new NamedDocumentNGramGraph();
                // Use category to filter datastring to valid words
                stdTemp.setDataString(filterDataString(dDoc.getDataString(), ic));
                
                //stdTemp.getDocumentGraph().WordEvaluator = ic;
                //stdTemp.getDocumentHistogram().WordEvaluator = ic;
                
                // Get similarity
                double dCurSimilarity = finalSimilarityToCategory(stdTemp, ic);
                // Store in hash
                hResults.put(ic.getName(), dCurSimilarity); // FIRST put the numeric value to sort
                if (dCurSimilarity > dMaxSimilarity) {
                    sSelectedCategory = ic.getName();
                    dMaxSimilarity = dCurSimilarity;
                }
                dEvidence.setValue(ic.getName(), dCurSimilarity);
            }
            // UpdateSecondaryStatus("Compared to " + String.valueOf(++iCnt) + " categories.", (double)iCnt / Categories.length);
        }
        // Returns results if necessary
        double dEntropy = statisticalCalculation.entropy(
                dEvidence.getProbabilityDistribution());
        return new Decision(dDoc, sSelectedCategory,
                dEntropy == 0 ? 1.0 : Math.min(1.0, 1.0 / (Math.pow(2, dEntropy))),
                    hResults);
    }
    
    /** Filters a data string to keep only words concerning a single category.
     *@param sStr The string to filter.
     *@param cCat The category to use for filtering.
     *@return A string including only appropriate words from the original string.
     **/
    protected String filterDataString(String sStr, NamedDocumentNGramGraph cCat) {
        //return cCat.evaluateText(gr.demokritos.iit.jinsect.utils.splitToWords(sStr));
        return sStr;
    }

    /**Calculates similarity of a document to a selected category.
     *@param dDoc The document.
     *@param cCat The category to use.
     *@return A measure of similarity of a given text to a given category, as a double number.
     **/
    private double finalSimilarityToCategory(DocumentNGramGraph dDoc,
            NamedDocumentNGramGraph cCat) {
        
        NGramCachedGraphComparator dcComp = new NGramCachedGraphComparator();
        GraphSimilarity sSimil = null;
        sSimil = dcComp.getSimilarityBetween(cCat, dDoc);

         // DEFAULT
//        if (sSimil != null) 
//            return sSimil.getOverallSimilarity();
//        else
//            return 0.0;
        
        // Value and containment
        if (sSimil != null) {
            sSimil.setCalculator(new CalculatorAdapter() {
                @Override
               public double Calculate(Object oCaller, Object oCalculationParams) {
                   GraphSimilarity sLocalSimil = (GraphSimilarity)oCaller;
                   return sLocalSimil.ValueSimilarity / sLocalSimil.SizeSimilarity;
               }
             });
             return sSimil.getOverallSimilarity();
        }
        else
            return 0.0;
         
    }
    
    /** Updates the evidence of the determiner with new data, in order to correct erroneous decisions.
     *@param dDoc The document.
     *@param sFinalCategory The correct category for the selected document.
     **/
    public void addEvidence(DocumentNGramGraph dDoc, String sFinalCategory) {
        addEvidence(new Decision(dDoc, sFinalCategory, 1.0, new HashMap(0)), sFinalCategory);
    }
    
    /** Updates the evidence of the determiner with new data, with respect to a previous
     * decision.
     *@param dPrv The previous decision info.
     *@param sFinalCategory The correct category for the selected document.
     **/
    public void addEvidence(Decision dPrv, String sFinalCategory) {
        String sSuggestedCategory = (String)dPrv.FinalDecision;
        DocumentNGramGraph dDoc = (DocumentNGramGraph)dPrv.Document;
        
        // Apply temporary datastring to merge
        //UpdateSecondaryStatus("Merging in progress - Finalizing document analysis to merge into category", 0.0);
        
        // Sync data string as needed
        //if (!dDoc.getTempDataString().equals(dDoc.getDataString()))
        //    dDoc.applyTempDataString();

        if (sFinalCategory != null)
        {
            if (Repository.existsObject(sFinalCategory, INSECTDB.CATEGORY_TYPE)) {
                // Update selected category
                NamedDocumentNGramGraph cCat =
                        (NamedDocumentNGramGraph)Repository.loadObject(sFinalCategory,
                        INSECTDB.CATEGORY_TYPE);
                double dCatDocs = CategoryEvidenceCount.getValue(cCat.getName());
                // Update graph
                cCat.merge(dDoc, dCatDocs == 0 ? 1.0 : (dCatDocs / ++dCatDocs));
                // Update doc count for category
                CategoryEvidenceCount.increaseValue(cCat.getName(), 1.0);

                // TODO : Reuse pruning
                /*
                UpdateSecondaryStatus("Merging in progress - Pruning", 0.75);
                if ((Double.valueOf(PruningFactorEd.getText()) < 5) && 
                    (Double.valueOf(PruningFactorEd.getText()) > -5)) {
                    // DEBUG LINES
                    System.out.println("Before pruning " + String.valueOf(cCat.length()));
                    //////////////
                    cCat.prune(Double.valueOf(PruningFactorEd.getText())); // Using UI value
                    // DEBUG LINES
                    System.out.println("After pruning " + String.valueOf(cCat.length()));
                    //////////////
                } 
                 */
                // Replace existing object
                Repository.saveObject(cCat, sFinalCategory, INSECTDB.CATEGORY_TYPE);
            }
            else {
                NamedDocumentNGramGraph cCat = new NamedDocumentNGramGraph();
                cCat.setName(sFinalCategory);
                cCat.setDataString(dDoc.getDataString());
                Repository.saveObject(cCat, sFinalCategory, INSECTDB.CATEGORY_TYPE);
            }                    

            
            if (!sFinalCategory.equals(sSuggestedCategory))
            {
                NamedDocumentNGramGraph cCat =
                        (NamedDocumentNGramGraph)Repository.loadObject(
                        sSuggestedCategory, INSECTDB.CATEGORY_TYPE);
                if (cCat != null) {
                    cCat.degrade(dDoc);
                    // Save object
                    Repository.saveObject(cCat, sSuggestedCategory, INSECTDB.CATEGORY_TYPE);
                }
            }
            

        }
    }    
    
    /**Returns an array of the available category names from the repository used in the object.
     *@return An array of string category names.
     **/
    public String[] getAvailableCategories() {
        return Repository.getObjectList(INSECTDB.CATEGORY_TYPE);
    }
    
    /**Resets categories and all other data of the Decider.
     **/
    public void reset() {
        Iterator iIter = Arrays.asList(getAvailableCategories()).iterator();
        while (iIter.hasNext())
            Repository.deleteObject((String)iIter.next(), INSECTDB.CATEGORY_TYPE);
    }
}
