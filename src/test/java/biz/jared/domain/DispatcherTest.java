package biz.jared.domain;

import biz.jared.Env;
import biz.jared.domain.enumeration.Direction;
import biz.jared.strategy.DispatchStrategy;
import biz.jared.strategy.PriorityFirstDispatchStrategy;
import biz.jared.strategy.SameDirectionNearestFirstPriorityStrategy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class DispatcherTest {
    private Dispatcher dispatcher;
    private Elevator e1 = new Elevator(1, new Floor(1), new SameDirectionNearestFirstPriorityStrategy());
    private Elevator e2 = new Elevator(2, new Floor(5), new SameDirectionNearestFirstPriorityStrategy());
    @BeforeMethod
    public void setUp(){
        DispatchStrategy dispatchStategy = new PriorityFirstDispatchStrategy();
        List<Elevator> elist = new ArrayList<Elevator>(){
            {
                add(e1);
                add(e2);
            }
        };
        dispatcher = new Dispatcher(elist, dispatchStategy);
    }

    @Test
    public void testDispatch() throws Exception {
        dispatcher.dispatch(Task.generate(new Floor(4), Direction.UP));
    }

//    @Test
    @Test(dependsOnMethods = "testQuit")
    public void testQuit2(){
        dispatcher.quit(e1);
        assertThat(Env.LATCH.getCount()).isZero();
    }

    @Test
    public void testQuit() throws Exception {
        dispatcher.quit(e2);
        assertThat(Env.LATCH.getCount()).isOne();
    }

}