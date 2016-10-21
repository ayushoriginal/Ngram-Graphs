/*
 * SVMDocDecider.java
 *
 * Created on 17 Φεβρουάριος 2006, 10:14 μμ
 *
 */

package gr.demokritos.iit.jinsect.classification;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextCategory;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.structs.Decision;
import libsvm.svm;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;


/** This class derives from {@link SVMDecider}, while at the same time holding every document it uses in 
 * training for future reference. This requires big amounts of memory, but the update is supposed to be better.
 *
 * @author PCKid
 */
public class SVMDocDecider extends SVMDecider {
    protected ArrayList DocumentList;
    /**
     * Creates a new instance of SVMDocDecider, given an {@link INSECTDB} repository.
     *@param dbRepository The repository to use.
     */
    public SVMDocDecider(INSECTDB dbRepository) {
        super(dbRepository);
        DocumentList = new ArrayList();
    }
    
    /**
     *Creates an SVM model according to input texts and returns a
     *hashmap containing the (index, CategoryName) mapping
     *@param dTolerance The tolerance of the SVM model.
     *@param dCost The cost of the SVM model.
     *@param dGamma The gamma of the SVM model.
     *@return A {@link HashMap} indicating which document uses in training corresponds to which category.
     */
    private HashMap createSVMModel(double dTolerance, double dCost, double dGamma) {
        // For all categories
        svm_problem spProblem = new svm_problem();
        spProblem.l = DocumentList.size(); // No of categories
        if (spProblem.l == 0) // If no instances were given
        {
            Model = null;
            return null ; // Cannot model
        }
            
        HashMap hCategories = new HashMap();
        
        // Init array of feture (node) lists
        spProblem.x = new svm_node[spProblem.l][];
        // Init array of expected outcome
        spProblem.y = new double[spProblem.l];
        
        // For every document
        ListIterator iIter = DocumentList.listIterator();
        int iCnt = 0;
        while (iIter.hasNext()) {
            // Load it
            Decision idDocInfo = (Decision)iIter.next();
            
            // Add it in the array of lists
            spProblem.x[iCnt] = documentToNodeArray((SimpleTextDocument)idDocInfo.Document);
            
            // Store category in category list, if needed
            if (!hCategories.containsKey(idDocInfo.FinalDecision))
                hCategories.put(idDocInfo.FinalDecision, hCategories.size() + 1);
            
            // Add decision to the output Set
            spProblem.y[iCnt++] = ((Integer)hCategories.get(idDocInfo.FinalDecision)).doubleValue();
            // DEBUG LINES
            //System.out.println(String.format("Category '%s' with index %d", sCatName, iIter.previousIndex()));
            //////////////
        }
        // Complete the problem description
        svm_parameter spParam = new svm_parameter();
        // default values
        spParam.svm_type = svm_parameter.C_SVC;
        spParam.kernel_type = svm_parameter.RBF;
        spParam.degree = 1;
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
               return null;
        }

        // Create train file
        try {
            FileWriter fw = new FileWriter("train.dat");
            for (int iLine = 0; iLine < spProblem.l; iLine++) {
                String sLine = String.valueOf(spProblem.y[iLine]) + " ";
                for (int iElem = 0 ; iElem < spProblem.x[iLine].length; iElem++) {

                    sLine += String.valueOf(spProblem.x[iLine][iElem].index) + ":" + 
                            String.valueOf(spProblem.x[iLine][iElem].value) + " ";
                }
                fw.write(sLine + "\n");
            }
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return hCategories;
        }
        
        // Create model
        try {
            Process p = (new ProcessBuilder("svmtrain.exe","-s 0", "-t 2", "-g " + String.valueOf(dGamma),
                    "-v 10", "train.dat", "model.dat")).start();
            
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
            return hCategories;
        }
                
        Model = svm.svm_train(spProblem, spParam);        
        return hCategories; // Return
    }
    
    public void addEvidence(Decision dPrv, String sFinalCategory) {
        String sSuggestedCategory = (String)dPrv.FinalDecision;
        SimpleTextDocument dDoc = (SimpleTextDocument)dPrv.Document;

        // Apply temporary datastring to merge
        // Sync data string as needed
        if (!dDoc.getTempDataString().equals(dDoc.getDataString()))
            dDoc.applyTempDataString();
        
        // Update category
        if (sFinalCategory != null)
        {
            if (Repository.existsObject(sFinalCategory, Repository.CATEGORY_TYPE)) {
                // Update selected category
                SimpleTextCategory cCat =(SimpleTextCategory)Repository.loadObject(sFinalCategory, Repository.CATEGORY_TYPE);
                cCat.mergeWith(dDoc);                
                // Replace existing object
                Repository.saveObject(cCat, sFinalCategory, Repository.CATEGORY_TYPE);
            }
            else {
                SimpleTextCategory cCat = new SimpleTextCategory(sFinalCategory);
                cCat.setDataString(dDoc.getDataString());
                Repository.saveObject(cCat, sFinalCategory, Repository.CATEGORY_TYPE);                
            }
        }

        DocumentList.add(new Decision(dDoc, sFinalCategory, 1.0, null));
    }
    
    protected Decision suggestCategory(SimpleTextDocument dDoc) {
        SimpleTextDocument stdTemp = new SimpleTextDocument();
        // DO NOT Use category to filter datastring to valid words        
        stdTemp.setDataString(dDoc.getTempDataString());
        // Create model
        HashMap hCats = createSVMModel(1e-3, 1e3, 10); // Default params
        // TODO: Remove remark
        // Check model
        if (Model == null)
            return new Decision(dDoc, "", 1.0, new HashMap());
        //////////////////////
        
        //double dPrediction = svm.svm_predict(Model, documentToNodeArray(stdTemp));
        double dPrediction;
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
                       System.out.println (line);
            //try {iRes = p.exitValue(); } catch (IllegalThreadStateException e) { Thread.sleep(500); }
            FileReader fRes = new FileReader("output");
            BufferedReader brOutput = new BufferedReader(fRes);
            String sCur = brOutput.readLine();
            dPrediction = Double.valueOf(sCur);
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
            dPrediction = -1;
        }
        
        Iterator iIter = hCats.keySet().iterator();
        // Init category
        String sCategory = "";
        while (iIter.hasNext()) {
            // Locate category
            String sCurCategory = (String)iIter.next();
            if (((Integer)hCats.get(sCurCategory)).doubleValue() == dPrediction) {
                sCategory = sCurCategory; // Found it
                break; // No more searching
            }
                
        }
        Decision dRes = new Decision(dDoc, sCategory, 1.0, new HashMap());
        
        return dRes;
    }
}
