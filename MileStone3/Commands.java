package test;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Commands {

    // Default IO interface
    public interface DefaultIO {
        String readText();
        String git = "";

        void write(String text);

        float readVal();

        void write(float val);

        // you may add default methods here
    }

    // the default IO to be used in all commands
    DefaultIO dio;

    public Commands(DefaultIO dio) {
        this.dio = dio;
    }

    // you may add other helper classes here
    private void sendFileToServer(PrintWriter file) {
        try {
            String line;
            sharedState.P = 0;
            // read the file
            while ((line = dio.readText()) != null && !line.equals("done")) {
                if (line.equals("")) continue;
                file.write(line + "\n");
                file.flush();
                sharedState.P++;
            }
            dio.write("Upload complete.\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAndSendFile(String fileName) {
        try {
            PrintWriter file = new PrintWriter(fileName);
            sendFileToServer(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class AnomalyRange {
        private final long start;
        private final long end;
        private final long range;

        AnomalyRange(long start, long end) {
            this.start = start;
            this.end = end;
            this.range = end - start + 1;
        }
    }


    // the shared state of all commands
    private static class SharedState {
        // implement here whatever you need
        private float correlation = (float) 0.9;
        public TimeSeries testCSV;
        public TimeSeries trainCSV;
        SimpleAnomalyDetector anomalyDetector;
        int P;
    }

    private final SharedState sharedState = new SharedState();


    // Command abstract class
    public abstract static class Command {
        protected String description;

        public Command(String description) {
            this.description = description;
        }

        public abstract void execute();
    }

    // implement here all other commands

    public class UploadCommand extends Command {

        public UploadCommand() {
            super("upload a time series csv file\n");
        }

        @Override
        public void execute() {

            // send the train file to the server line by line
            dio.write("Please upload your local train CSV file.\n");
            createAndSendFile("anomalyTrain.csv");

            // send the test file to the server line by line
            dio.write("Please upload your local test CSV file.\n");
            createAndSendFile("anomalyTest.csv");
        }
    }

    public class CorrelationSetting extends Command {

        public CorrelationSetting() {
            super("algorithm settings\n");
        }

        @Override
        public void execute() {
            dio.write("The current correlation threshold is " + sharedState.correlation + '\n');
            dio.write("Type a new threshold\n");
            float newCorrelation;

            while (true) {
                newCorrelation = dio.readVal();
                if (newCorrelation >= 0 && newCorrelation <= 1) {
                    break;
                } else {
                    dio.write("Please choose a value between 0 and 1\n");
                }
            }
            sharedState.correlation = newCorrelation;

        }
    }

    public class AnomalyDetection extends Command {

        public AnomalyDetection() {
            super("detect anomalies\n");
        }

        @Override
        public void execute() {
            sharedState.testCSV = new TimeSeries("anomalyTest.csv");
            sharedState.trainCSV = new TimeSeries("anomalyTrain.csv");
            sharedState.anomalyDetector = new SimpleAnomalyDetector(sharedState.correlation);
            sharedState.anomalyDetector.learnNormal(sharedState.trainCSV);
            sharedState.anomalyDetector.detect(sharedState.testCSV);
            dio.write("anomaly detection complete.\n");
        }
    }

    public class DisplayResults extends Command {

        public DisplayResults() {
            super("display results\n");
        }

        @Override
        public void execute() {
            for (int i = 0; i < sharedState.anomalyDetector.anomalyList.size(); i++) {
                dio.write(sharedState.anomalyDetector.anomalyList.get(i).timeStep + "\t");
                dio.write(sharedState.anomalyDetector.anomalyList.get(i).description + "\n");
            }
            dio.write("Done.\n");
        }
    }

    public class UploadAnomaliesAndAnalyzeResults extends Command {

        List<AnomalyRange> anomalyRangeList;
        List<AnomalyRange> anomalyRanges;

        private void start() {
            this.anomalyRangeList = new ArrayList<>();
            this.anomalyRanges = new ArrayList<>();
        }

        public UploadAnomaliesAndAnalyzeResults() {
            super("upload anomalies and analyze results\n");
        }

        private boolean isIntersecting(AnomalyRange first, AnomalyRange second) {
            return (second.start >= first.start && second.start <= first.end) ||
                    (second.end >= first.start && second.end <= first.end) ||
                    (first.start >= second.start && first.start <= second.end) ||
                    (first.end >= second.start && first.end <= second.end);
        }

        private void uploadAnomalies() {
            try {
                PrintWriter file = new PrintWriter("anomalies.csv");
                sendFileToServer(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void readAnomaliesIntoList(List<AnomalyRange> anomalyRangeList) {
            try {
                Scanner file = new Scanner(new FileReader("anomalies.csv"));
                for (int i = 0; i < sharedState.P; i++) {
                    String line = file.nextLine();
                    String[] chunks = line.split(",");
                    long first = Long.parseLong(chunks[0]);
                    long second = Long.parseLong(chunks[1]);
                    anomalyRangeList.add(new AnomalyRange(first, second));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        /**
         * This will combine the range of the objects with the same description
         */
        private void combineAnomalies(List<AnomalyRange> anomalyRanges) {
            int size = sharedState.anomalyDetector.anomalyList.size();
            List<AnomalyReport> anomalyList = sharedState.anomalyDetector.anomalyList;

            for (int i = 0; i < size - 1; i++) {
                String desc = anomalyList.get(i).description;
                long time = anomalyList.get(i).timeStep;
                for (int j = i; j < size; j++) {
                    if (!((j + 1) < size && Objects.equals(desc, anomalyList.get(j + 1).description))) {
                        anomalyRanges.add(new AnomalyRange(time, anomalyList.get(j).timeStep));
                        i = j;
                        break;
                    }
                }
            }
        }

        /**
         * This algorithm will find intersecting ranges and sort the AnomalyRange objects to the appropriate List
         */
        private void findIntersectingRanges(List<AnomalyRange> inRangeAnomalies, List<AnomalyRange> outOfRangeAnomalies) {
            for (AnomalyRange anomalyRange : this.anomalyRangeList) {
                for (AnomalyRange range : this.anomalyRanges) {
                    if (!inRangeAnomalies.contains(range)) {
                        if (isIntersecting(anomalyRange, range)) {
                            inRangeAnomalies.add(range);
                            outOfRangeAnomalies.remove(range);
                        } else {
                            if (!outOfRangeAnomalies.contains(range)) {
                                outOfRangeAnomalies.add(range);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void execute() {
            this.start();
            dio.write("Please upload your local anomalies file.\n");
            uploadAnomalies();
            readAnomaliesIntoList(this.anomalyRangeList);
            combineAnomalies(this.anomalyRanges);

            // N is representing the total rows that haven't been reported in the "anomalies.csv" file
            int N = sharedState.testCSV.records.size() - 1;
            for (AnomalyRange i : anomalyRangeList) {
                N -= i.range;
            }

            List<AnomalyRange> outOfRangeAnomalies = new ArrayList<>();
            List<AnomalyRange> inRangeAnomalies = new ArrayList<>();

            findIntersectingRanges(inRangeAnomalies, outOfRangeAnomalies);

            float FP = outOfRangeAnomalies.size();
            float TP = inRangeAnomalies.size();
            float result1 = TP / sharedState.P;
            float result2 = FP / N;
            DecimalFormat df = new DecimalFormat("#0.0");
            df.setMaximumFractionDigits(3);
            df.setRoundingMode(RoundingMode.DOWN);
            dio.write("True Positive Rate: " + df.format(result1) + "\nFalse Positive Rate: " + df.format(result2) + "\n");
        }
    }

    public class Exit extends Command {

        public Exit() {
            super("exit\n");
        }

        private void deleteAllFiles() {
            File deleteAnomalies = new File("anomalies.csv");
            if (!deleteAnomalies.delete()) dio.write("problem with deleting anomalies.csv file");
            File deleteAnomalyTest = new File("anomalyTest.csv");
            if (!deleteAnomalyTest.delete()) dio.write("problem with deleting anomalyTest.csv file");
            File deleteAnomalyTrain = new File("anomalyTrain.csv");
            if (!deleteAnomalyTrain.delete()) dio.write("problem with deleting anomalyTrain.csv file");
        }

        @Override
        public void execute() {
            deleteAllFiles();
            dio.write("bye");

        }
    }

}
