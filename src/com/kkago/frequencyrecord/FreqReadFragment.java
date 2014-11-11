package com.kkago.frequencyrecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FreqReadFragment extends DialogFragment {
	private ArrayList<FreqRead> list;
	private ListView listView;
	
	public FreqReadFragment() {
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState){
		View v = getActivity().getLayoutInflater().inflate(R.layout.freqread_dialog_fragment, null);
		
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
		
		
		listView = (ListView) v.findViewById(R.id.freqReadListViewDialog);
		final FreqReadAdapter adapter = new FreqReadAdapter(freqreads);
		listView.setAdapter(adapter);
		
		SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(
			listView, new SwipeDismissListViewTouchListener.DismissCallbacks() {
				@Override
				public boolean canDismiss(int position) {
					return true;
				}
				
				@Override
				public void onDismiss(ListView listView, int[] reverseSortedPositions) {
					for (int position : reverseSortedPositions) {
						adapter.remove(adapter.getItem(position));
						MainActivity.PlaceholderFragment.adapter.remove(MainActivity.PlaceholderFragment.adapter.getItem(position));
					}
					adapter.notifyDataSetChanged();
					MainActivity.PlaceholderFragment.adapter.notifyDataSetChanged();
					if (adapter.getCount() == 0){
						export(adapter);
						dismiss();
					}
				}});
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
		
		
		return new AlertDialog.Builder(getActivity()).setPositiveButton("OK",  new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				export(adapter);
			}
		}).setView(v).create();
	}
	
	private void export(FreqReadAdapter adapter){
		ArrayList<FreqRead> list = adapter.getList();
		
		File folder = new File(getActivity().getApplicationContext().getFilesDir() + "/SaveData");
		if (!folder.exists())
			folder.mkdir();
		File newfile = new File(folder.getAbsolutePath() + "/freqreads.txt");
		try {
			if (newfile.exists())
				newfile.delete();
			newfile.createNewFile();
			
			ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(newfile));
			outputStream.writeObject(list);
			
			outputStream.flush();
			outputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class FreqReadAdapter extends ArrayAdapter<FreqRead>{
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
				convertView = getActivity().getLayoutInflater().inflate(R.layout.freqread_list_layout, parent, false);
				convertView.setTag((Integer) position);
			}
			
			FreqRead fr = getItem(position);
			
			TextView freq = (TextView) convertView.findViewById(R.id.freqTextView);
			freq.setText(Double.toString(fr.frequencyValue) + " Hz");
			
			TextView freqname = (TextView) convertView.findViewById(R.id.freqNameTextView);
			freqname.setText(fr.frequencyName);
		
			return convertView;
		}
		
		public ArrayList<FreqRead> getList() {
	        return list;
	    }
	}
}
