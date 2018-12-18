# MAL (the Meta Attack Language)

The Meta Attack Language is, as its name suggests, a meta language that specifies all the rules and the elements for domain specific languages. It is an efficient way to avoid creating new attack graphs for every different case on the domain under study. MAL additionally promotes the re-usability of specified elements and has as a final goal to reduce the effort needed by a specific language developer in order to develop the language. As one can expect, it comes together with its respective compiler called MAL compiler.

## Getting Started

These instructions will guide you on how to have a copy of the project up and running on your local machine for development and testing purposes.

### Usage

Since this is a Maven project it is ought to be opened by any compatible IDE or to be used with the mvn command line tool.

To build the MAL compiler, which can be later used to create domain specific languages (DSLs), such as [VehicleLang](https://github.com/pontusj101/vehicleLang), simply issue the following command on the command line:

```
mvn clean install
```

### Using the release version - the .jar file

An easier alternative is to download and use the precompiled version of the MAL compiler, found on the releases of this repository.

To run the MAL compiler together with the DSL of your choice you simply have to issue the following command on the command line:

```
java -cp mal-compiler-0.0.1.jar se.kth.mal.Master -i <input_path> -o <ouput_path> -p <package_name> -t <tests_path>
```

More specifically, the usage of the MAL jar is presented below:

```
usage: 
 -f,--foreseeti       flag to use foreseeti backend
 -i,--input <arg>     input mal file path
 -o,--output <arg>    output folder path for generated code
 -p,--package <arg>   package name of generated code
 -t,--tests <arg>     output folder path for generated test code
 -v,--visual <arg>    icons for visualization

```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Pontus Johnson**
* **Robert Lagerström**
* **Mathias Ekstedt**

## Contributors

* **Sotirios Katsikeas**
* **Simon Wrede**
