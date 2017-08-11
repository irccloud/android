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
import android.view.KeyEvent;

import com.commonsware.cwac.richedit.Effect;
import com.commonsware.cwac.richedit.RichEditText;

import java.util.ArrayList;
import java.util.List;

public class IRCEditText extends RichEditText {
    private List<SpanWrapper> typingSpans = new ArrayList<>();
    private List<Effect> typingEffects = new ArrayList<>();
    int typing_fg = -1;
    int typing_bg = -1;

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
            private int size, selectionEnd;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                selectionEnd = getSelectionEnd();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                size = count - before;
                if (getSelectionStart() == getSelectionEnd()) {
                    if(size > 0) {
                        if (typingEffects.contains(RichEditText.BOLD)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof StyleSpan && ((StyleSpan) span.span).getStyle() == Typeface.BOLD) {
                                    if (span.end == getSelectionEnd() - size) {
                                        span.end = getSelectionEnd();
                                        found = true;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = getSelectionEnd() - size;
                                span.end = getSelectionEnd();
                                span.span = new StyleSpan(Typeface.BOLD);
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.ITALIC)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof StyleSpan && ((StyleSpan) span.span).getStyle() == Typeface.ITALIC) {
                                    if (span.end == getSelectionEnd() - size) {
                                        span.end = getSelectionEnd();
                                        found = true;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = getSelectionEnd() - size;
                                span.end = getSelectionEnd();
                                span.span = new StyleSpan(Typeface.ITALIC);
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.UNDERLINE)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof UnderlineSpan) {
                                    if (span.end == getSelectionEnd() - size) {
                                        span.end = getSelectionEnd();
                                        found = true;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = getSelectionEnd() - size;
                                span.end = getSelectionEnd();
                                span.span = new UnderlineSpan();
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.STRIKETHROUGH)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof StrikethroughSpan) {
                                    if (span.end == getSelectionEnd() - size) {
                                        span.end = getSelectionEnd();
                                        found = true;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = getSelectionEnd() - size;
                                span.end = getSelectionEnd();
                                span.span = new StrikethroughSpan();
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.FOREGROUND)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof ForegroundColorSpan && ((ForegroundColorSpan)span.span).getForegroundColor() == typing_fg) {
                                    if (span.end == getSelectionEnd() - size) {
                                        span.end = getSelectionEnd();
                                        found = true;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = getSelectionEnd() - size;
                                span.end = getSelectionEnd();
                                span.span = new ForegroundColorSpan(typing_fg);
                                typingSpans.add(span);
                            }
                        }
                        if (typingEffects.contains(RichEditText.BACKGROUND)) {
                            boolean found = false;
                            for (SpanWrapper span : typingSpans) {
                                if (span.span instanceof BackgroundColorSpan && ((BackgroundColorSpan)span.span).getBackgroundColor() == typing_bg) {
                                    if (span.end == getSelectionEnd() - size) {
                                        span.end = getSelectionEnd();
                                        found = true;
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                SpanWrapper span = new SpanWrapper();
                                span.start = getSelectionEnd() - size;
                                span.end = getSelectionEnd();
                                span.span = new BackgroundColorSpan(typing_bg);
                                typingSpans.add(span);
                            }
                        }
                    }
                } else {
                    typingSpans.clear();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                ArrayList<SpanWrapper> spansToRemove = new ArrayList<>();

                for(SpanWrapper span : typingSpans) {
                    if(span.end > editable.length()) {
                        span.end = editable.length();
                        if(span.end <= span.start) {
                            editable.removeSpan(span.span);
                            spansToRemove.add(span);
                            continue;
                        }
                    }

                    if(editable.getSpanStart(span.span) != -1) {
                        if (size > 0) {
                            if (span.start > selectionEnd - size) {
                                span.start += size;
                                span.end += size;
                            } else if(span.end > selectionEnd + size) {
                                span.end += size;
                            }
                        } else {
                            if (span.start > selectionEnd + size) {
                                span.start += size;
                                span.end += size;
                            } else if(span.end > selectionEnd - size) {
                                span.end += size;
                            }
                        }
                    }
                    editable.setSpan(span.span, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if(spansToRemove.size() > 0)
                    typingSpans.removeAll(spansToRemove);
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
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

    public String toIRC() {
        Spannable text = getText();
        if(typingSpans != null) {
            for (SpanWrapper w : typingSpans) {
                text.setSpan(w.span, w.start, w.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        StringBuilder out = new StringBuilder();

        int next;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), CharacterStyle.class);
            String fgColorRGB = null, bgColorRGB = null, fgColormIRC = null, bgColormIRC = null;
            boolean hasURL = false, shouldClear = false;
            for (CharacterStyle style : text.getSpans(i, next, CharacterStyle.class)) {
                if (style instanceof URLSpan) {
                    hasURL = true;
                    fgColorRGB = bgColorRGB = null;
                }
                if (style instanceof StyleSpan) {
                    int s = ((StyleSpan) style).getStyle();
                    if ((s & Typeface.BOLD) != 0) {
                        out.append((char)0x02);
                        shouldClear = true;
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append((char)0x1d);
                        shouldClear = true;
                    }
                }
                if (style instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style).getFamily();
                    if ("monospace".equals(s)) {
                        out.append((char)0x11);
                        shouldClear = true;
                    }
                }
                if (style instanceof UnderlineSpan) {
                    out.append((char)0x1f);
                    shouldClear = true;
                }
                if (style instanceof StrikethroughSpan) {
                    out.append((char)0x1e);
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
    }

    @Override
    public boolean hasEffect(Effect effect) {
        return typingEffects.contains(effect);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getEffectValue(Effect<T> effect) {
        if(getSelectionStart() == getSelectionEnd()) {
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

    @Override
    public void onSelectionChanged(int start, int end) {
        Spannable text = getText();
        if (text != null && text.length() > 0) {
            if(start != lastSelectionStart && end != lastSelectionEnd && (lastSelectionStart != start - 1 || lastSelectionStart != lastSelectionEnd || start != end)) {
                if (typingEffects != null)
                    typingEffects.clear();
                if (typingSpans != null)
                    typingSpans.clear();
                for (CharacterStyle style : text.getSpans(start, end, CharacterStyle.class)) {
                    if (text.getSpanFlags(style) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                        SpanWrapper w = new SpanWrapper();
                        w.start = text.getSpanStart(style);
                        w.end = text.getSpanEnd(style);
                        w.span = style;
                        typingSpans.add(w);

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
        lastSelectionStart = start;
        lastSelectionEnd = end;
        super.onSelectionChanged(start, end);
    }

    private void splitSpan(Spannable text, CharacterStyle style, int gap) {
        int start = text.getSpanStart(style);
        int end = text.getSpanEnd(style);
        text.removeSpan(style);
        if (end > getSelectionEnd()) {
            if (end - getSelectionEnd() > 0) {
                CharacterStyle newStyle = CharacterStyle.wrap(style);
                text.setSpan(newStyle, getSelectionEnd() + gap, end + gap, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                for(SpanWrapper w : typingSpans) {
                    if(w.span == style) {
                        w = new SpanWrapper();
                        w.start = getSelectionEnd() + gap;
                        w.end = end + gap;
                        w.span = newStyle;
                        typingSpans.add(w);
                        break;
                    }
                }
            }
        }
        if (start < getSelectionStart()) {
            text.setSpan(style, start, getSelectionStart(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for(SpanWrapper w : typingSpans) {
                if(w.span == style) {
                    w.start = start;
                    w.end = getSelectionStart() - gap;
                    break;
                }
            }
        }
    }

    private void splitInactiveSpans(Spannable text, int start, int end, int gap) {
        for (CharacterStyle style : text.getSpans(start, end, CharacterStyle.class)) {
            if(text.getSpanFlags(style) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                if (shouldRemoveSpan(style)) {
                    splitSpan(text, style, gap);
                }
            }
        }

        if(typingSpans != null) {
            for (SpanWrapper w : typingSpans) {
                text.setSpan(w.span, w.start, w.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public void clearTypingEffects() {
        if(typingEffects != null)
            typingEffects.clear();
        typing_fg = typing_bg = -1;

        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd(), 0);
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }

    public void toggleTypingEffect(Effect effect) {
        if(typingEffects.contains(effect)) {
            typingEffects.remove(effect);
            applyEffect(effect, false);
        } else {
            typingEffects.add(effect);
            applyEffect(effect, true);
        }
        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd(), 0);
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }

    public void applyForegroundColor(int color) {
        typing_fg = color;
        typingEffects.add(RichEditText.FOREGROUND);
        if(getSelectionEnd() < getText().length())
            applyEffect(RichEditText.FOREGROUND, color);
        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd(), 0);
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }

    public void applyBackgroundColor(int color) {
        typing_bg = color;
        typingEffects.add(RichEditText.BACKGROUND);
        if(getSelectionEnd() < getText().length())
            applyEffect(RichEditText.BACKGROUND, color);
        splitInactiveSpans(getText(), getSelectionStart(), getSelectionEnd(), 0);
        super.onSelectionChanged(getSelectionStart(), getSelectionEnd());
    }
}