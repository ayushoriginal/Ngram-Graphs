/*
 * ReceiveResultBehaviour.java
 *
 * Created on 15 ?????????????????????? 2007, 1:14 ????
 *
 */

package gr.demokritos.iit.jinsect.distributed;

import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;

/** Implements a behaviour that performs the receiving of a result.
 *
 * @author ggianna
 */
public class ReceiveResultBehaviour extends WakerBehaviour {
    boolean Blocking = false;
    ResultsAgent  myAgent;
    long Interval;
    PrintStream pOut = null;
    
    /** Initializes the receiving behaviour, with a given interval. Uses the output
     * stream of the linked agent as output stream.
     *@param a The result agent connected to the behaviour.
     *@param lInterval The interval indicating whem to wake.
     *@see WakerBehaviour
     */
    public ReceiveResultBehaviour(ResultsAgent a, long lInterval) {
        // Init
        super(a, lInterval);
        myAgent = a;
        Interval = lInterval;
        
        // Connect to output stream
        if (myAgent.OutFile.length() != 0) {
            try {
                pOut = new PrintStream(myAgent.OutFile);
            }
            catch (FileNotFoundException fnfe) {
                System.err.println("Cannot output to selected file:\n" + fnfe.getMessage());
                System.exit(1);
            }
        }
        if (pOut == null)
            pOut = System.err;
    }
    
    /** Initializes the behaviour with a given interval, and a result output stream.
     *@param a The result agent connected to the behaviour.
     *@param lInterval The interval indicating whem to wake.
     *@param pResultOutput The stream to use for output.
     */
    public ReceiveResultBehaviour(ResultsAgent a, long lInterval, PrintStream pResultOutput) {
        super(a, lInterval);
        myAgent = a;
        Interval = lInterval;
        pOut = pResultOutput;
        
    }
    
    /** Initializes the behaviour with a given interval, a result output stream,
     * as well as an indication of whether the wait is blocking.
     *@param a The result agent connected to the behaviour.
     *@param lInterval The interval indicating whem to wake.
     *@param pResultOutput The stream to use for output.
     *@param bBlocking If true, then the wait is blocking others until its completion.
     */
    public ReceiveResultBehaviour(ResultsAgent a, long lInterval, PrintStream pResultOutput,
            boolean bBlocking) {
        super(a, lInterval);
        myAgent = a;
        Interval = lInterval;
        pOut = pResultOutput;
        Blocking = bBlocking;
    }

    /** Performs the result reception event, eighter blocking or non-blocking (see 
     * constructor), and outputs result to given strem. 
     * Also keeps track of pending requests and manages completion of the process.
     */
    protected void onWake() {
        ACLMessage msg;
        if (Blocking) {
            msg = myAgent.blockingReceive();
        }
        else
            if ((msg = myAgent.receive()) == null) {
                myAgent.addBehaviour(new ReceiveResultBehaviour(myAgent, 
                        Interval, pOut, Blocking));
                return;
            }
        
        CalcResults caRes = null;
        SimilarityArray saRes = null;
        if (msg != null) {
            try 
            {
                // Check if not results
                if (msg.getPerformative() != ACLMessage.INFORM) 
                {
                    // Check if activation
                    if (msg.getPerformative() == ACLMessage.AGREE) {
                        Object oAgentInfo = msg.getContentObject();
                        synchronized (myAgent) {
                            myAgent.InitializedAgents.remove(oAgentInfo);
                            myAgent.ActiveAgents.add(oAgentInfo);
                        }
                        // Wait for messages
                        synchronized(System.err) {
                            if (!myAgent.Silent)
                                System.err.println("Received move complete message from " +
                                        oAgentInfo);
                        }

                    }
                    return;
                }
                // Results
                Object oTemp = msg.getContentObject();
                caRes = (CalcResults)oTemp;
                saRes = caRes.Simil;
                if (saRes != null)
                {
                    //pOut.print(msg.getSender().getLocalName().substring("CalcAgent".length() + 
                            //myAgent.AgentUniqueIDLength) + 
                            //"\t");
                    pOut.print(caRes.ID + 
                        "\t");
                    
                    //pOut.print(msg.getSender().getLocalName().substring("CalcAgent".length() + 
                            //myAgent.AgentUniqueIDLength) + 
                            //"\t");
                    try {
                        pOut.print(saRes.SimpleTextGraphSimil.ContainmentSimilarity + "\t" +
                            saRes.SimpleTextGraphSimil.ValueSimilarity + "\t" +
                            saRes.SimpleTextGraphSimil.SizeSimilarity + "\t" +
                            saRes.SimpleTextHistoSimil.ContainmentSimilarity + "\t" +
                            saRes.SimpleTextHistoSimil.ValueSimilarity + "\t" +
                            saRes.SimpleTextHistoSimil.SizeSimilarity + "\t" + 
                            saRes.SimpleTextOverallSimil.getOverallSimilarity() + "\t");
                    }
                    catch (NullPointerException ne) {
                        // Ignore
                        pOut.print("0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" + 
                            "0.0" + "\t");
                    }
                    try {
                        pOut.print(saRes.NGramGraphSimil.ContainmentSimilarity + "\t" +
                            saRes.NGramGraphSimil.ValueSimilarity + "\t" +
                            saRes.NGramGraphSimil.SizeSimilarity + "\t" +
                            saRes.NGramHistoSimil.ContainmentSimilarity + "\t" +
                            saRes.NGramHistoSimil.ValueSimilarity + "\t" +
                            saRes.NGramHistoSimil.SizeSimilarity + "\t" + 
                            saRes.NGramOverallSimil.getOverallSimilarity());
                    }
                    catch (NullPointerException ne) {
                        pOut.print("0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" +
                            "0.0" + "\t" + 
                            "0.0");
                    }
                    pOut.println();
                    
                }

            } catch (UnreadableException ex) {
                System.err.println("Invalid result returned...");
                ex.printStackTrace(System.err);
            }                        

            // Decrease pending requests
            synchronized (myAgent) {
                myAgent.PendingRequests--;
                myAgent.CompletedRequests++;
                
                if ((!myAgent.Silent) || (myAgent.ShowProgress))
                    System.err.println("Completed " + String.format("%7.4f", 
                        (double)myAgent.CompletedRequests / myAgent.AllRequests * 100) + "%");
                if (myAgent.CompletedRequests == myAgent.AllRequests)
                    myAgent.complete(); // Finished

                // DEBUG LINES
                // System.err.println("Pending " + myAgent.PendingRequests + " requests...");
                //////////////
            };
        }
    }
}

