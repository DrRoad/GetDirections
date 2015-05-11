package edu.rasm.pickel.getdirections;

import android.app.Activity;
import android.app.Dialog;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

    // UI components
    EditText etFrom;
    EditText etTo;
    Button btn;
    ProgressBar pb;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize UI components
        etFrom = (EditText)findViewById(R.id.etFrom);
        etTo = (EditText)findViewById(R.id.etTo);
        btn = (Button)findViewById(R.id.button);
        pb = (ProgressBar)findViewById(R.id.progressBar);
        tvResult = (TextView)findViewById(R.id.tvResult);

        // Getting Google Play availability status
        /* may want to put this where it is used more often*/
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        }else { // Google Play Services are available
        }
    }

        // onClick() method for get directions button
    public void getDirections(View v) {

        btn.setEnabled(false);
        String origin = etFrom.getText().toString();
        String dest = etTo.getText().toString();


        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);



       // new DownloadTask().execute(); //first method i used
    }

    private String getDirectionsUrl(String origin, String dest){

        // Origin of route
        String str_origin = "origin="+origin;

        // Destination of route
        String str_dest = "destination="+dest;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "xml";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    /** A method to download xml data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream(); //// maybe http://androidcookbook.com/Recipe.seam?recipeId=2217

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("My Log Entry", e.toString());
        }finally{
          iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {  // Integer was Void in example

        int progress_status;

        @Override
        protected void onPreExecute() {
            // update the UI immediately after the task is executed
            super.onPreExecute();

            Toast.makeText(MainActivity.this,
                    "Invoke onPreExecute()", Toast.LENGTH_SHORT).show();

            progress_status = 0;
            tvResult.setText("downloading 0%");

        }

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            while(progress_status<50){      // changed from 100 for two tasks

                progress_status += 2;

                publishProgress(progress_status);
                SystemClock.sleep(300);

            }

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            pb.setProgress(values[0]);
            tvResult.setText("downloading " +values[0]+"%");

        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Toast.makeText(MainActivity.this,
                    "Invoke onPostExecute()", Toast.LENGTH_SHORT).show();

            tvResult.setText("download complete");
            btn.setEnabled(true);

            tvResult.setText(result);


            /****************************************************************/
            /******  need to create AsyncTask for xml parsing based on   ****/
            /**  http://wptrafficanalyzer.in/blog/driving-route-from-my-location-to-destination-in-google-maps-android-api-v2/

            // from second example
            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);/**/

        }
    }







    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}













/* private class DownloadTask extends AsyncTask<Void, Integer, Void> {

        int progress_status;

        @Override
        protected void onPreExecute() {
            // update the UI immediately after the task is executed
            super.onPreExecute();

            Toast.makeText(MainActivity.this,
                    "Invoke onPreExecute()", Toast.LENGTH_SHORT).show();

            progress_status = 0;
            tvResult.setText("downloading 0%");

        }

        @Override
        protected Void doInBackground(Void... params) {

            while(progress_status<100){

                progress_status += 2;

                publishProgress(progress_status);
                SystemClock.sleep(300);

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            pb.setProgress(values[0]);
            tvResult.setText("downloading " +values[0]+"%");

        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            Toast.makeText(MainActivity.this,
                    "Invoke onPostExecute()", Toast.LENGTH_SHORT).show();

            tvResult.setText("download complete");
            btn.setEnabled(true);
        }
    }*/