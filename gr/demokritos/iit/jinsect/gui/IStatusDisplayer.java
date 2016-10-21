/*
 * IStatusDisplayer.java
 *
 * Created on May 23, 2007, 1:21 PM
 *
 */

package gr.demokritos.iit.jinsect.gui;

/**
 *
 * @author ggianna
 */
public interface IStatusDisplayer {
    /**Should set the status and the value as an indication of progress.
     */
    public void setStatus(final String sText, final double dValue);
    /**Should return the text part of the status.
     */
    public String getStatusText();
    /**Should indicate whether the status should be displayed.
     */
    public void setVisible(boolean bShow);
    /**Should indicate whether the status is displayed or not.
     */
    public boolean getVisible();
}
