/*
 * JTextAreaPrintStream.java
 *
 * Created on 8 Μάρτιος 2007, 5:15 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.gui.utils;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** A JTextArea wrapper to be used as PrintStream for redirection of System.err or System.out.
 *
 * @author ggianna
 */
public class JTextAreaPrintStream extends PrintStream {
    JTextArea OutputArea;
    /** Creates a new instance of JTextAreaPrintStream.
     *@param taOut The target textarea.
     */
    public JTextAreaPrintStream(JTextArea taOut) {
        super(System.out, true); // Redirect to system.out by default
        OutputArea = taOut;
    }

    public void println(final String x) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                OutputArea.append(x + "\n");
                //OutputArea.update(OutputArea.getGraphics());
            }
        });
    }
    

    public void print(final String s) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                OutputArea.cut();
                OutputArea.append(s.replaceAll("\r", "\n"));
                //OutputArea.update(OutputArea.getGraphics());
            }
        });
    }
    
    public void println() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                OutputArea.append("\n");
                //OutputArea.update(OutputArea.getGraphics());
            }
        });
    }
}
