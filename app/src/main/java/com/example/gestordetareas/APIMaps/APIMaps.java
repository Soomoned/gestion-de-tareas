package com.example.gestordetareas.APIMaps;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gestordetareas.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class APIMaps {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final Context context;
    private MapView mapView;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    public interface LocationCallback {
        void onLocationUpdated(GeoPoint location);

        void onLocationResult(LocationResult locationResult);
    }

    public APIMaps(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        initializeMap();
    }

    private void initializeMap() {
        Configuration.getInstance().load(context,
                context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE));

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
    }

    public void centerOnLocation(double latitude, double longitude, String title, int markerIcon) {
        IMapController mapController = mapView.getController();
        GeoPoint point = new GeoPoint(latitude, longitude);
        mapController.setCenter(point);

        if (title != null && markerIcon != 0) {
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(title);
            marker.setIcon(ContextCompat.getDrawable(context, markerIcon));
            mapView.getOverlays().add(marker);
        }
    }

    public void startUserLocationTracking(LocationCallback callback) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationUpdated(GeoPoint location) {

            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    updateUserMarker(userLocation);
                    if (callback != null) {
                        callback.onLocationUpdated(userLocation);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                new LocationRequest.Builder(5000).build(),
                (com.google.android.gms.location.LocationCallback) locationCallback,
                null
        );
    }

    private void updateUserMarker(GeoPoint location) {
        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setTitle("Tu ubicación");
            userMarker.setIcon(ContextCompat.getDrawable(context, R.drawable.arrow_direction));
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(location);
        mapView.invalidate();
    }

    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationCallback) locationCallback);
        }
    }

    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startUserLocationTracking(null);
        }
    }

    public void onDestroy() {
        stopLocationUpdates();
        if (mapView != null) {
            mapView.onDetach();
            mapView = null;
        }
    }

    public interface LocationPermissionCallback {
        void onLocationPermissionGranted();
        void onLocationPermissionDenied();
    }

    private LocationPermissionCallback permissionCallback;

    public void setLocationPermissionCallback(LocationPermissionCallback callback) {
        this.permissionCallback = callback;
    }
}