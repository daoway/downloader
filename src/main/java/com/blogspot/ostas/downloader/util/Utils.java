package com.blogspot.ostas.downloader.util;

public class Utils {

  private Utils() {
  }

  public static String bytesToHumanReadable(long bytes) {
    final int bytesLimit = 1024;
    if (bytes < bytesLimit) {
      return bytes + " b";
    }
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    char unit = "KMGTPE".charAt(exp - 1);
    return String.format("%.1f %sb", bytes / Math.pow(1024, exp), unit);
  }

}
