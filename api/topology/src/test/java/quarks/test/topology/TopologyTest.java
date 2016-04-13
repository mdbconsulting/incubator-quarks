/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package quarks.test.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import quarks.execution.Job;
import quarks.execution.mbeans.PeriodicMXBean;
import quarks.execution.services.ControlService;
import quarks.execution.services.RuntimeServices;
import quarks.function.Supplier;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.tester.Condition;

@Ignore("abstract, provides common tests for concrete implementations")
public abstract class TopologyTest extends TopologyAbstractTest {

    @Test
    public void testBasics() {
        final Topology t = newTopology("T123");
        assertEquals("T123", t.getName());
        assertSame(t, t.topology());
    }

    @Test
    public void testDefaultName() {
        final Topology t = newTopology();
        assertSame(t, t.topology());
        assertNotNull(t.getName());
    }

    @Test
    public void testStringContants() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");
        assertStream(t, s);

        Condition<Long> tc = t.getTester().tupleCount(s, 3);
        Condition<List<String>> contents = t.getTester().streamContents(s, "a", "b", "c");
        complete(t, tc);
        assertTrue(contents.valid());
    }

    @Test
    public void testNoStringContants() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings();

        Condition<Long> tc = t.getTester().tupleCount(s, 0);

        complete(t, tc);
        
        assertTrue(tc.valid());
    }
    
    @Test
    public void testRuntimeServices() throws Exception {
        Topology t = newTopology();
        TStream<String> s = t.strings("A");
        
        Supplier<RuntimeServices> serviceGetter =
                t.getRuntimeServiceSupplier();
        
        TStream<Boolean> b = s.map(tuple -> 
            serviceGetter.get().getService(ThreadFactory.class) != null
            && serviceGetter.get().getService(ScheduledExecutorService.class) != null
        );
        
        Condition<List<Boolean>> tc = t.getTester().streamContents(b, Boolean.TRUE);
        complete(t, tc);
        
        assertTrue(tc.valid());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAdaptablePoll() throws Exception {
        // Ensure the API supporting an adaptable poll() is functional.
        Job job = null;
        try {
            Topology t = newTopology();
            TStream<String> s = t.poll(() -> (new Date()).toString(),
                    1, TimeUnit.HOURS)
                    .alias("myAlias");
            
            AtomicInteger cnt = new AtomicInteger();
            s.peek(tuple -> cnt.incrementAndGet()); 
            
            Future<Job> jf = (Future<Job>) getSubmitter().submit(t);
            job = jf.get();
            assertEquals(Job.State.RUNNING, job.getCurrentState());
            
            setPollFrequency(s, 100, TimeUnit.MILLISECONDS);
            cnt.set(0);
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
            int curCnt = cnt.get();
            assertTrue("curCnt="+curCnt, curCnt >= 20);
            
            setPollFrequency(s, 1, TimeUnit.SECONDS);
            cnt.set(0);
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
            curCnt = cnt.get();
            assertTrue("curCnt="+curCnt, curCnt >= 2 && curCnt <= 4);
            
            setPollFrequency(s, 100, TimeUnit.MILLISECONDS);
            cnt.set(0);
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
            curCnt = cnt.get();
            assertTrue("curCnt="+curCnt, curCnt >= 20);
        }
        finally {
            if (job != null)
                job.stateChange(Job.Action.CLOSE);
        }
        
    }

    static <T> void setPollFrequency(TStream<T> pollStream, long period, TimeUnit unit) {
        ControlService cs = pollStream.topology().getRuntimeServiceSupplier()
                                    .get().getService(ControlService.class);
        PeriodicMXBean control = cs.getControl("periodic", pollStream.getAlias(), PeriodicMXBean.class);
        control.setPeriod(period, unit);
    }

}
