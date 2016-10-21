/*  Under LGPL licence
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.jinsect.*;
import gr.demokritos.iit.conceptualIndex.LocalWordNetMeaningExtractor;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.interoperability.GIFileLoader;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ggianna
 */
public class polarityEstimator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Load command line arguments
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);
        
        String sInputFile = utils.getSwitch(hSwitches, "inputFile", "input.txt");
        boolean bSaveModel = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "save", String.valueOf(false))).booleanValue();       
        boolean bLoadModel = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "load", String.valueOf(false))).booleanValue();
        boolean bWEKATrain = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "WEKATrain", String.valueOf(false))).booleanValue();
        boolean bWEKATest = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "WEKATest", String.valueOf(false))).booleanValue();
        int iMinNGram = Integer.valueOf(utils.getSwitch(hSwitches, 
                "minNGram", "3")).intValue();
        int iMaxNGram = Integer.valueOf(utils.getSwitch(hSwitches, 
                "maxNGram", "5")).intValue();
        int iMaxDist = Integer.valueOf(utils.getSwitch(hSwitches, 
                "maxDist", "5")).intValue();
        
        // Load indexer
        GIFileLoader gi = new GIFileLoader("gi.txt");
        // Init models for pos, neg, neutral
        DocumentNGramGraph gPositive = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
        DocumentNGramGraph gNegative = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
        DocumentNGramGraph gNeutral = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
        
        INSECTFileDB db = new INSECTFileDB("VR_", "models/");
        boolean bLoadedOK = false;
        if (bLoadModel) {
            System.err.print("Loading model...");
            gPositive = (DocumentNGramGraph)db.loadObject("pos", 
                    "graphModel");
            gNegative = (DocumentNGramGraph)db.loadObject("neg", 
                    "graphModel");
            gNeutral = (DocumentNGramGraph)db.loadObject("neutral", 
                    "graphModel");
            
            // Check if all OK
            bLoadedOK = (gPositive != null) && (gNegative != null) && 
                    (gNeutral != null);
            System.err.println("OK.");
        }
        
        // If loading failed, then read GI input
        if (!bLoadedOK)
        {
            System.err.print("Training model...");
            gPositive = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
            gNegative = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
            gNeutral = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
            
        
            // Create pos, neg, neutral model
            int iPosCnt = 0, iNegCnt = 0, iNeutralCnt = 0, iOverallCnt = 0;
            for (String sCurSense : gi.hSenseToDefinition.keySet()) {
                int iPol = gi.hSenseToPolarity.get(sCurSense);
                int iHashPos = sCurSense.lastIndexOf("#");
                
                String sCurWord = (iHashPos < 0) ? sCurSense : 
                    sCurSense.substring(0, iHashPos);
                String sDef = gi.hSenseToDefinition.get(sCurSense);
                
                DocumentNGramGraph gTmp = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
                int iColonIdx = sDef.lastIndexOf(":");
                if (iColonIdx > -1)
                    gTmp.setDataString(sCurWord + " " + 
                            sDef.substring(iColonIdx + 1).replace("\"[|]", " "));
                else
                    gTmp.setDataString(sCurWord.replace("\"[|]", " "));

                if (iPol == GIFileLoader.POLARITY_POSITIVE) {
                    gPositive.merge(gTmp, (1.0 - (iPosCnt / ++iPosCnt)));
                    // gNegative.degrade(gTmp);
                    // gNeutral.degrade(gTmp);
                }
                else if (iPol == GIFileLoader.POLARITY_NEGATIVE) {
                    gNegative.merge(gTmp, (1.0 - (iNegCnt / ++iNegCnt)));
                    // gPositive.degrade(gTmp);
                    // gNeutral.degrade(gTmp);
                }
                else {
                    
                    // IGNORE
                    //gNeutral.merge(gTmp, (1.0 - (iNeutralCnt / ++iNeutralCnt)));
                    gPositive.degrade(gTmp);
                    gNegative.degrade(gTmp);
                }

                // DEBUG LINES
                if (++iOverallCnt % 100 == 0)
                System.err.println("Updated " + String.valueOf(iOverallCnt) +
                        " of " + String.valueOf(gi.hSenseToDefinition.size()) + 
                        "...\t");
                //////////////
            }
            System.err.println("Training model...OK.");
            System.err.println(String.format("Pos: %d\tNeg: %d\tNeu: %d", iPosCnt,
                    iNegCnt, iNeutralCnt));
        }
        
        if (bSaveModel) {
            System.err.print("Saving model...");
            // Save models
            db.saveObject(gPositive, "pos", "graphModel");
            db.saveObject(gNegative, "neg", "graphModel");
            db.saveObject(gNeutral, "neutral", "graphModel");
            System.err.println("Done.");
        }
        
        // Create WEKA Training file if requested
        if (bWEKATrain) {
            System.out.println("@RELATION opinitionSum\n");
            System.out.println("@ATTRIBUTE posNormValSim NUMERIC\n" +
                    "@ATTRIBUTE negNormValSim NUMERIC\n" +
                    "@ATTRIBUTE neutralNormValSim NUMERIC\n" +
                    "@ATTRIBUTE polarity {pos,neg,neutral}\n\n" +
                    "@DATA");
            Distribution dResults = new Distribution();
            
            for (String sCurSense : gi.hSenseToDefinition.keySet()) {
                int iHashPos = sCurSense.lastIndexOf("#");
                String sCurWord = (iHashPos < 0) ? sCurSense : 
                    sCurSense.substring(0, iHashPos);
                String sDef = gi.hSenseToDefinition.get(sCurSense);
                int iCurPolarity = gi.hSenseToPolarity.get(sCurSense);
                String sCurPolarity = getPolarityStr(iCurPolarity);
                
                DocumentNGramGraph gTmp = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
                int iColonIdx = sDef.lastIndexOf(":");
                if (iColonIdx > -1)
                    gTmp.setDataString(sCurWord + " " + 
                            sDef.substring(iColonIdx + 1).replace("\"[|]", " "));
                else
                    gTmp.setDataString(sCurWord.replace("\"[|]", " "));

                int iPol = gi.hSenseToPolarity.get(sCurSense);
                NGramGraphComparator comp = new NGramGraphComparator();
                Distribution<String> d = new Distribution();
                Distribution<Integer> d2 = new Distribution();

                GraphSimilarity simil = comp.getSimilarityBetween(gTmp, gPositive);
                d.setValue("pos", simil.SizeSimilarity == 0.0 ? 0.0 :
                    simil.ValueSimilarity / simil.SizeSimilarity);
                d2.setValue(GIFileLoader.POLARITY_POSITIVE, simil.SizeSimilarity == 0.0 ? 0.0 :
                    simil.ValueSimilarity / simil.SizeSimilarity);
                
                simil = comp.getSimilarityBetween(gTmp, gNegative);                
                d.setValue("neg", simil.SizeSimilarity == 0.0 ? 0.0 :
                    simil.ValueSimilarity / simil.SizeSimilarity);
                d2.setValue(GIFileLoader.POLARITY_NEGATIVE, simil.SizeSimilarity == 0.0 ? 0.0 :
                    simil.ValueSimilarity / simil.SizeSimilarity);
                
//                simil = comp.getSimilarityBetween(gTmp, gNeutral);
//                d.setValue("neutral", simil.SizeSimilarity == 0.0 ? 0.0 :
//                    simil.ValueSimilarity / simil.SizeSimilarity);
//                d2.setValue(GIFileLoader.POLARITY_NONPOLAR, simil.SizeSimilarity == 0.0 ? 0.0 :
//                    simil.ValueSimilarity / simil.SizeSimilarity);

                // Check for correct estimation using simple similarity
                if (d2.getKeyOfMaxValue() == iCurPolarity)
                    dResults.increaseValue(sCurPolarity + "Correct", 1.0);
                dResults.increaseValue(sCurPolarity + "All", 1.0);
                
                // Output result
                System.out.println(String.format("%8.6f,%8.6f,%8.6f,%s", 
                        d.getValue("pos"), d.getValue("neg"), 
                        d.getValue("neutral"), sCurPolarity));
                
            }
            // DEBUG LINES
            //System.err.println("Terminating execution, due to WEKATrain flag.");
            //////////////
            
            // Output simple similarity success
            System.out.println("Results using SIMPLE SIMILARITY:\n" + 
                    dResults.toString());
        }
        
        LocalWordNetMeaningExtractor l = null;
        try {
            l = new LocalWordNetMeaningExtractor();
        } catch (IOException ex) {
            Logger.getLogger(polarityEstimator.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }       
        
        // For all expressions in file
        if (bWEKATest) {
            System.out.println("@RELATION opinitionSum\n");
            System.out.println("@ATTRIBUTE posNormValSim NUMERIC\n" +
                    "@ATTRIBUTE negNormValSim NUMERIC\n" +
                    "@ATTRIBUTE neutralNormValSim NUMERIC\n" +
                    "@ATTRIBUTE polarity {pos,neg,neutral}\n\n" +
                    "@DATA");
        }
        String sFullText = utils.loadFileToStringWithNewlines(sInputFile);
        Pattern p = Pattern.compile("\\w+[#]\\w+[#]\\d+");
        Matcher mSeeker = p.matcher(sFullText);
        while (mSeeker.find()) {
            // Extract match
            String sSubString = sFullText.substring(mSeeker.start(), 
                    mSeeker.end());
            String[] saTokens = sSubString.split("#");
            String sWord = saTokens[0];
            String sPOS = saTokens[1];
            int iSenseNum = Integer.valueOf(saTokens[2]);
            
            // Create graph for word
            DocumentNGramGraph gTest = new DocumentNGramSymWinGraph(iMinNGram, iMaxNGram, iMaxDist);
            gTest.setDataString(allTextForSense(l, sWord, sPOS, iSenseNum));
            // DEBUG LINES
            // System.err.println(String.format("Checking sense : %s # %s # %d", sWord,
                    //sPOS, iSenseNum));
            // System.err.println(gTest.getDataString());
            //////////////

            NGramGraphComparator comp = new NGramGraphComparator();
            Distribution<String> d = new Distribution();

            GraphSimilarity simil = comp.getSimilarityBetween(gTest, gPositive);
            d.setValue("pos",  simil.SizeSimilarity == 0 ?
                0.0 : (simil.ValueSimilarity / simil.SizeSimilarity));
            simil = comp.getSimilarityBetween(gTest, gNegative);
            d.setValue("neg", simil.SizeSimilarity == 0 ?
                0.0 : (simil.ValueSimilarity / simil.SizeSimilarity));
            //simil = comp.getSimilarityBetween(gTest, gNeutral);
            //d.setValue("neutral", simil.ValueSimilarity / simil.SizeSimilarity);

            // Output result
            if (!bWEKATest) {
                System.out.println(String.format("%s # %s # %d\n%s\n-->%s", sWord,
                    sPOS, iSenseNum, gTest.getDataString(), d.getKeyOfMaxValue()));
                // DEBUG LINES
            // System.err.println("Selected value: " + d.getKeyOfMaxValue());
            System.out.println("Similarity Values: \n" + d.toString());
            //////////////
            
        }
            else 
            {
                // Output result
                System.out.println(String.format("%8.6f,%8.6f,%8.6f,%8.6s", d.getValue("pos"),
                        d.getValue("neg"), d.getValue("neutral"),
                        "?"));
            }

        };
        
        
        /*
        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        DocumentNGramGraph dg = new DocumentNGramGraph(3,3,3);
        DocumentNGramGraph dg2 = new DocumentNGramGraph(3,3,3);
        DocumentNGramGraph dg3 = new DocumentNGramGraph(3,3,3);
        
        dg.setDataString("This is a simple test.");
        dg2.setDataString("This is a, not that simple, test.");
        dg3.setDataString("This is a not that simple test.");
        System.out.println(ngc.getSimilarityBetween(dg, dg2).toString());
        System.out.println(ngc.getSimilarityBetween(dg, dg3).toString());
        System.out.println(ngc.getSimilarityBetween(dg2, dg3).toString());
        //System.err.println("dg:\n" + utils.graphToDot(dg.getGraphLevel(0), true));
        //System.err.println("dg2:\n" + utils.graphToDot(dg2.getGraphLevel(0), true));
        //System.err.println("dg3:\n" + utils.graphToDot(dg3.getGraphLevel(0), true));
        System.err.flush();
        
        DocumentNGramSymWinGraph wdg = new DocumentNGramSymWinGraph(3,3,3);
        DocumentNGramSymWinGraph wdg2 = new DocumentNGramSymWinGraph(3,3,3);
        DocumentNGramSymWinGraph wdg3 = new DocumentNGramSymWinGraph(3,3,3);
        wdg.setDataString("This is a simple test.");
        wdg2.setDataString("This is a, not that simple, test.");
        wdg3.setDataString("This is a not that simple test.");
        System.out.println(ngc.getSimilarityBetween(wdg, wdg2).toString());
        System.out.println(ngc.getSimilarityBetween(wdg, wdg3).toString());
        System.out.println(ngc.getSimilarityBetween(wdg2, wdg3).toString());
        
        //System.err.println("wdg:" + utils.graphToDot(wdg.getGraphLevel(0), true));
        //System.err.println("wdg2:" + utils.graphToDot(wdg2.getGraphLevel(0), true));
        //System.err.println("wdg3:" + utils.graphToDot(wdg3.getGraphLevel(0), true));
         */
    }
    
    private static final String allTextForSense(LocalWordNetMeaningExtractor l, 
            String sWord, String sPOS, int iSenseNum) {
        return sWord + "\n" +
            l.getDefinition(sWord, sPOS, iSenseNum);
//            + "\n" +
//            l.getGloss(sWord, sPOS, iSenseNum);
//            + "\n" + 
//            l.getSenseWords(sWord, sPOS, iSenseNum);
    }

    private static final String getPolarityStr(Integer iCurPolarity) {
        String sCurPolarity = "";
        if (iCurPolarity == GIFileLoader.POLARITY_NEGATIVE)
            sCurPolarity = "neg";
        else if (iCurPolarity == GIFileLoader.POLARITY_POSITIVE)
            sCurPolarity = "pos";
        else
            sCurPolarity = "neutral";
        
        return sCurPolarity;
    }
}
