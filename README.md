# Welcome to KenLM for Java

## KenLM Language Model Toolkit

KenLM estimates, filters, and queries language models. Estimation is fast and scalable due to streaming algorithms explained in the paper

> Scalable Modified Kneser-Ney Language Model Estimation  
> Kenneth Heafield, Ivan Pouzyrevsky, Jonathan H. Clark, and Philipp Koehn.  
> ACL, Sofia, Bulgaria, 4—9 August, 2013.  
> [Paper](http://kheafield.com/professional/edinburgh/estimate_paper.pdf) [Slides](http://kheafield.com/professional/edinburgh/estimate_talk.pdf) [BibTeX](http://kheafield.com/professional/bib/Heafield-estimate.bib)

Querying is fast and low-memory, as shown in the paper

> KenLM: Faster and Smaller Language Model Queries  
> Kenneth Heafield.   
> WMT at EMNLP, Edinburgh, Scotland, United Kingdom, 30—31 July, 2011.   
> [Paper](http://kheafield.com/professional/avenue/kenlm.pdf) [Slides](http://kheafield.com/professional/avenue/kenlm_talk.pdf) [BibTeX](http://kheafield.com/professional/bib/Heafield-kenlm.bib)

## KenLM for Java

KenLM is a popular Language Modeling Toolkit that is written in performant C++.  KenLM for Java aims to make KenLM easily accessible to the Java ecosystem.  It wraps portable OSX and Linux builds of KenLM, and loads these native libraries via JNI.  Users of KenLM for Java need only to include a reference to the KenLM for Java jar, and then use the Java API.

## Supported Platforms

These platforms have been tested and are supported.  Other distributions and operating systems that are similar to those below are also likely to work.

- Ubuntu 14.04, 16.04 (x86_64)
- Amazon Linux 2012.03, 2016.03 (x86_64)
- RHEL 5 (x86_64)
- OSX/MacOS Sierra, Mavericks (x86_64)

## Non-Supported Platforms

These platforms are not currently supported.  Please request a platform by opening an issue on Github if you'd like to see it supported in the future.

- ARM
- x86_32
- Windows

## Current build 
- e6a600c8 (Sun Aug 28 13:19:54 2016)
- https://github.com/kpu/kenlm/commit/e6a600c8