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

package com.irccloud.android;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.emoji.text.EmojiCompat;
import androidx.core.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.util.Patterns;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;

import com.crashlytics.android.Crashlytics;
import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.UsersList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.model.User;

import org.xml.sax.XMLReader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric.sdk.android.services.common.Crash;

public class ColorFormatter {
    //From: https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
            "(?:"
                    + "(?:aaa|aarp|abarth|abb|abbott|abbvie|abc|able|abogado|abudhabi|academy|accenture|accountant|accountants|aco|actor|adac|ads|adult|aeg|aero|aetna|afamilycompany|afl|africa|agakhan|agency|aig|aigo|airbus|airforce|airtel|akdn|alfaromeo|alibaba|alipay|allfinanz|allstate|ally|alsace|alstom|americanexpress|americanfamily|amex|amfam|amica|amsterdam|analytics|android|anquan|anz|aol|apartments|app|apple|aquarelle|arab|aramco|archi|army|arpa|art|arte|asda|asia|associates|athleta|attorney|auction|audi|audible|audio|auspost|author|auto|autos|avianca|aws|axa|azure|a[cdefgilmoqrstuwxz])"
                    + "|(?:baby|baidu|banamex|bananarepublic|band|bank|bar|barcelona|barclaycard|barclays|barefoot|bargains|baseball|basketball|bauhaus|bayern|bbc|bbt|bbva|bcg|bcn|beats|beauty|beer|bentley|berlin|best|bestbuy|bet|bharti|bible|bid|bike|bing|bingo|bio|biz|black|blackfriday|blockbuster|blog|bloomberg|blue|bms|bmw|bnl|bnpparibas|boats|boehringer|bofa|bom|bond|boo|book|booking|bosch|bostik|boston|bot|boutique|box|bradesco|bridgestone|broadway|broker|brother|brussels|budapest|bugatti|build|builders|business|buy|buzz|bzh|b[abdefghijmnorstvwyz])"
                    + "|(?:cab|cafe|cal|call|calvinklein|cam|camera|camp|cancerresearch|canon|capetown|capital|capitalone|car|caravan|cards|care|career|careers|cars|cartier|casa|case|caseih|cash|casino|cat|catering|catholic|cba|cbn|cbre|cbs|ceb|center|ceo|cern|cfa|cfd|chanel|channel|charity|chase|chat|cheap|chintai|christmas|chrome|chrysler|church|cipriani|circle|cisco|citadel|citi|citic|city|cityeats|claims|cleaning|click|clinic|clinique|clothing|cloud|club|clubmed|coach|codes|coffee|college|cologne|com|comcast|commbank|community|company|compare|computer|comsec|condos|construction|consulting|contact|contractors|cooking|cookingchannel|cool|coop|corsica|country|coupon|coupons|courses|credit|creditcard|creditunion|cricket|crown|crs|cruise|cruises|csc|cuisinella|cymru|cyou|c[acdfghiklmnoruvwxyz])"
                    + "|(?:dabur|dad|dance|data|date|dating|datsun|day|dclk|dds|deal|dealer|deals|degree|delivery|dell|deloitte|delta|democrat|dental|dentist|desi|design|dev|dhl|diamonds|diet|digital|direct|directory|discount|discover|dish|diy|dnp|docs|doctor|dodge|dog|doha|domains|dot|download|drive|dtv|dubai|duck|dunlop|duns|dupont|durban|dvag|dvr|d[ejkmoz])"
                    + "|(?:earth|eat|eco|edeka|edu|education|email|emerck|energy|engineer|engineering|enterprises|epson|equipment|ericsson|erni|esq|estate|esurance|etisalat|eurovision|eus|events|everbank|exchange|expert|exposed|express|extraspace|e[cegrstu])"
                    + "|(?:fage|fail|fairwinds|faith|family|fan|fans|farm|farmers|fashion|fast|fedex|feedback|ferrari|ferrero|fiat|fidelity|fido|film|final|finance|financial|fire|firestone|firmdale|fish|fishing|fit|fitness|flickr|flights|flir|florist|flowers|fly|foo|food|foodnetwork|football|ford|forex|forsale|forum|foundation|fox|free|fresenius|frl|frogans|frontdoor|frontier|ftr|fujitsu|fujixerox|fun|fund|furniture|futbol|fyi|f[ijkmor])"
                    + "|(?:gal|gallery|gallo|gallup|game|games|gap|garden|gbiz|gdn|gea|gent|genting|george|ggee|gift|gifts|gives|giving|glade|glass|gle|global|globo|gmail|gmbh|gmo|gmx|godaddy|gold|goldpoint|golf|goo|goodyear|goog|google|gop|got|gov|grainger|graphics|gratis|green|gripe|grocery|group|guardian|gucci|guge|guide|guitars|guru|g[abdefghilmnpqrstuwy])"
                    + "|(?:hair|hamburg|hangout|haus|hbo|hdfc|hdfcbank|health|healthcare|help|helsinki|here|hermes|hgtv|hiphop|hisamitsu|hitachi|hiv|hkt|hockey|holdings|holiday|homedepot|homegoods|homes|homesense|honda|honeywell|horse|hospital|host|hosting|hot|hoteles|hotels|hotmail|house|how|hsbc|hughes|hyatt|hyundai|h[kmnrtu])"
                    + "|(?:ibm|icbc|ice|icu|ieee|ifm|ikano|imamat|imdb|immo|immobilien|inc|industries|infiniti|info|ing|ink|institute|insurance|insure|int|intel|international|intuit|investments|ipiranga|irish|iselect|ismaili|ist|istanbul|itau|itv|iveco|i[delmnoqrst])"
                    + "|(?:jaguar|java|jcb|jcp|jeep|jetzt|jewelry|jio|jll|jmp|jnj|jobs|joburg|jot|joy|jpmorgan|jprs|juegos|juniper|j[emop])"
                    + "|(?:kaufen|kddi|kerryhotels|kerrylogistics|kerryproperties|kfh|kia|kim|kinder|kindle|kitchen|kiwi|koeln|komatsu|kosher|kpmg|kpn|krd|kred|kuokgroup|kyoto|k[eghimnprwyz])"
                    + "|(?:lacaixa|ladbrokes|lamborghini|lamer|lancaster|lancia|lancome|land|landrover|lanxess|lasalle|lat|latino|latrobe|law|lawyer|lds|lease|leclerc|lefrak|legal|lego|lexus|lgbt|liaison|lidl|life|lifeinsurance|lifestyle|lighting|like|lilly|limited|limo|lincoln|linde|link|lipsy|live|living|lixil|llc|loan|loans|locker|locus|loft|lol|london|lotte|lotto|love|lpl|lplfinancial|ltd|ltda|lundbeck|lupin|luxe|luxury|l[abcikrstuvy])"
                    + "|(?:macys|madrid|maif|maison|makeup|man|management|mango|map|market|marketing|markets|marriott|marshalls|maserati|mattel|mba|mckinsey|med|media|meet|melbourne|meme|memorial|men|menu|merckmsd|metlife|miami|microsoft|mil|mini|mint|mit|mitsubishi|mlb|mls|mma|mobi|mobile|mobily|moda|moe|moi|mom|monash|money|monster|mopar|mormon|mortgage|moscow|moto|motorcycles|mov|movie|movistar|msd|mtn|mtr|museum|mutual|m[acdeghklmnopqrstuvwxyz])"
                    + "|(?:nab|nadex|nagoya|name|nationwide|natura|navy|nba|nec|net|netbank|netflix|network|neustar|new|newholland|news|next|nextdirect|nexus|nfl|ngo|nhk|nico|nike|nikon|ninja|nissan|nissay|nokia|northwesternmutual|norton|now|nowruz|nowtv|nra|nrw|ntt|nyc|n[acefgilopruz])"
                    + "|(?:obi|observer|off|office|okinawa|olayan|olayangroup|oldnavy|ollo|omega|one|ong|onl|online|onyourside|ooo|open|oracle|orange|org|organic|origins|osaka|otsuka|ott|ovh|om)"
                    + "|(?:page|panasonic|paris|pars|partners|parts|party|passagens|pay|pccw|pet|pfizer|pharmacy|phd|philips|phone|photo|photography|photos|physio|piaget|pics|pictet|pictures|pid|pin|ping|pink|pioneer|pizza|place|play|playstation|plumbing|plus|pnc|pohl|poker|politie|porn|post|pramerica|praxi|press|prime|pro|prod|productions|prof|progressive|promo|properties|property|protection|pru|prudential|pub|pwc|p[aefghklmnrstwy])"
                    + "|(?:qpon|quebec|quest|qvc|qa)"
                    + "|(?:racing|radio|raid|read|realestate|realtor|realty|recipes|red|redstone|redumbrella|rehab|reise|reisen|reit|reliance|ren|rent|rentals|repair|report|republican|rest|restaurant|review|reviews|rexroth|rich|richardli|ricoh|rightathome|ril|rio|rip|rmit|rocher|rocks|rodeo|rogers|room|rsvp|rugby|ruhr|run|rwe|ryukyu|r[eosuw])"
                    + "|(?:saarland|safe|safety|sakura|sale|salon|samsclub|samsung|sandvik|sandvikcoromant|sanofi|sap|sarl|sas|save|saxo|sbi|sbs|sca|scb|schaeffler|schmidt|scholarships|school|schule|schwarz|science|scjohnson|scor|scot|search|seat|secure|security|seek|select|sener|services|ses|seven|sew|sex|sexy|sfr|shangrila|sharp|shaw|shell|shia|shiksha|shoes|shop|shopping|shouji|show|showtime|shriram|silk|sina|singles|site|ski|skin|sky|skype|sling|smart|smile|sncf|soccer|social|softbank|software|sohu|solar|solutions|song|sony|soy|space|sport|spot|spreadbetting|srl|srt|stada|staples|star|starhub|statebank|statefarm|stc|stcgroup|stockholm|storage|store|stream|studio|study|style|sucks|supplies|supply|support|surf|surgery|suzuki|swatch|swiftcover|swiss|sydney|symantec|systems|s[abcdeghijklmnorstuvxyz])"
                    + "|(?:tab|taipei|talk|taobao|target|tatamotors|tatar|tattoo|tax|taxi|tci|tdk|team|tech|technology|tel|telefonica|temasek|tennis|teva|thd|theater|theatre|tiaa|tickets|tienda|tiffany|tips|tires|tirol|tjmaxx|tjx|tkmaxx|tmall|today|tokyo|tools|top|toray|toshiba|total|tours|town|toyota|toys|trade|trading|training|travel|travelchannel|travelers|travelersinsurance|trust|trv|tube|tui|tunes|tushu|tvs|t[cdfghjklmnortvwz])"
                    + "|(?:ubank|ubs|uconnect|unicom|university|uno|uol|ups|u[agksyz])"
                    + "|(?:vacations|vana|vanguard|vegas|ventures|verisign|versicherung|vet|viajes|video|vig|viking|villas|vin|vip|virgin|visa|vision|vistaprint|viva|vivo|vlaanderen|vodka|volkswagen|volvo|vote|voting|voto|voyage|vuelos|v[aceginu])"
                    + "|(?:wales|walmart|walter|wang|wanggou|warman|watch|watches|weather|weatherchannel|webcam|weber|website|wed|wedding|weibo|weir|whoswho|wien|wiki|williamhill|win|windows|wine|winners|wme|wolterskluwer|woodside|work|works|world|wow|wtc|wtf|w[fs])"
                    + "|(?:\\u03b5\\u03bb|\\u0431\\u0433|\\u0431\\u0435\\u043b|\\u0434\\u0435\\u0442\\u0438|\\u0435\\u044e|\\u043a\\u0430\\u0442\\u043e\\u043b\\u0438\\u043a|\\u043a\\u043e\\u043c|\\u043c\\u043a\\u0434|\\u043c\\u043e\\u043d|\\u043c\\u043e\\u0441\\u043a\\u0432\\u0430|\\u043e\\u043d\\u043b\\u0430\\u0439\\u043d|\\u043e\\u0440\\u0433|\\u0440\\u0443\\u0441|\\u0440\\u0444|\\u0441\\u0430\\u0439\\u0442|\\u0441\\u0440\\u0431|\\u0443\\u043a\\u0440|\\u049b\\u0430\\u0437|\\u0570\\u0561\\u0575|\\u05e7\\u05d5\\u05dd|\\u0627\\u0628\\u0648\\u0638\\u0628\\u064a|\\u0627\\u062a\\u0635\\u0627\\u0644\\u0627\\u062a|\\u0627\\u0631\\u0627\\u0645\\u0643\\u0648|\\u0627\\u0644\\u0627\\u0631\\u062f\\u0646|\\u0627\\u0644\\u062c\\u0632\\u0627\\u0626\\u0631|\\u0627\\u0644\\u0633\\u0639\\u0648\\u062f\\u064a\\u0629|\\u0627\\u0644\\u0639\\u0644\\u064a\\u0627\\u0646|\\u0627\\u0644\\u0645\\u063a\\u0631\\u0628|\\u0627\\u0645\\u0627\\u0631\\u0627\\u062a|\\u0627\\u06cc\\u0631\\u0627\\u0646|\\u0628\\u0627\\u0631\\u062a|\\u0628\\u0627\\u0632\\u0627\\u0631|\\u0628\\u064a\\u062a\\u0643|\\u0628\\u06be\\u0627\\u0631\\u062a|\\u062a\\u0648\\u0646\\u0633|\\u0633\\u0648\\u062f\\u0627\\u0646|\\u0633\\u0648\\u0631\\u064a\\u0629|\\u0634\\u0628\\u0643\\u0629|\\u0639\\u0631\\u0627\\u0642|\\u0639\\u0631\\u0628|\\u0639\\u0645\\u0627\\u0646|\\u0641\\u0644\\u0633\\u0637\\u064a\\u0646|\\u0642\\u0637\\u0631|\\u0643\\u0627\\u062b\\u0648\\u0644\\u064a\\u0643|\\u0643\\u0648\\u0645|\\u0645\\u0635\\u0631|\\u0645\\u0644\\u064a\\u0633\\u064a\\u0627|\\u0645\\u0648\\u0628\\u0627\\u064a\\u0644\\u064a|\\u0645\\u0648\\u0631\\u064a\\u062a\\u0627\\u0646\\u064a\\u0627|\\u0645\\u0648\\u0642\\u0639|\\u0647\\u0645\\u0631\\u0627\\u0647|\\u067e\\u0627\\u06a9\\u0633\\u062a\\u0627\\u0646|\\u0680\\u0627\\u0631\\u062a|\\u0915\\u0949\\u092e|\\u0928\\u0947\\u091f|\\u092d\\u093e\\u0930\\u0924|\\u092d\\u093e\\u0930\\u0924\\u092e\\u094d|\\u092d\\u093e\\u0930\\u094b\\u0924|\\u0938\\u0902\\u0917\\u0920\\u0928|\\u09ac\\u09be\\u0982\\u09b2\\u09be|\\u09ad\\u09be\\u09b0\\u09a4|\\u09ad\\u09be\\u09f0\\u09a4|\\u0a2d\\u0a3e\\u0a30\\u0a24|\\u0aad\\u0abe\\u0ab0\\u0aa4|\\u0b2d\\u0b3e\\u0b30\\u0b24|\\u0b87\\u0ba8\\u0bcd\\u0ba4\\u0bbf\\u0baf\\u0bbe|\\u0b87\\u0bb2\\u0b99\\u0bcd\\u0b95\\u0bc8|\\u0b9a\\u0bbf\\u0b99\\u0bcd\\u0b95\\u0baa\\u0bcd\\u0baa\\u0bc2\\u0bb0\\u0bcd|\\u0c2d\\u0c3e\\u0c30\\u0c24\\u0c4d|\\u0cad\\u0cbe\\u0cb0\\u0ca4|\\u0d2d\\u0d3e\\u0d30\\u0d24\\u0d02|\\u0dbd\\u0d82\\u0d9a\\u0dcf|\\u0e04\\u0e2d\\u0e21|\\u0e44\\u0e17\\u0e22|\\u10d2\\u10d4|\\u307f\\u3093\\u306a|\\u30af\\u30e9\\u30a6\\u30c9|\\u30b0\\u30fc\\u30b0\\u30eb|\\u30b3\\u30e0|\\u30b9\\u30c8\\u30a2|\\u30bb\\u30fc\\u30eb|\\u30d5\\u30a1\\u30c3\\u30b7\\u30e7\\u30f3|\\u30dd\\u30a4\\u30f3\\u30c8|\\u4e16\\u754c|\\u4e2d\\u4fe1|\\u4e2d\\u56fd|\\u4e2d\\u570b|\\u4e2d\\u6587\\u7f51|\\u4f01\\u4e1a|\\u4f5b\\u5c71|\\u4fe1\\u606f|\\u5065\\u5eb7|\\u516b\\u5366|\\u516c\\u53f8|\\u516c\\u76ca|\\u53f0\\u6e7e|\\u53f0\\u7063|\\u5546\\u57ce|\\u5546\\u5e97|\\u5546\\u6807|\\u5609\\u91cc|\\u5609\\u91cc\\u5927\\u9152\\u5e97|\\u5728\\u7ebf|\\u5927\\u4f17\\u6c7d\\u8f66|\\u5927\\u62ff|\\u5929\\u4e3b\\u6559|\\u5a31\\u4e50|\\u5bb6\\u96fb|\\u5de5\\u884c|\\u5e7f\\u4e1c|\\u5fae\\u535a|\\u6148\\u5584|\\u6211\\u7231\\u4f60|\\u624b\\u673a|\\u624b\\u8868|\\u62db\\u8058|\\u653f\\u52a1|\\u653f\\u5e9c|\\u65b0\\u52a0\\u5761|\\u65b0\\u95fb|\\u65f6\\u5c1a|\\u66f8\\u7c4d|\\u673a\\u6784|\\u6de1\\u9a6c\\u9521|\\u6e38\\u620f|\\u6fb3\\u9580|\\u70b9\\u770b|\\u73e0\\u5b9d|\\u79fb\\u52a8|\\u7ec4\\u7ec7\\u673a\\u6784|\\u7f51\\u5740|\\u7f51\\u5e97|\\u7f51\\u7ad9|\\u7f51\\u7edc|\\u8054\\u901a|\\u8bfa\\u57fa\\u4e9a|\\u8c37\\u6b4c|\\u8d2d\\u7269|\\u901a\\u8ca9|\\u96c6\\u56e2|\\u96fb\\u8a0a\\u76c8\\u79d1|\\u98de\\u5229\\u6d66|\\u98df\\u54c1|\\u9910\\u5385|\\u9999\\u683c\\u91cc\\u62c9|\\u9999\\u6e2f|\\ub2f7\\ub137|\\ub2f7\\ucef4|\\uc0bc\\uc131|\\ud55c\\uad6d|verm\\xf6gensberater|verm\\xf6gensberatung|xbox|xerox|xfinity|xihuan|xin|xn\\-\\-11b4c3d|xn\\-\\-1ck2e1b|xn\\-\\-1qqw23a|xn\\-\\-2scrj9c|xn\\-\\-30rr7y|xn\\-\\-3bst00m|xn\\-\\-3ds443g|xn\\-\\-3e0b707e|xn\\-\\-3hcrj9c|xn\\-\\-3oq18vl8pn36a|xn\\-\\-3pxu8k|xn\\-\\-42c2d9a|xn\\-\\-45br5cyl|xn\\-\\-45brj9c|xn\\-\\-45q11c|xn\\-\\-4gbrim|xn\\-\\-54b7fta0cc|xn\\-\\-55qw42g|xn\\-\\-55qx5d|xn\\-\\-5su34j936bgsg|xn\\-\\-5tzm5g|xn\\-\\-6frz82g|xn\\-\\-6qq986b3xl|xn\\-\\-80adxhks|xn\\-\\-80ao21a|xn\\-\\-80aqecdr1a|xn\\-\\-80asehdb|xn\\-\\-80aswg|xn\\-\\-8y0a063a|xn\\-\\-90a3ac|xn\\-\\-90ae|xn\\-\\-90ais|xn\\-\\-9dbq2a|xn\\-\\-9et52u|xn\\-\\-9krt00a|xn\\-\\-b4w605ferd|xn\\-\\-bck1b9a5dre4c|xn\\-\\-c1avg|xn\\-\\-c2br7g|xn\\-\\-cck2b3b|xn\\-\\-cg4bki|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-czr694b|xn\\-\\-czrs0t|xn\\-\\-czru2d|xn\\-\\-d1acj3b|xn\\-\\-d1alf|xn\\-\\-e1a4c|xn\\-\\-eckvdtc9d|xn\\-\\-efvy88h|xn\\-\\-estv75g|xn\\-\\-fct429k|xn\\-\\-fhbei|xn\\-\\-fiq228c5hs|xn\\-\\-fiq64b|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fjq720a|xn\\-\\-flw351e|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-fzys8d69uvgm|xn\\-\\-g2xx48c|xn\\-\\-gckr3f0f|xn\\-\\-gecrj9c|xn\\-\\-gk3at1e|xn\\-\\-h2breg3eve|xn\\-\\-h2brj9c|xn\\-\\-h2brj9c8c|xn\\-\\-hxt814e|xn\\-\\-i1b6b1a6a2e|xn\\-\\-imr513n|xn\\-\\-io0a7i|xn\\-\\-j1aef|xn\\-\\-j1amh|xn\\-\\-j6w193g|xn\\-\\-jlq61u9w7b|xn\\-\\-jvr189m|xn\\-\\-kcrx77d1x4a|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-kpu716f|xn\\-\\-kput3i|xn\\-\\-l1acc|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgb9awbf|xn\\-\\-mgba3a3ejt|xn\\-\\-mgba3a4f16a|xn\\-\\-mgba7c0bbn0a|xn\\-\\-mgbaakc7dvf|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbab2bd|xn\\-\\-mgbah1a3hjkrd|xn\\-\\-mgbai9azgqp6j|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbb9fbpob|xn\\-\\-mgbbh1a|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgbca7dzdo|xn\\-\\-mgberp4a5d4ar|xn\\-\\-mgbgu82a|xn\\-\\-mgbi4ecexp|xn\\-\\-mgbpl2fh|xn\\-\\-mgbt3dhd|xn\\-\\-mgbtx2b|xn\\-\\-mgbx4cd0ab|xn\\-\\-mix891f|xn\\-\\-mk1bu44c|xn\\-\\-mxtq1m|xn\\-\\-ngbc5azd|xn\\-\\-ngbe9e0a|xn\\-\\-ngbrx|xn\\-\\-node|xn\\-\\-nqv7f|xn\\-\\-nqv7fs00ema|xn\\-\\-nyqy26a|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-otu796d|xn\\-\\-p1acf|xn\\-\\-p1ai|xn\\-\\-pbt977c|xn\\-\\-pgbs0dh|xn\\-\\-pssy2u|xn\\-\\-q9jyb4c|xn\\-\\-qcka1pmc|xn\\-\\-qxam|xn\\-\\-rhqv96g|xn\\-\\-rovu88b|xn\\-\\-rvc1e0am3e|xn\\-\\-s9brj9c|xn\\-\\-ses554g|xn\\-\\-t60b56a|xn\\-\\-tckwe|xn\\-\\-tiq49xqyj|xn\\-\\-unup4y|xn\\-\\-vermgensberater\\-ctb|xn\\-\\-vermgensberatung\\-pwb|xn\\-\\-vhquv|xn\\-\\-vuq861b|xn\\-\\-w4r85el8fhu5dnra|xn\\-\\-w4rs40l|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xhq521b|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-y9a3aq|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zfr164b|xxx|xyz)"
                    + "|(?:yachts|yahoo|yamaxun|yandex|yodobashi|yoga|yokohama|you|youtube|yun|y[et])"
                    + "|(?:zappos|zara|zero|zip|zone|zuerich|z[amw])))";

