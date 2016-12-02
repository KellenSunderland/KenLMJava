/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 package com.kheafield.kenlm;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;

class NativeLibrary {

  private static final Logger logger = LoggerFactory.getLogger(NativeLibrary.class);

  private static final String LIB_PATH = "com.kheafield.kenlm/libken";
  private static final String MAC_EXTENSION = ".dylib";
  private static final String LINUX_EXTENSION = ".so";
  private static final String JAVA_OSX_NAME = "mac os x";
  private static final String JAVA_LINUX_NAME = "linux";
  private static final String JAVA_OS_KEY = "os.name";

  /**
   * Tries to locate and load the native KenLM library from the classpath according to the rules in
   * {@link #resolveLocation()}. In case resource was not found, delegates to the
   * standard {@link System#loadLibrary(String)}.
   */
  public static void load() throws FileNotFoundException {
    String location = resolveLocation();
    logger.info("Location resolved to: " + location);
    URL resource = getResource(location);
    if (resource == null) {
      logger.warn("load: cannot locate classpath resource {}; falling back to java.library.path.", location);
      System.loadLibrary("ken");
      return;
    }
    logger.debug("load: located native library {} under classpath {}", "ken", resource);
    String filePath = toFilePath(resource);
    logger.info("Loading resource");
    System.load(filePath);
    logger.info("Loading complete");
  }

  /**
   * Constructs platform-specific path: classpath/platform_lib_name.platform_extension
   *
   * @return platform dependent native library path
   */
  private static String resolveLocation() throws FileNotFoundException {
    String osName = System.getProperty(JAVA_OS_KEY).toLowerCase();
    if (isOsMatch(osName, JAVA_OSX_NAME)) {
      return LIB_PATH + MAC_EXTENSION;
    } else if (isOsMatch(osName, JAVA_LINUX_NAME)) {
      logger.info("Detected linux KenLM should be used");
      return LIB_PATH + LINUX_EXTENSION;
    } else {
      throw new FileNotFoundException("Could not find kenlm binary for your platform");
    }
  }

  private static boolean isOsMatch(String osName, String prefix) {
    return osName.toLowerCase().startsWith(prefix.toLowerCase());
  }

  private static URL getResource(String location) {
    ClassLoader loader = Optional
            .ofNullable(Thread.currentThread().getContextClassLoader())
            .orElse(NativeLibrary.class.getClassLoader());
    return loader.getResource(location);
  }

  /**
   * {@link System#load(String)} can only load from absolute physical path. If url is not a file
   * protocol, then the resource is copied into temporarily physical path.
   *
   * @param resource - resource location within classpath
   * @return file location to the given resource or location to the copied temporarily file.
   */
  static String toFilePath(URL resource) {
    if (!resource.getProtocol().equals("file")) {
      try {
        URL extracted = extractResource(resource);
        logger.debug("toFilePath: extracted resource {} into {}", resource, extracted);
        return extracted.getFile();
      } catch (IOException ex) {
        logger.error("toFilePath: exception caught", ex);
        Throwables.propagate(ex);
      }
    }
    return resource.getFile();
  }

  private static URL extractResource(URL url) throws IOException {
    String path = url.getFile();
    String name = com.google.common.io.Files.getNameWithoutExtension(path);
    String extension = com.google.common.io.Files.getFileExtension(path);
    File tmpFile = Files.createTempFile(name + "-", "." + extension).toFile();
    tmpFile.deleteOnExit();
    Resources.copy(url, Files.newOutputStream(tmpFile.toPath()));
    return tmpFile.toURI().toURL();
  }
}
