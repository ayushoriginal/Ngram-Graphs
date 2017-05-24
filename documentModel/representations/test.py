from DocumentNGramGraph import DocumentNGramGraph
from DocumentNGramSymWinGraph import DocumentNGramSymWinGraph
from DocumentNGramGaussNormGraph import DocumentNGramGaussNormGraph

ngg = DocumentNGramGraph(3,2,"abcdef")
#ngg = DocumentNGramGraph(3,2,"Do you Like this summary?")
# print it!
# print ngg.getngram()
ngg.GraphDraw()

ngswg = DocumentNGramSymWinGraph(3,4,"abcdef")
#ngg = DocumentNGramGraph(3,2,"Do you Like this summary?")
# print it!
#print ngg.getngram()
ngswg.GraphDraw()

nggng = DocumentNGramGaussNormGraph(3,4,"abcdef")
nggng.GraphDraw()