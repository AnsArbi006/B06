package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.printer.AstPrinter;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class ApprovalTest {
  @Test
  void complexQueryWithPatternBuilder() {
    // Eine komplexere Query eignet sich gut für Approval-Tests.
    String query = "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"";

    // Hier wird der AST in einen gut lesbaren String umgewandelt.
    String ast = AstPrinter.toString(AstBuilders.fromQuery(query, new AstBuilderPattern()::translate));

    // ApprovalTests vergleicht den Text mit der approved-Datei.
    Approvals.verify(ast);
  }

  @Test
  void bothBuildersPrettyPrintTheSame() {
    // Diese Query wird mit beiden Buildern übersetzt.
    String query = "(artist == \"Beatles\" or artist == \"Queen\") and year >= 1965";

    // Pattern-Builder-Ausgabe als Text.
    String patternAst =
        AstPrinter.toString(AstBuilders.fromQuery(query, new AstBuilderPattern()::translate));
    // Visitor-Builder-Ausgabe als Text.
    String visitorAst =
        AstPrinter.toString(AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate));

    // Beide Textausgaben werden zusammen geprüft.
    Approvals.verify("pattern: " + patternAst + System.lineSeparator() + "visitor: " + visitorAst);
  }
}
