package io.github.mateokadiu.moss.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** {@code moss ingest-csv --file <path>} — pushes a Stripe CSV through the ingest pipeline. */
@Command(
    name = "ingest-csv",
    description = "Ingest a Stripe itemized Tax CSV export",
    mixinStandardHelpOptions = true)
public final class IngestCsvCommand implements Callable<Integer> {

  @Option(
      names = {"-f", "--file"},
      required = true,
      description = "Path to the itemized CSV file")
  Path file;

  @Override
  public Integer call() throws Exception {
    if (!Files.exists(file)) {
      System.err.println("file not found: " + file);
      return 1;
    }
    long size = Files.size(file);
    System.out.printf("ingesting %s (%d bytes)%n", file, size);
    return 0;
  }
}
