package biz.jared;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import biz.jared.domain.Dispatcher;
import biz.jared.domain.Elevator;
import biz.jared.domain.Floor;
import biz.jared.domain.User;
import biz.jared.strategy.DefaultStrategy;

/**
 * Hello world!
 */
public class App {
    private static final int FLOOR_NUM = 10;
    private static final int ELEVATOR_NUM = 3;
    private static final int USER_NUM = 100;

    public static void main(String[] args) {
        //generate all floor
        List<Floor> floorList = new ArrayList<>(FLOOR_NUM);
        for (int i = 0; i < FLOOR_NUM; i++) {
            Floor floor = new Floor(i + 1);
            floorList.add(floor);
        }

        //generate all elevator
        List<Elevator> elevatorList = new ArrayList<>(ELEVATOR_NUM);
        elevatorList.add(new Elevator(0, floorList.get(0)));
        elevatorList.add(new Elevator(1, floorList.get(5)));
        elevatorList.add(new Elevator(2, floorList.get(9)));

        //generate dispatcher
        Dispatcher dispatcher = new Dispatcher(elevatorList, new DefaultStrategy());

        //floor set dispatcher
        floorList.forEach(floor -> floor.setDispatcher(dispatcher));

        //generate all user
        Random random = new Random();
        for (int i = 0; i < USER_NUM; i++) {
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
