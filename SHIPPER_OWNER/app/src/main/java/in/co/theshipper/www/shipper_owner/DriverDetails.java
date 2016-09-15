package in.co.theshipper.www.shipper_owner;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.android.gms.internal.zzir.runOnUiThread;

/**
 * Created by Admin on 9/11/2016.
 */
public class DriverDetails extends android.support.v4.app.Fragment implements View.OnClickListener{

    protected RequestQueue requestQueue;
    private String TAG = DriverDetails.class.getName();
    protected View view;
    private LinearLayout map_view,map;
    private Button callButton;
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap=null;
    private boolean stopTimer = false;
    private Timer timer;
    private Location location;
    private String received_driver_current_lat,received_driver_current_lng;
    private String driver_token="";
    private TextView location_datetime;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private Dialog dialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onAttach Called");
    }

    public DriverDetails(){
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onCreateView Called");
        if((getActivity().getIntent()!=null)&&(getActivity().getIntent().getExtras()!=null)) {
            Fn.logD("getActivity().getIntent().getExtras()","getActivity().getIntent().getExtras()");
            Bundle bundle = getActivity().getIntent().getExtras();
            driver_token = Fn.getValueFromBundle(bundle,"driver_token");
            Fn.logD("received_driver_token_intent",driver_token);
            getActivity().getIntent().setData(null);
            getActivity().setIntent(null);
        }else if(this.getArguments()!=null){
            Fn.logD("getArguments","getArguments");
            Bundle bundle = this.getArguments();
            driver_token = Fn.getValueFromBundle(bundle,"driver_token");
            Fn.logD("received_driver_token_argument",driver_token);

        }
        Fn.logD("bundle_driver_token", driver_token);
        Fn.SystemPrintLn(driver_token);
//        Fn.logD("booking_status_url",booking_status_url);
        view = inflater.inflate(R.layout.fragment_driver_details, container, false);
        map_view = (LinearLayout) view.findViewById(R.id.map_view);
        map = (LinearLayout) view.findViewById(R.id.map);
        location_datetime = (TextView) view.findViewById(R.id.location_datetime);
        callButton = (Button) view.findViewById(R.id.driver_mobile_no);
        callButton.setOnClickListener(this);
        dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog);
        dialog.setCancelable(true);
        Fn.logD("Map Added", "Map Added");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onViewCreated Called");
        super.onViewCreated(view, savedInstanceState);
        mMapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.map, mMapFragment, "MAP_FRAGMENT").commit();
        String driver_details_url = Constants.Config.ROOT_PATH+"my_driver_details";
        Fn.logD("driver_details_url", driver_details_url);
        HashMap<String,String> hashMap= new HashMap<String,String>();
        String driver_token = Fn.getPreference(getActivity(),"driver_token");
        hashMap.put("driver_token",driver_token);
        sendVolleyRequest(driver_details_url, Fn.checkParams(hashMap), "my_driver_details");
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onActivityCreated Called");
    }
    public void sendVolleyRequest(String URL, final HashMap<String,String> hMap,final String method){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Fn.logD("onResponse_booking_status", String.valueOf(response));
                if(method.equals("booking_status")) {
                    Fn.logD("booking_status","booking_status");
                    bookingStatusSuccess(response);
                }else if (method.equals("vehicle_location")) {
                    Fn.logD("vehicle_location","vehicle_location");
                    vehicleLocationSuccess(response);
                }else if(method.equals("draw_path")){
                    Fn.logD("method","method");
                    drawPath(response);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Fn.logD("onErrorResponse", String.valueOf(error));
                Fn.ToastShort(getActivity(), Constants.Message.NETWORK_ERROR);
            }
        }){
            @Override
            protected HashMap<String,String> getParams(){
                return hMap;
            }
        };
        stringRequest.setTag(TAG);
        Fn.addToRequestQue(requestQueue, stringRequest, getActivity());
    }
    protected void bookingStatusSuccess(String response){
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "bookingStatusSuccess Called");
        if(!Fn.CheckJsonError(response)){
//            Fn.logD("bookingStatusSuccess", "bookingStatusSuccess Called");
            Fn.logD("received_json", response);
            JSONObject jsonObject;
            JSONArray jsonArray;
            try {
                jsonObject = new JSONObject(response);
                String errFlag = jsonObject.getString("errFlag");
                if(errFlag.equals("1")){
                    Fn.logD("toastNotdone","toastNotdone");
                }
                else if(errFlag.equals("0"))
                {
                    TextView textView=new TextView(getActivity());
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView.setLayoutParams(layoutParams);
                    textView.setId(R.id.large_text);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,getResources().getDimensionPixelSize(R.dimen.large_text_size));
                    textView.setGravity(Gravity.CENTER);
                    if(jsonObject.has("likes"))
                    {
                        jsonArray = jsonObject.getJSONArray("likes");
                        int count = 0;
                        while (count < jsonArray.length())
                        {
                            Fn.logD("likes_entered", "likes_entered");
                            JSONObject JO = jsonArray.getJSONObject(count);

                                LinearLayout location_found_view = (LinearLayout) view.findViewById(R.id.location_found);
                                Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE","driver_found");
                                LinearLayout driver_found_view = (LinearLayout) view.findViewById(R.id.driver_found);
//                                Button driver_mobile_no_view = (Button) view.findViewById(R.id.driver_mobile_no);
                                TextView driver_name_view = (TextView) view.findViewById(R.id.driver_name);
                                String received_driver_name = JO.getString("driver_name");
                                String received_driver_mobile_no = JO.getString("driver_mobile_no");
                                String received_driver_location_datetime = JO.getString("driver_location_datetime");
                                received_driver_current_lat = JO.getString("driver_location_lat");
                                received_driver_current_lng = JO.getString("driver_location_lng");

//                                Fn.logD("received_driver_current_lat", received_driver_current_lat);
//                                Fn.logD("received_driver_current_lng", received_driver_current_lng);
                                location_datetime.setText("Last Seen: "+Fn.getDateName(received_driver_location_datetime));
                                location_found_view.setVisibility(View.VISIBLE);
                                driver_name_view.setText("Driver: "+received_driver_name);
                                callButton.setText(received_driver_mobile_no);
                                driver_found_view.setVisibility(View.VISIBLE);
                                location_found_view.setVisibility(View.VISIBLE);
                                map.setVisibility(View.VISIBLE);
                                map_view.setVisibility(View.GONE);
                                setUpMapIfNeeded();
                                TimerProgramm();
                        }
                    }
                    else
                    {
                        Fn.logD("vehicle_match",Constants.Message.NO_CURRENT_BOOKING);
                        textView.setText(Constants.Message.NO_CURRENT_BOOKING);
                        map_view.addView(textView);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            ErrorDialog(Constants.Title.SERVER_ERROR, Constants.Message.SERVER_ERROR);
//            Fn.ToastShort(getActivity(), Constants.Message.NETWORK_ERROR);
        }
    }
    protected void vehicleLocationSuccess(String response){
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "vehicleLocationSuccess Called");
        if(!Fn.CheckJsonError(response)){
//            Fn.logD("vehicleLocationSuccess", "vehicleLocationSuccess Called");
//            super.onPostExecute(JsonString);
//            String received_crn_no = "DefaultIsNothing";
            JSONObject jsonObject;
            JSONArray jsonArray;
            try {
                jsonObject = new JSONObject(response);
                //jsonArray = jsonObject.getJSONArray("likes");
                String errFlag = jsonObject.getString("errFlag");
//                errMsg = jsonObject.getString("errMsg");
                if(errFlag.equals("1")){
                    Fn.logD("toastNotdone","toastNotdone");
                }
                else if(errFlag.equals("0"))
                {
                    if(jsonObject.has("likes"))
                    {
                        jsonArray = jsonObject.getJSONArray("likes");
                        int count = 0;
                        while (count < jsonArray.length())
                        {
                            JSONObject JO = jsonArray.getJSONObject(count);
                            received_driver_current_lat = JO.getString("driver_location_lat");
                            received_driver_current_lng = JO.getString("driver_location_lng");
                            String received_driver_location_datetime = JO.getString("driver_location_datetime");
                            location_datetime.setText("Last Seen: "+Fn.getDateName(received_driver_location_datetime));
                            map.setVisibility(View.VISIBLE);
                            map_view.setVisibility(View.GONE);
                            Fn.logD("LocationSuccessCallingMap","LocationSuccessCallingMap");
                            setUpMapIfNeeded();
                            count++;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            Fn.ToastShort(getActivity(), Constants.Message.SERVER_ERROR);
        }
    }
    @Override
    public void onClick(View v) {
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onClick Called");
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + callButton.getText().toString()));
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callIntent);
    }
    public void TimerProgramm() {
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "TimerProgramm Called");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Fn.logD("TimerProgram_running", "TimerProgram_running");
                        if (!stopTimer) {
//                            hashMap.clear();
                            String driver_location_url = Constants.Config.ROOT_PATH + "get_driver_location";
                            HashMap<String, String> hashMap = new HashMap<String, String>();
                            hashMap.put("driver_token", driver_token );
                            sendVolleyRequest(driver_location_url, hashMap, "vehicle_location");
                        }
                    }
                });
            }
        }, Constants.Config.GET_DRIVER_LOCATION_DELAY, Constants.Config.GET_DRIVER_LOCATION_PERIOD);
    }
    private void setUpMapIfNeeded() {
        Fn.logD("setUpMapIfNeeded", "map_setup" + String.valueOf(mMap));
        // Do a null check to confirm that we have not already instantiated the map.
        if(mMap != null)
        {
            mMap.clear();
            mMap = null;
        }
        if (mMap == null) {
//            Try to obtain the map from the SupportMapFragment.
//            mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentByTag("MAP_FRAGMENT");
            Fn.logD("mMapFragment", String.valueOf(mMapFragment));
            mMap = mMapFragment.getMap();
            Fn.logD("map_not_null", String.valueOf(mMap));
            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);
                if(FullActivity.mGoogleApiClient.isConnected()) {
                    do{
                        location = Fn.getAccurateCurrentlocation(FullActivity.mGoogleApiClient, getActivity());
                    }while(location == null);
                    if (location != null) {
                        double current_lat = location.getLatitude();
                        double current_lng = location.getLongitude();
                        LatLng latlng = new LatLng(Double.parseDouble(received_driver_current_lat), Double.parseDouble(received_driver_current_lng));// This methods gets the users current longitude and latitude.
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));//Moves the camera to users current longitude and latitude
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, Constants.Config.MAP_HIGH_ZOOM_LEVEL));//Animates camera and zooms to preferred state on the user's current location.
                        Fn.logD("received_driver_current_lat", received_driver_current_lat);
                        Fn.logD("received_driver_current_lng", received_driver_current_lng);

                        try {
                            mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(received_driver_current_lat), Double.parseDouble(received_driver_current_lng))).title("Driver"));
