import pdb
from documentModel import *

ngg1 = DocumentNGramGraph(3,2,"abcdef")
ngg2 = DocumentNGramGraph(3,2,"abcdeff")
#ngg1.GraphDraw()
#ngg2.GraphDraw()
gs = getSimilarityNVS()

sc = gs.getSimilarityComponents(ngg1,ngg2)
print sc["SS"]," ",sc["VS"]
print gs.getSimilarityFromComponents(sc)

#bOp = binaryOperators()

#uniG = pdb.run('bOp.Union(ngg1,ngg2)')

#uniG = bOp.Union(ngg1,ngg2)
#interG = bOp.Intersect(ngg1,ngg2)
#deltaG = bOp.delta(ngg1,ngg2)
#invintG = bOp.inverse_intersection(ngg1,ngg2)

#print uniG
#print interG
#print deltaG
#print invintG
#zg.GraphDraw()
