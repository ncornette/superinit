package com.example;

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
}
