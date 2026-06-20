package com.example.receiver.ui;

import com.example.receiver.controller.ReceiverController;
import com.example.receiver.utils.OpenCvBootstrap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;

public final class ReceiverApp extends Application {
    private ReceiverController controller;
    private final ImageView imageView = new ImageView();
    private final Label status = new Label("Stopped");
    private final Label fps = new Label("FPS 0.0");
    private final Label latency = new Label("Latency 0 ms");
    private final Circle indicator = new Circle(6, Color.web("#B3261E"));
    private final TextArea logs = new TextArea();

    @Override
    public void start(Stage stage) {
        OpenCvBootstrap.load(this::log);
        Spinner<Integer> port = new Spinner<>(1024, 65535, 9000);
        Button start = new Button("Start Receiver");
        Button stop = new Button("Stop Receiver");
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.fitWidthProperty().bind(stage.widthProperty());
        imageView.fitHeightProperty().bind(stage.heightProperty().subtract(170));
        logs.setEditable(false);
        logs.setPrefRowCount(6);
        controller = new ReceiverController(
            imageView::setImage,
            text -> {
                status.setText(text);
                indicator.setFill(text.startsWith("Listening") ? Color.web("#1B8A3D") : Color.web("#B3261E"));
            },
            value -> fps.setText(String.format("FPS %.1f", value)),
            value -> latency.setText("Latency " + value + " ms"),
            this::log
        );
        start.setOnAction(event -> {
            try {
                controller.start(port.getValue());
            } catch (IOException e) {
                log("Unable to start receiver: " + e.getMessage());
            }
        });
        stop.setOnAction(event -> controller.stop());
        HBox toolbar = new HBox(12, indicator, status, fps, latency, port, start, stop);
        toolbar.setPadding(new Insets(12));
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(imageView);
        root.setBottom(logs);
        BorderPane.setMargin(imageView, new Insets(8));
        HBox.setHgrow(status, Priority.ALWAYS);
        stage.setTitle("Desktop Video Receiver");
        stage.setScene(new Scene(root, 1120, 760));
        stage.show();
        try {
            controller.start(port.getValue());
        } catch (IOException e) {
            log("Unable to auto-start receiver: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (controller != null) controller.close();
    }

    private void log(String message) {
        Platform.runLater(() -> logs.appendText("[" + LocalTime.now().withNano(0) + "] " + message + System.lineSeparator()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
