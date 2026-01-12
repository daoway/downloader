package com.blogspot.ostas.downloader.service;

import com.blogspot.ostas.downloader.service.model.Chunk;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RangeServiceTest {

  private RangeService rangeService  = Mockito.spy(new RangeService());

  @Test
  void divideIntervals() {
    int value = 10;
    int numberOfChunks = 3;
    var chunks = rangeService.rangeIntervals(value, numberOfChunks);

    assertThat(chunks).hasSize(numberOfChunks);

    var expected = Set.of(Chunk.of(0, 3, 0), Chunk.of(4, 7, 1), Chunk.of(8, 9, 2));
    assertThat(chunks).isEqualTo(expected);
  }

  @Test
  void divideIntervals_1() {
    int value = 11;
    int numberOfChunks = 3;

    var chunks = rangeService.rangeIntervals(value, numberOfChunks);

    assertThat(chunks).hasSize(numberOfChunks)
        .containsExactly(Chunk.of(0, 3, 0), Chunk.of(4, 7, 1), Chunk.of(8, 10, 2));
  }

  @Test
  void divideIntervals_2() {
    int value = 12;
    int numberOfChunks = 3;
    var chunks = rangeService.rangeIntervals(value, numberOfChunks);
    assertThat(chunks).hasSize(numberOfChunks)
        .containsExactly(Chunk.of(0, 3, 0), Chunk.of(4, 7, 1), Chunk.of(8, 11, 2));
  }

  @Test
  void divideIntervals_big_0() {
    long value = 10100000000L;
    int numberOfChunks = 8;
    var chunks = rangeService.rangeIntervals(value, numberOfChunks);
    assertThat(chunks).hasSize(numberOfChunks);
    var brokenRanges =
        chunks.stream().filter(chunk -> chunk.start() < 0 || chunk.end() < 0).toList();
    assertThat(brokenRanges).isEmpty();
  }

  @Test
  void divideIntervals_big_1() {
    long value = Long.MAX_VALUE - 1;
    int numberOfChunks = 64;
    var chunks = rangeService.rangeIntervals(value, numberOfChunks);
    assertThat(chunks).hasSize(numberOfChunks);
    var brokenRanges =
        chunks.stream().filter(chunk -> chunk.start() < 0 || chunk.end() < 0).toList();
    assertThat(brokenRanges).isEmpty();
  }

}
