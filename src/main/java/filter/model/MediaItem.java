package filter.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MediaItem(String title, String artist, Genre genre, int year) {

  /** file system (e.g. JFileChooser) */
  public static List<MediaItem> loadFromPath(Path path) {
    try (var br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return parseReader(br);
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading file: " + path, e);
    }
  }

  /** classpath resource (src/main/resources) */
  public static List<MediaItem> loadFromResource(String resourcePath) {
    Objects.requireNonNull(resourcePath);

    var cl = Thread.currentThread().getContextClassLoader();
    var rp = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

    var in = cl.getResourceAsStream(rp);
    if (in == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);

    try (in;
        var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return parseReader(br);
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading resource: " + resourcePath, e);
    }
  }

  private static List<MediaItem> parseReader(BufferedReader br) {
    return br.lines()
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .filter(line -> !line.startsWith("#"))
        .map(MediaItem::parseLineLenient)
        .flatMap(Optional::stream)
        .toList();
  }

  private static Optional<MediaItem> parseLineLenient(String line) {
    var parts = line.split(";");
    if (parts.length < 4) return Optional.empty();

    try {
      var title = parts[0].trim();
      var artist = parts[1].trim();
      var genre = parts[2].trim();
      var year = Integer.parseInt(parts[3].trim());
      return Optional.of(new MediaItem(title, artist, Genre.fromString(genre), year));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }
}
