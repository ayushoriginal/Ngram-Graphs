/*
 * ThreadList.java
 *
 * Created on 29 Ιανουάριος 2007, 5:24 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.threading;

import java.util.Iterator;

/** A list of threads for parallel execution.
 * Should probably be replaced by {@link ThreadPoolExecutor}.
 *
 * @author ggianna
 */
public class ThreadList extends ThreadQueue {
    
    /** Initializes a thread list, with a given maximum of threads running at the same time.
     *@param iMaxThreads The maximum number of threads running at the same time.
     */
    public ThreadList(int iMaxThreads) {
        super(iMaxThreads);
    }
    
    /** Initializes a thread list, with the default maximum number of threads 
     * running at the same time.
     */
    public ThreadList() {
        super();
    }
            
    /** Adds a {@link Runnable} object in the list for execution, if possible. There is no first-in,
     *first-out logic, as opposed to {@link ThreadQueue}.
     *@param r The runnable object to execute.
     *@return True if the object was queued for execution, or false if the queue was full.
     */
    @Override
    public boolean addThreadFor(Runnable r) {
        if (qThreads.size() == Max) {
            // Check all threads and dispose of dead threads
            Iterator iAllThreads = super.qThreads.iterator();
            boolean bRemoved=false;
            while (iAllThreads.hasNext()) {
                Thread tCur = (Thread)iAllThreads.next();
                if (!tCur.isAlive())
                {
                    iAllThreads.remove();
                    bRemoved=true;
                }
            }
            
            // Still full - Return false
            if (!bRemoved)
                return false;
        }
        
        // Made space for new thread
        Thread t = new Thread(r);
        qThreads.add(t);
        t.start();
        return true;
    }
    
}
