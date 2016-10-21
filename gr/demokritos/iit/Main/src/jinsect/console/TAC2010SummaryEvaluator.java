/*
 * test.java
 *
 * Created on May 4, 2007, 2:40 PM
 *
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.jinsect.gui.IStatusDisplayer;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.threading.ThreadList;
import gr.demokritos.iit.jinsect.utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Utility class for testing purposes only. */
public class TAC2010SummaryEvaluator {
    public static void main(String[] args) {
        Hashtable<String,String> hSwitches = utils.parseCommandLineSwitches(args);
        String sPeersDir = utils.getSwitch(hSwitches, "peersDir", "peers/");
        String sModelsDir = utils.getSwitch(hSwitches, "modelsDir", "peers/");
        boolean bNoModels = Boolean.valueOf(utils.getSwitch(hSwitches, "noModels",
                String.valueOf(Boolean.FALSE))).booleanValue();
        boolean bMerge = Boolean.valueOf(utils.getSwitch(hSwitches, "merge",
                String.valueOf(Boolean.FALSE))).booleanValue();
        boolean bSupportJacknifing = Boolean.valueOf(utils.getSwitch(hSwitches, "supportJack",
                String.valueOf(Boolean.FALSE))).booleanValue();

        IStatusDisplayer sdOut = new StreamOutputConsole(System.err, true);
        
        // Load document set
        DocumentSet dsPeers = new DocumentSet(sPeersDir, 1.0);
        dsPeers.createSets();
        DocumentSet dsModels = new DocumentSet(sModelsDir, 1.0);
        dsModels.createSets();

        int iAllFiles = dsPeers.getTrainingSet().size();
        int iCurFile = 0;
        Date dStart = new Date();
        ThreadList tl = new ThreadList();
        
        sdOut.setStatus(String.format("Starting... %s ", dStart.toString()), 0.0);
        // For each category
        for (String sCategory : (List<String>)dsPeers.getCategories()) {
            List<String> lCatFiles = DocumentSet.categorizedFileEntriesToStrings(
                    dsPeers.getFilesFromCategory(sCategory));
            // Get models
            List<String> lCandModelFiles = DocumentSet.categorizedFileEntriesToStrings(
                    dsModels.getFilesFromCategory(sCategory));
            ArrayList<String> lModelFiles = new ArrayList<String>();
            // Check and keep model files only
            for (String sCandModel : lCandModelFiles) {
                if (isModel(sCandModel))
                    lModelFiles.add(sCandModel);
            }

            // For each file
            for (String sCurFile : lCatFiles) {
                // Ignore model files, if requested
                if (bNoModels && isModel(sCurFile))
                    continue;
                
                // Create command line string
                final ArrayList<String> sArgs = new ArrayList<String>();
                sArgs.add("-summary="+sCurFile); // Summary file
                sArgs.add("-s"); // Silent
                sArgs.add("-prepend=" + (bNoModels ? "NoModels\t" : 
                    "AllPeers\t")); // Add per line
                if (bMerge) {
                    sArgs.add("-merge");
                    sArgs.add("-docClass=gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph");
                    sArgs.add("-compClass=gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator");
                }
                // If judging a model file
                if (isModel(sCurFile))
                    // Avoid self-comparison
                    sArgs.add("-avoidSelfComparison");
                // else if an automatic file
                else
                    if (bSupportJacknifing) // If jacknifing should be supported
                        if (!bNoModels) // If all peers
                            // Perform jackknifing
                            sArgs.add("-jack");
                // Add model files
                StringBuffer sbModels = new StringBuffer();
                for (String sCurModel: lModelFiles) {
                    sbModels.append(";" + sCurModel.trim());
                }
                sArgs.add("-models=" + sbModels.toString());
                while (!tl.addThreadFor(new Runnable() {

                    @Override
                    public void run() {
                        // Get summary
                        summarySingleFileEvaluator.main(
                                sArgs.toArray(new String[sArgs.size()]));
                    }
                }))
                    Thread.yield();
                
                if (iCurFile % 25 == 0) {
                    double dRemaining = (double)(iAllFiles - iCurFile);
                    double dPerFile = (double)(new Date().getTime() - dStart.getTime())
                            / (double)iCurFile;
                    sdOut.setStatus(String.format("Remaining %s. Complete (%%)",
                            utils.millisToMinSecString((long)(dPerFile *
                            dRemaining))),
                            1000 * iCurFile / iAllFiles / 10.0);
                }
                iCurFile++;
            }
        }
        try {
            tl.waitUntilCompletion();
        } catch (InterruptedException ex) {
            Logger.getLogger(TAC2010SummaryEvaluator.class.getName()).log(
                    Level.SEVERE, null, ex);
        }
        sdOut.setStatus("Finished." + new Date().toString(), 100);

    }

    protected static boolean isModel(String sFile) {
        return sFile.matches(".+[.][A-Z]");
    }

    private static boolean isPeer(String sFile) {
        return !isModel(sFile);
    }
}
