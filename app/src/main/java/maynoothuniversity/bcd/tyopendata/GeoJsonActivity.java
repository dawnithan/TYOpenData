package maynoothuniversity.bcd.tyopendata;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.ImageView;
import android.widget.TextView;

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
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.InputStream;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toNumber;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

public class GeoJsonActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener {

    // Defines a bounding box region to confine the camera to this region.
    // The first set of coordinates is the top left corner, the second set is the bottom right corner.
    private static final LatLngBounds BALLYFERMOT = new LatLngBounds.Builder()
            .include(new LatLng(53.401447, -6.400051))
            .include(new LatLng(53.281997, -6.297312))
            .build();

    // Defines our MapView and MapboxMap objects
    private MapView mapView;
    private MapboxMap mapboxMap;

    // This function is called when the activity starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required to use Mapbox maps.
        // You can either put your Mapbox API key in directly, or put it in your strings.xml and reference it like below
        Mapbox.getInstance(this, getString(R.string.mapbox_api_key));

        // Sets this activity's layout as the "activity_geojson" layout
        setContentView(R.layout.activity_geojson);

        // Defines the toolbar
        // To remove the toolbar title uncomment the commented line below
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Assign our MapView with the MapView in the layout (with id mapView)
        // Create the map and then wait for the callback which tells us the map is ready
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // This function is called when the map is created
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        // Set the bounds of the camera to the bounding box we defined before
        // We can also set the maximum amount we can zoom in, and the minimum amount we can zoom out
        mapboxMap.setLatLngBoundsForCameraTarget(BALLYFERMOT);
        mapboxMap.setMaxZoomPreference(18);
        mapboxMap.setMinZoomPreference(10);

        // Disables the ability to rotate the map
        mapboxMap.getUiSettings().setRotateGesturesEnabled(false);

        // Calls the function which will add markers to the map and cluster them
        addClusters();

