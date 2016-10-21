/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.summarization.analysis.SentenceSplitter;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author ggianna
 */
public class FileSentenceSplitter {
    public static void main(String[] sArgs) {
        Hashtable<String,String> hSwitches = utils.parseCommandLineSwitches(sArgs);
        String sInput = utils.getSwitch(hSwitches, "input", "");
        if (sInput.length() == 0) {
            System.err.println("No input. Terminating...");
            return;
        }

        SentenceSplitter ss = new SentenceSplitter();
        List<String> lSentences = ss.split(utils.loadFileToStringWithNewlines(sInput));
        for (String sSent : lSentences) {
            if (sSent.trim().length() > 0)
                System.out.println(sSent);
        }
    }

}
