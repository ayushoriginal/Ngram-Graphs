"""
  DocumentNGramSymWinGraph.py
 
  Created on May 23, 2017, 4:56 PM
 
"""

import networkx as nx
import pygraphviz as pgv
import matplotlib.pyplot as plt
from networkx.drawing.nx_agraph import graphviz_layout
from DocumentNGramGraph import DocumentNGramGraph

class DocumentNGramSymWinGraph(DocumentNGramGraph):
    # an extension of DocumentNGramGraph
    # for symmetric windowing    
    
    def buildGraph(self,verbose = False, d=[]):
        
        # set Data @class_variable
        self.setData(d)
        Data = self._Data
        
        # build ngram
        ng = self.build_ngram()
        s = len(ng)
        
        # calculate window
        win = self._Dwin//2
        
        # initialize graph
        self._Graph = nx.Graph()
        
        if(s>=2 and win>=1):
            # max possible window size (bounded by win)
            o = min(win,s)+1
            window = ng[1:o]
            i = o
            # first build the full window
            for gram in ng[0:s-1]:
                for w in window:
                    self.addEdgeInc(gram,w)
                window.pop(0)
                # if window's edge has reached
                # it's the limit of ng stop
                # appending
                if i<s:
                    window.append(ng[i][:])
                    i+=1
            # print Graph (optional)
            if verbose:
                self.GraphDraw(self._GPrintVerbose)
        return self._Graph
        
