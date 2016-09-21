package in.co.theshipper.www.shipper_owner;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationListener;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import in.co.theshipper.www.shipper_owner.Fn;

import static com.google.android.gms.internal.zzir.runOnUiThread;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyTrucks extends Fragment implements View.OnClickListener,GoogleMap.OnMarkerClickListener {

    protected final String get_vehicle_url = Constants.Config.ROOT_PATH+"my_trucks";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private PlaceAutocompleteFragment search_location;
    protected RequestQueue requestQueue;
    protected HashMap<String,String> hashMap;
    private View view;
    private String user_token,location_address,location_name;
    private LatLng southwest,northeast;
    private String TAG = FullActivity.class.getName();
    private boolean stopTimer = false;
    private Location location;
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap = null;
    private Double lattitude, longitude;
    private TextView error_message,driver_name;
    private LinearLayout lower_view,lowest_view;
    private Button callButton;
    private Timer timer;
    public Context context;
    private int vehicle_type = 0;
    private String cached_json_response = "";
    protected  DBController controller;
    protected SQLiteDatabase database;
    private RadioGroup radiogrp;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onAttach Called");
        this.context = context;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onCreate Called");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onCreateView Called");
        if (container == null) {
            return null;
        } else {

            view = inflater.inflate(R.layout.fragment_my_trucks, container, false);
            radiogrp = (RadioGroup) view.findViewById(R.id.check_box_selector);
            error_message = (TextView) view.findViewById(R.id.error_message);
            driver_name = (TextView) view.findViewById(R.id.driver_name);
            lower_view = (LinearLayout) view.findViewById(R.id.lower_view);
            lowest_view = (LinearLayout) view.findViewById(R.id.lowest_view);
            callButton = (Button) view.findViewById(R.id.callButton);
            callButton.setOnClickListener(this);
            return view;
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Fn.logD("onViewCreated", "onViewCreated");
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                .build();
        if (search_location == null) {
            Fn.logD("SEARCH_LOCATION_FRAGMENT", "autocompleteFragment_null");
            search_location = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.search_location);
            search_location.setHint(getResources().getString(R.string.search_location));
            Fn.logD("search_location_fragment", String.valueOf(search_location));
            search_location.setFilter(typeFilter);
            if ((southwest != null)) {
                Fn.SystemPrintLn("******haha**my curn loc is : " + southwest.longitude + " " + southwest.latitude);
                search_location.setBoundsBias(new LatLngBounds(southwest, northeast));
            }
            search_location.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    location_name = (String) place.getName();
                    location_address = (String) place.getAddress();
                    LatLng latLng = place.getLatLng();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));//Moves the camera to users current longitude and latitude
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.Config.MAP_SMALL_ZOOM_LEVEL));
                }

                @Override
                public void onError(Status status) {
                    Fn.logD("SEARCH_LOCATION_FRAGMENT", "onError");
                    // TODO: Handle the error.
                    Fn.logD("SEARCH_LOCATION_FRAGMENT", "An error occurred: " + status);
                }
            });
        }
        mMapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.map, mMapFragment, "MAP_FRAGMENT").commit();
        TimerProgramm();
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Fn.logD("ENTERED", "ONACTIVITYCREATED OF MYTRUCKS()");
        controller = new DBController(getActivity());
        database = controller.getWritableDatabase();
        String query = "SELECT " + controller.VEHICLETYPE_ID + "," + controller.VEHICLE_NAME + " FROM " + controller.TABLE_VIEW_VEHICLE_TYPE;
        final Cursor c = database.rawQuery(query, null);
        final long cnt = DatabaseUtils.queryNumEntries(database, controller.TABLE_VIEW_VEHICLE_TYPE);
        long count = cnt;

        if (count > 0) {
            final int id[]=new int[(int) count];
            c.moveToFirst();
            while (count > 0) {
                Fn.logD("Entered","count>0 for adding vehicletypes to MyTrucks()");
                RadioButton b = new RadioButton(getContext());
                b.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                b.setButtonDrawable(Fn.getVehicleImage(c.getInt(0)));
                radiogrp.addView(b);
                id[(int) (cnt-count)]=c.getInt(0);
                final long finalCount = count;
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vehicle_type =id[(int) (cnt- finalCount)];
                        mMap.clear();
                        lowest_view.setVisibility(View.INVISIBLE);
                        search_location.setText("");
                        LocationChanged();
                    }
                });
                count--;
                try{c.moveToNext();}
                catch (Exception e){Fn.logE("ERROR","PARSING THE CURSOR FOR THE VEHICLE_TYPE");}
            }

        }
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.callButton:
                Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onClick Called");
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + callButton.getText().toString()));
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);
                break;
        }
