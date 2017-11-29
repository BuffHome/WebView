package webview;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import koen.klibs.fxmanager.core.FXSceneManagedApp;
import koen.klibs.fxmanager.nodes.Attachments;

import java.util.Arrays;
import java.util.Scanner;

public class Main extends FXSceneManagedApp {
    private static int port;
    private static double width;
    private static double height;

    private HBox settings;
    private Thread stdThread;
    private WebView webView;

    public static void main(String[] args) {
        System.out.println("Got args: " + Arrays.toString(args));
        Main.port = args.length >= 1 ? toInt(args[0], 8000) : 8000;
        Main.width = args.length >= 2 ? toDouble(args[1], 900) : 900;
        Main.height = args.length >= 3 ? toDouble(args[2], 600) : 600;
        launch();
    }

    private static int toInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
    private static double toDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }


    @Override
    public void start() {
        initLayoutFields("http://127.0.0.1:" + port);
        setDarkModeCss(true);
        stdThread = stdinListener();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.out.println("stopping");
        stdThread.interrupt();
        System.exit(0);
    }

    private void initLayoutFields(String url) {
        webView = new WebView();
        webView.getEngine().load(url);

        webView.getEngine().getLoadWorker().exceptionProperty().addListener((observable, oldValue, newValue) -> {
            String s = "[ERROR] : ";
            if (observable != null && observable.getValue() != null) s += observable.getValue().getMessage();
            if (newValue != null) {
                s += "<br/>" + newValue.getMessage();
                System.out.println(newValue.getMessage());
            }
            webView.getEngine().loadContent(s);
        });

        BorderPane bp = new BorderPane(webView);
        bp.setPrefWidth(width);
        bp.setPrefHeight(height);
        bp.setPadding(new Insets(15,15,15,15));
        Pane mainPane = new Pane(bp);
        mainPane.setPrefWidth(width);
        mainPane.setPrefHeight(height);

        initSettingsPane(mainPane, 300, 80, "a", "b", "c");
        initWebEnginePane(mainPane, url);
        setRoot(mainPane);
    }

    private void initWebEnginePane(Pane mainPane, String initString) {
        VBox p = new VBox();

        TextField tf = new TextField(initString);
        tf.setPromptText("Enter a url and hit enter");
        tf.setOnAction((e) -> {
            String s = tf.getText();
            boolean isIp = s.matches(".+[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}.+");
//            System.out.println("isIp: " + isIp);
            if (!s.startsWith("http")) {
                if (!s.startsWith("www") && !isIp) {
                    s = "www." + s;
                }
                s = "http://" + s;
            }
            tf.setText(s);
            webView.getEngine().load(s);
        });
        tf.setFont(Font.font(18));
        tf.setMinWidth(500);

        p.getChildren().add(tf);
        mainPane.getChildren().add(p);
        p.layoutXProperty().bind(mainPane.widthProperty().subtract(p.widthProperty()));
        p.setLayoutY(0);

        Rectangle detection = new Rectangle(600, 150);
//        detection.setStroke(Paint.valueOf("green"));

        Attachments.invisibleOnMouseLeave(p, detection, false);
    }
    private void initSettingsPane(Pane mainPane, double w, double h, String... buttons) {
        settings = new HBox();
        settings.setBackground(new Background(
                new BackgroundFill(
                        Paint.valueOf("#2d2d2d"),
                        new CornerRadii(4),
                        null)));

        for (String button : buttons) {
            addButton(button);
        }

        Rectangle detection = new Rectangle(w + 100, h + 100);

        mainPane.getChildren().add(settings);
        settings.layoutXProperty().bind(mainPane.widthProperty().subtract(settings.widthProperty()));
        settings.layoutYProperty().bind(mainPane.heightProperty().subtract(settings.heightProperty()));
        Attachments.invisibleOnMouseLeave(settings, detection, false);
    }



    private void clearButtons() {
        Platform.runLater(() -> settings.getChildren().clear());
    }
    private void addButton(String s) {
        Button n = new Button(s);
        n.setFont(Font.font(18));
        n.setOnAction(event -> System.out.println(s));
        Platform.runLater(() -> settings.getChildren().add(n));
    }





    private Thread stdinListener() {
        Thread t = new Thread(() -> {
            try {
                Scanner sc = new Scanner(System.in);
                String line;
                while ((line = sc.nextLine()) != null && !Thread.currentThread().isInterrupted()) {
                    if (line.isEmpty()) continue;
                    if (line.startsWith("buttons")) {
                        String[] lineParts = line.split(" ");
                        if (lineParts.length < 2) continue;
                        switch (lineParts[1]) {
                            case "clear":
                                clearButtons();
                                break;
                            default:
                                System.out.println("new button added");
                                addButton(lineParts[1]);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("stdinListener stopped by exception: " + e.getMessage());
            }
        });
        t.start();
        return t;
    }
}
