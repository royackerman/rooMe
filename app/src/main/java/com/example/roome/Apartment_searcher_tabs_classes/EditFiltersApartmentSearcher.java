package com.example.roome.Apartment_searcher_tabs_classes;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.roome.ChoosingActivity;
import com.example.roome.FirebaseMediate;
import com.example.roome.MyPreferences;
import com.example.roome.R;
import com.example.roome.SignInActivity;
import com.example.roome.user_classes.ApartmentSearcherUser;
import com.example.roome.user_classes.RoommateSearcherUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;
import java.util.Calendar;

public class EditFiltersApartmentSearcher extends DialogFragment {

    public static final int MAX_RENT_VALUE = 4000;
    private RangeSeekBar costBar;


    private ImageView mChooseLocations;
    TextView mChosenLocations;
    String[] locations;
    boolean[] checkedLocations;
    ArrayList<Integer> mUserLocations = new ArrayList<>(); //todo:send the locations chosen to db when save pressed

    private ImageView mDisplayDate;
    private DatePickerDialog.OnDateSetListener mDateSetListener;

    private RadioButton twoRommatesMax;
    private RadioButton threeRommatesMax;
    private RadioButton fourRommatesMax;


    private RangeSeekBar ageRoommatesBar;

    // Firebase instance variables
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference firebaseDatabaseReference;
    private ApartmentSearcherUser asUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabaseReference = firebaseDatabase.getReference();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_filter_apartment_searcher, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        ImageView saveButton = getView().findViewById(R.id.btn_save_filters_as);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseDatabaseReference.child("users").child("ApartmentSearcherUser").child(MyPreferences.getUserUid(getContext())).setValue(asUser);
                Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                setSavedFiltersToLists();
                getDialog().dismiss();
            }
        });

        //-----------------------------cost range-------------------------------------
        costBar = getView().findViewById(R.id.rsb_cost_bar);
        costBar.setRangeValues(1000, MAX_RENT_VALUE);
        costBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                Number minVal = bar.getSelectedMinValue();
                Number maxVal = bar.getSelectedMaxValue();
                int min = (int) minVal;
                int max = (int) maxVal;
                asUser.setMaxRent(max);
                asUser.setMinRent(min);
            }

        });
        //---------------------------------------------------------------------------
        //----------------------------location selection----------------------------
        mChooseLocations = getView().findViewById(R.id.btn_choose_locations);
        mChosenLocations = getView().findViewById(R.id.tv_picked_locations);

        locations = getResources().getStringArray(R.array.locations);
        checkedLocations = new boolean[locations.length];

        mChooseLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity());
                mBuilder.setTitle("Locations in Jerusalem");
                mBuilder.setMultiChoiceItems(locations, checkedLocations, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                        if (isChecked) {
                            mUserLocations.add(position);
                        } else {
                            mUserLocations.remove((Integer.valueOf(position)));
                        }
                    }
                });

                mBuilder.setCancelable(false);
                mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        String item = "";
                        for (int i = 0; i < mUserLocations.size(); i++) {
                            item = item + locations[mUserLocations.get(i)];
                            if (i != mUserLocations.size() - 1) {
                                item = item + ", ";
                            }
                        }
                        mChosenLocations.setText(item);
                    }
                });

                mBuilder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                mBuilder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        for (int i = 0; i < checkedLocations.length; i++) {
                            checkedLocations[i] = false;
                            mUserLocations.clear();
                            mChosenLocations.setText("");
                        }
                    }
                });

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });
        //---------------------------------------------------------------------------
        //----------------------------entry date selection----------------------------
        mDisplayDate = getView().findViewById(R.id.iv_choose_entry_date);
        final TextView chosenDate = getView().findViewById(R.id.tv_click_here_entry_date);

        mDisplayDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        getActivity(),
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        mDateSetListener,
                        year, month, day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                String date = day + "/" + month + "/" + year;

                chosenDate.setText(date);
                asUser.setEarliestEntryDate(date);

            }
        };

        //---------------------------------------------------------------------------
        //---------------------------- max num roommates selection----------------------------
        twoRommatesMax = getView().findViewById(R.id.radio_btn_as_2);
        twoRommatesMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asUser.setMaxNumDesiredRoommates(2);
                twoRommatesMax.setChecked(true);
            }
        });

        threeRommatesMax = getView().findViewById(R.id.radio_btn_as_3);
        threeRommatesMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asUser.setMaxNumDesiredRoommates(3);
                threeRommatesMax.setChecked(true);
            }
        });

        fourRommatesMax = getView().findViewById(R.id.radio_btn_as_4);
        fourRommatesMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asUser.setMaxNumDesiredRoommates(4);
                fourRommatesMax.setChecked(true);
            }
        });
        //---------------------------------------------------------------------------
        //----------------------------roommates' age selection----------------------------
        ageRoommatesBar = getView().findViewById(R.id.rsb_age_roommates_bar);
        ageRoommatesBar.setRangeValues(16, 35);

        ageRoommatesBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                Number minVal = bar.getSelectedMinValue();
                Number maxVal = bar.getSelectedMaxValue();
                int min = (int) minVal;
                int max = (int) maxVal;
                asUser.setMaxAgeRequired(max);
                asUser.setMinAgeRequired(min);
            }

        });

        //---------------------------------------------------------------------------
        //----------------------------Things i care about selection----------------------------


        super.onActivityCreated(savedInstanceState);
        asUser = FirebaseMediate.getApartmentSearcherUserByUid(MyPreferences.getUserUid(getContext())); //todo is ok?
        setFiltersValuesFromDataBase();

    }

    /**
     * This method calls the method to filters out the the irrelevant roommate users from with
     * a specified list.
     */
    private void setSavedFiltersToLists() {
        filterOutRoommatesFromList(ChoosingActivity.NOT_SEEN);
    }

    /**
     * This method filters out the the irrelevant roommate users from the specified list.
     *
     * @param listName - The list to filter the irrelevant roommate users from.
     */
    private void filterOutRoommatesFromList(String listName) {
        ArrayList<String> listRoommatesIds = FirebaseMediate.getAptPrefList(listName, MyPreferences.getUserUid(getContext()));
        ArrayList<String> updatedUnSeenRoommatesIds = new ArrayList<>();
        for (String roommateId : listRoommatesIds) {
            RoommateSearcherUser roommate = FirebaseMediate.getRoommateSearcherUserByUid(roommateId);
            if (roommate.getApartment() != null) {
                double roommatesApartmentRent = roommate.getApartment().getRent();
                if (roommatesApartmentRent <= asUser.getMaxRent() && roommatesApartmentRent >= asUser.getMinRent()) {
                    updatedUnSeenRoommatesIds.add(roommateId);
                }
            }
        }
        FirebaseMediate.setAptPrefList(listName, MyPreferences.getUserUid(getContext()), updatedUnSeenRoommatesIds);
    }

    private void setMaxNumRoommatesRB(){
        int chosenNum = asUser.getMaxNumDesiredRoommates();
        switch (chosenNum){
            case 2:
                twoRommatesMax.setChecked(true);
                break;
            case 3:
                threeRommatesMax.setChecked(true);
                break;
            case 4:
                fourRommatesMax.setChecked(true);
                break;
//            case 0:
//                twoRommatesMax.setChecked(false);
//                threeRommatesMax.setChecked(false);
//                fourRommatesMax.setChecked(false);
//                break;

        }
    }


    //todo: things i care about
    /**
     * This method sets the filters to the users preferences values stored in database.
     */
    private void setFiltersValuesFromDataBase() {
        costBar.setSelectedMinValue(asUser.getMinRent());
        costBar.setSelectedMaxValue(asUser.getMaxRent());
        setMaxNumRoommatesRB();
        ageRoommatesBar.setSelectedMinValue(asUser.getMinAgeRequired());
        if (asUser.getMaxAgeRequired() != 0) { //until user edit his age in edit profile the default value is 0
            ageRoommatesBar.setSelectedMaxValue(asUser.getMaxAgeRequired());
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

}
