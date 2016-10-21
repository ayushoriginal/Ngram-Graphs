/*
 * ThreadQueue.java
 *
 * Created on 29 Ιανουάριος 2007, 1:14 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gr.demokritos.iit.jinsect.threading;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/** A queue of threads for parallel execution.
 * Should probably be replaced by {@link ThreadPoolExecutor}.
 *
 * @author ggianna
 */
public class ThreadQueue {
    /** The queue of threads.
     */
    protected Queue qThreads;
    /** The maximum number of running threads.
     */
    protected int Max = Runtime.getRuntime().availableProcessors();
    
    /** Initializes a thread queue, with the default maximum number of threads 
     * running at the same time.
     */    
    public ThreadQueue() {
        qThreads = new LinkedList();
    }
    
    /** Initializes a thread queue, with a given maximum of threads running at the same time.
     *@param iMax The maximum number of threads running at the same time.
     */
    public ThreadQueue(int iMax) {
        Max = iMax;
        qThreads = new LinkedList();
    }
    
    /** Adds a {@link Runnable} object in the queue for execution, if possible. First-in,
     *first-out logic is followed.
     *@param r The runnable object to execute.
     *@return True if the object was queued for execution, or false if the queue was full.
     */
    public boolean addThreadFor(Runnable r) {
        if (qThreads.size() == Max) {
            // Check head thread
            if (((Thread)qThreads.peek()).isAlive())
                return false;
            else
                qThreads.remove();
        }
        
        Thread t = new Thread(r);
        qThreads.add(t);
        t.start();
        return true;
    }
    
    /** Waits until all running threads have been complete. 
     */
    public void waitUntilCompletion() throws InterruptedException {
        Iterator iIter = qThreads.iterator();
        while (iIter.hasNext()) {
            Thread tCur = (Thread)iIter.next();
            if (tCur.isAlive())
                tCur.join();
        }
    }
}