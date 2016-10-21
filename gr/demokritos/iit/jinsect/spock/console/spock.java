/*
 * spock.java
 *
 * Created on July 13, 2007, 12:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.spock.console;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.IDocumentSet;
import gr.demokritos.iit.jinsect.algorithms.clustering.AverageLinkClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.CompleteLinkClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.IClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.SingleLinkClusterer;
import gr.demokritos.iit.jinsect.casc.structs.CASCGraph;
import gr.demokritos.iit.jinsect.documentModel.comparators.CachedDocumentComparator;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramSymWinDocument;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextDocument;
import gr.demokritos.iit.jinsect.documentModel.comparators.StandardDocumentComparator;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.interoperability.HTMLTagRemoverInteroperator;
import gr.demokritos.iit.jinsect.interoperability.SPOCKEval;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import gr.demokritos.iit.jinsect.threading.ThreadList;
import salvo.jesus.graph.Edge;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;

/**
 *
 * @author ggianna
 */
public class spock {
    
    public static final String SINGLE_LINK = "SingleLink";
    public static final String COMPLETE_LINK = "CompleteLink";
    public static final String AVERAGE_LINK = "AverageLink";
    
    public static void printUsage() {
        System.out.println("Syntax: spock InputDirectory [-evaluatorPath=EvalPath] [-optimizeNGramParams]" +
                " [-showNGramGraph]" +
                " [-showAggloLinkedCharGraph]" +
                " [-limitTo=name]" +
                "[-aggloGraphMethod=" + SINGLE_LINK + "|" + AVERAGE_LINK + "|" + COMPLETE_LINK + "]\n");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(0);
        }
        
        // Read command line
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        System.err.println("Using parameters:" + hSwitches.toString());
        Boolean bOptimizeNGramParams = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "optimizeNGramParams", "" + Boolean.FALSE));
        Boolean bShowNGramGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "showNGramGraph", "" + Boolean.TRUE));
        Boolean bShowAggloLinkedCharGraph = Boolean.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "showAggloLinkedCharGraph", "" + Boolean.FALSE));
        String sAggloGraphMethod = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "aggloGraphMethod", AVERAGE_LINK);
        String sEvalPath = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "evaluatorPath", null);
        String sLimitTo = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "limitTo", null);
        // Check if selected method is valid
        if ((SINGLE_LINK + "," + COMPLETE_LINK + "," + 
                AVERAGE_LINK).toLowerCase().indexOf(
                sAggloGraphMethod.toLowerCase()) < 0)
        {
            System.err.println("Invalid agglomeration method.\nValid options:" + SINGLE_LINK + 
                    "," + COMPLETE_LINK + "," + 
                    AVERAGE_LINK + "\nDefaulting to average.");
            sAggloGraphMethod = AVERAGE_LINK;
        }
        
        double distance;
        String sInput = args[0]; // Get input dir
        SPOCKEval seEval = null;
        try {
            seEval = new SPOCKEval(sEvalPath);
        } catch (IOException ex) {
            System.err.println("Cannot locate SPOCK evaluation script:" + sEvalPath);
            ex.printStackTrace(System.err);
            //return;
        }
        
        FileFormat fOut = new FileFormat();
