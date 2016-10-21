/*
 * CASCDistanceCalculator.java
 *
 * Created on April 3, 2007, 2:32 PM
 *
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 * @author ggianna
 */
public class CASCDistanceCalculator {

    protected String PathToCasc = "CASCDist";
    /** Creates a new instance of CASCDistanceCalculator */
    public CASCDistanceCalculator() throws IOException {
        
        Process p = Runtime.getRuntime().exec(PathToCasc);
        try {
            p.waitFor();
        }
        catch (InterruptedException iee) {
            // Ignore
            return;
        }
    }
    
    /** Creates a new instance of CASCDistanceCalculator */
    public CASCDistanceCalculator(String sPathToCasc) throws IOException {
        PathToCasc = sPathToCasc;
        
        Process p = Runtime.getRuntime().exec(PathToCasc);
        try {
            p.waitFor();
        }
        catch (InterruptedException iee) {
            // Ignore
            return;
        }
    }
    
    public double getDistanceFromRandom(String sCorrectString) {
        return getDistanceFromDOTGraph(sCorrectString, null);
    }
    
    public double getDistanceFromDOTGraph(String sCorrectString, String sDotString) {
        
        // Create temp dot file for correct graph
        File fCorrectTmp = null;
        if (sCorrectString != null) {
            try {
                fCorrectTmp = File.createTempFile("tmpCorrect", ".dot");
                FileWriter fwOut = new FileWriter(fCorrectTmp);
                BufferedWriter bf = new BufferedWriter(fwOut);
                
                bf.write(sCorrectString);
                bf.flush();
                bf.close();
                fwOut.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                return Double.NEGATIVE_INFINITY;
            }
        }
         
        ArrayList<String> alParams = new ArrayList();
        alParams.add(PathToCasc);
        alParams.add(fCorrectTmp.getPath());
        
        // Create secondary temp dot file if needed
        File fTmp = null;
        if (sDotString != null) {
            try {
                fTmp = File.createTempFile("tmp", ".dot");
                FileWriter fwOut = new FileWriter(fTmp);
                BufferedWriter bf = new BufferedWriter(fwOut);
                alParams.add(fTmp.getPath());
                
                bf.write(sDotString);
                bf.flush();
                bf.close();
                fwOut.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                return Double.NEGATIVE_INFINITY;
            }
        }
         
        String[] saCmd = new String[alParams.size()];
        saCmd = alParams.toArray(saCmd);
        ProcessBuilder pbP = new ProcessBuilder(saCmd);
        Process p;
        try {
            p = pbP.start();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return Double.NEGATIVE_INFINITY;
        }
        InputStream isIn = p.getInputStream();        
        BufferedReader br =
            new BufferedReader(new InputStreamReader(isIn));

        // read output lines from command
        String sStr = "";
        String sStrPrv = "";
        
        try {
            
            while ((sStr = br.readLine()) != null)
            {
                // Look for result line
                if (!sStr.matches("\\d+[:]\\d+\\s+[:]+\\s+\\d+\\s+\\(\\d+[.]\\d+[%]\\)")) {
                    sStrPrv = sStr;
                    continue;
                }
                    
                String sRes = sStr.substring(sStr.lastIndexOf("(") + 1, sStr.lastIndexOf("%)"));
                return Double.valueOf(sRes).doubleValue() / 100.0;
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return Double.NEGATIVE_INFINITY; // Error occured
        }
        
        // Clear temp files
        if (sDotString != null) {
            fCorrectTmp.delete();
            fTmp.delete();
        }
        
        return Double.NEGATIVE_INFINITY; // Should not reach this point. Output was incomplete.
    }
    
}
