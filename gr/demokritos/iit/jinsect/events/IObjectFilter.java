/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.events;

/** A templated interface that applies a filter to a selected object and returns
 * the filtered object.
 *
 * @author pckid
 */
public interface IObjectFilter<TObjectType>{
    /** Applies a filter over a given object and returns the filtered 
     * 
     * @param obj The object to filter.
     * @return The filtered instance of the object.
     */
    TObjectType filter(TObjectType obj);
}
