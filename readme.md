## Prerequisites ##

JAD
```
http://varaneckas.com/jad/
```


## How to run ##

This can be run in 3 different ways.
1. Navigate to the directory where the jar (decompiler.jar) file is kept.

```
java -Xmx100m -jar decompiler.jar
```

OR

2. double-click or run run.bat


OR

3. double-click decompiler.jar


## General Notes ##

You can select multiple files from a single directory, or select a directory
and the app will recursively decompile all *.class files within.

In all cases, the decompiled files will be named *.java and will
be copied to the same directory where the *.class files originally were.