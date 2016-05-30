package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Сева on 23.05.2016.
 */
public class ServerMain {
    public static final String SERVER_CLOSED = "server closed";
    public static final int PORT = 44444;
    public static final String START_SERVER = "start";
    public static final String FINISH_SERVER = "finish";
    public static final String INCORRECT_COMMAND = "incorrect command";
    public static final String SERVER_STARTED = "server started";

    private static final Logger LOG = LogManager.getLogger(ServerMain.class);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (!serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                Protocol.ServerCommand command = getCommand(inputStream);

                final Server server;
                LOG.info("Command " + command.getType() + " received");
                if (command.getType().equals(START_SERVER)) {
                    server = ServerFactory.createServer(command.getServerType());
                } else {
                    incorrectCommand(outputStream);
                    continue;
                }
                server.start(0);

                sendResponse(Protocol.ServerResponse.newBuilder()
                                .setResult(SERVER_STARTED)
                                .setPort(server.getPort())
                                .build(),
                        outputStream);

                command = getCommand(inputStream);
                LOG.info("Command " + command.getType() + " received");
                if (command.getType().equals(FINISH_SERVER)) {
                    server.close();
                    sendResponse(Protocol.ServerResponse.newBuilder()
                                    .setResult(SERVER_CLOSED)
                                    .setMeanResponseTime(server.getMeanResponseTime())
                                    .setMeanHandleTime(server.getMeanHandleTime())
                                    .build(),
                            outputStream);
                } else {
                    incorrectCommand(outputStream);
                    server.close();
                }
                LOG.info("Server closed");
            }
        }
    }

    private static void sendResponse(Protocol.ServerResponse response,
                                     DataOutputStream outputStream) throws IOException {
        byte[] data = response.toByteArray();
        outputStream.writeInt(data.length);
        outputStream.write(data);
        outputStream.flush();
    }

    private static Protocol.ServerCommand getCommand(DataInputStream inputStream) throws IOException {
        int size = inputStream.readInt();
        byte[] data = new byte[size];
        int processed = 0;
        while (processed < size) {
            processed += inputStream.read(data, processed, size - processed);
        }
        return Protocol.ServerCommand.parseFrom(data);
    }

    private static void incorrectCommand(DataOutputStream outputStream) throws IOException {
        sendResponse(Protocol.ServerResponse.newBuilder().setResult(INCORRECT_COMMAND).build(), outputStream);
    }
}
