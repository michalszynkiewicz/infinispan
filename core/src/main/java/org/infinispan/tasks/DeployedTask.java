package org.infinispan.tasks;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/19/16
 * Time: 2:18 PM
 */
public interface DeployedTask<T> {
   T run();
   String getName();
}
