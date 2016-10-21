/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.interoperability;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/** A class that opens an csv file containing the following fields of 
 * GeneralInquirer: Entry, Source, Positiv, Negativ, Defined
 * @author ggianna
 */
public class GIFileLoader {
    public static int POLARITY_POSITIVE = 1;
    public static int POLARITY_NEGATIVE = -1;
    public static int POLARITY_NONPOLAR = 0;
    
    private String sGIFilename;
    
    public Hashtable<String,String> hDefinitionToSense;
    public Hashtable<String,String> hSenseToDefinition;
    public Hashtable<String,Integer> hSenseToPolarity;
    public Hashtable<String,Integer> hDefinitionToPolarity;
            
    public GIFileLoader(String sFilename) {
        sGIFilename = sFilename;
        // Init hashtables
        hDefinitionToSense = new Hashtable<String, String>();
        hSenseToDefinition = new Hashtable<String, String>();
        hSenseToPolarity = new Hashtable<String, Integer>();
        hDefinitionToPolarity = new Hashtable<String, Integer>();
        
        String sFile = utils.loadFileToStringWithNewlines(sFilename);
        ArrayList<String> alLines = new ArrayList(Arrays.asList(
                sFile.split("\n")));
        for (String sLine : alLines) {
            String[] saTokens = sLine.split(",");
            if (saTokens.length < 5)
                continue;
            String sSense = saTokens[0];
            
            StringBuffer sbDef = new StringBuffer();
            int iCnt = 4;
            while (iCnt < saTokens.length)
                sbDef.append(saTokens[iCnt++]);
            String sDefinition = sbDef.toString();
            
            String sPolarity = saTokens[2]; // Positive
            int iPolarity = POLARITY_NONPOLAR;
            if (sPolarity.length() > 0)
                iPolarity = POLARITY_POSITIVE;
            else
                if (saTokens[3].length() > 0)
                    iPolarity = POLARITY_NEGATIVE;
            
            hDefinitionToSense.put(sDefinition, sSense);
            hSenseToDefinition.put(sSense, sDefinition);
            hSenseToPolarity.put(sSense, iPolarity);
            hDefinitionToPolarity.put(sDefinition, iPolarity);
        }
    }
    
    public static void main(String[] args) {
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);
        String sDefinition = utils.getSwitch(hSwitches, "definition", "test");
        String sGIFile = utils.getSwitch(hSwitches, "giFile", "./gi.txt");
       
        GIFileLoader loader = new GIFileLoader(sGIFile);
        Distribution<String> definitionScores = new Distribution<String>();
        
        DocumentNGramGraph dg = new DocumentNGramGraph(3,3,4);
        dg.setDataString(sDefinition);
        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        for (String sDef : loader.hDefinitionToPolarity.keySet()) {
            DocumentNGramGraph dgCur = new DocumentNGramGraph(3,3,4);
            dgCur.setDataString(sDef);
            GraphSimilarity gs = ngc.getSimilarityBetween(dg, dgCur);
            definitionScores.setValue(sDef, gs.ValueSimilarity / 
                    gs.SizeSimilarity);
        }
        
        String sBestDef = definitionScores.getKeyOfMaxValue();
        System.err.println(definitionScores.toString());
        System.out.println("Closest definition: " + 
                sBestDef);
        System.out.println("Polarity: " + 
                loader.hDefinitionToPolarity.get(sBestDef));
        
    }
}
