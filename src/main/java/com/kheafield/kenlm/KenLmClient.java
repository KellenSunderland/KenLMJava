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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class KenLmClient implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(KenLmClient.class);
  private long pointer = 0;

  /**
   * This method constructs a KenLM instance.
   * @param file_name Path to KenLM file (binary or ARPA).
   */
  public KenLmClient(String file_name) {
    try {
      NativeLibrary.load();
      pointer = construct(file_name);
    } catch (UnsatisfiedLinkError | FileNotFoundException e) {
      LOG.error("Can't instantiate KenLM.  Please ensure your platform is supported (supported " +
              "platforms are listed in the README.md)");
      throw new KenLMLoadException(e);
    }
  }

  private static native long construct(String file_name);

  private static native void destroy(long ptr);

  private static native int order(long ptr);

  private static native boolean registerWord(long ptr, String word, int id);

  private static native float prob(long ptr, int words[]);

  private static native float probForString(long ptr, String[] words);

  private static native boolean isKnownWord(long ptr, String word);

  private static native boolean isLmOov(long ptr, int word);

  private static native long probRule(long ptr, long pool);

  private static native float estimateRule(long ptr, long words[]);

  private static native float probString(long ptr, int words[], int start);

  private long getPointer() {
    if (pointer == 0) {
      throw new RuntimeException("KenLM pointer has not been properly initialized, or has already" +
              " been closed");
    }
    return pointer;
  }

  public int order() {
    return order(getPointer());
  }

  public void destroy() {
    destroy(getPointer());
  }

  /***
   * This method allows you to register a word with KenLM.  The word can then be used for any
   * subsequent probability calls.
   * @param word The word you want to register.
   * @param id The id you'd like to represent the word with.
   * @return true if the registration succeeded.
   */
  public boolean registerWord(String word, int id) {
    return registerWord(getPointer(), word, id);
  }

  /***
   * Returns prob of ngram sequence passed referred to by words.
   * @param words ngrams' ids.
   * @return logarithmic probability of words in sequence.
   */
  public float prob(int[] words) {
    return prob(getPointer(), words);
  }

  /***
   * Log probability for a sequence of strings
   * @param words Sequence of strings that KenLM will be queried with.
   * @return Log probability result.
   */
  public float probForString(String[] words) {
    return probForString(getPointer(), words);
  }

  public float probString(int[] words, int i) {
    return probString(getPointer(), words, i);
  }

  /***
   * This method is required to call {@link #probRule(long, long)}.  For performance reasons,
   * words get sent to probRule via a shared word buffer.  This buffer can be shared over
   * multiple requests.  The buffer allocated must be the size of the maximum number of ngrams in
   * the model (the order) plus one int to indicate the length.  It is recommended to create one
   * shared pool per thread that will call probRule.
   *
   * Note: this call is likely to be abstracted and deprecated in a future API.
   *
   * @param wordsBuffer The buffer that will contain the number of ngrams to call prob on (in the
   *                    first long), and then a list of ngram ids as longs.  By convention
   *                    negative ids will be considered non-terminals.  Initially may be empty as
   *                    long as it is the correct size.
   * @return A long representing a pointer to the new shared pool.
   */
  public static native long createPool(ByteBuffer wordsBuffer);

  /***
   * This method destroys the pool that was allocated with the {@link #createPool(ByteBuffer)}
   * call, and used during the {@link #probRule(long, long)} call.
   *
   * Note: this call is likely to be abstracted and deprecated in a future API.
   *
   * @param pointer A long representing a pointer to the new shared pool.
   */
  public static native void destroyPool(long pointer);

  /***
   * A probability query utilizing dynamic states.
   *
   * Note: this call is likely to be abstracted and deprecated in a future API.
   *
   * @param pool A handle to a pool previously created with {@link #createPool(ByteBuffer)}.
   *             This pool must have it's size present as the first long in the buffer.  If the
   *             size is N the buffer must then have N ngrams stored.
   * @return A result data object containing both the dynamic state and the
   * logarithmic probability of the ngrams.
   */
  public ProbResult probRule(long pool) {
    long packedResult = probRule(getPointer(), pool);
    int state = (int) (packedResult >> 32);
    float probVal = Float.intBitsToFloat((int) packedResult);

    return new ProbResult(state, probVal);
  }

  /***
   * An estimate of probability of a sequence of ngrams without state.
   * @param words Sequence of ngrams.
   * @return logarithmic probability.
   */
  public float estimateRule(long[] words) {
    return estimateRule(getPointer(), words);
  }

  /***
   * Test to see if a word defined by this id is present in the KenLM vocabulary.
   * @param wordId The word id whose presence we'll test.
   * @return True if the word has never been registered, false otherwise.
   */
  public boolean isLmOov(int wordId) {
    return isLmOov(getPointer(), wordId);
  }

  /***
   * Tests a String word to see if it is present in the KenLM vocabulary.
   * @param word Word to test for presence.
   * @return True if the word is present, false otherwise.
   */
  public boolean isKnownWord(String word) {
    return isKnownWord(getPointer(), word);
  }

  /***
   * It is required to call this method after use of a KenLM client instance.  Failure to close
   * this object will result in a memory leak.
   */
  @Override
  public synchronized void close() {
    if (pointer != 0) {
      destroy(pointer);
      pointer = 0;
    }
  }

  @Override
  protected void finalize() throws Throwable {
    try{
      close();
    } finally {
      super.finalize();
    }
  }

  /***
   * A class used when KenLM fails to initialize properly.
   */
  public static class KenLMLoadException extends RuntimeException {

    public KenLMLoadException(Throwable e) {
      super(e);
    }
  }

  /***
   * A class used to encapsulate the results of a {@link #probRule(long, long)} call.
   */
  public static class ProbResult {
    public final long state;
    public final float prob;

    public ProbResult(long state, float prob) {
      this.state = state;
      this.prob = prob;
    }
  }
}
