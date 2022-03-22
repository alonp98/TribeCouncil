package test;

import java.util.ArrayList;
import java.util.List;

public class SimpleAnomalyDetector implements TimeSeriesAnomalyDetector {

	public float correlationThreshold = (float) 0.9;
	List<CorrelatedFeatures> correlatedFeatures = new ArrayList<>();
	List<AnomalyReport> anomalyList = new ArrayList<>();

	SimpleAnomalyDetector(float correlationThreshold) {
		this.correlationThreshold = correlationThreshold;
	}

	@Override
	public void learnNormal(TimeSeries ts) {

		findCorrelativeValues(ts, correlationThreshold);
	}

	public void findCorrelativeValues(TimeSeries ts, float correlationThreshold) {
		int N = ts.records.get(0).size();

		// get the names of measurement variables
		String[] namesOfAttributes = new String[N];
		for (int i = 0; i < N; i++) {
			namesOfAttributes[i] = ts.records.get(0).get(i);
		}

		// Algorithm to find correlative features
		for (int i = 0; i < N; i++) {
			float max = 0;
			int flag = -1;
			for (int j = i + 1; j < N; j++) {
				float pearson = Math.abs(StatLib.pearson(ts.GetValuesOfAttribute(ts.records.get(0).get(i)), ts.GetValuesOfAttribute(ts.records.get(0).get(j))));
				if (pearson > max) {
					max = pearson;
					flag = j;
				}
			}
			if (flag != -1 && max > correlationThreshold) addNewEntry(ts, i, flag, namesOfAttributes, max);
		}
	}

	@Override
	public List<AnomalyReport> detect(TimeSeries ts) {

		// run through the correlatedFeatures array from the learnNormal method
		for (CorrelatedFeatures correlatedFeature : correlatedFeatures) {

			Point[] points = new Point[ts.records.size()];
			long timeStep = 1;

			String feature1 = correlatedFeature.feature1;
			String feature2 = correlatedFeature.feature2;

			// build the new points array, and for each new point check if it's deviation is bigger than the threshold
			for (int j = 0; j < ts.records.size() - 1; j++) {

				// build the points array
				points[j] = new Point(ts.GetValuesOfAttribute(feature1)[j], ts.GetValuesOfAttribute(feature2)[j]);

				// check if the current point has bigger deviation than the pre-defined threshold
				if (StatLib.dev(points[j], correlatedFeature.lin_reg) > correlatedFeature.threshold) {
					anomalyList.add(new AnomalyReport(feature1 + "-" + feature2, timeStep));
				}
				timeStep++;
			}
		}
		return anomalyList;
	}



	public List<CorrelatedFeatures> getNormalModel(){
		return correlatedFeatures;
	}

	private void addNewEntry(TimeSeries ts, int feature1, int feature2, String[] namesOfAttributes, float correlation) {
		Point[] points = new Point[ts.records.size() - 1];
		int index = 0;

		// initialize two temp float[] arrays from the correlated features we got from learnNormal
		float[] temp1 = ts.GetValuesOfAttribute(ts.records.get(0).get(feature1)).clone();
		float[] temp2 = ts.GetValuesOfAttribute(ts.records.get(0).get(feature2)).clone();

		for (int i = 0; i < temp1.length; i++) {
			points[index] = new Point(temp1[i], temp2[i]);
			index++;
		}

		// create a line from given points
		Line line = StatLib.linear_reg(points);

		// calculate the maximum deviation in the array of points
		float maxDeviation = 0;
		for (Point point : points) {
			float max = StatLib.dev(point, line);
			if (max > maxDeviation)
				maxDeviation = max;
		}
		// multiply max deviation to avoid false alarms
		maxDeviation *= 1.1;
		// create new object in correlatedFeatures List
		correlatedFeatures.add(new CorrelatedFeatures(namesOfAttributes[feature1], namesOfAttributes[feature2], correlation, line, maxDeviation));
	}
}
