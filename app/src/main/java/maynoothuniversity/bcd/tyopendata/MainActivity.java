package maynoothuniversity.bcd.tyopendata;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.geojson.GeoJsonPlugin;
import com.mapbox.mapboxsdk.plugins.geojson.GeoJsonPluginBuilder;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnLoadingGeoJsonListener;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnMarkerEventListener;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnLoadingGeoJsonListener, OnMarkerEventListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private GeoJsonPlugin geoJsonPlugin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_api_key));
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        geoJsonPlugin = new GeoJsonPluginBuilder()
                .withContext(this)
                .withMap(mapboxMap)
                .withOnLoadingURL(this)
                .withOnLoadingFileAssets(this)
                .withOnLoadingFilePath(this)
                .withMarkerClickListener(this)
                .build();

        geoJsonPlugin.setAssetsName("education_sample.geojson");
    }

    @Override
    public void onMarkerClickListener(Marker marker, JsonObject properties) {
        String name = properties.get("Name").toString();
        String address = properties.get("Address").toString();
        String phone = properties.get("Phone ").toString();
        String website = properties.get("Website ").toString();
        String email = properties.get("Email").toString();

        name = name.replace('"',' ');
        address = address.replace('"',' ');
        phone = phone.replace('"',' ');
        website = website.replace('"',' ');
        email = email.replace('"',' ');

        marker.setTitle(name);
        marker.setSnippet(address + "\n" + phone + "\n" + website + "\n" + email);
    }

    @Override
    public void onPreLoading() {

    }

    @Override
    public void onLoaded() {

    }

    @Override
    public void onLoadFailed(Exception e) {

    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}



