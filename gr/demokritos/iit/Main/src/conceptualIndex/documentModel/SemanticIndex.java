/*
 * SemanticIndex.java
 *
 * Created on 29 ?????????????????? 2006, 11:15 ????
 *
 */

package gr.demokritos.iit.conceptualIndex.documentModel;

import gr.demokritos.iit.conceptualIndex.IMeaningExtractor;
import gr.demokritos.iit.conceptualIndex.InternetWordNetMeaningExtractor;
import gr.demokritos.iit.conceptualIndex.LocalWordNetMeaningExtractor;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.events.CalculatorAdapter;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import java.util.logging.Level;
import java.util.logging.Logger;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.events.Notifier;

//import gr.demokritos.iit.jinsect.structs.WordDefinition;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.ArrayOfDefinition;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.Definition;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;

/** Represents an index of semantic information, connected to a {@link SymbolicGraph}. Each symbol
 * of the SymbolicGraph can be assigned a semantics. Connected symbols in the SymbolicGraph,
 * are considered to indicate inheritance of semantics. Therefore, the SemanticIndex takes into
 * account such inheritance and can be used to retrieve the semantics of every symbol in the
 * graph.
 *
 * @author ggianna
 */
public class SemanticIndex implements Notifier {
    /** A graph of the symbols used in the SemanticIndex.
     */
    SymbolicGraph Graph;
    /** A mapping between symbols and semantics.
     */
    HashMap SemanticLink;
    /** A meaning extractor, that is null by default, leading meaning lookups to the 
     * {@link InternetWordNetMeaningExtractor}. If not null, then the assigned meaning extractor
     * is used to get meanings.
     *@see InternetWordNetMeaningExtractor
     *@see LocalWordNetMeaningExtractor
     *@see IMeaningExtractor
     */
    public IMeaningExtractor MeaningExtractor = null;
    
    public NotificationListener Listener = null; 
    
    /** Creates a new instance of SemanticIndex, given a SymbolicGraph .
     *@param sgGraph The graph of symbols to use, in order to initialize the semantic index.
     *@see SymbolicGraph
     */
    public SemanticIndex(SymbolicGraph sgGraph) {
        Graph = sgGraph;
        SemanticLink = new HashMap();
    }
    
    /** Locates and returns the meaning of a string within this graph.
     *@param sString The string to look up.
     *@return The definition of the string as a {@link WordDefinition} object.
     */
    public WordDefinition getMeaning(String sString) {
        return getMeaning(new VertexImpl(sString));
    }
    
    /** Locates and returns the meaning of vertex' label within this graph.
     *@param vNode The vertex, the label of which is looked up.
     *@return The definition of the vertex label as a {@link WordDefinition} object.
     */
    public WordDefinition getMeaning(Vertex vNode) {
        if (SemanticLink.containsKey(vNode.toString()))
            return (WordDefinition)(SemanticLink.get(vNode.toString()));
        
        //DictService_Impl dServe = new DictService_Impl();
        //DictServiceSoap dsServe = dServe.getDictServiceSoap();
        //WordDefinition wd = dsServe.defineInDict("wn", vNode.toString()); // WordNet
        if (MeaningExtractor == null)
        {
            MeaningExtractor = new InternetWordNetMeaningExtractor(); // Init to default
        }
        WordDefinition wd = MeaningExtractor.getMeaning(vNode.toString());

        boolean bMeaningFound = (wd != null);
        if (bMeaningFound)
            bMeaningFound = getDefinitionsSize(wd) > 0;

        // If no meaning
        if (!bMeaningFound)
        {
            wd = null;
            vNode = gr.demokritos.iit.jinsect.utils.locateVertexInGraph(Graph, vNode.toString());
            if (vNode != null)
                while (wd == null)
                {
                    List neighbours = Graph.getAdjacentVertices(vNode);
                    List parents = new ArrayList();

                    Iterator iIter = neighbours.iterator();
                    while (iIter.hasNext()) {
                        Vertex vCandidateParent = (Vertex)iIter.next();
                        // Add parent neighbours to list
                        if (gr.demokritos.iit.jinsect.utils.locateDirectedEdgeInGraph(Graph, vCandidateParent, vNode) != null)
                            parents.add(vCandidateParent);
                    }
                    neighbours = parents; // Replace neighbours by parent list
                    // Inherit definitions into new definition
                    wd = new WordDefinition();
                    wd.setWord(vNode.toString());
                    ArrayOfDefinition aodDefs = new ArrayOfDefinition();
                    List lDefinitions = new ArrayList();

                    boolean bFoundMeaning = false;
                    iIter = neighbours.iterator(); // Reset iterator
                    while (iIter.hasNext()) {
                        WordDefinition wdParent = getMeaning((Vertex)iIter.next());
                        if (wdParent != null)
                            lDefinitions.addAll(wdParent.getDefinitions().getDefinition());
                    }

                    aodDefs.getDefinition().addAll(lDefinitions);
                    wd.setDefinitions(aodDefs);
                }
        }

        if (wd != null)
            SemanticLink.put(vNode.toString(), wd);

        return wd;        
    }
    
    /**
     * TODO: Implement 
     * Determines the similarity between two nodes of this graph.
     * 
     * @param vNode1 The first node used to perform the comparison.
     * @param vNode2 The second node used to perform the comparison.
     * @return The similarity of the two nodes as a {@linkGraphSimilarityy} object.
     * @see GraphSimilarity
     */
    public GraphSimilarity compareMeaningsOf(Vertex vNode1, Vertex vNode2) {
        GraphSimilarity isRes = new GraphSimilarity();
        
        // TODO: Implement
        return isRes;
    }
    
