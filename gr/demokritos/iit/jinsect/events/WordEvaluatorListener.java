/*
 * WordEvaluatorListener.java
 *
 * Created on 25 Ιανουάριος 2006, 1:16 πμ
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
public interface WordEvaluatorListener {
    /***
     * Should return false if the word to evaluate should be rejected.
     * Otherwise return true.
     *@param sWord The word to evaluate.
     ***/
    public boolean evaluateWord(String sWord);
}
