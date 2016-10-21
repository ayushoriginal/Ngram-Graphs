/*
 * ConsoleNotificationListener.java
 *
 * Created on November 2, 2007, 12:52 PM
 *
 */

package gr.demokritos.iit.jinsect.console;

import java.util.Date;
import gr.demokritos.iit.jinsect.events.NotificationListener;

/** A {@link NotificationListener} subclass, used to redirect notification events to the console 
 * (stderr). It is used to indicate progress in command-line execution.
 *
 * @author ggianna
 */
public class ConsoleNotificationListener implements NotificationListener  {
    /** The start of time counting. */
    private Date dStart = new Date();
    
    /** Constructor. */
    public ConsoleNotificationListener() {
    }
    
    /** Reset start of time counting to now. */
    public void resetStartTime() {
        dStart = new Date();
    }
    
    /** Outputs the notification to the console.
     *@param oSender Unused. Can contain the sender of the notification.
     *@param oParams Expects a Double object containing the percentage of the process completion.
     */
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
