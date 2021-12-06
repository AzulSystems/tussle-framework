# Benchmark common

The project contains common files and interfaces required for constructing ISV benchmarks.


Build:

```
$ mvn -DskipTests clean package
```

Deploy to local maven repository (~/.m2):

```
$ mvn install:install-file -Dfile=target/benchmarks-common-1.2.1-jar-with-dependencies.jar -DpomFile=pom.xml
```
