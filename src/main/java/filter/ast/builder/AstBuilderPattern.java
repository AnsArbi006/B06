package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayList;
import java.util.List;

public class AstBuilderPattern {

  // Public entry point
  // query  : expr EOF
  public Expr translate(FilterParser.QueryContext ctx) {
    // Starte bei der expr-Regel unterhalb von query.
    return buildExpr(ctx.expr());
  }

  // expr: orExpr
  private Expr buildExpr(FilterParser.ExprContext ctx) {
    // expr ist nur ein Durchreicher zu orExpr.
    return buildOrExpr(ctx.orExpr());
  }

  // orExpr : andExpr (OR andExpr)*
  private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
    // Nimm zuerst den linken Teilbaum.
    Expr current = buildAndExpr(ctx.andExpr(0));
    // Hänge alle weiteren Teile linksassoziativ mit Or an.
    for (int i = 1; i < ctx.andExpr().size(); i++) {
      // Aus links or rechts wird ein neuer Or-Knoten.
      current = new Expr.Or(current, buildAndExpr(ctx.andExpr(i)));
    }
    // Gib den fertig aufgebauten Teilbaum zurück.
    return current;
  }

  // andExpr: notExpr (AND notExpr)*
  private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
    // Nimm zuerst den linken Teilbaum.
    Expr current = buildNotExpr(ctx.notExpr(0));
    // Hänge alle weiteren Teile linksassoziativ mit And an.
    for (int i = 1; i < ctx.notExpr().size(); i++) {
      // Aus links and rechts wird ein neuer And-Knoten.
      current = new Expr.And(current, buildNotExpr(ctx.notExpr(i)));
    }
    // Gib den fertig aufgebauten Teilbaum zurück.
    return current;
  }

  // notExpr: NOT notExpr | primary
  private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
    // Wenn wirklich ein not vorkommt, baue einen Not-Knoten.
    if (ctx.NOT() != null) {
      // Der Kindausdruck wird rekursiv darunter gebaut.
      return new Expr.Not(buildNotExpr(ctx.notExpr()));
    }
    // Sonst liegt direkt ein primary-Ausdruck vor.
    return buildPrimary(ctx.primary());
  }

  // primary: comparison | '(' expr ')'
  private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
    // Ohne Klammern ist primary einfach ein comparison-Knoten.
    if (ctx.comparison() != null) {
      return buildComparison(ctx.comparison());
    }
    // Mit Klammern wird einfach der innere Ausdruck übernommen.
    return buildExpr(ctx.expr());
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  private Expr buildComparison(FilterParser.ComparisonContext ctx) {
    // Lies zuerst den Feldnamen wie artist oder year.
    String field = ctx.IDENTIFIER().getText();
    // Bei COMPOP handelt es sich um einen normalen Vergleich.
    if (ctx.COMPOP() != null) {
      // Aus Text wie == wird das passende Enum gebaut.
      return new Expr.Comparison(
          field, CompOp.fromSymbol(ctx.COMPOP().getText()), buildLiteral(ctx.literal()));
    }
    // Ohne COMPOP bleibt nur die in-Liste übrig.
    return new Expr.InList(field, buildLiteralList(ctx.literalList()));
  }

  // literalList: literal (',' literal)*
  private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
    // Hier sammeln wir alle Werte aus der Liste.
    List<Value> values = new ArrayList<>();
    // Jeder Literal-Kontext wird einzeln in einen Value umgewandelt.
    for (FilterParser.LiteralContext literalContext : ctx.literal()) {
      values.add(buildLiteral(literalContext));
    }
    // Die komplette Value-Liste geht zurück in den AST.
    return values;
  }

  // literal: STRING | NUMBER
  private Value buildLiteral(FilterParser.LiteralContext ctx) {
    // String-Literale stehen mit Anführungszeichen im Parse-Tree.
    if (ctx.STRING() != null) {
      // Hole den Originaltext inklusive Anführungszeichen.
      String raw = ctx.STRING().getText();
      // Strings behalten den Inhalt ohne die Anführungszeichen.
      return new Value.Str(raw.substring(1, raw.length() - 1));
    }
    // Zahlen werden als int in Value.Num gespeichert.
    return new Value.Num(Integer.parseInt(ctx.NUMBER().getText()));
  }
}
