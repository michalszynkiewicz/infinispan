package org.jboss.as.clustering.infinispan.task;

import org.infinispan.tasks.DeployedTask;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.clustering.infinispan.cs.factory.DeployedCacheStoreFactoryService;
import org.jboss.as.server.deployment.*;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.*;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/19/16
 * Time: 1:37 PM
 */
public class DeployableTaskProcessor implements DeploymentUnitProcessor {

   public static final String EXTERNAL_TASK = "ExternalTask";

   @Override
   public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
      DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
      Module module = deploymentUnit.getAttachment(Attachments.MODULE);
      ServicesAttachment servicesAttachment = deploymentUnit.getAttachment(Attachments.SERVICES);
      if (module != null && servicesAttachment != null)
         addServices(phaseContext, servicesAttachment, module);
   }

   private void addServices(DeploymentPhaseContext ctx, ServicesAttachment servicesAttachment, Module module) {
      List<String> implementationNames = servicesAttachment.getServiceImplementations(org.infinispan.tasks.DeployedTask.class.getName());
      ModuleClassLoader classLoader = module.getClassLoader();
      for (String serviceClassName : implementationNames) {
         try {
            Class<? extends DeployedTask> clazz = classLoader.loadClass(serviceClassName).asSubclass(org.infinispan.tasks.DeployedTask.class);
            Constructor<? extends DeployedTask> ctor = clazz.getConstructor();
            DeployedTask instance = ctor.newInstance();
            installService(ctx, serviceClassName, instance);
         } catch (Exception e) {
            InfinispanMessages.MESSAGES.unableToInstantiateClass(serviceClassName);
         }
      }
   }

   public final void installService(DeploymentPhaseContext ctx, String implementationClassName, DeployedTask instance) {
      TaskManagerService service = new TaskManagerService(implementationClassName, instance);
      ServiceName extensionServiceName = ServiceName.JBOSS.append(EXTERNAL_TASK, implementationClassName.replaceAll("\\.", "_"));
      InfinispanLogger.ROOT_LOGGER.installDeployedCacheStore(implementationClassName);
      ServiceBuilder<DeployedTask> serviceBuilder = ctx.getServiceTarget().addService(extensionServiceName, service);
      serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
      serviceBuilder.addDependency(DeployedCacheStoreFactoryService.SERVICE_NAME, DeployedTaskManager.class, service.getDeployedTaskManager());
      serviceBuilder.install();
   }

   @Override
   public void undeploy(DeploymentUnit context) {
      System.out.println("undeploy invoked"); // mstodo remove
   }

   protected static class TaskManagerService implements Service<DeployedTask> {

      protected final DeployedTask extension;
      protected final String className;
      protected InjectedValue<DeployedTaskManager> taskManager = new InjectedValue<>();

      protected TaskManagerService(String className, DeployedTask extension) {
         this.extension = extension;
         this.className = className;
      }

      @Override
      public void start(StartContext context) {
         InfinispanLogger.ROOT_LOGGER.deployedStoreStarted(className);
         taskManager.getValue().addDeployedTask(constructName(), extension);
      }

      @Override
      public void stop(StopContext context) {
         InfinispanLogger.ROOT_LOGGER.deployedStoreStopped(className);
         taskManager.getValue().removeDeployedTask(constructName());
      }

      public InjectedValue<DeployedTaskManager> getDeployedTaskManager() {
         return taskManager;
      }

      private String constructName() {
         return extension.getName();
      }

      @Override
      public DeployedTask getValue() throws IllegalStateException, IllegalArgumentException {
         return extension;
      }
   }
}
