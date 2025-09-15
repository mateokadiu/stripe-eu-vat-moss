package io.github.mateokadiu.moss.it.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Enforces module boundary rules across the moss codebase.
 *
 * <p>Dependency direction: ingest depends on enrich + ledger + shared; file depends on ledger +
 * shared; observe is a sink (must not be depended upon by domain modules). api + cli wire
 * everything; nothing else depends on them.
 */
class ModuleBoundariesTest {

  private static final String BASE = "io.github.mateokadiu.moss";

  private static com.tngtech.archunit.core.domain.JavaClasses moss() {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE);
  }

  @Test
  void sharedHasNoInboundFromOtherModules() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(BASE + ".shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                BASE + ".ledger..",
                BASE + ".enrich..",
                BASE + ".ingest..",
                BASE + ".file..",
                BASE + ".observe..",
                BASE + ".api..",
                BASE + ".cli..");
    rule.check(moss());
  }

  @Test
  void ingestMayNotDependOnFile() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(BASE + ".ingest..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BASE + ".file..");
    rule.check(moss());
  }

  @Test
  void fileMayNotDependOnIngest() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(BASE + ".file..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BASE + ".ingest..");
    rule.check(moss());
  }

  @Test
  void observeIsASink() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage(
                BASE + ".shared..",
                BASE + ".ledger..",
                BASE + ".enrich..",
                BASE + ".ingest..",
                BASE + ".file..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BASE + ".observe..");
    rule.check(moss());
  }
}
