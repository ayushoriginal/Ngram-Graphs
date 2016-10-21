/*
 * ObjectDB.java
 *
 * Created on 24 Ιανουάριος 2006, 10:32 μμ
 *
 */

package gr.demokritos.iit.jinsect.storage;

import java.io.Serializable;

/** This class describes a database interface, that supports categorized 
 * objects.
 *
 * @author PCKid
 */
public abstract class INSECTDB<TObjectType extends Serializable> {
    // Object types
    public static String CATEGORY_TYPE = "Category";
    public static String DOCUMENT_TYPE = "Document";
    
    /** Creates a new instance of ObjectDB */
    public INSECTDB() {
    }    
    
    /** Finalizes db. Descendants should perform finalization operations in this
     * member. */
    public void finalizeDB() {
        
    }
    
    /** Saves a given {@link Serializable} object, with a given name and a given
     * category.
     *@param oObj The object to save.
     *@param sObjectName The name (and unique identifier within the category) of the object.
     *@param sObjectCategory The category of the object.
     */
    public abstract void saveObject(Serializable oObj, String sObjectName, String sObjectCategory);    
    
    /** Loads a given {@link Serializable} object, with a given name and a given
     * category from the database.
     *@param sObjectName The name (and unique identifier within the category) of the object.
     *@param sObjectCategory The category of the saved object.
     *@return The loaded object.
     */
    public abstract TObjectType loadObject(String sObjectName, String sObjectCategory);
    
    /** Deletes a given object from the database.
     *@param sObjectName The name (and unique identifier within the category) of the object.
     *@param sObjectCategory The category of the saved object.
     */
    public abstract void deleteObject(String sObjectName, String sObjectCategory);
    
    /** Checks whether a given object exists in the database.
     *@param sObjectName The name (and unique identifier within the category) of the object.
     *@param sObjectCategory The category of the saved object.
     *@return True if the object exists, otherwise false.
     */
    public boolean existsObject(String sObjectName, String sObjectCategory) {
        return false;
    }
    
    /** Returns an array of object names for a given category. 
     *@param sObjectCategory The required category of the objects.
     *@return An String array, with all the saved objects corresponding to the 
     *given category.
     */
    public String[] getObjectList(String sObjectCategory) {
        return null;
    }
    
    /** Returns a string representation of a given object.
     *@param oObject The object to represent.
     *@return The string representation of the object. 
     */
    public abstract String getObjDataToString(Object oObject);
    /** Converts a string representation to an object equivalent.
     *@param sData The string representation of the object.
     *@return The represented object. 
     */
    public abstract TObjectType getStringToObjData(String sData);
}
