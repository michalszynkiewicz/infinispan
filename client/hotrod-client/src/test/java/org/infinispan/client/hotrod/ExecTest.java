package org.infinispan.client.hotrod;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.test.MultiHotRodServersTest;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.equivalence.AnyServerEquivalence;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.scripting.ScriptingManager;
import org.infinispan.stream.CacheCollectors;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

/**
 * ExecTest.
 *
 * @author Tristan Tarrant
 * @since 7.2
 */
@Test(groups = "functional", testName = "client.hotrod.ExecTest")
public class ExecTest extends MultiHotRodServersTest {
   private static final String SCRIPT_CACHE = "___script_cache";

   static final int NUM_SERVERS = 2;
   static final int SIZE = 20;

   @Override
   protected void createCacheManagers() throws Throwable {
      createHotRodServers(NUM_SERVERS, new ConfigurationBuilder());
   }

   @Test(expectedExceptions = HotRodClientException.class, expectedExceptionsMessageRegExp = ".*Unknown task 'nonExistent\\.js'.*")
   public void testRemovingNonExistentScript() {
      clients.get(0).getCache().execute("nonExistent.js", new HashMap<>());
   }

   public void testEmbeddedScriptRemoteExecution() throws IOException {
      String cacheName = "testEmbeddedScriptRemoteExecution";
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
      builder.dataContainer().keyEquivalence(new AnyServerEquivalence()).valueEquivalence(new AnyServerEquivalence()).compatibility().enable().marshaller(new GenericJBossMarshaller());
      defineInAll(cacheName, builder);
      ScriptingManager scriptingManager = manager(0).getGlobalComponentRegistry().getComponent(ScriptingManager.class);

      try (InputStream is = this.getClass().getResourceAsStream("/test.js")) {
         String script = TestingUtil.loadFileAsString(is);
         scriptingManager.addScript("testEmbeddedScriptRemoteExecution.js", script);
      }
      populateCache(cacheName);

      assertEquals(SIZE, clients.get(0).getCache(cacheName).size());
      Map<String, String> params = new HashMap<>();
      params.put("parameter", "guinness");
      Integer result = clients.get(0).getCache(cacheName).execute("testEmbeddedScriptRemoteExecution.js", params);
      assertEquals(SIZE + 1, result.intValue());
      assertEquals("guinness", clients.get(0).getCache(cacheName).get("parameter"));
   }

   public void testRemoteScriptRemoteExecution() throws IOException {
      String cacheName = "testRemoteScriptRemoteExecution";
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
      builder.dataContainer().keyEquivalence(new AnyServerEquivalence()).valueEquivalence(new AnyServerEquivalence()).compatibility().enable().marshaller(new GenericJBossMarshaller());
      defineInAll(cacheName, builder);
      try (InputStream is = this.getClass().getResourceAsStream("/test.js")) {
         String script = TestingUtil.loadFileAsString(is);
         clients.get(0).getCache(SCRIPT_CACHE).put("testRemoteScriptRemoteExecution.js", script);
      }
      populateCache(cacheName);

      assertEquals(SIZE, clients.get(0).getCache(cacheName).size());
      Map<String, String> params = new HashMap<>();
      params.put("parameter", "hoptimus prime");

      Integer result = clients.get(0).getCache(cacheName).execute("testRemoteScriptRemoteExecution.js", params);
      assertEquals(SIZE + 1, result.intValue());
      assertEquals("hoptimus prime", clients.get(0).getCache(cacheName).get("parameter"));
   }

   private void populateCache(String cacheName) {
      for (int i = 0; i < SIZE; i++)
         clients.get(i % NUM_SERVERS).getCache(cacheName).put(String.format("Key %d", i), String.format("Value %d", i));
   }

   public void testRemoteMapReduce() throws Exception {
      String cacheName = "testRemoteMapReduce";
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
      builder.dataContainer().keyEquivalence(new AnyServerEquivalence()).valueEquivalence(new AnyServerEquivalence()).compatibility().enable().marshaller(new GenericJBossMarshaller());
      defineInAll(cacheName, builder);
      RemoteCache<String, String> cache = clients.get(0).getCache(cacheName);
      RemoteCache<String, String> scriptCache = clients.get(0).getCache(SCRIPT_CACHE);
      loadData(cache, "/macbeth.txt");
      loadScript(scriptCache, "/wordCountMapper.js");
      loadScript(scriptCache, "/wordCountReducer.js");
      loadScript(scriptCache, "/wordCountCollator.js");
      Map<String, Double> results = cache.execute("wordCountMapper.js", new HashMap<String, String>());
      assertEquals(20, results.size());
      assertTrue(results.get("macbeth").equals(Double.valueOf(287)));
   }

