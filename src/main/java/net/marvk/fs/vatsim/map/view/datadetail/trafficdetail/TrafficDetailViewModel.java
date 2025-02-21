package net.marvk.fs.vatsim.map.view.datadetail.trafficdetail;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListExpression;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import net.marvk.fs.vatsim.map.data.Airline;
import net.marvk.fs.vatsim.map.data.Airport;
import net.marvk.fs.vatsim.map.data.FlightPlan;
import net.marvk.fs.vatsim.map.data.Pilot;
import net.marvk.fs.vatsim.map.view.datadetail.detailsubview.DetailSubViewModel;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

public class TrafficDetailViewModel extends DetailSubViewModel<ListExpression<FlightPlan>> {
    private static final Comparator<FlightPlan> CALLSIGN_COMPARATOR;

    static {
        final Comparator<FlightPlan> airline = Comparator.comparing(
                e -> e.getPilot().getAirline(),
                Comparator.nullsLast(Airline.comparingByIcao())
        );

        final Comparator<FlightPlan> flightNumber = Comparator.comparing(
                e -> e.getPilot().getFlightNumber(),
                Comparator.nullsLast(String::compareTo)
        );

        final Comparator<FlightPlan> callsign = Comparator.comparing(
                e -> e.getPilot().getCallsign(),
                Comparator.nullsLast(String::compareTo)
        );

        CALLSIGN_COMPARATOR = airline.thenComparing(flightNumber).thenComparing(callsign);
    }

    private final StringProperty title = new SimpleStringProperty();

    private final ObjectProperty<TrafficDetailView.Type> type = new SimpleObjectProperty<>();

    private final ReadOnlyListWrapper<FlightPlan> filteredSortedData = new ReadOnlyListWrapper<>();

    private final StringProperty query = new ReadOnlyStringWrapper();

    public TrafficDetailViewModel() {
        data.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                filteredSortedData.set(FXCollections.emptyObservableList());
            } else {
                final FilteredList<FlightPlan> filteredList = new FilteredList<>(newValue);
                filteredList.predicateProperty().bind(Bindings.createObjectBinding(
                        () -> this::matchesQuery,
                        query
                ));
                final SortedList<FlightPlan> sortedList = new SortedList<>(filteredList, CALLSIGN_COMPARATOR);
                filteredSortedData.set(sortedList);
            }
        });
    }

    private boolean matchesQuery(final FlightPlan flightPlan) {
        if (query.get() == null || query.get().isEmpty()) {
            return true;
        }

        final Pilot pilot = flightPlan.getPilot();

        if (StringUtils.containsIgnoreCase(pilot.getCallsign(), query.get())) {
            return true;
        }

        if (StringUtils.containsIgnoreCase(pilot.getRealName(), query.get())) {
            return true;
        }

        final Airport airport = switch (type.get()) {
            case ARRIVAL -> flightPlan.getDepartureAirport();
            case DEPARTURE -> flightPlan.getArrivalAirport();
            default -> null;
        };

        if (airport == null) {
            return false;
        }

        if (StringUtils.containsIgnoreCase(airport.getIcao(), query.get())) {
            return true;
        }

        if (airport.getNames().stream().anyMatch(e -> StringUtils.containsIgnoreCase(e.get(), query.get()))) {
            return true;
        }

        return false;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(final String title) {
        this.title.set(title);
    }

    public TrafficDetailView.Type getType() {
        return type.get();
    }

    public ObjectProperty<TrafficDetailView.Type> typeProperty() {
        return type;
    }

    public void setType(final TrafficDetailView.Type type) {
        this.type.set(type);
    }

    public ReadOnlyListProperty<FlightPlan> getFilteredSortedData() {
        return filteredSortedData.getReadOnlyProperty();
    }

    public String getQuery() {
        return query.get();
    }

    public StringProperty queryProperty() {
        return query;
    }

    public void setQuery(final String query) {
        this.query.set(query);
    }

    public void clearQuery() {
        query.set("");
    }

}
