/*
 * resultAssessor.java
 *
 * Created on January 19, 2007, 12:23 PM
 *
 */

package gr.demokritos.iit.jinsect.console;

import java.util.Hashtable;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.interoperability.RInteroperator;
import gr.demokritos.iit.jinsect.structs.Correlation;

/** Assesses the output of system performance given a set of performances for specific documents
 * and topics.
 *
 * @author pckid
 */
public class resultAssessor {
    public static double LastExecRes = Double.NEGATIVE_INFINITY;
    public static String OVERALL_OUTPUT_FILENAME = "jinsect.All.MacroAverage.txt";
    
    private static void printUsage() {
            System.err.println("Syntax:\nresultAssessor "+
                    "[-insectFile=jinsect.table] [-respFile=responsiveness.table] [-dirPrefix=results/] -do=(words|chars|all)");
            System.err.println("insectFile=jinsect.table\tFile containing jinsect data.\n" + 
                    "respFile=responsiveness.table\tFile containing responsiveness data.\n" +
                    "dirPrefix=results/\tBase result directory.\n" + 
                    "-do=(char|word|all)\tIndicates whether char/word/all kinds of n-grams " +
                        "will be taken into account.\n" +
                    "-getPerformance=(average|min|max)\tIndicates whether the average, " +
                        "best or worst performance of a system will represent its overall " +
                        "performance per subject.\n" +
                    "-noCorrelationCheck\tDo not use R application for correlation computation.\n" +
                    "-noExtractionOfAverages\tDo not extract averages to files.\n" +
                    "-noResponsiveness\tDo not check responsiveness.");
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // String sJInsectFile = "results/jinsect.table";   
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        if (gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"?", "").length() > 0) {
            printUsage();
            System.exit(0);
        }
        
