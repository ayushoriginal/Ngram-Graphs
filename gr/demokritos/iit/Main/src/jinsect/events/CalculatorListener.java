/*
 * CalculatorListener.java
 *
 * Created on 24 Ιανουάριος 2006, 11:42 μμ
 *
 */

package gr.demokritos.iit.jinsect.events;

/** A class representing calculators that act as listeners to calculation request events.
 *
 * @author PCKid
 */
public interface CalculatorListener<SenderType,ParamsType> {
    /** Calculates a value, given the caller and a set of arguments.
     *@param oCaller The caller.
     *@param oCalculationParams The event parameters, as an object.
     *@return The calculated value.
     */
    public double Calculate(SenderType oCaller, ParamsType oCalculationParams);
}
