/*
 * StreamOutputConsole.java
 *
 * Created on September 27, 2007, 4:45 PM
 *
 */

package gr.demokritos.iit.jinsect.console;

import java.io.OutputStream;
import java.io.PrintStream;
import gr.demokritos.iit.jinsect.gui.IStatusDisplayer;

/** A {@link IStatusDisplayer} outputting status to a given PrintStream.
 *
 * @author ggianna
 */
public class StreamOutputConsole implements IStatusDisplayer {
    
    /** Indicates whether progress percentage should be output. */
    boolean OutputProgressPercentage = false;
    /** The output stream. */
    PrintStream Output = null;
    /** Defines visibility of output. If false not output is supplied. */
    boolean bVisible = true;
    
    private String sLastText;
    
    /**
     * Creates a new instance of StreamOutputConsole, given an output stream and an
     * indication of whether progress percentage should be output.
     *@param osOut The output {@link PrintStream}.
     *@param bOutputProgressPercentage If true, progress percentage is output.
     */
    public StreamOutputConsole(PrintStream osOut, boolean bOutputProgressPercentage) {
        Output = osOut;
        OutputProgressPercentage = bOutputProgressPercentage;
    }

    @Override
    public void setStatus(final String sText, final double dValue) {
        StringBuffer sbOut = new StringBuffer(sText);
        if (OutputProgressPercentage)
            sbOut.append(dValue);
        sLastText = sbOut.toString();
        
        if (bVisible)
            Output.println(sLastText);
    }

    @Override
    public String getStatusText() {
        return sLastText;
    }

    /** Defines whether progress percentage should be output. 
     *@param bShow If true, output will be directed to the printstream, else it will not.
     */
    @Override
    public void setVisible(boolean bShow) {
        bVisible = bShow;
    }

    @Override
    public boolean getVisible() {
        return bVisible;
    }
    
}