//                                mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble("22.6256"), Double.parseDouble("88.3576"))).title("Driver"));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        String url = makeURL(received_driver_current_lat, received_driver_current_lng, String.valueOf(current_lat), String.valueOf(current_lng));
                        Log.d("made_url", url);
                        HashMap<String,String> hashMap = new HashMap<String,String>();
                        sendVolleyRequest(url, Fn.checkParams(hashMap), "draw_path");
                    }
                }
            }
        }
    }
    public String makeURL(String sourceLat, String sourceLng, String destLat,String destLng){
        StringBuilder urlString = new StringBuilder();
        try {
            urlString.append("https://maps.googleapis.com/maps/api/directions/json");
            urlString.append("?origin=");// from
            urlString.append(URLEncoder.encode(sourceLat,"UTF-8"));
            urlString.append(",");
            urlString.append(URLEncoder.encode(sourceLng,"UTF-8"));
            urlString.append("&destination=");// to
            urlString.append(URLEncoder.encode(destLat,"UTF-8"));
            urlString.append(",");
            urlString.append(URLEncoder.encode(destLng,"UTF-8"));
            urlString.append("&sensor=false&mode=driving&alternatives=true");
            urlString.append("&key="+URLEncoder.encode(getResources().getString(R.string.server_APIkey1), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return urlString.toString();
    }
    public void drawPath(String  result) {
        Fn.logD("DrawPathRunning", "DrawPathRunning");
        Fn.logD("JsonString", result);
        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(12)
                    .color(Color.parseColor("#05b1fb"))//Google maps blue color
                    .geodesic(true)
            );
            JSONArray legsArray = routes.getJSONArray("legs");
            JSONObject legs = legsArray.getJSONObject(0);
            JSONObject distance = legs.getJSONObject("distance");
            String distance_km  = distance.getString("text");
            JSONObject duration = legs.getJSONObject("duration");
            String duration_min  = duration.getString("text");
//                      Fn.logD("distance_km",distance_km);
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("");
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(duration_min + " ( " + distance_km + " ) ");
            Fn.logD("PolyLine Added", "PolyLineAdded");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private List<LatLng> decodePoly(String encoded) {
        Fn.logD("DecodePoly Running", "DecodePoly Running");
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }
        return poly;
    }
    private void ErrorDialog(String Title,String Message){
        Fn.showDialog(getActivity(), Title, Message);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
//                        startLocationUpdates();
//                        TimerProgramm();
                        break;
                    case Activity.RESULT_CANCELED:
                        Fn.showGpsAutoEnableRequest(FullActivity.mGoogleApiClient, getActivity());//keep asking if imp or do whatever
                        break;
                }
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
//        setUpMapIfNeeded();
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onStart Called");
    }
    //start of extra method
    @Override
    public void onResume() {
        super.onResume();
        Fn.startAllVolley(requestQueue);
        stopTimer = false;
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onResume Called");
    }
    @Override
    public void onPause() {
        super.onPause();
        Fn.stopAllVolley(requestQueue);
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onPause Called");
        stopTimer = true;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        if(mMap != null){
            mMap = null;
        }
//        FullActivity.fragmentManager.beginTransaction().remove(getChildFragmentManager().findFragmentByTag("MAP_FRAGMENT")).commitAllowingStateLoss();
        Fn.cancelAllRequest(requestQueue, TAG);
        Fn.logD("DRIVER_DETAILS_FRAGMENT_LIFECYCLE", "onDestroyView Called");
    }

}
