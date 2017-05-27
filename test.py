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

uni = Union(l1=0.5, l2 = 0.5, commutative=True,distributional=True)
op = LtoRNary(uni)

# strange error still here
# maybe dependent with 
# garbage collecting
# functions arguments
# as fields on ar's
#op.apply(ngg1,ngg2)
clo.apply(ngg1).GraphDraw()
