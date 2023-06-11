import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;






public class Driver implements ActionListener {
	
	String timeValue;
	String iconImage;
	String fileName;
	String color;
	IconMarker startMarker;
	boolean includeStops;
	boolean isRainbow;
	private static Timer timer = null;
	private static boolean run;
	
	JComboBox<String> timeList;
	JComboBox<String> iconList;
	JComboBox<String> colorList;
	JCheckBox stopBox;
	JButton playButton;
	JMapViewer map;
	JProgressBar bar;
	
	ArrayList<TripPoint> tripPoints; // With the stops
	ArrayList<TripPoint> movingPoints; //Without the stops
	ArrayList<Coordinate> coords; // List of coordinates (with stops)
	ArrayList<Coordinate> coords2; //List of Coordinates (without stops)
	


	public Driver() throws FileNotFoundException, IOException
	{
		TripPoint.readFile("triplog.csv");   	
		int stops = TripPoint.h1StopDetection();
		tripPoints = TripPoint.getTrip();
		movingPoints = TripPoint.getMovingTrip();
		
		// Make ArrayList of Just Coordinates (with stops)
		coords = new ArrayList<Coordinate>();
		for(int i = 0; i < tripPoints.size(); i++)
		{
			double lat = tripPoints.get(i).getLat();
			double lon = tripPoints.get(i).getLon();
			coords.add(new Coordinate(lat,lon));
		}
		
		// Make ArrayList of Just Coordinates (without stops)
		coords2 = new ArrayList<Coordinate>();
		for(int i = 0; i < movingPoints.size(); i++)
		{
			double lat = movingPoints.get(i).getLat();
			double lon = movingPoints.get(i).getLon();
			coords2.add(new Coordinate(lat,lon));
		}   
		setFrame();
	}
	
	public void setFrame()
	{
		JFrame frame = new JFrame("Project 5 - An Nguyen");
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	
    	frame.setVisible(true);

        
        // Set up Panel for input selections
    	JPanel panel = new JPanel();
    	
    	//JComboBox (Time)
        String[] times = {"Animation Time", "15", "30", "60", "90"};
        timeList = new JComboBox<>(times);
        timeList.setSelectedIndex(0);
        timeList.addActionListener(this);
        
        //JComboBox (Icon)
        String[] icons = {"Select Icon!", "Raccoon", "Yasuo", "Capybara!", "Dr. Maiti"};
        iconList = new JComboBox<>(icons);
        iconList.setSelectedIndex(0);
        iconList.addActionListener(this);
        
        //JComboBox (Color)
        String [] colors = {"Select Color", "Red", "Green", "Blue", "Rainbow"};
        colorList = new JComboBox<>(colors);
        colorList.setSelectedIndex(0);
        colorList.addActionListener(this);
        
        //JCheckBox
        stopBox = new JCheckBox("Include Stops");
        stopBox.setSelected(false);
        stopBox.addActionListener(this);
        
        //JButton
        playButton = new JButton("Play / Reset");
        playButton.addActionListener(this);
        
        //JProgressBar
        bar = new JProgressBar(0,100);
        bar.setValue(0);
        bar.setStringPainted(true);
        
        panel.add(timeList);
        panel.add(iconList);
        panel.add(colorList);
        panel.add(stopBox);
        panel.add(playButton);
        panel.add(bar);
        
        //JMapViewer
        map = new JMapViewer();
        map.setTileSource(new OsmTileSource.TransportMap());
        map.setDisplayPosition(new Coordinate(36.9072, -105.0369), 5);
        
        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new BorderLayout());
        mapPanel.add(map, BorderLayout.CENTER);
        map.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(panel);
        mainPanel.add(mapPanel);
        frame.add(mainPanel);
        frame.setSize(new Dimension(800, 600));
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == timeList)
		{
			timeValue = (String) timeList.getSelectedItem();
		}
		else if (e.getSource() == iconList)
		{
			iconImage = (String)iconList.getSelectedItem();
			
			//Get the image png name
			switch(iconImage)
			{
			case "Raccoon":
				fileName = "raccoon.png";
				break;
			case "Yasuo":
				fileName = "Yasuo1.png";
				break;
			case "Capybara!":
				fileName = "capybara.png";
				break;
			case "Dr. Maiti":
				fileName = "Dr. Maiti.png";
				break;
			}
		}
		else if(e.getSource() == colorList)
		{
			color = (String)colorList.getSelectedItem();
		}
		else if(e.getSource() == stopBox)
		{
			includeStops = stopBox.isSelected();
		}
		else if(e.getSource() == playButton)
		{
			
			map.removeAllMapMarkers();
			map.removeAllMapPolygons();
			
			BufferedImage image;
			String tempColor = color;
			
			
			
			//Find proper ArrayList depending on stops or not
			ArrayList<Coordinate> list;
			if(includeStops)
			{
				list = new ArrayList<Coordinate>(coords);
			}
			else
			{
				list = new ArrayList<Coordinate>(coords2);
			}
			
			try {
				image = ImageIO.read(new File(fileName));
				Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
				startMarker = new IconMarker(list.get(0), scaledImage);
				map.addMapMarker(startMarker);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			run = true;
			
			if(timer != null && timer.isRunning())
			{
	    		run = false;
			}
			
			
		
			
			int delay = (Integer.parseInt(timeValue))*1000/list.size(); // milliseconds
			timer = new Timer(delay, new ActionListener() {
				
				int i = 1;
				
				//Default settings for colors
				float hue = 0;
				float saturation = 1.0f;
				float brightness = 1.0f;
				
			    public void actionPerformed(ActionEvent evt) {
			        if (i < list.size() - 1 && run) { // subtract 1 from coords.size() to avoid creating a polygon with only one coordinate
			        	
			        	ArrayList<Coordinate> temp = new ArrayList<Coordinate>();
			        	Coordinate c1 = list.get(i);
			        	Coordinate c2 = list.get(i+1);
			        	temp.add(c1);
			        	temp.add(c1);
			        	temp.add(c2);
			            MapPolygonImpl polygon = new MapPolygonImpl(temp); // create polygon using the first i+2 coordinates
			            
			            //Set the color base on the input
			            if(tempColor.equals("Rainbow")) {
				            Color rainbowColor = Color.getHSBColor(hue, saturation, brightness);
				            hue += 0.05f;
				            if (hue > 1.0f) {
				                hue = 0;
				            }
				            polygon.setColor(rainbowColor);
			            }
			            else if(tempColor.equals("Red"))
			            {
			            	polygon.setColor(Color.RED);
			            }
			            else if(tempColor.equals("Green"))
			            {
			            	polygon.setColor(Color.GREEN);
			            }
			            else if(tempColor.equals("Blue"))
			            {
			            	polygon.setColor(Color.BLUE);
			            }
			            
			            polygon.setStroke(new BasicStroke(3));
			            
			            startMarker.setLat(list.get(i).getLat());
			            startMarker.setLon(list.get(i).getLon());
			          
			            //Add drawing to the Map GUI
			            map.addMapPolygon(polygon);
			          
			            
			            //Update the progress bar
			            int progress = (int)Math.ceil((i*1.0 * 100) / (list.size()));
			            bar.setValue(progress);
			            
			            i++;
			        }
			        else
			        {
			        	((Timer) evt.getSource()).stop();
			        }
			    }
			});

			timer.start();
		}
		
	}
	
    public static void main(String[] args) throws FileNotFoundException, IOException {

    	Driver driver = new Driver();
      
    }
}