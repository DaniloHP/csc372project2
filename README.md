# Judo: Java Pseudocode

Judo is a simple, readable language that targets Java 1.8.
```
for i in 1..100:
    if i mod 15 == 0:
        out("fizzbuzz")
    elf i mod 5 == 0:
        out("buzz")
    elf i mod 3 == 0:
        out("fizz")
```
The Translator also only requires Java 1.8 to run. Due to the scope of the project, a precompiled JAR file can be found in our [latest release](https://github.com/DaniloHP/csc372project2/releases/latest) or in this repository at [`/jar/judot.jar`](https://github.com/DaniloHP/csc372project2/blob/feature-docs/jar/judot.jar).

## Translating and running a Judo file
Before running, make sure you have at least Java 1.8 by running:
```shell
$ java -version
```
The output will vary depending on your JDK implementation, but we want to see `1.8.*` somewhere in there.

```shell
$ java -jar path/to/jar path/to/judo/file  
# judot will tell you where it saved the translated java file.
# Run it like a normal Java file:
$ javac path/to/java/file.java  # don't forget .java here
$ java path/to/java/file  # no .java here
```
