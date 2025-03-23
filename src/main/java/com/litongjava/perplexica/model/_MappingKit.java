package com.litongjava.perplexica.model;

import com.litongjava.db.activerecord.ActiveRecordPlugin;

/**
 * Generated by JFinal, do not modify this file.
 * <pre>
 * Example:
 * public void configPlugin(Plugins me) {
 *     ActiveRecordPlugin arp = new ActiveRecordPlugin(...);
 *     _MappingKit.mapping(arp);
 *     me.add(arp);
 * }
 * </pre>
 */
public class _MappingKit {
	
	public static void mapping(ActiveRecordPlugin arp) {
		arp.addMapping("max_search_chat_message", "id", MaxSearchChatMessage.class);
		arp.addMapping("max_search_chat_session", "id", MaxSearchChatSession.class);
	}
}


