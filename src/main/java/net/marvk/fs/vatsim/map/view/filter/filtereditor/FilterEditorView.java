package net.marvk.fs.vatsim.map.view.filter.filtereditor;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectResourceBundle;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;
import net.marvk.fs.vatsim.map.data.ControllerRating;
import net.marvk.fs.vatsim.map.data.ControllerType;
import net.marvk.fs.vatsim.map.data.Filter;
import net.marvk.fs.vatsim.map.view.filter.FilterStringListViewModel;
import org.kordamp.ikonli.fileicons.FileIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.octicons.Octicons;

import java.util.ResourceBundle;
import java.util.function.Function;

@Log4j2
public class FilterEditorView implements FxmlView<FilterEditorViewModel> {
    @FXML
    private CheckBox pilotsCheckbox;
    @FXML
    private CheckBox controllersCheckbox;

    @FXML
    private ToggleButton enabled;

    @FXML
    private Parent container;

    @FXML
    private TextField filterName;

    @FXML
    private ColorPicker textColorPicker;
    @FXML
    private ColorPicker backgroundColorPicker;

    @FXML
    private ToggleGroup callsignCidAndOr;
    @FXML
    private Label callsignCidOr;
    @FXML
    private Label callsignCidAnd;

    @FXML
    private ListView<FilterStringListViewModel> callsignList;
    @FXML
    private ToggleButton callsignRegex;
    @FXML
    private TextField callsignInput;
    @FXML
    private Button callsignSubmit;

    @FXML
    private ListView<FilterStringListViewModel> cidList;
    @FXML
    private ToggleButton cidRegex;
    @FXML
    private TextField cidInput;
    @FXML
    private Button cidSubmit;

    @FXML
    private ToggleGroup departuresArrivalsAndOr;
    @FXML
    private Label departuresArrivalsOr;
    @FXML
    private Label departuresArrivalsAnd;

    @FXML
    private ListView<FilterStringListViewModel> departuresList;
    @FXML
    private ToggleButton departuresRegex;
    @FXML
    private TextField departuresInput;
    @FXML
    private Button departuresSubmit;

    @FXML
    private ListView<FilterStringListViewModel> arrivalsList;
    @FXML
    private ToggleButton arrivalsRegex;
    @FXML
    private TextField arrivalsInput;
    @FXML
    private Button arrivalsSubmit;

    @FXML
    private ListView<ControllerRating> ratingsList;
    @FXML
    private ListView<ControllerType> facilitiesList;

    @FXML
    private ListView<Filter.FlightStatus> flightStatusList;
    @FXML
    private CheckBox flightPlanFiled;
    @FXML
    private ListView<Filter.FlightType> flightType;

    @InjectViewModel
    private FilterEditorViewModel viewModel;

    @InjectResourceBundle
    private ResourceBundle resourceBundle;

    public void initialize() {
        container.disableProperty().bind(viewModel.enabledProperty().not());

        new StringFilterList(
                callsignList,
                callsignRegex,
                callsignInput,
                callsignSubmit,
                viewModel.getCallsigns(),
                resourceBundle
        );
        new StringFilterList(
                cidList,
                cidRegex,
                cidInput,
                cidSubmit,
                viewModel.getCids(),
                resourceBundle
        );
        new StringFilterList(
                departuresList,
                departuresRegex,
                departuresInput,
                departuresSubmit,
                viewModel.getDepartures(),
                resourceBundle
        );
        new StringFilterList(
                arrivalsList,
                arrivalsRegex,
                arrivalsInput,
                arrivalsSubmit,
                viewModel.getArrivals(),
                resourceBundle
        );

        new MultipleSelection<>(
                ratingsList,
                viewModel.getAvailableRatings(),
                viewModel.ratingsProperty(),
                e -> "%s (%s)".formatted(e.getShortName(), e.getLongName())
        );
        new MultipleSelection<>(
                facilitiesList,
                viewModel.getAvailableFacilities(),
                viewModel.facilitiesProperty(),
                Enum::toString
        );
        new MultipleSelection<>(
                flightStatusList,
                viewModel.getAvailableFlightStatuses(),
                viewModel.flightStatusesProperty(),
                Enum::toString
        );
        new MultipleSelection<>(
                flightType,
                viewModel.getAvailableFlightTypes(),
                viewModel.flightTypesProperty(),
                Enum::toString
        );

        setupToggleGroup(callsignCidAndOr, callsignCidOr, callsignCidAnd, viewModel.callsignsCidsOperatorProperty());
        setupToggleGroup(departuresArrivalsAndOr, departuresArrivalsOr, departuresArrivalsAnd, viewModel.departuresArrivalsOperatorProperty());

        bindFilterName();

        bindColorPickers();
        bindType();

        viewModel.flightPlanFiledProperty().bindBidirectional(flightPlanFiled.selectedProperty());
        bindFilterEnabled();
    }

