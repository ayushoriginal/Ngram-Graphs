/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.documentModel.representations;

import gr.demokritos.iit.jinsect.structs.UniqueVertexHugeGraph;
import java.util.HashMap;

/**
 *
 * @author ggianna
 */
public class DocumentNGramHGraph extends DocumentNGramGraph {

    protected int Segments = 4;

    public DocumentNGramHGraph(int iMinSize, int iMaxSize, int iDist, int iSegments) {
        Segments = iSegments;
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        CorrelationWindow = iDist;
    }


    public DocumentNGramHGraph(int iSegments) {
        Segments = iSegments;
        InitHGraphs();
    }


    private void InitHGraphs() {
        // Create array of graphs
        NGramGraphArray = new UniqueVertexHugeGraph[MaxSize - MinSize + 1];
        // Init array
        for (int iCnt=MinSize; iCnt <= MaxSize; iCnt++)
            NGramGraphArray[iCnt - MinSize] = new UniqueVertexHugeGraph(Segments);
        // Create degraded edge list
        DegradedEdges = new HashMap();
    }

}
