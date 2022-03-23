package test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TimeSeries {

    List<List<String>> records = new ArrayList<>();

    // given csv file, read the file into a List of String Lists
    public TimeSeries(String csvFileName) {

        try (
                BufferedReader br = new BufferedReader(new FileReader(csvFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // given an attribute name, find the matching column and return all his values as float[]
    public float[] GetValuesOfAttribute(String name) {
        float[] values = new float[records.size() - 1];
        for (int i = 0; i < records.get(0).size(); i++) {
            if (Objects.equals(name, records.get(0).get(i))) {
                for (int j = 1; j < records.size(); j++) {
                    values[j - 1] = Float.parseFloat(records.get(j).get(i));
                }
                break;
            }
        }
        return values;
    }
}