package com.kkago.frequencyrecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FiltersDialogFragment extends DialogFragment {

	public static final String EXTRA_LOWERBOUND_VALUE = "com.kkago.android.filtersdialogfragment.lower";
	public static final String EXTRA_UPPERBOUND_VALUE = "com.kkago.android.filtersdialogfragment.upper";
	public static final String EXTRA_AVGPOINT_VALUE = "com.kkago.android.filtersdialogfragment.avgpoint";
	public static final String EXTRA_UPPER_LIMIT = "com.kkago.android.filtersdialogfragment.upperlimit";
	public static final String EXTRA_DF = "com.kkago.android.filtersdialogfragment.df";
	
	public static final String EXTRA_LOWERBOUND_RESULT = "com.kkago.android.filtersdialogfragment.lowerResult";
	public static final String EXTRA_UPPERBOUND_RESULT = "com.kkago.android.filtersdialogfragment.upperResult";
	public static final String EXTRA_AVGPOINT_RESULT = "com.kkago.android.filtersdialogfragment.avgpointResult";
	
	public double lowerBound;
	public double upperBound;
	public int upperLimit;
	public double df;
	public int avgPoint;
	
	EditText lowerBoundEdit;
	EditText upperBoundEdit;
	
	Spinner avgPointSpinner;
	
	public FiltersDialogFragment() {
		// TODO Auto-generated constructor stub
	}

	public static FiltersDialogFragment newInstance(double lower, double upper, int avgpoint, int limit, double df){
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_LOWERBOUND_VALUE, lower);
		args.putSerializable(EXTRA_UPPERBOUND_VALUE, upper);
		args.putSerializable(EXTRA_AVGPOINT_VALUE, avgpoint);
		args.putSerializable(EXTRA_UPPER_LIMIT, limit);
		args.putSerializable(EXTRA_DF, df);
		
		FiltersDialogFragment fragment = new FiltersDialogFragment();
		fragment.setArguments(args);
		
		return fragment;
	}
	
	private void sendResult(int resultCode){
		if (getTargetFragment() == null)
			return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_LOWERBOUND_RESULT, lowerBound);
		i.putExtra(EXTRA_UPPERBOUND_RESULT, upperBound);
		i.putExtra(EXTRA_AVGPOINT_RESULT, avgPoint);
		
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		lowerBound = (Double)getArguments().getSerializable(EXTRA_LOWERBOUND_VALUE);
		upperBound = (Double)getArguments().getSerializable(EXTRA_UPPERBOUND_VALUE);
		avgPoint = (Integer)getArguments().getSerializable(EXTRA_AVGPOINT_VALUE);
		upperLimit = (Integer)getArguments().getSerializable(EXTRA_UPPER_LIMIT);
		df = (Double)getArguments().getSerializable(EXTRA_DF);
		
		View v = getActivity().getLayoutInflater().inflate(R.layout.filters_dialog_layout, null);
		
		lowerBoundEdit = (EditText)v.findViewById(R.id.lowerBoundField);
		lowerBoundEdit.setText(Double.toString(lowerBound));
		lowerBoundEdit.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	double input = 0;
	        	try{
		        	input = Double.parseDouble(s.toString());
	        	}catch(NumberFormatException e){
	        		lowerBoundEdit.setText(Double.toString(lowerBound));
	        		Toast.makeText(getActivity(), "INVALID INPUT", Toast.LENGTH_SHORT).show();
	        		return;
	        	}
	        	lowerBound = input;
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    }); 
		
		upperBoundEdit = (EditText)v.findViewById(R.id.upperBoundField);
		upperBoundEdit.setText(Double.toString(upperBound));
		upperBoundEdit.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	double input = 0;
	        	
	        	try{
		        	input = Double.parseDouble(s.toString());
	        	}catch(NumberFormatException e){
	        		upperBoundEdit.setText(Double.toString(upperBound));
	        		Toast.makeText(getActivity(), "INVALID INPUT", Toast.LENGTH_SHORT).show();
	        		return;
	        	}
	        	upperBound = input;
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    }); 
		
		avgPointSpinner = (Spinner) v.findViewById(R.id.averagePointSpinner);
		ArrayAdapter<CharSequence> AvgAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.point_average_array, android.R.layout.simple_spinner_item);
		AvgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		avgPointSpinner.setAdapter(AvgAdapter);
		switch(avgPoint){
		case 1:avgPointSpinner.setSelection(0); break;
		case 3:avgPointSpinner.setSelection(1); break;
		case 5:avgPointSpinner.setSelection(2); break;
		case 7:avgPointSpinner.setSelection(3); break;
		}
		avgPointSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				switch(position){
				case 0: avgPoint = 1; break;
				case 1: avgPoint = 3; break;
				case 2: avgPoint = 5; break;
				case 3: avgPoint = 7; break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
		return new AlertDialog.Builder(getActivity()).setView(v).setTitle("Filters")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						sendResult(Activity.RESULT_OK);
					}
				})
				.create();
	}

}
