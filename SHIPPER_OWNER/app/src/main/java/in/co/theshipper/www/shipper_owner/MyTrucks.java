package in.co.theshipper.www.shipper_owner;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MyTrucks extends Fragment implements View.OnClickListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{
    protected  String TAG = MyTrucks.class.getName();
    private String name,mobile_no,vehicaltype_id,location_lat,location_lng;
    protected RequestQueue requestQueue;
    private String user_token;
    private View view;
    private TextView location_datetime,driver_name;
    private LinearLayout map,map_view;
    private ImageView driver_image;
    private Button driver_mobile_no;
    private Boolean isNetworkEnabled = false;
    GoogleApiClient mGoogleApiClient;
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap = null;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private Boolean stopTimer = false, stopForEver = false;
    private Timer timer;
    private Location location;
    private int googleCount = 0;

    public MyTrucks() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(getContext());
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
//                        startLocationUpdates();
                        TimerProgramm();
                        break;
                    case Activity.RESULT_CANCELED:
                        Fn.showGpsAutoEnableRequest(mGoogleApiClient, getContext());//keep asking if imp or do whatever
                        break;
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (container == null) {
            return null;
        } else {
            view = inflater.inflate(R.layout.fragment_my_trucks, container, false);
            return view;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Fn.logD("onViewCreated", "onViewCreated");
        mMapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.map, mMapFragment, "MAP_FRAGMENT").commit();
        TimerProgramm();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    public void TimerProgramm() {
        Fn.showProgressDialogLong(Constants.Message.CONNECTING,getContext());
//        Log.d("TimerProgram", "TimerProgram");
        int delay = Constants.Config.DELAY_LOCATION_CHECK; // delay for 20 sec.
        int period = Constants.Config.PERIOD_LOCATION_CHECK; // repeat every 20 sec.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Fn.SystemPrintLn("MyTrucksFragment_TimerProgram_running");
                        if ((stopTimer != true)&&(stopForEver != true)) {
                            Fn.SystemPrintLn("Start_location_check");
                            checkLocation();
                        }
                    }
                });
            }
        }, delay, period);
    }
    public void checkLocation(){
        if(Fn.isNetworkEnabled(getContext())){
            if (mGoogleApiClient.isConnected()) {
                Fn.logE("mGoogleApiClient", "true");
                Fn.logE("isNetworkEnabled", "true");
                location = Fn.getAccurateCurrentlocation(mGoogleApiClient, getContext());
                if (location != null) {
                    stopForEver = true;
                    sendOwnerLocation(location);
                }
            }else {
                googleCount++;
                if(googleCount == 3) {
                    stopForEver = true;
                    ErrorDialog(Constants.Title.NETWORK_ERROR, Constants.Message.NETWORK_ERROR);
                }
            }
        }
    }

    private void sendOwnerLocation(Location location){
        String get_my_trucks_url = Constants.Config.ROOT_PATH + "my_trucks";
        double current_lat = location.getLatitude();
        double current_lng = location.getLongitude();
        HashMap<String, String> hashMap = new HashMap<String, String>();
        user_token = Fn.getPreference(getActivity(), "user_token");
        hashMap.put("current_lat",String.valueOf(current_lat));
        hashMap.put("current_lng",String.valueOf(current_lng));
        hashMap.put("user_token", user_token);
        sendVolleyRequest(get_my_trucks_url, Fn.checkParams(hashMap));
    }

    public void sendVolleyRequest(String URL, final HashMap<String,String> hMap){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Fn.logD("onResponse", String.valueOf(response));
                stopForEver = true;
                Fn.dismissProgressDialogLong();
                setMarkers(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Fn.logD("onErrorResponse", String.valueOf(error));
                ErrorDialog(Constants.Title.NETWORK_ERROR, Constants.Message.NETWORK_ERROR);
            }
        }){
            @Override
            protected HashMap<String,String> getParams(){
                return hMap;
            }
        };
        stringRequest.setTag(TAG);
        Fn.addToRequestQue(requestQueue, stringRequest, getContext());
    }

    private void ErrorDialog(String Title,String Message){
        Fn.showDialog(getContext(), Title, Message);
    }

    public void setMarkers(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            String errFlag = jsonObject.getString("errFlag");
            String   errMsg = jsonObject.getString("errMsg");
            Fn.logD("errFlag",errFlag);
            Fn.logD("errMsg",errMsg);
            if(errFlag.equals("1")){
                Fn.Toast(getContext(),errMsg);
                Fn.logD("toastNotdone", "toastNotdone");
                ErrorDialog(Constants.Title.SERVER_ERROR, Constants.Message.SERVER_ERROR);
            }
            else if(errFlag.equals("0"))
            {
                if(jsonObject.has("likes")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("likes");
//                    Fn.Toast(this,errMsg);
                    Fn.logD("toastdone", "toastdone");
                    int count = 0;
                    while (count < jsonArray.length())
                    {
                        Fn.logD("likes_entered", "likes_entered");
                        JSONObject JO = jsonArray.getJSONObject(count);
                        name = JO.getString("name");
                        mobile_no = JO.getString("mobile_no");
                        vehicaltype_id = JO.getString("vehicletype_id");
                        location_lat = JO.getString("location_lat");
                        location_lng = JO.getString("location_lng");
                        /*Marker marker =  map.addMarker(new MarkerOptions()
                                        .position(new LatLng(43.2568193,-2.9225534))
                                        .anchor(0.5f, 0.5f)
                                        .title("Title3")
                                        .snippet("Snippet3"));*/
                        count++;
                    }
                }
                else
                {
                    Fn.Toast(getContext(),errMsg);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onResume Called");
        Fn.startAllVolley(requestQueue);
        stopTimer = false;
        //mGoogleApiClient.connect();
        Fn.logE("google_connected", "true");
        if (checkPlayServices())
        {
            Fn.logE("checkPlayServices", "true");
            if (Fn.isGpsEnabled(getContext())) {
                if (Fn.isNetworkEnabled(getContext())) {
                    Fn.logE("isNetworkEnabled", "true");
                    if (mGoogleApiClient.isConnected()) {
                        Fn.logE("mGoogleApiClient", "true");
                        isNetworkEnabled = true;
                        location = Fn.getAccurateCurrentlocation(mGoogleApiClient, getContext());
                        if (location != null) {
                            stopForEver = true;
                            sendOwnerLocation(location);
                        } else {
                            if (timer == null) {
                                TimerProgramm();
                            }
                        }
                    } else {
                        googleCount++;
                        if (timer == null) {
                            TimerProgramm();
                        }
                    }
                } else {
                    ErrorDialog(Constants.Title.NETWORK_ERROR, Constants.Message.NETWORK_ERROR);
                    if (timer == null) {
                        TimerProgramm();
                    }
                }
            } else {
                Fn.showGpsAutoEnableRequest(mGoogleApiClient, getContext());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Fn.startAllVolley(requestQueue);
    }

    @Override
    public void onPause() {
        super.onPause();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onPause Called");
        Fn.stopAllVolley(requestQueue);
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        stopTimer = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onDestroyView Called");
        Fn.cancelAllRequest(requestQueue,TAG);
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getContext());
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(getActivity(),result,1).show();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
