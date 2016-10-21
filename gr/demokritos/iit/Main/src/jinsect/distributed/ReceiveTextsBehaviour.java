/*
 * ReceiveTextsBehaviour.java
 *
 * Created on 15 ?????????????????????? 2007, 1:16 ????
 *
 */

package gr.demokritos.iit.jinsect.distributed;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/** A behaviour that receives texts and routes calculation of similarity between
 * them. Also checks for cancellation messages, in order to finish the reception 
 * process.
 */
public class ReceiveTextsBehaviour extends TickerBehaviour {
    long lTimeout;
    
    public ReceiveTextsBehaviour(Agent a, long l) {
        super(a, l);
        lTimeout = l;
    }
   
    /** Checks for incoming texts in a non-blocking way, and also checks for 
     * cancellation messages.
     */
    public void onTick() {
        ACLMessage msg;
        synchronized (myAgent) 
        {
            if ((msg = myAgent.receive()) == null) {
                // Wait for messages
                return;
            }
            
            if (msg.getPerformative() == ACLMessage.CANCEL)
            {
                myAgent.doDelete();
                return;
            }
            
            if (msg.getPerformative() != ACLMessage.INFORM) {
                synchronized (System.err) {
                    System.err.println("Ignoring message with unknown performative...");
                }
                
                return;
            }
            
            try {
                // Get texts
                Object[] oTexts = (Object[])msg.getContentObject();

                // Decompress texts
                String[] sTexts = new String[3];
                sTexts[0] = (String)oTexts[0];

                try {
                    ByteArrayInputStream bIn = new ByteArrayInputStream((byte[])oTexts[1]);
                    GZIPInputStream gzIn = new GZIPInputStream(bIn);
                    int iChar;
                    StringBuffer sbBuf = new StringBuffer();
                    while ((iChar = gzIn.read()) > -1)
                        sbBuf.append((char)iChar);

                    sTexts[1] = sbBuf.toString();
                }
                catch (IOException ioe) {
                    synchronized (System.err) {
                        ioe.printStackTrace(System.err);
                    }
                  sTexts[1] = new String((byte[])oTexts[1]);
                }

                try {
                    ByteArrayInputStream bIn = new ByteArrayInputStream((byte[])oTexts[2]);
                    GZIPInputStream gzIn = new GZIPInputStream(bIn);
                    int iChar;
                    StringBuffer sbBuf = new StringBuffer();
                    while ((iChar = gzIn.read()) > -1)
                        sbBuf.append((char)iChar);

                    sTexts[2] = sbBuf.toString();
                }
                catch (IOException ioe) {
                    synchronized (System.err) {
                        ioe.printStackTrace(System.err);
                    }
                    sTexts[2] = new String((byte[])oTexts[2]);
                }

                // Schedule calculation
                NGramDocumentComparatorAgent a = (NGramDocumentComparatorAgent)myAgent;
                a.addBehaviour(new CalcBehaviour(sTexts[1], sTexts[2], 
                        a.WordMin, a.WordMax, a.WordDist,
                        a.CharMin, a.CharMax, a.CharDist, 
                        a.Do.equals("char") || a.Do.equals("all"), 
                        a.Do.equals("word") || a.Do.equals("all"), a.Silent, sTexts[0]));
                // DEBUG LINES
                if (!a.Silent)
                    System.err.println("Successfully received texts:" + sTexts[0]);
                //////////////
            } catch (UnreadableException ex) {
                synchronized(System.err) {
                    System.err.println("Cannot read input texts...");
                    System.err.flush();
                    ex.printStackTrace();
                }
            }
        }
    }
}