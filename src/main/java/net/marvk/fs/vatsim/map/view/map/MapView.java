package net.marvk.fs.vatsim.map.view.map;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.javafx.scene.control.ContextMenuContent;
import de.saxsys.mvvmfx.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import lombok.extern.log4j.Log4j2;
import net.marvk.fs.vatsim.map.data.*;
import net.marvk.fs.vatsim.map.view.datadetail.DataDetailView;
import net.marvk.fs.vatsim.map.view.datadetail.DataDetailViewModel;
import net.marvk.fs.vatsim.map.view.painter.PainterExecutor;

import java.util.ResourceBundle;
import java.util.concurrent.*;

@Log4j2
public class MapView implements FxmlView<MapViewModel> {
    private static final double D_MOUSE_PAN = 1;
    private static final int MAX_SCALE = 16384;
    private static final int MIN_SCALE = 1;
    private static final double D_KEY_ZOOM = 0.1;
    private static final double D_KEY_PAN = 10;

    private static final int MAX_ITEMS_IN_CONTEXT_MENU_CATEGORY = 10;

    @FXML
    private StackPane stackPane;

    private final Canvas canvas;

    private final InputEventHandler inputEventHandler = new InputEventHandler();

    @InjectViewModel
    private MapViewModel viewModel;

    @InjectContext
    private Context context;

    @InjectResourceBundle
    private ResourceBundle resourceBundle;

    private final Renderer renderer = new Renderer();

    private MapContextMenu contextMenu;

    private ViewTuple<DataDetailView, DataDetailViewModel> detailView;

    @Inject
    public MapView(@Named("open_hand_cursor") final Cursor openHand, @Named("closed_hand_cursor") final Cursor closedHand) {
        this.canvas = new Canvas(100, 100);
        this.canvas.setFocusTraversable(true);

        this.canvas.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> canvas.requestFocus());

