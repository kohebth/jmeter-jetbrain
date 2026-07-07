package com.github.duync.jmeterviewer;

import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JMeterRunListenerAttacherTest {
    @Test
    void attachesListenerInsideTestPlanSubtree() {
        TestPlan plan = new TestPlan("Plan");
        ThreadGroup group = new ThreadGroup();
        HashTree tree = new HashTree();
        HashTree planTree = tree.add(plan);
        planTree.add(group);
        Object listener = new Object();

        JMeterRunListenerAttacher.attach(tree, listener);

        assertTrue(Arrays.asList(planTree.getArray()).contains(listener));
        assertEquals(1, tree.getArray().length);
    }

    @Test
    void fallsBackToRootWhenNoTestPlanExists() {
        ThreadGroup group = new ThreadGroup();
        HashTree tree = new HashTree();
        tree.add(group);
        Object listener = new Object();

        JMeterRunListenerAttacher.attach(tree, listener);

        assertTrue(Arrays.asList(tree.getArray()).contains(listener));
    }
}
