#### Tussle Framework Common

Tussle Framework - Throughput under SLE testing framework which includes common Benchmarks API, and tools, several benchmark and runners implementation.

The Tussle Framework Common project contains basic interfaces required for constructing ISV benchmarks and different helpful tools for processing results and making reports.


Build **tussle-common jar**:

```
$ mvn clean package -DskipTests

```

Optionally install **tussle-common jar** to local maven repository:

```
$ mvn install:install-file -Dfile=target/tussle-common-1.2.3-jar-with-dependencies.jar -DpomFile=pom.xml
```

Build and install tussle-common jar using provided script:

```
$ ./build.sh
```
