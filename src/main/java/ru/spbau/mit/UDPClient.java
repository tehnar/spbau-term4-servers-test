package ru.spbau.mit;

import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by Сева on 23.05.2016.
 */
public class UDPClient {
    private static final Logger LOG = LogManager.getLogger(UDPClient.class);
    private static final int TIMEOUT = 3000;
    private final TimeMeasure clientMeasure;

    UDPClient(TimeMeasure clientMeasure) {
        this.clientMeasure = clientMeasure;
    }

    void process(String serverIp, int serverPort, int n, int x, int delay) {
        TimeMeasure.Timer clientTimer = clientMeasure.startNewTimer();
        try {
            DatagramSocket socket = new DatagramSocket(0);
            socket.setSoTimeout(TIMEOUT);
            Random rnd = new Random();
            SocketAddress serverAddress = new InetSocketAddress(serverIp, serverPort);
            for (int i = 0; i < x; i++) {
                List<Integer> array = rnd.ints().limit(n).boxed().collect(Collectors.toList());


                ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteOutputStream);
                byte[] arrayData = Protocol.Array.newBuilder().addAllValue(array).build().toByteArray();
                dataOutputStream.writeInt(arrayData.length);
                dataOutputStream.write(arrayData);
                byte[] data = byteOutputStream.toByteArray();
                socket.send(new DatagramPacket(data, data.length, serverAddress));

                try {
                    socket.receive(new DatagramPacket(data, data.length));
                } catch (SocketTimeoutException e) {
                    LOG.warn(Throwables.getStackTraceAsString(e));
                    continue;
                }
                DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
                int size = inputStream.readInt();
                byte[] sortedArray = new byte[size];
                inputStream.read(sortedArray);
                List<Integer> sorted = Protocol.Array.parseFrom(sortedArray).getValueList();
                if (!Ordering.natural().isOrdered(sorted)) {
                    LOG.error("Received unsorted array!");
                }
                Collections.sort(array);
                if (!array.equals(sorted)) {
                    LOG.error("Received incorrect array!");
                }

                Thread.sleep(delay);
            }
        } catch (Exception e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
        clientTimer.stop();
    }
}
