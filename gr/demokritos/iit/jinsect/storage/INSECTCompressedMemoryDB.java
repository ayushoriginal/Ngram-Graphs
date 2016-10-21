/* Under the terms of LGPL
 */

package gr.demokritos.iit.jinsect.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** A class that uses memory for storage, while compressing the data of stored
 *  objects to optimize memory use.
 *
 * @author pckid
 */
public class INSECTCompressedMemoryDB<TObjectType extends Serializable> 
        extends INSECTMemoryDB implements Serializable {
    
    @Override
    public void saveObject(Serializable oObj, String sObjectName, String sObjectCategory) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzOut = new GZIPOutputStream(bOut);
            ObjectOutputStream oOut = new ObjectOutputStream(gzOut);
            oOut.writeObject(oObj);
            oOut.flush();
            oOut.close();
            gzOut.flush();
            gzOut.close();
            bOut.close();
        } catch (IOException e) {
            System.err.println("Cannot save object to memory. Reason:");
            e.printStackTrace(System.err);
            return;
        }
        
        ObjectMap.put(getObjectName(sObjectName, sObjectCategory), bOut.toByteArray());
    }
    
    @Override
    public TObjectType loadObject(String sObjectName, String sObjectCategory) {
        ByteArrayInputStream bIn = new ByteArrayInputStream((byte[])ObjectMap.get(
                getObjectName(sObjectName, sObjectCategory)));
        TObjectType tObj = null;
        try {
            GZIPInputStream gzIn = new GZIPInputStream(bIn);
            ObjectInputStream oIn = new ObjectInputStream(gzIn);
            tObj = (TObjectType) oIn.readObject();
            oIn.close();
            gzIn.close();
            bIn.close();
        } catch (IOException iOException) {
            System.err.println("Cannot load object from memory. Reason:");
            iOException.printStackTrace(System.err);
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("Cannot load object from memory. Reason:");
            classNotFoundException.printStackTrace(System.err);
        }
        
        return tObj;        
    }

    @Override
    protected String getObjectName(String sObjectName, String sObjectCategory) {
        return String.valueOf(super.getObjectName(sObjectName, sObjectCategory).hashCode());
    }
        
    private void readObject(java.io.ObjectInputStream in)
          throws IOException, ClassNotFoundException {
          ObjectMap = (HashMap)in.readObject();
    }
    
    private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
        out.writeObject(ObjectMap);
    }    
}
