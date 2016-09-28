package com.example;

import java.util.function.Predicate;

class NodeExecutedPredicate implements Predicate<InitNode> {
    @Override
    public boolean test(InitNode node) {
        boolean finished = node.finished();
        return finished;
    }
}
