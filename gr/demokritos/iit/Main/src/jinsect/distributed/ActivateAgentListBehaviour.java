/*
 * ActivateAgentListBehaviour.java
 *
 * Created on 15 ?????????????????????? 2007, 1:18 ????
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.distributed;

import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.io.PrintStream;
import java.util.ConcurrentModificationException;
import java.util.Queue;

/** This class initializes a set of calculating agents and randomly dispatches them to 
 * various containers. It waits every agent to inform for its arrival at the container, 
 * via a corresponding ReceiveResultBehaviour. Then it contacts every agent, in round robin
 * sending it data to perform calculations
 * and routes receive behaviors to accept those results.
 */
public class ActivateAgentListBehaviour extends WakerBehaviour {
    long Interval;
    Agent myAgent;
    Queue CalcArgs;
    PrintStream OutputStream;
    double MigrationProbability;
    Object[] AgentArgs;
    
    /** Creates the dispatching agent, given a set of calculation arguments to forward to 
     * created calculation agents and a migration probability for newly created agents.
     *@param a The agent that will manage the results of the calculations. 
     *@param lInterval The interval in millisecs after which the agent will act again until 
     * all calculations have been routed.
     *@param lCalcArgs A list of calculation arguments that wil lbe sent to the calculating agents.
     *@param pOut The debug output stream.
     *@param dMigrationProbability The probability that agents created will migrate to other
     * containers.
     *@param oaAgentArgs An array of arguments, common to all agents created.
     */
    public ActivateAgentListBehaviour(ResultsAgent a, long lInterval, Queue lCalcArgs, PrintStream pOut,
            double dMigrationProbability, Object[] oaAgentArgs) {
        // Init
        super(a, lInterval);
        myAgent = a;
        Interval = lInterval;
        CalcArgs = lCalcArgs;
        OutputStream = pOut;
        MigrationProbability = dMigrationProbability;
        AgentArgs = oaAgentArgs;
    }
    
    /** Performs the creation and dispatchiong of calculating agents, as well as the routing
     * of calculation parameters to them. Also, queues behaviours for the reception of results.
     */
    protected void onWake() {
        synchronized (myAgent) 
        {
            // Create and send agents
            ResultsAgent aCaller= (ResultsAgent)myAgent;
            try 
            {                        
                while (aCaller.InitializedAgents.size() + aCaller.ActiveAgents.size()
                        < aCaller.MaxAgents) {
                    // Create new agent
                    AgentController acCur = myAgent.getContainerController().createNewAgent("CalcAgent"+
                            aCaller.createAgentID(aCaller.AgentUniqueIDLength), 
                            "jinsect.distributed.NGramDocumentComparatorAgent", AgentArgs);
                    aCaller.InitializedAgents.add(acCur.getName());                            
                    acCur.start();
                    // Wait for move complete message
                    myAgent.addBehaviour(new ReceiveResultBehaviour(aCaller, Interval,
                        OutputStream));                                
                }
            } catch (StaleProxyException ex) {
                System.err.println("Cannot execute agent...");
                ex.printStackTrace(System.err);
            }

            // Check if there are any active agents
            if (aCaller.ActiveAgents.size() > 0) {
                if (aCaller.CurrentAgent == null)
                    // Position iterator to the end
                    aCaller.CurrentAgent = aCaller.ActiveAgents.listIterator(aCaller.ActiveAgents.size());
                while (aCaller.PendingRequests < aCaller.MaxPendingRequests)
                {
                    AgentData adCur = (AgentData)CalcArgs.poll();
                    if (adCur != null) {
                        // Send texts to next agent, using round robin.
                        String sNextAgentName;
                        if (!aCaller.CurrentAgent.hasPrevious())
                            aCaller.CurrentAgent = 
                                    aCaller.ActiveAgents.listIterator(aCaller.ActiveAgents.size());
                        
                        // Check for change in iterator data
                        try {
                            aCaller.CurrentAgent.previous();
                            aCaller.CurrentAgent.next();
                        }
                        catch (ConcurrentModificationException cmeE) {
                            aCaller.CurrentAgent = aCaller.ActiveAgents.listIterator(aCaller.ActiveAgents.size());
                        }

                        // Actually get name
                        sNextAgentName = (String)aCaller.CurrentAgent.previous();

                        // Send texts message
                        myAgent.addBehaviour(new SendTextsBehaviour(adCur.ID,
                                adCur.Texts[0], adCur.Texts[1], sNextAgentName));
                        // Wait for result message
                        myAgent.addBehaviour(new ReceiveResultBehaviour(aCaller, Interval,
                            OutputStream));                                
                        ((ResultsAgent)myAgent).PendingRequests++;
                    }
                    else
                        break; // AgentData have all been sent
                }

            }

            // If pending requests exist, then repeat behaviour.
            if ((!CalcArgs.isEmpty()))
                myAgent.addBehaviour(new ActivateAgentListBehaviour(aCaller, 
                        Interval, CalcArgs, OutputStream, MigrationProbability, AgentArgs));
                
        }
    }
}
