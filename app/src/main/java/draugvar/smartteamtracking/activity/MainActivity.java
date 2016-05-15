package draugvar.smartteamtracking.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import draugvar.smartteamtracking.R;
import draugvar.smartteamtracking.adapter.GroupItem;
import draugvar.smartteamtracking.data.Beacon;
import draugvar.smartteamtracking.data.Group;
import draugvar.smartteamtracking.data.Myself;
import draugvar.smartteamtracking.data.User;
import draugvar.smartteamtracking.rest.AddInRange;
import draugvar.smartteamtracking.rest.CreateGroup;
import draugvar.smartteamtracking.rest.GetBeacon;
import draugvar.smartteamtracking.rest.GetGroupCount;
import draugvar.smartteamtracking.rest.GetUsers;
import draugvar.smartteamtracking.rest.InviteUsersToGroup;
import draugvar.smartteamtracking.singleton.WorkflowManager;
import io.realm.Realm;

public class MainActivity extends AppCompatActivity {
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.d("LoginTask","Inside onCreate of MainActivity");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),FriendsActivity.class);
                startActivity(intent);
            }
        });
        //create our FastAdapter which will manage everything
        final FastItemAdapter fastAdapter = new FastItemAdapter();

        //set our adapters to the RecyclerView
        //we wrap our FastAdapter inside the ItemAdapter -> This allows us to chain adapters for more complex useCases
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.group_recycler_view);
        assert recyclerView != null;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fastAdapter);

        // groups population
        realm = Realm.getDefaultInstance();
        for (Group group : realm.where(Group.class).findAll()) {
            GroupItem groupItem = new GroupItem(group);
            fastAdapter.add(groupItem);
        }
        // ----- FASTADAPTER -- OnLongCLickListener -----
        fastAdapter.withOnLongClickListener(new FastAdapter.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v, IAdapter adapter, IItem item, final int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

                builder.setTitle("Delete this group");
                builder.setMessage("Are you sure?");

                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing but close the dialog
                        GroupItem groupItem = (GroupItem) fastAdapter.getAdapterItem(position);
                        fastAdapter.remove(position);
                        realm.beginTransaction();
                        realm.where(Group.class).equalTo("gid", groupItem.group.getGid()).findAll()
                                .deleteFirstFromRealm();
                        realm.commitTransaction();
                        dialog.dismiss();
                    }

                });

                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });
        // ----- FASTADAPTER -- OnCLickListener ------
        fastAdapter.withOnClickListener(new FastAdapter.OnClickListener() {
            @Override
            public boolean onClick(View v, IAdapter adapter, IItem item, int position) {
                GroupItem groupItem = (GroupItem) item;
                Intent intent = new Intent(getApplicationContext(), GroupActivity.class);
                intent.putExtra("gid", groupItem.group.getGid());
                startActivity(intent);
                return true;
            }
        });
        Myself myself = realm.where(Myself.class).findFirst();
        Log.d("MainActivity",myself.getUser().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setBeaconManager(){
        final BeaconManager beaconManager;
        // ----- Estimote Beacon set-up ----- //
        beaconManager = new BeaconManager(getApplicationContext());

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<com.estimote.sdk.Beacon> list) {
                /*showNotification(
                        "Your gate closes in 47 minutes.",
                        "Current security wait time is 15 minutes, "
                                + "and it's a 5 minute walk from security to the gate. "
                                + "Looks like you've got plenty of time!");*/
                for(com.estimote.sdk.Beacon beacon: list){
                    Log.d("ESTIMOTE", beacon.getProximityUUID().toString() + " " + beacon.getMajor()
                            + " " +  beacon.getMinor());
                }
            }
            @Override
            public void onExitedRegion(Region region) {
                // could add an "exit" notification too if you want (-:
            }
        });

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(new Region(
                        "monitored region",
                        null, // UUID
                        null, null)); // Major, Minor
            }
        });
    }
}