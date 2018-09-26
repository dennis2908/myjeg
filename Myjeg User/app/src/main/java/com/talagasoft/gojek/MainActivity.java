package com.talagasoft.gojek;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.talagasoft.gojek.model.Deposit;
import com.talagasoft.gojek.model.SettingServer;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener  {

    SharedPreferences mSetting = null;
    Boolean mLoggedIn=false;
    String mNama="";
    String mNoHp="";
    TextView mDeposit;
    TextView mPoint;
    SettingServer mSetServer;

    // Connection detector class
    ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mSetting = getSharedPreferences(getResources().getString(R.string.setting), Context.MODE_WORLD_READABLE);
        mSetServer=new SettingServer(this);
        int nTarif=mSetServer.tarif();

        mLoggedIn = mSetting.getBoolean("logged_in", false);
        mNama = mSetting.getString("nama", "Guest");
        mNoHp = mSetting.getString("no_hp", "0000");
        int mDepo=new Deposit(this).Saldo(mNoHp);

        SharedPreferences.Editor editor = mSetting.edit();
        editor.putString("deposit", String.valueOf(mDepo));
        editor.putInt("tarif",nTarif);
        editor.commit();


        Double o = Double.parseDouble(mSetting.getString("deposit","0"));
        DecimalFormat df = new DecimalFormat("###,###.##"); // or pattern "###,###.##$"

        mDeposit= (TextView) findViewById(R.id.deposit);

        mDeposit.setText(df.format(o));
        this.setTitle("Hai " + mNama);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading Panggil Tukang Ojek", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //mTextView = (TextView) findViewById( R.id.textview );
        cd = new ConnectionDetector(getApplicationContext());
        // flag for Internet connection status
        Boolean isInternetPresent = false;
        // Check if Internet present
        isInternetPresent = cd.isConnectingToInternet();
        if (!isInternetPresent) {
            // Internet Connection is not present
            alert.showAlertDialog(this, "Internet Connection Error","Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        callMenu(id);
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        mNama = mSetting.getString("nama", "Guest");
        this.setTitle("Hai " + mNama);

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        callMenu(id);


        return true;
    }
    private void callMenu(int id){
        Intent intent=null;
        if (id == R.id.nav_antar || id == R.id.cmdAntar) {
            intent = new Intent("com.talagasoft.gojek.MapsActivity");
            intent.putExtra("no_hp", mNoHp);
            intent.putExtra("nama", mNama);
            intent.putExtra("jenis","antar");
            startActivity(intent);

        } else if (id == R.id.nav_jemput || id == R.id.cmdJemput) {
            intent = new Intent("com.talagasoft.gojek.JemputActivity");
            intent.putExtra("no_hp", mNoHp);
            intent.putExtra("nama", mNama);
            intent.putExtra("jenis","jemput");
            startActivity(intent);

        } else if (id == R.id.nav_deposit || id == R.id.cmdDeposit) {
            intent = new Intent("com.talagasoft.gojek.DepositActivity");
            intent.putExtra("no_hp", mNoHp);
            intent.putExtra("nama", mNama);
            intent.putExtra("jenis","argo");
            startActivity(intent);

        } else if (id == R.id.nav_daftar) {

        } else if (id == R.id.nav_argo || id == R.id.cmdArgo) {
            intent = new Intent("com.talagasoft.gojek.MapsActivity");
            intent.putExtra("no_hp", mNoHp);
            intent.putExtra("nama", mNama);
            intent.putExtra("jenis","argo");
            startActivity(intent);

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }else if (id == R.id.action_settings) {
            intent = new Intent("com.talagasoft.gojek.AccountActivity");
            intent.putExtra("no_hp", mNoHp);
            intent.putExtra("nama", mNama);
            intent.putExtra("jenis","argo");
            startActivity(intent);
        }else if (id == R.id.cmdHistory) {
            intent = new Intent("com.talagasoft.gojek.DompetActivity");
            intent.putExtra("no_hp", mNoHp);
            intent.putExtra("nama", mNama);
            startActivity(intent);

        } else if (id == R.id.action_logout ||  id == R.id.cmdLogout) {

            SharedPreferences.Editor editor = mSetting.edit();
            //Adding values to editor
            editor.putBoolean("logged_in", false);
            editor.putString("no_hp", "0000");
            editor.putString("nama", "Guest");

            //Saving values to editor
            editor.commit();
            Toast.makeText(this, "Anda sudah logout, terimakasih. ", Toast.LENGTH_LONG);
            //alert.showAlertDialog(this, "Berhasil logout.","Silahkan dijalankan lagi aplikasi dan masukkan user baru anda.", false ) ;
            restart(this, 2);
        }

    }
    @Override
    public void onClick(View view) {
        callMenu(view.getId());
    }
    public static void restart(Context context, int delay) {
        if (delay == 0) {
            delay = 1;
        }
        Log.e("", "restarting app");
        Intent restartIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName() );
        PendingIntent intent = PendingIntent.getActivity(
                context, 0,
                restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + delay, intent);
        System.exit(2);
    }

}
