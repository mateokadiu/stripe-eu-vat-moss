package io.github.mateokadiu.moss.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MossCliTest {

  @Test
  void rootCommandListsSubcommands() {
    var sw = new StringWriter();
    new CommandLine(new MossCli()).setOut(new PrintWriter(sw)).execute("--help");

    String out = sw.toString();
    assertThat(out).contains("ingest-csv");
    assertThat(out).contains("close-period");
    assertThat(out).contains("generate-return");
    assertThat(out).contains("audit-replay");
  }

  @Test
  void closePeriodAcceptsValidCode() {
    int code = new CommandLine(new ClosePeriodCommand()).execute("2026Q3");
    assertThat(code).isEqualTo(0);
  }

  @Test
  void closePeriodRejectsInvalidCode() {
    int code = new CommandLine(new ClosePeriodCommand()).execute("2026-3");
    assertThat(code).isNotZero();
  }
}
