package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;
import biz.jared.strategy.DispatchStrategy;
import biz.jared.strategy.PriorityFirstDispatchStrategy;
import biz.jared.strategy.SameDirectionNearestFirstPriorityStrategy;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FloorTest {

    private Floor floor = new Floor(1);
    private Elevator e1 = new Elevator(1, new Floor(1), new SameDirectionNearestFirstPriorityStrategy());
    private Elevator e2 = new Elevator(2, new Floor(5), new SameDirectionNearestFirstPriorityStrategy());

    DispatchStrategy dispatchStategy = new PriorityFirstDispatchStrategy();
    List<Elevator> elist = new ArrayList<Elevator>() {
        {
            add(e1);
            add(e2);
        }
    };
    Dispatcher dispatcher = new Dispatcher(elist, dispatchStategy);

    @BeforeClass
    public void init() {
        floor.setDispatcher(dispatcher);
        floor.add(new User("lhy", new Floor(3)), Direction.UP);
        floor.add(new User("lhy", new Floor(6)), Direction.UP);
    }

    @Test
    public void testReduce() throws Exception {
        Set<User> reduce = floor.reduce(Direction.UP, 1);
        assertThat(reduce).hasSize(1);
    }

    @Test(dependsOnMethods = "testReduce")
    public void testCancel() throws Exception {
        floor.cancel(Direction.UP);
        Set<User> reduce = floor.reduce(Direction.UP, 1);
        assertThat(reduce).hasSize(0);
    }

    @Test
    public void testLocate() throws Exception {
        Direction locate = floor.locate(new Floor(12));
        assertThat(locate).isEqualByComparingTo(Direction.DOWN);
    }

    @AfterMethod
    public void after() {
        System.out.println("after");
    }

}