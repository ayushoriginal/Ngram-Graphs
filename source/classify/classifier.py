import numpy as np
from sklearn import svm


class classifier: 
    
    def __init__(self):
        pass
    def learn(self):
        pass
    def classify(self):
        pass
    
class SVM(classifier):
    def __init__(self):
        self._clf = None
    
    def learn_mat(self,X,labels):
        self._clf = svm.SVC(kernel='precomputed')
        # for kernelization we may use other techniques
        gram_mat = np.dot(X,X.T)
        self._clf.fit(gram_mat,labels)
    
    def test(self,X,labels):
        if(X.shape[0] != labels.shape[0]):
            raise ValueError('In order to classify labels must be in a numpy nx1 array and test to be in a nxm.')
        else:
            return np.mean(self.classify(X) * labels)
        
    def classify(self,X):
        return self._clf.predict(X)