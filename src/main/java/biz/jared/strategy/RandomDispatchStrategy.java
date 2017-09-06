package biz.jared.strategy;

import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

import java.util.List;
import java.util.Random;

/**
 * @author jared
 * @date 2017/08/30
 */
public class RandomDispatchStrategy implements DispatchStrategy {
    private Random random = new Random();

    @Override
    public Elevator select(List<Elevator> elevatorList, Task task) {
        return elevatorList.get(random.nextInt(elevatorList.size()));
    }

}