    private void bindFilterName() {
        filterName.textProperty()
                  .addListener((observable, oldValue, newValue) -> viewModel.nameProperty().set(newValue));
        viewModel.nameProperty()
                 .addListener((observable, oldValue, newValue) -> filterName.textProperty().set(newValue));
    }

    private void bindFilterEnabled() {
        viewModel.filterEnabledProperty().bindBidirectional(enabled.selectedProperty());
        ((FontIcon) enabled.getGraphic()).iconCodeProperty().bind(Bindings.createObjectBinding(
                () -> enabled.isSelected() ? Octicons.EYE_16 : Octicons.EYE_CLOSED_16,
                enabled.selectedProperty()
        ));
        final Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(Bindings.createStringBinding(
                () -> enabled.isSelected() ? resourceBundle.getString("common.enable") : resourceBundle.getString("common.disable"),
                enabled.selectedProperty()
        ));
        enabled.setTooltip(tooltip);
    }

    private void bindColorPickers() {
        bindColorPicker(textColorPicker, viewModel.textColorProperty());
        bindColorPicker(backgroundColorPicker, viewModel.backgroundColorProperty());
    }

    private void bindType() {
        pilotsCheckbox.disableProperty().bind(controllersCheckbox.selectedProperty().not());
        controllersCheckbox.disableProperty().bind(pilotsCheckbox.selectedProperty().not());

        final ObservableList<Filter.Type> viewModelSelected = viewModel.getFilterTypes();
        viewModelSelected.addListener((ListChangeListener<Filter.Type>) c -> {
            pilotsCheckbox.setSelected(viewModelSelected.contains(Filter.Type.PILOT));
            controllersCheckbox.setSelected(viewModelSelected.contains(Filter.Type.CONTROLLER));
        });

        setupCheckbox(pilotsCheckbox, Filter.Type.PILOT);
        setupCheckbox(controllersCheckbox, Filter.Type.CONTROLLER);
    }

