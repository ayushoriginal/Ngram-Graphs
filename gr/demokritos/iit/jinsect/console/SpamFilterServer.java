/*
 * Under LGPL licence
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.jinsect.documentModel.comparators.NGramGraphEuclidianComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.structs.Decision;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.threading.ThreadQueue;
import gr.demokritos.iit.jinsect.utils;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import mail.MimeMessage;
import mail.MimeMultiPart;
import mail.Part;
import mail.exceptions.ParseException;

/** A spam filter socket-based server (based on an architecture implemented by
 * Aris Kosmopoulos (akosmo@iit.demokritos.gr)
 *
 * @author ggianna
 */
public class SpamFilterServer {
    
    protected final static String M_TRAIN = "-s-h";
    protected final static String M_CLASSIFY = "-c";
    protected final static String M_TERMINATE = "-t";
    protected final static String M_TRAINHAM = "-h";
    protected final static String M_TRAINSPAM = "-s";
    protected final static String M_DONE = "DONE";
    
    protected final static String C_HAM = "ham";
    protected final static String C_SPAM = "spam";
    protected int MAX_ANALYSED_CHARS;
    
    protected DocumentNGramGraph dgSpam;
    protected DocumentNGramGraph dgHam;
    protected int Port;
    protected int RespondToPort;
    protected String RespondToHost;
    protected double Wareoff;
    protected int MinTrainSamples;
    
    protected int iSpamCnt, iHamCnt, iOverallCnt;
    protected final int MAX_SEND_ATTEMPTS = 5;
    protected INSECTFileDB<DocumentNGramGraph> repos;
    
    protected boolean bIgnoreFurtherTraining;
    protected boolean bEnableDegrade;
    protected boolean bEnableDelta;
    protected boolean bEnableParser;
    protected boolean bLinearWareoff;
    protected boolean bEuclidian;
    protected boolean bWeightedEuclidian;
    
    public SpamFilterServer(int iPortParam, int iRespondToPortParam) {
        Port = iPortParam;
        dgSpam = new DocumentNGramGraph();
        dgHam = new DocumentNGramGraph();
        iSpamCnt = 0;
        iHamCnt = 0;
        iOverallCnt = 0;
        RespondToPort = iRespondToPortParam;
        repos = new INSECTFileDB<DocumentNGramGraph>("ceas", "./models/");
        bLinearWareoff = false; // Exponential by default
        Wareoff = 1.7; // Default value
        bIgnoreFurtherTraining = false;
        bEnableDegrade = false;
        bEnableDelta = false;
        bEnableParser = false;
        MinTrainSamples = 10;
        bEuclidian = false;
        bWeightedEuclidian = false;
        MAX_ANALYSED_CHARS = 20000;
    }
    
    public void ignoreFurtherTraining() {
        bIgnoreFurtherTraining = true;
    }
    
    public void acceptFurtherTraining() {
        bIgnoreFurtherTraining = false;
    }
    
    public void setDegradeOn(boolean bOn) {
        bEnableDegrade = bOn;
    }
    
    public void setDeltaOn(boolean bOn) {
        bEnableDelta = bOn;
    }
    
    public void setParserOn(boolean bOn) {
        bEnableParser = bOn;
    }
    
    public void setWareoff(double dNewWareoff) {
        Wareoff = dNewWareoff;
    }
    
    public void setLinearWareoffOn(boolean bNewLinearWareoff) {
        bLinearWareoff = bNewLinearWareoff;
    }
    
    public void setMinTrainSamples(int iNewMinTrainSamples) {
        MinTrainSamples = iNewMinTrainSamples;
    }
    
    public void setEuclidianOn(boolean bNewEuclidian) {
        bEuclidian = bNewEuclidian;
    }
    
    public void setWeightedEuclidianOn(boolean bNewWEuclidian) {
        bWeightedEuclidian = bNewWEuclidian;
    }
    
    public void setMaxAnalysedChars(int iNewMaxAnalysedChars) {
        MAX_ANALYSED_CHARS = iNewMaxAnalysedChars;
    }
    
