package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import filter.ast.printer.AstPrinter;
import org.junit.jupiter.api.Test;

public class AstTest {
  @Test
  void patternBuilderBuildsSimpleComparison() {
    // Parse und baue einen sehr einfachen Vergleich.
    Expr expr = AstBuilders.fromQuery("artist == \"Beatles\"", new AstBuilderPattern()::translate);

    // Prüfe, ob wirklich der erwartete Comparison-Knoten entsteht.
    assertEquals(
        new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
        expr);
  }

  @Test
  void visitorBuilderRespectsOperatorPrecedence() {
    // Diese Query testet die Reihenfolge von or, and und not.
    Expr expr =
        AstBuilders.fromQuery(
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"",
            new AstBuilderVisitor()::translate);

    // Der Pretty-Print zeigt, ob der Baum korrekt geklammert wurde.
    assertEquals(
        "((genre in (\"rock\", \"jazz\")) or ((year <= 1990) and (not (artist == \"Beatles\"))))",
        AstPrinter.toString(expr));
  }

  @Test
  void bothBuildersProduceTheSameAst() {
    // Diese Query wird mit beiden Buildern erzeugt.
    String query = "(artist == \"Beatles\" or artist == \"Queen\") and year >= 1965";

    // Der Pattern-Builder baut den AST direkt per Rückgabewerten.
    Expr patternExpr = AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);
    // Der Visitor-Builder baut denselben AST über Stacks.
    Expr visitorExpr = AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);

    // Beide Varianten sollen strukturell genau gleich sein.
    assertEquals(patternExpr, visitorExpr);
  }

  @Test
  void simplifyRemovesDoubleNot() {
    // Baue absichtlich eine doppelte Negation.
    Expr expr =
        new Expr.Not(
            new Expr.Not(new Expr.Comparison("year", CompOp.GE, new Value.Num(1965))));

    // Nach simplify soll nur noch der innere Vergleich übrig bleiben.
    assertEquals("(year >= 1965)", AstPrinter.toString(AstBuilders.simplify(expr)));
  }
}
