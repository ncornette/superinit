package com.example;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;
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

        initA = new TestNode("A");
        initB = new TestNode("B");
        initC = new TestNode("C");
        initD = new TestNode("D");
        initE = new TestNode("E");
        initF = new TestNode("F");
        initG = new TestNode("G");
        initH = new TestNode("H");

        nodes = new Node[] {initA, initB, initC, initD, initE, initF, initG, initH};
    }

    @Test
    public void test_Loader() throws Exception {
        // Given
        initA.dependsOn(initB);

        //When
        InitLoader depLoader = new InitLoader();
        depLoader.load(nodes);
        depLoader.await();

        //Then
        assertThat(new NodeStartedPredicate()).accepts(nodes);
    }

    @Test
    public void test_Init_Depends_Circular() throws Exception {
        // Given
        initA.dependsOn(initB);

        try {
            initB.dependsOn(initA);
            fail("Should fail making circular dependency.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    @Test
    public <T> void test_Init_Depends_Multi() throws Exception {
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

        //When
        InitLoader depLoader = new InitLoader();

        Runnable terminateCallback = spy(new TestTerminateCallback(depLoader));
        depLoader.load(terminateCallback, nodes);

        verify(terminateCallback).run();

        //Then
        depLoader.await();
        assertThat(new NodeStartedPredicate()).acceptsAll(depLoader.resolved);
    }

    private static class TestTerminateCallback implements Runnable {
        private final InitLoader depLoader;

        public TestTerminateCallback(InitLoader depLoader) {
            this.depLoader = depLoader;
        }

        @Override
        public void run() {
            assertThat(new NodeStartedPredicate()).acceptsAll(depLoader.resolved);
        }
    }


    class TestNode extends Node {

        private String name;

        public TestNode(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            assertThat(isStarted()).overridingErrorMessage(this + " already started").isFalse();
            for (Node dependency : dependencies) {
                assertThat(new NodeStartedPredicate()).accepts(dependency);
            }
            super.run();
        }

        @Override
        public String toString() {
            return "TestNode{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
