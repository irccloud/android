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

package com.irccloud.android.test;

import android.test.AndroidTestCase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irccloud.android.Ignore;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Server;

import java.util.ArrayList;

public class MessageTypeTests extends AndroidTestCase {

	public void testMessageTypes() {
        //JSON.stringify(Object.keys(cbv().scroll.log.lineRenderer.messageHandlers))
        String types[] = new String[] { "channel_topic","buffer_msg","newsflash","invited","channel_invite","callerid","buffer_me_msg","twitch_hosttarget_start","twitch_hosttarget_stop","twitch_usernotice","your_unique_id","server_welcome","server_yourhost","server_created","myinfo","version","server_luserclient","server_luserop","server_luserunknown","server_luserchannels","server_luserconns","server_luserme","server_n_global","server_n_local","server_snomask","self_details","hidden_host_set","codepage","logged_in_as","logged_out","nick_locked","server_motdstart","server_motd","server_endofmotd","server_nomotd","motd_response","info_response","generic_server_info","error","unknown_umode","notice","wallops","too_fast","no_bots","bad_ping","nickname_in_use","invalid_nick_change","save_nick","nick_collision","bad_channel_mask","you_are_operator","sasl_success","sasl_fail","sasl_too_long","sasl_aborted","sasl_already","cap_ls","cap_list","cap_new","cap_del","cap_req","cap_ack","cap_nak","cap_raw","cap_invalid","rehashed_config","inviting_to_channel","invite_notify","user_chghost","channel_name_change","knock","kill_deny","chan_own_priv_needed","not_for_halfops","link_channel","chan_forbidden","joined_channel","you_joined_channel","parted_channel","you_parted_channel","kicked_channel","you_kicked_channel","quit","quit_server","kill","banned","socket_closed","connecting_failed","connecting_cancelled","wait","starircd_welcome","nickchange","you_nickchange","channel_mode_list_change","user_channel_mode","user_mode","channel_mode","channel_mode_is","channel_url","zurna_motd","loaded_module","unloaded_module","unhandled_line","unparsed_line","msg_services","ambiguous_error_message","list_usage","list_syntax","who_syntax","services_down","help_topics_start","help_topics","help_topics_end","helphdr","helpop","helptlr","helphlp","helpfwd","helpign","stats","statslinkinfo","statscommands","statscline","statsnline","statsiline","statskline","statsqline","statsyline","endofstats","statsbline","statsgline","statstline","statseline","statsvline","statslline","statsuptime","statsoline","statshline","statssline","statsuline","statsdebug","spamfilter","text","target_callerid","target_notified","time","admin_info","watch_status","sqline_nick" };

		checkTypes(types);
	}

    public void testBufferOverlayTypes() {
        //BufferOverlayHandler.js
        String types[] = new String[] { "who_response","who_special_response","whowas_response","whois_response","ignore_list","monitor_list","a_list","quiet_list","invited_list","invite_list","q_list","ircops","ban_list","modules_list","ban_exception_list","accept_list","map_list","silence_list","links_response","query_too_long","input_too_long","try_again","list_response_fetching","list_response_toomany","list_response","remote_isupport_params","names_reply","notice","services_down","time","ison","trace_response","monitor_offline","monitor_online","watch_status","channel_query","userhost","channel_invite" };

        checkTypes(types);
    }

    public void testConnectionPromptTypes() {
        //ConnectionPromptHandler.js
        String types[] = new String[] { "invalid_nick","no_such_channel","no_such_nick","bad_channel_key","bad_channel_name","need_registered_nick","blocked_channel","invite_only_chan","channel_full","channel_key_set","banned_from_channel","oper_only","invalid_nick_change","nickname_in_use","no_nick_change","no_messages_from_non_registered","not_registered","already_registered","too_many_channels","too_many_targets","no_such_server","unknown_command","unknown_error","help_not_found","accept_full","accept_exists","accept_not","nick_collision","nick_too_fast","save_nick","unknown_mode","user_not_in_channel","need_more_params","users_dont_match","chan_privs_needed","channame_in_use","users_disabled","invalid_operator_password","flood_warning","privs_needed","operator_fail","not_on_channel","ban_on_chan","cannot_send_to_chan","user_on_channel","pong","no_nick_given","no_text_to_send","no_origin","only_servers_can_change_mode","not_for_halfops","silence","monitor_full","no_channel_topic","channel_topic_is","mlock_restricted","cannot_do_cmd","secure_only_chan","cannot_change_chan_mode","knock_delivered","too_many_knocks","chan_open","knock_on_chan","knock_disabled","cannotknock","ownmode","nossl","link_channel","redirect_error","invalid_flood","join_flood","metadata_limit","metadata_targetinvalid","metadata_nomatchingkey","metadata_keyinvalid","metadata_keynotset","metadata_keynopermission","metadata_toomanysubs" };

        checkTypes(types);
    }

    private void checkTypes(String types[]) {
        ArrayList<String> missingTypes = new ArrayList<>();

        for(String type : types) {
            if(!NetworkConnection.getInstance().parserMap.containsKey(type))
                missingTypes.add(type);
        }

        assertTrue("Missing handlers: " + missingTypes.toString(), missingTypes.size() == 0);
    }
}
