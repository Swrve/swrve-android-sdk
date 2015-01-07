package io.converser.android.ui;

import android.app.ProgressDialog;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import io.converser.android.engine.ConverserEngine;
import io.converser.android.R;
import io.converser.android.engine.model.ConversationItem;
import io.converser.android.engine.model.Conversations;

public class ConversationListFragment extends Fragment implements OnItemClickListener {

    private ConverserEngine converserEngine;
    private Conversations conversations;
    private OnConversationClickedListener mConversationClickedListener;
    private ProgressDialog progressDialog;

    public static ConversationListFragment create() {
        return new ConversationListFragment();
    }

    public void setConversationClickedListener(OnConversationClickedListener listener) {
        this.mConversationClickedListener = listener;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof OnConversationClickedListener) {
            setConversationClickedListener((OnConversationClickedListener) getActivity());
        }

        setHasOptionsMenu(true);

        ListView lv = (ListView) getView().findViewById(R.id.cio__lv_inbox);
        registerForContextMenu(lv);

        lv.setEmptyView(getView().findViewById(R.id.cio__inbox_empty_notice));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.cio__conversation_list_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.cio__mi_refresh) {
            beginFetch();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void refresh() {
        beginFetch();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {

        if (v.getId() == R.id.cio__lv_inbox) {
            getActivity().getMenuInflater().inflate(R.menu.cio__conversation_list_context, menu);
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.cio__inbox_fragment, container, false);
    }

    private void beginFetch() {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.cio__refreshing));
        progressDialog.show();

        if (getView() != null) {
            //Before fetching, hide the tip. it'll be shown again if there is items
            android.widget.TextView cio__tvHelpText = (android.widget.TextView) getView().findViewById(R.id.cio__tvHelpText);
            cio__tvHelpText.setVisibility(View.INVISIBLE);
        }

        converserEngine.getConversations(new ConverserEngine.Callback<Conversations>() {

            @Override
            public void onSuccess(final Conversations response) {

                if (isDetached() || getActivity() == null) {
                    //this fragment is done for. dont try binding data or anything
                    return;
                }

                //Get back on the ui thread. there's a good chance we are off it from the engine processor
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (response != null) {
                            bindConversationData(response);

                            if (progressDialog != null) {
                                try {
                                    progressDialog.dismiss();
                                } finally {
                                }
                            }
                        } else {
                            Toast t = Toast.makeText(getActivity(), R.string.cio__error_retrieving_data, Toast.LENGTH_SHORT);
                            t.show();
                        }
                    }
                });

            }

            @Override
            public void onError(String error) {

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (progressDialog != null) {
                            try {
                                progressDialog.dismiss();
                            } finally {
                            }
                        }

                        Toast t = Toast.makeText(getActivity(), R.string.cio__error_retrieving_data, Toast.LENGTH_SHORT);
                        t.show();

                    }

                });
            }
        });
    }

    private void bindConversationData(Conversations conversations) {
        this.conversations = conversations;
        View view = getView();

        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(new int[]{R.attr.conversationListDecorIcon});
        int drawableId = ta.getResourceId(0, 0);
        ta.recycle();

        if (view != null) {
            ListView list = (ListView) view.findViewById(R.id.cio__lv_inbox);
            list.setAdapter(new ConversationsAdapter(drawableId));

            list.setOnItemClickListener(this);

            android.widget.TextView cio__tvHelpText = (android.widget.TextView) view.findViewById(R.id.cio__tvHelpText);
            if (conversations != null && conversations.getItems().size() > 0) {
                cio__tvHelpText.setVisibility(View.VISIBLE);
            } else {
                cio__tvHelpText.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        converserEngine.cancelOperations();
        converserEngine = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (converserEngine == null) {
            converserEngine = new ConverserEngine(getActivity().getApplicationContext());

        }
        if (conversations != null) {
            bindConversationData(conversations);
        } else {
            beginFetch();
        }

        getActivity().setTitle(R.string.cio__inbox);
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View row, int position, long id) {

        ConversationItem conversationItem = conversations.getItems().get(position);

        if (mConversationClickedListener != null) {
            mConversationClickedListener.onConversationItemClicked(conversationItem);
        }
    }

    public interface OnConversationClickedListener {
        void onConversationItemClicked(ConversationItem item);
    }

    private class ConversationsAdapter extends BaseAdapter {

        private int imageResourceId;
        private SimpleDateFormat sdf;

        public ConversationsAdapter(int decorImageId) {
            this.imageResourceId = decorImageId;
            sdf = new SimpleDateFormat("HH:mm dd MMM");
        }

        @Override
        public int getCount() {
            return conversations.getItems().size();
        }

        @Override
        public ConversationItem getItem(int position) {

            return conversations.getItems().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewGroup row = null;
            ConversationItem convItem = getItem(position);

            if (convertView != null) {
                row = (ViewGroup) convertView;
            } else {
                row = (ViewGroup) getLayoutInflater(null).inflate(R.layout.cio__inbox_row, parent, false);
            }

            android.widget.TextView header = (android.widget.TextView) row.findViewById(R.id.cio__tvConversationRowHeader);
            header.setText(convItem.getSubject());

            android.widget.TextView date = (android.widget.TextView) row.findViewById(R.id.cio__tvConversationRowDate);
            date.setText(sdf.format(convItem.getCreatedAt()));

            if (imageResourceId > 0) {
                android.widget.ImageView icon = (android.widget.ImageView) row.findViewById(R.id.cio__ivConversationRowIcon);
                icon.setImageResource(imageResourceId);
            }

            return row;
        }
    }


}
