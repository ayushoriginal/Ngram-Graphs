import sys
sys.path.append('..')
from source import *

sr = SequenceReader()
sr.read('Schizosaccharomyces_pombe.ASM294v2.30.dna.I.fa')
d = sr.getDictionary()
print d['name']

    
ngg1 = DocumentNGramGraph(3,2,d["name"])
ngg1.GraphDraw(False) 