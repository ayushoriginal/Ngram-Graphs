package gr.demokritos.iit.ducTools;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
/*
 * extractSubDocs.java
 *
 * Created on 11 Ιανουάριος 2007, 5:29 μμ
 *
 */

/**
 * A class to extract subdocuments from a DUC 2005 XML document.
 *
 * @author ggianna
 */
public class extractSubDocs {
    
    /**
     * @param args the command line arguments. The first argument is the name of the input file.
     * If there is no input file selected, then the file test.txt is assumed to be the input file.
     */
    public static void main(String[] args) {
        String sFile;
        if (args.length == 0)
            sFile = "test.txt";
        else
            sFile = args[0];
        
        //extractSubDocs(sFile);
        splitTexts(sFile);
    }

    /** Splits an input document to its subdocuments. Every input document is supposed to have a form
     * complying with the DUC 2005 XML format. The function creates a file for every subdocument.
     *@param sFile The filename to use as input.
     */
    public static void splitTexts(String sFile) {        
        try {
            FileReader frIn = new FileReader(sFile);
            BufferedReader brIn = new BufferedReader(frIn);
            String sText = "";
            String sLine;
            while ((sLine = brIn.readLine()) != null) {
                sText += sLine;
            }                

            String[] sSplitLine = 
                    sText.split("\\s*#[0-9]+\\s*-{8,}\\s*\\S*\\s*-{8,}\\s*|\\s*-{8,}\\s*\\S*\\s*-{8,}\\s*");
            sText = "";
            for (int iCnt = 0; iCnt < sSplitLine.length; iCnt++) {
                sText =  sSplitLine[iCnt];
                if (sText.length() > 0)  {
                    FileWriter fwOut = new FileWriter(sFile + iCnt);
                    System.out.println("TEXT " + iCnt + ":\n" + sText + "\n\n");
                    fwOut.write(sText);
                    fwOut.close();
                }
            }
            
            frIn.close();
            brIn.close();
        }
        catch (FileNotFoundException fnfE)
        {
            System.err.println(fnfE.getMessage());
            fnfE.printStackTrace();
            System.exit(1);
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
            System.exit(2);
        }
    }

    /** Removes the line tags from the lines of an input file.
     *@param sFile The filename of the input file.
     */
    public static void removeLineTag(String sFile) {
        try {
            FileReader frIn = new FileReader(sFile);
            BufferedReader brIn = new BufferedReader(frIn);
            String sLine;
            while ((sLine = brIn.readLine()) != null) {
                String[] sSplitLine = sLine.split("\\<line\\>|\\</line\\>");
                sLine = "";
                for (int iCnt = 0; iCnt < sSplitLine.length; iCnt++) {
                    sLine +=  sSplitLine[iCnt];
                }
                System.out.println(sLine);
            }
        }
        catch (FileNotFoundException fnfE)
        {
            System.err.println(fnfE.getMessage());
            fnfE.printStackTrace();
            System.exit(1);
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
            System.exit(2);
        }
        
    }
    
}
