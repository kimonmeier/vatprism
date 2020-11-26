package net.marvk.fs.vatsim.map.view.painter;

import javafx.scene.canvas.Canvas;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

public class PainterExecutor<T> {
    private final Painter<T> painter;
    private final Supplier<Collection<T>> paintablesSupplier;
    private final String name;

    private long lastDurationNanos = 0L;

    public PainterExecutor(final Painter<T> painter) {
        this((String) null, painter);
    }

    public PainterExecutor(final String name, final Painter<T> painter) {
        this(name, painter, () -> Collections.singletonList(null));
    }

    public PainterExecutor(final Painter<T> painter, final Supplier<Collection<T>> paintablesSupplier) {
        this(null, painter, paintablesSupplier);
    }

    public PainterExecutor(final String name, final Painter<T> painter, final Supplier<Collection<T>> paintablesSupplier) {
        this.painter = painter;
        this.paintablesSupplier = paintablesSupplier;
        this.name = name;
    }

    public void paint(final Canvas canvas) {
        final long start = System.nanoTime();
        for (final T t : paintablesSupplier.get()) {
            painter.paint(canvas, t);
        }
        lastDurationNanos = System.nanoTime() - start;
    }

    public String getName() {
        return name;
    }

    public long getLastDurationNanos() {
        return lastDurationNanos;
    }
}