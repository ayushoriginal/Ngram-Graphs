/*
 * IDocumentSet.java
 *
 * Created on July 13, 2007, 5:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.structs;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ggianna
 */
public interface IDocumentSet {
    public List getCategories();
    public void createSets();
    public ArrayList getFilesFromCategory(String sCategoryName);
    public ArrayList getTrainingSet();
    public ArrayList getTestSet();
}
