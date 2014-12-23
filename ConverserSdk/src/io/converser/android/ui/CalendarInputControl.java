package io.converser.android.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import io.converser.android.ConverserOptions;
import io.converser.android.R;
import io.converser.android.model.CalendarInput;

public class CalendarInputControl extends LinearLayout implements ConverserInput {

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    private CalendarInput model;
    private GregorianCalendar selectedDate;

    public CalendarInputControl(Context context) {
        super(context);
    }

    public CalendarInputControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CalendarInputControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final android.widget.Button calButton = (android.widget.Button) findViewById(R.id.cio__calControlButton);

        calButton.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {

                Calendar c = selectedDate;

                if (c == null) {
                    c = new GregorianCalendar();
                }

                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);


                // Create a new instance of DatePickerDialog and return it
                DatePickerDialog dpd = new DatePickerDialog(getContext(), new OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {

                        selectedDate = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                        calButton.setText(sdf.format(selectedDate.getTime()));

                        validateDate();

                    }


                }, year, month, day);


                if (Build.VERSION.SDK_INT >= 11) {
                    dpd.getDatePicker().setCalendarViewShown(false);
                    dpd.getDatePicker().setSpinnersShown(true);


                    dpd.getDatePicker().setMinDate(c.getTime().getTime());

                    if (model.getStart() != null) {
                        dpd.getDatePicker().setMinDate(model.getStart().getTime());
                    }

                    if (model.getEnd() != null) {
                        dpd.getDatePicker().setMaxDate(model.getEnd().getTime());
                    }

                }
                dpd.show();
            }
        });

    }

    public void init() {
        final android.widget.TextView descLabel = (android.widget.TextView) findViewById(R.id.cio__calControlDesc);
        descLabel.setText(model.getDescription());
    }

    public CalendarInput getModel() {
        return model;
    }

    public void setModel(CalendarInput model) {
        this.model = model;
        init();
    }

    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {
        if (model != null && selectedDate != null) {
            dataMap.put(model.getTag(), sdf.format(selectedDate.getTime()));
        }

    }

    private void validateDate() {

        Calendar c = new GregorianCalendar();


        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        GregorianCalendar today = new GregorianCalendar(year, month, day);

        String msg = null;
        if (model.getStart() != null) {
            //Check and make sure that the selected date is >= start
            if (selectedDate.getTime().compareTo(model.getStart()) < 0) {
                msg = "Please select a date no sooner than " + model.getStart().toGMTString();
            }
        } else {
            if (selectedDate.compareTo(today) < 0) {
                msg = "Please select a date no sooner than today";
            }
        }

        if (model.getEnd() != null) {
            //Check and make sure that the selected date is >= start
            if (selectedDate.getTime().compareTo(model.getEnd()) > 0) {
                msg = "Please select a date no later than " + model.getEnd().toGMTString();
            }
        }

        //Ok, some weird rules now, weekends?

        if (!ConverserOptions.getInstance().isAllowedPickWeekends()) {
            int dow = selectedDate.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                msg = getContext().getString(R.string.cio__weekend_warning);
            }
        }

        if (msg != null) {

            selectedDate = today;
            final AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
            adb.setMessage(msg);
            adb.setPositiveButton(android.R.string.ok, null);
            post(new Runnable() {

                @Override
                public void run() {
                    adb.show();
                }
            });
        }

    }

    @Override
    public String validate() {

        if (model.isOptional()) {
            return null;
        }

        if (selectedDate == null) {
            return "Please Select a date";
        }

        Calendar c = new GregorianCalendar();


        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        GregorianCalendar today = new GregorianCalendar(year, month, day);

        if (model.getStart() != null) {
            //Check and make sure that the selected date is >= start
            if (selectedDate.getTime().compareTo(model.getStart()) < 0) {
                return "Please select a date no sooner than " + model.getStart().toGMTString();
            }
        } else {
            if (selectedDate.compareTo(today) < 0) {
                return "Please select a date no sooner than today";
            }
        }

        if (model.getEnd() != null) {
            //Check and make sure that the selected date is >= start
            if (selectedDate.getTime().compareTo(model.getEnd()) > 0) {
                return "Please select a date no later than " + model.getEnd().toGMTString();
            }
        }

        //Ok, some weird rules now, weekends?

        if (!ConverserOptions.getInstance().isAllowedPickWeekends()) {
            int dow = selectedDate.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                return getContext().getString(R.string.cio__weekend_warning);
            }
        }

        return null;
    }

}