//        IDocumentSet dsSrc = null;
//        if (new File(sInput).isDirectory())
//            dsSrc = new DocumentSet(sInput,1.0);
//        else {
//            FileFormat ffIn = new FileFormat();
//            ffIn.parseFile(sInput);
//            dsSrc = ffIn;
//        }
//        dsSrc.createSets();
        
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
                            CASCGraph gTree = 
                                    getNGramDistanceGraph(sInput, sLimitTo, iMinSize, iMaxSize, iDist,
                                    dGraphImp, true, tmDocs);
                            //CASCGraph gTree = getNGramDistanceAgglomerativelyClusteredGraph(sInputDir, 
                                    //iMinSize, iMaxSize, iDist, AVERAGE_LINK);
                            
                            // Create clusters
                            Hashtable toUpdate = fOut.getClusters();
                            updateHashtable(toUpdate, gTree, "test");
                            fOut.setClusters(toUpdate);
                            String sFile = "./tempClusters.spock";
                            // Output file
                            fOut.createFile(sFile);
                            // Run evaluation
                            distance=seEval.evaluate(gr.demokritos.iit.jinsect.utils.loadFileToString(sFile));
                            
                            if (distance > dBest) {
                                // Output star for new best result
                                System.out.print("*");
                                System.out.println("Config:" + iMinSize + "," + 
                                        iMaxSize + "," + iDist + "," + dGraphImp + 
                                        "(" + distance + ")");
                                dBest = distance;
                                sBest = ""; // TODO: Use from ilias
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
        
//        System.err.println("Using for WORD n-grams params: 1,1,8,0.6 (Result 0.74)");
//        System.err.println("Using for CHAR n-grams params: 3,4,1,0.8 (Result 0.791)");
        
        if (bShowNGramGraph) {
            CASCGraph gTree = getNGramDistanceGraph(sInput, sLimitTo, 1, 1, 3,
                    0.8, false, new TreeMap());
            //System.out.println(graphToCASCDot(gTree));
            
            // Create clusters
            Hashtable toUpdate = fOut.getClusters();
            updateHashtable(toUpdate, gTree, "test");
            fOut.setClusters(toUpdate);
            String sFile = "./tempClusters.spock";
            // Output file
            fOut.createFile(sFile);
            // Run evaluation
            distance=seEval.evaluate(gr.demokritos.iit.jinsect.utils.loadFileToString(sFile));
            System.out.println("Similarity:" + distance);
            
        }
        
        // Output results
        if (bShowAggloLinkedCharGraph) {
            System.out.println();
            System.out.println("\nOur NGram Agglo-AverageLink answer:");
            CASCGraph gAgglAverage = getNGramDistanceAgglomerativelyClusteredGraph(sInput, sLimitTo,
                    3, 4, 1, 0.8, sAggloGraphMethod);
            //System.out.println(jinsect.casc.console.casc.graphToCASCDot(gAgglAverage));
            // Create clusters
            Hashtable toUpdate = fOut.getClusters();
            updateHashtable(toUpdate, gAgglAverage, "test");
            fOut.setClusters(toUpdate);
            String sFile = "./tempClusters.spock";
            // Output file
            fOut.createFile(sFile);
            // Run evaluation
            distance=seEval.evaluate(gr.demokritos.iit.jinsect.utils.loadFileToString(sFile));
            System.out.println("Similarity:" + distance);
        }

        
    }
    
    public static void updateHashtable(Hashtable hToUpdate, CASCGraph gClusters, String sName) {
        ArrayList<Set> lCurrentSets = new ArrayList<Set>();
        Iterator iVertices = gClusters.getVerticesIterator();
        while (iVertices.hasNext()) {
            Vertex v = (Vertex)iVertices.next();
            Iterator<Set> iCurrentSets = lCurrentSets.iterator();
            boolean bAlreadyContained = false;
            while (iCurrentSets.hasNext()) {
                Set sCur = iCurrentSets.next();
                if (sCur.contains(v.toString()))
                {
                    bAlreadyContained = true;
                    break;
                }
            }
            if (bAlreadyContained)
                continue;
            
            // Add connected set as cluster
            Set sVertices = gClusters.getConnectedSet(v);
            TreeSet<String> sFilesInCluster = new TreeSet<String>();
            Iterator ivFileNames = sVertices.iterator();
            // Add all vertex labels (i.e. filenames) to cluster
            while (ivFileNames.hasNext()) {
                Vertex vCur = (Vertex)ivFileNames.next();
                sFilesInCluster.add(vCur.toString());
            }
            
            LinkedList lCur;
            if (hToUpdate.contains(sName))
                lCur = ((LinkedList)hToUpdate.get(sName));
            else {
                lCur = new LinkedList();
                hToUpdate.put(sName,lCur);
            }

            lCur.add(new TreeSet(sFilesInCluster));
            lCurrentSets.add(sFilesInCluster);
        }
    }
    
    public static CASCGraph getNGramDistanceGraph(String sInput, String sLimitTo,
            int iMinNGram, int iMaxNGram,
            int iDist, final double dGraphImportance, boolean bSilent, TreeMap hLoadedDocs) {
        final TreeMap hmDocumentIndex = hLoadedDocs;
        
        // Create document set
        IDocumentSet dsSrc = null;
        if (new File(sInput).isDirectory())
            dsSrc = new DocumentSet(sInput,1.0);
        else {
            FileFormat ffIn = new FileFormat();
            ffIn.parseFile(sInput);
            dsSrc = ffIn;
        }
        dsSrc.createSets();
        
        // Init distribution of distances
        final Distribution dDist = new Distribution();
        
        if (!bSilent)
            System.err.println("Processing documents.");
        // For all documents
        Iterator iDocIter;
        if (sLimitTo == null)
            iDocIter = dsSrc.getTrainingSet().iterator();
        else
            iDocIter = dsSrc.getFilesFromCategory(sLimitTo).iterator();
        
        ThreadList t = new ThreadList(Runtime.getRuntime().availableProcessors());
        HTMLTagRemoverInteroperator htrRemover = null;
        try {
            htrRemover = new HTMLTagRemoverInteroperator(null);
        } catch (IOException ex) {
            System.err.println("Cannot find HTML tag remover.");
            ex.printStackTrace();
            return null;
        }
               
//        while (iDocIter.hasNext()) {
//            // Get n-gram document
//            final String sFile = ((CategorizedFileEntry)iDocIter.next()).getFileName();
//            if (hmDocumentIndex.containsKey(sFile))
//                continue; // Ignore already calculated files
//            
//            final NGramDocument dCurDoc = new NGramDocument(iMinNGram, iMaxNGram, iDist,
//                    iMinNGram, iMaxNGram);
//            final HTMLTagRemoverInteroperator htrRemoverArg = htrRemover;
//            // Multi-threading
//            while (!t.addThreadFor(new Runnable() {
//                public void run() {
//                    synchronized (dCurDoc) {
//                        dCurDoc.setDataString(htrRemoverArg.removeTagsFromFile(sFile));
//                    }
//                    // and store
//                    synchronized (hmDocumentIndex) {
//                        hmDocumentIndex.put(sFile, dCurDoc);
//                    }
//                }
//            }))
//                Thread.yield();
//            
//            if (!bSilent)
//                System.err.print(".");
//        }
//        try {
//            t.waitUntilCompletion();
//        } catch (InterruptedException ex) {
//            ex.printStackTrace(System.err);
//            return null;
//        }
//        if (!bSilent)
//            System.err.println("\nProcessing complete.");
                
        CASCGraph gTree = new CASCGraph();
        
        int iTotalSize = dsSrc.getTrainingSet().size();
        int iCurProgress = 0;
        
        
        // First pass
        // For every document
        TreeSet tsDocSet;
        if (sLimitTo == null)
            tsDocSet = new TreeSet(dsSrc.getTrainingSet());
        else
            tsDocSet = new TreeSet(dsSrc.getFilesFromCategory(sLimitTo));
        iDocIter = tsDocSet.iterator();
        int iTotalCmpCnt = tsDocSet.size() * (tsDocSet.size() - 1) / 2;
        int iCurCmpCnt = 0;
        Date dStartTime = new Date();
        
        while (iDocIter.hasNext()) {
            double dMaxSimil = Double.NEGATIVE_INFINITY;
            String sParentDoc = null;
            CategorizedFileEntry cfeCur = (CategorizedFileEntry)iDocIter.next();
            String sCurDoc = cfeCur.getFileName();
            
            // Compare with every other document
            Iterator iCompareToIter = tsDocSet.tailSet(cfeCur).iterator();
            // Multi-threading
            final Distribution dSimils = new Distribution();
            while (iCompareToIter.hasNext()) {
                String sCompareToDoc = ((CategorizedFileEntry)iCompareToIter.next()).getFileName();
                // Ignore self and parent
                if (sCompareToDoc.equals(sCurDoc)) {
                    //System.err.println("Ignoring " + sCompareToDoc);
                    continue;
                }
                
                // Params
                final String sCurDocArg = sCurDoc;
                final String sCompareToDocArg = sCompareToDoc;
                final HTMLTagRemoverInteroperator htrRemoverArg = htrRemover;
                final int iMinNGramArg = iMinNGram;
                final int iMaxNGramArg = iMaxNGram;
                final int iDistArg = iDist;
                final NGramSymWinDocument nd1 = new NGramSymWinDocument(iMinNGramArg, iMaxNGramArg, iDistArg,
                        iMinNGramArg, iMaxNGramArg);
                nd1.setDataString(htrRemoverArg.removeTagsFromFile(sCurDocArg));
                
                while (!t.addThreadFor(new Runnable() {
                    public void run() {
                        ///////////////////////
                        StandardDocumentComparator sdcComparator = 
                                new CachedDocumentComparator(dGraphImportance);
                        // Actually compare
                        GraphSimilarity sSimil;
                        try {                    
                            synchronized (hmDocumentIndex) {
                                NGramSymWinDocument nd2 = new NGramSymWinDocument(iMinNGramArg, iMaxNGramArg, iDistArg,
                                        iMinNGramArg, iMaxNGramArg);
                                // REMOVE TAGS
                                nd2.setDataString(htrRemoverArg.removeTagsFromFile(sCompareToDocArg));
                                
                                sSimil = sdcComparator.getSimilarityBetween(nd1, nd2);
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
                iCurCmpCnt++;
                Date dCurTime = new Date();
                long lRemaining = (iTotalCmpCnt - iCurCmpCnt + 1) * 
                        (long)((double)(dCurTime.getTime() - dStartTime.getTime()) / iCurCmpCnt);
                String sRemaining = String.format(" - Remaining: %40s\r", 
                        gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining));                
                System.err.print("Completed " + String.format("%4.2f%%", 100.0 * (double)iCurCmpCnt / iTotalCmpCnt ) +
                        ". Remaining:" + sRemaining + "\r");
            }
            try {
                t.waitUntilCompletion();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
                return null;
            }
            dMaxSimil = dSimils.maxValue();
            sParentDoc = (String)dSimils.getKeyOfMaxValue();
            
            if (dMaxSimil > 0)
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
        System.err.println();
        if (!bSilent)
            System.err.println("Complete.");
        
        if (!bSilent)
            System.err.println("Similarity distribution:" + dDist.average(false) + "," + 
                dDist.standardDeviation(false));
        
        
        return gTree;
    }

    public static CASCGraph getNGramDistanceAgglomerativelyClusteredGraph(String sInput, 
            String sLimitTo, int iMinNGram, 
            int iMaxNGram, int iDist, double dGraphImportance, String sClusteringType) {
        final TreeMap hmDocumentIndex = new TreeMap();
        
        // Create document set
        IDocumentSet dsSrc = null;
        if (new File(sInput).isDirectory())
            dsSrc = new DocumentSet(sInput,1.0);
        else {
            FileFormat ffIn = new FileFormat();
            ffIn.parseFile(sInput);
            dsSrc = ffIn;
        }
        dsSrc.createSets();
        
        // Init distribution of distances
        final Distribution dDist = new Distribution();
        
        System.err.println("Processing documents.");
        // For all documents
        Iterator iDocIter;
        if (sLimitTo == null)
            iDocIter = dsSrc.getTrainingSet().iterator();
        else
            iDocIter = dsSrc.getFilesFromCategory(sLimitTo).iterator();
        ThreadList t = new ThreadList(Runtime.getRuntime().availableProcessors());
        
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
    
    public static final String getFileNameOnly(String sFilePath) {
        String sFilename = new File(sFilePath).getName();
        return sFilename;
        //return sFilename.substring(0, 
                //sFilename.lastIndexOf(".") < 0 ? sFilename.length() : sFilename.lastIndexOf("."));
    }
    
}
