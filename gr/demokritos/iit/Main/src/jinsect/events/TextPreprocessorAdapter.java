/*
 * TextPreprocessorAdapter.java
 *
 * Created on 25 Ιανουάριος 2006, 9:35 πμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.events;

import java.util.regex.Pattern;

/**
 *
 * @author PCKid
 */
public class TextPreprocessorAdapter implements TextPreprocessorListener{
    public String preprocess(String sText) {
        return sText.replaceAll("\\s",  "");   // Remove whitespaces
    }
}
