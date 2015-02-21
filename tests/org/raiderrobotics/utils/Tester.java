package org.raiderrobotics.utils;

/**
 * Author: toropov023
 * Date: 2015-02-21
 */
public class Tester {

    public static void main(String[] args){
        System.out.print("Testing scheduler: " + testScheduler());
    }

    static boolean testScheduler(){
        System.out.print("Scheduling delayed task!");

        int task;
        task = ConcreteScheduler.scheduleDelayed(() -> {
            System.out.println("Delayed task that never runs");
        }, 3 * 1000L);

        ConcreteScheduler.scheduleDelayed(() -> {
            System.out.println("Canceling the task with output: " + ConcreteScheduler.cancelTask(task));
        }, 2 * 1000L);

        return true;
    }
}
