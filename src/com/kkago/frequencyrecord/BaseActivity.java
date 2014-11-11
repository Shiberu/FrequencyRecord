package com.kkago.frequencyrecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

public abstract class BaseActivity extends ActionBarActivity {
	 protected DrawerLayout mDrawerLayout;
	 protected ListView mDrawerList;
	 protected ActionBarDrawerToggle mDrawerToggle;
	 protected RelativeLayout _completeLayout, _activityLayout;
	 // nav drawer title
	 protected CharSequence mDrawerTitle;

	  // used to store app title
	 protected CharSequence mTitle;

	  protected ArrayList<NavDrawerItem> navDrawerItems;
	 protected NavDrawerListAdapter adapter;

	 
	  @Override
	 protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.drawer);
	  // if (savedInstanceState == null) {
	  // // on first time display view for first nav item
	  // // displayView(0);
	  // }
	 }

	  @Override
	  public boolean onOptionsItemSelected(MenuItem item){
		  // Pass the event to ActionBarDrawerToggle, if it returns
		  // true, then it has handled the app icon touch event
		  if (mDrawerToggle.onOptionsItemSelected(item)) {
			  return true;
		  }
		  // Handle your other action bar items...
		  return super.onOptionsItemSelected(item);
	  }
	  
	  public void set(String[] navMenuTitles,TypedArray navMenuIcons) {
	  mTitle = mDrawerTitle = getTitle();

	   mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	  mDrawerList = (ListView) findViewById(R.id.left_drawer);

	   navDrawerItems = new ArrayList<NavDrawerItem>();

	   // adding nav drawer items
	  if(navMenuIcons==null){
	  for(int i=0;i<navMenuTitles.length;i++){
	   navDrawerItems.add(new NavDrawerItem(navMenuTitles[i])); 
	  }}else{
	   for(int i=0;i<navMenuTitles.length;i++){
	    navDrawerItems.add(new NavDrawerItem(navMenuTitles[i],navMenuIcons.getResourceId(i, -1))); 
	   }
	  }

	   mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

	   // setting the nav drawer list adapter
	  adapter = new NavDrawerListAdapter(getApplicationContext(),
	    navDrawerItems);
	  mDrawerList.setAdapter(adapter);

	   // enabling action bar app icon and behaving it as toggle button
	  getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	  getSupportActionBar().setHomeButtonEnabled(true);
	  // getSupportActionBar().setIcon(R.drawable.ic_drawer);

	   mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
	    R.drawable.ic_launcher, // nav menu toggle icon //TODO
	    R.string.app_name, // nav drawer open - description for
	         // accessibility
	    R.string.app_name // nav drawer close - description for
	         // accessibility
	  ) {
	   public void onDrawerClosed(View view) {
	    // calling onPrepareOptionsMenu() to show action bar icons
	    supportInvalidateOptionsMenu();
	   }

	    public void onDrawerOpened(View drawerView) {
	    // calling onPrepareOptionsMenu() to hide action bar icons
	    supportInvalidateOptionsMenu();
	   }
	  };
	  mDrawerToggle.setDrawerIndicatorEnabled(true);
	  mDrawerLayout.setDrawerListener(mDrawerToggle);

	 }
	  
	  class DrawerItemClickListener implements ListView.OnItemClickListener {
		    @Override
		    public void onItemClick(AdapterView parent, View view, int position, long id) {
		        selectItem(position);
		    }
		}

	  
	  
		/** Swaps fragments in the main content view */
		private void selectItem(int position) {
		    // Create a new fragment and specify the planet to show based on position
			
			switch (position){
			case 0: setupFragment(position);
					break;
			case 1: setupFragment(position);
					break;
			case 2: setupFragment(position);
					break;
			}

		    // Highlight the selected item, update the title, and close the drawer
		    mDrawerList.setItemChecked(position, true);
		    //setTitle(mPlanetTitles[position]);
		    mDrawerLayout.closeDrawer(mDrawerList);
		}
		
		public abstract void setupFragment(int position);

		@Override
		public void setTitle(CharSequence title) {
		    mTitle = title;
		    getActionBar().setTitle(mTitle);
		}
		
}

