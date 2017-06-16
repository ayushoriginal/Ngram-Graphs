import threading
import copy
import warnings

# a general Operator class
class Operator(object):
    def __init__(self):
        pass
    
# a genera Unary operator class
# raising a value error on appliance
# if number of arguments =/= 1
class UnaryOperator(Operator):
    def __init__(self):
        pass
    def apply(self,*args):
        l = len(args)
        if(l!=1):
            raise ValueError('Unary operators accept exactly one argument')
    
# Clones input to output
class Clone(UnaryOperator):
    def apply(self,*args):
        super(self.__class__, self).apply(args)
        return copy.deepcopy(args[0])

# a general NaryOperator class        
class NaryOperator(Operator):
    def __init__(self):
        pass
    def apply(self,*args):
        pass

# a binary operator class
# defines if it is commutative (a*b)=(b*a)
# or distributional a*(b*c)=(a*b)*c.
class BinaryOperator(NaryOperator):
    _commutative = False
    _distributional = False
    # On appliance binary operator
    # class raises a simple value error
    def apply(self,*args):
        l = len(args)
        if(l!=2):
            raise ValueError('Binary operators accept exactly two arguments!\n'+str(l)+' given.')

# Union implements union operation of ngram-graphs
class Union(BinaryOperator):
    # generaly union is considered commutative and not distributional
    # we add also a learning factor which controls recalculation of new weights
    def __init__(self,lf = 0.5, commutative=True,distributional=False):
        self._commutative = commutative
        self._distributional = distributional
        self._lf = lf

    # a setter function for learning factor
    def setLF(self,lf=0.5):
        self._lf = lf
    
    # apply union to two ngram graphs
    def apply(self,*args,**kwargs):
        # checks if operator is binary
        super(self.__class__, self).apply(*args)
        
        # a checks for a deepcopy argument
        # default operation is deepcopy
        # inorder to not corrupt the original 
        # mutable input arguments
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        
        # extract ngram-graphs from input
        g1,g2 = args
        a = g1
        b = g2
        indexer = [0,1]
        # If graphs operation is commutative
        # replace it's appliance order
        if(self._commutative):
            if(g1.size()<g2.size()):
                a = g2
                b = g1
                indexer = [1,0]
        # applies deepcopy only on argument a
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
            
        
        gg2 = b.getGraph()
        rg = r.getGraph()
        re = rg.edges()
        
        # pseudocode:
        # For graphs G1,G2 where smallGraph = min(G1,G2) & bigGraph = max(G1,G2)
        # bigGraph gets deepcopied to bigGraph'
        # For all (A,B) belongs in smallGraph edges
        #    if (A,B) belongs also to bigGraph edges (deep-copied graph)
        #       replace the weight with value w1*lf+w2*(1-lf) on bigGraph'
        #    else
        #       add edge to bigGraph' with the value it has on small graph
        # return bigGraph'
        for (u,v,w) in gg2.edges(data=True):
            if((u,v) in re):
                ed = rg.get_edge_data(u, v)
                indexed = [ed['weight'],w['weight']]
                wp = (self._lf*indexed[indexer[0]]+(1-self._lf)*indexed[indexer[1]])
            else:
                wp = w['weight']
            r.setEdge(u,v,wp)
        return r

