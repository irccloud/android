/*
 * Copyright (c) 2017 IRCCloud, Ltd.
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
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.commonsware.cwac.richedit.Effect;
import com.commonsware.cwac.richedit.RichEditText;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

public class IRCEditText extends RichEditText {
    private List<SpanWrapper> typingSpans = new ArrayList<>();
    private List<Effect> typingEffects = new ArrayList<>();
    int typing_fg = -1;
    int typing_bg = -1;
    private boolean selectionChangedByUser = false;
    private Runnable selectionChangeRunnable = new Runnable() {
        @Override
        public void run() {
            selectionChangedByUser = true;
            onSelectionChanged(getSelectionStart(), getSelectionEnd());
        }
    };

    public IRCEditText(Context context) {
        super(context);
        init();
    }

    public IRCEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IRCEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            private int size, oldLength, changeStart;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                selectionChangedByUser = false;
                oldLength = s.length();
                changeStart = start;
                //android.util.Log.d("IRCCloud", "Text will change: " + start + ", " + count + ", after: " + after);
                removeCallbacks(selectionChangeRunnable);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                size = count - before;
                if (getSelectionStart() == getSelectionEnd()) {
                    //android.util.Log.d("IRCCloud", "change start: " + changeStart + " end: " + getSelectionEnd() + " size: " + size);
                    if(size > 0) {
                        if (typingEffects.contains(RichEditText.BOLD)) {
                            //android.util.Log.d("IRCCloud", "Typing effects contains bold");
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof StyleSpan && ((StyleSpan) span.span).getStyle() == Typeface.BOLD) {
                                    if (changeStart >= span.start && changeStart <= span.end) {
                                        if(span.start == span.end) {
                                            //android.util.Log.d("IRCCloud", "Replacing zero-length span");
                                            typingSpans.remove(span);
                                        } else {
                                            //android.util.Log.d("IRCCloud", "Expanding existing span");
                                            found = true;
                                            span.end += size;
                                            span.added = false;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                //android.util.Log.d("IRCCloud", "Inserting new span");
                                SpanWrapper span = new SpanWrapper();
                                span.start = changeStart;
                                span.end = span.start + size;
                                span.span = new StyleSpan(Typeface.BOLD);
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.ITALIC)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof StyleSpan && ((StyleSpan) span.span).getStyle() == Typeface.ITALIC) {
                                    if (changeStart >= span.start && changeStart <= span.end) {
                                        if(span.start == span.end) {
                                            typingSpans.remove(span);
                                        } else {
                                            found = true;
                                            span.end += size;
                                            span.added = false;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = changeStart;
                                span.end = span.start + size;
                                span.span = new StyleSpan(Typeface.ITALIC);
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.UNDERLINE)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof UnderlineSpan) {
                                    if (changeStart >= span.start && changeStart <= span.end) {
                                        if(span.start == span.end) {
                                            typingSpans.remove(span);
                                        } else {
                                            found = true;
                                            span.end += size;
                                            span.added = false;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = changeStart;
                                span.end = span.start + size;
                                span.span = new UnderlineSpan();
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.STRIKETHROUGH)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof StrikethroughSpan) {
                                    if (changeStart >= span.start && changeStart <= span.end) {
                                        if(span.start == span.end) {
                                            typingSpans.remove(span);
                                        } else {
                                            found = true;
                                            span.end += size;
                                            span.added = false;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = changeStart;
                                span.end = span.start + size;
                                span.span = new StrikethroughSpan();
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.FOREGROUND)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof ForegroundColorSpan && ((ForegroundColorSpan)span.span).getForegroundColor() == typing_fg) {
                                    if (changeStart >= span.start && changeStart <= span.end) {
                                        if(span.start == span.end) {
                                            typingSpans.remove(span);
                                        } else {
                                            found = true;
                                            span.end += size;
                                            span.added = false;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = changeStart;
                                span.end = span.start + size;
                                span.span = new ForegroundColorSpan(typing_fg);
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.BACKGROUND)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof BackgroundColorSpan && ((BackgroundColorSpan)span.span).getBackgroundColor() == typing_bg) {
                                    if (changeStart >= span.start && changeStart <= span.end) {
                                        if(span.start == span.end) {
                                            typingSpans.remove(span);
                                        } else {
                                            found = true;
                                            span.end += size;
                                            span.added = false;
                                        }
                                        break;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = changeStart;
                                span.end = span.start + size;
                                span.span = new BackgroundColorSpan(typing_bg);
                                typingSpans.add(span);
                            }
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(oldLength > 0 && editable.length() == 0) {
                    typingSpans.clear();
                    typingEffects.clear();
                } else {
                    ArrayList<SpanWrapper> spansToRemove = new ArrayList<>();

                    for (SpanWrapper span : typingSpans) {
                        //android.util.Log.d("IRCCloud", "Span: " + span);
                        if (size > 0) {
                            if (span.added) {
                                if (span.start >= changeStart) {
                                    //android.util.Log.d("IRCCloud", "span starts after insertion range, offsetting");
                                    span.start += size;
                                    span.end += size;
                                } else if (span.end > changeStart) {
                                    //android.util.Log.d("IRCCloud", "span ends within insertion range, expanding");
                                    span.end += size;
                                }
                            }
                        } else {
                            if (span.start > changeStart) {
                                //android.util.Log.d("IRCCloud", "span starts after deletion range, offsetting");
                                span.start += size;
                                span.end += size;
                            } else if (span.end > changeStart) {
                                //android.util.Log.d("IRCCloud", "span ends after deletion range, shrinking");
                                span.end += size;
                            }
                        }

                        if (span.end > editable.length()) {
                            //android.util.Log.d("IRCCloud", "span exceeds editable range, truncating");
                            span.end = editable.length();
                        }

                        if (span.end < span.start) {
                            //android.util.Log.d("IRCCloud", "span too small, removing");
                            editable.removeSpan(span.span);
                            spansToRemove.add(span);
                            continue;
                        }

                        if(span.start >= 0 && span.end <= editable.length()) {
                            editable.setSpan(span.span, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            span.added = true;
                        }
                    }

                    if (spansToRemove.size() > 0) {
                        //android.util.Log.d("IRCCloud", "Removing spans: " + typingSpans);
                        typingSpans.removeAll(spansToRemove);
                    }
                    //android.util.Log.d("IRCCloud", "Spans: " + typingSpans);
                }
                postDelayed(selectionChangeRunnable, 100);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        selectionChangedByUser = true;
        try {
            return super.onTouchEvent(event);
        } catch (NullPointerException e) {
            //Android 6.0 bug
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        selectionChangedByUser = true;
        boolean result = super.onKeyUp(keyCode, event);
        if(!event.isCtrlPressed() && !event.isShiftPressed() && !event.isMetaPressed()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                lastSelectionStart = -1;
                lastSelectionEnd = -2;
                onSelectionChanged(getSelectionStart(), getSelectionEnd());
            }
        }
        return result;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if(text == null || text.length() == 0) {
            clearTypingEffects();
            onSelectionChanged(0, 0);
        }
    }

    private void applyTypingSpans() {
        if(typingSpans != null) {
            Spannable text = getText();
            for (SpanWrapper w : typingSpans) {
                if(w.start >= 0 && w.end >= w.start && w.end <= text.length())
                    text.setSpan(w.span, w.start, w.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public String toIRC() {
        Spannable text = getText();
        StringBuilder out = new StringBuilder();

        int next;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), CharacterStyle.class);
            String fgColorRGB = null, bgColorRGB = null, fgColormIRC = null, bgColormIRC = null;
            boolean hasURL = false, shouldClear = false, hasBold = false, hasItalics = false, hasUnderline = false, hasStrikethrough = false, hasMonospace = false;
            for (CharacterStyle style : text.getSpans(i, next, CharacterStyle.class)) {
                if (style instanceof URLSpan) {
                    hasURL = true;
                    fgColorRGB = bgColorRGB = null;
                }
                if (style instanceof StyleSpan) {
                    int s = ((StyleSpan) style).getStyle();
                    if ((s & Typeface.BOLD) != 0) {
                        hasBold = true;
                        shouldClear = true;
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        hasItalics = true;
                        shouldClear = true;
                    }
                }
                if (style instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style).getFamily();
                    if ("monospace".equals(s)) {
                        hasMonospace = true;
                        shouldClear = true;
                    }
                }
                if (style instanceof UnderlineSpan) {
                    hasUnderline = true;
                    shouldClear = true;
                }
                if (style instanceof StrikethroughSpan) {
                    hasStrikethrough = true;
                    shouldClear = true;
                }
                if(!hasURL) {
                    if (style instanceof ForegroundColorSpan) {
                        fgColorRGB = String.format("%06X", 0xFFFFFF & ((ForegroundColorSpan) style).getForegroundColor());
                        for (int c = 0; c < ColorFormatter.COLOR_MAP.length; c++) {
                            String color = ColorFormatter.COLOR_MAP[c];
                            if (fgColorRGB.equalsIgnoreCase(color) || fgColorRGB.equalsIgnoreCase(ColorFormatter.DARK_FG_SUBSTITUTIONS.get(color))) {
                                fgColormIRC = String.valueOf(c);
                                if (fgColormIRC.length() == 1)
                                    fgColormIRC = "0" + fgColormIRC;
                                break;
                            }
                        }
                    }
                    if (style instanceof BackgroundColorSpan) {
                        bgColorRGB = String.format("%06X", 0xFFFFFF & ((BackgroundColorSpan) style).getBackgroundColor());
                        for (int c = 0; c < ColorFormatter.COLOR_MAP.length; c++) {
                            String color = ColorFormatter.COLOR_MAP[c];
                            if (bgColorRGB.equalsIgnoreCase(color)) {
                                bgColormIRC = String.valueOf(c);
                                if (bgColormIRC.length() == 1)
                                    bgColormIRC = "0" + bgColormIRC;
                                break;
                            }
                        }
                    }
                }
            }
            if(hasBold)
                out.append((char)0x02);

            if(hasItalics)
                out.append((char)0x1d);

            if(hasUnderline)
                out.append((char)0x1f);

            if(hasMonospace)
                out.append((char)0x11);

            if(hasStrikethrough)
                out.append((char)0x1e);

            if(fgColorRGB != null || bgColorRGB != null) {
                if((fgColormIRC != null && (bgColorRGB == null || bgColormIRC != null)) || (fgColormIRC == null && bgColormIRC != null)) {
                    out.append((char) 0x03);
                    if (fgColormIRC != null)
                        out.append(fgColormIRC);
                    if (bgColormIRC != null)
                        out.append(",").append(bgColormIRC);
                } else {
                    out.append((char) 0x04);
                    if (fgColorRGB != null)
                        out.append(fgColorRGB);
                    if (bgColorRGB != null)
                        out.append(",").append(bgColorRGB);
                }
                shouldClear = true;
            }
            out.append(text.subSequence(i, next));
            if(shouldClear)
                out.append("\u000f");
        }

        return out.toString();
    }

    private class SpanWrapper {
        public int start;
        public int end;
        public Object span;
        public boolean added;

        @Override
        public String toString() {
            return "{ start: " + start + ", end: " + end + ", span: " + span + " }";
        }
    }

    @Override
    public boolean hasEffect(Effect effect) {
        return typingEffects.contains(effect);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getEffectValue(Effect<T> effect) {
        if(getSelectionStart() == getSelectionEnd() || effect.valueInSelection(this) == null) {
            if(effect == RichEditText.FOREGROUND)
                return (T)Integer.valueOf(typing_fg);
            else if(effect == RichEditText.BACKGROUND)
                return (T)Integer.valueOf(typing_bg);
            else
                return (T)Boolean.valueOf(hasEffect(effect));
        } else {
            return (effect.valueInSelection(this));
        }
    }

    private boolean shouldRemoveSpan(CharacterStyle style) {
        if (style instanceof StyleSpan && ((StyleSpan) style).getStyle() == Typeface.BOLD)
            return !typingEffects.contains(RichEditText.BOLD);
        if (style instanceof StyleSpan && ((StyleSpan) style).getStyle() == Typeface.ITALIC)
            return !typingEffects.contains(RichEditText.ITALIC);
        if (style instanceof UnderlineSpan)
            return !typingEffects.contains(RichEditText.UNDERLINE);
        if (style instanceof ForegroundColorSpan)
            return ((ForegroundColorSpan)style).getForegroundColor() != typing_fg;
        if (style instanceof BackgroundColorSpan)
            return ((BackgroundColorSpan)style).getBackgroundColor() != typing_bg;
        return false;
    }

    private int lastSelectionStart, lastSelectionEnd;

    private boolean compare(CharacterStyle s1, CharacterStyle s2) {
        if(s1 instanceof StyleSpan && s2 instanceof StyleSpan)
            return ((StyleSpan)s1).getStyle() == ((StyleSpan)s2).getStyle();
        if(s1 instanceof ForegroundColorSpan && s2 instanceof ForegroundColorSpan)
            return ((ForegroundColorSpan)s1).getForegroundColor() == ((ForegroundColorSpan)s2).getForegroundColor();
        if(s1 instanceof BackgroundColorSpan && s2 instanceof BackgroundColorSpan)
            return ((BackgroundColorSpan)s1).getBackgroundColor() == ((BackgroundColorSpan)s2).getBackgroundColor();
        if(s1 instanceof UnderlineSpan && s2 instanceof UnderlineSpan)
            return true;
        if(s1 instanceof StrikethroughSpan && s2 instanceof StrikethroughSpan)
            return true;
        if(s1 instanceof TypefaceSpan && s2 instanceof TypefaceSpan)
            return ((TypefaceSpan)s1).getFamily().equals(((TypefaceSpan)s2).getFamily());
        return false;
    }

    private void updateSpans() {
        Spannable text = getText();
        for (CharacterStyle style : text.getSpans(0, text.length(), CharacterStyle.class)) {
            if (text.getSpanFlags(style) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                boolean found = false;
                for (SpanWrapper w : typingSpans) {
                    if (w.span == style) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    SpanWrapper w = new SpanWrapper();
                    w.start = text.getSpanStart(style);
                    w.end = text.getSpanEnd(style);
                    w.span = style;
                    typingSpans.add(w);
                    //android.util.Log.d("IRCCloud", "Adding span at " + w.start + ", " + w.end);
                }
            }
        }

        ArrayList<SpanWrapper> spansToRemove = new ArrayList<>();
        for (SpanWrapper span : typingSpans) {
            for (SpanWrapper span2 : typingSpans) {
                if(!spansToRemove.contains(span) && span2 != span && span2.start == span.start && span2.end == span.end && compare((CharacterStyle)span2.span, (CharacterStyle)span.span))
                    spansToRemove.add(span2);
            }
        }

        if(spansToRemove.size() > 0) {
            //android.util.Log.d("IRCCloud", "Removing duplicates: " + spansToRemove);
            typingSpans.removeAll(spansToRemove);
        }

        //android.util.Log.d("IRCCloud", "Spans: " + typingSpans);
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        if(selectionChangedByUser) {
            Spannable text = getText();
            if (text != null && text.length() > 0) {
                if (start != lastSelectionStart && end != lastSelectionEnd && (lastSelectionStart != start - 1 || lastSelectionStart != lastSelectionEnd || start != end)) {
                    if (typingEffects != null)
                        typingEffects.clear();
                    for (CharacterStyle style : text.getSpans(start, end, CharacterStyle.class)) {
                        if (text.getSpanFlags(style) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                            text.removeSpan(style);
                        }
                    }
                    applyTypingSpans();
                    updateSpans();
                    for (CharacterStyle style : text.getSpans(start, end, CharacterStyle.class)) {
                        if (text.getSpanFlags(style) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                            if (style instanceof StyleSpan && ((StyleSpan) style).getStyle() == Typeface.BOLD)
                                typingEffects.add(RichEditText.BOLD);
                            if (style instanceof StyleSpan && ((StyleSpan) style).getStyle() == Typeface.ITALIC)
                                typingEffects.add(RichEditText.ITALIC);
                            if (style instanceof UnderlineSpan)
                                typingEffects.add(RichEditText.UNDERLINE);
                            if (style instanceof ForegroundColorSpan) {
                                typingEffects.add(RichEditText.FOREGROUND);
                                typing_fg = ((ForegroundColorSpan) style).getForegroundColor();
                            }
                            if (style instanceof BackgroundColorSpan) {
                                typingEffects.add(RichEditText.BACKGROUND);
                                typing_bg = ((BackgroundColorSpan) style).getBackgroundColor();
                            }
                        }
                    }
                }
            }
        }
        lastSelectionStart = start;
        lastSelectionEnd = end;
        super.onSelectionChanged(start, end);
    }

    private void splitSpan(Spannable text, CharacterStyle style) {
        int start = text.getSpanStart(style);
        int end = text.getSpanEnd(style);
        text.removeSpan(style);

        if(start >= getSelectionStart() && end <= getSelectionEnd()) {
            for(SpanWrapper w : typingSpans) {
                if(w.span == style) {
                    typingSpans.remove(w);
                    break;
                }
            }
            return;
        }

        for(SpanWrapper w : typingSpans) {
            if(w.span == style) {
                w.start = start;
                w.end = end;
            }
        }
        if (start < getSelectionEnd() && end > getSelectionEnd()) {
            CharacterStyle newStyle;
            if(style instanceof ForegroundColorSpan)
                newStyle = new ForegroundColorSpan(((ForegroundColorSpan)style).getForegroundColor());
            else if(style instanceof BackgroundColorSpan)
                newStyle = new BackgroundColorSpan(((BackgroundColorSpan)style).getBackgroundColor());
            else if(style instanceof StyleSpan)
                newStyle = new StyleSpan(((StyleSpan)style).getStyle());
            else if(style instanceof UnderlineSpan)
                newStyle = new UnderlineSpan();
            else if(style instanceof StrikethroughSpan)
                newStyle = new StrikethroughSpan();
            else
                newStyle = CharacterStyle.wrap(style);
            text.setSpan(newStyle, getSelectionEnd(), end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for(SpanWrapper w : typingSpans) {
                if(w.span == style) {
                    w = new SpanWrapper();
                    w.start = getSelectionEnd();
                    w.end = end;
                    w.span = newStyle;
                    typingSpans.add(w);
                    break;
                }
            }
        }
        if (start < getSelectionStart() && end > getSelectionStart()) {
            text.setSpan(style, start, getSelectionStart(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for(SpanWrapper w : typingSpans) {
                if(w.span == style) {
                    w.start = start;
                    w.end = getSelectionStart();
                    break;
                }
            }
        }
    }

    private void splitInactiveSpans(Spannable text, int start, int end) {
        if(typingSpans != null) {
            updateSpans();
            applyTypingSpans();

            for (CharacterStyle style : text.getSpans(start, end, CharacterStyle.class)) {
                if (text.getSpanFlags(style) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                    if (shouldRemoveSpan(style)) {
                        splitSpan(text, style);
                    }
                }
            }
        }
    }

    public void clearTypingEffects() {
        if(typingEffects != null)
            typingEffects.clear();
        if(typingSpans != null)
            typingSpans.clear();
        typing_fg = typing_bg = -1;

        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd());
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
        applyTypingSpans();
    }

    public void toggleTypingEffect(Effect effect) {
        if(typingEffects.contains(effect)) {
            typingEffects.remove(effect);
            if(typingSpans != null)
                typingSpans.clear();
            applyEffect(effect, false);
        } else {
            typingEffects.add(effect);
            applyEffect(effect, true);
        }
        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd());
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }

    public void applyForegroundColor(int color) {
        typing_fg = color;
        typingEffects.add(RichEditText.FOREGROUND);
        if(getSelectionEnd() <= getText().length())
            applyEffect(RichEditText.FOREGROUND, color);
        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd());
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }

    public void applyBackgroundColor(int color) {
        typing_bg = color;
        typingEffects.add(RichEditText.BACKGROUND);
        if(getSelectionEnd() <= getText().length())
            applyEffect(RichEditText.BACKGROUND, color);
        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd());
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }
}