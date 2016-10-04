package com.ncornette.superinit;

import com.ncornette.superinit.InitLoaderTest.WaitTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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

        loaderCallback = mock(InitLoaderCallback.class);
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
        verify(runnableA, timeout(600)).run();
        verify(runnableB, timeout(600)).run();
        verify(runnableC, timeout(600)).run();

        InOrder inOrder = inOrder(runnableA, runnableB);
        inOrder.verify(runnableB).run();
        inOrder.verify(runnableA).run();

        verify(loaderCallback, timeout(600)).onFinished();
        verify(loaderCallback, never()).onError(any(Throwable.class));

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
        verify(runnableA, timeout(600)).run();
        verify(runnableB, timeout(600)).run();
        verify(runnableC, timeout(600)).run();

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
        verify(runnableB, timeout(600)).run();
        initLoader.interrupt();

        initLoader.awaitTermination();

        verify(runnableA, never()).run();
        assertThat(nodeA.getError()).isNotNull();

        verify(loaderCallback, atLeastOnce()).onError(any(InterruptedException.class));
        verify(loaderCallback, never()).onFinished();

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



}