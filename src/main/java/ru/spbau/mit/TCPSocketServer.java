package ru.spbau.mit;

import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

/**
 * Created by Сева on 29.05.2016.
 */
public abstract class TCPSocketServer extends Server {
    private static final Logger LOG = LogManager.getLogger(TCPSocketServer.class);
    protected ServerSocket serverSocket;

    @Override
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        this.port = serverSocket.getLocalPort();
    }

    protected void processClient(Socket socket) {
        try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                List<Integer> list = readArray(inputStream);
                TimeMeasure.Timer responseTimer = responseTimeMeasure.startNewTimer();
                TimeMeasure.Timer handleTimer = handleTimeMeasure.startNewTimer();
                Collections.sort(list);
                handleTimer.stop();
                outputStream.write(prepareArray(list));
                outputStream.flush();
                responseTimer.stop();
            }
        } catch (EOFException ignored) {
        } catch (Exception e) {
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }

}
