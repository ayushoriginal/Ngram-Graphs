#!/usr/bin/env python

"""
   NGramCachedGraphComparator.py
 
   Created on May 24, 2017, 3:56 PM
"""
from ..representations.DocumentNGramGraph import DocumentNGramGraph

"""
 An n-gram graph similarity class
 that calculates a set of ngram graph
 similarity measures implementing
 basic similarity extraction functions.
 
 @author ysig
"""
class getSimilarity:
    def __init__(self):
        pass
    
    def getSimilarityDouble(self,ngg1,ngg2):
        return 0.0
    
    def getSimilarityComponents(self,ngg1,ngg2):
        return {"SS" : 0, "VS" : 0, "NVS" : 0}
        
    def getSimilarityFromComponents(self,Dict):
        return 0.0

class getSimilaritySS(getSimilarity):
    
    def __init__(self):
        pass
    
    def getSimilarityDouble(self,ngg1,ngg2):
        return (min(ngg1.minW(),ngg2.minW())*1.0)/max(ngg1.maxW(),ngg2.maxW())
        
    def getSimilarityComponents(self,ngg1,ngg2):
        return {"SS" : (min(ngg1.minW(),ngg2.minW())/max(ngg1.maxW(),ngg2.maxW()))}
        
    def getSimilarityFromComponents(self,Dict):
        if (Dict.has_key("SS")):
            return Dict["SS"]
        else: 
            return 0.0
    


class getSimilarityVS(getSimilarity):
    
    def __init__(self):
        pass
    
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
        
    def getSimilarityComponents(self,ngg1,ngg2):
        return {"VS" : self.getSimilarityDouble(ngg1,ngg2)}
        
    def getSimilarityFromComponents(self,Dict):
        if (Dict.has_key("VS")):
            return Dict["VS"]
        else: 
            return 0.0
    
class getSimilarityNVS(getSimilarity):
    
    def __init__(self):
        pass
    
    def getSimilarityDouble(self,ngg1,ngg2):
        SS = getSimilaritySS()
        VS = getSimilarityVS()
        return (VS.getSimilarityDouble(ngg1,ngg2)*1.0)/SS.getSimilarityDouble(ngg1,ngg2)
    
    def getSimilarityComponents(self,ngg1,ngg2):
        SS = getSimilaritySS()
        VS = getSimilarityVS()
        return {"SS" : SS.getSimilarityDouble(ngg1,ngg2), "VS" : VS.getSimilarityDouble(ngg1,ngg2)}
    
    def getSimilarityFromComponents(self,Dict):
        if((Dict.has_key("SS") and Dict.has_key("VS")) and (str(Dict["SS"]) != "0.0")):
            return (Dict["VS"]*1.0)/Dict["SS"]
        else:
            return 0.0
            