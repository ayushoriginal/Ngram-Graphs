# Ngram-Graphs

[Click here to see my introductory powerpoint presentation on this topic](http://www.slideshare.net/ayushoriginal/2016-m7-w2)

[![Gitter](https://badges.gitter.im/Ngram-Graphs/Lobby.svg)](https://gitter.im/Ngram-Graphs/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# JInsect
The JINSECT toolkit is a Java-based toolkit and library that supports and demonstrates the use of n-gram graphs within Natural Language Processing applications, ranging from summarization and summary evaluation to text classiﬁcation and indexing. This repository has parts of the collaborative work that Ayush Pareek did with Dr. George Giannakopoulos to build this tool.

## Main concepts

## Code Snippets
* Create an n-gram graph from a string:

```java

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;

...

// The string we want to represent
String sTmp = "Hello graph!";

// The default document n-gram graph with min n-gram size 
// and max n-gram size set to 3, and dist parameter set to 3
DocumentNGramGraph dngGraph = new DocumentNGramGraph();

// Create the graph
dngGraph.setDataString(sTmp);

```

* Create an n-gram graph from a file 

```java

...

import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.io.IOException;

...
        
	// The filename of the file the contents of which will form the graph
	String sFilename = "ayush_file.txt";
	DocumentNGramGraph dngGraph = new DocumentNGramGraph(); 
	// Load the data string from the file, also dealing with exceptions
	try {
		dngGraph.loadDataStringFromFile(sFilename);
	} catch (IOException ex) {
		ex.printStackTrace();
	}

```

* Output graph to DOT file 

```java

import
gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.utils;

...

	// create the n-gram graph
	String sData = "Hello there, graph world!";
	DocumentNGramGraph dngGraph = new DocumentNGramGraph();
	dngGraph.setDataString(sData);

	/* The following command gets the first n-gram graph level (with the
	minimum n-gram size) and renders it, using the utils package, 
	as a DOT string */
	System.out.println(utils.graphToDot(dngGraph.getGraphLevel(0), true));


```

* Compare two graphs, extracting their similarity 

```java

...

import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.utils;
import java.io.IOException;

...

    String sTmp = "Hello graph!";
    DocumentNGramGraph dngGraph = new DocumentNGramGraph(); 
    dngGraph.setDataString(sTmp);
    String sTmp2 = "Hello other graph!";
    DocumentNGramGraph dngGraph2 = new DocumentNGramGraph(); 
    dngGraph2.setDataString(sTmp2);

    // Create a comparator object
    NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
    // Extract similarity
    GraphSimilarity gs = ngc.getSimilarityBetween(dngGraph, dngGraph2);
    // Output similarity (all three components: containment, value and size)
	System.out.println(gs.toString());

```

* Merge two graphs 

```java

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;

...

	// create the two graphs
	String sTmpA = "Hello graph A!";
	String sTmpB = "Hello graph B!";
	DocumentNGramGraph dngGraphA = new DocumentNGramGraph();
	DocumentNGramGraph dngGraphB = new DocumentNGramGraph();
	dngGraphA.setDataString(sTmpA);
	dngGraphB.setDataString(sTmpB);

	// perform merging with weight factor 0.5 (averaging)
	// result is on dngGraphA
	dngGraphA.mergeGraph(dngGraphB, 0.5);

```

* Load and save a graph to a file

```java

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;

...

		// string to be represented
		String sTmp = "Hello there, I am an example string!";
		DocumentNGramGraph dngGraph = new DocumentNGramGraph();
		INSECTFileDB<DocumentNGramGraph> db = new INSECTFileDB<DocumentNGramGraph>();
		
		// if the file already exists
		if (db.existsObject("test", "graph")) { 
			dngGraph = db.loadObject("test", "graph");
		}
		else {
			// Create the graph
			dngGraph.setDataString(sTmp);

			// save object to file
			db.saveObject(dngGraph, "test", "graph");
		}

```



## This version of the library has implementations of the following features-
- The n-gram graphs (NGG) representations. See [thesis, Chapter 3](http://www.iit.demokritos.gr/~ggianna/thesis.pdf) for more info.
- The NGG operators update/merge, intersect, allNotIn, etc. See [thesis, Chapter 4](http://www.iit.demokritos.gr/~ggianna/thesis.pdf) for more info.
- The AutoSummENG summary evaluation family of methods.
- INSECTDB storage abstraction for object serialization.
- A very rich (and useful!) utils class which one *must* consult before trying to work with the graphs.
- Tools for the estimation of optimal parameters of n-gram graphs
- Support for [DOT](http://www.graphviz.org/doc/info/lang.html) language representation of NGGs.
...and many many side-projects that are hidden including a chunker based on something similar to a language model, a semantic index that builds upon string subsumption to determine meaning and many others. Most of these are, sadly, not documented or published.

This library version:
* supports efficient multi-threaded execution
* contains examples of application for classification
* contains examples of application for clustering
* contains command-line application for language-neutral summarization

**TODO:** 
* Clean problematic classes that have dependencies from Web services.


## License
JInsect is under [LGPL license](https://www.gnu.org/licenses/lgpl.html).


![0](http://i.imgur.com/mHGeaqR.jpg)
![1](http://i.imgur.com/pcLYLJw.jpg)
![2](http://i.imgur.com/40rOSnC.jpg)
![3](http://i.imgur.com/82OkzDg.jpg)
![4](http://i.imgur.com/annHVI5.jpg)
![5](http://i.imgur.com/2JB4FNV.jpg)
![6](http://i.imgur.com/b5Wzux8.jpg)
![7](http://i.imgur.com/7dWFI2V.jpg)
![8](http://i.imgur.com/WbWsiNJ.jpg)
![9](http://i.imgur.com/KrQEXwF.jpg)
![10](http://i.imgur.com/Kc9twoM.jpg)
![11](http://i.imgur.com/qjX937m.jpg)
![12](http://i.imgur.com/T3Cn4KG.jpg)
![13](http://i.imgur.com/0b9GSnu.jpg)
![14](http://i.imgur.com/HDa6HHk.jpg)
![15](http://i.imgur.com/wUDIaFh.jpg)
![16](http://i.imgur.com/haxVrls.jpg)
![17](http://i.imgur.com/qsdSIMG.jpg)

![18](https://i.imgur.com/uVJ6A5e.jpg)
