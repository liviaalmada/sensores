package trajsensor.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class Trajectory {

	private LinkedList<TrajectoryPoint> points;
	private String vehicle;
	
	public Trajectory() {
		points = new LinkedList<TrajectoryPoint>();
	}
	
	public void addNewPoint(TrajectoryPoint d){
		vehicle = d.getIdVehicle();
		points.add(d);
	}
	
	public TrajectoryPoint getFirst(){
		return points.getFirst();
	}

	public TrajectoryPoint getLast(){
		return points.getLast();
	}
	
	public void sort(){
		Collections.sort(points, new SensorDataComparator());
	}
	
	public Iterator<TrajectoryPoint> getIterator(){
		return points.iterator();
	}
	
	@SuppressWarnings("unused")
	private class SensorDataComparator implements Comparator<TrajectoryPoint> {

		public int compare(TrajectoryPoint o1, TrajectoryPoint o2) {
			// TODO Auto-generated method stub
			if (o1.getTimestamp().before(o2.getTimestamp()))
				return -1;
			else if (o1.getTimestamp().before(o2.getTimestamp()))
				return 1;
			else
				return 0;
		}

	}
	
	public String toString(){
		if(!points.isEmpty()){
			return vehicle+": "+points+"\n";
		}
		return null;
	}
	

}
