package com.goeuro.travel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.goeuro.travel.MyLocation.LocationResult;

public class HomeActivity extends Activity {

	private EditText dateEditText;
	private static final String LOG_TAG = "GoEuro";
	private static final String PLACES_API_BASE = "http://pre.dev.goeuro.de:12345/api/v1/suggest/position/en/name/potsdam";
	private HashMap<String, Double> resultMap;

	private double myLat = -1;
	private double myLong = -1;
	
	private AutoCompleteTextView sourceView;
	private AutoCompleteTextView destView;
	private Button searchBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_layout);

		// init
		dateEditText = (EditText) findViewById(R.id.id_date);
		

		// location result
		LocationResult locationResult = new LocationResult() {
			@Override
			public void gotLocation(Location location) {
				myLat = location.getLatitude();
				myLong = location.getLongitude();
			}
		};
		MyLocation myLocation = new MyLocation();
		myLocation.getLocation(this.getApplicationContext(), locationResult);

		sourceView = (AutoCompleteTextView) findViewById(R.id.source_txtview);
		destView = (AutoCompleteTextView) findViewById(R.id.dest_txtview);
	
		
		sourceView.setAdapter(new LocationListAdapter(this,
				android.R.layout.simple_list_item_1,destView));

		
		destView.setAdapter(new LocationListAdapter(this,
				android.R.layout.simple_list_item_1,sourceView));

		searchBtn = (Button) findViewById(R.id.id_srchbtn);
		searchBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Toast.makeText(getApplicationContext(),
						"Search Functionality Not Implemented", 2000).show();
			}
		});
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	//date picker dialog
	public void showDatePickerDialog(View v) {
		DialogFragment newFragment = new DatePickerFragment();
		newFragment.show(getFragmentManager(), "datePicker");
	}

	
	//date picker fragment
	public class DatePickerFragment extends DialogFragment implements
			DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
			DatePicker dp = dialog.getDatePicker();
			dp.setMinDate(System.currentTimeMillis() - 1000);
			return dialog;
		}

		
		public void onDateSet(DatePicker view, int year, int month, int day) {

			dateEditText.setText(day + "-" + (month + 1) + "-" + year);
			
			
			if(!sourceView.getText().toString().trim().equals("") && !destView.getText().toString().trim().equals(""))
				searchBtn.setEnabled(true);
		}

		/*
		 * Support function could be used if we need to represent the month in a
		 * different format. public String getMonthForInt(int num) { String
		 * month = "wrong"; DateFormatSymbols dfs = new DateFormatSymbols();
		 * String[] months = dfs.getMonths(); if (num >= 0 && num <= 11 ) {
		 * month = months[num]; } return month; }
		 */
	}

	
	
	
	
	
	
	//auto complete funcitonality
	private ArrayList<String> autocomplete(String input,String ignore) {
		ArrayList<String> resultList = null;

		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		try {
			StringBuilder sb = new StringBuilder(PLACES_API_BASE);
			// sb.append("?sensor=false&key=" + API_KEY);
			// sb.append("&components=country:uk");
			// sb.append("&input=" + URLEncoder.encode(input, "utf8"));
			//
			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Places API URL", e);
			return resultList;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Places API", e);
			return resultList;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			// Create a JSON object hierarchy from the results
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray predsJsonArray = jsonObj.getJSONArray("results");

			Double sourceLat = new Double(myLat);
			Double sourceLong = new Double(myLong);

			// Extract the Place descriptions from the results
			resultList = new ArrayList<String>(predsJsonArray.length());
			resultMap = new HashMap<String, Double>();
			
			for (int i = 0; i < predsJsonArray.length(); i++) {

				
				String name = predsJsonArray.getJSONObject(i).getString("name");
				
				
				//check the prefix
				if(!name.toLowerCase().startsWith(input.toLowerCase()))
					continue;
				
				//check if the source or dest is already added in the other field
				if(ignore!=null && name.equals(ignore))
					continue;
				
				Double destLat = (Double) predsJsonArray.getJSONObject(i)
						.getJSONObject("geo_position").getDouble("latitude");
				Double destLong = (Double) predsJsonArray.getJSONObject(i)
						.getJSONObject("geo_position").getDouble("longitude");

				Double dist = distance(sourceLat, sourceLong, destLat, destLong);

				resultMap.put(name, dist);

				ValueComparator bvc = new ValueComparator(resultMap);
				TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(
						bvc);
				sorted_map.putAll(resultMap);
				resultList = new ArrayList(sorted_map.keySet());

			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}

		return resultList;
	}

	//value comparater used to sort the map
	private class ValueComparator implements Comparator<String> {

		Map<String, Double> base;

		public ValueComparator(Map<String, Double> base) {
			this.base = base;
		}

		public int compare(String a, String b) {
			if (base.get(a) <= base.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	}

	//distance between two coordinates
	public double distance(double fromLat, double fromLon, double toLat,
			double toLon) {

		double theta = fromLon - toLon;
		double dist = Math.sin(deg2rad(fromLat)) * Math.sin(deg2rad(toLat))
				+ Math.cos(deg2rad(fromLat)) * Math.cos(deg2rad(toLat))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		// if (unit == "K") {
		// dist = dist * 1.609344;
		// } else if (unit == "N") {
		dist = dist * 0.8684;
		// }
		return (dist);
	}

	//This function converts decimal degrees to radians
	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	//This function converts radians to decimal degrees
	private double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}

	
	//list adapter for location list
	public class LocationListAdapter extends ArrayAdapter<String> implements
			Filterable {

		private ArrayList<String> resultList;
		private AutoCompleteTextView otherView;
		
		public LocationListAdapter(Context context, int textViewResourceId,AutoCompleteTextView view) {
			super(context, textViewResourceId);
			otherView = view;
		}

		@Override
		public int getCount() {
			return resultList.size();
		}

		@Override
		public String getItem(int index) {
			return resultList.get(index);
		}

		@Override
		public android.widget.Filter getFilter() {
			Filter filter = new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();
					if (constraint != null) {
						// Retrieve the autocomplete results.
						
						resultList = autocomplete(constraint.toString(),otherView.getText().toString());
						
						// Assign the data to the FilterResults
						filterResults.values = resultList;
						filterResults.count = resultList.size();
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					if (results != null && results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
					
					
					if(!sourceView.getText().toString().trim().equals("") && !destView.getText().toString().trim().equals("") && !dateEditText.getText().toString().trim().equals(""))
						searchBtn.setEnabled(true);
					else
						searchBtn.setEnabled(false);
				}

			};
			return filter;
		}
	}
}
