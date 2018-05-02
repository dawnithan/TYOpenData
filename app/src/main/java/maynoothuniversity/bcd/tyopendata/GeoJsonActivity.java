package maynoothuniversity.bcd.tyopendata;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import java.io.InputStream;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class GeoJsonActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener {

    // Bounding box
    private static final LatLngBounds BALLYFERMOT = new LatLngBounds.Builder()
            .include(new LatLng(53.401447, -6.400051))
            .include(new LatLng(53.281997, -6.297312))
            .build();

    private MapView mapView;
    private MapboxMap mapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_api_key));
        setContentView(R.layout.activity_geojson);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);
    }

    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.setLatLngBoundsForCameraTarget(BALLYFERMOT);
        mapboxMap.setMaxZoomPreference(18);
        mapboxMap.setMinZoomPreference(10);

        addClusters();

        mapboxMap.addOnMapClickListener(this);

    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint,
                "unclustered-points1", "unclustered-points2", "unclustered-points3", "unclustered-points4");
        if (!features.isEmpty()) {
            Feature selectedFeature = features.get(0);
            String title = selectedFeature.getStringProperty("Name");
            Toast.makeText(this, "You selected " + title, Toast.LENGTH_SHORT).show();
        }
    }

    private void addClusters() {
        String geoJson = loadGeoJsonFromAsset("ballyfermot.geojson");
        try {
            GeoJsonSource geoJsonSource = new GeoJsonSource("data-layer", geoJson, new GeoJsonOptions()
                    .withCluster(true)
                    .withClusterMaxZoom(14)
                    .withClusterRadius(50)
            );
            mapboxMap.addSource(geoJsonSource);
        } catch (Exception exception) {
            Log.e("AddClusters", exception.toString());
        }

        int[][] layers = new int[][]{
                new int[]{150, Color.parseColor("#dd1c77")},
                new int[]{20, Color.parseColor("#addd8e")},
                new int[]{0, Color.parseColor("#2b8cbe")}
        };

        // Set up icons
        Bitmap citizenIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.citizens_marker);
        Bitmap educationIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.education_marker);
        Bitmap healthIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.health_marker);
        Bitmap sportIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.sport_marker);
        mapboxMap.addImage("citizen-image", citizenIcon);
        mapboxMap.addImage("education-image", educationIcon);
        mapboxMap.addImage("health-image", healthIcon);
        mapboxMap.addImage("sport-image", sportIcon);

        SymbolLayer dataUnclustered1 = new SymbolLayer("unclustered-points1", "data-layer");
        dataUnclustered1.withProperties(
                iconImage("citizen-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Citizen Service")
        );
        SymbolLayer dataUnclustered2 = new SymbolLayer("unclustered-points2", "data-layer");
        dataUnclustered2.withProperties(
                iconImage("education-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Education")
        );
        SymbolLayer dataUnclustered3 = new SymbolLayer("unclustered-points3", "data-layer");
        dataUnclustered3.withProperties(
                iconImage("health-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Health")
        );
        SymbolLayer dataUnclustered4 = new SymbolLayer("unclustered-points4", "data-layer");
        dataUnclustered4.withProperties(
                iconImage("sport-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Sport")
        );

        mapboxMap.addLayer(dataUnclustered1);
        mapboxMap.addLayer(dataUnclustered2);
        mapboxMap.addLayer(dataUnclustered3);
        mapboxMap.addLayer(dataUnclustered4);

        for (int i = 0; i < layers.length; i++) {
            CircleLayer circlesData = new CircleLayer("clusterData-" + i, "data-layer");
            circlesData.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );
            circlesData.setFilter(
                    i == 0 ? gte(Expression.literal(("point_count")), layers[i][0]) :
                            all(gte(Expression.literal(("point_count")), layers[i][0]), lt(Expression.literal(("point_count")), layers[i - 1][0]))
            );
            mapboxMap.addLayer(circlesData);
        }
        SymbolLayer countData = new SymbolLayer("countData", "data-layer");
        countData.setProperties(
                textField("{point_count}"),
                textSize(12f),
                textColor(Color.WHITE)
        );
        mapboxMap.addLayer(countData);
    }

    @Nullable
    private String loadGeoJsonFromAsset(String filename) {
        try {
            // Load GeoJSON file
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");

        } catch (Exception exception) {
            Log.e("GeoJSON Activity ", "Exception Loading GeoJSON: " + exception.toString());
            exception.printStackTrace();
            return null;
        }
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