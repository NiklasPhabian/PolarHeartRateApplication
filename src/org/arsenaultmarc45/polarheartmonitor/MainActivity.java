package org.arsenaultmarc45.polarheartmonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import com.androidplot.Plot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.SimpleXYSeries;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


/**
 * This program connect to a bluetooth polar heart rate monitor and display data
 * @author Marco
 *
 */
public class MainActivity extends Activity  implements OnItemSelectedListener, Observer {

	boolean searchBt = true;
	ConnectThread reader;
	BluetoothAdapter mBluetoothAdapter;
	Set<BluetoothDevice> pairedDevices;
	boolean menuBool = false;
	int i =0;
	private XYPlot plot;
	SimpleXYSeries series1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		}

	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		reader.cancel();
	}

	public void onStart(){
		super.onStart();
		DataHandler.getInstance().addObserver(this);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    
		if (!mBluetoothAdapter.isEnabled()) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.bluetooth)
			.setMessage(R.string.bluetoothOff)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 

					mBluetoothAdapter.enable();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					listBT();
				}
			})
			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 
					searchBt = false;
				}
			})
			.show();

		}
		else{
			listBT();
		}
		
		
		
		
		// initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.dynamicPlot);
 
        // Create a couple arrays of y-values to plot:
        Number[] series1Numbers = {};
 
        // Turn the above arrays into XYSeries':
        series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Heart Rate");                             // Set the display title of the series
 
      
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
 

 
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
				

	}
	
	public void listBT(){
		if(searchBt){
			//Discover bluetooth devices
			List<String> list = new ArrayList<String>();
			list.add("");
			pairedDevices = mBluetoothAdapter.getBondedDevices();
			// If there are paired devices
			if (pairedDevices.size() > 0) {
				// Loop through paired devices
				for (BluetoothDevice device : pairedDevices) {
					// Add the name and address to an array adapter to show in a ListView
					list.add(device.getName() + "\n" + device.getAddress());
				}
			}


			//Populate drop down
			Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner1.setOnItemSelectedListener(this);
			spinner1.setAdapter(dataAdapter);
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			reader.cancel();
			System.out.println("menu pes�");
			menuBool=false;
			return true;
		}
		else if (id==R.id.about){
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main,
					container, false);
			return rootView;
		}
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		if(arg2!=0){
			reader = new ConnectThread((BluetoothDevice) pairedDevices.toArray()[arg2-1], this);
			reader.start();
			menuBool=true;

		}

	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		menu.findItem(R.id.action_settings).setEnabled(menuBool);
		return true;
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {


	}

	public void connectionError(){
		menuBool=false;
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getBaseContext(),getString(R.string.couldnotconnect),Toast.LENGTH_SHORT).show();
				TextView rpm = (TextView) findViewById(R.id.rpm);
				rpm.setText("0 RPM");
			}
		});
	}
	
	public void receiveData(){
		
		runOnUiThread(new Runnable() {
			public void run() {
				TextView rpm = (TextView) findViewById(R.id.rpm);
				rpm.setText(DataHandler.getInstance().getLastValue()+" RPM");
				
				series1.addLast(0, DataHandler.getInstance().getLastValue());				
				plot.redraw();
				
				TextView min = (TextView) findViewById(R.id.min);
				min.setText("Min "+DataHandler.getInstance().getMin()+" RPM");
				
				TextView max = (TextView) findViewById(R.id.max);
				max.setText("Max "+DataHandler.getInstance().getMax()+" RPM");
				
			}
		});
	}

	@Override
	public void update(Observable observable, Object data) {

		receiveData();
		
	}
	
	    
}