    /** Computes the similarity between two word definitions, represented as 
     * {@link WordDefinition} objects.
     *@param wd1 The first word definition.
     *@param wd2 The second word definition.
     *@return The degree of similarity between <code>wd1</code>,<code>wd2</code> as a double. 
     * A value of 1.0 indicates complete match (that is to say the definitions are equivalent,
     * while a value of 0.0 indicates no match.
     */
    public static double compareWordDefinitions(WordDefinition wd1, WordDefinition wd2) {
        Definition d;
        
        Iterator iIter = Arrays.asList(wd1.getDefinitions().getDefinition()).iterator();
        double dRes = 0.0;
        while (iIter.hasNext())
        {
            d = (Definition)iIter.next();
            Iterator iIter2 = Arrays.asList(wd2.getDefinitions().getDefinition()).iterator();
            while (iIter2.hasNext())
            {
                Definition d2 = (Definition)iIter2.next();
                // DEBUG LINES
                //System.out.println("Comparing:\n" + d.getWordDefinition() + "\nTO\n" + d2.getWordDefinition());
                //if (Listener != null)
                //    Listener.Notify(this, "Comparing:\n" + d.getWord() + "\nTO\n" + d2.getWord());
                //////////////
                if (d.getWordDefinition() == d2.getWordDefinition()) {
                    dRes += 1.0 / (wd1.getDefinitions().getDefinition().size()
                            * wd2.getDefinitions().getDefinition().size());
                }
                else
                {
                    GraphSimilarity isRes;
                    DocumentNGramSymWinGraph istd1 = new DocumentNGramSymWinGraph();
                    DocumentNGramSymWinGraph istd2 = new DocumentNGramSymWinGraph();
                    // Use synonyms only
                     istd1.setDataString(d.getWordDefinition().replaceAll(
                            "(\\(.+\\))|(--)", ""));
                    istd2.setDataString(d2.getWordDefinition().replaceAll(
                            "(\\(.+\\))|(--)", ""));
                    // Use all
                    //istd1.setDataString(d.getWordDefinition());
                    //istd2.setDataString(d2.getWordDefinition());
                    NGramCachedGraphComparator sdc = new NGramCachedGraphComparator();
                    try {
                        isRes = sdc.getSimilarityBetween(istd1, istd2);
                        isRes.setCalculator(new CalculatorAdapter() {
                            public double Calculate(Object oCaller, Object oCalculatorParams) {
                                GraphSimilarity is = (GraphSimilarity)oCaller;                                
                                return is.ValueSimilarity / is.SizeSimilarity;
                            }
                        });
                        dRes += isRes.getOverallSimilarity() / 
                                (wd1.getDefinitions().getDefinition().size() *
                                wd2.getDefinitions().getDefinition().size());
                    }
                    catch (Exception e) {
                        // Do nothing
                        e.printStackTrace(System.err);
                    }
                    
                }
//                else
//                    // Word exists in other definition
//                if ((d.getWordDefinition().indexOf("{" + d2.getWord() + "}") > 0)
//                    || (d2.getWordDefinition().indexOf("{" + d.getWord() + "}") > 0))
//                {
//                    dRes += 0.1 / wd1.getDefinitions().getDefinition().length; // Matched definition
//                    break;
//                }
                    
            }            
        }
        return dRes;
    }
    
    /** Converts a word definition to its string representation.
     *@param oWd The word definition object to represent as string.
     *@return A string representation of the word definition object.
     *@see WordDefinition
     */
    public static String meaningToString(Object oWd) {
        String sRes = "";
        if ((oWd == null) || !(oWd instanceof WordDefinition))
            return sRes; // No meaning
        
        WordDefinition wd = (WordDefinition)oWd;
        
        for (Definition dDef: wd.getDefinitions().getDefinition()) {
            sRes += dDef.getWordDefinition()+"\n";
        }
        return sRes;
    }
    
    /** Test function. Compares two words and tests using the comparison function.
     *@param args The command line parameters. They are not used.
     */
    public static void main(String[] args) {
        SymbolicGraph sg = new SymbolicGraph(1, 6);        
        
        String s1 = args.length > 0 ? args[0] : "smart";
        String s2 = args.length > 1 ? args[1] : "stupid";
        String s3 = args.length > 2 ? args[2] : "This is a pretty good day. Nice. It's really beautiful.";
        sg.setDataString(s1 + " " + s2 + " " + s3);
        SemanticIndex si = new SemanticIndex(sg);
        try {
            si.MeaningExtractor = new LocalWordNetMeaningExtractor();
        } catch (IOException ex) {
            Logger.getLogger(SemanticIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        WordDefinition wd1 = si.getMeaning(s1);
        WordDefinition wd2 = si.getMeaning(s2);

        if (wd1 == null) {
            System.err.println("No definition found for:" + s1);
            return;
        }
        if (wd2 == null) {
            System.err.println("No definition found for:" + s2);
            return;
        }
        System.out.println("Result of comparison between '" + s1 + "' and '" + s2 + "':" + 
                SemanticIndex.compareWordDefinitions(wd1, wd2));
        System.out.println(SemanticIndex.meaningToString(si.getMeaning(s3)));
    }

    @Override
    public void setNotificationListener(NotificationListener nlListener) {
        Listener = nlListener;
    }

    @Override
    public void removeNotificationListener() {
        Listener = null;
    }

    @Override
    public NotificationListener getNotificationListener() {
        return Listener;
    }

    private final int getDefinitionsSize(WordDefinition wd) {
        return wd.getDefinitions().getDefinition().size();
    }
}
