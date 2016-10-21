/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/** A classto manipulate graphs in files, directly from the
 * console.
 *
 * @author ggianna
 */
public class graphIO {
    public static void main(String[] saArgs) {
        Hashtable hSwitches = utils.parseCommandLineSwitches(saArgs);
        String sFile=utils.getSwitch(hSwitches, "i", "");
        boolean bZipped = Boolean.valueOf(utils.getSwitch(hSwitches, "z",
                String.valueOf(false))).booleanValue();
        String sDotParams=utils.getSwitch(hSwitches, "dotParams", "");

        ObjectInputStream os = null;
        try {
            FileInputStream fs = new FileInputStream(sFile);
            InputStream is;
            if (bZipped)
                is = new GZIPInputStream(fs);
            else
                is = fs;
            os = new ObjectInputStream(is);
            File fTmp = File.createTempFile("jinsect", "graph");
            FileWriter fw = new FileWriter(fTmp);
            DocumentNGramGraph g = (DocumentNGramGraph) os.readObject();
            UniqueVertexGraph uv = g.getGraphLevel(0);
            fw.write(utils.graphToDot(uv, true));
            fw.close();
            String sCmd = String.format("/usr/bin/dot -Tpdf %s " +
                    "< '%s' > '%s.pdf'", sDotParams,
                fTmp.getAbsolutePath(), fTmp.getAbsolutePath());
            System.err.println(sCmd);
            try {
                Runtime.getRuntime().exec(sCmd.split(" ")).waitFor();
            } catch (InterruptedException ex) {
                Logger.getLogger(graphIO.class.getName()).log(Level.SEVERE, null, ex);
            }
            sCmd = "bash -c \"/opt/kde3/bin/kpdf '" + fTmp.getAbsolutePath() +
                    ".pdf'\"";
            System.err.println(sCmd);
            try {
                Runtime.getRuntime().exec(sCmd).waitFor();
            } catch (InterruptedException ex) {
                Logger.getLogger(graphIO.class.getName()).log(Level.SEVERE, null, ex);
            }
            os.close();
            is.close();
            if (is != fs)
                fs.close();
            // fTmp.delete();
        } catch (IOException ex) {
            Logger.getLogger(graphIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (ClassNotFoundException ex) {
            Logger.getLogger(graphIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
