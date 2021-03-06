# MARK - Easy BigData Ranking

[![pipeline status](https://gitlab.cylab.be/cylab/mark/badges/master/pipeline.svg)](https://gitlab.cylab.be/cylab/mark/commits/master)
[![coverage report](https://gitlab.cylab.be/cylab/mark/badges/master/coverage.svg)](https://gitlab.cylab.be/cylab/mark/-/commits/master)
[![Maven Central](https://img.shields.io/maven-central/v/be.cylab.mark/server)](https://mvnrepository.com/artifact/be.cylab.mark/server)
[![javadoc](https://javadoc.io/badge2/be.cylab.mark/server/javadoc.svg)](https://javadoc.io/doc/be.cylab.mark/server)

Development now takes place at https://gitlab.cylab.be/cylab/mark

The **multi-agent ranking framework (MARK)** aims to provide all the building blocks that are required to build large scale **detection and ranking systems**. Namely, the framework provides the following key components: 

* a **distributed storage** suited for very large datasets and BigData applications, 
* a **web based visualization and management interface**,
* a distributed **execution framework** to reliably execute the detection algorithms and balance the compute load between multiple servers,
* and an easy to configure **triggering mechanism** that allows to execute the detection algorithms when new data is available or when configured conditions are met.

All these components are implemented in a generic way. They are independent of the type of data that will be processed by the framework. This allows the data scientist to focus on his core business: **developing effective detection algorithms**.

![](./documentation/architecture.png)



![](documentation/cascade.png)



![](documentation/ranking.png)



![](documentation/status.png)



## Getting started

To get started with installation and to implement your own detection algorithms, check our [tutorials](https://cylab.be/research/mark)

