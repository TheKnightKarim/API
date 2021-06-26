package com.envyful.api.forge.concurrency.listener;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 *
 * Simple listener class for running tasks on the minecraft thread.
 *
 */
public class ServerTickListener {

    private final List<Runnable> tasks = Lists.newArrayList();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        for (Runnable task : this.tasks) {
            task.run();
        }

        this.tasks.clear();
    }

    public void addTask(Runnable runnable) {
        this.tasks.add(runnable);
    }
}
