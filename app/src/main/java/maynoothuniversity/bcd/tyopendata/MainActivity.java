package maynoothuniversity.bcd.tyopendata;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.ClusterItem;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.ClusterManagerPlugin;
import com.mapbox.mapboxsdk.plugins.cluster.geometry.Point;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnLoadingGeoJsonListener;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnMarkerEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnLoadingGeoJsonListener, OnMarkerEventListener {

    // Bounding box
    private static final LatLngBounds BALLYFERMOT = new LatLngBounds.Builder()
            .include(new LatLng(53.401447, -6.400051))
            .include(new LatLng(53.281997, -6.297312))
            .build();

    private MapView mapView;
    private MapboxMap mapboxMap;
    private List<Point> pointList;
    private FeatureCollection featureCollection;
    private ClusterManagerPlugin<MyItem> mClusterManagerPlugin;

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
//                .withRandomFillColor()
//                .build();
//        geoJsonPlugin.setAssetsName("ballyfermot.geojson");

    }

    @Override
    public void onMarkerClickListener(Marker marker, JsonObject properties) {
//        String name, address, phone, website, email = "";
//        String snip = "";
//        if (properties.get("Name") != null) {
//            name = properties.get("Name").toString();
//            name = name.replace('"', ' ');
//            marker.setTitle(name);
//        }
//        if (properties.get("Address") != null) {
//            address = properties.get("Address").toString();
//            address = address.replace('"', ' ');
//            snip += address;
//        }
//        if (properties.get("Phone") != null) {
//            phone = properties.get("Phone").toString();
//            phone = phone.replace('"', ' ');
//            snip += phone;
//        }
//        if (properties.get("Website") != null) {
//            website = properties.get("Website").toString();
//            website = website.replace('"', ' ');
//            snip += website;
//        }
//        if (properties.get("Email") != null) {
//            email = properties.get("Email").toString();
//            email = email.replace('"', ' ');
//            snip += email;
//
//        }
//        marker.setSnippet(snip);
    }

    protected void doCluster() {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(53.342315, -6.354475), 14));
        mapboxMap.addOnCameraIdleListener(mClusterManagerPlugin);
        try {
            readItems();
        } catch (JSONException exception) {
            Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
            Log.d("Cluster Problem: ", exception.toString());
        }
    }

    private void readItems() throws JSONException {
        InputStream inputStream = getResources().openRawResource(R.raw.citizen_services);
        List<MyItem> items = new MyItemReader().read(inputStream);
        mClusterManagerPlugin.addItems(items);

        inputStream = getResources().openRawResource(R.raw.education);
        items = new MyItemReader().read(inputStream);
        mClusterManagerPlugin.addItems(items);

        inputStream = getResources().openRawResource(R.raw.health);
        items = new MyItemReader().read(inputStream);
        mClusterManagerPlugin.addItems(items);

        inputStream = getResources().openRawResource(R.raw.sport);
        items = new MyItemReader().read(inputStream);
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
                double lat = object.getDouble("Latitude");
                double lng = object.getDouble("Longitude");
                if (!object.isNull("Name")) {
                    title = object.getString("Name");
                }
                if (!object.isNull("Address")) {
                    if (object.getString("Phone").length() > 0) {
                        snippet += "Address: " + object.getString("Address") + "\n";
                    }
                }
                if (!object.isNull("Phone")) {
                    if (object.getString("Phone").length() > 0) {
                        snippet += "Phone: " + object.getString("Phone") + "\n";
                    }
                }
                if (!object.isNull("Website")) {
                    if (object.getString("Website").length() > 0) {
                        snippet += "Website: " + object.getString("Website");
                        if (object.getString("Email").length() > 0) {
                            snippet += "\n";
                        } else {
                            // don't append
                            snippet += "";
                        }
                    }
                }
                if (!object.isNull("Email")) {
                    if (object.getString("Email").length() > 0) {
                        snippet += "Email: " + object.getString("Email");
                    }
                }
                items.add(new MyItem(lat, lng, title, snippet));
            }
            return items;
        }
    }

    // GeoJSON Listeners
    @Override
    public void onPreLoading() {

    }

    @Override
    public void onLoaded() {

    }

    @Override
    public void onLoadFailed(Exception e) {

    }

    // MapBox Lifecycle Methods
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




