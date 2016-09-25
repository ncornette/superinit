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

    private Node[] nodes;
    private TestNode initA;
    private TestNode initB;
    private TestNode initC;
    private TestNode initD;
    private TestNode initE;
    private TestNode initF;
    private TestNode initG;
    private TestNode initH;

    @Before
    public void setUp() throws Exception {

        initA = new TestNode("A", new Wait(500, "A"));
        initB = new TestNode("B", new Wait(500, "B"));
        initC = new TestNode("C", new Wait(500, "C"));
        initD = new TestNode("D", new Wait(500, "D"));
        initE = new TestNode("E", new Wait(500, "E"));
        initF = new TestNode("F", new Wait(500, "F"));
        initG = new TestNode("G", new Wait(500, "G"));
        initH = new TestNode("H", new Wait(500, "H"));

        nodes = new Node[] {initA, initB, initC, initD, initE, initF, initG, initH};
    }

    @Test
    public void test_InitLoader() throws Exception {

        // Given
        initA.dependsOn(initB);

        // When
        InitLoader depLoader = new InitLoader(5);

        assertInitialized(depLoader);
    }

    @Test
    public void test_InitLoader_Reject_Circular() throws Exception {

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

        // Then
        assertInitialized(new InitLoader(6));
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

        // Then
        assertInitialized(new InitLoader(10));
    }

    private void assertInitialized(InitLoader depLoader) throws InterruptedException {
        Runnable terminateCallback = spy(new TestTerminateCallback(nodes));
        depLoader.load(terminateCallback, nodes);

        //Then
        verify(terminateCallback, timeout(6000)).run();
    }

    private static class TestTerminateCallback implements Runnable {
        private final Node[] nodes;

        public TestTerminateCallback(Node... nodes) {
            this.nodes = nodes;
        }

        @Override
        public void run() {
            assertThat(new NodeStartedPredicate()).acceptsAll(Arrays.asList(nodes));
        }
    }

    private static class Wait implements Runnable {
        private int millis;
        private String name;

        public Wait(int millis, String name) {
            this.millis = millis;
            this.name = name;
        }

        @Override
        public void run() {
            System.out.printf("Running %s on %s%n", name, Thread.currentThread().getName());
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                fail("Interrupted", e);
            }
        }
    }


    class TestNode extends Node {

        private String name;

        public TestNode(String name, Runnable task) {
            super(task);
            this.name = name;
        }

        public TestNode(String name) {
            this(name, null);
        }

        @Override
        public void run() {
            assertThat(isStarted()).overridingErrorMessage(this + " already started").isFalse();
            super.run();
        }

        @Override
        protected void runTask() {
            for (Node dependency : dependencies) {
                assertThat(new NodeStartedPredicate()).accepts(dependency);
            }

            super.runTask();
        }

        @Override
        public String toString() {
            return "TestNode{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
