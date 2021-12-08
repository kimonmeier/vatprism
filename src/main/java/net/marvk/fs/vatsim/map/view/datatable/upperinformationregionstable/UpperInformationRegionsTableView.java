package net.marvk.fs.vatsim.map.view.datatable.upperinformationregionstable;

import com.google.inject.Inject;
import javafx.collections.ObservableList;
import net.marvk.fs.vatsim.map.data.FlightInformationRegionBoundary;
import net.marvk.fs.vatsim.map.data.UpperInformationRegion;
import net.marvk.fs.vatsim.map.view.TextFlowHighlighter;
import net.marvk.fs.vatsim.map.view.datatable.AbstractTableView;

import java.util.stream.Collectors;

public class UpperInformationRegionsTableView extends AbstractTableView<UpperInformationRegionsTableViewModel, UpperInformationRegion> {
    @Inject
    public UpperInformationRegionsTableView(final TextFlowHighlighter textFlowHighlighter) {
        super(textFlowHighlighter);
    }

    @Override
    protected void initializeColumns() {
        newColumnBuilder()
                .titleKey("common.icao")
                .stringObservableValueFactory(UpperInformationRegion::icaoProperty)
                .sortable()
                .mono(true)
                .widthFactor(0.6)
                .build();

        newColumnBuilder()
                .titleKey("common.name")
                .stringObservableValueFactory(UpperInformationRegion::nameProperty)
                .sortable()
                .mono(true)
                .widthFactor(2.5)
                .build();

        this.<ObservableList<FlightInformationRegionBoundary>>newColumnBuilder()
                .titleKey("common.uirs")
                .objectObservableValueFactory(UpperInformationRegion::getFlightInformationRegionBoundaries)
                .toStringMapper(UpperInformationRegionsTableView::firbIcaos)
                .mono(true)
                .widthFactor(6)
                .build();

        this.<Number>newColumnBuilder()
                .titleKey("common.number_of_controllers")
                .objectObservableValueFactory(e -> e.getControllers().sizeProperty())
                .toStringMapper(AbstractTableView::emptyIfZero)
                .sortable()
                .mono(true)
                .widthFactor(0.8)
                .build();
    }

    private static String firbIcaos(final ObservableList<FlightInformationRegionBoundary> firbs) {
        return firbs.stream().map(FlightInformationRegionBoundary::getIcao).collect(Collectors.joining(", "));
    }
}
