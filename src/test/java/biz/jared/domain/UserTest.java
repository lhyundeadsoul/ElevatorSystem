package biz.jared.domain;

import biz.jared.strategy.SameDirectionNearestFirstPriorityStrategy;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {
    User user = new User("lhy", new Floor(11));

    @Test
//    @Test(dependsOnMethods = "testSelect")
    public void testEnterElevator() throws Exception {
        System.out.println("EnterElevator");
        user.getStopwatch().start();

        Elevator elevator = new Elevator(1, new Floor(3), new SameDirectionNearestFirstPriorityStrategy());
        user.enterElevator(elevator);
    }

    //    @Test
    @Test(dependsOnMethods = "testEnterElevator")
    public void testSelect() throws Exception {
        user.select(new Floor(2));
        System.out.println("Select");
    }

    @Test
    public void testEquals() {
        User lhy = new User("lhy", new Floor(1));
        boolean equals = user.equals(lhy);
        assertThat(equals).isTrue();

        lhy = null;
        equals = user.equals(lhy);
        assertThat(equals).isFalse();

        lhy = user;
        equals = user.equals(lhy);
        assertThat(equals).isTrue();
    }
}