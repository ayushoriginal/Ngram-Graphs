/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.jinsect.storage;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author panagiotis
 * 
 * Contains the override methods of INSECTFileDB
 * 
 */
public class INSECTFileDBWithDir extends INSECTFileDB{

    public INSECTFileDBWithDir(String sPrefix, String sBaseDir) {
        super(sPrefix, sBaseDir);
    }

    public INSECTFileDBWithDir() {
        super();
    }
    
    public static final String ListCategoryName = "nameList";
   
    /**
     * returns a String table with all names for the category that asked
     * @param sObjectCategory the category name
     * @return a String table with all names 
     */
    
    @Override
    public String[] getObjectList(String sObjectCategory) {
        
            if((super.getObjectList(sObjectCategory).length == 0)||super.getObjectList(sObjectCategory).length == 1) {
                String[] tableList = new String[0];
                return tableList;
            }else{
                ArrayList<String> nlist = (ArrayList<String>)loadObject(sObjectCategory, ListCategoryName);
                String[] tableList = new String [nlist.size()];
        
                return nlist.toArray(tableList);
             
            }
                  
        
       
    }

    
    
    /**
     * save object with a given name
     * @param oObj the save object
     * @param sObjectName the object name 
     * @param sObjectCategory  the category name
     */
    
    @Override
    public void saveObject(Serializable oObj, String sObjectName, String sObjectCategory) {
        
        super.saveObject(oObj, sObjectName, sObjectCategory);
        if (existsObject(sObjectCategory, ListCategoryName)){
            
            ArrayList<String> nlist = (ArrayList<String>)loadObject(sObjectCategory, ListCategoryName); //create a name list that it contains all names of save object
            nlist.add(sObjectName);                                                                     // add name in the name list
        
            super.saveObject(nlist, sObjectCategory, ListCategoryName);                                 // save the name list
            
        }else {
            ArrayList<String>  nlist = new ArrayList<String>();
            nlist.add(sObjectName);
        
            super.saveObject(nlist, sObjectCategory, ListCategoryName);
        }
        
        
    }
    
   
    
    /**
     * deletes the object
     * @param sObjectName the object name
     * @param sObjectCategory  the category name
     */
    @Override
   public void deleteObject(String sObjectName, String sObjectCategory) {
        int index;
        super.deleteObject(sObjectName, sObjectCategory);                       // delete the object
        ArrayList<String> nlist = (ArrayList<String>)loadObject(sObjectCategory, ListCategoryName); // load the name list
        index= nlist.indexOf(sObjectName);                                      //find the index in tha name list
        nlist.remove(index);                                                    // remove name from name list
        super.saveObject(nlist, sObjectCategory, ListCategoryName);             //save new name list
    }
    
    

}
