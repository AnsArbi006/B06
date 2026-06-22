package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.printer.AstPrinter;
import net.jqwik.api.*;

public class RoundtripPropertiesTest {

  @Property
  boolean patternBuilderSurvivesRoundtrip(@ForAll("simpleQueries") String query) {
    // Erstes Parsen und Bauen mit dem Pattern-Builder.
    var expr = AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);
    // Danach wieder in Text drucken und noch einmal parsen.
    var roundtrip = AstBuilders.fromQuery(AstPrinter.toString(expr), new AstBuilderPattern()::translate);
    // Der AST soll nach dem Hin-und-zurück gleich bleiben.
    return expr.equals(roundtrip);
  }

  @Property
  boolean visitorBuilderSurvivesRoundtrip(@ForAll("simpleQueries") String query) {
    // Erstes Parsen und Bauen mit dem Visitor-Builder.
    var expr = AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);
    // Danach wieder in Text drucken und noch einmal parsen.
    var roundtrip = AstBuilders.fromQuery(AstPrinter.toString(expr), new AstBuilderVisitor()::translate);
    // Der AST soll nach dem Hin-und-zurück gleich bleiben.
    return expr.equals(roundtrip);
  }

  @Property
  boolean bothBuildersAgree(@ForAll("simpleQueries") String query) {
    // AST aus dem Pattern-Builder erzeugen.
    var patternExpr = AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);
    // AST aus dem Visitor-Builder erzeugen.
    var visitorExpr = AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);
    // Beide Bäume sollen denselben Inhalt haben.
    return patternExpr.equals(visitorExpr);
  }

  @Property
  boolean simplifyRemovesDoubleNegation(@ForAll("comparisons") String comparison) {
    // Baue bewusst not not vor einen Vergleich.
    var expr =
        AstBuilders.fromQuery("not not " + comparison, new AstBuilderPattern()::translate);
    // Das ist die Version ohne doppelte Negation.
    var expected = AstBuilders.fromQuery(comparison, new AstBuilderPattern()::translate);
    // simplify soll beide Varianten gleich machen.
    return AstBuilders.simplify(expr).equals(expected);
  }

  // ---------- @Provide-Methods for Arbitraries ----------

  @Provide
  Arbitrary<String> fields() {
    return Arbitraries.of("title", "artist", "genre", "year");
  }

  @Provide
  Arbitrary<String> stringLiterals() {
    return Arbitraries.strings()
        .withChars("abcxyz")
        .ofMinLength(1)
        .ofMaxLength(5)
        .map(s -> "\"" + s + "\"");
  }

  @Provide
  Arbitrary<String> numberLiterals() {
    return Arbitraries.integers().between(1900, 2025).map(Object::toString);
  }

  @Provide
  Arbitrary<String> comparisons() {
    Arbitrary<String> ops = Arbitraries.of("==", "!=", "<", "<=", ">", ">=");

    Arbitrary<String> stringComp =
        Combinators.combine(fields(), ops, stringLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    Arbitrary<String> numberComp =
        Combinators.combine(Arbitraries.of("year"), ops, numberLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    return Arbitraries.oneOf(stringComp, numberComp);
  }

  @Provide
  Arbitrary<String> simpleQueries() {
    return comparisons()
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(
            list -> {
              if (list.size() == 1) return list.getFirst();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                  String conn = Arbitraries.of(" and ", " or ").sample();
                  sb.append(conn);
                }
                sb.append(list.get(i));
              }
              return sb.toString();
            });
  }
}
