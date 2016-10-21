/*
 * SPOCKEval.java
 *
 * Created on July 13, 2007, 12:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author ggianna
 */
public class SPOCKEval {
    String EvalPath = "/downloads/Torrents/Data/Spock/spock_challenge/evaluate_clusters.py";
    /**
     * @param sPath the SPOCK path
     */
    public SPOCKEval(String sPath)  throws IOException {
        if (sPath != null)
            EvalPath = sPath;
        
        Process p = Runtime.getRuntime().exec(EvalPath);
        try {
            p.waitFor();
        }
        catch (InterruptedException iee) {
            // Ignore
            return;
        }
    }
    
    public double evaluate(String sClusters) {
        ArrayList<String> alParams = new ArrayList();
        alParams.add(EvalPath);

        // Create temp dot file for correct graph
        File fCorrectTmp = null;
        if (sClusters != null) {
            try {
                fCorrectTmp = File.createTempFile("tmpClusters", ".spock");
                FileWriter fwOut = new FileWriter(fCorrectTmp);
                BufferedWriter bf = new BufferedWriter(fwOut);
                
                bf.write(sClusters);
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
        alParams.add(fCorrectTmp.getPath());

        ProcessBuilder pbP = new ProcessBuilder(saCmd);
        Process p;
        try {
            p = pbP.start();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            fCorrectTmp.delete();
            return Double.NEGATIVE_INFINITY; // Error occured
        }
        InputStream isIn = p.getInputStream();        
        BufferedReader br =
            new BufferedReader(new InputStreamReader(isIn));

        // read output lines from command
        String sStr = "";
        String sRes = "";
        try {
            
            while ((sStr = br.readLine()) != null)
            {
                sRes += sStr;
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            fCorrectTmp.delete();
            return Double.NEGATIVE_INFINITY; // Error occured
        }
        
        fCorrectTmp.delete();
        return Double.valueOf(sRes);
    }
    
}
