/*
 * ILoadableTextPrint.java
 *
 * Created on June 14, 2007, 2:24 PM
 *
 */

package gr.demokritos.iit.jinsect.documentModel;

/** This interface describes a TextPrint that can be loaded from a file.
 *
 * @author ggianna
 */
public interface ILoadableTextPrint extends ITextPrint {
    /** Should load the given file into a text print representation. */
    public void loadDataStringFromFile(String sFilename);
}