   @Test(enabled = false, description = "Disabling this test until the distributed scripts in DIST mode are fixed - ISPN-6173")
   public void testRemoteMapReduceWithStreams() throws Exception {
      String cacheName = "testRemoteMapReduce_Streams";
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
      builder.dataContainer().keyEquivalence(new AnyServerEquivalence()).valueEquivalence(new AnyServerEquivalence()).compatibility().enable().marshaller(new GenericJBossMarshaller());
      defineInAll(cacheName, builder);
      RemoteCache<String, String> cache = clients.get(0).getCache(cacheName);
      RemoteCache<String, String> scriptCache = clients.get(0).getCache(SCRIPT_CACHE);
      loadData(cache, "/macbeth.txt");
      loadScript(scriptCache, "/wordCountStream.js");

      Map<String, Long> results = cache.execute("wordCountStream.js", new HashMap<String, String>());
      assertEquals(3209, results.size());
      assertTrue(results.get("macbeth").equals(Long.valueOf(287)));
   }

   @Test(enabled = false, description = "Disabling this test until the distributed scripts in DIST mode are fixed - ISPN-6173")
   public void testRemoteMapReduceWithStreams_DistributedMode() throws Exception {
      String cacheName = "testRemoteMapReduce_Streams_dist";
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, true);
      builder.dataContainer().keyEquivalence(new AnyServerEquivalence()).valueEquivalence(new AnyServerEquivalence())
              .compatibility().enable().marshaller(new GenericJBossMarshaller());
      defineInAll(cacheName, builder);
      waitForClusterToForm(cacheName);

      RemoteCache<String, String> cache = clients.get(0).getCache(cacheName);
      RemoteCache<String, String> scriptCache = clients.get(1).getCache(SCRIPT_CACHE);
      loadData(cache, "/macbeth.txt");
      loadScript(scriptCache, "/wordCountStream_dist.js");

      ArrayList<Map<String, Long>> results = cache.execute("wordCountStream_dist.js", new HashMap<String, String>());
      assertEquals(2, results.size());
      assertEquals(3209, results.get(0).size());
      assertEquals(3209, results.get(1).size());
      assertTrue(results.get(0).get("macbeth").equals(Long.valueOf(287)));
      assertTrue(results.get(1).get("macbeth").equals(Long.valueOf(287)));
   }

   @Test(enabled = false, description = "Disabling this test until the distributed scripts in DIST mode are fixed - ISPN-6173")
   public void testRemoteMapReduceWithStreams_DistributedMode1() throws Exception {
      String cacheName = "testRemoteMapReduce_Streams_dist1";
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
      builder.dataContainer().keyEquivalence(new AnyServerEquivalence()).valueEquivalence(new AnyServerEquivalence())
              .compatibility().enable().marshaller(new GenericJBossMarshaller());
      defineInAll(cacheName, builder);
      waitForClusterToForm(cacheName);

      RemoteCache<String, String> cache = clients.get(1).getCache(cacheName);
      RemoteCache<String, String> scriptCache = clients.get(1).getCache(SCRIPT_CACHE);
      loadData(cache, "/macbeth.txt");
      loadScript(scriptCache, "/wordCountStream_dist.js");
      waitForClusterToForm();

      ArrayList<Map<String, Long>> results = cache.execute("wordCountStream_dist.js", new HashMap<String, String>());
      assertEquals(2, results.size());
      assertEquals(3209, results.get(0).size());
      assertEquals(3209, results.get(1).size());
      assertTrue(results.get(0).get("macbeth").equals(Long.valueOf(287)));
   }

   private void loadData(BasicCache<String, String> cache, String fileName) throws IOException {
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(fileName)))) {
         int chunkSize = 10;
         int chunkId = 0;

         CharBuffer cbuf = CharBuffer.allocate(1024 * chunkSize);
         while (bufferedReader.read(cbuf) >= 0) {
            Buffer buffer = cbuf.flip();
            String textChunk = buffer.toString();
            cache.put(fileName + (chunkId++), textChunk);
            cbuf.clear();
         }
      }
   }

   private void loadScript(BasicCache<String, String> scriptCache, String fileName) throws IOException {
      try (InputStream is = this.getClass().getResourceAsStream(fileName)) {
         String script = TestingUtil.loadFileAsString(is);
         scriptCache.put(fileName.replaceAll("\\/", ""), script);
      }
   }

}