        // Adds a click listener to register taps on the map
        mapboxMap.addOnMapClickListener(this);
    }

    // This function is called when the map is tapped/clicked
    @Override
    public void onMapClick(@NonNull LatLng point) {

        // Get the point of the screen that was clicked
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);

        // Query the map's features to see if the point lines up with one of the given layer IDs
        // These layer IDs denote different types of markers on the map
        // If it's one of the "unclustered-points" layers, add the feature to the "features" list
        // If it's one of the "clusterData" layers, add the feature to the "cluster" list
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "unclustered-points1", "unclustered-points2", "unclustered-points3", "unclustered-points4");
        List<Feature> cluster = mapboxMap.queryRenderedFeatures(screenPoint, "clusterData-" + 1, "clusterData-" + 2);

        // Define variables for the screen orientation, current zoom, and half the map size
        Display screenOrientation = getWindowManager().getDefaultDisplay();
        double currentZoom = mapboxMap.getCameraPosition().zoom;
        int half = (int)mapboxMap.getHeight()/2;

        // If a cluster is tapped
        if(cluster.size() > 0) {
            // Update the camera to zoom in 2.25 units more from the current zoom
            CameraPosition position = new CameraPosition.Builder()
                    .target(point)
                    .zoom(currentZoom+2.25)
                    .build();
            mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
        }

        // If a marker is tapped
        else if (!features.isEmpty()) {
            // Retrieve the information contained within the selected feature
            Feature selectedFeature = features.get(0);

            // Centre the camera on the selected feature
            if (screenOrientation.getWidth() == screenOrientation.getHeight()) {
                // The orientation is "Square", centre as normal
                CameraPosition position = new CameraPosition.Builder()
                        .target(point)
                        .build();
                mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
            } else {
                if (screenOrientation.getWidth() < screenOrientation.getHeight()) {
                    // The orientation is "Portrait", centre as normal
                    CameraPosition position = new CameraPosition.Builder()
                            .target(point)
                            .build();
                    mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
                } else {
                    // The orientation is "Landscape", add padding (using the half variable)
                    // This ensures our popup does not cover the marker we are moving to
                    mapboxMap.setPadding(0, 0, 0, half);
                    CameraPosition position = new CameraPosition.Builder()
                            .target(point)
                            .build();
                    mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position));
                }
            }

            // Create an alert dialog builder to act as our information popup - quick and dirty fix for SymbolLayers
            AlertDialog.Builder popupBuilder = new AlertDialog.Builder(GeoJsonActivity.this);

            // We'll use a custom layout for the dialog
            // Inflating means to render the object in memory
            View view = getLayoutInflater().inflate(R.layout.popup, null);

            // In my popup layout, I have 5 blank TextViews which have relevant IDs
            // Here, we can define them and give them variable names
            TextView name = view.findViewById(R.id.name);
            TextView address = view.findViewById(R.id.address);
            TextView phone = view.findViewById(R.id.phone);
            TextView website = view.findViewById(R.id.website);
            TextView email = view.findViewById(R.id.email);

            // In my marker features, I have a number of properties
            // I can assign them to their relevant TextView by getting the String property
            // Note: the key (i.e. the name of the column in your data) for the property is case-sensitive
            name.setText(selectedFeature.getStringProperty("Name"));
            address.setText(selectedFeature.getStringProperty("Address"));

            // We can save these properties to variables to make working with these values easier
            String phoneNo = selectedFeature.getStringProperty("Phone");
            String web = selectedFeature.getStringProperty("Website");
            String mail = selectedFeature.getStringProperty("Email");

            // If the phone number exists, then set it to the Phone TextView
            if(phoneNo.length() > 0) phone.setText(selectedFeature.getStringProperty("Phone"));

            // To prevent gaps, I do a bit of fudging (not necessary)
            // If the website exists, set the website to the website TextView and the email to the email TextView
            // Else if the website doesn't exist but the email does, set the email to the website TextView
            if(web.length() > 0) {
                website.setText(web);
                email.setText(mail);
            } else if (mail.length() > 0) {
                website.setText(mail);
            }

            // Set the inflated view to the popup and create the dialog
            popupBuilder.setView(view);
            AlertDialog dialog = popupBuilder.create();

            // Before enabling the dialog, we can remove the default dim that a dialog makes
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(0.0f); // 0 = no dim, 1 = full dim
            }

            // We can also reposition the dialog by setting the Gravity to BOTTOM
            WindowManager.LayoutParams wlp;
            if (window != null) {
                wlp = window.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
                window.setAttributes(wlp);
            }

            // Finally, show the dialog popup
            dialog.show();
        }
    }

    // This function is called from onMapReady() to add markers and cluster them
    // Original code can be found on the Mapbox demo GitHub repo: https://bit.ly/2K5VbVQ
    private void addClusters() {
        // We can load our GeoJSON data which is saved in our "assets" folder using this function
        String geoJson = loadGeoJsonFromAsset();
        try {
            // Create an empty geoJsonSource
            GeoJsonSource geoJsonSource = null;
            // If our data is not null
            if (geoJson != null) {
                // Fill the source with the data along with an ID
                // We enable clustering as an option, with the max zoom needed to open the cluster being 16
                // And the radius that a cluster uses to take in markers around being 50 units
                geoJsonSource = new GeoJsonSource("data-layer", geoJson, new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(16)
                        .withClusterRadius(50)
                );
            }
            if (geoJsonSource != null) {
                // Add the source to the map
                mapboxMap.addSource(geoJsonSource);
            }
        } catch (Exception exception) {
            Timber.e(exception.toString());
        }

        // Defines the different steps of colour depending on the number of objects within a cluster
        // The number indicates how much is needed in order for the colour to change
        // i.e. blue < 20, green > 20, etc.
        int[][] layers = new int[][]{
                new int[]{150, Color.parseColor("#dd1c77")},
                new int[]{20, Color.parseColor("#addd8e")},
                new int[]{0, Color.parseColor("#2b8cbe")}
        };

        // Set up marker icons by referencing them as Bitmap images from the drawable folder
        Bitmap citizenIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.citizens_marker);
        Bitmap educationIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.education_marker);
        Bitmap healthIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.health_marker);
        Bitmap sportIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.sport_marker);

        // We can assign these images key names by adding them to the map like this
        mapboxMap.addImage("citizen-image", citizenIcon);
        mapboxMap.addImage("education-image", educationIcon);
        mapboxMap.addImage("health-image", healthIcon);
        mapboxMap.addImage("sport-image", sportIcon);

        // These are the base markers that are not clustered
        // Each type is given a separate layer ID
        // Each type references the same source ID (the geoJsonSource from the start of the method)
        SymbolLayer dataUnclustered1 = new SymbolLayer("unclustered-points1", "data-layer");
        // iconImage uses the key name for images referenced above
        // iconIgnorePlacement set to true means icons will be visible regardless of zoom or overlap with other icons
        // withFilter will check that the "Type" value in the data is equal to the parameter (e.g. the feature is a "Citizen Service")
        dataUnclustered1.withProperties(
                iconImage("citizen-image"),
                iconIgnorePlacement(true),
                iconSize(0.70f)
            ).withFilter(eq(literal("Type"),"Citizen Service")
        );
        SymbolLayer dataUnclustered2 = new SymbolLayer("unclustered-points2", "data-layer");
        dataUnclustered2.withProperties(
                iconImage("education-image"),
                iconIgnorePlacement(true),
                iconSize(0.70f)
            ).withFilter(eq(literal("Type"),"Education")
        );
        SymbolLayer dataUnclustered3 = new SymbolLayer("unclustered-points3", "data-layer");
        dataUnclustered3.withProperties(
                iconImage("health-image"),
                iconIgnorePlacement(true),
                iconSize(0.70f)
            ).withFilter(eq(literal("Type"),"Health")
        );
        SymbolLayer dataUnclustered4 = new SymbolLayer("unclustered-points4", "data-layer");
        dataUnclustered4.withProperties(
                iconImage("sport-image"),
                iconIgnorePlacement(true),
                iconSize(0.70f)
            ).withFilter(eq(literal("Type"),"Sport")
        );

        // Add these different markers to the map
        mapboxMap.addLayer(dataUnclustered1);
        mapboxMap.addLayer(dataUnclustered2);
        mapboxMap.addLayer(dataUnclustered3);
        mapboxMap.addLayer(dataUnclustered4);

        // For each of the cluster types (their colour/size)
        for (int i = 0; i < layers.length; i++) {
            // Create a circle to represent the cluster
            // Each layer gets its own clusterData with the same source ID
            CircleLayer circlesData = new CircleLayer("clusterData-" + i, "data-layer");
            circlesData.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );
            // Add a filter to the cluster layer that hides the circles based on "point_count"
            Expression pointCount = toNumber(get("point_count"));
            circlesData.setFilter(
                    i == 0
                            ? gte(pointCount, literal(layers[i][0])) :
                            all(
                                    gte(pointCount, literal(layers[i][0])),
                                    lt(pointCount, literal(layers[i - 1][0]))
                            )
            );
            mapboxMap.addLayer(circlesData);
        }
        // Create the text which counts how many features are in each cluster
        SymbolLayer countData = new SymbolLayer("countData", "data-layer");
        countData.setProperties(
                textField("{point_count}"),
                textSize(12f),
                textColor(Color.WHITE)
        );
        mapboxMap.addLayer(countData);
    }

    // This function loads a GeoJSON file from the "assets" folder, called from the addClusters() method
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

    // This function adds a menu to the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info_menu, menu);
        return true;
    }

    // This function opens the info dialog when selected from the toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get the id of the item selected, though in this case there is only 1 possible item
        // The switch statement is a layover from older code and could be reworked
        int id = item.getItemId();
        switch (id) {
            case R.id.action_info:
                // As with the main dialog popup, we create another dialog builder using a light dialog theme
                // and supplying a custom layout alongside it
                AlertDialog.Builder infoBuilder = new AlertDialog.Builder(GeoJsonActivity.this, R.style.Theme_AppCompat_Light_Dialog);
                View view = getLayoutInflater().inflate(R.layout.info, null);

                // Here we set the view, title, and a message that is saved in strings.xml
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
                dialog.show();

                // Two TextViews are assigned apart from the setMessage in order to customize them
                // as well as making them hyperlink-able through the setMovementMethod method
                TextView contact = dialog.findViewById(R.id.contact_us);
                TextView credits = dialog.findViewById(R.id.credits);
                if (contact != null) contact.setMovementMethod(LinkMovementMethod.getInstance());
                if (credits != null) credits.setMovementMethod(LinkMovementMethod.getInstance());

                // Defines the 4 logos in the info dialog
                ImageView sfiLogo = dialog.findViewById(R.id.SFI_logo);
                ImageView bcdLogo = dialog.findViewById(R.id.BCD_logo);
                ImageView muLogo = dialog.findViewById(R.id.MU_logo);
                ImageView stdomLogo = dialog.findViewById(R.id.StDom_logo);

                // Set click listeners to each image to make them also hyperlink to their relevant website using intents
                if (sfiLogo != null) {
                    sfiLogo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.sfi.ie/"));
                            startActivity(i);
                        }
                    });
                }
                if (bcdLogo != null) {
                    bcdLogo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dashboards.maynoothuniversity.ie/"));
                            startActivity(i);
                        }
                    });
                }
                if (muLogo != null) {
                    muLogo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.maynoothuniversity.ie/"));
                            startActivity(i);
                        }
                    });
                }
                if (stdomLogo != null) {
                    stdomLogo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://stdominicsballyfermot.ie/"));
                            startActivity(i);
                        }
                    });
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // MapBox lifecycle methods
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