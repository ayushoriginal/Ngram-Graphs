/*
 * PooledThreadList.java
 *
 * Created on October 17, 2007, 5:28 PM
 *
 */

package gr.demokritos.iit.jinsect.threading;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author ggianna
 */
public class PooledThreadList extends ThreadList
{
    private final int nThreads;
    private final PoolWorker[] threads;
    private final LinkedList queue;
    protected boolean bShouldTerminate = false;

    public PooledThreadList(int nThreads)
    {
        this.nThreads = nThreads;
        queue = new LinkedList();
        threads = new PoolWorker[nThreads];

        for (int i=0; i<nThreads; i++) {
            threads[i] = new PoolWorker(this);
            threads[i].start();
        }
    }

    public boolean addThreadFor(Runnable r) {
        synchronized(queue) {
            if (queue.size() >= 10*nThreads)
                return false;
            queue.addLast(r);
            queue.notify();
        }
        return true;
    }

    public synchronized void waitUntilCompletion() throws InterruptedException {
        while (true) {
            synchronized (queue) {
                if (queue.isEmpty())
                        return;
            }

            Thread.yield();
        }
    }
    
    public void terminateThreads() throws InterruptedException {
        while (true) {
            boolean bAllDown = true;
            synchronized (queue) {
                if (queue.isEmpty()) {
                    bShouldTerminate = true;
                    for (int i=0; i<nThreads; i++) {
                        if (threads[i].isAlive()) {
                            bAllDown = false;
                        }
                    }
                    if (bAllDown) {
                        queue.notifyAll();
                        return;
                    }
                }
            }

            Thread.yield();
        }
        
    }
    
    private class PoolWorker extends Thread {
        PooledThreadList Parent;
        
        public PoolWorker(PooledThreadList pParent) {
            Parent = pParent;
        }
        
        public void run() {
            Runnable r;

            while (true) {
                synchronized(queue) {
                    while (queue.isEmpty()) {
                        try
                        {
                            queue.wait();
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                        if (Parent.bShouldTerminate)
                            return; // End thread
                    }

                    r = (Runnable) queue.removeFirst();
                }

                // If we don't catch RuntimeException, 
                // the pool could leak threads
                try {
                    r.run();
                }
                catch (RuntimeException e) {
                    // You might want to log something here
                }
            }
        }
    }
    
}