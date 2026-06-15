package filter.model;

public enum Genre {
  ROCK,
  JAZZ,
  GRUNGE,
  ELECTRONIC,
  PUNK_ROCK,
  POWER_METAL,
  FOLK_METAL,
  FOLK_ROCK,
  INDUSTRIAL_METAL,
  UNKNOWN;

  public static Genre fromString(String raw) {
    if (raw == null) return UNKNOWN;

    try {
      var normalized = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
      return Genre.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return UNKNOWN;
    }
  }

  @Override
  public String toString() {
    return name().toLowerCase().replace('_', ' ');
  }
}
