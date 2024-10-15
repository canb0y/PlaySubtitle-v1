package com.itbn.playsubtitle.v1.settings;

import android.os.Bundle;
import android.content.Context;
import android.app.AlertDialog;
import android.app.Dialog;
import android.widget.Button;
import android.preference.EditTextPreference;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.AttributeSet;

public class ValidatedEditTextPreference extends EditTextPreference {
    
    public ValidatedEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public ValidatedEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    private class EditTextWatcher implements TextWatcher {
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean mTextStatus = false;
            
            if (s.length() > 0 && s.length() <= 4) {
                mTextStatus = true;
            } else {
                mTextStatus = false;
            }
            
            onEditTextChanged(mTextStatus);
        }
            
        @Override
        public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
            
        @Override
        public void afterTextChanged(Editable s) {}
    }
    
    EditTextWatcher mTextWatcher = new EditTextWatcher();
    
    protected void onEditTextChanged(boolean z) {
        Dialog mDialog = getDialog();
        
        if (mDialog instanceof AlertDialog) {
            AlertDialog mAlertDialog = (AlertDialog) mDialog;
            Button mButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            mButton.setEnabled(z);
        }
    }
    
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        getEditText().removeTextChangedListener(mTextWatcher);
        getEditText().addTextChangedListener(mTextWatcher);
        onEditTextChanged(false);
    }
}
