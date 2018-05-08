package maynoothuniversity.bcd.tyopendata;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.InputStream;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
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

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_layers_black_24dp);
        toolbar.setOverflowIcon(drawable);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);
    }

    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.setLatLngBoundsForCameraTarget(BALLYFERMOT);
        mapboxMap.setMaxZoomPreference(18);
        mapboxMap.setMinZoomPreference(10);

        mapboxMap.getUiSettings().setRotateGesturesEnabled(false);

        addClusters();

        mapboxMap.addOnMapClickListener(this);

    }

    // Interaction!
    @Override
    public void onMapClick(@NonNull LatLng point) {
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "unclustered-points1", "unclustered-points2", "unclustered-points3", "unclustered-points4");
        List<Feature> cluster = mapboxMap.queryRenderedFeatures(screenPoint, "clusterData-" + 1, "clusterData-" + 2);

        Display screenOrientation = getWindowManager().getDefaultDisplay();
        double currentZoom = mapboxMap.getCameraPosition().zoom;
        int half = (int)mapboxMap.getHeight()/2;

        // Zoom into cluster
        if(cluster.size() > 0) {
            CameraPosition position = new CameraPosition.Builder()
                    .target(point)
                    .zoom(currentZoom+2.25)
                    .build();
            mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
        }

        // Select a marker
        if (!features.isEmpty()) {
            Feature selectedFeature = features.get(0);
//            String title = selectedFeature.getStringProperty("Name");
//            Toast.makeText(this, "You selected " + title, Toast.LENGTH_SHORT).show();

            if (screenOrientation.getWidth() == screenOrientation.getHeight()) {
                // Square
                CameraPosition position = new CameraPosition.Builder()
                        .target(point)
                        .build();
                mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
            } else {
                if (screenOrientation.getWidth() < screenOrientation.getHeight()) {
                    // Portrait
                    CameraPosition position = new CameraPosition.Builder()
                            .target(point)
                            .build();
                    mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
                } else {
                    // Landscape
                    mapboxMap.setPadding(0, 0, 0, half);
                    CameraPosition position = new CameraPosition.Builder()
                            .target(point)
                            .build();
                    mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
                }
            }

            AlertDialog.Builder popupBuilder = new AlertDialog.Builder(GeoJsonActivity.this);
            View view = getLayoutInflater().inflate(R.layout.popup, null);

            TextView name = view.findViewById(R.id.name);
            TextView address = view.findViewById(R.id.address);
            TextView phone = view.findViewById(R.id.phone);
            TextView website = view.findViewById(R.id.website);
            TextView email = view.findViewById(R.id.email);

            name.setText(selectedFeature.getStringProperty("Name"));
            address.setText(selectedFeature.getStringProperty("Address"));
            if(selectedFeature.getStringProperty("Phone").length() > 0) phone.setText(String.format("Tel no.: %s", selectedFeature.getStringProperty("Phone")));
            if(selectedFeature.getStringProperty("Website").length() > 0) website.setText(selectedFeature.getStringProperty("Website"));
            if(selectedFeature.getStringProperty("Email").length() > 0) email.setText(selectedFeature.getStringProperty("Email"));

            popupBuilder.setView(view);
            AlertDialog dialog = popupBuilder.create();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(0.0f); // 0 = no dim, 1 = full dim
            }

            WindowManager.LayoutParams wlp;
            if (window != null) {
                wlp = window.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
                window.setAttributes(wlp);
            }
            dialog.show();
        }
    }

    // Clustering!
    private void addClusters() {
        String geoJson = loadGeoJsonFromAsset();
        try {
            GeoJsonSource geoJsonSource = null;
            if (geoJson != null) {
                geoJsonSource = new GeoJsonSource("data-layer", geoJson, new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(16)
                        .withClusterRadius(50)
                );
            }
            if (geoJsonSource != null) {
                mapboxMap.addSource(geoJsonSource);
            }
        } catch (Exception exception) {
            Timber.e(exception.toString());
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
                iconSize(0.67f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Citizen Service")
        );
        SymbolLayer dataUnclustered2 = new SymbolLayer("unclustered-points2", "data-layer");
        dataUnclustered2.withProperties(
                iconImage("education-image"),
                iconSize(0.67f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Education")
        );
        SymbolLayer dataUnclustered3 = new SymbolLayer("unclustered-points3", "data-layer");
        dataUnclustered3.withProperties(
                iconImage("health-image"),
                iconSize(0.67f),
                visibility(VISIBLE)
            ).withFilter(eq(Expression.literal("Type"),"Health")
        );
        SymbolLayer dataUnclustered4 = new SymbolLayer("unclustered-points4", "data-layer");
        dataUnclustered4.withProperties(
                iconImage("sport-image"),
                iconSize(0.67f),
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
    private String loadGeoJsonFromAsset() {
        try {
            // Load GeoJSON file
            InputStream is = getAssets().open("ballyfermot.geojson");
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

    // Toolbar Options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.layer_menu, menu);
        getMenuInflater().inflate(R.menu.info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_info:
                AlertDialog.Builder infoBuilder = new AlertDialog.Builder(GeoJsonActivity.this, R.style.Theme_AppCompat_Light_Dialog);
                View view = getLayoutInflater().inflate(R.layout.info, null);

                infoBuilder.setView(view);
                infoBuilder.setTitle("Info");
                infoBuilder.setMessage(R.string.info_message);
                infoBuilder.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // close the dialog
                    }
                });

                AlertDialog dialog = infoBuilder.create();
                //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#f2f2f2")));
                dialog.show();
                ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                break;

//            case R.id.citizen_toggle:
//                toggle("CS");
//                makeToast("WIP", 1);
//                break;
//            case R.id.education_toggle:
//                toggle("Edu");
//                makeToast("WIP", 1);
//                break;
//            case R.id.health_toggle:
//                toggle("Health");
//                makeToast("WIP", 1);
//                break;
//            case R.id.sport_toggle:
//                toggle("Sport");
//                makeToast("WIP", 1);
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle(String type) {
        if (type.equals("CS")) {
            changeLayer("unclustered-points1");
        }
        if (type.equals("Edu")) {
            changeLayer("unclustered-points2");
        }
        if (type.equals("Health")) {
            changeLayer("unclustered-points3");
        }
        if (type.equals("Sport")) {
            changeLayer("unclustered-points4");
        }
    }

    private void changeLayer(String layerName) {
        // toggle the individual markers
        Layer layer_points = mapboxMap.getLayer(layerName);
        if (layer_points != null) {
            if (VISIBLE.equals(layer_points.getVisibility().getValue())) {
                layer_points.setProperties(visibility(NONE));
            } else {
                layer_points.setProperties(visibility(VISIBLE));
            }
        }
        for(int i = 0; i <= 3; i++) {
            // toggle circles
            Layer layer = mapboxMap.getLayer("clusterData-" + i);
            if (layer != null) {
                if (VISIBLE.equals(layer.getVisibility().getValue())) {
                    layer.setProperties(visibility(NONE));
                } else {
                    layer.setProperties(visibility(VISIBLE));
                }
            }
            Layer layer_nums = mapboxMap.getLayer("countData");
            if (layer_nums != null) {
                layer_nums.setProperties(
                        textField("{point_count}"),
                        textSize(12f),
                        textColor(Color.WHITE)
                );
            }
        }
    }

    private void makeToast(String message, int duration) {
        if(duration == 1) {
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
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