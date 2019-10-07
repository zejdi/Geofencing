package com.testing.sim;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, ProgressChangeListener {


    private NavigationView navigationView;

    private DirectionsRoute directionsRoute;
    private LatLng storeLocat, destLocat;
    private LatLng myLoc;
    private String fcmToken;

    private Boolean leftTheStore = false, arrivedToCostum = false;
    private final String TAG = "NavigationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        navigationView = findViewById(R.id.nav);
        navigationView.onCreate(savedInstanceState);

        directionsRoute = (DirectionsRoute) getIntent().getSerializableExtra("directionRoute");
        storeLocat = getIntent().getParcelableExtra("storeLocation");
        destLocat = getIntent().getParcelableExtra("destLocation");

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                Log.d(TAG, task.getResult().getToken());
                fcmToken = task.getResult().getToken();
            }
        });

        navigationView.initialize(this, new CameraPosition.Builder().target(new LatLng(41.98743941407912,20.959081649780273)).zoom(12d).build());

    }


    @Override
    public void onNavigationReady(boolean isRunning) {

        Toast.makeText(this, "Running", Toast.LENGTH_SHORT).show();
        NavigationViewOptions navigationViewOptions = NavigationViewOptions.builder()
                .progressChangeListener(this)
                .shouldSimulateRoute(true)
                .directionsRoute(directionsRoute)
                .build();

        navigationView.startNavigation(navigationViewOptions);
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        myLoc = new LatLng(location);
        if( !leftTheStore )
        {
            if( myLoc.distanceTo(storeLocat) > 100 )
            {
                pushNotification(1);
                leftTheStore = !leftTheStore;
            }
        }
        if( !arrivedToCostum )
        {
            if( myLoc.distanceTo(destLocat) < 100 )
            {
                pushNotification(2);
                arrivedToCostum = !arrivedToCostum;
            }
        }
    }

    public void pushNotification(final int event)
    {
        String url = "https://fcm.googleapis.com/fcm/send";
        StringRequest stringRequest = new StringRequest(Request.Method.POST,url,null,null) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "key=AAAALB0acuE:APA91bHz7399Ghi7mcIP8aCPTHB792rjwWdnST87iZQWltkj3fARWcK8EShT6Apbuxu2ocYaXOttFaaFcRq64eX8vL3Go8ew68rjVi2qW4osS05_0rjnG8FqFLIyp_jySv2hM9X4RHjY");
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String json;
                switch (event) {
                    case 1:
                        json = "{\n" +
                                " \"notification\":{ " +
                                " \"title\":\"Left Store\"" +
                                " \"body\":\"The driver has left the store\"}," +
                                "  \"to\":\"dLWIskbPJqE:APA91bGxq5yr4guIpAqZoerpIRjBzlrIfHy-UT5svzZGv41Kvf9G7rq6luHRDeHw3DoP1fnTYLE23JUYFVmhNO_H-jPvnHY4XPhlcXK441GStDj7xkSqr7zMTFc5a8V960r1h83qzrmN\"\n" +
                                "}";
                        break;
                    case 2:
                        json = "{\n" +
                                " \"notification\":{ " +
                                " \"title\":\"Arrived\"" +
                                " \"body\":\"The driver has arrived\"}," +
                                "  \"to\":\"dLWIskbPJqE:APA91bGxq5yr4guIpAqZoerpIRjBzlrIfHy-UT5svzZGv41Kvf9G7rq6luHRDeHw3DoP1fnTYLE23JUYFVmhNO_H-jPvnHY4XPhlcXK441GStDj7xkSqr7zMTFc5a8V960r1h83qzrmN\"\n" +
                                "}";
                        break;
                    default:
                        json = "{}";
                };
                try {
                    Log.d(TAG, json);
                    return json.getBytes("utf-8");
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
        };
        Volley.newRequestQueue(this).add(stringRequest);
    }

    @Override
    protected void onStart() {
        navigationView.onStart();
        super.onStart();
    }

    @Override
    protected void onResume() {
        navigationView.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        navigationView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        navigationView.onDestroy();
        leftTheStore = false;
        arrivedToCostum = false;
        super.onDestroy();
    }


}
