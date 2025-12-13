package io.github.mateokadiu.moss.cli;

import io.github.mateokadiu.moss.shared.Period;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** {@code moss close-period 2026Q3} — pins ECB rates and freezes the read-model snapshot. */
@Command(
    name = "close-period",
    description = "Close a reporting period",
    mixinStandardHelpOptions = true)
public final class ClosePeriodCommand implements Callable<Integer> {

  @Parameters(index = "0", description = "Period code, e.g. 2026Q3")
  String code;

  @Override
  public Integer call() {
    Period p = Period.parse(code);
    System.out.printf("closing %s (deadline %s)%n", p.code(), p.filingDeadline());
    return 0;
  }
}
