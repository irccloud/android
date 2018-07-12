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

package com.irccloud.android.data;

import android.content.res.Resources;
import androidx.databinding.BindingAdapter;
import android.widget.TextView;

public class TextColorBindingAdapter {
    @BindingAdapter("android:textColor")
    public static void setTextColor(TextView view, int res) {
        try {
            if(view.getResources().getResourceTypeName(res).equals("color"))
                view.setTextColor(view.getResources().getColor(res));
            else
                view.setTextColor(view.getResources().getColorStateList(res));
        } catch (Resources.NotFoundException e) {
            view.setTextColor(res);
        }
    }
}
