package trajsensor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;

import trajsensor.model.Trajectory;
import trajsensor.model.TrajectoryPoint;

public class SensorDataReader {

	private static final String SEPARATOR = ";";
	private static final int SENSOR_CODE_INDEX = 0;
	private static final int DATE_INDEX = 1;
	private static final int TIMESTAMP_INDEX = 2;
	private static final int SPEED_INDEX = 5;
	private static final int VEHICLE_ID_INDEX = 8;

	private static HashMap<String, Trajectory> trajectoryMap = new HashMap<String, Trajectory>();
	private static ArrayList<TrajectoryPoint> missingPointsList = new ArrayList<TrajectoryPoint>();
	private static HashSet<String> sensorsCodeList = new HashSet<String>();
	private static HashMap<String, Long> sensorsCodeNodeIdMap = new HashMap<String, Long>();
	private static int[][] odMatrix;
	private static long[][] maxTimeMatrix;
	public static long id;

	public static Long getNodeIdOfSensor(String code) {
		if (sensorsCodeNodeIdMap != null) {
			return sensorsCodeNodeIdMap.get(code);
		}
		return null;
	}

	public static void readTrajectories(String pathFile, boolean clearAll) throws IOException, ParseException {
		String strLine = null;
		FileInputStream fstream = new FileInputStream(pathFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		
		if (clearAll)
			clearAll();

		while ((strLine = br.readLine()) != null) {
			String[] split = strLine.split(SEPARATOR);
			String sensorCodeStr = split[SENSOR_CODE_INDEX];
			String[] dateStr = split[DATE_INDEX].split("-");
			String[] timestampStr = split[TIMESTAMP_INDEX].split(":");
			String speedStr = split[SPEED_INDEX];
			String vehicleId = null;

			if (split.length == VEHICLE_ID_INDEX + 1) {
				vehicleId = split[VEHICLE_ID_INDEX];
			}

			TrajectoryPoint point = new TrajectoryPoint(sensorCodeStr, vehicleId,
					Double.valueOf(speedStr.replace(",", ".")), getCalendarTime(dateStr, timestampStr));

			loadPoint(point);

			loadSensorCode(sensorCodeStr);
			
		}
		br.close();
	}

	private static void clearAll() {
		trajectoryMap.clear();
		missingPointsList.clear();
		sensorsCodeList.clear();
		sensorsCodeNodeIdMap.clear();
		id=0;
	}

	private static Date getCalendarTime(String[] dateStr, String[] timestamp) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(Integer.valueOf(dateStr[0]), Integer.valueOf(dateStr[1]) - 1, Integer.valueOf(dateStr[2]),
				Integer.valueOf(timestamp[0]), Integer.valueOf(timestamp[1]), Integer.valueOf(timestamp[2]));
		return cal.getTime();
	}

	private static void loadSensorCode(String sensorCodeStr) {

		if (!sensorsCodeList.contains(sensorCodeStr)) {
			sensorsCodeNodeIdMap.put(sensorCodeStr, (long) id);
			id++;
			sensorsCodeList.add(sensorCodeStr);
		}
	}

	private static void loadPoint(TrajectoryPoint point) {
		if (point.getIdVehicle() != null) {
			Trajectory trajectory = createOrObtainTrajectory(point);
			trajectory.addNewPoint(point);

		} else {
			missingPointsList.add(point);
		}
	}

	private static Trajectory createOrObtainTrajectory(TrajectoryPoint point) {
		if (!trajectoryMap.containsKey(point.getIdVehicle())) {
			Trajectory traj = new Trajectory();
			trajectoryMap.put(point.getIdVehicle(), traj);
			return traj;
		} else {
			return trajectoryMap.get(point.getIdVehicle());
		}
	}


	public static void generateODMatrix() {
		odMatrix = new int[sensorsCodeList.size()+1][sensorsCodeList.size()+1];
		maxTimeMatrix = new long[sensorsCodeList.size()+1][sensorsCodeList.size()+1];

		for (String vehicleId : trajectoryMap.keySet()) {
			Trajectory trajectory = trajectoryMap.get(vehicleId);
			TrajectoryPoint first = trajectory.getFirst(), last = trajectory.getLast();

			Long fromID = sensorsCodeNodeIdMap.get(first.getCodeSensor());
			Long toID = sensorsCodeNodeIdMap.get(last.getCodeSensor());
			odMatrix[fromID.intValue()][toID.intValue()]++;
			updateMaxTravelTimeInfo(first, last, fromID, toID);

		}

	}

	public static void saveODMatrix(String filepath) {
		try {
			FileWriter writer = new FileWriter(filepath + "IDS");
			FileWriter writerm = new FileWriter(filepath + "MATRIX");
			for (String code : sensorsCodeNodeIdMap.keySet()) {
				writer.write(code + " , " + sensorsCodeNodeIdMap.get(code));
				writer.write("\n");
			}

			for (int i = 0; i < odMatrix.length; i++) {
				for (int j = 0; j < odMatrix.length; j++) {
					writerm.write(i + ", " + j + " , " + odMatrix[i][j]);
					writerm.write("\n");
				}

			}

			writer.close();
			writerm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void updateMaxTravelTimeInfo(TrajectoryPoint ant, TrajectoryPoint suc, Long fromID, Long toID) {
		// update maximal travel time
		long deltaTime = suc.getTimestamp().getTime() - ant.getTimestamp().getTime();
		if (deltaTime > maxTimeMatrix[fromID.intValue()][toID.intValue()]) {
			maxTimeMatrix[fromID.intValue()][toID.intValue()] = deltaTime;
		}

		// add info that pass by a node
		odMatrix[fromID.intValue()][fromID.intValue()]++;
	}


	public static void saveTrajectories(String filepath) {
		try {
			FileWriter writer = new FileWriter(filepath + "TRAJ");
			for (Trajectory t : trajectoryMap.values()) {
				t.sort();
				writer.write(t.toString());
			}

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		if (args.length == 0) {
			// String path = args[0];
			// File directory = new
			// File(path)/media/livia/DATA/Workspace/maven.1470949999564/missingdatatraj/;
			File directory = new File("/media/livia/DATA/Workspace/maven.1470949999564/missingdatatraj/arquivos/");
			File[] listFiles = directory.listFiles();

			for (File file : listFiles) {
				System.out.println(file.getAbsolutePath());
				if (file.getName().startsWith("Ofuscado")) {
					try {
						SensorDataReader.readTrajectories(file.getAbsolutePath(), true);
						SensorDataReader.saveTrajectories(directory + "/output/" + file.getName().replaceAll(".csv", "_"));
						SensorDataReader.generateODMatrix();
						SensorDataReader.saveODMatrix(directory + "/output/" + file.getName().replaceAll(".csv", "_"));

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

		}

	}
}
