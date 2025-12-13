package io.github.mateokadiu.moss.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/** Picocli root command. Mirrors the REST surface for one-off ops. */
@Command(
    name = "moss",
    mixinStandardHelpOptions = true,
    version = "stripe-eu-vat-moss 0.1.0",
    description = "EU VAT One-Stop-Shop CLI",
    subcommands = {
      IngestCsvCommand.class,
      ClosePeriodCommand.class,
      GenerateReturnCommand.class,
      AuditReplayCommand.class
    })
public class MossCli {

  public static void main(String[] args) {
    int exit = new CommandLine(new MossCli()).execute(args);
    System.exit(exit);
  }
}
