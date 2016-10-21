/*
 * TextSpectralSpellPreprocessor.java
 *
 * Created on 27 Φεβρουάριος 2007, 4:49 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.events;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import gr.demokritos.iit.jinsect.interoperability.SpectralSpellInteroperator;

/**
 *
 * @author ggianna
 */
public class TextSpectralSpellPreprocessor extends TextPreprocessorAdapter {
    protected Hashtable hFoundWords;
    protected TreeMap tWords;
    protected String[] Args; 
    protected Semaphore sWordMapSem;
    /** Creates a new instance of TextSpectralSpellPreprocessor */
    public TextSpectralSpellPreprocessor(String sArgs) {
        tWords = null; // Init to null
        hFoundWords = new Hashtable();
        sWordMapSem = new Semaphore(1);
        Args = sArgs.split(" ");
    }
    
    public String preprocess(String sStr) {
        // Split text to words
        String[] saRes = gr.demokritos.iit.jinsect.utils.splitToWords(sStr);
        StringBuffer sb = new StringBuffer();
        try {
            sWordMapSem.acquire();
            // DEBUG LINES
            // System.err.println("Acquiring semaphore...");
            //////////////
            
            // Create word replacement map if needed
            if (tWords == null)
                createWordReplacementMap();

            // For every word
            Iterator iWords = Arrays.asList(saRes).iterator();
            while (iWords.hasNext()) {
                String sCurWord=(String)iWords.next();
                // Replace it with its hash 
                String sHash = tWords.get(sCurWord).toString();
                sb.append(sHash.replaceAll("\\W", "A")).append(" ");
            }                    
        }
        catch (InterruptedException ie) {
            ie.printStackTrace(System.err);
        }
        finally {
            sWordMapSem.release();
        }
        
        // Return replaced text
        return sb.toString();
    }
    
    public void addDocument(String sDoc) {
        tWords = null; // Reset replacement map
        String[] saRes = gr.demokritos.iit.jinsect.utils.splitToWords(sDoc);
        Iterator iWords = Arrays.asList(saRes).iterator();
        while (iWords.hasNext()) {
            String sCurWord=(String)iWords.next();
            hFoundWords.put(sCurWord,1.0);
        }
    }
    
    private void createWordReplacementMap() {
        try {
            // Create output file
            File fTmp = File.createTempFile("ssinput","txt");
            PrintStream pOut = new PrintStream(fTmp);
            
            // For every word
            Iterator iWords = hFoundWords.keySet().iterator();
            while (iWords.hasNext()) {
                String sCur = (String)iWords.next();
                // Print word in a new line
                pOut.println(sCur);
            }
            pOut.flush();            
            pOut.close();
            
            // Call spectral spell to extract feature vector
            SpectralSpellInteroperator ssi;
            if (Args.length == 0)
                ssi = new SpectralSpellInteroperator(fTmp.toString());
            else {
                // DEBUG LINES
                // System.out.println("Using args:" + jinsect.utils.printList(Arrays.asList(Args)));
                //////////////
                ssi = new SpectralSpellInteroperator(fTmp.toString(), Args);
            }
            
            tWords = ssi.execute();
            fTmp.delete();
        }
        catch (IOException ioe) {
            ioe.printStackTrace(System.err);
            return;
        }
        
    }
}
