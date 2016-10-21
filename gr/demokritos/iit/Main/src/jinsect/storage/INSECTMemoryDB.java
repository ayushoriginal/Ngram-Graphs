/*
 * INSECTMemoryDB.java
 *
 * Created on 31 Ιανουάριος 2006, 9:24 μμ
 *
 */

package gr.demokritos.iit.jinsect.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author PCKid
 */
public class INSECTMemoryDB<TObjectType extends Serializable> extends INSECTDB {
    /** The map object used to look up objects. */
    protected HashMap ObjectMap;
    /** Creates a new instance of INSECTMemoryDB */
    public INSECTMemoryDB() {
        ObjectMap = new HashMap();
    }

    protected String getObjectName(String sObjectName, String sObjectCategory) {
        return sObjectName + "." + sObjectCategory;
    }
    
    public void saveObject(Serializable oObj, String sObjectName, String sObjectCategory) {
        ObjectMap.put(getObjectName(sObjectName, sObjectCategory), oObj);
    }
    
    public TObjectType loadObject(String sObjectName, String sObjectCategory) {
        return (TObjectType)ObjectMap.get(getObjectName(sObjectName, sObjectCategory));
    }
    
    public void deleteObject(String sObjectName, String sObjectCategory) {
        if (existsObject(sObjectName, sObjectCategory))
            ObjectMap.remove(getObjectName(sObjectName, sObjectCategory));
    }
    
    @Override
    public boolean existsObject(String sObjectName, String sObjectCategory) {
        return ObjectMap.containsKey(getObjectName(sObjectName, sObjectCategory));
    }
    
    @Override
    public String[] getObjectList(String sObjectCategory) {
        Iterator iIter = ObjectMap.keySet().iterator();
        ArrayList lList = new ArrayList();
        while (iIter.hasNext()) {
            String sFullName = (String)iIter.next();
            lList.add(sFullName.substring(0, sFullName.length() - sObjectCategory.length() - 1));
        }
        // Return actual object names
        String[] aRes = new String[lList.size()];
        if (lList.size() > 0)            
            return (String [])lList.toArray(aRes);
        else
            return aRes;
    }
    
    public String getObjDataToString(Object oObject) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        try {
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(oObject);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null; // Failed
        }
        
        return bos.toString();
    }
    
    public TObjectType getStringToObjData(String sData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream bos = new ObjectOutputStream(baos);
            bos.writeBytes(sData);    
        }
        catch (IOException e) {
            e.printStackTrace();
            return null; // Failed
        }
                
        ByteArrayInputStream bin = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois;
        Object oRes;
        try {
            ois = new ObjectInputStream(bin);
            oRes = ois.readObject();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null; // Failed
        }
        catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            return null; // Class not found
        }
        return (TObjectType)oRes;
    }    
}
