/*
 * TokenIteratorListener.java
 *
 * Created on 31 Ιανουάριος 2006, 3:47 μμ
 */

package gr.demokritos.iit.jinsect.events;

import java.util.List;
import java.util.ListIterator;
/**
 *
 * @author ggianna
 */
public interface TokenGeneratorListener {
    public List getTokens();
    public ListIterator getIterator();
}
