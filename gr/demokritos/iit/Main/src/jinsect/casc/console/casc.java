/*
 * casc.java
 *
 * Created on 16 Μαρτίου 2007, 12:49 μμ
 *
 * @author ggianna
 */

package gr.demokritos.iit.jinsect.casc.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import gr.demokritos.iit.jinsect.algorithms.clustering.AverageLinkClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.CompleteLinkClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.IClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.SingleLinkClusterer;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.interoperability.CASCDistanceCalculator;
import gr.demokritos.iit.jinsect.casc.structs.CASCGraph;
import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import gr.demokritos.iit.jinsect.threading.ThreadList;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import salvo.jesus.graph.WeightedEdge;

/** Helper class used in CASC evaluation of 2006 (see 
 * <a href='http://www.cs.helsinki.fi/u/ttonteri/casc/index.html'>Computer-Assisted Stemmatology Challenge</a>).
 *
 * @author ggianna
 */
public class casc {
        
    public static final String SINGLE_LINK = "SingleLink";
    public static final String COMPLETE_LINK = "CompleteLink";
    public static final String AVERAGE_LINK = "AverageLink";
    
    public static void main(String[] args) {
        // TODO: REMOVE
//        TreeMap tmTemp = new TreeMap();
//        NGramDocument ndTmp = new NGramDocument();
//        ndTmp.loadDataStringFromFile(
//                "/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/Text1.txt");
//        tmTemp.put("/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/Text1.txt", ndTmp);
//        
//        ndTmp = new NGramDocument();
//        ndTmp.loadDataStringFromFile(
//                "/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/Text2.txt");
//        tmTemp.put("/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/Text2.txt", ndTmp);
//        
//        ndTmp = new NGramDocument();
//        ndTmp.loadDataStringFromFile(
//                "/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/Text3.txt");
//        tmTemp.put("/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/Text3.txt", ndTmp);
//        
//        TreeMap tmVoc = getVocabulary(tmTemp);
//        try {
//            
//            BufferedWriter bwTmp = new BufferedWriter(
//                    new FileWriter("/home/ggianna/Documents/Thesis/Dissertation Supervision/Kahiouteas/SSpell/voc"));
//            Iterator iVoc = tmVoc.keySet().iterator();
//            while (iVoc.hasNext()) {
//                String sCurWord = (String)iVoc.next();
//                bwTmp.write(sCurWord.replaceAll("\\n|\\r|\\t", "_") + "\n");
//            }
//            bwTmp.flush();
//            bwTmp.close();
//        } catch (IOException ex) {
//            ex.printStackTrace(System.err);
//        }
//        System.exit(0);
        ///////////////
        
        // Read command line
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        System.err.println("Using parameters:" + hSwitches.toString());
        Boolean bShowCorrectGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "showCorrectGraph", 
                "" + Boolean.FALSE));
        Boolean bShowDummyGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "showDummyGraph", 
                "" + Boolean.FALSE));
        Boolean bCalcRandom = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "calcRandom", 
                "" + Boolean.FALSE));
        Integer iRandomCalcs = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "randomCalcs", 
                "10"));
        Boolean bOptimizeNGramParams = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "optimizeNGramParams", "" + Boolean.FALSE));
        Boolean bShowNGramGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "showNGramGraph", "" + Boolean.TRUE));
        Boolean bShowAggloLinkedCharGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "showAggloLinkedCharGraph", "" + Boolean.FALSE));
        String sAggloGraphMethod = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "aggloGraphMethod", AVERAGE_LINK);
        // Check if selected method is valid
        if ((SINGLE_LINK + "," + COMPLETE_LINK + "," + AVERAGE_LINK).toLowerCase().indexOf(
                sAggloGraphMethod.toLowerCase()) < 0)
        {
            System.err.println("Invalid agglomeration method.\nValid options:" + SINGLE_LINK + "," + 
                    COMPLETE_LINK + "," + AVERAGE_LINK + "\nDefaulting to average.");
            sAggloGraphMethod = AVERAGE_LINK;
        }
        Boolean bShowLDAGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "showLDAGraph", "" + Boolean.FALSE));
        Boolean bShowLevenshteinGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "showLevenshteinGraph", "" + Boolean.FALSE));
        
        // Test graph distance
        //testGraphDist();
        //System.exit(0);
        
        // the output now should be 72.5 % according to the example
        CASCGraph gDefault = getDefaultGraph();
        if (bShowCorrectGraph) {
            System.out.println("Correct graph:");
            System.out.println(graphToCASCDot(gDefault));
        }
        
        double distance;
        
        if (bShowDummyGraph) {
            CASCGraph gExample=getExampleGraph();
            System.out.println("Example graph:");
            System.out.println(graphToCASCDot(gExample));
            CASCGraph gDummy=getDummyGraph();
            System.out.println("Dummy Example graph:");
            System.out.println(graphToCASCDot(gDummy));
            
            distance=calcCASCDistanceBetween(gDefault, gExample);
            //distance=calcDistanceBetween(gDefault, gDummy);
            System.out.println("CASC Similarity between example and default:" + distance);
            distance=calcCASCDistanceBetween(gDefault, gDummy);
            //distance=calcDistanceBetween(gDefault, gDummy);
            System.out.println("CASC Similarity between dummy example and default:" + distance);
            //System.out.println("Our Implementation Similarity between dummy and default:" + distance);
        }
        
        ThreadList tRandoms = new ThreadList(4);
        
        if (bCalcRandom) {
            System.out.println("Calculating average random graph distance...");
            final Distribution dRandRes = new Distribution();
            for (int i=0; i < iRandomCalcs.intValue(); i++) {
                try {
                        final CASCGraph gDefaultArg = gDefault;
                        // Multi-threading
                        while (!tRandoms.addThreadFor(new Runnable() {
                            public void run() {
                                CASCGraph gRandom=getRandomGraph();

                                double dDistance=calcCASCDistanceBetween(gDefaultArg, gRandom);
                                //distance=calcDistanceBetween(gDefault, gRandom);
                                synchronized (dRandRes) {
                                    dRandRes.setValue(dDistance, dRandRes.getValue(dDistance) + 1.0);
                                }
                                synchronized (System.err) {
                                    System.err.print(".");
                                }
                            }
                        }))
                            Thread.yield();
                }
                catch (Exception e) {
                    continue;
                }
            }
            
            try {
                tRandoms.waitUntilCompletion();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
                return;
            }

            System.err.println("\nCalculation Complete.");
            System.out.println("Average similarity between random and default:" + 
                    dRandRes.average(false));
        }
        else
            System.out.println("Average similarity between random and default: 0.523 " +
                    "(Pre-calculated over 1000 iterations)");
        
        
        String sInputDir = args[0]; // Get directory
        
        if (bOptimizeNGramParams) {
            Distribution dNGramRes = new Distribution();
            double dBest = Double.NEGATIVE_INFINITY;
            double dLast = Double.NEGATIVE_INFINITY;
            String sBest = "";
            
            int iNoImprovement = 0;
            System.err.println("Calculating best performance for n-gram parameters " +
                    "\n(* indicates new high in performance, every 10% progress a % is print):");
            int iCnt=0;
            
            for (int iMinSize=3; iMinSize<=10; iMinSize++) {
                for (int iMaxSize = iMinSize + 1; iMaxSize<iMinSize + 5; iMaxSize++) {
                    for (int iDist = 1; iDist<=10; iDist++) {
                        System.err.println(); // Indicate conf change
                        iNoImprovement = 0;
                        TreeMap tmDocs = new TreeMap();
                        for (double dGraphImp = 0.7; dGraphImp <= 0.9; dGraphImp+=0.05) {
                            CASCGraph gTree = getNGramDistanceGraph(sInputDir, iMinSize, iMaxSize, iDist,
                                    dGraphImp, true, tmDocs);
                            //CASCGraph gTree = getNGramDistanceAgglomerativelyClusteredGraph(sInputDir, 
                                    //iMinSize, iMaxSize, iDist, AVERAGE_LINK);

                            distance=calcCASCDistanceBetween(gDefault, gTree);
                            if (distance > dBest) {
                                // Output star for new best result
                                System.out.print("*");
                                System.out.println("Config:" + iMinSize + "," + 
                                        iMaxSize + "," + iDist + "," + dGraphImp + 
                                        "(" + distance + ")");
                                dBest = distance;
                                sBest = graphToCASCDot(gTree);
                                iNoImprovement = 0;
                            }
                            else {
                                if (dLast > distance)
                                    System.out.print("\\");
                                else
                                    if (dLast == distance)
                                        System.out.print("-");
                                    else
                                        System.out.print("/");
                                iNoImprovement++;
                            }
                            // If no improvement for 100 steps, ignore rest...
                            if (iNoImprovement >= 100)
                            {
                                iNoImprovement = 0;
                                break;
                            }

                            //System.out.println("\nSimilarity:" + distance);
                            dNGramRes.setValue("" + iMinSize + "," + iMaxSize + "," + iDist +
                                    "," + dGraphImp, distance);
                            if ((++iCnt) % 1000 == 0) {
                                System.err.print(" % ");
                            }
                            dLast = distance;
                        }
                    }
                }            
            }
            System.err.println();
            System.out.println("Calculation complete. Best Config:" + dNGramRes.getKeyOfMaxValue().toString());
            System.out.println("Best Result:" + dNGramRes.maxValue());
        }
        
        System.err.println("Using for WORD n-grams params: 1,1,8,0.6 (Result 0.74)");
        System.err.println("Using for CHAR n-grams params: 3,4,1,0.8 (Result 0.791)");
        
        if (bShowNGramGraph) {
            CASCGraph gTree = getNGramDistanceGraph(sInputDir, 3, 4, 1,
                    0.8, false, new TreeMap());
            System.out.println(graphToCASCDot(gTree));
            distance=calcCASCDistanceBetween(gDefault, gTree);
            System.out.println("Similarity:" + distance);
            
        }
        
        // Output results
        if (bShowAggloLinkedCharGraph) {
            System.out.println();
            System.out.println("\nOur NGram Agglo-AverageLink answer:");
            CASCGraph gAgglAverage = getNGramDistanceAgglomerativelyClusteredGraph(sInputDir, 
                    3, 4, 1, 0.8, sAggloGraphMethod);
            System.out.println(graphToCASCDot(gAgglAverage));
            distance=calcCASCDistanceBetween(gDefault, gAgglAverage);
            System.out.println("Similarity:" + distance);
        }

        
        if (bShowLDAGraph) {
            // Output results
            System.out.println();
            System.out.println("\nOur NGram LDA answer:");
            CASCGraph gLDA = getLDABasedGraph();
            System.out.println(graphToCASCDot(gLDA));
            //distance=calcCASCDistanceBetween(gDefault, gLDA);
            distance=calcDistanceBetween(gDefault, gLDA);
            System.out.println("Similarity:" + distance);
        }
        
        if (bShowLevenshteinGraph) {
            System.out.println();
            System.out.println("\nOur Levenshtein answer:");
            CASCGraph gLevenshtein = getLevenshteinDistanceGraph(sInputDir);
            System.out.println(graphToCASCDot(gLevenshtein));
            //distance=calcCASCDistanceBetween(gDefault, gLevenshtein);
            distance=calcDistanceBetween(gDefault, gLevenshtein);
            System.out.println("Similarity:" + distance);
        }
    }
    
    public static CASCGraph getLevenshteinDistanceGraph(String sInputDir) {
        final TreeMap hmDocumentIndex = new TreeMap();
        
        // Create document set
        DocumentSet dsSrc = new DocumentSet(sInputDir,1.0);
        dsSrc.createSets();
        
        // Init distribution of distances
        final Distribution dDist = new Distribution();
        
        System.err.println("Processing documents.");
        // For all documents
        Iterator iDocIter = dsSrc.getTrainingSet().iterator();
        ThreadList t = new ThreadList(4);
        
        while (iDocIter.hasNext()) {
            // Get n-gram document
            final String sFile = ((CategorizedFileEntry)iDocIter.next()).getFileName();
            System.err.print(".");
           
            // Multi-threading
            while (!t.addThreadFor(new Runnable() {
                public void run() {
                    String sCurDoc = gr.demokritos.iit.jinsect.utils.loadFileToString(sFile);
                    // and store
                    synchronized (hmDocumentIndex) {
                        hmDocumentIndex.put(sFile, sCurDoc);
                    }
                }
            }))
                Thread.yield();
            
        }
        try {
            t.waitUntilCompletion();
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
        System.err.println("\nProcessing complete.");
        
        
        CASCGraph gTree = new CASCGraph();
        
        int iTotalSize = dsSrc.getTrainingSet().size();
        int iCurProgress = 0;
        
        
        // First pass
        // For every document
        iDocIter = dsSrc.getTrainingSet().iterator();
        
        while (iDocIter.hasNext()) {
            double dMaxSimil = Double.NEGATIVE_INFINITY;
            String sParentDoc = null;            
            String sCurDoc = ((CategorizedFileEntry)iDocIter.next()).getFileName();
            
            // Compare with every other document
            Iterator iCompareToIter = dsSrc.getTrainingSet().iterator();
            // Multi-threading
            final Distribution dSimils = new Distribution();
            while (iCompareToIter.hasNext()) {
                String sCompareToDoc = ((CategorizedFileEntry)iCompareToIter.next()).getFileName();
                // Ignore self and parent
                if (sCompareToDoc.equals(sCurDoc)) {
                    // System.err.println("Ignoring " + sCompareToDoc);
                    continue;
                }
                // and already connected documents
                if (gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(gTree, getFileNameOnly(sCompareToDoc), 
                        getFileNameOnly(sCurDoc)) != null)
                {
                    // System.err.println("Ignoring " + sCompareToDoc + " as already connected.");
                    continue;
                }
                
                final String sCurDocArg = sCurDoc;
                final String sCompareToDocArg = sCompareToDoc;
                while (!t.addThreadFor(new Runnable() {
                    public void run() {
                        // Actually compare
                        double dSimil = 0.0;
                        synchronized (hmDocumentIndex) {
                            dSimil = 
                                org.apache.commons.lang.StringUtils.getLevenshteinDistance(
                                    (String)hmDocumentIndex.get(sCurDocArg),
                                    (String)hmDocumentIndex.get(sCompareToDocArg));
                        }

                        // Check for max similarity
                        synchronized (dSimils) {
                            dSimils.setValue(sCompareToDocArg, dSimil);
                            //synchronized (System.err) {
                                //System.err.println(sCurDocArg + "-" + sCompareToDocArg + ":"
                                        //+ dSimil);
                            //}
                        }
                        
                        synchronized (dDist) {
                            // Increase occurence of similarity
                            dDist.setValue(dSimil, 
                                    dDist.getValue(dSimil) + 1);
                        }
                    }
                }))
                    Thread.yield();
            }
            
            try {
                t.waitUntilCompletion();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
                return null;
            }
            
            dMaxSimil = dSimils.maxValue();
            sParentDoc = (String)dSimils.getKeyOfMaxValue();
            
            try {
                // Get maximum similarity document as parent
                gTree.addEdge(new VertexImpl(getFileNameOnly(sParentDoc)), 
                        new VertexImpl(getFileNameOnly(sCurDoc)), dMaxSimil);
            } catch (Exception ex) {
                System.err.println("Cannot add edge...");
                ex.printStackTrace(System.err);
            }
            iCurProgress ++;
            System.err.print(String.format("Progress %2.2f %%\r", 100.0 * 
                    (double)iCurProgress  / iTotalSize));
        }
        System.err.println("Complete.");
        
        System.err.println("Similarity distribution:" + dDist.average(false) + "," + 
                dDist.standardDeviation(false));
        
        // Second pass. Create dummy documents.
        // Get parent 
        double dNeedsLatentParentThreshold = dDist.average(false) + dDist.standardDeviation(false);
        //double dNotSiblingsThreshold = dDist.average(false);
        
        Iterator iEdges = gTree.getEdgeSet().iterator();
        int iParentInc = 0;
        HashMap hParents = new HashMap();
        
        // For every edge
        while (iEdges.hasNext()) {
            WeightedEdge weCur = (WeightedEdge)iEdges.next();
            // Check if weight below threshold
            if (weCur.getWeight() < dNeedsLatentParentThreshold) {
                addParent(hParents, weCur.getVertexA(), weCur.getVertexB(), 
                        gTree, dNeedsLatentParentThreshold);
                try {
                    
                    gTree.removeEdge(weCur);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
                iEdges = gTree.getEdgeSet().iterator();
            }
        }
        
        return gTree;
    }
    
    /** Returns a list iterator object that runs through the edge set of a given graph
     * in weight ascending order.
     *@param g The given {@link UniqueVertexGraph}.
     *@return A list iterator on the sorted graph edges.
     */
    public static ListIterator getEdgeIteratorByWeight(UniqueVertexGraph g) {
        Iterator iEdges = null;
        // Order edges by weight ascending
        iEdges = g.getEdgeSet().iterator();
        List sortedWeights = new LinkedList();
        List sortedEdges = new LinkedList();
        while (iEdges.hasNext()) {
            WeightedEdge weCur = (WeightedEdge)iEdges.next();
            // Search for weight
            int index = Collections.binarySearch(sortedWeights, weCur.getWeight());
            // Add the non-existent item to the list
            if (index < 0) {
                sortedWeights.add(-index-1, weCur.getWeight());
                sortedEdges.add(-index-1, weCur);
            }
            else
            {
                // Add edge just before the edge with the same weight.
                sortedEdges.add(index, weCur);
                sortedWeights.add(index, weCur.getWeight());
            }
        }
        ListIterator lRes = sortedEdges.listIterator();
        return lRes;
    }
    
    public static CASCGraph getNGramDistanceGraph(String sInputDir, int iMinNGram, int iMaxNGram,
            int iDist, final double dGraphImportance, boolean bSilent, TreeMap hLoadedDocs) {
        final TreeMap hmDocumentIndex = hLoadedDocs;
        
        // Create document set
        DocumentSet dsSrc = new DocumentSet(sInputDir,1.0);
        dsSrc.createSets(true);
        
        // Init distribution of distances
        final Distribution dDist = new Distribution();
        
        if (!bSilent)
            System.err.println("Processing documents.");
        // For all documents
        Iterator iDocIter = dsSrc.getTrainingSet().iterator();
        ThreadList t = new ThreadList(4);
        
        while (iDocIter.hasNext()) {
            // Get n-gram document
            final String sFile = ((CategorizedFileEntry)iDocIter.next()).getFileName();
            if (hmDocumentIndex.containsKey(sFile))
                continue; // Ignore already calculated files
            
            final NGramDocument dCurDoc = new NGramDocument(iMinNGram, iMaxNGram, iDist,
                    iMinNGram, iMaxNGram);
            // Multi-threading
            while (!t.addThreadFor(new Runnable() {
                public void run() {
                    synchronized (dCurDoc) {
                        dCurDoc.loadDataStringFromFile(sFile);
                    }
                    // and store
                    synchronized (hmDocumentIndex) {
                        hmDocumentIndex.put(sFile, dCurDoc);
                    }
                }
            }))
                Thread.yield();
            
            if (!bSilent)
                System.err.print(".");
        }
        try {
            t.waitUntilCompletion();
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
        if (!bSilent)
            System.err.println("\nProcessing complete.");
        
        /*
        System.err.println("Creating LDA utility file.");
        File f = new File("LDADocTermMatrix.txt");
        try {
            FileWriter fwOut = new FileWriter(f);
            fwOut.write(getLDAArray(hmDocumentIndex));
            fwOut.close();
            System.err.println(f.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        System.err.println("Saved LDA utility file.");
        
        //System.exit(0); // TODO: Remove
        */
        
        /*
        System.err.println("Creating SSpell utility file.");
        File f = new File("SSpell.txt");
        try {
            FileWriter fwOut = new FileWriter(f);
            Iterator iVoc = getVocabulary(hmDocumentIndex).keySet().iterator();
            while (iVoc.hasNext()) {
                fwOut.write((String)iVoc.next());
                fwOut.write("\n");
                System.err.print(".");
            }
            System.err.println();
            fwOut.close();
            System.err.println(f.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        System.err.println("Saved SSpell utility file.");
         */
        
        CASCGraph gTree = new CASCGraph();
        
        int iTotalSize = dsSrc.getTrainingSet().size();
        int iCurProgress = 0;
        
        
        // First pass
        // For every document
        iDocIter = dsSrc.getTrainingSet().iterator();
        
        while (iDocIter.hasNext()) {
            double dMaxSimil = Double.NEGATIVE_INFINITY;
            String sParentDoc = null;            
            String sCurDoc = ((CategorizedFileEntry)iDocIter.next()).getFileName();
            
            // Compare with every other document
            Iterator iCompareToIter = dsSrc.getTrainingSet().iterator();
            // Multi-threading
            final Distribution dSimils = new Distribution();
            while (iCompareToIter.hasNext()) {
                String sCompareToDoc = ((CategorizedFileEntry)iCompareToIter.next()).getFileName();
                // Ignore self and parent
                if (sCompareToDoc.equals(sCurDoc)) {
                    //System.err.println("Ignoring " + sCompareToDoc);
                    continue;
                }
                // and already connected documents
                if (gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(gTree, getFileNameOnly(sCompareToDoc), 
                        getFileNameOnly(sCurDoc)) != null)
                {
                    //System.err.println("Ignoring " + sCompareToDoc + " as already connected.");
                    continue;
                }
                
                final String sCurDocArg = sCurDoc;
                final String sCompareToDocArg = sCompareToDoc;
                
                while (!t.addThreadFor(new Runnable() {
                    public void run() {
                        ///////////////////////
                        StandardDocumentComparator sdcComparator = 
                                new StandardDocumentComparator(dGraphImportance);
                        // Actually compare
                        GraphSimilarity sSimil;
                        try {                    
                            synchronized (hmDocumentIndex) {
                                sSimil = sdcComparator.getSimilarityBetween(
                                        (NGramDocument)hmDocumentIndex.get(sCurDocArg),
                                        (NGramDocument)hmDocumentIndex.get(sCompareToDocArg));
                            }
                        }
                        catch (InvalidClassException ice) {
                            System.err.println("Cannot happen...");
                            ice.printStackTrace(System.err);
                            sSimil = new GraphSimilarity();
                        }

                        // Check for max similarity
                        synchronized (dSimils) {
                            dSimils.setValue(sCompareToDocArg, sSimil.getOverallSimilarity());
                            synchronized (System.err) {
                                //System.err.println(sCurDocArg + "-" + sCompareToDocArg + ":"
                                        //+ sSimil.getOverallSimilarity());
                            }
                        }

                        synchronized (dDist) {
                            // Increase occurence of similarity
                            dDist.setValue(sSimil.getOverallSimilarity(), 
                                    dDist.getValue(sSimil.getOverallSimilarity()) + 1);
                        }

                        ///////////////////////
                    }
                }))
                    Thread.yield();
            }
            
            try {
                t.waitUntilCompletion();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
                return null;
            }
            
            dMaxSimil = dSimils.maxValue();
            sParentDoc = (String)dSimils.getKeyOfMaxValue();
            
            try {
                // Get maximum similarity document as parent
                gTree.addEdge(new VertexImpl(getFileNameOnly(sParentDoc)), 
                        new VertexImpl(getFileNameOnly(sCurDoc)), dMaxSimil);
            } catch (Exception ex) {
                System.err.println("Cannot add edge...");
                ex.printStackTrace(System.err);
            }
            iCurProgress ++;
            if (!bSilent)
                System.err.print(String.format("Progress %2.2f %%\r", 100.0 * 
                    (double)iCurProgress  / iTotalSize));
        }
        if (!bSilent)
            System.err.println("Complete.");
        
        if (!bSilent)
            System.err.println("Similarity distribution:" + dDist.average(false) + "," + 
                dDist.standardDeviation(false));
        
        // Second pass. Create dummy documents.
        // Get parent 
        double dNeedsLatentParentThreshold = Math.min(dDist.average(false) 
          + dDist.standardDeviation(false), 1.0);
        //double dNotSiblingsThreshold = dDist.average(false);
        
        ListIterator liEdges = getEdgeIteratorByWeight(gTree);
        

        int iParentInc = 0;
        HashMap hParents = new HashMap();
        
        //iEdges = gTree.getEdgeSet().iterator();
        // For every edge in descending similarity (weight) order
        while (liEdges.hasNext()) {
            WeightedEdge weCur = (WeightedEdge)liEdges.next();
            if ((hParents.get(weCur.getVertexA()) == weCur.getVertexB()) ||
                    (hParents.get(weCur.getVertexB()) == weCur.getVertexA()))
                    continue;
            // Check if weight below threshold
            if (weCur.getWeight() < dNeedsLatentParentThreshold) {
                addParent(hParents, weCur.getVertexA(), weCur.getVertexB(), 
                        gTree, (weCur.getWeight() + 1.0) / 2.0);
                try {
                    gTree.removeEdge(weCur);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
                //iEdges = gTree.getEdgeSet().iterator();
                liEdges = getEdgeIteratorByWeight(gTree);
            }
        }
        
        return gTree;
    }
    
    public static CASCGraph getNGramDistanceAgglomerativelyClusteredGraph(String sInputDir, int iMinNGram, 
            int iMaxNGram, int iDist, double dGraphImportance, String sClusteringType) {
        final TreeMap hmDocumentIndex = new TreeMap();
        
        // Create document set
        DocumentSet dsSrc = new DocumentSet(sInputDir,1.0);
        dsSrc.createSets();
        
        // Init distribution of distances
        final Distribution dDist = new Distribution();
        
        System.err.println("Processing documents.");
        // For all documents
        Iterator iDocIter = dsSrc.getTrainingSet().iterator();
        ThreadList t = new ThreadList(4);
        
        while (iDocIter.hasNext()) {
            // Get n-gram document
            final NGramDocument dCurDoc = new SimpleTextDocument(iMinNGram, iMaxNGram, iDist);
            final String sFile = ((CategorizedFileEntry)iDocIter.next()).getFileName();
            System.err.print(".");
           
            // Multi-threading
            while (!t.addThreadFor(new Runnable() {
                public void run() {
                    synchronized (dCurDoc) {
                        dCurDoc.loadDataStringFromFile(sFile);
                    }
                    // and store
                    synchronized (hmDocumentIndex) {
                        hmDocumentIndex.put(sFile, dCurDoc);
                    }
                }
            }))
                Thread.yield();
            
        }
        try {
            t.waitUntilCompletion();
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
        System.err.println("\nProcessing complete.");
        
        int iTotalSize = dsSrc.getTrainingSet().size();
        int iCurProgress = 0;
        
        
        // First pass
        // For every document
        IClusterer cClusterer;
        if (sClusteringType.equals(COMPLETE_LINK))
            cClusterer = new CompleteLinkClusterer();
        else
            if (sClusteringType.equals(SINGLE_LINK))
                cClusterer = new SingleLinkClusterer();
            else
                cClusterer = new AverageLinkClusterer();

        // Create file name set for clustering
        HashSet hsDocNames = new HashSet();
        Iterator iDocs = dsSrc.getTrainingSet().iterator();
        while (iDocs.hasNext()) {
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)iDocs.next();
            hsDocNames.add(cfeCur.getFileName());
        }
        
        // Apply clustering
        final StandardDocumentComparator sdcComparator = 
                new StandardDocumentComparator(dGraphImportance);
        cClusterer.calculateClusters(hsDocNames, new SimilarityComparatorListener() {
            public ISimilarity getSimilarityBetween(Object oFirst, 
                    Object oSecond) throws InvalidClassException {
                String sCurDocArg = (String)oFirst;
                String sCompareToDocArg = (String)oSecond;
                GraphSimilarity sSimil;
                try {                    
                    synchronized (hmDocumentIndex) {
                        sSimil = sdcComparator.getSimilarityBetween(
                                (NGramDocument)hmDocumentIndex.get(sCurDocArg),
                                (NGramDocument)hmDocumentIndex.get(sCompareToDocArg));
                    }
                }
                catch (InvalidClassException ice) {
                    System.err.println("Cannot happen...");
                    ice.printStackTrace(System.err);
                    sSimil = new GraphSimilarity();
                }
                return sSimil;
            }
        });
        UniqueVertexGraph gHierarchy = cClusterer.getHierarchy();
                
        // Second pass - Create new CAS graph
        CASCGraph gTree = new CASCGraph();
        
        // DEBUG LINES
        // System.out.println("\n" + jinsect.utils.graphToDot(gHierarchy, false) + "\n\n");
        //////////////
        
        // Reformat vertices and determine their type
        Iterator iIter = gHierarchy.getEdgeSet().iterator();
        while (iIter.hasNext()) {
            Edge e = (Edge)iIter.next();
            
            Vertex vNewA = new VertexImpl(e.getVertexA().toString());
            Vertex vNewB = new VertexImpl(e.getVertexB().toString());
            
            // Add edge to new graph
            try {
                Edge eCur = gTree.addEdge(vNewA, vNewB);
                // DEBUG LINES
                // System.err.println("Adding " + eCur);
                //////////////

            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }

            StringBuffer sbLabel = new StringBuffer();
            String[] saFiles = vNewA.getLabel().split(",");
            if (saFiles.length > 1)
                // Not a leaf => latent, due to clustering algorithm.
                gTree.setVertexType(vNewA, gTree.LATENT);
            // Create new name
            Iterator iFiles = Arrays.asList(saFiles).iterator();
            while (iFiles.hasNext()) {
                sbLabel.append(getFileNameOnly((String)iFiles.next()) + "_");
            }
            vNewA.setLabel(sbLabel.toString());
            
            sbLabel = new StringBuffer();
            saFiles = vNewB.getLabel().split(",");
            if (saFiles.length > 1)
                // Not a leaf => latent, due to clustering algorithm.
                gTree.setVertexType(vNewB, gTree.LATENT);
            // Create new name
            iFiles = Arrays.asList(saFiles).iterator();
            while (iFiles.hasNext()) {
                sbLabel.append(getFileNameOnly((String)iFiles.next()) + "_");
            }            
            vNewB.setLabel(sbLabel.toString());
        }
        
        // Return result
        return gTree;
    }
    
    /** Returns a graph based on LDA analysis of the underlying n-gram frequencies.
     * The file CASC/LDAcascOutput.txt is expected as input.
     */
    public static CASCGraph getLDABasedGraph() {
        CASCGraph sgRes = new CASCGraph();
        
        TreeMap tDistros = new TreeMap();
        // Init feature vectors as distros
        try {
            FileReader frLDA = new FileReader(new File("CASC/LDAcascOutput.txt"));
            BufferedReader br = new BufferedReader(frLDA);
            
            String sCurLine = "";
            while ((sCurLine = br.readLine()) != null)
            {
                String[] saInp = sCurLine.split(" ");
                if (saInp.length < 2) // At least a doc and a value
                    continue;
                Distribution dCurDoc = new Distribution();
                String sDoc = saInp[0]; // Get document name
                for (int iCnt=1; iCnt < saInp.length; iCnt++) {
                    dCurDoc.setValue(Double.valueOf(iCnt), Double.valueOf(saInp[iCnt]).doubleValue());
                }
                tDistros.put(sDoc, dCurDoc);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        
        Distribution dSimils = new Distribution();
        Iterator iIter = tDistros.keySet().iterator();
        while (iIter.hasNext()) {
            String sCurDoc = (String)iIter.next();
            
            Iterator iOtherIter = tDistros.tailMap(sCurDoc).keySet().iterator();
            double dMinDist = Double.POSITIVE_INFINITY;
            String sParentDoc = null;
            while (iOtherIter.hasNext()) {
                String sOtherDoc = (String)iOtherIter.next();
                if (sCurDoc.equals(sOtherDoc))
                    continue;
                // and already connected documents
                if (gr.demokritos.iit.jinsect.utils.locateEdgeInGraph(sgRes, sOtherDoc, 
                        sCurDoc) != null)
                {
                    //System.err.println("Ignoring " + sCompareToDoc + " as already connected.");
                    continue;
                }
                
                // EUCLIDEAN - Bad results
                // double dCurDist = euclideanDistance((Distribution)tDistros.get(sCurDoc), 
                //         (Distribution)tDistros.get(sOtherDoc));
                // KL - Bad results
                //double dCurDist = KLDivergenceCalculator.KL_asymmetric(
                        //(Distribution)tDistros.get(sCurDoc), 
                        //(Distribution)tDistros.get(sOtherDoc));
                //double dCurDist = manhattanDistance((Distribution)tDistros.get(sCurDoc), 
                        //(Distribution)tDistros.get(sOtherDoc));
                double dCurDist = MPDDistance((Distribution)tDistros.get(sCurDoc), 
                        (Distribution)tDistros.get(sOtherDoc));
                dSimils.setValue(dCurDist, dSimils.getValue(dCurDist) + 1);
                if (dCurDist < dMinDist) {
                    sParentDoc = sOtherDoc;
                    dMinDist = dCurDist;
                }
            }
            try {
                if (sParentDoc != null)
                    sgRes.addEdge(new VertexImpl(sParentDoc), new VertexImpl(sCurDoc), 
                            (1 - dMinDist) * 100);
                
            } catch (Exception ex) {
                System.err.println("Cannot add edge. Error:");
                ex.printStackTrace(System.err);
            }
        }
        System.err.println("Manhattan Similarity distribution (LDA):" + dSimils.average(false) +
                "," + dSimils.standardDeviation(false));
        return sgRes;
    }
    
    /**
     *@author ilias
     *@author ggianna
     *
     * The computation of Euclidean distance is performed.
     * This measure is symmetric and non-negative.
     *
     * @param p the first distribution, as a {@link Distribution}.
     * @param q the second distribution, as a {@link Distribution}.
     * @return  zero if q and p are equal - the euclidean distance, viewing 
     * the distributions as feature vectors.
     */
    public static double euclideanDistance(Distribution p, Distribution q) {
        double sum=0;
        if(p.asTreeMap().size()==p.asTreeMap().size()) {
            Iterator iDimIter = p.asTreeMap().keySet().iterator();
            while (iDimIter.hasNext()) {
                Object oCurKey = iDimIter.next();
                sum+=Math.pow(p.getValue(oCurKey)-q.getValue(oCurKey), 2.0);
            }
        } else {
            return 0.0;
        }
        return Math.cbrt(sum);
    }
    
    /**
     *@author ilias
     *@author ggianna
     *
     * The computation of most prominent delta (MPD) distance is performed.
     * This measure is symmetric and non-negative.
     *
     * @param p the first distribution, as a {@link Distribution}.
     * @param q the second distribution, as a {@link Distribution}.
     * @return  zero if q and p are equal - the MPD distance, viewing 
     * the distributions as feature vectors.
     */
    public static double MPDDistance(Distribution p, Distribution q) {
        double dMaxDelta=Double.NEGATIVE_INFINITY;
        if(p.asTreeMap().size()==p.asTreeMap().size()) {
            Iterator iDimIter = p.asTreeMap().keySet().iterator();
            while (iDimIter.hasNext()) {
                Object oCurKey = iDimIter.next();
                if (Math.abs(p.getValue(oCurKey)-q.getValue(oCurKey)) > dMaxDelta)
                    dMaxDelta = Math.abs(p.getValue(oCurKey)-q.getValue(oCurKey));
            }
        } else {
            return 0.0;
        }
        return dMaxDelta;
    }
    
    /**
     *@author ilias
     *@author ggianna
     *
     * The computation of Manhattan distance is performed.
     * This measure is symmetric and non-negative.
     *
     * @param p the first distribution, as a {@link Distribution}.
     * @param q the second distribution, as a {@link Distribution}.
     * @return  zero if q and p are equal - the Manhattan distance, viewing 
     * the distributions as feature vectors.
     */
    public static double manhattanDistance(Distribution p, Distribution q) {
        double sum=0;
        if(p.asTreeMap().size()==p.asTreeMap().size()) {
            Iterator iDimIter = p.asTreeMap().keySet().iterator();
            while (iDimIter.hasNext()) {
                Object oCurKey = iDimIter.next();
                sum+=Math.abs(p.getValue(oCurKey)-q.getValue(oCurKey));
            }
        } else {
            return 0;
        }
        return sum;
    }
    
    public static void addParent(HashMap hParents, Vertex vA, Vertex vB, CASCGraph gTree,
            double dWeightToParent) {
        Vertex vParA = (Vertex)hParents.get(vA);
        Vertex vParB = (Vertex)hParents.get(vB);
                
        if (vParA == vParB) {
            if (vParA == null)
            {        
                Vertex vPar = gTree.setVertexType(new VertexImpl("PAR" + hParents.size()),
                        gTree.LATENT); // Latent node
                try {

                    gTree.addEdge(vPar, vA, dWeightToParent);
                    gTree.addEdge(vPar, vB, dWeightToParent);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
                // Update parent list
                hParents.put(vA, vPar);
                hParents.put(vB, vPar);
            }
        }
        else 
            if ((vParA == null) || (vParB == null)) {
                Vertex vPar;
                if (vParA != null)
                    vPar = vParA;
                else
                    vPar = vParB;
                
                try {
                    if (vParA == null)
                        gTree.addEdge(vPar, vA, dWeightToParent);
                    if (vParB == null)
                        gTree.addEdge(vPar, vB, dWeightToParent);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
                // Update parent list
                hParents.put(vA, vPar);
                hParents.put(vB, vPar);
            
            }
            else {
                // Every parent is created to approximate match
                addParent(hParents, vParA, vParB, gTree, (dWeightToParent + 1.0) / 2.0);
            }
                
    }
    
    public static double calcCASCDistanceBetween(CASCGraph gCorrect, CASCGraph g2) {
        CASCDistanceCalculator cdcCalc = null;
        try {
            cdcCalc = new CASCDistanceCalculator("CASC/dist");
        } catch (IOException ex) {
            System.err.println("Cannot execute CASC distance calculator. CALCDist should exist in the" +
                    " path and be executable.");
            ex.printStackTrace(System.err);
            return Double.NEGATIVE_INFINITY;
        }
        return cdcCalc.getDistanceFromDOTGraph(graphToCASCDot(gCorrect),
                graphToCASCDot(g2));
    }
    
    public static double calcDistanceBetween(CASCGraph gCorrect, CASCGraph g2) {
        return calcDistanceBetween(gCorrect, g2, true);
    }
    public static double calcDistanceBetween(CASCGraph gCorrect, CASCGraph g2, boolean bSilent)
    {
        // Normalization factor
        int iNumberOfNodes = 0;
        double dRes = 0; 
        TreeSet tsVisited = new TreeSet();
        
        // Init vertex set
        Iterator iIter = gCorrect.getVerticesIterator();
        // by adding ONLY document vertices to a sorted map
        TreeSet tsVertexSet = new TreeSet();
        while (iIter.hasNext()) {
            Vertex v = (Vertex)iIter.next();
            // Check for lowercase start
            if (gCorrect.getVertexType(v) == gCorrect.NORMAL)
                tsVertexSet.add(v.getLabel());
        }
        
        Iterator iA = tsVertexSet.iterator();
        while (iA.hasNext()) {
            Vertex vA = new VertexImpl((String)iA.next());
            Iterator iB = tsVertexSet.iterator();
            
            while (iB.hasNext()) {
                Vertex vB = new VertexImpl((String)iB.next());
                
                if (vA.getLabel().equals(vB.getLabel()))
                    continue;
                
                Iterator iC = tsVertexSet.iterator();
                while (iC.hasNext()) {
                    // Calc distance for gCorrect
                    Vertex vC = new VertexImpl((String)iC.next());
                    
                     if (vA.getLabel().equals(vC.getLabel())
                        || vB.getLabel().equals(vC.getLabel()))
                        continue; // Do NOT check circles
                    
                    Triplet tCur = new Triplet(vA.getLabel(), vB.getLabel(),
                            vC.getLabel());
                    if (tsVisited.contains(tCur))
                        continue;
                        
                    Vertex vLocalA = (Vertex)gCorrect.UniqueVertices.get(vA.getLabel());
                    Vertex vLocalB = (Vertex)gCorrect.UniqueVertices.get(vB.getLabel());
                    Vertex vLocalC = (Vertex)gCorrect.UniqueVertices.get(vC.getLabel());
                    
                    double dDistAB1 = gCorrect.getShortestLinkBetween(vA, vB).size();
                    double dDistAC1 = gCorrect.getShortestLinkBetween(vA, vC).size();

                    // Calc distance for g2
                    vLocalA = (Vertex)g2.UniqueVertices.get(vA.getLabel());
                    vLocalB = (Vertex)g2.UniqueVertices.get(vB.getLabel());
                    vLocalC = (Vertex)g2.UniqueVertices.get(vC.getLabel());
                    // Ignore non-common nodes
                    if ((vLocalA == null) || (vLocalB == null) || (vLocalC == null))
                        continue;
                    
                    double dDistAB2 = g2.getShortestLinkBetween(vLocalA, vLocalB).size();
                    double dDistAC2 = g2.getShortestLinkBetween(vLocalA, vLocalC).size();
                    
                    // Check matching
                    if (!bSilent)
                        // Indicate problem
                        System.err.println("Disagreement for nodes:" + vA.getLabel() +
                                "," + vB.getLabel() + "," + vC.getLabel());
                    
                    dRes += 1 - (Math.abs(gr.demokritos.iit.jinsect.utils.sign(dDistAB1 - dDistAC1) -
                            gr.demokritos.iit.jinsect.utils.sign(dDistAB2 - dDistAC2)) / 2);
                    //System.err.println("Adding..." + (1 - (Math.abs(jinsect.utils.sign(dDistAB1 - dDistAC1) -
                            //jinsect.utils.sign(dDistAB2 - dDistAC2)) / 2)));
                    iNumberOfNodes++;
                    
                    // Add to visited
                    tsVisited.add(tCur);
                }
            }
        }
        
        return dRes / iNumberOfNodes;
    }
    
    public static CASCGraph getExampleGraph() {
        CASCGraph gTree = new CASCGraph();
        try {
            // Top subgraph
            gTree.addEdge(gTree.setVertexType(new VertexImpl("1"), gTree.LATENT),
                    new VertexImpl("p3"));
            gTree.addEdge(new VertexImpl("1"), 
                    gTree.setVertexType(new VertexImpl("2"), gTree.LATENT));
            
            gTree.addEdge(gTree.setVertexType(new VertexImpl("2"), gTree.LATENT), 
                    new VertexImpl("p16"));
            gTree.addEdge(new VertexImpl("2"), 
                    gTree.setVertexType(new VertexImpl("3"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("3"), 
                    gTree.setVertexType(new VertexImpl("4"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("4"), 
                    gTree.setVertexType(new VertexImpl("7"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("4"), 
                    gTree.setVertexType(new VertexImpl("8"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("8"), 
                    gTree.setVertexType(new VertexImpl("12"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("8"), 
                    gTree.setVertexType(new VertexImpl("13"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("3"), 
                    gTree.setVertexType(new VertexImpl("5"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("5"), 
                    gTree.setVertexType(new VertexImpl("6"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("6"), 
                    gTree.setVertexType(new VertexImpl("11"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("6"), 
                    gTree.setVertexType(new VertexImpl("10"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("5"), 
                    gTree.setVertexType(new VertexImpl("9"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("9"), 
                    gTree.setVertexType(new VertexImpl("14"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("9"), 
                    gTree.setVertexType(new VertexImpl("15"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("7"), new VertexImpl("p10"));
            gTree.addEdge(new VertexImpl("7"), new VertexImpl("p5"));
            
            
            gTree.addEdge(new VertexImpl("10"), new VertexImpl("p1"));
            gTree.addEdge(new VertexImpl("10"), new VertexImpl("p4"));            
            gTree.addEdge(new VertexImpl("11"), new VertexImpl("p7"));
            gTree.addEdge(new VertexImpl("11"), new VertexImpl("p9"));
            
            gTree.addEdge(new VertexImpl("12"), new VertexImpl("p6"));
            gTree.addEdge(new VertexImpl("12"), new VertexImpl("p14"));
            gTree.addEdge(new VertexImpl("13"), new VertexImpl("p8"));
            gTree.addEdge(new VertexImpl("13"), new VertexImpl("p11"));

            gTree.addEdge(new VertexImpl("14"), new VertexImpl("p12"));
            gTree.addEdge(new VertexImpl("14"), new VertexImpl("p13"));
            gTree.addEdge(new VertexImpl("15"), new VertexImpl("p2"));
            gTree.addEdge(new VertexImpl("15"), new VertexImpl("p15"));
        } catch (Exception ex) {
            System.err.println("Cannot add edge. Reason:");
            ex.printStackTrace(System.err);
        }
        return gTree;
        
    }
    
    public static CASCGraph getDummyGraph() {
        CASCGraph gTree = new CASCGraph();
        try {
            // Top subgraph
            gTree.addEdge(new VertexImpl("1"),
                    gTree.setVertexType(new VertexImpl("p3"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("1"), 
                    new VertexImpl("2"));
            
            gTree.addEdge(new VertexImpl("2"), 
                    gTree.setVertexType(new VertexImpl("p16"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("2"), 
                    gTree.setVertexType(new VertexImpl("3"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("3"), 
                    gTree.setVertexType(new VertexImpl("4"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("4"), 
                    gTree.setVertexType(new VertexImpl("7"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("4"), 
                    gTree.setVertexType(new VertexImpl("8"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("8"), 
                    gTree.setVertexType(new VertexImpl("12"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("8"), 
                    gTree.setVertexType(new VertexImpl("13"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("3"), 
                    gTree.setVertexType(new VertexImpl("5"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("5"), 
                    gTree.setVertexType(new VertexImpl("6"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("6"), 
                    gTree.setVertexType(new VertexImpl("11"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("6"), 
                    gTree.setVertexType(new VertexImpl("10"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("5"), 
                    gTree.setVertexType(new VertexImpl("9"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("9"), 
                    gTree.setVertexType(new VertexImpl("14"), gTree.LATENT));
            gTree.addEdge(new VertexImpl("9"), 
                    gTree.setVertexType(new VertexImpl("15"), gTree.LATENT));
            
            gTree.addEdge(new VertexImpl("7"), new VertexImpl("p10"));
            gTree.addEdge(new VertexImpl("7"), new VertexImpl("p5"));
            
            
            gTree.addEdge(new VertexImpl("10"), new VertexImpl("p1"));
            gTree.addEdge(new VertexImpl("10"), new VertexImpl("p4"));            
            gTree.addEdge(new VertexImpl("11"), new VertexImpl("p7"));
            gTree.addEdge(new VertexImpl("11"), new VertexImpl("p9"));
            
            gTree.addEdge(new VertexImpl("12"), new VertexImpl("p6"));
            gTree.addEdge(new VertexImpl("12"), new VertexImpl("p14"));
            gTree.addEdge(new VertexImpl("13"), new VertexImpl("p8"));
            gTree.addEdge(new VertexImpl("13"), new VertexImpl("p11"));

            gTree.addEdge(new VertexImpl("14"), new VertexImpl("p12"));
            gTree.addEdge(new VertexImpl("14"), new VertexImpl("p13"));
            gTree.addEdge(new VertexImpl("15"), new VertexImpl("p2"));
            gTree.addEdge(new VertexImpl("15"), new VertexImpl("p15"));
        } catch (Exception ex) {
            System.err.println("Cannot add edge. Reason:");
            ex.printStackTrace(System.err);
        }
        return gTree;
        
    }
    
    public static CASCGraph getDefaultGraph() {
        CASCGraph gTree = new CASCGraph();
        try {
            // Top subgraph
            Vertex vFrom = gTree.setVertexType(new VertexImpl("Start"), gTree.LATENT);
            Vertex vTo = gTree.setVertexType(new VertexImpl("NE1"), gTree.LATENT);
            gTree.addEdge(vFrom, vTo);
            
            gTree.addEdge(new VertexImpl("NE1"), new VertexImpl("p8"));
            gTree.addEdge(new VertexImpl("NE1"), new VertexImpl("p11"));
            vTo = gTree.setVertexType(new VertexImpl("NE1NW1"), gTree.LATENT);
            gTree.addEdge(new VertexImpl("NE1"), vTo);
            
            gTree.addEdge(new VertexImpl("p8"), new VertexImpl("p14"));
            gTree.addEdge(new VertexImpl("p8"), new VertexImpl("p6"));
            
            gTree.addEdge(new VertexImpl("NE1NW1"), new VertexImpl("p5"));
            gTree.addEdge(new VertexImpl("NE1NW1"), new VertexImpl("p10"));
            
            // SW Subgraph
            vTo = gTree.setVertexType(new VertexImpl("SW1"), gTree.LATENT);
            gTree.addEdge(new VertexImpl("Start"), vTo);
            
            gTree.addEdge(vTo, new VertexImpl("p2"));
            gTree.addEdge(vTo, new VertexImpl("p16"));
            gTree.addEdge(vTo, new VertexImpl("p13"));
            gTree.addEdge(vTo, new VertexImpl("p15"));
            
            gTree.addEdge(new VertexImpl("p13"), new VertexImpl("p12"));
            
            // SE Subgraph
            gTree.addEdge(new VertexImpl("Start"), new VertexImpl("p9"));
            
            gTree.addEdge(new VertexImpl("p9"), new VertexImpl("p7"));
            
            vTo = gTree.setVertexType(new VertexImpl("P9SE1"), gTree.LATENT);
            gTree.addEdge(new VertexImpl("p9"), vTo);
            
            gTree.addEdge(new VertexImpl("p7"), new VertexImpl("p3"));
            
            gTree.addEdge(new VertexImpl(vTo), new VertexImpl("p4"));
            gTree.addEdge(new VertexImpl(vTo), new VertexImpl("p1"));
        } catch (Exception ex) {
            System.err.println("Cannot add edge. Reason:");
            ex.printStackTrace(System.err);
        }
        return gTree;
    }
    
    public static CASCGraph getRandomGraph() {
        CASCGraph gTree = new CASCGraph();
        int NO_OF_DOCUMENTS = 16; // Number of documents
        
        // Init doc list
        ArrayList l = new ArrayList();
        for (int iDocNum=1; iDocNum <= NO_OF_DOCUMENTS; iDocNum++) {
            l.add("p" + iDocNum);
        }
        
        try {
            int iParentInc = 0;
            // Create edges
            Iterator iNodes = l.iterator();
            while (iNodes.hasNext()) {
                String sCurNode = (String)iNodes.next();
                
                // Randomly select another node
                String sOtherNode = sCurNode;
                while (sOtherNode == sCurNode)
                    sOtherNode = (String)l.get((int)(Math.random() * l.size()));
                // 1 / NO_OF_DOCUMENTS probability for common parent
                if (Math.random() < 0.33)
                {
                    String sParent = String.valueOf("PAR" + iParentInc++);
                    Vertex vPar = new VertexImpl(sParent);
                    gTree.setVertexType(vPar, gTree.LATENT);
                    
                    // Create common parent
                    gTree.addEdge(new VertexImpl(sCurNode), vPar);
                    gTree.addEdge(new VertexImpl(sOtherNode), vPar);
                    gTree.addEdge(new VertexImpl("TOPNODE"), vPar);
                }
                else
                    gTree.addEdge(new VertexImpl(sCurNode), new VertexImpl(sOtherNode));
            }
            
        } catch (Exception ex) {
            System.err.println("Cannot add edge. Reason:");
            ex.printStackTrace(System.err);
        }
        return gTree;
        
    }
    
    public static String getFileNameOnly(String sFilePath) {
        String sFilename = new File(sFilePath).getName();
        return sFilename.substring(0, 
                sFilename.lastIndexOf(".") < 0 ? sFilename.length() : sFilename.lastIndexOf("."));
    }
    
    private static String getLDAArray(TreeMap hmDocs) {
        String sDocOrder = "";
        // Get the full sorted vocabulary
        TreeMap t = getVocabulary(hmDocs);
        
        // Init string buffer
        StringBuffer sbRes = new StringBuffer();
        // For every document
        Iterator iIter = hmDocs.keySet().iterator();
        while (iIter.hasNext()) {
            String sCurDoc =  (String)iIter.next();
            sDocOrder += sCurDoc + "\n";
            NGramDocument nCur = (NGramDocument)hmDocs.get(sCurDoc);
            
            Distribution dCurDoc = new Distribution();
            dCurDoc.asTreeMap().putAll(nCur.getDocumentHistogram().NGramHistogram);
            
            // For each n-gram in the full vocabulary
            Iterator iNGramIter = t.entrySet().iterator();
            while (iNGramIter.hasNext()) {
                Map.Entry e = (Map.Entry)iNGramIter.next();
                Object oCurNGram = e.getKey();
                
                sbRes.append((int)dCurDoc.getValue(oCurNGram) + " ");
            }
            sbRes.append("\n");
        }
        
        System.err.println("Document order:\n" + sDocOrder);
        return sbRes.toString();
    }
    
    /** Returns the sorted vocabulary of a given map of type &lt filename, {@link NGramDocument} &gt
     *@param hmDocs The map of filenames and their documents.
     *@return The distribution of the vocabulary (of n-grams).
     *@see Distribution
     */
    private static TreeMap getVocabulary(TreeMap hmDocs) {
        TreeMap hmOverallNGramList = new TreeMap();
        Iterator iIter = hmDocs.keySet().iterator();
        while (iIter.hasNext()) {
            NGramDocument nCur = (NGramDocument)hmDocs.get((String)iIter.next());
            
            // Get all n-grams
            Iterator iNGramIter = nCur.getDocumentHistogram().NGramHistogram.keySet().iterator();
            while (iNGramIter.hasNext())
                hmOverallNGramList.put(iNGramIter.next(), 1.0);
        }
        
        return hmOverallNGramList;
    }
    
    public static void testGraphDist() {
        CASCGraph g1 = new CASCGraph();
        try {
            g1.addEdge(new VertexImpl("1"), new VertexImpl("2"));
            g1.addEdge(new VertexImpl("1"), new VertexImpl("3"));
            g1.addEdge(new VertexImpl("2"), new VertexImpl("4"));
            g1.addEdge(new VertexImpl("4"), new VertexImpl("5"));
            g1.addEdge(new VertexImpl("6"), new VertexImpl("5"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        System.out.println(graphToCASCDot(g1));
        
        CASCGraph g2 = new CASCGraph();
        try {
            g2.addEdge(new VertexImpl("1"), new VertexImpl("6"));
            g2.addEdge(new VertexImpl("1"), new VertexImpl("5"));
            g2.addEdge(new VertexImpl("2"), new VertexImpl("3"));
            g2.addEdge(new VertexImpl("4"), new VertexImpl("6"));
            g2.addEdge(new VertexImpl("6"), new VertexImpl("3"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        System.out.println(graphToCASCDot(g2));
        
        CASCGraph g3 = new CASCGraph();
        try {
            g3.addEdge(new VertexImpl("1"), new VertexImpl("2"));
            g3.addEdge(new VertexImpl("1"), new VertexImpl("3"));
            g3.addEdge(new VertexImpl("2"), new VertexImpl("4"));
            g3.addEdge(new VertexImpl("4"), new VertexImpl("5"));
            g3.addEdge(new VertexImpl("6"), new VertexImpl("4"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        System.out.println(graphToCASCDot(g3));
        
        System.out.println("Similarity to self: " + calcCASCDistanceBetween(g1, g1));
        System.out.println("Similarity to g2: " + calcCASCDistanceBetween(g1, g2));
        System.out.println("Similarity to g3: " + calcCASCDistanceBetween(g1, g3));
        
        System.out.println("Similarity to self: " + calcDistanceBetween(g1, g1));
        System.out.println("Similarity to g2: " + calcDistanceBetween(g1, g2));
        System.out.println("Similarity to g3: " + calcDistanceBetween(g1, g3));
    }

    /** Renders a graph to its DOT representation (See GraphViz for more info on the format). The DOT
     * file follows the CASC (Stemmatology Challenge) directives.
     *@param gTree The input graph.
     *@return The DOT formatted string representation of the graph.
     */
    public static String graphToCASCDot(CASCGraph gTree) {
        StringBuffer sb = new StringBuffer();
        String sConnector;
        
        // Render graph
        sb.append("graph {\n");
        sConnector = "--";
        
        Iterator iIter = gTree.getEdgeSet().iterator();
        TreeSet tsDescribed = new TreeSet();
        
        while (iIter.hasNext()) {
            Edge e = (Edge)iIter.next();
            
            // Render not already described vertices
            if (!tsDescribed.contains(e.getVertexA().getLabel())) {
                tsDescribed.add(e.getVertexA().getLabel());
                if (gTree.getVertexType(e.getVertexA()) == gTree.NORMAL)
                    // NORMAL
                    sb.append("\t" + e.getVertexA().getLabel() + 
                        " [label=\"" + e.getVertexA().getLabel() + "\" shape=plaintext fontsize=24]\n");
                else
                    // LATENT
                    sb.append("\t" + e.getVertexA().getLabel() + 
                        " [shape=point]\n");
            }
            
            if (!tsDescribed.contains(e.getVertexB().getLabel())) {
                tsDescribed.add(e.getVertexB().getLabel());
                if (gTree.getVertexType(e.getVertexB()) == gTree.NORMAL)
                    // NORMAL
                    sb.append("\t" + e.getVertexB().getLabel() + 
                        " [label=\"" + e.getVertexB().getLabel() + "\" shape=plaintext fontsize=24]\n");
                else
                    // LATENT
                    sb.append("\t" + e.getVertexB().getLabel() + 
                        " [shape=point]\n");
            }
            
            // Render edge
            if (e instanceof WeightedEdge)
                sb.append("\t" + e.getVertexA() + " " + sConnector + " " + e.getVertexB() + 
                    " [weight=" + (int)(((WeightedEdge)e).getWeight() * 100) + "]\n");
            else
                sb.append("\t" + e.getVertexA() + " " + sConnector + " " + e.getVertexB() + "\n");
        }
        sb.append("}");
        
        return sb.toString();
    }
}

class Triplet implements Comparable {
    private String sString;
    public Triplet(String sV1, String sV2, String sV3) {
        TreeSet tsSet = new TreeSet();
        tsSet.add(sV1 + "_");
        tsSet.add(sV2 + "_");
        tsSet.add(sV3 + "_");
        
        sString = tsSet.toString();
    }
    
    public boolean equals(Object oObj) {
        return ((Triplet)oObj).indexOf(sString) >= 0;
    }
    
    public int indexOf(String s) {
        return sString.indexOf(s);
        
    }
    
    public int compareTo(Object o) {
        return sString.compareTo(((Triplet)o).sString);
    }
    
}