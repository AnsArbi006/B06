package filter.ast.eval;

import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import filter.model.MediaItem;

public class Evaluator {

  public static boolean matches(MediaItem item, Expr expr) {
    return switch (expr) {
      case Expr.And a -> matches(item, a.left()) && matches(item, a.right());
      case Expr.Or o -> matches(item, o.left()) || matches(item, o.right());
      case Expr.Not n -> !matches(item, n.inner());
      case Expr.Comparison c -> evalComparison(item, c);
      case Expr.InList in -> evalInList(item, in);
    };
  }

  private static boolean evalComparison(MediaItem item, Expr.Comparison c) {
    var fieldValue = fieldValueAsString(item, c.field());
    var v = c.value();

    // keep it simple and use string comparison if possible
    return switch (c.op()) {
      case EQ -> fieldValue.equals(valueAsString(v));
      case NE -> !fieldValue.equals(valueAsString(v));
      case LT, LE, GT, GE -> compareNumeric(fieldValue, v, c.op());
    };
  }

  private static boolean compareNumeric(String fieldValue, Value v, CompOp op) {
    try {
      int left = Integer.parseInt(fieldValue);
      int right =
          switch (v) {
            case Value.Num n -> n.value();
            case Value.Str s -> Integer.parseInt(s.text());
          };
      return switch (op) {
        case LT -> left < right;
        case LE -> left <= right;
        case GT -> left > right;
        case GE -> left >= right;
        default -> false;
      };
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean evalInList(MediaItem item, Expr.InList in) {
    var fieldValue = fieldValueAsString(item, in.field());
    var values = in.values();
    return values.stream().anyMatch(v -> fieldValue.equals(valueAsString(v)));
  }

  private static String fieldValueAsString(MediaItem item, String field) {
    return switch (field) {
      case "title" -> item.title();
      case "artist" -> item.artist();
      case "genre" -> item.genre().toString();
      case "year" -> java.lang.String.valueOf(item.year());
      default -> "";
    };
  }

  private static String valueAsString(Value v) {
    return switch (v) {
      case Value.Str s -> s.text();
      case Value.Num n -> java.lang.String.valueOf(n.value());
    };
  }
}
