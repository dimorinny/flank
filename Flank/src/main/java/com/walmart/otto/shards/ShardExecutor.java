package com.walmart.otto.shards;

import com.walmart.otto.Constants;
import com.walmart.otto.configurator.Configurator;
import com.walmart.otto.tools.GcloudTool;
import com.walmart.otto.tools.GsUtilTool;
import com.walmart.otto.tools.ToolManager;
import com.walmart.otto.utils.FilterUtils;
import com.walmart.otto.utils.XMLUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ShardExecutor {
  private ExecutorService executorService;
  private ShardCreator shardCreator;
  private Configurator configurator;
  private ToolManager toolManager;

  public ShardExecutor(Configurator configurator, ToolManager toolManager) {
    this.configurator = configurator;
    this.toolManager = toolManager;
    shardCreator = new ShardCreator(configurator);
  }

  public int execute(List<String> testCases, String bucket)
      throws InterruptedException, ExecutionException, IOException {

    int code = executeShards(testCases, bucket);

    if (configurator.isFetchXMLFiles()) {
      fetchResults(toolManager.get(GsUtilTool.class));
    }

    return code;
  }

  private int executeShards(List<String> testCases, String bucket)
      throws InterruptedException, ExecutionException {
    List<Future<Integer>> futures = new ArrayList<>();
    List<String> shards = shardCreator.getShards(testCases);

    System.out.println(
        shards.size() + " shards will be executed on: " + configurator.getDeviceIds() + "\n");

    executorService = Executors.newFixedThreadPool(shards.size());

    for (int i = 0; i < shards.size(); i++) {
      printTests(shards.get(i), i);
      futures.add(executeShard(shards.get(i), bucket));
    }

    int resultCode = 0;

    for (Future<Integer> future : futures) {
      int code = future.get();
      if (code != 0) {
        resultCode = code;
      }
    }

    executorService.shutdown();

    return resultCode;
  }

  private Future<Integer> executeShard(String testCase, String bucket) throws RuntimeException {
    Callable<Integer> testCaseCallable = getCallable(testCase, bucket);

    return executorService.submit(testCaseCallable);
  }

  private void fetchResults(GsUtilTool gsUtilTool) throws IOException, InterruptedException {
    Map<String, String> resultsMap = gsUtilTool.fetchResults();

    //Add device name to test case names
    resultsMap.forEach(
        (filename, device) ->
            XMLUtils.updateXML(
                Constants.RESULTS_DIR + File.separator + filename, device, "testcase", "name"));
  }

  private void printTests(String testsString, int index) {
    String tests = FilterUtils.filterString(testsString, "class");
    if (tests.length() > 0 && tests.charAt(tests.length() - 1) == ',') {
      tests = tests.substring(0, tests.length() - 1);
    }
    System.out.println("Executing shard " + index + ": " + tests + "\n");
  }

  private Callable<Integer> getCallable(String testCase, String bucket) throws RuntimeException {
    return () -> {
      final GcloudTool gcloudTool = toolManager.get(GcloudTool.class);
      return gcloudTool.runInstrumentationCommand(testCase, bucket);
    };
  }
}
