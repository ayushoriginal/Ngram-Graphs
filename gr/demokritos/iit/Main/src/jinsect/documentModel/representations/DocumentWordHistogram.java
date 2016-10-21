/*
 * INSECTWordHistogram.java
 *
 * Created on 31 Ιανουάριος 2006, 10:34 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.documentModel.representations;
import gr.demokritos.iit.jinsect.events.TokenGeneratorListener;
import java.util.List;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;

/**
 *
 * @author PCKid
 */
public class DocumentWordHistogram extends DocumentNGramHistogram {
    public TokenGeneratorListener TokenGenerator = null;
    
    /** Creates a new instance of INSECTWordHistogram */
    public DocumentWordHistogram() {
        MinSize = 1;
        MaxSize = 2;
    }
    
    public DocumentWordHistogram(int iMinSize, int iMaxSize) {
        MinSize = iMinSize;
        MaxSize = iMaxSize;
    }
    
    public void createHistogram(){
        String sUsableString = "".concat(DataString);
        ListIterator iTokenIter;
        List lTokenList;
        if (TokenGenerator != null) {
            lTokenList = TokenGenerator.getTokens();
        } else {
            lTokenList = Arrays.asList(gr.demokritos.iit.jinsect.utils.splitToWords(sUsableString));
        }
        
        LinkedList lTokens = new LinkedList();
        lTokens.addAll(lTokenList);
        
        // Remove blanks
        iTokenIter = lTokens.listIterator();
        // Determine
        while (iTokenIter.hasNext()) {
            String sCurToken = (String)iTokenIter.next();
            if (sCurToken.trim().equals(""))
                iTokenIter.remove();
        }
        
        // Set current n-gram size
        for (int iNGramSize=MinSize; iNGramSize <= MaxSize; iNGramSize++) {
            // Set iterator to token list
            iTokenIter = lTokens.listIterator();
            int iLen = lTokens.size();
            // For all tokens
            while (iTokenIter.hasNext()) {
                String sCurNGram = "";
                StringBuffer sWholeNGram = new StringBuffer();
                // For the current token size, add tokens
                int iCurTokenCnt;
                for (iCurTokenCnt=0; (iCurTokenCnt < iNGramSize) && (iTokenIter.hasNext());
                    iCurTokenCnt++) {
                    sWholeNGram.append((String)iTokenIter.next());
                    sWholeNGram.append("_/\\_");
                }
                // Check if we have the correct n-gram size.
                if (iCurTokenCnt < iNGramSize)
                    sCurNGram = "";
                else
                    sCurNGram = sWholeNGram.toString();
                
                // Backtrack to first token
                while (--iCurTokenCnt > 0)
                    if (iTokenIter.hasPrevious())
                        iTokenIter.previous();
                
                // Skip n-gram if shorter than the requested
                if (sCurNGram.length() == 0)
                    break; // Leave n-gram formation for this size of n-gram
                
                
                // Check for evaluator
                if (WordEvaluator != null)
                    // Evaluate
                    if (!WordEvaluator.evaluateWord(sCurNGram))
                        // If it dit not evaluate ignore
                        continue;
                
                if (this.NGramHistogram.containsKey(sCurNGram)) {
                    double dPrev = ((Double)NGramHistogram.get(sCurNGram)).doubleValue();
                    // If it already exists, increase count
                    NGramHistogram.put(sCurNGram, dPrev + 1.0);
                } else
                    // else init
                    NGramHistogram.put(sCurNGram, 1.0);
            }
        }
        
    }
    
    public static void main(String[] args) {
        DocumentWordHistogram dwhTest = new DocumentWordHistogram(3, 5);
        String sText = "This is a small text to indicate what will happen...";
        dwhTest.setDataString(sText);
        System.out.println(gr.demokritos.iit.jinsect.utils.printList(Arrays.asList(
                dwhTest.NGramHistogram.entrySet().toArray())));
    }
}
