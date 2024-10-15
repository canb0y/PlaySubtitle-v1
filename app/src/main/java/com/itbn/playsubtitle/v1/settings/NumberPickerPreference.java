package com.itbn.playsubtitle.v1.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.preference.DialogPreference;

public class NumberPickerPreference extends DialogPreference {
    
    private NumberPicker mPicker;
    private Integer mNumber = 0;
    
    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }
    
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView title = view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
    }
    
    @Override
    protected View onCreateDialogView() {
        mPicker = new NumberPicker(getContext());
        mPicker.setMinValue(14);
        mPicker.setMaxValue(34);
        mPicker.setValue(mNumber);
        return mPicker;
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mPicker.clearFocus();
            setValue(mPicker.getValue());
        }
    }
    
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mNumber) : (Integer) defaultValue);
    }
    
    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }
        
        if (value != mNumber) {
            mNumber = value;
            notifyChanged();
        }
    }
    
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }
}