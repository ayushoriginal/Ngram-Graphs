import numpy as np
import sys
sys.path.append('..')
from source import *


def randPerm(S,L,fact=0.8):
    indices = np.random.permutation(S.shape[0])
    training_idx, test_idx = indices[:int(l*fact)], indices[int(l*fact):]
    training, test = S[training_idx,:], S[test_idx,:]
    training_labels, test_labels = L[training_idx], L[test_idx]
    return training, test, training_labels, test_labels

sr = SequenceReader()
sr.read('./biodata/UCNEs/hg19_UCNEs.fasta')
hd = sr.getDictionary()

print hd

sr.read('./biodata/UCNEs/galGal3_UCNEs.fasta')
cd = sr.getDictionary()

print cd

n=3
Dwin=2
subjectMap = {}
ngg = {}
l1 = len(hd.keys())
l2 = len(cd.keys())
l = l1 + l2

i = 0
for key,a in hd.iteritems():
    subjectMap[i] = (key,'humans')
    ngg[i] = DocumentNGramGraph(3,2,a)
    i+=1

for key,b in cd.iteritems():
    subjectMap[i] = (key,'chickens')
    ngg[i] = DocumentNGramGraph(3,2,b)
    i+=1

S = np.empty([l, l])
L = np.empty([l])
sop = SimilarityNVS()

i=0
for key,a in hd.iteritems():
    l=0
    L[i] = 0 #0 for humans
    for kh,h in hd.iteritems():
        S[i,l] = sop.getSimilarityDouble(ngg[i],ngg[l])
        l+=1 
    for kc,c in cd.iteritems():
        S[i,l] = sop.getSimilarityDouble(ngg[i],ngg[l])
        l+=1
    i+=1
    
for key,b in cd.iteritems():
    l=0
    L[i] = 1 #1 for chickens
    for kh,h in hd.iteritems():
        S[i,l] = sop.getSimilarityDouble(ngg[i],ngg[l])
        l+=1 
    for kc,c in cd.iteritems():
        S[i,l] = sop.getSimilarityDouble(ngg[i],ngg[l])
        l+=1
    i+=1
        
svm = SVM()
reps = 10
acc = 0

S1 = S[0:l1,:]
S2 = S[l1:,:]
L1 = L[0:l1]
L2 = L[l1:]

for i in range(1, reps+1):
    tr1, te1, trl1, tel1 = randPerm(S1,L1,fact=0.8)
    tr2, te2, trl2, tel2 = randPerm(S2,L2,fact=0.8)
    
    training = np.concatenate((tr1, tr2), axis=0)
    training_labels = np.concatenate((trl1, trl2), axis=0)
    testing = np.concatenate((te1, te2), axis=0)
    testing_labels = np.concatenate((tel1, tel2), axis=0)
    print training
    print training_labels
    print testing
    print testing_labels
    svm.learn_mat(training,training_labels)
    acc += svm.test(testing,testing_labels)

print "Overall classification accuracy for "+str(reps)+" repetitions\n of random 80%/20% split of train/test data\n is "+str(acc/reps)+"%."

