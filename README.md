Fusepool-SMA
============

This project is an implementation of a dictionary-matching algorithm as an enhancer engine.

###What is Fusepool SMA?

Fusepool SMA is a pure Java based Apache Stanbol enhancement engine, it is based on the Aho-Corasick string matching algorithm. It was created as an alternative solution for entity extraction, because it is much more flexible than any model-based NER solution. 

The Aho-Corasic string matching algorithm is a dictionary-matching algorithm that locates elements of a finite set of words and expressions which is called the dictionary, within the input text. The input of the enhancer is plain, unstructured text, the output is RDF that contains the extracted entities and additional information on the enhancement.


##Stanbol Enhancement Engine

The outcome of this project is an OSGi bundle (enhancement engine) for Apache Stanbol that takes plain text documents as input and gives RDF triples as output.

The enhancer engine can contains multiple SMA instances, each instance is a separate module, and therefore each module has its own configuration page inside Stanbol Configuration Manager. It also means that different SMA instances can be part of different enhancer chains.

Currenty Fusepool-SMA has one enhancer instance that uses the disease ontology from http://disease-ontology.org/. The name of this instance is "smaDisease". This same name must be used when configuring a chain in order to use it.
