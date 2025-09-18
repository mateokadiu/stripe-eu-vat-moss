package io.github.mateokadiu.moss.shared;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/** Factory for v7 UUIDs (time-ordered) used as aggregate/event identifiers. */
public final class Ids {

  private Ids() {}

  /** A new v7 UUID — sortable by creation time, suitable as a primary key. */
  public static UUID newV7() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
