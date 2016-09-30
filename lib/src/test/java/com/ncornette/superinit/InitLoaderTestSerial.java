package com.ncornette.superinit;

import org.junit.Test;

public class InitLoaderTestSerial extends InitLoaderTest {

    @Override
    protected void setupDependencies() {

        // I
        // \--- H
        //      \--- G
        //           \--- F
        //                \--- E
        //                     \--- D
        //                          \--- C
        //                               \--- B
        //                                    \--- A

        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initC.dependsOn(initD);
        initD.dependsOn(initE);
        initE.dependsOn(initF);
        initF.dependsOn(initG);
        initG.dependsOn(initH);
        initH.dependsOn(initI);
    }

    @Override @Test
    public void test_InitNode_Run_Twice() throws Exception {
        super.test_InitNode_Run_Twice();
    }

    @Override @Test
    public void test_InitLoader_1Thread() throws Exception {
        super.test_InitLoader_1Thread();
    }

    @Override @Test
    public void test_InitLoader_2Threads() throws Exception {
        super.test_InitLoader_2Threads();
    }

    @Override @Test
    public void test_InitLoader_3Threads() throws Exception {
        super.test_InitLoader_3Threads();
    }

    @Override @Test
    public void test_InitLoader_5Threads() throws Exception {
        super.test_InitLoader_5Threads();
    }

    @Override @Test
    public void test_InitLoader_9Threads() throws Exception {
        super.test_InitLoader_9Threads();
    }

    @Override @Test
    public void test_InitLoader_Reject_Direct_Circular_Dependencies() throws Exception {
        super.test_InitLoader_Reject_Direct_Circular_Dependencies();
    }

    @Override @Test
    public void test_InitLoader_Reject_Indirect_Circular_Dependencies() throws Exception {
        super.test_InitLoader_Reject_Indirect_Circular_Dependencies();
    }

    @Override @Test
    public void test_InitLoader_Reject_Self_Dependency() throws Exception {
        super.test_InitLoader_Reject_Self_Dependency();
    }

    @Override @Test
    public void test_InitLoader_CheckOrder() throws Exception {
        super.test_InitLoader_CheckOrder();
    }

    @Override @Test
    public void test_InitLoader_Task_Error() throws Exception {
        super.test_InitLoader_Task_Error();
    }
}
