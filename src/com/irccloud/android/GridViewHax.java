/*
 * Copyright (c) 2014 IRCCloud, Ltd.
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

package com.irccloud.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

/**
 * Created by sam on 2/12/14.
 */
public class GridViewHax extends GridView {
    public GridViewHax(Context context) {
        super(context);
    }

    public GridViewHax(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public GridViewHax(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    //The nick suggestions GridView will steal focus from the input box if we don't override this
    public boolean shouldShowSelector() {
        return false;
    }
}
