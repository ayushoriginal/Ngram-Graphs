/*
 * Main.java
 *
 * Created on 24 Ιανουάριος 2006, 10:30 μμ
 *
 */

package gr.demokritos.iit.jinsect;

import gr.demokritos.iit.jinsect.gui.NGramCorrelationForm;


/**
 *
 * @author PCKid
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	 
        /*MainINSECTForm m = new MainINSECTForm();
        m.addWindowListener(new WindowDefaultAdapter());
        m.setVisible(true);*/
     
        NGramCorrelationForm f = new NGramCorrelationForm();
        f.addWindowListener(new WindowDefaultAdapter());
        f.setVisible(true);
    }
    
}
