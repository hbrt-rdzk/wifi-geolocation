package com.hbrtrdzk.wifigeolocation;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String coordinates;
    double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.plume_background));
        Bundle extras = getIntent().getExtras();
        coordinates = extras.getString("Location");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        setLatitudeAndLongitude();
        LatLng pin = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(pin).title("Your location."));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pin, 16), 2000, null);
    }

    private void setLatitudeAndLongitude() {
        if (coordinates == null) {
            longitude = 0;
            latitude = 0;
        } else {
            String[] parts = coordinates.split(",", 2);
            latitude = Double.parseDouble(parts[0]);
            longitude = Double.parseDouble(parts[1]);
        }
    }
}