# possibly not optimal
# a class that models intersection operation between two ngram-graphs
class Intersect(BinaryOperator):
    # Intersect is commutative, but not distributional
    def __init__(self,commutative=True,distributional=False):
        self._commutative = commutative
        self._distributional = distributional
    
    # apply intersection between to two ngram graphs
    def apply(self,*args,**kwargs):
        
        # checks if operator is binary
        super(self.__class__, self).apply(*args)
        
        # a checks for a deepcopy argument
        # default operation is deepcopy
        # inorder to not corrupt the original 
        # mutable input arguments
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        
        # extract ngram-graphs from input
        g1,g2 = args
        a = g1
        b = g2
        # If graphs operation is commutative
        # replace it's appliance order
        if(self._commutative):
            if(g1.size()<g2.size()):
                a = g2
                b = g1

        # applies deepcopy only on argument a            
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a

        gg2 = b.getGraph()
        
        rg = r.getGraph()
        re = rg.edges(data=True)        
        
        # pseudocode:
        # For graphs G1,G2 where smallGraph = min(G1,G2) & bigGraph = max(G1,G2)
        # bigGraph gets deepcopied to bigGraph'
        # For all (A,B) belongs in smallGraph edges
        #    if (A,B) belongs also to bigGraph edges (deep-copied graph)
        #       replace the weight with value ((w1+w2)/2) on bigGraph'
        #    else
        #       remove edge from bigGraph' with the value it has on small graph
        # return bigGraph'
        for (u,v,w) in gg2:
            if((u,v) in re):
                # upon common reassign weights
                ed = re.get_edge_data(u, v)
                r.setEdge(u,v,(ed['weight']+w['weight'])/2.0)
            else:
                # delte the non common
                r.delEdge(u,v)
        # deletes unreached nodes (trims graph)
		r.deleteUnreachedNodes()
        return r

# applies a delta operator between two arguments
class delta(BinaryOperator):
    def apply(self,*args,**kwargs):
        
        # checks if operator is binary
        super(self.__class__, self).apply(*args)
        
        # a checks for a deepcopy argument
		# default operation is deepcopy
        # inorder to not corrupt the original 
        # mutable input arguments
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True

        a,b = args
        # applies deepcopy only on argument a
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
            
        gg2 = b.getGraph()
        gg2ed = gg2.edges()
        
        rg = r.getGraph()
        re = rg.edges()
        
        # pseudocode:
        # For graphs G1,G2
        # G1 gets deepcopied to G1'
        # For all (A,B) belongs in G1 edges
        #    if (A,B) belongs also to G2 edges (deep-copied graph)
        #       delete it from G1'
        # return G1'
        for (u,v) in re:
            if((u,v) in gg2ed):
                r.delEdge(u,v)
        # deletes unreached nodes (trims graph)
        r.deleteUnreachedNodes()
        return r

# calculates inverse intersection
# not optimal but maybe stationary optimal
class inverse_intersection(BinaryOperator):
    # calculates apllication inverse_intersection 
    # between two n gram graphs
    def apply(self,*args,**kwargs):
        # checks if operator is binary
        super(self.__class__, self).apply(*args)
        
        # a checks for a deepcopy argument
        # default operation is deepcopy
        # inorder to not corrupt the original 
        # mutable input arguments
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        a,b = args
        
        # applies deepcopy only on argument a
        if (dc):
            r = copy.deepcopy(a)
        else:
            r = a
            
        gg2 = b.getGraph()
        gg2ed = gg2.edges(data=True)
        
        rg = r.getGraph()
        re = rg.edges()
        
        # pseudocode:
        # For graphs G1,G2
        # G1 gets deepcopied to G1'
        # For all (A,B) belongs in G2 edges
        #    if (A,B) belongs also to G1 edges (deep-copied graph)
        #       delete it from G1'
        #    else
        #       add edges to G1'
        # return G1'
        for (u,v,w) in gg2ed:
            if((u,v) in re):
                r.delEdge(u,v)
            else:
                r.setEdge(u,v,w['weight'])
        r.deleteUnreachedNodes()
        return r
# implents "update", which is the correct way 
# similarity wise of applying Union on multiple
# arguments
class Update(NaryOperator):
    def __init__(self,*args):
        super(self.__class__,self).__init__(*args)
        self._Op = Union()
        # undex is used as an index for the current number of arguments
        # that have been updated
        self._undex = 0
    
    def apply(self,*args,**kwargs):
        
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        
        nargs = len(args)
        
        # Start from left
        # while I have more items to the right
            # Get next item
            # call UnionOp.apply.(leftItem, rightItem)
        # apply recursively

		# recursion sink: 0->problem, 1->return.
        if (nargs==0):
            return None
        elif (nargs==1):
            if(dc):
                return copy.deepcopy(args[0])
            else:
                res = args[0]
                # !!! Strange error on return ...
                # if printed here, result is regular
                # returned object is of none type
                return res
        else:
            # Apply on the first two
            self._undex += 1
            # from undex-1 arguments unified this is the 1
            # so the learning factor is assigned relatively
            lF = (self._undex/(self._undex+1))
            # replace the learning factor with the new one
            self._Op.setLF(lF)
            # apply arguments one by one
            z = self._Op.apply(args[0],args[1],dc=dc)
            q = list(args[2:])
            # with the same method on the rest of the elements
            self.apply(*([z] + q),dc=False)

