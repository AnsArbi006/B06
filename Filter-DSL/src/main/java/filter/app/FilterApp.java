package filter.app;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilders;
import filter.ast.eval.Evaluator;
import filter.ast.nodes.Expr;
import filter.model.MediaItem;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

public class FilterApp extends JFrame {

  private final MediaTableModel tableModel = new MediaTableModel();
  private final JTable table = new JTable(tableModel);

  private final JTextField queryField = new JTextField();
  private final JTextArea resultArea = new JTextArea(5, 40);

  // IMPORTANT: these are MODEL row indices (indices into tableModel.getItems())
  private List<Integer> matchingRows = List.of();

  public static void run() {
    SwingUtilities.invokeLater(
        () -> {
          var app = new FilterApp();
          app.setVisible(true);
        });
  }

  public FilterApp() {
    super("Filter Demo");

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    setupMenuBar();
    setupMainPanel();

    pack();
    setLocationRelativeTo(null);
  }

  private void setupMenuBar() {
    var menuBar = new JMenuBar();
    var fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    var openItem = new JMenuItem("Open...");
    openItem.addActionListener(_ -> openFile());
    fileMenu.add(openItem);

    menuBar.add(fileMenu);
    setJMenuBar(menuBar);
  }

  private void setupMainPanel() {
    installSorting();

    var tableScroll = new JScrollPane(table);

    table.setDefaultRenderer(
        Object.class,
        new DefaultTableCellRenderer() {
          @Override
          public Component getTableCellRendererComponent(
              JTable table,
              Object value,
              boolean isSelected,
              boolean hasFocus,
              int row,
              int column) {

            var c =
                super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // row is a VIEW row index once sorting/filtering is enabled -> convert to MODEL index
            int modelRow = table.convertRowIndexToModel(row);

            if (matchingRows.contains(modelRow)) c.setBackground(Color.YELLOW);
            else c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            return c;
          }
        });

    var searchPanel = new JPanel(new BorderLayout(5, 5));
    searchPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
    searchPanel.add(queryField, BorderLayout.CENTER);
    var searchButton = new JButton("Search");
    searchPanel.add(searchButton, BorderLayout.EAST);

    searchButton.addActionListener(_ -> runSearch());
    queryField.addActionListener(_ -> runSearch()); // Enter im Textfeld

    resultArea.setEditable(false);
    var resultScroll = new JScrollPane(resultArea);
    resultScroll.setBorder(BorderFactory.createTitledBorder("Results"));

    var topPanel = new JPanel(new BorderLayout());
    topPanel.add(searchPanel, BorderLayout.NORTH);
    topPanel.add(tableScroll, BorderLayout.CENTER);

    add(topPanel, BorderLayout.CENTER);
    add(resultScroll, BorderLayout.SOUTH);
  }

  private void installSorting() {
    var sorter = new TableRowSorter<>(tableModel);
    table.setRowSorter(sorter);

    var de = Collator.getInstance(Locale.GERMANY);

    sorter.setComparator(0, (a, b) -> de.compare(a.toString(), b.toString())); // Title
    sorter.setComparator(1, (a, b) -> de.compare(a.toString(), b.toString())); // Artist
    sorter.setComparator(2, (a, b) -> de.compare(a.toString(), b.toString())); // Genre
    sorter.setComparator(3, Comparator.comparingInt(a -> (Integer) a)); // Year
  }

  private void openFile() {
    // quick&dirty - don't do this at home!
    var exampleName = "songlist.txt";
    var examplePath = Path.of("build", "resources", "main", exampleName);

    var chooser = new JFileChooser();

    var startDir = examplePath.getParent().toFile();
    if (startDir.isDirectory()) chooser.setCurrentDirectory(startDir);

    var exampleFile = examplePath.toFile();
    if (exampleFile.isFile()) chooser.setSelectedFile(exampleFile);

    int ret = chooser.showOpenDialog(this);
    if (ret != JFileChooser.APPROVE_OPTION) return;

    var file = chooser.getSelectedFile();
    var items = loadItemsFromFile(file);
    tableModel.setItems(items);

    matchingRows = List.of();
    table.repaint();
    resultArea.setText("");
  }

  // CSV mit Semikolon: title;artist;genre;year
  private List<MediaItem> loadItemsFromFile(File file) {
    try {
      return MediaItem.loadFromPath(file.toPath());
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          this, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      return List.of();
    }
  }

  private void runSearch() {
    var query = queryField.getText().trim();
    if (query.isEmpty()) {
      matchingRows = List.of();
      table.repaint();
      resultArea.setText("");
      return;
    }

    Expr expr;
    try {
      expr = parseQuery(query);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          this, "Parse error: " + e.getMessage(), "Parse Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    var items = tableModel.getItems();

    // IMPORTANT: compute matching rows in MODEL coordinates
    matchingRows =
        IntStream.range(0, items.size())
            .filter(i -> Evaluator.matches(items.get(i), expr))
            .boxed()
            .toList();

    var text =
        matchingRows.stream()
            .map(i -> formatItem(items.get(i)))
            .collect(Collectors.joining("\n", "", "\n"));

    table.repaint();
    resultArea.setText(text);
  }

  private String formatItem(MediaItem item) {
    return "%s (%s, %s, %d)".formatted(item.title(), item.artist(), item.genre(), item.year());
  }

  private Expr parseQuery(String src) {
    return AstBuilders.fromQuery(src, new AstBuilderPattern()::translate);
  }
}
