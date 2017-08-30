package com.walmart.otto;

import com.linkedin.dex.parser.DexParser;
import com.linkedin.dex.parser.TestMethod;
import com.walmart.otto.configurator.ConfigReader;
import com.walmart.otto.configurator.Configurator;
import com.walmart.otto.shards.ShardExecutor;
import com.walmart.otto.tools.GcloudTool;
import com.walmart.otto.tools.GsUtilTool;
import com.walmart.otto.tools.ProcessExecutor;
import com.walmart.otto.tools.ToolManager;
import com.walmart.otto.utils.FileUtils;
import com.walmart.otto.utils.FilterUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Flank {
  private ToolManager toolManager;
  private Configurator configurator;

  public void start(String[] args)
      throws RuntimeException, IOException, InterruptedException, ExecutionException {

    for (String file : new String[]{args[0], args[1]}) {
      if (!FileUtils.doFileExist(file)) {
        throw new FileNotFoundException("File not found: " + file);
      }
    }

    configurator = new ConfigReader(Constants.CONFIG_PROPERTIES).getConfiguration();
    toolManager = new ToolManager().load(loadTools(args[0], args[1], configurator));

    if (configurator.getProjectName() == null) {
      configurator.setProjectName(getProjectName(toolManager));
    }

    List<String> testCases = getTestCaseNames(args);

    if (testCases.size() == 0) {
      throw new IllegalArgumentException("No tests found within the specified package!");
    }

    GsUtilTool gsUtilTool = toolManager.get(GsUtilTool.class);

    new ShardExecutor(configurator, toolManager)
        .execute(testCases, gsUtilTool.uploadAPKsToBucket());

    gsUtilTool.deleteAPKs();

    gsUtilTool.fetchBucket();
  }

  public static void main(String[] args) {
    Flank flank = new Flank();

    try {
      if (validateArguments(args)) {
        flank.start(args);
      }
    } catch (RuntimeException | IOException | InterruptedException | ExecutionException e) {
      exitWithFailure(e);
    }
  }

  private static void exitWithFailure(Exception e) {
    e.printStackTrace();
    System.exit(-1);
  }

  private ToolManager.Config loadTools(String appAPK, String testAPK, Configurator configurator) {
    ToolManager.Config toolConfig = new ToolManager.Config();

    toolConfig.appAPK = appAPK;
    toolConfig.testAPK = testAPK;
    toolConfig.configurator = configurator;
    toolConfig.processExecutor = new ProcessExecutor(configurator);

    return toolConfig;
  }

  private String getProjectName(ToolManager toolManager) throws IOException, InterruptedException {
    System.setOut(getEmptyStream());

    String text = toolManager.get(GcloudTool.class).getProjectName() + "-flank";

    System.setOut(originalStream);
    return text;
  }

  private static boolean validateArguments(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: Flank <app-apk> <test-apk> [package-name]");
      return false;
    }
    return true;
  }

  private List<String> getTestCaseNames(String[] args) {
    System.setOut(getEmptyStream());
    List<String> filteredTests = new ArrayList<>();

    for (TestMethod testMethod : DexParser.findTestMethods(args[1])) {
      if (testMethod.getAnnotationNames().stream().noneMatch(str -> str.contains("Ignore"))) {
        filteredTests.add(testMethod.getTestName());
      }
    }

    if (args.length == 3) {
      filteredTests = FilterUtils.filterTests(filteredTests, args[2]);
    }

    System.setOut(originalStream);
    return filteredTests;
  }

  private static PrintStream originalStream = System.out;

  private PrintStream getEmptyStream() {
    PrintStream emptyStream = null;
    try {
      emptyStream =
          new PrintStream(
              new OutputStream() {
                public void write(int b) {
                }
              },
              false,
              "UTF-8");
    } catch (UnsupportedEncodingException ignored) {
    }
    return emptyStream;
  }
}
