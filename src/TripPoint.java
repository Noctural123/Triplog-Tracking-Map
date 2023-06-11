import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class TripPoint {

	private double lat;	// latitude
	private double lon;	// longitude
	private int time;	// time in minutes

	private static final double H1_DISTANCE = 0.6;	// kilometers for heuristic threshold

	private static ArrayList<TripPoint> trip;	// ArrayList of every point in a trip
	private static ArrayList<TripPoint> movingtrip;	// ArrayList of only moving points in a trip

	// default constructor
	public TripPoint() {
		time = 0;
		lat = 0.0;
		lon = 0.0;
	}

	// constructor given time, latitude, and longitude
	public TripPoint(int time, double lat, double lon) {
		this.time = time;
		this.lat = lat;
		this.lon = lon;
	}

	// returns time
	public int getTime() {
		return time;
	}

	// returns latitude
	public double getLat() {
		return lat;
	}

	// returns longitude
	public double getLon() {
		return lon;
	}

	// returns a copy of trip ArrayList
	public static ArrayList<TripPoint> getTrip() {
		return new ArrayList<>(trip);
	}

	// uses the haversine formula for great sphere distance between two points
	public static double haversineDistance(TripPoint first, TripPoint second) {
		// distance between latitudes and longitudes
		double lat1 = first.getLat();
		double lat2 = second.getLat();
		double lon1 = first.getLon();
		double lon2 = second.getLon();

		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		// convert to radians
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);

		// apply formulae
		double a = Math.pow(Math.sin(dLat / 2), 2) +
				Math.pow(Math.sin(dLon / 2), 2) *
				Math.cos(lat1) *
				Math.cos(lat2);
		double rad = 6371;
		double c = 2 * Math.asin(Math.sqrt(a));
		return rad * c;
	}

	// finds the average speed between two TripPoints in km/hr
	public static double avgSpeed(TripPoint a, TripPoint b) {

		int timeInMin = Math.abs(a.getTime() - b.getTime());

		double dis = haversineDistance(a, b);

		double kmpmin = dis / timeInMin;

		return kmpmin*60;
	}

	// returns the total time of trip in hours
	public static double totalTime() {
		int minutes = trip.get(trip.size()-1).getTime();
		double hours = minutes / 60.0;
		return hours;
	}

	// finds the total distance traveled over the trip
	public static double totalDistance() throws FileNotFoundException, IOException {

		double distance = 0.0;

		if (trip.isEmpty()) {
			readFile("triplog.csv");
		}

		for (int i = 1; i < trip.size(); ++i) {
			distance += haversineDistance(trip.get(i-1), trip.get(i));
		}

		return distance;
	}

	public String toString() {

		return null;
	}

	public static void readFile(String filename) throws FileNotFoundException, IOException {

		// construct a file object for the file with the given name.
		File file = new File(filename);

		// construct a scanner to read the file.
		Scanner fileScanner = new Scanner(file);

		// initiliaze trip
		trip = new ArrayList<TripPoint>();

		// create the Array that will store each lines data so we can grab the time, lat, and lon
		String[] fileData = null;

		// grab the next line
		while (fileScanner.hasNextLine()) {
			String line = fileScanner.nextLine();

			// split each line along the commas
			fileData = line.split(",");

			// only write relevant lines
			if (!line.contains("Time")) {
				// fileData[0] corresponds to time, fileData[1] to lat, fileData[2] to lon
				trip.add(new TripPoint(Integer.parseInt(fileData[0]), Double.parseDouble(fileData[1]), Double.parseDouble(fileData[2])));
			}
		}

		// close scanner
		fileScanner.close();
	}

	// remove the stopped points based on heuristic 1, and return the number of stopped points
	public static int h1StopDetection() throws FileNotFoundException, IOException {
		// track number of stops and initialize movingtrip
		int numStops = 0;
		if (trip == null) {
			readFile("triplog.csv");
		}
		movingtrip = new ArrayList<TripPoint>();
		// add first point because it will never be a stop
		movingtrip.add(trip.get(0));

		// iterate through the TripPoints
		for (int i = 1; i < trip.size(); ++i) {
			// find the distance between two consectutive points
			double distance = haversineDistance(trip.get(i), trip.get(i-1));
			// check the distance against the threshold
			if (distance > H1_DISTANCE) {
				// this point is not a stop
				movingtrip.add(trip.get(i));
			}
			else {
				// this point is a stop
				++numStops;
			}
		}

		return numStops;
	}
	
	public static int h2StopDetectionSimplified() throws FileNotFoundException, IOException {
		int stops = 0;
		if (trip == null) {
			readFile("triplog.csv");
		}
		movingtrip = getTrip();
		
		double radius = 0.5;
		int minGroupSize = 3;
		
		ArrayList<TripPoint> stopGroup = new ArrayList<>();
		
		boolean aStop = false;
		
		for (int i = 0; i < trip.size(); ++i, aStop = false) {
			TripPoint curPoint = trip.get(i);
			
			// check to see if curPoint is part of the current Stop Group
			for (int j = 0; j < stopGroup.size(); ++j) {
				if (haversineDistance(curPoint, stopGroup.get(j)) <= radius) {
					stopGroup.add(curPoint);
					aStop = true;
					break;
				}
			}
			
			if (!aStop) {
				// first remove previous group
				if (stopGroup.size() >= minGroupSize) {
					movingtrip.removeAll(stopGroup);
					stops += stopGroup.size();
				}
				
				// then create new group
				stopGroup = new ArrayList<>();
				stopGroup.add(curPoint);
			}
			
		}
		
		// add the final group if there is one
		if (stopGroup.size() >= minGroupSize) {
			movingtrip.removeAll(stopGroup);
			stops += stopGroup.size();
		}
		
		return stops;
	}
	
	// remove the stopped points based on heuristic 2, and return the number of stopped points
	public static int h2StopDetection() throws FileNotFoundException, IOException {
		// track number of stops and initialize movingtrip
		int numStops = 0;
		if (trip == null) {
			readFile("triplog.csv");
		}
		movingtrip = getTrip();

		// set radius threshold for idle zone
		double idleRadius = 0.5; // assuming units are in kilometers

		// set minimum number of points required to identify an idle zone
		int minIdlePoints = 3;

		// initialize list of idle zones
		ArrayList<ArrayList<TripPoint>> idleZones = new ArrayList<>();

		// initialize list of points for current idle zone
		ArrayList<TripPoint> currentIdleZone = new ArrayList<>();

		// iterate through ArrayList of TripPoint objects
		for (int i = 0; i < trip.size(); i++) {
			TripPoint currentPoint = trip.get(i);

			// check if current point is within idle radius of any point in current idle zone
			boolean withinIdleZone = false;
			for (TripPoint idlePoint : currentIdleZone) {
				if (haversineDistance(currentPoint, idlePoint) <= idleRadius) {
					withinIdleZone = true;
					break;
				}
			}

			if (withinIdleZone) {
				// add current point to current idle zone
				currentIdleZone.add(currentPoint);
			} else {
				// check if current idle zone has enough points to be considered an idle zone
				if (currentIdleZone.size() >= minIdlePoints) {
					// add current idle zone to list of idle zones
					idleZones.add(currentIdleZone);
				}

				// start a new idle zone with current point
				currentIdleZone = new ArrayList<>();
				currentIdleZone.add(currentPoint);
			}
		}

		// check if last idle zone has enough points to be considered an idle zone
		if (currentIdleZone.size() >= minIdlePoints) {
			idleZones.add(currentIdleZone);
		}

		// now remove all idle zones from movingtrip, also calculate numStops
		for (int i = 0; i < idleZones.size(); ++i) {
			movingtrip.removeAll(idleZones.get(i));
			numStops += idleZones.get(i).size();
		}

		return numStops;
	}

	// total moving time in hours
	public static double movingTime() {
		// find time in minutes only counting the moving points
		int minutes = (movingtrip.size()-1)*5;
		double hours = minutes / 60.0;
		return hours;
	}

	// total stopped time in hours
	public static double stoppedTime() {
		// stopped time is just total time minus moving time
		double hours = totalTime()-movingTime();
		return hours;
	}

	// average moving speed in km per hour
	public static double avgMovingSpeed() {
		double totalDistance = 0.0;			// all distances summed
		double totalTime = movingTime();	// time spent moving

		for (int i = 1; i < movingtrip.size(); ++i) {
			// add distance between two consecutive points to totalDistance
			totalDistance += haversineDistance(movingtrip.get(i), movingtrip.get(i-1));
		}

		return totalDistance/totalTime;
	}

	// returns a copy of trip ArrayList
	public static ArrayList<TripPoint> getMovingTrip() {
		return new ArrayList<>(movingtrip);
	}











































}
