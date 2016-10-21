/*
 * SimilarityArray.java
 *
 * Created on 9 Φεβρουάριος 2007, 5:22 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.structs;

import java.io.OutputStream;
import java.io.Serializable;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;

/** A serializable object containing similarity information.
 *
 * @author ggianna
 */
public class SimilarityArray implements Serializable {
    public GraphSimilarity SimpleTextGraphSimil = null;
    public GraphSimilarity SimpleTextHistoSimil = null;
    public GraphSimilarity SimpleTextOverallSimil = null;
    public GraphSimilarity NGramGraphSimil = null;
    public GraphSimilarity NGramHistoSimil = null;    
    public GraphSimilarity NGramOverallSimil = null;
    
    private void writeObject(java.io.ObjectOutputStream oosOut) throws java.io.IOException {
        GraphSimilarity sSimil;
        
        sSimil = (SimpleTextGraphSimil == null) ? new GraphSimilarity() : SimpleTextGraphSimil;
        oosOut.writeDouble(sSimil.ContainmentSimilarity);
        oosOut.writeDouble(sSimil.ValueSimilarity);
        oosOut.writeDouble(sSimil.SizeSimilarity);
        
        sSimil = (SimpleTextHistoSimil == null) ? new GraphSimilarity() : SimpleTextHistoSimil;
        oosOut.writeDouble(sSimil.ContainmentSimilarity);
        oosOut.writeDouble(sSimil.ValueSimilarity);
        oosOut.writeDouble(sSimil.SizeSimilarity);
        
        sSimil = (SimpleTextOverallSimil == null) ? new GraphSimilarity() : SimpleTextOverallSimil;
        oosOut.writeDouble(sSimil.ContainmentSimilarity);
        oosOut.writeDouble(sSimil.ValueSimilarity);
        oosOut.writeDouble(sSimil.SizeSimilarity);
        
        sSimil = (NGramGraphSimil == null) ? new GraphSimilarity() : NGramGraphSimil;
        oosOut.writeDouble(sSimil.ContainmentSimilarity);
        oosOut.writeDouble(sSimil.ValueSimilarity);
        oosOut.writeDouble(sSimil.SizeSimilarity);
        
        sSimil = (NGramHistoSimil == null) ? new GraphSimilarity() : NGramHistoSimil;
        oosOut.writeDouble(sSimil.ContainmentSimilarity);
        oosOut.writeDouble(sSimil.ValueSimilarity);
        oosOut.writeDouble(sSimil.SizeSimilarity);
        
        sSimil = (NGramOverallSimil == null) ? new GraphSimilarity() : NGramOverallSimil;
        oosOut.writeDouble(sSimil.ContainmentSimilarity);
        oosOut.writeDouble(sSimil.ValueSimilarity);
        oosOut.writeDouble(sSimil.SizeSimilarity);
        
    }
    
    private void readObject(java.io.ObjectInputStream ooisIn)  throws java.io.IOException,
        java.lang.ClassNotFoundException {
        SimpleTextGraphSimil = new GraphSimilarity();
        SimpleTextGraphSimil.ContainmentSimilarity = ooisIn.readDouble();
        SimpleTextGraphSimil.ValueSimilarity = ooisIn.readDouble();
        SimpleTextGraphSimil.SizeSimilarity = ooisIn.readDouble();
        
        SimpleTextHistoSimil = new GraphSimilarity();
        SimpleTextHistoSimil.ContainmentSimilarity = ooisIn.readDouble();
        SimpleTextHistoSimil.ValueSimilarity = ooisIn.readDouble();
        SimpleTextHistoSimil.SizeSimilarity = ooisIn.readDouble();
        
        SimpleTextOverallSimil = new GraphSimilarity();
        SimpleTextOverallSimil.ContainmentSimilarity = ooisIn.readDouble();
        SimpleTextOverallSimil.ValueSimilarity = ooisIn.readDouble();
        SimpleTextOverallSimil.SizeSimilarity = ooisIn.readDouble();
        
        NGramGraphSimil = new GraphSimilarity();
        NGramGraphSimil.ContainmentSimilarity = ooisIn.readDouble();
        NGramGraphSimil.ValueSimilarity = ooisIn.readDouble();
        NGramGraphSimil.SizeSimilarity = ooisIn.readDouble();

        NGramHistoSimil = new GraphSimilarity();    
        NGramHistoSimil.ContainmentSimilarity = ooisIn.readDouble();
        NGramHistoSimil.ValueSimilarity = ooisIn.readDouble();
        NGramHistoSimil.SizeSimilarity = ooisIn.readDouble();

        NGramOverallSimil = new GraphSimilarity();        
        NGramOverallSimil.ContainmentSimilarity = ooisIn.readDouble();
        NGramOverallSimil.ValueSimilarity = ooisIn.readDouble();
        NGramOverallSimil.SizeSimilarity = ooisIn.readDouble();

    }
}
