package biz.jared;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import biz.jared.domain.Dispatcher;
import biz.jared.domain.Elevator;
import biz.jared.domain.Floor;
import biz.jared.domain.User;
import biz.jared.strategy.RandomDispatchStrategy;

/**
 * Hello world!
 */
public class App {
    private static final int FLOOR_NUM = 5;
    private static final int ELEVATOR_NUM = 2;
    private static final int USER_NUM = 3;

    public static void main(String[] args) throws InterruptedException {
        //generate all floor
        List<Floor> floorList = new ArrayList<>(FLOOR_NUM);
        for (int i = 0; i < FLOOR_NUM; i++) {
            Floor floor = new Floor(i + 1);
            floorList.add(floor);
            if (i > 0) {
                floor.previous(floorList.get(i - 1));
                floorList.get(i - 1).next(floorList.get(i - 1));
            }
        }

        //generate all elevator
        List<Elevator> elevatorList = new ArrayList<>(ELEVATOR_NUM);
        for (int i = 0; i < ELEVATOR_NUM; i++) {
            elevatorList.add(new Elevator(i, floorList.get(0)));
        }

        //generate dispatcher
        Dispatcher dispatcher = new Dispatcher(elevatorList, new RandomDispatchStrategy());

        //elevator set dispatcher
        elevatorList.forEach(elevator -> elevator.setDispatcher(dispatcher));

        //floor set dispatcher
        floorList.forEach(floor -> floor.setDispatcher(dispatcher));

        //elevator run
        elevatorList.forEach(elevator -> new Thread(elevator, "elevator-thread-" + elevator.getId()).start());

        //generate all user
        Random random = new Random();
        for (int i = 0; i < USER_NUM; i++) {
            TimeUnit.SECONDS.sleep(2);
            //站在什么楼层
            Floor srcFloor = floorList.get(random.nextInt(FLOOR_NUM));
            //想去什么楼层
            Floor targetFloor = floorList.get(random.nextInt(FLOOR_NUM));

            User user = new User("lucy" + i, targetFloor);
            //srcFloor.locate(targetFloor).opposite()，结果Direction一定是对的，但是这里也支持传错的，也符合实际
            srcFloor.add(user, srcFloor.locate(targetFloor).opposite());
            //srcFloor.add(user, Direction.DOWN);
        }
    }
}
