### Groningen [![Build Status](https://travis-ci.org/sladeware/groningen.png?branch=master)](https://travis-ci.org/sladeware/groningen)

A generational genetic algorithm approach to Java Virtual Machine settings optimization for a variety of servers.

#### Building

Groningen requires `protobuf version >= 2.4.1`. In case you have own locally
built protobuf, make sure to specify the right path to it as follows:

```shell
$ export PROTOC_EXECUTABLE=/somewhere/protobuf-2.4.1/build/bin/protoc
```

By default `/usr/bin/protoc` will be used.

Next, get the sources and build it:

```shell
$ mvn package
$ mvn assembly:assembly
```

#### Running

You can run Groningen as follows:

```shell
$ java -jar target/groningen-1.0-SNAPSHOT-jar-with-dependencies.jar [options]
```

The available options can be found on [deployment page](https://github.com/sladeware/groningen/wiki/Deployment#command-line-arguments).

#### Release Engineering

* Snapshot

    ```shell
    $ mvn clean deploy -DperformRelease=true
    ```

* Release

    ```shell
    $ mvn release:clean -DperformRelease=true
    $ mvn release:prepare -DperformRelease=true
    $ mvn release:perform -DperformRelease=true
    ```
