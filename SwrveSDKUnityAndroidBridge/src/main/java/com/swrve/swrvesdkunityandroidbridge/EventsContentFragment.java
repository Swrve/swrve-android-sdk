package com.swrve.swrvesdkunityandroidbridge;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveSDK;

public class EventsContentFragment extends Fragment implements View.OnClickListener
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.frag_events, null);
        Button btnQueueEvent = (Button) v.findViewById(R.id.btnQueueEvent);
        btnQueueEvent.setOnClickListener(this);
        Button btnAction = (Button) v.findViewById(R.id.btnAction);
        btnAction.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if(id == R.id.btnQueueEvent)
        {
            btnQueueEvent();
        }
        else if(id == R.id.btnAction)
        {
            btnAction();
        }
    }

    private void btnQueueEvent() {
        EditText editText = (EditText) getView().findViewById(R.id.eventName);
        String eventName = editText.getText().toString();
        if(eventName.length()>0) {
            SwrveSDK.event(eventName);
            Snackbar.make(getView(), eventName + " queued", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void btnAction() {
        RadioGroup radioGroup = (RadioGroup) getView().findViewById(R.id.radioGroupActions);
        int selectedId = radioGroup.getCheckedRadioButtonId();
        View radioButton = radioGroup.findViewById(selectedId);
        int radioId = radioGroup.indexOfChild(radioButton);
        int action = ((RadioButton) radioGroup.getChildAt(radioId)).getId();

        if(action == R.id.radioPurchaseEvent) {
            SwrveSDK.purchase("BANANA_PACK", "gold", 120, 99);
            Snackbar.make(getView(), "Purchase BANANA_PACK, gold, 120 cost, 99 quantity", Snackbar.LENGTH_SHORT).show();
        } else if(action == R.id.radioCurrencyGiven) {
            SwrveSDK.currencyGiven("gold", 99);
            Snackbar.make(getView(), "99 gold currency given", Snackbar.LENGTH_SHORT).show();
        } else if(action == R.id.radioIap) {
            SwrveIAPRewards rewards = new SwrveIAPRewards("gold", 200);
            SwrveSDK.iap(1, "CURRENCY_PACK", 9.99, "USD", rewards);
            Snackbar.make(getView(), "In App Purchase", Snackbar.LENGTH_SHORT).show();
        } else if(action == R.id.radioSendQueue) {
            SwrveSDK.sendQueuedEvents();
            Snackbar.make(getView(), "Queued events sent", Snackbar.LENGTH_SHORT).show();
        }
    }
}

