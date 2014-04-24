/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.XMLReader;

import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.util.Patterns;

import com.irccloud.android.data.ServersDataSource;

public class ColorFormatter {
    //From: https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
        "(?:"
        + "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
        + "|(?:biz|b[abdefghijmnorstvwyz])"
        + "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
        + "|d[ejkmoz]"
        + "|(?:edu|e[cegrstu])"
        + "|f[ijkmor]"
        + "|(?:gov|g[abdefghilmnpqrstuwy])"
        + "|h[kmnrtu]"
        + "|(?:info|int|i[delmnoqrst])"
        + "|(?:jobs|j[emop])"
        + "|k[eghimnprwyz]"
        + "|l[abcikrstuvy]"
        + "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
        + "|(?:name|net|n[acefgilopruz])"
        + "|(?:org|om)"
        + "|(?:pro|p[aefghklmnrstwy])"
        + "|qa"
        + "|r[eosuw]"
        + "|s[abcdeghijklmnortuvyz]"
        + "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
        + "|u[agksyz]"
        + "|v[aceginu]"
        + "|w[fs]"
        + "|(?:\u03b4\u03bf\u03ba\u03b9\u03bc\u03ae|\u0438\u0441\u043f\u044b\u0442\u0430\u043d\u0438\u0435|\u0440\u0444|\u0441\u0440\u0431|\u05d8\u05e2\u05e1\u05d8|\u0622\u0632\u0645\u0627\u06cc\u0634\u06cc|\u0625\u062e\u062a\u0628\u0627\u0631|\u0627\u0644\u0627\u0631\u062f\u0646|\u0627\u0644\u062c\u0632\u0627\u0626\u0631|\u0627\u0644\u0633\u0639\u0648\u062f\u064a\u0629|\u0627\u0644\u0645\u063a\u0631\u0628|\u0627\u0645\u0627\u0631\u0627\u062a|\u0628\u06be\u0627\u0631\u062a|\u062a\u0648\u0646\u0633|\u0633\u0648\u0631\u064a\u0629|\u0641\u0644\u0633\u0637\u064a\u0646|\u0642\u0637\u0631|\u0645\u0635\u0631|\u092a\u0930\u0940\u0915\u094d\u0937\u093e|\u092d\u093e\u0930\u0924|\u09ad\u09be\u09b0\u09a4|\u0a2d\u0a3e\u0a30\u0a24|\u0aad\u0abe\u0ab0\u0aa4|\u0b87\u0ba8\u0bcd\u0ba4\u0bbf\u0baf\u0bbe|\u0b87\u0bb2\u0b99\u0bcd\u0b95\u0bc8|\u0b9a\u0bbf\u0b99\u0bcd\u0b95\u0baa\u0bcd\u0baa\u0bc2\u0bb0\u0bcd|\u0baa\u0bb0\u0bbf\u0b9f\u0bcd\u0b9a\u0bc8|\u0c2d\u0c3e\u0c30\u0c24\u0c4d|\u0dbd\u0d82\u0d9a\u0dcf|\u0e44\u0e17\u0e22|\u30c6\u30b9\u30c8|\u4e2d\u56fd|\u4e2d\u570b|\u53f0\u6e7e|\u53f0\u7063|\u65b0\u52a0\u5761|\u6d4b\u8bd5|\u6e2c\u8a66|\u9999\u6e2f|\ud14c\uc2a4\ud2b8|\ud55c\uad6d|xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-3e0b707e|xn\\-\\-45brj9c|xn\\-\\-80akhbyknj4f|xn\\-\\-90a3ac|xn\\-\\-9t4b11yi5a|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-deba0ad|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-gecrj9c|xn\\-\\-h2brj9c|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-s9brj9c|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah|xxx)"
        + "|y[et]"
        + "|z[amw]))";

