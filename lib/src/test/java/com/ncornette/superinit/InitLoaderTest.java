package com.ncornette.superinit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ncornette.superinit.InitNodeTest.nodeExecutionError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public abstract class InitLoaderTest {

    private static final long VERIFY_TIMEOUT = 3000;
    List<TestInitNode> initNodes;
    TestInitNode initA;
    TestInitNode initB;
    TestInitNode initC;
    TestInitNode initD;
    TestInitNode initE;
    TestInitNode initF;
    TestInitNode initG;
    TestInitNode initH;
    TestInitNode initI;
    private InitLoaderCallback spyLoadedCallback;
    private InitLoader initLoader;

    @Before
    public void setUp() throws Exception {

        initA = new TestInitNode(new WaitTask("A", taskDelay()));
        initB = new TestInitNode(new WaitTask("B", taskDelay()));
        initD = new TestInitNode(new WaitTask("D", taskDelay()));
        initC = new TestInitNode(new WaitTask("C", taskDelay()));
        initE = new TestInitNode(new WaitTask("E", taskDelay()));
        initF = new TestInitNode(new WaitTask("F", taskDelay()));
        initG = new TestInitNode(new WaitTask("G", taskDelay()));
        initH = new TestInitNode(new WaitTask("H", taskDelay()));
        initI = new TestInitNode(new WaitTask("I", 0));

        initNodes = new ArrayList<>();
        initNodes.addAll(Arrays.asList(initA, initB, initC, initD, initE, initF, initG, initH, initI));

        spyLoadedCallback = spy(new AssertNodesExecutedCallback(initNodes));
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
                    initNode.error() ? "Error: "+ initNode.getError().getMessage() :
                            initNode.cancelled() ? "Cancelled" :
                                    initNode.success() ? "Success" :
                                            "Not Executed."));

        }
    }

    protected int taskDelay() {
        return 50;
    }

    protected abstract void setupDependencies();

    @Test
    public void test_InitNode_Cancel() throws Exception {

        // Given
        setupDependencies();

        // When
        initLoader = new InitLoader(1);
        // Need at least one delayed task.
        TestInitNode wait = new TestInitNode(new WaitTask("Wait", 20));
        initNodes.get(0).dependsOn(wait);
        initNodes.add(wait);

        initLoader.load(spyLoadedCallback, initNodes);
        initLoader.cancel();
        initLoader.awaitTermination();

        // Then
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(0)).onError((Throwable) anyObject());
    }


    @Test
    public void test_InitLoader_1Thread() throws Exception {

        // Given
        setupDependencies();

        // When
        initLoader = new InitLoader(1);
        initLoader.load(spyLoadedCallback, initNodes);

        // Then
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
    }

    @Test
    public void test_InitLoader_2Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        initLoader = new InitLoader(2);
        initLoader.load(spyLoadedCallback, initNodes);

        // Then
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
    }

    @Test
    public void test_InitLoader_3Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        initLoader = new InitLoader(3);
        initLoader.load(spyLoadedCallback, initNodes);

        // Then
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
    }

    @Test
    public void test_InitLoader_5Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        initLoader = new InitLoader(5);
        initLoader.load(spyLoadedCallback, initNodes);

        // Then
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
    }

    @Test
    public void test_InitLoader_9Threads() throws Exception {

        // Given
        setupDependencies();

        // When
        initLoader = new InitLoader(9);
        initLoader.load(spyLoadedCallback, initNodes);

        // Then
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
    }

    @Test
    public void test_InitLoader_Reject_Direct_Circular_Dependencies() throws Exception {

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
    public void test_InitLoader_Reject_Indirect_Circular_Dependencies() throws Exception {

        // Given
        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initC.dependsOn(initA);

        try {

            // When
            initLoader = new InitLoader(6);
            initLoader.load(spyLoadedCallback, initNodes);
            fail("Should fail with circular dependency.");
        } catch (IllegalArgumentException e) {

            // Then
            System.out.println(e.getMessage());
            assertThat(e.getMessage()).isNotEmpty();
            initLoader.cancel();
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

    @Test
    public void test_InitLoader_CheckOrder() throws Exception {

        // Given
        setupDependencies();

        // When
        Collection<InitNode> resolved = new ArrayList<>();
        InitLoader.dep_resolve(initNodes, resolved);

        // Then
        boolean atStart = true;
        InitNode previousNode = null;
        for (InitNode initNode : resolved) {
            if (initNode.dependencies().isEmpty()) {
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
    public void test_InitLoader_Task_Error() throws Exception {

        // Given
        setupDependencies();
        WaitTaskError errorTask = new WaitTaskError(100, "ERROR");
        TestInitNode errorNode = new TestInitNode(errorTask);
        initA.dependsOn(errorNode);
        initI.dependsOn(errorNode);
        initNodes.add(errorNode);

        // When
        initLoader = new InitLoader(6);
        initLoader.load(spyLoadedCallback, initNodes);

        // Then
        assertOnErrorDescendantsCancelled(initLoader, errorNode);

        Collection<InitNode> errorTreeNodes = errorNode.newNodesWithDescendants();
        assertThat(errorTreeNodes).extracting("task").contains(
                errorTask, new WaitTask("A", taskDelay()), new WaitTask("I", 0));
    }

    private void assertOnErrorDescendantsCancelled(InitLoader initLoader, InitNode errorNode) throws InterruptedException {

        initLoader.awaitTermination();

        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onFinished();
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(0)).onError(any(Throwable.class));
        verify(spyLoadedCallback, timeout(VERIFY_TIMEOUT).times(1)).onNodeError(argThat(nodeExecutionError(errorNode)));

        assertThat(errorNode.error()).isTrue();
        assertThat(errorNode.cancelled()).isFalse();
        assertThat(errorNode.success()).isFalse();

        List<InitNode> cancelledNodes = new ArrayList<>();
        getAllDescendants(errorNode, cancelledNodes);

        ArrayList<TestInitNode> successNodes = new ArrayList<>(initNodes);
        successNodes.removeAll(cancelledNodes);
        successNodes.remove(errorNode);

        for (InitNode cancelledNode : cancelledNodes) {
            if (cancelledNode instanceof TerminateInitNode) {
                continue;
            }
            assertThat(cancelledNode.finished()).isTrue();
            assertThat(cancelledNode.cancelled()).isTrue();
        }

        for (InitNode successNode : successNodes) {
            assertThat(successNode.finished()).isTrue();
            assertThat(successNode.success()).isTrue();
        }
    }

    static void getAllDescendants(InitNode node, List<InitNode> descendants) {
        for (InitNode descendant : node.descendants()) {
            descendants.add(descendant);
            getAllDescendants(descendant, descendants);
        }
    }

    private static class AssertNodesExecutedCallback implements InitLoaderCallback {
        private final List<? extends InitNode> initNodes;

        public AssertNodesExecutedCallback(List<? extends InitNode> initNodes1) {
            initNodes = initNodes1;
        }

        @Override
        public void onFinished() {
            for (InitNode initNode : initNodes) {
                assertThat(initNode.finished() || initNode.cancelled()).isTrue();
            }
            System.out.println("---> InitLoaderCallback: onFinished()");
        }

        @Override
        public void onNodeError(NodeExecutionError t) {
            System.out.println("---> InitLoaderCallback: onNodeError()");
            System.out.print("---> "); t.printStackTrace(System.out);
        }

        @Override
        public void onError(Throwable t) {
            System.out.println("---> InitLoaderCallback: onError()");
            System.out.print("---> "); t.printStackTrace(System.out);
        }
    }

    static class WaitTaskError extends WaitTask {

        private AtomicInteger countDown = new AtomicInteger();

        WaitTaskError(int millis, String name) {
            this(millis, name, Integer.MAX_VALUE);
        }

        WaitTaskError(int millis, String name, int countDown) {
            super(name, millis);
            this.countDown.set(countDown);
        }

        @Override
        public void run() {
            super.run();
            if (countDown.getAndDecrement() > 0) {
                throw new RuntimeException("This task throws a RuntimeException");
            }
        }

    }

    static class WaitTask implements Runnable {

        private String name;
        private int millis;

        WaitTask(String name, int millis) {
            this.millis = millis;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            }
        }

        @Override
        public String toString() {
            return "WaitTask{" +
                    "'" + name + '\'' +
                    ", " + millis +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WaitTask waitTask = (WaitTask) o;

            if (millis != waitTask.millis) return false;
            return name.equals(waitTask.name);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + millis;
            return result;
        }
    }

    class TestInitNode extends InitNode {

        private String name;

        TestInitNode(WaitTask task) {
            super(task);
            this.name = task.name;
        }

        TestInitNode(String name, Runnable task) {
            super(task);
            this.name = name;
        }


        TestInitNode(String name) {
            super();
            this.name = name;
        }

        @Override
        protected void runTask() {
            //System.out.printf("%s running on Thread: %s%n", this, Thread.currentThread());
            for (InitNode initNode : dependencies()) {
                assertThat(initNode.finished()).isTrue();
            }

            super.runTask();
        }

        @Override
        public String toString() {
            return "Test" + super.toString();
        }

    }
}
