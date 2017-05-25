from getSimilarity import *
import threading
import copy

class Operator:
    def __init__(self):
        pass
    
    
class UnaryOperator(Operator):
    def __init__(self):
        pass
    def apply(self,*args):
        l = len(args)
        if(l!=2):
            raise ValueError('Unary operators accept exactly one argument')
    

class Clone(UnaryOperator):
    def apply(self,*a):
        super(Intersect, self).apply(args)
        return copy.deepcopy(a[0])
        
class NaryOperator(Operator):
    def __init__(self):
        pass
    def apply(self,*args):
        pass

class BinaryOperator(NaryOperator):
    self._commutative = False
    self._distributional = False
    def apply(self,*args):
        l = len(args)
        if(l!=2):
            raise ValueError('Binary operators accept exactly two arguments')
        
class Union(BinaryOperator):
    def __init__(self,l1=0.5, l2 = 0.5, commutative=True,distributional=False):
        self._commutative = commutative
        self._distributional = distributional
        self._lf1 = l1
        self._lf2 = l2
    
    def setLF(self,lf1=0.5,lf2=0.5):
        self._lf1 = l1
        self._lf2 = l2
        
    def apply(self,g1,g2,dc=True):
        super(Union, self).apply(args)
        a = g1
        b = g2
        if(self._commutative):
            if(g1.size()<g2.size()):
                a = g2
                b = g1
            
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
            
        gg2 = b.getGraph()
        rg = r.getGraph()
        re = rg.edges()
        
        # reassign weights
        for (u,v,w) in gg2.edges(data=True):
            if((u,v) in re):
                ed = rg.get_edge_data(u, v)
                wp = (self._lf1*ed['weight']+self._lf2*w['weight'])
            else:
                wp = w['weight']
            r.setEdge(u,v,wp)
        return r

class Intersect(BinaryOperator):
    def __init__(self,commutative=True,distributional=False):
        self._commutative = commutative
        self._distributional = distributional
    
    def apply(self,g1,g2,dc=True):
        super(Intersect, self).apply(args)
        a = g1
        b = g2
        if(self._commutative):
            if(g1.size()<g2.size()):
                a = g2
                b = g1
            
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
        gg2 = b.getGraph()
        gg2e = gg2.edges()
        
        rg = r.getGraph()
        re = rg.edges(data=True)        
        # reassign weights
        for (u,v,w) in re:
            if((u,v) in gg2e):
                # upon common reassign weights
                ed = gg2.get_edge_data(u, v)
                r.setEdge(u,v,(ed['weight']+w['weight'])/2.0)
            else:
                # delte the non common
                r.delEdge(u,v)
        return r

class delta(BinaryOperator):
    def apply(self,a,b,dc):
        super(delta, self).apply(args)
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
            
        gg2 = b.getGraph()
        gg2ed = gg2.edges()
        
        rg = r.getGraph()
        re = rg.edges()
        
    
        for (u,v) in re:
            if((u,v) in gg2ed):
                r.delEdge(u,v)
        r.deleteUnreachedNodes()
        return r

class inverse_intersection(BinaryOperator):
    def apply(self,a,b,dc):
        super(inverse_intersection, self).apply(args)
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
            
        gg2 = b.getGraph()
        gg2ed = gg2.edges(data=True)
        
        rg = r.getGraph()
        re = rg.edges()
        
    
        for (u,v,w) in gg2ed:
            if((u,v) in re):
                r.delEdge(u,v)
            else:
                r.setEdge(u,v,w['weight'])
        r.deleteUnreachedNodes()
        return r

class Similarity(BinaryOperator):
    
    def __init__(Similarity_Type):
        switch(Similarity_Type):
            case "SS":
                self._gS = getSimilaritySS()
            case "VS":
                self._gS = getSimilarityVS()
            case "NVS":
                self._gS = getSimilarityNVS()
                
    def apply(self,*args):
        super(Similarity, self).apply(args)
        return self._gS.getSimilarityDouble(args)
    
class Update(NaryOperator):
    def __init__(self,*vargs):
        super(Update,self).__init__(args)
        self._Op = Union()
    
    def apply(self,*args,dc = True):
        # Start from left
        # while I have more items to the right
            # Get next item
            # call op.(leftItem, rightItem)
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        
        nargs = len(args)

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
        else:
            # Apply on the first two
            self._undex += 1
            lF = (self._undex/self._undex+1)
            Op.setLF(lF,1-lF)
            z = self.Op.apply(args[0],args[1],dc)
            q = list(args[2:])
            # non_comm with the same method on the rest
            self.apply(*([z] + q),dc=False)
    
class ParallelNary(NaryOperator):
    def __init__(self,op, nthreads = 0):
        self._Op = op
        q = int(nthreads)
        self._nthreads = q
        if(q<0):
            raise ValueError('Nthreads must be positive!')
        try:
            if(not Op._distributable):
                return LtoRNary(op)
        except AttributeError:
            return LtoRNary(op)
        
    def apply(*args,dc=True):
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
                self._comres = args[0]
                return 
        else:
            l = []
            # arrange tuples
            for count, thing in enumerate(args):
                if((count%2)==1):
                    l.append((prev,thing))
                else:
                    prev = thing
            
            # create a mutable object to pass to threads
            fargs = {}

            lock = threading.Lock()
            threads = []
            # threads on tuples of input

            # positive thrads means a number

            # positive threads mean's run at most n threads
            i=0
            if(self._nthreads>0):
                for (a,b) in l:
                    thr = threading.Thread(target=self._executor, args=(self._Op.apply,a,b,dc,fargs,str(i),lock)
                    threads.append(thr) 
                    i+=1
                    if(i%nthreads==0):
                        # start threads
                        [t.start() for t in threads]
            
                        # wait for threads to finish
                        [t.join() for t in threads]
                        threads = [] 
                # if some threads have not been executed
                # run 'em!
                if(i%nthreads!=0):
                        [t.start() for t in threads]
                        # wait for threads to finish
                        [t.join() for t in threads]
            # infinity threads means no boundary
            elif(self._nthreads==0):
                for (a,b) in l:
                    thr = threading.Thread(target=self._single_executor, args=(self._Op.apply,a,b,dc,fargs,str(i),lock)
                    threads.append(thr) 
                    i+=1
                [t.start() for t in threads]
                # wait for threads to finish
                [t.join() for t in threads]
            

            # if the argument length is odd append the last param
            # not added in touples
            # recursive call for the rest
            if((nargs%2)!=0):
                fargs[str(len(l))]=args[-1]
                self.apply(*(fargs.values()),dc = False)
            else:
                self.apply(*(fargs.values()), dc = False)
        
    def _single_executor(f,a,b,dc,fargs,dst,lock):
        ## calculate operators apply on a,b
        r = f.apply(a,b,dc)
        ## lock
        
        lock.acquire()
        ## add to results
        fargs[dst] = r
        ## unlock
        lock.release()
        
class LtoRNary(NaryOperator):
    def __init__(self,op):
        self._Op = op
    
    def apply(self, *args):
        # Start from left
        # while I have more items to the right
            # Get next item
            # call op.(leftItem, rightItem)
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True

        nargs = len(args)

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
        else:
            # Apply on the first two
            z = self.Op.apply(args[0],args[1],dc)
            q = list(args[2:])
            # non_comm with the same method on the rest
            self.apply(*([z] + q),dc=False)
            