package in.co.theshipper.www.shipper_owner;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class FlashActivity extends AppCompatActivity {
    protected  String TAG = FlashActivity.class.getName();
    protected RequestQueue requestQueue;
    protected  DBController controller;
    protected SQLiteDatabase database;
    private  HashMap<String, String> queryValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       getSupportActionBar().hide();
        setContentView(R.layout.activity_flash);
        Fn.logE("FLASH_ACTIVITY_LIFECYCLE", "onCreate");
        getSupportActionBar().hide();
        controller = new DBController(this);
        //new fetch().execute();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                NextActivity();
            }
        }, Constants.Config.FLASH_TO_MAIN_DELAY);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fn.logE(TAG,"onActivityResult");
    }
    @Override
    protected void onResume() {
        Fn.logE("FLASH_ACTIVITY_LIFECYCLE", "onResume");
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        Fn.logE("FLASH_ACTIVITY_LIFECYCLE", "onPause Called");
    }
    @Override
    protected void onDestroy() {
        Fn.logE("FLASH_ACTIVITY_LIFECYCLE","onDestroy Called");
        super.onDestroy();
    }

    public void NextActivity() {
        String user_token = Fn.getPreference(this, "user_token");
        Fn.logD("user_token", user_token);
        if (!user_token.equals("defaultStringIfNothingFound")) {
            Intent intent1 = new Intent(this, FullActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent1);
            finish();
        }else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
    /*class fetch extends AsyncTask<String, String, Void> {
        // private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        InputStream is = null;
        String result = "";
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(String... params) {
            String url_select = Constants.Config.ROOT_PATH+"fetch_view";
            String view_name[] = new String[4];
            view_name[0] = controller.TABLE_VIEW_BASE_FARE;
            view_name[1] = controller.TABLE_VIEW_CITY;
            view_name[2] = controller.TABLE_VIEW_PRICING;
            view_name[3] = controller.TABLE_VIEW_VEHICLE_TYPE;
            String primary_key[] = new String[4];
            primary_key[0] = controller.ID;
            primary_key[1] = controller.CITY_ID;
            primary_key[2] = controller._ID;
            primary_key[3] = controller.VEHICLETYPE_ID;
            String timestamp[] = new String[4];
            String q1="select max(update_date) from ";
            //String q2=" order by update_date desc limit 1";
            String s = "";
            //String query3 = "SELECT update_date FROM ";
            database = controller.getWritableDatabase();
            for (int k = 0; k < 4; k++) {
                long cnt = 0;
                try {
                    cnt = DatabaseUtils.queryNumEntries(database, view_name[k]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Fn.SystemPrintLn("count" + cnt);
                if (cnt > 0) {
//                     Fn.SystemPrintLn("entered");
                    Cursor d =database.rawQuery(q1+view_name[k],null);
                    d.moveToFirst();
                    s=d.getString(0);
                    timestamp[k] = s;
                }
                else {
                    timestamp[k] = "";
                }
            }
            for (int j = 0; j < 4; j++) {
                try {
                    URL url = new URL(url_select);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    // write out form parameters
                    String postParamaters = "view_name=" + view_name[j] + "&latest_date="+timestamp[j];
                    conn.setFixedLengthStreamingMode(postParamaters.getBytes().length);
                    PrintWriter out = new PrintWriter(conn.getOutputStream());
                    out.print(postParamaters);
                    out.close();
                    // conn.setRequestMethod("GET");
                    conn.connect();
                    is = conn.getInputStream();
                } catch (Exception e) {
                    Fn.logE("log_tag", "Error in http connection " + j + e.toString());
                }
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();
                    result = sb.toString();

                    Fn.SystemPrintLn("hahaha" + result + "hahaha");

                } catch (Exception e) {
                    //  handle exception
                    Fn.logE("log_tag", "Error converting result " + e.toString());
                }
                try {
                    String errFlag;
                    String errMsg;
                    JSONObject jsonObject = new JSONObject(result);
                    errFlag = jsonObject.getString("errFlag");
                    errMsg = jsonObject.getString("errMsg");
//                     Fn.SystemPrintLn(errFlag + errMsg);
                    JSONObject UpdationObject;
                    JSONArray jsonArray;
                    if (jsonObject.has("likes"))
                    {
                        controller.deleteTable(j);
                        Fn.logD("table"+j+"deleted","delete table called");
                        ////controller.createTable(j);
                        jsonArray = jsonObject.getJSONArray("likes");
                        int count = 0;
                        while (count < jsonArray.length()) {
                            UpdationObject = jsonArray.getJSONObject(count);
                            queryValues = new HashMap<String, String>();
                            if (j == 0) {
                                queryValues.put(controller.VEHICLETYPE_ID, UpdationObject.get(controller.VEHICLETYPE_ID).toString());
                                queryValues.put(controller.CITY_ID, UpdationObject.get(controller.CITY_ID).toString());
                                queryValues.put(controller.VEHICLE_NAME, UpdationObject.get(controller.VEHICLE_NAME).toString());
                                queryValues.put(controller.BASE_FARE, UpdationObject.get(controller.BASE_FARE).toString());
                                queryValues.put(controller.MAXIMUM_WEIGHT, UpdationObject.get(controller.MAXIMUM_WEIGHT).toString());
                                queryValues.put(controller.FREEWAITING_TIME, UpdationObject.get(controller.FREEWAITING_TIME).toString());
                                queryValues.put(controller.WAITING_CHARGE, UpdationObject.get(controller.WAITING_CHARGE).toString());
                                queryValues.put(controller.NIGHT_HOLDING_CHARGE, UpdationObject.get(controller.NIGHT_HOLDING_CHARGE).toString());
                                queryValues.put(controller.HARD_COPY_CHALLAN, UpdationObject.get(controller.HARD_COPY_CHALLAN).toString());
                                queryValues.put(controller.DIMENSION, UpdationObject.get(controller.DIMENSION).toString());
                                queryValues.put(controller.TRANSIT_CHARGE, UpdationObject.get(controller.TRANSIT_CHARGE).toString());
                                queryValues.put(controller.IS_ACTIVE, UpdationObject.get(controller.IS_ACTIVE).toString());
                                queryValues.put(controller.UPDATE_DATE, UpdationObject.get(controller.UPDATE_DATE).toString());
                                controller.insert(queryValues, j);
                                Fn.logE("TAG", "yes");
                            } else if (j == 1) {
                                queryValues.put(controller.CITY_ID, UpdationObject.get(controller.CITY_ID).toString());
                                queryValues.put(controller.CITY_NAME, UpdationObject.get(controller.CITY_NAME).toString());
                                queryValues.put(controller.IS_ACTIVE, UpdationObject.get(controller.IS_ACTIVE).toString());
                                queryValues.put(controller.UPDATE_DATE, UpdationObject.get(controller.UPDATE_DATE).toString());
                                controller.insert(queryValues, j);
                                Fn.logE("TAG", "yes");
                            } else if (j == 2) {
                                queryValues.put(controller.VEHICLETYPE_ID, UpdationObject.get(controller.VEHICLETYPE_ID).toString());
                                queryValues.put(controller.CITY_ID, UpdationObject.get(controller.CITY_ID).toString());
                                queryValues.put(controller.VEHICLE_NAME, UpdationObject.get(controller.VEHICLE_NAME).toString());
                                queryValues.put(controller.FROM_DISTANCE, UpdationObject.get(controller.FROM_DISTANCE).toString());
                                queryValues.put(controller.TO_DISTANCE, UpdationObject.get(controller.TO_DISTANCE).toString());
                                queryValues.put(controller.PRICE_KM, UpdationObject.get(controller.PRICE_KM).toString());
                                queryValues.put(controller.IS_ACTIVE, UpdationObject.get(controller.IS_ACTIVE).toString());
                                queryValues.put(controller.UPDATE_DATE, UpdationObject.get(controller.UPDATE_DATE).toString());
                                controller.insert(queryValues, j);
                                Fn.logE("TAG", "yes");
                            } else {
                                queryValues.put(controller.VEHICLETYPE_ID, UpdationObject.get(controller.VEHICLETYPE_ID).toString());
                                queryValues.put(controller.VEHICLE_NAME, UpdationObject.get(controller.VEHICLE_NAME).toString());
                                queryValues.put(controller.IS_ACTIVE, UpdationObject.get(controller.IS_ACTIVE).toString());
                                queryValues.put(controller.UPDATE_DATE, UpdationObject.get(controller.UPDATE_DATE).toString());
                                controller.insert(queryValues, j);
                                Fn.logE("TAG", "yes");
                            }
                            count++;
                        }
                    }
                    else if(errMsg.compareTo("Your request successful")==0)
                    {
                        controller.deleteTable(j);
                        Fn.logD("table" + j + "deleted", "delete table called");
                        Fn.logD("Entered","where errMsg=your request successful");
                    }
                } catch (Exception e) {
                    // handle exception
                    Fn.logE("log_tag", "Error parsing data " + e.toString());
                }
            }
            return null;
        }
        protected void onPostExecute(Void v) {
            try {
                controller.getAll();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            database.close();
        }
    }*/
}
