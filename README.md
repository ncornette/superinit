[![Build Status](https://travis-ci.org/ncornette/superinit.svg?branch=master)](https://travis-ci.org/ncornette/superinit)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b9b30724b03149f3abde10c021be7437)](https://www.codacy.com/app/nicolas-cornette/superinit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ncornette/superinit&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/ncornette/superinit/branch/master/graph/badge.svg)](https://codecov.io/gh/ncornette/superinit)
[ ![Download](https://api.bintray.com/packages/ncornette/maven/superinit/images/download.svg) ](https://bintray.com/ncornette/maven/superinit/_latestVersion)

# Superinit

Async dependency tree loader

## Gradle

```groovy

repositories {
    jcenter()
}

dependencies {
	// Your dependencies
	...

	// Async dependency tree loader
    // https://github.com/ncornette/superinit
    compile 'com.ncornette.superinit:superinit:0.9.4'
}
```

## Usage

Wrap any `Runnable` into a `InitNode` object, then define dependencies :


```java
    InitNode nodeA = new InitNode(runnableA);
    InitNode nodeB = new InitNode(runnableB);
    InitNode nodeC = new InitNode(runnableC);

    // Setup dependencies
    nodeA.dependsOn(nodeB);

```

Then use `InitLoader.load()` to execute all nodes in order based on dependencies, and in parallel when possible.

```java

    InitLoader initLoader =  new InitLoader(3); // N Threads in Thread pool executor

    // Execute tasks
    initLoader.load(loaderCallback, nodeA, nodeB, nodeC);

```

Use the `InitLoaderCallback` interface to be notified of the `InitLoader`

```java
public interface InitLoaderCallback {

    // Is called once on success, even if error occurred
    // Not called if InitLoader.interrupt() is called during execution 
    void onFinished();

    // Is called each time a node execution fails with an Exception
    void onNodeError(NodeExecutionError nodeError);

    // Is called when an exception occurs outside a node execution
    void onError(Throwable error);

}
```

## Features
A `ThreadpoolExecutor` with a fixed number of Threads will excute the tasks. Even with One Thread, execution order 
is guaranteed to be respected.

Circular dependencies are prevented as early as possible, and will throw `IllegalArgumentException` :  

 - When adding a dependency with `dependsOn()` for direct circular dependencies.
 - When calling `Initloader.load()` for indirect circular dependencies.

Nodes use `CountDownLatch` to wait for execution of their dependencies and to notify their execution is finished.



## Reference

[Dependency Resolving Algorithm](http://www.electricmonk.nl/docs/dependency_resolving_algorithm/dependency_resolving_algorithm.html)


## License

    Copyright 2016 Nicolas Cornette

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
