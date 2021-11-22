# Judo: Java Pseudocode

Judo is a simple, readable language that targets Java 1.8. It's strongly typed and determines types through type inference.
```
for i in 1..100:
    if i mod 15 == 0:
        out("fizzbuzz")
    elf i mod 5 == 0:
        out("buzz")
    elf i mod 3 == 0:
        out("fizz")
```
The Translator also only requires Java 1.8 to run. Due to the scope of the project, a precompiled JAR file can be found in our [latest release](https://github.com/DaniloHP/csc372project2/releases/latest) and in this repository at [`/jar/judot.jar`](https://github.com/DaniloHP/csc372project2/blob/main/jar/judot.jar).

## Translating and running a Judo file
Before running, make sure you have at least Java 1.8 by running:
```shell
$ java -version
```
The output will vary depending on your JDK implementation, but we want to see `1.8.*` somewhere in there.

To run the Judo translator, execute the JAR we provided like so:
```shell
$ java -jar path/to/jar path/to/judo/file  
# judot will tell you where it saved the translated java file.
# Run it like a normal Java file:
$ javac path/to/java/file.java  # don't forget .java here
$ java path/to/java/file  # no .java here
```
Or, we provided a [convenience script](https://github.com/DaniloHP/csc372project2/tree/main/judot) which is used like so:
```shell
$ ./judot path/to/jar path/to/judo/file judo file arguments
```
For example, to run our program 4 from the root of the repository:
```shell
$ ./judot jar/judot.jar judo-files/required/Program4.txt 10

   *
  ***
 *****
*******
 *****
  ***
   *
```
Our program 4 creates a star triangle. If the Judo file translates correctly, you should just see the Java file's output like above.
## For Graders
Our programs 1-5 are in [`/judo-files/required/`](https://github.com/DaniloHP/csc372project2/tree/main/judo-files/required).
