/*
 * SpectralSpellInteroperator.java
 *
 * Created on 27 Φεβρουάριος 2007, 5:00 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author ggianna
 */
public class SpectralSpellInteroperator {
    private String SSpellExecutable = "lib/sspell_train";
    private String InputFile;
    private String[] Args;
    
    public SpectralSpellInteroperator(String sInputFile) {
        this(sInputFile, "-modes 1 -d 1".split(" "));
    }
    
    public SpectralSpellInteroperator(String sExecutable, String sInputFile) {
        this(sInputFile, "-modes 1 -d 1".split(" "));
        SSpellExecutable = sExecutable;
    }
    
    /** Creates a new instance of SpectralSpellInteroperator */
    public SpectralSpellInteroperator(String sExecutable, String sInputFile, String[] sArgs) {
        this(sInputFile, sArgs);
        SSpellExecutable = sExecutable;
    }
    
    /** Creates a new instance of SpectralSpellInteroperator */
    public SpectralSpellInteroperator(String sInputFile, String[] sArgs) {
        InputFile = sInputFile;
        Args = sArgs;
    }
    
    public TreeMap execute() {
        TreeMap tmRes = new TreeMap();
        
        try {
            // Redirect R output to a stream, to check results
            ArrayList alCmd = new ArrayList();
            alCmd.add(SSpellExecutable);
            alCmd.add("-in");
            alCmd.add(InputFile);
            
            alCmd.addAll(Arrays.asList(Args));
            
            ProcessBuilder pbP = new ProcessBuilder(alCmd);
            
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
            BufferedReader br =
                new BufferedReader(new InputStreamReader(isIn));
            
            // read output lines from command
            String sStr = "";
            boolean bStartOfVectors = false;
            while ((sStr = br.readLine()) != null)
            {
                // DEBUG LINES
                // System.err.println(sStr);
                //////////////
                
                // Check for feature vector size line, indicative of vector start
                if (sStr.matches("\\d+")) {
                    bStartOfVectors = true;
                    continue;
                }
                
                // If we have not reached it, continue...
                if (!bStartOfVectors)
                    continue;
                
                // else, get StringRepresentation data
                StringRepresentation sr = readVectorLine(sStr);
                // and put it into the map
                tmRes.put(sr.getKey(), sr.getValue());
            }
        }
        catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return tmRes; // Failure
        }
            
        return tmRes;
    }
    
    private StringRepresentation readVectorLine(String sLine) {
        TreeMap tLineToks = new TreeMap();
        StringRepresentation sRes = null;

                
        // Split by spaces
        String[] sTokens = sLine.split(" ");
        Iterator iTok = Arrays.asList(sTokens).iterator();
        while (iTok.hasNext()) {
            String sCurTok = (String)iTok.next();
            // If feature description, add to vector
            if (sCurTok.matches("\\d+:\\d+")) {
                String[] saVecData = sCurTok.split(":");
                tLineToks.put(saVecData[0], saVecData[1]);
            }
            else
            {
                sRes = new StringRepresentation(sCurTok);
            }
        }
        
        // Even accept null
        if (sRes == null)
            sRes = new StringRepresentation("");
        
        sRes.setValue(tLineToks);
        
        return sRes;
    }
    
    public static void main(String[] sArgs) {
        SpectralSpellInteroperator s = new SpectralSpellInteroperator("lib/test.txt");
        s.execute();
    }
}

class StringRepresentation implements Entry<String,TreeMap> {
    private String sKey;
    private TreeMap tmVector;
    
    StringRepresentation(String sk) {
        sKey = sk;
        tmVector = new TreeMap();
    }
    
    public String getKey() {
        return sKey;
    }

    public TreeMap getValue() {
        return tmVector;
    }

    public TreeMap setValue(TreeMap value) {
        TreeMap tmRes = tmVector;
        tmVector = (TreeMap)value;
        // Return old value
        return tmVector;
    }
    
}