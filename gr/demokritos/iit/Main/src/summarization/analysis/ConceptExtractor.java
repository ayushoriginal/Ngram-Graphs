/*
 * Under LGPL licence.
 */

package gr.demokritos.iit.summarization.analysis;

import gr.demokritos.iit.conceptualIndex.LocalWordNetMeaningExtractor;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import java.util.ArrayList;
import java.util.List;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.jinsect.IMatching;
import java.util.HashSet;
import java.util.Iterator;
import salvo.jesus.graph.Vertex;
import salvo.jesus.graph.VertexImpl;
import gr.demokritos.iit.conceptualIndex.documentModel.SemanticIndex;
import gr.demokritos.iit.jinsect.events.Notifier;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.WordDefinition;
import gr.demokritos.iit.conceptualIndex.documentModel.SymbolicGraph;
import gr.demokritos.iit.conceptualIndex.documentModel.comparators.DefaultDefinitionComparator;
import gr.demokritos.iit.conceptualIndex.documentModel.comparators.SingleMeaningDefComparator;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;

/** This class breaks down a text into substrings and attempt to annotate the
 * given text with a set of concepts.
 *
 * @author pckid
 */
public class ConceptExtractor implements IMatching<String>, Notifier {
    protected SemanticIndex Index;
    protected NotificationListener Listener;
            
    public ConceptExtractor(SemanticIndex siIndex) {
        Index = siIndex;
    }
    
    public List<WordDefinition> extractDefinitions(String sText) {
        
        ArrayList<WordDefinition> alRes = new ArrayList<WordDefinition>();
        
        // Analyse chunk in substrings
        List lSubStrings = utils.getSubStrings(sText, sText.length(), this);
        if (lSubStrings.size() == 0)
            return alRes;
        
        if (Listener != null)
            Listener.Notify(this, utils.printList(lSubStrings));
        ArrayList<String> lOptions = new ArrayList<String>();
        lOptions.addAll(lSubStrings);

        Iterator<String> iIter = lOptions.iterator();

        HashSet<String> hSubstringSet = new HashSet<String>();

        // For every substring set
        while (iIter.hasNext()) {
            Object oNext = iIter.next();
            List lNext;
            if (oNext instanceof List) {
                lNext = (List)oNext;
            }
            else 
            {
                lNext = new ArrayList();
                lNext.add(oNext);
            }
            // If substring has been analyzed, ignore.
            if (hSubstringSet.contains(lNext.toString()))
                continue;


            if (Listener != null)
                Listener.Notify(this, "Case " + utils.printList(lNext));
            hSubstringSet.add(lNext.toString());
            // Create vertex list
            List lNodes = new ArrayList();
            Iterator iSubstrings = lNext.iterator();
            while (iSubstrings.hasNext()) {
                lNodes.add(new VertexImpl(iSubstrings.next()));
            }

            // Attempt retrieval of union meanings
            Iterator iNodes = lNodes.iterator();
            String sUnionMeaning = "";
            while (iNodes.hasNext()) {
                String sCur = ((Vertex)iNodes.next()).toString();
                Object oTxt = Index.getMeaning(new VertexImpl(sCur));
                if (oTxt != null) {
                    alRes.add((WordDefinition)oTxt);
                }
                else
                    if (Listener != null)
                        Listener.Notify(this, "No meaning found...");

            }
        }
        
        return alRes;
    }
    
