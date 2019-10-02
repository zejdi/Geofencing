package com.testing.sim;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationEngineCallback<LocationEngineResult>, View.OnClickListener
,Callback<DirectionsResponse>{

    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

    MapView map;
    MapboxNavigation mapboxNavigation;
    MapboxMap mapBoxMap;
    SymbolManager symbolManager;
    NavigationRoute navigationRoute;
    NavigationMapRoute navigationMapRoute;

    DirectionsRoute directionsRoute;
    LocationEngine locationEngine;

    String fcmToken;
    public NotificationChannel channel;
    LatLng storeLocat,destLocat;

    LocationEngineRequest locationEngineRequest;
    LocationComponent locationComponent;
    LatLng mylocation;

    Button button,notificationButt;

    Boolean leftStore = false;
    Boolean entered = false;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(permissions,100);

        Mapbox.getInstance(this,getString(R.string.mapbox_token));

        initFirebase();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel();
        }

        MapboxNavigationOptions mapboxNavigationOptions = MapboxNavigationOptions.builder().isDebugLoggingEnabled(true).enableFasterRouteDetection(true).
                build();

        mapboxNavigation = new MapboxNavigation(this,getString(R.string.mapbox_token),mapboxNavigationOptions);
        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        button = findViewById(R.id.button);
        notificationButt = findViewById(R.id.notif);


        button.setOnClickListener(this);
        notificationButt.setOnClickListener(this);

        storeLocat = new LatLng( 41.98527027161482,20.958738327026367);
        destLocat = new LatLng(42.01033849637934,20.971956253051758);

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
        mapboxNavigation.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        mapBoxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                symbolManager = new SymbolManager(map,mapBoxMap,style);
                style.addImage("store",getDrawable(R.drawable.store));
                style.addImage("destination",getDrawable(R.drawable.destination));

                SymbolOptions symbolOptions = new SymbolOptions();
                symbolOptions.withIconImage("store");
                symbolOptions.withIconSize(0.4f);
                symbolOptions.withLatLng(new LatLng( 41.98527027161482,20.958738327026367));
                symbolManager.create(symbolOptions);

                symbolOptions = new SymbolOptions();
                symbolOptions.withIconImage("destination");
                symbolOptions.withIconSize(0.4f);
                symbolOptions.withLatLng(new LatLng(42.01033849637934,20.971956253051758));
                symbolManager.create(symbolOptions);

                LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions.builder(MainActivity.this,style).build();
                locationComponent = mapBoxMap.getLocationComponent();
                locationComponent.activateLocationComponent(locationComponentActivationOptions);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.NORMAL);
                locationComponent.setLocationComponentEnabled(true);
                locationComponent.zoomWhileTracking(16f);

                locationEngineRequest = new LocationEngineRequest.Builder(1000).setDisplacement(10f).setMaxWaitTime(2000).build();
                locationEngine = LocationEngineProvider.getBestLocationEngine(MainActivity.this);
                locationEngine.requestLocationUpdates(locationEngineRequest,MainActivity.this,getMainLooper());

            }
        });
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
        mylocation = new LatLng(result.getLocations().get(0));
        locationComponent.forceLocationUpdate(result.getLastLocation());
        if(mylocation.distanceTo(storeLocat)>100 && !leftStore)
        {
            pushNotification(1);
            leftStore=true;
        }
        if(mylocation.distanceTo(destLocat)<100 && !entered)
        {
            pushNotification(2);
            entered = true;
        }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {

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
                            "    \"key\":\"dLWIskbPJqE:APA91bGxq5yr4guIpAqZoerpIRjBzlrIfHy-UT5svzZGv41Kvf9G7rq6luHRDeHw3DoP1fnTYLE23JUYFVmhNO_H-jPvnHY4XPhlcXK441GStDj7xkSqr7zMTFc5a8V960r1h83qzrmN\",\n" +
                            "    \"message\":\"Hello how are you?\"\n" +
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
                                "    \"key\":\"dLWIskbPJqE:APA91bGxq5yr4guIpAqZoerpIRjBzlrIfHy-UT5svzZGv41Kvf9G7rq6luHRDeHw3DoP1fnTYLE23JUYFVmhNO_H-jPvnHY4XPhlcXK441GStDj7xkSqr7zMTFc5a8V960r1h83qzrmN\",\n" +
                                "    \"message\":\"Hello how are you?\"\n" +
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

                navigationRoute = NavigationRoute.builder(MainActivity.this)
                .accessToken(getString(R.string.mapbox_token))
                .origin(Point.fromLngLat(mylocation.getLongitude(),mylocation.getLatitude()))
                .destination(Point.fromLngLat(   20.971956253051758, 42.01033849637934))
                .build();
                navigationRoute.getRoute(this);
                break;
            case R.id.notif:
                pushNotification(1);
                break;
                default:

        }
    }

    @Override
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if(response.body() != null)
        {
            directionsRoute = response.body().routes().get(0);
            if(navigationMapRoute != null)
            {
                navigationMapRoute.removeRoute();
                navigationMapRoute.addRoute(directionsRoute);
            }
            else {
                navigationMapRoute = new NavigationMapRoute(map, mapBoxMap);
            }

            navigationMapRoute.addRoute(directionsRoute);

            locationEngine.removeLocationUpdates(this);
            ReplayRouteLocationEngine replayRouteLocationEngine = new ReplayRouteLocationEngine();
            replayRouteLocationEngine.assign(directionsRoute);
            replayRouteLocationEngine.updateSpeed(5000);
            replayRouteLocationEngine.requestLocationUpdates(new LocationEngineRequest.Builder(200).setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY).setFastestInterval(100).build(),
                    this,
                    getMainLooper());
            replayRouteLocationEngine.run();
//            NavigationLauncherOptions navigationLauncherOptions = NavigationLauncherOptions.builder().shouldSimulateRoute(true).directionsRoute(directionsRoute).build();
//            NavigationLauncher.startNavigation(MainActivity.this,navigationLauncherOptions);
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
                if(task.isSuccessful())
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


}
