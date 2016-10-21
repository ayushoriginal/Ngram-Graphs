/*
 * HierLDACaller.java
 *
 * Created on October 12, 2007, 3:49 PM
 *
 */

package gr.demokritos.iit.jinsect.interoperability;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.console.ConsoleNotificationListener;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextHistoDocument;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.threading.ThreadList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import probabilisticmodels.HierLDAGibbs;

/**
 *
 * @author ggianna
 */
public class HierLDACaller implements NotificationListener {
    public static Date dStart;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        dStart = new Date();
        
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        String sInputDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "inputDir", ".");
        String sTermTopicsOut = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "termTopicOutput", "./termTopicAnalysis.txt");
        String sNormalizedTermTopicsOut = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "normTermTopicOutput", 
                "./normTermTopicAnalysis.txt");
        String sTopicsSuperTopicOut = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "topicSupertopicOutput", 
                "./topicSupertopicAnalysis.txt");
        int iLevels = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "levels", "5")).intValue();
        int iIterations = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "iters", "10000")).intValue();
        int iBurnInIterations = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "burnIn", "1000")).intValue();
        double dAlpha = Double.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "alpha", "2.0")).doubleValue();
        double dBeta = Double.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "beta", "0.5")).doubleValue();
        int iTermsPerTopic = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "termsPerTopic", "100")).intValue();
        int iThreads = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, 
                "threads", String.valueOf(Runtime.getRuntime().availableProcessors()))).intValue();
        
        TreeMap<Integer,String> tmReverseIndex = new TreeMap<Integer,String>();
        int[][] documentTopicMatrix = getDocumentTermMatrix(sInputDir, tmReverseIndex);
        HierLDAGibbs hierLDA = new HierLDAGibbs(iLevels,documentTopicMatrix, dAlpha, dBeta);
        hierLDA.setProgressIndicator(new ConsoleNotificationListener()); // Indicate progress
        hierLDA.performGibbs(iIterations, iBurnInIterations, iThreads);
        System.err.println();
        
        try {
            
            BufferedWriter bTermsTopicsOut = new BufferedWriter(new FileWriter(sTermTopicsOut));
            BufferedWriter bNormalizedTermTopicsOut = 
                    new BufferedWriter(new FileWriter(sNormalizedTermTopicsOut));
            BufferedWriter bTopicsSuperTopicOut = new BufferedWriter(new FileWriter(sTopicsSuperTopicOut));
            
            for (int iLvlCnt=0; iLvlCnt < iLevels; iLvlCnt++) {
                bTermsTopicsOut.write("Level #" + iLvlCnt + "\n");
                bNormalizedTermTopicsOut.write("Level #" + iLvlCnt + "\n");
                bTopicsSuperTopicOut.write("Level #" + iLvlCnt + "\n");
                
                for (int iTopicCnt=0; iTopicCnt <= iLvlCnt; iTopicCnt++) {
                    bTermsTopicsOut.write("---Topic" + iLvlCnt + "." + iTopicCnt + "\n");
                    String sTermsPerTopic = hierLDA.printoutTopicTerms(iLvlCnt,iTopicCnt,
                            iTermsPerTopic,tmReverseIndex);
                    bTermsTopicsOut.write(sTermsPerTopic);
                    bTermsTopicsOut.write("---End Topic" + iLvlCnt + "." + "\n");
                    
                    bNormalizedTermTopicsOut.write("---Topic #" + iLvlCnt + "." + iTopicCnt + "\n");
                    String sNormTermsPerTopic = hierLDA.printoutNormalizedTopicTerms(iLvlCnt,
                            iTopicCnt,iTermsPerTopic,tmReverseIndex);
                    bNormalizedTermTopicsOut.write(sNormTermsPerTopic);
                    bNormalizedTermTopicsOut.write("---End Topic #" + iLvlCnt + "." + iTopicCnt + "\n");
                    
                    if ((iLvlCnt > 0) && (iTopicCnt < iLvlCnt)) {
                        String sTopicPerSuperTopic = 
                                hierLDA.calcTopicProbsUnderSuperTopic(iLvlCnt,iTopicCnt).toString();
                        bTopicsSuperTopicOut.write("---Topic #" + iLvlCnt + "." + iTopicCnt + "\n");
                        bTopicsSuperTopicOut.write(sTopicPerSuperTopic);
                        bTopicsSuperTopicOut.write("\n---End Topic  #" + iLvlCnt + "." + iTopicCnt + "\n");
                    }
                }
            }
            bTermsTopicsOut.close();
            bTopicsSuperTopicOut.close();
            bNormalizedTermTopicsOut.close();
        } catch (IOException ex) {
            System.err.println("Could not complete the process:");
            ex.printStackTrace(System.err);
        }
    }
    
    public static int[][] getDocumentTermMatrix(List<CategorizedFileEntry> lDocs, Map<Integer,String> tmReverseIndex) {
        int[][] iaRes = new int[lDocs.size()][];
        
        TreeMap<String, Integer> hOverall = new TreeMap<String, Integer>();
        //TreeMap<Integer, String> tmReverseIndex = new TreeMap<Integer, String>();
        Vector<Distribution> vDocs = new Vector(lDocs.size());
        ThreadList tl = new ThreadList();
        
        Iterator iIter = lDocs.iterator();
        while (iIter.hasNext()) {
            // Prepare thread params
            final CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
            final Map<Integer,String> tmReverseIndexArg = Collections.synchronizedMap(tmReverseIndex);
            final Map<String, Integer> hOverallArg = Collections.synchronizedMap(hOverall);
            final List<Distribution> vDocsArg = Collections.synchronizedList(vDocs);
            
            // Execute in thread
            while (!tl.addThreadFor(new Runnable() {
                @Override
                public void run() {
                    SimpleTextHistoDocument sthdDoc = new SimpleTextHistoDocument(1,1,1);
                    sthdDoc.loadDataStringFromFile(cfeCur.getFileName());

                    Distribution dDoc = new Distribution();
                    Iterator iTerms = sthdDoc.getDocumentHistogram().NGramHistogram.keySet().iterator();
                    while (iTerms.hasNext()) {
                        String sTerm = (String)iTerms.next();
                        int iTermOcc = ((Double)sthdDoc.getDocumentHistogram().NGramHistogram.get(sTerm)).intValue();
                        dDoc.increaseValue(sTerm,(double)iTermOcc);

                        synchronized (hOverallArg) {
                            // Add term to term list if not there
                            if (!hOverallArg.containsKey(sTerm))
                            {
                                synchronized (tmReverseIndexArg) {
                                    tmReverseIndexArg.put(hOverallArg.size(), sTerm);
                                }
                                hOverallArg.put(sTerm, hOverallArg.size());
                            }
                        }
                    }
                    synchronized (vDocsArg) {
                        vDocsArg.add(dDoc);
                    }
                }
            }))
                Thread.yield();
            
            // DEBUG LINES
            System.err.print(".");
            //////////////
        }
        
        try {
            // DEBUG LINES
            System.err.println();
            //////////////
            tl.waitUntilCompletion();
        } catch (InterruptedException ex) {
            Logger.getLogger(HierLDACaller.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int iDocCnt=0; iDocCnt < vDocs.size(); iDocCnt++) {
            iaRes[iDocCnt] = new int[hOverall.size()];
            for (int iCnt=0; iCnt < hOverall.size(); iCnt++) {
                //iaRes[iDocCnt][iCnt] = 0;
                iaRes[iDocCnt][iCnt] = (int)vDocs.get(iDocCnt).getValue(tmReverseIndex.get(iCnt));
            }
        }
        
        return iaRes;
    }

    public static int[][] getDocumentTermMatrix(String sBaseDir, Map<Integer,String> tmReverseIndex) {
        
        DocumentSet dsSet = new DocumentSet(sBaseDir, 1.0);
        dsSet.createSets();
        ThreadList tl = new ThreadList();
        
        int[][] iaRes = new int[dsSet.getTrainingSet().size()][];
        
        TreeMap<String, Integer> hOverall = new TreeMap<String, Integer>();
        //TreeMap<Integer, String> tmReverseIndex = new TreeMap<Integer, String>();
        Vector<Distribution> vDocs = new Vector(dsSet.getTrainingSet().size());
        
        Iterator iIter = dsSet.getTrainingSet().iterator();
        while (iIter.hasNext()) {
            // Prepare thread params
            final CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
            final Map<Integer,String> tmReverseIndexArg = Collections.synchronizedMap(tmReverseIndex);
            final Map<String, Integer> hOverallArg = Collections.synchronizedMap(hOverall);
            final List<Distribution> vDocsArg = Collections.synchronizedList(vDocs);
            
            // Execute in thread
            while (!tl.addThreadFor(new Runnable() {
                @Override
                public void run() {
                    SimpleTextHistoDocument sthdDoc = new SimpleTextHistoDocument(1,1,1);
                    sthdDoc.loadDataStringFromFile(cfeCur.getFileName());

                    Distribution dDoc = new Distribution();
                    Iterator iTerms = sthdDoc.getDocumentHistogram().NGramHistogram.keySet().iterator();
                    while (iTerms.hasNext()) {
                        String sTerm = (String)iTerms.next();
                        int iTermOcc = ((Double)sthdDoc.getDocumentHistogram().NGramHistogram.get(sTerm)).intValue();
                        dDoc.increaseValue(sTerm,(double)iTermOcc);

                        synchronized (hOverallArg) {
                            // Add term to term list if not there
                            if (!hOverallArg.containsKey(sTerm))
                            {
                                synchronized (tmReverseIndexArg) {
                                    tmReverseIndexArg.put(hOverallArg.size(), sTerm);
                                }
                                hOverallArg.put(sTerm, hOverallArg.size());
                            }
                        }
                    }
                    synchronized (vDocsArg) {
                        vDocsArg.add(dDoc);
                    }
                    // DEBUG LINES
                    System.err.print(".");
                    //////////////
                }
            }))
                Thread.yield();
            
        }
        
        try {
            tl.waitUntilCompletion();
            // DEBUG LINES
            System.err.println();
            //////////////
        } catch (InterruptedException ex) {
            Logger.getLogger(HierLDACaller.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int iDocCnt=0; iDocCnt < vDocs.size(); iDocCnt++) {
            iaRes[iDocCnt] = new int[hOverall.size()];
            for (int iCnt=0; iCnt < hOverall.size(); iCnt++) {
                //iaRes[iDocCnt][iCnt] = 0;
                iaRes[iDocCnt][iCnt] = (int)vDocs.get(iDocCnt).getValue(tmReverseIndex.get(iCnt));
            }
        }
        
        return iaRes;
    }
    
    /** Expecting a double number as parameter to indicate progress. */
    public void Notify(Object oSender, Object oParams) {
        double dVar = ((Double)oParams).doubleValue();
        
        long lLeft = (long)((1.0 - dVar) * (double)(new Date().getTime() - dStart.getTime()) / dVar);
        String sLeft;
        
        if (((int)(dVar * 10000) % 5) == 0) {
            if (dVar < 0.0001)
                sLeft = "Calculating remaining time...";
            else
                sLeft = String.format("%35s", gr.demokritos.iit.jinsect.utils.millisToMinSecString(lLeft));
            System.err.print(String.format("%5.3f%%", ((Double)oParams).doubleValue() * 100.0) + 
                " complete..." + sLeft + "\r");
        }
    }
}
