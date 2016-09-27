package com.example;

import java.util.function.Predicate;

class NodeStartedPredicate implements Predicate<InitNode> {
    @Override
    public boolean test(InitNode init) {
        return (init.success() || init.cancelled()) && init.getError() == null;
    }
}
