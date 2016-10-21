/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.demokritos.iit.conceptualIndex.documentModel.comparators;

import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;
import java.util.List;

/** Describes classes than can compare WordDefinition objects.
 *
 * @author pckid
 */
public interface IDefinitionComparator {
    public double CompareDefinitionLists(List<WordDefinition> lWordDefinitions1,
            List<WordDefinition> lWordDefinitions2);
}
