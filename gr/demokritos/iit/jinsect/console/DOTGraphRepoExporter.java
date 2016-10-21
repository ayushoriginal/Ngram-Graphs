/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.jinsect.console;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import salvo.jesus.graph.Vertex;

/** A class that simply uses a INSECTFileDB to retrieve a DocumentNGramGraph
 * and uses its toDot method to create a dot formatted file. It can also use
 * a simple saved DocumentNGramGraph file as input, with the same output.
 *
 * @author pckid
 */
public class DOTGraphRepoExporter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Parse options
        Hashtable hSwitches = utils.parseCommandLineSwitches(args);
        
        DocumentNGramGraph dgCur = null;
        // If a file, load
        String sFilename = utils.getSwitch(hSwitches, "file", "");
        if (sFilename.length() > 0) {
            ObjectInputStream oisIn = null;
            try {
                System.err.print("Loading file...");
                oisIn = new ObjectInputStream(new FileInputStream(sFilename));
                dgCur = (DocumentNGramGraph) oisIn.readObject();
                oisIn.close();
                System.err.print("Done.");
            } catch (IOException ex) {
                Logger.getLogger(DOTGraphRepoExporter.class.getName()).log(
                        Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(DOTGraphRepoExporter.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
            finally {
                try {
                    oisIn.close();
                } catch (IOException ex) {
                    Logger.getLogger(DOTGraphRepoExporter.class.getName()).log(
                            Level.SEVERE, null, ex);
                }
            }
        }
        else 
        // Else use repository
        {
            boolean bOK = hSwitches.size() >= 4;
            String sDir = utils.getSwitch(hSwitches, "dir", "");
            bOK &= (sDir.length() > 0);
            String sPrefix = utils.getSwitch(hSwitches, "prefix", 
                    Boolean.FALSE.toString());
            bOK &= !sPrefix.equalsIgnoreCase(Boolean.FALSE.toString());
            String sObject = utils.getSwitch(hSwitches, "object", 
                    Boolean.FALSE.toString());
            bOK &= !sObject.equalsIgnoreCase(Boolean.FALSE.toString());
            String sObjectCategory = utils.getSwitch(hSwitches, "category", 
                    Boolean.FALSE.toString());
            bOK &= !sObject.equalsIgnoreCase(Boolean.FALSE.toString());
            if (!bOK)
            {
                printSyntax();
                return;
            }
            INSECTFileDB<DocumentNGramGraph> fdRepos = new INSECTFileDB(sPrefix,
                    sDir);
            if (!fdRepos.existsObject(sObject, sObjectCategory)) {
                System.err.println("Object " + sObject + " not found in " +
                        "repository (" + sDir + "," + sPrefix + ") for category" +
                        " " + sObjectCategory);
                return;
            }
            System.err.print("Loading file...");
            dgCur = fdRepos.loadObject(sObject, sObjectCategory);
            System.err.println("Done.");
            if (dgCur == null) {
                System.err.println("Object " + sObject + " could not be loaded " +
                        "from repository (" + sDir + "," + sPrefix + ") for " +
                        "category " + sObjectCategory);
                return;
            }
        }
        
        boolean bStats = Boolean.valueOf(utils.getSwitch(hSwitches, "stats", 
                Boolean.FALSE.toString()));
        if (dgCur != null) {
            
            String sOutFile = utils.getSwitch(hSwitches, "out", 
                    "");
            boolean bOutput = !sOutFile.equalsIgnoreCase("none");
            try {
                PrintStream pOut = null;
                // Take into account the none option
                if ((sOutFile.length() > 0) && bOutput)
                    pOut = new PrintStream(sOutFile);
                else
                    pOut = System.out;
                for (int iLvlCnt=dgCur.getMinSize(); iLvlCnt <= dgCur.getMaxSize();
                    iLvlCnt++) {
                    if (bStats) {
                        String sStats = getStats(dgCur.getGraphLevelByNGramSize(iLvlCnt));
                        System.err.println("Stats for level:" + iLvlCnt + "\n" +
                                sStats);
                    }
                    if (bOutput) {
                        System.err.print("Outputting dot representation for " +
                                "level " + iLvlCnt + "...");
                        String sRes = utils.graphToDot(
                                dgCur.getGraphLevelByNGramSize(iLvlCnt), true);
                        pOut.print(sRes);
                        System.err.println("Done.");
                    }
                }
                if (pOut != System.out)
                    pOut.close();
                System.err.println("Done.");
            } catch (IOException ex) {
                Logger.getLogger(DOTGraphRepoExporter.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }
        
    }

    public static String getStats(UniqueVertexGraph uvGraph) {
        StringBuffer res = new StringBuffer();
        System.err.print("Counting nodes...");
        res.append("Nodes' count: " + uvGraph.getVerticesCount());
        System.err.print("Done.");
        System.err.print("Counting edges...");
        res.append(",Edges' count: " + uvGraph.getEdgesCount());
        System.err.print("Done.");
        System.err.print("Calculating degree...");
        res.append(",Degree: " + uvGraph.getDegree());
        System.err.print("Done.");
        System.err.print("Analysis per degree...");
        res.append("\nAnalysis per degree: \n");
        Distribution<Double> dInstancesPerDegree = new Distribution<Double>();
        for (Vertex vCur : ((Set<Vertex>)uvGraph.getVertexSet())) {
            dInstancesPerDegree.increaseValue(Double.valueOf(uvGraph.getDegree(vCur)), 
                    1.0);
        }
        res.append(dInstancesPerDegree.toString() + "\n");
        res.append("Degree distribution (Mean, SD, Variance): ");
        System.err.print("Done.");
        double dMeanDegree = dInstancesPerDegree.average(false);
        double dSDDegree = dInstancesPerDegree.standardDeviation(false);
        double dVarDegree = Math.sqrt(dSDDegree);
        res.append(dMeanDegree + " , " + dSDDegree + " , " + dVarDegree + "\n");
        
        double dUThreshold = dMeanDegree + 2 * dSDDegree;
        double dDThreshold = dMeanDegree - 2 * dSDDegree;
        for (Vertex vCur : ((Set<Vertex>)uvGraph.getVertexSet())) {
            if (uvGraph.getDegree(vCur) > dUThreshold)
                res.append("Interesting node with degree over μ+2*σ - (Degree " + uvGraph.getDegree(vCur) +
                        "): ***" + vCur.toString() + "***\n");
            if (uvGraph.getDegree(vCur) < dDThreshold)
                res.append("Interesting node with degree under μ-2*σ - (Degree " + uvGraph.getDegree(vCur) +
                        "): ***" + vCur.toString() + "***\n");
        }
        
        System.err.print("Done.");
        
        res.append(dInstancesPerDegree.toString());
        res.append("\n");
        return res.toString();
    }
    
    public static void printSyntax() {
        System.err.println("Syntax:\n" + DOTGraphRepoExporter.class.getName() +
                " ([-dir=reposDir/ -prefix=filePrefix -object=objectName " +
                "-category=objectCategory]|[-file=path/filename]) " +
                "[-out=(path/outputFile|none)] [-stats]");
    }
}