        String sPrefix = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"dirPrefix", "results/");
        String sInsFile = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"insectFile", sPrefix+"jinsect.table");
        String sRespFile = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"respFile", sPrefix+"responsiveness.table");
        String sDo = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"do", "all");
        String sGetPerformance = gr.demokritos.iit.jinsect.utils.getSwitch(
                hSwitches,"getPerformance", "average");
        boolean bCompareToResponsiveness = !Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(
                hSwitches,"noResponsiveness", String.valueOf(false)));
        boolean bExtractAverages = !Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(
                hSwitches,"noExtractionOfAverages", String.valueOf(false)));
        boolean bCorrelationCheck = !Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(
                hSwitches,"noCorrelationCheck", String.valueOf(false)));
        
        
        if ((sDo.length() == 0) || ("char_word_all__".indexOf(sDo) % 5 != 0))
        {
            // Invalid or undefined method
            printUsage();
            System.exit(0);
        }
        if ((sGetPerformance.length() == 0) || ("average_min_max_".indexOf(sGetPerformance+"_") < 0))
        {
            // Invalid or undefined method
            printUsage();
            System.exit(0);
        }
        boolean bSilent=gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "s", "FALSE").equals("TRUE");        
        
        if (!bSilent)
            System.err.println("Using parameters:\n" + hSwitches);
        
        String sJInsectFile = sInsFile;
        String sResponsivenessFile = sRespFile;
        TreeMap tmTheme = new TreeMap();
        TreeMap tmSystem;
        
        String[] sAxis={};
        if (sDo.equals("char")) {
            String[] sUsedAxis = {
                "CharGraphCooccurence", "CharGraphValue", "CharGraphSize", 
                "NHistoContainment",
                "NHistoValue", "NHistoSize", "NOverallSimil",
                "Responsiveness"};
            sAxis = sUsedAxis;
        } else if (sDo.equals("word"))
        {
            String[] sUsedAxis = {
                "GraphCooccurence", "GraphValue", "GraphSize", "HistoContainment",
                "HistoValue", "HistoSize", "OverallSimil", 
                "Responsiveness"};
            sAxis = sUsedAxis;
        } else if (sDo.equals("all")) {
            String[] sUsedAxis = {
                "GraphCooccurence", "GraphValue", "GraphSize", "HistoContainment",
                "HistoValue", "HistoSize", "OverallSimil", 
                "CharGraphCooccurence", "CharGraphValue", "CharGraphSize", 
                "NHistoContainment",
                "NHistoValue", "NHistoSize", "NOverallSimil",
                "Responsiveness"
            };
            sAxis = sUsedAxis;
        };
        
        // Reads and averages data from all axes
        TreeMap hmSystemAverages = new TreeMap();
        
        //if (JOptionPane.showConfirmDialog(null,"Do you wish to extract averages?") == JOptionPane.YES_OPTION)
        if (bExtractAverages)
        // For all axes
        for (int iCnt=0;iCnt < sAxis.length; iCnt++) {
            String sCurAxis = sAxis[iCnt];
            tmTheme.clear();
            if (sCurAxis.equals("Responsiveness")) {
                if (bCompareToResponsiveness)
                    tmTheme = GetDataFromFile(sResponsivenessFile, tmTheme, 0, 3, 4, null, true);
                else
                    continue;
            }
            else
            if (sCurAxis.indexOf("Rouge") > -1)
                continue; // Ignore rouge*
            else
                tmTheme = GetDataFromFile(sJInsectFile, tmTheme, 0, 1, iCnt + 2, null, true);
            
            PrintStream pOut;
            try {
                pOut = new PrintStream(new FileOutputStream(sPrefix + "jinsect." + sCurAxis + ".Average.txt"));
                //pOut = new PrintStream(new FileOutputStream("results/responsiveness.Average.txt"));
            }
            catch (IOException ioe) {
                System.err.println("Cannot write to output file..." + ioe.getMessage());
                ioe.printStackTrace(System.err);
                return; // Quit
            }
            // Get average
            Iterator iThemeIter = tmTheme.keySet().iterator();
            while (iThemeIter.hasNext()) {
                String sCurTheme = (String)iThemeIter.next();
                tmSystem = (TreeMap)tmTheme.get(sCurTheme);
        
                
                Iterator iSysIter = tmSystem.keySet().iterator();
                while (iSysIter.hasNext()) {
                    String sCurSysID = (String)iSysIter.next();
                    
                    // Store average into System Averages hash.
                    Distribution dCur;
                    if (hmSystemAverages.containsKey(sCurSysID)) {
                        dCur = (Distribution)hmSystemAverages.get(sCurSysID);
                    }
                    else
                    {
                        dCur = new Distribution();
                        hmSystemAverages.put(sCurSysID, dCur);
                    }
                    // Select min, max of average performance as indicative performance
                    if (sGetPerformance.equals("min"))
                        dCur.setValue(dCur.asTreeMap().size(),
                            ((Distribution)tmSystem.get(sCurSysID)).minValue());
                    else if (sGetPerformance.equals("max"))
                        dCur.setValue(dCur.asTreeMap().size(),
                            ((Distribution)tmSystem.get(sCurSysID)).maxValue());
                    else
                        dCur.setValue(dCur.asTreeMap().size(),
                            ((Distribution)tmSystem.get(sCurSysID)).average(true));
                }
            }                        
            // Print out average of scores
            Iterator iSysIter = hmSystemAverages.keySet().iterator();
            while (iSysIter.hasNext()) {
                String sCurSysID = (String)iSysIter.next();
                Distribution dCurSystemAverageScores = (Distribution)hmSystemAverages.get(sCurSysID);
                //System.out.println(sCurSysID + "\t" + dCurSystemAverageScores.average(true));
                pOut.println(sCurSysID + "\t" + dCurSystemAverageScores.average(true));
            }
            hmSystemAverages.clear();
        }
        
        
        //if (JOptionPane.showConfirmDialog(null,"Do you wish to continue?") == JOptionPane.NO_OPTION)
            //return;
        
        String sThemeHeader = "SysID\t"; // Init header line
        tmTheme.clear();
        // For all axes
        for (int iCnt=0;iCnt < sAxis.length; iCnt++) {
            String sCurAxis = sAxis[iCnt];
            
            if (sCurAxis.equals("Responsiveness")) {
                if (bCompareToResponsiveness)
                    tmTheme = GetDataFromFile(sPrefix + "jinsect." + sCurAxis + ".Average.txt", tmTheme,
                    -1, 0, 1, sCurAxis, false); // Ignore theme
                else
                    continue;
            }
            else
            //tmTheme = GetDataFromFile("results/jinsect." + sCurAxis + ".MacroAverage.txt", tmTheme, 0, 1, 2, sCurAxis, false);
                tmTheme = GetDataFromFile(sPrefix + "jinsect." + sCurAxis + ".Average.txt", tmTheme,
                    -1, 0, 1, sCurAxis, false); // Ignore theme
            
            
            sThemeHeader += sCurAxis;
            if (iCnt < sAxis.length - 1)
                sThemeHeader += "\t";
        }
        
        PrintStream pOut;
        try {
            pOut = new PrintStream(new FileOutputStream(sPrefix + OVERALL_OUTPUT_FILENAME));
            //pOut = new PrintStream(new FileOutputStream("results/responsiveness.Average.txt"));
            //System.out.println(sThemeHeader);
            pOut.println(sThemeHeader); // Add header line
        }
        catch (IOException ioe) {
            System.err.println("Cannot write to output file..." + ioe.getMessage());
            ioe.printStackTrace(System.err);
            return; // Quit
        }
        // Output
        Iterator iThemeIter = tmTheme.keySet().iterator();
        while (iThemeIter.hasNext()) {
            String sCurTheme = (String)iThemeIter.next();
            tmSystem = (TreeMap)tmTheme.get(sCurTheme);
            Iterator iSysIter = tmSystem.keySet().iterator();
            while (iSysIter.hasNext()) {
                String sCurSysID = (String)iSysIter.next();
                // Output category values
                ////System.out.print(sCurTheme + "\t" + sCurSysID + "\t");                
                //pOut.print(sCurTheme + "\t" + sCurSysID + "\t");
                //System.out.print(sCurSysID + "\t");
                pOut.print(sCurSysID + "\t");
                
                //Iterator iCategory = ((Distribution)tmSystem.get(sCurSysID)).asTreeMap().keySet().iterator();
                for (int iCatCnt = 0; iCatCnt < sAxis.length; iCatCnt++) {
                    
                    String sCurCategory = sAxis[iCatCnt];
                    if (sCurCategory.equals("Responsiveness") && !bCompareToResponsiveness)
                        continue;
                    Double dVal = ((Distribution)tmSystem.get(sCurSysID)).getValue(sCurCategory);
                    String sVal;
                    if (dVal == null)
                        sVal = "NA";
                    else
                        sVal = String.format("%8.6f", dVal);
                    //System.out.print( sVal);
                    pOut.print(sVal);
                    if (iCatCnt != sAxis.length - 1)
                    {
                        //System.out.print("\t");
                        pOut.print("\t");
                    }
                }
                pOut.println();
            }

        }
        
        // Check correlation to Responsiveness
        if (bCorrelationCheck && bCompareToResponsiveness)
        try {
            RInteroperator r = new RInteroperator();
            Distribution dSpearman = new Distribution();
            Distribution dPearson = new Distribution();
            
            if (!bSilent)
                System.err.println("Metric\tPearson\tSpearman");
            for (int iCnt=0;iCnt<sAxis.length;iCnt++) {
                if (!sAxis[iCnt].equals("Responsiveness")) {
                    if (sAxis[iCnt].indexOf("Size") == -1) {
                        Correlation cCor = r.getCorrelationBetween(sPrefix + OVERALL_OUTPUT_FILENAME,
                            sAxis[iCnt],"Responsiveness");
                        // DEBUG LINES
                        if (!bSilent)
                            System.err.println(sAxis[iCnt] + "\t" + cCor.Pearson + "\t" + cCor.Spearman);
                        //////////////
                        dSpearman.setValue(sAxis[iCnt], cCor.Spearman);
                        dPearson.setValue(sAxis[iCnt], cCor.Pearson);
                    }
                }
            }
            // Extract averages
            double dCorr = (2 * dSpearman.maxValue() * dPearson.maxValue()) / 
                    (dSpearman.maxValue() + dPearson.maxValue());
            
            LastExecRes = dCorr; // Update last execution result
            System.out.println(dCorr);
        }
        catch (IOException ioe) {
            System.err.println("Cannot execute R. Aborting...");
            ioe.printStackTrace(System.err);
            System.exit(0);
        }
        
        
    }
    
    /** Reads DUC results files, as well as JInsect files. 
     *@param sDataFile The file to import.
     *@param tmTheme A treemap to hold per topic (theme) performances.
     *@param iThemeFieldIndex An indicator of the index of the topic (theme) field in the file 
     * fields.
     *@param iSysIDFieldIndex An indicator of the index of the system id field in the file 
     * fields.
     *@param iValFieldIndex An indicator of the index of the value (performance) field in the file 
     * fields.
     *@param sCategory An (optional) category for the results. Indicates no category, if null.
     *@param hasHeader If true indicates header line in file.
     *@return A {@link TreeMap} holding the results of the reading.
     */
    private static TreeMap GetDataFromFile(String sDataFile, TreeMap tmTheme, int iThemeFieldIndex,
            int iSysIDFieldIndex, int iValFieldIndex, String sCategory, boolean hasHeader) {
        
        TreeMap hmSystem;
        Double dVal;
        
        // Read 1st file
        try {
            FileReader frIn = new FileReader(sDataFile);
            BufferedReader bfIn = new BufferedReader(frIn);

            String sLine;
            if (hasHeader)
                sLine = bfIn.readLine(); // Ignore header line
            while ((sLine = bfIn.readLine()) != null)
            {
                String[] saArrayData = sLine.split("\t");
                if (saArrayData.length < Math.max(iThemeFieldIndex,Math.max(iSysIDFieldIndex, iValFieldIndex))) {
                    System.err.println("Line '" + sLine + "' is malformed. Ignoring...");
                    continue;
                }
                String sTheme = (iThemeFieldIndex >= 0) ? saArrayData[iThemeFieldIndex] : "";
                String sSysID = saArrayData[iSysIDFieldIndex];
                try {
                    dVal = Double.valueOf(saArrayData[iValFieldIndex]);
                }
                catch (Exception nfe) {
                    System.err.println("Could not translate " + sLine + "(" + iValFieldIndex + ") to double:" + nfe.getMessage());
                    //System.err.println("Could not translate " + saArrayData[iValFieldIndex] + " to double:" + nfe.getMessage());
                    nfe.printStackTrace(System.err);
                    continue; // Skip this iteration
                }
                
                // Create Theme - SystemID tree
                if (!tmTheme.containsKey(sTheme)) {
                    // Create new hash for system
                    hmSystem = new TreeMap();
                    // Put it under theme
                    tmTheme.put(sTheme, hmSystem);
                }
                else
                {
                    hmSystem = (TreeMap)tmTheme.get(sTheme);
                }
                
                Distribution dDist;
                if (!hmSystem.containsKey(sSysID)) {
                    // Create new distribution
                    dDist = new Distribution();
                    // Add distribution to System distribution of results
                    hmSystem.put(sSysID, dDist);
                }
                else {
                    dDist = (Distribution)hmSystem.get(sSysID);
                }
                
                if (sCategory != null)
                    dDist.setValue(sCategory, dVal); // Insert value as category result
                else
                    dDist.setValue(dDist.asTreeMap().size(), dVal); // Insert value as new result in distribution
            }            
        }
        catch (IOException ioe) {
            System.err.println("IO Problem:" + ioe.getMessage());
            ioe.printStackTrace(System.err);
            return null;
        }
        
        return tmTheme;
        
    }
    
    
}
