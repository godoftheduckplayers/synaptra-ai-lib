package com.ducks.synaptra.memory.episode.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RecordEvent {
  private String content;
  private StatusType status;
}