//        Toast.makeText(context, "vehicle type" + vehicle_type, Toast.LENGTH_LONG).show();
    }
    private void setUpMapIfNeeded() {
        if(getActivity()!=null) {
            Fn.logD("map_setup", "map_setup");
            // Do a null check to confirm that we have not already instantiated the map.
            if (mMap == null) {
                // Try to obtain the map from the SupportMapFragment.
//            mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentByTag("MAP_FRAGMENT");
                Fn.logD("mMapFragment", String.valueOf(mMapFragment));
                mMap = mMapFragment.getMap();
                // Check if we were successful in obtaining the map.
                Fn.logD("map_not_null", String.valueOf(mMap));
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
                    if (FullActivity.mGoogleApiClient.isConnected()) {
                        Fn.logD("mGoogleApiClient", "true");
                        do {
                            location = Fn.getAccurateCurrentlocation(FullActivity.mGoogleApiClient, getActivity());
                        } while (location == null);
                        if (location != null) {
                            Fn.logD("location", "not null");
                            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());// This methods gets the users current longitude and latitude.
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));//Moves the camera to users current longitude and latitude
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, Constants.Config.MAP_SMALL_ZOOM_LEVEL));//Animates camera and zooms to preferred state on the user's current location.
                        }
                    }
                }
            }
        }
    }
    /**** The mapfragment's id must be removed from the FragmentManager
     **** or else if the same it is passed on the next time then
     **** app will crash ****/
    public void TimerProgramm() {
        Fn.logD("TimerProgram", "TimerProgram");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Fn.logD("TimerProgram_running", "TimerProgram_running");
                        if (!stopTimer) {
                            Fn.SystemPrintLn("****Vehicle type " + vehicle_type);
                            if (vehicle_type != 0) {
                                LocationChanged();
                            }
                        }
                    }
                });
            }
        }, Constants.Config.GET_DRIVER_LOCATION_DELAY, Constants.Config.GET_DRIVER_LOCATION_PERIOD);
    }
    public void LocationChanged() {
        try {
            if(FullActivity.mGoogleApiClient.isConnected()) {
                Fn.logD("mGoogleApiClient", "true");
                do{
                    if(getActivity()!=null) {
                        location = Fn.getAccurateCurrentlocation(FullActivity.mGoogleApiClient, getActivity());
                    }
                }while(location == null);
                if (location != null) {
                    Fn.logD("location_not_null", "location_not_null");
                    lattitude = location.getLatitude();
                    longitude = location.getLongitude();
                    LatLng latlng = new LatLng(lattitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));//Moves the camera to users current longitude and latitude
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, Constants.Config.MAP_SMALL_ZOOM_LEVEL));
                    user_token = Fn.getPreference(getActivity(), "user_token");
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put("vehicle_type", String.valueOf(vehicle_type));
                    hashMap.put("current_lat", String.valueOf(lattitude));
                    hashMap.put("current_lng", String.valueOf(longitude));
                    hashMap.put("user_token", user_token);
                    sendVolleyRequest(get_vehicle_url, Fn.checkParams(hashMap));
                }
            }else{
                error_message.setText(Constants.Message.SERVER_ERROR);
                error_message.setVisibility(View.VISIBLE);
                lower_view.setVisibility(View.GONE);
                if(mMap != null){
                    mMap.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendVolleyRequest(String URL, final HashMap<String,String> hMap){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Fn.logD("onResponse", String.valueOf(response));
                timer.cancel();
                vehicleFindSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Fn.logD("onErrorResponse", String.valueOf(error));
                error_message.setText(Constants.Message.NETWORK_ERROR);
                error_message.setVisibility(View.VISIBLE);
                lower_view.setVisibility(View.GONE);
                if(mMap != null){
                    mMap.clear();
                }
            }
        }){
            @Override
            protected HashMap<String,String> getParams(){
                return hMap;
            }
        };
        stringRequest.setTag(TAG);
        if(getActivity()!=null) {
            Fn.addToRequestQue(requestQueue, stringRequest, getActivity());
        }
    }
    public void vehicleFindSuccess(String response) {
        if(cached_json_response != response){
            Fn.logD("previous_cached_response",cached_json_response);
            Fn.logD("present_cached_response",response);
            cached_json_response = response;
            if (!Fn.CheckJsonError(response)) {
                error_message.setVisibility(View.GONE);
                lower_view.setVisibility(View.VISIBLE);
//                JSONObject jsonObject;
//                JSONArray jsonArray;
//                String received_current_lat, received_current_lng, json_string, errFlag, errMsg;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String errFlag = jsonObject.getString("errFlag");
                    String errMsg = jsonObject.getString("errMsg");
                    Fn.logD("errFlag", errFlag);
                    Fn.logD("errMsg", errMsg);
                    //jsonArray = jsonObject.getJSONArray("likes");
                    if (errFlag.equals("1")) {
                        // Toast.makeText(ctx, errMsg, Toast.LENGTH_LONG).show();
                        Fn.logD("toastNotdone", "toastNotdone");
                    } else if (errFlag.equals("0")) {
                        if (jsonObject.has("likes")) {
                            JSONArray jsonArray = jsonObject.getJSONArray("likes");
                            //Toast.makeText(ctx,errMsg,Toast.LENGTH_LONG).show();
                            Fn.logD("toastdone", "toastdone");
                            int count = 0;
                            if(mMap != null) {
                                mMap.clear();
                            }
                            int marker_image = Fn.getMarkerImage(vehicle_type);
                            //                        bookNow.setTextColor(R.color.pure_white);
                            mMap.setOnMarkerClickListener(this);
                            while (count < jsonArray.length()) {
                                Fn.logD("likes_entered", "likes_entered");
                                JSONObject JO = jsonArray.getJSONObject(count);
                                String received_name = JO.getString("name");
                                String received_mobile_no = JO.getString("mobile_no");
                                String received_vehicletype_id = JO.getString("vehicletype_id");
                                String received_current_lat = JO.getString("location_lat");
                                String received_current_lng = JO.getString("location_lng");
                                Fn.logD("received_current_lat", received_current_lat);
                                Fn.logD("received_current_lng", received_current_lng);
                                //received_useremail = JO.getString("email");
                                if(mMap != null) {
                                    mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(Double.parseDouble(received_current_lat), Double.parseDouble(received_current_lng)))
                                            .icon(BitmapDescriptorFactory.fromResource(marker_image))
                                            .title(received_name)
                                            .snippet(received_mobile_no));
                                }
                                count++;
                            }
                        } else {
                            //                        bookNow.setTextColor(R.color.pure_white);
                            //Toast.makeText(ctx,errMsg, Toast.LENGTH_LONG).show();
                        }
//                        Fn.logE("BOOK_KNOW_FRAGMENT_LIFECYCLE", "onPostExecute Called");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                error_message.setText(Constants.Message.SERVER_ERROR);
                error_message.setVisibility(View.VISIBLE);
                lower_view.setVisibility(View.GONE);
                if(mMap != null){
                    mMap.clear();
                }
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(getActivity()!=null) {
            switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
                case REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
//                        startLocationUpdates();
                          TimerProgramm();
                            break;
                        case Activity.RESULT_CANCELED:
                            Fn.showGpsAutoEnableRequest(FullActivity.mGoogleApiClient, getActivity());//keep asking if imp or do whatever
                            break;
                    }
                    break;
            }
        }
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        Fn.logD("Marker", "clicked");
        marker.showInfoWindow();
        driver_name.setText(marker.getTitle());
        callButton.setText(marker.getSnippet());
        lowest_view.setVisibility(View.VISIBLE);
        return true;
    }
    @Override
    public void onStart() {
        super.onStart();
        setUpMapIfNeeded();
    }
    @Override
    public void onResume() {
        super.onResume();
        Fn.startAllVolley(requestQueue);
        stopTimer = false;
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onResume Called");
    }
    @Override
    public void onPause() {
        super.onPause();
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onPause Called");
        Fn.stopAllVolley(requestQueue);
        stopTimer = true;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        Fn.cancelAllRequest(requestQueue, TAG);
        if (mMap != null) {
            mMap = null;
        }
        database.close();
//        FullActivity.fragmentManager.beginTransaction().remove(getChildFragmentManager().findFragmentByTag("MAP_FRAGMENT")).commitAllowingStateLoss();
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onDestroyView Called");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        Fn.logE("MYTRUCKS_FRAGMENT_LIFECYCLE", "onDetach Called");
    }
}
