[![Travis](https://img.shields.io/travis/rust-lang/rust.svg?style=plastic)](https://github.com/lhyundeadsoul/ElevatorSystem)
[![CircleCI](https://img.shields.io/circleci/project/github/RedSparr0w/node-csgo-parser.svg)](https://github.com/lhyundeadsoul/ElevatorSystem)
[![codebeat badge](https://codebeat.co/badges/8863dd20-d5c9-4191-9825-2f86a27b449c)](https://codebeat.co/projects/github-com-lhyundeadsoul-elevatorsystem-master)
![](https://img.shields.io/badge/language-java-blue.svg)
![](https://img.shields.io/github/issues/lhyundeadsoul/ElevatorSystem.svg)
![](https://img.shields.io/github/forks/lhyundeadsoul/ElevatorSystem.svg)
![](https://img.shields.io/github/stars/lhyundeadsoul/ElevatorSystem.svg)
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
- [x] sync block
- [ ] UT
- [x] CI
- [x] 全部电梯满载时会死循环
- [x] 加入吞吐、平均等待时间等指标