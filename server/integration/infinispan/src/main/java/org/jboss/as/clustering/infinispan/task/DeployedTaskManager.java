package org.jboss.as.clustering.infinispan.task;

import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.tasks.DeployedTask;
import org.infinispan.tasks.Task;
import org.infinispan.tasks.TaskManager;
import org.infinispan.tasks.impl.TaskManagerImpl;

import java.util.*;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/20/16
 * Time: 12:53 PM
 */
@Scope(Scopes.GLOBAL)
public class DeployedTaskManager {
   private Map<String, DeployedTaskWrapper> tasks = new HashMap<>();

   @Inject
   public void init(TaskManager taskManager) {
      DeployedTaskEngine engine = new DeployedTaskEngine(this);                                         // mstodo this might require another proxy/intermediary object
      ((TaskManagerImpl) taskManager).registerTaskEngine(engine);
   }

   public List<Task> getTasks() {
      Collection<DeployedTaskWrapper> tasks = this.tasks.values();
      return new ArrayList<>(tasks);
   }

   public <T>DeployedTaskWrapper getTask(String taskName) {
      //noinspection unchecked
      return (DeployedTaskWrapper<T>) tasks.get(taskName);
   }

   public boolean handles(String taskName) {
      return tasks.containsKey(taskName);
   }

   public <T> void addDeployedTask(String name, DeployedTask<T> task) {
      DeployedTaskWrapper taskWrapper = new DeployedTaskWrapper<T>(task);
      tasks.put(name, taskWrapper);
   }

   public void removeDeployedTask(String name) {
      tasks.remove(name);
   }
}
