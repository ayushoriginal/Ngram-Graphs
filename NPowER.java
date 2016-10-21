/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.npower;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author ggianna
 */
public class NPowER {

    public static void printSyntax() {
        System.out.println("Syntax: NPowER \"-peer=peer.txt\" "
                + "\"-models=model1.txt[:model2.txt:model3.txt:...]\" "
                + "[-minN=3] [-maxN=3] [-dwin=3]"
                + " [-minScore=0.0] [-maxScore=1.0] [-allScores]"
                + " [-noSelfModel]");
        
        // TODO: Add the ability to output all scores (AutoSummENG, MeMoG, NPowER)
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);

        String sPeer = utils.getSwitch(hSwitches, "peer", "").trim();
        String sModels = utils.getSwitch(hSwitches, "models", "").trim();
        Integer iMinN = Integer.valueOf(utils.getSwitch(hSwitches, "minN",
                "3").trim());
        Integer iMaxN = Integer.valueOf(utils.getSwitch(hSwitches, "maxN",
                "3").trim());
        Integer iDWin = Integer.valueOf(utils.getSwitch(hSwitches, "dwin",
                "3").trim());
        Double dMinScore = Double.valueOf(utils.getSwitch(hSwitches,
                "minScore", "0.0").trim());
        Double dMaxScore = Double.valueOf(utils.getSwitch(hSwitches,
                "maxScore", "1.0").trim());
        boolean bAllScores = Boolean.valueOf(utils.getSwitch(hSwitches,
                "allScores", String.valueOf(false)).trim());
        boolean bNoSelfModel = Boolean.valueOf(utils.getSwitch(hSwitches,
                "noSelfModel", String.valueOf(false)).trim());

        // Show params to command line
        System.err.println(String.format("MinN: %d, MaxN: %d, Dwin: %d\n"
                + "MinScore: %4.2f, MaxScore: %4.2f\n", iMinN, iMaxN, iDWin,
                dMinScore, dMaxScore)
                + (bAllScores ? "AllScores" : "") + " "
                + (bNoSelfModel ? "NoSelfModel" : ""));

        if ((sPeer.length() == 0) || (sModels.length() == 0)) {
            printSyntax();
            return;
        }

        // Init merged model graph (MeMoG)
        DocumentNGramGraph dggModel = new DocumentNGramSymWinGraph(iMinN, iMaxN,
                iDWin);

        // Init AutoSummENG graph map and result distribution
        Map<String, DocumentNGramGraph> mModelGraphs =
                new TreeMap<String, DocumentNGramGraph>();
        Distribution<String> dRes = new Distribution<String>();

        List<String> lModelFiles = new ArrayList<String>();
        
        // For every model
        double dMergeCnt = 0.0;
        for (String sCurModel : sModels.split(":")) {
            File fCurModel = new File(sCurModel);
            if (!fCurModel.canRead()) {
                System.err.println("Could not read model " + sCurModel);
                return;
            }
            if (fCurModel.getName().equals(new File(sPeer).getName()) && 
                    bNoSelfModel) {
                System.err.println("Ignoring self as model: " + sCurModel);
                continue;
            }
            // Update model list
            lModelFiles.add(sCurModel);

            DocumentNGramGraph dggCur = new DocumentNGramSymWinGraph(iMinN,
                    iMaxN, iDWin);
            try {
                // Load file
                dggCur.loadDataStringFromFile(sCurModel);
                // Update AutoSummENG map
                mModelGraphs.put(sCurModel, dggCur);

                // Always increase merge count
                if (dMergeCnt++ == 0) {
                    // Init graph
                    dggModel = dggCur;
                } else {
                    // Merge into MeMoG
                    dggModel.merge(dggCur, 1.0 / dMergeCnt);
                }
            } catch (IOException ex) {
                System.err.println("Could not read model " + sCurModel
                        + "\n" + ex.getLocalizedMessage());
                return;
            }
        }

        // Load peer text graph
        DocumentNGramGraph dggPeer = new DocumentNGramSymWinGraph(iMinN,
                iMaxN, iDWin);
        if (!new File(sPeer).canRead()) {
            System.err.println("Could not read peer file " + sPeer);
            return;
        }
        try {
            dggPeer.loadDataStringFromFile(sPeer);
        } catch (IOException ex) {
            System.err.println("Could not read peer file " + sPeer
                    + "\n" + ex.getLocalizedMessage());
        }

        // Calculate MeMoG
        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        GraphSimilarity gs = ngc.getSimilarityBetween(dggPeer, dggModel);

        // Calculate AutoSummENG
        for (String sCurModel : lModelFiles) {
            NGramCachedGraphComparator ngcA = new NGramCachedGraphComparator();
            gs = ngcA.getSimilarityBetween(dggPeer,
                    mModelGraphs.get(sCurModel));
            dRes.setValue(sCurModel, gs.ValueSimilarity);
        }

        // Calculate overall value
        // Model (based on TAC 2009 and TAC 2010 - A corpus)
        //5.2905 * AutoSummENG +
        //      3.0053 * MeMoG +
        //      0.5866
        double dMeMoG = gs.ValueSimilarity;
        double dAutoSummENG = dRes.average(true);
        // Actually using normalized score
        double dNPowER = (5.2905 * dAutoSummENG + 3.0053 * dMeMoG + 0.5866) / 10;
        dNPowER = dMinScore + (dMaxScore - dMinScore) * dNPowER;
        
        if (bAllScores) {
            System.err.println("Showing all scores (AutoSummENG MeMoG NPowER)");
            System.out.println(String.format("%8.6f %8.6f %8.6f", dAutoSummENG, 
                    dMeMoG, dNPowER));
        }
        else
            System.out.println(String.format("%8.6f", dNPowER));
    }
}
