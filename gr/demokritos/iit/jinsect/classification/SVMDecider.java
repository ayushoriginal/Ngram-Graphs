/*
 * SVMDecider.java
 *
 * Created on 14 Φεβρουάριος 2006, 11:00 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.classification;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeSet;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextCategory;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.structs.Decision;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import salvo.jesus.graph.WeightedEdge;

/** This class describes objects that use Support Vector Machine (SVM) mechanics, in order to 
 * classify texts to given categories. The class uses LibSVM implementation of SVMs 
 * (see <a href='http://www.csie.ntu.edu.tw/~cjlin/libsvm/'>LibSVM</a> for more).
 * @author PCKid
 */
public class SVMDecider extends Decider {
    /** The SVM model produced. */
    svm_model Model;    
    /** The list of categories. */
    ArrayList CategoryList;
    
    HashMap EdgeMapping = new HashMap();
    /** Default value for SVM tolerance. */
    private double dTolerance = 1e-3;
    /** Default value for SVM cost. */
    private double dCost = 100;    
    /** Default value for SVM gamma. */
    private double dGamma = 10;
    
    /**
     * Creates a new instance of SVMDecider, given an {@link INSECTDB} repository of data.
     */
    public SVMDecider(INSECTDB dbRepository) {
        super(dbRepository);
        CategoryList = new ArrayList();
    }
    
