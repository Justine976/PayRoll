package ui.navigation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.Parent;

public class ViewManager {
    private final Map<String, Supplier<Parent>> factories = new HashMap<>();
    private final Map<String, Parent> cache = new HashMap<>();

    public void register(String key, Supplier<Parent> factory) {
        factories.put(key, factory);
    }

    public Parent getView(String key) {
        return cache.computeIfAbsent(key, k -> {
            Supplier<Parent> factory = factories.get(k);
            if (factory == null) {
                throw new IllegalArgumentException("No view registered for key: " + k);
            }
            return factory.get();
        });
    }

    public void clear() {
        cache.clear();
        factories.clear();
    }
}
