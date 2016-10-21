/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramSymWinHierDocument;
import gr.demokritos.iit.jinsect.indexing.GraphIndex;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 *
 * @author ggianna
 */
public class summarySingleFileHierEvaluator {
    int MinN, Levels;
    double DistFactor;
    boolean AvoidSelfComparison;
    ArrayList<GraphIndex> GraphIndices;

    public summarySingleFileHierEvaluator(int MinN, double dDistFactor, int Levels,
            boolean AvoidSelfComparison) {
        this.MinN = MinN;
        this.DistFactor = dDistFactor;
        this.AvoidSelfComparison = AvoidSelfComparison;
        this.Levels = Levels;

        // Init Graph indices' struct
        this.GraphIndices = new ArrayList<GraphIndex>(Levels);
        for (int iLevelCnt = 0; iLevelCnt < Levels; iLevelCnt++)
            this.GraphIndices.add(new GraphIndex());
    }

    /** Performs comparison between a (summary) text file and a set of model (summary)
     * text files. The comparison result is the average similarity of the given
     * text to the individuals of the text set.
     * @param sSummaryTextFile The filename of the text file to use.
     * @param ssModelFiles A set of strings, containing the filenames of the model
     *  texts.
     * @return A double value indicating the average <b>value</b> similarity
     * between the given text and the model texts.
     */
    public double doCompare(String sSummaryTextFile, Set<String> ssModelFiles) {
        // Init return struct
        Distribution dRes = new Distribution(); // Distro of results

        NGramSymWinHierDocument ndNDoc1 = new NGramSymWinHierDocument(MinN, 
                Levels, DistFactor, GraphIndices);

        // Read first file
        ndNDoc1.loadDataStringFromFile(sSummaryTextFile);

        File fSummaryFile = new File(sSummaryTextFile);

        // Init Comparator Class
        Iterator<String> iOtherIter = ssModelFiles.iterator();
        while (iOtherIter.hasNext()) {
            String sModelFile = iOtherIter.next();
            // Skip self-comparison if asked
            if (new File(sModelFile).getName().equals(fSummaryFile.getName()) &&
                    AvoidSelfComparison)
            {
                System.err.print(String.format("Skipping '%s' to '%s' comparison",
                        sModelFile, fSummaryFile));
                continue;
            }
            // Load model data
            // Init document class
            NGramSymWinHierDocument ndNDoc2 = new NGramSymWinHierDocument(MinN, 
                    Levels, DistFactor, GraphIndices);
            ndNDoc2.loadDataStringFromFile(sModelFile);

            // Save and Output results
            GraphSimilarity sSimil = null;
            // Get simple text similarities
            sSimil = ndNDoc1.compareTo(ndNDoc2);
            dRes.increaseValue(sSimil.ValueSimilarity, 1.0);

        }

        return dRes.average(false);
    }

    /** Performs comparison between the graph representation of a (summary)
     * text file and a set of (model summary) text files.
     * The comparison result is the similarity of the given
     * text to the union of the representation of the texts in the text set.
     * <b>NOT IMPLEMENTED</b>
     * @param sSummaryTextFile The filename of the text file to use.
     * @param ssModelFiles A set of strings, containing the filenames of the model
     *  texts.
     * @return A double value indicating the <b>normalized value</b> similarity
     * between the given text representation and the model texts set representation.
     */
    public double doGraphCompareToSet(String sSummaryTextFile,
            Set<String> ssModelFiles, String sGraphModelClassName,
            String sComparatorClassName, int iMinNGramRank, int iMaxNGramRank,
            int iNGramDist) {
        throw new NotImplementedException();
    }

    /** Main function for usage from the command line.
     * @param args The command line arguments
     */
    public static void main(String[] args) {
       // Parse commandline
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);
        if (utils.getSwitch(hSwitches,"?", "").length() > 0) {
            System.exit(0);
        }

        // Vars
        int NMin, Levels;
        double DistFactor;
        String SummaryFile, ModelDir;
        boolean Silent, Merge, bAvoidSelfComparison;

        try {
            NMin = Integer.valueOf(utils.getSwitch(hSwitches,"nMin", "4"));
            Levels = Integer.valueOf(utils.getSwitch(hSwitches,"levels", "5"));
            DistFactor = Double.valueOf(utils.getSwitch(hSwitches,"distFactor", "2"));
            // Get summary and model dir
            SummaryFile = utils.getSwitch(hSwitches, "summary", "summary.txt");
            ModelDir = utils.getSwitch(hSwitches, "modelDir", "models" +
                    System.getProperty("file.separator"));
            // Determine if silent
            Silent=utils.getSwitch(hSwitches, "s", "FALSE").equals("TRUE");
            bAvoidSelfComparison = utils.getSwitch(hSwitches,
                    "avoidSelfComparison", "FALSE").equals("TRUE");

            if (!Silent)
                System.err.println("Using parameters:\n" + hSwitches);

        }
        catch (ClassCastException cce) {
            System.err.println("Malformed switch:" + cce.getMessage() + ". Aborting...");
            return;
        }
        summarySingleFileHierEvaluator ssfeEval = new
                summarySingleFileHierEvaluator(NMin, DistFactor, Levels,
                bAvoidSelfComparison);
        DocumentSet dsModels = new DocumentSet(ModelDir, 1.0);
        dsModels.createSets(true);

        double dRes = Double.NaN;
        Set<String> ssModels = dsModels.toFilenameSet(DocumentSet.FROM_WHOLE_SET);
        dRes = ssfeEval.doCompare(SummaryFile, ssModels);

        System.out.println(String.format("%12.10f", dRes));
    }
    

}
