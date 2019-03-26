# foreseeti securiCAD generator

In order to produce language JAR files compatible with foreseeti's securiCAD, additional prerequisite information has to be provided:
* securiCAD libraries must be available for compile time dependencies
* the MAL language spec has to use supported categories
* Image files for asset class icons must be provided (optional)

## securiCAD libraries

The easiest way is to build the foreseeti `kernel-CAD` repository which will publish the dependencies in the local Maven repository.

In order for Maven to find the dependencies, they must be declared in the `pom.xml` file by adding the following inside the `<dependencies>' section:
```
<dependency>
    <groupId>com.foreseeti</groupId>
    <artifactId>corelib</artifactId>
    <version>1.4.7</version>
</dependency>
<dependency>
    <groupId>com.foreseeti</groupId>
    <artifactId>simulator</artifactId>
    <version>1.4.7</version>
</dependency>
```

Replace `1.4.7` with the suitable version spec.

If using Maven is not an option, the following JARs have to be placed on the MAL compiler's classpath:
* Corelib - `corelib-<ver>.jar`
* Simulator - `simulator-<ver>.jar`

## Note on Categories

Currently, securiCAD *requires* the categories used in the MAL spec to be taken from the following set:
* `Communication`
* `Container`
* `Networking`
* `Security`
* `System`
* `User`
* `Zone`

Future releases of securiCAD may support dynamic generation of categories based on the language.

## Asset class icons

In order for the securiCAD tools to render the asset class icons, they must be provided to the MAL compiler and built into the language JAR file.

securiCAD supports two different image formats - PNG and SVG. PNG is the only format supported by securiCAD Professional. securiCAD Enterprise supports both SVG and PNG, with SVG being the preferred format.

Icon files are provided by specifying a directory containing all image files, where the file names without the file extensions are matched with the asset names from the language specification. For example, an asset class called `Network` must have the image file name `Network.svg` or `Network.png`.

If SVG files are provided, PNGs will automatically be generated in the resulting language JAR. This is the preferred way as the resulting jar will work in securiCAD Professional and give securiCAD Enterprise its preferred format.

Providing PNGs will work with both products but will give a suboptimal rendering in the securiCAD Enterprise Web Modeler.