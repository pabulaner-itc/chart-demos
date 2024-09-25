package com.intechcore.chartdemjavafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        VBox vbox = new VBox(
                ChartProvider.getChart(ChartProvider.ChartType.BAR),
                ChartProvider.getChart(ChartProvider.ChartType.LINE),
                ChartProvider.getChart(ChartProvider.ChartType.PIE));
        Scene scene = new Scene(vbox, 900, 500);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}