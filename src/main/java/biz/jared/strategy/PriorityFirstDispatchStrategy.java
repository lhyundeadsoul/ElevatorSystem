package biz.jared.strategy;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

import static biz.jared.Env.MAX_LOAD;

/**
 * 能给出最好的执行优先级的电梯可获得任务
 *
 * @author jared
 * @date 2017/08/30
 */
public class PriorityFirstDispatchStrategy implements DispatchStrategy {

    @Override
    public Elevator select(List<Elevator> elevatorList, Task task) {
        if (elevatorList == null) {
            return null;
        }
        //get read lock and lock the elevatorList
        ReadWriteLock readWriteLock = null;
        if (!elevatorList.isEmpty()) {
            readWriteLock = elevatorList.get(0).getDispatcher().getReadWriteLock();
            readWriteLock.readLock().lock();
        }
        //select the elevator which has the best priority
        int priority = Integer.MAX_VALUE;
        Elevator best = null;
        for (Elevator elevator : elevatorList) {
            if (elevator.getCurrLoad().size() == MAX_LOAD) {
                continue;
            }
            int possiblePriority = elevator.tryReceive(task);
            if (priority > possiblePriority) {
                priority = possiblePriority;
                best = elevator;
            }
        }
        if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
        }
        return best;
    }

}
