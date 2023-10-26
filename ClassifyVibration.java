import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;
import java.io.File;
import weka.core.SerializationHelper;
//import javax.swing.*;
//import java.awt.*;
import javax.swing.JOptionPane;

/* A class with the main function and Processing visualizations to run the demo */

public class ClassifyVibration extends PApplet {

	
	
	FFT fft;
	AudioIn in;
	Waveform waveform;
	int bands = 512;
	int nsamples = 1024;
	float[] spectrum = new float[bands];
	float[] fftFeatures = new float[bands];
	String[] classNames = {"Interaction#1", "Interaction#2", "Neutral", "Noise1"};
	int classIndex = 0;
	int dataCount = 0;
	boolean started = false;
	boolean is_training = false;
	List<Integer> intResList = new ArrayList<Integer>();
	Queue<Integer> count = new LinkedList<>();

	MLClassifier classifier;
	
	Map<String, List<DataInstance>> trainingData = new HashMap<>();
	{for (String className : classNames){
		trainingData.put(className, new ArrayList<DataInstance>());
	}}
	
	public static void main(String[] args) {
		PApplet.main("ClassifyVibration");
	}
	
	DataInstance captureInstance (String label){
		DataInstance res = new DataInstance();
		res.label = label;
		res.measurements = fftFeatures.clone();
		return res;
	}
	
	public void settings() {
		size(512, 400);
	}

	public void setup() {
		
		/* list all audio devices */
		Sound.list();
		Sound s = new Sound(this);
		  
		/* select microphone device */
		s.inputDevice(5);
		    
		/* create an Input stream which is routed into the FFT analyzer */
		fft = new FFT(this, bands);
		in = new AudioIn(this, 0);
		waveform = new Waveform(this, nsamples);
		waveform.input(in);
		
		/* start the Audio Input */
		in.start();
		  
		/* patch the AudioIn */
		fft.input(in);
	}

	public void draw() {
		background(0);
		fill(0);
		stroke(255);
		
		waveform.analyze();

		beginShape();
		  
		for(int i = 0; i < nsamples; i++)
		{
			vertex(
					map(i, 0, nsamples, 0, width),
					map(waveform.data[i], -1, 1, 0, height)
					);
		}
		
		endShape();

		fft.analyze(spectrum);

		for(int i = 0; i < bands; i++){

			/* the result of the FFT is normalized */
			/* draw the line for frequency band i scaling it up by 40 to get more amplitude */
			line( i, height, i, height - spectrum[i]*height*40);
			fftFeatures[i] = spectrum[i];
		} 

		fill(255);
		textSize(30);
		if(mousePressed && (mouseButton == RIGHT)) {
			is_training = !is_training;
		}
		// application interface
		if(is_training == false) {
			if(started == false) {
				text("Press Space Bar to Start", 20, 30);
			}else {
				// add code to stabilize classification results -- set a threshold for the probabilities
				String guessedLabel = classifier.classify(captureInstance(null));
				count.poll();
				if(guessedLabel.equals("Interaction#1")) {
					intResList.add(1);
					count.offer(1);
				}else if(guessedLabel.equals("Interaction#2")) {
					intResList.add(2);
					count.offer(2);
				}else {
					count.offer(0);
				}
				
				// Remove interference
		        if (count.size() == 3) {
		            Integer first = count.poll();
		            Integer second = count.poll();
		            Integer third = count.poll();

		            if (second.equals(1) || second.equals(2) 
		            	&& first.equals(0) && third.equals(0)) {
		                intResList.remove(intResList.size()-1);
		            }
		            
		            count.offer(first);
		            count.offer(second);
		            count.offer(third);
		        }
			    
		        text("Press Space Bar to End", 20, 30);
			}
		}else {
			text(classNames[classIndex], 20, 30);
			dataCount = trainingData.get(classNames[classIndex]).size();
			text("Data collected: " + dataCount, 20, 60);
		}
	}
	
	
	// save and load functions
	public void saveModel(String path) {
	    try {
	        SerializationHelper.write(path, classifier);
	        System.out.println("Model saved to " + path);
	    } catch (Exception e) {
	        e.printStackTrace();
	        System.out.println("Error saving model!");
	    }
	}
	public void loadModel(String path) {
	    try {
	        classifier = (MLClassifier) SerializationHelper.read(path);
	        System.out.println("Model loaded from " + path);
	    } catch (Exception e) {
	        e.printStackTrace();
	        System.out.println("Error loading model!");
	    }
	}
    public void deleteModel(String path) {
        File file = new File(path);
        
        if (file.exists()) {
            try {
                if (file.delete()) {
                    System.out.println("Model deleted from " + path);
                } else {
                    System.err.println("Cannot delete it " + path);
                }
            } catch (SecurityException e) {
                System.err.println("no access to " + path);
                e.printStackTrace();
            }
        } else {
            System.err.println("Model doesn't exist" + path);
        }
    }
	
	public void keyPressed() {
		

		if (key == CODED && keyCode == DOWN) {
			classIndex = (classIndex + 1) % classNames.length;
		}
		
		else if (key == 't') {
			if(classifier == null) {
				println("Start training ...");
				classifier = new MLClassifier();
				classifier.train(trainingData);
			}else {
				classifier = null;
			}
		}
		
		else if (key == 's') {
			// Yang: add code to save your trained model for later use
			println("Saving model ...");
			saveModel("model.model");
		}
		
		else if (key == 'l') {
			// Yang: add code to load your previously trained model
			println("Loading model ...");
			loadModel("model.model");
		}
		
		else if(key == 'd') {
			println("Deleting model...");
			deleteModel("model.model");
		}
		
		else if (keyCode == 32) {
			if(classifier == null) {
				classifier = new MLClassifier();
				loadModel("model.model");
			}
			if(started == false) {
				started = true;
				count.offer(0);
				count.offer(0);
				count.offer(0);
				println("start recording now");
			}else {
				started = false;
				println("end recording now");
				int count_1 = 0;
				int count_2 = 0;
				
				// remove last special interference
		        if (count.size() == 3) {
		            Integer first = count.poll();
		            Integer second = count.poll();
		            Integer third = count.poll();

		            if (third.equals(1) || third.equals(2) 
		            	&& first.equals(0) && second.equals(0)) {
		                intResList.remove(intResList.size()-1);
		            }
		        }
				
				for(int i : intResList) {
					if(i == 1) {
						count_1++;
					}else{
						count_2++;
					}
				}
	            String resultMessage;
	            if (count_1 >= count_2 && count_1 != 0) {
	                resultMessage = "Interaction#1";
	            } else if (count_1 < count_2 && count_2 != 0) {
	                resultMessage = "Interaction#2";
	            } else {
	                resultMessage = "Neutral";
	            }
	            intResList.clear();

	            // Display the result in a GUI dialog
	            JOptionPane.showMessageDialog(null, "Result: " + resultMessage, "Classification Result", JOptionPane.INFORMATION_MESSAGE);
			}
		}
			
		else {
			trainingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
		}
	}



}
