package com.anyway.backroom;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.anyway.backroom.broadcastReceiver.BackRoomBroadcastReceiver;
import com.anyway.backroom.fragment.DeviceListFragment;
import com.anyway.backroom.fragment.DeviceListFragment.DeviceActionListener;
import com.anyway.backroom.fragment.WiFiChatFragment;
import com.anyway.backroom.fragment.WiFiChatFragment.MessageTarget;
import com.anyway.backroom.thread.ChatManager;
import com.anyway.backroom.thread.ClientSocketHandler;
import com.anyway.backroom.thread.GroupOwnerSocketHandler;
import android.os.Handler.Callback;

import java.io.IOException;


public class MainActivity extends Activity implements ChannelListener,Callback, MessageTarget,DeviceActionListener,ConnectionInfoListener {

    public static final String TAG = "backroom";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    public static final int SERVER_PORT = 4545;
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private DeviceListFragment listFragment;
    private WiFiChatFragment chatFragment;
    private Handler handler = new Handler(this);
    public boolean isChat = false;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled)
    {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        if (getFragmentManager().findFragmentByTag("deviceList") == null){
            listFragment = new DeviceListFragment();
            getFragmentManager().beginTransaction().add(R.id.main_layout,listFragment,"deviceList").commit();
        }

        discorverPeers();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        receiver = new BackRoomBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            if (manager != null && channel != null)
            {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } else
            {
                Log.e(TAG, "channel or manager is null");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void resetData()
    {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentByTag("deviceList");

        if (fragmentList != null)
        {
            fragmentList.clearPeers();
        }
    }



    @Override
    public void connect(WifiP2pConfig config)
    {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect()
    {
        if(manager != null && channel != null){
            manager.removeGroup(channel, new ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

                }

                @Override
                public void onSuccess() {

                }

            });
        }

    }

    @Override
    public void onChannelDisconnected()
    {
        if (manager != null && !retryChannel)
        {
            Toast.makeText(this, "断开连接！", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else
        {
            Toast.makeText(
                    this,
                    "WiFi没有开启，请尝试打开WiFi",
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                chatFragment.pushMessage("G: " + readMessage);
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                chatFragment.setChatManager((ChatManager) obj);

        }
        return true;
    }

    @Override
    public Handler getHandler() {
        return this.handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Thread thread = null;
        if (info.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                thread = new GroupOwnerSocketHandler(((MessageTarget) this).getHandler());
                thread.start();
            } catch (IOException e) {
                Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            thread = new ClientSocketHandler(((MessageTarget) this).getHandler(), info.groupOwnerAddress);
            thread.start();
        }
        chatFragment = new WiFiChatFragment();
        getFragmentManager().beginTransaction().replace(R.id.main_layout, chatFragment, "chatFragment").commit();
        isChat = true;
    }

    public void turnBack(){
        chatFragment = (WiFiChatFragment) getFragmentManager().findFragmentByTag("chatFragment");
        if (chatFragment != null){
            getFragmentManager().beginTransaction().replace(R.id.main_layout, listFragment, "deviceList").commit();
            isChat = false;
        }
        discorverPeers();
    }

    public void discorverPeers(){
        if (manager != null && channel != null){
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "正在搜索附近设备....", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(MainActivity.this, "搜索失败 : " + reasonCode, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
