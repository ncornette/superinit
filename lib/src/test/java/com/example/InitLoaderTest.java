package com.example;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public abstract class InitLoaderTest {

    protected List<InitNode> initNodes;
    protected TestInitNode initA;
    protected TestInitNode initB;
    protected TestInitNode initC;
    protected TestInitNode initD;
    protected TestInitNode initE;
    protected TestInitNode initF;
    protected TestInitNode initG;
    protected TestInitNode initH;
    protected AssertNodesExecutedCallback spyLoadedCallback;
    protected TestInitNode initI;

    @Before
    public void setUp() throws Exception {

        initA = new TestInitNode("A", new WaitTask(taskDelay(), "A"));
        initB = new TestInitNode("B", new WaitTask(taskDelay(), "B"));
        initD = new TestInitNode("D", new WaitTask(taskDelay(), "D"));
        initC = new TestInitNode("C", new WaitTask(taskDelay(), "C"));
        initE = new TestInitNode("E", new WaitTask(taskDelay(), "E"));
        initF = new TestInitNode("F", new WaitTask(taskDelay(), "F"));
        initG = new TestInitNode("G", new WaitTask(taskDelay(), "G"));
        initH = new TestInitNode("H", new WaitTask(taskDelay(), "H"));
        initI = new TestInitNode("I");

        initNodes = new ArrayList<>();
        initNodes.addAll(Arrays.asList(initA, initB, initC, initD, initE, initF, initG, initH, initI));

        spyLoadedCallback = spy(new AssertNodesExecutedCallback(initNodes));
    }

    protected int taskDelay() {
        return 50;
    }

    protected abstract void setupDependencies();

    @Test
    public void test_InitLoader_Task_Error() throws Exception {

        // Given
        setupDependencies();
        TestInitNode initError = new TestInitNode("ERROR", new WaitErrorTask(0, "ERROR"));
        initNodes.get(new Random().nextInt(initNodes.size())).dependsOn(initError);
        initNodes.add(initError);
        spyLoadedCallback = spy(new AssertNodesExecutedCallback(initNodes));

        // When
        InitLoader initLoader = new InitLoader(6);
        initLoader.load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onError(eq(initError), any(Throwable.class));

        initLoader.await();
        verify(spyLoadedCallback, new Times(0)).onTerminate();

        assertThat(initError.error()).isNotNull();
        assertThat(initError.success()).isFalse();

    }

    @Test
    public void test_InitLoader_CheckOrder() throws Exception {

        // Given
        setupDependencies();

        // When
        Collection<InitNode> resolved = new ArrayList<>();
        InitLoader.dep_resolve(initNodes, resolved);

        //Then
        boolean atStart = true;
        InitNode previousNode = null;
        for (InitNode initNode : resolved) {
            if (initNode.dependencies.isEmpty()) {
                if (!atStart) {
                    fail(String.format("Independent %s encountered after dependent Node %s\n" +
                            "All independent Nodes should be executed first.",
                            initNode, previousNode));
                }
            } else {
                atStart = false;
            }
            previousNode = initNode;
        }
    }

    @Test
    public void test_InitLoader_1Thread() throws Exception {

        // Given
        setupDependencies();

        // When
        new InitLoader(1).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_2Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        new InitLoader(2).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_3Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        new InitLoader(3).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_5Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        new InitLoader(5).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_9Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        new InitLoader(9).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_Reject_Circular_Dependencies() throws Exception {

        // Given
        initA.dependsOn(initB);

        try {

            // When
            initB.dependsOn(initA);
            fail("Should fail making circular dependency.");
        } catch (IllegalArgumentException e) {

            // Then
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    @Test
    public void test_InitLoader_Reject_Self_Dependency() throws Exception {

        try {

            // When
            initB.dependsOn(initB);
            fail("Should fail making circular dependency.");
        } catch (IllegalArgumentException e) {

            // Then
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    private static class AssertNodesExecutedCallback implements InitLoader.InitLoaderCallback {
        private final Collection<InitNode> initNodes;

        public AssertNodesExecutedCallback(InitNode... initNodes) {
            this(Arrays.asList(initNodes));
        }

        public AssertNodesExecutedCallback(List<InitNode> initNodes) {
            this.initNodes = initNodes;
        }

        @Override
        public void onTerminate() {
            assertThat(new NodeStartedPredicate()).acceptsAll(initNodes);
        }

        @Override
        public void onError(InitNode initNode, Throwable t) {
            //fail("error", t);
        }
    }

    private static class WaitErrorTask extends WaitTask {

        public WaitErrorTask(int millis, String name) {
            super(millis, name);
        }

        @Override
        public void run() {
            super.run();
            throw new RuntimeException("Error");
        }

    }

    private static class WaitTask implements Runnable {
        private int millis;
        private String name;

        public WaitTask(int millis, String name) {
            this.millis = millis;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                fail("Interrupted", e);
            }
        }

        @Override
        public String toString() {
            return "WaitTask{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }


    class TestInitNode extends InitNode {

        private String name;

        public TestInitNode(String name, Runnable task) {
            super(task);
            this.name = name;
        }

        public TestInitNode(String name) {
            super();
            this.name = name;
        }

        @Override
        public void run() {
            if (cancelled()) {
                return;
            }
            assertThat(finished()).overridingErrorMessage(this + " already executed").isFalse();
            super.run();
        }

        @Override
        protected void runTask() {
            for (InitNode dependency : dependencies) {
                assertThat(new NodeStartedPredicate()).accepts(dependency);
            }

            super.runTask();
        }

        @Override
        public String toString() {
            return "TestInitNode{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
