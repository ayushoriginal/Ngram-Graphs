import copy
from documentModel import *

ngg1 = DocumentNGramGraph(3,2,"abcdef")
ngg2 = DocumentNGramGraph(3,2,"abcdeff")
#ngg1.GraphDraw()
#ngg2.GraphDraw()
gs = getSimilarityNVS()

sc = gs.getSimilarityComponents(ngg1,ngg2)
print sc["SS"]," ",sc["VS"]
print gs.getSimilarityFromComponents(sc)

bOp = binaryOperators()
uniG = bOp.Union(ngg1,ngg2)
print uniG
#zg.GraphDraw()