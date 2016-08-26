package com.anyway.backroom.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.anyway.backroom.R;
import com.anyway.backroom.thread.ChatManager;

import java.util.ArrayList;
import java.util.List;

public class WiFiChatFragment extends Fragment {

    private View view;

    private ChatManager chatManager;
    private TextView chatLine;
    private ListView listView;
    ChatMessageAdapter adapter = null;
    private List<String> items = new ArrayList<String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_chat, container, false);
        chatLine = (TextView) view.findViewById(R.id.txtChatLine);
        listView = (ListView) view.findViewById(android.R.id.list);
        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1, items);
        listView.setAdapter(adapter);
        view.findViewById(R.id.btn_send).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        if (chatManager != null) {
                            chatManager.write(chatLine.getText().toString().getBytes());
                            pushMessage("S: " + chatLine.getText().toString());
                            chatLine.setText("");
                            chatLine.clearFocus();
                        }
                    }
                });
        view.findViewById(R.id.btn_back).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                }
        );
        return view;
    }

    public interface MessageTarget {
        public Handler getHandler();
    }

    public void setChatManager(ChatManager obj) {
        chatManager = obj;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void pushMessage(String readMessage) {
        adapter.add(readMessage);
        adapter.notifyDataSetChanged();
    }

    public class ChatMessageAdapter extends ArrayAdapter<String> {

        List<String> messages = null;

        public ChatMessageAdapter(Context context, int textViewResourceId, List<String> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_1, null);
            }
            String message = items.get(position);
            if (message != null && !message.isEmpty()) {
                TextView nameText = (TextView) v.findViewById(android.R.id.text1);

                if (nameText != null) {
                    nameText.setText(message);
                    if (message.startsWith("S: ")) {
                        nameText.setTextAppearance(getActivity(), R.style.normalText);
                    } else {
                        nameText.setTextAppearance(getActivity(), R.style.boldText);
                    }
                }
            }
            return v;
        }
    }
}
