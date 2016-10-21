/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cnevectorexperiments;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ggianna
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Get switches
        Hashtable<String, String> hSwitches = utils.parseCommandLineSwitches(args);
        String sFiles = utils.getSwitch(hSwitches, "files", "");
        final double dTrain = 0.5;
        // Init class index
        final HashMap<String, DocumentNGramGraph> hmClasses = new HashMap<String,
                DocumentNGramGraph>();
        final HashMap<String, List<String>> hmPerClassTrain =
                new HashMap<String, List<String>>();
        final HashMap<String, List<String>> hmPerClassTest =
                new HashMap<String, List<String>>();
        
        // Init class count
        final Distribution<String> dInstanceCounts = new Distribution<String>();
        
        if (sFiles.trim().length() == 0) {
            System.err.println("No files input.");
            return;
        }

        ExecutorService es = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());

        final String[] saFiles = sFiles.trim().split(";");
        // For each data file
        for (final String sCurFile : saFiles) {
            es.submit(new Runnable() {

                public void run() {
                    // Read file
                    // Ignore first line (header)
                    List<String> lsTmp = Arrays.asList(utils.loadFileToStringWithNewlines(
                            sCurFile).split("\n"));
                    final List<String> lsCurLines = lsTmp.subList(1, lsTmp.size());
                    // Shuffle instances
                    utils.shuffleList(lsCurLines);
                    // Split training and test set
                    final List<String> lTest = lsCurLines.subList((int)(dTrain * lsCurLines.size())
                            , lsCurLines.size());
                    hmPerClassTest.put(sCurFile, lTest);
                    List<String> lTrain = lsCurLines.subList(0, (int)(dTrain *
                            lsCurLines.size()));
                    // Update training and test lists
                    hmPerClassTest.put(sCurFile, lTest);
                    hmPerClassTrain.put(sCurFile, lTrain);

                    // For every training instance
                    for (String sCurInstance: lTrain) {
                        String sSeq = null;
                        // Read string
                        for (String sField : sCurInstance.split("\t")) {
                            if (sField.trim().toUpperCase().matches("(T*G*C*A*)+")) {
                                sSeq = sField;
                                break;
                            }
                        }
                        // If sequence not found
                        if (sSeq == null)
                        {
                            System.err.println("Sequence not found. Ignoring line:\n"
                                    + sCurInstance);
                            continue;
                        }
                        // Create seq graph
                        DocumentNGramGraph ngCur = new DocumentNGramSymWinGraph(1, 5,
                                5);
                        ngCur.setDataString(sSeq);
                        // Update class graph
                        String sClassName = sCurFile;
                        if (!hmClasses.containsKey(sClassName)) {
                            hmClasses.put(sClassName, new DocumentNGramSymWinGraph(1,
                                    5, 5));
                        }
                        double dClassCnt = dInstanceCounts.getValue(sClassName);
                        hmClasses.get(sClassName).merge(ngCur, 1.0 / (dClassCnt + 1.0));
                        dInstanceCounts.increaseValue(sClassName, 1.0);
                        System.err.print(".");
                    }
                    System.err.println(". Done " + sCurFile);
                }
            });
        }
        try {
            es.shutdown();
            es.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }


        // Init train output
        PrintStream psTrain = null;
        try {
            if (new File("train.csv").exists())
                new File("train.csv").delete();
            psTrain = new PrintStream("train.csv");
            // DONE: Update header based on classes
            psTrain.println(headerPerClasses(saFiles) + "\tInstanceID\tClass");
            ///////////////////////////////////////
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        // For each class
        for (String sCurFile : saFiles) {
            List<String> lTrain = hmPerClassTrain.get(sCurFile);

            // For every training instance
            for (String sCurInstance: lTrain) {
                String sClass = sCurFile;
                String sSeqName = null;
                String sSeq = null;
                // Read string
                for (String sField : sCurInstance.split("\t")) {
                    // Assign sequence name
                    if ((sSeqName == null) && (sField.trim().length() > 0))
                        sSeqName = sField.trim();

                    if (sField.trim().toUpperCase().matches("(T*G*C*A*)+")) {
                        sSeq = sField;
                        break;
                    }
                }
                // If sequence not found
                if (sSeq == null)
                {
                    System.err.println("Sequence not found. Ignoring line:\n"
                            + sCurInstance);
                    continue;
                }

                psTrain.println(utils.printIterable(
                        getDistances(hmClasses, sSeq).asTreeMap().values(),
                        ", ") + ", " + sSeqName + ", " + sClass);
            }

        }
        psTrain.close();

        // Init test output
        PrintStream psTest = null;
        try {
            if (new File("test.csv").exists())
                new File("test.csv").delete();
            psTest = new PrintStream("test.csv");
            // DONE: Update header based on classes
            psTest.println(headerPerClasses(saFiles) + "\tInstanceID\tClass");
            ///////////////////////////////////////
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        // For each class
        for (String sCurFile : saFiles) {
            List<String> lTest = hmPerClassTest.get(sCurFile);

            // For every test instance
            for (String sCurInstance: lTest) {
                String sClass = sCurFile;
                String sSeqName = null;
                String sSeq = null;
                // Read string
                for (String sField : sCurInstance.split("\t")) {
                    // Assign sequence name
                    if ((sSeqName == null) && (sField.trim().length() > 0))
                        sSeqName = sField.trim();
                    
                    if (sField.trim().toUpperCase().matches("(T*G*C*A*)+")) {
                        sSeq = sField;
                        break;
                    }
                }
                // If sequence not found
                if (sSeq == null)
                {
                    System.err.println("Sequence not found. Ignoring line:\n"
                            + sCurInstance);
                    continue;
                }

                psTest.println(utils.printIterable(
                        getDistances(hmClasses, sSeq).asTreeMap().values(),
                        ", ") + ", " + sSeqName + ", " + sClass);
            }

        }
        psTest.close();
    }

    private static Distribution<String> getDistances(HashMap<String, DocumentNGramGraph>
            hmClassGraphs, String sSeq) {
        Distribution<String> dDistances = new Distribution<String>();
        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        // Create seq graph
        DocumentNGramGraph ngCur = new DocumentNGramSymWinGraph(1, 5,
                5);
        ngCur.setDataString(sSeq);
        // For each class
        for (String sCurClass: hmClassGraphs.keySet()) {
            GraphSimilarity gs = ngc.getSimilarityBetween(
                    hmClassGraphs.get(sCurClass), ngCur);
            double dSim = gs.SizeSimilarity == 0.0 ? 0.0 :
                gs.ValueSimilarity / gs.SizeSimilarity;
            dDistances.setValue(sCurClass,
                    dSim);
        }
        return dDistances;
    }

    private static String headerPerClasses(String[] saClasses) {
        StringBuilder sb = new StringBuilder();
        for (String sCurClass : saClasses) {
            sb.append(sCurClass);
            sb.append("\t");
        }
        return sb.toString();
    }
}
