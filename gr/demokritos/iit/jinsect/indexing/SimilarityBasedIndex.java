/*
 * SimilarityBasedIndex.java
 *
 * Created on May 5, 2008, 3:04 PM
 *
 */

package gr.demokritos.iit.jinsect.indexing;

import gr.demokritos.iit.jinsect.algorithms.clustering.AverageLinkClusterer;
import gr.demokritos.iit.jinsect.algorithms.clustering.IClusterer;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import gr.demokritos.iit.jinsect.events.ProgressEvent;
import gr.demokritos.iit.jinsect.events.SimilarityComparatorListener;
import gr.demokritos.iit.jinsect.storage.IFileLoader;
import gr.demokritos.iit.jinsect.storage.INSECTDB;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.storage.INSECTMemoryDB;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.structs.ISimilarity;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.tacTools.ACQUAINT2DocumentSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import salvo.jesus.graph.Vertex;

/** A class that describes a hierarchical index, based on similarity. 
 * The index contains a representation for each cluster of 
 * documents, being able to also identify the best cluster for a given new
 * graph.
 * 
 * @author ggianna
 */
public class SimilarityBasedIndex implements Serializable, 
        IIndex<DocumentNGramGraph> {
    protected SimilarityComparatorListener Comparator;
    protected Set<DocumentNGramGraph> NamedObjects;
    protected IClusterer Clusterer;
    protected UniqueVertexGraph Hierarchy;
    //OBSOLETE: protected HashMap<String,DocumentNGramGraph> ClusterToGraph;
    
    /** An {@link INSECTDB} type storage, to hold the representations of the
     * documents.
     */
    protected INSECTDB<DocumentNGramGraph> Storage;
    protected final String CLUSTER_OBJECT_CATEGORY = "ClusterData";
    
    /** A notifier for the progress of various tasks. */
    public NotificationListener Notifier = null;
    
    /** A {@link IFileLoader} variable that can load documents given an 
     * identifier. If null, then the document id is used as a file name and the
     * corresponding file is attempted to be loaded.
     */
    public IFileLoader<String> Loader = null;
    
    // The grammar graph that should be removed from the document graphs to 
    //give the content graph.
    //
    //public DocumentNGramGraph Grammar = null;
    
    private Vertex TopVertex = null;
    
    /** Used as a constructor for loading purposes (serializable interface). */
    private SimilarityBasedIndex() {
    }
    
    /**
     * Creates a new instance of SimilarityBasedIndex, given a set of Graphs, and a 
     * similarity calculator.
     * @param sGraphs The set of {@link NamedDocumentNGramGraph}s to use as training 
     * for the index. Each pair is expected to contain the name of the graph and the graph itself.
     * @param sclComparator If null, a default similarity comparator for graphs is
     * used. Otherwise, the given {@link SimilarityComparatorListener) is used to
     * compare graphs.
     * @param dbStorage If null, then representations are stored in memory. 
     * Otherwise the INSECTDB storage provided is used to store document 
     * representations.
     */
    public SimilarityBasedIndex(Set<NamedDocumentNGramGraph> sNamedObjects, 
            SimilarityComparatorListener sclComparator, 
            INSECTDB<DocumentNGramGraph> dbStorage) {
        // Init variables
        Comparator = sclComparator;
        NamedObjects = new HashSet<DocumentNGramGraph>(sNamedObjects);
        
        // Init storage to new memory storage, if null
        if (dbStorage == null)
            Storage = new INSECTMemoryDB();
        else
            Storage = dbStorage;
        
        // Init storage with single documents
        for (NamedDocumentNGramGraph n : sNamedObjects) {
            Storage.saveObject(n, n.getName(), CLUSTER_OBJECT_CATEGORY);
        }
    }
    
    /** Initializes the comparator object to a default comparator, if null. */
    protected void initComparator() {
        final ProgressEvent peCreation = new ProgressEvent("Comparison", 0.0);
        
        final NGramCachedGraphComparator gcComparator = 
                new NGramCachedGraphComparator();
        
        if (Comparator == null)
            Comparator = new SimilarityComparatorListener() {
                public ProgressEvent event = peCreation;
                
            @Override
                public synchronized ISimilarity getSimilarityBetween(Object oFirst, 
                        Object oSecond) throws InvalidClassException {
                    NamedDocumentNGramGraph pCurDocArg = 
                            (NamedDocumentNGramGraph)oFirst;
                    NamedDocumentNGramGraph pCompareToDocArg = 
                            (NamedDocumentNGramGraph)oSecond;
                    GraphSimilarity sSimil = null;

                    peCreation.updateSubtask("Comparing documents");
                    sSimil = gcComparator.getSimilarityBetween(pCurDocArg,
                            pCompareToDocArg);
                    if (Notifier != null)
                        Notifier.Notify(this, 
                                peCreation.updateProgress(peCreation.Progress + 1.0));
                    /*sSimil.setCalculator(new CalculatorListener<GraphSimilarity, GraphSimilarity>() {
                        public double Calculate(GraphSimilarity oCaller, GraphSimilarity oCalculationParams) {
                            // Return size normalized value similarity
                            return oCalculationParams.ValueSimilarity / oCalculationParams.SizeSimilarity;
                        }
                    });*/
                    return sSimil;
                }
            };
    }
    /** Creates the index, by creating the clusters, and the corresponding 
     * representing graphs for each cluster.
     */
    @Override
    public void createIndex() {
        
        // Init clusterer
        Clusterer = new AverageLinkClusterer();
        // Init comparator
        initComparator();
        
        ProgressEvent peCreation = new ProgressEvent("Index creation", 0.0);
         if (Notifier != null)
             Notifier.Notify(this, peCreation.updateSubtask("Calculating clusters..."));
        // Calculate clusters
        Clusterer.calculateClusters(NamedObjects, Comparator);
        // Get hierarchy
         if (Notifier != null)
             Notifier.Notify(this, peCreation.updateSubtask("Getting hierarchy..."));
        Hierarchy = Clusterer.getHierarchy();        
        
    }
    
    /** Splits a cluster name in the corresponding graph names, by a simple 
     * split using a comma as the delimiter.
     */
    protected Set<String> getDocumentIDsFromCluster(String sClusterLabel) {
        // Split document names, using clusterer document separator.
        Set sRes = new HashSet(Arrays.asList(sClusterLabel.split(
                AverageLinkClusterer.CLUSTER_NAME_SEPARATOR)));
        
        return sRes;
    }
    
    /** Calculates the representing object of a cluster. 
     *@param sClusterLabel The label of the cluster to look up.
     *@return An object representing the cluster.
     */
    protected DocumentNGramGraph getRepresentationFromCluster(String sClusterLabel) {
        // DEBUG LINES
        // System.err.println("Looking up cluster : " + sClusterLabel);
        //////////////
        
        // Check storage
        if (Storage != null)
            if (Storage.existsObject(sClusterLabel, CLUSTER_OBJECT_CATEGORY))
                return Storage.loadObject(sClusterLabel, CLUSTER_OBJECT_CATEGORY);
        
        ProgressEvent peProgress = new ProgressEvent("Representation extraction", 0.0);
        if (Notifier != null)
             Notifier.Notify(this, peProgress.updateSubtask(sClusterLabel).increaseProgress());
        
        // Else, if not calculated yet.
        // Find cluster vertex
        Vertex vCluster = utils.locateVertexInGraph(Hierarchy, sClusterLabel);
        if (vCluster == null) {
            // DEBUG LINES
            System.err.println("FAILED look up for cluster : " + sClusterLabel);
            //////////////
            
            // If not found, return null
            return null;
        }
        
        DocumentNGramGraph gRes = null;
        // Get adjacent vertices
        List<Vertex> lvChildren = utils.getAdjacentIncomingVertices(Hierarchy, vCluster);
        // DEBUG LINES
        // System.err.println("Children count for " + vCluster.toString() + " = " +
                // String.valueOf(lvChildren.size()));
        //////////////
        int iMergeCnt = 0;
        // If children exist
        if (lvChildren.size() > 0) {
            // Get first child graph
            ListIterator<Vertex> liIter = lvChildren.listIterator();
            Vertex vCur = liIter.next();
            gRes = (DocumentNGramGraph) getRepresentationFromCluster(vCur.getLabel()).clone();
            // For every other child
            while (liIter.hasNext()) {
                Vertex vOtherChild = liIter.next();
                // Get corresponding graph
                DocumentNGramGraph gOther = getRepresentationFromCluster(vOtherChild.getLabel());
                // and merge, with diminishing change based on new data
                gRes.merge(gOther, 0.5 * (1.0 - (iMergeCnt / (lvChildren.size()))));
            }
        }
        else // if no children exist
        {
            DocumentNGramGraph dg = null;
            // Check storage for the graph
            if (Storage != null)
                dg = Storage.loadObject(sClusterLabel, CLUSTER_OBJECT_CATEGORY);
            return dg;
        }
        
        // Update name, if appropriate
        if (gRes instanceof NamedDocumentNGramGraph)
            ((NamedDocumentNGramGraph)gRes).setName(sClusterLabel);
        // Update storage
        Storage.saveObject(gRes, sClusterLabel, CLUSTER_OBJECT_CATEGORY);
        // Return graph
        return gRes;
    }
    
    /** Returns the set of documents of the cluster that is most appropriate,
     * given a document graph.
     *@param dngCur The graph of the document used.
     *@return A {@link Set} of strings, corresponding to the document IDs in the
     *cluster that has the most similar content to the given document.
     */
    @Override
    public Set<String> locateSimilarDocuments(DocumentNGramGraph dngCur) {
        String sClusterLabel = null;
        // Init similarity to low value
        double dSim = 0.0;
        double dPrvSim = 0.0;
                        
        // Remove grammar
        //if (Grammar != null)
        //  dgCur = dgCur.allNotIn(Grammar);
        
        // Init current cluster to top
        Vertex vBestCandidate = null;
        Vertex vCur = getRootHierarchyNode(Hierarchy);
        
        // DEBUG LINES
        // Store index path
        LinkedList<String> lPath = new LinkedList<String>();
        lPath.add(vCur.getLabel());
        //////////////
        do {
            dPrvSim = dSim;
            
            // Get similarity of all childen of the current node to given doc
            Iterator iChildren = utils.getAdjacentIncomingVertices(Hierarchy, 
                    vCur).iterator();
            vBestCandidate = vCur; // Best candidate is the current vertex
            
            // If not reached leaf
            if (iChildren.hasNext())
            {                
                // For every child
                while (iChildren.hasNext()) {
                    Vertex vCandidate = (Vertex)iChildren.next();
                    double dCurSim = Double.NEGATIVE_INFINITY;
                    try {
                        // DEBUG LINES
                        // System.out.println("Comparing to..." + vCandidate.getLabel());
                        //////////////
                        
                        // Init comparator if required
                        initComparator();
                        dCurSim = Comparator.getSimilarityBetween(
                                dngCur, 
                                getRepresentationFromCluster(vCandidate.getLabel())).getOverallSimilarity();
                    } catch (InvalidClassException ex) {
                        System.err.println("Invalid document type. Ignoring...");
                        ex.printStackTrace(System.err);
                    }
                    // If candidate is more similar than the parent
                    if (dCurSim > dSim)
                    {
                        // Update best candidate
                        vBestCandidate = vCandidate;
                        // and similarity
                        dSim = dCurSim;
                    }
                }
            }
            
            vCur = vBestCandidate; // Update current position
            sClusterLabel = vBestCandidate.getLabel(); // Update best cluster label
            // DEBUG LINES
            // Add current node to path
            lPath.add(sClusterLabel);
            //////////////
        } while (dPrvSim < dSim);
        
        // DEBUG LINES
        System.err.println(utils.printIterable(lPath, "->\n"));
        //////////////
        return getDocumentIDsFromCluster(sClusterLabel);
    }
    
    /** Get the top node of the cluster hierarchy. Once ran, the node is cached
     *for future reference.
     *@param g The graph, the root node of which is saught.
     *@return The root node vertex, or null if the search fails.
     */
    private final Vertex getRootHierarchyNode(UniqueVertexGraph g) {
        // If calculated return
        if (TopVertex != null)
            return TopVertex;
        
        // Else locate:
        // Init to first (random) vertex. Check if root.
        Vertex vCur = (Vertex)g.getVertexSet().iterator().next();
        List lParents = utils.getOutgoingAdjacentVertices(g, vCur);
        
        // While we have parent list
        while ((lParents != null)) {
            // If the parent list is empty, the root has been found.
            if (lParents.size() == 0)
                return vCur;
            vCur = (Vertex)lParents.get(0); // Get first item
            // Get current parents
            lParents = utils.getOutgoingAdjacentVertices(g, vCur);
        }
        
        // Return last candidate
        return vCur;
    }
    
    
    private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
        // Comparator = (SimilarityComparatorListener)in.readObject();
        
        NamedObjects = (Set<DocumentNGramGraph>)in.readObject();
        Clusterer = new AverageLinkClusterer();
        Hierarchy = (UniqueVertexGraph)in.readObject();
        //ClusterToGraph = (HashMap<String,DocumentNGramGraph>)in.readObject();
        
        //if (Storage instanceof Serializable)
        //    Storage = (INSECTDB<DocumentNGramGraph>)in.readObject();
    }
    
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
        // out.writeObject(Comparator);
        
        out.writeObject(NamedObjects);
        out.writeObject(Hierarchy);
        //out.writeObject(ClusterToGraph);
        
        //if (Storage instanceof Serializable)
        //    out.writeObject(Storage);
    }
    

      
    /** Function testing the functionality of the class. */
    public static void main(String[] args) {
        Hashtable hCmd = utils.parseCommandLineSwitches(args);
        
        // PARAMETERS
        int iFilesPerFold = Integer.valueOf(utils.getSwitch(hCmd, 
                "filesPerFold", "-1")).intValue();
        int FoldCount = Integer.valueOf(utils.getSwitch(hCmd, 
                "foldCount", "-1")).intValue();
        int iStopAtFold = Integer.valueOf(utils.getSwitch(hCmd, 
                "stopAtFold", String.valueOf(Integer.MAX_VALUE))).intValue();
        int iMaxDocs  = Integer.valueOf(utils.getSwitch(hCmd, 
                "maxDocs", String.valueOf(Integer.MAX_VALUE))).intValue();
        boolean bUseExistingIndex = Boolean.valueOf(utils.getSwitch(hCmd, 
                "useExistingIndex", String.valueOf(Boolean.TRUE))).booleanValue();
        boolean bNoGrammar = Boolean.valueOf(utils.getSwitch(hCmd, 
                "noGrammar", String.valueOf(Boolean.FALSE))).booleanValue();
        final String GrammarFileName = utils.getSwitch(hCmd, 
                "grammarFileName", "IndexGrammar.dat");
        final String IndexFileName = utils.getSwitch(hCmd, 
                "indexFileName", "IndexData.dat");
        String sDocStream = utils.getSwitch(hCmd, 
                "docStream", "TAC2008/data/cna_eng/cna_eng_200410");
                
        
        // Init document set
        ACQUAINT2DocumentSet ts = new ACQUAINT2DocumentSet(sDocStream);
        ts.createSets();
        
        // Determine folds
        if (FoldCount == -1)
            FoldCount = ts.getTrainingSet().size() / iFilesPerFold;
        if (FoldCount < 0)
            FoldCount = 10;
        iFilesPerFold = ts.getTrainingSet().size() / FoldCount;
        
        // DEBUG LINES
        System.err.println("Files per fold: " + String.valueOf(iFilesPerFold));
        System.err.println("Folds: " + String.valueOf(FoldCount));
        System.err.println("Total files: " + String.valueOf(ts.getTrainingSet().size()));
        //////////////
        
        DocumentNGramGraph Grammar = null;
        
        // Extract grammar
        if (!bNoGrammar)
            if (new File(GrammarFileName).exists()) {
                System.err.print("Loading grammar...");
                try {
                    // Attempt load
                    FileInputStream fsIn = new FileInputStream(GrammarFileName);
                    GZIPInputStream gsIn = new GZIPInputStream(fsIn);
                    ObjectInputStream osIn = new ObjectInputStream(gsIn);
                    Grammar = (DocumentNGramGraph)osIn.readObject();
                    osIn.close();
                    gsIn.close();
                    fsIn.close();
                }
                catch (Exception e) {
                    System.err.println("Could not load existing grammar. " +
                            "Continuing...");
                    e.printStackTrace(System.err);
                }
                System.err.print("Done.");
                if (Grammar != null)
                System.err.println("Grammar size " + 
                        String.valueOf(Grammar.getGraphLevel(0).getEdgesCount() +
                        Grammar.getGraphLevel(0).getVerticesCount()));
            }
        
        // If loading failed
        if ((Grammar == null) && !bNoGrammar) {
            System.err.println("Extracting grammar...");
            Grammar = new DocumentNGramSymWinGraph(3,3,3);
            DocumentNGramGraph GrammarPrv = null;
            Set<String> sAllGrammarDocs = ts.toFilenameSet(
                    ACQUAINT2DocumentSet.FROM_WHOLE_SET);
            int iFoldSize=sAllGrammarDocs.size() / FoldCount; // Ten-fold
            // Init first document
            DocumentNGramSymWinGraph ngdTmp1 = null;
            for (int iFirstCnt=0; iFirstCnt < FoldCount - 1; iFirstCnt++) {
                int iSecondCnt = iFirstCnt + 1;
                int iCountNum = iFirstCnt + 1;
                if (iStopAtFold == iCountNum) break; // Stop eariler if required

                System.err.print(String.format("Pass %d...", iCountNum));
                List alCurFiles = null;
                if (ngdTmp1 == null) {
                    ngdTmp1 = new DocumentNGramSymWinGraph(3,3,3);
                    alCurFiles = new ArrayList(sAllGrammarDocs).subList(iFirstCnt * iFoldSize, (iFirstCnt * iFoldSize) + (iFoldSize - 1));
                    String sGrammarText = utils.loadFileSetToString(new HashSet(alCurFiles), ts);
                    ngdTmp1.setDataString(sGrammarText);
                }
                System.err.print("Fold 1st data piece size " + 
                    String.valueOf(ngdTmp1.getGraphLevel(0).getEdgesCount()
                     + ngdTmp1.getGraphLevel(0).getVerticesCount()) + ".");


                DocumentNGramSymWinGraph ngdTmp2 = new DocumentNGramSymWinGraph(3,3,3);
                alCurFiles = new ArrayList(sAllGrammarDocs).subList(iSecondCnt * iFoldSize, (iSecondCnt * iFoldSize) + (iFoldSize - 1));
                ngdTmp2.setDataString(utils.loadFileSetToString(new HashSet(alCurFiles), ts));
                System.err.print("Fold 2nd data piece size " + 
                    String.valueOf(ngdTmp2.getGraphLevel(0).getEdgesCount()
                     + ngdTmp2.getGraphLevel(0).getVerticesCount()) + ".");

                System.err.print("Intersecting pieces...");                    
                Grammar = ngdTmp1.intersectGraph(ngdTmp2);

                // Update first document to be the second for the next comparison
                ngdTmp1 = ngdTmp2;
                
                // Combine with previous results
                if (GrammarPrv != null) {
                    System.err.print("Merging with previous grammar...");
                    Grammar = Grammar.intersectGraph(GrammarPrv);
                }
                GrammarPrv = Grammar;

                System.err.println("Done.");
                System.err.println("Grammar size " + 
                    String.valueOf(Grammar.getGraphLevel(0).getEdgesCount()
                     + Grammar.getGraphLevel(0).getVerticesCount()));
            }
            ngdTmp1 = null; // Nullify to free memory
            System.err.println("Extracting grammar...Done.");
            
            // Save Grammar to file
            System.err.print("Saving grammar to file...");
            try {
                FileOutputStream fsOut = new FileOutputStream(GrammarFileName);
                GZIPOutputStream gsOut = new GZIPOutputStream(fsOut);
                ObjectOutputStream osOut = new ObjectOutputStream(gsOut);
                osOut.writeObject(Grammar);
                osOut.close();
                gsOut.close();
                fsOut.close();
            }
            catch (Exception e) {
                System.err.println("Cannot save grammar object. " +
                        "Continuing normally...");
                e.printStackTrace(System.err);
            }
            System.err.println("Done.");
        }
        
        // Init index var
        SimilarityBasedIndex gi = null;
        boolean bIndexLoadedOK = false;
        if (bUseExistingIndex) {
            System.err.print("Loading index from file...");
            try {
                FileInputStream fs = new FileInputStream(IndexFileName);
                GZIPInputStream gs = new GZIPInputStream(fs);
                ObjectInputStream os = new ObjectInputStream(gs);
                gi = (SimilarityBasedIndex)os.readObject();
                os.close();
                gs.close();
                fs.close();
                // Set db
                gi.Storage = new INSECTFileDB("",
                    "statedata/");
                bIndexLoadedOK = true;
            }
            catch (Exception e) {
                System.err.println("Could not load index. Continuing...");
                e.printStackTrace(System.err);
                bIndexLoadedOK = false;
            }
            System.err.println("Done.");
        }
        
        if (!bIndexLoadedOK)
        {
            System.err.println("Extracting document contents...");
            Set<NamedDocumentNGramGraph> sDocGraphs = new HashSet<NamedDocumentNGramGraph>();
            // For all documents
            for (Iterator iDoc=ts.toFilenameSet(ACQUAINT2DocumentSet.FROM_WHOLE_SET).iterator(); 
                iDoc.hasNext();) {
                // Init named graph
                NamedDocumentNGramGraph nCur = new NamedDocumentNGramGraph();
                String sFileID = (String)iDoc.next();

                System.err.print("Extracting content from " + sFileID + ".");
                nCur.setName(sFileID);
                nCur.setDataString(ts.loadFile(sFileID));
                
                if (!bNoGrammar) {
                    System.err.print("Size before reduction " + 
                            String.valueOf(nCur.getGraphLevel(0).getEdgesCount() +
                            nCur.getGraphLevel(0).getVerticesCount()) 
                            + ".");
                    nCur = (NamedDocumentNGramGraph)nCur.allNotIn(Grammar);
                    System.err.print("Size after reduction " + 
                            String.valueOf(nCur.getGraphLevel(0).getEdgesCount() +
                            nCur.getGraphLevel(0).getVerticesCount()) 
                            + ".");
                }

                sDocGraphs.add(nCur);
                System.err.println("Done.");

                // TODO: REMOVE 
                if (--iMaxDocs <= 0) break;
                ////////
            }
            System.err.println("Extracting document contents...Done.");

            // Create index
            SimilarityComparatorListener siml = null;
            gi = new SimilarityBasedIndex(sDocGraphs, siml, new INSECTFileDB("",
                    "statedata/"));
            gi.Notifier = new NotificationListener() {
                int iCnt = 0;
                @Override
                public synchronized void Notify(Object oSender, 
                        Object oParams) {
                    ProgressEvent pe = (ProgressEvent)oParams;
                    if (pe.Progress % 500.0 == 0)
                        System.err.println(pe.toString());
                }
            };

            System.err.print("Creating index...");
            gi.createIndex();
            System.err.println("Done.");
        }
        
        // Save index to file
        if (!bIndexLoadedOK) {
            System.err.print("Saving index to file...");
            try {
                FileOutputStream fsOut = new FileOutputStream(IndexFileName);
                GZIPOutputStream gsOut = new GZIPOutputStream(fsOut);
                ObjectOutputStream osOut = new ObjectOutputStream(gsOut);
                osOut.writeObject(gi);
                osOut.close();
                gsOut.close();
                fsOut.close();
            }
            catch (Exception e) {
                System.err.println("Cannot save index object. " +
                        "Continuing normally...");
                e.printStackTrace(System.err);
            }
            System.err.println("Done.");
        
        }
    
        // DEBUG LINES
        // System.err.println(utils.graphToDot(gi.Hierarchy, true));
        //////////////
        
        ACQUAINT2DocumentSet tsOther = new ACQUAINT2DocumentSet("TAC2008/data/ltw_eng/ltw_eng_200410");
        tsOther.createSets();
        // Set loader to docset handler
        gi.Loader = tsOther;
        
        // Lookup first file
        Object[] oFiles = tsOther.toFilenameSet(ACQUAINT2DocumentSet.FROM_WHOLE_SET).toArray();
        String sName = (String)oFiles[0];
        // Init document graph
        String sFileText;
        sFileText = gi.Loader.loadFile(sName);
        NamedDocumentNGramGraph dgCur = new NamedDocumentNGramGraph();
        dgCur.setDataString(sFileText);
        if (!bNoGrammar)
            dgCur = (NamedDocumentNGramGraph)dgCur.allNotIn(Grammar);
        System.out.println("Index lookup for file " + sName + "...");
        System.out.println("Cluster: " + gi.locateSimilarDocuments(dgCur));
        System.out.println("Index lookup for file " + sName + "...Done.");

        sName = (String)oFiles[oFiles.length - 1];
        sFileText = gi.Loader.loadFile(sName);
        dgCur = new NamedDocumentNGramGraph();
        dgCur.setDataString(sFileText);
        if (!bNoGrammar)
            dgCur = (NamedDocumentNGramGraph)dgCur.allNotIn(Grammar);
        System.out.println("Index lookup for file " + sName + "...");
        System.out.println("Cluster: " + gi.locateSimilarDocuments(dgCur));
        System.out.println("Index lookup for file " + sName + "...Done.");

        // Reset loader to original docset handler
        gi.Loader = ts;
        sName = "CNA_ENG_20041022.0031";
        sFileText = gi.Loader.loadFile(sName);
        dgCur = new NamedDocumentNGramGraph();
        dgCur.setDataString(sFileText);
        if (!bNoGrammar)
            dgCur = (NamedDocumentNGramGraph)dgCur.allNotIn(Grammar);
        System.out.println("Index lookup for file " + sName + "...");
        System.out.println("Cluster: " + gi.locateSimilarDocuments(dgCur));
        System.out.println("Index lookup for file " + sName + "...Done.");

        sName = "CNA_ENG_20041022.0034";
        sFileText = gi.Loader.loadFile(sName);
        dgCur = new NamedDocumentNGramGraph();
        dgCur.setDataString(sFileText);
        if (!bNoGrammar)
            dgCur = (NamedDocumentNGramGraph)dgCur.allNotIn(Grammar);
        System.out.println("Index lookup for file " + sName + "...");
        System.out.println("Cluster: " + gi.locateSimilarDocuments(dgCur));
        System.out.println("Index lookup for file " + sName + "...Done.");
        
    }
}
