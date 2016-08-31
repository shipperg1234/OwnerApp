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

import com.android.volley.RequestQueue;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.HashMap;
import java.util.Timer;

public class MyTrucks extends Fragment implements View.OnClickListener{
    private View view;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onAttach Called");
    }

    public MyTrucks() {
        // Required empty public constructor
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onStart() {
        super.onStart();
//        setUpMapIfNeeded();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onStart Called");
    }
    //start of extra method
    @Override
    public void onResume() {
        super.onResume();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onResume Called");
    }
    @Override
    public void onPause() {
        super.onPause();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onPause Called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Fn.logD("MY_TRUCKS_FRAGMENT_LIFECYCLE", "onDestroyView Called");
    }

    @Override
    public void onClick(View v) {

    }
}