    public static final String GOOD_IRI_CHAR =
            "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    public static final Pattern WEB_URL = Pattern.compile(
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
                    + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                    + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                    + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])))"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~\\$"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)");

	private static final String[] COLOR_MAP = {
		"FFFFFF", //white
		"000000", //black
		"000080", //navy
		"008000", //green
		"FF0000", //red
		"800000", //maroon
		"800080", //purple
		"FFA500", //orange
		"FFFF00", //yellow
		"00FF00", //lime
		"008080", //teal
		"00FFFF", //cyan
		"0000FF", //blue
		"FF00FF", //magenta
		"808080", //grey
		"C0C0C0", //silver
	};

    public static final HashMap<String,String> emojiMap = new HashMap<String, String>(){{
        put("poodle","\uD83D\uDC29");
        put("black_joker","\uD83C\uDCCF");
        put("dog2","\uD83D\uDC15");
        put("hotel","\uD83C\uDFE8");
        put("fuelpump","\u26FD");
        put("mouse2","\uD83D\uDC01");
        put("nine","\u0039\u20E3");
        put("basketball","\uD83C\uDFC0");
        put("earth_asia","\uD83C\uDF0F");
        put("heart_eyes","\uD83D\uDE0D");
        put("arrow_heading_down","\u2935\uFE0F");
        put("fearful","\uD83D\uDE28");
        put("o","\u2B55\uFE0F");
        put("waning_gibbous_moon","\uD83C\uDF16");
        put("pensive","\uD83D\uDE14");
        put("mahjong","\uD83C\uDC04");
        put("closed_umbrella","\uD83C\uDF02");
        put("grinning","\uD83D\uDE00");
        put("mag_right","\uD83D\uDD0E");
        put("round_pushpin","\uD83D\uDCCD");
        put("nut_and_bolt","\uD83D\uDD29");
        put("no_bell","\uD83D\uDD15");
        put("incoming_envelope","\uD83D\uDCE8");
        put("repeat","\uD83D\uDD01");
        put("notebook_with_decorative_cover","\uD83D\uDCD4");
        put("arrow_forward","\u25B6\uFE0F");
        put("dvd","\uD83D\uDCC0");
        put("ram","\uD83D\uDC0F");
        put("cloud","\u2601\uFE0F");
        put("curly_loop","\u27B0");
        put("trumpet","\uD83C\uDFBA");
        put("love_hotel","\uD83C\uDFE9");
        put("pig2","\uD83D\uDC16");
        put("fast_forward","\u23E9");
        put("ox","\uD83D\uDC02");
        put("checkered_flag","\uD83C\uDFC1");
        put("sunglasses","\uD83D\uDE0E");
        put("weary","\uD83D\uDE29");
        put("heavy_multiplication_x","\u2716\uFE0F");
        put("last_quarter_moon","\uD83C\uDF17");
        put("confused","\uD83D\uDE15");
        put("night_with_stars","\uD83C\uDF03");
        put("grin","\uD83D\uDE01");
        put("lock_with_ink_pen","\uD83D\uDD0F");
        put("paperclip","\uD83D\uDCCE");
        put("black_large_square","\u2B1B\uFE0F");
        put("seat","\uD83D\uDCBA");
        put("envelope_with_arrow","\uD83D\uDCE9");
        put("bookmark","\uD83D\uDD16");
        put("closed_book","\uD83D\uDCD5");
        put("repeat_one","\uD83D\uDD02");
        put("file_folder","\uD83D\uDCC1");
        put("violin","\uD83C\uDFBB");
        put("boar","\uD83D\uDC17");
        put("water_buffalo","\uD83D\uDC03");
        put("snowboarder","\uD83C\uDFC2");
        put("smirk","\uD83D\uDE0F");
        put("bath","\uD83D\uDEC0");
        put("scissors","\u2702\uFE0F");
        put("waning_crescent_moon","\uD83C\uDF18");
        put("confounded","\uD83D\uDE16");
        put("sunrise_over_mountains","\uD83C\uDF04");
        put("joy","\uD83D\uDE02");
        put("straight_ruler","\uD83D\uDCCF");
        put("computer","\uD83D\uDCBB");
        put("link","\uD83D\uDD17");
        put("arrows_clockwise","\uD83D\uDD03");
        put("book","\uD83D\uDCD6");
        put("open_book","\uD83D\uDCD6");
        put("snowflake","\u2744\uFE0F");
        put("open_file_folder","\uD83D\uDCC2");
        put("left_right_arrow","\u2194");
        put("musical_score","\uD83C\uDFBC");
        put("elephant","\uD83D\uDC18");
        put("cow2","\uD83D\uDC04");
        put("womens","\uD83D\uDEBA");
        put("runner","\uD83C\uDFC3");
        put("running","\uD83C\uDFC3");
        put("bathtub","\uD83D\uDEC1");
        put("crescent_moon","\uD83C\uDF19");
        put("arrow_up_down","\u2195");
        put("sunrise","\uD83C\uDF05");
        put("smiley","\uD83D\uDE03");
        put("kissing","\uD83D\uDE17");
        put("black_medium_small_square","\u25FE\uFE0F");
        put("briefcase","\uD83D\uDCBC");
        put("radio_button","\uD83D\uDD18");
        put("arrows_counterclockwise","\uD83D\uDD04");
        put("green_book","\uD83D\uDCD7");
        put("black_small_square","\u25AA\uFE0F");
        put("page_with_curl","\uD83D\uDCC3");
        put("arrow_upper_left","\u2196");
        put("running_shirt_with_sash","\uD83C\uDFBD");
        put("octopus","\uD83D\uDC19");
        put("tiger2","\uD83D\uDC05");
        put("restroom","\uD83D\uDEBB");
        put("surfer","\uD83C\uDFC4");
        put("passport_control","\uD83D\uDEC2");
        put("slot_machine","\uD83C\uDFB0");
        put("phone","\u260E");
        put("telephone","\u260E");
        put("kissing_heart","\uD83D\uDE18");
        put("city_sunset","\uD83C\uDF06");
        put("arrow_upper_right","\u2197");
        put("smile","\uD83D\uDE04");
        put("minidisc","\uD83D\uDCBD");
        put("back","\uD83D\uDD19");
        put("low_brightness","\uD83D\uDD05");
        put("blue_book","\uD83D\uDCD8");
        put("page_facing_up","\uD83D\uDCC4");
        put("moneybag","\uD83D\uDCB0");
        put("arrow_lower_right","\u2198");
        put("tennis","\uD83C\uDFBE");
        put("baby_symbol","\uD83D\uDEBC");
        put("circus_tent","\uD83C\uDFAA");
        put("leopard","\uD83D\uDC06");
        put("black_circle","\u26AB\uFE0F");
        put("customs","\uD83D\uDEC3");
        put("8ball","\uD83C\uDFB1");
        put("kissing_smiling_eyes","\uD83D\uDE19");
        put("city_sunrise","\uD83C\uDF07");
        put("heavy_plus_sign","\u2795");
        put("arrow_lower_left","\u2199");
        put("sweat_smile","\uD83D\uDE05");
        put("ballot_box_with_check","\u2611");
        put("floppy_disk","\uD83D\uDCBE");
        put("high_brightness","\uD83D\uDD06");
        put("muscle","\uD83D\uDCAA");
        put("orange_book","\uD83D\uDCD9");
        put("date","\uD83D\uDCC5");
        put("currency_exchange","\uD83D\uDCB1");
        put("heavy_minus_sign","\u2796");
        put("ski","\uD83C\uDFBF");
        put("toilet","\uD83D\uDEBD");
        put("ticket","\uD83C\uDFAB");
        put("rabbit2","\uD83D\uDC07");
        put("umbrella","\u2614\uFE0F");
        put("trophy","\uD83C\uDFC6");
        put("baggage_claim","\uD83D\uDEC4");
        put("game_die","\uD83C\uDFB2");
        put("potable_water","\uD83D\uDEB0");
        put("rainbow","\uD83C\uDF08");
        put("laughing","\uD83D\uDE06");
        put("satisfied","\uD83D\uDE06");
        put("heavy_division_sign","\u2797");
        put("cd","\uD83D\uDCBF");
        put("mute","\uD83D\uDD07");
        put("dizzy","\uD83D\uDCAB");
        put("calendar","\uD83D\uDCC6");
        put("heavy_dollar_sign","\uD83D\uDCB2");
        put("wc","\uD83D\uDEBE");
        put("clapper","\uD83C\uDFAC");
        put("umbrella","\u2614");
        put("cat2","\uD83D\uDC08");
        put("horse_racing","\uD83C\uDFC7");
        put("door","\uD83D\uDEAA");
        put("bowling","\uD83C\uDFB3");
        put("non-potable_water","\uD83D\uDEB1");
        put("left_luggage","\uD83D\uDEC5");
        put("bridge_at_night","\uD83C\uDF09");
        put("innocent","\uD83D\uDE07");
        put("coffee","\u2615");
        put("white_large_square","\u2B1C\uFE0F");
        put("speaker","\uD83D\uDD08");
        put("speech_balloon","\uD83D\uDCAC");
        put("card_index","\uD83D\uDCC7");
        put("credit_card","\uD83D\uDCB3");
        put("wavy_dash","\u3030");
        put("shower","\uD83D\uDEBF");
        put("performing_arts","\uD83C\uDFAD");
        put("dragon","\uD83D\uDC09");
        put("no_entry_sign","\uD83D\uDEAB");
        put("football","\uD83C\uDFC8");
        put("flower_playing_cards","\uD83C\uDFB4");
        put("bike","\uD83D\uDEB2");
        put("carousel_horse","\uD83C\uDFA0");
        put("smiling_imp","\uD83D\uDE08");
        put("parking","\uD83C\uDD7F\uFE0F");
        put("sound","\uD83D\uDD09");
        put("thought_balloon","\uD83D\uDCAD");
        put("sparkle","\u2747\uFE0F");
        put("chart_with_upwards_trend","\uD83D\uDCC8");
        put("yen","\uD83D\uDCB4");
        put("diamond_shape_with_a_dot_inside","\uD83D\uDCA0");
        put("video_game","\uD83C\uDFAE");
        put("smoking","\uD83D\uDEAC");
        put("rugby_football","\uD83C\uDFC9");
        put("musical_note","\uD83C\uDFB5");
        put("no_bicycles","\uD83D\uDEB3");
        put("ferris_wheel","\uD83C\uDFA1");
        put("wink","\uD83D\uDE09");
        put("vs","\uD83C\uDD9A");
        put("eight_spoked_asterisk","\u2733\uFE0F");
        put("gemini","\u264A\uFE0F");
        put("gemini","\u264A");
        put("white_flower","\uD83D\uDCAE");
        put("white_small_square","\u25AB\uFE0F");
        put("chart_with_downwards_trend","\uD83D\uDCC9");
        put("spades","\u2660\uFE0F");
        put("dollar","\uD83D\uDCB5");
        put("five","\u0035\uFE0F\u20E3");
        put("bulb","\uD83D\uDCA1");
        put("dart","\uD83C\uDFAF");
        put("no_smoking","\uD83D\uDEAD");
        put("zero","\u0030\u20E3");
        put("notes","\uD83C\uDFB6");
        put("cancer","\u264B");
        put("roller_coaster","\uD83C\uDFA2");
        put("mountain_cableway","\uD83D\uDEA0");
        put("bicyclist","\uD83D\uDEB4");
        put("no_entry","\u26D4\uFE0F");
        put("seven","\u0037\uFE0F\u20E3");
        put("leftwards_arrow_with_hook","\u21A9\uFE0F");
        put("100","\uD83D\uDCAF");
        put("leo","\u264C");
        put("arrow_backward","\u25C0");
        put("euro","\uD83D\uDCB6");
        put("anger","\uD83D\uDCA2");
        put("black_large_square","\u2B1B");
        put("put_litter_in_its_place","\uD83D\uDEAE");
        put("saxophone","\uD83C\uDFB7");
        put("mountain_bicyclist","\uD83D\uDEB5");
        put("virgo","\u264D");
        put("fishing_pole_and_fish","\uD83C\uDFA3");
        put("aerial_tramway","\uD83D\uDEA1");
        put("green_heart","\uD83D\uDC9A");
        put("white_large_square","\u2B1C");
        put("libra","\u264E");
        put("arrow_heading_up","\u2934");
        put("pound","\uD83D\uDCB7");
        put("bomb","\uD83D\uDCA3");
        put("do_not_litter","\uD83D\uDEAF");
        put("coffee","\u2615\uFE0F");
        put("arrow_left","\u2B05");
        put("guitar","\uD83C\uDFB8");
        put("walking","\uD83D\uDEB6");
        put("microphone","\uD83C\uDFA4");
        put("scorpius","\u264F");
        put("arrow_heading_down","\u2935");
        put("ship","\uD83D\uDEA2");
        put("mahjong","\uD83C\uDC04\uFE0F");
        put("sagittarius","\u2650");
        put("yellow_heart","\uD83D\uDC9B");
        put("arrow_up","\u2B06");
        put("registered","\u00AE");
        put("truck","\uD83D\uDE9A");
        put("money_with_wings","\uD83D\uDCB8");
        put("zzz","\uD83D\uDCA4");
        put("capricorn","\u2651");
        put("arrow_down","\u2B07");
        put("scissors","\u2702");
        put("musical_keyboard","\uD83C\uDFB9");
        put("movie_camera","\uD83C\uDFA5");
        put("rowboat","\uD83D\uDEA3");
        put("no_pedestrians","\uD83D\uDEB7");
        put("aquarius","\u2652");
        put("purple_heart","\uD83D\uDC9C");
        put("cl","\uD83C\uDD91");
        put("articulated_lorry","\uD83D\uDE9B");
        put("chart","\uD83D\uDCB9");
        put("boom","\uD83D\uDCA5");
        put("collision","\uD83D\uDCA5");
        put("pisces","\u2653");
        put("wind_chime","\uD83C\uDF90");
        put("children_crossing","\uD83D\uDEB8");
        put("cinema","\uD83C\uDFA6");
        put("speedboat","\uD83D\uDEA4");
        put("point_up","\u261D\uFE0F");
        put("gift_heart","\uD83D\uDC9D");
        put("cool","\uD83C\uDD92");
        put("white_check_mark","\u2705");
        put("bouquet","\uD83D\uDC90");
        put("kr","\uD83C\uDDF0\uD83C\uDDF7");
        put("tractor","\uD83D\uDE9C");
        put("tm","\u2122");
        put("confetti_ball","\uD83C\uDF8A");
        put("sweat_drops","\uD83D\uDCA6");
        put("rice_scene","\uD83C\uDF91");
        put("mens","\uD83D\uDEB9");
        put("headphones","\uD83C\uDFA7");
        put("white_circle","\u26AA");
        put("traffic_light","\uD83D\uDEA5");
        put("revolving_hearts","\uD83D\uDC9E");
        put("pill","\uD83D\uDC8A");
        put("eight_pointed_black_star","\u2734\uFE0F");
        put("free","\uD83C\uDD93");
        put("couple_with_heart","\uD83D\uDC91");
        put("black_circle","\u26AB");
        put("cancer","\u264B\uFE0F");
        put("monorail","\uD83D\uDE9D");
        put("arrow_backward","\u25C0\uFE0F");
        put("tanabata_tree","\uD83C\uDF8B");
        put("droplet","\uD83D\uDCA7");
        put("virgo","\u264D\uFE0F");
        put("fr","\uD83C\uDDEB\uD83C\uDDF7");
        put("white_medium_square","\u25FB");
        put("school_satchel","\uD83C\uDF92");
        put("minibus","\uD83D\uDE90");
        put("one","\u0031\u20E3");
        put("art","\uD83C\uDFA8");
        put("airplane","\u2708");
        put("vertical_traffic_light","\uD83D\uDEA6");
        put("v","\u270C\uFE0F");
        put("heart_decoration","\uD83D\uDC9F");
        put("black_medium_square","\u25FC");
        put("kiss","\uD83D\uDC8B");
        put("id","\uD83C\uDD94");
        put("wedding","\uD83D\uDC92");
        put("email","\u2709");
        put("envelope","\u2709");
        put("mountain_railway","\uD83D\uDE9E");
        put("crossed_flags","\uD83C\uDF8C");
        put("dash","\uD83D\uDCA8");
        put("tram","\uD83D\uDE8A");
        put("mortar_board","\uD83C\uDF93");
        put("white_medium_small_square","\u25FD");
        put("ambulance","\uD83D\uDE91");
        put("recycle","\u267B\uFE0F");
        put("heart","\u2764\uFE0F");
        put("tophat","\uD83C\uDFA9");
        put("construction","\uD83D\uDEA7");
        put("ab","\uD83C\uDD8E");
        put("black_medium_small_square","\u25FE");
        put("love_letter","\uD83D\uDC8C");
        put("heartbeat","\uD83D\uDC93");
        put("new","\uD83C\uDD95");
        put("suspension_railway","\uD83D\uDE9F");
        put("ru","\uD83C\uDDF7\uD83C\uDDFA");
        put("bamboo","\uD83C\uDF8D");
        put("hankey","\uD83D\uDCA9");
        put("poop","\uD83D\uDCA9");
        put("shit","\uD83D\uDCA9");
        put("train","\uD83D\uDE8B");
        put("fire_engine","\uD83D\uDE92");
        put("ribbon","\uD83C\uDF80");
        put("rotating_light","\uD83D\uDEA8");
        put("arrow_up","\u2B06\uFE0F");
        put("part_alternation_mark","\u303D\uFE0F");
        put("ring","\uD83D\uDC8D");
        put("golf","\u26F3\uFE0F");
        put("broken_heart","\uD83D\uDC94");
        put("ng","\uD83C\uDD96");
        put("skull","\uD83D\uDC80");
        put("dolls","\uD83C\uDF8E");
        put("bus","\uD83D\uDE8C");
        put("beer","\uD83C\uDF7A");
        put("police_car","\uD83D\uDE93");
        put("gift","\uD83C\uDF81");
        put("triangular_flag_on_post","\uD83D\uDEA9");
        put("gem","\uD83D\uDC8E");
        put("japanese_goblin","\uD83D\uDC7A");
        put("two_hearts","\uD83D\uDC95");
        put("ok","\uD83C\uDD97");
        put("information_desk_person","\uD83D\uDC81");
        put("flags","\uD83C\uDF8F");
        put("oncoming_bus","\uD83D\uDE8D");
        put("beers","\uD83C\uDF7B");
        put("sparkles","\u2728");
        put("oncoming_police_car","\uD83D\uDE94");
        put("birthday","\uD83C\uDF82");
        put("rocket","\uD83D\uDE80");
        put("one","\u0031\uFE0F\u20E3");
        put("couplekiss","\uD83D\uDC8F");
        put("ghost","\uD83D\uDC7B");
        put("sparkling_heart","\uD83D\uDC96");
        put("sos","\uD83C\uDD98");
        put("guardsman","\uD83D\uDC82");
        put("u7121","\uD83C\uDE1A\uFE0F");
        put("a","\uD83C\uDD70");
        put("trolleybus","\uD83D\uDE8E");
        put("baby_bottle","\uD83C\uDF7C");
        put("three","\u0033\uFE0F\u20E3");
        put("ophiuchus","\u26CE");
        put("taxi","\uD83D\uDE95");
        put("jack_o_lantern","\uD83C\uDF83");
        put("helicopter","\uD83D\uDE81");
        put("anchor","\u2693");
        put("congratulations","\u3297\uFE0F");
        put("o2","\uD83C\uDD7E");
        put("angel","\uD83D\uDC7C");
        put("rewind","\u23EA");
        put("heartpulse","\uD83D\uDC97");
        put("snowflake","\u2744");
        put("dancer","\uD83D\uDC83");
        put("up","\uD83C\uDD99");
        put("b","\uD83C\uDD71");
        put("leo","\u264C\uFE0F");
        put("busstop","\uD83D\uDE8F");
        put("libra","\u264E\uFE0F");
        put("secret","\u3299\uFE0F");
        put("star","\u2B50\uFE0F");
        put("oncoming_taxi","\uD83D\uDE96");
        put("christmas_tree","\uD83C\uDF84");
        put("steam_locomotive","\uD83D\uDE82");
        put("cake","\uD83C\uDF70");
        put("arrow_double_up","\u23EB");
        put("two","\u0032\u20E3");
        put("watch","\u231A\uFE0F");
        put("relaxed","\u263A\uFE0F");
        put("parking","\uD83C\uDD7F");
        put("alien","\uD83D\uDC7D");
        put("sagittarius","\u2650\uFE0F");
        put("cupid","\uD83D\uDC98");
        put("church","\u26EA");
        put("lipstick","\uD83D\uDC84");
        put("arrow_double_down","\u23EC");
        put("bride_with_veil","\uD83D\uDC70");
        put("cookie","\uD83C\uDF6A");
        put("car","\uD83D\uDE97");
        put("red_car","\uD83D\uDE97");
        put("santa","\uD83C\uDF85");
        put("railway_car","\uD83D\uDE83");
        put("bento","\uD83C\uDF71");
        put("snowman","\u26C4\uFE0F");
        put("sparkle","\u2747");
        put("space_invader","\uD83D\uDC7E");
        put("family","\uD83D\uDC6A");
        put("blue_heart","\uD83D\uDC99");
        put("nail_care","\uD83D\uDC85");
        put("no_entry","\u26D4");
        put("person_with_blond_hair","\uD83D\uDC71");
        put("chocolate_bar","\uD83C\uDF6B");
        put("oncoming_automobile","\uD83D\uDE98");
        put("fireworks","\uD83C\uDF86");
        put("bullettrain_side","\uD83D\uDE84");
        put("stew","\uD83C\uDF72");
        put("arrow_left","\u2B05\uFE0F");
        put("arrow_down","\u2B07\uFE0F");
        put("alarm_clock","\u23F0");
        put("it","\uD83C\uDDEE\uD83C\uDDF9");
        put("fountain","\u26F2\uFE0F");
        put("imp","\uD83D\uDC7F");
        put("couple","\uD83D\uDC6B");
        put("massage","\uD83D\uDC86");
        put("man_with_gua_pi_mao","\uD83D\uDC72");
        put("candy","\uD83C\uDF6C");
        put("blue_car","\uD83D\uDE99");
        put("sparkler","\uD83C\uDF87");
        put("bullettrain_front","\uD83D\uDE85");
        put("egg","\uD83C\uDF73");
        put("jp","\uD83C\uDDEF\uD83C\uDDF5");
        put("heart","\u2764");
        put("us","\uD83C\uDDFA\uD83C\uDDF8");
        put("two_men_holding_hands","\uD83D\uDC6C");
        put("arrow_right","\u27A1");
        put("haircut","\uD83D\uDC87");
        put("man_with_turban","\uD83D\uDC73");
        put("hourglass_flowing_sand","\u23F3");
        put("lollipop","\uD83C\uDF6D");
        put("interrobang","\u2049\uFE0F");
        put("balloon","\uD83C\uDF88");
        put("train2","\uD83D\uDE86");
        put("fork_and_knife","\uD83C\uDF74");
        put("arrow_right","\u27A1\uFE0F");
        put("sweet_potato","\uD83C\uDF60");
        put("airplane","\u2708\uFE0F");
        put("fountain","\u26F2");
        put("two_women_holding_hands","\uD83D\uDC6D");
        put("barber","\uD83D\uDC88");
        put("tent","\u26FA\uFE0F");
        put("older_man","\uD83D\uDC74");
        put("high_heel","\uD83D\uDC60");
        put("golf","\u26F3");
        put("custard","\uD83C\uDF6E");
        put("rice","\uD83C\uDF5A");
        put("tada","\uD83C\uDF89");
        put("metro","\uD83D\uDE87");
        put("tea","\uD83C\uDF75");
        put("dango","\uD83C\uDF61");
        put("clock530","\uD83D\uDD60");
        put("cop","\uD83D\uDC6E");
        put("womans_clothes","\uD83D\uDC5A");
        put("syringe","\uD83D\uDC89");
        put("leftwards_arrow_with_hook","\u21A9");
        put("older_woman","\uD83D\uDC75");
        put("scorpius","\u264F\uFE0F");
        put("sandal","\uD83D\uDC61");
        put("clubs","\u2663\uFE0F");
        put("boat","\u26F5");
        put("sailboat","\u26F5");
        put("honey_pot","\uD83C\uDF6F");
        put("curry","\uD83C\uDF5B");
        put("light_rail","\uD83D\uDE88");
        put("three","\u0033\u20E3");
        put("sake","\uD83C\uDF76");
        put("oden","\uD83C\uDF62");
        put("clock11","\uD83D\uDD5A");
        put("clock630","\uD83D\uDD61");
        put("hourglass","\u231B\uFE0F");
        put("dancers","\uD83D\uDC6F");
        put("capricorn","\u2651\uFE0F");
        put("purse","\uD83D\uDC5B");
        put("loop","\u27BF");
        put("hash","\u0023\uFE0F\u20E3");
        put("baby","\uD83D\uDC76");
        put("m","\u24C2");
        put("boot","\uD83D\uDC62");
        put("ramen","\uD83C\uDF5C");
        put("station","\uD83D\uDE89");
        put("wine_glass","\uD83C\uDF77");
        put("watch","\u231A");
        put("sushi","\uD83C\uDF63");
        put("sunny","\u2600");
        put("anchor","\u2693\uFE0F");
        put("partly_sunny","\u26C5\uFE0F");
        put("clock12","\uD83D\uDD5B");
        put("clock730","\uD83D\uDD62");
        put("ideograph_advantage","\uD83C\uDE50");
        put("hourglass","\u231B");
        put("handbag","\uD83D\uDC5C");
        put("cloud","\u2601");
        put("construction_worker","\uD83D\uDC77");
        put("footprints","\uD83D\uDC63");
        put("spaghetti","\uD83C\uDF5D");
        put("cocktail","\uD83C\uDF78");
        put("fried_shrimp","\uD83C\uDF64");
        put("pear","\uD83C\uDF50");
        put("clock130","\uD83D\uDD5C");
        put("clock830","\uD83D\uDD63");
        put("accept","\uD83C\uDE51");
        put("boat","\u26F5\uFE0F");
        put("sailboat","\u26F5\uFE0F");
        put("pouch","\uD83D\uDC5D");
        put("princess","\uD83D\uDC78");
        put("bust_in_silhouette","\uD83D\uDC64");
        put("eight","\u0038\uFE0F\u20E3");
        put("open_hands","\uD83D\uDC50");
        put("left_right_arrow","\u2194\uFE0F");
        put("arrow_upper_left","\u2196\uFE0F");
        put("bread","\uD83C\uDF5E");
        put("tangerine","\uD83C\uDF4A");
        put("tropical_drink","\uD83C\uDF79");
        put("fish_cake","\uD83C\uDF65");
        put("peach","\uD83C\uDF51");
        put("clock230","\uD83D\uDD5D");
        put("clock930","\uD83D\uDD64");
        put("aries","\u2648\uFE0F");
        put("clock1","\uD83D\uDD50");
        put("mans_shoe","\uD83D\uDC5E");
        put("shoe","\uD83D\uDC5E");
        put("point_up","\u261D");
        put("facepunch","\uD83D\uDC4A");
        put("punch","\uD83D\uDC4A");
        put("japanese_ogre","\uD83D\uDC79");
        put("busts_in_silhouette","\uD83D\uDC65");
        put("crown","\uD83D\uDC51");
        put("fries","\uD83C\uDF5F");
        put("lemon","\uD83C\uDF4B");
        put("icecream","\uD83C\uDF66");
        put("cherries","\uD83C\uDF52");
        put("black_small_square","\u25AA");
        put("email","\u2709\uFE0F");
        put("envelope","\u2709\uFE0F");
        put("clock330","\uD83D\uDD5E");
        put("clock1030","\uD83D\uDD65");
        put("clock2","\uD83D\uDD51");
        put("m","\u24C2\uFE0F");
        put("athletic_shoe","\uD83D\uDC5F");
        put("wave","\uD83D\uDC4B");
        put("white_small_square","\u25AB");
        put("boy","\uD83D\uDC66");
        put("bangbang","\u203C");
        put("womans_hat","\uD83D\uDC52");
        put("banana","\uD83C\uDF4C");
        put("speak_no_evil","\uD83D\uDE4A");
        put("shaved_ice","\uD83C\uDF67");
        put("phone","\u260E\uFE0F");
        put("telephone","\u260E\uFE0F");
        put("strawberry","\uD83C\uDF53");
        put("clock430","\uD83D\uDD5F");
        put("cn","\uD83C\uDDE8\uD83C\uDDF3");
        put("clock1130","\uD83D\uDD66");
        put("clock3","\uD83D\uDD52");
        put("ok_hand","\uD83D\uDC4C");
        put("diamonds","\u2666\uFE0F");
        put("girl","\uD83D\uDC67");
        put("relaxed","\u263A");
        put("eyeglasses","\uD83D\uDC53");
        put("pineapple","\uD83C\uDF4D");
        put("raising_hand","\uD83D\uDE4B");
        put("four","\u0034\u20E3");
        put("ice_cream","\uD83C\uDF68");
        put("information_source","\u2139\uFE0F");
        put("hamburger","\uD83C\uDF54");
        put("four_leaf_clover","\uD83C\uDF40");
        put("pencil2","\u270F\uFE0F");
        put("u55b6","\uD83C\uDE3A");
        put("clock1230","\uD83D\uDD67");
        put("clock4","\uD83D\uDD53");
        put("part_alternation_mark","\u303D");
        put("aquarius","\u2652\uFE0F");
        put("+1","\uD83D\uDC4D");
        put("thumbsup","\uD83D\uDC4D");
        put("man","\uD83D\uDC68");
        put("necktie","\uD83D\uDC54");
        put("eyes","\uD83D\uDC40");
        put("bangbang","\u203C\uFE0F");
        put("apple","\uD83C\uDF4E");
        put("raised_hands","\uD83D\uDE4C");
        put("hibiscus","\uD83C\uDF3A");
        put("doughnut","\uD83C\uDF69");
        put("pizza","\uD83C\uDF55");
        put("maple_leaf","\uD83C\uDF41");
        put("clock5","\uD83D\uDD54");
        put("gb","\uD83C\uDDEC\uD83C\uDDE7");
        put("uk","\uD83C\uDDEC\uD83C\uDDE7");
        put("-1","\uD83D\uDC4E");
        put("thumbsdown","\uD83D\uDC4E");
        put("wolf","\uD83D\uDC3A");
        put("woman","\uD83D\uDC69");
        put("shirt","\uD83D\uDC55");
        put("tshirt","\uD83D\uDC55");
        put("green_apple","\uD83C\uDF4F");
        put("person_frowning","\uD83D\uDE4D");
        put("sunflower","\uD83C\uDF3B");
        put("meat_on_bone","\uD83C\uDF56");
        put("fallen_leaf","\uD83C\uDF42");
        put("scream_cat","\uD83D\uDE40");
        put("small_red_triangle","\uD83D\uDD3A");
        put("clock6","\uD83D\uDD55");
        put("clap","\uD83D\uDC4F");
        put("bear","\uD83D\uDC3B");
        put("warning","\u26A0\uFE0F");
        put("jeans","\uD83D\uDC56");
        put("ear","\uD83D\uDC42");
        put("arrow_up_down","\u2195\uFE0F");
        put("arrow_upper_right","\u2197\uFE0F");
        put("person_with_pouting_face","\uD83D\uDE4E");
        put("blossom","\uD83C\uDF3C");
        put("smiley_cat","\uD83D\uDE3A");
        put("poultry_leg","\uD83C\uDF57");
        put("leaves","\uD83C\uDF43");
        put("fist","\u270A");
        put("es","\uD83C\uDDEA\uD83C\uDDF8");
        put("small_red_triangle_down","\uD83D\uDD3B");
        put("white_medium_square","\u25FB\uFE0F");
        put("clock7","\uD83D\uDD56");
        put("tv","\uD83D\uDCFA");
        put("taurus","\u2649\uFE0F");
        put("de","\uD83C\uDDE9\uD83C\uDDEA");
        put("panda_face","\uD83D\uDC3C");
        put("hand","\u270B");
        put("raised_hand","\u270B");
        put("dress","\uD83D\uDC57");
        put("nose","\uD83D\uDC43");
        put("arrow_forward","\u25B6");
        put("pray","\uD83D\uDE4F");
        put("corn","\uD83C\uDF3D");
        put("heart_eyes_cat","\uD83D\uDE3B");
        put("rice_cracker","\uD83C\uDF58");
        put("mushroom","\uD83C\uDF44");
        put("chestnut","\uD83C\uDF30");
        put("v","\u270C");
        put("arrow_up_small","\uD83D\uDD3C");
        put("clock8","\uD83D\uDD57");
        put("radio","\uD83D\uDCFB");
        put("pig_nose","\uD83D\uDC3D");
        put("kimono","\uD83D\uDC58");
        put("lips","\uD83D\uDC44");
        put("rabbit","\uD83D\uDC30");
        put("ear_of_rice","\uD83C\uDF3E");
        put("smirk_cat","\uD83D\uDE3C");
        put("interrobang","\u2049");
        put("rice_ball","\uD83C\uDF59");
        put("mount_fuji","\uD83D\uDDFB");
        put("tomato","\uD83C\uDF45");
        put("seedling","\uD83C\uDF31");
        put("arrow_down_small","\uD83D\uDD3D");
        put("clock9","\uD83D\uDD58");
        put("vhs","\uD83D\uDCFC");
        put("church","\u26EA\uFE0F");
        put("beginner","\uD83D\uDD30");
        put("u7981","\uD83C\uDE32");
        put("feet","\uD83D\uDC3E");
        put("paw_prints","\uD83D\uDC3E");
        put("hearts","\u2665\uFE0F");
        put("dromedary_camel","\uD83D\uDC2A");
        put("bikini","\uD83D\uDC59");
        put("pencil2","\u270F");
        put("tongue","\uD83D\uDC45");
        put("cat","\uD83D\uDC31");
        put("european_castle","\uD83C\uDFF0");
        put("herb","\uD83C\uDF3F");
        put("kissing_cat","\uD83D\uDE3D");
        put("five","\u0035\u20E3");
        put("tokyo_tower","\uD83D\uDDFC");
        put("seven","\u0037\u20E3");
        put("eggplant","\uD83C\uDF46");
        put("ballot_box_with_check","\u2611\uFE0F");
        put("spades","\u2660");
        put("evergreen_tree","\uD83C\uDF32");
        put("cold_sweat","\uD83D\uDE30");
        put("hocho","\uD83D\uDD2A");
        put("knife","\uD83D\uDD2A");
        put("clock10","\uD83D\uDD59");
        put("two","\u0032\uFE0F\u20E3");
        put("trident","\uD83D\uDD31");
        put("u7a7a","\uD83C\uDE33");
        put("aries","\u2648");
        put("newspaper","\uD83D\uDCF0");
        put("congratulations","\u3297");
        put("pisces","\u2653\uFE0F");
        put("camel","\uD83D\uDC2B");
        put("point_up_2","\uD83D\uDC46");
        put("convenience_store","\uD83C\uDFEA");
        put("dragon_face","\uD83D\uDC32");
        put("hash","\u0023\u20E3");
        put("black_nib","\u2712");
        put("pouting_cat","\uD83D\uDE3E");
        put("sleepy","\uD83D\uDE2A");
        put("statue_of_liberty","\uD83D\uDDFD");
        put("taurus","\u2649");
        put("grapes","\uD83C\uDF47");
        put("no_good","\uD83D\uDE45");
        put("deciduous_tree","\uD83C\uDF33");
        put("scream","\uD83D\uDE31");
        put("wheelchair","\u267F\uFE0F");
        put("black_nib","\u2712\uFE0F");
        put("heavy_check_mark","\u2714\uFE0F");
        put("four","\u0034\uFE0F\u20E3");
        put("gun","\uD83D\uDD2B");
        put("mailbox_closed","\uD83D\uDCEA");
        put("black_square_button","\uD83D\uDD32");
        put("u5408","\uD83C\uDE34");
        put("secret","\u3299");
        put("iphone","\uD83D\uDCF1");
        put("recycle","\u267B");
        put("clubs","\u2663");
        put("dolphin","\uD83D\uDC2C");
        put("flipper","\uD83D\uDC2C");
        put("point_down","\uD83D\uDC47");
        put("school","\uD83C\uDFEB");
        put("whale","\uD83D\uDC33");
        put("heavy_check_mark","\u2714");
        put("warning","\u26A0");
        put("tired_face","\uD83D\uDE2B");
        put("japan","\uD83D\uDDFE");
        put("copyright","\u00A9");
        put("melon","\uD83C\uDF48");
        put("crying_cat_face","\uD83D\uDE3F");
        put("palm_tree","\uD83C\uDF34");
        put("astonished","\uD83D\uDE32");
        put("stars","\uD83C\uDF20");
        put("ok_woman","\uD83D\uDE46");
        put("six","\u0036\uFE0F\u20E3");
        put("microscope","\uD83D\uDD2C");
        put("u7121","\uD83C\uDE1A");
        put("mailbox","\uD83D\uDCEB");
        put("u6307","\uD83C\uDE2F\uFE0F");
        put("white_square_button","\uD83D\uDD33");
        put("zap","\u26A1");
        put("u6e80","\uD83C\uDE35");
        put("calling","\uD83D\uDCF2");
        put("mouse","\uD83D\uDC2D");
        put("zap","\u26A1\uFE0F");
        put("hearts","\u2665");
        put("point_left","\uD83D\uDC48");
        put("department_store","\uD83C\uDFEC");
        put("horse","\uD83D\uDC34");
        put("arrow_lower_right","\u2198\uFE0F");
        put("tropical_fish","\uD83D\uDC20");
        put("heavy_multiplication_x","\u2716");
        put("grimacing","\uD83D\uDE2C");
        put("moyai","\uD83D\uDDFF");
        put("new_moon_with_face","\uD83C\uDF1A");
        put("watermelon","\uD83C\uDF49");
        put("bow","\uD83D\uDE47");
        put("cactus","\uD83C\uDF35");
        put("flushed","\uD83D\uDE33");
        put("diamonds","\u2666");
        put("telescope","\uD83D\uDD2D");
        put("u6307","\uD83C\uDE2F");
        put("black_medium_square","\u25FC\uFE0F");
        put("mailbox_with_mail","\uD83D\uDCEC");
        put("red_circle","\uD83D\uDD34");
        put("u6709","\uD83C\uDE36");
        put("capital_abcd","\uD83D\uDD20");
        put("vibration_mode","\uD83D\uDCF3");
        put("cow","\uD83D\uDC2E");
        put("wheelchair","\u267F");
        put("point_right","\uD83D\uDC49");
        put("factory","\uD83C\uDFED");
        put("monkey_face","\uD83D\uDC35");
        put("shell","\uD83D\uDC1A");
        put("blowfish","\uD83D\uDC21");
        put("house","\uD83C\uDFE0");
        put("sob","\uD83D\uDE2D");
        put("first_quarter_moon_with_face","\uD83C\uDF1B");
        put("see_no_evil","\uD83D\uDE48");
        put("soccer","\u26BD\uFE0F");
        put("sleeping","\uD83D\uDE34");
        put("angry","\uD83D\uDE20");
        put("hotsprings","\u2668");
        put("crystal_ball","\uD83D\uDD2E");
        put("end","\uD83D\uDD1A");
        put("mailbox_with_no_mail","\uD83D\uDCED");
        put("large_blue_circle","\uD83D\uDD35");
        put("soccer","\u26BD");
        put("abcd","\uD83D\uDD21");
        put("mobile_phone_off","\uD83D\uDCF4");
        put("u6708","\uD83C\uDE37");
        put("fax","\uD83D\uDCE0");
        put("tiger","\uD83D\uDC2F");
        put("star","\u2B50");
        put("bug","\uD83D\uDC1B");
        put("izakaya_lantern","\uD83C\uDFEE");
        put("lantern","\uD83C\uDFEE");
        put("fuelpump","\u26FD\uFE0F");
        put("dog","\uD83D\uDC36");
        put("turtle","\uD83D\uDC22");
        put("house_with_garden","\uD83C\uDFE1");
        put("open_mouth","\uD83D\uDE2E");
        put("baseball","\u26BE");
        put("last_quarter_moon_with_face","\uD83C\uDF1C");
        put("kissing_closed_eyes","\uD83D\uDE1A");
        put("hear_no_evil","\uD83D\uDE49");
        put("tulip","\uD83C\uDF37");
        put("eight_spoked_asterisk","\u2733");
        put("rage","\uD83D\uDE21");
        put("dizzy_face","\uD83D\uDE35");
        put("six_pointed_star","\uD83D\uDD2F");
        put("on","\uD83D\uDD1B");
        put("postbox","\uD83D\uDCEE");
        put("u7533","\uD83C\uDE38");
        put("large_orange_diamond","\uD83D\uDD36");
        put("1234","\uD83D\uDD22");
        put("no_mobile_phones","\uD83D\uDCF5");
        put("books","\uD83D\uDCDA");
        put("satellite","\uD83D\uDCE1");
        put("x","\u274C");
        put("eight_pointed_black_star","\u2734");
        put("ant","\uD83D\uDC1C");
        put("japanese_castle","\uD83C\uDFEF");
        put("hotsprings","\u2668\uFE0F");
        put("pig","\uD83D\uDC37");
        put("hatching_chick","\uD83D\uDC23");
        put("office","\uD83C\uDFE2");
        put("hushed","\uD83D\uDE2F");
        put("six","\u0036\u20E3");
        put("full_moon_with_face","\uD83C\uDF1D");
        put("stuck_out_tongue","\uD83D\uDE1B");
        put("eight","\u0038\u20E3");
        put("cherry_blossom","\uD83C\uDF38");
        put("information_source","\u2139");
        put("cry","\uD83D\uDE22");
        put("no_mouth","\uD83D\uDE36");
        put("globe_with_meridians","\uD83C\uDF10");
        put("arrow_heading_up","\u2934\uFE0F");
        put("soon","\uD83D\uDD1C");
        put("postal_horn","\uD83D\uDCEF");
        put("u5272","\uD83C\uDE39");
        put("large_blue_diamond","\uD83D\uDD37");
        put("symbols","\uD83D\uDD23");
        put("signal_strength","\uD83D\uDCF6");
        put("name_badge","\uD83D\uDCDB");
        put("loudspeaker","\uD83D\uDCE2");
        put("negative_squared_cross_mark","\u274E");
        put("arrow_right_hook","\u21AA\uFE0F");
        put("bee","\uD83D\uDC1D");
        put("honeybee","\uD83D\uDC1D");
        put("sunny","\u2600\uFE0F");
        put("frog","\uD83D\uDC38");
        put("baby_chick","\uD83D\uDC24");
        put("goat","\uD83D\uDC10");
        put("post_office","\uD83C\uDFE3");
        put("sun_with_face","\uD83C\uDF1E");
        put("stuck_out_tongue_winking_eye","\uD83D\uDE1C");
        put("ocean","\uD83C\uDF0A");
        put("rose","\uD83C\uDF39");
        put("mask","\uD83D\uDE37");
        put("persevere","\uD83D\uDE23");
        put("o","\u2B55");
        put("new_moon","\uD83C\uDF11");
        put("top","\uD83D\uDD1D");
        put("small_orange_diamond","\uD83D\uDD38");
        put("scroll","\uD83D\uDCDC");
        put("abc","\uD83D\uDD24");
        put("camera","\uD83D\uDCF7");
        put("closed_lock_with_key","\uD83D\uDD10");
        put("mega","\uD83D\uDCE3");
        put("beetle","\uD83D\uDC1E");
        put("snowman","\u26C4");
        put("crocodile","\uD83D\uDC0A");
        put("hamster","\uD83D\uDC39");
        put("exclamation","\u2757\uFE0F");
        put("heavy_exclamation_mark","\u2757\uFE0F");
        put("hatched_chick","\uD83D\uDC25");
        put("sheep","\uD83D\uDC11");
        put("european_post_office","\uD83C\uDFE4");
        put("star2","\uD83C\uDF1F");
        put("arrow_right_hook","\u21AA");
        put("volcano","\uD83C\uDF0B");
        put("stuck_out_tongue_closed_eyes","\uD83D\uDE1D");
        put("smile_cat","\uD83D\uDE38");
        put("triumph","\uD83D\uDE24");
        put("waxing_crescent_moon","\uD83C\uDF12");
        put("partly_sunny","\u26C5");
        put("neutral_face","\uD83D\uDE10");
        put("underage","\uD83D\uDD1E");
        put("loud_sound","\uD83D\uDD0A");
        put("small_blue_diamond","\uD83D\uDD39");
        put("memo","\uD83D\uDCDD");
        put("pencil","\uD83D\uDCDD");
        put("fire","\uD83D\uDD25");
        put("key","\uD83D\uDD11");
        put("outbox_tray","\uD83D\uDCE4");
        put("triangular_ruler","\uD83D\uDCD0");
        put("fish","\uD83D\uDC1F");
        put("whale2","\uD83D\uDC0B");
        put("arrow_lower_left","\u2199\uFE0F");
        put("bird","\uD83D\uDC26");
        put("question","\u2753");
        put("monkey","\uD83D\uDC12");
        put("hospital","\uD83C\uDFE5");
        put("swimmer","\uD83C\uDFCA");
        put("disappointed","\uD83D\uDE1E");
        put("milky_way","\uD83C\uDF0C");
        put("blush","\uD83D\uDE0A");
        put("joy_cat","\uD83D\uDE39");
        put("disappointed_relieved","\uD83D\uDE25");
        put("first_quarter_moon","\uD83C\uDF13");
        put("expressionless","\uD83D\uDE11");
        put("keycap_ten","\uD83D\uDD1F");
        put("grey_question","\u2754");
        put("battery","\uD83D\uDD0B");
        put("telephone_receiver","\uD83D\uDCDE");
        put("white_medium_small_square","\u25FD\uFE0F");
        put("bar_chart","\uD83D\uDCCA");
        put("video_camera","\uD83D\uDCF9");
        put("flashlight","\uD83D\uDD26");
        put("inbox_tray","\uD83D\uDCE5");
        put("lock","\uD83D\uDD12");
        put("bookmark_tabs","\uD83D\uDCD1");
        put("snail","\uD83D\uDC0C");
        put("penguin","\uD83D\uDC27");
        put("grey_exclamation","\u2755");
        put("rooster","\uD83D\uDC13");
        put("bank","\uD83C\uDFE6");
        put("worried","\uD83D\uDE1F");
        put("baseball","\u26BE\uFE0F");
        put("earth_africa","\uD83C\uDF0D");
        put("yum","\uD83D\uDE0B");
        put("frowning","\uD83D\uDE26");
        put("moon","\uD83C\uDF14");
        put("waxing_gibbous_moon","\uD83C\uDF14");
        put("unamused","\uD83D\uDE12");
        put("cyclone","\uD83C\uDF00");
        put("tent","\u26FA");
        put("electric_plug","\uD83D\uDD0C");
        put("pager","\uD83D\uDCDF");
        put("clipboard","\uD83D\uDCCB");
        put("wrench","\uD83D\uDD27");
        put("unlock","\uD83D\uDD13");
        put("package","\uD83D\uDCE6");
        put("koko","\uD83C\uDE01");
        put("ledger","\uD83D\uDCD2");
        put("snake","\uD83D\uDC0D");
        put("koala","\uD83D\uDC28");
        put("chicken","\uD83D\uDC14");
        put("atm","\uD83C\uDFE7");
        put("exclamation","\u2757");
        put("heavy_exclamation_mark","\u2757");
        put("rat","\uD83D\uDC00");
        put("white_circle","\u26AA\uFE0F");
        put("earth_americas","\uD83C\uDF0E");
        put("relieved","\uD83D\uDE0C");
        put("nine","\u0039\uFE0F\u20E3");
        put("anguished","\uD83D\uDE27");
        put("full_moon","\uD83C\uDF15");
        put("sweat","\uD83D\uDE13");
        put("foggy","\uD83C\uDF01");
        put("mag","\uD83D\uDD0D");
        put("pushpin","\uD83D\uDCCC");
        put("hammer","\uD83D\uDD28");
        put("bell","\uD83D\uDD14");
        put("e-mail","\uD83D\uDCE7");
        put("sa","\uD83C\uDE02");
        put("notebook","\uD83D\uDCD3");
        put("twisted_rightwards_arrows","\uD83D\uDD00");
        put("zero","\u0030\uFE0F\u20E3");
        put("racehorse","\uD83D\uDC0E");

        put("doge", "\uD83D\uDC36");
        put("<3", "\u2764");
        put("</3", "\uD83D\uDC94");
        put(")", "\uD83D\uDE03");
        put("-)", "\uD83D\uDE03");
        put("(", "\uD83D\uDE1E");
        put("'(", "\uD83D\uDE22");
        put("_(", "\uD83D\uDE2D");
        put(";)", "\uD83D\uDE09");
        put(";p", "\uD83D\uDE1C");
    }};

    public static Pattern EMOJI = null;

    public static final HashMap<String,String> conversionMap = new HashMap<String, String>(){{
        put("\uD83C\uDDEF\uD83C\uDDF5", "\uDBB9\uDCE5"); // JP
        put("\uD83C\uDDF0\uD83C\uDDF7", "\uDBB9\uDCEE"); // KR
        put("\uD83C\uDDE9\uD83C\uDDEA", "\uDBB9\uDCE8"); // DE
        put("\uD83C\uDDE8\uD83C\uDDF3", "\uDBB9\uDCED"); // CN
        put("\uD83C\uDDFA\uD83C\uDDF8", "\uDBB9\uDCE6"); // US
        put("\uD83C\uDDEB\uD83C\uDDF7", "\uDBB9\uDCE7"); // FR
        put("\uD83C\uDDEA\uD83C\uDDF8", "\uDBB9\uDCEB"); // ES
        put("\uD83C\uDDEE\uD83C\uDDF9", "\uDBB9\uDCE9"); // IT
        put("\uD83C\uDDF7\uD83C\uDDFA", "\uDBB9\uDCEC"); // RU
        put("\uD83C\uDDEC\uD83C\uDDE7", "\uDBB9\uDCEA"); // GB
        put("\u0030\u20E3", "\uDBBA\uDC37"); // ZERO
        put("\u0031\u20E3", "\uDBBA\uDC2E"); // ONE
        put("\u0032\u20E3", "\uDBBA\uDC2F"); // TWO
        put("\u0033\u20E3", "\uDBBA\uDC30"); // THREE
        put("\u0034\u20E3", "\uDBBA\uDC31"); // FOUR
        put("\u0035\u20E3", "\uDBBA\uDC32"); // FIVE
        put("\u0036\u20E3", "\uDBBA\uDC33"); // SIX
        put("\u0037\u20E3", "\uDBBA\uDC34"); // SEVEN
        put("\u0038\u20E3", "\uDBBA\uDC35"); // EIGHT
        put("\u0039\u20E3", "\uDBBA\uDC36"); // NINE
        put("\u0023\u20E3", "\uDBBA\uDC2C"); // HASH
        put("\u0030\uFE0F\u20E3", "\uDBBA\uDC37"); // ZERO
        put("\u0031\uFE0F\u20E3", "\uDBBA\uDC2E"); // ONE
        put("\u0032\uFE0F\u20E3", "\uDBBA\uDC2F"); // TWO
        put("\u0033\uFE0F\u20E3", "\uDBBA\uDC30"); // THREE
        put("\u0034\uFE0F\u20E3", "\uDBBA\uDC31"); // FOUR
        put("\u0035\uFE0F\u20E3", "\uDBBA\uDC32"); // FIVE
        put("\u0036\uFE0F\u20E3", "\uDBBA\uDC33"); // SIX
        put("\u0037\uFE0F\u20E3", "\uDBBA\uDC34"); // SEVEN
        put("\u0038\uFE0F\u20E3", "\uDBBA\uDC35"); // EIGHT
        put("\u0039\uFE0F\u20E3", "\uDBBA\uDC36"); // NINE
        put("\u0023\uFE0F\u20E3", "\uDBBA\uDC2C"); // HASH
        put("\u24C2\uFE0F", "\u24c2"); // M
        put("\u2139\uFE0F", "\u2139"); // INFORMATION_SOURCE
        put("\u3297\uFE0F", "\u3297"); // CONGRATULATIONS
        put("\u3299\uFE0F", "\u3299"); // SECRET
    }};

    public static Pattern CONVERSION = null;

	public static Spanned html_to_spanned(String msg) {
		return html_to_spanned(msg, false, null);
	}
	
	public static Spanned html_to_spanned(String msg, boolean linkify, final ServersDataSource.Server server) {
		if(msg == null)
			msg = "";

        boolean disableConvert = false;
        try {
            if(NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null)
                disableConvert = NetworkConnection.getInstance().getUserInfo().prefs.getBoolean("emoji-disableconvert");
        } catch (Exception e) {
        }

        if(!disableConvert && Build.VERSION.SDK_INT >= 14) {
            if(EMOJI == null) {
                String p = "\\B:(";
                for(String key : emojiMap.keySet()) {
                    if(p.length() > 4)
                        p += "|";
                    p += key.replace("-", "\\-").replace("+", "\\+").replace(")", "\\)").replace("(", "\\(");
                }
                p += "):\\B";

                EMOJI = Pattern.compile(p);
            }

            Matcher m = EMOJI.matcher(msg);
            while (m.find()) {
                if (emojiMap.containsKey(m.group(1))) {
                    msg = m.replaceFirst(emojiMap.get(m.group(1)));
                    m = EMOJI.matcher(msg);
                }
            }

            if(CONVERSION == null) {
                String p = "(";
                for(String key : conversionMap.keySet()) {
                    if(p.length() > 2)
                        p += "|";
                    p += key;
                }
                p += ")";

                CONVERSION = Pattern.compile(p);
            }

            m = CONVERSION.matcher(msg);
            while (m.find()) {
                if (conversionMap.containsKey(m.group(1))) {
                    msg = m.replaceFirst(conversionMap.get(m.group(1)));
                    m = CONVERSION.matcher(msg);
                }
            }
        }

		Spannable output = (Spannable)Html.fromHtml(msg, null, new Html.TagHandler() {
			@Override
			public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
				int len = output.length();
				if(tag.startsWith("_bg")) {
					String rgb = "#";
					if(tag.length() == 9) {
						rgb += tag.substring(3);
					} else {
						rgb += "ffffff";
					}
			        if(opening) {
			        	try {
			        		output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), len, len, Spannable.SPAN_MARK_MARK);
			        	} catch (IllegalArgumentException e) {
			        		output.setSpan(new BackgroundColorSpan(Color.parseColor("#ffffff")), len, len, Spannable.SPAN_MARK_MARK);
			        	}
			        } else {
			            Object obj = getLast(output, BackgroundColorSpan.class);
			            int where = output.getSpanStart(obj);

			            output.removeSpan(obj);

			            if (where != len) {
			            	try {
			            		output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				        	} catch (IllegalArgumentException e) {
			            		output.setSpan(new BackgroundColorSpan(Color.parseColor("#ffffff")), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				        	}
			            }
			        }
				}
			}
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			private Object getLast(Editable text, Class kind) {
		        Object[] objs = text.getSpans(0, text.length(), kind);

		        if (objs.length == 0) {
		            return null;
		        } else {
		            for(int i = objs.length;i>0;i--) {
		                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
		                    return objs[i-1];
		                }
		            }
		            return null;
		        }
		    }
		});

        String chanTypes = "#";
        if(server != null && server.CHANTYPES != null && server.CHANTYPES.length() > 0)
            chanTypes = server.CHANTYPES;

        final String pattern = "\\B([" + chanTypes + "][^\ufe0e\ufe0f\u20e3<>!?\"()\\[\\],\\s\ufe55]+)";

		if(linkify) {
            Linkify.addLinks(output, WEB_URL, null, new MatchFilter() {
		        public final boolean acceptMatch(CharSequence s, int start, int end) {
		        	if(start >= 6 && s.subSequence(start - 6, end).toString().toLowerCase().startsWith("irc://"))
		        		return false;
		        	if(start >= 7 && s.subSequence(start - 7, end).toString().toLowerCase().startsWith("ircs://"))
		        		return false;
		        	if(s.subSequence(start, end).toString().toLowerCase().startsWith("https://"))
		        		return false;
		        	if(s.subSequence(start, end).toString().toLowerCase().startsWith("http://"))
		        		return false;
                    if(start >= 1 && s.subSequence(start - 1, end).toString().matches(pattern))
                        return false;
		        	return Linkify.sUrlMatchFilter.acceptMatch(s, start, end);
		        }
		    }, new TransformFilter() {
                        @Override
                        public String transformUrl(Matcher match, String url) {
                            if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("imageviewer", true)) {
                                String lower = url.toLowerCase();
                                if(lower.matches("(^.*\\/.*\\.png$)|(^.*\\/.*\\.jpe?g$)|(^.*\\/.*\\.gif$)|" +
                                        "(^(www\\.)?flickr\\.com/photos/.*$)|" +
                                        "(^(www\\.)?instagram\\.com/p/.*$)|(^(www\\.)?instagr\\.am/p/.*$)|" +
                                        "(^(www\\.)?imgur\\.com/(?!a/).*$)|" +
                                        "(^d\\.pr/i/.*)|(^droplr\\.com/i/.*)|"+
                                        "(^cl\\.ly/.*)"
                                        ) && !lower.matches("(^cl\\.ly/robots\\.txt$)|(^cl\\.ly/image/?$)")) {
                                    return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME) + "://" + url;
                                }
                            }
                            return "http://" + url;
                        }
                    });
			//based on http://daringfireball.net/2010/07/improved_regex_for_matching_urls
			Linkify.addLinks(output, Pattern.compile("https?://(" +
                    "(?:|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)" +
                    "(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))" +
                    "+(?:(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))", Pattern.CASE_INSENSITIVE), null, null, new TransformFilter() {
                @Override
                public String transformUrl(Matcher match, String url) {
                    if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("imageviewer", true)) {
                        String lower = url.toLowerCase();
                        if(lower.matches("(^.*\\/.*\\.png$)|(^.*\\/.*\\.jpe?g$)|(^.*\\/.*\\.gif$)|" +
                                "(^https?://(www\\.)?flickr\\.com/photos/.*$)|" +
                                "(^https?://(www\\.)?instagram\\.com/p/.*$)|(^https?://(www\\.)?instagr\\.am/p/.*$)|" +
                                "(^https?://(www\\.)?imgur\\.com/(?!a/).*$)|" +
                                "(^https?://d\\.pr/i/.*)|(^https?://droplr\\.com/i/.*)|"+
                                "(^https?://cl\\.ly/.*)"
                        ) && !lower.matches("(^https?://cl\\.ly/robots\\.txt$)|(^https?://cl\\.ly/image/?$)")) {
                            if(lower.startsWith("http://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME) + "://" + url.substring(7);
                            else if(lower.startsWith("https://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME_SECURE) + "://" + url.substring(8);
                        }
                    }
                    if(url.toLowerCase().startsWith("https:"))
                        url = "https" + url.substring(5);
                    else
                        url = "http" + url.substring(4);
                    return url;
                }
            });
			Linkify.addLinks(output, Patterns.EMAIL_ADDRESS, "mailto:");
			Linkify.addLinks(output, Pattern.compile("ircs?://[^<>\"()\\[\\],\\s]+"), null, null, new TransformFilter() {
		        public final String transformUrl(final Matcher match, String url) {
		            return url.replace("#", "%23");
		        }
		    });
    		
			
		}
        if(server != null) {
            Linkify.addLinks(output, Pattern.compile(pattern), null, new MatchFilter() {
                        public final boolean acceptMatch(CharSequence s, int start, int end) {
                            try {
                                Integer.parseInt(s.subSequence(start+1, end).toString());
                                return false;
                            } catch (NumberFormatException e) {
                                return true;
                            }
                        }
                    }, new TransformFilter() {
                        public final String transformUrl(final Matcher match, String url) {
                            String channel = match.group(1);
                            try {
                                channel = URLEncoder.encode(channel, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                            }
                            return "irc://" + server.cid + "/" + channel;
                        }
                    });
        }

		return output;
	}
	
	public static String irc_to_html(String msg) {
		int pos=0;
		boolean bold=false, underline=false, italics=false;
		String fg="", bg="";
		StringBuilder builder = new StringBuilder(msg);
		
		while(pos < builder.length()) {
			if(builder.charAt(pos) == 2) { //Bold
				String html = "";
				if(bold)
					html += "</b>";
				else
					html += "<b>";
				bold = !bold;
				builder.deleteCharAt(pos);
				builder.insert(pos, html);
			} else if(builder.charAt(pos) == 22 || builder.charAt(pos) == 29) { //Italics
				String html = "";
				if(italics)
					html += "</i>";
				else
					html += "<i>";
				italics = !italics;
				builder.deleteCharAt(pos);
				builder.insert(pos, html);
			} else if(builder.charAt(pos) == 31) { //Underline
				String html = "";
				if(underline)
					html += "</u>";
				else
					html += "<u>";
				underline = !underline;
				builder.deleteCharAt(pos);
				builder.insert(pos, html);
			} else if(builder.charAt(pos) == 15) { //Formatting clear
				String html = "";
				if(fg.length() > 0) {
					html += "</font>";
					fg = "";
				}
				if(bg.length() > 0) {
					html += "</_bg" + bg + ">";
					bg = "";
				}
				if(bold) {
					html += "</b>";
					bold = false;
				}
				if(underline) {
					html += "</u>";
					underline = false;
				}
				if(italics) {
					html += "</i>";
					italics = false;
				}
				builder.deleteCharAt(pos);
				if(html.length() > 0)
					builder.insert(pos, html);
			} else if(builder.charAt(pos) == 3 || builder.charAt(pos) == 4) { //Color
				boolean rgb = (builder.charAt(pos) == 4);
				int count = 0;
				String new_fg="", new_bg="";
				builder.deleteCharAt(pos);
				if(pos < builder.length()) {
					while(pos+count < builder.length() && (
							(builder.charAt(pos+count) >= '0' && builder.charAt(pos+count) <= '9') ||
							rgb && ((builder.charAt(pos+count) >= 'a' && builder.charAt(pos+count) <= 'f') ||
							(builder.charAt(pos+count) >= 'A' && builder.charAt(pos+count) <= 'F')))) {
						if((++count == 2 && !rgb) || count == 6)
							break;
					}
					if(count > 0) {
						if(count < 3 && !rgb) {
							try {
								int col = Integer.parseInt(builder.substring(pos, pos + count));
								if(col > 15) {
									count--;
									col /= 10;
								}
								new_fg = COLOR_MAP[col];
							} catch (NumberFormatException e) {
		    					new_fg = builder.substring(pos, pos + count);
							}
						} else
	    					new_fg = builder.substring(pos, pos + count);
						builder.delete(pos, pos + count);
					}
					if(pos < builder.length() && builder.charAt(pos) == ',') {
						builder.deleteCharAt(pos);
						if(new_fg.length() == 0)
							new_fg = "clear";
						new_bg = "clear";
						count = 0;
						while(pos+count < builder.length() && (
								(builder.charAt(pos+count) >= '0' && builder.charAt(pos+count) <= '9') ||
								rgb && ((builder.charAt(pos+count) >= 'a' && builder.charAt(pos+count) <= 'f') ||
								(builder.charAt(pos+count) >= 'A' && builder.charAt(pos+count) <= 'F')))) {
                            if((++count == 2 && !rgb) || count == 6)
								break;
						}
						if(count > 0) {
							if(count < 3 && !rgb) {
								try {
									int col = Integer.parseInt(builder.substring(pos, pos + count));
									if(col > 15) {
										count--;
										col /= 10;
									}
									new_bg = COLOR_MAP[col];
								} catch (NumberFormatException e) {
			    					new_bg = builder.substring(pos, pos + count);
								}
							} else
		    					new_bg = builder.substring(pos, pos + count);
							builder.delete(pos, pos + count);
						}
					}
					String html = "";
					if(new_fg.length() == 0 && new_bg.length() == 0) {
						new_fg = "clear";
						new_bg = "clear";
					}
					if(new_fg.length() > 0 && !new_fg.equals(fg) && fg.length() > 0) {
						html += "</font>";
					}
					if(new_bg.length() > 0 && !new_bg.equals(bg) && bg.length() > 0) {
						html += "</_bg" + bg + ">";
					}
					if(new_bg.length() > 0) {
						if(!new_bg.equals(bg)) {
							if(new_bg.equals("clear")) {
								bg = "";
							} else {
								bg = "";
								if(new_bg.length() == 6) {
									bg = new_bg;
								} else if(new_bg.length() == 3) {
									bg += new_bg.charAt(0);
									bg += new_bg.charAt(0);
									bg += new_bg.charAt(1);
									bg += new_bg.charAt(1);
									bg += new_bg.charAt(2);
									bg += new_bg.charAt(2);
								} else {
									bg = "#ffffff";
								}
	    						html += "<_bg" + bg + ">";
							}
						}
					}
					if(new_fg.length() > 0) {
						if(!new_fg.equals(fg)) {
							if(new_fg.equals("clear")) {
								fg = "";
							} else {
								fg = "";
								if(new_fg.length() == 6) {
									fg = new_fg;
								} else if(new_fg.length() == 3) {
									fg += new_fg.charAt(0);
									fg += new_fg.charAt(0);
									fg += new_fg.charAt(1);
									fg += new_fg.charAt(1);
									fg += new_fg.charAt(2);
									fg += new_fg.charAt(2);
								} else {
									fg = "#000000";
								}
	    						html += "<font color=\"#" + fg + "\">";
							}
						}
					}
					builder.insert(pos, html);
				}
			}
			pos++;
		}
		if(fg.length() > 0) {
			builder.append("</font>");
		}
		if(bg.length() > 0) {
			builder.append("</_bg" + bg + ">");
		}
		if(bold)
			builder.append("</b>");
		if(underline)
			builder.append("</u>");
		if(italics)
			builder.append("</i>");

		return builder.toString();
	}
}
