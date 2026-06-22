package filter.ast.printer;

import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.stream.Collectors;

public class AstPrinter {

  public static String toString(Expr expr) {
    return printExpr(expr);
  }

  private static String printExpr(Expr e) {
    return switch (e) {
      case Expr.And(var l, var r) -> "(" + printExpr(l) + " and " + printExpr(r) + ")";
      case Expr.Or(var l, var r) -> "(" + printExpr(l) + " or " + printExpr(r) + ")";
      case Expr.Not(var inner) -> "(not " + printExpr(inner) + ")";
      case Expr.Comparison(var field, var op, var value) ->
          "(" + field + " " + op.toString() + " " + printValue(value) + ")";
      case Expr.InList(var field, var values) ->
          "("
              + field
              + " in ("
              + values.stream().map(AstPrinter::printValue).collect(Collectors.joining(", "))
              + "))";
    };
  }

  private static String printValue(Value v) {
    return switch (v) {
      case Value.Str(var s) -> "\"" + s + "\"";
      case Value.Num(var n) -> Integer.toString(n);
    };
  }
}
