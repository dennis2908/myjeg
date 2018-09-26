package com.talagasoft.gojek;

import android.app.Fragment;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.talagasoft.gojek.model.ChatModel;
import com.talagasoft.gojek.model.Driver;
import com.talagasoft.gojek.model.HttpXml;
import com.talagasoft.gojek.model.User;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends  AbstractMapActivity
        implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
        LocationListener,  GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleMap.OnMapClickListener {

    // global variabel

    String TAG="MapsActivity";

    SharedPreferences mSetting;
    boolean needsInit=false;

    // gooogle api
    Location location;
    LocationManager locationManager;
    GoogleMap map;
    Criteria criteria;
    private GoogleApiClient mGoogleApiClient;
    private int PLACE_PICKER_REQUEST = 1;
    String mWebsite="",mJenis="";

    // penumpang variabel
    String mNomorHp, mNama,mPlaceTujuanText;
    float mToLat,mToLng;
    LatLng myLatLng;
    private AutoCompleteAdapter mAdapter;
    private AutoCompleteTextView mPredictTextView;
    TextView mJarak,mAsal,mTujuan,mOngkos,mDeposit;
    int mOngkosVal=0,mDepositVal=0,mTarif=0;
    PolylineOptions mPolyline;
    private Place mPlaceTujuan;
    private float mJarakVal;
    private boolean mWaitingDriver;

    // driver variabel
    Bitmap mIconDriver,mIconDriverAccept;
    List<Driver> arrDriver = new ArrayList<Driver>();
    private boolean mDriverLagiNgantar;


    // handle timer schedule mulai jalan ke tujuan
    TimerTask _task;
    final Handler _handler = new Handler();
    Timer _timer;

    int mMode=0, MODE_CARI=0, MODE_DRIVER=1, MODE_ANTAR=2;
    //0 - lagi cari alamat tujuan, 1 - nunggu driver, 2 - lagi jalan sama driver cie-ciee
    // controls
    Button btnCall,btnStart,btnStop,btnRate,btnChat;
    Button btnTopUp,btnSubmit,btnRefresh;

    LinearLayout section_result;

    private User mMember, mDriver;

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }
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
        this.map=map1;
        map.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));
        map.setOnInfoWindowClickListener(this);

        map.setMyLocationEnabled(true);

        myLocation();

        mAdapter.setRadiusPlace(1,myLatLng); //set radius 10 km dari sekarang

        if (needsInit && myLatLng != null ) {

            CameraUpdate center=CameraUpdateFactory.newLatLng(myLatLng);
            CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
            map.moveCamera(center);
            map.animateCamera(zoom);

        }
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
    private void getDriverPosition(){
        if(mDriver != null){
            mDriver.loadByPhoneJob(mDriver.get_handphone(),"driver");
        }
    }
    private void getDriverCurrentLocation(){
        if(myLatLng == null ) return;
        arrDriver.clear();
        String mUrl= mWebsite + "drivers.php?hp="+mNomorHp+"&lat="+myLatLng.latitude+"&lng="+myLatLng.longitude;
        HttpXml web=new HttpXml(mUrl);
        web.getGroup("people");
        float lat,lng;
        String hp,nama,driverHp;
        int status;
        for (int i = 0; i < web.getCount(); i++) {
            lat = Float.parseFloat(web.getKeyIndex(i,"lat"));
            lng = Float.parseFloat(web.getKeyIndex(i,"lng"));
            hp = web.getKeyIndex(i,"handphone");
            nama = web.getKeyIndex(i,"user_name");
            status = Integer.parseInt(web.getKeyIndex(i,"status"));
            driverHp=web.getKeyIndex(i,"driver");

            Driver drv=new Driver(this);
                drv.set_user_name(nama);
                drv.set_handphone(hp);
                drv.set_lng(lng);
                drv.set_lat(lat);
            arrDriver.add( drv );

            if ( driverHp.contains(mNomorHp)  && status == MODE_ANTAR){    //2 - penumpang di accept oleh driver
                mDriver=new User(this);
                mDriver.loadByPhoneJob(hp,"driver");
                //save to setting tujuan untuk menghindari buffer kosong
                SharedPreferences.Editor editor = mSetting.edit();
                editor.putString("driver_handphone", hp);
                editor.putString("driver_name",nama);
                editor.commit();

            }

        }
    }
    public void addMarkerDrivers(){
        for(int i=0;i<arrDriver.size();i++) {
            Driver driver=arrDriver.get(i);
            addMarkerDriver(map, driver.get_lat(), driver.get_lng(),
                    driver.get_user_name(), driver.get_handphone());
            Log.d("addMarkerDrivers","Nama: "+driver.get_user_name()+", Pos: "+driver.get_lat()+"/"+driver.get_lng());
        }
    }
    public void myLocation(){

        getLocation();

        if(location==null){
            Log.d(TAG,"location==null");
            myLatLng=new LatLng(0,0);
        } else {
            myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        addMarkerAsal();
        moveCameraTo(map, myLatLng);
        Log.d("myLocation","Lat/Lng:"+myLatLng.latitude+"/"+myLatLng.longitude);

    }
    private void DrawRoutePoly(){
        if(mPolyline==null)return;
        Polyline line = map.addPolyline(mPolyline);
    }
    public void DrawRoute(LatLng from,LatLng to){

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

        mJarakVal=mDistance;
        mJarak.setText(""+mDistance+" KM");
        int o = (int) (mDistance * mTarif);   //ongkos 1000 tiap kilometer
        DecimalFormat df = new DecimalFormat("###,###.##"); // or pattern "###,###.##$"
        mOngkos.setText(df.format(o));
        mOngkosVal=o;

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
    private void moveCameraTo(GoogleMap mMap, LatLng currentLocation)
    {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    }
    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, marker.getTitle()+" - " + marker.getSnippet(), Toast.LENGTH_LONG).show();
    }

    private void addMarkerMe(GoogleMap map, double lat, double lon,
                           String title, String snippet) {
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
                //.icon(BitmapDescriptorFactory.fromBitmap(mIconDriver))
                //.draggable(true)
                .snippet(snippet));
    }
    private void addMarkerDriver(GoogleMap map, double lat, double lon,
                           String title, String snippet) {
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(mIconDriver))
                .draggable(true)
                .snippet(snippet));
    }
    private void addMarkerDriverAccept(GoogleMap map, double lat, double lon,
                                 String title, String snippet) {
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(mIconDriverAccept))
                .draggable(true)
                .snippet(snippet));
    }
    private void addMarkerTo(GoogleMap map, double lat, double lon,
                                 String title, String snippet) {
        map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(title)
                //.icon(BitmapDescriptorFactory.fromBitmap(R.drawable.places_ic_search))
                .draggable(true)
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
        if( mAdapter != null )
            mAdapter.setGoogleApiClient( mGoogleApiClient );


        getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    @Override
    public void onPause() {
        super.onPause();
        ////Toast.makeText(getApplicationContext(),"14. onPause()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //'Toast.makeText(getApplicationContext(),"16. onDestroy()", Toast.LENGTH_SHORT).show();
        stopTimer();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mWebsite=getResources().getString(R.string.url_source);

        mIconDriver = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.driver);   // where university is the icon name that is used as a marker.

        mIconDriverAccept = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.driver_accept);   // where university is the icon name that is used as a marker.

        if (getIntent().hasExtra("jenis")){
            mJenis=getIntent().getStringExtra("jenis");
        }
        mWaitingDriver = false;
        mDriver = null;
        mMember=null;

        stopTimer();

        initControls();
        btnChat=(Button) findViewById(R.id.btnChat);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startChat();
            }
        });

        mSetting = getSharedPreferences("setting_gojek", Context.MODE_PRIVATE);

        mNama=mSetting.getString("nama", "Guest");
        mNomorHp=mSetting.getString("no_hp", "0000000000");

        if(mSetting.getString("tujuan_nama","").isEmpty()==false){
                mToLat=mSetting.getFloat("tujuan_lat",0);
                mToLng=mSetting.getFloat("tujuan_lng",0);
                mPlaceTujuanText=mSetting.getString("tujuan_name","");
        }
        mMember=new User(this);
        mMember.loadByPhoneJob(mNomorHp,"penumpang");

        mTarif=mSetting.getInt("tarif",5000);

        mDepositVal = Integer.parseInt(mSetting.getString("deposit","0"));
        DecimalFormat df = new DecimalFormat("###,###.##"); // or pattern "###,###.##$"
        mDeposit.setText(df.format(mDepositVal));

        section_result.setVisibility(View.GONE);

        if (readyToGo()) {

            mAdapter = new AutoCompleteAdapter( this );
            mPredictTextView = (AutoCompleteTextView) findViewById( R.id.txtSearch);
            mPredictTextView.setAdapter( mAdapter );

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
        onClickControls();
    }
    private void onClickControls(){
        mPredictTextView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AutoCompletePlace place = (AutoCompletePlace) parent.getItemAtPosition( position );
                findPlaceById( place.getId() );
            }
        });
        btnTopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topUpSaldo();
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitTujuan();
            }
        });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshMap();
            }
        });
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callPhone();
            }
        });


        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAntar();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAntar();
            }
        });
        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateDriver();
            }
        });

    }
    private void startAntar(){

    }
    private void stopAntar(){

    }
    private void topUpSaldo(){
        Intent intent = new Intent("com.talagasoft.gojek.DepositActivity");
        intent.putExtra("no_hp", mNomorHp);
        intent.putExtra("nama", mNama);
        startActivity(intent);
    }
    private void callPhone(){
        if( mDriver == null ){
            Toast.makeText(getBaseContext(),"Belum ada driver yang accept untuk mulai chating!",Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mDriver.get_handphone()));
        startActivity(intent);
    }
    private void startChat(){
        if( mDriver == null ){
            Toast.makeText(getBaseContext(),"Belum ada driver yang accept untuk mulai chating!",Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(new Intent("com.talagasoft.gojek.ChatActivity"));

    }
    private void submitTujuan(){

        if(mDepositVal<mOngkosVal){
            Toast.makeText(getBaseContext(),"Deposit tidak mencukupi !",Toast.LENGTH_LONG).show();
            return;
        }
        if  ( SubmitOrder() ){
            LinearLayout divSearch=(LinearLayout) findViewById(R.id.divSearch);
            divSearch.setVisibility(View.GONE);
            LinearLayout divResult=(LinearLayout) findViewById(R.id.section_result);
            divResult.setVisibility(View.GONE);

            Toast.makeText(getBaseContext(),"Order sudah masuk, tunggu respon dari driver disekitar anda.",Toast.LENGTH_LONG).show();
            getDriverCurrentLocation();
            addMarkerDrivers();
            mWaitingDriver=true;
            mMode=1;        //waiting driver
            startTimer();

        } else {
            Toast.makeText(getBaseContext(),"Gagal submit order anda, coba lagi nanti",Toast.LENGTH_LONG).show();
        }

    }
    private void refreshMap(){
        map.clear();
        if(mWaitingDriver) {
            addMarkerAsal();
            addMarkerTujuan();
            DrawRoutePoly();
        }
        getDriverCurrentLocation();
        addMarkerDrivers();
    }
    private void initControls(){

        section_result = (LinearLayout) findViewById(R.id.section_result);

        mJarak = (TextView) findViewById(R.id.jarak) ;
        mAsal = (TextView) findViewById(R.id.asal) ;
        mTujuan = (TextView) findViewById(R.id.tujuan) ;
        mOngkos = (TextView) findViewById(R.id.ongkos) ;
        mDeposit=(TextView) findViewById(R.id.deposit);
        btnTopUp=(Button) findViewById(R.id.btnTopUp);
        btnSubmit=(Button) findViewById(R.id.btnSubmit);
        btnRefresh=(Button) findViewById(R.id.btnRefresh);
        btnCall=(Button) findViewById(R.id.btnCall);
        btnStart =(Button) findViewById(R.id.btnStart);
        btnStop =(Button) findViewById(R.id.btnStop);
        btnRate =(Button) findViewById(R.id.btnRate);

    }

    private boolean SubmitOrder() {
        boolean ret=false;
        Log.d(TAG,"SubmitOrder: mNomorHp="+mNomorHp+", from="+myLatLng.latitude+"/"+myLatLng.longitude
        +", to="+mToLat+"/"+mToLng+", Jarak="+mJarakVal);

        if (mMember.newOrder(mNomorHp,myLatLng,new LatLng(mToLat,mToLng),mJarakVal,mOngkosVal)){
            ret=true;
        }
        return ret;
    }


    private void getLocation() {
        try {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            criteria = new Criteria();
            if (locationManager == null) {
                Log.d(TAG, "locationManager == null");
            }

            location = locationManager.getLastKnownLocation(locationManager
                    .getBestProvider(criteria, false));
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    6000, 1, (LocationListener) this);
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
        } finally {
            Log.d("getLocation","Unable load getLcation()");
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
            mAdapter.setGoogleApiClient( null );
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void findPlaceById( String id ) {
        if( TextUtils.isEmpty( id ) || mGoogleApiClient == null || !mGoogleApiClient.isConnected() )
            return;

        Places.GeoDataApi.getPlaceById( mGoogleApiClient, id ) .setResultCallback( new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if( places.getStatus().isSuccess() ) {
                    Place place = places.get( 0 );
                    mPlaceTujuan=place;
                    displayPlace( place );
                    mPredictTextView.setText( "" );
                    mPredictTextView.clearFocus();
                    mAdapter.clear();
                    // Check if no view has focus:
                    if (mPredictTextView.hasFocus()) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mPredictTextView.getWindowToken(), 0);
                    }
                }
                //Release the PlaceBuffer to prevent a memory leak
                places.release();
            }
        } );
    }

    private void displayPlace( Place place ) {
        if(myLatLng==null){
            Toast.makeText(this,"Unknown Curent Location",Toast.LENGTH_LONG).show();
            return;
        }
        if( place == null )
            return;

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
        Log.d("content",content);

        //mCurrentPlace = String.valueOf(Places.GeoDataApi.getPlaceById(mGoogleApiClient,myLatLng.toString()));


        mAsal.setText(AddressFromLatLng(myLatLng));
        mTujuan.setText(place.getAddress());

        mPlaceTujuan=place;
        mPlaceTujuanText=content;

        //save to setting tujuan untuk menghindari buffer kosong
        SharedPreferences.Editor editor = mSetting.edit();
        editor.putFloat("tujuan_lat", (float) mPlaceTujuan.getLatLng().latitude);
        editor.putFloat("tujuan_lng", (float) mPlaceTujuan.getLatLng().longitude);
        editor.putString("tujuan_name", String.valueOf(mPlaceTujuan.getName()));
        editor.commit();

        map.clear();
        addMarkerAsal();
        DrawRoute(myLatLng,place.getLatLng());
        LinearLayout section_result= (LinearLayout) findViewById(R.id.section_result);
        section_result.setVisibility(View.VISIBLE);

        myLocation();
        startTimer();


    }
    private void addMarkerTujuan(){
        mToLat = mSetting.getFloat("tujuan_lat", 0);
        mToLng=mSetting.getFloat("tujuan_lng",0);
        String tname=mSetting.getString("tujuan_name","");
        addMarkerTo(map, mToLat, mToLng, tname, mPlaceTujuanText);
    }

    private String AddressFromLatLng(LatLng myLatLng) {
        Geocoder geocoder;

        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        String address="Current Location";
        try {
            addresses = geocoder.getFromLocation(myLatLng.latitude, myLatLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
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
    private void startTimer() {
        if (_timer == null) {
            _timer = new Timer();
            mDriverLagiNgantar=false;
        }
        _task = new TimerTask() {
            public void run() {
                _handler.post(new Runnable() {
                    public void run() {

                        Log.d("statTimer", "Timer set off with mode : "+mMode);

                        map.clear();

                        addMarkerAsal();                     //add my marker
                        addMarkerTujuan();                //add marker tujuan
                        DrawRoutePoly();                                                        //gambar rute

                        getDriverCurrentLocation(); //driver yg ada disekitaran
                        //dan driver yg siap ambil penumpang ini
                        addMarkerDrivers();

                        if( mWaitingDriver ) {
                            //mulai cari driver setelah submit
                            if( mDriver == null ) {
                                getDriverCurrentLocation(); //driver yg ada disekitaran
                                                            //dan driver yg siap ambil penumpang ini
                                addMarkerDrivers();
                                //cek ada driver yang accept ?
                                // mDriver = null ??
                            } else {
                                //ok sudah dapat driver ...
                                addMarkerDriverAccept(map, mDriver.get_lat(), mDriver.get_lng(),
                                        mDriver.get_user_name(), "Accepted By Driver");       //add marker driver saat ini
                                getDriverPosition();

                                if(mDriver.get_status()==2) {       //driver siapp !!
                                    mDriverLagiNgantar=true;
                                }
                            }
                        }
                        if( mDriver != null ) {
                            //apabila driver lagi nganter tetapi status lagi nyari lagi
                            //artinya sudah selesai antar
                            //stop timer dan close order
                            if (mDriver.get_status() == 1 && mDriverLagiNgantar == true) {
                                //1-driver lagi cari penumpang lagi
                                //stop transaction and close status and reset
                                stopTimer();
                                rateDriver();
                                mWaitingDriver = false;
                                SharedPreferences.Editor editor = mSetting.edit();
                                editor.putString("driver_handphone", "");
                                editor.putString("driver_name","");
                                editor.commit();
                                mDriver=null;
                            }

                        }
                    }
                });
            }};


        _timer.schedule(_task, 300, 30000);
    }

    private void addMarkerAsal() {
        if(mMember!=null){
            mMember.pushMyLatLng(mNomorHp,myLatLng.latitude,myLatLng.longitude);
            mMember.loadByPhoneJob(mNomorHp,"penumpang");
            addMarkerMe(map,myLatLng.latitude,myLatLng.longitude,mMember.get_user_name(),mMember.get_handphone());
        }
    }

    private void rateDriver() {
        //reset tujuan
        SharedPreferences.Editor editor = mSetting.edit();
        editor.putFloat("tujuan_lat", 0);
        editor.putFloat("tujuan_lng", 0);
        editor.putString("tujuan_name", "");
        editor.commit();

        startActivity(new Intent("com.talagasoft.gojek.RateDriverActivity"));
    }

    public void stopTimer() {
        if (_task != null) {
            Log.d("TIMER", "timer canceled");
            if(_timer != null ) _timer.cancel();
            _timer=null;
        }
    }
}
