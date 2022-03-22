package test;


import test.Commands.DefaultIO;
import test.Server.ClientHandler;

import java.io.*;

public class AnomalyDetectionHandler implements ClientHandler {

    @Override
    public void handleClient(InputStream inFromClient, OutputStream outToClient) {

        SocketIO fio = new SocketIO(inFromClient, outToClient);
        CLI cli = new CLI(fio);
        cli.start();
        fio.close();
    }

    public static class SocketIO implements DefaultIO {

        BufferedReader in;
        PrintWriter out;

        public SocketIO(InputStream inputFileName, OutputStream outputFileName) {
            out = new PrintWriter(new OutputStreamWriter(outputFileName));
            in = new BufferedReader(new InputStreamReader(inputFileName));
        }

        @Override
        public String readText() {
            try {
                return in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void write(String text) {
            out.print(text);
            out.flush();
        }

        @Override
        public float readVal() {
            try {
                return Float.parseFloat(in.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public void write(float val) {
            out.print(val);
            out.flush();
        }

        public void close() {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.close();
        }
    }


}
