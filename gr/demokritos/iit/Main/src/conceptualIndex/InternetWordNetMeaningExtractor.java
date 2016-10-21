/*
 * InternetWordNetMeaningExtractor.java
 *
 * Created on 10 Ιανουάριος 2007, 2:03 μμ
 *
 */

package gr.demokritos.iit.conceptualIndex;

import gr.demokritos.iit.jinsect.supportUtils.linguistic.DictService;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.DictServiceSoap;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;

/** This class is used to retrieve word meanings using the DictService web service than can be
 * found at 
 * <a href ='http://services.aonaware.com/webservices/'>http://services.aonaware.com/webservices/</a>.
 * @author ggianna
 */
public class InternetWordNetMeaningExtractor implements IMeaningExtractor {
    
    /** Fetches word net definitions of a given string.
     *@param sString The word to lookup.
     *@return The definition of the word looked up.
     */
    public WordDefinition getMeaning(String sString) {
        DictService dServe = new DictService();
        DictServiceSoap dsServe = dServe.getDictServiceSoap();
        WordDefinition wd = dsServe.defineInDict("wn", sString); // WordNet

        return wd;
    }
}
