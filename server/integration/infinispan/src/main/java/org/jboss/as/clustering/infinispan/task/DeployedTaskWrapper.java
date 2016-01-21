package org.jboss.as.clustering.infinispan.task;

import org.infinispan.tasks.DeployedTask;
import org.infinispan.tasks.Task;
import org.infinispan.tasks.TaskExecutionMode;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/20/16
 * Time: 2:04 PM
 */
public class DeployedTaskWrapper<T> implements Task {
   private final DeployedTask<T> task;

   public DeployedTaskWrapper(DeployedTask<T> task) {
      this.task = task;
   }

   @Override
   public String getName() {
      return task.getName();
   }

   public T run() {
      return task.run();
   }

   @Override
   public String getType() {
      return null;
   }

   @Override
   public TaskExecutionMode getExecutionMode() {
      return null;
   }
}
