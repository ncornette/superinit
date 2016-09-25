package com.example;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class InitLoaderTest {

    private InitNode[] initNodes;
    private TestInitNode initA;
    private TestInitNode initB;
    private TestInitNode initC;
    private TestInitNode initD;
    private TestInitNode initE;
    private TestInitNode initF;
    private TestInitNode initG;
    private TestInitNode initH;
    private assertNodesExecutedCallback spyLoadedCallback;

    @Before
    public void setUp() throws Exception {

        initA = new TestInitNode("A", new WaitTask(50, "A"));
        initB = new TestInitNode("B", new WaitTask(50, "B"));
        initC = new TestInitNode("C", new WaitTask(50, "C"));
        initD = new TestInitNode("D", new WaitTask(50, "D"));
        initE = new TestInitNode("E", new WaitTask(50, "E"));
        initF = new TestInitNode("F", new WaitTask(50, "F"));
        initG = new TestInitNode("G", new WaitTask(50, "G"));
        initH = new TestInitNode("H", new WaitTask(50, "H"));

        initNodes = new InitNode[] {initA, initB, initC, initD, initE, initF, initG, initH};

        spyLoadedCallback = spy(new assertNodesExecutedCallback(initNodes));
    }

    @Test
    public void test_InitLoader() throws Exception {

        // When
        new InitLoader(6).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).run();
    }

    @Test
    public void test_InitLoader_Async() throws Exception {

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initD.dependsOn(initC);
        initF.dependsOn(initB);

        // +--- C
        // |    +--- D
        // |    \--- B
        // |         +--- A
        // |         \--- F
        // +--- E
        // +--- G
        // +--- H

        // When
        new InitLoader(6).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).run();
    }

    @Test
    public void test_InitLoader_Async_OneThread() throws Exception {

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initD.dependsOn(initC);
        initF.dependsOn(initB);

        // +--- C
        // |    +--- D
        // |    \--- B
        // |         +--- A
        // |         \--- F
        // +--- E
        // +--- G
        // +--- H

        // When
        new InitLoader(1).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).run();
    }

    @Test
    public void test_InitLoader_Async_Serial() throws Exception {

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initC.dependsOn(initD);
        initD.dependsOn(initE);
        initE.dependsOn(initF);
        initF.dependsOn(initG);
        initG.dependsOn(initH);

        // When
        new InitLoader(6).load(spyLoadedCallback, initNodes);

        //Then
        verify(spyLoadedCallback, timeout(6000)).run();
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

    private static class assertNodesExecutedCallback implements Runnable {
        private final InitNode[] initNodes;

        public assertNodesExecutedCallback(InitNode... initNodes) {
            this.initNodes = initNodes;
        }

        @Override
        public void run() {
            assertThat(new NodeStartedPredicate()).acceptsAll(Arrays.asList(initNodes));
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
            assertThat(isStarted()).overridingErrorMessage(this + " already started").isFalse();
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
