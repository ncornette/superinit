package com.ncornette.superinit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InitNode implements Runnable {

    private static final Set<InitNode> EMPTY_SET = Collections.emptySet();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    Set<InitNode> dependencies = EMPTY_SET;
    Set<InitNode> descendants = EMPTY_SET;

    private Runnable task;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private volatile boolean started = false;
    private volatile boolean executed = false;
    private volatile boolean cancelled = false;
    private volatile Exception error = null;

    public Collection<InitNode> newNodesWithDescendants() {
        return newNodesWithDescendants(this);
    }

    public InitNode() {
        this(null);
    }

    public InitNode(Runnable task) {
        this.task = task;
    }

    public boolean finished() {
        return countDownLatch.getCount() == 0;
    }

    public boolean success() {
        return executed;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public boolean error() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    public InitNode dependsOn(Collection<InitNode> dependencies) {
        if (this.dependencies == EMPTY_SET) {
            this.dependencies = new HashSet<>();
        }
        for (InitNode dependency : dependencies) {
            if (dependency.dependencies.contains(this) || dependency == this) {
                throw new IllegalArgumentException(String.format(
                        "Error adding dependency: %s, circular dependency detected", dependency));
            }
        }
        setDependencies(dependencies);
        setDescendantOf(dependencies);
        return this;
    }

    public InitNode dependsOn(InitNode... newDependencies) {
        return dependsOn(Arrays.asList(newDependencies));
    }

    public void await() throws InterruptedException {
        countDownLatch.await();
    }

    protected boolean await(long value, TimeUnit unit) throws InterruptedException {
        return countDownLatch.await(value, unit);
    }

    public void cancel() {
        System.out.println("cancel: " + this);
        if (!started) {
            cancelled = true;
        }

        try {
            for (InitNode descendant : descendants) {
                System.out.println("cancel desc: " + descendant);
                if (!descendant.cancelled()) {
                    descendant.cancel();
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("unlock: "+this);
            unlock();
        }
    }

    @Override
    public void run() {
        if (success() || error()) {
            throw new IllegalStateException(String.format("%s already executed.", this.toString()));
        }
        if (this.uncaughtExceptionHandler != null) {
            Thread.currentThread().setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        }
        for (InitNode dependency : dependencies) {
            try {
                while(!dependency.await(100, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                error = e;
                cancel();
                throw new NodeExecutionError(this, e);
            }
        }

        if (cancelled()) {
            return;
        }

        try {
            started = true;
            runTask();
        } catch (Exception e) {
            error = e;
            cancel();
            throw new NodeExecutionError(this, e);
        }

        error = null;
        executed = true;
        cancelled = false;
        countDownLatch.countDown();
    }

    protected void runTask() {
        if (this.task != null) {
            this.task.run();
        }
    }

    @Override
    public String toString() {
        return "InitNode{" +
                "" + task +
                '}';
    }

    void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    void unlock() {
        countDownLatch.countDown();
    }


    InitNode newNode() {
        InitNode newInitNode = new InitNode(this.task);
        return newInitNode;
    }

    static Collection<InitNode> newNodesWithDescendants(InitNode... rootNodes) {
        return newNodesWithDescendants(Arrays.asList(rootNodes));
    }

    static Collection<InitNode> newNodesWithDescendants(Collection<InitNode> rootNodes) {
        HashMap<InitNode, InitNode> nodesList = new HashMap<>();
        for (InitNode rootNode : rootNodes) {
            newNodesWithDescendants(rootNode, rootNode.newNode(), rootNode.descendants, nodesList);
        }
        return nodesList.values();
    }

    private static void newNodesWithDescendants(InitNode node, InitNode newNode, Set<InitNode> descendants, HashMap<InitNode, InitNode> nodes) {
        if (nodes.containsKey(node)) {
            return;
        }
        nodes.put(node, newNode);
        for (InitNode descendant : descendants) {
            if (!(descendant instanceof TerminateInitNode)) {
                InitNode newDescendant = descendant.newNode();
                newDescendant.dependsOn(newNode);
                newNodesWithDescendants(descendant, newDescendant, descendant.descendants, nodes);
            }
        }
    }

    private boolean setDependencies(Collection<InitNode> dependencies) {
        return this.dependencies.addAll(dependencies);
    }

    private void setDescendantOf(Collection<InitNode> dependencies) {
        for (InitNode dependency : dependencies) {
            if (dependency.descendants == EMPTY_SET) {
                dependency.descendants = new HashSet<>();
            }
            dependency.descendants.add(this);
        }

    }

}
