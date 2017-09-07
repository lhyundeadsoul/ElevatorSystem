# ElevatorSystem
a Multi-elevator System

## Design
![arch](src/main/resources/ElevatorSystem.png)


## Feature
1. support multi-task/multi-elevator dispatch
2. support elevator maximum load
3. support most of operations of elevator e.g. grab/cancel/abandon
4. support dispatch strategy extensible endpoint

## Legacy Problem
- [x] 电梯来了，和用户方向不同，用户也不能上
- [x] dispatcher任务分配优先级还没排查
- [x] 电梯任务抢占
- [x] todo
- [ ] sync block
- [ ] UT
- [ ] CI
- [ ] 全部电梯满载时会死循环