    private void setupCheckbox(final CheckBox controllersCheckbox, final Filter.Type controller) {
        final ObservableList<Filter.Type> viewModelSelected = viewModel.getFilterTypes();
        controllersCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                viewModelSelected.add(controller);
            } else {
                viewModelSelected.remove(controller);
            }
        });
    }

    private static void bindColorPicker(final ColorPicker colorPicker, final ObjectProperty<Color> colorProperty) {
        colorPicker.valueProperty()
                   .addListener((observable, oldValue, newValue) -> colorProperty.set(newValue));
        colorProperty
                .addListener((observable, oldValue, newValue) -> colorPicker.valueProperty().set(newValue));
    }

    private static void setupToggleGroup(final ToggleGroup toggleGroup, final Label or, final Label and, final ObjectProperty<Filter.Operator> operator) {
        or.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                toggleGroup.selectToggle(toggleGroup.getToggles().get(0));
                e.consume();
            }
        });
        and.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                toggleGroup.selectToggle(toggleGroup.getToggles().get(1));
                e.consume();
            }
        });

        operator.addListener((observable, oldValue, newValue) ->
                toggleGroup.selectToggle(switch (newValue) {
                    case OR -> toggleGroup.getToggles().get(0);
                    case AND -> toggleGroup.getToggles().get(1);
                })
        );
        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) ->
                operator.set(switch (toggleGroup.getToggles().indexOf(toggleGroup.getSelectedToggle())) {
                    case 0 -> Filter.Operator.OR;
                    case 1 -> Filter.Operator.AND;
                    default -> throw new IllegalStateException();
                })
        );
    }

    @FXML
    private void save() {
        viewModel.save();
    }

    private static class StringMappedListCell<T> extends ListCell<T> {

        private final Function<T, String> cellValueMapper;

        public StringMappedListCell(final Function<T, String> cellValueMapper) {
            this.cellValueMapper = cellValueMapper;
        }

        @Override
        public void updateItem(final T item, final boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setText(null);
            } else {
                setText(cellValueMapper.apply(item));
            }
        }
    }

    private static class StringFilterList {

        private final ObjectProperty<FilterStringListViewModel> selected = new SimpleObjectProperty<>();
        private final BooleanProperty inputDisabled = new SimpleBooleanProperty();
        private final IntegerProperty height = new SimpleIntegerProperty(24);
        private final ListView<FilterStringListViewModel> list;

        private final ToggleButton regex;
        private final TextField input;
        private final Button submit;
        private final ObservableList<FilterStringListViewModel> items;
        private final ResourceBundle resourceBundle;

        public StringFilterList(
                final ListView<FilterStringListViewModel> list,
                final ToggleButton regex,
                final TextField input,
                final Button submit,
                final ObservableList<FilterStringListViewModel> items,
                final ResourceBundle resourceBundle
        ) {
            this.list = list;
            this.regex = regex;
            this.input = input;
            this.submit = submit;
            this.items = items;
            this.resourceBundle = resourceBundle;

            list.setItems(items);
            final ChangeListener<Boolean> focusChange = (observable, oldValue, newValue) -> {
                if (!list.isFocused() && !regex.isFocused() && !input.isFocused() && !submit.isFocused()) {
                    clearSelection();
                }
            };
            list.focusedProperty().addListener(focusChange);
            regex.focusedProperty().addListener(focusChange);
            input.focusedProperty().addListener(focusChange);
            submit.focusedProperty().addListener(focusChange);

            selected.bind(list.getSelectionModel().selectedItemProperty());
            inputDisabled.bind(input.textProperty().isEmpty().or(input.textProperty().isNull()));

            list.setCellFactory(param -> new FilterStringListViewModelListCell(resourceBundle));
            input.setOnAction(e -> addToList());
            submit.setOnAction(e -> addToList());

            final FontIcon submitGraphic = (FontIcon) submit.getGraphic();

            submitGraphic.iconCodeProperty().bind(Bindings.createObjectBinding(
                    () -> selected.get() == null ? Octicons.PLUS_16 : Octicons.PENCIL_16,
                    selected
            ));
            selected.addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    input.setText(null);
                } else {
                    input.setText(newValue.getContent());
                }
            });
            list.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    final EventTarget target = e.getTarget();

                    if (target instanceof FilterStringListViewModelListCell) {
                        final FilterStringListViewModelListCell cell = (FilterStringListViewModelListCell) target;
                        if (cell.getValue() == null) {
                            clearSelection();
                        }
                    } else if (target instanceof Group) {
                        clearSelection();
                    }
                    e.consume();
                }
            });
            regex.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (selected.get() != null) {
                    selected.get().setRegex(newValue);
                }
            });
            final Tooltip regexTooltip = new Tooltip();
            regexTooltip.textProperty().bind(Bindings.createStringBinding(
                    () -> {
                        final String matchingTypeKey = regex.isSelected() ? "filter_editor.regex" : "filter_editor.wildcard";
                        return resourceBundle
                                .getString("filter_editor.tooltip.matching_type")
                                .replace("{matching_type}", resourceBundle.getString(matchingTypeKey));
                    },
                    regex.selectedProperty()
            ));
            regex.setTooltip(regexTooltip);

            regex.prefHeightProperty().bind(height);
            input.prefHeightProperty().bind(height);
            submit.prefHeightProperty().bind(height);
            submit.disableProperty().bind(inputDisabled);

            final FontIcon regexGraphic = (FontIcon) regex.getGraphic();

            regexGraphic.iconCodeProperty().bind(Bindings.createObjectBinding(
                    () -> regex.isSelected() ? FileIcons.REGEX : Octicons.TYPOGRAPHY_16,
                    regex.selectedProperty()
            ));
        }

        private void clearSelection() {
            list.getSelectionModel().clearSelection();
        }

        private void addToList() {
            if (selected.get() == null && !inputDisabled.get()) {
                list.getItems().add(new FilterStringListViewModel(regex.isSelected(), input.getText()));
                input.setText(null);
            } else {
                if (!inputDisabled.get()) {
                    selected.get().setContent(input.getText());
                }
                selected.get().setRegex(regex.isSelected());
            }
        }

        private static class FilterStringListViewModelListCell extends ListCell<FilterStringListViewModel> {

            private final ReadOnlyObjectWrapper<FilterStringListViewModel> value = new ReadOnlyObjectWrapper<>();
            private final ResourceBundle resourceBundle;
            private HBox content;

            private Label label;
            private Button button;
            private FontIcon typeIcon;
            private FontIcon errorIcon;
            private Tooltip errorTooltip;
            private VBox errorIconHolder;

            public FilterStringListViewModelListCell(final ResourceBundle resourceBundle) {
                this.resourceBundle = resourceBundle;
            }

            private Parent content(final FilterStringListViewModel item) {
                if (content == null) {
                    label = new Label();
                    button = new Button();
                    final FontIcon buttonIcon = new FontIcon(Octicons.TRASHCAN_16);
                    buttonIcon.getStyleClass().addAll("filter-list-icon");
                    button.setGraphic(buttonIcon);

                    final Region region = new Region();
                    region.setPrefWidth(0);
                    region.setPrefHeight(0);
                    HBox.setHgrow(region, Priority.ALWAYS);

                    typeIcon = new FontIcon();
                    typeIcon.getStyleClass().add("filter-list-icon");
                    final VBox fontIconHolder = new VBox(typeIcon);
                    fontIconHolder.setAlignment(Pos.CENTER);
                    fontIconHolder.setPadding(new Insets(0, 20, 0, 5));

                    errorIcon = new FontIcon(Octicons.ALERT_16);
                    errorIcon.getStyleClass().addAll("filter-list-icon", "filter-list-icon-error");
                    errorIconHolder = new VBox(errorIcon);
                    errorIconHolder.setAlignment(Pos.CENTER);
                    errorIconHolder.setPadding(new Insets(0, 5, 0, 5));
                    errorTooltip = new Tooltip(resourceBundle.getString("filter_editor.invalid_regex"));
                    errorTooltip.setShowDelay(Duration.ZERO);

                    content = new HBox(fontIconHolder, label, errorIconHolder, region, button);
                    content.setAlignment(Pos.CENTER_LEFT);
                }

                button.setOnAction(e -> getListView().getItems().remove(item));
                if (item == null) {
                    errorIcon.visibleProperty().unbind();
                    errorIcon.setVisible(false);
                } else {
                    errorIcon.visibleProperty().bind(item.validProperty().not());
                    errorIcon.visibleProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue) {
                            Tooltip.install(errorIconHolder, errorTooltip);
                        } else {
                            Tooltip.uninstall(errorIconHolder, errorTooltip);
                        }
                    });
                }
                label.textProperty().bind(item.contentProperty());
                typeIcon.iconCodeProperty().bind(Bindings.createObjectBinding(
                        () -> item.isRegex() ? FileIcons.REGEX : Octicons.TYPOGRAPHY_16,
                        item.regexProperty()
                ));

                return content;
            }

            @Override
            public void updateItem(final FilterStringListViewModel item, final boolean empty) {
                super.updateItem(item, empty);
                value.set(item);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(content(item));
                }
            }

            public FilterStringListViewModel getValue() {
                return value.get();
            }

            public ReadOnlyObjectProperty<FilterStringListViewModel> valueProperty() {
                return value.getReadOnlyProperty();
            }
        }
    }

    private static class MultipleSelection<T> {
        private boolean changing = false;

        public MultipleSelection(
                final ListView<T> listView,
                final ReadOnlyListProperty<T> available,
                final ListProperty<T> selectedInModel,
                final Function<T, String> cellValueMapper
        ) {
            listView.setItems(available);
            listView.setCellFactory(param -> new StringMappedListCell<>(cellValueMapper));
            final MultipleSelectionModel<T> selectionModel = listView.getSelectionModel();
            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

            selectionModel.getSelectedIndices().addListener((ListChangeListener<Integer>) c -> {
                if (!changing) {
                    changing = true;

                    while (c.next()) {
                        for (final Integer i : c.getRemoved()) {
                            final T t = available.get(i);

                            if (selectedInModel.contains(t)) {
                                selectedInModel.remove(t);
                            }
                        }

                        for (final Integer i : c.getAddedSubList()) {
                            final T t = available.get(i);

                            if (!selectedInModel.contains(t)) {
                                selectedInModel.add(t);
                            }
                        }
                    }

                    changing = false;
                }
            });

            selectedInModel.addListener((ListChangeListener<T>) c -> {
                if (!changing) {
                    changing = true;

                    while (c.next()) {
                        for (final T t : c.getRemoved()) {
                            selectionModel.clearSelection(available.indexOf(t));
                        }

                        for (final T t : c.getAddedSubList()) {
                            selectionModel.select(available.indexOf(t));
                        }
                    }

                    changing = false;
                }
            });
        }
    }
}



























