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

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
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
                "unclustered-points-CITIZEN", "unclustered-points-EDUCATION", "unclustered-points-HEALTH", "unclustered-points-SPORT");
        if (!features.isEmpty()) {
            Feature selectedFeature = features.get(0);
            String title = selectedFeature.getStringProperty("Name");
            Toast.makeText(this, "You selected " + title, Toast.LENGTH_SHORT).show();
        }
    }

    private void addClusters() {
        //String geoJson = loadGeoJsonFromAsset("ballyfermot.geojson");
        String CITIZEN_SERVICES = loadGeoJsonFromAsset("citizen_services.geojson");
        String EDUCATION = loadGeoJsonFromAsset("education.geojson");
        String HEALTH = loadGeoJsonFromAsset("health.geojson");
        String SPORT = loadGeoJsonFromAsset("sport.geojson");

        // Create layers for each data source (CS, education, health, sport)
        try {
            if (CITIZEN_SERVICES != null) {
                GeoJsonSource citizenServicesSource = new GeoJsonSource("citizen-services-layer", CITIZEN_SERVICES, new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                );
                mapboxMap.addSource(citizenServicesSource);
            }
            if (EDUCATION != null) {
                GeoJsonSource educationSource = new GeoJsonSource("education-layer", EDUCATION, new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                );
                mapboxMap.addSource(educationSource);
            }
            if (HEALTH != null) {
                GeoJsonSource healthSource = new GeoJsonSource("health-layer", HEALTH, new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                );
                mapboxMap.addSource(healthSource);
            }
            if (SPORT != null) {
                GeoJsonSource sportSource = new GeoJsonSource("sport-layer", SPORT, new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                );
                mapboxMap.addSource(sportSource);
            }
        } catch (Exception exception) {
            Timber.e(exception.toString());
        }

        // Circles based on density
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

        // Markers on their own
        SymbolLayer citzensUnclustered = new SymbolLayer("unclustered-points-CITIZEN", "citizen-services-layer");
        citzensUnclustered.withProperties(
                iconImage("citizen-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
        );
        SymbolLayer educationUnclustered = new SymbolLayer("unclustered-points-EDUCATION", "education-layer");
        educationUnclustered.withProperties(
                iconImage("education-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
        );
        SymbolLayer healthUnclustered = new SymbolLayer("unclustered-points-HEALTH", "health-layer");
        healthUnclustered.withProperties(
                iconImage("health-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
        );
        SymbolLayer sportUnclustered = new SymbolLayer("unclustered-points-SPORT", "sport-layer");
        sportUnclustered.withProperties(
                iconImage("sport-image"),
                iconSize(0.5f),
                visibility(VISIBLE)
        );

        mapboxMap.addLayer(citzensUnclustered);
        mapboxMap.addLayer(healthUnclustered);
        mapboxMap.addLayer(educationUnclustered);
        mapboxMap.addLayer(sportUnclustered);

        for (int i = 0; i < layers.length; i++) {
            CircleLayer csCircle = new CircleLayer("csData-" + i, "citizen-services-layer");
            csCircle.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );
            CircleLayer eduCircle = new CircleLayer("eduData-" + i, "education-layer");
            eduCircle.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );
            CircleLayer healthCircle = new CircleLayer("healthData-" + i, "health-layer");
            healthCircle.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );
            CircleLayer sportCircle = new CircleLayer("sportData-" + i, "sport-layer");
            sportCircle.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );

            // Filter
            csCircle.setFilter(
                    i == 0 ? gte(Expression.literal(("point_count")), layers[i][0]) :
                            all(gte(Expression.literal(("point_count")), layers[i][0]), lt(Expression.literal(("point_count")), layers[i - 1][0]))
            );
            eduCircle.setFilter(
                    i == 0 ? gte(Expression.literal(("point_count")), layers[i][0]) :
                            all(gte(Expression.literal(("point_count")), layers[i][0]), lt(Expression.literal(("point_count")), layers[i - 1][0]))
            );
            healthCircle.setFilter(
                    i == 0 ? gte(Expression.literal(("point_count")), layers[i][0]) :
                            all(gte(Expression.literal(("point_count")), layers[i][0]), lt(Expression.literal(("point_count")), layers[i - 1][0]))
            );
            sportCircle.setFilter(
                    i == 0 ? gte(Expression.literal(("point_count")), layers[i][0]) :
                            all(gte(Expression.literal(("point_count")), layers[i][0]), lt(Expression.literal(("point_count")), layers[i - 1][0]))
            );

            mapboxMap.addLayer(csCircle);
            mapboxMap.addLayer(eduCircle);
            mapboxMap.addLayer(healthCircle);
            mapboxMap.addLayer(sportCircle);
        }

        // Number text (e.g., '2', '3')
        SymbolLayer csText = new SymbolLayer("csDataText", "citizen-services-layer");
        csText.setProperties(
                textField("{point_count}"),
                textSize(12f),
                textColor(Color.WHITE)
        );
        SymbolLayer eduText = new SymbolLayer("eduDataText", "education-layer");
        eduText.setProperties(
                textField("{point_count}"),
                textSize(12f),
                textColor(Color.WHITE)
        );
        SymbolLayer healthText = new SymbolLayer("healthDataText", "health-layer");
        healthText.setProperties(
                textField("{point_count}"),
                textSize(12f),
                textColor(Color.WHITE)
        );
        SymbolLayer sportText = new SymbolLayer("sportDataText", "sport-layer");
        sportText.setProperties(
                textField("{point_count}"),
                textSize(12f),
                textColor(Color.WHITE)
        );

        mapboxMap.addLayer(csText);
        mapboxMap.addLayer(eduText);
        mapboxMap.addLayer(healthText);
        mapboxMap.addLayer(sportText);
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
            Timber.e("Exception Loading GeoJSON: %s", exception.toString());
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
