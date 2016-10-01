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


public class Drivers extends Fragment {
    private ListView list;
    private ListAdapter listAdapter;
    private String TAG = Drivers.class.getName();
    protected RequestQueue requestQueue;
    private String user_token;
    private ArrayList<HashMap<String,String>> values = new ArrayList<HashMap<String, String>>();

    public Drivers() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.fragment_drivers, container, false);
        list = (ListView) v.findViewById(R.id.my_driver_list);
//        user_token= Fn.getPreference(getActivity(),"user_token");
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listAdapter = new SimpleAdapter(getContext(),values,R.layout.driver_list_view,new String[] {"vehicle_image","driver_name","vehicle_no","driver_token"},
                new int[] {R.id.vehicle_image,R.id.driver_name, R.id.vehicle_no,R.id.driver_token});
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
        Fn.logD("Entered","onActivityCreated of Drivers");

    }

    private void createRequest(final int page_no){
        final String user_token = Fn.getPreference(getActivity(),"user_token");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.Config.ROOT_PATH+"my_driver_list",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Toast.makeText(MainActivity.this,response,Toast.LENGTH_LONG).show();
                        Fn.logD("Response for DRIVERS_LIST_FRAGMENT_LIFECYCLE received",response);
                        uiUpdate(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(MainActivity.this,error.toString(),Toast.LENGTH_LONG).show();
                        Fn.logD("error", ": volley request failed");
//                        ErrorDialog(Constants.Title.NETWORK_ERROR, Constants.Message.NETWORK_ERROR);
                        Fn.ToastShort(getActivity(),Constants.Message.NETWORK_ERROR);
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("user_token",user_token);
                params.put("page_no",String.valueOf(page_no));
//                Fn.logD("user_token",user_token);
                return params;
            }

        };
        requestQueue = Volley.newRequestQueue(getContext());
        stringRequest.setTag(TAG);
        requestQueue.add(stringRequest);

    }
    protected void uiUpdate(String response)
    {
        try {
            if(!Fn.CheckJsonError(response)) {
                String errFlag;
                String errMsg;
                JSONObject jsonObject = new JSONObject(response);
                errFlag = jsonObject.getString("errFlag");
                errMsg = jsonObject.getString("errMsg");
                Fn.logD("response errflag and errMsg", errFlag + " " + errMsg);
                JSONObject UpdationObject;
                JSONArray jsonArray;
                if (jsonObject.has("likes")) {
                    jsonArray = jsonObject.getJSONArray("likes");
                    int count = 0;

                    while (count < jsonArray.length()) {
                        UpdationObject = jsonArray.getJSONObject(count);
                        HashMap<String, String> qvalues = new HashMap<String, String>();
                        Fn.logD("driver_token received ", UpdationObject.get("driver_token").toString());
                        qvalues.put("driver_token", UpdationObject.get("driver_token").toString());
                        qvalues.put("driver_name", UpdationObject.get("driver_name").toString());
                        qvalues.put("vehicle_no", UpdationObject.get("driver_vehicle_no").toString());
                        qvalues.put("vehicle_image", Integer.toString(Fn.getVehicleImage(Integer.parseInt(UpdationObject.get("vehicletype_id").toString()))));
                        values.add(qvalues);
                        count++;
                    }
                }
            }else{
//                ErrorDialog(Constants.Title.SERVER_ERROR,Constants.Message.SERVER_ERROR);
                Fn.ToastShort(getActivity(),Constants.Message.SERVER_ERROR);
            }
        }
        catch(Exception e)
        {
            // handle exception
            Fn.logE("log_tag", "Error parsing data " + e.toString());
        }
        ((BaseAdapter)listAdapter).notifyDataSetChanged();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                View child_view = list.getChildAt(position-list.getFirstVisiblePosition());
                TextView driver_token = (TextView) child_view.findViewById(R.id.driver_token);
//                String PhoneNum = number.getText().toString();
                Fn.logD("Child View  :",String.valueOf(child_view));
                Fn.logD("onItemClick", "list clicked at position: " + position + " value driver_token =" + driver_token.getText());
                Fragment fragment = new Fragment();
                fragment = new DriverDetails();
                Bundle bundle = new Bundle();
                bundle.putString("driver_token", driver_token.getText().toString());
                fragment.setArguments(Fn.CheckBundle(bundle));
                FragmentManager fragmentManager = FullActivity.fragmentManager;
                FragmentTransaction transaction = fragmentManager.beginTransaction();
//                Fragment fragment = new BookNow();
                transaction.replace(R.id.main_content, fragment, Constants.Config.CURRENT_FRAG_TAG);
                if((FullActivity.homeFragmentIndentifier == -5)){
                    transaction.addToBackStack(null);
                    FullActivity.homeFragmentIndentifier =  transaction.commit();
                }else{
                    transaction.commit();
                    Fn.logD("fragment instanceof Driver","homeidentifier != -1");
                }
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_driver_details_fragment);

            }
        });
    }
    private void ErrorDialog(String Title,String Message){
        Fn.showDialog(getActivity(), Title, Message);
    }

    @Override
    public void onResume() {
        super.onResume();
        Fn.startAllVolley(requestQueue);
        Fn.logE("DRIVER_LIST_FRAGMENT_LIFECYCLE", "onResume Called");
    }
    @Override
    public void onPause() {
        super.onPause();
        Fn.stopAllVolley(requestQueue);
        Fn.logE("DRIVER_LIST_FRAGMENT_LIFECYCLE", "onPause Called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Fn.cancelAllRequest(requestQueue, TAG);
        Fn.logE("DRIVER_LIST_FRAGMENT_LIFECYCLE", "onDestroy Called");
    }
}
