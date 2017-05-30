import pdb
from documentModel import *

ngg1 = DocumentNGramGraph(3,2,"abcdef")
ngg2 = DocumentNGramGraph(3,2,"abcdeff")
#ngg1.GraphDraw()
#ngg2.GraphDraw()
gs = SimilarityNVS()

sc = gs.getSimilarityComponents(ngg1,ngg2)
print sc["SS"]," ",sc["VS"]
print gs.getSimilarityFromComponents(sc)

nop = LtoRNary(gs)
print gs.apply(ngg1,ngg2)
bop = Union(lf=0.5, commutative=True,distributional=True)
nop = LtoRNary(bop)
# strange error still here
# maybe dependent with 
# garbage collecting
# functions arguments
# as fields on ar's
bop.apply(ngg1,ngg2).GraphDraw()