    public static final String GOOD_IRI_CHAR =
            "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    public static final Pattern WEB_URL = Pattern.compile(
            "(?i)((?:(http|https|rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
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
                    + "([\\/\\?\\#](?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~\\$"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_\\^\\{\\}\\[\\]\\<\\>\\|])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)");

    public static final String[] COLOR_MAP = {
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
            // http://anti.teamidiot.de/static/nei/*/extended_mirc_color_proposal.html
            "470000",
            "472100",
            "474700",
            "324700",
            "004700",
            "00472c",
            "004747",
            "002747",
            "000047",
            "2e0047",
            "470047",
            "47002a",
            "740000",
            "743a00",
            "747400",
            "517400",
            "007400",
            "007449",
            "007474",
            "004074",
            "000074",
            "4b0074",
            "740074",
            "740045",
            "b50000",
            "b56300",
            "b5b500",
            "7db500",
            "00b500",
            "00b571",
            "00b5b5",
            "0063b5",
            "0000b5",
            "7500b5",
            "b500b5",
            "b5006b",
            "ff0000",
            "ff8c00",
            "ffff00",
            "b2ff00",
            "00ff00",
            "00ffa0",
            "00ffff",
            "008cff",
            "0000ff",
            "a500ff",
            "ff00ff",
            "ff0098",
            "ff5959",
            "ffb459",
            "ffff71",
            "cfff60",
            "6fff6f",
            "65ffc9",
            "6dffff",
            "59b4ff",
            "5959ff",
            "c459ff",
            "ff66ff",
            "ff59bc",
            "ff9c9c",
            "ffd39c",
            "ffff9c",
            "e2ff9c",
            "9cff9c",
            "9cffdb",
            "9cffff",
            "9cd3ff",
            "9c9cff",
            "dc9cff",
            "ff9cff",
            "ff94d3",
            "000000",
            "131313",
            "282828",
            "363636",
            "4d4d4d",
            "656565",
            "818181",
            "9f9f9f",
            "bcbcbc",
            "e2e2e2",
            "ffffff",
    };

    public static final HashMap<String, String> DARK_FG_SUBSTITUTIONS = new HashMap<String, String>() {{
        put("000080","4682b4");
        put("008000","32cd32");
        put("800000","FA8072");
        put("800080","DA70D6");
        put("008080","20B2AA");
        put("0000FF","00BFF9");
    }};

    public static final HashMap<String, String> emojiMap = new HashMap<String, String>() {{
        put("umbrella_with_rain_drops", "☔");
        put("coffee", "☕");
        put("aries", "♈");
        put("taurus", "♉");
        put("sagittarius", "♐");
        put("capricorn", "♑");
        put("aquarius", "♒");
        put("pisces", "♓");
        put("anchor", "⚓");
        put("white_check_mark", "✅");
        put("sparkles", "✨");
        put("question", "❓");
        put("grey_question", "❔");
        put("grey_exclamation", "❕");
        put("exclamation", "❗");
        put("heavy_exclamation_mark", "❗");
        put("heavy_plus_sign", "➕");
        put("heavy_minus_sign", "➖");
        put("heavy_division_sign", "➗");
        put("hash", "#️⃣");
        put("keycap_star", "*️⃣");
        put("zero", "0️⃣");
        put("one", "1️⃣");
        put("two", "2️⃣");
        put("three", "3️⃣");
        put("four", "4️⃣");
        put("five", "5️⃣");
        put("six", "6️⃣");
        put("seven", "7️⃣");
        put("eight", "8️⃣");
        put("nine", "9️⃣");
        put("copyright", "©️");
        put("registered", "®️");
        put("mahjong", "🀄");
        put("black_joker", "🃏");
        put("a", "🅰️");
        put("b", "🅱️");
        put("o2", "🅾️");
        put("parking", "🅿️");
        put("ab", "🆎");
        put("cl", "🆑");
        put("cool", "🆒");
        put("free", "🆓");
        put("id", "🆔");
        put("new", "🆕");
        put("ng", "🆖");
        put("ok", "🆗");
        put("sos", "🆘");
        put("up", "🆙");
        put("vs", "🆚");
        put("flag-ac", "🇦🇨");
        put("flag-ad", "🇦🇩");
        put("flag-ae", "🇦🇪");
        put("flag-af", "🇦🇫");
        put("flag-ag", "🇦🇬");
        put("flag-ai", "🇦🇮");
        put("flag-al", "🇦🇱");
        put("flag-am", "🇦🇲");
        put("flag-ao", "🇦🇴");
        put("flag-aq", "🇦🇶");
        put("flag-ar", "🇦🇷");
        put("flag-as", "🇦🇸");
        put("flag-at", "🇦🇹");
        put("flag-au", "🇦🇺");
        put("flag-aw", "🇦🇼");
        put("flag-ax", "🇦🇽");
        put("flag-az", "🇦🇿");
        put("flag-ba", "🇧🇦");
        put("flag-bb", "🇧🇧");
        put("flag-bd", "🇧🇩");
        put("flag-be", "🇧🇪");
        put("flag-bf", "🇧🇫");
        put("flag-bg", "🇧🇬");
        put("flag-bh", "🇧🇭");
        put("flag-bi", "🇧🇮");
        put("flag-bj", "🇧🇯");
        put("flag-bl", "🇧🇱");
        put("flag-bm", "🇧🇲");
        put("flag-bn", "🇧🇳");
        put("flag-bo", "🇧🇴");
        put("flag-bq", "🇧🇶");
        put("flag-br", "🇧🇷");
        put("flag-bs", "🇧🇸");
        put("flag-bt", "🇧🇹");
        put("flag-bv", "🇧🇻");
        put("flag-bw", "🇧🇼");
        put("flag-by", "🇧🇾");
        put("flag-bz", "🇧🇿");
        put("flag-ca", "🇨🇦");
        put("flag-cc", "🇨🇨");
        put("flag-cd", "🇨🇩");
        put("flag-cf", "🇨🇫");
        put("flag-cg", "🇨🇬");
        put("flag-ch", "🇨🇭");
        put("flag-ci", "🇨🇮");
        put("flag-ck", "🇨🇰");
        put("flag-cl", "🇨🇱");
        put("flag-cm", "🇨🇲");
        put("cn", "🇨🇳");
        put("flag-cn", "🇨🇳");
        put("flag-co", "🇨🇴");
        put("flag-cp", "🇨🇵");
        put("flag-cr", "🇨🇷");
        put("flag-cu", "🇨🇺");
        put("flag-cv", "🇨🇻");
        put("flag-cw", "🇨🇼");
        put("flag-cx", "🇨🇽");
        put("flag-cy", "🇨🇾");
        put("flag-cz", "🇨🇿");
        put("de", "🇩🇪");
        put("flag-de", "🇩🇪");
        put("flag-dg", "🇩🇬");
        put("flag-dj", "🇩🇯");
        put("flag-dk", "🇩🇰");
        put("flag-dm", "🇩🇲");
        put("flag-do", "🇩🇴");
        put("flag-dz", "🇩🇿");
        put("flag-ea", "🇪🇦");
        put("flag-ec", "🇪🇨");
        put("flag-ee", "🇪🇪");
        put("flag-eg", "🇪🇬");
        put("flag-eh", "🇪🇭");
        put("flag-er", "🇪🇷");
        put("es", "🇪🇸");
        put("flag-es", "🇪🇸");
        put("flag-et", "🇪🇹");
        put("flag-eu", "🇪🇺");
        put("flag-fi", "🇫🇮");
        put("flag-fj", "🇫🇯");
        put("flag-fk", "🇫🇰");
        put("flag-fm", "🇫🇲");
        put("flag-fo", "🇫🇴");
        put("fr", "🇫🇷");
        put("flag-fr", "🇫🇷");
        put("flag-ga", "🇬🇦");
        put("gb", "🇬🇧");
        put("uk", "🇬🇧");
        put("flag-gb", "🇬🇧");
        put("flag-gd", "🇬🇩");
        put("flag-ge", "🇬🇪");
        put("flag-gf", "🇬🇫");
        put("flag-gg", "🇬🇬");
        put("flag-gh", "🇬🇭");
        put("flag-gi", "🇬🇮");
        put("flag-gl", "🇬🇱");
        put("flag-gm", "🇬🇲");
        put("flag-gn", "🇬🇳");
        put("flag-gp", "🇬🇵");
        put("flag-gq", "🇬🇶");
        put("flag-gr", "🇬🇷");
        put("flag-gs", "🇬🇸");
        put("flag-gt", "🇬🇹");
        put("flag-gu", "🇬🇺");
        put("flag-gw", "🇬🇼");
        put("flag-gy", "🇬🇾");
        put("flag-hk", "🇭🇰");
        put("flag-hm", "🇭🇲");
        put("flag-hn", "🇭🇳");
        put("flag-hr", "🇭🇷");
        put("flag-ht", "🇭🇹");
        put("flag-hu", "🇭🇺");
        put("flag-ic", "🇮🇨");
        put("flag-id", "🇮🇩");
        put("flag-ie", "🇮🇪");
        put("flag-il", "🇮🇱");
        put("flag-im", "🇮🇲");
        put("flag-in", "🇮🇳");
        put("flag-io", "🇮🇴");
        put("flag-iq", "🇮🇶");
        put("flag-ir", "🇮🇷");
        put("flag-is", "🇮🇸");
        put("it", "🇮🇹");
        put("flag-it", "🇮🇹");
        put("flag-je", "🇯🇪");
        put("flag-jm", "🇯🇲");
        put("flag-jo", "🇯🇴");
        put("jp", "🇯🇵");
        put("flag-jp", "🇯🇵");
        put("flag-ke", "🇰🇪");
        put("flag-kg", "🇰🇬");
        put("flag-kh", "🇰🇭");
        put("flag-ki", "🇰🇮");
        put("flag-km", "🇰🇲");
        put("flag-kn", "🇰🇳");
        put("flag-kp", "🇰🇵");
        put("kr", "🇰🇷");
        put("flag-kr", "🇰🇷");
        put("flag-kw", "🇰🇼");
        put("flag-ky", "🇰🇾");
        put("flag-kz", "🇰🇿");
        put("flag-la", "🇱🇦");
        put("flag-lb", "🇱🇧");
        put("flag-lc", "🇱🇨");
        put("flag-li", "🇱🇮");
        put("flag-lk", "🇱🇰");
        put("flag-lr", "🇱🇷");
        put("flag-ls", "🇱🇸");
        put("flag-lt", "🇱🇹");
        put("flag-lu", "🇱🇺");
        put("flag-lv", "🇱🇻");
        put("flag-ly", "🇱🇾");
        put("flag-ma", "🇲🇦");
        put("flag-mc", "🇲🇨");
        put("flag-md", "🇲🇩");
        put("flag-me", "🇲🇪");
        put("flag-mf", "🇲🇫");
        put("flag-mg", "🇲🇬");
        put("flag-mh", "🇲🇭");
        put("flag-mk", "🇲🇰");
        put("flag-ml", "🇲🇱");
        put("flag-mm", "🇲🇲");
        put("flag-mn", "🇲🇳");
        put("flag-mo", "🇲🇴");
        put("flag-mp", "🇲🇵");
        put("flag-mq", "🇲🇶");
        put("flag-mr", "🇲🇷");
        put("flag-ms", "🇲🇸");
        put("flag-mt", "🇲🇹");
        put("flag-mu", "🇲🇺");
        put("flag-mv", "🇲🇻");
        put("flag-mw", "🇲🇼");
        put("flag-mx", "🇲🇽");
        put("flag-my", "🇲🇾");
        put("flag-mz", "🇲🇿");
        put("flag-na", "🇳🇦");
        put("flag-nc", "🇳🇨");
        put("flag-ne", "🇳🇪");
        put("flag-nf", "🇳🇫");
        put("flag-ng", "🇳🇬");
        put("flag-ni", "🇳🇮");
        put("flag-nl", "🇳🇱");
        put("flag-no", "🇳🇴");
        put("flag-np", "🇳🇵");
        put("flag-nr", "🇳🇷");
        put("flag-nu", "🇳🇺");
        put("flag-nz", "🇳🇿");
        put("flag-om", "🇴🇲");
        put("flag-pa", "🇵🇦");
        put("flag-pe", "🇵🇪");
        put("flag-pf", "🇵🇫");
        put("flag-pg", "🇵🇬");
        put("flag-ph", "🇵🇭");
        put("flag-pk", "🇵🇰");
        put("flag-pl", "🇵🇱");
        put("flag-pm", "🇵🇲");
        put("flag-pn", "🇵🇳");
        put("flag-pr", "🇵🇷");
        put("flag-ps", "🇵🇸");
        put("flag-pt", "🇵🇹");
        put("flag-pw", "🇵🇼");
        put("flag-py", "🇵🇾");
        put("flag-qa", "🇶🇦");
        put("flag-re", "🇷🇪");
        put("flag-ro", "🇷🇴");
        put("flag-rs", "🇷🇸");
        put("ru", "🇷🇺");
        put("flag-ru", "🇷🇺");
        put("flag-rw", "🇷🇼");
        put("flag-sa", "🇸🇦");
        put("flag-sb", "🇸🇧");
        put("flag-sc", "🇸🇨");
        put("flag-sd", "🇸🇩");
        put("flag-se", "🇸🇪");
        put("flag-sg", "🇸🇬");
        put("flag-sh", "🇸🇭");
        put("flag-si", "🇸🇮");
        put("flag-sj", "🇸🇯");
        put("flag-sk", "🇸🇰");
        put("flag-sl", "🇸🇱");
        put("flag-sm", "🇸🇲");
        put("flag-sn", "🇸🇳");
        put("flag-so", "🇸🇴");
        put("flag-sr", "🇸🇷");
        put("flag-ss", "🇸🇸");
        put("flag-st", "🇸🇹");
        put("flag-sv", "🇸🇻");
        put("flag-sx", "🇸🇽");
        put("flag-sy", "🇸🇾");
        put("flag-sz", "🇸🇿");
        put("flag-ta", "🇹🇦");
        put("flag-tc", "🇹🇨");
        put("flag-td", "🇹🇩");
        put("flag-tf", "🇹🇫");
        put("flag-tg", "🇹🇬");
        put("flag-th", "🇹🇭");
        put("flag-tj", "🇹🇯");
        put("flag-tk", "🇹🇰");
        put("flag-tl", "🇹🇱");
        put("flag-tm", "🇹🇲");
        put("flag-tn", "🇹🇳");
        put("flag-to", "🇹🇴");
        put("flag-tr", "🇹🇷");
        put("flag-tt", "🇹🇹");
        put("flag-tv", "🇹🇻");
        put("flag-tw", "🇹🇼");
        put("flag-tz", "🇹🇿");
        put("flag-ua", "🇺🇦");
        put("flag-ug", "🇺🇬");
        put("flag-um", "🇺🇲");
        put("flag-un", "🇺🇳");
        put("us", "🇺🇸");
        put("flag-us", "🇺🇸");
        put("flag-uy", "🇺🇾");
        put("flag-uz", "🇺🇿");
        put("flag-va", "🇻🇦");
        put("flag-vc", "🇻🇨");
        put("flag-ve", "🇻🇪");
        put("flag-vg", "🇻🇬");
        put("flag-vi", "🇻🇮");
        put("flag-vn", "🇻🇳");
        put("flag-vu", "🇻🇺");
        put("flag-wf", "🇼🇫");
        put("flag-ws", "🇼🇸");
        put("flag-xk", "🇽🇰");
        put("flag-ye", "🇾🇪");
        put("flag-yt", "🇾🇹");
        put("flag-za", "🇿🇦");
        put("flag-zm", "🇿🇲");
        put("flag-zw", "🇿🇼");
        put("koko", "🈁");
        put("sa", "🈂️");
        put("u7121", "🈚");
        put("u6307", "🈯");
        put("u7981", "🈲");
        put("u7a7a", "🈳");
        put("u5408", "🈴");
        put("u6e80", "🈵");
        put("u6709", "🈶");
        put("u6708", "🈷️");
        put("u7533", "🈸");
        put("u5272", "🈹");
        put("u55b6", "🈺");
        put("ideograph_advantage", "🉐");
        put("accept", "🉑");
        put("cyclone", "🌀");
        put("foggy", "🌁");
        put("closed_umbrella", "🌂");
        put("night_with_stars", "🌃");
        put("sunrise_over_mountains", "🌄");
        put("sunrise", "🌅");
        put("city_sunset", "🌆");
        put("city_sunrise", "🌇");
        put("rainbow", "🌈");
        put("bridge_at_night", "🌉");
        put("ocean", "🌊");
        put("volcano", "🌋");
        put("milky_way", "🌌");
        put("earth_africa", "🌍");
        put("earth_americas", "🌎");
        put("earth_asia", "🌏");
        put("globe_with_meridians", "🌐");
        put("new_moon", "🌑");
        put("waxing_crescent_moon", "🌒");
        put("first_quarter_moon", "🌓");
        put("moon", "🌔");
        put("waxing_gibbous_moon", "🌔");
        put("full_moon", "🌕");
        put("waning_gibbous_moon", "🌖");
        put("last_quarter_moon", "🌗");
        put("waning_crescent_moon", "🌘");
        put("crescent_moon", "🌙");
        put("new_moon_with_face", "🌚");
        put("first_quarter_moon_with_face", "🌛");
        put("last_quarter_moon_with_face", "🌜");
        put("full_moon_with_face", "🌝");
        put("sun_with_face", "🌞");
        put("star2", "🌟");
        put("stars", "🌠");
        put("thermometer", "🌡️");
        put("mostly_sunny", "🌤️");
        put("sun_small_cloud", "🌤️");
        put("barely_sunny", "🌥️");
        put("sun_behind_cloud", "🌥️");
        put("partly_sunny_rain", "🌦️");
        put("sun_behind_rain_cloud", "🌦️");
        put("rain_cloud", "🌧️");
        put("snow_cloud", "🌨️");
        put("lightning", "🌩️");
        put("lightning_cloud", "🌩️");
        put("tornado", "🌪️");
        put("tornado_cloud", "🌪️");
        put("fog", "🌫️");
        put("wind_blowing_face", "🌬️");
        put("hotdog", "🌭");
        put("taco", "🌮");
        put("burrito", "🌯");
        put("chestnut", "🌰");
        put("seedling", "🌱");
        put("evergreen_tree", "🌲");
        put("deciduous_tree", "🌳");
        put("palm_tree", "🌴");
        put("cactus", "🌵");
        put("hot_pepper", "🌶️");
        put("tulip", "🌷");
        put("cherry_blossom", "🌸");
        put("rose", "🌹");
        put("hibiscus", "🌺");
        put("sunflower", "🌻");
        put("blossom", "🌼");
        put("corn", "🌽");
        put("ear_of_rice", "🌾");
        put("herb", "🌿");
        put("four_leaf_clover", "🍀");
        put("maple_leaf", "🍁");
        put("fallen_leaf", "🍂");
        put("leaves", "🍃");
        put("mushroom", "🍄");
        put("tomato", "🍅");
        put("eggplant", "🍆");
        put("grapes", "🍇");
        put("melon", "🍈");
        put("watermelon", "🍉");
        put("tangerine", "🍊");
        put("lemon", "🍋");
        put("banana", "🍌");
        put("pineapple", "🍍");
        put("apple", "🍎");
        put("green_apple", "🍏");
        put("pear", "🍐");
        put("peach", "🍑");
        put("cherries", "🍒");
        put("strawberry", "🍓");
        put("hamburger", "🍔");
        put("pizza", "🍕");
        put("meat_on_bone", "🍖");
        put("poultry_leg", "🍗");
        put("rice_cracker", "🍘");
        put("rice_ball", "🍙");
        put("rice", "🍚");
        put("curry", "🍛");
        put("ramen", "🍜");
        put("spaghetti", "🍝");
        put("bread", "🍞");
        put("fries", "🍟");
        put("sweet_potato", "🍠");
        put("dango", "🍡");
        put("oden", "🍢");
        put("sushi", "🍣");
        put("fried_shrimp", "🍤");
        put("fish_cake", "🍥");
        put("icecream", "🍦");
        put("shaved_ice", "🍧");
        put("ice_cream", "🍨");
        put("doughnut", "🍩");
        put("cookie", "🍪");
        put("chocolate_bar", "🍫");
        put("candy", "🍬");
        put("lollipop", "🍭");
        put("custard", "🍮");
        put("honey_pot", "🍯");
        put("cake", "🍰");
        put("bento", "🍱");
        put("stew", "🍲");
        put("fried_egg", "🍳");
        put("cooking", "🍳");
        put("fork_and_knife", "🍴");
        put("tea", "🍵");
        put("sake", "🍶");
        put("wine_glass", "🍷");
        put("cocktail", "🍸");
        put("tropical_drink", "🍹");
        put("beer", "🍺");
        put("beers", "🍻");
        put("baby_bottle", "🍼");
        put("knife_fork_plate", "🍽️");
        put("champagne", "🍾");
        put("popcorn", "🍿");
        put("ribbon", "🎀");
        put("gift", "🎁");
        put("birthday", "🎂");
        put("jack_o_lantern", "🎃");
        put("christmas_tree", "🎄");
        put("santa", "🎅");
        put("fireworks", "🎆");
        put("sparkler", "🎇");
        put("balloon", "🎈");
        put("tada", "🎉");
        put("confetti_ball", "🎊");
        put("tanabata_tree", "🎋");
        put("crossed_flags", "🎌");
        put("bamboo", "🎍");
        put("dolls", "🎎");
        put("flags", "🎏");
        put("wind_chime", "🎐");
        put("rice_scene", "🎑");
        put("school_satchel", "🎒");
        put("mortar_board", "🎓");
        put("medal", "🎖️");
        put("reminder_ribbon", "🎗️");
        put("studio_microphone", "🎙️");
        put("level_slider", "🎚️");
        put("control_knobs", "🎛️");
        put("film_frames", "🎞️");
        put("admission_tickets", "🎟️");
        put("carousel_horse", "🎠");
        put("ferris_wheel", "🎡");
        put("roller_coaster", "🎢");
        put("fishing_pole_and_fish", "🎣");
        put("microphone", "🎤");
        put("movie_camera", "🎥");
        put("cinema", "🎦");
        put("headphones", "🎧");
        put("art", "🎨");
        put("tophat", "🎩");
        put("circus_tent", "🎪");
        put("ticket", "🎫");
        put("clapper", "🎬");
        put("performing_arts", "🎭");
        put("video_game", "🎮");
        put("dart", "🎯");
        put("slot_machine", "🎰");
        put("8ball", "🎱");
        put("game_die", "🎲");
        put("bowling", "🎳");
        put("flower_playing_cards", "🎴");
        put("musical_note", "🎵");
        put("notes", "🎶");
        put("saxophone", "🎷");
        put("guitar", "🎸");
        put("musical_keyboard", "🎹");
        put("trumpet", "🎺");
        put("violin", "🎻");
        put("musical_score", "🎼");
        put("running_shirt_with_sash", "🎽");
        put("tennis", "🎾");
        put("ski", "🎿");
        put("basketball", "🏀");
        put("checkered_flag", "🏁");
        put("snowboarder", "🏂");
        put("woman-running", "🏃‍♀️");
        put("man-running", "🏃‍♂️");
        put("runner", "🏃‍♂️");
        put("running", "🏃‍♂️");
        put("woman-surfing", "🏄‍♀️");
        put("man-surfing", "🏄‍♂️");
        put("surfer", "🏄‍♂️");
        put("sports_medal", "🏅");
        put("trophy", "🏆");
        put("horse_racing", "🏇");
        put("football", "🏈");
        put("rugby_football", "🏉");
        put("woman-swimming", "🏊‍♀️");
        put("man-swimming", "🏊‍♂️");
        put("swimmer", "🏊‍♂️");
        put("woman-lifting-weights", "🏋️‍♀️");
        put("man-lifting-weights", "🏋️‍♂️");
        put("weight_lifter", "🏋️‍♂️");
        put("woman-golfing", "🏌️‍♀️");
        put("man-golfing", "🏌️‍♂️");
        put("golfer", "🏌️‍♂️");
        put("racing_motorcycle", "🏍️");
        put("racing_car", "🏎️");
        put("cricket_bat_and_ball", "🏏");
        put("volleyball", "🏐");
        put("field_hockey_stick_and_ball", "🏑");
        put("ice_hockey_stick_and_puck", "🏒");
        put("table_tennis_paddle_and_ball", "🏓");
        put("snow_capped_mountain", "🏔️");
        put("camping", "🏕️");
        put("beach_with_umbrella", "🏖️");
        put("building_construction", "🏗️");
        put("house_buildings", "🏘️");
        put("cityscape", "🏙️");
        put("derelict_house_building", "🏚️");
        put("classical_building", "🏛️");
        put("desert", "🏜️");
        put("desert_island", "🏝️");
        put("national_park", "🏞️");
        put("stadium", "🏟️");
        put("house", "🏠");
        put("house_with_garden", "🏡");
        put("office", "🏢");
        put("post_office", "🏣");
        put("european_post_office", "🏤");
        put("hospital", "🏥");
        put("bank", "🏦");
        put("atm", "🏧");
        put("hotel", "🏨");
        put("love_hotel", "🏩");
        put("convenience_store", "🏪");
        put("school", "🏫");
        put("department_store", "🏬");
        put("factory", "🏭");
        put("izakaya_lantern", "🏮");
        put("lantern", "🏮");
        put("japanese_castle", "🏯");
        put("european_castle", "🏰");
        put("rainbow-flag", "🏳️‍🌈");
        put("waving_white_flag", "🏳️");
        put("pirate_flag", "🏴‍☠️");
        put("flag-england", "🏴󠁧󠁢󠁥󠁮󠁧󠁿");
        put("flag-scotland", "🏴󠁧󠁢󠁳󠁣󠁴󠁿");
        put("flag-wales", "🏴󠁧󠁢󠁷󠁬󠁳󠁿");
        put("waving_black_flag", "🏴");
        put("rosette", "🏵️");
        put("label", "🏷️");
        put("badminton_racquet_and_shuttlecock", "🏸");
        put("bow_and_arrow", "🏹");
        put("amphora", "🏺");
        put("skin-tone-2", "🏻");
        put("skin-tone-3", "🏼");
        put("skin-tone-4", "🏽");
        put("skin-tone-5", "🏾");
        put("skin-tone-6", "🏿");
        put("rat", "🐀");
        put("mouse2", "🐁");
        put("ox", "🐂");
        put("water_buffalo", "🐃");
        put("cow2", "🐄");
        put("tiger2", "🐅");
        put("leopard", "🐆");
        put("rabbit2", "🐇");
        put("cat2", "🐈");
        put("dragon", "🐉");
        put("crocodile", "🐊");
        put("whale2", "🐋");
        put("snail", "🐌");
        put("snake", "🐍");
        put("racehorse", "🐎");
        put("ram", "🐏");
        put("goat", "🐐");
        put("sheep", "🐑");
        put("monkey", "🐒");
        put("rooster", "🐓");
        put("chicken", "🐔");
        put("dog2", "🐕");
        put("pig2", "🐖");
        put("boar", "🐗");
        put("elephant", "🐘");
        put("octopus", "🐙");
        put("shell", "🐚");
        put("bug", "🐛");
        put("ant", "🐜");
        put("bee", "🐝");
        put("honeybee", "🐝");
        put("beetle", "🐞");
        put("fish", "🐟");
        put("tropical_fish", "🐠");
        put("blowfish", "🐡");
        put("turtle", "🐢");
        put("hatching_chick", "🐣");
        put("baby_chick", "🐤");
        put("hatched_chick", "🐥");
        put("bird", "🐦");
        put("penguin", "🐧");
        put("koala", "🐨");
        put("poodle", "🐩");
        put("dromedary_camel", "🐪");
        put("camel", "🐫");
        put("dolphin", "🐬");
        put("flipper", "🐬");
        put("mouse", "🐭");
        put("cow", "🐮");
        put("tiger", "🐯");
        put("rabbit", "🐰");
        put("cat", "🐱");
        put("dragon_face", "🐲");
        put("whale", "🐳");
        put("horse", "🐴");
        put("monkey_face", "🐵");
        put("dog", "🐶");
        put("pig", "🐷");
        put("frog", "🐸");
        put("hamster", "🐹");
        put("wolf", "🐺");
        put("bear", "🐻");
        put("panda_face", "🐼");
        put("pig_nose", "🐽");
        put("feet", "🐾");
        put("paw_prints", "🐾");
        put("chipmunk", "🐿️");
        put("eyes", "👀");
        put("eye-in-speech-bubble", "👁️‍🗨️");
        put("eye", "👁️");
        put("ear", "👂");
        put("nose", "👃");
        put("lips", "👄");
        put("tongue", "👅");
        put("point_up_2", "👆");
        put("point_down", "👇");
        put("point_left", "👈");
        put("point_right", "👉");
        put("facepunch", "👊");
        put("punch", "👊");
        put("wave", "👋");
        put("ok_hand", "👌");
        put("+1", "👍");
        put("thumbsup", "👍");
        put("-1", "👎");
        put("thumbsdown", "👎");
        put("clap", "👏");
        put("open_hands", "👐");
        put("crown", "👑");
        put("womans_hat", "👒");
        put("eyeglasses", "👓");
        put("necktie", "👔");
        put("shirt", "👕");
        put("tshirt", "👕");
        put("jeans", "👖");
        put("dress", "👗");
        put("kimono", "👘");
        put("bikini", "👙");
        put("womans_clothes", "👚");
        put("purse", "👛");
        put("handbag", "👜");
        put("pouch", "👝");
        put("mans_shoe", "👞");
        put("shoe", "👞");
        put("athletic_shoe", "👟");
        put("high_heel", "👠");
        put("sandal", "👡");
        put("boot", "👢");
        put("footprints", "👣");
        put("bust_in_silhouette", "👤");
        put("busts_in_silhouette", "👥");
        put("boy", "👦");
        put("girl", "👧");
        put("male-farmer", "👨‍🌾");
        put("male-cook", "👨‍🍳");
        put("male-student", "👨‍🎓");
        put("male-singer", "👨‍🎤");
        put("male-artist", "👨‍🎨");
        put("male-teacher", "👨‍🏫");
        put("male-factory-worker", "👨‍🏭");
        put("man-boy-boy", "👨‍👦‍👦");
        put("man-boy", "👨‍👦");
        put("man-girl-boy", "👨‍👧‍👦");
        put("man-girl-girl", "👨‍👧‍👧");
        put("man-girl", "👨‍👧");
        put("man-man-boy", "👨‍👨‍👦");
        put("man-man-boy-boy", "👨‍👨‍👦‍👦");
        put("man-man-girl", "👨‍👨‍👧");
        put("man-man-girl-boy", "👨‍👨‍👧‍👦");
        put("man-man-girl-girl", "👨‍👨‍👧‍👧");
        put("man-woman-boy", "👨‍👩‍👦");
        put("family", "👨‍👩‍👦");
        put("man-woman-boy-boy", "👨‍👩‍👦‍👦");
        put("man-woman-girl", "👨‍👩‍👧");
        put("man-woman-girl-boy", "👨‍👩‍👧‍👦");
        put("man-woman-girl-girl", "👨‍👩‍👧‍👧");
        put("male-technologist", "👨‍💻");
        put("male-office-worker", "👨‍💼");
        put("male-mechanic", "👨‍🔧");
        put("male-scientist", "👨‍🔬");
        put("male-astronaut", "👨‍🚀");
        put("male-firefighter", "👨‍🚒");
        put("male_red_haired", "👨‍🦰");
        put("male_curly_haired", "👨‍🦱");
        put("male_bald", "👨‍🦲");
        put("male_white_haired", "👨‍🦳");
        put("male-doctor", "👨‍⚕️");
        put("male-judge", "👨‍⚖️");
        put("male-pilot", "👨‍✈️");
        put("man-heart-man", "👨‍❤️‍👨");
        put("man-kiss-man", "👨‍❤️‍💋‍👨");
        put("man", "👨");
        put("female-farmer", "👩‍🌾");
        put("female-cook", "👩‍🍳");
        put("female-student", "👩‍🎓");
        put("female-singer", "👩‍🎤");
        put("female-artist", "👩‍🎨");
        put("female-teacher", "👩‍🏫");
        put("female-factory-worker", "👩‍🏭");
        put("woman-boy-boy", "👩‍👦‍👦");
        put("woman-boy", "👩‍👦");
        put("woman-girl-boy", "👩‍👧‍👦");
        put("woman-girl-girl", "👩‍👧‍👧");
        put("woman-girl", "👩‍👧");
        put("woman-woman-boy", "👩‍👩‍👦");
        put("woman-woman-boy-boy", "👩‍👩‍👦‍👦");
        put("woman-woman-girl", "👩‍👩‍👧");
        put("woman-woman-girl-boy", "👩‍👩‍👧‍👦");
        put("woman-woman-girl-girl", "👩‍👩‍👧‍👧");
        put("female-technologist", "👩‍💻");
        put("female-office-worker", "👩‍💼");
        put("female-mechanic", "👩‍🔧");
        put("female-scientist", "👩‍🔬");
        put("female-astronaut", "👩‍🚀");
        put("female-firefighter", "👩‍🚒");
        put("female_red_haired", "👩‍🦰");
        put("female_curly_haired", "👩‍🦱");
        put("female_bald", "👩‍🦲");
        put("female_white_haired", "👩‍🦳");
        put("female-doctor", "👩‍⚕️");
        put("female-judge", "👩‍⚖️");
        put("female-pilot", "👩‍✈️");
        put("woman-heart-man", "👩‍❤️‍👨");
        put("couple_with_heart", "👩‍❤️‍👨");
        put("woman-heart-woman", "👩‍❤️‍👩");
        put("woman-kiss-man", "👩‍❤️‍💋‍👨");
        put("couplekiss", "👩‍❤️‍💋‍👨");
        put("woman-kiss-woman", "👩‍❤️‍💋‍👩");
        put("woman", "👩");
        put("couple", "👫");
        put("man_and_woman_holding_hands", "👫");
        put("two_men_holding_hands", "👬");
        put("two_women_holding_hands", "👭");
        put("female-police-officer", "👮‍♀️");
        put("male-police-officer", "👮‍♂️");
        put("cop", "👮‍♂️");
        put("woman-with-bunny-ears-partying", "👯‍♀️");
        put("dancers", "👯‍♀️");
        put("man-with-bunny-ears-partying", "👯‍♂️");
        put("bride_with_veil", "👰");
        put("blond-haired-woman", "👱‍♀️");
        put("blond-haired-man", "👱‍♂️");
        put("person_with_blond_hair", "👱‍♂️");
        put("man_with_gua_pi_mao", "👲");
        put("woman-wearing-turban", "👳‍♀️");
        put("man-wearing-turban", "👳‍♂️");
        put("man_with_turban", "👳‍♂️");
        put("older_man", "👴");
        put("older_woman", "👵");
        put("baby", "👶");
        put("female-construction-worker", "👷‍♀️");
        put("male-construction-worker", "👷‍♂️");
        put("construction_worker", "👷‍♂️");
        put("princess", "👸");
        put("japanese_ogre", "👹");
        put("japanese_goblin", "👺");
        put("ghost", "👻");
        put("angel", "👼");
        put("alien", "👽");
        put("space_invader", "👾");
        put("imp", "👿");
        put("skull", "💀");
        put("woman-tipping-hand", "💁‍♀️");
        put("information_desk_person", "💁‍♀️");
        put("man-tipping-hand", "💁‍♂️");
        put("female-guard", "💂‍♀️");
        put("male-guard", "💂‍♂️");
        put("guardsman", "💂‍♂️");
        put("dancer", "💃");
        put("lipstick", "💄");
        put("nail_care", "💅");
        put("woman-getting-massage", "💆‍♀️");
        put("massage", "💆‍♀️");
        put("man-getting-massage", "💆‍♂️");
        put("woman-getting-haircut", "💇‍♀️");
        put("haircut", "💇‍♀️");
        put("man-getting-haircut", "💇‍♂️");
        put("barber", "💈");
        put("syringe", "💉");
        put("pill", "💊");
        put("kiss", "💋");
        put("love_letter", "💌");
        put("ring", "💍");
        put("gem", "💎");
        put("bouquet", "💐");
        put("wedding", "💒");
        put("heartbeat", "💓");
        put("broken_heart", "💔");
        put("two_hearts", "💕");
        put("sparkling_heart", "💖");
        put("heartpulse", "💗");
        put("cupid", "💘");
        put("blue_heart", "💙");
        put("green_heart", "💚");
        put("yellow_heart", "💛");
        put("purple_heart", "💜");
        put("gift_heart", "💝");
        put("revolving_hearts", "💞");
        put("heart_decoration", "💟");
        put("diamond_shape_with_a_dot_inside", "💠");
        put("bulb", "💡");
        put("anger", "💢");
        put("bomb", "💣");
        put("zzz", "💤");
        put("boom", "💥");
        put("collision", "💥");
        put("sweat_drops", "💦");
        put("droplet", "💧");
        put("dash", "💨");
        put("hankey", "💩");
        put("poop", "💩");
        put("shit", "💩");
        put("muscle", "💪");
        put("dizzy", "💫");
        put("speech_balloon", "💬");
        put("thought_balloon", "💭");
        put("white_flower", "💮");
        put("100", "💯");
        put("moneybag", "💰");
        put("currency_exchange", "💱");
        put("heavy_dollar_sign", "💲");
        put("credit_card", "💳");
        put("yen", "💴");
        put("dollar", "💵");
        put("euro", "💶");
        put("pound", "💷");
        put("money_with_wings", "💸");
        put("chart", "💹");
        put("seat", "💺");
        put("computer", "💻");
        put("briefcase", "💼");
        put("minidisc", "💽");
        put("floppy_disk", "💾");
        put("cd", "💿");
        put("dvd", "📀");
        put("file_folder", "📁");
        put("open_file_folder", "📂");
        put("page_with_curl", "📃");
        put("page_facing_up", "📄");
        put("date", "📅");
        put("calendar", "📆");
        put("card_index", "📇");
        put("chart_with_upwards_trend", "📈");
        put("chart_with_downwards_trend", "📉");
        put("bar_chart", "📊");
        put("clipboard", "📋");
        put("pushpin", "📌");
        put("round_pushpin", "📍");
        put("paperclip", "📎");
        put("straight_ruler", "📏");
        put("triangular_ruler", "📐");
        put("bookmark_tabs", "📑");
        put("ledger", "📒");
        put("notebook", "📓");
        put("notebook_with_decorative_cover", "📔");
        put("closed_book", "📕");
        put("book", "📖");
        put("open_book", "📖");
        put("green_book", "📗");
        put("blue_book", "📘");
        put("orange_book", "📙");
        put("books", "📚");
        put("name_badge", "📛");
        put("scroll", "📜");
        put("memo", "📝");
        put("pencil", "📝");
        put("telephone_receiver", "📞");
        put("pager", "📟");
        put("fax", "📠");
        put("satellite_antenna", "📡");
        put("loudspeaker", "📢");
        put("mega", "📣");
        put("outbox_tray", "📤");
        put("inbox_tray", "📥");
        put("package", "📦");
        put("e-mail", "📧");
        put("incoming_envelope", "📨");
        put("envelope_with_arrow", "📩");
        put("mailbox_closed", "📪");
        put("mailbox", "📫");
        put("mailbox_with_mail", "📬");
        put("mailbox_with_no_mail", "📭");
        put("postbox", "📮");
        put("postal_horn", "📯");
        put("newspaper", "📰");
        put("iphone", "📱");
        put("calling", "📲");
        put("vibration_mode", "📳");
        put("mobile_phone_off", "📴");
        put("no_mobile_phones", "📵");
        put("signal_strength", "📶");
        put("camera", "📷");
        put("camera_with_flash", "📸");
        put("video_camera", "📹");
        put("tv", "📺");
        put("radio", "📻");
        put("vhs", "📼");
        put("film_projector", "📽️");
        put("prayer_beads", "📿");
        put("twisted_rightwards_arrows", "🔀");
        put("repeat", "🔁");
        put("repeat_one", "🔂");
        put("arrows_clockwise", "🔃");
        put("arrows_counterclockwise", "🔄");
        put("low_brightness", "🔅");
        put("high_brightness", "🔆");
        put("mute", "🔇");
        put("speaker", "🔈");
        put("sound", "🔉");
        put("loud_sound", "🔊");
        put("battery", "🔋");
        put("electric_plug", "🔌");
        put("mag", "🔍");
        put("mag_right", "🔎");
        put("lock_with_ink_pen", "🔏");
        put("closed_lock_with_key", "🔐");
        put("key", "🔑");
        put("lock", "🔒");
        put("unlock", "🔓");
        put("bell", "🔔");
        put("no_bell", "🔕");
        put("bookmark", "🔖");
        put("link", "🔗");
        put("radio_button", "🔘");
        put("back", "🔙");
        put("end", "🔚");
        put("on", "🔛");
        put("soon", "🔜");
        put("top", "🔝");
        put("underage", "🔞");
        put("keycap_ten", "🔟");
        put("capital_abcd", "🔠");
        put("abcd", "🔡");
        put("1234", "🔢");
        put("symbols", "🔣");
        put("abc", "🔤");
        put("fire", "🔥");
        put("flashlight", "🔦");
        put("wrench", "🔧");
        put("hammer", "🔨");
        put("nut_and_bolt", "🔩");
        put("hocho", "🔪");
        put("knife", "🔪");
        put("gun", "🔫");
        put("microscope", "🔬");
        put("telescope", "🔭");
        put("crystal_ball", "🔮");
        put("six_pointed_star", "🔯");
        put("beginner", "🔰");
        put("trident", "🔱");
        put("black_square_button", "🔲");
        put("white_square_button", "🔳");
        put("red_circle", "🔴");
        put("large_blue_circle", "🔵");
        put("large_orange_diamond", "🔶");
        put("large_blue_diamond", "🔷");
        put("small_orange_diamond", "🔸");
        put("small_blue_diamond", "🔹");
        put("small_red_triangle", "🔺");
        put("small_red_triangle_down", "🔻");
        put("arrow_up_small", "🔼");
        put("arrow_down_small", "🔽");
        put("om_symbol", "🕉️");
        put("dove_of_peace", "🕊️");
        put("kaaba", "🕋");
        put("mosque", "🕌");
        put("synagogue", "🕍");
        put("menorah_with_nine_branches", "🕎");
        put("clock1", "🕐");
        put("clock2", "🕑");
        put("clock3", "🕒");
        put("clock4", "🕓");
        put("clock5", "🕔");
        put("clock6", "🕕");
        put("clock7", "🕖");
        put("clock8", "🕗");
        put("clock9", "🕘");
        put("clock10", "🕙");
        put("clock11", "🕚");
        put("clock12", "🕛");
        put("clock130", "🕜");
        put("clock230", "🕝");
        put("clock330", "🕞");
        put("clock430", "🕟");
        put("clock530", "🕠");
        put("clock630", "🕡");
        put("clock730", "🕢");
        put("clock830", "🕣");
        put("clock930", "🕤");
        put("clock1030", "🕥");
        put("clock1130", "🕦");
        put("clock1230", "🕧");
        put("candle", "🕯️");
        put("mantelpiece_clock", "🕰️");
        put("hole", "🕳️");
        put("man_in_business_suit_levitating", "🕴️");
        put("female-detective", "🕵️‍♀️");
        put("male-detective", "🕵️‍♂️");
        put("sleuth_or_spy", "🕵️‍♂️");
        put("dark_sunglasses", "🕶️");
        put("spider", "🕷️");
        put("spider_web", "🕸️");
        put("joystick", "🕹️");
        put("man_dancing", "🕺");
        put("linked_paperclips", "🖇️");
        put("lower_left_ballpoint_pen", "🖊️");
        put("lower_left_fountain_pen", "🖋️");
        put("lower_left_paintbrush", "🖌️");
        put("lower_left_crayon", "🖍️");
        put("raised_hand_with_fingers_splayed", "🖐️");
        put("middle_finger", "🖕");
        put("reversed_hand_with_middle_finger_extended", "🖕");
        put("spock-hand", "🖖");
        put("black_heart", "🖤");
        put("desktop_computer", "🖥️");
        put("printer", "🖨️");
        put("three_button_mouse", "🖱️");
        put("trackball", "🖲️");
        put("frame_with_picture", "🖼️");
        put("card_index_dividers", "🗂️");
        put("card_file_box", "🗃️");
        put("file_cabinet", "🗄️");
        put("wastebasket", "🗑️");
        put("spiral_note_pad", "🗒️");
        put("spiral_calendar_pad", "🗓️");
        put("compression", "🗜️");
        put("old_key", "🗝️");
        put("rolled_up_newspaper", "🗞️");
        put("dagger_knife", "🗡️");
        put("speaking_head_in_silhouette", "🗣️");
        put("left_speech_bubble", "🗨️");
        put("right_anger_bubble", "🗯️");
        put("ballot_box_with_ballot", "🗳️");
        put("world_map", "🗺️");
        put("mount_fuji", "🗻");
        put("tokyo_tower", "🗼");
        put("statue_of_liberty", "🗽");
        put("japan", "🗾");
        put("moyai", "🗿");
        put("grinning", "😀");
        put("grin", "😁");
        put("joy", "😂");
        put("smiley", "😃");
        put("smile", "😄");
        put("sweat_smile", "😅");
        put("laughing", "😆");
        put("satisfied", "😆");
        put("innocent", "😇");
        put("smiling_imp", "😈");
        put("wink", "😉");
        put("blush", "😊");
        put("yum", "😋");
        put("relieved", "😌");
        put("heart_eyes", "😍");
        put("sunglasses", "😎");
        put("smirk", "😏");
        put("neutral_face", "😐");
        put("expressionless", "😑");
        put("unamused", "😒");
        put("sweat", "😓");
        put("pensive", "😔");
        put("confused", "😕");
        put("confounded", "😖");
        put("kissing", "😗");
        put("kissing_heart", "😘");
        put("kissing_smiling_eyes", "😙");
        put("kissing_closed_eyes", "😚");
        put("stuck_out_tongue", "😛");
        put("stuck_out_tongue_winking_eye", "😜");
        put("stuck_out_tongue_closed_eyes", "😝");
        put("disappointed", "😞");
        put("worried", "😟");
        put("angry", "😠");
        put("rage", "😡");
        put("cry", "😢");
        put("persevere", "😣");
        put("triumph", "😤");
        put("disappointed_relieved", "😥");
        put("frowning", "😦");
        put("anguished", "😧");
        put("fearful", "😨");
        put("weary", "😩");
        put("sleepy", "😪");
        put("tired_face", "😫");
        put("grimacing", "😬");
        put("sob", "😭");
        put("open_mouth", "😮");
        put("hushed", "😯");
        put("cold_sweat", "😰");
        put("scream", "😱");
        put("astonished", "😲");
        put("flushed", "😳");
        put("sleeping", "😴");
        put("dizzy_face", "😵");
        put("no_mouth", "😶");
        put("mask", "😷");
        put("smile_cat", "😸");
        put("joy_cat", "😹");
        put("smiley_cat", "😺");
        put("heart_eyes_cat", "😻");
        put("smirk_cat", "😼");
        put("kissing_cat", "😽");
        put("pouting_cat", "😾");
        put("crying_cat_face", "😿");
        put("scream_cat", "🙀");
        put("slightly_frowning_face", "🙁");
        put("slightly_smiling_face", "🙂");
        put("upside_down_face", "🙃");
        put("face_with_rolling_eyes", "🙄");
        put("woman-gesturing-no", "🙅‍♀️");
        put("no_good", "🙅‍♀️");
        put("man-gesturing-no", "🙅‍♂️");
        put("woman-gesturing-ok", "🙆‍♀️");
        put("ok_woman", "🙆‍♀️");
        put("man-gesturing-ok", "🙆‍♂️");
        put("woman-bowing", "🙇‍♀️");
        put("man-bowing", "🙇‍♂️");
        put("bow", "🙇‍♂️");
        put("see_no_evil", "🙈");
        put("hear_no_evil", "🙉");
        put("speak_no_evil", "🙊");
        put("woman-raising-hand", "🙋‍♀️");
        put("raising_hand", "🙋‍♀️");
        put("man-raising-hand", "🙋‍♂️");
        put("raised_hands", "🙌");
        put("woman-frowning", "🙍‍♀️");
        put("person_frowning", "🙍‍♀️");
        put("man-frowning", "🙍‍♂️");
        put("woman-pouting", "🙎‍♀️");
        put("person_with_pouting_face", "🙎‍♀️");
        put("man-pouting", "🙎‍♂️");
        put("pray", "🙏");
        put("rocket", "🚀");
        put("helicopter", "🚁");
        put("steam_locomotive", "🚂");
        put("railway_car", "🚃");
        put("bullettrain_side", "🚄");
        put("bullettrain_front", "🚅");
        put("train2", "🚆");
        put("metro", "🚇");
        put("light_rail", "🚈");
        put("station", "🚉");
        put("tram", "🚊");
        put("train", "🚋");
        put("bus", "🚌");
        put("oncoming_bus", "🚍");
        put("trolleybus", "🚎");
        put("busstop", "🚏");
        put("minibus", "🚐");
        put("ambulance", "🚑");
        put("fire_engine", "🚒");
        put("police_car", "🚓");
        put("oncoming_police_car", "🚔");
        put("taxi", "🚕");
        put("oncoming_taxi", "🚖");
        put("car", "🚗");
        put("red_car", "🚗");
        put("oncoming_automobile", "🚘");
        put("blue_car", "🚙");
        put("truck", "🚚");
        put("articulated_lorry", "🚛");
        put("tractor", "🚜");
        put("monorail", "🚝");
        put("mountain_railway", "🚞");
        put("suspension_railway", "🚟");
        put("mountain_cableway", "🚠");
        put("aerial_tramway", "🚡");
        put("ship", "🚢");
        put("woman-rowing-boat", "🚣‍♀️");
        put("man-rowing-boat", "🚣‍♂️");
        put("rowboat", "🚣‍♂️");
        put("speedboat", "🚤");
        put("traffic_light", "🚥");
        put("vertical_traffic_light", "🚦");
        put("construction", "🚧");
        put("rotating_light", "🚨");
        put("triangular_flag_on_post", "🚩");
        put("door", "🚪");
        put("no_entry_sign", "🚫");
        put("smoking", "🚬");
        put("no_smoking", "🚭");
        put("put_litter_in_its_place", "🚮");
        put("do_not_litter", "🚯");
        put("potable_water", "🚰");
        put("non-potable_water", "🚱");
        put("bike", "🚲");
        put("no_bicycles", "🚳");
        put("woman-biking", "🚴‍♀️");
        put("man-biking", "🚴‍♂️");
        put("bicyclist", "🚴‍♂️");
        put("woman-mountain-biking", "🚵‍♀️");
        put("man-mountain-biking", "🚵‍♂️");
        put("mountain_bicyclist", "🚵‍♂️");
        put("woman-walking", "🚶‍♀️");
        put("man-walking", "🚶‍♂️");
        put("walking", "🚶‍♂️");
        put("no_pedestrians", "🚷");
        put("children_crossing", "🚸");
        put("mens", "🚹");
        put("womens", "🚺");
        put("restroom", "🚻");
        put("baby_symbol", "🚼");
        put("toilet", "🚽");
        put("wc", "🚾");
        put("shower", "🚿");
        put("bath", "🛀");
        put("bathtub", "🛁");
        put("passport_control", "🛂");
        put("customs", "🛃");
        put("baggage_claim", "🛄");
        put("left_luggage", "🛅");
        put("couch_and_lamp", "🛋️");
        put("sleeping_accommodation", "🛌");
        put("shopping_bags", "🛍️");
        put("bellhop_bell", "🛎️");
        put("bed", "🛏️");
        put("place_of_worship", "🛐");
        put("octagonal_sign", "🛑");
        put("shopping_trolley", "🛒");
        put("hammer_and_wrench", "🛠️");
        put("shield", "🛡️");
        put("oil_drum", "🛢️");
        put("motorway", "🛣️");
        put("railway_track", "🛤️");
        put("motor_boat", "🛥️");
        put("small_airplane", "🛩️");
        put("airplane_departure", "🛫");
        put("airplane_arriving", "🛬");
        put("satellite", "🛰️");
        put("passenger_ship", "🛳️");
        put("scooter", "🛴");
        put("motor_scooter", "🛵");
        put("canoe", "🛶");
        put("sled", "🛷");
        put("flying_saucer", "🛸");
        put("skateboard", "🛹");
        put("zipper_mouth_face", "🤐");
        put("money_mouth_face", "🤑");
        put("face_with_thermometer", "🤒");
        put("nerd_face", "🤓");
        put("thinking_face", "🤔");
        put("face_with_head_bandage", "🤕");
        put("robot_face", "🤖");
        put("hugging_face", "🤗");
        put("the_horns", "🤘");
        put("sign_of_the_horns", "🤘");
        put("call_me_hand", "🤙");
        put("raised_back_of_hand", "🤚");
        put("left-facing_fist", "🤛");
        put("right-facing_fist", "🤜");
        put("handshake", "🤝");
        put("crossed_fingers", "🤞");
        put("hand_with_index_and_middle_fingers_crossed", "🤞");
        put("i_love_you_hand_sign", "🤟");
        put("face_with_cowboy_hat", "🤠");
        put("clown_face", "🤡");
        put("nauseated_face", "🤢");
        put("rolling_on_the_floor_laughing", "🤣");
        put("drooling_face", "🤤");
        put("lying_face", "🤥");
        put("woman-facepalming", "🤦‍♀️");
        put("man-facepalming", "🤦‍♂️");
        put("face_palm", "🤦");
        put("sneezing_face", "🤧");
        put("face_with_raised_eyebrow", "🤨");
        put("face_with_one_eyebrow_raised", "🤨");
        put("star-struck", "🤩");
        put("grinning_face_with_star_eyes", "🤩");
        put("zany_face", "🤪");
        put("grinning_face_with_one_large_and_one_small_eye", "🤪");
        put("shushing_face", "🤫");
        put("face_with_finger_covering_closed_lips", "🤫");
        put("face_with_symbols_on_mouth", "🤬");
        put("serious_face_with_symbols_covering_mouth", "🤬");
        put("face_with_hand_over_mouth", "🤭");
        put("smiling_face_with_smiling_eyes_and_hand_covering_mouth", "🤭");
        put("face_vomiting", "🤮");
        put("face_with_open_mouth_vomiting", "🤮");
        put("exploding_head", "🤯");
        put("shocked_face_with_exploding_head", "🤯");
        put("pregnant_woman", "🤰");
        put("breast-feeding", "🤱");
        put("palms_up_together", "🤲");
        put("selfie", "🤳");
        put("prince", "🤴");
        put("man_in_tuxedo", "🤵");
        put("mrs_claus", "🤶");
        put("mother_christmas", "🤶");
        put("woman-shrugging", "🤷‍♀️");
        put("man-shrugging", "🤷‍♂️");
        put("shrug", "🤷");
        put("woman-cartwheeling", "🤸‍♀️");
        put("man-cartwheeling", "🤸‍♂️");
        put("person_doing_cartwheel", "🤸");
        put("woman-juggling", "🤹‍♀️");
        put("man-juggling", "🤹‍♂️");
        put("juggling", "🤹");
        put("fencer", "🤺");
        put("woman-wrestling", "🤼‍♀️");
        put("man-wrestling", "🤼‍♂️");
        put("wrestlers", "🤼");
        put("woman-playing-water-polo", "🤽‍♀️");
        put("man-playing-water-polo", "🤽‍♂️");
        put("water_polo", "🤽");
        put("woman-playing-handball", "🤾‍♀️");
        put("man-playing-handball", "🤾‍♂️");
        put("handball", "🤾");
        put("wilted_flower", "🥀");
        put("drum_with_drumsticks", "🥁");
        put("clinking_glasses", "🥂");
        put("tumbler_glass", "🥃");
        put("spoon", "🥄");
        put("goal_net", "🥅");
        put("first_place_medal", "🥇");
        put("second_place_medal", "🥈");
        put("third_place_medal", "🥉");
        put("boxing_glove", "🥊");
        put("martial_arts_uniform", "🥋");
        put("curling_stone", "🥌");
        put("lacrosse", "🥍");
        put("softball", "🥎");
        put("flying_disc", "🥏");
        put("croissant", "🥐");
        put("avocado", "🥑");
        put("cucumber", "🥒");
        put("bacon", "🥓");
        put("potato", "🥔");
        put("carrot", "🥕");
        put("baguette_bread", "🥖");
        put("green_salad", "🥗");
        put("shallow_pan_of_food", "🥘");
        put("stuffed_flatbread", "🥙");
        put("egg", "🥚");
        put("glass_of_milk", "🥛");
        put("peanuts", "🥜");
        put("kiwifruit", "🥝");
        put("pancakes", "🥞");
        put("dumpling", "🥟");
        put("fortune_cookie", "🥠");
        put("takeout_box", "🥡");
        put("chopsticks", "🥢");
        put("bowl_with_spoon", "🥣");
        put("cup_with_straw", "🥤");
        put("coconut", "🥥");
        put("broccoli", "🥦");
        put("pie", "🥧");
        put("pretzel", "🥨");
        put("cut_of_meat", "🥩");
        put("sandwich", "🥪");
        put("canned_food", "🥫");
        put("leafy_green", "🥬");
        put("mango", "🥭");
        put("moon_cake", "🥮");
        put("bagel", "🥯");
        put("smiling_face_with_3_hearts", "🥰");
        put("partying_face", "🥳");
        put("woozy_face", "🥴");
        put("hot_face", "🥵");
        put("cold_face", "🥶");
        put("pleading_face", "🥺");
        put("lab_coat", "🥼");
        put("goggles", "🥽");
        put("hiking_boot", "🥾");
        put("womans_flat_shoe", "🥿");
        put("crab", "🦀");
        put("lion_face", "🦁");
        put("scorpion", "🦂");
        put("turkey", "🦃");
        put("unicorn_face", "🦄");
        put("eagle", "🦅");
        put("duck", "🦆");
        put("bat", "🦇");
        put("shark", "🦈");
        put("owl", "🦉");
        put("fox_face", "🦊");
        put("butterfly", "🦋");
        put("deer", "🦌");
        put("gorilla", "🦍");
        put("lizard", "🦎");
        put("rhinoceros", "🦏");
        put("shrimp", "🦐");
        put("squid", "🦑");
        put("giraffe_face", "🦒");
        put("zebra_face", "🦓");
        put("hedgehog", "🦔");
        put("sauropod", "🦕");
        put("t-rex", "🦖");
        put("cricket", "🦗");
        put("kangaroo", "🦘");
        put("llama", "🦙");
        put("peacock", "🦚");
        put("hippopotamus", "🦛");
        put("parrot", "🦜");
        put("raccoon", "🦝");
        put("lobster", "🦞");
        put("mosquito", "🦟");
        put("microbe", "🦠");
        put("badger", "🦡");
        put("swan", "🦢");
        put("bone", "🦴");
        put("leg", "🦵");
        put("foot", "🦶");
        put("tooth", "🦷");
        put("female_superhero", "🦸‍♀️");
        put("male_superhero", "🦸‍♂️");
        put("female_supervillain", "🦹‍♀️");
        put("male_supervillain", "🦹‍♂️");
        put("cheese_wedge", "🧀");
        put("cupcake", "🧁");
        put("salt", "🧂");
        put("face_with_monocle", "🧐");
        put("adult", "🧑");
        put("child", "🧒");
        put("older_adult", "🧓");
        put("bearded_person", "🧔");
        put("person_with_headscarf", "🧕");
        put("woman_in_steamy_room", "🧖‍♀️");
        put("man_in_steamy_room", "🧖‍♂️");
        put("person_in_steamy_room", "🧖‍♂️");
        put("woman_climbing", "🧗‍♀️");
        put("person_climbing", "🧗‍♀️");
        put("man_climbing", "🧗‍♂️");
        put("woman_in_lotus_position", "🧘‍♀️");
        put("person_in_lotus_position", "🧘‍♀️");
        put("man_in_lotus_position", "🧘‍♂️");
        put("female_mage", "🧙‍♀️");
        put("mage", "🧙‍♀️");
        put("male_mage", "🧙‍♂️");
        put("female_fairy", "🧚‍♀️");
        put("fairy", "🧚‍♀️");
        put("male_fairy", "🧚‍♂️");
        put("female_vampire", "🧛‍♀️");
        put("vampire", "🧛‍♀️");
        put("male_vampire", "🧛‍♂️");
        put("mermaid", "🧜‍♀️");
        put("merman", "🧜‍♂️");
        put("merperson", "🧜‍♂️");
        put("female_elf", "🧝‍♀️");
        put("male_elf", "🧝‍♂️");
        put("elf", "🧝‍♂️");
        put("female_genie", "🧞‍♀️");
        put("male_genie", "🧞‍♂️");
        put("genie", "🧞‍♂️");
        put("female_zombie", "🧟‍♀️");
        put("male_zombie", "🧟‍♂️");
        put("zombie", "🧟‍♂️");
        put("brain", "🧠");
        put("orange_heart", "🧡");
        put("billed_cap", "🧢");
        put("scarf", "🧣");
        put("gloves", "🧤");
        put("coat", "🧥");
        put("socks", "🧦");
        put("red_envelope", "🧧");
        put("firecracker", "🧨");
        put("jigsaw", "🧩");
        put("test_tube", "🧪");
        put("petri_dish", "🧫");
        put("dna", "🧬");
        put("compass", "🧭");
        put("abacus", "🧮");
        put("fire_extinguisher", "🧯");
        put("toolbox", "🧰");
        put("bricks", "🧱");
        put("magnet", "🧲");
        put("luggage", "🧳");
        put("lotion_bottle", "🧴");
        put("thread", "🧵");
        put("yarn", "🧶");
        put("safety_pin", "🧷");
        put("teddy_bear", "🧸");
        put("broom", "🧹");
        put("basket", "🧺");
        put("roll_of_paper", "🧻");
        put("soap", "🧼");
        put("sponge", "🧽");
        put("receipt", "🧾");
        put("nazar_amulet", "🧿");
        put("bangbang", "‼️");
        put("interrobang", "⁉️");
        put("tm", "™️");
        put("information_source", "ℹ️");
        put("left_right_arrow", "↔️");
        put("arrow_up_down", "↕️");
        put("arrow_upper_left", "↖️");
        put("arrow_upper_right", "↗️");
        put("arrow_lower_right", "↘️");
        put("arrow_lower_left", "↙️");
        put("leftwards_arrow_with_hook", "↩️");
        put("arrow_right_hook", "↪️");
        put("watch", "⌚");
        put("hourglass", "⌛");
        put("keyboard", "⌨️");
        put("eject", "⏏️");
        put("fast_forward", "⏩");
        put("rewind", "⏪");
        put("arrow_double_up", "⏫");
        put("arrow_double_down", "⏬");
        put("black_right_pointing_double_triangle_with_vertical_bar", "⏭️");
        put("black_left_pointing_double_triangle_with_vertical_bar", "⏮️");
        put("black_right_pointing_triangle_with_double_vertical_bar", "⏯️");
        put("alarm_clock", "⏰");
        put("stopwatch", "⏱️");
        put("timer_clock", "⏲️");
        put("hourglass_flowing_sand", "⏳");
        put("double_vertical_bar", "⏸️");
        put("black_square_for_stop", "⏹️");
        put("black_circle_for_record", "⏺️");
        put("m", "Ⓜ️");
        put("black_small_square", "▪️");
        put("white_small_square", "▫️");
        put("arrow_forward", "▶️");
        put("arrow_backward", "◀️");
        put("white_medium_square", "◻️");
        put("black_medium_square", "◼️");
        put("white_medium_small_square", "◽");
        put("black_medium_small_square", "◾");
        put("sunny", "☀️");
        put("cloud", "☁️");
        put("umbrella", "☂️");
        put("snowman", "☃️");
        put("comet", "☄️");
        put("phone", "☎️");
        put("telephone", "☎️");
        put("ballot_box_with_check", "☑️");
        put("shamrock", "☘️");
        put("point_up", "☝️");
        put("skull_and_crossbones", "☠️");
        put("radioactive_sign", "☢️");
        put("biohazard_sign", "☣️");
        put("orthodox_cross", "☦️");
        put("star_and_crescent", "☪️");
        put("peace_symbol", "☮️");
        put("yin_yang", "☯️");
        put("wheel_of_dharma", "☸️");
        put("white_frowning_face", "☹️");
        put("relaxed", "☺️");
        put("female_sign", "♀️");
        put("male_sign", "♂️");
        put("gemini", "♊");
        put("cancer", "♋");
        put("leo", "♌");
        put("virgo", "♍");
        put("libra", "♎");
        put("scorpius", "♏");
        put("chess_pawn", "♟️");
        put("spades", "♠️");
        put("clubs", "♣️");
        put("hearts", "♥️");
        put("diamonds", "♦️");
        put("hotsprings", "♨️");
        put("recycle", "♻️");
        put("infinity", "♾️");
        put("wheelchair", "♿");
        put("hammer_and_pick", "⚒️");
        put("crossed_swords", "⚔️");
        put("medical_symbol", "⚕️");
        put("staff_of_aesculapius", "⚕️");
        put("scales", "⚖️");
        put("alembic", "⚗️");
        put("gear", "⚙️");
        put("atom_symbol", "⚛️");
        put("fleur_de_lis", "⚜️");
        put("warning", "⚠️");
        put("zap", "⚡");
        put("white_circle", "⚪");
        put("black_circle", "⚫");
        put("coffin", "⚰️");
        put("funeral_urn", "⚱️");
        put("soccer", "⚽");
        put("baseball", "⚾");
        put("snowman_without_snow", "⛄");
        put("partly_sunny", "⛅");
        put("thunder_cloud_and_rain", "⛈️");
        put("ophiuchus", "⛎");
        put("pick", "⛏️");
        put("helmet_with_white_cross", "⛑️");
        put("chains", "⛓️");
        put("no_entry", "⛔");
        put("shinto_shrine", "⛩️");
        put("church", "⛪");
        put("mountain", "⛰️");
        put("umbrella_on_ground", "⛱️");
        put("fountain", "⛲");
        put("golf", "⛳");
        put("ferry", "⛴️");
        put("boat", "⛵");
        put("sailboat", "⛵");
        put("skier", "⛷️");
        put("ice_skate", "⛸️");
        put("woman-bouncing-ball", "⛹️‍♀️");
        put("man-bouncing-ball", "⛹️‍♂️");
        put("person_with_ball", "⛹️‍♂️");
        put("tent", "⛺");
        put("fuelpump", "⛽");
        put("scissors", "✂️");
        put("airplane", "✈️");
        put("email", "✉️");
        put("envelope", "✉️");
        put("fist", "✊");
        put("hand", "✋");
        put("raised_hand", "✋");
        put("v", "✌️");
        put("writing_hand", "✍️");
        put("pencil2", "✏️");
        put("black_nib", "✒️");
        put("heavy_check_mark", "✔️");
        put("heavy_multiplication_x", "✖️");
        put("latin_cross", "✝️");
        put("star_of_david", "✡️");
        put("eight_spoked_asterisk", "✳️");
        put("eight_pointed_black_star", "✴️");
        put("snowflake", "❄️");
        put("sparkle", "❇️");
        put("x", "❌");
        put("negative_squared_cross_mark", "❎");
        put("heavy_heart_exclamation_mark_ornament", "❣️");
        put("heart", "❤️");
        put("arrow_right", "➡️");
        put("curly_loop", "➰");
        put("loop", "➿");
        put("arrow_heading_up", "⤴️");
        put("arrow_heading_down", "⤵️");
        put("arrow_left", "⬅️");
        put("arrow_up", "⬆️");
        put("arrow_down", "⬇️");
        put("black_large_square", "⬛");
        put("white_large_square", "⬜");
        put("star", "⭐");
        put("o", "⭕");
        put("wavy_dash", "〰️");
        put("part_alternation_mark", "〽️");
        put("congratulations", "㊗️");
        put("secret", "㊙️");

        put("like", "👍");
        put("thumbs_up", "👍");
        put("dislike", "👎");
        put("thumbs_down", "👎");
        put("doge", "🐕");
        put("aubergine", "🍆");
        put("gust_of_wind", "💨");
        put("party_popper", "🎉");
        put("shock", "😱");
        put("atom", "⚛️");
        put("<3", "❤️");
        put("</3", "💔");
        put("simple_smile", "🙂");
        put(":)", "🙂");
        put(":-)", "🙂");
        put(")", "🙂");
        put("-)", "🙂");
        put("=D", "😃");
        put(":D", "😀");
        put("D", "😀");
        put(":(", "😞");
        put("(", "😞");
        put(":'(", "😢");
        put("'(", "😢");
        put(":_(", "😭");
        put("_(", "😭");
        put("loudly_crying_face", "😭");
        put("sad_tears", "😭");
        put("bawl", "😭");
        put(";)", "😉");
        put(";p", "😜");
        put("XD", "😆");
        put("^_^", "😄");
        put("^_^;", "😅");
        put("rofl", "🤣");
        put(":|", "😐");
        put("|", "😐");
        put(">.<", "😣");
        put("ufo", "🛸");
        put("female_wizard", "🧙‍♀️");
        put("male_wizard", "🧙‍♂️");
        put("brontosaurus", "🦕");
        put("diplodocus", "🦕");
        put("tyrannosaurus", "🦖");
        put("steak", "🥩");
        put("soup_tin", "🥫");
        put("baseball_cap", "🧢");
        put("female_yoga", "🧘‍♀️");
        put("male_yoga", "🧘‍♂️");
        put("female_sauna", "🧖‍♀️");
        put("male_sauna", "🧖‍♂️");
        put("hijab", "🧕");
        put("ladybird", "🐞");
        put("ladybug", "🐞");
        put("ladybeetle", "🐞");
        put("coccinellid", "🐞");
        put("diamond", "💎");
        put("angel_face", "😇");
        put("smiling_devil", "😈");
        put("frowning_devil", "👿");
        put("mad_rage", "😡");
        put("angry_rage", "😡");
        put("mad", "😠");
        put("steam_train", "🚂");
        put("graduation_cap", "🎓");
        put("lightbulb", "💡");
        put("cool_dude", "😎");
        put("deal_with_it", "😎");
        put("liar", "🤥");
        put("bunny", "🐰");
        put("bunny2", "🐇");
        put("cigarette", "🚬");
        put("fag", "🚬");
        put("water_wave", "🌊");
        put("crazy_face", "🤪");
        put("sh", "🤫");
        put("angry_swearing", "🤬");
        put("mad_swearing", "🤬");
        put("cursing", "🤬");
        put("swearing", "🤬");
        put("pissed_off", "🤬");
        put("fuck", "🤬");
        put("oops", "🤭");
        put("throwing_up", "🤮");
        put("being_sick", "🤮");
        put("mind_blown", "🤯");
        put("lightning_bolt", "⚡");
        put("confetti", "🎊");
        put("rubbish", "🗑️");
        put("trash", "🗑️");
        put("garbage", "🗑️");
        put("bin", "🗑️");
        put("wastepaper_basket", "🗑️");
    }};

    public static Pattern EMOJI = null;

    public static final HashMap<String, String> conversionMap = new HashMap<String, String>() {{
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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
        }
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

    public static final HashMap<String, String> quotes = new HashMap<String, String>() {{
        put("\"", "\"");
        put("'", "'");
        put(")", "(");
        put("]", "[");
        put("}", "{");
        put(">", "<");
        put("”", "”");
        put("’", "’");
        put("»", "«");
    }};

    public static Pattern CONVERSION = null;

    public static Pattern IS_EMOJI = null;

    public static Pattern IS_BLOCKQUOTE = Pattern.compile("(^|\\n)>(?![<>]|[\\W_](?:[<>/OoDpb|\\\\{}()\\[\\]](?=\\s|$)))([^\\n]+)");
    public static Pattern IS_CODE_SPAN = Pattern.compile("`([^`\\n]+?)`");

    public static Pattern HTML_ENTITY = Pattern.compile("&[^\\s;]+;");

    private static TextClassifier tc;

    public static void init() {
        if(sourceSansPro == null)
            sourceSansPro = ResourcesCompat.getFont(IRCCloudApplication.getInstance().getApplicationContext(), R.font.sourcesansproregular);

        if(Hack == null)
            Hack = ResourcesCompat.getFont(IRCCloudApplication.getInstance().getApplicationContext(), R.font.hackregular);

        if(EMOJI == null) {
            long start = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder(16384);
            sb.append("\\B:(");
            for (String key : emojiMap.keySet()) {
                if (sb.length() > 4)
                    sb.append("|");
                for(int i = 0; i < key.length(); i++) {
                    char c = key.charAt(i);
                    if(c == '-' || c == '+' || c == '(' || c == ')')
                        sb.append('\\');
                    sb.append(c);

                }
            }
            sb.append("):\\B");

            EMOJI = Pattern.compile(sb.toString());

            sb.setLength(0);
            sb.append("(");
            for (String key : conversionMap.keySet()) {
                if (sb.length() > 2)
                    sb.append("|");
                sb.append(key);
            }
            sb.append(")");

            CONVERSION = Pattern.compile(sb.toString());

            sb.setLength(0);
            sb.append("(?:");
            for (String key : emojiMap.keySet()) {
                if (sb.length() > 3)
                    sb.append("|");
                sb.append(emojiMap.get(key));
            }
            for (String value : conversionMap.values()) {
                if (sb.length() > 3)
                    sb.append("|");
                sb.append(value);
            }
            sb.append("|\u200d|\ufe0f)");

            IS_EMOJI = Pattern.compile(sb.toString().replace(":)|","").replace("*", "\\*"));

            Crashlytics.log(Log.INFO, "IRCCloud", "Compiled :emocode: regex from " + emojiMap.size() + " keys in " + (System.currentTimeMillis() - start) + "ms");
        }

        if(tc == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            tc = IRCCloudApplication.getInstance().getSystemService(TextClassificationManager.class).getTextClassifier();
        }
    }

    public static String emojify(String msg) {
        if (msg == null)
            return "";

        boolean disableConvert = false;
        try {
            if (NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
                disableConvert = NetworkConnection.getInstance().getUserInfo().prefs.getBoolean("emoji-disableconvert");
            } else {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
                disableConvert = prefs.getBoolean("emoji-disableconvert", false);
            }
        } catch (Exception e) {
        }

        StringBuilder builder = new StringBuilder(msg);
        int offset;

        if (!disableConvert) {
            Matcher m = EMOJI.matcher(msg);
            while (m.find()) {
                if (emojiMap.containsKey(m.group(1))) {
                    offset = msg.length() - builder.length();
                    builder.replace(m.start(1) - offset - 1, m.end(1) - offset + 1, emojiMap.get(m.group(1)));
                }
            }
            msg = builder.toString();
        }

        Matcher m = CONVERSION.matcher(msg);
        while (m.find()) {
            if (conversionMap.containsKey(m.group(1))) {
                offset = msg.length() - builder.length();
                builder.replace(m.start(1) - offset, m.end(1) - offset, conversionMap.get(m.group(1)));
            }
        }
        return builder.toString();
    }

    public static boolean is_emoji(String text) {
        return text != null && text.length() > 0 && IS_EMOJI.matcher(text.trim()).replaceAll("").length() == 0;
    }

    public static boolean is_blockquote(String text) {
        return text != null && text.length() > 0 && IS_BLOCKQUOTE.matcher(text).matches();
    }

    public static String insert_codespans(String msg) {
        StringBuilder output = new StringBuilder(msg);
        Matcher m = IS_CODE_SPAN.matcher(msg);

        while(m.find()) {
            output.setCharAt(m.start(), (char)0x11);
            output.setCharAt(m.end() - 1, (char)0x11);
        }

        return output.toString();
    }

    public static Spanned html_to_spanned(String msg) {
        return html_to_spanned(msg, false, null, null, false);
    }

    public static Spanned html_to_spanned(String msg, boolean linkify, final Server server) {
        return html_to_spanned(msg, linkify, server, null, false);
    }

    public static CharSequence strip(String msg) {
        if(Build.VERSION.SDK_INT >= 19 && EmojiCompat.get().getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED)
            return EmojiCompat.get().process(html_to_spanned(irc_to_html(TextUtils.htmlEncode(emojify(msg)))).toString());
        else
            return html_to_spanned(irc_to_html(TextUtils.htmlEncode(emojify(msg)))).toString();
    }

    public static Spanned html_to_spanned(String msg, boolean linkify, final Server server, final JsonNode entities) {
        return html_to_spanned(msg, linkify, server, entities, false);
    }

    public static Spanned html_to_spanned(String msg, boolean linkify, final Server server, final JsonNode entities, final boolean colorize_mentions) {
        if (msg == null)
            msg = "";

        final ArrayList<Mention> mention_spans = new ArrayList<>();

        final Spannable output = (Spannable) Html.fromHtml(msg, null, new Html.TagHandler() {
            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                int len = output.length();
                if (tag.startsWith("_bg")) {
                    String rgb = "#";
                    if (tag.length() == 9) {
                        rgb += tag.substring(3);
                    } else {
                        rgb += "ffffff";
                    }
                    if (opening) {
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
                } else if(tag.equals("large")) {
                    if (opening) {
                        output.setSpan(new LargeSpan(), len, len, Spannable.SPAN_MARK_MARK);
                    } else {
                        Object obj = getLast(output, LargeSpan.class);
                        int where = output.getSpanStart(obj);

                        output.removeSpan(obj);

                        if (where != len) {
                            output.setSpan(new LargeSpan(), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                } else if(tag.equals("pre")) {
                    if (opening) {
                        output.setSpan(new TypefaceSpan(Hack), len, len, Spannable.SPAN_MARK_MARK);
                    } else {
                        Object obj = getLast(output, TypefaceSpan.class);
                        int where = output.getSpanStart(obj);

                        output.removeSpan(obj);

                        if (where != len) {
                            output.setSpan(new TypefaceSpan(Hack), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                } else if(tag.equals("nick")) {
                    if (opening) {
                        output.setSpan(new ForegroundColorSpan(0), len, len, Spannable.SPAN_MARK_MARK);
                    } else {
                        Object obj = getLast(output, ForegroundColorSpan.class);
                        int where = output.getSpanStart(obj);

                        output.removeSpan(obj);

                        if (where != len) {
                            String nick = output.subSequence(where, len).toString();
                            if(server != null) {
                                User u = UsersList.getInstance().findUserOnConnection(server.getCid(), nick);
                                if(u != null)
                                    nick = u.nick;
                            }
                            Mention m = new Mention();
                            m.position = where;
                            m.length = len;
                            mention_spans.add(m);
                            if(server != null && !nick.equalsIgnoreCase(server.getNick())) {
                                m.span = new ForegroundColorSpan(Color.parseColor("#" + ColorScheme.colorForNick(nick, ColorScheme.getInstance().isDarkTheme)));
                            }
                        }
                    }
                }
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            private Object getLast(Editable text, Class kind) {
                Object[] objs = text.getSpans(0, text.length(), kind);

                if (objs.length == 0) {
                    return null;
                } else {
                    for (int i = objs.length; i > 0; i--) {
                        if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                            return objs[i - 1];
                        }
                    }
                    return null;
                }
            }
        });

        for (Mention m : mention_spans) {
            if(m.span != null && colorize_mentions)
                output.setSpan(m.span, m.position, m.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                output.setSpan(new ForegroundColorSpan(ColorScheme.getInstance().collapsedRowNickColor), m.position, m.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (linkify) {
            String chanTypes = Buffer.DEFAULT_CHANTYPES;
            if (server != null && server.CHANTYPES != null && server.CHANTYPES.length() > 0)
                chanTypes = server.CHANTYPES;
            final String pattern = "\\B([" + chanTypes + "]([^\ufe0e\ufe0f\u20e3<>\",\\s][^<>\",\\s]*))";

            MatchFilter noOverlapFilter = new MatchFilter() {
                @Override
                public boolean acceptMatch(CharSequence s, int start, int end) {
                    return output.getSpans(start, end, URLSpan.class).length == 0;
                }
            };

            Linkify.addLinks(output, WEB_URL, null, new MatchFilter() {
                public final boolean acceptMatch(CharSequence s, int start, int end) {
                    if (start >= 6 && s.subSequence(start - 6, end).toString().toLowerCase().startsWith("irc://"))
                        return false;
                    if (start >= 7 && s.subSequence(start - 7, end).toString().toLowerCase().startsWith("ircs://"))
                        return false;
                    if (start >= 1 && s.subSequence(start - 1, end).toString().matches(pattern))
                        return false;
                    if (s.subSequence(start, end).toString().matches("[0-9\\.]+"))
                        return false;
                    return Linkify.sUrlMatchFilter.acceptMatch(s, start, end);
                }
            }, new TransformFilter() {
                @Override
                public String transformUrl(Matcher match, String url) {
                    if (!url.contains("://")) {
                        if (url.toLowerCase().startsWith("irc."))
                            url = "irc://" + url;
                        else
                            url = "http://" + url;
                    } else {
                        String protocol = url.toLowerCase().substring(0, url.indexOf("://"));
                        url = protocol + url.substring(protocol.length());
                    }

                    char last = url.charAt(url.length() - 1);
                    if (isPunctuation(last)) {
                        url = url.substring(0, url.length() - 1);
                        last = url.charAt(url.length() - 1);
                    }

                    if (quotes.containsKey(String.valueOf(last))) {
                        char open = quotes.get(String.valueOf(last)).charAt(0);
                        int countOpen = 0, countClose = 0;
                        for (int i = 0; i < url.length(); i++) {
                            char c = url.charAt(i);
                            if (c == open)
                                countOpen++;
                            else if (c == last)
                                countClose++;
                        }
                        if (countOpen != countClose) {
                            url = url.substring(0, url.length() - 1);
                        }
                    }

                    if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("imageviewer", true)) {
                        String lower = url.toLowerCase();
                        if (lower.contains("?"))
                            lower = lower.substring(0, lower.indexOf("?"));

                        if (ImageList.isImageURL(lower)) {
                            if (lower.startsWith("http://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME) + "://" + url.substring(7);
                            else if (lower.startsWith("https://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME_SECURE) + "://" + url.substring(8);
                        }
                    }

                    if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("videoviewer", true)) {
                        String lower = url.toLowerCase();
                        if (lower.contains("?"))
                            lower = lower.substring(0, lower.indexOf("?"));

                        if (lower.matches("(^.*/.*\\.3gpp?)|(^.*/.*\\.mp4$)|(^.*/.*\\.m4v$)|(^.*/.*\\.webm$)")) {
                            if (lower.startsWith("http://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.VIDEO_SCHEME) + "://" + url.substring(7);
                            else if (lower.startsWith("https://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.VIDEO_SCHEME_SECURE) + "://" + url.substring(8);
                        }
                    }

                    if (entities != null && entities.has("pastes")) {
                        if (NetworkConnection.pastebin_uri_template != null) {
                            UriTemplate template = UriTemplate.fromTemplate(NetworkConnection.pastebin_uri_template);
                            for (JsonNode paste : entities.get("pastes")) {
                                String paste_url = template.set("id", paste.get("id").asText()).expand();
                                if (url.startsWith(paste_url)) {
                                    if (url.toLowerCase().startsWith("http://"))
                                        return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.PASTE_SCHEME) + "://" + paste_url.substring(7) + "?id=" + paste.get("id").asText();
                                    else
                                        return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.PASTE_SCHEME) + "://" + paste_url.substring(8) + "?id=" + paste.get("id").asText();
                                }
                            }
                        }
                    }
                    return url;
                }
            });
            Linkify.addLinks(output, Patterns.EMAIL_ADDRESS, "mailto:", noOverlapFilter, null);
            Linkify.addLinks(output, Pattern.compile("ircs?://[^<>\",\\s]+"), null, noOverlapFilter, new TransformFilter() {
                public final String transformUrl(final Matcher match, String url) {
                    char last = url.charAt(url.length() - 1);
                    if (isPunctuation(last)) {
                        url = url.substring(0, url.length() - 1);
                        last = url.charAt(url.length() - 1);
                    }

                    if (quotes.containsKey(String.valueOf(last))) {
                        char open = quotes.get(String.valueOf(last)).charAt(0);
                        int countOpen = 0, countClose = 0;
                        for (int i = 0; i < url.length(); i++) {
                            char c = url.charAt(i);
                            if (c == open)
                                countOpen++;
                            else if (c == last)
                                countClose++;
                        }
                        if (countOpen != countClose) {
                            url = url.substring(0, url.length() - 1);
                        }
                    }

                    return url.replace("#", "%23");
                }
            });
            Linkify.addLinks(output, Pattern.compile("spotify:([a-zA-Z0-9:]+)"), null, noOverlapFilter, new TransformFilter() {
                public final String transformUrl(final Matcher match, String url) {
                    return "https://open.spotify.com/" + url.substring(8).replace(":", "/");
                }
            });
            if (server != null) {
                Linkify.addLinks(output, Pattern.compile(pattern), null, new MatchFilter() {
                    public final boolean acceptMatch(CharSequence s, int start, int end) {
                        try {
                            Integer.parseInt(s.subSequence(start + 1, end).toString());
                            return false;
                        } catch (NumberFormatException e) {
                            return output.getSpans(start, end, URLSpan.class).length == 0;
                        }
                    }
                }, new TransformFilter() {
                    public final String transformUrl(final Matcher match, String url) {
                        String channel = match.group(1);
                        try {
                            channel = URLEncoder.encode(channel, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                        }
                        return IRCCloudApplication.getInstance().getResources().getString(R.string.IRCCLOUD_SCHEME) + "://cid/" + server.getCid() + "/" + channel;
                    }
                });
            }

            URLSpan[] spans = output.getSpans(0, output.length(), URLSpan.class);
            for (URLSpan span : spans) {
                int start = output.getSpanStart(span);
                int end = output.getSpanEnd(span);
                output.removeSpan(span);

                char last = output.charAt(end - 1);
                if (isPunctuation(last))
                    end--;

                if (quotes.containsKey(String.valueOf(output.charAt(end - 1)))) {
                    char close = output.charAt(end - 1);
                    char open = quotes.get(String.valueOf(output.charAt(end - 1))).charAt(0);
                    int countOpen = 0, countClose = 0;
                    for (int i = start; i < end; i++) {
                        char c = output.charAt(i);
                        if (c == open)
                            countOpen++;
                        else if (c == close)
                            countClose++;
                    }
                    if (countOpen != countClose) {
                        end--;
                    }
                }

                span = new URLSpanNoUnderline(span.getURL());
                output.setSpan(span, start, end, 0);
            }
        }

        for(int i = 0; i < output.length() - 1; i++) {
            char ch = output.charAt(i);
            if(((ch == '←' || ch == '→' || ch == '⇐' || ch == '↔' || ch == '↮') && output.charAt(i+1) != 0xFE0F) || ch == 0x202f) {
                output.setSpan(new TypefaceSpan(sourceSansPro), i, i+1, 0);
            }
        }

        Typeface csFont = IRCCloudApplication.getInstance().getCsFont();
        if(csFont != null) {
            Matcher matcher = Pattern.compile("comic sans", Pattern.CASE_INSENSITIVE).matcher(output);
            while (matcher.find()) {
                output.setSpan(new TypefaceSpan(csFont), matcher.start(), matcher.end(), 0);
            }
        }

        if(Build.VERSION.SDK_INT >= 19 && EmojiCompat.get().getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED)
            return (Spanned)EmojiCompat.get().process(output);
        else
            return output;
    }

    public static void detectLinks(final Spannable output) {
        if(output == null)
            return;

        synchronized (output) {
            try {
                if (tc != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        tc.generateLinks(new TextLinks.Request.Builder(output).build()).apply(output, TextLinks.APPLY_STRATEGY_IGNORE, new Function<TextLinks.TextLink, TextLinks.TextLinkSpan>() {
                            @Override
                            public TextLinks.TextLinkSpan apply(TextLinks.TextLink textLink) {
                                return new TextLinkSpanNoUnderline(textLink);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    ClickableSpan[] spans = output.getSpans(0, output.length(), ClickableSpan.class);
                    for (ClickableSpan span : spans) {
                        int start = output.getSpanStart(span);
                        int end = output.getSpanEnd(span);
                        output.removeSpan(span);

                        char last = output.charAt(end - 1);
                        if (isPunctuation(last))
                            end--;

                        String text = output.toString().substring(start, end);
                        if (text.length() < 7 && text.matches("[0-9]+"))
                            continue;

                        if (quotes.containsKey(String.valueOf(output.charAt(end - 1)))) {
                            char close = output.charAt(end - 1);
                            char open = quotes.get(String.valueOf(output.charAt(end - 1))).charAt(0);
                            int countOpen = 0, countClose = 0;
                            for (int i = start; i < end; i++) {
                                char c = output.charAt(i);
                                if (c == open)
                                    countOpen++;
                                else if (c == close)
                                    countClose++;
                            }
                            if (countOpen != countClose) {
                                end--;
                            }
                        }

                        output.setSpan(span, start, end, 0);
                    }
                }
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }
        }
    }

    public static Typeface sourceSansPro;
    private static Typeface Hack;
    public static class TypefaceSpan extends MetricAffectingSpan {
        private Typeface typeFace;

        public TypefaceSpan(Typeface typeFace) {
            this.typeFace = typeFace;
        }

        @Override
        public void updateDrawState(TextPaint paint) {
            if(typeFace != null) {
                int oldStyle;
                Typeface old = paint.getTypeface();
                if (old == null) {
                    oldStyle = 0;
                } else {
                    oldStyle = old.getStyle();
                }

                int fake = oldStyle & ~typeFace.getStyle();
                if ((fake & Typeface.BOLD) != 0) {
                    paint.setFakeBoldText(true);
                }
                if ((fake & Typeface.ITALIC) != 0) {
                    paint.setTextSkewX(-0.25f);
                }
                paint.setTypeface(typeFace);
            }
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            updateDrawState(paint);
        }
    }

    private static class LargeSpan extends MetricAffectingSpan {
        public LargeSpan() {
        }

        @Override
        public void updateMeasureState(TextPaint textPaint) {
            textPaint.setTextSize(textPaint.getTextSize() * 2);
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            textPaint.setTextSize(textPaint.getTextSize() * 2);
        }
    }

    private static boolean isPunctuation(char c) {
        return (c == '.' || c == '!' || c == '?' || c == ',' || c == ':' || c == ';');
    }

    public static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            boolean keepUnderline = ds.isUnderlineText();
            super.updateDrawState(ds);
            ds.setUnderlineText(keepUnderline);
        }
    }

    @TargetApi(28)
    public static class TextLinkSpanNoUnderline extends TextLinks.TextLinkSpan {
        public TextLinkSpanNoUnderline(TextLinks.TextLink textLink) {
            super(textLink);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            boolean keepUnderline = ds.isUnderlineText();
            super.updateDrawState(ds);
            ds.setUnderlineText(keepUnderline);
        }
    }

    private static String closeTags(boolean bold, boolean underline, boolean italics, boolean strike, String fg, String bg) {
        StringBuilder builder = new StringBuilder();

        if (fg.length() > 0) {
            builder.append("</font>");
        }
        if (bg.length() > 0) {
            builder.append("</_bg").append(bg).append(">");
        }
        if (bold) {
            builder.append("</b>");
        }
        if (underline) {
            builder.append("</u>");
        }
        if (italics) {
            builder.append("</i>");
        }
        if (strike) {
            builder.append("</strike>");
        }

        return builder.toString();
    }

    private static String openTags(boolean bold, boolean underline, boolean italics, boolean strike, String fg, String bg) {
        StringBuilder builder = new StringBuilder();

        if (fg.length() > 0) {
            builder.append("<font color=\"#").append(fg).append("\">");
        }
        if (bg.length() > 0) {
            builder.append("<_bg").append(bg).append(">");
        }
        if (bold) {
            builder.append("<b>");
        }
        if (underline) {
            builder.append("<u>");
        }
        if (italics) {
            builder.append("<i>");
        }
        if (strike) {
            builder.append("<strike>");
        }

        return builder.toString();
    }

    private static class Mention {
        public int position;
        public int length;
        public boolean at_mention;
        public ForegroundColorSpan span;

        public String toString() {
            return "{position=" + position + ", length=" + length + ", at_mention:" + at_mention + "}";
        }
    }

    private static void offset_mention_map(HashMap<String, ArrayList<Mention>> mentions_map, int start, int offset) {
        for(ArrayList<Mention> mentions : mentions_map.values()) {
            for(Mention m : mentions) {
                if(m.position > start)
                    m.position += offset;
            }
        }
    }

    public static String irc_to_html(String msg) {
        return irc_to_html(msg, null, 0, null, 0);
    }

    public static String irc_to_html(String msg, JsonNode mentions, int mention_offset, JsonNode mention_data, int cid) {
        if (msg == null)
            return "";

        HashMap<String, ArrayList<Mention>> mentions_map = new HashMap<>();
        if(mentions != null) {
            Iterator<Map.Entry<String, JsonNode>> i = mentions.fields();
            while(i.hasNext()) {
                Map.Entry<String, JsonNode> entry = i.next();
                ArrayList<Mention> mention_list = new ArrayList<>();
                Iterator<JsonNode> j = entry.getValue().elements();
                while(j.hasNext()) {
                    JsonNode node = j.next();
                    Mention m = new Mention();
                    m.position = node.get(0).asInt() + mention_offset;
                    m.length = node.get(1).asInt();
                    if(m.position >= 0 && m.position + m.length <= msg.length())
                        mention_list.add(m);
                }
                if(mention_list.size() > 0)
                    mentions_map.put(entry.getKey(), mention_list);
            }
        }

        if(mentions_map != null && !mentions_map.isEmpty()) {
            Matcher m = HTML_ENTITY.matcher(msg);
            while (m.find()) {
                if(m.start() >= mention_offset)
                    offset_mention_map(mentions_map, m.start(), m.end() - m.start() - 1);
            }
        }

        int pos = 0;
        boolean bold = false, underline = false, italics = false, monospace = false, strike = false;
        String fg = "", bg = "";
        StringBuilder builder = new StringBuilder(msg);
        builder.append((char)0x0f);
        builder.insert(0, "<irc>");

        if(mentions_map != null && !mentions_map.isEmpty()) {
            offset_mention_map(mentions_map, -1, 5);

            int length = builder.codePointCount(0, builder.length());
            for(int i = 0; i < length; i++) {
                int cp = builder.codePointAt(i);
                int size = cp / 65536;
                if(size > 0) {
                    offset_mention_map(mentions_map, i, size);
                    i += size;
                }
            }

            for (Map.Entry<String,ArrayList<Mention>> entry : mentions_map.entrySet()) {
                for (Mention m : entry.getValue()) {
                    m.at_mention = m.position > 0 && builder.charAt(m.position - 1) == '@';
                    builder.replace(m.position, m.position + m.length, new String(new char[m.length]).replace("\0", "A"));
                    builder.insert(m.position + m.length, "</nick>");
                    builder.insert(m.position, "<nick>");
                    offset_mention_map(mentions_map, m.position, 13);
                    m.position += 6;
                }
            }
        }

        try {
            int old_length = builder.length();
            while (pos < builder.length()) {
                if (mentions_map != null && !mentions_map.isEmpty() && old_length != builder.length()) {
                    offset_mention_map(mentions_map, pos, builder.length() - old_length);
                }
                old_length = builder.length();
                if (builder.charAt(pos) == 0x02) { //Bold
                    builder.deleteCharAt(pos);
                    String html = closeTags(bold, underline, italics, strike, fg, bg);
                    builder.insert(pos, html);
                    pos += html.length();
                    bold = !bold;
                    builder.insert(pos, openTags(bold, underline, italics, strike, fg, bg));
                } else if (builder.charAt(pos) == 0x1d) { //Italics
                    builder.deleteCharAt(pos);
                    String html = closeTags(bold, underline, italics, strike, fg, bg);
                    builder.insert(pos, html);
                    pos += html.length();
                    italics = !italics;
                    builder.insert(pos, openTags(bold, underline, italics, strike, fg, bg));
                } else if (builder.charAt(pos) == 0x1e) { //Strikethrough
                    builder.deleteCharAt(pos);
                    String html = closeTags(bold, underline, italics, strike, fg, bg);
                    builder.insert(pos, html);
                    pos += html.length();
                    strike = !strike;
                    builder.insert(pos, openTags(bold, underline, italics, strike, fg, bg));
                } else if (builder.charAt(pos) == 0x1f) { //Underline
                    builder.deleteCharAt(pos);
                    String html = closeTags(bold, underline, italics, strike, fg, bg);
                    builder.insert(pos, html);
                    pos += html.length();
                    underline = !underline;
                    builder.insert(pos, openTags(bold, underline, italics, strike, fg, bg));
                } else if (builder.charAt(pos) == 0x12 || builder.charAt(pos) == 0x16) { //Reverse
                    builder.deleteCharAt(pos);
                    String html = closeTags(bold, underline, italics, strike, fg, bg);
                    builder.insert(pos, html);
                    pos += html.length();
                    String oldFg = fg;
                    fg = bg;
                    bg = oldFg;

                    if (fg.length() == 0)
                        fg = COLOR_MAP[ColorScheme.getInstance().isDarkTheme ? 1 : 0];

                    if (bg.length() == 0)
                        bg = COLOR_MAP[ColorScheme.getInstance().isDarkTheme ? 0 : 1];
                    builder.insert(pos, openTags(bold, underline, italics, strike, fg, bg));
                } else if (builder.charAt(pos) == 0x11) { //Monospace
                    String html = closeTags(bold, underline, italics, strike, fg, bg);
                    if (monospace) {
                        html += "</pre>";
                        if (fg.equals(Integer.toHexString(ColorScheme.getInstance().codeSpanForegroundColor).substring(2))) {
                            fg = "";
                        }
                        if (bg.equals(Integer.toHexString(ColorScheme.getInstance().codeSpanBackgroundColor).substring(2))) {
                            bg = "";
                        }
                    } else {
                        html += "<pre>";
                        if (fg.length() == 0 && bg.length() == 0) {
                            fg = Integer.toHexString(ColorScheme.getInstance().codeSpanForegroundColor).substring(2);
                            bg = Integer.toHexString(ColorScheme.getInstance().codeSpanBackgroundColor).substring(2);
                        }
                    }
                    html += openTags(bold, underline, italics, strike, fg, bg);
                    monospace = !monospace;
                    builder.deleteCharAt(pos);
                    builder.insert(pos, html);
                } else if (builder.charAt(pos) == 0x0f) { //Formatting clear
                    builder.deleteCharAt(pos);
                    if (monospace)
                        builder.insert(pos, "</pre>");
                    builder.insert(pos, closeTags(bold, underline, italics, strike, fg, bg));
                    bold = underline = italics = monospace = false;
                    fg = bg = "";
                } else if (builder.charAt(pos) == 0x03 || builder.charAt(pos) == 0x04) { //Color
                    boolean rgb = (builder.charAt(pos) == 4);
                    int count = 0;
                    String new_fg = "", new_bg = "";
                    builder.deleteCharAt(pos);
                    if (pos < builder.length()) {
                        while (pos + count < builder.length() && (
                                (builder.charAt(pos + count) >= '0' && builder.charAt(pos + count) <= '9') ||
                                        rgb && ((builder.charAt(pos + count) >= 'a' && builder.charAt(pos + count) <= 'f') ||
                                                (builder.charAt(pos + count) >= 'A' && builder.charAt(pos + count) <= 'F')))) {
                            if ((++count == 2 && !rgb) || count == 6)
                                break;
                        }
                        if (count > 0) {
                            if (count < 3 && !rgb) {
                                try {
                                    int col = Integer.parseInt(builder.substring(pos, pos + count));
                                    if (col > COLOR_MAP.length) {
                                        count--;
                                        col /= 10;
                                    }
                                    if (col == 99)
                                        new_fg = "";
                                    else
                                        new_fg = COLOR_MAP[col];
                                } catch (NumberFormatException e) {
                                    new_fg = builder.substring(pos, pos + count);
                                }
                            } else
                                new_fg = builder.substring(pos, pos + count);
                            builder.delete(pos, pos + count);
                        }
                        if (pos < builder.length() && builder.charAt(pos) == ',') {
                            builder.deleteCharAt(pos);
                            if (new_fg.length() == 0)
                                new_fg = "clear";
                            new_bg = "clear";
                            count = 0;
                            while (pos + count < builder.length() && (
                                    (builder.charAt(pos + count) >= '0' && builder.charAt(pos + count) <= '9') ||
                                            rgb && ((builder.charAt(pos + count) >= 'a' && builder.charAt(pos + count) <= 'f') ||
                                                    (builder.charAt(pos + count) >= 'A' && builder.charAt(pos + count) <= 'F')))) {
                                if ((++count == 2 && !rgb) || count == 6)
                                    break;
                            }
                            if (count > 0) {
                                if (count < 3 && !rgb) {
                                    try {
                                        int col = Integer.parseInt(builder.substring(pos, pos + count));
                                        if (col > COLOR_MAP.length) {
                                            count--;
                                            col /= 10;
                                        }
                                        if (col == 99)
                                            new_bg = "";
                                        else
                                            new_bg = COLOR_MAP[col];
                                    } catch (NumberFormatException e) {
                                        new_bg = builder.substring(pos, pos + count);
                                    }
                                } else
                                    new_bg = builder.substring(pos, pos + count);
                                builder.delete(pos, pos + count);
                            } else {
                                builder.insert(pos, ",");
                            }
                        }
                        String html = "";
                        if (new_fg.length() == 0 && new_bg.length() == 0) {
                            new_fg = "clear";
                            new_bg = "clear";
                        }
                        if (new_fg.length() > 0 && fg.length() > 0) {
                            html += "</font>";
                        }
                        if (new_bg.length() > 0 && bg.length() > 0) {
                            html += "</_bg" + bg + ">";
                        }
                        if (new_bg.length() > 0) {
                            if (new_bg.equals("clear")) {
                                bg = "";
                            } else {
                                bg = "";
                                if (new_bg.length() == 6) {
                                    bg = new_bg;
                                } else if (new_bg.length() == 3) {
                                    bg += new_bg.charAt(0);
                                    bg += new_bg.charAt(0);
                                    bg += new_bg.charAt(1);
                                    bg += new_bg.charAt(1);
                                    bg += new_bg.charAt(2);
                                    bg += new_bg.charAt(2);
                                } else {
                                    bg = "ffffff";
                                }
                                if (bg.length() > 0)
                                    html += "<_bg" + bg + ">";
                            }
                        }
                        if (new_fg.length() > 0) {
                            if (new_fg.equals("clear")) {
                                fg = "";
                            } else {
                                fg = "";
                                if (new_fg.length() == 6) {
                                    fg = new_fg;
                                } else if (new_fg.length() == 3) {
                                    fg += new_fg.charAt(0);
                                    fg += new_fg.charAt(0);
                                    fg += new_fg.charAt(1);
                                    fg += new_fg.charAt(1);
                                    fg += new_fg.charAt(2);
                                    fg += new_fg.charAt(2);
                                } else {
                                    fg = "000000";
                                }
                            }
                            if (ColorScheme.getInstance().theme != null && bg.length() == 0) {
                                if (ColorScheme.getInstance().isDarkTheme && DARK_FG_SUBSTITUTIONS.containsKey(fg))
                                    fg = DARK_FG_SUBSTITUTIONS.get(fg);
                                if (Integer.toHexString(ColorScheme.getInstance().contentBackgroundColor).equalsIgnoreCase("ff" + fg)) {
                                    int red = Integer.parseInt(fg.substring(0, 1), 16);
                                    int blue = Integer.parseInt(fg.substring(2, 3), 16);
                                    int green = Integer.parseInt(fg.substring(4, 5), 16);

                                    red += 0x22;
                                    if (red > 0xFF)
                                        red = 0xFF;
                                    green += 0x22;
                                    if (green > 0xFF)
                                        green = 0xFF;
                                    blue += 0x22;
                                    if (blue > 0xFF)
                                        blue = 0xFF;

                                    fg = String.format("%02x%02x%02x", red, green, blue);
                                }
                            }
                            if (fg.length() > 0)
                                html += "<font color=\"#" + fg + "\">";
                        }
                        builder.insert(pos, html);
                    }
                } else {
                    pos++;
                }
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
            NetworkConnection.printStackTraceToCrashlytics(e);
        }

        builder.append("</irc>");

        if(mentions_map != null && !mentions_map.isEmpty()) {
            for(Map.Entry<String, ArrayList<Mention>> e : mentions_map.entrySet()) {
                for(Mention m : e.getValue()) {
                    String nick = e.getKey();
                    if(m.at_mention) {
                        if (mention_data != null) {
                            if (mention_data.has(nick)) {
                                JsonNode node = mention_data.get(nick);
                                if (node.has("display_name") && !node.get("display_name").isNull() && node.get("display_name").asText().length() > 0)
                                    nick = node.get("display_name").asText();
                            }
                        } else {
                            nick = UsersList.getInstance().getDisplayName(cid, nick);
                        }
                    }
                    if(m.position > 0 && m.position + m.length < builder.length())
                        builder.replace(m.position, m.position + m.length, nick);
                }
            }
        }

        return builder.toString();
    }

    public static String strip_colors(String msg) {
        if (msg == null)
            return "";

        int pos = 0;
        StringBuilder builder = new StringBuilder(msg);

        try {
            while (pos < builder.length()) {
                if (builder.charAt(pos) == 0x03 || builder.charAt(pos) == 0x04) { //Color
                    boolean rgb = (builder.charAt(pos) == 4);
                    int count = 0;
                    String new_fg = "", new_bg = "";
                    builder.deleteCharAt(pos);
                    if (pos < builder.length()) {
                        while (pos + count < builder.length() && (
                                (builder.charAt(pos + count) >= '0' && builder.charAt(pos + count) <= '9') ||
                                        rgb && ((builder.charAt(pos + count) >= 'a' && builder.charAt(pos + count) <= 'f') ||
                                                (builder.charAt(pos + count) >= 'A' && builder.charAt(pos + count) <= 'F')))) {
                            if ((++count == 2 && !rgb) || count == 6)
                                break;
                        }
                        if (count > 0) {
                            if (count < 3 && !rgb) {
                                try {
                                    int col = Integer.parseInt(builder.substring(pos, pos + count));
                                    if (col > COLOR_MAP.length) {
                                        count--;
                                    }
                                } catch (NumberFormatException e) {
                                }
                            }
                            builder.delete(pos, pos + count);
                        }
                        if (pos < builder.length() && builder.charAt(pos) == ',') {
                            builder.deleteCharAt(pos);
                            count = 0;
                            while (pos + count < builder.length() && (
                                    (builder.charAt(pos + count) >= '0' && builder.charAt(pos + count) <= '9') ||
                                            rgb && ((builder.charAt(pos + count) >= 'a' && builder.charAt(pos + count) <= 'f') ||
                                                    (builder.charAt(pos + count) >= 'A' && builder.charAt(pos + count) <= 'F')))) {
                                if ((++count == 2 && !rgb) || count == 6)
                                    break;
                            }
                            if (count > 0) {
                                if (count < 3 && !rgb) {
                                    try {
                                        int col = Integer.parseInt(builder.substring(pos, pos + count));
                                        if (col > COLOR_MAP.length) {
                                            count--;
                                        }
                                    } catch (NumberFormatException e) {
                                    }
                                }
                                builder.delete(pos, pos + count);
                            } else {
                                builder.insert(pos, ",");
                            }
                        }
                    }
                } else {
                    pos++;
                }
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
            NetworkConnection.printStackTraceToCrashlytics(e);
        }

        return builder.toString();
    }
}
