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
    __n = 2 
    #consider not having characters but lists of objects
    __Data = []
    #data size build for reuse of len(data)
    __dSize = 0
    #stores the ngram
    __ngram = []
    #store the ngram graph
    __Graph = nx.Graph()
    #window for graph construction
    __Dwin = 2
    # a printing flag determining if the printing result will be stored on document or
    # be displayed on string
    __GPrintVerbose = True

    # initialization
    def __init__(self, n=2, Dwin=2, Data = [], GPrintVerbose = True):
        # data must be "listable"
        self.__Dwin = abs(int(Dwin))
        self.__n = abs(int(n))
        self.setData(Data)
        self.__GPrintVerbose = GPrintVerbose
        if(not (self.__Data == [])):
            self.buildGraph()
            
    # we will now define @method buildGraph
    # which takes a data input
    # segments ngrams
    # and creates ngrams based on a given window
    # !notice: at this developmental stage the weighting method
    # may not be correct
    def buildGraph(self,verbose = False, d=[]):
        self.setData(d)
        Data = self.__Data
        ng = self.build_ngram()
        s = len(ng)
        win = self.__Dwin
        self.__Graph = nx.Graph()
        o = max(-1,s-win-1)
        if(o>=0):
            window = ng[1:win]
            i = win
            for gram in ng[0:o+1]:
                window.append(ng[i][:])
                for w in window:
                    self.addEdgeInc(gram,w)
                window.pop(0)
                i+=1
            if verbose:
                self.GraphDraw(self.__GPrintVerbose)
        return self.__Graph
       
        
    # add's an edge if it's non existent
    # if it is increments it's weight
    # !notice: reiweighting technique may be false 
    # at this developmental stage
    def addEdgeInc(self,a,b):
        #A = repr(a)#str(a)
        #B = repr(b)#str(b)
        #merging can also be done in other ways
        #add an extra class variable
        A = ''.join(a)
        B = ''.join(b)
        if (A,B) in self.__Graph.edges():
            edata = self.__Graph.get_edge_data(A, B)
            #print "updating edge between (",A,B,") to weigh",(edata['weight']+1)
            self.__Graph.add_edge(A, B, weight=edata['weight']+1)
        else:
            #print "adding edge between (",A,B,")"
            self.__Graph.add_edge(A, B, key='edge', weight=1)
            
    
    # creates ngram's of window based on @param n
    def build_ngram(self,d = []):
        self.setData(d)
        Data = self.__Data
        l = Data[0:min(self.__n,self.__dSize)]
        q = []
        q.append(l[:])
        if(self.__n<self.__dSize):
            for d in Data[min(self.__n,self.__dSize):]:
                l.pop(0)
                l.append(d)
                q.append(l[:])
        self.__ngram = q
        return q
     
    # draws a graph using math plot lib
    def GraphDraw(self, verbose = True, lf = True, ns = 1000, wf= True):
        pos = graphviz_layout(self.__Graph)
        #pos = sring_layout(self.__Graph, scale=1)
        #nx.draw(self.__Graph,pos = pos,node_size=ns,with_labels = lf, node_color = 'm')
        nx.draw(self.__Graph, pos=graphviz_layout(self.__Graph), node_size=ns, cmap=plt.cm.Blues, node_color=range(len(self.__Graph)), prog='dot', with_labels = lf)
        if wf:
            weight_labels = nx.get_edge_attributes(self.__Graph,'weight')
            nx.draw_networkx_edge_labels(self.__Graph,pos = pos,edge_labels = weight_labels)
        if verbose:
            plt.show()
        else:
            plt.savefig('g.png')
            #or to dot
            #nx.drawing.nx_pydot.write_dot(self.__Graph,'g.dot')
    
    # set functions for structure's protected fields
    def setData(self,Data):
        if not(Data == []):
            self.__Data = list(Data)
            self.__dSize = len(self.__Data)
     
    def setN(self,n):
        self.__n=n

    def setDwin(self,win):
        self.__Dwin = win
    
    # get functions for structures protected fields
    def getMin(self):
        return self.__MinSize
    
    def getngram(self):
        return self.__ngram
    
    def getGraph(self):
        return self.__Graph

#test script

#1. construct a 2-gram graph of window_size = 2
#   from the word "abcdef"
ngg = DocumentNGramGraph(2,2,"abcdef")
#ngg = DocumentNGramGraph(3,2,"Do you Like this summary?")
# print it!
#print ngg.getngram()
ngg.GraphDraw()