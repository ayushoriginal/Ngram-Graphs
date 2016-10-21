/* Under the terms of LGPL licence.
 */

package gr.demokritos.iit.jinsect.indexing;

import java.util.Set;

/** An interface class describing indices of documents.
 *
 * @author pckid
 */
public interface IIndex<TRepresentationType> {
    /** Performs the required operations to create the index. */
    public void createIndex();
    /** Returns a set of String IDs, corresponding to the documents most 
     *relevant to a given document representation
     */
    public Set<String> locateSimilarDocuments(TRepresentationType dngCur);
}
