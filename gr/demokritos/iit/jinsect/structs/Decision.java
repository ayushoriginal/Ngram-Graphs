/*
 * Decision.java
 *
 * Created on 12 Φεβρουάριος 2006, 12:13 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gr.demokritos.iit.jinsect.structs;

import java.util.Map;
import gr.demokritos.iit.jinsect.documentModel.documentTypes.NGramDocument;

/**
 *
 * @author PCKid
 */
public class Decision {
    public Object Document;
    public Object FinalDecision;
    public double DecisionBelief;
    public Map DecisionEvidence;
    
    /** Creates a new instance of Decision concerning a single document
     *@param dDocument The document concerning the decision.
     *@param oFinalDecision The object indicating the final decision.
     *@param dDecisionBelief A measure of the belief of the decision.
     *@param mDecisionEvidence The evidence supporting the decision.
     **/
    public Decision(Object dDocument, Object oFinalDecision, 
            double dDecisionBelief, Map mDecisionEvidence) {
        Document = dDocument;
        FinalDecision = oFinalDecision;
        DecisionBelief = dDecisionBelief;
        DecisionEvidence = mDecisionEvidence;
    }
    
    public String toString() {
        return String.format("Final Decision:'%s', Belief:'%3.2f%%', Evidence:'%s'",
                FinalDecision.toString(), DecisionBelief * 100, DecisionEvidence.toString());
    }
}
