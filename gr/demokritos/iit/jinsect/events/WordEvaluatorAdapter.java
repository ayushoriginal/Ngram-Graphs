/*
 * WordEvaluatorAdapter.java
 *
 * Created on 25 Ιανουάριος 2006, 1:17 πμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.events;

/**
 *
 * @author PCKid
 */
public class WordEvaluatorAdapter implements WordEvaluatorListener {
    
    /** Creates a new instance of WordEvaluatorAdapter */
    public WordEvaluatorAdapter() {
    }
    
    public boolean evaluateWord(String sWord) {
        return true; // Accept all
    }
}
