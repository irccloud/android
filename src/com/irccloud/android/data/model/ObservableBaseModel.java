/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.data.model;

import android.databinding.Observable;
import android.databinding.PropertyChangeRegistry;
import android.os.Handler;
import android.os.Looper;

import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.raizlabs.android.dbflow.structure.BaseModel;

public class ObservableBaseModel extends BaseModel implements Observable {
    private PropertyChangeRegistry callbacks = new PropertyChangeRegistry();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void addOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    public void notifyPropertyChanged(final int id) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                callbacks.notifyChange(ObservableBaseModel.this, id);
            }
        });
    }
}
