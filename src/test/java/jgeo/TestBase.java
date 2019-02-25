package jgeo;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TestBase {


    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("* Test starting: " + description.getMethodName());
        }

        @Override
        protected void finished(Description description) {
            System.out.println("* Test finished: " + description.getMethodName());
            System.out.println();
        }
    };
}
