#!/usr/bin/env python

"""
   binaryOperators.py
 
   Created on May 24, 2017, 6:02 PM
"""

import threading
import copy

"""
 * A class of binary operators
 * for n gram graphs
 * still undefined what happens when they dont have
 * the same size
 * @author ysig
"""

class binaryOperators:
    
    def __init__(self):
        pass
    
    def Union(self,*args, **kwargs):
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        nargs = len(args)
        print nargs
        if (nargs==0):
            return None
        elif (nargs==1):
            if(dc):
                return copy.deepcopy(args[0])
            else:
                res = args[0]
                print "Final Graph is:" 
                res.GraphDraw()
                return res
        l = []
        # arrange tuples
        for count, thing in enumerate(args):
            if((count%2)==1):
               l.append((prev,thing))
            else:
               prev = thing
        i=0
        fargs = {}

        lock = threading.Lock()
        threads = []
        # threads on tuples
        for (a,b) in l:
            thr = threading.Thread(target=self._Union2, args=(a,b,fargs,str(i), lock, dc))
            threads.append(thr) 
            i+=1
        
        
        [t.start() for t in threads]
        
        print "Starting Threads:", threads
        
        [t.join() for t in threads]
        
        #print "fargs graph"
        #fargs["0"].GraphDraw()

        
        if((nargs%2)!=0):
            fargs[str(len(l))]=args[-1]
            self.Union(*(fargs.values()),dc = False)
        else:
            q = fargs.values()
            self.Union(*(fargs.values()), dc = False)
        
    def _Union2(self,a,b,results,i,lock,dc):
        #calculate union
        if (dc):
            result = copy.deepcopy(a)
        else:
            result = a
            
        gg2 = b.getGraph()
        rg = result.getGraph()
        er = rg.edges()
        
        # reassign weights
        for (u,v,w) in gg2.edges(data=True):
            if((u,v) in er):
                ed = rg.get_edge_data(u, v)
                r = (ed['weight']+w['weight'])/2.0
            else:
                r = w['weight']
            result.setEdge(u,v,r)
        #lock
        
        lock.acquire()
        #add to results
        results[i] = result
        #unlock
        lock.release()
            