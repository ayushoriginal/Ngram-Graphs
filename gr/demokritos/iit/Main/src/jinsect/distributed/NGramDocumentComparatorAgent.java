/*
 * NGramDocumentComparatorAgent.java
 *
 * Created on 9 ?????????????????????? 2007, 4:10 ????
 *
 */

package gr.demokritos.iit.jinsect.distributed;

import jade.core.AID;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.structs.SimilarityArray;

/** A mobile agent that manages the comparison between two documents. It has the
 * ability to migrate and inform its dispatcher of its arrival. Then it performs any
 * calculation it receives via agent messages.
 *
 * @author ggianna
 */
public class NGramDocumentComparatorAgent extends MobileAgent {
    // Constants
    public static final int INVALID_STATE = 1;
    public static final int ACTIVE_STATE = 1;
    public static final int MIGRATE_STATE = 2;
    public static final int CALC_STATE = 3;
    public static final int FINISHED_STATE = 100;
    public static final String TERMINATION_MESSAGE = "TERM";
    
    protected AID ResultConsumer;
    //protected CalcResults Result;
    //protected String CalcID;
    private transient ArrayList ContainerList;
    
    // Fields
    protected String sDocument1;
    protected String sDocument2;
    //protected int iState = INVALID_STATE; // Init state to invalid
    protected double MigrationProbability = 0.8;
    Integer WordMin, WordMax, WordDist, CharMin, CharMax, CharDist;
    String Do;
    boolean Silent;
    //Object[] CallerArgs;
    
    /** Creates a new instance of NGramDocumentComparatorAgent */
    public NGramDocumentComparatorAgent() {
        super();
    }
/*            
    public NGramDocumentComparatorAgent(AID aidResultConsumer, String[] saCallerArgs) {
        super();
        ResultConsumer = aidResultConsumer;
    }
 */
    
    //public synchronized void setState(int iNewState) {
        //iState = iNewState;
    //}
    
    /** Prepares and sends a calculated result. 
     *@param saResult The similarity array to return.
     *@param sID The unique comparison identifier.
     */
    public synchronized void setResult(SimilarityArray saResult, String sID) {
        CalcResults Result = new CalcResults();
        Result.Simil = saResult;
        Result.ID = sID;
        this.addBehaviour(new SendResultBehaviour(Result));
        //this.addBehaviour(new ReceiveTextsBehaviour(this, 100));
    }
    
    /** Parses arguments and initializes migration. */
    public void setup() {       
        Object[] oArgs = getArguments();
        if ((oArgs == null) || (oArgs.length == 0))
        {
            System.err.println("No args...");
        }
        
        // First argument is the caller
        ResultConsumer = (AID)oArgs[0];
        
        // Get string args
        String[] args;
        ArrayList alArgs = new ArrayList();
        for (int iCnt = 0; iCnt < oArgs.length; iCnt++) {
            if (oArgs[iCnt] instanceof String)
                alArgs.add(oArgs[iCnt]);
        }
        args = new String[alArgs.size()];
        args = (String[])alArgs.toArray(args);
        
        // DEBUG LINES
        // System.err.println(this.getName() + ": Parsing args...");
        //////////////
        
        Hashtable hSwitches = gr.demokritos.iit.jinsect.utils.parseCommandLineSwitches(args);
            
        // Parse commandline
        try {
            WordMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordMin", "1"));
            WordMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordMax", "2"));
            WordDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"wordDist", "3"));
            CharMin = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charMin", "3"));
            CharMax = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charMax", "5"));
            CharDist = Integer.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"charDist", "3"));
            MigrationProbability = Double.valueOf(gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"migProb", 
                    String.valueOf(MigrationProbability)));
            
            // Define method
            Do = gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches,"do", "all");
            // Determine if silent
            Silent=gr.demokritos.iit.jinsect.utils.getSwitch(hSwitches, "s", "FALSE").equals("TRUE");
            
            if (!Silent)
                System.err.println(this.getName() + ":" + "Using parameters:\n" + hSwitches);
            
            
        }
        catch (ClassCastException cce) {
            System.err.println(this.getName() + ":" + "Malformed switch:" + cce.getMessage() + ". Aborting...");
        }
        
        //setState(ACTIVE_STATE); // Set state to active
        super.setup(); // Adds check for locations
        
    }

    /** Displays debug info before migration. */
    protected void beforeMove() {
        if (!Silent)
            // DEBUG LINES
            synchronized (System.err) {
                System.err.println(getLocalName()+" is now migrating.");
            }
            //////////////

    }
    /** Updates dispatcher upon arrival to target container. */
    protected void afterMove() {
        if (!Silent)
            // DEBUG LINES
            synchronized (System.err) {
                System.err.println(this.getName() + ":" + "Move finished...");
            }
            //////////////
        super.afterMove();        
        
        // Inform dispatcher on complete move
        addBehaviour(new InformForCompleteMoveBehaviour());
        
    }
    
    /** Offers debug info for the termination of the agent. */
    public void takeDown() {
        //setState(FINISHED_STATE);
        
        if (!Silent)
            // DEBUG LINES
            System.err.println(this.getName() + ":" + "Finished.");
            //////////////
    }
    
    /** Returns an array of active locations. 
     *@return The array of locations.
     */
    public Location[] getActiveContainerList() {
        if (ContainerList == null)
            return null; // Not initialized yet
        
        Location[] laRes = new Location[ContainerList.size()];
        laRes = (Location[])ContainerList.toArray(laRes);
        
        return laRes;
    }
    
    /** Updates the location list and schedules migrations. 
     *@param iIter An iterator of candidate locations.
     */
    public void updateLocations(Iterator iIter) {
        //if (iState != ACTIVE_STATE) // If not in active state ignore
        //{
            //if (!Silent)
                //System.err.println(this.getName() + ":" + "Ignoring locations...");
            //return;
        //}
            
        
        if (ContainerList != null)
            ContainerList.clear();
        ContainerList = new ArrayList();
        while (iIter.hasNext())
            ContainerList.add((Location)iIter.next());
        
        // Enter migration state
        //setState(MIGRATE_STATE);
        // Schedule migrate
        addBehaviour(new MigrateBehaviour());
    }
}

