package com.kkago.frequencyrecord;

import ca.uol.aig.fftpack.RealDoubleFFT;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import jxl.*;
import jxl.write.*;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphView.GraphViewData;

public class MainActivity extends BaseActivity {
	public static boolean displayRaw;
	public PlaceholderFragment fragment;
	
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int ROW_DEFAULT = 2;
	private static int REFRESH_RATE = 5;
	private static final int REQUEST_SETTINGS = 3208;
	private static final int REQUEST_FILTERS = 2508;
	
	private static int refreshIndex;
	private static RealDoubleFFT transformer;
	private static int sample_frequency;
	private static int blockSize;
	private static double noiseLevel;
	private static double threshold;
	private static double graphRange;
	private static double currPeakFreq;
	private static boolean graphNeedReset;
	
	public static ArrayList<FreqRead> currTargetFreq;
	
	private static int averagePoint;
	private static boolean bandpassEnabled;
	private static double bandpassLow;
	private static double bandpassHigh;
	private static boolean bandPassReset;
	
	private static boolean excelWriteFlag;
	
	static boolean started;
	static double initTime;
	
	private String[] navMenuTitles;
	private TypedArray navMenuIcons;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drawer);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml
		navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml
		
		set(navMenuTitles,navMenuIcons);
		
		if (savedInstanceState == null) {
			fragment = new PlaceholderFragment();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.content_frame, fragment).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		int row = ROW_DEFAULT;
		
		RecordAudio recorder;
		
		TextView frequencyView;
		ToggleButton toggleButton;
		Button freqMarkButton;
		TextView deltaFreqView;
		TextView numColView;
		TextView currNoiseView;
		TextView currThresholdView;
		LinearLayout graphLayout;
		ListView targetCheckListView;
		public static FreqReadAdapter adapter;
		
		WritableWorkbook workbook;
		WritableSheet currSheet;
		
		GraphViewData[] graphData;
		GraphViewData[] rawData;
		GraphViewSeries series;
		GraphViewSeries rawSeries;
		
		BarGraphView graphView;
		
		private class RecordAudio extends AsyncTask<Void, double[], Void> {
			private AudioRecord audioRecord;
			public int blockSize;
			public int sample_frequency;
			
			public RecordAudio (int freq, int blockSize){
				super();
				this.sample_frequency = freq;
				this.blockSize = blockSize;
				int bufferSize = AudioRecord.getMinBufferSize(sample_frequency,CHANNEL_CONFIG, AUDIO_ENCODING);
				audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sample_frequency,CHANNEL_CONFIG, AUDIO_ENCODING, bufferSize);
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				if(isCancelled()){
					return null;
				}
				
				short[] buffer = new short[blockSize];
				double[] toTransform = new double[blockSize];
				
				try{
					audioRecord.startRecording();
				}
				catch(IllegalStateException e){
					Log.e("Recording failed", e.toString());
				}
				while (started) {
					if(isCancelled()){
						break;
					}
					
					int bufferReadResult = audioRecord.read(buffer, 0, blockSize);
					
					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
					}
					
					transformer.ft(toTransform);
					noiseLevel = averageNoise(toTransform);
					filterData(threshold, toTransform);
					
					if (toggleButton.isChecked()){
						updateWorkbook(toTransform);
					}
					publishProgress(toTransform);
				}
				
				try{
					audioRecord.stop();
				}
				catch(IllegalStateException e){
					Log.e("Stop failed", e.toString());
				}
				
				return null;
			}
			
	        protected void onProgressUpdate(double[]... toTransform) {
	        	if (refreshIndex < REFRESH_RATE){
	        		refreshIndex ++;
	        		return;
	        	}
	        	refreshIndex=0;
	        	
	        	if (rawData != null && graphData.length != blockSize)
	        		graphData = new GraphViewData[blockSize];
	        	if (rawData != null && rawData.length != blockSize)
	        		rawData = new GraphViewData[blockSize];
	        	
	        	double [] filterData = averageFilter(averagePoint, toTransform);
	        	
	        	double max = 0.0;
	        	int index = 0;
	        	for (int i = 0; i < toTransform[0].length; i++) {
	        		double currFreq = (1.0*sample_frequency)/(2.0*blockSize) * i;
	        		if (toTransform[0][i] > max){
	        			max = toTransform[0][i];
	        			index = i;
	        		}
	        		graphData[i] = new GraphViewData(currFreq,filterData[i]);
	        		rawData[i] = new GraphViewData(currFreq,toTransform[0][i]);
	        	}
	        	
	        	//TODO
	        	if (currTargetFreq != null && currTargetFreq.size() != 0){
	        		HashMap<Integer, FreqRead> dict = new HashMap<Integer,FreqRead>();
	        		for (int j=0; j<currTargetFreq.size(); j++){
	        			FreqRead current = currTargetFreq.get(j);
	        			current.flag = 2;
	        			int targetIndex = (int)current.frequencyValue / (int)((1.0*sample_frequency)/(2.0*blockSize));
	        			dict.put(targetIndex, current);
	        		}
	        		ArrayList<Integer> localMax = new ArrayList<Integer>();
	        		double currMax = 0;
	        		int counter = 0;
	        		for (int i=0; i<filterData.length-5; i++){
	        			if (filterData[i] < 0.5 || filterData[i] < noiseLevel){
	        				continue;
	        			}
	        			if (currMax < filterData[i]){
	        				currMax = filterData[i];
	        			} else{
	        				if (counter == 2){
	        					counter = 0;
	        					localMax.add(i);
	        					Log.e("wtf"+ i, "jomg");
	        					while (currMax > filterData[i] || currMax > filterData[i+1] && i<filterData.length-5){
	        						currMax = filterData[i];
	        						i++;
	        					}
	        				}else{
	        					counter ++;
	        				}
	        			}
		        	}
	        		
	        		for (int i=0; i<localMax.size(); i++){
	        			for (int j=-6; j<7; j++){
	        				if (dict.keySet().contains(localMax.get(i)+j)){
	        					if (Math.abs(j) <= 3){
		        					dict.get(localMax.get(i)+j).flag = 0;
		        				}else{
		        					dict.get(localMax.get(i)+j).flag = 1;
		        				}
	        				}
	        			}
	        		}
	        		adapter.notifyDataSetChanged();
	        	}
	        	
	        	
	        	if (series == null)
	        		series = new GraphViewSeries("Filtered",new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, 10),graphData);
	        	if (rawSeries == null && displayRaw)
	        		rawSeries = new GraphViewSeries("Raw",new GraphViewSeries.GraphViewSeriesStyle(Color.RED, 10),rawData);
	        	series.resetData(graphData);
	        	rawSeries.resetData(rawData);
	        	
	        	if (graphNeedReset){
	        		graphView.removeAllSeries();
	        		if (displayRaw)
	        			graphView.addSeries(rawSeries);	
	        		graphView.addSeries(series);
	        		graphView.redrawAll();
		        	graphNeedReset = false;
	        	}
	        	
	        	updateNoiseLevel();
	        	
	        	if (max < threshold) //Ignore low decibel values
	        		return;
	        	
	        	currPeakFreq = (1.0*sample_frequency)/(2.0*blockSize) * index;
	        	
	        	frequencyView.setText(Double.toString(currPeakFreq) + " Hz");
	        }
	        
	        public void retire(){
	        	audioRecord.release();
	        }
		}
		
		public PlaceholderFragment() {
			refreshIndex =0;
			sample_frequency =8000;
			blockSize=256;
			started=false;
			initTime=0;
			noiseLevel = 0.0;
			threshold = 0.0;
			graphRange = 30.0;
			displayRaw = true;
			graphNeedReset = true;
			averagePoint = 3;
			bandpassEnabled = false;
			bandpassLow = 0;
			bandpassHigh = sample_frequency/2;
			bandPassReset = false;
			excelWriteFlag = false;
		}
		
		private WritableSheet createSheet(WritableWorkbook wb, String sheetName, int sheetIndex){
			return wb.createSheet(sheetName, sheetIndex);
		}
		
		private void updateWorkbook(double[]... toTransform){
			if (currSheet == null)
				return;
			
			try {
				Number timeStamp = new Number(0, row, System.currentTimeMillis()/1000.0-initTime);
				currSheet.addCell(timeStamp);
				
				for (int i = 0; i < toTransform[0].length/2; i++) {
	        			Number newCell = new Number(i+2, row, toTransform[0][i]);
	        			currSheet.addCell(newCell);
	        	}
			} catch (RowsExceededException e) {
				Log.e("ROWS_EXCEEDED", e.getMessage());
			} catch (WriteException e) {
				Log.e("WRITE_EXCEPTION", e.getMessage());
			}
			row++;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			transformer = new RealDoubleFFT(blockSize);
			
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			frequencyView = (TextView) rootView.findViewById(R.id.frequencyView);
			started = true;
			
			graphLayout = (LinearLayout) rootView.findViewById(R.id.graphViewLayout);
			graphData = new GraphViewData[blockSize];
			rawData = new GraphViewData[blockSize];
			
			graphView = new BarGraphView(getActivity(), "Frequency Response");
			graphView.setManualYAxisBounds(graphRange, 0);
			graphView.setScrollable(true);
			graphView.getGraphViewStyle().setGridColor(Color.WHITE);
			graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.WHITE);
			graphView.getGraphViewStyle().setVerticalLabelsColor(Color.WHITE);
			graphView.getGraphViewStyle().setLegendWidth(200);
			graphView.setShowLegend(true);
			graphView.setLegendAlign(LegendAlign.TOP);
			graphLayout.addView(graphView);
			
			File savedata = new File(getActivity().getApplicationContext().getFilesDir() + "/SaveData/freqreads.txt");
			currTargetFreq = null;
			
			if (savedata.exists()){
				ObjectInputStream inputStream;
				try {
					inputStream = new ObjectInputStream(new FileInputStream(savedata));
					
					currTargetFreq = (ArrayList<FreqRead>)inputStream.readObject();
				} catch (StreamCorruptedException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else{
				currTargetFreq = new ArrayList<FreqRead>(); //Empty ArrayList
			}
			
			targetCheckListView = (ListView) rootView.findViewById(R.id.frequencyListView);
			adapter = new FreqReadAdapter(currTargetFreq);
			targetCheckListView.setAdapter(adapter);
			
			toggleButton = (ToggleButton) rootView.findViewById(R.id.recordToggleButton);
			toggleButton.setChecked(false);
			toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HHmm");
					Date date = new Date();
					String fileName = "FreqRec_"+dateFormat.format(date)+".xls";
					File docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
					File file = new File( docDir.getAbsolutePath() + "/FR");
					file.mkdir();
					
					if (isChecked){
						WorkbookSettings wbSettings = new WorkbookSettings();
						wbSettings.setUseTemporaryFileDuringWrite(true);
						
						File target = new File(file.getAbsolutePath() + "/"+ fileName);
						
						try{
							workbook = Workbook.createWorkbook(target, wbSettings);
						}
						catch(IOException ex){
							Log.e("IOException",ex.getMessage());
						}
						
						currSheet = createSheet(workbook, "First Sheet", 0);
						
						formatSpreadsheet (fileName);
						excelWriteFlag = true;
						
						initTime = System.currentTimeMillis()/1000.0;
					}
					else{
						try {
							workbook.write();
							workbook.close();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (WriteException e) {
							e.printStackTrace();
						} catch(ActivityNotFoundException e){
							Log.d("JAJA!",getActivity().getFilesDir().getAbsolutePath()+"/"+fileName);
						}
						row = ROW_DEFAULT;
						excelWriteFlag = false;
					}
				}
			});
		
			freqMarkButton = (Button) rootView.findViewById(R.id.frequencyRecordButton);
			freqMarkButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Context c = PlaceholderFragment.this.getActivity();
					AlertDialog.Builder builder = new AlertDialog.Builder(c);
					builder.setTitle("Frequency Name");
					
					final double currFreqVal = currPeakFreq;
					final EditText input = new EditText(c);
					input.setInputType(InputType.TYPE_CLASS_TEXT);
					builder.setView(input);
					
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					    	File savedata = new File(getActivity().getApplicationContext().getFilesDir() + "/SaveData/freqreads.txt");
							ArrayList<FreqRead> freqreads = null;
							
							if (savedata.exists()){
								ObjectInputStream inputStream;
								try {
									inputStream = new ObjectInputStream(new FileInputStream(savedata));
									
									freqreads = (ArrayList<FreqRead>)inputStream.readObject();
								} catch (StreamCorruptedException e) {
									e.printStackTrace();
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}catch (ClassNotFoundException e) {
									e.printStackTrace();
								}
							} else{
								freqreads = new ArrayList<FreqRead>(); //Empty ArrayList
							}
					    	freqreads.add(new FreqRead(currFreqVal, input.getText().toString()));
					    	adapter.getList().add(new FreqRead(currFreqVal, input.getText().toString()));
					    	adapter.notifyDataSetChanged();
					    	
					    	File folder = new File(getActivity().getApplicationContext().getFilesDir() + "/SaveData");
							if (!folder.exists())
								folder.mkdir();
							File newfile = new File(folder.getAbsolutePath() + "/freqreads.txt");
							try {
								if (newfile.exists())
									newfile.delete();
								newfile.createNewFile();
								
								ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(newfile));
								outputStream.writeObject(freqreads);
								
								outputStream.flush();
								outputStream.close();
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
					    }
					});
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        dialog.cancel();
					    }
					});
					builder.show();
				}
			});
			
			deltaFreqView = (TextView) rootView.findViewById(R.id.deltaFrequencyDetailView);
			numColView = (TextView) rootView.findViewById(R.id.outputInfoDetailView);
			currNoiseView = (TextView) rootView.findViewById(R.id.currNoiseLevelDetailView);
			currThresholdView = (TextView) rootView.findViewById(R.id.currThresholdDetailView);
			
			recorder = new RecordAudio(sample_frequency, blockSize);
			recorder.execute();
			return rootView;
		}
		
		@Override
		public void onPause(){
			super.onPause();
			recorder.retire();
			recorder.cancel(true);
			recorder = null;
		}
		
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data){
			if (resultCode != Activity.RESULT_OK) return;
			if (requestCode == REQUEST_SETTINGS){
				int freqRes = (Integer)data.getSerializableExtra(SettingsDialogFragment.EXTRA_FREQUENCY_RESULT);
				int nfftRes = (Integer)data.getSerializableExtra(SettingsDialogFragment.EXTRA_NFFT_RESULT);
				threshold = (Double)data.getSerializableExtra(SettingsDialogFragment.EXTRA_THRESHOLD_RESULT);
				graphRange = (Double)data.getSerializableExtra(SettingsDialogFragment.EXTRA_RANGEMAX_RESULT);
				displayRaw = ((boolean[])data.getSerializableExtra(SettingsDialogFragment.EXTRA_RAWTOGGLE_RESULT))[0];
				
				graphView.setManualYAxisBounds(graphRange, 0);
				graphData = new GraphViewData[nfftRes];
				rawData = new GraphViewData[nfftRes];
				graphNeedReset = true;
				
				if (!isSampleRateValid (freqRes)){
					freqRes = sample_frequency;
					String msg = "INVALID SAMPLE FREQUENCY";
					Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
				}
				if(freqRes == sample_frequency && nfftRes == blockSize){
					return;
				}
				
				if(recorder != null){
					recorder.retire();
					recorder.cancel(true);
				}
				
				transformer = new RealDoubleFFT(nfftRes);
				recorder = new RecordAudio(freqRes, nfftRes);
				recorder.execute();
				
				if (freqRes != sample_frequency){
					sample_frequency = freqRes;
					String msg1 = "Sampling frequency set at ";
					Toast.makeText(getActivity(), msg1+Integer.toString(sample_frequency)+" Hz", Toast.LENGTH_SHORT).show();
					switch(sample_frequency){
					case 8000: REFRESH_RATE = 5; break;
					case 11025: REFRESH_RATE = 7; break;
					case 16000: REFRESH_RATE = 8; break;
					case 22050: REFRESH_RATE = 12; break;
					case 44100: REFRESH_RATE = 20; break;
					}
				}
				if (nfftRes != blockSize){
					blockSize = nfftRes;
					String msg2 = "NFFT set at ";
					Toast.makeText(getActivity(), msg2+Integer.toString(recorder.blockSize), Toast.LENGTH_SHORT).show();
				}
				
				bandpassLow = 0;
				bandpassHigh = sample_frequency/2;
				updateInfoFields();
			} else if (requestCode == REQUEST_FILTERS){
				double lowResult = (Double)data.getSerializableExtra(FiltersDialogFragment.EXTRA_LOWERBOUND_RESULT);
				double highResult = (Double)data.getSerializableExtra(FiltersDialogFragment.EXTRA_UPPERBOUND_RESULT);
				averagePoint = (Integer)data.getSerializableExtra(FiltersDialogFragment.EXTRA_AVGPOINT_RESULT);
				
				if (lowResult < 0){
					Toast.makeText(getActivity(), "ERROR: Bound less than 0", Toast.LENGTH_SHORT).show();
				} else if (lowResult > highResult){
					Toast.makeText(getActivity(), "ERROR: lower bound exceeded upper bound", Toast.LENGTH_SHORT).show();
				} else if (highResult-lowResult < sample_frequency/(double)blockSize){
					Toast.makeText(getActivity(), "ERROR: bandwidth too small", Toast.LENGTH_SHORT).show();
				} else{
					bandpassLow = lowResult;
				}
				
				if (highResult > sample_frequency/2){
					Toast.makeText(getActivity(), "ERROR: Bound over frequency limit", Toast.LENGTH_SHORT).show();
				} else if (lowResult > highResult){
					Toast.makeText(getActivity(), "ERROR: lower bound exceeded upper bound", Toast.LENGTH_SHORT).show();
				} else if (highResult-lowResult < sample_frequency/(double)blockSize){
					Toast.makeText(getActivity(), "ERROR: bandwidth too small", Toast.LENGTH_SHORT).show();
				} else{
					bandpassHigh = highResult;
				}
				
				if (bandpassLow == 0 && bandpassHigh == sample_frequency/2){
					bandPassReset = false;
				} else{
					bandPassReset = true;
				}
			}
		}
		
		private void updateInfoFields(){
			if(deltaFreqView != null)
				deltaFreqView.setText(Double.toString((sample_frequency*1.0)/(blockSize*1.0)));
			if(numColView != null)
				numColView.setText(Double.toString(blockSize));
			if(currThresholdView != null){
				if (Double.toString(threshold).length() < 5)
					currThresholdView.setText(Double.toString(threshold)+ " dB");
				else
					currThresholdView.setText(Double.toString(threshold).substring(0,4) + " dB");
			}
		}
		
		private void updateNoiseLevel(){
			if (currNoiseView != null){
				if (Double.toString(noiseLevel).length() < 5)
					currNoiseView.setText(Double.toString(noiseLevel)+ " dB");
				else
					currNoiseView.setText(Double.toString(noiseLevel).substring(0,4) + " dB");
			}
		}
		
		private void formatSpreadsheet (String fileName){
			Label title = new Label(0,0,fileName+" RECORDED AT " + sample_frequency + " Hz, PROCESSED AT " + "NFFT = " + blockSize);
			Label frequency = new Label(1,0,"FREQUENCY (HZ)");
			Label time = new Label(0,1,"TIME (s)");
			
			try {
				currSheet.addCell(title);
				currSheet.addCell(frequency);
				currSheet.addCell(time);
			} catch (RowsExceededException e) {
				e.printStackTrace();
			} catch (WriteException e) {
				e.printStackTrace();
			}
			
			for (int i=0; i<blockSize; i++){				
				Number currFreq = new Number(i+2,0,(1.0*sample_frequency)/(2.0*blockSize) * i);
				try {
					currSheet.addCell(currFreq);
				} catch (RowsExceededException e) {
					e.printStackTrace();
				} catch (WriteException e) {
					e.printStackTrace();
				}
			}
		}
		
		private boolean isSampleRateValid(int sampleRate){
			int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			return bufferSize > 0;
		}
		
		private double averageNoise(double[]...toTransform){
			int entries = toTransform[0].length;
			double total = 0;
			for(int i=0; i<entries; i++){
				if (toTransform[0][i]>0)
					total += toTransform[0][i];
			}
			return total/(double)entries;
		}
		
		private void filterData (double threshold, double[] ...toTransform){
			for(int i=0; i<toTransform[0].length; i++){
				if (toTransform[0][i] < threshold)
					toTransform[0][i] = 0;
				double currFreq = (1.0*sample_frequency)/(2.0*blockSize) * i;
        		if (bandPassReset){
        			if (currFreq < bandpassLow || currFreq > bandpassHigh){
        				toTransform[0][i]=0;
        			}
        		}
			}
		}
		
		private double[] averageFilter (int point, double [] ...toTransform){
			if(point ==1)
				return toTransform[0];
			
			double[] res = new double[toTransform[0].length];
			
			if(point ==3){
				for(int i=0; i<toTransform[0].length; i++){
					if (i==0){
						res[i] = (toTransform[0][i] + toTransform[0][i+1]*2)/3.0;
					}else if (i== (toTransform[0].length)-1){
						res[i] = (toTransform[0][i] + toTransform[0][i-1]*2)/3.0;
					} 
					else{
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1])/3.0;
					}
				}
			} else if (point == 5){
				for(int i=0; i<toTransform[0].length; i++){
					if (i==0){
						res[i] = (toTransform[0][i] + toTransform[0][i+1]*2 + toTransform[0][i+2]*2)/5.0;
					}else if (i== 1){
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1]
								+toTransform[0][i+2]*2)/5.0;
					} else if (i== toTransform[0].length-1){
						res[i] = (toTransform[0][i] + toTransform[0][i-1]*2 + toTransform[0][i-2]*2)/5.0;
					} else if (i== toTransform[0].length-2){
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1]
								+toTransform[0][i-2]*2)/5.0;
					}
					else{
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1]
								+toTransform[0][i+2]+toTransform[0][i-2])/5.0;
					}
				}
			} else if (point ==7){
				for(int i=0; i<toTransform[0].length; i++){
					if (i==0){
						res[i] = (toTransform[0][i] + toTransform[0][i+1]*2 + toTransform[0][i+2]*2
								+ toTransform[0][i+3]*2)/7.0;
					}else if (i== 1){
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1]
								+toTransform[0][i+2]*2 + toTransform[0][i+3]*2)/7.0;
					}else if (i== 2){
						res[i] = (toTransform[0][i] + toTransform[0][i-2] + toTransform[0][i-1]
								+ toTransform[0][i+1] + toTransform[0][i+2] + toTransform[0][i+3]*2)/7.0;
					}else if (i== toTransform[0].length-1){
						res[i] = (toTransform[0][i] + toTransform[0][i-1]*2 + toTransform[0][i-2]*2
								+toTransform[0][i-3]*2)/7.0;
					} else if (i== toTransform[0].length-2){
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1]
								+toTransform[0][i-2]*2 + toTransform[0][i-3]*2)/7.0;
					} else if (i== toTransform[0].length-3){
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i-2] + toTransform[0][i-3]*2
								+  toTransform[0][i+1] + toTransform[0][i+2])/7.0;
					}
					else{
						res[i] = (toTransform[0][i] + toTransform[0][i-1] + toTransform[0][i+1]
								+toTransform[0][i+2]+toTransform[0][i-2]
								+toTransform[0][i+3]+toTransform[0][i-3])/5.0;
					}
				}
			}
			return res;
		}
		
		public class FreqReadAdapter extends ArrayAdapter<FreqRead>{
			private ArrayList<FreqRead> list;
			
			public FreqReadAdapter(ArrayList<FreqRead> freqreads){
				super(getActivity(),0,freqreads);
				list = freqreads;
			}
			
			@Override
			public void remove(FreqRead item){
				super.remove(item);
				list.remove(item);
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent){
				if(convertView == null || convertView.getTag() != (Integer)position){
					convertView = getActivity().getLayoutInflater().inflate(R.layout.freqread_display_list_layout, parent, false);
					convertView.setTag((Integer) position);
				}
				
				FreqRead fr = getItem(position);
				
				ImageView freq = (ImageView) convertView.findViewById(R.id.searchImageView);
				switch(fr.flag){
				case 0:freq.setBackgroundResource(R.color.green);
					break;
				case 1:freq.setBackgroundResource(R.color.orange);
					break;
				case 2:freq.setBackgroundResource(R.color.red);
					break;
				}
				
				TextView freqname = (TextView) convertView.findViewById(R.id.freqNameTextView);
				freqname.setText(fr.frequencyName);
			
				return convertView;
			}
			
			public ArrayList<FreqRead> getList() {
		        return list;
		    }
		}
	}

	@Override
	public void setupFragment(int position) {
		FragmentManager fm = getSupportFragmentManager();
		DialogFragment dialog = null;
		if (excelWriteFlag)
			return;
		
		switch (position){
		case 0:boolean[] passIn = new boolean[1];
				passIn[0] = displayRaw;
				fm = getSupportFragmentManager();
				dialog = SettingsDialogFragment.newInstance(sample_frequency, blockSize, threshold, graphRange, passIn);
				dialog.setTargetFragment(fragment, REQUEST_SETTINGS);
				dialog.show(fm, "Settings");
				break;
		case 1: fm = getSupportFragmentManager();
				dialog = FiltersDialogFragment.newInstance(bandpassLow, bandpassHigh, averagePoint,
				((int)sample_frequency/2), ((double)sample_frequency/(double)blockSize));
				dialog.setTargetFragment(fragment, REQUEST_FILTERS);
				dialog.show(fm, "Filters");
				break;
		case 2: fm = getSupportFragmentManager();
				dialog = new FreqReadFragment();
				dialog.show(fm, "Filters");
				break;
		}
	}
}
