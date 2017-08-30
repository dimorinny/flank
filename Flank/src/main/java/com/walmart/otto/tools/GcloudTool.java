package com.walmart.otto.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.walmart.otto.utils.FileUtils.getSimpleName;

public class GcloudTool extends Tool {

  public GcloudTool(ToolManager.Config config) {
    super(ToolManager.GCLOUD_TOOL, config);
  }

  public void runGcloud(String testCase, String bucket)
      throws RuntimeException, IOException, InterruptedException {
    // don't quote arguments or ProcessBuilder will error in strange ways.
    String[] runGcloud =
        new String[] {
          getConfigurator().getGcloud(),
          "firebase",
          "test",
          "android",
          "run",
          "--type",
          "instrumentation",
          "--app",
          bucket + getSimpleName(getAppAPK()),
          "--test",
          bucket + getSimpleName(getTestAPK()),
          "--results-bucket",
          "gs://" + bucket.split("/")[2],
          "--device-ids",
          getConfigurator().getDeviceIds(),
          "--os-version-ids",
          getConfigurator().getOsVersionIds(),
          "--locales",
          getConfigurator().getLocales(),
          "--orientations",
          getConfigurator().getOrientations(),
          "--timeout",
          getConfigurator().getShardTimeout() + "m",
          "--results-dir",
          bucket.split("/")[3],
          "--test-targets",
          testCase
        };

    List<String> gcloudList = new ArrayList<>(Arrays.asList(runGcloud));

    addParameterIfValueSet(
        gcloudList, "--environment-variables", getConfigurator().getEnvironmentVariables());

    addParameterIfValueSet(
        gcloudList, "--directories-to-pull", getConfigurator().getDirectoriesToPull());

    String[] cmdArray = gcloudList.toArray(new String[0]);

    executeGcloud(cmdArray, testCase);
  }

  private void addParameterIfValueSet(List<String> list, String parameter, String value) {
    if (!value.isEmpty()) {
      list.add(parameter);
      list.add(value);
    }
  }

  private void executeGcloud(String[] commands, String test)
      throws RuntimeException, IOException, InterruptedException {
    List<String> inputStreamList = new ArrayList<>();
    List<String> errorStreamList = new ArrayList<>();

    String resultsLink = null;

    executeCommand(commands, inputStreamList, errorStreamList);

    for (String line : errorStreamList) {
      if (line.contains("More details are available")) {
        resultsLink = line;
      } else if (line.contains("ERROR")) {
        //TODO retry when error is returned from FTL
      }
    }

    for (String line : inputStreamList) {
      System.out.println(line);
    }
    if (resultsLink != null) {
      System.out.println("\n" + resultsLink + "\n");
    }
  }

  public String getProjectName() throws IOException, InterruptedException {
    String[] projectDetails =
        new String[] {getConfigurator().getGcloud(), "config", "get-value", "project"};
    List<String> inputStreamList = new ArrayList<>();
    String projectName = "";

    executeCommand(projectDetails, inputStreamList, new ArrayList<>());

    for (String projectProperties : inputStreamList) {
      projectName = projectProperties;
    }
    return projectName;
  }
}
