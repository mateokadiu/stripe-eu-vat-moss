package io.github.mateokadiu.moss.cli;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** {@code moss audit-replay --tx <uuid> --as-of <instant>} — print historical aggregate state. */
@Command(
    name = "audit-replay",
    description = "Replay an aggregate as it was at a given transaction time",
    mixinStandardHelpOptions = true)
public final class AuditReplayCommand implements Callable<Integer> {

  @Option(
      names = {"--tx"},
      required = true,
      description = "Transaction aggregate UUID")
  UUID tx;

  @Option(
      names = {"--as-of"},
      required = true,
      description = "ISO-8601 instant")
  String asOf;

  @Override
  public Integer call() {
    Instant when = Instant.parse(asOf);
    System.out.printf("replaying %s as of %s%n", tx, when);
    return 0;
  }
}
