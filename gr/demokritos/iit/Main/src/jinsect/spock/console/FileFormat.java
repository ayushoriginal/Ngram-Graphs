/*
 * FileFormat.java
 *
 * Created on July 13, 2007, 4:40 PM
 *
 */

package gr.demokritos.iit.jinsect.spock.console;

// import the necessary libraries
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.structs.IDocumentSet;


/**
 * This class deals with the formating of the file that represents the
clusters
 * in the spock challenge. The class provides methods for parsing such files
 * and also creating such files.
 */
public class FileFormat implements IDocumentSet {
   
    /** Number of clusters: in this file format each line is a cluster */
    int numberOfClusters;
   
    /**
     * Hashtable that contains the clusters.
     * Each record has a key=name and value=linkedlist.
     * Each list item is TreeSet.
     * form the cluster.
     */
    Hashtable clusters;
   
    /** The base directory of the file structure.
     */
    String baseDir = ".";
    
    /**
     * Constructor method:
     * Creates a new instance of FileFormat
     */
    public FileFormat() {
        this.numberOfClusters=0;
        this.clusters=new Hashtable();
    }
   
    /**
     * Parse the file specified by the fileFormat string
     * and store the necessary info to the data structures
     * of this class.
     * @param fileName the file to parse
     */
    public void parseFile(String fileName) {
        System.out.println("Parsing file "+fileName+"...");
        File fTmp = new File(fileName);
        if (fTmp.isFile())            
            baseDir = fTmp.getParent();
            
        
        String currentLine;
        try {
            BufferedReader in=new BufferedReader(new FileReader(fileName));
            while ((currentLine=in.readLine())!=null) {
                this.numberOfClusters++;
                String[] splitted=currentLine.split("\\s");
                // the 3 last strings are: "#", "name", "surname"
                String key=splitted[splitted.length-2]+" "+splitted[splitted.length-1];
                // create the set of docs that form the cluster
                TreeSet docSet=new TreeSet();
                for (int i=0; i<splitted.length-3; i++)
                    docSet.add(splitted[i]);
                // add the cluster to the correct entry in the Hashtable
                if (this.clusters.containsKey(key)) {
                    LinkedList
currentList=(LinkedList)this.clusters.get(key);
                    currentList.add(docSet);
                    this.clusters.put(key, currentList);
                }
                else {
                    // there is no entry yet with this name
                    LinkedList list=new LinkedList();
                    list.add(docSet);
                    this.clusters.put(key, list);
                }
            }  
            // dispose all the resources
            in.close();
            System.out.println("Parsing of file completed.");
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }   
    }
   
    /**
     * Create a file according to the spock-challenge format
     * using the data structure that contains the clusters.
     * @param fileName the file to create
     */
    public void createFile(String fileName) {
        System.out.println("Creating file "+fileName+"...");
        try {
            BufferedWriter out = new BufferedWriter(new
FileWriter(fileName));
            LinkedList currentList;
            TreeSet currentSet;
            Iterator iter=this.clusters.entrySet().iterator();
            while (iter.hasNext()) {
                Entry entry=(Entry)iter.next();
                String name=(String)entry.getKey();
                currentList=(LinkedList)entry.getValue();
                Iterator iList=currentList.iterator();
                while (iList.hasNext()) {
                    currentSet=(TreeSet)iList.next();
                    Iterator iSet=currentSet.iterator();
                    String docCluster="";
                    while (iSet.hasNext()) {
                        docCluster=docCluster+(String)iSet.next()+" ";
                    }
                    // add the name to the string
                    docCluster=docCluster+"# "+name;
                    // write to file
                    out.write(docCluster+"\n");
                }
            }
            // dispose all resources
            out.close();
            System.out.println("File created.");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
   
   
    // Get methods and set methods
   
    /**
     * Get the number of clusters
     * @return  number of clusters
     */
    public int getNumberOfClusters() {
        return this.numberOfClusters;
    }
   
    /**
     * Get the clusters data structure
     * @return  the Hashtable that contains the clusters
     */
    public Hashtable getClusters() {
        return this.clusters;
    }
   
    /**
     * Set the number of clusters
     * @param numOfClusters the number of clusters
     */
    public void setNumberOfClusters(int numOfClusters) {
        this.numberOfClusters=numOfClusters;
    }
   
    /**
     * Set the cluster data structure
     * @param clusterHash the Hashtable that contains the clusters
     */
    public void setClusters(Hashtable clusterHash) {
        this.clusters=new Hashtable(clusterHash);
    }

    public List getCategories() {
        ArrayList lCats = new ArrayList(this.clusters.keySet());
        return lCats;
    }

    public void createSets() {
        // Ignore
    }

    public ArrayList getFilesFromCategory(String sCategoryName) {
        ArrayList alRes = new ArrayList();
        String sCurName = sCategoryName;
        String sCurFileName;

        Iterator<TreeSet> iClusters=((LinkedList)clusters.get(sCurName)).iterator();
        while (iClusters.hasNext()) {
            TreeSet tsCur = iClusters.next();

            Iterator<String> iFileNames = tsCur.iterator(); 
            while (iFileNames.hasNext()) {

                StringBuffer sb = new StringBuffer();
                sb.append(baseDir);
                sb.append(System.getProperty("file.separator"));
                sCurFileName = iFileNames.next();
                sb.append(sCurFileName.split("[.]")[1] + System.getProperty("file.separator"));
                sb.append(sCurFileName);
                CategorizedFileEntry cfeCur = new CategorizedFileEntry(sb.toString(),
                        sCurName);
                alRes.add(cfeCur);
            }
        }
        
        return alRes;
    }

    public ArrayList getTrainingSet() {
        ArrayList alRes = new ArrayList();
        Iterator<String> iNames = clusters.keySet().iterator();
        while (iNames.hasNext()) {
            String sCurName = iNames.next();
            String sCurFileName;
            
            Iterator<TreeSet> iClusters=((LinkedList)clusters.get(sCurName)).iterator();
            while (iClusters.hasNext()) {
                TreeSet tsCur = iClusters.next();
                
                Iterator<String> iFileNames = tsCur.iterator(); 
                while (iFileNames.hasNext()) {
                    
                    StringBuffer sb = new StringBuffer();
                    sb.append(baseDir);
                    sb.append(System.getProperty("file.separator"));
                    sCurFileName = iFileNames.next();
                    sb.append(sCurFileName.split("[.]")[1] + System.getProperty("file.separator"));
                    sb.append(sCurFileName);
                    CategorizedFileEntry cfeCur = new CategorizedFileEntry(sb.toString(),
                            sCurName);
                    alRes.add(cfeCur);
                }
            }
        }
        
        return alRes;
    }

    public ArrayList getTestSet() {
        return new ArrayList(); // Empty set returned
    }

}
