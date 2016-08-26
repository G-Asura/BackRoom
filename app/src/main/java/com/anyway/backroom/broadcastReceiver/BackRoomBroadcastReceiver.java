package com.anyway.backroom.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import com.anyway.backroom.MainActivity;
import com.anyway.backroom.fragment.DeviceListFragment;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class BackRoomBroadcastReceiver extends BroadcastReceiver
{

	private WifiP2pManager manager;
	private Channel channel;
	private MainActivity activity;

	/**
	 * @param manager
	 *            WifiP2pManager system service
	 * @param channel
	 *            Wifi p2p channel
	 * @param activity
	 *            activity associated with the receiver
	 */
	public BackRoomBroadcastReceiver(WifiP2pManager manager, Channel channel,
			MainActivity activity)
	{
		super();
		this.manager = manager;
		this.channel = channel;
		this.activity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
		{
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
			{
				activity.setIsWifiP2pEnabled(true);
			} else
			{
				activity.setIsWifiP2pEnabled(false);
				activity.resetData();

			}
			Log.d(MainActivity.TAG, "P2P 状态发生改变 - " + state);
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
		{
			if (activity.isChat){
				activity.turnBack();
			}

			if (manager != null)
			{
				manager.requestPeers(channel, (PeerListListener) activity.getFragmentManager().findFragmentByTag("deviceList"));
			}
			Log.d(MainActivity.TAG, "附近设备有变化");
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action))
		{

			if (manager == null)
			{
				return;
			}

			NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

			if (networkInfo.isConnected())
			{
				manager.requestConnectionInfo(channel, activity);
			} else
			{
				activity.resetData();
			}
		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))
		{
			DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager().findFragmentByTag("deviceList");
			if (fragment != null && !activity.isChat){
				fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
			}
		}
	}
}
