package io.github.mateokadiu.moss.cli;

import io.github.mateokadiu.moss.shared.Period;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** {@code moss generate-return 2026Q3 --out return.xml} — write the SAF-OSS XML to disk. */
@Command(
    name = "generate-return",
    description = "Generate a SAF-OSS XML return for a closed period",
    mixinStandardHelpOptions = true)
public final class GenerateReturnCommand implements Callable<Integer> {

  @Parameters(index = "0", description = "Period code, e.g. 2026Q3")
  String code;

  @Option(
      names = {"-o", "--out"},
      required = true,
      description = "Output XML file")
  Path out;

  @Override
  public Integer call() throws Exception {
    Period p = Period.parse(code);
    String stub =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<SAF-OSS version=\"1.0\"><Header><Period>"
            + p.code()
            + "</Period></Header></SAF-OSS>";
    Files.writeString(out, stub);
    System.out.printf("wrote %s%n", out);
    return 0;
  }
}
