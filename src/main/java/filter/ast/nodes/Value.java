package filter.ast.nodes;

public sealed interface Value {
  record Str(String text) implements Value {}

  record Num(int value) implements Value {}
}
