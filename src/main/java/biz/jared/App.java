package biz.jared;

import biz.jared.domain.Dispatcher;
import biz.jared.domain.Elevator;
import biz.jared.domain.Floor;
import biz.jared.domain.User;
import biz.jared.strategy.DispatchStrategy;
import biz.jared.strategy.PriorityCalculationStrategy;
import biz.jared.strategy.PriorityFirstDispatchStrategy;
import biz.jared.strategy.SameDirectionNearestFirstPriorityStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static biz.jared.Env.ELEVATOR_NUM;
import static biz.jared.Env.FLOOR_NUM;

/**
 * App Start
 * 1）可编译，2）可运行，3）可测试，4）可读，5）可维护，6）可重用。
 * 通过自动化测试的代码只能达到第3）级
 * 而通过Code Review的代码少会在第4）级甚至更高。
 *
 * @author jared
 */
public class App {

    private static final int USER_NUM = 13;
    private static Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws InterruptedException {
        //generate all floor
        List<Floor> floorList = new ArrayList<>(FLOOR_NUM);
        for (int i = 0; i < FLOOR_NUM; i++) {
            Floor floor = new Floor(i + 1);
            floorList.add(floor);
            if (i > 0) {
                floorList.get(i - 1).next(floor);
            }
        }

        //make priority strategy
        PriorityCalculationStrategy priorityStrategy = new SameDirectionNearestFirstPriorityStrategy();

        //generate all elevator
        List<Elevator> elevatorList = new ArrayList<>(ELEVATOR_NUM);
        for (int i = 0; i < ELEVATOR_NUM; i++) {
            elevatorList.add(new Elevator(i, floorList.get(0), priorityStrategy));
        }

        //make dispatch strategy
        //DispatchStrategy dispatchStrategy = new RandomDispatchStrategy();
        DispatchStrategy dispatchStrategy = new PriorityFirstDispatchStrategy();

        //generate dispatcher
        Dispatcher dispatcher = new Dispatcher(elevatorList, dispatchStrategy);

        //elevator set dispatcher
        elevatorList.forEach(elevator -> elevator.setDispatcher(dispatcher));

        //floor set dispatcher
        floorList.forEach(floor -> floor.setDispatcher(dispatcher));

        //elevator run
        elevatorList.forEach(elevator -> new Thread(elevator, "elevator-thread-" + elevator.getId()).start());

        //simulation
//        simulation1u(floorList);
//        simulationNu(floorList);
        randomSimulate(floorList);
    }

    private static void randomSimulate(List<Floor> floorList) throws InterruptedException {
        //generate all user
        Random random = new Random();
        for (int i = 0; i < USER_NUM; i++) {
            TimeUnit.SECONDS.sleep(1);
            //站在什么楼层
            int randomSrcFloorNo = random.nextInt(FLOOR_NUM);
            Floor srcFloor = floorList.get(randomSrcFloorNo);
            //想去什么楼层
            Floor targetFloor = floorList.get(differentFloorNo(randomSrcFloorNo));
            User user = new User("lucy" + i, targetFloor);
            System.out.println("src_floorNo=" + srcFloor.getFloorNo() + " " + user);
            LOGGER.info("{} come to src_floorNo={}",user,srcFloor.getFloorNo());
            //srcFloor.locate(targetFloor).opposite()，结果Direction一定是对的，但是这里也支持传错的，也符合实际
            srcFloor.add(user, srcFloor.locate(targetFloor).opposite());
            //srcFloor.add(user, Direction.DOWN);
        }
    }

    private static void simulation1u(List<Floor> floorList) throws InterruptedException {
        Floor srcFloor = floorList.get(3);
        //想去什么楼层
        Floor targetFloor = floorList.get(2);
        User user = new User("lucy0", targetFloor);
        System.out.println("src_floorNo=" + srcFloor.getFloorNo() + " " + user);
        srcFloor.add(user, srcFloor.locate(targetFloor).opposite());
    }

    private static void simulationNu(List<Floor> floorList) throws InterruptedException {
        //user 1
        Floor srcFloor = floorList.get(3);
        //想去什么楼层
        Floor targetFloor = floorList.get(0);
        User user = new User("lucy0", targetFloor);
        System.out.println("src_floorNo=" + srcFloor.getFloorNo() + " " + user);
        srcFloor.add(user, srcFloor.locate(targetFloor).opposite());

        TimeUnit.SECONDS.sleep(1);

        //user 2
        Floor srcFloor2 = floorList.get(2);
        //想去什么楼层
        Floor targetFloor2 = floorList.get(4);
        user = new User("lucy1", targetFloor2);
        System.out.println("src_floorNo=" + srcFloor2.getFloorNo() + " " + user);
        srcFloor2.add(user, srcFloor2.locate(targetFloor2).opposite());

        TimeUnit.SECONDS.sleep(1);

        //user 3
        Floor srcFloor3 = floorList.get(1);
        //想去什么楼层
        Floor targetFloor3 = floorList.get(3);
        user = new User("lucy2", targetFloor3);
        System.out.println("src_floorNo=" + srcFloor3.getFloorNo() + " " + user);
        srcFloor3.add(user, srcFloor3.locate(targetFloor3).opposite());
    }

    /**
     * 返回一下不一样的楼层
     *
     * @param randomSrcFloorNo
     * @return
     */
    private static int differentFloorNo(int randomSrcFloorNo) {
        Random random = new Random();
        int result;
        do {
            result = random.nextInt(FLOOR_NUM);
        } while (result == randomSrcFloorNo);
        return result;
    }
}
