package com.example.android.procon_kosen;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatePickerFragment extends DialogFragment {

    private EditText DateEditText;
    private Calendar mCal = Calendar.getInstance();

    public DatePickerFragment (EditText v) {
        this.DateEditText = v;
    }

    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener(){
        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {

            mCal.set(Calendar.YEAR, year);
            mCal.set(Calendar.MONTH, month);
            mCal.set(Calendar.DAY_OF_MONTH, day);

            String myFormat = "dd/MM/yyyy"; //In which you need put here
            SimpleDateFormat dateFormat = new SimpleDateFormat(myFormat, Locale.US);

            DateEditText.setText(dateFormat.format(mCal.getTime()));

        }
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), date, year, month, day);
    }

}