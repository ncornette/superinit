package com.example;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class InitLoaderTest {

    protected List<InitNode> initNodes;
    private TestInitNode initA;
    private TestInitNode initB;
    private TestInitNode initC;
    private TestInitNode initD;
    private TestInitNode initE;
    private TestInitNode initF;
    private TestInitNode initG;
    private TestInitNode initH;
    private AssertNodesExecutedCallback spyLoadedCallback;

    @Before
    public void setUp() throws Exception {

        initA = new TestInitNode("A", new WaitTask(50, "A"));
        initB = new TestInitNode("B", new WaitTask(50, "B"));
        initD = new TestInitNode("D", new WaitTask(50, "D"));
        initC = new TestInitNode("C", new WaitTask(50, "C"));
        initE = new TestInitNode("E", new WaitTask(50, "E"));
        initF = new TestInitNode("F", new WaitTask(50, "F"));
        initG = new TestInitNode("G", new WaitTask(50, "G"));
        initH = new TestInitNode("H", new WaitTask(50, "H"));

        initNodes = Arrays.asList(initA, initB, initC, initD, initE, initF, initG, initH);
        Collections.shuffle(initNodes);

        spyLoadedCallback = spy(new AssertNodesExecutedCallback(initNodes));
    }

    @Test
    public void test_InitLoader() throws Exception {

        // When
        new InitLoader(6).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_Task_Error() throws Exception {

        for (int i = 0; i < 100; i++) {
            // Given
            TestInitNode initError = new TestInitNode("ERROR", new WaitErrorTask(0, "ERROR"));
            TestInitNode initNode3 = new TestInitNode("TASK3", new WaitTask(0, "TASK3"));
            TestInitNode initNode2 = new TestInitNode("TASK2", new WaitTask(0, "TASK2"));
            TestInitNode initNode1 = new TestInitNode("TASK1", new WaitTask(0, "TASK1"));
            List<InitNode> nodes = Arrays.asList(initError, initNode1, initNode2, initNode3);

            Collections.shuffle(nodes);

            spyLoadedCallback = spy(new AssertNodesExecutedCallback(nodes));

            initNode1.dependsOn(initError);

            // When
            InitLoader initLoader = new InitLoader(6);
            initLoader.load(spyLoadedCallback, initNode1, initError, initNode2, initNode3);

            //Then
            verify(spyLoadedCallback, timeout(6000)).onError(eq(initError), any(Throwable.class));

            initLoader.await();
            verify(spyLoadedCallback, new Times(0)).onTerminate();

            assertThat(initError.error()).isNotNull();
            assertThat(initError.success()).isFalse();

            assertThat(initNode1.error()).isNull();
            assertThat(initNode1.success()).isFalse();


            assertThat(initNode2.error()).isNull();
            assertThat(initNode2.success() || initNode2.cancelled()).isTrue();

            assertThat(initNode3.error()).isNull();
            assertThat(initNode3.success() || initNode3.cancelled()).isTrue();
        }
    }

    @Test
    public void test_InitLoader_Async_CheckOrder() throws Exception {
        // +--- C
        // |    +--- D
        // |    \--- B
        // |         +--- A
        // |         \--- F
        // +--- E
        // +--- G
        // +--- H

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initD.dependsOn(initC);
        initF.dependsOn(initB);


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
    public void test_InitLoader_Async_NThreads() throws Exception {
        // +--- C
        // |    +--- D
        // |    \--- B
        // |         +--- A
        // |         \--- F
        // +--- E
        // +--- G
        // +--- H

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initD.dependsOn(initC);
        initF.dependsOn(initB);


        // When
        new InitLoader(3).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_Async_OneThread() throws Exception {
        // +--- C
        // |    +--- D
        // |    \--- B
        // |         +--- A
        // |         \--- F
        // +--- E
        // +--- G
        // +--- H

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initD.dependsOn(initC);
        initF.dependsOn(initB);


        // When
        new InitLoader(1).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_Async_Serial_NThreads() throws Exception {

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initC.dependsOn(initD);
        initD.dependsOn(initE);
        initE.dependsOn(initF);
        initF.dependsOn(initG);
        initG.dependsOn(initH);

        // When
        new InitLoader(3).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).onTerminate();
    }

    @Test
    public void test_InitLoader_Async_Serial_OneThread() throws Exception {

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initC.dependsOn(initD);
        initD.dependsOn(initE);
        initE.dependsOn(initF);
        initF.dependsOn(initG);
        initG.dependsOn(initH);

        // When
        new InitLoader(1).load(spyLoadedCallback, initNodes);

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
            this(name, null);
        }

        @Override
        public void run() {
            if (cancelled()) {
                return;
            }
            assertThat(finished()).overridingErrorMessage(this + " already started").isFalse();
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