        this.canvas.cursorProperty().bind(Bindings.createObjectBinding(
                () -> {
                    if (inputEventHandler.controlDown.get() || inputEventHandler.controlWasDownOnClick.get()) {
                        return Cursor.CROSSHAIR;
                    } else if (inputEventHandler.rightMouseDown.get()) {
                        return Cursor.HAND;
                    } else if (inputEventHandler.leftMouseDown.get()) {
                        return closedHand;
                    } else if (inputEventHandler.middleMouseDown.get()) {
                        return Cursor.HAND;
                    } else {
                        return openHand;
                    }
                },
                inputEventHandler.leftMouseDown,
                inputEventHandler.leftMouseDrag,
                inputEventHandler.rightMouseDown,
                inputEventHandler.rightMouseDrag,
                inputEventHandler.middleMouseDown,
                inputEventHandler.controlDown
        ));
    }

    public void initialize() {
        this.contextMenu = new MapContextMenu();

        this.viewModel.viewWidthProperty().bind(this.canvas.widthProperty());
        this.viewModel.viewHeightProperty().bind(this.canvas.heightProperty());

        this.stackPane.getChildren().add(new MapCanvasPane(canvas));
        loadDetailView();

        this.stackPane.getChildren().add(detailViewOverlay());

        this.canvas.setOnScroll(inputEventHandler::onScroll);

        this.canvas.setOnMouseDragged(inputEventHandler::onDrag);
        this.canvas.setOnMousePressed(inputEventHandler::onStart);
        this.canvas.setOnMouseReleased(inputEventHandler::onRelease);
        this.canvas.setOnKeyPressed(inputEventHandler::onKeyPressed);
        this.canvas.setOnKeyReleased(inputEventHandler::onKeyReleased);
        this.canvas.setOnMouseClicked(inputEventHandler::onMouseClicked);

        this.canvas.setOnMouseMoved(inputEventHandler::onMove);

        final GraphicsContext c = this.canvas.getGraphicsContext2D();
        viewModel.fontSizeProperty().addListener((observable, oldValue, newValue) ->
                c.setFont(createFont(newValue.doubleValue()))
        );
        c.setFont(createFont(viewModel.getFontSize()));

        invalidateCanvas();

        viewModel.subscribe("REPAINT", (key, payload) -> invalidateCanvas());

        addContextMenuShadow();
    }

    private Font createFont(final double size) {
        return Font.font("B612 Mono", size);
    }

    private void addContextMenuShadow() {
        final AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(contextMenu.getShadowHolder());
        anchorPane.setMouseTransparent(true);
        stackPane.getChildren().add(anchorPane);
    }

    private void loadDetailView() {
        detailView = FluentViewLoader
                .fxmlView(DataDetailView.class)
                .context(context)
                .load();

        detailView.getViewModel().dataProperty().bindBidirectional(viewModel.selectedItemProperty());
        detailView.getView().visibleProperty().bind(Bindings.createBooleanBinding(
                () -> viewModel.getSelectedItem() != null,
                viewModel.selectedItemProperty()
        ));
    }

    private Parent detailViewOverlay() {
        final var pane = new BorderPane();
        pane.setPickOnBounds(false);
        pane.setRight(detailView.getView());

        return pane;
    }

    private synchronized void invalidateCanvas() {
        renderer.invalidate();
    }

    private class InputEventHandler {
        private double lastX = 0;
        private double lastY = 0;

        private final BooleanProperty leftMouseDown = new SimpleBooleanProperty();
        private final BooleanProperty leftMouseDrag = new SimpleBooleanProperty();
        private final BooleanProperty rightMouseDown = new SimpleBooleanProperty();
        private final BooleanProperty rightMouseDrag = new SimpleBooleanProperty();
        private final BooleanProperty middleMouseDown = new SimpleBooleanProperty();
        private final BooleanProperty controlDown = new SimpleBooleanProperty();
        private Point2D currentMousePosition = null;

        private final BooleanProperty controlWasDownOnClick = new SimpleBooleanProperty();

        public void onStart(final MouseEvent event) {
            controlDown.set(event.isControlDown());
            contextMenu.hideAndClear();

            final boolean controlAndPrimary = event.isPrimaryButtonDown() && event.isControlDown();
            controlWasDownOnClick.set(controlAndPrimary);

            if (!event.isControlDown()) {
                if (event.isSecondaryButtonDown()) {
                    if (controlDown.get()) {
                        viewModel.openClosest();
                    } else {
                        contextMenu.show(event.getX(), event.getY(), event.getScreenX(), event.getScreenY(), viewModel.showingContextMenu());
                    }
                } else if (event.isMiddleButtonDown()) {
                    viewModel.openClosest();
                }
            }

            leftMouseDown.set(event.isPrimaryButtonDown());
            rightMouseDown.set(event.isSecondaryButtonDown());
            middleMouseDown.set(event.isMiddleButtonDown());

            lastX = event.getX();
            lastY = event.getY();
        }

        public void onDrag(final MouseEvent event) {
            controlDown.set(event.isControlDown());
            leftMouseDrag.set(event.isPrimaryButtonDown());
            rightMouseDrag.set(event.isSecondaryButtonDown());

            if (controlWasDownOnClick.get()) {
                viewModel.setDistanceMeasureCanvas(new DistanceMeasure(
                        new Point2D(lastX, lastY),
                        new Point2D(event.getX(), event.getY()),
                        false
                ));
            } else if (leftMouseDown.get() && !controlDown.get()) {
                doDrag(event);
            }
        }

        public void onMove(final MouseEvent event) {
            controlDown.set(event.isControlDown());
            currentMousePosition = new Point2D(event.getX(), event.getY());
            refreshCurrentMousePosition();
        }

        public void onRelease(final MouseEvent event) {
            controlDown.set(event.isControlDown());
            if (controlWasDownOnClick.get()) {
                viewModel.setDistanceMeasureCanvas(new DistanceMeasure(
                        new Point2D(lastX, lastY),
                        new Point2D(event.getX(), event.getY()),
                        true
                ));
            }

            controlWasDownOnClick.set(false);

            leftMouseDown.set(event.isPrimaryButtonDown());
            rightMouseDown.set(event.isSecondaryButtonDown());
            middleMouseDown.set(event.isMiddleButtonDown());

            leftMouseDrag.set(event.isPrimaryButtonDown());
            rightMouseDrag.set(event.isSecondaryButtonDown());
        }

        public void refreshCurrentMousePosition() {
            if (currentMousePosition != null) {
                viewModel.mouseViewPositionProperty().set(currentMousePosition);
            }
        }

        public void onScroll(final ScrollEvent event) {
            contextMenu.hideAndClear();
            viewModel.recalculateMouseWorldPosition();
            zoomMap(event.getDeltaY());
        }

        public void onKeyPressed(final KeyEvent keyEvent) {
            switch (keyEvent.getCode()) {
                case CONTROL -> controlDown.set(true);
                case ESCAPE -> viewModel.setSelectedItem(null);
                // Discard controlDown when focus changes to search box
                case F -> {
                    if (controlDown.get()) {
                        controlDown.set(false);
                    }
                }
                case PLUS, E, ADD -> zoomMap(D_KEY_ZOOM);
                case MINUS, Q, SUBTRACT -> zoomMap(-D_KEY_ZOOM);
                case W -> panMap(0, -D_KEY_PAN);
                case A -> panMap(D_KEY_PAN, 0);
                case S -> panMap(0, D_KEY_PAN);
                case D -> panMap(-D_KEY_PAN, 0);
            }
        }

        public void onKeyReleased(final KeyEvent keyEvent) {
            switch (keyEvent.getCode()) {
                case CONTROL -> controlDown.set(false);
            }
        }

        private void doDrag(final MouseEvent event) {
            final double x = event.getX();
            final double y = event.getY();
            final double dx = (x - lastX) * D_MOUSE_PAN / MapVariables.WORLD_ASPECT_RATIO;
            final double dy = -(y - lastY) * D_MOUSE_PAN / MapVariables.WORLD_ASPECT_RATIO;

            lastX = x;
            lastY = y;

            panMap(dx, dy);
        }

        public void onMouseClicked(final MouseEvent event) {
            if (event.isStillSincePress()) {
                viewModel.setDistanceMeasureCanvas(null);
            }
        }

        private void panMap(final double dx, final double dy) {
            final double aspect = (
                    2 * MapVariables.WORLD_WIDTH * (1 / viewModel.scaleProperty().get())) /
                    viewModel.viewWidthProperty().get();

            final Point2D worldCenter = viewModel.getWorldCenter();
            viewModel.setWorldCenter(worldCenter.add(dx * aspect, dy * aspect));
        }

        private void zoomMap(final double d) {
            final double fScroll = Math.pow(viewModel.getScrollSpeed(), 1. / 4);
            final boolean scollingIn = d > 0;
            final double delta = scollingIn ? fScroll : 1.0 / fScroll;

            final double oldScale = viewModel.scaleProperty().get();
            final double newScale = Math.min(Math.max(oldScale * delta, MIN_SCALE), MAX_SCALE);

            if (Double.compare(oldScale, newScale) == 0) {
                return;
            }

            viewModel.scaleProperty().set(newScale);
            final Point2D worldCenter = viewModel.getWorldCenter();
            final Point2D mouseWorldPosition = viewModel.getMouseWorldPosition().multiply(-1);

            final double f = oldScale / newScale;

            viewModel.setWorldCenter(mouseWorldPosition.multiply(1 - f).add(worldCenter.multiply(f)));
        }
    }

    private final class MapContextMenu extends ContextMenu {
        private ContextMenuViewModel contextMenuViewModel = null;

        private final DataVisitor<String> labelVisitor = new LabelVisitor();
        private final Region shadowHolder = new Region();

        public MapContextMenu() {
            setSkin(createDefaultSkin());
            shadowHolder.prefWidthProperty().bind(widthProperty());
            shadowHolder.prefHeightProperty().bind(heightProperty());
        }

        @Override
        public void show(final Node anchor, final double screenX, final double screenY) {
            super.show(anchor, screenX, screenY);
        }

        public void show(final double x, final double y, final double screenX, final double screenY, final ContextMenuViewModel items) {
            shadowHolder.setTranslateX(x);
            shadowHolder.setTranslateY(y);
            shadowHolder.setVisible(true);

            setupItems(items);

            show(stackPane, screenX, screenY);
        }

        @Override
        public void hide() {
            shadowHolder.setVisible(false);
            viewModel.hideContextMenu();
            super.hide();
        }

        private void setupItems(final ContextMenuViewModel contextMenuViewModel) {
            getItems().clear();

            this.contextMenuViewModel = new ContextMenuViewModel(contextMenuViewModel);

            boolean addSeparator = false;

            final int contextMenuItemCount = this.contextMenuViewModel.getContextMenuItems().size();
            for (int i = 0; i < contextMenuItemCount; i++) {
                final ContextMenuItems<? extends Data> contextMenuItems =
                        this.contextMenuViewModel.getContextMenuItems().get(i);
                final ObservableList<? extends Data> items = contextMenuItems.getItems();

                final boolean notEmpty = !items.isEmpty();

                if (addSeparator && notEmpty) {
                    getItems().add(new SeparatorMenuItem());
                    addSeparator = false;
                }

                if (notEmpty) {
                    final MenuItem header = header(contextMenuItems.getLabel());
                    getItems().add(header);
                    addSeparator = true;
                }

                for (int j = 0; j < Math.min(items.size(), MAX_ITEMS_IN_CONTEXT_MENU_CATEGORY); j++) {
                    final Data item = items.get(j);
                    final MenuItem data = item(item);
                    getItems().add(data);
                    data.setOnAction(e -> {
                        hide();
                        if (inputEventHandler.controlDown.get()) {
                            viewModel.goToItem();
                        }
                    });
                }

                if (items.size() > MAX_ITEMS_IN_CONTEXT_MENU_CATEGORY) {
                    getItems().add(header(" ...%d more".formatted(items.size() - MAX_ITEMS_IN_CONTEXT_MENU_CATEGORY)));
                }
            }

            if (getItems().isEmpty()) {
                getItems().add(header(resourceBundle.getString("map.nothing_here")));
            }

            final ContextMenuContent cmc = (ContextMenuContent) getSkin().getNode();

            final ObservableList<Node> cmcChildren = cmc.getItemsContainer().getChildren();

            for (final Node child : cmcChildren) {
                if (!(child instanceof ContextMenuContent.MenuItemContainer)) {
                    continue;
                }

                final MenuItem menuItem = ((ContextMenuContent.MenuItemContainer) child).getItem();

                if (!(menuItem instanceof DataMenuItem)) {
                    continue;
                }

                child.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        final Data item = ((DataMenuItem) menuItem).getData();

                        if (item != null) {
                            viewModel.setSelectedItem(item);
                        }
                    }
                });
            }
        }

        private MenuItem item(final Data data) {
            final MenuItem menuItem = new DataMenuItem(" " + labelVisitor.visit(data), data);
            menuItem.getStyleClass().add("menu-item-data");
            return menuItem;
        }

        private MenuItem header(final String label) {
            final MenuItem header = new MenuItem(label);
            header.getStyleClass().add("menu-item-header");
            return header;
        }

        public void hideAndClear() {
            if (isShowing()) {
                viewModel.setSelectedItem(null);
                hide();
            }
        }

        public Node getShadowHolder() {
            shadowHolder.getStyleClass().add("context-menu-drop-shadow-holder");
            return shadowHolder;
        }

        private class LabelVisitor implements DataVisitor<String> {
            @Override
            public String visit(final Airport airport) {
                return airport.getIcao();
            }

            @Override
            public String visit(final FlightInformationRegionBoundary firb) {
                return firb.getIcao() + (firb.isOceanic() ? " Oceanic" : "");
            }

            @Override
            public String visit(final Pilot visitor) {
                return visitor.getCallsign();
            }

            @Override
            public String visit(final UpperInformationRegion upperInformationRegion) {
                return upperInformationRegion.getIcao();
            }
        }

    }

    private static class DataMenuItem extends MenuItem {
        private final Data data;

        public DataMenuItem(final String text, final Data data) {
            super(text);
            this.data = data;
        }

        public Data getData() {
            return data;
        }
    }

    private final class Renderer {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Semaphore renderSemaphore = new Semaphore(1);

        private final DoubleProperty lastFrameTimeMillis = new SimpleDoubleProperty();

        private int frame = 0;

        private Renderer() {
            start();
        }

        private void start() {
            executor.submit(() -> {
                while (true) {
                    try {
                        renderSemaphore.acquire();
                        log.trace("Drawing frame %d".formatted(frame));
                        final long start = System.nanoTime();
                        redraw().get();
                        final long frameTimeNanos = System.nanoTime() - start;
                        viewModel.onFrameCompleted(frameTimeNanos);
                        final double frameTimeMillis = frameTimeNanos / 1000000.;
                        lastFrameTimeMillis.set(frameTimeMillis);
                        log.debug("Drew frame %d in %sms".formatted(frame, frameTimeMillis));
                    } catch (final InterruptedException | ExecutionException e) {
                        log.error("Failed to draw frame %d".formatted(frame), e);
                    } finally {
                        frame += 1;
                    }
                }
            });
        }

        private RunnableFuture<Void> redraw() {
            final FutureTask<Void> task = new FutureTask<>(() -> {
                for (final PainterExecutor<?> painterExecutor : viewModel.getPainterExecutors()) {
                    painterExecutor.paint(canvas.getGraphicsContext2D());
                    log.trace(painterExecutor.getName() + " finished in " + (painterExecutor.getLastDurationNanos() / 1000000.0) + "ms");
                }
            }, null);

            Platform.runLater(task);

            return task;
        }

        public void invalidate() {
            incrementSemaphore();
        }

        private void incrementSemaphore() {
            if (renderSemaphore.availablePermits() == 0) {
                renderSemaphore.release();
            }
        }
    }
}








































