#!/usr/bin/env python

"""
 * DocumentNGramGraph.java
 *
 * Created on 17/5/2017 16:00
 *
"""

"""
package gr.demokritos.iit.jinsect.documentModel.representations;
import gr.demokritos.iit.jinsect.structs.IMergeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import gr.demokritos.iit.jinsect.events.NormalizerListener;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.structs.EdgeCachedLocator;
import gr.demokritos.iit.jinsect.structs.UniqueVertexGraph;
import gr.demokritos.iit.jinsect.utils;
import gr.demokritos.iit.jinsect.events.TextPreprocessorListener;
import java.util.Arrays;
import java.util.List;
import salvo.jesus.graph.*;
"""


"""
 *  Represents the graph of a document, with vertices n-grams of the document and edges the number
 * of the n-grams' co-occurences within a given window.
 #* 
 * @author ysig
"""

import networkx as nx

class DocumentNGramGraph:
    #n for the n-graph
    __n = 2 
    #consider not having characters but obgect lists
    __Data = []
    __dSize = 0
    __Graph = nx.Graph()
    __ngrams_graph_d = {}
    __size = 0
    __Dwin = 2
    
    Normalizer = None;
    WordEvaluator = None;
    TextPreprocessor = None;
    
    __NGramGraphArray =[];
    __eclLocator = None;
    
    # initialization
    def __init__(self, Data = [], n=2, Dwin=2):
        # data must be "listable"
        self.__Data = list(Data)
        self.__dSize = len(Data)
        self.__Dwin = abs(int(Dwin))
        self.__n = abs(int(n))
        if(not (*self.__Data is [])):
            self.__Graph = buildGraph(Data)
            
    # we will now define @method buildGraph
    # which takes a data input
    # segments ngrams
    # and adds the to graph form
    def buildGraph(self,Data):
        ng = ngram(Data)
        s = len(ng)
        win = __Dwin
        __Graph = nx.Graph()
        ed = ng[0:min(win,s)-1]
        for e in ed:
            addEdgeInc(Data[0],e)
        o = max(-1,s-win-1)
        if(o>=0):
            window = ng[0:win-2]
            i = win-1
            for gram in ng[0:o]:
                window.append(ng[i])
                for w in window:
                    addEdgeInc(gram,w)
                window.pop(ng[0])
                i+=1
            
                
        
        
    def addEdgeInc(self,A,B):
        if (A,B) in __Graph.edges():
            data = __Graph.get_edge_data(A, B, key='edge')
            __Graph.add_edge(A, B, weight=data['weight']+1)
        else:
            __Graph.add_edge(A, B, key='edge', weight=1)

    
    # creates ngram's of window based on @param n
    def ngram(self,Data):
        l = Data[0:(min(n,__dSize)-1)]
        q = []
        q.append(l)
        if(n<__dSize):
            for d in Data[(min(n,__dSize)-1):end]:
                l.pop(l[0])
                l.append(d)
                q.append(l)
        return q
            
    def setN(self,n):
        self.__n=n

    def getMin(self):
        return self.__MinSize

a = DocumentNGramGraph(4)
print a.getMin()