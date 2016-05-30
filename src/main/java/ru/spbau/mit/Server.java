package ru.spbau.mit;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Сева on 23.05.2016.
 */
public abstract class Server implements Closeable {
    protected int port;
    TimeMeasure handleTimeMeasure = new TimeMeasure();
    TimeMeasure responseTimeMeasure = new TimeMeasure();

    public int getPort() {
        return port;
    }

    public abstract void start(int port) throws IOException;

    public double getMeanHandleTime() {
        return handleTimeMeasure.mean();
    }

    public double getMeanResponseTime() {
        return responseTimeMeasure.mean();
    }

    protected List<Integer> readArray(DataInputStream inputStream) throws IOException {
        int size = inputStream.readInt();
        byte[] arrayData = new byte[size];
        int processedBytes = 0;
        while (processedBytes < size) {
            processedBytes += inputStream.read(arrayData, processedBytes, size - processedBytes);
        }
        return new ArrayList<>(Protocol.Array.parseFrom(arrayData).getValueList());
    }

    protected byte[] prepareArray(List<Integer> array) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream);

        byte[] sortedArray = Protocol.Array.newBuilder().addAllValue(array).build().toByteArray();
        outputStream.writeInt(sortedArray.length);
        outputStream.write(sortedArray);
        return byteArrayOutputStream.toByteArray();
    }
}
