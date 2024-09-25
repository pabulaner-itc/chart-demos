package com.intechcore.chartdemofairacc;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.HistogramRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.AbstractErrorDataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.TransposedDataSet;
import io.fair_acc.dataset.testdata.spi.GaussFunction;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.MathDataSet;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class ChartProvider {

    public static Node getSimpleChart() {
        final int N_SAMPLES = 512;
        final StackPane root = new StackPane();

        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        root.getChildren().add(chart);

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");
        // lineChartPlot.getDatasets().add(dataSet1); // for single data set
        chart.getDatasets().addAll(dataSet1, dataSet2); // two data sets

        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues1 = new double[N_SAMPLES];
        final double[] yValues2 = new double[N_SAMPLES];
        for (int n = 0; n < N_SAMPLES; n++) {
            xValues[n] = n;
            yValues1[n] = Math.cos(Math.toRadians(10.0 * n / 8)) * n / 8;
            yValues2[n] = Math.sin(Math.toRadians(10.0 * n / 8)) * Math.cos(n / 8.0) * 10;
        }
        dataSet1.set(xValues, yValues1);
        dataSet2.set(xValues, yValues2);

        return root;
    }

    final static int N_SAMPLES = 32;

    public static GridPane getCharts() {
        GridPane root = new GridPane();
        // bar plots
        root.addRow(0, getChart(true, false, false, LineStyle.NONE, true), getChart(false, false, false, LineStyle.NONE, true), getChart(false, true, false, LineStyle.NONE, true));
        root.addRow(1, getChart(true, false, true, LineStyle.NONE, true), getChart(false, false, true, LineStyle.NONE, true), getChart(false, true, true, LineStyle.NONE, true));

        // histogram plots
        root.addRow(2, getChart(true, false, false, LineStyle.BEZIER_CURVE, false), getChart(true, true, false, LineStyle.HISTOGRAM, false), getChart(false, true, false, LineStyle.HISTOGRAM_FILLED, false));
        root.addRow(3, getChart(true, false, true, LineStyle.BEZIER_CURVE, false), getChart(true, true, true, LineStyle.HISTOGRAM, false), getChart(false, true, true, LineStyle.HISTOGRAM_FILLED, false));

        return root;
    }

    public static Chart getChart(final boolean shifted, final boolean stacked, final boolean vertical, final LineStyle lineStyle, final boolean drawBars) {
        final HistogramRenderer renderer = new HistogramRenderer();
        renderer.setDrawBars(drawBars);
        renderer.setPolyLineStyle(lineStyle);
        if (drawBars) {
            renderer.setPolyLineStyle(LineStyle.NONE);
        } else {
            renderer.setDrawBars(false);
        }
        renderer.setShiftBar(shifted);

        renderer.getDatasets().setAll(getTestDataSets(stacked, vertical));
        if (vertical) {
            renderer.setAutoSorting(false); // N.B. for the time being, auto-sorting needs to be disabled for vertical datasets....
        }

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("abscissa", null);
        final DefaultNumericAxis yAxis = new DefaultNumericAxis("ordinate" + (stacked ? " (stacked)" : ""), null);
        yAxis.setAutoRangeRounding(false);
        yAxis.setAutoRangePadding(0.3);

        final XYChart chart;
        chart = new XYChart(vertical ? yAxis : xAxis, vertical ? xAxis : yAxis);
        chart.getRenderers().set(0, renderer);
        chart.getLegend().getNode().visibleProperty().set(true);
        chart.setLegendVisible(false);
        GridPane.setHgrow(chart, Priority.ALWAYS);
        GridPane.setVgrow(chart, Priority.ALWAYS);

        return chart;
    }

    private static List<DataSet> getTestDataSets(final boolean stacked, final boolean transposed) {
        final List<DataSet> dataSets = new ArrayList<>();
        for (int centre : new int[] { 2 * N_SAMPLES / 5, N_SAMPLES / 3, 2 * N_SAMPLES / 3 }) {
            final AbstractErrorDataSet<?> gauss = new GaussFunction("h" + centre, N_SAMPLES, centre, 0.1 * N_SAMPLES);
            gauss.addDataLabel(centre, "special point for " + gauss.getName());
            gauss.addDataStyle(centre, "strokeColor=cyan; fillColor=cyan; markerColor=cyan;");
            dataSets.add(gauss);
        }
        if (stacked) {
            final SummingDataSet sum123 = new SummingDataSet("Sum", new SummingDataSet("Sum", dataSets.toArray(new DataSet[0])));
            final SummingDataSet sum12 = new SummingDataSet("Sum", new SummingDataSet("Sum", dataSets.subList(0, 1).toArray(new DataSet[0])));
            dataSets.set(0, sum123);
            dataSets.set(1, sum12);
        }

        if (transposed) {
            dataSets.set(0, TransposedDataSet.transpose(dataSets.get(0)));
            dataSets.set(1, TransposedDataSet.transpose(dataSets.get(1)));
            dataSets.set(2, TransposedDataSet.transpose(dataSets.get(2)));
        }

        return dataSets;
    }

    public static class SummingDataSet extends MathDataSet { // NOSONAR NOPMD -- too many parents is out of our control (Java intrinsic)
        public SummingDataSet(final String name, final DataSet... functions) {
            super(name, (dataSets, returnFunction) -> {
                if (dataSets.isEmpty()) {
                    return;
                }
                final ArrayDeque<DataSet> lockQueue = new ArrayDeque<>(dataSets.size());
                try {
                    // TODO: this deadlocks and errors on invalid index access (-1)
                    dataSets.forEach(ds -> {
                        lockQueue.push(ds);
                        ds.lock().readLock();
                    });
                    returnFunction.clearData();
                    final DataSet firstDataSet = dataSets.get(0);
                    returnFunction.add(firstDataSet.get(DIM_X, 0), 0);
                    returnFunction.add(firstDataSet.get(DIM_X, firstDataSet.getDataCount() - 1), 0);
                    dataSets.forEach(ds -> returnFunction.set(DataSetMath.addFunction(returnFunction, ds), false));
                } finally {
                    // unlock in reverse order
                    while (!lockQueue.isEmpty()) {
                        lockQueue.pop().lock().readUnLock();
                    }
                }
            }, functions);
        }
    }
}
