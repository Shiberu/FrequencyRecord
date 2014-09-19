package com.kkago.frequencyrecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SettingsDialogFragment extends DialogFragment {
	public static final String EXTRA_FREQUENCY_VALUE = "com.kkago.android.settingsdialogfragment.frequency";
	public static final String EXTRA_NFFT_VALUE = "com.kkago.android.settingsdialogfragment.nfft";
	public static final String EXTRA_THRESHOLD_VALUE = "com.kkago.android.settingsdialogfragment.threshold";
	public static final String EXTRA_RANGEMAX_VALUE = "com.kkago.android.settingsdialogfragment.rangemax";
	public static final String EXTRA_RAWTOGGLE_VALUE = "com.kkago.android.settingsdialogfragment.rawtoggle";
	
	public static final String EXTRA_FREQUENCY_RESULT = "com.kkago.android.settingsdialogfragment.frequencyResult";
	public static final String EXTRA_NFFT_RESULT = "com.kkago.android.settingsdialogfragment.nfftResult";
	public static final String EXTRA_THRESHOLD_RESULT = "com.kkago.android.settingsdialogfragment.thresholdResult";
	public static final String EXTRA_RANGEMAX_RESULT = "com.kkago.android.settingsdialogfragment.rangemaxResult";
	public static final String EXTRA_RAWTOGGLE_RESULT = "com.kkago.android.settingsdialogfragment.rawtoggleResult";
	
	private final int GRAPH_RANGE_MIN = 20;
	
	private int currentFrequency = 0;
	private int currentNFFT = 0;
	private double currentThreshold =0;
	private double currentRangemax = 30;
	private boolean[] currentRawToggle = new boolean[1];
	
	Spinner freqSpinner;
	Spinner NFFTSpinner;
	
	TextView filterLevelView;
	SeekBar thresholdBar;
	TextView graphRangeView;
	SeekBar graphRangeBar;
	
	Switch rawGraphSwitch;
	
	public SettingsDialogFragment() {
		// TODO Auto-generated constructor stub
	}

	public static SettingsDialogFragment newInstance(int frequency, int nfft, double threshold, double rangemax, boolean[] toggle){
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_FREQUENCY_VALUE, frequency);
		args.putSerializable(EXTRA_NFFT_VALUE, nfft);
		args.putSerializable(EXTRA_THRESHOLD_VALUE, threshold);
		args.putSerializable(EXTRA_RANGEMAX_VALUE, rangemax);
		args.putSerializable(EXTRA_RAWTOGGLE_VALUE, toggle);
		
		SettingsDialogFragment fragment = new SettingsDialogFragment();
		fragment.setArguments(args);
		
		return fragment;
	}
	
	private void sendResult(int resultCode){
		if (getTargetFragment() == null)
			return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_FREQUENCY_RESULT, currentFrequency);
		i.putExtra(EXTRA_NFFT_RESULT, currentNFFT);
		i.putExtra(EXTRA_THRESHOLD_RESULT, currentThreshold);
		i.putExtra(EXTRA_RANGEMAX_RESULT, currentRangemax);
		i.putExtra(EXTRA_RAWTOGGLE_RESULT, currentRawToggle);
		
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		currentFrequency = (Integer)getArguments().getSerializable(EXTRA_FREQUENCY_VALUE);
		currentNFFT = (Integer)getArguments().getSerializable(EXTRA_NFFT_VALUE);
		currentThreshold = (Double)getArguments().getSerializable(EXTRA_THRESHOLD_VALUE);
		currentRangemax = (Double)getArguments().getSerializable(EXTRA_RANGEMAX_VALUE);
		currentRawToggle = (boolean[])getArguments().getSerializable(EXTRA_RAWTOGGLE_VALUE);
		
		View v = getActivity().getLayoutInflater().inflate(R.layout.settings_dialog_layout, null);
		
		freqSpinner = (Spinner) v.findViewById(R.id.frequencySpinner);
		ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.frequency_array, android.R.layout.simple_spinner_item);
		freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		freqSpinner.setAdapter(freqAdapter);
		switch(currentFrequency){
		case 8000: freqSpinner.setSelection(0); break;
		case 11025: freqSpinner.setSelection(1); break;
		case 16000: freqSpinner.setSelection(2); break;
		case 22050: freqSpinner.setSelection(3); break;
		case 44100: freqSpinner.setSelection(4); break;
		}
		
		freqSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				switch(position){
				case 0: currentFrequency = 8000; break;
				case 1: currentFrequency = 11025; break;
				case 2: currentFrequency = 16000; break;
				case 3: currentFrequency = 22050; break;
				case 4: currentFrequency = 44100; break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
		NFFTSpinner = (Spinner) v.findViewById(R.id.NFFTSpinner);
		ArrayAdapter<CharSequence> NFFTAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.NFFT_array, android.R.layout.simple_spinner_item);
		NFFTAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		NFFTSpinner.setAdapter(NFFTAdapter);
		switch(currentNFFT){
		case 128:NFFTSpinner.setSelection(0); break;
		case 256:NFFTSpinner.setSelection(1); break;
		case 512:NFFTSpinner.setSelection(2); break;
		}
		NFFTSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				switch(position){
				case 0: currentNFFT = 128; break;
				case 1: currentNFFT = 256; break;
				case 2: currentNFFT = 512; break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
		filterLevelView = (TextView) v.findViewById(R.id.filterLevelView);
		filterLevelView.setText(Double.toString(currentThreshold) + " dB");
		
		thresholdBar = (SeekBar) v.findViewById(R.id.noiseFilterSeekbar);
		thresholdBar.setProgress((int)currentThreshold);
		
		thresholdBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				currentThreshold = progress;
				if (filterLevelView != null)
					filterLevelView.setText(Double.toString(currentThreshold) + " dB");
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
		});
		
		graphRangeView = (TextView) v.findViewById(R.id.graphMaxValueView);
		graphRangeView.setText("0 dB to "+Double.toString(currentRangemax) + " dB");
		
		graphRangeBar = (SeekBar) v.findViewById(R.id.graphMaxSeekBar);
		graphRangeBar.setProgress((int)currentRangemax - GRAPH_RANGE_MIN);
		graphRangeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				currentRangemax = progress + GRAPH_RANGE_MIN;
				if (graphRangeView != null)
					graphRangeView.setText("0 dB to "+Double.toString(currentRangemax) + " dB");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
		
		rawGraphSwitch = (Switch) v.findViewById(R.id.rawGraphToggleSwitch);
		rawGraphSwitch.setChecked(currentRawToggle[0]);
		rawGraphSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				currentRawToggle[0]=isChecked;
			}
		});
		
		return new AlertDialog.Builder(getActivity()).setView(v).setTitle("Settings")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						sendResult(Activity.RESULT_OK);
					}
				})
				.create();
	}
}
