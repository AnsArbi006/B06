package filter.app;

import filter.model.Genre;
import filter.model.MediaItem;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

class MediaTableModel extends AbstractTableModel {

  private final List<MediaItem> items = new ArrayList<>();

  private final String[] columns = {"Title", "Artist", "Genre", "Year"};

  public void setItems(List<MediaItem> newItems) {
    items.clear();
    items.addAll(newItems);
    fireTableDataChanged();
  }

  public List<MediaItem> getItems() {
    return items;
  }

  @Override
  public int getRowCount() {
    return items.size();
  }

  @Override
  public int getColumnCount() {
    return columns.length;
  }

  @Override
  public String getColumnName(int column) {
    return columns[column];
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    var item = items.get(rowIndex);
    return switch (columnIndex) {
      case 0 -> item.title();
      case 1 -> item.artist();
      case 2 -> item.genre();
      case 3 -> item.year();
      default -> null;
    };
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    var old = items.get(rowIndex);
    var updated =
        switch (columnIndex) {
          case 0 -> new MediaItem((String) aValue, old.artist(), old.genre(), old.year());
          case 1 -> new MediaItem(old.title(), (String) aValue, old.genre(), old.year());
          case 2 ->
              new MediaItem(
                  old.title(), old.artist(), Genre.fromString(aValue.toString()), old.year());
          case 3 ->
              new MediaItem(
                  old.title(), old.artist(), old.genre(), Integer.parseInt(aValue.toString()));
          default -> old;
        };
    items.set(rowIndex, updated);
    fireTableRowsUpdated(rowIndex, rowIndex);
  }
}
