import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;
import weka.core.SerializationHelper;

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
	List<Integer> intResList = new ArrayList<Integer>();

	MLClassifier classifier;
	
	Map<String, List<DataInstance>> trainingData = new HashMap<>();
	{for (String className : classNames){
		trainingData.put(className, new ArrayList<DataInstance>());
	}}
	
	DataInstance captureInstance (String label){
		DataInstance res = new DataInstance();
		res.label = label;
		res.measurements = fftFeatures.clone();
		return res;
	}
	
	public static void main(String[] args) {
		PApplet.main("ClassifyVibration");
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
		if(classifier != null) {
			String guessedLabel = classifier.classify(captureInstance(null));
			
			// Yang: add code to stabilize your classification results -- set a threshold for the probabilities
			if(started  == true) {
//				System.out.println(guessedLabel.equals("Interaction#1"));
				if(guessedLabel.equals("Interaction#1")) {
//					System.out.println("1");
					intResList.add(1);
				}else if(guessedLabel.equals("Interaction#2")) {
					intResList.add(2);
//					System.out.println("2");
				}else {
//					System.out.println("else");
				}
			}
			
			text("classified as: " + guessedLabel, 20, 30);
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
		
		else if (keyCode == 32) {
			if(started == false) {
				started = true;
				println("start recording now");
			}else {
				started = false;
				println("end recording now");
				int count_1 = 0;
				int count_2 = 0;
				for(int i : intResList) {
					if(i == 1) {
						count_1++;
					}else{
						count_2++;
					}
				}
				if(count_1 >= count_2 && count_1 != 0) {
					println("Interaction#1");
				}else if(count_1 < count_2 && count_2 != 0) {
					println("Interaction#2");
				}else {
					println("Neutral");
				}
				intResList.clear();
			}
		}
			
		else {
			trainingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
		}
	}

}
