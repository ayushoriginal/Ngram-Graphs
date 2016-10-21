/*
 * Dictionary.java
 *
 * Created on 24 Ιανουάριος 2006, 10:33 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.structs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import gr.demokritos.iit.jinsect.structs.WordDefinition;
import java.io.FileReader;
import java.io.FileNotFoundException;

/**
 *
 * @author PCKid
 */
public class Dictionary implements Serializable {    
    private HashSet WordDefs;
    public String Name;
    private int MaxWordSize;
    public boolean AddSubStrings = true;
    public boolean RemoveSubStrings = false;
    
    /**
     * Creates a new instance of Dictionary
     */
    public Dictionary(String sName, int iMaxWordSize) {
        WordDefs = new HashSet();
        Name = sName;
        MaxWordSize = iMaxWordSize;
    }
    
    public int length() {
        return this.WordDefs.size();
    }

    public int size() {
        return length();
    }

    /***
     * Adds a word, creating the corresponding definition, to the dictionary. Also adds substrings. 
     * @param sWord The word to add
     */
    public void addWord(String sWord) {
        
        // Add word
        if (!AddSubStrings)
        {
            WordDefinition wd = new WordDefinition(sWord);
            WordDefs.add(wd);
            return;
        }
        
        // Add word and substrings
        int iLen = sWord.length();
        
        // Set current n-gram size
        for (int iNGramSize=1; iNGramSize <= iLen; iNGramSize++)
        {
            // Traverse the whole string
            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
            {
                // If not enough letters, break
                if (iLen < iCurStart + iNGramSize)
                    break;
                
                // else add substring to definitions
                String sCurNGram = sWord.substring(iCurStart, iCurStart + iNGramSize);
                if (sCurNGram.length() <= this.MaxWordSize)
                {
                    WordDefinition wdDef = new WordDefinition(sCurNGram);
                    this.WordDefs.add(wdDef);
                }
            }
        }
    }
    
    /***
     * Removes word sWord without removing substrings.
     * @param sWord The word to remove
     ***/    
    public void removeWord(String sWord) {
        removeWord(sWord, RemoveSubStrings);
    }
    
    public void removeWord(String sWord, boolean bRemoveSubStrings) {
        // Remove word and dispose of definition (using brackets)
        {
            WordDefinition wdDef = new WordDefinition(sWord);
            this.WordDefs.remove(wdDef);
        }
        if (!bRemoveSubStrings)
            return;
        
        // Removing substrings
        int iLen = sWord.length();
        // Set current n-gram size
        for (int iNGramSize=1; iNGramSize <= iLen; iNGramSize++)
        {
            // Traverse the whole string
            for (int iCurStart = 0; iCurStart < iLen; iCurStart++)
            {
                // If not enough letters, break
                if (iLen < iCurStart + iNGramSize)
                    break;
                
                // else remove substring from definitions
                String sCurNGram = sWord.substring(iCurStart, iCurStart + iNGramSize);
                if (sCurNGram.length() <= this.MaxWordSize)
                {
                    WordDefinition wdDef = new WordDefinition(sCurNGram);
                    this.WordDefs.remove(wdDef);
                }
            }
        }
    }

    /***
     * Loads a text file with filename sFilename and adds all its words to the definitions
     * @param sFileName The name of the file to open.
     ***/
    public void loadFromFile(String sFileName) throws FileNotFoundException, IOException {
        // Open file
        FileReader frFile = new FileReader(sFileName);
        String sText = "";
        // Read content
        int c = frFile.read();
        while (c  != -1) {
            sText += c;
            c = frFile.read();
        }
        // Actually add text
        addText(sText);
        
        // Close file
        frFile.close();
    }
    
    /**
     * Adds all words of a selected text to the list of definitions.
     *@param sText The input text.
     */
    public void addText(String sText) {
        // Split text        
        String[] sWords = gr.demokritos.iit.jinsect.utils.splitToWords(sText);
        java.util.Iterator iIter = java.util.Arrays.asList(sWords).iterator();
        // Add words
        while (iIter.hasNext())
            addWord((String)iIter.next());
        
    }
    
    // Removes all words in @param sText from the definitions
    public void removeText(String sText) {
        // Split text        
        String[] sWords = gr.demokritos.iit.jinsect.utils.splitToWords(sText);
        java.util.Iterator iIter = java.util.Arrays.asList(sWords).iterator();
        // Remove words
        while (iIter.hasNext())
            removeWord((String)iIter.next());
        
    }
    
    public void addWordDef(WordDefinition wdDef) {
        WordDefs.add(wdDef);
    }

    public void removeWordDef(WordDefinition wdDef) {
        WordDefs.remove(wdDef);
    }
    
    // TODO: Create hashed definitions (perhaps in another class) for use with approximation
    // and Levenshtein distance
    
    public String toString() {
        return Name + " " + WordDefs.toString();
    }
    
    public void clear() {
        WordDefs.clear();
    }
    
    public boolean contains(String sWord) {
        return WordDefs.contains(new WordDefinition(sWord));
    }
}
