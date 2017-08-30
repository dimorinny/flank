package com.walmart.otto.tools;

import java.io.IOException;
import java.util.List;

public class ProcessBuilder {
  private java.lang.ProcessBuilder pb;
  private List<String> inputStream;
  private List<String> errorStream;

  public ProcessBuilder(String[] command, List<String> inputStream, List<String> errorStream)
      throws IOException {
    this.inputStream = inputStream;
    this.errorStream = errorStream;
    pb = new java.lang.ProcessBuilder(command);
  }

  public void start() throws IOException, InterruptedException {
    Process process = pb.start();

    StreamBoozer seInfo = new StreamBoozer(process.getInputStream(), inputStream);
    StreamBoozer seError = new StreamBoozer(process.getErrorStream(), errorStream);

    seInfo.start();
    seError.start();

    seInfo.join();
    seError.join();
  }
}
