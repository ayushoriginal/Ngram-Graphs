/*
 * LocalWordNetMeaningExtractor.java
 *
 * Created on 10 Ιανουάριος 2007, 2:05 μμ
 *
 */

package gr.demokritos.iit.conceptualIndex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.ArrayOfDefinition;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.Definition;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.Dictionary;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;

/** This class uses locally installed <a href='http://wordnet.princeton.edu/'>WordNet</a> 
 * to extract word definitions.
 *
 * @author ggianna
 */
public class LocalWordNetMeaningExtractor implements IMeaningExtractor {
    
    /** Creates a new instance of LocalWordNetMeaningExtractor, checking there is a reachable 
     * WordNet instance installed.
     *@throws IOException If WordNet is not found an appropriate {@link IOException} is thrown.
     *@see IOException
     */
    public LocalWordNetMeaningExtractor() throws IOException {
        Process p = Runtime.getRuntime().exec("wordnet");
        try {
            p.waitFor();
        }
        catch (InterruptedException iee) {
            // Ignore
            return;
        }
        
        if (p.exitValue() == 0)
        {
             throw new IOException("WordNet cannot be found and executed.");
        }
    }
    
    /** Looks up a word using a locally installed WordNet instance.
     *@return The definition of the word being looked up.
     *@see WordDefinition
     */
    public WordDefinition getMeaning(String sString) {
        // Create definition object and add definitions
        WordDefinition wd = new WordDefinition();
        wd.setWord(sString);
        ArrayOfDefinition aodDefs = new ArrayOfDefinition();
        List lDefinitions = new ArrayList();
        String sDefinition = "";
        try {
            String[] saCmd = {"wordnet",sString,"-over"};
            // DEBUG LINES
            // System.out.println("Executing: " + saCmd.toString());
            //////////////
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
        
            BufferedReader br =
                new BufferedReader(new InputStreamReader(isIn));
    
            // read output lines from command
    
            String str;
            while ((str = br.readLine()) != null) {
                // Only add definition lines
                if (str.matches("\\d+[.]\\s+([(]\\d+[)])*.+")) {
                    // Add definition line, removing idices and so forth
                    //sDefinition += str.replaceAll("\\d+[.]\\s+[(]\\d+[)]", "");
                    sDefinition = str.replaceAll("\\d+[.]\\s+[(]\\d+[)]", "");
                    Definition dTmp = new Definition();
                    dTmp.setWord(sString);
                    dTmp.setDictionary(new Dictionary());
                    dTmp.setWordDefinition(sDefinition);
                    lDefinitions.add(dTmp);
                }
            }
    
            // wait for command to terminate
    
            try {
                p.waitFor();
            }
            catch (InterruptedException e) {
                System.err.println("process was interrupted");
            }
    
            // check its exit value
//            if (p.exitValue() != 0)
//                System.err.println("exit value was non-zero");
    
            // close stream
    
            br.close();                

            isIn.close();

            if (sDefinition.length() == 0)
                return null; // No definition found
        }
        catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return null; // Failure
        }
        aodDefs.getDefinition().addAll(lDefinitions);
        wd.setDefinitions(aodDefs);                        
        
