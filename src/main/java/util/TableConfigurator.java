package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import service.SettingsService;

public final class TableConfigurator {
    private static final SettingsService SETTINGS = new SettingsService();
    private static final Map<String, List<String>> cache = new HashMap<>();

    private TableConfigurator() {}

    public static <T> void apply(TableView<T> table, String tableKey) {
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        List<String> order = loadOrder(tableKey);
        if (order.isEmpty()) return;
        ObservableList<TableColumn<T, ?>> cols = table.getColumns();
        List<TableColumn<T, ?>> reordered = new ArrayList<>();
        for (String name : order) {
            for (TableColumn<T, ?> c : cols) {
                if (c.getText().equals(name)) {
                    reordered.add(c);
                    break;
                }
            }
        }
        for (TableColumn<T, ?> c : cols) if (!reordered.contains(c)) reordered.add(c);
        cols.setAll(reordered);
    }

    public static <T> void persist(TableView<T> table, String tableKey) {
        List<String> order = table.getColumns().stream().map(TableColumn::getText).toList();
        cache.put(tableKey, order);
        saveAll();
    }

    private static List<String> loadOrder(String key) {
        if (cache.isEmpty()) {
            String data = SETTINGS.loadTableConfig();
            if (data != null && !data.isBlank()) {
                String[] blocks = data.split(";");
                for (String block : blocks) {
                    String[] pair = block.split("=", 2);
                    if (pair.length == 2) {
                        cache.put(pair[0], List.of(pair[1].split(",")));
                    }
                }
            }
        }
        return cache.getOrDefault(key, List.of());
    }

    private static void saveAll() {
        StringBuilder sb = new StringBuilder();
        cache.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(k).append('=').append(String.join(",", v));
        });
        SETTINGS.saveTableConfig(sb.toString());
    }
}
