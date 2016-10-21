/*
 * DocumentWordDistroGraph.java
 *
 * Created on 12 Φεβρουάριος 2007, 3:42 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.documentModel.representations;
import java.util.LinkedList;
import java.util.Vector;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.events.TokenGeneratorListener;
import gr.demokritos.iit.jinsect.events.NotificationListener;
import java.util.List;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.HashMap;
/**
 *
 *
 * @author ggianna
 */
public class DocumentWordDistroGraph extends DocumentNGramDistroGraph {
    public TokenGeneratorListener TokenGenerator = null;
    public NotificationListener DeletionNotificationListener = null;
    /**
     * Creates a new instance of DocumentWordDistroGraph
     */
    public DocumentWordDistroGraph() {
        MinSize = 1;
        MaxSize = 2;
        CorrelationWindow = 3;
    }
    
    public DocumentWordDistroGraph(int iMinSize, int iMaxSize, int iCorrelationWindow) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
        CorrelationWindow = iCorrelationWindow;
    }
    
    public void createGraphs() {
        String sUsableString = new StringBuffer().append(DataString).toString();
        
        // Use preprocessor if available
        if (TextPreprocessor != null)
            sUsableString = TextPreprocessor.preprocess(sUsableString);
        //else
            //sUsableString = new String(sUsableString);
        
        List lTokenList;
        ListIterator iTokenIter;
        if (TokenGenerator != null)
            lTokenList =  TokenGenerator.getTokens();
        else
            lTokenList = Arrays.asList(gr.demokritos.iit.jinsect.utils.splitToWords(sUsableString));
        
        LinkedList lTokens = new LinkedList();
        lTokens.addAll(lTokenList);
        
        // Remove blanks
        iTokenIter = lTokens.listIterator();
        while (iTokenIter.hasNext()) {
            String sCurToken = (String)iTokenIter.next();
            if (sCurToken.trim().equals(""))
                iTokenIter.remove();
        }
        
        int iLen = lTokens.size();
        // Create token histogram.
        HashMap hTokenAppearence = new HashMap();
        // 1st pass. Populate histogram.
        ///////////////////////////////
        // For all sizes create corresponding levels
        for (int iNGramSize = MinSize; iNGramSize <= MaxSize; iNGramSize++)
        {
            // If n-gram bigger than text
            if (iLen < iNGramSize)
                // then Ignore
                continue;
            
            // The String has a size of at least [iNGramSize]
            String sCurNGram = "";
            
            iTokenIter = lTokens.listIterator(); 
            while (iTokenIter.hasNext())
            {
                // Clear ngram
                sCurNGram = "";
                
                // If reached end
                if (iLen < iTokenIter.nextIndex() + iNGramSize)
                    // then break
                    break;
                
                // Compose n-gram from words
                int iTokenCnt, iCurStart;
                // Get current start index
                iCurStart = iTokenIter.nextIndex();
                String sCurToken = "";
                for (iTokenCnt = 0; iTokenIter.hasNext() && (iTokenCnt < iNGramSize); iTokenCnt++) {                    
                    // Evaluate token
                    sCurToken = (String)iTokenIter.next();
                    if (WordEvaluator != null)
                        if (!WordEvaluator.evaluateWord(sCurToken))
                        {
                            // and ignore if it does not evaluate
                            iTokenCnt--;
                            // Reached end
                            if (!iTokenIter.hasNext())
                                break;
                            continue;
                        }
                    sCurNGram = new StringBuffer().append(sCurNGram).append(" ").append(sCurToken).toString().trim();
                }
                // Incomplete token group (reached end while building)
                if (iTokenCnt < iNGramSize)
                    break; // Ignore the token group and end procedure.
                
                // Update Histogram
                if (hTokenAppearence.containsKey(sCurNGram))
                    hTokenAppearence.put(sCurNGram, ((Double)hTokenAppearence.get(sCurNGram)).doubleValue() + 1.0);
                else
                    hTokenAppearence.put(sCurNGram, 1.0);

                // Position iteration one position after the previous start
                try {
                    while (iTokenIter.previousIndex() != iCurStart)
                        iTokenIter.previous();
                }
                catch (Exception e) {
                    // Invalid index. Ignore.
                }
            }
        }
        
        // 2nd pass. Create graph.
        ///////////////////////////////
        // For all sizes create corresponding levels
        for (int iNGramSize = MinSize; iNGramSize <= MaxSize; iNGramSize++)
        {
            // If n-gram bigger than text
            if (iLen < iNGramSize)
                // then Ignore
                continue;
            
            Vector PrecedingNeighbours = new Vector();
            UniqueVertexGraph gGraph = getGraphLevelByNGramSize(iNGramSize);
            
            // The String has a size of at least [iNGramSize]
            String sCurNGram = "";
            
            iTokenIter = lTokens.listIterator(); 
            while (iTokenIter.hasNext())
            {
                // Clear ngram
                sCurNGram = "";
                
                // If reached end
                if (iLen < iTokenIter.nextIndex() + iNGramSize)
                    // then break
                    break;
                
                // Compose n-gram from words
                int iTokenCnt, iCurStart;
                // Get current start index
                iCurStart = iTokenIter.nextIndex();
                String sCurToken = "";
                for (iTokenCnt = 0; iTokenCnt < iNGramSize; iTokenCnt++) {                    
                    // Evaluate token
                    sCurToken = (String)iTokenIter.next();
                    if (WordEvaluator != null)
                        if (!WordEvaluator.evaluateWord(sCurToken))
                        {
                            // and ignore if it does not evaluate
                            iTokenCnt--;
                            // Reached end
                            if (!iTokenIter.hasNext())
                                break;
                            continue;
                        }
                    sCurNGram = new StringBuffer().append(sCurNGram).append(" ").append(sCurToken).toString().trim();                    
                    // Reached end
                    if (!iTokenIter.hasNext())
                        break;
                }
                // Incomplete token group (reached end while building)
                if (iTokenCnt < iNGramSize)
                    break; // Ignore the token group and end procedure.
                
                // Position iteration one position after the previous start
                try {
                    while (iTokenIter.previousIndex() != iCurStart)
                        iTokenIter.previous();
                }
                catch (Exception e) {
                    // Invalid index. Ignore.
                }
                
                String[] aFinalNeighbours;
                // Normalize
                if (Normalizer != null)
                    aFinalNeighbours = (String[])Normalizer.normalize(null, PrecedingNeighbours.toArray());
                else
                {
                    aFinalNeighbours = new String[PrecedingNeighbours.size()];
                    PrecedingNeighbours.toArray(aFinalNeighbours);
                }
                createEdgesConnecting(gGraph, sCurNGram, java.util.Arrays.asList(aFinalNeighbours), 
                        hTokenAppearence);
                
                PrecedingNeighbours.add(sCurNGram);
                if (PrecedingNeighbours.size() > CorrelationWindow)
                    PrecedingNeighbours.removeElementAt(0);// Remove first element
            }
            int iNeighboursLen = PrecedingNeighbours.size();
            if ((iNeighboursLen <= CorrelationWindow) && (iNeighboursLen > 0)) {
                createEdgesConnecting(gGraph, sCurNGram, (List)PrecedingNeighbours, 
                        hTokenAppearence);
            }
        }        
    }
    
    
    public void deleteItem(String sItem) {
        super.deleteItem(sItem);
        // Notify listener
        if (DeletionNotificationListener != null)
            DeletionNotificationListener.Notify(this, sItem);
    }
    
}
