/*
 * CooccurenceToTextConverter.java
 *
 * Created on October 8, 2007, 4:55 PM
 *
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;

/**
 * @author ggianna
 */
public class CooccurenceToTextConverter {   
    /**
     *Takes a directory and converts the (text) files it contains into co-occurence indicative text files.
     *Expects the main directory to contain subdirectories indicating categories of texts.
     *Outputs the files to a second directory with the same structure and names as the source directory.
     */
    public static void main(String[] args) {
        Hashtable hmCmd = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        
        String sBaseDir = gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "fromDir", "data");
        String sOutputDir = gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "toDir", "newdata");
        int iMinNGram = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "minN", "1")).intValue();
        int iMaxNGram = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "maxN", "1")).intValue();
        int iDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "dist", "1")).intValue();
        String sUseMap = gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "useMap", "");
        String sSaveMapTo = gr.demokritos.iit.jinsect.utils.getSwitch(hmCmd, "saveMapTo", "");
        
        Hashtable hmEdges=null;
        try {
            FileInputStream fsIn = new FileInputStream(sUseMap);
            ObjectInputStream ois = new ObjectInputStream(fsIn);
            if (sUseMap.length() > 0)
                hmEdges = (Hashtable)ois.readObject();
            else
                hmEdges = new Hashtable();
            fsIn.close();
            ois.close();
        } catch (FileNotFoundException ex) {
            System.err.println(sUseMap + " not found. Continuing using an empty map.");
            hmEdges = new Hashtable();
        } catch (IOException ex) {
            System.err.println(sUseMap + " cannot be read. Continuing using an empty map.");
            hmEdges = new Hashtable();
            ex.printStackTrace(System.err);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
            return;
        }
        
        DocumentSet ds = new DocumentSet(sBaseDir, 1.0);
        ds.createSets();
        DocumentNGramGraph dngCur = new DocumentNGramGraph(iMinNGram,iMaxNGram,iDist);
        Iterator iIter = ds.getTrainingSet().iterator();
        while (iIter.hasNext()) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
            dngCur.setDataString("");
            try {
                dngCur.loadDataStringFromFile(cfeCur.getFileName());
                File fDir = new File(sOutputDir + System.getProperty("file.separator") + cfeCur.getCategory());
                fDir.mkdir();
                File f = new File(cfeCur.getFileName());
                String sTarget = sOutputDir + System.getProperty("file.separator") + 
                        cfeCur.getCategory() + System.getProperty("file.separator") + f.getName();
                FileWriter fOut = new FileWriter(sTarget);
                fOut.write(dngCur.toCooccurenceText(hmEdges));
                fOut.close();
                
                System.err.print(".");
            } catch (IOException ex) {
                System.err.println("Could not convert file " + cfeCur.getFileName());
                ex.printStackTrace(System.err);
            }
        }
        System.err.println();
        if (sSaveMapTo.length() > 0) {
            try {
                FileOutputStream fo = new FileOutputStream(sSaveMapTo);
                ObjectOutputStream oos = new ObjectOutputStream(fo);
                oos.writeObject(hmEdges);
                fo.close();
                oos.close();
            } catch (FileNotFoundException ex) {
                System.err.println("Could not save hashmap.");
                ex.printStackTrace(System.err);
            } catch (IOException ex) {
                System.err.println("Could not save hashmap.");
                ex.printStackTrace(System.err);
            }
        }
    }
}
