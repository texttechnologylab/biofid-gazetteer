# BIOfid Gazetteer

Skip-Gram Based Taxon Tagger for the [TextImager]() Pipeline

[![BIOfid](http://img.shields.io/badge/project-BIOfid-4C7828.svg)](https://www.biofid.de/en/)
[![version](https://img.shields.io/github/license/texttechnologylab/biofid-gazetteer)]()
[![latest](https://img.shields.io/github/v/release/texttechnologylab/biofid-gazetteer)]()

[![Paper 1](http://img.shields.io/badge/paper-ACL_Anthology-B31B1B.svg)](https://aclanthology.org/K19-1081/)
[![Conference 1](http://img.shields.io/badge/conference-CoNLL_2019-4b44ce.svg)](https://lrec2022.lrec-conf.org/)

[![Article](http://img.shields.io/badge/article-Springer-B31B1B.svg)](https://link.springer.com/article/10.1007/s10579-021-09553-5)
[![Journal](http://img.shields.io/badge/journal-Language_Resources_and_Evaluation,_Volume_56-4b44ce.svg)](https://www.springer.com/journal/10579)

## Description

A Java-based gazetteer tagger, developed for the [BIOfid](https://www.biofid.de/en/) project.
Recognizes biological entities provided with large lists (_gazetters_) in texts.

Utilizes a Java `ConcurrentHashMap`-backed tree-search algorithm parallelized with Java 8 streams that tags arbitrary texts of `n` words in `O(c · n)` time by looking up (&rightarrow; `c`) each word in a previously created tree.
Each node in the tree represents a word from the given input lists.
All leaves must have a label (usually an URI); any node in the tree _may_ have a label.
Also allows to create skip-grams and abbreviations from input terms.

## Note

The tagger is highly suspectible to false positives, such as vernacular names that double as common names of people (espcially prominent in German, eg. [Schneider](https://de.wikipedia.org/wiki/Schneider_(Begriffskl%C3%A4rung))).
Please keep this in mind while curating input lists/gazetteers.

## ⚠ Deprecation Pending ⚠

This repository is in the process of being replaced by a Rust implemenation: [gazetteer-rs](https://github.com/texttechnologylab/gazetteer-rs)

## Citation

The tool was used in the creation of all BIOfid corpora. Please cite 
