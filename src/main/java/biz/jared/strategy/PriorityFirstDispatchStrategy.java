package biz.jared.strategy;

import java.util.List;

import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

/**
 * @author jared
 * @date 2017/08/30
 */
public class PriorityFirstDispatchStrategy implements DispatchStrategy {

    @Override
    public Elevator select(List<Elevator> elevatorList, Task task) {
        int priority = Integer.MAX_VALUE;
        Elevator best = null;
        for (Elevator elevator : elevatorList) {
            int possiblePriority = elevator.tryReceive(task);
            if (priority > possiblePriority) {
                priority = possiblePriority;
                best = elevator;
            }
        }
        return best;
    }

}
