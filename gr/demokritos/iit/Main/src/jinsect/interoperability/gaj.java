/*
 * gaj.java
 *
 * Created on 1 Φεβρουάριος 2007, 12:31 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.io.File;
import java.io.IOException;
import gr.demokritos.iit.jinsect.console.resultAssessor;
import gr.demokritos.iit.jinsect.console.summaryEvaluator;

/**
 *
 * @author ggianna
 */
public class gaj {
    
    /** Creates a new instance of gaj */
    public gaj() {
    }
    
    public static void main(String sArgs[]) {
        System.err.println("The result was:" + gaj.evalCharParams(3,5,3)); // Best 7,8,6
    }
    
    public static double evalWordParams(int iWordMin, int iWordMax, int iWordDist) {
        // Check for results subdir
        File fRes = new File("results/");
        if (!fRes.exists()) {
            if (!fRes.mkdir())
                System.err.println("Cannot create results dir. Aborting...");
                return Double.NEGATIVE_INFINITY;
        }
        
        try {
            // Create output file
            File fTemp = File.createTempFile("sumeval",".table");

            // Eval summaries
            String []sParams = {"-wordMin=" + iWordMin, "-wordMax="+iWordMax, "-wordDist="+iWordDist,
                "-do=word", "-o="+fTemp.getAbsoluteFile(), "-t=2", "-NOTs"}; // TODO: Change back to -s
            summaryEvaluator.main(sParams);
            // Calc overall results and correlation to responsiveness
            String[] sAssessorParams = {"-insectFile="+fTemp.getAbsoluteFile(), "-respFile=responsiveness.table", 
                "-dirPrefix=" + fRes.getPath() + "/", "-do=word", "-s"};
            resultAssessor.main(sAssessorParams);
            fTemp.delete();
            
        }
        catch (IOException ioe) {
            System.err.println("Cannot create results file. Aborting...");
            return Double.NEGATIVE_INFINITY;
            
        }
        System.err.println("Normalized value: " + normalizeResult(resultAssessor.LastExecRes));
        return normalizeResult(resultAssessor.LastExecRes);
    }
    
    public static double evalCharParams(int iCharMin, int iCharMax, int iCharDist) {
        // Check for results subdir
        File fRes = new File("results/");
        if (!fRes.exists()) {
            if (!fRes.mkdir())
                System.err.println("Cannot create results dir. Aborting...");
                return Double.NEGATIVE_INFINITY;
        }
        
        try {
            // Create output file
            File fTemp = File.createTempFile("sumeval",".table");

            // Eval summaries
            String []sParams = {"-charMin=" + iCharMin, "-charMax="+iCharMax, "-charDist="+iCharDist,
                "-do=char", "-o="+fTemp.getAbsoluteFile(), "-t=2", "-NOTs"}; // TODO: Change back to -s
            summaryEvaluator.main(sParams);
            // Calc overall results and correlation to responsiveness
            String[] sAssessorParams = {"-insectFile="+fTemp.getAbsoluteFile(), "-respFile=responsiveness.table", 
                "-dirPrefix=" + fRes.getPath() + "/", "-do=char", "-s"};
            resultAssessor.main(sAssessorParams);
            fTemp.delete();
            
        }
        catch (IOException ioe) {
            System.err.println("Cannot create results file. Aborting...");
            return Double.NEGATIVE_INFINITY;
            
        }
        
        System.err.println("Normalized value: " + normalizeResult(resultAssessor.LastExecRes));
        return normalizeResult(resultAssessor.LastExecRes);
    }
    
    public static double normalizeResult(double dInp) {
        return 1000.0*(Math.exp(20.0*(dInp)-20.0)); // Return execution result, exponentially modified and
            // Normalized to 1000
    }
}
