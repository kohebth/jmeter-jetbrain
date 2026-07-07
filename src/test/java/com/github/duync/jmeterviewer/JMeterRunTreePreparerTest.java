package com.github.duync.jmeterviewer;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JMeterRunTreePreparerTest {
    @Test
    void addsMissingLoopControllerBeforeRun() {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("No Loop Controller");
        HashTree tree = new HashTree();
        tree.add(threadGroup);

        JMeterRunTreePreparer.prepare(tree);

        Controller controller = threadGroup.getSamplerController();
        LoopController loopController = assertInstanceOf(LoopController.class, controller);
        assertEquals(1, loopController.getLoops());
        assertEquals(1, threadGroup.getNumThreads());
        assertEquals(AbstractThreadGroup.ON_SAMPLE_ERROR_CONTINUE,
                threadGroup.getPropertyAsString(AbstractThreadGroup.ON_SAMPLE_ERROR));
    }

    @Test
    void enablesSchedulerWhenDurationIsConfigured() {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Duration Group");
        threadGroup.setDuration(1);
        HashTree tree = new HashTree();
        tree.add(threadGroup);

        JMeterRunTreePreparer.prepare(tree);

        assertTrue(threadGroup.getScheduler());
        assertEquals(1, threadGroup.getDuration());
    }
}
