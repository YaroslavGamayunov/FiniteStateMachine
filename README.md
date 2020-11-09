# Finite State Machine
[![codecov](https://codecov.io/gh/YaroslavGamayunov/FiniteStateMachine/branch/master/graph/badge.svg?token=TF0D017DHG)](https://codecov.io/gh/YaroslavGamayunov/FiniteStateMachine)
## Description
Implementation of Finite State Machine supporting determination, minimization and building on regular expressions

Гамаюнов Ярослав, Б05-923

## Technologies
This project is created using Kotlin and Junit for testing purposes

## Quick start
In order to run .jar and work with gradle you will need [JRE](https://www.oracle.com/java/technologies/javase-jre8-downloads.html)
. After JRE Installed, You need to run this gradle task from the root of the project:
```
$ ./gradlew build
```

In order to create problem jar, run this: 
```
$ ./gradlew buildProblemCLI
```

In order to create jar for testing finite state machine, run this: 
```
$ ./gradlew buildStateMachineCLI
```

In both cases you will see .jar file in build/libs
.If you wish to run .jar, run this:
```
$ java -jar filename.jar
```