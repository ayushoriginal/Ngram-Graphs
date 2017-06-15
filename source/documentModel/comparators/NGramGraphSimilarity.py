#!/usr/bin/env python

"""
   NGramCachedGraphComparator.py
 
   Created on May 24, 2017, 3:56 PM
"""

"""
 An n-gram graph similarity class
 that calculates a set of ngram graph
 similarity measures implementing
 basic similarity extraction functions.
 
 @author ysig
"""
from Operator import *


# a general similarity class
# that acts as a pseudo-interface
# defining the basic class methods
class Similarity(BinaryOperator):
    
    def __init__(self, commutative = True, distributional = False):
       self._commutative = commutative
       self._distributional = distributional
        
    # given two ngram graphs
    # returns the given similarity as double
    def getSimilarityDouble(self,ngg1,ngg2):
        return 0.0
    
    # given two ngram graphs
    # returns some midway extracted similarity components
    # as a dictionary between of sting keys (similarity-name)
    # and double values
    def getSimilarityComponents(self,ngg1,ngg2):
        return {"SS" : 0, "VS" : 0, "NVS" : 0}
    
    # from the similarity components extracts
    # what she wants for the given class
    def getSimilarityFromComponents(self,Dict):
        return 0.0
	
    def apply(self,*args,**kwargs):
      super(Similarity, self).apply(*args)
      return self.getSimilarityDouble(*args)

class SimilaritySS(Similarity):
    
    # given two ngram graphs
    # returns the SS-similarity as double
    def getSimilarityDouble(self,ngg1,ngg2):
        return (min(ngg1.minW(),ngg2.minW())*1.0)/max(ngg1.maxW(),ngg2.maxW())
    
    # given two ngram graphs
    # returns the SS-similarity
    # components on a dictionary
    def getSimilarityComponents(self,ngg1,ngg2):
        return {"SS" : (min(ngg1.minW(),ngg2.minW())/max(ngg1.maxW(),ngg2.maxW()))}
    
    # given similarity components
    # extracts the SS measure
    # if existent and returns it (as double)
    def getSimilarityFromComponents(self,Dict):
        if (Dict.has_key("SS")):
            return Dict["SS"]
        else: 
            return 0.0
    


class SimilarityVS(Similarity):
    
    # given two ngram graphs
    # returns the VS-similarity as double    
    def getSimilarityDouble(self,ngg1,ngg2):
        s = 0.0
        g1 = ngg1.getGraph()
        g2 = ngg2.getGraph()
        edges2 = g2.edges()
        for (u,v,d) in g1.edges(data=True):
            if((u,v) in edges2):
                dp = g2.get_edge_data(u, v)
                s+= min(d['weight'],dp['weight'])/max(d['weight'],dp['weight'])
        return s/max(g1.number_of_edges(),g2.number_of_edges())

    # given two ngram graphs
    # returns the VS-similarity
    # components on a dictionary        
    def getSimilarityComponents(self,ngg1,ngg2):
        return {"VS" : self.getSimilarityDouble(ngg1,ngg2)}
    
    # given similarity components
    # extracts the SS measure
    # if existent and returns it (as double)
    def getSimilarityFromComponents(self,Dict):
        if (Dict.has_key("VS")):
            return Dict["VS"]
        else: 
            return 0.0
    
class SimilarityNVS(Similarity):
     
    # given two ngram graphs
    # returns the NVS-similarity as double    
    def getSimilarityDouble(self,ngg1,ngg2):
        SS = SimilaritySS()
        VS = SimilarityVS()
        return (VS.getSimilarityDouble(ngg1,ngg2)*1.0)/SS.getSimilarityDouble(ngg1,ngg2)
    
    # given two ngram graphs
    # returns the NVS-similarity
    # components e.g. SS and VS
    # on a dictionary
    def getSimilarityComponents(self,ngg1,ngg2):
        SS = SimilaritySS()
        VS = SimilarityVS()
        return {"SS" : SS.getSimilarityDouble(ngg1,ngg2), "VS" : VS.getSimilarityDouble(ngg1,ngg2)}
    
    # given a dictionary containing
    # SS similarity and VS similarity
    # extracts NVS if SS is not 0
    def getSimilarityFromComponents(self,Dict):
        if((Dict.has_key("SS") and Dict.has_key("VS")) and (str(Dict["SS"]) != "0.0")):
            return (Dict["VS"]*1.0)/Dict["SS"]
        else:
            return 0.0
            
