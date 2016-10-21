/*
 * TokenIteratorListener.java
 *
 * Created on 31 Ιανουάριος 2006, 3:38 μμ
 */

package gr.demokritos.iit.jinsect.events;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
/**
 *
 * @author ggianna
 */
public class TokenGeneratorAdapter implements TokenGeneratorListener {
    private List lTokens;
    public TokenGeneratorAdapter(String sSource, String sRegExp) {
        String[] sTokenStrings = sSource.split(sRegExp);
        lTokens = Arrays.asList(sTokenStrings);
    }
    
    public ListIterator getIterator() {
        return lTokens.listIterator();
    }
    
    public List getTokens() {
        return lTokens;
    }
}