    public List<String> extractConceptDescriptions(String sText) {
        ArrayList<String> alRes = new ArrayList<String>();
        
        // Analyse chunk in substrings
        List lSubStrings = utils.getSubStrings(sText, sText.length(), this);
        if (lSubStrings.size() == 0)
            return alRes;
        
        if (Listener != null)
            Listener.Notify(this, utils.printList(lSubStrings));
        ArrayList<String> lOptions = new ArrayList<String>();
        lOptions.addAll(lSubStrings);

        Iterator<String> iIter = lOptions.iterator();

        HashSet<String> hSubstringSet = new HashSet<String>();

        // For every substring set
        while (iIter.hasNext()) {
            Object oNext = iIter.next();
            List lNext;
            if (oNext instanceof List) {
                lNext = (List)oNext;
            }
            else 
            {
                lNext = new ArrayList();
                lNext.add(oNext);
            }
            // If substring has been analyzed, ignore.
            if (hSubstringSet.contains(lNext.toString()))
                continue;


            if (Listener != null)
                Listener.Notify(this, "Case " + utils.printList(lNext));
            hSubstringSet.add(lNext.toString());
            // Create vertex list
            List lNodes = new ArrayList();
            Iterator iSubstrings = lNext.iterator();
            while (iSubstrings.hasNext()) {
                lNodes.add(new VertexImpl(iSubstrings.next()));
            }

            // Attempt retrieval of union meanings
            Iterator iNodes = lNodes.iterator();
            String sUnionMeaning = "";
            while (iNodes.hasNext()) {
                String sCur = ((Vertex)iNodes.next()).toString();
                Object oTxt = Index.getMeaning(new VertexImpl(sCur));
                if (oTxt != null) {
                    String sConceptTxt = SemanticIndex.meaningToString(oTxt);
                    sUnionMeaning += "-" + sConceptTxt + "-";
                }
                else
                    if (Listener != null)
                        Listener.Notify(this, "No meaning found...");

            }
            alRes.add(sUnionMeaning);

        }
        
        return alRes;
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

    @Override
    /** Returns a match, only if a meaning is found or the string is less than
     * two characters long.
     */
    public boolean match(String o1) {
        WordDefinition wd = Index.getMeaning(o1);
        return (wd != null) || (o1.length() < 2);
    }
    
    public static void main(String[] args) {
        // Init db
        INSECTFileDB<SymbolicGraph> db = new INSECTFileDB<SymbolicGraph>("tmp", "." + System.getProperty("file.separator"));
        SymbolicGraph sgOverallGraph = null;
        if (db.existsObject("OverallGraph", "sg"))
            System.err.println("Loading existing overall graph...");
            sgOverallGraph = db.loadObject("OverallGraph", "sg");
        
        if (sgOverallGraph == null) {
            System.err.println("Calculating overall graph...");
            sgOverallGraph = new SymbolicGraph(1, 7); // Init graph with a min of 2

            DocumentSet ds = new DocumentSet(
                    "/home/ggianna/Documents/JApplications/JInsect/data/DUC2006Minimal/", 
                    1.0);
            ds.createSets();
            String sText = utils.loadFileSetToString(ds.toFilenameSet(DocumentSet.FROM_WHOLE_SET));
            sgOverallGraph.setDataString(sText);
            // Save graph
            db.saveObject(sgOverallGraph, "OverallGraph", "sg");
        }
        
        SemanticIndex siIndex = null;
            
        siIndex = new SemanticIndex(sgOverallGraph);
        try {
            siIndex.MeaningExtractor = new LocalWordNetMeaningExtractor();
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            siIndex.MeaningExtractor = null;
        }
        ConceptExtractor ce = new ConceptExtractor(siIndex);
        String sToExtract = "not decide autonomously";
        System.out.println("**" + sToExtract + "**\n" + 
                utils.printIterable(ce.extractConceptDescriptions(sToExtract), "\n"));
        List<WordDefinition> l1 = ce.extractDefinitions(sToExtract);
        sToExtract = "take a free decision";
        System.out.println("**" + sToExtract + "**\n" + 
                utils.printIterable(ce.extractConceptDescriptions(sToExtract), "\n"));
        List<WordDefinition> l2 = ce.extractDefinitions(sToExtract);
        
        DefaultDefinitionComparator ddc = new DefaultDefinitionComparator(siIndex);
        System.out.println(ddc.CompareDefinitionLists(l1, l2));
        SingleMeaningDefComparator smc = new SingleMeaningDefComparator(siIndex);
        System.out.println(smc.CompareDefinitionLists(l1, l2));
    }
}