/** A one-shot behaviour that performs the migration of an agent, via random selection
 * of host containers. 
 *
 */
class MigrateBehaviour extends OneShotBehaviour {
    private String MigrationTarget = null;
    
    /** Performs the migration. It uses the parent agents migration probability 
     * to determine if migration should occur.
    */
    public void action() {
        // Implement random selection of host
        Location[] lLocations;
        NGramDocumentComparatorAgent aAgent = ((NGramDocumentComparatorAgent)myAgent);
        if ((lLocations = aAgent.getActiveContainerList()) == null)
            return;
        
        // Check if should migrate
        boolean bMigrate = Math.random() < aAgent.MigrationProbability;
        bMigrate = bMigrate && (lLocations.length > 1); // Make sure that we do not require migration if
            // only one container exists
        
        if (bMigrate)
        {
            int iServerIdx = (int)Math.floor(Math.random() * (double)lLocations.length);
            int iCnt = 0;
            while (lLocations[iServerIdx].getID().equals(myAgent.here().getID())) {
                ++iServerIdx;
                iServerIdx %= lLocations.length;
                
                if (++iCnt == 3)
                    break;  // 3 attempts to migrate
            }
            
            if (lLocations[iServerIdx].getID().equals(myAgent.here().getID())) {
                myAgent.addBehaviour(new InformForCompleteMoveBehaviour());
                if (!aAgent.Silent) {
                    synchronized (System.err) {
                        System.err.println("Local execution...");
                    }
                }
                return;
            }
            else
                if (!aAgent.Silent)
                    System.err.println("Migrating to " + lLocations[iServerIdx].getName());
            
            myAgent.doMove(lLocations[iServerIdx]);
        }
        else {
            myAgent.addBehaviour(new InformForCompleteMoveBehaviour());
            if (!aAgent.Silent) {
                synchronized (System.err) {
                    System.err.println("Local execution...");
                }
            }
            return;
        }
    }
}

/** An one-shot behaviour that sends the results to the dispatcher agent. */
class SendResultBehaviour extends OneShotBehaviour {
    CalcResults Result;
    
    /** Initializes the SendResultBehaviour using a calculated result. 
     *@param rResult The calculated result to send.
     */
    public SendResultBehaviour(CalcResults rResult) {
        super();
        
        Result = rResult;
    }
    
    /** Actually sends the result to the dispatching agent.
     */
    public void action() {
        synchronized (myAgent) {
            NGramDocumentComparatorAgent a = (NGramDocumentComparatorAgent)myAgent;
        
             // Send result before closing down
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setSender(a.getAID());
            msg.addReceiver(a.ResultConsumer);
            try {
                msg.setContentObject(Result);
            } catch (IOException ex) {
                System.err.println("Cannot add result to message. Sending empty message.");
                ex.printStackTrace(System.err);
            }

            a.send(msg);
        }
        
   }
}

/** One-shot behaviour that is used to inform the dispatcher of complete arrival. */
class InformForCompleteMoveBehaviour extends OneShotBehaviour {
    /** Sends the arrival message. 
     */
    public void action() {
        synchronized (myAgent) {
            NGramDocumentComparatorAgent a = (NGramDocumentComparatorAgent)myAgent;
        
             // Send result before closing down
            ACLMessage msg = new ACLMessage(ACLMessage.AGREE);
            msg.setSender(a.getAID());
            msg.addReceiver(a.ResultConsumer);
            try {
                msg.setContentObject(a.getName());
            } catch (IOException ex) {
                System.err.println("Cannot send move completion message. Sending empty message.");
                ex.printStackTrace(System.err);
            }

            a.send(msg);
            if (!a.Silent) {
                synchronized(System.err) {
                    System.err.println("Sending move completion message");
                }
            }
            // Schedule message receiving
            a.addBehaviour(new ReceiveTextsBehaviour(a, 100));
        }
        
   }
    
}