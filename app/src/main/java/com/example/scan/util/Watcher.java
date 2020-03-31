package com.example.scan.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class Watcher implements TextWatcher{
    // class to remove Error when text changes
        private EditText textRef;       // todo : Check for memory leaks
        public Watcher(EditText textRef){
            this.textRef = textRef;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            this.textRef.setError(null);
        }
}
