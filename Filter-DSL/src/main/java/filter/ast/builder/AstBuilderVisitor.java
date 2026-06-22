package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  // Die Stacks sammeln Zwischenergebnisse, bis der aktuelle Knoten sie zusammensetzt.
  private final Deque<Expr> exprStack = new ArrayDeque<>();
  private final Deque<Value> valueStack = new ArrayDeque<>();
  private final Deque<List<Value>> listStack = new ArrayDeque<>();

  // Public entry point
  public Expr translate(FilterParser.QueryContext ctx) {
    // Alte Ausdrücke entfernen, damit ein neuer Lauf sauber startet.
    exprStack.clear();
    // Alte Values entfernen, damit nichts vom letzten Lauf übrig bleibt.
    valueStack.clear();
    // Alte Listen entfernen, damit auch Listen sauber neu gebaut werden.
    listStack.clear();
    // Starte die Visitor-Verarbeitung bei query.
    visit(ctx);
    // Am Ende liegt genau ein fertiger AST oben auf dem Stack.
    return exprStack.pop();
  }

  // query  : expr EOF
  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    // query reicht einfach an expr weiter.
    visit(ctx.expr());
    return null;
  }

  // expr: orExpr
  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    // expr reicht einfach an orExpr weiter.
    visit(ctx.orExpr());
    return null;
  }

  // orExpr : andExpr (OR andExpr)*
  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    // Besuche zuerst den linken Teil.
    visit(ctx.andExpr(0));
    // Hole das Ergebnis des linken Teilbaums.
    Expr current = exprStack.pop();
    // Verknüpfe alle restlichen Teile mit Or.
    for (int i = 1; i < ctx.andExpr().size(); i++) {
      // Baue den rechten Teilbaum.
      visit(ctx.andExpr(i));
      // Erzeuge einen neuen Or-Knoten aus links und rechts.
      current = new Expr.Or(current, exprStack.pop());
    }
    // Lege den fertigen Teilbaum wieder auf den Stack.
    exprStack.push(current);
    return null;
  }

  // andExpr: notExpr (AND notExpr)*
  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    // Besuche zuerst den linken Teil.
    visit(ctx.notExpr(0));
    // Hole das Ergebnis des linken Teilbaums.
    Expr current = exprStack.pop();
    // Verknüpfe alle restlichen Teile mit And.
    for (int i = 1; i < ctx.notExpr().size(); i++) {
      // Baue den rechten Teilbaum.
      visit(ctx.notExpr(i));
      // Erzeuge einen neuen And-Knoten aus links und rechts.
      current = new Expr.And(current, exprStack.pop());
    }
    // Lege den fertigen Teilbaum wieder auf den Stack.
    exprStack.push(current);
    return null;
  }

  // notExpr: NOT notExpr | primary
  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    // Falls not vorkommt, muss ein Not-Knoten gebaut werden.
    if (ctx.NOT() != null) {
      // Verarbeite zuerst den inneren Ausdruck.
      visit(ctx.notExpr());
      // Wickle das Ergebnis in einen Not-Knoten.
      exprStack.push(new Expr.Not(exprStack.pop()));
    } else {
      // Ohne not liegt direkt primary vor.
      visit(ctx.primary());
    }
    return null;
  }

  // primary: comparison | '(' expr ')'
  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    // Ohne Klammern verarbeiten wir comparison direkt.
    if (ctx.comparison() != null) {
      visit(ctx.comparison());
    } else {
      // Mit Klammern besuchen wir den inneren Ausdruck.
      visit(ctx.expr());
    }
    return null;
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    // Lies zuerst den Feldnamen.
    String field = ctx.IDENTIFIER().getText();
    // Mit COMPOP bauen wir einen normalen Vergleich.
    if (ctx.COMPOP() != null) {
      // Der rechte Wert wird zuerst besucht und auf valueStack gelegt.
      visit(ctx.literal());
      // Danach entsteht daraus ein Comparison-Knoten auf exprStack.
      exprStack.push(new Expr.Comparison(field, CompOp.fromSymbol(ctx.COMPOP().getText()), valueStack.pop()));
    } else {
      // Für in (...) wird zuerst die komplette Werteliste gebaut.
      visit(ctx.literalList());
      // Danach entsteht daraus ein InList-Knoten auf exprStack.
      exprStack.push(new Expr.InList(field, listStack.pop()));
    }
    return null;
  }

  // literalList: literal (',' literal)*
  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    // Hier sammeln wir alle Values der Liste.
    List<Value> values = new ArrayList<>();
    // Jedes literal wird einzeln besucht und danach vom valueStack geholt.
    for (FilterParser.LiteralContext literalContext : ctx.literal()) {
      visit(literalContext);
      values.add(valueStack.pop());
    }
    // Die fertige Liste wird für den Vergleich gespeichert.
    listStack.push(values);
    return null;
  }

  // literal: STRING | NUMBER
  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    // Strings müssen ohne die äußeren Anführungszeichen gespeichert werden.
    if (ctx.STRING() != null) {
      // Hole den Originaltext inklusive Anführungszeichen.
      String raw = ctx.STRING().getText();
      valueStack.push(new Value.Str(raw.substring(1, raw.length() - 1)));
    } else {
      // Zahlen werden direkt als int geparst.
      valueStack.push(new Value.Num(Integer.parseInt(ctx.NUMBER().getText())));
    }
    return null;
  }
}
