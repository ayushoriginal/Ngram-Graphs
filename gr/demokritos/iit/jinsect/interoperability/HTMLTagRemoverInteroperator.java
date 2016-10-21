/*
 * HTMLTagRemoverInteroperator.java
 *
 * Created on July 13, 2007, 2:08 PM
 *
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class HTMLTagRemoverInteroperator {    
    String ProgPath = "/home/ggianna/bin/html2text.py";
    
    public HTMLTagRemoverInteroperator(String sPath) throws IOException {
        if (sPath != null)
            ProgPath = sPath;
        
        Process p = Runtime.getRuntime().exec(ProgPath + " null");
        try {
            p.waitFor();
        }
        catch (InterruptedException iee) {
            // Ignore
            return;
        }
    }
    
    public String removeTagsFromFile(String sFilename) {
        ArrayList<String> alParams = new ArrayList();
        alParams.add(ProgPath);
        alParams.add(sFilename);
        String[] saCmd = new String[alParams.size()];
        saCmd = alParams.toArray(saCmd);
        
        ProcessBuilder pbP = new ProcessBuilder(saCmd);
        Process p;
        try {
            p = pbP.start();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return null; // Error occured
        }
        InputStream isIn = p.getInputStream();        
        BufferedReader br =
            new BufferedReader(new InputStreamReader(isIn));

        // read output lines from command
        String sStr = "";
        StringBuffer sbRes = new StringBuffer();
        try {
            
            while ((sStr = br.readLine()) != null)
            {
                sbRes.append(sStr);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return null; // Error occured
        }
        
        return sbRes.toString();
    }
    
}
