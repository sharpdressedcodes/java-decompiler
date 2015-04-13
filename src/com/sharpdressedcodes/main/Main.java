package com.sharpdressedcodes.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

@SuppressWarnings("serial")
public class Main extends JPanel {

	private static String jadPath;
	private static Main _app;
	private static File _lastPath;
	final static String _settingsName = "sharpdressedcodes.decompiler.properties";

	public static void main(final String[] args) {

		// Schedule a job for the event dispatch thread: creating and showing
		// this application's GUI.

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				createAndShowGUI(args);
			}
		});

	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private static void createAndShowGUI(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		// Create and set up the window.
		JFrame frame = new JFrame("Java Decompiler");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Main app = new Main();
		JButton cmdFile = new JButton();
		JButton cmdDirectory = new JButton();
		
		_app = app;
		_lastPath = null;
		
		app.loadSettings();
		
		if ((jadPath == null || jadPath.equals("")) && !handleJadDialog(app)){
			JOptionPane.showMessageDialog(app, "Error: No Jad file selected. Can't continue, aborting.");
			return;
		}
		
		WindowListener exitListener = new WindowListener(){
			@Override
			public void windowClosing(WindowEvent e){
				_app.saveSettings();
			}

			@Override
			public void windowActivated(WindowEvent e) {}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowOpened(WindowEvent e) {}
		};

		frame.addWindowListener(exitListener);		
		
		cmdFile.setText("Decompile Files");
		cmdFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleCmdFileClick(_app);
			}
		});
		
		cmdDirectory.setText("Decompile Directory");
		cmdDirectory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleCmdDirectoryClick(_app);
			}
		});

		app.add(cmdFile);
		app.add(cmdDirectory);

		// Add content to the window.
		frame.add(app);

		// Display the window.
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);		
		frame.setVisible(true);

	}
	
	public void loadSettings(){
				
		Properties props = new Properties();
	    InputStream is = null;
	 
	    // First try loading from the current directory
	    try {
	        File f = new File(_settingsName);
	        is = new FileInputStream( f );
	    }
	    catch ( IOException e ) { is = null; }	    
	 
	    try {
	        if ( is == null ) {
	            // Try loading from classpath
	            is = getClass().getResourceAsStream(_settingsName);
	        }
	 
	        // Try loading properties from the file (if found)
	        if (is != null){
	        	props.load( is );
	        	jadPath = props.getProperty("jadPath", "");
	    	    String s = props.getProperty("lastPath", "");	    
	    	    _lastPath = s.equals("") ? null : new File(s);
	        }
	        
	    }
	    catch ( Exception e ) { }
	    
	}
	
	public void saveSettings(){
		
		try {
	        Properties props = new Properties();
	        props.setProperty("jadPath", jadPath == null ? "" : jadPath);
	        props.setProperty("lastPath", _lastPath == null ? "" : _lastPath.getAbsolutePath());	        
	        File f = new File(_settingsName);
	        OutputStream out = new FileOutputStream( f );
	        props.store(out, "Decompiler settings file. DO NOT EDIT!");
	    } catch (IOException e ) {
	    	JOptionPane.showMessageDialog(_app, e.getMessage());
	    }
		
	}
	
	public static boolean handleJadDialog(Main app){
		
		final JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileNameExtensionFilter("Jad Executable", "exe"));
		chooser.setMultiSelectionEnabled(true);		
		if (_lastPath != null) {
			chooser.setCurrentDirectory(_lastPath);
		}
		
		if (chooser.showOpenDialog(app) == JFileChooser.APPROVE_OPTION) {			
			_lastPath = chooser.getCurrentDirectory();
			jadPath = chooser.getSelectedFile().getAbsolutePath();
			return true;
		}
		
		return false;
		
	}
	
	public static void handleCmdFileClick(Main app) {
		
		final JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileNameExtensionFilter("Java Classes", "class"));
		chooser.setMultiSelectionEnabled(true);		
		if (_lastPath != null) {
			chooser.setCurrentDirectory(_lastPath);
		}
		
		if (chooser.showOpenDialog(app) == JFileChooser.APPROVE_OPTION) {
			
			_lastPath = chooser.getCurrentDirectory();
			File[] selected = chooser.getSelectedFiles();
			int count = 0;
			
			if (selected.length > 0) {
				for (int i = 0; i < selected.length; i++) {						
					try {
						if (decompileFile(selected[i].getAbsolutePath(), null)){
							count++;
						}
					} catch (FileNotFoundException e){
						JOptionPane.showMessageDialog(app, e.getMessage());
					} catch (IOException e){
						JOptionPane.showMessageDialog(app, e.getMessage());
					}
				}
				JOptionPane.showMessageDialog(app, String.format(
					"Complete. %d decompiled. %d errors.",
					count,
					selected.length - count
				));
			}			
		}
		
	}
	
	public static void handleCmdDirectoryClick(Main app) {

		int count = 0;
		int errors = 0;
		final JFileChooser chooser = new JFileChooser();

		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (_lastPath != null) {
			chooser.setCurrentDirectory(_lastPath);
		}

		if (chooser.showOpenDialog(app) == JFileChooser.APPROVE_OPTION) {
			_lastPath = chooser.getSelectedFile();
			try {
				int[] arr = decompileDirectory(_lastPath.getAbsolutePath());
				count += arr[0];
				errors += arr[1];
			} catch (FileNotFoundException e){
				JOptionPane.showMessageDialog(app, e.getMessage());
			} catch (IOException e){
				JOptionPane.showMessageDialog(app, e.getMessage());
			}
			JOptionPane.showMessageDialog(app, String.format(
				"Complete. %d decompiled. %d errors.",
				count,
				errors
			));
		}

	}

	private static boolean decompileFile(String input, String output) 
			throws FileNotFoundException, IOException {

		String command = null;
		String result = null;
		Process p = null;
		BufferedReader r = null;
		StringBuilder sb = null;
		final String lookup = "\r\n\r\n";
		OutputStream stream = null;
		File compiledFile = new File(input);
		File decompiledFile = new File(output == null ? input.replaceFirst(".class", ".java") : output);

		if (!compiledFile.exists()) {
			throw new FileNotFoundException("Error: File does not exist. [" + input + "]");
		}

		if (decompiledFile.exists()) {
			decompiledFile.delete();
		}
			
		command = String.format(
			"%s %s %s",
			jadPath,
			"-noctor -p -t2 -space -nonlb",
			compiledFile.getAbsolutePath()
		);
		p = Runtime.getRuntime().exec(command);
		r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		sb = new StringBuilder();

		do {
			result = r.readLine();
			if (result != null)
				sb.append(result + "\r\n");
		} while (result != null);

		r.close();
		p.destroy();

		// remove comments inserted by author of JAD
		int pos = sb.indexOf(lookup);
		if (pos > -1)
			sb = new StringBuilder(sb.substring(pos + lookup.length()));

		stream = new FileOutputStream(decompiledFile);
		stream.write(sb.toString().getBytes(), 0, sb.length());
		stream.flush();
		stream.close();

		return true;

	}

	public static int[] decompileDirectory(String directory)
			throws FileNotFoundException, IOException {

		List<File> allFiles = getAllFiles(directory);
		int count = 0;
		int errors = 0;

		for (int i = 0; i < allFiles.size(); i++) {
			if (allFiles.get(i).getAbsolutePath().toLowerCase().endsWith(".class")) {
				if (decompileFile(allFiles.get(i).getAbsolutePath(), null)) {
					count++;
				} else {
					errors++;
				}
			}
		}

		return new int[]{count, errors};

	}

	private static List<File> getAllFiles(String directory) {

		List<File> allFiles = new ArrayList<File>();
		Queue<File> dirs = new LinkedList<File>();
		
		dirs.add(new File(directory));
		
		while (!dirs.isEmpty()) {
			for (File f : dirs.poll().listFiles()) {
				if (f.isDirectory()) {
					dirs.add(f);
				} else if (f.isFile()) {
					allFiles.add(f);
				}
			}
		}

		return allFiles;

	}

}