    private void createSVMModel(double dTolerance, double dCost, double dGamma) {
        // For all categories
        svm_problem spProblem = new svm_problem();
        CategoryList.clear();
        CategoryList.addAll(Arrays.asList(Repository.getObjectList(Repository.CATEGORY_TYPE)));
        spProblem.l = CategoryList.size(); // No of categories
        
        // Init array of feture (node) lists
        spProblem.x = new svm_node[spProblem.l][];
        // Init array of expected outcome
        spProblem.y = new double[spProblem.l];
        
        // For every category
        ListIterator iIter = CategoryList.listIterator();
        int iCnt = 0;
        while (iIter.hasNext()) {
            // Load it
            String sCatName = (String)iIter.next();
            SimpleTextCategory cCurCat = (SimpleTextCategory)Repository.loadObject(sCatName, Repository.CATEGORY_TYPE);
            
            // Add it in the array of lists
            spProblem.x[iCnt] = documentToNodeArray(cCurCat);
            // Add category in the output Set
            spProblem.y[iCnt++] = (double)iIter.previousIndex();
            // DEBUG LINES
            //System.out.println(String.format("Category '%s' with index %d", sCatName, iIter.previousIndex()));
            //////////////
        }
        // Complete the problem description
        svm_parameter spParam = new svm_parameter();
        // default values
        spParam.svm_type = svm_parameter.C_SVC;
        spParam.kernel_type = svm_parameter.RBF;
        spParam.degree = 0;
        spParam.gamma = dGamma;
        spParam.coef0 = 0;
        spParam.nu = 0.5;
        spParam.cache_size = 100;
        spParam.C = dCost;
        spParam.eps = dTolerance;
        spParam.p = 0.1;
        spParam.shrinking = 0;
        spParam.probability = 0;
        spParam.nr_weight = 0;
        spParam.weight_label = new int[0];
        spParam.weight = new double[0];

        String error_msg = svm.svm_check_parameter(spProblem,spParam);

        if(error_msg != null)
        {
                System.err.print("Error: "+error_msg+"\n");
               return;
        }

        // Create train file
        System.out.println("Creating train file...");
        
        try {
            FileWriter fw = new FileWriter("train.dat");
            for (int iLine = 0; iLine < spProblem.l; iLine++) {
                fw.write(String.valueOf(spProblem.y[iLine]) + " ");
                for (int iElem = 0 ; iElem < spProblem.x[iLine].length; iElem++) {

                    fw.write(String.valueOf(spProblem.x[iLine][iElem].index) + ":" + 
                            String.valueOf(spProblem.x[iLine][iElem].value) + " ");
                    if ((iElem % 1000 == 0)) System.out.println(String.valueOf(iElem) + " elements complete out of" +
                            String.valueOf(spProblem.x[iLine].length));
                }
                fw.write("\n");
                if ((iLine % 1000 == 0)) System.out.println(String.valueOf(iLine) + " lines complete out of" +
                        String.valueOf(spProblem.l));
            }
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        // Create model
        try {
            // Create model file
            System.out.println("Creating model file...");
            Process p = (new ProcessBuilder("svmtrain.exe","-s " + String.valueOf(spParam.svm_type), 
                    "-t " + String.valueOf(spParam.kernel_type), 
                    "-g " + String.valueOf(dGamma),
                    //"-p " + String.valueOf(spParam.eps), 
                    //"-c " + String.format("%.5f", spParam.C), 
                    "-v 10",
                     "train.dat", "model.dat")).start();
            
            InputStreamReader reader =                // step 3
                new InputStreamReader ( p.getInputStream () );

              BufferedReader buf_reader =
                 new BufferedReader (reader );        // step 4.

              String line;
              while ((line = buf_reader.readLine ()) != null)
                           System.out.println (line);
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
            return;
        }
        
        // Create model file
        System.out.println("All created OK.");
        // Create model
        Model = svm.svm_train(spProblem, spParam);        
    }
    
    /** Suggests a category for a given document.
     *@param dDoc The document to classify.
     *@return A {@link Decision} upon the classification of the given document.
     */
    protected Decision suggestCategory(SimpleTextDocument dDoc) {
        SimpleTextDocument stdTemp = new SimpleTextDocument();
        // DO NOT Use category to filter datastring to valid words        
        stdTemp.setDataString(dDoc.getTempDataString());
        // Check model
        if (Model == null) // If dirty
            createSVMModel(dTolerance, dCost, dGamma);
        
        if (Model == null) // If failed
            return new Decision(dDoc, "", 1.0, new HashMap());
        
        //double dPrediction = svm.svm_predict(Model, documentToNodeArray(stdTemp));
        double dPrediction = -1;
        svm_node[] naDoc = documentToNodeArray(stdTemp);
        // Create train file
        try {
            FileWriter fw = new FileWriter("test.dat");
            String sLine = String.valueOf(0) + " ";
            for (int iElem = 0 ; iElem < naDoc.length; iElem++) {

                sLine += String.valueOf(naDoc[iElem].index) + ":" + 
                        String.valueOf(naDoc[iElem].value) + " ";
            }
            fw.write(sLine + "\n");
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            int iRes=-1;
            Process p = (new ProcessBuilder("svmpredict.exe", "test.dat", "model.dat", "output")).start();
            InputStreamReader reader =                // step 3
                new InputStreamReader ( p.getInputStream () );

            BufferedReader buf_reader =
             new BufferedReader (reader );        // step 4.

            String line;
            while ((line = buf_reader.readLine ()) != null)
                //System.out.println (line);
                ;
            
            //try {iRes = p.exitValue(); } catch (IllegalThreadStateException e) { Thread.sleep(500); }
            FileReader fRes = new FileReader("output");
            BufferedReader brOutput = new BufferedReader(fRes);
            String sCur = brOutput.readLine();
            if (sCur != "")
                dPrediction = Double.valueOf(sCur);
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
            dPrediction = -1;
        }
        
        try {
            String sCategory = (String)CategoryList.get((int)dPrediction);
            Decision dRes = new Decision(dDoc, sCategory, 1.0, new HashMap());            
            return dRes;
        }
        catch (Exception e) {
            Decision dRes = new Decision(dDoc, "", 1.0, new HashMap()); // Failed
            return dRes;
        }
    }
    
    public void addEvidence(Decision dPrv, String sFinalCategory) {
        String sSuggestedCategory = (String)dPrv.FinalDecision;
        SimpleTextDocument dDoc = (SimpleTextDocument)dPrv.Document;

        // Apply temporary datastring to merge
        // Sync data string as needed
        if (!dDoc.getTempDataString().equals(dDoc.getDataString()))
            dDoc.applyTempDataString();

        if (sFinalCategory != null)
        {
            if (Repository.existsObject(sFinalCategory, Repository.CATEGORY_TYPE)) {
                boolean bCorrectedOK = false;

                int iIterations = 0;
                while (!bCorrectedOK) {
                    // Update selected category
                    SimpleTextCategory cCat =(SimpleTextCategory)Repository.loadObject(sFinalCategory, Repository.CATEGORY_TYPE);
                    cCat.mergeWith(dDoc);                
                    // Replace existing object
                    Repository.saveObject(cCat, sFinalCategory, Repository.CATEGORY_TYPE);
                    Model = null; // Reset to null - dirty
                    bCorrectedOK = true;
                    /* DO NOT UPDATE MODEL FOR TIME BEING
                    // Update model
                    createSVMModel(dTolerance, dCost, dGamma);
                    bCorrectedOK = this.suggestCategory(dDoc).equals(sFinalCategory);
                    if (!bCorrectedOK) {
                        if ((++iIterations > 10) || (dCost * 10 > 1e12) || 
                                (dTolerance / 10 < 1e-12)) {
                            System.out.println("Failed to converge.");
                            break;
                        }
                        else {
                            System.out.println(String.format("Attempting retrain with Cost %E and Tolerance %E",
                                    dCost, dTolerance));
                            dCost *= 1.1;
                            dTolerance /= 1.1;
                            dGamma *= 1.5;
                        }*/
                         // DO NOT MODIFY ERRONEOUS CATEGORY
                        if (!sFinalCategory.equals(sSuggestedCategory))
                        {
                            SimpleTextCategory cErrCat =(SimpleTextCategory)Repository.loadObject(sSuggestedCategory, Repository.CATEGORY_TYPE);
                            if (cErrCat != null) {
                                cErrCat.rejectDocument(dDoc);
                                // Save object
                                Repository.saveObject(cErrCat, sSuggestedCategory, Repository.CATEGORY_TYPE);
                            }
                        }
                         /*
                    }
                    */
                }

            }
            else {
                SimpleTextCategory cCat = new SimpleTextCategory(sFinalCategory);
                cCat.setDataString(dDoc.getDataString());
                Repository.saveObject(cCat, sFinalCategory, Repository.CATEGORY_TYPE);
                Model = null; // Reset to null - dirty
                // Update model
                //createSVMModel(dTolerance, dCost, dGamma);
            }                    

        }

    }

    /** Provides an svm_node array structure, derived from a text document, in order to use the LibSVM
     * functions.
     *@param dDoc The text document to represent.
     *@return An svm_node array structure representing the given document.
     */
    protected svm_node[] documentToNodeArray(SimpleTextDocument dDoc) {
        // Use treeset for correct feature ordering
        TreeSet tsNodes = new TreeSet();

        // For every level in it extract edges
        DocumentNGramGraph dg = dDoc.getDocumentGraph();
        for (int iCnt = dg.getMinSize(); iCnt <= dg.getMaxSize(); iCnt++) {
            UniqueVertexGraph g = dg.getGraphLevelByNGramSize(iCnt);
            // Get category graph edges
            Iterator edgeIter = g.getEdgeSet().iterator();
            // For every edge
            while (edgeIter.hasNext()) {
                WeightedEdge we = (WeightedEdge)edgeIter.next();
                if (dg.degredationDegree(we) > 3)
                    continue;
                // Create edge node
                svm_ordered_node snCur = new svm_ordered_node();
                String sData = (we.getVertexA().getLabel() + " " + we.getVertexB().getLabel());
                // Add mapping for edge feature
                if (!EdgeMapping.containsKey(sData))
                    EdgeMapping.put(sData, EdgeMapping.size() + 1);
                snCur.index = ((Integer)EdgeMapping.get(sData)).intValue();
                snCur.value = we.getWeight();

                tsNodes.add(snCur);
            }                
        }

        // Add it in the array of lists
        svm_node[] snaTemp = new svm_node[tsNodes.size()];
        Iterator iIter = tsNodes.iterator();
        int iCnt = 0;
        while (iIter.hasNext()) {
            snaTemp[iCnt++] = (svm_node)iIter.next();
        }
                
        return snaTemp;
    }
    
    public void reset() {
        Model = null;
        CategoryList.clear();
        EdgeMapping.clear();
        super.reset();    
    }

}

/** Local class, providing comparable svm_nodes, to use in TreeMap.
 */
class svm_ordered_node extends svm_node implements Comparable {
    public int compareTo(Object o) {
        svm_node n = (svm_node)o;
        return index - n.index;
    }
    
    public String toString() {
        return String.format("(%d, %f)", index, value);
    }
}