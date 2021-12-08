package net.marvk.fs.vatsim.map.view.datatable.airportstable;

import com.google.inject.Inject;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.geometry.Point2D;
import net.marvk.fs.vatsim.map.GeomUtil;
import net.marvk.fs.vatsim.map.data.Airport;
import net.marvk.fs.vatsim.map.data.Country;
import net.marvk.fs.vatsim.map.view.TextFlowHighlighter;
import net.marvk.fs.vatsim.map.view.datatable.AbstractTableView;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

public class AirportsTableView extends AbstractTableView<AirportsTableViewModel, Airport> {
    @Inject
    public AirportsTableView(final TextFlowHighlighter textFlowHighlighter) {
        super(textFlowHighlighter);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void initializeColumns() {
        this.<String>newColumnBuilder()
            .titleKey("common.icao")
            .stringObservableValueFactory(Airport::icaoProperty)
            .sortable()
            .mono(true)
            .widthFactor(0.5)
            .build();

        this.<String>newColumnBuilder()
            .titleKey("table.airports.name")
            .stringObservableValueFactory(AirportsTableView::airportName)
            .sortable()
            .mono(false)
            .widthFactor(2.5)
            .build();

        this.<Point2D>newColumnBuilder()
            .titleKey("table.airports.longitude")
            .objectObservableValueFactory(Airport::positionProperty)
            .toStringMapper(e -> GeomUtil.formatLon(e.getY()))
            .sortable(Comparator.comparingDouble(Point2D::getY))
            .mono(true)
            .widthFactor(0.9)
            .build();

        this.<Point2D>newColumnBuilder()
            .titleKey("table.airports.latitude")
            .objectObservableValueFactory(Airport::positionProperty)
            .toStringMapper(e -> GeomUtil.formatLat(e.getX()))
            .sortable(Comparator.comparingDouble(Point2D::getX))
            .mono(true)
            .widthFactor(0.9)
            .build();

        this.<String>newColumnBuilder()
            .titleKey("common.fir")
            .stringObservableValueFactory(AirportsTableView::firIcao)
            .sortable()
            .mono(true)
            .widthFactor(0.5)
            .build();

        this.<Country>newColumnBuilder()
            .titleKey("table.airports.country")
            .objectObservableValueFactory(Airport::countryProperty)
            .toStringMapper(Country::getName)
            .sortable()
            .widthFactor(1.25)
            .build();

        this.<Number>newColumnBuilder()
            .titleKey("table.airports.departures")
            .objectObservableValueFactory(e -> e.getDeparting().sizeProperty())
            .toStringMapper(AbstractTableView::emptyIfZero)
            .sortable()
            .mono(true)
            .widthFactor(0.85)
            .build();

        this.<Number>newColumnBuilder()
            .titleKey("table.airports.arrivals")
            .objectObservableValueFactory(e -> e.getArriving().sizeProperty())
            .toStringMapper(AbstractTableView::emptyIfZero)
            .sortable()
            .mono(true)
            .widthFactor(0.85)
            .build();

        this.<Number>newColumnBuilder()
            .titleKey("table.airports.total")
            .objectObservableValueFactory(Airport::trafficCountProperty)
            .toStringMapper(AbstractTableView::emptyIfZero)
            .sortable()
            .mono(true)
            .widthFactor(0.85)
            .build();

        this.<Number>newColumnBuilder()
            .titleKey("common.controller")
            .objectObservableValueFactory(e -> e.getControllers().sizeProperty())
            .toStringMapper(AbstractTableView::emptyIfZero)
            .sortable()
            .mono(true)
            .widthFactor(0.85)
            .build();
    }

    private static ReadOnlyStringProperty firIcao(final Airport e) {
        if (e.getFlightInformationRegionBoundary() == null) {
            return EMPTY;
        }

        return e.getFlightInformationRegionBoundary().icaoProperty();
    }

    private static ObservableStringValue airportName(final Airport airport, final ObservableStringValue query) {
        return airport
                .getNames()
                .stream()
                .filter(e -> StringUtils.containsIgnoreCase(e.get(), query.get()))
                .findFirst()
                .orElse(airport.getNames().get(0));
    }
}
