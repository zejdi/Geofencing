package com.testing.sim;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
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
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener
,Callback<DirectionsResponse> {

    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

    private MapView map;
    private MapboxMap mapBoxMap;

    private SymbolManager symbolManager;
    private NavigationRoute navigationRoute;
    private DirectionsRoute directionsRoute;

    private NotificationChannel channel;
    private LatLng storeLocat,destLocat;

    private  LocationComponent locationComponent;
    private  LatLng mylocation;

    private Button button;

    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(permissions,100);

        Mapbox.getInstance(this,getString(R.string.mapbox_token));

        FirebaseMessaging.getInstance().subscribeToTopic("costumer").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    Toast.makeText(MainActivity.this, "Yes", Toast.LENGTH_SHORT).show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel();
        }

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        button = findViewById(R.id.button);

        button.setOnClickListener(this);

        destLocat = new LatLng(41.99668933900524,20.961098670959473);

        map.onCreate(savedInstanceState);


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
        super.onDestroy();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        mapBoxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                style.addImage("dest",getDrawable(R.drawable.destination));

                symbolManager = new SymbolManager(map,mapboxMap,style);

                SymbolOptions symbolOptions = new SymbolOptions()
                        .withIconImage("dest")
                        .withIconSize(0.6f)
                        .withLatLng(destLocat);
                symbolManager.create(symbolOptions);



                LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions.builder(MainActivity.this, style).build();
                locationComponent = mapBoxMap.getLocationComponent();
                locationComponent.activateLocationComponent(locationComponentActivationOptions);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.NORMAL);
                locationComponent.zoomWhileTracking(14d);
                locationComponent.setLocationComponentEnabled(true);
            }
        });
    }




    @Override
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if(response.body() != null) {
            directionsRoute = response.body().routes().get(0);
            Intent intent = new Intent(this,NavigationActivity.class);
            intent.putExtra("directionRoute",directionsRoute);
            intent.putExtra("storeLocation",storeLocat);
            intent.putExtra("destLocation",destLocat);
            startActivity(intent);
//            NavigationLauncherOptions navigationLauncherOptions = NavigationLauncherOptions.builder()
//                    .directionsRoute(directionsRoute)
//                    .shouldSimulateRoute(true)
//                    .build();
//            NavigationLauncher.startNavigation(this,navigationLauncherOptions);
        }


    }

    @Override
    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

    }




    @Override
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.button:
                if(locationComponent.getLastKnownLocation() != null) {
                    storeLocat = new LatLng(locationComponent.getLastKnownLocation());
                    navigationRoute = NavigationRoute.builder(MainActivity.this)
                            .accessToken(getString(R.string.mapbox_token))
                            .origin(Point.fromLngLat(storeLocat.getLongitude(), storeLocat.getLatitude()))
                            .destination(Point.fromLngLat(destLocat.getLongitude(), destLocat.getLatitude()))
                            .build();
                    navigationRoute.getRoute(this);
                }
                break;
                default:

        }
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

}