        return wd;
    }
    
    /**
     * Returns the gloss of a given word, for a given part-of-speech and a 
     * predefined sense number.
     * @param sWord The word looked up.
     * @param sPOS Can be <b>v</b>erb, <b>n</b>oun, <b>a</b>djective or
     *  <b>r</b>adverb.
     * @param iSenseNum The sense number of interest.
     * @return A string containing the gloss part of the given sense number, for
     * the given word and part-of-speech.
     */
    public String getGloss(String sWord, String sPOS, int iSenseNum) {
        String sGloss = "";
        try {
            String[] saCmd = {"wordnet", sWord,"-syns" + sPOS, 
                "-n" + String.valueOf(iSenseNum), "-g"};
            // DEBUG LINES
            // System.out.println("Executing: " + saCmd.toString());
            //////////////
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
        
            BufferedReader br =
                new BufferedReader(new InputStreamReader(isIn));
    
            // read output lines from command
    
            String str;
            while ((str = br.readLine()) != null) {
                // Only add definition lines
                if ((str.indexOf("--") > 0) && (str.indexOf("=>") < 0)) {
                    // Check definition line for gloss
                    if (str.indexOf("\"") > 0)
                        sGloss = str.substring(str.indexOf("\""), 
                        str.lastIndexOf("\"") + 1);
                }
            }
    
            // wait for command to terminate
            try {
                p.waitFor();
            }
            catch (InterruptedException e) {
                System.err.println("process was interrupted");
            }
    
            // check its exit value
//            if (p.exitValue() != 0)
//                System.err.println("exit value was non-zero");
    
            // close stream
            br.close();                
            isIn.close();

            if (sGloss.length() == 0)
                return null; // No definition found
        }
        catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return null; // Failure
        }
        
        return sGloss;
    }
    
    /**
     * Returns the definition of a given word, for a given part-of-speech and a 
     * predefined sense number.
     * @param sWord The word looked up.
     * @param sPOS Can be <b>v</b>erb, <b>n</b>oun, <b>a</b>djective or
     *  <b>r</b>adverb.
     * @param iSenseNum The sense number of interest.
     * @return A string containing the definition part of the given sense number, 
     * for the given word and part-of-speech.
     */
    public String getDefinition(String sWord, String sPOS, int iSenseNum) {
        String sDefinition = "";
        try {
            String[] saCmd = {"wordnet", sWord,"-syns" + sPOS, 
                "-n" + String.valueOf(iSenseNum), "-g"};
            // DEBUG LINES
            // System.out.println("Executing: " + saCmd.toString());
            //////////////
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
        
            BufferedReader br =
                new BufferedReader(new InputStreamReader(isIn));
    
            // read output lines from command
    
            String str;
            while ((str = br.readLine()) != null) {
                // Only add definition lines
                if ((str.indexOf("--") > 0) && (str.indexOf("=>") < 0)) {
                    // Check definition line for gloss
                    if (str.indexOf("(") > 0)
                        if (str.indexOf("; \"") > 0)
                            sDefinition = str.substring(str.indexOf("(") + 1,
                                    str.indexOf("; \""));
                        else
                            sDefinition = str.substring(str.indexOf("(") + 1,
                                str.lastIndexOf(")"));
                }
            }
    
            // wait for command to terminate
            try {
                p.waitFor();
            }
            catch (InterruptedException e) {
                System.err.println("process was interrupted");
            }
    
            // check its exit value
//            if (p.exitValue() != 0)
//                System.err.println("exit value was non-zero");
    
            // close stream
            br.close();                
            isIn.close();

            if (sDefinition.length() == 0)
                return null; // No definition found
        }
        catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return null; // Failure
        }
        
        return sDefinition;
    }

    /**
     * Returns the synonym words of a given word, for a given part-of-speech and a 
     * predefined sense number.
     * @param sWord The word looked up.
     * @param sPOS Can be <b>v</b>erb, <b>n</b>oun, <b>a</b>djective or
     *  <b>r</b>adverb.
     * @param iSenseNum The sense number of interest.
     * @return A string containing the synonyms part of the given sense number, 
     * for the given word and part-of-speech, as the synonyms appear if the 
     * gloss parameter is given to the WordNet executable.
     */
    public String getSenseWords(String sWord, String sPOS, int iSenseNum) {
        String sSynonyms = "";
        try {
            String[] saCmd = {"wordnet", sWord,"-syns" + sPOS, 
                "-n" + String.valueOf(iSenseNum), "-g"};
            // DEBUG LINES
            // System.out.println("Executing: " + saCmd.toString());
            //////////////
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
        
            BufferedReader br =
                new BufferedReader(new InputStreamReader(isIn));
    
            // read output lines from command
    
            String str;
            while ((str = br.readLine()) != null) {
                // Only add definition lines
                if (str.indexOf("--") > -1) {
                    // Check definition line for gloss
                    if (str.indexOf("=>") > -1)
                        sSynonyms += str.substring(str.indexOf("=>") + 2,
                                str.indexOf(" -- ")) + " ";
                    else
                        sSynonyms += str.substring(0, str.indexOf("--")) + " ";
                }
            }
    
            // wait for command to terminate
            try {
                p.waitFor();
            }
            catch (InterruptedException e) {
                System.err.println("process was interrupted");
            }
    
            // DEBUG LINES
            // check its exit value
            //if (p.exitValue() != 0)
                //System.err.println("exit value was non-zero");
            //////////////
    
            // close stream
            br.close();                
            isIn.close();

            if (sSynonyms.length() == 0)
                return null; // No definition found
        }
        catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return null; // Failure
        }
        
        return sSynonyms;
    }
}
