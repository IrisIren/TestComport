package sample;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    private static final int MAX_FRAME_POINTS = 4000;
    private static ConcurrentLinkedQueue<Double> EcgSample = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Double> RRPeaks = new ConcurrentLinkedQueue<>();
    private XYChart.Series<Number, Number> ecgSeries;
    private XYChart.Series<Number, Number> rPeakSeries;
    private int xSeriesData;

    private NumberAxis xAxis;

    private void init(Stage primaryStage) {
        xAxis = new NumberAxis(0, MAX_FRAME_POINTS, MAX_FRAME_POINTS / 10);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(true);

        final LineChart<Number, Number> sc = new LineChart<>(xAxis, yAxis);

        sc.setAnimated(false);
        sc.setId("realtime ECG graph");
        sc.setTitle("ECG-signal");
        sc.getStylesheets().add(Main.class.getResource("chart.css").toExternalForm());
        ecgSeries = new LineChart.Series<>();
        ecgSeries.setName("realtime ECG graph");
        sc.getData().add(ecgSeries);

        rPeakSeries = new LineChart.Series<>();
        rPeakSeries.setName("RRPeaks");
        sc.getData().add(rPeakSeries);
        primaryStage.setScene(new Scene(sc));
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        init(primaryStage);
        primaryStage.show();

        prepareTimeline();
    }

    public static void main(String[] args) {

        AtomicBoolean fileIsFinished = new AtomicBoolean(false);
        BlockingQueue<Double> queue = new LinkedBlockingQueue<>();

        BluetoothModelReceiver bluetoothModelReceiver = new BluetoothModelReceiver(queue, "C:\\Users\\Ирина\\IdeaProjects\\ECG-finalProgramm\\src\\sample\\ECG.txt", fileIsFinished);
        bluetoothModelReceiver.start();

        ECGSignalHandler ecgSignalHandler = new ECGSignalHandler(queue, EcgSample, RRPeaks, fileIsFinished);
        ecgSignalHandler.start();
        launch(args);
    }

    private void prepareTimeline() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                addDataToSeries();
            }
        }.start();
    }

    private void addDataToSeries() {
        for (int i = 0; i < MAX_FRAME_POINTS / 50; i++) {
            if (EcgSample.isEmpty()) break;
            ecgSeries.getData().add(new LineChart.Data<>(xSeriesData++, EcgSample.remove()));
        }

        Double[] dArray = RRPeaks.toArray(new Double[RRPeaks.size()]);
        for (int i = 1; i < dArray.length; i++) {
            rPeakSeries.getData().add(new LineChart.Data<>(dArray[i], 1500));

        }

        if (ecgSeries.getData().size() > MAX_FRAME_POINTS) {
            ecgSeries.getData().remove(0, ecgSeries.getData().size() - MAX_FRAME_POINTS);
        }
        xAxis.setLowerBound(Math.max(xSeriesData - MAX_FRAME_POINTS, 0));
        xAxis.setUpperBound(Math.max(xSeriesData - 1, MAX_FRAME_POINTS));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}