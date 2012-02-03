package org.openbel.belframework.kam;

import java.awt.Component;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * {@link TextAreaCellRenderer} defines a
 * {@link TableCellRenderer table cell renderer} that determines row height of
 * a {@link TableColumn table column} suitable for wrapping text.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class TextAreaCellRenderer extends JTextArea implements TableCellRenderer {
    private static final long serialVersionUID = -7417398212391533532L;

    private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

    private final Map<JTable, Map<Object, Map<Object, Integer>>> tablecellSizes =
            new HashMap<JTable, Map<Object, Map<Object, Integer>>>();

    public TextAreaCellRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v,
            boolean selected,
            boolean hasFocus, int row, int column) {
        // set the Font, Color, etc.
        renderer.getTableCellRendererComponent(t, v,
                selected, hasFocus, row, column);
        setForeground(renderer.getForeground());
        setBackground(renderer.getBackground());
        setBorder(renderer.getBorder());
        setFont(renderer.getFont());
        setText(renderer.getText());

        TableColumnModel columnModel = t.getColumnModel();
        setSize(columnModel.getColumn(column).getWidth(), 0);
        int height_wanted = (int) getPreferredSize().getHeight();
        addSize(t, row, column, height_wanted);
        height_wanted = findTotalMaximumRowSize(t, row);
        if (height_wanted != t.getRowHeight(row)) {
            t.setRowHeight(row, height_wanted);
        }
        return this;
    }

    private void addSize(JTable t, int row, int column, int height) {
        Map<Object, Map<Object, Integer>> rowsMap = tablecellSizes.get(t);
        if (rowsMap == null) {
            tablecellSizes.put(t, rowsMap = new HashMap<Object, Map<Object, Integer>>());
        }
        Map<Object, Integer> rowheightsMap = rowsMap.get(row);
        if (rowheightsMap == null) {
            rowsMap.put(row, rowheightsMap = new HashMap<Object, Integer>());
        }
        rowheightsMap.put(column, height);
    }

    private int findTotalMaximumRowSize(JTable t, int row) {
        int maximum_height = 0;
        Enumeration<TableColumn> columns = t.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn tc = columns.nextElement();
            TableCellRenderer cellRenderer = tc.getCellRenderer();
            if (cellRenderer instanceof TextAreaCellRenderer) {
                TextAreaCellRenderer tar = (TextAreaCellRenderer) cellRenderer;
                maximum_height = Math.max(maximum_height,
                        tar.findMaximumRowSize(t, row));
            }
        }
        return maximum_height;
    }

    private int findMaximumRowSize(JTable t, int row) {
        Map<Object, Map<Object, Integer>> rows = tablecellSizes.get(t);
        if (rows == null) return 0;
        Map<Object, Integer> rowheights = rows.get(row);
        if (rowheights == null) return 0;
        int maximum_height = 0;
        for (Map.Entry<Object, Integer> entry : rowheights.entrySet()) {
            int cellHeight = entry.getValue();
            maximum_height = Math.max(maximum_height, cellHeight);
        }
        return maximum_height;
    }
}