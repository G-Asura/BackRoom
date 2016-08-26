/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyway.backroom.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.anyway.backroom.MainActivity;
import com.anyway.backroom.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends ListFragment implements PeerListListener {

	private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
	View mContentView = null;
	private WifiP2pDevice device;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		mContentView = inflater.inflate(R.layout.fragment_device_list, null);
		return mContentView;
	}

	public WifiP2pDevice getDevice()
	{
		return device;
	}

	private static String getDeviceStatus(int deviceStatus)
	{
		Log.d(MainActivity.TAG, "Peer status :" + deviceStatus);
		switch (deviceStatus)
		{
		case WifiP2pDevice.AVAILABLE:
			return "Available";
		case WifiP2pDevice.INVITED:
			return "Invited";
		case WifiP2pDevice.CONNECTED:
			return "Connected";
		case WifiP2pDevice.FAILED:
			return "Failed";
		case WifiP2pDevice.UNAVAILABLE:
			return "Unavailable";
		default:
			return "Unknown";

		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		final WifiP2pDevice dev = (WifiP2pDevice) getListAdapter().getItem(position);
		Dialog dialog = new AlertDialog.Builder(this.getActivity()).setIcon(android.R.drawable.btn_dialog).setTitle(
				"详细信息").setMessage(dev.toString()).setPositiveButton(
				"开始聊天",new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						WifiP2pConfig config = new WifiP2pConfig();
						config.deviceAddress = dev.deviceAddress;
						config.wps.setup = WpsInfo.PBC;
						((DeviceActionListener) getActivity()).connect(config);
					}
				}).setNegativeButton("返回列表", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
		}).create();
		dialog.show();
	}

	private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice>
	{

		private List<WifiP2pDevice> items;

		/**
		 * @param context
		 * @param textViewResourceId
		 * @param objects
		 */
		public WiFiPeerListAdapter(Context context, int textViewResourceId, List<WifiP2pDevice> objects)
		{
			super(context, textViewResourceId, objects);
			items = objects;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = convertView;
			if (v == null)
			{
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row_devices, null);
			}
			WifiP2pDevice device = items.get(position);
			if (device != null)
			{
				TextView dev_name = (TextView) v.findViewById(R.id.device_name);
				TextView dev_status = (TextView) v.findViewById(R.id.device_status);
				if (dev_name != null)
				{
					dev_name.setText(device.deviceName);
				}
				if (dev_status != null)
				{
					dev_status.setText(getDeviceStatus(device.status));
				}
			}

			return v;

		}
	}

	public void updateThisDevice(WifiP2pDevice device)
	{
		this.device = device;
		TextView view = (TextView) mContentView.findViewById(R.id.my_name);
		view.setText(device.deviceName);
		view = (TextView) mContentView.findViewById(R.id.my_status);
		view.setText(getDeviceStatus(device.status));
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peerList)
	{
		peers.clear();
		peers.addAll(peerList.getDeviceList());
		((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
		if (peers.size() == 0)
		{
			Log.d(MainActivity.TAG, "No devices found");
			return;
		}

	}

	public void clearPeers()
	{
		peers.clear();
		((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	}

	/**
	 * An interface-callback for the activity to listen to fragment interaction
	 * events.
	 */
	public interface DeviceActionListener
	{

		void connect(WifiP2pConfig config);
		void disconnect();
	}

}
