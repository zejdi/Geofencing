package com.testing.sim;

import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;

public class Navigation extends AppCompatActivity implements OnNavigationReadyCallback {


    private NavigationView navigationView;
    MapboxMap mapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        navigationView = findViewById(R.id.nav);
        navigationView.initialize(this, new CameraPosition.Builder().target(new LatLng(41.98743941407912,20.959081649780273)).zoom(12d).build());
    }

    @Override
    public void onNavigationReady(boolean isRunning) {

        Toast.makeText(this, "Running", Toast.LENGTH_SHORT).show();
    }
}
