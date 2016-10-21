/*
 * ResultsAgent.java
 *
 * Created on 12 ?????????????????????? 2007, 4:36 ????
 *
 */

package gr.demokritos.iit.jinsect.distributed;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.GZIPOutputStream;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;

/** An agent to manage the whole comparison process of summaries, that results into
 * evaluation. This is actually an agent implementation of the summary evaluation 
 *process that can be found in package <code>console</code>.
 *
 *@see gr.demokritos.iit.jinsect.console.summaryEvaluator
 * @author ggianna
 */
public class ResultsAgent extends Agent {    
    protected String OutFile, SummaryDir, ModelDir, Do;
    protected PrintStream psOutputStream;
    protected boolean Silent, ShowProgress;
    protected int PendingRequests = 0; // Init to zero
    protected int CompletedRequests = 0;
    protected int AllRequests = 0;
    protected int AgentUniqueIDLength = 10; // Use 5 digits to randomly determine created agent names
    protected int MaxPendingRequests = 20;
    protected int MaxAgents = 10;
    protected LinkedList ActiveAgents;
    protected LinkedList InitializedAgents;
    protected ListIterator CurrentAgent = null;
    protected double MigrationProbability = 0.8;
    
    protected Integer WordMin, WordMax, WordDist, CharMin, CharMax, CharDist, WeightMethod;

