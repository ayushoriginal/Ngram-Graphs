/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.demokritos.iit.conceptualIndex.documentModel.comparators;

import gr.demokritos.iit.conceptualIndex.documentModel.SemanticIndex;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author ggianna
 */
public class SingleMeaningDefComparator extends DefaultDefinitionComparator {
    public SingleMeaningDefComparator(SemanticIndex si) {
        super(si);
    }

    
    @Override
    public double CompareDefinitionLists(List<WordDefinition> lWordDefinitions1,
            List<WordDefinition> lWordDefinitions2) {
        // For every meaning in Meanings1
        double dMaxSim = 0.0;
        Iterator iIter1 = lWordDefinitions1.iterator();
        while (iIter1.hasNext()) {
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
        }
        return dMaxSim;
    }
    
}
