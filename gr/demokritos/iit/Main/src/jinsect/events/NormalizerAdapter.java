/*
 * NormalizerAdapter.java
 *
 * Created on 25 Ιανουάριος 2006, 2:52 πμ
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
public class NormalizerAdapter implements NormalizerListener {
    
    /** Creates a new instance of NormalizerAdapter */
    public NormalizerAdapter() {
    }
    
    public Object[] normalize(Object oParams, Object[] oaItems) {
        return oaItems; // No normalization
    }
}
