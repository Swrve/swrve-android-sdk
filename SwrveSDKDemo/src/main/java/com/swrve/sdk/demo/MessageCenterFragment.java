package com.swrve.sdk.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.swrve.sdk.ISwrveResourcesListener;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.messaging.SwrveBaseCampaign;

import java.util.List;

public class MessageCenterFragment extends Fragment {

    private MessageCenterAdapter messageCenterAdapter;
    private ListView messageCenterList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frag_message_center, null);

        // Obtain latest campaigns for the user
        final List<SwrveBaseCampaign> messageCenterCampaigns = SwrveSDK.getInstance().getMessageCenterCampaigns();

        // Populate the list
        messageCenterAdapter = new MessageCenterAdapter(getContext(), R.layout.message_center_item, messageCenterCampaigns);
        messageCenterList = (ListView) v.findViewById(R.id.message_center_list);
        messageCenterList.setAdapter(messageCenterAdapter);
        // Display the campaign when an item is clicked
        messageCenterList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SwrveSDK.getInstance().showMessageCenterCampaign(messageCenterCampaigns.get(i));
                // Notify the adapter
                updateList();
            }
        });

        // Subscribe to the user resources and campaigns lister.
        // It will be triggered when new campaigns have been downloaded.
        SwrveSDK.getInstance().setResourcesListener(new ISwrveResourcesListener() {
            @Override
            public void onResourcesUpdated() {
                // Notify the adapter
                updateList();
            }
        });

        return v;
    }

    private void updateList() {
        messageCenterAdapter.updateItems(SwrveSDK.getInstance().getMessageCenterCampaigns());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the list of campaigns as it means that we are back from displaying are message
        // or there might be new content
        updateList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unsubscribe to the resource and campaigns listener
        SwrveSDK.getInstance().setResourcesListener(null);
    }

    private class MessageCenterAdapter extends ArrayAdapter<SwrveBaseCampaign> {

        private final int itemResourceId;
        private final List<SwrveBaseCampaign> items;

        public MessageCenterAdapter(Context context, int itemResourceId, List<SwrveBaseCampaign> items) {
            super(context, itemResourceId, items);
            this.itemResourceId = itemResourceId;
            this.items = items;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(itemResourceId, parent, false);
            TextView subjectTextView = (TextView) rowView.findViewById(R.id.campaign_subject);
            Button deleteButton = (Button) rowView.findViewById(R.id.delete_button);
            ImageView statusImageView = (ImageView) rowView.findViewById(R.id.campaign_status_icon);

            final SwrveBaseCampaign item = getItem(position);

            // Subject
            subjectTextView.setText(item.getSubject());

            // Status icon
            switch (item.getStatus()) {
                case Unseen:
                    statusImageView.setImageResource(android.R.drawable.presence_invisible);
                    break;
                case Seen:
                    statusImageView.setImageResource(android.R.drawable.presence_online);
                    break;
            }

            // Delete button
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Delete")
                            .setMessage("Are you sure you want to delete this message from your inbox?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // This user won't see this campaign again
                                    SwrveSDK.getInstance().removeMessageCenterCampaign(item);
                                    // Notify the adapter
                                    updateList();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });

            return rowView;
        }

        public void updateItems(List<SwrveBaseCampaign> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }
    }
}