# Implements a way parallel way for 
# applying an Nary operator
class ParallelNary(NaryOperator):
    
    # initializes a parallel Nary
    # operator for operator op
    # and number of threads = 0
    # parallel Nary can correctly
    # be applied when operator
    # is distributable
    def __init__(self,op, nthreads = 0):
        self._Op = op
        q = int(nthreads)
        self._nthreads = q
        if(q<0):
            raise ValueError('Nthreads must be positive!')
        try:
            if(not op._distributable):
                warnings.warn("Given operator is not defined as distributable.\nResult may be false.", UserWarning)
        except AttributeError:
				warnings.warn("Given operator is not defined as distributable.\nResult may be false.", UserWarning)        

    # applies an Nary Parallel operator
    def apply(self,*args,**kwargs):
		
        # a checks for a deepcopy argument
        # default operation is deepcopy
        # inorder to not corrupt the original 
        # mutable input arguments		
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True
        nargs = len(args)
        print nargs

		# recursion sink
        if (nargs==0):
            return None
        elif (nargs==1):
            if(dc):
                return copy.deepcopy(args[0])
            else:
                # !!! Strange error on return ...
                # if printed here, result is regular
                # returned object is of none type
                return args[0]
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
			# for positive threads
            if(self._nthreads>0):
                for (a,b) in l:
                    thr = threading.Thread(target=self._executor, args=(self._Op.apply,a,b,dc,fargs,str(i),lock))
                    threads.append(thr) 
                    i+=1
					#group - start - join every time we reach n threads
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
                    thr = threading.Thread(target=self._single_executor, args=(self._Op.apply,a,b,dc,fargs,str(i),lock))
                    threads.append(thr) 
                    i+=1
                [t.start() for t in threads]
                # wait for threads to finish
                [t.join() for t in threads]
            

            # if the argument length is odd append the last param
            # not added in touples
            # recursive call for the rest
            #
            if((nargs%2)!=0):
                fargs[str(len(l))]=args[-1]
                return self.apply(*(fargs.values()),dc = False)
            else:
                return self.apply(*(fargs.values()), dc = False)
            
            
    # a single executor for parallel Nary
    def _single_executor(self,f,a,b,dc,fargs,dst,lock):
        ## calculate operators apply - given by f - on a,b
        r = f(a,b,dc=dc)
        ## lock
        lock.acquire()
        ## add to results
        fargs[dst] = r
        ## unlock
        lock.release()
        
# Implements an N to R Nary 
# operator (serial execution of
# an "apply" of an other operator)
class LtoRNary(NaryOperator):
    def __init__(self,op):
        self._Op = op
    
    # L to R Nary operator application
	# Pseudo code
    # Start from left
        # while I have more items to the right
            # Get next item
            # call op.(leftItem, rightItem)
    def apply(self, *args,**kwargs):
        
        # a checks for a deepcopy argument
        # default operation is deepcopy
        # inorder to not corrupt the original 
        # mutable input arguments
        if(kwargs.has_key("dc")):
            dc = kwargs["dc"]
        else:
            dc = True

        nargs = len(args)
        # recursion sink
        if (nargs==0):
            return None
        elif (nargs==1):
            if(dc):
                return copy.deepcopy(args[0])
            else:
                # !!! Strange error on return ...
                # if printed here, result is regular
                # returned object is of none type
                return args[0]
        else:
            # Apply on the first two
            z = self._Op.apply(args[0],args[1],dc=dc)
            q = list(args[2:])
            # recursively apply the result to the rest
			# now dc is false
            return self.apply(*([z] + q),dc=False)
            
