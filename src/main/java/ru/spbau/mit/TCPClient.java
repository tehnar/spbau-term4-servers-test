package ru.spbau.mit;

import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by Сева on 23.05.2016.
 */
public class TCPClient {
    private static final Logger LOG = LogManager.getLogger(TCPClient.class);
    private final TimeMeasure clientMeasure;

    TCPClient(TimeMeasure clientMeasure) {
        this.clientMeasure = clientMeasure;
    }

    private static List<Integer> receiveArray(DataInputStream inputStream) throws IOException {
        int size = inputStream.readInt();
        byte[] data = new byte[size];
        int processed = 0;
        while (processed < size) {
            processed += inputStream.read(data, processed, size - processed);
        }
        return Protocol.Array.parseFrom(data).getValueList();
    }

    private static void sendArray(Protocol.Array array,
                                  DataOutputStream outputStream) throws IOException {
        byte[] data = array.toByteArray();
        outputStream.writeInt(data.length);
        outputStream.write(data);
        outputStream.flush();
    }

    public void process(String serverIp, int serverPort, int n, int x, int delay) {
        TimeMeasure.Timer clientTimer = clientMeasure.startNewTimer();
        try (Socket socket = new Socket(serverIp, serverPort);
             DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
            for (int i = 0; i < x; i++) {
                doQuery(n, inputStream, outputStream);
                Thread.sleep(delay);
            }
        } catch (Exception e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
        clientTimer.stop();
    }

    public void processOneRequestPerConnection(String serverIp, int serverPort, int n, int x, int delay) {
        TimeMeasure.Timer clientTimer = clientMeasure.startNewTimer();
        for (int i = 0; i < x; i++) {
            try (Socket socket = new Socket(serverIp, serverPort);
                 DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                doQuery(n, inputStream, outputStream);
                Thread.sleep(delay);
            } catch (Exception e) {
                LOG.error(Throwables.getStackTraceAsString(e));
            }
        }
        clientTimer.stop();
    }

    private void doQuery(int n, DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        Random rnd = new Random();
        List<Integer> array = rnd.ints().limit(n).boxed().collect(Collectors.toList());
        sendArray(Protocol.Array.newBuilder().addAllValue(array).build(), outputStream);
        List<Integer> sorted = receiveArray(inputStream);

        if (!Ordering.natural().isOrdered(sorted)) {
            LOG.error("Received unsorted array!");
        }
        Collections.sort(array);
        if (!array.equals(sorted)) {
            LOG.error("Received incorrect array!");
        }
    }
}
