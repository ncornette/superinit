package com.ncornette.superinit;

import com.ncornette.superinit.InitLoaderTest.WaitTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class InitNodeTest {

    private Runnable runnableA;
    private Runnable runnableB;
    private Runnable runnableC;
    private InitLoaderCallback loaderCallback;
    private InitLoader initLoader;

    @Before
    public void setUp() throws Exception {

        runnableA = spy(new WaitTask("A", 40));
        runnableB = spy(new WaitTask("B", 40));
        runnableC = spy(new WaitTask("C", 40));

        loaderCallback = spy(new LogInitLoaderCallback());
    }

    @After
    public void tearDown() throws Exception {
        if (initLoader == null) {
            System.out.println("No initLoader for this test");
            return;
        }

        initLoader.awaitTermination();
        for (InitNode initNode : initLoader.resolved) {
            System.out.println(String.format("Result for %s: %s", initNode,
                    initNode.success() ? "Success" :
                            initNode.cancelled() ? "Cancelled" :
                                    initNode.error() ? "Error: "+ initNode.getError().getMessage() :
                                            "Not Executed."));

        }

    }

    @Test
    public void test_InitNode() throws Exception {

        // Given
        InitNode nodeA = new InitNode(runnableA);
        InitNode nodeB = new InitNode(runnableB);
        InitNode nodeC = new InitNode(runnableC);

        nodeA.dependsOn(nodeB);

        // When
        initLoader = new InitLoader(3);
        initLoader.load(loaderCallback, nodeA, nodeB, nodeC);

        // Then
        verify(runnableA, timeout(600).times(1)).run();
        verify(runnableB, timeout(600).times(1)).run();
        verify(runnableC, timeout(600).times(1)).run();

        InOrder inOrder = inOrder(runnableA, runnableB);
        inOrder.verify(runnableB).run();
        inOrder.verify(runnableA).run();

        verify(loaderCallback, timeout(600).times(1)).onFinished();
        verify(loaderCallback, timeout(600).times(0)).onError(any(Throwable.class));
        verify(loaderCallback, timeout(600).times(0)).onNodeError(any(NodeExecutionError.class));

    }

    @Test
    public void test_InitNode_Await() throws Exception {

        // Given
        InitNode nodeA = new InitNode(runnableA);
        InitNode nodeB = new InitNode(runnableB);
        nodeA.dependsOn(nodeB);

        // When
        initLoader = new InitLoader(3);
        initLoader.load(loaderCallback, nodeA, nodeB);

        // Then
        verify(runnableA, times(0)).run();
        nodeA.await();
        verify(runnableA, times(1)).run();
    }

    @Test
    public void test_InitNode_NullCallback_Allowed() throws Exception {

        // Given

        // Define nodes
        InitNode nodeA = new InitNode(runnableA);
        InitNode nodeB = new InitNode(runnableB);
        InitNode nodeC = new InitNode(runnableC);

        // Define dependencies
        nodeA.dependsOn(nodeB);

        // When

        // Load tasks
        initLoader = new InitLoader(3);
        initLoader.load(null, nodeA, nodeB, nodeC);

        // Then
        verify(runnableA, timeout(600).times(1)).run();
        verify(runnableB, timeout(600).times(1)).run();
        verify(runnableC, timeout(600).times(1)).run();

        InOrder inOrder = inOrder(runnableA, runnableB);
        inOrder.verify(runnableB).run();
        inOrder.verify(runnableA).run();
    }

    @Test
    public void test_InitNode_Interrupted() throws Exception {

        // Given

        // Define nodes

        InitNode nodeA = new InitNode(runnableA);
        InitNode nodeB = new InitNode(runnableB);
        InitNode nodeC = new InitNode(runnableC);

        // Define dependencies
        nodeA.dependsOn(nodeB);

        // When

        // Load tasks
        initLoader = new InitLoader(3);
        initLoader.load(loaderCallback, nodeA, nodeB, nodeC);

        // Then
        verify(runnableB, timeout(600).times(1)).run();
        initLoader.interrupt();

        initLoader.awaitTermination();

        verify(runnableA, timeout(600).times(0)).run();
        assertThat(nodeA.getError()).isNotNull();

        verify(loaderCallback, timeout(600).times(0)).onFinished();
        verify(loaderCallback, timeout(600).times(0)).onError(any(InterruptedException.class));
        verify(loaderCallback, timeout(600).times(3)).onNodeError(any(NodeExecutionError.class));
    }

    static ArgumentMatcher<NodeExecutionError> nodeExecutionError(final InitNode errorNode) {
        return new ArgumentMatcher<NodeExecutionError>() {
            @Override
            public boolean matches(Object argument) {
                if (argument instanceof NodeExecutionError) {
                    NodeExecutionError nodeExecutionError = (NodeExecutionError) argument;
                    if (nodeExecutionError.getCause().getClass() == errorNode.getError().getClass()
                            && nodeExecutionError.getCause().getMessage() == errorNode.getError().getMessage()
                            && nodeExecutionError.node().task() == errorNode.task()) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Test
    public void test_InitNode_Load_Twice() throws Exception {

        // Given
        initLoader = new InitLoader(2);
        InitNode node = new InitNode();
        initLoader.load(null, node);

        try {

            // When
            initLoader.load(null, node);
            fail("Expected failure when trying to load twice");
        } catch (IllegalStateException e) {

            // Then
            assertThat(e.getMessage()).isNotEmpty();
        }
    }


    @Test
    public void test_InitNode_Run_Twice() throws Exception {

        // When
        InitNode node = new InitNode();
        node.run();

        try {

            // When
            node.run();
            fail("Expected failure when trying to run node twice");
        } catch (IllegalStateException e) {

            // Then
            assertThat(e.getMessage()).isNotEmpty();
        }
    }


    @Test
    public void test_Retry() throws Exception {

        // Given

        // Define nodes

        InitNode nodeA = new InitNode(runnableA);
        InitNode nodeB = new InitNode(runnableB);
        InitNode nodeC = new InitNode(runnableC);
        InitNode nodeError = new InitNode(new InitLoaderTest.WaitTaskError(200, "Error", 2));

        // Define dependencies
        nodeA.dependsOn(nodeB);
        nodeB.dependsOn(nodeError);

        // When

        // Load tasks
        initLoader = new InitLoader(3);
        initLoader.load(loaderCallback, nodeA, nodeB, nodeC);

        // Then
        initLoader.awaitTasks();
        verify(runnableC, timeout(600)).run();

        verify(loaderCallback, timeout(600).times(1)).onNodeError(argThat(nodeExecutionError(nodeError)));
        verify(loaderCallback, timeout(600).times(1)).onFinished();
        verify(runnableA, timeout(600).times(0)).run();

        // First retry
        initLoader.retry();
        initLoader.awaitTasks();

        verify(loaderCallback, timeout(600).times(2)).onNodeError(argThat(nodeExecutionError(nodeError)));
        verify(loaderCallback, timeout(600).times(2)).onFinished();
        verify(runnableA, never()).run();

        // Second Retry with new Callback
        loaderCallback = mock(InitLoaderCallback.class);
        initLoader.retry(loaderCallback);
        initLoader.awaitTermination();

        verify(loaderCallback, timeout(600).times(1)).onFinished();
        verify(loaderCallback, timeout(600).times(0)).onNodeError(any(NodeExecutionError.class));
        verify(loaderCallback, timeout(600).times(0)).onError(any(Throwable.class));
        verify(runnableA, timeout(600).times(1)).run();

        // Retry on terminated loader
        try {
            initLoader.retry();
            fail("Should fail, calling retry on terminated or shutdown InitLoader");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isNotEmpty();
        }
    }


    private class LogInitLoaderCallback implements InitLoaderCallback {

        @Override
        public void onFinished() {
            System.err.println("finished");
        }

        @Override
        public void onNodeError(NodeExecutionError nodeError) {
            nodeError.printStackTrace();
        }

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
        }
    }
}