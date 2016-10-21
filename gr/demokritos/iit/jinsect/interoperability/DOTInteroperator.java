/*
 * DOTInteroperator.java
 *
 * Created on May 4, 2007, 2:44 PM
 *
 */

package gr.demokritos.iit.jinsect.interoperability;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 *
 * @author ggianna
 */
public class DOTInteroperator {
    
    /** Creates a new instance of DOTInteroperator */
    public DOTInteroperator() throws IOException {
        Process p = Runtime.getRuntime().exec("dot -V");
        try {
            p.waitFor();
        }
        catch (InterruptedException iee) {
            // Ignore
            return;
        }
    }
    
    public void convertDOTtoImage(String sDOT, String sImageType, String sImageFileName) {
        try {
            // Create temp dot file
            File fTempFile = File.createTempFile("jinsect",".dot");
            PrintStream pOut = new PrintStream(fTempFile);
            pOut.print(sDOT);
            pOut.flush();
            pOut.close();
            
            // Run Dot program
            String[] saCmd = {"dot","-T"+sImageType, "-o " + sImageFileName, fTempFile.getAbsoluteFile().toString()};
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            // wait for command to terminate
            try {
                p.waitFor();
            }
            catch (InterruptedException e) {
                System.err.println("Process was interrupted");
            }
            // check its exit value
            if (p.exitValue() != 0)
                System.err.println("Exit value was non-zero");
            
            // delete temp file
            fTempFile.delete();
        }
        catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return; // Failure
        }
    }
}
