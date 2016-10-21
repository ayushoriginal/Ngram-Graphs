/*
 * NormalizerListener.java
 *
 * Created on 25 Ιανουάριος 2006, 2:51 πμ
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
public interface NormalizerListener {
    /***
     *Normalizes items in an array, using specific parameters
     *@param oParams The parameters object
     *@param oaItems The object to be normalized
     ***/
    public Object[] normalize(Object oParams, Object[] oaItems);
}
