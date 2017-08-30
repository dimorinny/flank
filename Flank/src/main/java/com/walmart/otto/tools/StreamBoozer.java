package com.walmart.otto.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StreamBoozer extends Thread {
  private final InputStream in;
  private final List<String> lines;

  public StreamBoozer(InputStream in, List<String> lines) {
    this.in = in;
    this.lines = lines;
  }

  @Override
  public void run() {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String line;
      while ((line = br.readLine()) != null) {
        lines.add(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (br != null) {
          br.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
