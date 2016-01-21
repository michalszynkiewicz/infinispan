package org.jboss.as.clustering.infinispan.task;

import org.infinispan.factories.annotations.Inject;
import org.infinispan.tasks.Task;
import org.infinispan.tasks.TaskContext;
import org.infinispan.tasks.TaskManager;
import org.infinispan.tasks.impl.TaskManagerImpl;
import org.infinispan.tasks.spi.TaskEngine;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/20/16
 * Time: 12:32 PM
 */
public class DeployedTaskEngine implements TaskEngine {
   private final DeployedTaskManager manager;

   public DeployedTaskEngine(DeployedTaskManager manager) {
      this.manager = manager;
   }

   @Override
   public String getName() {
      return "Deployed";
   }

   @Override
   public List<Task> getTasks() {
      return manager.getTasks();
   }

   @Override
   public <T> CompletableFuture<T> runTask(String taskName, TaskContext context) {
      //noinspection unchecked                    // mstodo take a look at suppressing
      return CompletableFuture.completedFuture((T)manager.getTask(taskName).run()); // mstodo run should probably just go and the result should be wrapped here in a completable future
   }

   @Override
   public boolean handles(String taskName) {
      return manager.handles(taskName);
   }
}
