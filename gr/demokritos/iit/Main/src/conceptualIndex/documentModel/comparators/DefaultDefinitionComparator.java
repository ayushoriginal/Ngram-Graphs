/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.conceptualIndex.documentModel.comparators;

import gr.demokritos.iit.conceptualIndex.documentModel.SemanticIndex;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;
import java.util.Iterator;
import java.util.List;

/** The default definition comparator, that uses the descriptions of the
 * definitions to compare between definition lists.
 *
 * @author pckid
 */
public class DefaultDefinitionComparator implements IDefinitionComparator {
    protected SemanticIndex Index;
    
    public DefaultDefinitionComparator(SemanticIndex si) {
        Index = si;
    }

    @Override
    public double CompareDefinitionLists(List<WordDefinition> lWordDefinitions1,
            List<WordDefinition> lWordDefinitions2) {
        // For every meaning in Meanings1
        double dRes = 0.0;
        Iterator iIter1 = lWordDefinitions1.iterator();
        while (iIter1.hasNext()) {
            double dMaxSim = 0.0;
            Iterator iIter2 = lWordDefinitions2.iterator();
            WordDefinition d1 = (WordDefinition)iIter1.next();
            
            // For every meaning in Meanings2
            while (iIter2.hasNext()) {
                WordDefinition d2 = (WordDefinition)iIter2.next();
                // Keep max similarity
                dMaxSim = Math.max(dMaxSim, Index.compareWordDefinitions(d1, d2));
            }
            // DEBUG LINES
            // appendToLog("Concluded similarity of " + dMaxSim);
            //////////////
            dRes += dMaxSim;
        }
        dRes = 2 * dRes / (lWordDefinitions1.size() + lWordDefinitions2.size());
        
        return dRes;
    }

}
