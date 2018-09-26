package com.talagasoft.gojekdriver;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.talagasoft.gojekdriver.model.HttpXml;
import com.talagasoft.gojekdriver.model.Penumpang;
import com.talagasoft.gojekdriver.model.PenumpangRecord;

import org.w3c.dom.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends  AbstractMapActivity
        implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
        LocationListener,  GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, OnMapClickListener, GoogleMap.OnMapLongClickListener {

    //global
    String TAG="MapsActivity";
    SharedPreferences mSetting;
    private boolean needsInit=false;
    String mWebsite="";

    //google API
    Location location;
    LocationManager locationManager;
    GoogleMap map;
    Criteria criteria;
    private GoogleApiClient mGoogleApiClient;
    private int PLACE_PICKER_REQUEST = 1;
    PolylineOptions mPolyline;

    //driver variabel
    String mNomorHp = "", mNama="";
    Bitmap mIconDriver=null,mIconPeople=null;
    LatLng myLatLng,myLatLngOld;

    //penumpang variabel
    float mToLat,mToLng;        //  lokasi penjemputan
    float mToLatEnd,mToLngEnd;  //  lokasi tujuan di antar
    private Place mPlaceTujuan;
    String mPlaceTujuanText="";
    String mNoHpPenumpang="";
    TextView mSelNama;
    private Marker mSelMarker;
    List<PenumpangRecord> arPenumpang = new ArrayList<PenumpangRecord>();
    int mSelIndex=0;     // selected index from arPenumpang

    int mMode=0;    //0 - mode driver lg cari penumpang
                    //1 - mode driver lg menuju tempat penumpang
                    //2 -
                    //3 - mode driver antar ke tempat tujuan
    int MODE_CARI=0,MODE_JEMPUT=1,MODE_ANTAR=3;

    //--handle mulai jalan ke tempat penumpang
    TimerTask _task;
    final Handler _handler = new Handler();
    Timer _timer;

    // chat model
    private ChatModel _chat;
    private boolean mMode3Init=false;
    // controls
    ListView lvPenumpang;
    Button btnStopTujuan,btnStart,btnCall,btnChat,btnRefresh,btnSiap,btnStop;
    ImageButton btnSend;
    TextView lblChat,lblChatEdit;
    LinearLayout divChat;


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapClick(LatLng latLng) {
        map.clear();
        map.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .title("Starting Point")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.antar))
        );
        myLatLng=latLng;
    }
    @Override
    public void onMapReady(final GoogleMap map1) {
        this.map = map1;
        map.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));
        map.setOnInfoWindowClickListener(this);
        map.setMyLocationEnabled(true);
        myLocation();
        //ketika kembali load tetapi masih dalam keadaan antar, kembali tampilkan rutenya
        if((mMode==MODE_ANTAR || mMode==MODE_JEMPUT) && !mNoHpPenumpang.isEmpty() ){
            DrawRoute(myLatLng,new LatLng(mToLat,mToLng));
            if(mMode==MODE_JEMPUT){
                btnSiap.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
            }
            if(mMode==MODE_ANTAR){
                btnSiap.setVisibility(View.GONE);
                btnStopTujuan.setVisibility(View.VISIBLE);
            }
        } else {
            mMode = MODE_CARI;
            getCalonPenumpang();
        }

        if (needsInit && myLatLng != null ) {

            CameraUpdate center=CameraUpdateFactory.newLatLng(myLatLng);
            CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
            map.moveCamera(center);
            map.animateCamera(zoom);

        }
        //langsung scan penumpang
        Button btnRefresh=(Button)findViewById(R.id.btnRefesh);
        btnRefresh.setVisibility(View.GONE);
        startTimer();
    }
    private double getRadius(int inKm){
        double latDistance = Math.toRadians(myLatLng.latitude - inKm);
        double lngDistance = Math.toRadians(myLatLng.longitude - inKm);
        double a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)) +
                (Math.cos(Math.toRadians(myLatLng.latitude))) *
                        (Math.cos(Math.toRadians(inKm))) *
                        (Math.sin(lngDistance / 2)) *
                        (Math.sin(lngDistance / 2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = 6371 / c;
        if (dist<50){
                    /* Include your code here to display your records */
        }
        return dist;

    }

    private void getCalonPenumpang(){
        if (myLatLng == null ) return;
        arPenumpang=new Penumpang(this).getAllNewOrder(mNama,myLatLng);
        if (arPenumpang == null) {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
            return;
        }
        String[] values = new String[arPenumpang.size()];
        for(int i=0;i<arPenumpang.size();i++) {
            PenumpangRecord penumpangRecord = arPenumpang.get(i);
            addMarkerPenumpang(map, penumpangRecord.getLat(), penumpangRecord.getLng(),
                    penumpangRecord.getName(), penumpangRecord.getPhone());
            values[i]=penumpangRecord.getName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values);
        lvPenumpang.setAdapter(adapter);
        //lvPenumpang.setVisibility(View.VISIBLE);

    }
    public void myLocation(){

        getLocation();

        if(location==null){
            Log.d(TAG,"location==null");
            myLatLng=new LatLng(-6.584711,107.4667);
        } else {
            if (myLatLng != null){
                if ( ! (myLatLng.latitude==location.getLatitude() && myLatLng.longitude==location.getLongitude())){
                    myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d("myLocation","Receipt new location "+myLatLng.toString());
                    return;
                }
            }
            myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        PushMyLatLng();
        moveToCurrentLocation(map, myLatLng);
    }
    private void PushMyLatLng(){
        if(myLatLngOld!=null) {
            if (myLatLng.latitude == myLatLngOld.latitude && myLatLng.longitude == myLatLngOld.longitude) {
                Log.d("PushMyLatLng", "Same with old myLatLng, skipped...");
                return;
            }
        }
        String mUrl=getResources().getString(R.string.url_source)+"pushme.php?hp="+mNomorHp+"&lat="+myLatLng.latitude+"&lng="+myLatLng.longitude;
        HttpXml web=new HttpXml();
        StringBuilder doc=web.GetUrlData(mUrl);
        if(doc != null) {
            Log.d("PushMyLatLng", "Lat/Lng:"+myLatLng.latitude+"/"+myLatLng.longitude);
            addMarkerMe(map, myLatLng.latitude, myLatLng.longitude,"Me", mNama);
        } else {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
        }
        myLatLngOld=myLatLng;
    }
    private void DrawRoutePoly(){
        if(mPolyline==null){
            Log.d(TAG,"mPolyline is null");
            return;
        }
        Polyline line = map.addPolyline(mPolyline);
    }

    public void DrawRoute(LatLng from,LatLng to){

        mToLat= (float) to.latitude;
        mToLng= (float) to.longitude;

        float mDistance = getDistanceKm(from,to);

        Document document= null;
        try {
            document = new GMapV2Direction().getDocument(from,to,"drive");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(document == null){
            Toast.makeText(getBaseContext(),"Unable DrawRoute",Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<LatLng> oLat=new GMapV2Direction().getDirection(document);

        mPolyline=new PolylineOptions();
        for(int i=0;i<oLat.size();i++){
            mPolyline.add(oLat.get(i));
        }
        mPolyline.width(5);
        mPolyline.color(Color.BLUE);

        DrawRoutePoly();
    }


    public String getDistance(LatLng my_latlong, LatLng frnd_latlong) {
        float distance=getDistanceFloat(my_latlong,frnd_latlong);
        String dist = distance + " M";

        if (distance > 1000.0f) {
            distance = distance / 1000.0f;
            dist = distance + " KM";
        }
        return dist;
    }
    public float getDistanceKm(LatLng my_latlong, LatLng frnd_latlong) {
        float distance=getDistanceFloat(my_latlong,frnd_latlong);
        if (distance > 1000.0f) {
            distance = distance / 1000.0f;
        } else {
            distance=1.0f;
        }
        return distance;

    }
    public float getDistanceFloat(LatLng my_latlong, LatLng frnd_latlong) {
        Location l1 = new Location("One");
        l1.setLatitude(my_latlong.latitude);
        l1.setLongitude(my_latlong.longitude);

        Location l2 = new Location("Two");
        l2.setLatitude(frnd_latlong.latitude);
        l2.setLongitude(frnd_latlong.longitude);

        float distance = l1.distanceTo(l2);
        return distance;
    }
    public double CalculationByDistance(double initialLat, double initialLong, double finalLat, double finalLong){
        /*PRE: All the input values are in radians!*/
        double latDiff = finalLat - initialLat;
        double longDiff = finalLong - initialLong;
        double earthRadius = 6371; //In Km if you want the distance in km

        double distance = 2*earthRadius*Math.asin(Math.sqrt(Math.pow(Math.sin(latDiff/2.0),2)+Math.cos(initialLat)*Math.cos(finalLat)*Math.pow(Math.sin(longDiff/2),2)));

        return distance;

    }
    private void moveToCurrentLocation(GoogleMap mMap, LatLng currentLocation)
    {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(9), 3000, null);


    }
    @Override
    public void onInfoWindowClick(Marker marker) {
        mSelMarker=marker;
        ShowTujuan();
    }
    private void ShowTujuan(){
        if (mSelMarker == null) {
            Toast.makeText(this, "Pilih Penumpang !", Toast.LENGTH_LONG).show();
            return;
        }
        for(int i=0;i<arPenumpang.size();i++){
            if(arPenumpang.get(i).getPhone().equals(mSelMarker.getSnippet())){
                mSelIndex=i;
                mToLatEnd = (float) arPenumpang.get(i).getTo_lat();
                mToLngEnd = (float) arPenumpang.get(i).getTo_lng();
                mNoHpPenumpang = arPenumpang.get(i).getPhone();

                mPlaceTujuanText=AddressFromLatLng(new LatLng(mToLatEnd,mToLngEnd));

                String s="Nama: "+mSelMarker.getTitle()
                        + ", Hp: "+mNoHpPenumpang
                        + ", Tujuan: " + mPlaceTujuanText
                        + ", Pos: " + mToLatEnd +  "/" + mToLngEnd;
                Log.d(TAG,s);
                mSelNama.setText(s);
                saveSettingTujuan();

            }
        }


    }

    private void addMarkerMe(GoogleMap map, double lat, double lon,
                           String title, String snippet) {
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
//                .icon(BitmapDescriptorFactory.fromBitmap(mIconDriver))
                .snippet(snippet));
    }
    private void addMarkerPenumpang(
        GoogleMap map, double lat, double lon, String title, String snippet) {

        MarkerOptions marker=new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(mIconPeople))
                .draggable(false)
                .snippet(snippet);
        map.addMarker(marker);
    }
    private void addMarkerTo(
            GoogleMap map, double lat, double lon, String title, String snippet) {
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
                .draggable(false)
                .snippet(snippet));
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(getClass().getSimpleName(),
                String.format("%f:%f", location.getLatitude(),
                location.getLongitude()));

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //if( mAdapter != null )
        //    mAdapter.setGoogleApiClient( mGoogleApiClient );
        //getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }



    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        setupControls();

        stopTimer();

        mWebsite=getResources().getString(R.string.url_source);
        mSelNama= (TextView) findViewById(R.id.lblTujuan);
        mSelNama.setText("Ready");
        mIconDriver = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.driver);
        mIconPeople = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.people);

        loadSetting();
        setupButtonClick();

        if (readyToGo()) {
            MapFragment mapFrag=(MapFragment)getFragmentManager().findFragmentById(R.id.map);
            if (savedInstanceState == null) {
                needsInit=true;
            }
            mapFrag.getMapAsync(this);
            mGoogleApiClient = new GoogleApiClient
                    .Builder( this )
                    .enableAutoManage( this, 0, this )
                    .addApi( Places.GEO_DATA_API )
                    .addApi( Places.PLACE_DETECTION_API )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .build();

        }
    }
    private boolean sudahPilihPenumpang(){
        if (mNoHpPenumpang.isEmpty()) {
            Toast.makeText(getBaseContext(), "Pilih Penumpang !", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    private void setupControls(){
        //init control
        divChat=(LinearLayout)findViewById(R.id.divChat);
        divChat.setVisibility(View.GONE);
        lblChat=(TextView)findViewById(R.id.lblChat);
        lblChatEdit=(TextView)findViewById(R.id.lblText);
        btnStopTujuan = (Button) findViewById(R.id.btnStopTujuan);
        btnStart=(Button) findViewById(R.id.btnStartTujuan);
        btnCall=(Button) findViewById(R.id.btnCall);
        btnChat=(Button) findViewById(R.id.btnChat);
        lvPenumpang= (ListView) findViewById(R.id.lvPenumpang);
        btnRefresh=(Button) findViewById(R.id.btnRefesh);
        btnSiap=(Button) findViewById(R.id.btnSiap);
        btnSend =(ImageButton) findViewById(R.id.btnSend);
        btnStop =(Button) findViewById(R.id.btnStop);

    }
    private void setupButtonClick(){

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRefresh.setVisibility(View.GONE);
                mMode = MODE_CARI;
                startTimer();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTimer();
                map.clear();
                addMarkerMe(map, myLatLng.latitude, myLatLng.longitude,"Me", mNama);
                btnStop.setVisibility(View.GONE);
                Button btnStartTujuan=(Button)findViewById(R.id.btnStartTujuan);
                btnStartTujuan.setVisibility(View.VISIBLE);
                Toast.makeText(getBaseContext(),"Silahkan naikan penumpang, klik tombol Start " +
                        " apabila sudah mulai mengantarkan ke tempat tujuan.",Toast.LENGTH_LONG).show();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        btnSiap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( !sudahPilihPenumpang()) return;
                ambilPenumpang(view);
            }
        });

        lvPenumpang.setVisibility(View.GONE);
        lvPenumpang.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LatLng loc = new LatLng(arPenumpang.get(i).getLat(),arPenumpang.get(i).getLng());
                moveToCurrentLocation(map,loc);
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!sudahPilihPenumpang()) return;
                if(divChat.getVisibility()==View.GONE){
                    _chat=new ChatModel(getBaseContext(),mNomorHp,mNoHpPenumpang);
                    divChat.setVisibility(View.VISIBLE);
                } else {
                    divChat.setVisibility(View.GONE);
                }
            }
        });
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!sudahPilihPenumpang())return;
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mNoHpPenumpang));
                startActivity(intent);
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!sudahPilihPenumpang())return;
                //mulai antar penumpang ke tujuan
                mMode = MODE_ANTAR;
                startTimer();
                btnStart.setVisibility(View.GONE);
                btnStopTujuan.setVisibility(View.VISIBLE);
            }
        });
        btnStopTujuan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( !sudahPilihPenumpang()) return;
                finishOrder();
            }
        });

    }

    private void ambilPenumpang(View view) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Konfimasi")
                .setMessage("Ambil penumpang ini ? " + mSelMarker.getTitle())
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if ( acceptOrder() ){
                                    Toast.makeText(getBaseContext(),
                                            "Data kesiapan anda sudah masuk server, silahkan jemput." +
                                                    "Apabila sudah ditempat klik tombol Stop diatas.",
                                            Toast.LENGTH_LONG).show();
                                    btnRefresh.setVisibility(View.INVISIBLE);
                                    btnSiap.setVisibility(View.INVISIBLE);
                                    btnStop.setVisibility(View.VISIBLE);
                                    mMode = MODE_JEMPUT;    //driver sedang menuju penumpang
                                    DrawRoute(myLatLng,mSelMarker.getPosition());
                                    saveSettingTujuan();
                                } else {
                                    Toast.makeText(getBaseContext(),"Gagal kirim, coba lagi !",Toast.LENGTH_LONG).show();
                                };
                            }
                        })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void finishOrder(){
        Penumpang p=new Penumpang(getBaseContext());
        if ( p.FinishOrder(mNoHpPenumpang) ) {
            mMode = MODE_CARI;
            mMode3Init=false;
            mSelMarker=null; //reset
            startTimer();
            Button btnStopTujuan=(Button)findViewById(R.id.btnStopTujuan);
            btnStopTujuan.setVisibility(View.GONE);
            Button btnSiap=(Button)findViewById(R.id.btnSiap);
            btnSiap.setVisibility(View.VISIBLE);
            Toast.makeText(getBaseContext(),"Tugas anda sudah selesai, " +
                    "penghasilan sudah ditambahkan di dompet anda. " +
                    "Terimakasih.",Toast.LENGTH_LONG).show();
            resetSettingTujuan();

        } else {
            Toast.makeText(getBaseContext(),"Ada kesalahan tutup order ! " +
                    " Silahkan informasikan ke bagian admin " +
                    " untuk memastikan saldo dompet anda bertambah.",Toast.LENGTH_LONG).show();
        }
    }
    private void saveSettingTujuan(){
        //save to setting tujuan untuk menghindari buffer kosong
        SharedPreferences.Editor editor = mSetting.edit();
        editor.putFloat("tujuan_lat", mToLat);
        editor.putFloat("tujuan_lng", mToLng);
        editor.putString("tujuan_name", mPlaceTujuanText);
        editor.putString("no_hp_penumpang",mNoHpPenumpang);
        editor.putFloat("tujuan_lat_end", mToLatEnd);
        editor.putFloat("tujuan_lng_end", mToLngEnd);
        editor.putInt("mode", mMode);
        editor.commit();
    }
    private void resetSettingTujuan(){
        SharedPreferences.Editor editor = mSetting.edit();
        editor.putFloat("tujuan_lat", 0);
        editor.putFloat("tujuan_lng", 0);
        editor.putString("tujuan_name", "");
        editor.putInt("mode", mMode);
        editor.putString("no_hp_penumpang","");
        editor.putFloat("tujuan_lat_end", 0);
        editor.putFloat("tujuan_lng_end", 0);
        editor.commit();
    }
    private void loadSetting(){
        if ( mSetting == null) {
            mSetting = getSharedPreferences("setting_gojek", Context.MODE_PRIVATE);
        }
        mNama=mSetting.getString("nama", "Guest");
        mNomorHp=mSetting.getString("no_hp", "0000000000");
        mMode=mSetting.getInt("mode",0);
        mNoHpPenumpang=mSetting.getString("no_hp_penumpang",mNoHpPenumpang);
        mPlaceTujuanText=mSetting.getString("tujuan_nama","");
        mToLat=mSetting.getFloat("tujuan_lat",0);
        mToLng=mSetting.getFloat("tujuan_lng",0);
        mToLatEnd=mSetting.getFloat("tujuan_lat_end",0);
        mToLngEnd=mSetting.getFloat("tujuan_lng_end",0);


    }
    private void sendMessage() {
        _chat.send(lblChatEdit.getText().toString());
        lblChatEdit.setText("");

    }

    private void startTimer() {
        if (_timer == null) {
            _timer = new Timer();
        }
        _task = new TimerTask() {
            public void run() {
                _handler.post(new Runnable() {
                    public void run() {

                        Log.d("statTimer", "Timer set off with mode : "+mMode);
                        PushMyLatLng();
                        map.clear();

                        if( mMode == MODE_JEMPUT ) {
                            addMarkerMe(map, myLatLng.latitude, myLatLng.longitude, "Me", mNama);
                            addMarkerPenumpang(map, mToLat,mToLng,mPlaceTujuanText, mNoHpPenumpang);
                            DrawRoute(myLatLng, new LatLng(mToLat, mToLng));
                            DrawRoutePoly();

                        }  else if (mMode == MODE_ANTAR){

                            addMarkerMe(map, myLatLng.latitude, myLatLng.longitude, "Me", mNama);
                            addMarkerPenumpang(map, mToLatEnd, mToLngEnd,mNoHpPenumpang, mPlaceTujuanText);
                            //if(mMode3Init==false) {
                                DrawRoute(myLatLng, new LatLng(mToLatEnd, mToLngEnd));
                                mMode3Init=true;
                            //}
                            DrawRoutePoly();

                        } else {        // MODE_CARI
//                            displayPlaceChild();
                            addMarkerMe(map, myLatLng.latitude, myLatLng.longitude,"Me", mNama);
                            getCalonPenumpang();
                        }
                        if(divChat.getVisibility()==View.VISIBLE){
                            lblChat.setText(Html.fromHtml(_chat.refresh()));
                        }
                    }
                });
            }};


        _timer.schedule(_task, 300, 30000);
    }
    public void stopTimer() {
        if (_task != null) {
            Log.d("TIMER", "timer canceled");
            _timer.cancel();
            _timer=null;
        }
    }



    private boolean acceptOrder() {
        boolean ret=false;
        Penumpang penumpang = new Penumpang(this);
        mNoHpPenumpang = mSelMarker.getSnippet();
        if (penumpang.AcceptOrder(mNomorHp, mNoHpPenumpang)){
            ret=true;
        }
        return ret;
    }
    private void getLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        if(locationManager == null){
            Log.d(TAG,"locationManager == null");
        }
        try {


            location = locationManager.getLastKnownLocation(locationManager
                    .getBestProvider(criteria, false));
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    6000, 1, (LocationListener) this);

            if (location == null) { //gps provider error try passive
                location = locationManager.getLastKnownLocation(locationManager
                        .getBestProvider(criteria, false));
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        2000, 1, (LocationListener) this);
            }
            if (location == null) { //gps provider error try passive
                location = locationManager.getLastKnownLocation(locationManager
                        .getBestProvider(criteria, false));
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                        2000, 1, (LocationListener) this);
            }
            if (location == null) {
                if (map != null) {
                    location = map.getMyLocation();
                }
            }
        } catch(Exception e) {
            Toast.makeText(getBaseContext(),"GPS disconnected !" + e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if( mGoogleApiClient != null ) {
            try {
                mGoogleApiClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
    private void displayPlace( Place place ) {
        if(myLatLng == null || place == null){
            Log.d(TAG,"myLatLng null or place null, unknown Curent Location");
            return;
        }

        String content = "";
        if( !TextUtils.isEmpty( place.getName() ) ) {
            content += "Name: " + place.getName() + "\n";
        }
        if( !TextUtils.isEmpty( place.getAddress() ) ) {
            content += "Address: " + place.getAddress() + "\n";
        }
        if( !TextUtils.isEmpty( place.getPhoneNumber() ) ) {
            content += "Phone: " + place.getPhoneNumber();
        }
        if( !TextUtils.isEmpty( place.getLatLng().toString() ) ) {
            content += "LatLng: " + place.getLatLng().toString() + "\n";
        }
        //Log.d("content",content);

        //map.clear();
        //String curPlace= String.valueOf(Places.GeoDataApi.getPlaceById(mGoogleApiClient,myLatLng.toString()));
        //mAsal.setText(AddressFromLatLng(myLatLng));
        //mTujuan.setText(place.getAddress());

        mPlaceTujuan = place;
        mPlaceTujuanText = content;
        mToLat = (float) mPlaceTujuan.getLatLng().latitude;
        mToLng = (float) mPlaceTujuan.getLatLng().longitude;

        saveSettingTujuan();

//        displayPlaceChild();

        DrawRoute(myLatLng,place.getLatLng());

        myLocation();
    }
    private void displayPlaceChildx(){
        float lat = mSetting.getFloat("tujuan_lat", 0);
        float lng=mSetting.getFloat("tujuan_lng",0);
        String tname=mSetting.getString("tujuan_name","");
        addMarkerTo(map, lat, lng,tname, mPlaceTujuanText);
    }

    private String AddressFromLatLng(LatLng ll) {
        Geocoder geocoder;

        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        String address="";
        try {
            addresses = geocoder.getFromLocation(ll.latitude, ll.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            if(addresses.size()>0) {
                address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return address;
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if( requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK ) {
            mPlaceTujuan=PlacePicker.getPlace( data, this );
            displayPlace( mPlaceTujuan );
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }
}