    /** Uses the agent command line arguments to initialize the evaluation process.
     * More on the arguments can be found at 
     * {@link gr.demokritos.iit.jinsect.console.summaryEvaluator}, with the exception
     * of migration probability (-migProb switch), that indicates the probability
     * of migration of calculating agents.
     *@see gr.demokritos.iit.jinsect.console.summaryEvaluator
     */
    public void setup() {
        // Read input arguments
        String[] args;
        Object[] oArgs= getArguments();
        if ((oArgs != null) && (oArgs.length > 0)) {
            // Get string args
            ArrayList alArgs = new ArrayList();
            for (int iCnt = 0; iCnt < oArgs.length; iCnt++) {
                if (oArgs[iCnt] instanceof String)
                    alArgs.add(oArgs[iCnt]);
            }
            args = new String[alArgs.size()];
            args = (String[])alArgs.toArray(args);
            
        }
        else {
            args = new String[1];
            args[0] = "";
        }
            
        // Parse string arguments
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
        if (gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"?", "").length() > 0) {
            System.exit(0);
        }
            
        
        try {
            // Define output file
            OutFile = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "o", "");
            // Get summary and model dir
            SummaryDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "summaryDir", "summaries"
                    + System.getProperty("file.separator"));
            ModelDir = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "modelDir", "models"
                    + System.getProperty("file.separator"));
            // Get Maximum Pending Calculation Requests
            MaxPendingRequests = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "queueSize", 
                    String.valueOf(MaxPendingRequests)));
            // Get Maximum Agents dispatched
            MaxAgents = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "t", 
                    String.valueOf(MaxAgents)));
            // Determine if silent
            Silent=gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "s", "FALSE").equals("TRUE");
            // Determine if progress is indicated
            ShowProgress=gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "progress", "FALSE").equals("TRUE");
            // Get preferred migration probability
            MigrationProbability = Double.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "migProb", 
                    String.valueOf(MigrationProbability)));
            // Determine calculation parameters
            WordMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordMin", "1"));
            WordMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordMax", "2"));
            WordDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordDist", "3"));
            CharMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charMin", "3"));
            CharMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charMax", "5"));
            CharDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charDist", "3"));
            // Define method
            Do = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"do", "all");
            if ((Do.length() == 0) || ("char_word_all__".indexOf(Do) % 5 != 0))
            {
                synchronized(System.err) {
                    System.err.println("Invalid 'Do' parameter: defaulting to value 'all'.");
                }
                Do = "all";
            }
            
            
            // Start
            synchronized (System.out) {
                System.out.println("Starting..." + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()));
            }
            
            
            if (!Silent)
                System.err.println(this.getName() + ":" + "Using parameters:\n" + hSwitches);
            
            
        }
        catch (ClassCastException cce) {
            System.err.println(this.getName() + ":" + "Malformed switch:" 
                    + cce.getMessage() + ". Aborting...");
        }
        
        // Read input text sets
        DocumentSet dsSummarySet = new DocumentSet(SummaryDir, 1.0); // TODO: Change back to 1.0
        final DocumentSet dsModelSet = new DocumentSet(ModelDir, 1.0);
        dsSummarySet.createSets();
        dsModelSet.createSets();
        
        if (dsSummarySet.getTrainingSet().size() *  dsModelSet.getTrainingSet().size()== 0)
        {
            System.err.println(this.getName() + ":" + "Empty document set...");
            System.exit(-1);
        }
        else {
            if (!Silent)
                System.err.println(this.getName() + ":" + "Getting ready for approximately " + 
                    (int)((double)dsSummarySet.getTrainingSet().size() * 
                        ( (double)dsModelSet.getTrainingSet().size() / dsModelSet.getCategories().size()))
                    + " calcs...");
        }
        
        // Init counting vars and other needed structs
        int iTotal = dsSummarySet.getTrainingSet().size();
        LinkedList qCalcArgs = new LinkedList();
        ActiveAgents = new LinkedList();
        InitializedAgents = new LinkedList();
        
        // For every category
        Iterator iCatIter = dsModelSet.getCategories().iterator();
        // Init start date
        Date dStart = new Date();
        while (iCatIter.hasNext()) {
            String sCurCategory = (String)iCatIter.next();
            if (!Silent)
                System.err.println(this.getName() + ":" + "Processing category:" + sCurCategory);
            
            // Load model file list
            List lModelFiles = dsModelSet.getFilesFromCategory(sCurCategory, dsModelSet.FROM_TRAINING_SET);
            // For every file in the same category as the models
            Iterator iIter = dsSummarySet.getFilesFromCategory(sCurCategory, 
                    dsSummarySet.FROM_TRAINING_SET).iterator();            
            while (iIter.hasNext()) {
                final CategorizedFileEntry cfeCur = (CategorizedFileEntry)iIter.next();
                
                // For every model
                Iterator iCurModel = lModelFiles.iterator();
                while (iCurModel.hasNext()) {
                    CategorizedFileEntry cfeModel = (CategorizedFileEntry)iCurModel.next();
                    String sSumName = new File(cfeCur.getFileName()).getName();
                    String sModelName = new File(cfeModel.getFileName()).getName();
                    if (sSumName.equals(sModelName))
                        continue; // Ignore docs with exactly same names (i.e. same files)
                    
                    // Create data for calculating agent
                    String[] sFileNameData = new File(cfeCur.getFileName()).getName().split("\\.");
                    String sID = sFileNameData[0] + "\t" + sFileNameData[4]; // Only print Theme and SystemID
                    // Only add parameters to list
                    AgentData adCur = new AgentData();
                    
                    adCur.ID = sID;
                    adCur.Texts = new String[2];
                    adCur.Texts[0] = readFromFile(cfeCur.getFileName());
                    adCur.Texts[1] = readFromFile(cfeModel.getFileName());
                    // Add data to data queue
                    qCalcArgs.add(adCur);
                }
            }
        }       
        // Set size of pending data requests
        AllRequests = qCalcArgs.size();
        
        // DEBUG LINES
        if (!Silent)
            System.err.println("Processed " + AllRequests + " calcs...");
        //////////////
        
        // Init output stream        
        if (OutFile.length() != 0) {
            try {
                psOutputStream = new PrintStream(OutFile);
            }
            catch (FileNotFoundException fnfe) {
                System.err.println("Cannot output to selected file:\n" + fnfe.getMessage());
                System.exit(1);
            }
        }
        if (psOutputStream == null)
            psOutputStream = System.err;
        
        // Init agent args
        Object[] saArgs = {this.getAID(),
            "-wordMin=" + WordMin, "-wordMax=" + WordMax, "-wordDist=" + WordDist,
            "=charMin=" + CharMin, "charMax=" + CharMax, "charDist=" + CharDist,
            "-do=" + Do, Silent ? "-s" : "", 
            "-migProb=" + String.valueOf(MigrationProbability)};
        // Wait for messages
        addBehaviour(new ActivateAgentListBehaviour(this, 50, qCalcArgs, psOutputStream, MigrationProbability,
                saArgs));
    }
    
    /** Shuts down the agent, with a debug message. */
    public void takeDown() {
            // Start
            synchronized (System.out) {
                System.out.println("Finished..." + 
                        (new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")).format(new Date()));
            }
    }

    /** Reads a given file into a string .
     *@param sFileName The file to read.
     */
    protected String readFromFile(String sFileName) {
        ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
        FileInputStream fiIn = null;
        BufferedInputStream bIn = null;
        String sDataString="";
        try {
            fiIn = new FileInputStream(sFileName);
            bIn = new BufferedInputStream(fiIn);
            int iData = 0;
            while ((iData = bIn.read()) > -1)
                bsOut.write(iData);
            sDataString = bsOut.toString();
            fiIn.close();
            bIn.close();            
        }
        catch(IOException ioe) {
            ioe.printStackTrace(System.err);
        }
        
        return sDataString;
    }
    
    /** Creates a unique agent ID with a given length.
     *@param iLen The required length for the returned string.
     *@return The unique ID.
     */
    protected String createAgentID(int iLen) {
        String sTemp = String.valueOf(new Date().getTime());
        return sTemp.substring(sTemp.length() - iLen); // Return last [iLen] digits
    }
    
    /** Completes the evaluation process, updating the dispatched agents of the 
     *event .
     */
    public void complete() {
        System.err.println("Completed successfully. Releasing sent agents...");
        
        String sCur;
        Object oNext;
        // For every agent
        while ((oNext=ActiveAgents.poll()) != null) {
            sCur = (String)oNext;
            // Send termination signal
            ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
            msg.setSender(getAID());
            msg.addReceiver(new AID(sCur, false));
            msg.setContent(NGramDocumentComparatorAgent.TERMINATION_MESSAGE);

            send(msg);        
        }
        psOutputStream.flush();
        psOutputStream.close();
        
        addBehaviour(new WakerBehaviour(this, 10000) {
            public void onWake() {
                doDelete();
            }
        });
        
    }
}

/** Defines required data for CalcAgent
 */
class AgentData {
    //Object[] Arguments;
    String [] Texts;
    String ID;
    //String AgentName;
}

/** Sends a pair of texts to a CalcAgent
 */
class SendTextsBehaviour extends OneShotBehaviour {
    String ID;
    byte[] bText1, bText2;
    String SendTo;
    
    public SendTextsBehaviour(String sID, String sText1, String sText2, String sSendTo) {
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            GZIPOutputStream gzOut = new GZIPOutputStream(bOut);
            int iCnt = 0;
            while (iCnt < sText1.length())
                gzOut.write(sText1.charAt(iCnt++));
            gzOut.flush();
            gzOut.close();
            
            bText1 = bOut.toByteArray();
        }
        catch (IOException ioe) {
            synchronized (System.err) {
                ioe.printStackTrace(System.err);
                System.err.flush();
            }
            bText1 = sText1.getBytes();
        }
        
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            GZIPOutputStream gzOut = new GZIPOutputStream(bOut);
            int iCnt = 0;
            while (iCnt < sText2.length())
                gzOut.write(sText2.charAt(iCnt++));
            gzOut.flush();
            gzOut.close();
            
            bText2 = bOut.toByteArray();
        }
        catch (IOException ioe) {
            synchronized (System.err) {
                ioe.printStackTrace(System.err);
                System.err.flush();
            }
            bText2 = sText2.getBytes();
        }
            
        ID = sID;
        SendTo = sSendTo;
    }
    public void action() {
        ResultsAgent a = (ResultsAgent)myAgent;
        
         // Send result before closing down
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setSender(a.getAID());
        msg.addReceiver(new AID(SendTo, false));
        try {
            Object[] oMsg=new Object[3];
            oMsg[0] = ID;
            oMsg[1] = bText1;
            oMsg[2] = bText2;
            
            msg.setContentObject(oMsg);
        } catch (IOException ex) {
            System.err.println("Cannot add result to message. Sending empty message.");
            ex.printStackTrace(System.err);
        }
        
        a.send(msg);        
   }
    
}
