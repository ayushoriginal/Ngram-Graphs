/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ILoadableTextPrint;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.indexing.GraphIndex;
import gr.demokritos.iit.jinsect.structs.ArrayGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.util.ArrayList;

/** A class for a hierarchical representation of a document.
 * The representation splits a string into a series of segments of a given
 * size and then uses the {@link GraphIndex} structure to recursively create
 * different levels of graphs.
 *
 * @author ggianna
 */
public class NGramSymWinHierDocument implements ILoadableTextPrint {

    protected int MinN;
    protected int Levels;
    protected double DistFactor;
    protected String DataString;
    protected ArrayList<int[][]> LevelArrays;
    protected ArrayList<DocumentNGramGraph> LevelGraphs;
    protected ArrayList<GraphIndex> GraphIndices;

    public NGramSymWinHierDocument(int iMinN, int iLevels, double dDistFactor,
                ArrayList<GraphIndex> giGraphIndices) {
        MinN = iMinN;
        Levels = iLevels;
        DistFactor = dDistFactor;
        GraphIndices = giGraphIndices;
        clear();
    }

    @Override
    public void loadDataStringFromFile(String sFilename) {
        setDataString(utils.loadFileToStringWithNewlines(sFilename));
    }

    public void clear() {
        LevelArrays = new ArrayList<int[][]>(Levels);
        LevelGraphs = new ArrayList<DocumentNGramGraph>(Levels);
    }

    public void setDataString(String sDataString) {
        // Clear existing data
        clear();
        // Set datastring
        DataString = sDataString;

        // For the first level
        // Create array
        LevelArrays.add(StringToIntArray(sDataString));

        ArrayGraph ag = new ArrayGraph();
        DocumentNGramGraph dgZero = ag.getGraphForArray(LevelArrays.get(0), 
                (int)DistFactor, 10000);
        // DEBUG LINES
//        System.err.println(utils.graphToDot(
//            dgZero.getGraphLevel(0), true));
        //////////////
        LevelGraphs.add(dgZero);

        // For every level
        for (int iLvl=1; iLvl<Levels; iLvl++) {
            // Get prv level array
            int[][] iaaPrv = LevelArrays.get(iLvl - 1);
            // Init level submatrix
            int[][] iaaNew = new int[1][];
            int[] iaNew = new int[iaaPrv[0].length / MinN];
            // For every step
            for (int iCurPos=0, iNewPos = 0; iCurPos < iaaPrv[0].length; iCurPos += MinN) {
                // The size is based on the level and distance factor
                int iSNeighborhoodSize = (int)(DistFactor * iLvl);
                
                if (iCurPos + iSNeighborhoodSize > iaaPrv[0].length)
                    break;
                
                int[][] iaaCur = new int[1][];
                int[] iaCur = new int[iSNeighborhoodSize];
                
                // TODO: IMPROVE BEHAVIOUR FOR MinN > 1
                // It does not break down the neighbours to MinN-grams...
                
                // Copy sub-matrix
                System.arraycopy(iaaPrv[0], iCurPos, iaCur, 0, iSNeighborhoodSize);
                // Assign row to single-row matrix
                iaaCur[0] = iaCur;
                // Create graph
                DocumentNGramGraph ng = ag.getGraphForArray(iaaCur, 
                        (int)(DistFactor * iLvl),
                        Integer.MAX_VALUE);
                iaNew[iNewPos++] = GraphIndices.get(iLvl).searchForGraphInIndex(ng);
            }
            iaaNew[0] = iaNew;
            // Add array to list of arrays
            LevelArrays.add(iaaNew);
            DocumentNGramGraph dgLvl = ag.getGraphForArray(iaaNew,
                    (int)(DistFactor * iLvl), 10000);
            LevelGraphs.add(dgLvl);
        }
    }

    public DocumentNGramGraph getLevelGraph(int iLevel) {
        return LevelGraphs.get(iLevel);
    }

    public final DocumentNGramGraph getTopLevelGraph() {
        // DEBUG LINES
//        System.err.println(utils.graphToDot(
//            LevelGraphs.get(Levels - 1).getGraphLevel(0), true));
        //////////////
        return LevelGraphs.get(Levels - 1);
    }
    
    public GraphSimilarity compareTo(NGramSymWinHierDocument other) {
        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        GraphSimilarity gs = new GraphSimilarity();
        double dSum = 0.0;
        for (int iLvl=1; iLvl<Levels; iLvl++) {
         GraphSimilarity gsLvl = ngc.getSimilarityBetween(this.getLevelGraph(iLvl),
                other.getLevelGraph(iLvl));
         gs.ValueSimilarity +=  gsLvl.ValueSimilarity * utils.sumFromTo(1, iLvl);
         gs.ContainmentSimilarity +=  gsLvl.ContainmentSimilarity * utils.sumFromTo(1, iLvl);
         gs.SizeSimilarity +=  gsLvl.SizeSimilarity * utils.sumFromTo(1, iLvl);
         dSum += utils.sumFromTo(1, iLvl);
        }
        gs.ValueSimilarity /= dSum;
        gs.ContainmentSimilarity /= dSum;
        gs.SizeSimilarity /=  dSum;
        return gs;
    }

    protected int[][] StringToIntArray(String sToConvert) {
        int[] iaRes = new int[sToConvert.length()];
        for (int iCnt = 0; iCnt < sToConvert.length(); iCnt++) {
            iaRes[iCnt] = Character.codePointAt(sToConvert, iCnt);
        }

        int [][]iaaRes = new int[1][];
        iaaRes[0] = iaRes;
        
        return iaaRes;
    }
    
    public static void main(String[] saArgs) {
        int iLevels = 3;
        double dDistFactor = 5;
        // Init level graph indices
        ArrayList<GraphIndex> giGraphIndices;
        giGraphIndices = new ArrayList<GraphIndex>(iLevels);
        for (int iLevelCnt = 0; iLevelCnt < iLevels; iLevelCnt++)
            giGraphIndices.add(new GraphIndex());

        // Create strings
        String s1 = utils.repeatString("This is the first test.", 10);
        String s2 = utils.repeatString("This is the second test.", 5);
        String s3 = utils.reverseString(s2);
        NGramSymWinHierDocument n1 = new NGramSymWinHierDocument(2, iLevels,
                dDistFactor, giGraphIndices);
        n1.setDataString(s1);
        NGramSymWinHierDocument n2 = new NGramSymWinHierDocument(2, iLevels,
                dDistFactor, giGraphIndices);
        n2.setDataString(s2);
        NGramSymWinHierDocument n3 = new NGramSymWinHierDocument(2, iLevels,
                dDistFactor, giGraphIndices);
        n3.setDataString(s3);

        System.out.println(n1.compareTo(n2));
        System.out.println(n1.compareTo(n1));
        System.out.println(n1.compareTo(n3));
        System.out.println(n2.compareTo(n3));

    }

    @Override
    public DocumentNGramHistogram getDocumentHistogram() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDocumentHistogram(DocumentNGramHistogram idnNew) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DocumentNGramGraph getDocumentGraph() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDocumentGraph(DocumentNGramGraph idgNew) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
