package in.co.theshipper.www.shipper_owner;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import in.co.theshipper.www.shipper_owner.Fn;
import in.co.theshipper.www.shipper_owner.FullActivity;
import in.co.theshipper.www.shipper_owner.R;


public class CurrentBooking extends Fragment {
    private ListView list;
    private ListAdapter listAdapter;
    private String TAG = FutureBooking.class.getName();
    protected RequestQueue requestQueue;
    private String user_token;
    private ArrayList<HashMap<String,String>> values = new ArrayList<HashMap<String, String>>();

    public CurrentBooking() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.fragment_future_booking, container, false);
        list = (ListView) v.findViewById(R.id.future_booking_list);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listAdapter = new SimpleAdapter(getContext(),values,R.layout.booking_list_view,new String[] {"crn_no","vehicle_type","datetime1","pickup_point","dropoff_point","vehicle_image"},
                new int[] {R.id.crn_no,R.id.vehicle_type, R.id.datetime1,R.id.pickup_point,R.id.dropoff_point,R.id.vehicle_image});
        list.setAdapter(listAdapter);
        createRequest(0);
        list.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void onLoadMore(int page) {
                System.out.println("*****page on loadMore: "+page);
                createRequest(page);
                //return true;
            }
        });
        Fn.logD("Entered","onActivityCreated of FutureBooking");

    }

    private void createRequest(final int page_no){
        if(getActivity() != null) {
            final String user_token = Fn.getPreference(getActivity(), "user_token");
            StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.Config.ROOT_PATH + "owner_current_booking_list",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            //Toast.makeText(MainActivity.this,response,Toast.LENGTH_LONG).show();
                            Fn.logD("Response for CURRENT_BOOKING_FRAGMENT_LIFECYCLE recieved", response);
                            Fn.logD("Response for CURRENT recieved", response);
                            uiUpdate(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            //Toast.makeText(MainActivity.this,error.toString(),Toast.LENGTH_LONG).show();
                            Fn.logD("error", ": volley request failed");
//                        ErrorDialog(Constants.Title.NETWORK_ERROR, Constants.Message.NETWORK_ERROR);
                            Fn.ToastShort(getActivity(), Constants.Message.NETWORK_ERROR);
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("user_token", user_token);
                    params.put("page_no", String.valueOf(page_no));
//                Fn.logD("user_token",user_token);
                    return params;
                }

            };
            requestQueue = Volley.newRequestQueue(getContext());
            stringRequest.setTag(TAG);
            requestQueue.add(stringRequest);
        }
    }
    protected void uiUpdate(String response)
    {
        if(getActivity() != null) {
            try {
                if (!Fn.CheckJsonError(response)) {
                    String errFlag;
                    String errMsg;
                    JSONObject jsonObject = new JSONObject(response);
                    errFlag = jsonObject.getString("errFlag");
                    errMsg = jsonObject.getString("errMsg");
                    Fn.logD("response errflag and errMsg", errFlag + " " + errMsg);
                    JSONObject UpdationObject;
                    JSONArray jsonArray;
                    Fn.logD("response", response);
                    if (jsonObject.has("likes")) {
                        Fn.logD("has","likes");
                        jsonArray = jsonObject.getJSONArray("likes");
                        int count = 0;

                        while (count < jsonArray.length()) {
                            UpdationObject = jsonArray.getJSONObject(count);
                            HashMap<String, String> qvalues = new HashMap<String, String>();
                            Fn.logD("crn_no and datetime1 recieved ", UpdationObject.get("crn_no").toString() + UpdationObject.get("booking_datetime").toString());
                            qvalues.put("crn_no", UpdationObject.get("crn_no").toString());
                            qvalues.put("datetime1", Fn.getDateName(UpdationObject.get("booking_datetime").toString()));
                            qvalues.put("vehicle_type", Fn.VehicleName(UpdationObject.get("vehicletype_id").toString(), getActivity()));
                            qvalues.put("pickup_point", UpdationObject.get("pickup_point").toString());
                            qvalues.put("dropoff_point", UpdationObject.get("dropoff_point").toString());
                            qvalues.put("vehicle_image", Integer.toString(Fn.getVehicleImage(Integer.parseInt(UpdationObject.get("vehicletype_id").toString()))));
                            values.add(qvalues);
                            count++;
                        }
                    }
                } else {
//                ErrorDialog(Constants.Title.SERVER_ERROR,Constants.Message.SERVER_ERROR);
                    Fn.ToastShort(getActivity(), Constants.Message.SERVER_ERROR);
                }
            } catch (Exception e) {
                // handle exception
                Fn.logE("log_tag", "Error parsing data " + e.toString());
            }
            ((BaseAdapter) listAdapter).notifyDataSetChanged();
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    View child_view = list.getChildAt(position - list.getFirstVisiblePosition());
                    TextView crn_no = (TextView) child_view.findViewById(R.id.crn_no);
//                String PhoneNum = number.getText().toString();
                    Fn.logD("Child View  :", String.valueOf(child_view));
                    Fn.logD("onItemClick", "list clicked at position: " + position + " value crn_no =" + crn_no.getText());
                    Fragment fragment = new Fragment();
                    fragment = new BookingDetails();
                    Bundle bundle = new Bundle();
                    bundle.putString("crn_no", crn_no.getText().toString());
                    fragment.setArguments(Fn.CheckBundle(bundle));
                    FragmentManager fragmentManager = FullActivity.fragmentManager;
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
//                Fragment fragment = new BookNow();
                    transaction.replace(R.id.main_content, fragment, Constants.Config.CURRENT_FRAG_TAG);
                    if ((FullActivity.homeFragmentIndentifier == -5)) {
                        transaction.addToBackStack(null);
                        FullActivity.homeFragmentIndentifier = transaction.commit();
                    } else {
                        transaction.commit();
                        Fn.logD("fragment instanceof Book", "homeidentifier != -1");
                    }
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_current_booking_detail_fragment);

                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Fn.startAllVolley(requestQueue);
        Fn.logE("CURRENT_BOOKING_FRAGMENT_LIFECYCLE", "onResume Called");
    }
    @Override
    public void onPause() {
        super.onPause();
        Fn.stopAllVolley(requestQueue);
        Fn.logE("CURRENT_BOOKING_FRAGMENT_LIFECYCLE", "onPause Called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Fn.cancelAllRequest(requestQueue, TAG);
        Fn.logE("CURRENT_BOOKING_FRAGMENT_LIFECYCLE", "onDestroy Called");
    }
}
