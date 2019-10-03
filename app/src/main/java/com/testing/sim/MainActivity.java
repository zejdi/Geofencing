package com.testing.sim;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Camera;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.services.android.navigation.ui.v5.MapboxNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener
,Callback<DirectionsResponse>, OnNavigationReadyCallback,ProgressChangeListener {

    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

    MapView map;
    MapboxNavigation mapboxNavigation;
    MapboxMap mapBoxMap;
    SymbolManager symbolManager;
    NavigationRoute navigationRoute;
    NavigationView navigationView;

    DirectionsRoute directionsRoute;
    LocationEngine locationEngine;

    public static String fcmToken;
    public NotificationChannel channel;
    LatLng storeLocat,destLocat;

    LocationEngineRequest locationEngineRequest;
    LocationComponent locationComponent;
    LatLng mylocation;

    Button button;

    Boolean leftStore = false;
    Boolean entered = false;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(permissions,100);
//        startActivity(new Intent(this,Navigation.class));

        Mapbox.getInstance(this,getString(R.string.mapbox_token));

        initFirebase();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel();
        }

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        navigationView = findViewById(R.id.navigation);
        button = findViewById(R.id.button);

        button.setOnClickListener(this);

        destLocat = new LatLng(41.99668933900524,20.961098670959473);

        map.onCreate(savedInstanceState);
        navigationView.onCreate(savedInstanceState);

        map.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        map.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        map.onDestroy();
        mapboxNavigation.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        mapBoxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                style.addImage("dest",getDrawable(R.drawable.destination));


                LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions.builder(MainActivity.this, style).build();
                locationComponent = mapBoxMap.getLocationComponent();
                locationComponent.activateLocationComponent(locationComponentActivationOptions);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.NORMAL);
                locationComponent.setLocationComponentEnabled(true);
                try{
                    locationComponent.getLocationEngine().getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
                        @Override
                         public void onSuccess(LocationEngineResult result) {
                            storeLocat = new LatLng(result.getLastLocation());
                            if(storeLocat != null)
                            {
                                Toast.makeText(MainActivity.this, "Location received", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, storeLocat.toString());
                                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds.Builder().include(storeLocat).include(destLocat).build(),1));
                            }
                         }

                         @Override
                         public void onFailure(@NonNull Exception exception) {

                         }
                     });
                  }
                catch (NullPointerException e)
                {
                    e.printStackTrace();
                }
                navigationView.onMapReady(mapboxMap);
                navigationView.initialize(new OnNavigationReadyCallback() {
                    @Override
                    public void onNavigationReady(boolean isRunning) {

                    }
                }, new CameraPosition.Builder().target(new LatLng(storeLocat)).zoom(11d).build());

            }
        });
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
                            "  \"data\":{\n" +
                            "    \"recipient\":\"dLWIskbPJqE:APA91bGxq5yr4guIpAqZoerpIRjBzlrIfHy-UT5svzZGv41Kvf9G7rq6luHRDeHw3DoP1fnTYLE23JUYFVmhNO_H-jPvnHY4XPhlcXK441GStDj7xkSqr7zMTFc5a8V960r1h83qzrmN\",\n" +
                            "    \"sender\":\"" + fcmToken + "\"\n" +
                            "  },\n" +
                            " \"notification\":{ " +
                            " \"title\":\"Left Store\"" +
                            " \"body\":\"The driver has left the store\"}," +
                            "  \"to\":\"/topics/costumer\"\n" +
                            "}";
                    break;
                    case 2:
                        json = "{\n" +
                                "  \"data\":{\n" +
                                "    \"recipient\":\"dLWIskbPJqE:APA91bGxq5yr4guIpAqZoerpIRjBzlrIfHy-UT5svzZGv41Kvf9G7rq6luHRDeHw3DoP1fnTYLE23JUYFVmhNO_H-jPvnHY4XPhlcXK441GStDj7xkSqr7zMTFc5a8V960r1h83qzrmN\",\n" +
                                "    \"sender\":\"" + fcmToken + "\"\n" +
                                "  },\n" +
                                " \"notification\":{ " +
                                " \"title\":\"Arrived\"" +
                                " \"body\":\"The driver has arrived\"}," +
                                "  \"to\":\"/topics/costumer\"\n" +
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
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.button:
                navigationView.setVisibility(View.VISIBLE);
                map.setVisibility(View.GONE);
                navigationView.retrieveNavigationMapboxMap().addCustomMarker(new SymbolOptions().withIconImage("dest").withLatLng(destLocat));
                navigationRoute = NavigationRoute.builder(MainActivity.this)
                .accessToken(getString(R.string.mapbox_token))
                .origin(Point.fromLngLat(storeLocat.getLongitude(),storeLocat.getLatitude()))
                .destination(Point.fromLngLat(destLocat.getLongitude(),destLocat.getLatitude()))
                .build();
                navigationRoute.getRoute(this);
                break;
                default:

        }
    }

    @Override
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if(response.body() != null) {
            Toast.makeText(this, String.valueOf(response.body().routes().size()), Toast.LENGTH_SHORT).show();
            directionsRoute = response.body().routes().get(0);
            navigationView.retrieveNavigationMapboxMap().updateLocationLayerRenderMode(RenderMode.NORMAL);
            NavigationViewOptions navigationViewOptions = NavigationViewOptions.builder()
                    .directionsRoute(directionsRoute)
                    .shouldSimulateRoute(true)
                    .progressChangeListener(this)
                    .build();
            navigationView.startNavigation(navigationViewOptions);
//            NavigationLauncherOptions navigationLauncherOptions = NavigationLauncherOptions.builder()
//                    .directionsRoute(directionsRoute)
//                    .shouldSimulateRoute(true)
//                    .build();
//            NavigationLauncher.startNavigation(this,navigationLauncherOptions);
        }


    }

    public void initFirebase()
    {

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                Log.d(TAG, task.getResult().getToken());
                fcmToken = task.getResult().getToken();
            }
        });

            FirebaseMessaging.getInstance().subscribeToTopic("costumer").addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful())
                        Toast.makeText(MainActivity.this, "Yes", Toast.LENGTH_SHORT).show();
                }
            });
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void initNotificationChannel()
    {

            channel = new NotificationChannel("test","testing0", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            channel.enableLights(true);
            channel.setLightColor(Color.rgb(233,23,23));
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }


    }


    @Override
    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

    }


    @Override
    public void onNavigationReady(boolean isRunning) {
        Toast.makeText(this, "Navigation running", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        navigationView.retrieveNavigationMapboxMap().updateLocation(location);
    }
}
