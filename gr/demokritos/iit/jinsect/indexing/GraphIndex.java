/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.indexing;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An index of graphs, using semi-linear access based on two similarity
 * thresholds.
 * @author ggianna
 */
public class GraphIndex {
    protected double minForMerging = 0.8;
    protected double maxForMerging = 0.9;
    protected Distribution<String> dCategoryInstanceCount = new Distribution<String>();
    protected Distribution<String> dNameInstanceCount  = new Distribution<String>();
    protected Map<String, DocumentNGramGraph> indexOfGraphs =
            new HashMap<String, DocumentNGramGraph>();

    public void setMinForMerging(double dMinForMerging) {
        minForMerging = dMinForMerging;
    }

    public void setMaxForMerging(double dMaxForMerging) {
        maxForMerging = dMaxForMerging;
    }

    public double getMinForMerging() {
        return minForMerging;
    }

    public double getMaxForMerging() {
        return maxForMerging;
    }

    /**
     * Searches into the index for a given graph, updating the index if required.
     * Returns the symbol number for the given graph.
     * @param dgNewGraph The graph to seek into the index.
     * @return The (probably newly added) symbol of the given graph, as
     * assigned in the index.
     */
    public int searchForGraphInIndex(DocumentNGramGraph dgNewGraph) {
        int idgName = 0;
        NGramCachedGraphComparator ngcIntra = new NGramCachedGraphComparator();
        // for every graph in the index
        Set<String> graphSet = indexOfGraphs.keySet();

        DocumentNGramGraph dgSeg = (DocumentNGramGraph)dgNewGraph.clone();
        for (String graphName : graphSet) {
            // Get class graph
            DocumentNGramGraph ClassGraph = indexOfGraphs.get(graphName);
            GraphSimilarity gs;
            gs = ngcIntra.getSimilarityBetween(dgSeg, ClassGraph);
            if (calcOverallSimilarity(gs) >= maxForMerging) {
                idgName = Integer.valueOf(graphName);
                break;
            } else if (calcOverallSimilarity(gs) >= minForMerging) {
                idgName = Integer.valueOf(graphName);

                ClassGraph.mergeGraph(dgSeg, 1 - (dNameInstanceCount.getValue(graphName) /
                        (dNameInstanceCount.getValue(graphName) + 1)));
                // count the instances for the contribution of the current name
                dNameInstanceCount.increaseValue(graphName, 1.0);
                break;
            }
            // Remove current index graph subpart from instance graph
            // if the graph is not contained already in the index graph.
            if (1.0 - gs.ContainmentSimilarity > 10e-5) {
                // int iPrvSize = dgSeg.length();
                dgSeg = dgSeg.allNotIn(ClassGraph);
                // int iNxtSize = dgSeg.length();
            }
        }

        // Add new graph index
        if (idgName == 0) {
            if (!indexOfGraphs.isEmpty()) {
                idgName = graphSet.size() + 1;
            } else {
                idgName++;
            }

            indexOfGraphs.put(Integer.toString(idgName), dgSeg);
        }

        return idgName;
    }

    /**
     * Calculates the overall similarity between two graphs.
     * Default calculation is Normalized Value Similarity.
     * @param gsSim
     * @return
     */
    protected final double calcOverallSimilarity(GraphSimilarity gsSim) {
        return (gsSim.SizeSimilarity == 0) ? 0.0 :
            gsSim.ValueSimilarity / gsSim.SizeSimilarity;
    }

}
