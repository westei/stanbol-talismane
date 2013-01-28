Apache Stanbol talismane integration
================

[Talismane](https://github.com/urieli/talismane) is a [AGPL](http://www.fsf.org/licenses/agpl.html) licensed language analysis tool suite that supports French. This project aims to provide a standalone server providing a RESTful API that can than be used by Apache Stanbol for NLP processing of texts.

## Install Talismane

The Talismane are not available on any Maven server. Because of that it is necessary

* clone [talismane](https://github.com/urieli/talismane) form github.com
* compiles using Apache Ant and to
* install  the jars to the local maven repo using Apache Maven

This steps can be done by using the `install-talismane.sh` in the same directory.

## Running the Stanbol Talismane Server

### Building the Server

By building the project with

    mvm clean install
    
the runable JAR for the server is also be assembled. You can find the JAR under

    talismane-server/target/at.salzburgresearch.stanbol.stanbol.enhancer.nlp.talismane.server-1.0.0-SNAPSHOT-jar-with-dependencies.jar

Before running the server you should copy this jar file to an dedicated directory.

### Running the Server

The server supports the following command line parameters

* `-h --help` : Prints an help screen similar to this documentation
* `-p --port {port}`: the port (default 8080)
* `-t --analyser-threads {analyzer-thread}`: The size of the thread pool used for Talismane to tokenize and POS tag sentences (default: 10)

__Example__: To following command will start the server on port 8082

    java -Xmx1g -jar at.salzburgresearch.stanbol.stanbol.enhancer.nlp.talismane.server-1.0.0-SNAPSHOT-jar-with-dependencies.jar -p 8082


License(s):
-----------

All modules are dual licensed under the [AGPL](http://www.fsf.org/licenses/agpl.html) and the [Apache License Version 2.0](LICENSE).

### Why two licenses

While I am no expert the intension of having two licenses is the following: Executing this code requires to confirm to the more restrictive rules defined by the [AGPL](http://www.fsf.org/licenses/agpl.html) the more permissive Apache License will still allow users to take code snippets or utility classes and do with them what ever they want.