package maynoothuniversity.bcd.tyopendata;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.ClusterItem;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.ClusterManagerPlugin;
import com.mapbox.mapboxsdk.plugins.geojson.GeoJsonPlugin;
import com.mapbox.mapboxsdk.plugins.geojson.GeoJsonPluginBuilder;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnLoadingGeoJsonListener;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnMarkerEventListener;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnLoadingGeoJsonListener, OnMarkerEventListener {

    // Bounding box
    private static final LatLngBounds BALLYFERMOT = new LatLngBounds.Builder()
            .include(new LatLng(53.401447, -6.400051))
            .include(new LatLng(53.281997, -6.297312))
            .build();

    private MapView mapView;
    private MapboxMap mapboxMap;
    private ClusterManagerPlugin<MyItem> mClusterManagerPlugin;

    // IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);

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

        mapboxMap.setLatLngBoundsForCameraTarget(BALLYFERMOT);
        mapboxMap.setMaxZoomPreference(18);
        mapboxMap.setMinZoomPreference(10);

        mClusterManagerPlugin = new ClusterManagerPlugin<>(this, mapboxMap);
        doCluster();

//        GeoJsonPlugin geoJsonPlugin = new GeoJsonPluginBuilder()
//                .withContext(this)
//                .withMap(mapboxMap)
//                .withOnLoadingURL(this)
//                .withOnLoadingFileAssets(this)
//                .withOnLoadingFilePath(this)
//                .withMarkerClickListener(this)
//                .build();
//
//        geoJsonPlugin.setAssetsName("education_sample.geojson");
    }

    @Override
    public void onMarkerClickListener(Marker marker, JsonObject properties) {
//        String name = properties.get("Name").toString();
//        String address = properties.get("Address").toString();
//        String phone = properties.get("Phone ").toString();
//        String website = properties.get("Website ").toString();
//        String email = properties.get("Email").toString();
//
//        name = name.replace('"',' ');
//        address = address.replace('"',' ');
//        phone = phone.replace('"',' ');
//        website = website.replace('"',' ');
//        email = email.replace('"',' ');
//
//        marker.setTitle(name);
//        marker.setSnippet(address + "\n" + phone + "\n" + website + "\n" + email);
    }

    protected void doCluster() {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(53.342315,-6.354475), 14));
        mapboxMap.addOnCameraIdleListener(mClusterManagerPlugin);
        try {
            readItems();
        } catch (JSONException exception) {
            Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
            //Log.d("Cluster Problem: ", exception.toString());
        }
    }

    private void readItems() throws JSONException {
        InputStream inputStream = getResources().openRawResource(R.raw.education_sample_javascript);
        List<MyItem> items = new MyItemReader().read(inputStream);
        mClusterManagerPlugin.addItems(items);
    }

    public static class MyItem implements ClusterItem {
        private final LatLng mPosition;
        private String mTitle;
        private String mSnippet;

        public MyItem(double lat, double lng) {
            mPosition = new LatLng(lat, lng);
            mTitle = null;
            mSnippet = null;
        }

        private MyItem(double lat, double lng, String title, String snippet) {
            mPosition = new LatLng(lat, lng);
            mTitle = title;
            mSnippet = snippet;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getSnippet() {
            return mSnippet;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public void setSnippet(String snippet) {
            mSnippet = snippet;
        }
    }

    private static class MyItemReader {

        private static final String REGEX_INPUT_BOUNDARY_BEGINNING = "\\A";


        private List<MyItem> read(InputStream inputStream) throws JSONException {
            List<MyItem> items = new ArrayList<>();
            String json = new Scanner(inputStream).useDelimiter(REGEX_INPUT_BOUNDARY_BEGINNING).next();
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String title = "";
                String snippet = "";
                JSONObject object = array.getJSONObject(i);
                double lat = object.getDouble("Latitiude"); // lol
                double lng = object.getDouble("Longitude");
                if (!object.isNull("Name")) {
                    title = object.getString("Name");
                }
                if (!object.isNull("Address")) {
                    snippet += object.getString("Address") + "\n";
                }
                if (!object.isNull("Phone")) {
                    snippet += object.getString("Phone") + "\n";
                }
                if (!object.isNull("Website")) {
                    snippet += object.getString("Website") + "\n";
                }
                if (!object.isNull("Email")) {
                    snippet += object.getString("Email");
                }
                items.add(new MyItem(lat, lng, title, snippet));
            }
            return items;
        }
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



