"""
  DocumentNGramSymWinGaussGraph.py
 
  Created on May 23, 2017, 5:56 PM
 
"""

import networkx as nx
import pygraphviz as pgv
import matplotlib.pyplot as plt
from networkx.drawing.nx_agraph import graphviz_layout
from DocumentNGramGraph import DocumentNGramGraph
import math

class DocumentNGramGaussNormGraph(DocumentNGramGraph):
    # an extension of DocumentNGramGraph
    # for symmetric windowing    
    _sigma = 1
    _mean = 0
    _a = 1/math.sqrt(2*math.pi)
    _b = 2
    
    def buildGraph(self,verbose = False, d=[]):
        # set Data @class_variable
        self.setData(d)
        Data = self._Data
        
        # build ngram
        ng = self.build_ngram()
        s = len(ng)
        
        # calculate window
        win = (3*self._Dwin)//2
        
        # calculate gaussian params 
        self.set_dsf(self._Dwin//2,0)
        
        # initialize graph
        self._Graph = nx.Graph()
        if(s>=2 and self._Dwin>=1):
            # max possible window size (bounded by win)
            o = min(win,s)+1
            window = ng[1:o]
            i = o
            # first build the full window
            for gram in ng[0:s-1]:
                j = 1
                for w in window:
                    # weigh in the correct way
                    self.addEdgeInc(gram,w,format(self.pdf(j),'.2f'))
                    j+=1
                window.pop(0)
                # if window's edge has reached
                # it's the limit of ng stop
                # appending
                if i<s:
                    window.append(ng[i][:])
                    i+=1
            if verbose:
                self.GraphDraw(self._GPrintVerbose)
        return self._Graph

    # sets mean, sigma to support
    # multiple pdf function calls
    # without the need of recalculations
    def set_dsf(self,sigma=1,mean=0):
        self._sigma = sigma
        self._mean = mean
        self._a = 1.0/(sigma * math.sqrt(2*math.pi))
        self._b = 2.0*(sigma**2)
        print self._a
        print self._b
        
    # calculates given a distance and a mena given inside
    # the 
    def pdf(self,x):
        return self._a*math.exp(-(x*1.0)/self._b)
