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

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.text.emoji.EmojiCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.util.Patterns;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric.sdk.android.services.common.Crash;

public class ColorFormatter {
    //From: https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
            "(?:"
                    + "(?:aaa|aarp|abarth|abb|abbott|abbvie|abc|able|abogado|abudhabi|academy|accenture|accountant|accountants|aco|active|actor|adac|ads|adult|aeg|aero|aetna|afamilycompany|afl|africa|agakhan|agency|aig|aigo|airbus|airforce|airtel|akdn|alfaromeo|alibaba|alipay|allfinanz|allstate|ally|alsace|alstom|americanexpress|americanfamily|amex|amfam|amica|amsterdam|analytics|android|anquan|anz|aol|apartments|app|apple|aquarelle|arab|aramco|archi|army|arpa|art|arte|asda|asia|associates|athleta|attorney|auction|audi|audible|audio|auspost|author|auto|autos|avianca|aws|axa|azure|a[cdefgilmoqrstuwxz])"
                    + "|(?:baby|baidu|banamex|bananarepublic|band|bank|bar|barcelona|barclaycard|barclays|barefoot|bargains|baseball|basketball|bauhaus|bayern|bbc|bbt|bbva|bcg|bcn|beats|beauty|beer|bentley|berlin|best|bestbuy|bet|bharti|bible|bid|bike|bing|bingo|bio|biz|black|blackfriday|blanco|blockbuster|blog|bloomberg|blue|bms|bmw|bnl|bnpparibas|boats|boehringer|bofa|bom|bond|boo|book|booking|boots|bosch|bostik|boston|bot|boutique|box|bradesco|bridgestone|broadway|broker|brother|brussels|budapest|bugatti|build|builders|business|buy|buzz|bzh|b[abdefghijmnorstvwyz])"
                    + "|(?:cab|cafe|cal|call|calvinklein|cam|camera|camp|cancerresearch|canon|capetown|capital|capitalone|car|caravan|cards|care|career|careers|cars|cartier|casa|case|caseih|cash|casino|cat|catering|catholic|cba|cbn|cbre|cbs|ceb|center|ceo|cern|cfa|cfd|chanel|channel|chase|chat|cheap|chintai|christmas|chrome|chrysler|church|cipriani|circle|cisco|citadel|citi|citic|city|cityeats|claims|cleaning|click|clinic|clinique|clothing|cloud|club|clubmed|coach|codes|coffee|college|cologne|com|comcast|commbank|community|company|compare|computer|comsec|condos|construction|consulting|contact|contractors|cooking|cookingchannel|cool|coop|corsica|country|coupon|coupons|courses|credit|creditcard|creditunion|cricket|crown|crs|cruise|cruises|csc|cuisinella|cymru|cyou|c[acdfghiklmnoruvwxyz])"
                    + "|(?:dabur|dad|dance|data|date|dating|datsun|day|dclk|dds|deal|dealer|deals|degree|delivery|dell|deloitte|delta|democrat|dental|dentist|desi|design|dev|dhl|diamonds|diet|digital|direct|directory|discount|discover|dish|diy|dnp|docs|doctor|dodge|dog|doha|domains|dot|download|drive|dtv|dubai|duck|dunlop|duns|dupont|durban|dvag|dvr|d[ejkmoz])"
                    + "|(?:earth|eat|eco|edeka|edu|education|email|emerck|energy|engineer|engineering|enterprises|epost|epson|equipment|ericsson|erni|esq|estate|esurance|etisalat|eurovision|eus|events|everbank|exchange|expert|exposed|express|extraspace|e[cegrstu])"
                    + "|(?:fage|fail|fairwinds|faith|family|fan|fans|farm|farmers|fashion|fast|fedex|feedback|ferrari|ferrero|fiat|fidelity|fido|film|final|finance|financial|fire|firestone|firmdale|fish|fishing|fit|fitness|flickr|flights|flir|florist|flowers|fly|foo|food|foodnetwork|football|ford|forex|forsale|forum|foundation|fox|free|fresenius|frl|frogans|frontdoor|frontier|ftr|fujitsu|fujixerox|fun|fund|furniture|futbol|fyi|f[ijkmor])"
                    + "|(?:gal|gallery|gallo|gallup|game|games|gap|garden|gbiz|gdn|gea|gent|genting|george|ggee|gift|gifts|gives|giving|glade|glass|gle|global|globo|gmail|gmbh|gmo|gmx|godaddy|gold|goldpoint|golf|goo|goodhands|goodyear|goog|google|gop|got|gov|grainger|graphics|gratis|green|gripe|grocery|group|guardian|gucci|guge|guide|guitars|guru|g[abdefghilmnpqrstuwy])"
                    + "|(?:hair|hamburg|hangout|haus|hbo|hdfc|hdfcbank|health|healthcare|help|helsinki|here|hermes|hgtv|hiphop|hisamitsu|hitachi|hiv|hkt|hockey|holdings|holiday|homedepot|homegoods|homes|homesense|honda|honeywell|horse|hospital|host|hosting|hot|hoteles|hotels|hotmail|house|how|hsbc|hughes|hyatt|hyundai|h[kmnrtu])"
                    + "|(?:ibm|icbc|ice|icu|ieee|ifm|ikano|imamat|imdb|immo|immobilien|industries|infiniti|info|ing|ink|institute|insurance|insure|int|intel|international|intuit|investments|ipiranga|irish|iselect|ismaili|ist|istanbul|itau|itv|iveco|iwc|i[delmnoqrst])"
                    + "|(?:jaguar|java|jcb|jcp|jeep|jetzt|jewelry|jio|jlc|jll|jmp|jnj|jobs|joburg|jot|joy|jpmorgan|jprs|juegos|juniper|j[emop])"
                    + "|(?:kaufen|kddi|kerryhotels|kerrylogistics|kerryproperties|kfh|kia|kim|kinder|kindle|kitchen|kiwi|koeln|komatsu|kosher|kpmg|kpn|krd|kred|kuokgroup|kyoto|k[eghimnprwyz])"
                    + "|(?:lacaixa|ladbrokes|lamborghini|lamer|lancaster|lancia|lancome|land|landrover|lanxess|lasalle|lat|latino|latrobe|law|lawyer|lds|lease|leclerc|lefrak|legal|lego|lexus|lgbt|liaison|lidl|life|lifeinsurance|lifestyle|lighting|like|lilly|limited|limo|lincoln|linde|link|lipsy|live|living|lixil|loan|loans|locker|locus|loft|lol|london|lotte|lotto|love|lpl|lplfinancial|ltd|ltda|lundbeck|lupin|luxe|luxury|l[abcikrstuvy])"
                    + "|(?:macys|madrid|maif|maison|makeup|man|management|mango|map|market|marketing|markets|marriott|marshalls|maserati|mattel|mba|mckinsey|med|media|meet|melbourne|meme|memorial|men|menu|meo|merckmsd|metlife|miami|microsoft|mil|mini|mint|mit|mitsubishi|mlb|mls|mma|mobi|mobile|mobily|moda|moe|moi|mom|monash|money|monster|mopar|mormon|mortgage|moscow|moto|motorcycles|mov|movie|movistar|msd|mtn|mtr|museum|mutual|m[acdeghklmnopqrstuvwxyz])"
                    + "|(?:nab|nadex|nagoya|name|nationwide|natura|navy|nba|nec|net|netbank|netflix|network|neustar|new|newholland|news|next|nextdirect|nexus|nfl|ngo|nhk|nico|nike|nikon|ninja|nissan|nissay|nokia|northwesternmutual|norton|now|nowruz|nowtv|nra|nrw|ntt|nyc|n[acefgilopruz])"
                    + "|(?:obi|observer|off|office|okinawa|olayan|olayangroup|oldnavy|ollo|omega|one|ong|onl|online|onyourside|ooo|open|oracle|orange|org|organic|origins|osaka|otsuka|ott|ovh|om)"
                    + "|(?:page|panasonic|panerai|paris|pars|partners|parts|party|passagens|pay|pccw|pet|pfizer|pharmacy|phd|philips|phone|photo|photography|photos|physio|piaget|pics|pictet|pictures|pid|pin|ping|pink|pioneer|pizza|place|play|playstation|plumbing|plus|pnc|pohl|poker|politie|porn|post|pramerica|praxi|press|prime|pro|prod|productions|prof|progressive|promo|properties|property|protection|pru|prudential|pub|pwc|p[aefghklmnrstwy])"
                    + "|(?:qpon|quebec|quest|qvc|qa)"
                    + "|(?:racing|radio|raid|read|realestate|realtor|realty|recipes|red|redstone|redumbrella|rehab|reise|reisen|reit|reliance|ren|rent|rentals|repair|report|republican|rest|restaurant|review|reviews|rexroth|rich|richardli|ricoh|rightathome|ril|rio|rip|rmit|rocher|rocks|rodeo|rogers|room|rsvp|rugby|ruhr|run|rwe|ryukyu|r[eosuw])"
                    + "|(?:saarland|safe|safety|sakura|sale|salon|samsclub|samsung|sandvik|sandvikcoromant|sanofi|sap|sapo|sarl|sas|save|saxo|sbi|sbs|sca|scb|schaeffler|schmidt|scholarships|school|schule|schwarz|science|scjohnson|scor|scot|search|seat|secure|security|seek|select|sener|services|ses|seven|sew|sex|sexy|sfr|shangrila|sharp|shaw|shell|shia|shiksha|shoes|shop|shopping|shouji|show|showtime|shriram|silk|sina|singles|site|ski|skin|sky|skype|sling|smart|smile|sncf|soccer|social|softbank|software|sohu|solar|solutions|song|sony|soy|space|spiegel|spot|spreadbetting|srl|srt|stada|staples|star|starhub|statebank|statefarm|statoil|stc|stcgroup|stockholm|storage|store|stream|studio|study|style|sucks|supplies|supply|support|surf|surgery|suzuki|swatch|swiftcover|swiss|sydney|symantec|systems|s[abcdeghijklmnortuvxyz])"
                    + "|(?:tab|taipei|talk|taobao|target|tatamotors|tatar|tattoo|tax|taxi|tci|tdk|team|tech|technology|tel|telecity|telefonica|temasek|tennis|teva|thd|theater|theatre|tiaa|tickets|tienda|tiffany|tips|tires|tirol|tjmaxx|tjx|tkmaxx|tmall|today|tokyo|tools|top|toray|toshiba|total|tours|town|toyota|toys|trade|trading|training|travel|travelchannel|travelers|travelersinsurance|trust|trv|tube|tui|tunes|tushu|tvs|t[cdfghjklmnortvwz])"
                    + "|(?:ubank|ubs|uconnect|unicom|university|uno|uol|ups|u[agksyz])"
                    + "|(?:vacations|vana|vanguard|vegas|ventures|verisign|versicherung|vet|viajes|video|vig|viking|villas|vin|vip|virgin|visa|vision|vista|vistaprint|viva|vivo|vlaanderen|vodka|volkswagen|volvo|vote|voting|voto|voyage|vuelos|v[aceginu])"
                    + "|(?:wales|walmart|walter|wang|wanggou|warman|watch|watches|weather|weatherchannel|webcam|weber|website|wed|wedding|weibo|weir|whoswho|wien|wiki|williamhill|win|windows|wine|winners|wme|wolterskluwer|woodside|work|works|world|wow|wtc|wtf|w[fs])"
                    + "|(?:\\u03b5\\u03bb|\\u0431\\u0433|\\u0431\\u0435\\u043b|\\u0434\\u0435\\u0442\\u0438|\\u0435\\u044e|\\u043a\\u0430\\u0442\\u043e\\u043b\\u0438\\u043a|\\u043a\\u043e\\u043c|\\u043c\\u043a\\u0434|\\u043c\\u043e\\u043d|\\u043c\\u043e\\u0441\\u043a\\u0432\\u0430|\\u043e\\u043d\\u043b\\u0430\\u0439\\u043d|\\u043e\\u0440\\u0433|\\u0440\\u0443\\u0441|\\u0440\\u0444|\\u0441\\u0430\\u0439\\u0442|\\u0441\\u0440\\u0431|\\u0443\\u043a\\u0440|\\u049b\\u0430\\u0437|\\u0570\\u0561\\u0575|\\u05e7\\u05d5\\u05dd|\\u0627\\u0628\\u0648\\u0638\\u0628\\u064a|\\u0627\\u062a\\u0635\\u0627\\u0644\\u0627\\u062a|\\u0627\\u0631\\u0627\\u0645\\u0643\\u0648|\\u0627\\u0644\\u0627\\u0631\\u062f\\u0646|\\u0627\\u0644\\u062c\\u0632\\u0627\\u0626\\u0631|\\u0627\\u0644\\u0633\\u0639\\u0648\\u062f\\u064a\\u0629|\\u0627\\u0644\\u0639\\u0644\\u064a\\u0627\\u0646|\\u0627\\u0644\\u0645\\u063a\\u0631\\u0628|\\u0627\\u0645\\u0627\\u0631\\u0627\\u062a|\\u0627\\u06cc\\u0631\\u0627\\u0646|\\u0628\\u0627\\u0631\\u062a|\\u0628\\u0627\\u0632\\u0627\\u0631|\\u0628\\u064a\\u062a\\u0643|\\u0628\\u06be\\u0627\\u0631\\u062a|\\u062a\\u0648\\u0646\\u0633|\\u0633\\u0648\\u062f\\u0627\\u0646|\\u0633\\u0648\\u0631\\u064a\\u0629|\\u0634\\u0628\\u0643\\u0629|\\u0639\\u0631\\u0627\\u0642|\\u0639\\u0631\\u0628|\\u0639\\u0645\\u0627\\u0646|\\u0641\\u0644\\u0633\\u0637\\u064a\\u0646|\\u0642\\u0637\\u0631|\\u0643\\u0627\\u062b\\u0648\\u0644\\u064a\\u0643|\\u0643\\u0648\\u0645|\\u0645\\u0635\\u0631|\\u0645\\u0644\\u064a\\u0633\\u064a\\u0627|\\u0645\\u0648\\u0628\\u0627\\u064a\\u0644\\u064a|\\u0645\\u0648\\u0642\\u0639|\\u0647\\u0645\\u0631\\u0627\\u0647|\\u067e\\u0627\\u06a9\\u0633\\u062a\\u0627\\u0646|\\u0680\\u0627\\u0631\\u062a|\\u0915\\u0949\\u092e|\\u0928\\u0947\\u091f|\\u092d\\u093e\\u0930\\u0924|\\u092d\\u093e\\u0930\\u0924\\u092e\\u094d|\\u092d\\u093e\\u0930\\u094b\\u0924|\\u0938\\u0902\\u0917\\u0920\\u0928|\\u09ac\\u09be\\u0982\\u09b2\\u09be|\\u09ad\\u09be\\u09b0\\u09a4|\\u09ad\\u09be\\u09f0\\u09a4|\\u0a2d\\u0a3e\\u0a30\\u0a24|\\u0aad\\u0abe\\u0ab0\\u0aa4|\\u0b2d\\u0b3e\\u0b30\\u0b24|\\u0b87\\u0ba8\\u0bcd\\u0ba4\\u0bbf\\u0baf\\u0bbe|\\u0b87\\u0bb2\\u0b99\\u0bcd\\u0b95\\u0bc8|\\u0b9a\\u0bbf\\u0b99\\u0bcd\\u0b95\\u0baa\\u0bcd\\u0baa\\u0bc2\\u0bb0\\u0bcd|\\u0c2d\\u0c3e\\u0c30\\u0c24\\u0c4d|\\u0cad\\u0cbe\\u0cb0\\u0ca4|\\u0d2d\\u0d3e\\u0d30\\u0d24\\u0d02|\\u0dbd\\u0d82\\u0d9a\\u0dcf|\\u0e04\\u0e2d\\u0e21|\\u0e44\\u0e17\\u0e22|\\u10d2\\u10d4|\\u307f\\u3093\\u306a|\\u30af\\u30e9\\u30a6\\u30c9|\\u30b0\\u30fc\\u30b0\\u30eb|\\u30b3\\u30e0|\\u30b9\\u30c8\\u30a2|\\u30bb\\u30fc\\u30eb|\\u30d5\\u30a1\\u30c3\\u30b7\\u30e7\\u30f3|\\u30dd\\u30a4\\u30f3\\u30c8|\\u4e16\\u754c|\\u4e2d\\u4fe1|\\u4e2d\\u56fd|\\u4e2d\\u570b|\\u4e2d\\u6587\\u7f51|\\u4f01\\u4e1a|\\u4f5b\\u5c71|\\u4fe1\\u606f|\\u5065\\u5eb7|\\u516b\\u5366|\\u516c\\u53f8|\\u516c\\u76ca|\\u53f0\\u6e7e|\\u53f0\\u7063|\\u5546\\u57ce|\\u5546\\u5e97|\\u5546\\u6807|\\u5609\\u91cc|\\u5609\\u91cc\\u5927\\u9152\\u5e97|\\u5728\\u7ebf|\\u5927\\u4f17\\u6c7d\\u8f66|\\u5927\\u62ff|\\u5929\\u4e3b\\u6559|\\u5a31\\u4e50|\\u5bb6\\u96fb|\\u5de5\\u884c|\\u5e7f\\u4e1c|\\u5fae\\u535a|\\u6148\\u5584|\\u6211\\u7231\\u4f60|\\u624b\\u673a|\\u624b\\u8868|\\u653f\\u52a1|\\u653f\\u5e9c|\\u65b0\\u52a0\\u5761|\\u65b0\\u95fb|\\u65f6\\u5c1a|\\u66f8\\u7c4d|\\u673a\\u6784|\\u6de1\\u9a6c\\u9521|\\u6e38\\u620f|\\u6fb3\\u9580|\\u70b9\\u770b|\\u73e0\\u5b9d|\\u79fb\\u52a8|\\u7ec4\\u7ec7\\u673a\\u6784|\\u7f51\\u5740|\\u7f51\\u5e97|\\u7f51\\u7ad9|\\u7f51\\u7edc|\\u8054\\u901a|\\u8bfa\\u57fa\\u4e9a|\\u8c37\\u6b4c|\\u8d2d\\u7269|\\u901a\\u8ca9|\\u96c6\\u56e2|\\u96fb\\u8a0a\\u76c8\\u79d1|\\u98de\\u5229\\u6d66|\\u98df\\u54c1|\\u9910\\u5385|\\u9999\\u683c\\u91cc\\u62c9|\\u9999\\u6e2f|\\ub2f7\\ub137|\\ub2f7\\ucef4|\\uc0bc\\uc131|\\ud55c\\uad6d|verm\\xf6gensberater|verm\\xf6gensberatung|xbox|xerox|xfinity|xihuan|xin|xn\\-\\-11b4c3d|xn\\-\\-1ck2e1b|xn\\-\\-1qqw23a|xn\\-\\-2scrj9c|xn\\-\\-30rr7y|xn\\-\\-3bst00m|xn\\-\\-3ds443g|xn\\-\\-3e0b707e|xn\\-\\-3hcrj9c|xn\\-\\-3oq18vl8pn36a|xn\\-\\-3pxu8k|xn\\-\\-42c2d9a|xn\\-\\-45br5cyl|xn\\-\\-45brj9c|xn\\-\\-45q11c|xn\\-\\-4gbrim|xn\\-\\-54b7fta0cc|xn\\-\\-55qw42g|xn\\-\\-55qx5d|xn\\-\\-5su34j936bgsg|xn\\-\\-5tzm5g|xn\\-\\-6frz82g|xn\\-\\-6qq986b3xl|xn\\-\\-80adxhks|xn\\-\\-80ao21a|xn\\-\\-80aqecdr1a|xn\\-\\-80asehdb|xn\\-\\-80aswg|xn\\-\\-8y0a063a|xn\\-\\-90a3ac|xn\\-\\-90ae|xn\\-\\-90ais|xn\\-\\-9dbq2a|xn\\-\\-9et52u|xn\\-\\-9krt00a|xn\\-\\-b4w605ferd|xn\\-\\-bck1b9a5dre4c|xn\\-\\-c1avg|xn\\-\\-c2br7g|xn\\-\\-cck2b3b|xn\\-\\-cg4bki|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-czr694b|xn\\-\\-czrs0t|xn\\-\\-czru2d|xn\\-\\-d1acj3b|xn\\-\\-d1alf|xn\\-\\-e1a4c|xn\\-\\-eckvdtc9d|xn\\-\\-efvy88h|xn\\-\\-estv75g|xn\\-\\-fct429k|xn\\-\\-fhbei|xn\\-\\-fiq228c5hs|xn\\-\\-fiq64b|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fjq720a|xn\\-\\-flw351e|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-fzys8d69uvgm|xn\\-\\-g2xx48c|xn\\-\\-gckr3f0f|xn\\-\\-gecrj9c|xn\\-\\-gk3at1e|xn\\-\\-h2breg3eve|xn\\-\\-h2brj9c|xn\\-\\-h2brj9c8c|xn\\-\\-hxt814e|xn\\-\\-i1b6b1a6a2e|xn\\-\\-imr513n|xn\\-\\-io0a7i|xn\\-\\-j1aef|xn\\-\\-j1amh|xn\\-\\-j6w193g|xn\\-\\-jlq61u9w7b|xn\\-\\-jvr189m|xn\\-\\-kcrx77d1x4a|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-kpu716f|xn\\-\\-kput3i|xn\\-\\-l1acc|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgb9awbf|xn\\-\\-mgba3a3ejt|xn\\-\\-mgba3a4f16a|xn\\-\\-mgba7c0bbn0a|xn\\-\\-mgbaakc7dvf|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbab2bd|xn\\-\\-mgbai9azgqp6j|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbb9fbpob|xn\\-\\-mgbbh1a|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgbca7dzdo|xn\\-\\-mgberp4a5d4ar|xn\\-\\-mgbgu82a|xn\\-\\-mgbi4ecexp|xn\\-\\-mgbpl2fh|xn\\-\\-mgbt3dhd|xn\\-\\-mgbtx2b|xn\\-\\-mgbx4cd0ab|xn\\-\\-mix891f|xn\\-\\-mk1bu44c|xn\\-\\-mxtq1m|xn\\-\\-ngbc5azd|xn\\-\\-ngbe9e0a|xn\\-\\-ngbrx|xn\\-\\-node|xn\\-\\-nqv7f|xn\\-\\-nqv7fs00ema|xn\\-\\-nyqy26a|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1acf|xn\\-\\-p1ai|xn\\-\\-pbt977c|xn\\-\\-pgbs0dh|xn\\-\\-pssy2u|xn\\-\\-q9jyb4c|xn\\-\\-qcka1pmc|xn\\-\\-qxam|xn\\-\\-rhqv96g|xn\\-\\-rovu88b|xn\\-\\-rvc1e0am3e|xn\\-\\-s9brj9c|xn\\-\\-ses554g|xn\\-\\-t60b56a|xn\\-\\-tckwe|xn\\-\\-tiq49xqyj|xn\\-\\-unup4y|xn\\-\\-vermgensberater\\-ctb|xn\\-\\-vermgensberatung\\-pwb|xn\\-\\-vhquv|xn\\-\\-vuq861b|xn\\-\\-w4r85el8fhu5dnra|xn\\-\\-w4rs40l|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xhq521b|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-y9a3aq|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zfr164b|xperia|xxx|xyz)"
                    + "|(?:yachts|yahoo|yamaxun|yandex|yodobashi|yoga|yokohama|you|youtube|yun|y[et])"
                    + "|(?:zappos|zara|zero|zip|zippo|zone|zuerich|z[amw])))";

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
        put("umbrella_with_rain_drops", "\u2614");
        put("coffee", "\u2615");
        put("aries", "\u2648");
        put("taurus", "\u2649");
        put("sagittarius", "\u2650");
        put("capricorn", "\u2651");
        put("aquarius", "\u2652");
        put("pisces", "\u2653");
        put("anchor", "\u2693");
        put("white_check_mark", "\u2705");
        put("sparkles", "\u2728");
        put("question", "\u2753");
        put("grey_question", "\u2754");
        put("grey_exclamation", "\u2755");
        put("exclamation", "\u2757");
        put("heavy_exclamation_mark", "\u2757");
        put("heavy_plus_sign", "\u2795");
        put("heavy_minus_sign", "\u2796");
        put("heavy_division_sign", "\u2797");
        put("hash", "\u0023\uFE0F\u20E3");
        put("keycap_star", "\u002A\uFE0F\u20E3");
        put("zero", "\u0030\uFE0F\u20E3");
        put("one", "\u0031\uFE0F\u20E3");
        put("two", "\u0032\uFE0F\u20E3");
        put("three", "\u0033\uFE0F\u20E3");
        put("four", "\u0034\uFE0F\u20E3");
        put("five", "\u0035\uFE0F\u20E3");
        put("six", "\u0036\uFE0F\u20E3");
        put("seven", "\u0037\uFE0F\u20E3");
        put("eight", "\u0038\uFE0F\u20E3");
        put("nine", "\u0039\uFE0F\u20E3");
        put("copyright", "\u00A9\uFE0F");
        put("registered", "\u00AE\uFE0F");
        put("mahjong", "\uD83C\uDC04");
        put("black_joker", "\uD83C\uDCCF");
        put("a", "\uD83C\uDD70\uFE0F");
        put("b", "\uD83C\uDD71\uFE0F");
        put("o2", "\uD83C\uDD7E\uFE0F");
        put("parking", "\uD83C\uDD7F\uFE0F");
        put("ab", "\uD83C\uDD8E");
        put("cl", "\uD83C\uDD91");
        put("cool", "\uD83C\uDD92");
        put("free", "\uD83C\uDD93");
        put("id", "\uD83C\uDD94");
        put("new", "\uD83C\uDD95");
        put("ng", "\uD83C\uDD96");
        put("ok", "\uD83C\uDD97");
        put("sos", "\uD83C\uDD98");
        put("up", "\uD83C\uDD99");
        put("vs", "\uD83C\uDD9A");
        put("flag-ac", "\uD83C\uDDE6\uD83C\uDDE8");
        put("flag-ad", "\uD83C\uDDE6\uD83C\uDDE9");
        put("flag-ae", "\uD83C\uDDE6\uD83C\uDDEA");
        put("flag-af", "\uD83C\uDDE6\uD83C\uDDEB");
        put("flag-ag", "\uD83C\uDDE6\uD83C\uDDEC");
        put("flag-ai", "\uD83C\uDDE6\uD83C\uDDEE");
        put("flag-al", "\uD83C\uDDE6\uD83C\uDDF1");
        put("flag-am", "\uD83C\uDDE6\uD83C\uDDF2");
        put("flag-ao", "\uD83C\uDDE6\uD83C\uDDF4");
        put("flag-aq", "\uD83C\uDDE6\uD83C\uDDF6");
        put("flag-ar", "\uD83C\uDDE6\uD83C\uDDF7");
        put("flag-as", "\uD83C\uDDE6\uD83C\uDDF8");
        put("flag-at", "\uD83C\uDDE6\uD83C\uDDF9");
        put("flag-au", "\uD83C\uDDE6\uD83C\uDDFA");
        put("flag-aw", "\uD83C\uDDE6\uD83C\uDDFC");
        put("flag-ax", "\uD83C\uDDE6\uD83C\uDDFD");
        put("flag-az", "\uD83C\uDDE6\uD83C\uDDFF");
        put("flag-ba", "\uD83C\uDDE7\uD83C\uDDE6");
        put("flag-bb", "\uD83C\uDDE7\uD83C\uDDE7");
        put("flag-bd", "\uD83C\uDDE7\uD83C\uDDE9");
        put("flag-be", "\uD83C\uDDE7\uD83C\uDDEA");
        put("flag-bf", "\uD83C\uDDE7\uD83C\uDDEB");
        put("flag-bg", "\uD83C\uDDE7\uD83C\uDDEC");
        put("flag-bh", "\uD83C\uDDE7\uD83C\uDDED");
        put("flag-bi", "\uD83C\uDDE7\uD83C\uDDEE");
        put("flag-bj", "\uD83C\uDDE7\uD83C\uDDEF");
        put("flag-bl", "\uD83C\uDDE7\uD83C\uDDF1");
        put("flag-bm", "\uD83C\uDDE7\uD83C\uDDF2");
        put("flag-bn", "\uD83C\uDDE7\uD83C\uDDF3");
        put("flag-bo", "\uD83C\uDDE7\uD83C\uDDF4");
        put("flag-bq", "\uD83C\uDDE7\uD83C\uDDF6");
        put("flag-br", "\uD83C\uDDE7\uD83C\uDDF7");
        put("flag-bs", "\uD83C\uDDE7\uD83C\uDDF8");
        put("flag-bt", "\uD83C\uDDE7\uD83C\uDDF9");
        put("flag-bv", "\uD83C\uDDE7\uD83C\uDDFB");
        put("flag-bw", "\uD83C\uDDE7\uD83C\uDDFC");
        put("flag-by", "\uD83C\uDDE7\uD83C\uDDFE");
        put("flag-bz", "\uD83C\uDDE7\uD83C\uDDFF");
        put("flag-ca", "\uD83C\uDDE8\uD83C\uDDE6");
        put("flag-cc", "\uD83C\uDDE8\uD83C\uDDE8");
        put("flag-cd", "\uD83C\uDDE8\uD83C\uDDE9");
        put("flag-cf", "\uD83C\uDDE8\uD83C\uDDEB");
        put("flag-cg", "\uD83C\uDDE8\uD83C\uDDEC");
        put("flag-ch", "\uD83C\uDDE8\uD83C\uDDED");
        put("flag-ci", "\uD83C\uDDE8\uD83C\uDDEE");
        put("flag-ck", "\uD83C\uDDE8\uD83C\uDDF0");
        put("flag-cl", "\uD83C\uDDE8\uD83C\uDDF1");
        put("flag-cm", "\uD83C\uDDE8\uD83C\uDDF2");
        put("cn", "\uD83C\uDDE8\uD83C\uDDF3");
        put("flag-cn", "\uD83C\uDDE8\uD83C\uDDF3");
        put("flag-co", "\uD83C\uDDE8\uD83C\uDDF4");
        put("flag-cp", "\uD83C\uDDE8\uD83C\uDDF5");
        put("flag-cr", "\uD83C\uDDE8\uD83C\uDDF7");
        put("flag-cu", "\uD83C\uDDE8\uD83C\uDDFA");
        put("flag-cv", "\uD83C\uDDE8\uD83C\uDDFB");
        put("flag-cw", "\uD83C\uDDE8\uD83C\uDDFC");
        put("flag-cx", "\uD83C\uDDE8\uD83C\uDDFD");
        put("flag-cy", "\uD83C\uDDE8\uD83C\uDDFE");
        put("flag-cz", "\uD83C\uDDE8\uD83C\uDDFF");
        put("de", "\uD83C\uDDE9\uD83C\uDDEA");
        put("flag-de", "\uD83C\uDDE9\uD83C\uDDEA");
        put("flag-dg", "\uD83C\uDDE9\uD83C\uDDEC");
        put("flag-dj", "\uD83C\uDDE9\uD83C\uDDEF");
        put("flag-dk", "\uD83C\uDDE9\uD83C\uDDF0");
        put("flag-dm", "\uD83C\uDDE9\uD83C\uDDF2");
        put("flag-do", "\uD83C\uDDE9\uD83C\uDDF4");
        put("flag-dz", "\uD83C\uDDE9\uD83C\uDDFF");
        put("flag-ea", "\uD83C\uDDEA\uD83C\uDDE6");
        put("flag-ec", "\uD83C\uDDEA\uD83C\uDDE8");
        put("flag-ee", "\uD83C\uDDEA\uD83C\uDDEA");
        put("flag-eg", "\uD83C\uDDEA\uD83C\uDDEC");
        put("flag-eh", "\uD83C\uDDEA\uD83C\uDDED");
        put("flag-er", "\uD83C\uDDEA\uD83C\uDDF7");
        put("es", "\uD83C\uDDEA\uD83C\uDDF8");
        put("flag-es", "\uD83C\uDDEA\uD83C\uDDF8");
        put("flag-et", "\uD83C\uDDEA\uD83C\uDDF9");
        put("flag-eu", "\uD83C\uDDEA\uD83C\uDDFA");
        put("flag-fi", "\uD83C\uDDEB\uD83C\uDDEE");
        put("flag-fj", "\uD83C\uDDEB\uD83C\uDDEF");
        put("flag-fk", "\uD83C\uDDEB\uD83C\uDDF0");
        put("flag-fm", "\uD83C\uDDEB\uD83C\uDDF2");
        put("flag-fo", "\uD83C\uDDEB\uD83C\uDDF4");
        put("fr", "\uD83C\uDDEB\uD83C\uDDF7");
        put("flag-fr", "\uD83C\uDDEB\uD83C\uDDF7");
        put("flag-ga", "\uD83C\uDDEC\uD83C\uDDE6");
        put("gb", "\uD83C\uDDEC\uD83C\uDDE7");
        put("uk", "\uD83C\uDDEC\uD83C\uDDE7");
        put("flag-gb", "\uD83C\uDDEC\uD83C\uDDE7");
        put("flag-gd", "\uD83C\uDDEC\uD83C\uDDE9");
        put("flag-ge", "\uD83C\uDDEC\uD83C\uDDEA");
        put("flag-gf", "\uD83C\uDDEC\uD83C\uDDEB");
        put("flag-gg", "\uD83C\uDDEC\uD83C\uDDEC");
        put("flag-gh", "\uD83C\uDDEC\uD83C\uDDED");
        put("flag-gi", "\uD83C\uDDEC\uD83C\uDDEE");
        put("flag-gl", "\uD83C\uDDEC\uD83C\uDDF1");
        put("flag-gm", "\uD83C\uDDEC\uD83C\uDDF2");
        put("flag-gn", "\uD83C\uDDEC\uD83C\uDDF3");
        put("flag-gp", "\uD83C\uDDEC\uD83C\uDDF5");
        put("flag-gq", "\uD83C\uDDEC\uD83C\uDDF6");
        put("flag-gr", "\uD83C\uDDEC\uD83C\uDDF7");
        put("flag-gs", "\uD83C\uDDEC\uD83C\uDDF8");
        put("flag-gt", "\uD83C\uDDEC\uD83C\uDDF9");
        put("flag-gu", "\uD83C\uDDEC\uD83C\uDDFA");
        put("flag-gw", "\uD83C\uDDEC\uD83C\uDDFC");
        put("flag-gy", "\uD83C\uDDEC\uD83C\uDDFE");
        put("flag-hk", "\uD83C\uDDED\uD83C\uDDF0");
        put("flag-hm", "\uD83C\uDDED\uD83C\uDDF2");
        put("flag-hn", "\uD83C\uDDED\uD83C\uDDF3");
        put("flag-hr", "\uD83C\uDDED\uD83C\uDDF7");
        put("flag-ht", "\uD83C\uDDED\uD83C\uDDF9");
        put("flag-hu", "\uD83C\uDDED\uD83C\uDDFA");
        put("flag-ic", "\uD83C\uDDEE\uD83C\uDDE8");
        put("flag-id", "\uD83C\uDDEE\uD83C\uDDE9");
        put("flag-ie", "\uD83C\uDDEE\uD83C\uDDEA");
        put("flag-il", "\uD83C\uDDEE\uD83C\uDDF1");
        put("flag-im", "\uD83C\uDDEE\uD83C\uDDF2");
        put("flag-in", "\uD83C\uDDEE\uD83C\uDDF3");
        put("flag-io", "\uD83C\uDDEE\uD83C\uDDF4");
        put("flag-iq", "\uD83C\uDDEE\uD83C\uDDF6");
        put("flag-ir", "\uD83C\uDDEE\uD83C\uDDF7");
        put("flag-is", "\uD83C\uDDEE\uD83C\uDDF8");
        put("it", "\uD83C\uDDEE\uD83C\uDDF9");
        put("flag-it", "\uD83C\uDDEE\uD83C\uDDF9");
        put("flag-je", "\uD83C\uDDEF\uD83C\uDDEA");
        put("flag-jm", "\uD83C\uDDEF\uD83C\uDDF2");
        put("flag-jo", "\uD83C\uDDEF\uD83C\uDDF4");
        put("jp", "\uD83C\uDDEF\uD83C\uDDF5");
        put("flag-jp", "\uD83C\uDDEF\uD83C\uDDF5");
        put("flag-ke", "\uD83C\uDDF0\uD83C\uDDEA");
        put("flag-kg", "\uD83C\uDDF0\uD83C\uDDEC");
        put("flag-kh", "\uD83C\uDDF0\uD83C\uDDED");
        put("flag-ki", "\uD83C\uDDF0\uD83C\uDDEE");
        put("flag-km", "\uD83C\uDDF0\uD83C\uDDF2");
        put("flag-kn", "\uD83C\uDDF0\uD83C\uDDF3");
        put("flag-kp", "\uD83C\uDDF0\uD83C\uDDF5");
        put("kr", "\uD83C\uDDF0\uD83C\uDDF7");
        put("flag-kr", "\uD83C\uDDF0\uD83C\uDDF7");
        put("flag-kw", "\uD83C\uDDF0\uD83C\uDDFC");
        put("flag-ky", "\uD83C\uDDF0\uD83C\uDDFE");
        put("flag-kz", "\uD83C\uDDF0\uD83C\uDDFF");
        put("flag-la", "\uD83C\uDDF1\uD83C\uDDE6");
        put("flag-lb", "\uD83C\uDDF1\uD83C\uDDE7");
        put("flag-lc", "\uD83C\uDDF1\uD83C\uDDE8");
        put("flag-li", "\uD83C\uDDF1\uD83C\uDDEE");
        put("flag-lk", "\uD83C\uDDF1\uD83C\uDDF0");
        put("flag-lr", "\uD83C\uDDF1\uD83C\uDDF7");
        put("flag-ls", "\uD83C\uDDF1\uD83C\uDDF8");
        put("flag-lt", "\uD83C\uDDF1\uD83C\uDDF9");
        put("flag-lu", "\uD83C\uDDF1\uD83C\uDDFA");
        put("flag-lv", "\uD83C\uDDF1\uD83C\uDDFB");
        put("flag-ly", "\uD83C\uDDF1\uD83C\uDDFE");
        put("flag-ma", "\uD83C\uDDF2\uD83C\uDDE6");
        put("flag-mc", "\uD83C\uDDF2\uD83C\uDDE8");
        put("flag-md", "\uD83C\uDDF2\uD83C\uDDE9");
        put("flag-me", "\uD83C\uDDF2\uD83C\uDDEA");
        put("flag-mf", "\uD83C\uDDF2\uD83C\uDDEB");
        put("flag-mg", "\uD83C\uDDF2\uD83C\uDDEC");
        put("flag-mh", "\uD83C\uDDF2\uD83C\uDDED");
        put("flag-mk", "\uD83C\uDDF2\uD83C\uDDF0");
        put("flag-ml", "\uD83C\uDDF2\uD83C\uDDF1");
        put("flag-mm", "\uD83C\uDDF2\uD83C\uDDF2");
        put("flag-mn", "\uD83C\uDDF2\uD83C\uDDF3");
        put("flag-mo", "\uD83C\uDDF2\uD83C\uDDF4");
        put("flag-mp", "\uD83C\uDDF2\uD83C\uDDF5");
        put("flag-mq", "\uD83C\uDDF2\uD83C\uDDF6");
        put("flag-mr", "\uD83C\uDDF2\uD83C\uDDF7");
        put("flag-ms", "\uD83C\uDDF2\uD83C\uDDF8");
        put("flag-mt", "\uD83C\uDDF2\uD83C\uDDF9");
        put("flag-mu", "\uD83C\uDDF2\uD83C\uDDFA");
        put("flag-mv", "\uD83C\uDDF2\uD83C\uDDFB");
        put("flag-mw", "\uD83C\uDDF2\uD83C\uDDFC");
        put("flag-mx", "\uD83C\uDDF2\uD83C\uDDFD");
        put("flag-my", "\uD83C\uDDF2\uD83C\uDDFE");
        put("flag-mz", "\uD83C\uDDF2\uD83C\uDDFF");
        put("flag-na", "\uD83C\uDDF3\uD83C\uDDE6");
        put("flag-nc", "\uD83C\uDDF3\uD83C\uDDE8");
        put("flag-ne", "\uD83C\uDDF3\uD83C\uDDEA");
        put("flag-nf", "\uD83C\uDDF3\uD83C\uDDEB");
        put("flag-ng", "\uD83C\uDDF3\uD83C\uDDEC");
        put("flag-ni", "\uD83C\uDDF3\uD83C\uDDEE");
        put("flag-nl", "\uD83C\uDDF3\uD83C\uDDF1");
        put("flag-no", "\uD83C\uDDF3\uD83C\uDDF4");
        put("flag-np", "\uD83C\uDDF3\uD83C\uDDF5");
        put("flag-nr", "\uD83C\uDDF3\uD83C\uDDF7");
        put("flag-nu", "\uD83C\uDDF3\uD83C\uDDFA");
        put("flag-nz", "\uD83C\uDDF3\uD83C\uDDFF");
        put("flag-om", "\uD83C\uDDF4\uD83C\uDDF2");
        put("flag-pa", "\uD83C\uDDF5\uD83C\uDDE6");
        put("flag-pe", "\uD83C\uDDF5\uD83C\uDDEA");
        put("flag-pf", "\uD83C\uDDF5\uD83C\uDDEB");
        put("flag-pg", "\uD83C\uDDF5\uD83C\uDDEC");
        put("flag-ph", "\uD83C\uDDF5\uD83C\uDDED");
        put("flag-pk", "\uD83C\uDDF5\uD83C\uDDF0");
        put("flag-pl", "\uD83C\uDDF5\uD83C\uDDF1");
        put("flag-pm", "\uD83C\uDDF5\uD83C\uDDF2");
        put("flag-pn", "\uD83C\uDDF5\uD83C\uDDF3");
        put("flag-pr", "\uD83C\uDDF5\uD83C\uDDF7");
        put("flag-ps", "\uD83C\uDDF5\uD83C\uDDF8");
        put("flag-pt", "\uD83C\uDDF5\uD83C\uDDF9");
        put("flag-pw", "\uD83C\uDDF5\uD83C\uDDFC");
        put("flag-py", "\uD83C\uDDF5\uD83C\uDDFE");
        put("flag-qa", "\uD83C\uDDF6\uD83C\uDDE6");
        put("flag-re", "\uD83C\uDDF7\uD83C\uDDEA");
        put("flag-ro", "\uD83C\uDDF7\uD83C\uDDF4");
        put("flag-rs", "\uD83C\uDDF7\uD83C\uDDF8");
        put("ru", "\uD83C\uDDF7\uD83C\uDDFA");
        put("flag-ru", "\uD83C\uDDF7\uD83C\uDDFA");
        put("flag-rw", "\uD83C\uDDF7\uD83C\uDDFC");
        put("flag-sa", "\uD83C\uDDF8\uD83C\uDDE6");
        put("flag-sb", "\uD83C\uDDF8\uD83C\uDDE7");
        put("flag-sc", "\uD83C\uDDF8\uD83C\uDDE8");
        put("flag-sd", "\uD83C\uDDF8\uD83C\uDDE9");
        put("flag-se", "\uD83C\uDDF8\uD83C\uDDEA");
        put("flag-sg", "\uD83C\uDDF8\uD83C\uDDEC");
        put("flag-sh", "\uD83C\uDDF8\uD83C\uDDED");
        put("flag-si", "\uD83C\uDDF8\uD83C\uDDEE");
        put("flag-sj", "\uD83C\uDDF8\uD83C\uDDEF");
        put("flag-sk", "\uD83C\uDDF8\uD83C\uDDF0");
        put("flag-sl", "\uD83C\uDDF8\uD83C\uDDF1");
        put("flag-sm", "\uD83C\uDDF8\uD83C\uDDF2");
        put("flag-sn", "\uD83C\uDDF8\uD83C\uDDF3");
        put("flag-so", "\uD83C\uDDF8\uD83C\uDDF4");
        put("flag-sr", "\uD83C\uDDF8\uD83C\uDDF7");
        put("flag-ss", "\uD83C\uDDF8\uD83C\uDDF8");
        put("flag-st", "\uD83C\uDDF8\uD83C\uDDF9");
        put("flag-sv", "\uD83C\uDDF8\uD83C\uDDFB");
        put("flag-sx", "\uD83C\uDDF8\uD83C\uDDFD");
        put("flag-sy", "\uD83C\uDDF8\uD83C\uDDFE");
        put("flag-sz", "\uD83C\uDDF8\uD83C\uDDFF");
        put("flag-ta", "\uD83C\uDDF9\uD83C\uDDE6");
        put("flag-tc", "\uD83C\uDDF9\uD83C\uDDE8");
        put("flag-td", "\uD83C\uDDF9\uD83C\uDDE9");
        put("flag-tf", "\uD83C\uDDF9\uD83C\uDDEB");
        put("flag-tg", "\uD83C\uDDF9\uD83C\uDDEC");
        put("flag-th", "\uD83C\uDDF9\uD83C\uDDED");
        put("flag-tj", "\uD83C\uDDF9\uD83C\uDDEF");
        put("flag-tk", "\uD83C\uDDF9\uD83C\uDDF0");
        put("flag-tl", "\uD83C\uDDF9\uD83C\uDDF1");
        put("flag-tm", "\uD83C\uDDF9\uD83C\uDDF2");
        put("flag-tn", "\uD83C\uDDF9\uD83C\uDDF3");
        put("flag-to", "\uD83C\uDDF9\uD83C\uDDF4");
        put("flag-tr", "\uD83C\uDDF9\uD83C\uDDF7");
        put("flag-tt", "\uD83C\uDDF9\uD83C\uDDF9");
        put("flag-tv", "\uD83C\uDDF9\uD83C\uDDFB");
        put("flag-tw", "\uD83C\uDDF9\uD83C\uDDFC");
        put("flag-tz", "\uD83C\uDDF9\uD83C\uDDFF");
        put("flag-ua", "\uD83C\uDDFA\uD83C\uDDE6");
        put("flag-ug", "\uD83C\uDDFA\uD83C\uDDEC");
        put("flag-um", "\uD83C\uDDFA\uD83C\uDDF2");
        put("flag-un", "\uD83C\uDDFA\uD83C\uDDF3");
        put("us", "\uD83C\uDDFA\uD83C\uDDF8");
        put("flag-us", "\uD83C\uDDFA\uD83C\uDDF8");
        put("flag-uy", "\uD83C\uDDFA\uD83C\uDDFE");
        put("flag-uz", "\uD83C\uDDFA\uD83C\uDDFF");
        put("flag-va", "\uD83C\uDDFB\uD83C\uDDE6");
        put("flag-vc", "\uD83C\uDDFB\uD83C\uDDE8");
        put("flag-ve", "\uD83C\uDDFB\uD83C\uDDEA");
        put("flag-vg", "\uD83C\uDDFB\uD83C\uDDEC");
        put("flag-vi", "\uD83C\uDDFB\uD83C\uDDEE");
        put("flag-vn", "\uD83C\uDDFB\uD83C\uDDF3");
        put("flag-vu", "\uD83C\uDDFB\uD83C\uDDFA");
        put("flag-wf", "\uD83C\uDDFC\uD83C\uDDEB");
        put("flag-ws", "\uD83C\uDDFC\uD83C\uDDF8");
        put("flag-xk", "\uD83C\uDDFD\uD83C\uDDF0");
        put("flag-ye", "\uD83C\uDDFE\uD83C\uDDEA");
        put("flag-yt", "\uD83C\uDDFE\uD83C\uDDF9");
        put("flag-za", "\uD83C\uDDFF\uD83C\uDDE6");
        put("flag-zm", "\uD83C\uDDFF\uD83C\uDDF2");
        put("flag-zw", "\uD83C\uDDFF\uD83C\uDDFC");
        put("koko", "\uD83C\uDE01");
        put("sa", "\uD83C\uDE02\uFE0F");
        put("u7121", "\uD83C\uDE1A");
        put("u6307", "\uD83C\uDE2F");
        put("u7981", "\uD83C\uDE32");
        put("u7a7a", "\uD83C\uDE33");
        put("u5408", "\uD83C\uDE34");
        put("u6e80", "\uD83C\uDE35");
        put("u6709", "\uD83C\uDE36");
        put("u6708", "\uD83C\uDE37\uFE0F");
        put("u7533", "\uD83C\uDE38");
        put("u5272", "\uD83C\uDE39");
        put("u55b6", "\uD83C\uDE3A");
        put("ideograph_advantage", "\uD83C\uDE50");
        put("accept", "\uD83C\uDE51");
        put("cyclone", "\uD83C\uDF00");
        put("foggy", "\uD83C\uDF01");
        put("closed_umbrella", "\uD83C\uDF02");
        put("night_with_stars", "\uD83C\uDF03");
        put("sunrise_over_mountains", "\uD83C\uDF04");
        put("sunrise", "\uD83C\uDF05");
        put("city_sunset", "\uD83C\uDF06");
        put("city_sunrise", "\uD83C\uDF07");
        put("rainbow", "\uD83C\uDF08");
        put("bridge_at_night", "\uD83C\uDF09");
        put("ocean", "\uD83C\uDF0A");
        put("volcano", "\uD83C\uDF0B");
        put("milky_way", "\uD83C\uDF0C");
        put("earth_africa", "\uD83C\uDF0D");
        put("earth_americas", "\uD83C\uDF0E");
        put("earth_asia", "\uD83C\uDF0F");
        put("globe_with_meridians", "\uD83C\uDF10");
        put("new_moon", "\uD83C\uDF11");
        put("waxing_crescent_moon", "\uD83C\uDF12");
        put("first_quarter_moon", "\uD83C\uDF13");
        put("moon", "\uD83C\uDF14");
        put("waxing_gibbous_moon", "\uD83C\uDF14");
        put("full_moon", "\uD83C\uDF15");
        put("waning_gibbous_moon", "\uD83C\uDF16");
        put("last_quarter_moon", "\uD83C\uDF17");
        put("waning_crescent_moon", "\uD83C\uDF18");
        put("crescent_moon", "\uD83C\uDF19");
        put("new_moon_with_face", "\uD83C\uDF1A");
        put("first_quarter_moon_with_face", "\uD83C\uDF1B");
        put("last_quarter_moon_with_face", "\uD83C\uDF1C");
        put("full_moon_with_face", "\uD83C\uDF1D");
        put("sun_with_face", "\uD83C\uDF1E");
        put("star2", "\uD83C\uDF1F");
        put("stars", "\uD83C\uDF20");
        put("thermometer", "\uD83C\uDF21\uFE0F");
        put("mostly_sunny", "\uD83C\uDF24\uFE0F");
        put("sun_small_cloud", "\uD83C\uDF24\uFE0F");
        put("barely_sunny", "\uD83C\uDF25\uFE0F");
        put("sun_behind_cloud", "\uD83C\uDF25\uFE0F");
        put("partly_sunny_rain", "\uD83C\uDF26\uFE0F");
        put("sun_behind_rain_cloud", "\uD83C\uDF26\uFE0F");
        put("rain_cloud", "\uD83C\uDF27\uFE0F");
        put("snow_cloud", "\uD83C\uDF28\uFE0F");
        put("lightning", "\uD83C\uDF29\uFE0F");
        put("lightning_cloud", "\uD83C\uDF29\uFE0F");
        put("tornado", "\uD83C\uDF2A\uFE0F");
        put("tornado_cloud", "\uD83C\uDF2A\uFE0F");
        put("fog", "\uD83C\uDF2B\uFE0F");
        put("wind_blowing_face", "\uD83C\uDF2C\uFE0F");
        put("hotdog", "\uD83C\uDF2D");
        put("taco", "\uD83C\uDF2E");
        put("burrito", "\uD83C\uDF2F");
        put("chestnut", "\uD83C\uDF30");
        put("seedling", "\uD83C\uDF31");
        put("evergreen_tree", "\uD83C\uDF32");
        put("deciduous_tree", "\uD83C\uDF33");
        put("palm_tree", "\uD83C\uDF34");
        put("cactus", "\uD83C\uDF35");
        put("hot_pepper", "\uD83C\uDF36\uFE0F");
        put("tulip", "\uD83C\uDF37");
        put("cherry_blossom", "\uD83C\uDF38");
        put("rose", "\uD83C\uDF39");
        put("hibiscus", "\uD83C\uDF3A");
        put("sunflower", "\uD83C\uDF3B");
        put("blossom", "\uD83C\uDF3C");
        put("corn", "\uD83C\uDF3D");
        put("ear_of_rice", "\uD83C\uDF3E");
        put("herb", "\uD83C\uDF3F");
        put("four_leaf_clover", "\uD83C\uDF40");
        put("maple_leaf", "\uD83C\uDF41");
        put("fallen_leaf", "\uD83C\uDF42");
        put("leaves", "\uD83C\uDF43");
        put("mushroom", "\uD83C\uDF44");
        put("tomato", "\uD83C\uDF45");
        put("eggplant", "\uD83C\uDF46");
        put("grapes", "\uD83C\uDF47");
        put("melon", "\uD83C\uDF48");
        put("watermelon", "\uD83C\uDF49");
        put("tangerine", "\uD83C\uDF4A");
        put("lemon", "\uD83C\uDF4B");
        put("banana", "\uD83C\uDF4C");
        put("pineapple", "\uD83C\uDF4D");
        put("apple", "\uD83C\uDF4E");
        put("green_apple", "\uD83C\uDF4F");
        put("pear", "\uD83C\uDF50");
        put("peach", "\uD83C\uDF51");
        put("cherries", "\uD83C\uDF52");
        put("strawberry", "\uD83C\uDF53");
        put("hamburger", "\uD83C\uDF54");
        put("pizza", "\uD83C\uDF55");
        put("meat_on_bone", "\uD83C\uDF56");
        put("poultry_leg", "\uD83C\uDF57");
        put("rice_cracker", "\uD83C\uDF58");
        put("rice_ball", "\uD83C\uDF59");
        put("rice", "\uD83C\uDF5A");
        put("curry", "\uD83C\uDF5B");
        put("ramen", "\uD83C\uDF5C");
        put("spaghetti", "\uD83C\uDF5D");
        put("bread", "\uD83C\uDF5E");
        put("fries", "\uD83C\uDF5F");
        put("sweet_potato", "\uD83C\uDF60");
        put("dango", "\uD83C\uDF61");
        put("oden", "\uD83C\uDF62");
        put("sushi", "\uD83C\uDF63");
        put("fried_shrimp", "\uD83C\uDF64");
        put("fish_cake", "\uD83C\uDF65");
        put("icecream", "\uD83C\uDF66");
        put("shaved_ice", "\uD83C\uDF67");
        put("ice_cream", "\uD83C\uDF68");
        put("doughnut", "\uD83C\uDF69");
        put("cookie", "\uD83C\uDF6A");
        put("chocolate_bar", "\uD83C\uDF6B");
        put("candy", "\uD83C\uDF6C");
        put("lollipop", "\uD83C\uDF6D");
        put("custard", "\uD83C\uDF6E");
        put("honey_pot", "\uD83C\uDF6F");
        put("cake", "\uD83C\uDF70");
        put("bento", "\uD83C\uDF71");
        put("stew", "\uD83C\uDF72");
        put("fried_egg", "\uD83C\uDF73");
        put("cooking", "\uD83C\uDF73");
        put("fork_and_knife", "\uD83C\uDF74");
        put("tea", "\uD83C\uDF75");
        put("sake", "\uD83C\uDF76");
        put("wine_glass", "\uD83C\uDF77");
        put("cocktail", "\uD83C\uDF78");
        put("tropical_drink", "\uD83C\uDF79");
        put("beer", "\uD83C\uDF7A");
        put("beers", "\uD83C\uDF7B");
        put("baby_bottle", "\uD83C\uDF7C");
        put("knife_fork_plate", "\uD83C\uDF7D\uFE0F");
        put("champagne", "\uD83C\uDF7E");
        put("popcorn", "\uD83C\uDF7F");
        put("ribbon", "\uD83C\uDF80");
        put("gift", "\uD83C\uDF81");
        put("birthday", "\uD83C\uDF82");
        put("jack_o_lantern", "\uD83C\uDF83");
        put("christmas_tree", "\uD83C\uDF84");
        put("santa", "\uD83C\uDF85");
        put("fireworks", "\uD83C\uDF86");
        put("sparkler", "\uD83C\uDF87");
        put("balloon", "\uD83C\uDF88");
        put("tada", "\uD83C\uDF89");
        put("confetti_ball", "\uD83C\uDF8A");
        put("tanabata_tree", "\uD83C\uDF8B");
        put("crossed_flags", "\uD83C\uDF8C");
        put("bamboo", "\uD83C\uDF8D");
        put("dolls", "\uD83C\uDF8E");
        put("flags", "\uD83C\uDF8F");
        put("wind_chime", "\uD83C\uDF90");
        put("rice_scene", "\uD83C\uDF91");
        put("school_satchel", "\uD83C\uDF92");
        put("mortar_board", "\uD83C\uDF93");
        put("medal", "\uD83C\uDF96\uFE0F");
        put("reminder_ribbon", "\uD83C\uDF97\uFE0F");
        put("studio_microphone", "\uD83C\uDF99\uFE0F");
        put("level_slider", "\uD83C\uDF9A\uFE0F");
        put("control_knobs", "\uD83C\uDF9B\uFE0F");
        put("film_frames", "\uD83C\uDF9E\uFE0F");
        put("admission_tickets", "\uD83C\uDF9F\uFE0F");
        put("carousel_horse", "\uD83C\uDFA0");
        put("ferris_wheel", "\uD83C\uDFA1");
        put("roller_coaster", "\uD83C\uDFA2");
        put("fishing_pole_and_fish", "\uD83C\uDFA3");
        put("microphone", "\uD83C\uDFA4");
        put("movie_camera", "\uD83C\uDFA5");
        put("cinema", "\uD83C\uDFA6");
        put("headphones", "\uD83C\uDFA7");
        put("art", "\uD83C\uDFA8");
        put("tophat", "\uD83C\uDFA9");
        put("circus_tent", "\uD83C\uDFAA");
        put("ticket", "\uD83C\uDFAB");
        put("clapper", "\uD83C\uDFAC");
        put("performing_arts", "\uD83C\uDFAD");
        put("video_game", "\uD83C\uDFAE");
        put("dart", "\uD83C\uDFAF");
        put("slot_machine", "\uD83C\uDFB0");
        put("8ball", "\uD83C\uDFB1");
        put("game_die", "\uD83C\uDFB2");
        put("bowling", "\uD83C\uDFB3");
        put("flower_playing_cards", "\uD83C\uDFB4");
        put("musical_note", "\uD83C\uDFB5");
        put("notes", "\uD83C\uDFB6");
        put("saxophone", "\uD83C\uDFB7");
        put("guitar", "\uD83C\uDFB8");
        put("musical_keyboard", "\uD83C\uDFB9");
        put("trumpet", "\uD83C\uDFBA");
        put("violin", "\uD83C\uDFBB");
        put("musical_score", "\uD83C\uDFBC");
        put("running_shirt_with_sash", "\uD83C\uDFBD");
        put("tennis", "\uD83C\uDFBE");
        put("ski", "\uD83C\uDFBF");
        put("basketball", "\uD83C\uDFC0");
        put("checkered_flag", "\uD83C\uDFC1");
        put("snowboarder", "\uD83C\uDFC2");
        put("woman-running", "\uD83C\uDFC3\u200D\u2640\uFE0F");
        put("man-running", "\uD83C\uDFC3\u200D\u2642\uFE0F");
        put("runner", "\uD83C\uDFC3\u200D\u2642\uFE0F");
        put("running", "\uD83C\uDFC3\u200D\u2642\uFE0F");
        put("woman-surfing", "\uD83C\uDFC4\u200D\u2640\uFE0F");
        put("man-surfing", "\uD83C\uDFC4\u200D\u2642\uFE0F");
        put("surfer", "\uD83C\uDFC4\u200D\u2642\uFE0F");
        put("sports_medal", "\uD83C\uDFC5");
        put("trophy", "\uD83C\uDFC6");
        put("horse_racing", "\uD83C\uDFC7");
        put("football", "\uD83C\uDFC8");
        put("rugby_football", "\uD83C\uDFC9");
        put("woman-swimming", "\uD83C\uDFCA\u200D\u2640\uFE0F");
        put("man-swimming", "\uD83C\uDFCA\u200D\u2642\uFE0F");
        put("swimmer", "\uD83C\uDFCA\u200D\u2642\uFE0F");
        put("woman-lifting-weights", "\uD83C\uDFCB\uFE0F\u200D\u2640\uFE0F");
        put("man-lifting-weights", "\uD83C\uDFCB\uFE0F\u200D\u2642\uFE0F");
        put("weight_lifter", "\uD83C\uDFCB\uFE0F\u200D\u2642\uFE0F");
        put("woman-golfing", "\uD83C\uDFCC\uFE0F\u200D\u2640\uFE0F");
        put("man-golfing", "\uD83C\uDFCC\uFE0F\u200D\u2642\uFE0F");
        put("golfer", "\uD83C\uDFCC\uFE0F\u200D\u2642\uFE0F");
        put("racing_motorcycle", "\uD83C\uDFCD\uFE0F");
        put("racing_car", "\uD83C\uDFCE\uFE0F");
        put("cricket_bat_and_ball", "\uD83C\uDFCF");
        put("volleyball", "\uD83C\uDFD0");
        put("field_hockey_stick_and_ball", "\uD83C\uDFD1");
        put("ice_hockey_stick_and_puck", "\uD83C\uDFD2");
        put("table_tennis_paddle_and_ball", "\uD83C\uDFD3");
        put("snow_capped_mountain", "\uD83C\uDFD4\uFE0F");
        put("camping", "\uD83C\uDFD5\uFE0F");
        put("beach_with_umbrella", "\uD83C\uDFD6\uFE0F");
        put("building_construction", "\uD83C\uDFD7\uFE0F");
        put("house_buildings", "\uD83C\uDFD8\uFE0F");
        put("cityscape", "\uD83C\uDFD9\uFE0F");
        put("derelict_house_building", "\uD83C\uDFDA\uFE0F");
        put("classical_building", "\uD83C\uDFDB\uFE0F");
        put("desert", "\uD83C\uDFDC\uFE0F");
        put("desert_island", "\uD83C\uDFDD\uFE0F");
        put("national_park", "\uD83C\uDFDE\uFE0F");
        put("stadium", "\uD83C\uDFDF\uFE0F");
        put("house", "\uD83C\uDFE0");
        put("house_with_garden", "\uD83C\uDFE1");
        put("office", "\uD83C\uDFE2");
        put("post_office", "\uD83C\uDFE3");
        put("european_post_office", "\uD83C\uDFE4");
        put("hospital", "\uD83C\uDFE5");
        put("bank", "\uD83C\uDFE6");
        put("atm", "\uD83C\uDFE7");
        put("hotel", "\uD83C\uDFE8");
        put("love_hotel", "\uD83C\uDFE9");
        put("convenience_store", "\uD83C\uDFEA");
        put("school", "\uD83C\uDFEB");
        put("department_store", "\uD83C\uDFEC");
        put("factory", "\uD83C\uDFED");
        put("izakaya_lantern", "\uD83C\uDFEE");
        put("lantern", "\uD83C\uDFEE");
        put("japanese_castle", "\uD83C\uDFEF");
        put("european_castle", "\uD83C\uDFF0");
        put("rainbow-flag", "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08");
        put("waving_white_flag", "\uD83C\uDFF3\uFE0F");
        put("flag-england", "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F");
        put("flag-scotland", "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC73\uDB40\uDC63\uDB40\uDC74\uDB40\uDC7F");
        put("flag-wales", "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC77\uDB40\uDC6C\uDB40\uDC73\uDB40\uDC7F");
        put("waving_black_flag", "\uD83C\uDFF4");
        put("rosette", "\uD83C\uDFF5\uFE0F");
        put("label", "\uD83C\uDFF7\uFE0F");
        put("badminton_racquet_and_shuttlecock", "\uD83C\uDFF8");
        put("bow_and_arrow", "\uD83C\uDFF9");
        put("amphora", "\uD83C\uDFFA");
        put("skin-tone-2", "\uD83C\uDFFB");
        put("skin-tone-3", "\uD83C\uDFFC");
        put("skin-tone-4", "\uD83C\uDFFD");
        put("skin-tone-5", "\uD83C\uDFFE");
        put("skin-tone-6", "\uD83C\uDFFF");
        put("rat", "\uD83D\uDC00");
        put("mouse2", "\uD83D\uDC01");
        put("ox", "\uD83D\uDC02");
        put("water_buffalo", "\uD83D\uDC03");
        put("cow2", "\uD83D\uDC04");
        put("tiger2", "\uD83D\uDC05");
        put("leopard", "\uD83D\uDC06");
        put("rabbit2", "\uD83D\uDC07");
        put("cat2", "\uD83D\uDC08");
        put("dragon", "\uD83D\uDC09");
        put("crocodile", "\uD83D\uDC0A");
        put("whale2", "\uD83D\uDC0B");
        put("snail", "\uD83D\uDC0C");
        put("snake", "\uD83D\uDC0D");
        put("racehorse", "\uD83D\uDC0E");
        put("ram", "\uD83D\uDC0F");
        put("goat", "\uD83D\uDC10");
        put("sheep", "\uD83D\uDC11");
        put("monkey", "\uD83D\uDC12");
        put("rooster", "\uD83D\uDC13");
        put("chicken", "\uD83D\uDC14");
        put("dog2", "\uD83D\uDC15");
        put("pig2", "\uD83D\uDC16");
        put("boar", "\uD83D\uDC17");
        put("elephant", "\uD83D\uDC18");
        put("octopus", "\uD83D\uDC19");
        put("shell", "\uD83D\uDC1A");
        put("bug", "\uD83D\uDC1B");
        put("ant", "\uD83D\uDC1C");
        put("bee", "\uD83D\uDC1D");
        put("honeybee", "\uD83D\uDC1D");
        put("beetle", "\uD83D\uDC1E");
        put("fish", "\uD83D\uDC1F");
        put("tropical_fish", "\uD83D\uDC20");
        put("blowfish", "\uD83D\uDC21");
        put("turtle", "\uD83D\uDC22");
        put("hatching_chick", "\uD83D\uDC23");
        put("baby_chick", "\uD83D\uDC24");
        put("hatched_chick", "\uD83D\uDC25");
        put("bird", "\uD83D\uDC26");
        put("penguin", "\uD83D\uDC27");
        put("koala", "\uD83D\uDC28");
        put("poodle", "\uD83D\uDC29");
        put("dromedary_camel", "\uD83D\uDC2A");
        put("camel", "\uD83D\uDC2B");
        put("dolphin", "\uD83D\uDC2C");
        put("flipper", "\uD83D\uDC2C");
        put("mouse", "\uD83D\uDC2D");
        put("cow", "\uD83D\uDC2E");
        put("tiger", "\uD83D\uDC2F");
        put("rabbit", "\uD83D\uDC30");
        put("cat", "\uD83D\uDC31");
        put("dragon_face", "\uD83D\uDC32");
        put("whale", "\uD83D\uDC33");
        put("horse", "\uD83D\uDC34");
        put("monkey_face", "\uD83D\uDC35");
        put("dog", "\uD83D\uDC36");
        put("pig", "\uD83D\uDC37");
        put("frog", "\uD83D\uDC38");
        put("hamster", "\uD83D\uDC39");
        put("wolf", "\uD83D\uDC3A");
        put("bear", "\uD83D\uDC3B");
        put("panda_face", "\uD83D\uDC3C");
        put("pig_nose", "\uD83D\uDC3D");
        put("feet", "\uD83D\uDC3E");
        put("paw_prints", "\uD83D\uDC3E");
        put("chipmunk", "\uD83D\uDC3F\uFE0F");
        put("eyes", "\uD83D\uDC40");
        put("eye-in-speech-bubble", "\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F");
        put("eye", "\uD83D\uDC41\uFE0F");
        put("ear", "\uD83D\uDC42");
        put("nose", "\uD83D\uDC43");
        put("lips", "\uD83D\uDC44");
        put("tongue", "\uD83D\uDC45");
        put("point_up_2", "\uD83D\uDC46");
        put("point_down", "\uD83D\uDC47");
        put("point_left", "\uD83D\uDC48");
        put("point_right", "\uD83D\uDC49");
        put("facepunch", "\uD83D\uDC4A");
        put("punch", "\uD83D\uDC4A");
        put("wave", "\uD83D\uDC4B");
        put("ok_hand", "\uD83D\uDC4C");
        put("+1", "\uD83D\uDC4D");
        put("thumbsup", "\uD83D\uDC4D");
        put("-1", "\uD83D\uDC4E");
        put("thumbsdown", "\uD83D\uDC4E");
        put("clap", "\uD83D\uDC4F");
        put("open_hands", "\uD83D\uDC50");
        put("crown", "\uD83D\uDC51");
        put("womans_hat", "\uD83D\uDC52");
        put("eyeglasses", "\uD83D\uDC53");
        put("necktie", "\uD83D\uDC54");
        put("shirt", "\uD83D\uDC55");
        put("tshirt", "\uD83D\uDC55");
        put("jeans", "\uD83D\uDC56");
        put("dress", "\uD83D\uDC57");
        put("kimono", "\uD83D\uDC58");
        put("bikini", "\uD83D\uDC59");
        put("womans_clothes", "\uD83D\uDC5A");
        put("purse", "\uD83D\uDC5B");
        put("handbag", "\uD83D\uDC5C");
        put("pouch", "\uD83D\uDC5D");
        put("mans_shoe", "\uD83D\uDC5E");
        put("shoe", "\uD83D\uDC5E");
        put("athletic_shoe", "\uD83D\uDC5F");
        put("high_heel", "\uD83D\uDC60");
        put("sandal", "\uD83D\uDC61");
        put("boot", "\uD83D\uDC62");
        put("footprints", "\uD83D\uDC63");
        put("bust_in_silhouette", "\uD83D\uDC64");
        put("busts_in_silhouette", "\uD83D\uDC65");
        put("boy", "\uD83D\uDC66");
        put("girl", "\uD83D\uDC67");
        put("male-farmer", "\uD83D\uDC68\u200D\uD83C\uDF3E");
        put("male-cook", "\uD83D\uDC68\u200D\uD83C\uDF73");
        put("male-student", "\uD83D\uDC68\u200D\uD83C\uDF93");
        put("male-singer", "\uD83D\uDC68\u200D\uD83C\uDFA4");
        put("male-artist", "\uD83D\uDC68\u200D\uD83C\uDFA8");
        put("male-teacher", "\uD83D\uDC68\u200D\uD83C\uDFEB");
        put("male-factory-worker", "\uD83D\uDC68\u200D\uD83C\uDFED");
        put("man-boy-boy", "\uD83D\uDC68\u200D\uD83D\uDC66\u200D\uD83D\uDC66");
        put("man-boy", "\uD83D\uDC68\u200D\uD83D\uDC66");
        put("man-girl-boy", "\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC66");
        put("man-girl-girl", "\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC67");
        put("man-girl", "\uD83D\uDC68\u200D\uD83D\uDC67");
        put("man-man-boy", "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66");
        put("man-man-boy-boy", "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66\u200D\uD83D\uDC66");
        put("man-man-girl", "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67");
        put("man-man-girl-boy", "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC66");
        put("man-man-girl-girl", "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC67");
        put("man-woman-boy", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66");
        put("family", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66");
        put("man-woman-boy-boy", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66");
        put("man-woman-girl", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67");
        put("man-woman-girl-boy", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66");
        put("man-woman-girl-girl", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67");
        put("male-technologist", "\uD83D\uDC68\u200D\uD83D\uDCBB");
        put("male-office-worker", "\uD83D\uDC68\u200D\uD83D\uDCBC");
        put("male-mechanic", "\uD83D\uDC68\u200D\uD83D\uDD27");
        put("male-scientist", "\uD83D\uDC68\u200D\uD83D\uDD2C");
        put("male-astronaut", "\uD83D\uDC68\u200D\uD83D\uDE80");
        put("male-firefighter", "\uD83D\uDC68\u200D\uD83D\uDE92");
        put("male-doctor", "\uD83D\uDC68\u200D\u2695\uFE0F");
        put("male-judge", "\uD83D\uDC68\u200D\u2696\uFE0F");
        put("male-pilot", "\uD83D\uDC68\u200D\u2708\uFE0F");
        put("man-heart-man", "\uD83D\uDC68\u200D\u2764\uFE0F\u200D\uD83D\uDC68");
        put("man-kiss-man", "\uD83D\uDC68\u200D\u2764\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68");
        put("man", "\uD83D\uDC68");
        put("female-farmer", "\uD83D\uDC69\u200D\uD83C\uDF3E");
        put("female-cook", "\uD83D\uDC69\u200D\uD83C\uDF73");
        put("female-student", "\uD83D\uDC69\u200D\uD83C\uDF93");
        put("female-singer", "\uD83D\uDC69\u200D\uD83C\uDFA4");
        put("female-artist", "\uD83D\uDC69\u200D\uD83C\uDFA8");
        put("female-teacher", "\uD83D\uDC69\u200D\uD83C\uDFEB");
        put("female-factory-worker", "\uD83D\uDC69\u200D\uD83C\uDFED");
        put("woman-boy-boy", "\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66");
        put("woman-boy", "\uD83D\uDC69\u200D\uD83D\uDC66");
        put("woman-girl-boy", "\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66");
        put("woman-girl-girl", "\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67");
        put("woman-girl", "\uD83D\uDC69\u200D\uD83D\uDC67");
        put("woman-woman-boy", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC66");
        put("woman-woman-boy-boy", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66");
        put("woman-woman-girl", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67");
        put("woman-woman-girl-boy", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66");
        put("woman-woman-girl-girl", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67");
        put("female-technologist", "\uD83D\uDC69\u200D\uD83D\uDCBB");
        put("female-office-worker", "\uD83D\uDC69\u200D\uD83D\uDCBC");
        put("female-mechanic", "\uD83D\uDC69\u200D\uD83D\uDD27");
        put("female-scientist", "\uD83D\uDC69\u200D\uD83D\uDD2C");
        put("female-astronaut", "\uD83D\uDC69\u200D\uD83D\uDE80");
        put("female-firefighter", "\uD83D\uDC69\u200D\uD83D\uDE92");
        put("female-doctor", "\uD83D\uDC69\u200D\u2695\uFE0F");
        put("female-judge", "\uD83D\uDC69\u200D\u2696\uFE0F");
        put("female-pilot", "\uD83D\uDC69\u200D\u2708\uFE0F");
        put("woman-heart-man", "\uD83D\uDC69\u200D\u2764\uFE0F\u200D\uD83D\uDC68");
        put("couple_with_heart", "\uD83D\uDC69\u200D\u2764\uFE0F\u200D\uD83D\uDC68");
        put("woman-heart-woman", "\uD83D\uDC69\u200D\u2764\uFE0F\u200D\uD83D\uDC69");
        put("woman-kiss-man", "\uD83D\uDC69\u200D\u2764\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68");
        put("couplekiss", "\uD83D\uDC69\u200D\u2764\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC68");
        put("woman-kiss-woman", "\uD83D\uDC69\u200D\u2764\uFE0F\u200D\uD83D\uDC8B\u200D\uD83D\uDC69");
        put("woman", "\uD83D\uDC69");
        put("couple", "\uD83D\uDC6B");
        put("man_and_woman_holding_hands", "\uD83D\uDC6B");
        put("two_men_holding_hands", "\uD83D\uDC6C");
        put("two_women_holding_hands", "\uD83D\uDC6D");
        put("female-police-officer", "\uD83D\uDC6E\u200D\u2640\uFE0F");
        put("male-police-officer", "\uD83D\uDC6E\u200D\u2642\uFE0F");
        put("cop", "\uD83D\uDC6E\u200D\u2642\uFE0F");
        put("woman-with-bunny-ears-partying", "\uD83D\uDC6F\u200D\u2640\uFE0F");
        put("dancers", "\uD83D\uDC6F\u200D\u2640\uFE0F");
        put("man-with-bunny-ears-partying", "\uD83D\uDC6F\u200D\u2642\uFE0F");
        put("bride_with_veil", "\uD83D\uDC70");
        put("blond-haired-woman", "\uD83D\uDC71\u200D\u2640\uFE0F");
        put("blond-haired-man", "\uD83D\uDC71\u200D\u2642\uFE0F");
        put("person_with_blond_hair", "\uD83D\uDC71\u200D\u2642\uFE0F");
        put("man_with_gua_pi_mao", "\uD83D\uDC72");
        put("woman-wearing-turban", "\uD83D\uDC73\u200D\u2640\uFE0F");
        put("man-wearing-turban", "\uD83D\uDC73\u200D\u2642\uFE0F");
        put("man_with_turban", "\uD83D\uDC73\u200D\u2642\uFE0F");
        put("older_man", "\uD83D\uDC74");
        put("older_woman", "\uD83D\uDC75");
        put("baby", "\uD83D\uDC76");
        put("female-construction-worker", "\uD83D\uDC77\u200D\u2640\uFE0F");
        put("male-construction-worker", "\uD83D\uDC77\u200D\u2642\uFE0F");
        put("construction_worker", "\uD83D\uDC77\u200D\u2642\uFE0F");
        put("princess", "\uD83D\uDC78");
        put("japanese_ogre", "\uD83D\uDC79");
        put("japanese_goblin", "\uD83D\uDC7A");
        put("ghost", "\uD83D\uDC7B");
        put("angel", "\uD83D\uDC7C");
        put("alien", "\uD83D\uDC7D");
        put("space_invader", "\uD83D\uDC7E");
        put("imp", "\uD83D\uDC7F");
        put("skull", "\uD83D\uDC80");
        put("woman-tipping-hand", "\uD83D\uDC81\u200D\u2640\uFE0F");
        put("information_desk_person", "\uD83D\uDC81\u200D\u2640\uFE0F");
        put("man-tipping-hand", "\uD83D\uDC81\u200D\u2642\uFE0F");
        put("female-guard", "\uD83D\uDC82\u200D\u2640\uFE0F");
        put("male-guard", "\uD83D\uDC82\u200D\u2642\uFE0F");
        put("guardsman", "\uD83D\uDC82\u200D\u2642\uFE0F");
        put("dancer", "\uD83D\uDC83");
        put("lipstick", "\uD83D\uDC84");
        put("nail_care", "\uD83D\uDC85");
        put("woman-getting-massage", "\uD83D\uDC86\u200D\u2640\uFE0F");
        put("massage", "\uD83D\uDC86\u200D\u2640\uFE0F");
        put("man-getting-massage", "\uD83D\uDC86\u200D\u2642\uFE0F");
        put("woman-getting-haircut", "\uD83D\uDC87\u200D\u2640\uFE0F");
        put("haircut", "\uD83D\uDC87\u200D\u2640\uFE0F");
        put("man-getting-haircut", "\uD83D\uDC87\u200D\u2642\uFE0F");
        put("barber", "\uD83D\uDC88");
        put("syringe", "\uD83D\uDC89");
        put("pill", "\uD83D\uDC8A");
        put("kiss", "\uD83D\uDC8B");
        put("love_letter", "\uD83D\uDC8C");
        put("ring", "\uD83D\uDC8D");
        put("gem", "\uD83D\uDC8E");
        put("bouquet", "\uD83D\uDC90");
        put("wedding", "\uD83D\uDC92");
        put("heartbeat", "\uD83D\uDC93");
        put("broken_heart", "\uD83D\uDC94");
        put("two_hearts", "\uD83D\uDC95");
        put("sparkling_heart", "\uD83D\uDC96");
        put("heartpulse", "\uD83D\uDC97");
        put("cupid", "\uD83D\uDC98");
        put("blue_heart", "\uD83D\uDC99");
        put("green_heart", "\uD83D\uDC9A");
        put("yellow_heart", "\uD83D\uDC9B");
        put("purple_heart", "\uD83D\uDC9C");
        put("gift_heart", "\uD83D\uDC9D");
        put("revolving_hearts", "\uD83D\uDC9E");
        put("heart_decoration", "\uD83D\uDC9F");
        put("diamond_shape_with_a_dot_inside", "\uD83D\uDCA0");
        put("bulb", "\uD83D\uDCA1");
        put("anger", "\uD83D\uDCA2");
        put("bomb", "\uD83D\uDCA3");
        put("zzz", "\uD83D\uDCA4");
        put("boom", "\uD83D\uDCA5");
        put("collision", "\uD83D\uDCA5");
        put("sweat_drops", "\uD83D\uDCA6");
        put("droplet", "\uD83D\uDCA7");
        put("dash", "\uD83D\uDCA8");
        put("hankey", "\uD83D\uDCA9");
        put("poop", "\uD83D\uDCA9");
        put("shit", "\uD83D\uDCA9");
        put("muscle", "\uD83D\uDCAA");
        put("dizzy", "\uD83D\uDCAB");
        put("speech_balloon", "\uD83D\uDCAC");
        put("thought_balloon", "\uD83D\uDCAD");
        put("white_flower", "\uD83D\uDCAE");
        put("100", "\uD83D\uDCAF");
        put("moneybag", "\uD83D\uDCB0");
        put("currency_exchange", "\uD83D\uDCB1");
        put("heavy_dollar_sign", "\uD83D\uDCB2");
        put("credit_card", "\uD83D\uDCB3");
        put("yen", "\uD83D\uDCB4");
        put("dollar", "\uD83D\uDCB5");
        put("euro", "\uD83D\uDCB6");
        put("pound", "\uD83D\uDCB7");
        put("money_with_wings", "\uD83D\uDCB8");
        put("chart", "\uD83D\uDCB9");
        put("seat", "\uD83D\uDCBA");
        put("computer", "\uD83D\uDCBB");
        put("briefcase", "\uD83D\uDCBC");
        put("minidisc", "\uD83D\uDCBD");
        put("floppy_disk", "\uD83D\uDCBE");
        put("cd", "\uD83D\uDCBF");
        put("dvd", "\uD83D\uDCC0");
        put("file_folder", "\uD83D\uDCC1");
        put("open_file_folder", "\uD83D\uDCC2");
        put("page_with_curl", "\uD83D\uDCC3");
        put("page_facing_up", "\uD83D\uDCC4");
        put("date", "\uD83D\uDCC5");
        put("calendar", "\uD83D\uDCC6");
        put("card_index", "\uD83D\uDCC7");
        put("chart_with_upwards_trend", "\uD83D\uDCC8");
        put("chart_with_downwards_trend", "\uD83D\uDCC9");
        put("bar_chart", "\uD83D\uDCCA");
        put("clipboard", "\uD83D\uDCCB");
        put("pushpin", "\uD83D\uDCCC");
        put("round_pushpin", "\uD83D\uDCCD");
        put("paperclip", "\uD83D\uDCCE");
        put("straight_ruler", "\uD83D\uDCCF");
        put("triangular_ruler", "\uD83D\uDCD0");
        put("bookmark_tabs", "\uD83D\uDCD1");
        put("ledger", "\uD83D\uDCD2");
        put("notebook", "\uD83D\uDCD3");
        put("notebook_with_decorative_cover", "\uD83D\uDCD4");
        put("closed_book", "\uD83D\uDCD5");
        put("book", "\uD83D\uDCD6");
        put("open_book", "\uD83D\uDCD6");
        put("green_book", "\uD83D\uDCD7");
        put("blue_book", "\uD83D\uDCD8");
        put("orange_book", "\uD83D\uDCD9");
        put("books", "\uD83D\uDCDA");
        put("name_badge", "\uD83D\uDCDB");
        put("scroll", "\uD83D\uDCDC");
        put("memo", "\uD83D\uDCDD");
        put("pencil", "\uD83D\uDCDD");
        put("telephone_receiver", "\uD83D\uDCDE");
        put("pager", "\uD83D\uDCDF");
        put("fax", "\uD83D\uDCE0");
        put("satellite_antenna", "\uD83D\uDCE1");
        put("loudspeaker", "\uD83D\uDCE2");
        put("mega", "\uD83D\uDCE3");
        put("outbox_tray", "\uD83D\uDCE4");
        put("inbox_tray", "\uD83D\uDCE5");
        put("package", "\uD83D\uDCE6");
        put("e-mail", "\uD83D\uDCE7");
        put("incoming_envelope", "\uD83D\uDCE8");
        put("envelope_with_arrow", "\uD83D\uDCE9");
        put("mailbox_closed", "\uD83D\uDCEA");
        put("mailbox", "\uD83D\uDCEB");
        put("mailbox_with_mail", "\uD83D\uDCEC");
        put("mailbox_with_no_mail", "\uD83D\uDCED");
        put("postbox", "\uD83D\uDCEE");
        put("postal_horn", "\uD83D\uDCEF");
        put("newspaper", "\uD83D\uDCF0");
        put("iphone", "\uD83D\uDCF1");
        put("calling", "\uD83D\uDCF2");
        put("vibration_mode", "\uD83D\uDCF3");
        put("mobile_phone_off", "\uD83D\uDCF4");
        put("no_mobile_phones", "\uD83D\uDCF5");
        put("signal_strength", "\uD83D\uDCF6");
        put("camera", "\uD83D\uDCF7");
        put("camera_with_flash", "\uD83D\uDCF8");
        put("video_camera", "\uD83D\uDCF9");
        put("tv", "\uD83D\uDCFA");
        put("radio", "\uD83D\uDCFB");
        put("vhs", "\uD83D\uDCFC");
        put("film_projector", "\uD83D\uDCFD\uFE0F");
        put("prayer_beads", "\uD83D\uDCFF");
        put("twisted_rightwards_arrows", "\uD83D\uDD00");
        put("repeat", "\uD83D\uDD01");
        put("repeat_one", "\uD83D\uDD02");
        put("arrows_clockwise", "\uD83D\uDD03");
        put("arrows_counterclockwise", "\uD83D\uDD04");
        put("low_brightness", "\uD83D\uDD05");
        put("high_brightness", "\uD83D\uDD06");
        put("mute", "\uD83D\uDD07");
        put("speaker", "\uD83D\uDD08");
        put("sound", "\uD83D\uDD09");
        put("loud_sound", "\uD83D\uDD0A");
        put("battery", "\uD83D\uDD0B");
        put("electric_plug", "\uD83D\uDD0C");
        put("mag", "\uD83D\uDD0D");
        put("mag_right", "\uD83D\uDD0E");
        put("lock_with_ink_pen", "\uD83D\uDD0F");
        put("closed_lock_with_key", "\uD83D\uDD10");
        put("key", "\uD83D\uDD11");
        put("lock", "\uD83D\uDD12");
        put("unlock", "\uD83D\uDD13");
        put("bell", "\uD83D\uDD14");
        put("no_bell", "\uD83D\uDD15");
        put("bookmark", "\uD83D\uDD16");
        put("link", "\uD83D\uDD17");
        put("radio_button", "\uD83D\uDD18");
        put("back", "\uD83D\uDD19");
        put("end", "\uD83D\uDD1A");
        put("on", "\uD83D\uDD1B");
        put("soon", "\uD83D\uDD1C");
        put("top", "\uD83D\uDD1D");
        put("underage", "\uD83D\uDD1E");
        put("keycap_ten", "\uD83D\uDD1F");
        put("capital_abcd", "\uD83D\uDD20");
        put("abcd", "\uD83D\uDD21");
        put("1234", "\uD83D\uDD22");
        put("symbols", "\uD83D\uDD23");
        put("abc", "\uD83D\uDD24");
        put("fire", "\uD83D\uDD25");
        put("flashlight", "\uD83D\uDD26");
        put("wrench", "\uD83D\uDD27");
        put("hammer", "\uD83D\uDD28");
        put("nut_and_bolt", "\uD83D\uDD29");
        put("hocho", "\uD83D\uDD2A");
        put("knife", "\uD83D\uDD2A");
        put("gun", "\uD83D\uDD2B");
        put("microscope", "\uD83D\uDD2C");
        put("telescope", "\uD83D\uDD2D");
        put("crystal_ball", "\uD83D\uDD2E");
        put("six_pointed_star", "\uD83D\uDD2F");
        put("beginner", "\uD83D\uDD30");
        put("trident", "\uD83D\uDD31");
        put("black_square_button", "\uD83D\uDD32");
        put("white_square_button", "\uD83D\uDD33");
        put("red_circle", "\uD83D\uDD34");
        put("large_blue_circle", "\uD83D\uDD35");
        put("large_orange_diamond", "\uD83D\uDD36");
        put("large_blue_diamond", "\uD83D\uDD37");
        put("small_orange_diamond", "\uD83D\uDD38");
        put("small_blue_diamond", "\uD83D\uDD39");
        put("small_red_triangle", "\uD83D\uDD3A");
        put("small_red_triangle_down", "\uD83D\uDD3B");
        put("arrow_up_small", "\uD83D\uDD3C");
        put("arrow_down_small", "\uD83D\uDD3D");
        put("om_symbol", "\uD83D\uDD49\uFE0F");
        put("dove_of_peace", "\uD83D\uDD4A\uFE0F");
        put("kaaba", "\uD83D\uDD4B");
        put("mosque", "\uD83D\uDD4C");
        put("synagogue", "\uD83D\uDD4D");
        put("menorah_with_nine_branches", "\uD83D\uDD4E");
        put("clock1", "\uD83D\uDD50");
        put("clock2", "\uD83D\uDD51");
        put("clock3", "\uD83D\uDD52");
        put("clock4", "\uD83D\uDD53");
        put("clock5", "\uD83D\uDD54");
        put("clock6", "\uD83D\uDD55");
        put("clock7", "\uD83D\uDD56");
        put("clock8", "\uD83D\uDD57");
        put("clock9", "\uD83D\uDD58");
        put("clock10", "\uD83D\uDD59");
        put("clock11", "\uD83D\uDD5A");
        put("clock12", "\uD83D\uDD5B");
        put("clock130", "\uD83D\uDD5C");
        put("clock230", "\uD83D\uDD5D");
        put("clock330", "\uD83D\uDD5E");
        put("clock430", "\uD83D\uDD5F");
        put("clock530", "\uD83D\uDD60");
        put("clock630", "\uD83D\uDD61");
        put("clock730", "\uD83D\uDD62");
        put("clock830", "\uD83D\uDD63");
        put("clock930", "\uD83D\uDD64");
        put("clock1030", "\uD83D\uDD65");
        put("clock1130", "\uD83D\uDD66");
        put("clock1230", "\uD83D\uDD67");
        put("candle", "\uD83D\uDD6F\uFE0F");
        put("mantelpiece_clock", "\uD83D\uDD70\uFE0F");
        put("hole", "\uD83D\uDD73\uFE0F");
        put("man_in_business_suit_levitating", "\uD83D\uDD74\uFE0F");
        put("female-detective", "\uD83D\uDD75\uFE0F\u200D\u2640\uFE0F");
        put("male-detective", "\uD83D\uDD75\uFE0F\u200D\u2642\uFE0F");
        put("sleuth_or_spy", "\uD83D\uDD75\uFE0F\u200D\u2642\uFE0F");
        put("dark_sunglasses", "\uD83D\uDD76\uFE0F");
        put("spider", "\uD83D\uDD77\uFE0F");
        put("spider_web", "\uD83D\uDD78\uFE0F");
        put("joystick", "\uD83D\uDD79\uFE0F");
        put("man_dancing", "\uD83D\uDD7A");
        put("linked_paperclips", "\uD83D\uDD87\uFE0F");
        put("lower_left_ballpoint_pen", "\uD83D\uDD8A\uFE0F");
        put("lower_left_fountain_pen", "\uD83D\uDD8B\uFE0F");
        put("lower_left_paintbrush", "\uD83D\uDD8C\uFE0F");
        put("lower_left_crayon", "\uD83D\uDD8D\uFE0F");
        put("raised_hand_with_fingers_splayed", "\uD83D\uDD90\uFE0F");
        put("middle_finger", "\uD83D\uDD95");
        put("reversed_hand_with_middle_finger_extended", "\uD83D\uDD95");
        put("spock-hand", "\uD83D\uDD96");
        put("black_heart", "\uD83D\uDDA4");
        put("desktop_computer", "\uD83D\uDDA5\uFE0F");
        put("printer", "\uD83D\uDDA8\uFE0F");
        put("three_button_mouse", "\uD83D\uDDB1\uFE0F");
        put("trackball", "\uD83D\uDDB2\uFE0F");
        put("frame_with_picture", "\uD83D\uDDBC\uFE0F");
        put("card_index_dividers", "\uD83D\uDDC2\uFE0F");
        put("card_file_box", "\uD83D\uDDC3\uFE0F");
        put("file_cabinet", "\uD83D\uDDC4\uFE0F");
        put("wastebasket", "\uD83D\uDDD1\uFE0F");
        put("spiral_note_pad", "\uD83D\uDDD2\uFE0F");
        put("spiral_calendar_pad", "\uD83D\uDDD3\uFE0F");
        put("compression", "\uD83D\uDDDC\uFE0F");
        put("old_key", "\uD83D\uDDDD\uFE0F");
        put("rolled_up_newspaper", "\uD83D\uDDDE\uFE0F");
        put("dagger_knife", "\uD83D\uDDE1\uFE0F");
        put("speaking_head_in_silhouette", "\uD83D\uDDE3\uFE0F");
        put("left_speech_bubble", "\uD83D\uDDE8\uFE0F");
        put("right_anger_bubble", "\uD83D\uDDEF\uFE0F");
        put("ballot_box_with_ballot", "\uD83D\uDDF3\uFE0F");
        put("world_map", "\uD83D\uDDFA\uFE0F");
        put("mount_fuji", "\uD83D\uDDFB");
        put("tokyo_tower", "\uD83D\uDDFC");
        put("statue_of_liberty", "\uD83D\uDDFD");
        put("japan", "\uD83D\uDDFE");
        put("moyai", "\uD83D\uDDFF");
        put("grinning", "\uD83D\uDE00");
        put("grin", "\uD83D\uDE01");
        put("joy", "\uD83D\uDE02");
        put("smiley", "\uD83D\uDE03");
        put("smile", "\uD83D\uDE04");
        put("sweat_smile", "\uD83D\uDE05");
        put("laughing", "\uD83D\uDE06");
        put("satisfied", "\uD83D\uDE06");
        put("innocent", "\uD83D\uDE07");
        put("smiling_imp", "\uD83D\uDE08");
        put("wink", "\uD83D\uDE09");
        put("blush", "\uD83D\uDE0A");
        put("yum", "\uD83D\uDE0B");
        put("relieved", "\uD83D\uDE0C");
        put("heart_eyes", "\uD83D\uDE0D");
        put("sunglasses", "\uD83D\uDE0E");
        put("smirk", "\uD83D\uDE0F");
        put("neutral_face", "\uD83D\uDE10");
        put("expressionless", "\uD83D\uDE11");
        put("unamused", "\uD83D\uDE12");
        put("sweat", "\uD83D\uDE13");
        put("pensive", "\uD83D\uDE14");
        put("confused", "\uD83D\uDE15");
        put("confounded", "\uD83D\uDE16");
        put("kissing", "\uD83D\uDE17");
        put("kissing_heart", "\uD83D\uDE18");
        put("kissing_smiling_eyes", "\uD83D\uDE19");
        put("kissing_closed_eyes", "\uD83D\uDE1A");
        put("stuck_out_tongue", "\uD83D\uDE1B");
        put("stuck_out_tongue_winking_eye", "\uD83D\uDE1C");
        put("stuck_out_tongue_closed_eyes", "\uD83D\uDE1D");
        put("disappointed", "\uD83D\uDE1E");
        put("worried", "\uD83D\uDE1F");
        put("angry", "\uD83D\uDE20");
        put("rage", "\uD83D\uDE21");
        put("cry", "\uD83D\uDE22");
        put("persevere", "\uD83D\uDE23");
        put("triumph", "\uD83D\uDE24");
        put("disappointed_relieved", "\uD83D\uDE25");
        put("frowning", "\uD83D\uDE26");
        put("anguished", "\uD83D\uDE27");
        put("fearful", "\uD83D\uDE28");
        put("weary", "\uD83D\uDE29");
        put("sleepy", "\uD83D\uDE2A");
        put("tired_face", "\uD83D\uDE2B");
        put("grimacing", "\uD83D\uDE2C");
        put("sob", "\uD83D\uDE2D");
        put("open_mouth", "\uD83D\uDE2E");
        put("hushed", "\uD83D\uDE2F");
        put("cold_sweat", "\uD83D\uDE30");
        put("scream", "\uD83D\uDE31");
        put("astonished", "\uD83D\uDE32");
        put("flushed", "\uD83D\uDE33");
        put("sleeping", "\uD83D\uDE34");
        put("dizzy_face", "\uD83D\uDE35");
        put("no_mouth", "\uD83D\uDE36");
        put("mask", "\uD83D\uDE37");
        put("smile_cat", "\uD83D\uDE38");
        put("joy_cat", "\uD83D\uDE39");
        put("smiley_cat", "\uD83D\uDE3A");
        put("heart_eyes_cat", "\uD83D\uDE3B");
        put("smirk_cat", "\uD83D\uDE3C");
        put("kissing_cat", "\uD83D\uDE3D");
        put("pouting_cat", "\uD83D\uDE3E");
        put("crying_cat_face", "\uD83D\uDE3F");
        put("scream_cat", "\uD83D\uDE40");
        put("slightly_frowning_face", "\uD83D\uDE41");
        put("slightly_smiling_face", "\uD83D\uDE42");
        put("upside_down_face", "\uD83D\uDE43");
        put("face_with_rolling_eyes", "\uD83D\uDE44");
        put("woman-gesturing-no", "\uD83D\uDE45\u200D\u2640\uFE0F");
        put("no_good", "\uD83D\uDE45\u200D\u2640\uFE0F");
        put("man-gesturing-no", "\uD83D\uDE45\u200D\u2642\uFE0F");
        put("woman-gesturing-ok", "\uD83D\uDE46\u200D\u2640\uFE0F");
        put("ok_woman", "\uD83D\uDE46\u200D\u2640\uFE0F");
        put("man-gesturing-ok", "\uD83D\uDE46\u200D\u2642\uFE0F");
        put("woman-bowing", "\uD83D\uDE47\u200D\u2640\uFE0F");
        put("man-bowing", "\uD83D\uDE47\u200D\u2642\uFE0F");
        put("bow", "\uD83D\uDE47\u200D\u2642\uFE0F");
        put("see_no_evil", "\uD83D\uDE48");
        put("hear_no_evil", "\uD83D\uDE49");
        put("speak_no_evil", "\uD83D\uDE4A");
        put("woman-raising-hand", "\uD83D\uDE4B\u200D\u2640\uFE0F");
        put("raising_hand", "\uD83D\uDE4B\u200D\u2640\uFE0F");
        put("man-raising-hand", "\uD83D\uDE4B\u200D\u2642\uFE0F");
        put("raised_hands", "\uD83D\uDE4C");
        put("woman-frowning", "\uD83D\uDE4D\u200D\u2640\uFE0F");
        put("person_frowning", "\uD83D\uDE4D\u200D\u2640\uFE0F");
        put("man-frowning", "\uD83D\uDE4D\u200D\u2642\uFE0F");
        put("woman-pouting", "\uD83D\uDE4E\u200D\u2640\uFE0F");
        put("person_with_pouting_face", "\uD83D\uDE4E\u200D\u2640\uFE0F");
        put("man-pouting", "\uD83D\uDE4E\u200D\u2642\uFE0F");
        put("pray", "\uD83D\uDE4F");
        put("rocket", "\uD83D\uDE80");
        put("helicopter", "\uD83D\uDE81");
        put("steam_locomotive", "\uD83D\uDE82");
        put("railway_car", "\uD83D\uDE83");
        put("bullettrain_side", "\uD83D\uDE84");
        put("bullettrain_front", "\uD83D\uDE85");
        put("train2", "\uD83D\uDE86");
        put("metro", "\uD83D\uDE87");
        put("light_rail", "\uD83D\uDE88");
        put("station", "\uD83D\uDE89");
        put("tram", "\uD83D\uDE8A");
        put("train", "\uD83D\uDE8B");
        put("bus", "\uD83D\uDE8C");
        put("oncoming_bus", "\uD83D\uDE8D");
        put("trolleybus", "\uD83D\uDE8E");
        put("busstop", "\uD83D\uDE8F");
        put("minibus", "\uD83D\uDE90");
        put("ambulance", "\uD83D\uDE91");
        put("fire_engine", "\uD83D\uDE92");
        put("police_car", "\uD83D\uDE93");
        put("oncoming_police_car", "\uD83D\uDE94");
        put("taxi", "\uD83D\uDE95");
        put("oncoming_taxi", "\uD83D\uDE96");
        put("car", "\uD83D\uDE97");
        put("red_car", "\uD83D\uDE97");
        put("oncoming_automobile", "\uD83D\uDE98");
        put("blue_car", "\uD83D\uDE99");
        put("truck", "\uD83D\uDE9A");
        put("articulated_lorry", "\uD83D\uDE9B");
        put("tractor", "\uD83D\uDE9C");
        put("monorail", "\uD83D\uDE9D");
        put("mountain_railway", "\uD83D\uDE9E");
        put("suspension_railway", "\uD83D\uDE9F");
        put("mountain_cableway", "\uD83D\uDEA0");
        put("aerial_tramway", "\uD83D\uDEA1");
        put("ship", "\uD83D\uDEA2");
        put("woman-rowing-boat", "\uD83D\uDEA3\u200D\u2640\uFE0F");
        put("man-rowing-boat", "\uD83D\uDEA3\u200D\u2642\uFE0F");
        put("rowboat", "\uD83D\uDEA3\u200D\u2642\uFE0F");
        put("speedboat", "\uD83D\uDEA4");
        put("traffic_light", "\uD83D\uDEA5");
        put("vertical_traffic_light", "\uD83D\uDEA6");
        put("construction", "\uD83D\uDEA7");
        put("rotating_light", "\uD83D\uDEA8");
        put("triangular_flag_on_post", "\uD83D\uDEA9");
        put("door", "\uD83D\uDEAA");
        put("no_entry_sign", "\uD83D\uDEAB");
        put("smoking", "\uD83D\uDEAC");
        put("no_smoking", "\uD83D\uDEAD");
        put("put_litter_in_its_place", "\uD83D\uDEAE");
        put("do_not_litter", "\uD83D\uDEAF");
        put("potable_water", "\uD83D\uDEB0");
        put("non-potable_water", "\uD83D\uDEB1");
        put("bike", "\uD83D\uDEB2");
        put("no_bicycles", "\uD83D\uDEB3");
        put("woman-biking", "\uD83D\uDEB4\u200D\u2640\uFE0F");
        put("man-biking", "\uD83D\uDEB4\u200D\u2642\uFE0F");
        put("bicyclist", "\uD83D\uDEB4\u200D\u2642\uFE0F");
        put("woman-mountain-biking", "\uD83D\uDEB5\u200D\u2640\uFE0F");
        put("man-mountain-biking", "\uD83D\uDEB5\u200D\u2642\uFE0F");
        put("mountain_bicyclist", "\uD83D\uDEB5\u200D\u2642\uFE0F");
        put("woman-walking", "\uD83D\uDEB6\u200D\u2640\uFE0F");
        put("man-walking", "\uD83D\uDEB6\u200D\u2642\uFE0F");
        put("walking", "\uD83D\uDEB6\u200D\u2642\uFE0F");
        put("no_pedestrians", "\uD83D\uDEB7");
        put("children_crossing", "\uD83D\uDEB8");
        put("mens", "\uD83D\uDEB9");
        put("womens", "\uD83D\uDEBA");
        put("restroom", "\uD83D\uDEBB");
        put("baby_symbol", "\uD83D\uDEBC");
        put("toilet", "\uD83D\uDEBD");
        put("wc", "\uD83D\uDEBE");
        put("shower", "\uD83D\uDEBF");
        put("bath", "\uD83D\uDEC0");
        put("bathtub", "\uD83D\uDEC1");
        put("passport_control", "\uD83D\uDEC2");
        put("customs", "\uD83D\uDEC3");
        put("baggage_claim", "\uD83D\uDEC4");
        put("left_luggage", "\uD83D\uDEC5");
        put("couch_and_lamp", "\uD83D\uDECB\uFE0F");
        put("sleeping_accommodation", "\uD83D\uDECC");
        put("shopping_bags", "\uD83D\uDECD\uFE0F");
        put("bellhop_bell", "\uD83D\uDECE\uFE0F");
        put("bed", "\uD83D\uDECF\uFE0F");
        put("place_of_worship", "\uD83D\uDED0");
        put("octagonal_sign", "\uD83D\uDED1");
        put("shopping_trolley", "\uD83D\uDED2");
        put("hammer_and_wrench", "\uD83D\uDEE0\uFE0F");
        put("shield", "\uD83D\uDEE1\uFE0F");
        put("oil_drum", "\uD83D\uDEE2\uFE0F");
        put("motorway", "\uD83D\uDEE3\uFE0F");
        put("railway_track", "\uD83D\uDEE4\uFE0F");
        put("motor_boat", "\uD83D\uDEE5\uFE0F");
        put("small_airplane", "\uD83D\uDEE9\uFE0F");
        put("airplane_departure", "\uD83D\uDEEB");
        put("airplane_arriving", "\uD83D\uDEEC");
        put("satellite", "\uD83D\uDEF0\uFE0F");
        put("passenger_ship", "\uD83D\uDEF3\uFE0F");
        put("scooter", "\uD83D\uDEF4");
        put("motor_scooter", "\uD83D\uDEF5");
        put("canoe", "\uD83D\uDEF6");
        put("sled", "\uD83D\uDEF7");
        put("flying_saucer", "\uD83D\uDEF8");
        put("zipper_mouth_face", "\uD83E\uDD10");
        put("money_mouth_face", "\uD83E\uDD11");
        put("face_with_thermometer", "\uD83E\uDD12");
        put("nerd_face", "\uD83E\uDD13");
        put("thinking_face", "\uD83E\uDD14");
        put("face_with_head_bandage", "\uD83E\uDD15");
        put("robot_face", "\uD83E\uDD16");
        put("hugging_face", "\uD83E\uDD17");
        put("the_horns", "\uD83E\uDD18");
        put("sign_of_the_horns", "\uD83E\uDD18");
        put("call_me_hand", "\uD83E\uDD19");
        put("raised_back_of_hand", "\uD83E\uDD1A");
        put("left-facing_fist", "\uD83E\uDD1B");
        put("right-facing_fist", "\uD83E\uDD1C");
        put("handshake", "\uD83E\uDD1D");
        put("hand_with_index_and_middle_fingers_crossed", "\uD83E\uDD1E");
        put("i_love_you_hand_sign", "\uD83E\uDD1F");
        put("face_with_cowboy_hat", "\uD83E\uDD20");
        put("clown_face", "\uD83E\uDD21");
        put("nauseated_face", "\uD83E\uDD22");
        put("rolling_on_the_floor_laughing", "\uD83E\uDD23");
        put("drooling_face", "\uD83E\uDD24");
        put("lying_face", "\uD83E\uDD25");
        put("woman-facepalming", "\uD83E\uDD26\u200D\u2640\uFE0F");
        put("man-facepalming", "\uD83E\uDD26\u200D\u2642\uFE0F");
        put("face_palm", "\uD83E\uDD26");
        put("sneezing_face", "\uD83E\uDD27");
        put("face_with_one_eyebrow_raised", "\uD83E\uDD28");
        put("grinning_face_with_star_eyes", "\uD83E\uDD29");
        put("grinning_face_with_one_large_and_one_small_eye", "\uD83E\uDD2A");
        put("face_with_finger_covering_closed_lips", "\uD83E\uDD2B");
        put("serious_face_with_symbols_covering_mouth", "\uD83E\uDD2C");
        put("smiling_face_with_smiling_eyes_and_hand_covering_mouth", "\uD83E\uDD2D");
        put("face_with_open_mouth_vomiting", "\uD83E\uDD2E");
        put("shocked_face_with_exploding_head", "\uD83E\uDD2F");
        put("pregnant_woman", "\uD83E\uDD30");
        put("breast-feeding", "\uD83E\uDD31");
        put("palms_up_together", "\uD83E\uDD32");
        put("selfie", "\uD83E\uDD33");
        put("prince", "\uD83E\uDD34");
        put("man_in_tuxedo", "\uD83E\uDD35");
        put("mother_christmas", "\uD83E\uDD36");
        put("woman-shrugging", "\uD83E\uDD37\u200D\u2640\uFE0F");
        put("man-shrugging", "\uD83E\uDD37\u200D\u2642\uFE0F");
        put("shrug", "\uD83E\uDD37");
        put("woman-cartwheeling", "\uD83E\uDD38\u200D\u2640\uFE0F");
        put("man-cartwheeling", "\uD83E\uDD38\u200D\u2642\uFE0F");
        put("person_doing_cartwheel", "\uD83E\uDD38");
        put("woman-juggling", "\uD83E\uDD39\u200D\u2640\uFE0F");
        put("man-juggling", "\uD83E\uDD39\u200D\u2642\uFE0F");
        put("juggling", "\uD83E\uDD39");
        put("fencer", "\uD83E\uDD3A");
        put("woman-wrestling", "\uD83E\uDD3C\u200D\u2640\uFE0F");
        put("man-wrestling", "\uD83E\uDD3C\u200D\u2642\uFE0F");
        put("wrestlers", "\uD83E\uDD3C");
        put("woman-playing-water-polo", "\uD83E\uDD3D\u200D\u2640\uFE0F");
        put("man-playing-water-polo", "\uD83E\uDD3D\u200D\u2642\uFE0F");
        put("water_polo", "\uD83E\uDD3D");
        put("woman-playing-handball", "\uD83E\uDD3E\u200D\u2640\uFE0F");
        put("man-playing-handball", "\uD83E\uDD3E\u200D\u2642\uFE0F");
        put("handball", "\uD83E\uDD3E");
        put("wilted_flower", "\uD83E\uDD40");
        put("drum_with_drumsticks", "\uD83E\uDD41");
        put("clinking_glasses", "\uD83E\uDD42");
        put("tumbler_glass", "\uD83E\uDD43");
        put("spoon", "\uD83E\uDD44");
        put("goal_net", "\uD83E\uDD45");
        put("first_place_medal", "\uD83E\uDD47");
        put("second_place_medal", "\uD83E\uDD48");
        put("third_place_medal", "\uD83E\uDD49");
        put("boxing_glove", "\uD83E\uDD4A");
        put("martial_arts_uniform", "\uD83E\uDD4B");
        put("curling_stone", "\uD83E\uDD4C");
        put("croissant", "\uD83E\uDD50");
        put("avocado", "\uD83E\uDD51");
        put("cucumber", "\uD83E\uDD52");
        put("bacon", "\uD83E\uDD53");
        put("potato", "\uD83E\uDD54");
        put("carrot", "\uD83E\uDD55");
        put("baguette_bread", "\uD83E\uDD56");
        put("green_salad", "\uD83E\uDD57");
        put("shallow_pan_of_food", "\uD83E\uDD58");
        put("stuffed_flatbread", "\uD83E\uDD59");
        put("egg", "\uD83E\uDD5A");
        put("glass_of_milk", "\uD83E\uDD5B");
        put("peanuts", "\uD83E\uDD5C");
        put("kiwifruit", "\uD83E\uDD5D");
        put("pancakes", "\uD83E\uDD5E");
        put("dumpling", "\uD83E\uDD5F");
        put("fortune_cookie", "\uD83E\uDD60");
        put("takeout_box", "\uD83E\uDD61");
        put("chopsticks", "\uD83E\uDD62");
        put("bowl_with_spoon", "\uD83E\uDD63");
        put("cup_with_straw", "\uD83E\uDD64");
        put("coconut", "\uD83E\uDD65");
        put("broccoli", "\uD83E\uDD66");
        put("pie", "\uD83E\uDD67");
        put("pretzel", "\uD83E\uDD68");
        put("cut_of_meat", "\uD83E\uDD69");
        put("sandwich", "\uD83E\uDD6A");
        put("canned_food", "\uD83E\uDD6B");
        put("crab", "\uD83E\uDD80");
        put("lion_face", "\uD83E\uDD81");
        put("scorpion", "\uD83E\uDD82");
        put("turkey", "\uD83E\uDD83");
        put("unicorn_face", "\uD83E\uDD84");
        put("eagle", "\uD83E\uDD85");
        put("duck", "\uD83E\uDD86");
        put("bat", "\uD83E\uDD87");
        put("shark", "\uD83E\uDD88");
        put("owl", "\uD83E\uDD89");
        put("fox_face", "\uD83E\uDD8A");
        put("butterfly", "\uD83E\uDD8B");
        put("deer", "\uD83E\uDD8C");
        put("gorilla", "\uD83E\uDD8D");
        put("lizard", "\uD83E\uDD8E");
        put("rhinoceros", "\uD83E\uDD8F");
        put("shrimp", "\uD83E\uDD90");
        put("squid", "\uD83E\uDD91");
        put("giraffe_face", "\uD83E\uDD92");
        put("zebra_face", "\uD83E\uDD93");
        put("hedgehog", "\uD83E\uDD94");
        put("sauropod", "\uD83E\uDD95");
        put("t-rex", "\uD83E\uDD96");
        put("cricket", "\uD83E\uDD97");
        put("cheese_wedge", "\uD83E\uDDC0");
        put("face_with_monocle", "\uD83E\uDDD0");
        put("adult", "\uD83E\uDDD1");
        put("child", "\uD83E\uDDD2");
        put("older_adult", "\uD83E\uDDD3");
        put("bearded_person", "\uD83E\uDDD4");
        put("person_with_headscarf", "\uD83E\uDDD5");
        put("woman_in_steamy_room", "\uD83E\uDDD6\u200D\u2640\uFE0F");
        put("man_in_steamy_room", "\uD83E\uDDD6\u200D\u2642\uFE0F");
        put("person_in_steamy_room", "\uD83E\uDDD6\u200D\u2642\uFE0F");
        put("woman_climbing", "\uD83E\uDDD7\u200D\u2640\uFE0F");
        put("person_climbing", "\uD83E\uDDD7\u200D\u2640\uFE0F");
        put("man_climbing", "\uD83E\uDDD7\u200D\u2642\uFE0F");
        put("woman_in_lotus_position", "\uD83E\uDDD8\u200D\u2640\uFE0F");
        put("person_in_lotus_position", "\uD83E\uDDD8\u200D\u2640\uFE0F");
        put("man_in_lotus_position", "\uD83E\uDDD8\u200D\u2642\uFE0F");
        put("female_mage", "\uD83E\uDDD9\u200D\u2640\uFE0F");
        put("mage", "\uD83E\uDDD9\u200D\u2640\uFE0F");
        put("male_mage", "\uD83E\uDDD9\u200D\u2642\uFE0F");
        put("female_fairy", "\uD83E\uDDDA\u200D\u2640\uFE0F");
        put("fairy", "\uD83E\uDDDA\u200D\u2640\uFE0F");
        put("male_fairy", "\uD83E\uDDDA\u200D\u2642\uFE0F");
        put("female_vampire", "\uD83E\uDDDB\u200D\u2640\uFE0F");
        put("vampire", "\uD83E\uDDDB\u200D\u2640\uFE0F");
        put("male_vampire", "\uD83E\uDDDB\u200D\u2642\uFE0F");
        put("mermaid", "\uD83E\uDDDC\u200D\u2640\uFE0F");
        put("merman", "\uD83E\uDDDC\u200D\u2642\uFE0F");
        put("merperson", "\uD83E\uDDDC\u200D\u2642\uFE0F");
        put("female_elf", "\uD83E\uDDDD\u200D\u2640\uFE0F");
        put("male_elf", "\uD83E\uDDDD\u200D\u2642\uFE0F");
        put("elf", "\uD83E\uDDDD\u200D\u2642\uFE0F");
        put("female_genie", "\uD83E\uDDDE\u200D\u2640\uFE0F");
        put("male_genie", "\uD83E\uDDDE\u200D\u2642\uFE0F");
        put("genie", "\uD83E\uDDDE\u200D\u2642\uFE0F");
        put("female_zombie", "\uD83E\uDDDF\u200D\u2640\uFE0F");
        put("male_zombie", "\uD83E\uDDDF\u200D\u2642\uFE0F");
        put("zombie", "\uD83E\uDDDF\u200D\u2642\uFE0F");
        put("brain", "\uD83E\uDDE0");
        put("orange_heart", "\uD83E\uDDE1");
        put("billed_cap", "\uD83E\uDDE2");
        put("scarf", "\uD83E\uDDE3");
        put("gloves", "\uD83E\uDDE4");
        put("coat", "\uD83E\uDDE5");
        put("socks", "\uD83E\uDDE6");
        put("bangbang", "\u203C\uFE0F");
        put("interrobang", "\u2049\uFE0F");
        put("tm", "\u2122\uFE0F");
        put("information_source", "\u2139\uFE0F");
        put("left_right_arrow", "\u2194\uFE0F");
        put("arrow_up_down", "\u2195\uFE0F");
        put("arrow_upper_left", "\u2196\uFE0F");
        put("arrow_upper_right", "\u2197\uFE0F");
        put("arrow_lower_right", "\u2198\uFE0F");
        put("arrow_lower_left", "\u2199\uFE0F");
        put("leftwards_arrow_with_hook", "\u21A9\uFE0F");
        put("arrow_right_hook", "\u21AA\uFE0F");
        put("watch", "\u231A");
        put("hourglass", "\u231B");
        put("keyboard", "\u2328\uFE0F");
        put("eject", "\u23CF\uFE0F");
        put("fast_forward", "\u23E9");
        put("rewind", "\u23EA");
        put("arrow_double_up", "\u23EB");
        put("arrow_double_down", "\u23EC");
        put("black_right_pointing_double_triangle_with_vertical_bar", "\u23ED\uFE0F");
        put("black_left_pointing_double_triangle_with_vertical_bar", "\u23EE\uFE0F");
        put("black_right_pointing_triangle_with_double_vertical_bar", "\u23EF\uFE0F");
        put("alarm_clock", "\u23F0");
        put("stopwatch", "\u23F1\uFE0F");
        put("timer_clock", "\u23F2\uFE0F");
        put("hourglass_flowing_sand", "\u23F3");
        put("double_vertical_bar", "\u23F8\uFE0F");
        put("black_square_for_stop", "\u23F9\uFE0F");
        put("black_circle_for_record", "\u23FA\uFE0F");
        put("m", "\u24C2\uFE0F");
        put("black_small_square", "\u25AA\uFE0F");
        put("white_small_square", "\u25AB\uFE0F");
        put("arrow_forward", "\u25B6\uFE0F");
        put("arrow_backward", "\u25C0\uFE0F");
        put("white_medium_square", "\u25FB\uFE0F");
        put("black_medium_square", "\u25FC\uFE0F");
        put("white_medium_small_square", "\u25FD");
        put("black_medium_small_square", "\u25FE");
        put("sunny", "\u2600\uFE0F");
        put("cloud", "\u2601\uFE0F");
        put("umbrella", "\u2602\uFE0F");
        put("snowman", "\u2603\uFE0F");
        put("comet", "\u2604\uFE0F");
        put("phone", "\u260E\uFE0F");
        put("telephone", "\u260E\uFE0F");
        put("ballot_box_with_check", "\u2611\uFE0F");
        put("shamrock", "\u2618\uFE0F");
        put("point_up", "\u261D\uFE0F");
        put("skull_and_crossbones", "\u2620\uFE0F");
        put("radioactive_sign", "\u2622\uFE0F");
        put("biohazard_sign", "\u2623\uFE0F");
        put("orthodox_cross", "\u2626\uFE0F");
        put("star_and_crescent", "\u262A\uFE0F");
        put("peace_symbol", "\u262E\uFE0F");
        put("yin_yang", "\u262F\uFE0F");
        put("wheel_of_dharma", "\u2638\uFE0F");
        put("white_frowning_face", "\u2639\uFE0F");
        put("relaxed", "\u263A\uFE0F");
        put("female_sign", "\u2640\uFE0F");
        put("male_sign", "\u2642\uFE0F");
        put("gemini", "\u264A");
        put("cancer", "\u264B");
        put("leo", "\u264C");
        put("virgo", "\u264D");
        put("libra", "\u264E");
        put("scorpius", "\u264F");
        put("spades", "\u2660\uFE0F");
        put("clubs", "\u2663\uFE0F");
        put("hearts", "\u2665\uFE0F");
        put("diamonds", "\u2666\uFE0F");
        put("hotsprings", "\u2668\uFE0F");
        put("recycle", "\u267B\uFE0F");
        put("wheelchair", "\u267F");
        put("hammer_and_pick", "\u2692\uFE0F");
        put("crossed_swords", "\u2694\uFE0F");
        put("staff_of_aesculapius", "\u2695\uFE0F");
        put("scales", "\u2696\uFE0F");
        put("alembic", "\u2697\uFE0F");
        put("gear", "\u2699\uFE0F");
        put("atom_symbol", "\u269B\uFE0F");
        put("fleur_de_lis", "\u269C\uFE0F");
        put("warning", "\u26A0\uFE0F");
        put("zap", "\u26A1");
        put("white_circle", "\u26AA");
        put("black_circle", "\u26AB");
        put("coffin", "\u26B0\uFE0F");
        put("funeral_urn", "\u26B1\uFE0F");
        put("soccer", "\u26BD");
        put("baseball", "\u26BE");
        put("snowman_without_snow", "\u26C4");
        put("partly_sunny", "\u26C5");
        put("thunder_cloud_and_rain", "\u26C8\uFE0F");
        put("ophiuchus", "\u26CE");
        put("pick", "\u26CF\uFE0F");
        put("helmet_with_white_cross", "\u26D1\uFE0F");
        put("chains", "\u26D3\uFE0F");
        put("no_entry", "\u26D4");
        put("shinto_shrine", "\u26E9\uFE0F");
        put("church", "\u26EA");
        put("mountain", "\u26F0\uFE0F");
        put("umbrella_on_ground", "\u26F1\uFE0F");
        put("fountain", "\u26F2");
        put("golf", "\u26F3");
        put("ferry", "\u26F4\uFE0F");
        put("boat", "\u26F5");
        put("sailboat", "\u26F5");
        put("skier", "\u26F7\uFE0F");
        put("ice_skate", "\u26F8\uFE0F");
        put("woman-bouncing-ball", "\u26F9\uFE0F\u200D\u2640\uFE0F");
        put("man-bouncing-ball", "\u26F9\uFE0F\u200D\u2642\uFE0F");
        put("person_with_ball", "\u26F9\uFE0F\u200D\u2642\uFE0F");
        put("tent", "\u26FA");
        put("fuelpump", "\u26FD");
        put("scissors", "\u2702\uFE0F");
        put("airplane", "\u2708\uFE0F");
        put("email", "\u2709\uFE0F");
        put("envelope", "\u2709\uFE0F");
        put("fist", "\u270A");
        put("hand", "\u270B");
        put("raised_hand", "\u270B");
        put("v", "\u270C\uFE0F");
        put("writing_hand", "\u270D\uFE0F");
        put("pencil2", "\u270F\uFE0F");
        put("black_nib", "\u2712\uFE0F");
        put("heavy_check_mark", "\u2714\uFE0F");
        put("heavy_multiplication_x", "\u2716\uFE0F");
        put("latin_cross", "\u271D\uFE0F");
        put("star_of_david", "\u2721\uFE0F");
        put("eight_spoked_asterisk", "\u2733\uFE0F");
        put("eight_pointed_black_star", "\u2734\uFE0F");
        put("snowflake", "\u2744\uFE0F");
        put("sparkle", "\u2747\uFE0F");
        put("x", "\u274C");
        put("negative_squared_cross_mark", "\u274E");
        put("heavy_heart_exclamation_mark_ornament", "\u2763\uFE0F");
        put("heart", "\u2764\uFE0F");
        put("arrow_right", "\u27A1\uFE0F");
        put("curly_loop", "\u27B0");
        put("loop", "\u27BF");
        put("arrow_heading_up", "\u2934\uFE0F");
        put("arrow_heading_down", "\u2935\uFE0F");
        put("arrow_left", "\u2B05\uFE0F");
        put("arrow_up", "\u2B06\uFE0F");
        put("arrow_down", "\u2B07\uFE0F");
        put("black_large_square", "\u2B1B");
        put("white_large_square", "\u2B1C");
        put("star", "\u2B50");
        put("o", "\u2B55");
        put("wavy_dash", "\u3030\uFE0F");
        put("part_alternation_mark", "\u303D\uFE0F");
        put("congratulations", "\u3297\uFE0F");
        put("secret", "\u3299\uFE0F");

        //IRCCloud aliases
        put("like", get("+1"));
        put("doge", get("dog"));
        put("aubergine", get("eggplant"));
        put("gust_of_wind", get("dash"));
        put("party_popper", get("tada"));
        put("shock", get("scream"));
        put("atom", get("atom_symbol"));
        put("<3", get("heart"));
        put("</3", get("broken_heart"));
        put(")", get("smiley"));
        put("')", get("smiley"));
        put("-)", get("disappointed"));
        put("(", get("cry"));
        put("_(", get("sob"));
        put("loudly_crying_face", get("sob"));
        put("sad_tears", get("sob"));
        put("bawl", get("sob"));
        put(";)", get("wink"));
        put(";p", get("stuck_out_tongue_winking_eye"));
        put("simple_smile", ":)");
        put("slightly_smiling_face", ":)");
        put("ufo", get("flying_saucer"));
        put("throwing_up", get("face_with_open_mouth_vomiting"));
        put("being_sick", get("face_with_open_mouth_vomiting"));
        put("sh", get("face_with_finger_covering_closed_lips"));
        put("oops", get("smiling_face_with_smiling_eyes_and_hand_covering_mouth"));
        put("female_wizard", get("female_mage"));
        put("male_wizard", get("male_mage"));
        put("brontosaurus", get("sauropod"));
        put("diplodocus", get("sauropod"));
        put("tyrannosaurus", get("t-rex"));
        put("steak", get("cut_of_meat"));
        put("soup_tin", get("canned_food"));
        put("baseball_cap", get("billed_cap"));
        put("female_yoga", get("woman_in_lotus_position"));
        put("male_yoga", get("man_in_lotus_position"));
        put("female_sauna", get("woman_in_steamy_room"));
        put("male_sauna", get("man_in_steamy_room"));
        put("hijab", get("person_with_headscarf"));
        put("crazy_face", get("grinning_face_with_one_large_and_one_small_eye"));
        put("diamond", get("gem"));
        put("ladybird", get("beetle"));
        put("ladybug", get("beetle"));
        put("ladybeetle", get("beetle"));
        put("coccinellid", get("beetle"));
        put("cursing", get("serious_face_with_symbols_covering_mouth"));
        put("swearing", get("serious_face_with_symbols_covering_mouth"));
        put("fuck", get("serious_face_with_symbols_covering_mouth"));
        put("angry_swearing", get("serious_face_with_symbols_covering_mouth"));
        put("mad_swearing", get("serious_face_with_symbols_covering_mouth"));
        put("pissed_off", get("serious_face_with_symbols_covering_mouth"));
        put("angel_face", get("innocent"));
        put("smiling_devil", get("smiling_imp"));
        put("frowning_devil", get("imp"));
        put("mad_rage", get("rage"));
        put("angry_rage", get("rage"));
        put("mad", get("angry"));
        put("steam_train", get("steam_locomotive"));
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

        if (linkify) {
            Linkify.addLinks(output, WEB_URL, null, new MatchFilter() {
                public final boolean acceptMatch(CharSequence s, int start, int end) {
                    if (start >= 6 && s.subSequence(start - 6, end).toString().toLowerCase().startsWith("irc://"))
                        return false;
                    if (start >= 7 && s.subSequence(start - 7, end).toString().toLowerCase().startsWith("ircs://"))
                        return false;
                    if (start >= 1 && s.subSequence(start - 1, end).toString().matches(pattern))
                        return false;
                    if(s.subSequence(start, end).toString().matches("[0-9\\.]+"))
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

                        boolean isImageEnt = false;
                        if (entities != null && entities.has("files")) {
                            if (NetworkConnection.file_uri_template != null) {
                                UriTemplate template = UriTemplate.fromTemplate(NetworkConnection.file_uri_template);
                                for (JsonNode file : entities.get("files")) {
                                    String file_url = template.set("id", file.get("id").asText()).expand();
                                    String u = file_url.toLowerCase();
                                    String extension = file.hasNonNull("extension")?file.get("extension").asText():("." + file.get("mime_type").asText().substring(6));
                                    isImageEnt = ((lower.equals(u) || lower.startsWith(u + "/")) && file.get("mime_type").asText().startsWith("image/"));
                                    if (isImageEnt) {
                                        if(!lower.endsWith(extension.toLowerCase()))
                                            url = template.set("name", file.get("id").asText() + extension).expand();
                                        break;
                                    }
                                }
                            }
                        }

                        if (isImageEnt || ImageList.isImageURL(lower)) {
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

                        boolean isVideoEnt = false;
                        if (entities != null && entities.has("files")) {
                            if (NetworkConnection.file_uri_template != null) {
                                UriTemplate template = UriTemplate.fromTemplate(NetworkConnection.file_uri_template);
                                for (JsonNode file : entities.get("files")) {
                                    String file_url = template.set("id", file.get("id").asText()).expand();
                                    String u = file_url.toLowerCase();
                                    String mime = file.get("mime_type").asText();
                                    isVideoEnt = ((lower.equals(u) || lower.startsWith(u + "/")) && (
                                            mime.equals("video/mp4") ||
                                                    mime.equals("video/webm") ||
                                                    mime.equals("video/3gpp")
                                    ));
                                    if (isVideoEnt) {
                                        url = file_url;
                                        break;
                                    }
                                }
                            }
                        }

                        if (isVideoEnt || lower.matches("(^.*/.*\\.3gpp?)|(^.*/.*\\.mp4$)|(^.*/.*\\.m4v$)|(^.*/.*\\.webm$)") ||
                                url.toLowerCase().matches("(^https?://(www\\.)?facebook\\.com/video\\.php\\?.*$)|" +
                                        "(^https?://(www\\.)?facebook\\.com/.*/videos/[0-9]+/?)")) {
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
                                        return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.PASTE_SCHEME) + "://" + paste_url.substring(7) + "?id=" + paste.get("id").asText() + "&own_paste=" + (paste.has("own_paste") && paste.get("own_paste").asBoolean() ? "1" : "0");
                                    else
                                        return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.PASTE_SCHEME) + "://" + paste_url.substring(8) + "?id=" + paste.get("id").asText() + "&own_paste=" + (paste.has("own_paste") && paste.get("own_paste").asBoolean() ? "1" : "0");
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

        }
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
                    if(m.position >= 0 && m.position + m.length < msg.length())
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
}
