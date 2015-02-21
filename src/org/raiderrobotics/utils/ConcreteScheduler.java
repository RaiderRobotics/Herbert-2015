package org.raiderrobotics.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Author: toropov023
 * Date: 2015-02-21
 */
public class ConcreteScheduler {
    static private int taskId = 0;
    private static Map<Integer, ScheduledFuture> map = new HashMap<>();
    private static ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);

    /**
     * Schedule a runnable to be executed after a specified delay time
     * @param runnable Runnable to execute
     * @param delay Delay time in milliseconds (1 second = 1000 milliseconds)
     * @return Task id (can be used to cancel the task later on)
     */
    public static int scheduleDelayed(Runnable runnable, long delay){
        taskId++;

        map.put(taskId, service.schedule(runnable, delay, TimeUnit.MILLISECONDS));
        return taskId;
    }

    /**
     * Cancel a scheduled task given its task id
     * @param taskId Id of the task to be canceled
     * @return True is successfully canceled the task (false if invalid id, task was already executed or any exceptions caught)
     */
    public static boolean cancelTask(int taskId) {
        ScheduledFuture future = map.remove(taskId);

        return future != null && future.cancel(true);
    }
}
