/* Created 18/05/2008
 */

package gr.demokritos.iit.jinsect.events;

import java.io.Serializable;

/** A class describing a progress event, with a task description and a 
 * progress indicating number.
 *
 * @author pckid
 */
public class ProgressEvent implements Serializable {
    /** The name of the task under progress.
     */
    public String TaskName;
    /** A number indicative of the progress.
     */
    public double Progress;
    /** The name of the subtask under progress.
     */
    public String SubtaskName;
    
    /** Initializes a progrss evenet.
     * @param sTaskName The name of the task under progress.
     * @param dProgress The current progress.
     */
    public ProgressEvent(String sTaskName, double dProgress) {
        TaskName = sTaskName;
        Progress = dProgress;
        SubtaskName = ""; // Init to  empty
    }
    
    /** Updates the progress of the object.
     * @param dNewProgress The new value for the progress.
     */
    public final ProgressEvent updateProgress(double dNewProgress) {
        Progress = dNewProgress;
        return this;
    }
    
    /** Updates the progress of the object by an increase of one.
     */
    public final ProgressEvent increaseProgress() {
        Progress++;
        return this;
    }
    
    /** Updates the current subtask of the object.
     * @param sSubtaskName The name of the current subtask
     */
    public final ProgressEvent updateSubtask(String sSubtaskName) {
        SubtaskName = sSubtaskName;
        return this;
    }
    
    @Override
    public String toString() {
        return TaskName + " - " + SubtaskName + " (" + String.valueOf(Progress)
                + ")";
    }
}
