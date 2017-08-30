package com.walmart.otto.utils;

import java.io.File;
import java.util.regex.Pattern;

public class FileUtils {

  public static String getSimpleName(String file) {
    String[] parts = file.split(Pattern.quote(File.separator));
    return parts[parts.length - 1];
  }

  public static boolean doFileExist(String filePath) {
    return new File(filePath).exists();
  }
}
