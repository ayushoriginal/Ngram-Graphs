/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.storage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author ggianna
 */
public class INSECTFileRawDB<TObjectType extends Serializable> extends INSECTFileDB {

    /** Creates a new instance of INSECTFileDB */
    public INSECTFileRawDB(String sPrefix, String sBaseDir) {
        super(sPrefix, sBaseDir);
    }

    @Override
    public void saveObject(Serializable oObj, String sObjectName, String sObjectCategory) {
        try {
            FileOutputStream fsOut = new FileOutputStream(getFileName(sObjectName, sObjectCategory));

            ObjectOutputStream oOut = new ObjectOutputStream(fsOut);
            oOut.writeObject(oObj);
            // Complete the GZIP file
            fsOut.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public TObjectType loadObject(String sObjectName, String sObjectCategory) {
        FileInputStream fsIn = null;
        ObjectInputStream iIn = null;
        try {
            fsIn = new FileInputStream(getFileName(sObjectName, sObjectCategory));
            iIn = new ObjectInputStream(fsIn);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        Object oRes;
        try {
            oRes = iIn.readObject();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            fsIn.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return (TObjectType)oRes;
    }

}
