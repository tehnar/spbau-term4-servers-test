package ru.spbau.mit;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Сева on 23.05.2016.
 */
public class ServerTestGUI {
    private static final Logger LOG = LogManager.getLogger(ServerTestGUI.class);

    private static final String[] CHART_TITLES = {"Array sorting time", "Server process time", "Client process time"};

    private static JButton testButton = new JButton("Test");
    private static JFXPanel chartPanelFX = new JFXPanel();
    private static List<List<XYChart.Series<Number, Number>>> data = Arrays.asList(
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    private static XYChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());

    private static TimeMeasure clientMeasure = new TimeMeasure();
    private static int curChart = 0;
    private static JProgressBar progressBar = new JProgressBar(0, 100);


    public static void main(String[] args) {
        JFrame frame = new JFrame("Servers test");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

        JPanel serverChoosePanel = new JPanel();
        serverChoosePanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;

        constraints.insets = new Insets(10, 0, 10, 10);
        constraints.gridwidth = 1;

        constraints.gridy = 0;
        serverChoosePanel.add(new JLabel("N:", SwingConstants.RIGHT), constraints);

        constraints.gridy = 1;
        serverChoosePanel.add(new JLabel("M:", SwingConstants.RIGHT), constraints);

        constraints.gridy = 2;
        serverChoosePanel.add(new JLabel("X:", SwingConstants.RIGHT), constraints);

        constraints.gridy = 3;
        serverChoosePanel.add(new JLabel("Delay (ms):", SwingConstants.RIGHT), constraints);

        constraints.gridx = 1;


        JTextField nVal = new JTextField("0", 10);
        constraints.gridy = 0;
        serverChoosePanel.add(nVal, constraints);

        JTextField mVal = new JTextField("0", 10);
        constraints.gridy = 1;
        serverChoosePanel.add(mVal, constraints);

        JTextField xVal = new JTextField("0", 10);
        constraints.gridy = 2;
        serverChoosePanel.add(xVal, constraints);

        JTextField delayVal = new JTextField("0", 10);
        constraints.gridy = 3;
        serverChoosePanel.add(delayVal, constraints);

        constraints.insets = new Insets(10, 30, 10, 10);
        constraints.gridx = 2;

        constraints.gridy = 0;
        serverChoosePanel.add(new JLabel("Change param:", SwingConstants.RIGHT), constraints);

        constraints.gridy = 1;
        serverChoosePanel.add(new JLabel("range:", SwingConstants.RIGHT), constraints);

        constraints.gridy = 2;
        serverChoosePanel.add(new JLabel("step:", SwingConstants.RIGHT), constraints);


        constraints.gridx = 3;
        constraints.insets = new Insets(10, 0, 10, 10);

        JComboBox<String> paramsCombo = new JComboBox<>(new String[]{"N", "M", "X", "Delay"});
        constraints.gridy = 0;
        serverChoosePanel.add(paramsCombo, constraints);

        constraints.gridy = 1;
        JPanel rangePanel = new JPanel();
        JTextField minRangeVal = new JTextField(5);
        rangePanel.add(minRangeVal);
        rangePanel.add(new JLabel("-"));
        JTextField maxRangeVal = new JTextField(5);
        rangePanel.add(maxRangeVal);
        serverChoosePanel.add(rangePanel, constraints);

        JTextField stepVal = new JTextField(10);
        constraints.gridy = 2;
        serverChoosePanel.add(stepVal, constraints);

        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.gridy = 4;
        constraints.gridx = 0;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.NONE;

        JPanel testPanel = new JPanel();
        testPanel.add(new JLabel("Server IP:"));
        JTextField ipVal = new JTextField(10);
        testPanel.add(ipVal);
        serverChoosePanel.add(testPanel, constraints);

        constraints.gridy = 5;
        serverChoosePanel.add(testButton, constraints);

        constraints.insets = new Insets(10, 0, 0, 0);
        constraints.gridy = 6;
        serverChoosePanel.add(progressBar, constraints);

        topPanel.add(serverChoosePanel);
        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
        chartPanelFX.setScene(new Scene(chart));
        chartPanelFX.setPreferredSize(new Dimension(1000, 600));
        chartPanel.add(chartPanelFX);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JButton prevButton = new JButton("<");
        JButton saveButton = new JButton("Save");
        JButton nextButton = new JButton(">");

        prevButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttons.add(prevButton);
        buttons.add(saveButton);
        buttons.add(nextButton);

        chartPanel.add(buttons);
        topPanel.add(chartPanel);

        frame.add(topPanel);

//        mainPanel.add(logArea, constraints);
        //frame.add(mainPanel);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();

        nVal.setEnabled(false); // selected at the beginning
        Map<String, JTextField> varFields = ImmutableMap.of(
                "N", nVal,
                "M", mVal,
                "X", xVal,
                "Delay", delayVal
        );
        paramsCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                varFields.get(e.getItem()).setEnabled(false);
            }
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                varFields.get(e.getItem()).setEnabled(true);
            }
        });

        testButton.addActionListener(e -> {
            testButton.setEnabled(false);
            int n = Integer.parseInt(nVal.getText());
            int m = Integer.parseInt(mVal.getText());
            int x = Integer.parseInt(xVal.getText());
            int delay = Integer.parseInt(delayVal.getText());
            int from = Integer.parseInt(minRangeVal.getText());
            int to = Integer.parseInt(maxRangeVal.getText());
            int step = Integer.parseInt(stepVal.getText());
            String change = (String) paramsCombo.getSelectedItem();
            new Thread(() -> runTest(n, m, x, delay, change, from, to, step, ipVal.getText())).start();
        });

        saveButton.addActionListener(e -> Platform.runLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
            fileChooser.setCurrentDirectory(Paths.get(".").toFile());
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                WritableImage image = chart.snapshot(new SnapshotParameters(), null);
                File file = fileChooser.getSelectedFile();
                if (!file.getAbsolutePath().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException e1) {
                    LOG.error(Throwables.getStackTraceAsString(e1));
                }
            }
        }));

        chart.setAnimated(false);
        nextButton.addActionListener(e -> {
            curChart = (curChart + 1) % data.size();
            Platform.runLater(ServerTestGUI::updateChart);
        });

        prevButton.addActionListener(e -> {
            curChart = (curChart - 1 + data.size()) % data.size();
            Platform.runLater(ServerTestGUI::updateChart);
        });
    }

    private static void runTest(int n, int m, int x, int delay, String changeName, int from, int to,
                                int step, String serverIp) {
        for (List<XYChart.Series<Number, Number>> aData : data) {
            aData.clear();
        }
        int maxProgress = ServerFactory.SERVER_TYPES.length * (to - from + step) / step;
        int curProgress = 0;

        for (String serverType : ServerFactory.SERVER_TYPES) {
            List<XYChart.Data<Integer, Double>> datasetHandle = new ArrayList<>();
            List<XYChart.Data<Integer, Double>> datasetResponse = new ArrayList<>();
            List<XYChart.Data<Integer, Double>> datasetClient = new ArrayList<>();
            for (int val = from; val <= to; val += step) {
                try (Socket socket = new Socket(serverIp, ServerMain.PORT);
                     DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                    sendServerCommand(Protocol.ServerCommand
                            .newBuilder()
                            .setType(ServerMain.START_SERVER)
                            .setServerType(serverType)
                            .build(), outputStream);
                    Protocol.ServerResponse startResponse = getResponse(inputStream);
                    LOG.info(startResponse.getResult());
                    if (!startResponse.getResult().equals(ServerMain.SERVER_STARTED)) {
                        return;
                    }
                    int port = startResponse.getPort();
                    ExecutorService threadPool = Executors.newCachedThreadPool();
                    switch (changeName) {
                        case "M":
                            m = val;
                            break;
                        case "N":
                            n = val;
                            break;
                        case "X":
                            x = val;
                            break;
                        case "Delay":
                            delay = val;
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal change name: " + changeName);
                    }

                    for (int i = 0; i < m; i++) {
                        final int n1 = n, x1 = x, delay1 = delay;
                        if (serverType.equals(ServerFactory.UDP_POOL_BLOCKING)
                                || serverType.equals(ServerFactory.UDP_FIXED_POOL_BLOCKING)) {
                            threadPool.submit(() -> new UDPClient(clientMeasure)
                                    .process(serverIp, port, n1, x1, delay1));
                        } else {
                            if (serverType.equals(ServerFactory.BLOCKING_ONE_THREAD)) {
                                threadPool.submit(() -> new TCPClient(clientMeasure)
                                        .processOneRequestPerConnection(serverIp, port, n1, x1, delay1));
                            } else {
                                threadPool.submit(() -> new TCPClient(clientMeasure)
                                        .process(serverIp, port, n1, x1, delay1));
                            }
                        }
                    }
                    threadPool.shutdown();
                    threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    sendServerCommand(Protocol.ServerCommand.newBuilder().setType(
                                    ServerMain.FINISH_SERVER).build(), outputStream);
                    Protocol.ServerResponse finishResponse = getResponse(inputStream);
                    datasetHandle.add(new XYChart.Data<>(val, finishResponse.getMeanHandleTime()));
                    datasetResponse.add(new XYChart.Data<>(val, finishResponse.getMeanResponseTime()));
                    datasetClient.add(new XYChart.Data<>(val, clientMeasure.mean()));

                } catch (Exception e) {
                    LOG.error(Throwables.getStackTraceAsString(e));
                }
                curProgress++;
                final int progress = curProgress;
                SwingUtilities.invokeLater(() -> progressBar.setValue(progress * 100 / maxProgress));
            }

            data.get(0).add(new XYChart.Series(serverType, FXCollections.observableArrayList(datasetHandle)));
            data.get(1).add(new XYChart.Series(serverType, FXCollections.observableArrayList(datasetResponse)));
            data.get(2).add(new XYChart.Series(serverType, FXCollections.observableArrayList(datasetClient)));
        }
        Platform.runLater(() -> {
            chart.getXAxis().setLabel(changeName);
            chart.getYAxis().setLabel("ms");
            updateChart();
        });
        SwingUtilities.invokeLater(() -> testButton.setEnabled(true));
    }

    private static void updateChart() {
        chart.setTitle(CHART_TITLES[curChart]);
        chart.setData(FXCollections.observableArrayList(data.get(curChart)));
    }

    private static void sendServerCommand(Protocol.ServerCommand command,
                                          DataOutputStream outputStream) throws IOException {
        byte[] data = command.toByteArray();
        outputStream.writeInt(data.length);
        outputStream.write(data);
        outputStream.flush();
    }

    private static Protocol.ServerResponse getResponse(DataInputStream inputStream) throws IOException {
        int size = inputStream.readInt();
        byte[] data = new byte[size];
        int processed = 0;
        while (processed < size) {
            processed += inputStream.read(data, processed, size - processed);
        }
        return Protocol.ServerResponse.parseFrom(data);
    }
}