    public void loadModels() {
        dgSpam = repos.loadObject(C_SPAM, "DGFile");
        dgHam = repos.loadObject(C_HAM, "DGFile");
        if ((dgSpam == null) || (dgHam == null)) {
            System.err.println("Loading failed...Resetting models.");
            dgSpam = new DocumentNGramGraph();
            dgHam = new DocumentNGramGraph();
        }
    }
    
    public void saveModels() {
        System.err.print("Saving models...");
        repos.saveObject(dgSpam, C_SPAM, "DGFile");
        repos.saveObject(dgHam, C_HAM, "DGFile");
        System.err.println("Done.");
    }
    
    public void start() {
        ServerSocket sServer = null;
        String sMessage = "";
        String[] sMessageParts = null;
        try {
            sServer = new ServerSocket(Port);
        } catch (IOException ex) {
            Logger.getLogger(SpamFilterServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        Date dStarted = null;
        while (sMessage != null) {
            try {
                System.err.print("Listening...");
                
                Socket csClient = sServer.accept();
                // Set start time
                dStarted = new Date();
                BufferedReader brInput = new BufferedReader(new InputStreamReader(
                    csClient.getInputStream()));
                sMessage = brInput.readLine();
                // = csClient.getPort();
                RespondToHost = csClient.getInetAddress().getHostAddress();
            } catch (IOException ex) {
                Logger.getLogger(SpamFilterServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            sMessageParts = sMessage.split(" ");
            String sMessageType = sMessageParts[0]; // Type - 1st argument
            // Train
            if (M_TRAIN.contains(sMessageType)) {
                // Only if further training is allowed
                if (!bIgnoreFurtherTraining) {
                    String sFile = sMessageParts[1]; // Filename - 2nd argument

                    Decision dTmp = judge(sFile);
                    System.err.print("Judged to be " + dTmp.FinalDecision + " with" +
                            " a score of " + dTmp.DecisionBelief + " with evidence:");
                    System.err.println(utils.printIterable(
                            dTmp.DecisionEvidence.values(), ","));

                    // Load file into graph
                    DocumentNGramGraph gTmp = (DocumentNGramGraph)dTmp.Document;
                    ThreadQueue tUpdates = new ThreadQueue();
                    
                    // Update models, on judgement failure
                    boolean bUpdated = false;
                    // Create thread args
                    final DocumentNGramGraph dgHamArg = dgHam;
                    final DocumentNGramGraph dgSpamArg = dgSpam;
                    final DocumentNGramGraph gTmpArg = gTmp;
                    final int iHamCntArg = iHamCnt;
                    final int iSpamCntArg = iSpamCnt;
                    final boolean bLinearWareoffArg = bLinearWareoff;
                    // For ham
                    if (sMessageType.equalsIgnoreCase(M_TRAINHAM) &&
                            (!dTmp.FinalDecision.toString().equalsIgnoreCase(C_HAM) 
                            || (iHamCnt < MinTrainSamples))) {
                        while (!tUpdates.addThreadFor(new Runnable() {

                            @Override
                            public void run() {
                                if (!bLinearWareoffArg)
                                    // Exponential
                                    dgHamArg.merge(gTmpArg, Math.pow(Wareoff, 
                                            -iHamCntArg));
                                else
                                    if (iHamCntArg == 0)
                                        dgHamArg.merge(gTmpArg, 1.0);
                                    else
                                    // Linear
                                        dgHamArg.merge(gTmpArg, Math.pow(
                                            (double)iHamCntArg 
                                            / (iHamCntArg + 1.0), 
                                            Wareoff));
                            }
                        }))
                            Thread.yield();
                        
                        if (bEnableDegrade) {
                            while (!tUpdates.addThreadFor(new Runnable() {

                                @Override
                                public void run() {
                                    dgSpamArg.degrade(gTmpArg);
                                }
                            }))
                                Thread.yield();
                        }
                        try {
                            tUpdates.waitUntilCompletion();
                        } catch (InterruptedException ex) {
                            System.err.println("I cannot wait!!! :-)");
                            Logger.getLogger(SpamFilterServer.class.getName()
                                    ).log(Level.SEVERE, null, ex);
                            return;
                        }
                        
                        bUpdated = true;
                        iHamCnt++;
                    }
                    // For spam
                    if (sMessageType.equalsIgnoreCase(M_TRAINSPAM) &&
                            (!dTmp.FinalDecision.toString().equalsIgnoreCase(C_SPAM) 
                            || (iSpamCnt < MinTrainSamples))) {
                        while (!tUpdates.addThreadFor(new Runnable() {

                            @Override
                            public void run() {
                                if (!bLinearWareoffArg)
                                    dgSpamArg.merge(gTmpArg, Math.pow(Wareoff, 
                                            -iSpamCntArg));
                                else
                                    if (iSpamCntArg == 0)
                                        dgSpamArg.merge(gTmpArg, 1.0);
                                    else
                                        dgSpamArg.merge(gTmpArg, Math.pow(
                                            (double)iSpamCntArg 
                                            / (iSpamCntArg + 1.0),
                                            Wareoff));
                            }
                        }))
                            Thread.yield();
                        
                        if (bEnableDegrade) {
                            while (!tUpdates.addThreadFor(new Runnable() {

                                @Override
                                public void run() {
                                    dgHamArg.degrade(gTmpArg);
                                }
                            }))
                                Thread.yield();
                        }
                        
                        try {
                            tUpdates.waitUntilCompletion();
                        } catch (InterruptedException ex) {
                            System.err.println("I cannot wait!!! :-)");
                            Logger.getLogger(SpamFilterServer.class.getName()
                                    ).log(Level.SEVERE, null, ex);
                            return;
                        }
                        
                        iSpamCnt++;
                        bUpdated = true;
                    }
                    
                    if (bUpdated) {
                        System.err.print("Update performed.");
                        NGramGraphEuclidianComparator ngc = new 
                                NGramGraphEuclidianComparator();
                        // DEBUG LINES
                        if (!bEuclidian)
                            System.err.println("Similarity between ham and spam: " + 
                                ngc.getSimilarityBetween(dgHam, dgSpam).toString());
                        else
                            System.err.println("Similarity between ham and spam: " + 
                                    ngc.getEuclidianSimilarityBetween(dgHam, dgSpam, 
                                    bWeightedEuclidian).getOverallSimilarity());
                        //////////////
                            
                        
                        if (bEnableDelta) {
                            DeltaPerformer dpHamTmp = new DeltaPerformer(dgHam, 
                                    dgSpam);
                            DeltaPerformer dpSpamTmp = new DeltaPerformer(dgSpam, 
                                    dgHam);
                            while (!tUpdates.addThreadFor(dpHamTmp)) 
                                Thread.yield();
                            while (!tUpdates.addThreadFor(dpSpamTmp)) 
                                Thread.yield();
                            try {
                                tUpdates.waitUntilCompletion();
                            } catch (InterruptedException ex) {
                                System.err.println("I can't wait!!! :-)");
                                Logger.getLogger(SpamFilterServer.class.getName()
                                        ).log(Level.SEVERE, null, ex);
                                return;
                            }
                            dgHam = dpHamTmp.resultGraph;
                            dgSpam = dpSpamTmp.resultGraph;
                            
                            // DEBUG LINES
                            if (!bEuclidian)
                                System.err.println("Delta performed. Similarity " +
                                    "between ham and spam: " + 
                                    ngc.getSimilarityBetween(dgHam, dgSpam).toString());
                            else
                                System.err.println("Delta performed. Similarity " +
                                        "between ham and spam: " + 
                                        ngc.getEuclidianSimilarityBetween(dgHam, dgSpam, 
                                        bWeightedEuclidian));
                            //////////////
                        }
                        // saveModels();
                    }
                    System.err.println(String.format("Trained a total of %d ham and " +
                            "%d spam after %d messages.", iHamCnt, iSpamCnt, 
                            ++iOverallCnt));
                }
                sendMessage(dStarted, 50, M_DONE);
            }
            
            // Classify
            if (M_CLASSIFY.contains(sMessageType)) {
                System.err.print("Classifying...");
                // Load file
                String sFile = sMessageParts[1]; // Filename - 2nd argument
                Decision dTmp = judge(sFile);
                
                // Prepare response
                StringBuffer sbMsg = new StringBuffer();
                sbMsg.append("class=" + dTmp.FinalDecision);
                sbMsg.append(" score=" + String.valueOf(dTmp.DecisionBelief));
                sbMsg.append(" tfile=dummyFile\n");
                // Send response
                sendMessage(dStarted, 50, sbMsg.toString());
                System.err.println(String.format("%s(%f). Evidence: %s ", 
                        dTmp.FinalDecision, dTmp.DecisionBelief,
                        utils.printIterable(dTmp.DecisionEvidence.values(), 
                        ",")));
            }
            
            // Terminate
            if (M_TERMINATE.contains(sMessageType)) {
                sendMessage(dStarted, 50, M_DONE);
                sMessage = null;
            }
        }
        if (sServer != null)
            try {
                sServer.close();
            } catch (IOException ex) {
                Logger.getLogger(SpamFilterServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        System.err.println("Finalized.");
    }
    
    protected void sendMessage(Date dStarted, long lDelayMillis, String sMsg) {
        // Wait until necessary delay is inserted
        long lElapsed = new Date().getTime() - dStarted.getTime();
        if (lDelayMillis > lElapsed)
            try {
               Thread.sleep(lDelayMillis - lElapsed);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpamFilterServer.class.getName()).log(Level.WARNING, null, ex);
            }
        
        // MAX_SEND_ATTEMPTS connection attempts
        Socket sResp = null;
        try {
            int iTries = 0;
            while (iTries < MAX_SEND_ATTEMPTS) {
                System.err.println("Attempting reply to:" + RespondToHost + "," +
                        RespondToPort);
                try {
                    sResp = new Socket(RespondToHost, RespondToPort);
                    Thread.sleep(lDelayMillis);
                    break;
                }
                catch (IOException ex) {
                    Logger.getLogger(SpamFilterServer.class.getName()).log(
                            Level.WARNING, null, ex);
                    
                    iTries++;
                }
                catch (InterruptedException ie) {
                    // Ignore
                    System.err.println("I can't wait!!! :-)");
                }
            }
            
            // Failed
            if (iTries >= MAX_SEND_ATTEMPTS) {
                System.err.println("\n ERROR: Failed to reply...");
                return;
            }
            
            BufferedWriter bwOut = new BufferedWriter(
                    new OutputStreamWriter(sResp.getOutputStream()));
            bwOut.write(sMsg);
            bwOut.flush();
            sResp.close();
        } catch (UnknownHostException ex) {
            Logger.getLogger(SpamFilterServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SpamFilterServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    protected Decision judge(String sFile) {
        System.err.print("Loading file " + sFile + "...");
        // Load file into graph
        DocumentNGramGraph gTmp = new DocumentNGramGraph();
        // Read content
        gTmp.setDataString(extractContent(sFile));

        System.err.print("Judging file " + sFile + "...");
        // Compare message to both and determine
        double dScore = 0.0;
        
        // Init comparators
        NGramGraphEuclidianComparator ngcS = new NGramGraphEuclidianComparator();
        ngcS.setNotificationListener(new NotificationListener() {
            long lCnt = 0;
            @Override
            public void Notify(Object oSender, Object oParams) {
                if (lCnt++ % 100 == 0)
                    working();
            }
        });

        NGramGraphEuclidianComparator ngcH = new NGramGraphEuclidianComparator();
        ngcH.setNotificationListener(new NotificationListener() {
            long lCnt = 0;
            @Override
            public void Notify(Object oSender, Object oParams) {
                if (lCnt++ % 100 == 0)
                    working();
            }
        });
        
        // DONE: Multithread
        ThreadQueue t = new ThreadQueue();
        final DocumentNGramGraph dgHamArg = dgHam;
        final DocumentNGramGraph dgSpamArg = dgSpam;
        final DocumentNGramGraph gTmpArg = gTmp;
        final Hashtable<String, Double> hRes = new Hashtable<String, Double>();
        final NGramGraphEuclidianComparator ngcHArg = ngcH;
        final NGramGraphEuclidianComparator ngcSArg = ngcS;
        final boolean bEuclidianArg = bEuclidian;
        
        // Compare to ham
        while (!t.addThreadFor(new Runnable() {
            @Override
            public void run() {
                if (!bEuclidian) {
                    GraphSimilarity gsTmp = 
                            (GraphSimilarity)ngcHArg.getSimilarityBetween(
                            dgHamArg, gTmpArg);
                    double dHamScore = (gsTmp.SizeSimilarity == 0.0) ? 0.0 : 
                        gsTmp.ValueSimilarity / gsTmp.SizeSimilarity; 
                    synchronized (hRes) {
                        hRes.put(C_HAM, dHamScore);
                    }
                }
                else
                    synchronized (hRes) {
                        if (dgHamArg.length() == 0)
                            hRes.put(C_HAM, 0.0);
                        else
                            hRes.put(C_HAM, ngcHArg.getEuclidianSimilarityBetween(
                                dgHamArg, gTmpArg, bWeightedEuclidian
                                ).getOverallSimilarity() /
                                dgHamArg.length());
                    }
                    
            }
        }))
            Thread.yield();
        
        // Compare to spam
        while (!t.addThreadFor(new Runnable() {

            @Override
            public void run() {
                if (!bEuclidianArg) {
                    GraphSimilarity gsTmp = 
                            (GraphSimilarity)ngcSArg.getSimilarityBetween(dgSpamArg, 
                            gTmpArg);
                    double dSpamScore = (gsTmp.SizeSimilarity == 0.0) ? 0.0 : 
                        gsTmp.ValueSimilarity / gsTmp.SizeSimilarity; 
                    synchronized (hRes) {
                        hRes.put(C_SPAM, dSpamScore);
                    }
                }
                else
                    synchronized (hRes) {
                        if (dgSpamArg.length() == 0)
                            hRes.put(C_SPAM, 0.0);
                        else
                            hRes.put(C_SPAM, ngcSArg.getEuclidianSimilarityBetween(
                                dgSpamArg, gTmpArg, bWeightedEuclidian
                                ).getOverallSimilarity() /
                                dgSpamArg.length());
                    }
            }
        }))
            Thread.yield();
        
        try {
            t.waitUntilCompletion();
        } catch (InterruptedException ex) {
            System.err.println("Interrupted comparison. Quitting.");
            Logger.getLogger(SpamFilterServer.class.getName()).log(Level.SEVERE, 
                    null, ex);
            return null; // Quit
        }
        
        double dHamScore = hRes.get(C_HAM);
        double dSpamScore = hRes.get(C_SPAM);
        // Exctract results
        try {
            dScore = (Math.log1p(dSpamScore) - Math.log1p(dHamScore)) / 
                Math.log1p(Math.max(dSpamScore, dHamScore));
        }
        catch (Exception e) {
            System.err.println("Cannot determine:" + 
                    String.format("Ham: %10.8f\tSpam: %10.8f", dHamScore,
                    dSpamScore));
            dScore = (iSpamCnt / iHamCnt) / Math.max(iSpamCnt, iHamCnt); // Cannot determine
        }
        if (Double.isNaN(dScore))
            dScore = 0.0;

        // Determine class
        String sClass = (dHamScore > dSpamScore) ? C_HAM : C_SPAM;
        
        Decision dRes = new Decision(gTmp, sClass, dScore, hRes);
        System.err.println("Done.");
        return dRes;
    }
            
    protected String extractContent(String sFile) {
        if (!bEnableParser) {
            System.err.print("Parser skipped.");
            String sText = utils.loadFileToString(sFile, MAX_ANALYSED_CHARS);
            if (sText.length() > MAX_ANALYSED_CHARS)
                return sText.substring(0, MAX_ANALYSED_CHARS);
            return sText;
        }
        
        InputStream is = null;
        try {
                is = new BufferedInputStream(new FileInputStream(sFile));
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        }

        MimeMessage message = null;
        try {
            message = new MimeMessage(is);
        }
        catch (ParseException pe) {
            System.err.println("Cannot parse file:" + pe.toString());
            return "";
        }
        catch (Exception e) {
            System.err.println("Error opening file. Using whole message...");
            return utils.loadFileToString(sFile, MAX_ANALYSED_CHARS); // Parse failed
        }
        
        try {
            is.close();
        } catch (IOException ex) {
            Logger.getLogger(SpamFilterServer.class.getName()).log(Level.WARNING, null, ex);
        }
            
        // Read title
        String sTitle = "";
        if (message.getHeaders().getHeader("subject") != null)
            sTitle = message.getHeaders().getHeader("subject").getValue();
        
        MimeMultiPart mimeMultipart = null;
        try {
            if (message.getHeaders().getContentType().getBaseType().toString(
                ).contains("multipart")) {
                mimeMultipart = (MimeMultiPart) message.getPart();
                StringBuffer sbBodyText = new StringBuffer();
                for (Part pCur : mimeMultipart.getParts()) {
                    if (pCur.getContentType().getBaseType().toString().equalsIgnoreCase(
                            "text"))
                        sbBodyText.append(pCur.toString(1));
                }

                return sTitle + "\n" + sbBodyText.toString();
            }
            else
            {
                return sTitle + "\n" + message.getPart().toString(1);
            }
        }
        catch (ParseException pe) {
            System.err.println("Cannot parse multipart file:" + pe.toString());
            return "";

        }
    }
    
    protected final String getHeader(Part pMsgPart, String sHeader) {
        if (pMsgPart.getHeaders().getHeader(sHeader) != null)
            return pMsgPart.getHeaders().getHeader(sHeader).getValue();
        else
            return "";
    }
    
    protected final synchronized void working() {
        System.err.print(".");
        System.err.flush();
    }
    
    public static void main(String sArgs[]) {
        Hashtable hSwitches = utils.parseCommandLineSwitches(sArgs);
        int iPort = Integer.valueOf(utils.getSwitch(hSwitches, "port", 
                String.valueOf(32167)));
        int iRespondToPort = Integer.valueOf(utils.getSwitch(hSwitches, "port", 
                String.valueOf(32168)));
        int iMinTrainSamples = Integer.valueOf(utils.getSwitch(hSwitches, 
                "minTrainSamples", String.valueOf(10)));
        int iMaxLen = Integer.valueOf(utils.getSwitch(hSwitches, 
                "maxLen", String.valueOf(20000)));
        double dWareoff = Double.valueOf(utils.getSwitch(hSwitches, "wareoff", 
                String.valueOf(-1.0)));
        boolean bLinearWareoff = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "linearWareoff", String.valueOf(false)));
        boolean bLoadModels = Boolean.valueOf(utils.getSwitch(hSwitches, "loadModels", 
                String.valueOf(false)));
        boolean bIgnoreTraining = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "ignoreTraining", String.valueOf(false)));
        boolean bDelta = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "delta", String.valueOf(false)));
        boolean bDegrade = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "degrade", String.valueOf(false)));
        boolean bUseParser = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "parse", String.valueOf(false)));
        boolean bSaveModels = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "saveModels", String.valueOf(false)));
        boolean bWeightedEuclidian = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "wEuclidian", String.valueOf(false)));
        boolean bEuclidian = Boolean.valueOf(utils.getSwitch(hSwitches, 
                "euclidian", String.valueOf(false))) || bWeightedEuclidian;
        
        SpamFilterServer sServer = new SpamFilterServer(iPort, iRespondToPort);
        if (bLoadModels)
            sServer.loadModels();
        if (bIgnoreTraining)
            sServer.ignoreFurtherTraining();
        sServer.setDeltaOn(bDelta);
        sServer.setDegradeOn(bDegrade);
        sServer.setParserOn(bUseParser);
        sServer.setLinearWareoffOn(bLinearWareoff);
        sServer.setMinTrainSamples(iMinTrainSamples);
        sServer.setEuclidianOn(bEuclidian);
        sServer.setMaxAnalysedChars(20000);
        sServer.setWeightedEuclidianOn(bWeightedEuclidian);
        if (dWareoff >= 0.0)
            sServer.setWareoff(dWareoff);
        sServer.start();
        if (bSaveModels)
            sServer.saveModels();
    }

}

/** A Runnable class, than performs delta between two DocumentNGramGraphs. */
class DeltaPerformer implements Runnable {
    private DocumentNGramGraph deltaG1, deltaG2;
    public DocumentNGramGraph resultGraph;
    
    public DeltaPerformer(DocumentNGramGraph g1, DocumentNGramGraph g2) {
        deltaG1 = g1;
        deltaG2 = g2;
    }
    
    @Override
    public final void run() {
        synchronized (deltaG1) {
            synchronized (deltaG2) {
                resultGraph = deltaG1.allNotIn(deltaG2);
            }
        }
    }
    
}