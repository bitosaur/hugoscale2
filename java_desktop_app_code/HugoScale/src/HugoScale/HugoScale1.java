package HugoScale;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JTextField;
import java.awt.Dimension;
import javax.swing.UIManager;
import java.awt.Font;

public class HugoScale1 {

	private JFrame frame;
	
	static SerialPort chosenPort; // not static for multiple serial plots 
	static int x = 0;				// keep it static to keep comparable time among multiple serial plots
	static final int baudRate = 9600; // for multiple serial ports, baudRate may have to be set at each object
	static final int NSAMPLES = 10;
	private JTextField calibrateValue;
	
	static float equation_m; static float equation_c; static float x1; static float x2; static float y2;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					HugoScale1 window = new HugoScale1();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
   public static void saveCalibrationData(float m, float c)
   {
       try{
     // Create file 
     FileWriter fstream = new FileWriter("HugoScale_calib.txt");
     BufferedWriter out = new BufferedWriter(fstream);
     out.write(String.valueOf(m));out.write("\n");
     out.write(String.valueOf(c));out.write("\n");
     //Close the output stream
     out.close();
     }catch (Exception e){//Catch exception if any
       System.err.println("Error: " + e.getMessage());
     }
   } 

   public static void loadCalibrationData()
   {
	   try {
		 //the file to be opened for reading  
		   FileInputStream fis=new FileInputStream("HugoScale_calib.txt");       
		   Scanner sc=new Scanner(fis);    //file to be scanned  
		   //returns true if there is another line to read  
		   equation_m = sc.nextFloat();
		   equation_c = sc.nextFloat();
		   System.out.println("Starting Equation is Y = "+String.valueOf(equation_m)+" X + "+String.valueOf(equation_c));
		   sc.close();     //closes the scanner  
	   }catch (Exception e){//Catch exception if any
       System.err.println("Error: " + e.getMessage());
     }
   } 
	   
	/**
	 * Create the application.
	 */
	public HugoScale1() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		float[] lastSensorVals = new float[NSAMPLES];
		MyArrayMaths m = new MyArrayMaths();
		
		// create and configure the window		
		frame = new JFrame();
		frame.setTitle("Sensor Graph GUI");
		//frame.setBounds(100, 100, 450, 300);
		frame.setSize(600, 400);
		frame.getContentPane().setLayout(new BorderLayout());		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// create a drop-down box and connect button, then place them at the top of the window
		JComboBox<String> portList = new JComboBox<String>();
		JButton connectButton = new JButton("Connect");
		JButton zeroButton = new JButton("Set Zero");
		
		zeroButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				x1 = m.AvgArrayFloat(lastSensorVals);
				equation_c = -1 * equation_m * x1;
				System.out.println("Equation is Y = "+String.valueOf(equation_m)+" X + "+String.valueOf(equation_c));
				saveCalibrationData(equation_m, equation_c); // persistence
			}
		});
		
		
		JButton calibrateButton = new JButton("Calibrate");		
		calibrateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				try {
					y2 = Float.parseFloat(calibrateValue.getText());
					x2 = m.AvgArrayFloat(lastSensorVals);
					equation_m = y2/ (x2 - x1);
					
					System.out.println("Equation is Y = "+String.valueOf(equation_m)+" X + "+String.valueOf(equation_c));
					saveCalibrationData(equation_m, equation_c); // persistence
				}
				catch(NumberFormatException ee) {System.out.println("Please enter weight to calibrate");}
			}
		});
		
		
		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(connectButton);
		topPanel.add(zeroButton);
		Component rigidArea = Box.createRigidArea(new Dimension(20, 20));
		topPanel.add(rigidArea);
		topPanel.add(calibrateButton);

		calibrateValue = new JTextField();
		calibrateValue.setBackground(UIManager.getColor("Button.disabledShadow"));
		calibrateValue.setToolTipText("Enter calibration weight in lbs");
		topPanel.add(calibrateValue);
		calibrateValue.setColumns(5);	
		
		frame.getContentPane().add(topPanel, BorderLayout.NORTH);
		

		
		// create text display panel
		JPanel textPanel = new JPanel();

		JLabel lblCurVal = new JLabel("CurVal");
		lblCurVal.setFont(new Font("Tahoma", Font.PLAIN, 16));
		textPanel.add(lblCurVal);
		
		Component verticalGlue = Box.createVerticalGlue();
		textPanel.add(verticalGlue);

		JLabel lblAvg = new JLabel("Avg");
		lblAvg.setFont(new Font("Tahoma", Font.PLAIN, 16));
		textPanel.add(lblAvg);
		
		frame.getContentPane().add(textPanel, BorderLayout.SOUTH);

		// populate the drop-down box
		SerialPort[] portNames = SerialPort.getCommPorts();
		for(int i = 0; i < portNames.length; i++)
			portList.addItem(portNames[i].getSystemPortName());
		
		// create the line graph
		XYSeries series = new XYSeries("Instanteneous Reading");
		XYSeries series2 = new XYSeries("Moving Average");
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		dataset.addSeries(series2);
		
		JFreeChart chart = ChartFactory.createXYLineChart("HugoScale Readings", "Time", "Reading", dataset);
		ChartPanel chartPanel = new ChartPanel(chart);
		frame.getContentPane().add(chartPanel, BorderLayout.CENTER);

		
		// Load Equation parameters from file
		loadCalibrationData();
		
		
		// configure the connect button and use another thread to listen for data
		connectButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				if(connectButton.getText().equals("Connect")) {
					// attempt to connect to the serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					chosenPort.setBaudRate(baudRate);

					System.out.println("Current Baud Rate is " + chosenPort.getBaudRate());

					if(chosenPort.openPort()) {
						connectButton.setText("Disconnect");
						portList.setEnabled(false);
					}
					
					// create a new thread that listens for incoming text and populates the graph
					Thread thread = new Thread(){
						@Override public void run() {
							Scanner scanner = new Scanner(chosenPort.getInputStream());
							System.out.println("Listener thread started");

							while(scanner.hasNextLine()) {
								try {
									x++;
									String line = scanner.nextLine();
									// int number = Integer.parseInt(line);
									float number = Float.parseFloat(line);
									float weight = equation_m * number + equation_c;
									series.add(x, weight);
									lblCurVal.setText("Current: "+String.valueOf(weight));
									lastSensorVals[x % NSAMPLES] = number;
									
									float avgnumber = m.AvgArrayFloat(lastSensorVals);
									float avgweight = equation_m * avgnumber + equation_c;
									lblAvg.setText("Average: "+String.valueOf(avgweight));									
									series2.add(x, avgweight);
									
									frame.repaint();
								
									System.out.print(x); System.out.print(", "); System.out.print(line); System.out.print(", ");System.out.println(m.AvgArrayFloat(lastSensorVals));
								} catch(Exception e) {e.printStackTrace(); }
							}
							scanner.close();
						}
					};
					thread.start();
				} else {
					// disconnect from the serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setText("Connect");
					series.clear();
					x = 0;
				}
			}
		});			
		
	}
	
}