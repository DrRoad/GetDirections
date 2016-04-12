/**
 *	MainActivity.java
 *	Another epic battle fought by Ryan Pickelsimer
 *	May 11, 2015
 *
 *	This activity takes user input locations and displays
 *	driving directions from an origin to a destination.
 *	AsyncTasks are used for background processing and a
 *	progress bar shows during processing.
 **/

package edu.rasm.pickel.getdirections;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;


/** Activity gets 2 locations from user and displays driving directions **/
public class MainActivity extends ActionBarActivity {

    final static int SLEEP_DURATION = 100;

    // UI components
    EditText etFrom;
    EditText etTo;
    Button btn;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize UI components
        etFrom = (EditText)findViewById(R.id.etFrom);
        etTo = (EditText)findViewById(R.id.etTo);
        btn = (Button)findViewById(R.id.button);
        tvResult = (TextView)findViewById(R.id.tvResult);
    }

    /** onClick() method for get directions button **/
    public void getDirections(View v) {

        // disable button while background processing
        // enabled again in XMLTask
        btn.setEnabled(false);

        // User input locations
        String origin = etFrom.getText().toString();
        String dest = etTo.getText().toString();

        // use input to get a url string for directions
        String url = getDirectionsUrl(origin, dest);

        // AsyncTask for downloading content
        DownloadTask downloadTask = new DownloadTask();

        // Start downloading xml data from Google Directions API
        downloadTask.execute(url);
    }


    /**********************************************************************************/
    /****************************     Downloading data     ****************************/
    /**********************************************************************************/


    /** Returns an url string with directions **/
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
            // Get URL
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            // Data to string
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine()) != null){
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

        // Return xml string of directions - start tag <DirectionsResponse>
        return data;
    }

    /** AsyncTask for downloading xml data. **/
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        // UI elements
        int progress_status;
        ProgressBar pb;

        @Override
        protected void onPreExecute() {
            // update the UI immediately after the task is executed
            super.onPreExecute();

            /** Getting Google Play availability status  **/
            int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
            if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

                int requestCode = 10;
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, MainActivity.this, requestCode);
                dialog.show();

            }
            else { /** Google Play Services are available **/

                pb = (ProgressBar)findViewById(R.id.progressBar);
                progress_status = 0;

                // show the ProgressBar
                pb.setVisibility(View.VISIBLE);

                // update progress
                pb.setProgress(progress_status);
                tvResult.setText("downloading 0%");
            }
        }

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // Increment progress up to half of total background processing
            while(progress_status < 50){

                progress_status += 2;

                // send progress to UI thread
                publishProgress(progress_status);

                // sleep to show progress bar
                SystemClock.sleep(SLEEP_DURATION);
            }

            String s  = null;
            try {

                // Download directions with url
                s = downloadUrl(url[0]);
            }
            catch (IOException e) {
                Log.d("Background Task",e.toString());
            }

            // Return a string with xml data
            return s;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            // Show progress
            pb.setProgress(values[0]);
            tvResult.setText("downloading " + values[0] + "%");
        }

        // Executes in UI thread, after the execution of doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Close the progress bar
            pb.setVisibility(View.GONE);

            // Start background processing for xml parsing
            XMLTask xmlTask = new XMLTask();
            xmlTask.execute(result);
        }
    }


    /********************************************************************************/
    /****************************     XML Processing     ****************************/
    /********************************************************************************/


    /** Parse xml string to a displayable string **/
    private String parseXML(String is) throws
            XmlPullParserException, IOException, URISyntaxException {

        // xml classes used to read xml file
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();

        // Set xml string to input
        parser.setInput(new StringReader(is));

        // Start and end tags and end document
        int eventType = parser.getEventType();

        // current tag
        String currentTag;

        // current value of tag/element
        String currentElement;

        int counter = 0;
        StringBuilder sb = new StringBuilder();

        // parse the entire xml file until done
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // look for start tags
            if (eventType == XmlPullParser.START_TAG) {
                // get the name of the start tag
                currentTag = parser.getName();

                if (currentTag.equals("step")) {
                    sb.append("Step " + ++counter + ":\n");
                    sb.append("-----------------------------------------\n");
                }
                // Driving directions
                else if (currentTag.equals("html_instructions")) {
                    currentElement = parser.nextText();

                    // Replace html bold tags
                    currentElement = currentElement.replace("<b>", "");
                    currentElement = currentElement.replace("</b>", "");

                    // Get rid of div tags
                    String s;
                    while (currentElement.contains("<")) {
                        if (currentElement.indexOf(">") == currentElement.lastIndexOf(">")){
                            s = "";
                        } else {
                            s = " ";
                        }
                        currentElement = currentElement.replace(currentElement.substring(currentElement.indexOf("<"),
                                currentElement.indexOf(">") + 1), s);
                    }

                    // Append html parsed string to directions string
                    currentElement += ".\n\n";
                    sb.append(currentElement);
                }
            }

            // If the end of the string is reached
            else if (eventType == XmlPullParser.END_TAG) {
                currentTag = parser.getName();

                if (currentTag.equals("DirectionsResponse")) {

                    // return displayable string
                    sb.append("------------------------------------\n\n");
                    return sb.toString();
                }
            }

            // get next tag
            eventType = parser.next();
        }

        // return null if something went wrong
        return null;
    }

    /** AsyncTask for xml parsing **/
    private class XMLTask extends AsyncTask<String, Integer, String> {

        int progress_status;
        ProgressBar pb;

        @Override
        protected void onPreExecute() {
            // update the UI immediately after the task is executed
            super.onPreExecute();

            pb = (ProgressBar)findViewById(R.id.progressBar);
            progress_status = 50;

            // show the ProgressBar
            pb.setVisibility(View.VISIBLE);

            // set progress
            pb.setProgress(progress_status);
        }

        // Parse xml string in a separate thread
        @Override
        protected String doInBackground(String... is) {

            // Progress for second task
            while(progress_status < 100){

                progress_status += 2;
                publishProgress(progress_status);
                SystemClock.sleep(SLEEP_DURATION);
            }

            // Make sure string is not empty
            if (is[0] != null) {

                try {
                    // return a displayable string
                    return parseXML(is[0]);

                } catch (Exception e) {
                    Log.e("XMLExample", e.getMessage());
                }
            }

            // Return null if something went wring
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            pb.setProgress(values[0]);
            tvResult.setText("downloading " + values[0] + "%");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            tvResult.setText("download complete");
            btn.setEnabled(true);

            // Id string is not empty, display result directions
            if (result != null) {
                tvResult.setText(result);
            }

            // Close the progress bar
            pb.setVisibility(View.GONE);
        }
    }


    /**************  Inherited methods not used  ***************/

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

// AsyncTask Template
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