/*
 * CategorizedFileEntry.java
 *
 * Created on 1 Φεβρουάριος 2006, 2:00 πμ
 *
 */

package gr.demokritos.iit.jinsect.structs;

/** An entry object including information about a file and a given category.
 *
 * @author PCKid
 */
public class CategorizedFileEntry implements Comparable {
    private String Filename;
    private String Category;
    
    /***
     *Creates an entry for a file that belongs to a specified category.
     *@param sFilename The filename of the file
     *@param sCategory The name of the category, where the file belongs
     ***/
    public CategorizedFileEntry(String sFilename, String sCategory)
    {
        Filename = sFilename;
        Category = sCategory;
    }
    
    /***
     *Returns the category of this <code>CategorizedFileEntry</code>.
     *@return The string name of the category, where the file belongs.
     ***/
    public String getCategory() {
        return Category;
    }
    
    /**
     *Returns the file name of this <code>CategorizedFileEntry</code>.
     *@return The string file name this object is referring to.
     */
    public String getFileName() {
        return Filename;
    }
    
    public int compareTo(Object oObj) {
        CategorizedFileEntry cfeOther = (CategorizedFileEntry)oObj;
        return getFileName().compareTo(cfeOther.getFileName());
    }
    
    /** Returns an expressive representation of the entry.
     * 
     * @return A String of the form "Category::Filename".
     */
    public String toString() {
        return Category + "::" + Filename;
    }
}