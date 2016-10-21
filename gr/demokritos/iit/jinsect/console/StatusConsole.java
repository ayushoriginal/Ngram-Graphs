/*
 * StatusConsole.java
 *
 * Created on May 23, 2007, 1:16 PM
 *
 */

package gr.demokritos.iit.jinsect.console;

import javax.swing.SwingUtilities;
import gr.demokritos.iit.jinsect.gui.IStatusDisplayer;
import gr.demokritos.iit.jinsect.gui.StatusFrame;

/** This class describes object that can display status ({@link IStatusDisplayer} subclass)
 * and directs the output to the console (stderr).
 * @author ggianna
 */
public class StatusConsole implements IStatusDisplayer {
    boolean bShowOutput = true;
    protected String LabelText;
    protected int Width;
    
    /** Creates a new instance of StatusConsole */
    public StatusConsole(int iWidth) {
        Width = iWidth;
    }

    public synchronized void setStatus(final String sText, final double dValue) {
        LabelText = sText;
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() 
                {
                    String sTxt = sText;
                    if (dValue != -1)
                        sTxt = String.format("%3.2f%% - ", dValue * 100).concat(sTxt);
                    if (sTxt.length() < Width)
                        sTxt.concat(gr.demokritos.iit.jinsect.utils.repeatString(" ", Width-sTxt.length()));
                    else
                        sTxt = sTxt.substring(0,Width);
                        
                    if (bShowOutput)
                        System.err.print(sTxt + "\r");
                    
                    //ProgressBar.update(ProgressBar.getGraphics());
                    //update(getGraphics()); 
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** Enables or disables the output of messages.
     *@param bShow If true enables output, else disables it.
     */
    public void setVisible(boolean bShow) {
        bShowOutput = bShow;
        System.err.println();
    }
    
    public boolean getVisible() {
        return bShowOutput;
    }
    
    public synchronized String getStatusText() {
        return LabelText;
    }
}
