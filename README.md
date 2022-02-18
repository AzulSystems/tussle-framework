##### Benchmark common

The project contains common interfaces required for constructing ISV benchmarks and different helpful tools for processing results and making reports.


Build **benchmarks-common jar**:

```
$ mvn clean package -DskipTests

```

Optionally install **benchmarks-common jar** to local maven repository:

```
$ mvn install:install-file -Dfile=target/benchmarks-common-1.2.3-jar-with-dependencies.jar -DpomFile=pom.xml
```

Build and install benchmarks-common jar using provided script:

```
$ ./build.sh
```
