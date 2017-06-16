import pdb
import sys
sys.path.append('..')
from source import *


#ngg1 = DocumentNGramGraph(3,2,"abcdef")
#ngg2 = DocumentNGramGraph(3,2,"abcdeff")
ngg1 = DocumentNGramGraph(3,2,"A test")
ngg2 = DocumentNGramGraph(3,2,"Another test test")
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
pop = ParallelNary(bop)

nop.apply(ngg1,ngg2).GraphDraw()


