package com.walmart.otto.tools;

import com.walmart.otto.Constants;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static com.walmart.otto.utils.FileUtils.getSimpleName;

public class GsUtilTool extends Tool {
  private String bucket;

  public GsUtilTool(ToolManager.Config config) {
    super(ToolManager.GSUTIL_TOOL, config);
  }

  // Match _GenerateUniqueGcsObjectName from api_lib/firebase/test/arg_validate.py
  //
  // Example: 2017-05-31_17:19:36.431540_hRJD
  //
  // https://cloud.google.com/storage/docs/naming
  private String uniqueObjectName() {
    StringBuilder bucketName = new StringBuilder();
    Instant instant = Instant.now();

    bucketName.append(
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.")
            .withZone(ZoneOffset.UTC)
            .format(instant));
    bucketName.append(String.valueOf(instant.getNano()).substring(0, 6));
    bucketName.append("_");

    Random random = new Random();
    // a-z: 97 - 122
    // A-Z: 65 - 90
    for (int i = 0; i < 4; i++) {
      int ascii = random.nextInt(26);
      char letter = (char) (ascii + 'a');

      if (ascii % 2 == 0) {
        letter -= 32; // upcase
      }

      bucketName.append(letter);
    }
    bucketName.append("/");

    return bucketName.toString();
  }

  public String uploadAPKsToBucket() throws RuntimeException, IOException, InterruptedException {

    bucket = getConfigurator().getProjectBucket();

    if (!findGSFile(bucket)) {
      System.out.println("\nCreating bucket: " + bucket + "\n");
      executeCommand(createBucket(bucket));
    }

    bucket = bucket + uniqueObjectName();

    System.out.println("Uploading: " + getAppAPK() + " to: " + bucket + "\n");

    executeCommand(copyFileToBucket(getAppAPK(), bucket));

    System.out.println("Uploading: " + getTestAPK() + " to: " + bucket + "\n");

    executeCommand(copyFileToBucket(getTestAPK(), bucket));

    return bucket;
  }

  public Map<String, String> fetchResults() throws IOException, InterruptedException {
    Map<String, String> xmlFileAndDevice = new HashMap<String, String>();
    File currentDir = new File("");
    File resultsDir =
        new File(currentDir.getAbsolutePath() + File.separator + Constants.RESULTS_DIR);

    boolean createdFolder = resultsDir.mkdirs();

    if (createdFolder) {
      System.out.println("Created folder: " + resultsDir.getAbsolutePath() + "\n");
    }

    System.out.println("Fetching results to: " + resultsDir.getAbsolutePath());

    String[] fetchFiles = fetchXMLFiles(resultsDir);

    executeCommand(fetchFiles, new ArrayList<>());

    getDeviceNames(xmlFileAndDevice);

    return xmlFileAndDevice;
  }

  public void fetchBucket() throws IOException, InterruptedException {
    if (!getConfigurator().isFetchBucket()) {
      return;
    }

    File currentDir = new File("");
    File resultsDir = new File(currentDir.getAbsolutePath() + File.separator + "bucket");

    boolean createdFolder = resultsDir.mkdirs();

    if (createdFolder) {
      System.out.println("Created folder: " + resultsDir.getAbsolutePath());
    }

    System.out.println("\nFetching bucket to: " + resultsDir.getAbsolutePath());

    String[] fetchBucket = fetchBucket(resultsDir);

    executeCommand(fetchBucket, new ArrayList<>());
  }

  public boolean findGSFile(String fileName) throws IOException, InterruptedException {
    List<String> errorStreamList = new ArrayList<>();
    System.setOut(getEmptyStream());
    executeCommand(findFile(fileName), new ArrayList<>(), errorStreamList);
    System.setOut(originalStream);

    for (String input : errorStreamList) {
      if (input.contains("Exception")) {
        return false;
      }
    }
    return true;
  }

  public void deleteAPKs() throws IOException, InterruptedException {
    executeCommand(deleteApp());
    executeCommand(deleteTest());
  }

  private void getDeviceNames(Map<String, String> xmlFileAndDevice) {
    for (String name : getErrorStreamList()) {
      String[] line;
      if (name.contains("Copying gs")) {
        line = name.split(Pattern.quote("/"));
        xmlFileAndDevice.put(line[line.length - 1].replace("xml...", "xml"), line[line.length - 2]);
      }
    }
  }

  private String[] fetchXMLFiles(File file) {
    return new String[]{
        getConfigurator().getGsutil(),
        "-m",
        "cp",
        "-r",
        "-U",
        bucket + "**/*.xml",
        file.getAbsolutePath()
    };
  }

  private String[] fetchBucket(File file) {
    return new String[]{
        getConfigurator().getGsutil(), "-m", "cp", "-r", "-U", bucket, file.getAbsolutePath()
    };
  }

  private String[] findFile(String name) {
    return new String[]{getConfigurator().getGsutil(), "--quiet", "ls", name};
  }

  private String[] createBucket(String nameOfBucket) {
    return new String[]{
        getConfigurator().getGsutil(), "--quiet", "mb", nameOfBucket,
    };
  }

  private String[] deleteApp() {
    return new String[]{
        getConfigurator().getGsutil(), "--quiet", "rm", "-r", bucket + getSimpleName(getAppAPK())
    };
  }

  private String[] deleteTest() {
    return new String[]{
        getConfigurator().getGsutil(), "--quiet", "rm", "-r", bucket + getSimpleName(getTestAPK())
    };
  }

  private String[] copyFileToBucket(String file, String bucket) {
    return new String[]{
        getConfigurator().getGsutil(), "--quiet", "cp", file, bucket,
    };
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
