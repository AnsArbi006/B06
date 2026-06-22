package filter.ast.nodes;

public enum CompOp {
  EQ {
    public String toString() {
      return "==";
    }
  },
  NE {
    public String toString() {
      return "!=";
    }
  },
  LT {
    public String toString() {
      return "<";
    }
  },
  LE {
    public String toString() {
      return "<=";
    }
  },
  GT {
    public String toString() {
      return ">";
    }
  },
  GE {
    public String toString() {
      return ">=";
    }
  };

  public static CompOp fromSymbol(String s) {
    return switch (s) {
      case "==" -> EQ;
      case "!=" -> NE;
      case "<" -> LT;
      case "<=" -> LE;
      case ">" -> GT;
      case ">=" -> GE;
      default -> throw new IllegalArgumentException("Unknown operator: " + s);
    };
  }
}
