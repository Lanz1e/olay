package com.example.aluno.olay;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.example.aluno.olay.slidinguppanel.SlidingUpPanelLayout;
import com.example.aluno.olay.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.example.aluno.olay.slidinguppanel.SlidingUpPanelLayout.PanelState;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private SlidingUpPanelLayout mLayout;

    FloatingActionButton fabcreate;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    String jsonResult;

    //dialogo dos markers
    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 768;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapEvent);
        mapFrag.getMapAsync(this);

        fabcreate = (FloatingActionButton) findViewById(R.id.fabCreat);
        fabcreate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CreateActivity.class);
                startActivity(intent);
            }
        });

        //SLIDER
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.addPanelSlideListener(new PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                //Log.i(TAG, "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
                //Log.i(TAG, "onPanelStateChanged " + newState);
            }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(PanelState.COLLAPSED);
            }
        });
        mLayout.setAnchorPoint(0.7f);
        mLayout.setPanelState(PanelState.HIDDEN);
    }

    //OVERRIDE NO VOLTAR PARA N FECHAR O APP, SOMENTE ESCONDER O SLIDER
    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == PanelState.EXPANDED || mLayout.getPanelState() == PanelState.ANCHORED)) {
            mLayout.setPanelState(PanelState.COLLAPSED);
        }
        else if(mLayout != null && mLayout.getPanelState() == PanelState.COLLAPSED){
            mLayout.setPanelState(PanelState.HIDDEN);
            fabcreate.setVisibility(View.VISIBLE);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap=googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }

        //TODA VEZ Q CLICAR NO MAPA ESCONDE O SLIDER
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {
                mLayout.setPanelState(PanelState.HIDDEN);
                if (!(fabcreate.getVisibility()==View.VISIBLE)) {
                    fabcreate.setVisibility(View.VISIBLE);
                }
            }
        });

        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                                @Override
                                                public boolean onMarkerClick(Marker marker) {
                                                    if(marker.getTitle().equals("Posição atual")){
                                                        mostrarToast("O marker Roxo representa sua posição atual.");
                                                    }
                                                    else if(!isOnline()){
                                                        mostrarToast("Não foi possível acessar a internet.");
                                                    }
                                                    else{
                                                        fabcreate.setVisibility(View.GONE);
                                                        criaCards(marker);
                                                    }
                                                    return true;
                                                }
                                            });
        accessWebService();
    }

    public void criaCards(Marker marker) {

        try {
            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray jsonMainNode = jsonResponse.optJSONArray("evento");
            //cria 3 vetores, para id,nome e preço
            final String[] id = new String[jsonMainNode.length()];
            final String[] localizacao = new String[jsonMainNode.length()];
            final String[] linkImagemEvento = new String[jsonMainNode.length()];
            final String[] data = new String[jsonMainNode.length()];
            final String[] nome = new String[jsonMainNode.length()];
            final String[] descricao = new String[jsonMainNode.length()];
            final String[] hora = new String[jsonMainNode.length()];

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                //cada vetor recebe seu valor respectivo
                id [i]=jsonChildNode.getString("idEvento");
                localizacao [i]=jsonChildNode.getString("localizacaoEvento");
                linkImagemEvento [i]=jsonChildNode.getString("linkImageEvento");
                data [i]=jsonChildNode.getString("dataEvento");
                nome [i]=jsonChildNode.getString("nomeEvento");
                descricao [i]=jsonChildNode.getString("descricaoEvento");
                hora [i]=jsonChildNode.getString("horaEvento");


                if(marker.getTitle().equals(id[i])){
                    final String linkImagem=linkImagemEvento[i];

                    TextView lblnome=(TextView) findViewById(R.id.lblNomeEvento);
                    lblnome.setText(nome[i]);

                    TextView lbldatahora=(TextView) findViewById(R.id.lblDataHora);
                    lbldatahora.setText("Dia "+data[i]+" às "+hora[i]);

                    TextView lbldescricao=(TextView) findViewById(R.id.lblDescricao);
                    lbldescricao.setText(descricao[i]);

                    ImageView imvfoto = (ImageView) findViewById(R.id.imvFoto);

                    //abaixando RES da imagem para gastar menos memória RAM
                    int size = (int) Math.ceil(Math.sqrt(MAX_WIDTH * MAX_HEIGHT));

                    // Loads given image
                    Picasso.with(imvfoto.getContext())
                            .load(linkImagemEvento[i])
                            .transform(new BitmapTransform(MAX_WIDTH, MAX_HEIGHT))
                            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                            .resize(size, size)
                            .centerInside()
                            .into(imvfoto);

                    //CASO O USUARIO QUEIRA AMPLIAR A FOTO
                    imvfoto.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(mLayout.getPanelState() == PanelState.COLLAPSED){
                                mLayout.setPanelState(PanelState.EXPANDED);
                            }else if(mLayout.getPanelState() == PanelState.EXPANDED || mLayout.getPanelState() == PanelState.ANCHORED){
                                Uri uri = Uri.parse(linkImagem);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                        }
                    });

                    mLayout.setPanelState(PanelState.COLLAPSED);
                }
            }

        } catch (JSONException e) {
            mostrarToast("Error" + e.toString());
        } catch(Exception e){
            mostrarToast("Error" + e.toString());
        }

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Posição atual");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11));

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissão negada.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //---------------------------JSON-----------------------------

    class JsonReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(params[0]);
            try {
                HttpResponse response = httpclient.execute(httppost);
                //recebe a lista de eventos como json
                jsonResult = inputStreamToString(
                        response.getEntity().getContent()).toString();
            }

            catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private StringBuilder inputStreamToString(InputStream is) {
            String rLine = "";
            StringBuilder answer = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            try {
                while ((rLine = rd.readLine()) != null) {
                    answer.append(rLine);
                }
            }

            catch (IOException e) {
                e.printStackTrace();
            }
            return answer;
        }

        @Override
        protected void onPostExecute(String result) {
            try{
                carregaMarkers();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void accessWebService() {
        JsonReadTask task = new JsonReadTask();
        task.execute(new String[] { "http://192.168.1.105/olay/pegaEventos.php" });
    }

    //metodo para receber lista ejogar no spinner
    public void carregaMarkers() {

        try {
            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray jsonMainNode = jsonResponse.optJSONArray("evento");
            //cria 3 vetores, para id,nome e preço
            final String[] id = new String[jsonMainNode.length()];
            final String[] localizacao = new String[jsonMainNode.length()];

            for (int i = 0; i < jsonMainNode.length(); i++) {
                JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                //cada vetor recebe seu valor respectivo
                id [i]=jsonChildNode.getString("idEvento");
                localizacao [i]=jsonChildNode.getString("localizacaoEvento");


                String[] latLon=localizacao[i].split(",");

                mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(Double.parseDouble(latLon[0]), Double.parseDouble(latLon[1])))
                        .title(id[i]));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //TOASTS
    void mostrarToast(String texto){
        Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
    }
    void mostrarToastL(String texto){
        Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_LONG).show();
    }

    //VERIFICAR A INTERNET
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}