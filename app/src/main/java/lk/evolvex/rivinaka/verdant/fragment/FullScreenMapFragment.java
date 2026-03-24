package lk.evolvex.rivinaka.verdant.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.util.PolylineDecoder;

public class FullScreenMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_LAT = "lat";
    private static final String ARG_LNG = "lng";
    private static final String ARG_NAME = "name";

    private double destLat;
    private double destLng;
    private String destName;

    private MapView mapView;
    private GoogleMap googleMap;
    private ProgressBar progressBar;
    private FusedLocationProviderClient fusedLocationClient;

    public FullScreenMapFragment() {
        // Required empty public constructor
    }

    public static FullScreenMapFragment newInstance(double lat, double lng, String name) {
        FullScreenMapFragment fragment = new FullScreenMapFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LNG, lng);
        args.putString(ARG_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            destLat = getArguments().getDouble(ARG_LAT);
            destLng = getArguments().getDouble(ARG_LNG);
            destName = getArguments().getString(ARG_NAME);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_screen_map, container, false);
        
        mapView = view.findViewById(R.id.full_map_view);
        progressBar = view.findViewById(R.id.map_progress_bar);
        TextView tvDestName = view.findViewById(R.id.tv_dest_name);
        tvDestName.setText(destName);

        view.findViewById(R.id.btn_close_map).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        view.findViewById(R.id.btn_open_external_map).setOnClickListener(v -> openGoogleMaps());

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        LatLng destination = new LatLng(destLat, destLng);
        googleMap.addMarker(new MarkerOptions().position(destination).title(destName));

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            fetchRoute();
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 15f));
            progressBar.setVisibility(View.GONE);
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    private void fetchRoute() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                LatLng dest = new LatLng(destLat, destLng);
                
                String url = getDirectionsUrl(origin, dest);
                new DownloadTask().execute(url);

                LatLngBounds bounds = new LatLngBounds.Builder()
                        .include(origin)
                        .include(dest)
                        .build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                
                TextView tvRouteDetails = getView().findViewById(R.id.tv_route_details);
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), destLat, destLng, results);
                float distanceInKm = results[0] / 1000;
                tvRouteDetails.setText(String.format("Distance: %.2f km", distanceInKm));
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(destLat, destLng), 15f));
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String key = "key=" + getString(R.string.google_maps_key);
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&" + key;
        return "https://maps.googleapis.com/maps/api/directions/json?" + parameters;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    private String downloadUrl(String strUrl) throws Exception {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            if (iStream != null) iStream.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            if (result == null) {
                progressBar.setVisibility(View.GONE);
                return;
            }

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
            }

            if (lineOptions != null) {
                googleMap.addPolyline(lineOptions);
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    private class DirectionsJSONParser {
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<>();
            try {
                org.json.JSONArray jRoutes = jObject.getJSONArray("routes");
                for (int i = 0; i < jRoutes.length(); i++) {
                    org.json.JSONArray jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List<HashMap<String, String>> path = new ArrayList<>();
                    for (int j = 0; j < jLegs.length(); j++) {
                        org.json.JSONArray jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = PolylineDecoder.decode(polyline);
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }
    }

    private void openGoogleMaps() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destLat + "," + destLng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(getContext(), "Google Maps app not installed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
                fetchRoute();
            }
        }
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
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
