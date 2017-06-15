#!/usr/bin/env python

"""
 * DocumentNGramGraph.java
 *
 * Created on 17/5/2017 16:00
 *
"""

import networkx as nx
import pygraphviz as pgv
import matplotlib.pyplot as plt
from networkx.drawing.nx_agraph import graphviz_layout

"""
 *  Represents the graph of a document, with vertices n-grams of the document and edges the number
 * of the n-grams' co-occurences within a given window.
 #* 
 * @author ysig
"""

class DocumentNGramGraph:
    #n for the n-graph
    _n = 3 
    #consider not having characters but lists of objects
    _Data = []
    #data size build for reuse of len(data)
    _dSize = 0
    #stores the ngram
    _ngram = []
    #store the ngram graph
    _Graph = nx.Graph()
    #window for graph construction
    _Dwin = 2
    # a printing flag determining if the printing result will be stored on document or
    # be displayed on string
    _GPrintVerbose = True

    # the graph stores it's maximum and minimum weigh
    _maxW = 0
    _minW = float("inf")
    # initialization
    def __init__(self, n=3, Dwin=2, Data = [], GPrintVerbose = True):
        # data must be "listable"
        self._Dwin = abs(int(Dwin))
        self._n = abs(int(n))
        self.setData(Data)
        self._GPrintVerbose = GPrintVerbose
        if(not (self._Data == [])):
            _maxW = 0
            _minW = float("inf")
            self.buildGraph()
            
    # we will now define @method buildGraph
    # which takes a data input
    # segments ngrams
    # and creates ngrams based on a given window
    # !notice: at this developmental stage the weighting method
    # may not be correct
    def buildGraph(self,verbose = False, d=[]):
        # set Data @class_var
        self.setData(d)
        Data = self._Data
        
        # build ngram
        ng = self.build_ngram()
        s = len(ng)
    
        win = self._Dwin
        
        #init graph
        self._Graph = nx.DiGraph()
        
        o = min(self._Dwin,s)
        if(o>=1):
            window = [ng[0]]
            # append the first full window
            # while adding the needed edges
            for gram in ng[1:o]:
                for w in window:
                    self.addEdgeInc(gram,w)
                window.append(gram)
                
            # with full window span till
            # the end.
            for gram in ng[o:]:
                for w in window:
                    self.addEdgeInc(gram,w)
                window.pop(0)
                window.append(gram)
                
            # print graph (optional)
            if verbose:
                self.GraphDraw(self._GPrintVerbose)
        return self._Graph
       
    
    # add's an edge if it's non existent
    # if it is increments it's weight
    # !notice: reiweighting technique may be false 
    # at this developmental stage
    def addEdgeInc(self,a,b,w=1):
        #A = repr(a)#str(a)
        #B = repr(b)#str(b)
        #merging can also be done in other ways
        #add an extra class variable
        A = ''.join(a)
        B = ''.join(b)
        if (A,B) in self._Graph.edges():
            edata = self._Graph.get_edge_data(A, B)
            #print "updating edge between (",A,B,") to weigh",(edata['weight']+1)
            r = weight=edata['weight']+w
        else:
            #print "adding edge between (",A,B,")"
            r = w
        # update/add edge weight
        self.setEdge(A,B,r)
    
    # creates ngram's of window based on @param n
    def build_ngram(self,d = []):
        self.setData(d)
        Data = self._Data
        l = Data[0:min(self._n,self._dSize)]
        q = []
        q.append(l[:])
        if(self._n<self._dSize):
            for d in Data[min(self._n,self._dSize):]:
                l.pop(0)
                l.append(d)
                q.append(l[:])
        self._ngram = q
        return q
     
    # draws a graph using math plot lib
    def GraphDraw(self, verbose = True, lf = True, ns = 1000, wf= True):
        pos = graphviz_layout(self._Graph)
        #pos = sring_layout(self._Graph, scale=1)
        #nx.draw(self._Graph,pos = pos,node_size=ns,with_labels = lf, node_color = 'm')
        nx.draw(self._Graph, pos=graphviz_layout(self._Graph), node_size=ns, cmap=plt.cm.Blues, node_color=range(len(self._Graph)), prog='dot', with_labels = lf)
        if wf:
            weight_labels = nx.get_edge_attributes(self._Graph,'weight')
            nx.draw_networkx_edge_labels(self._Graph,pos = pos,edge_labels = weight_labels)
        if verbose:
            plt.show()
        else:
            #plt.savefig('g.png')
            #or to dot
            nx.drawing.nx_pydot.write_dot(self._Graph,'g.dot')
    
    ## set functions for structure's protected fields

    def setData(self,Data):
        if not(Data == []):
            self._Data = list(Data)
            self._dSize = len(self._Data)
    
    # sets an edges weight
    def setEdge(self,a,b,w=1):
        self._Graph.add_edge(a, b, key='edge', weight=w)
        self._maxW = max(self._maxW,w)
        self._minW = min(self._minW,w)
	
	# deletes
    def delEdge(self,u,v):
        self._Graph.remove_edge(u,v)
    

	# trims the graph by removing unreached nodes
    def deleteUnreachedNodes(self):
        self._Graph.remove_nodes_from(nx.isolates(self._Graph))
        
    def setN(self,n):
        self._n=n

    def setDwin(self,win):
        self._Dwin = win
    
    def size(self):
        return self._Graph.size()
    
    ## get functions for structures protected fields
    def getMin(self):
        return self._MinSize
    
    def getngram(self):
        return self._ngram
    
    def getGraph(self):
        return self._Graph
    
    def maxW(self):
        return self._maxW
    
    def minW(self):
        return self._minW
#test script

#1. construct a 2-gram graph of window_size = 2
#   from the word "abcdef"
