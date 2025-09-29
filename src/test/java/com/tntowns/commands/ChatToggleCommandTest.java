package com.tntowns.commands;

import com.tntowns.model.ChatChannel;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatToggleCommandTest {

	@Test
	void chatToggle_sets_channel() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		ChatToggleCommand cmd = new ChatToggleCommand(townService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("tntownchat");

		boolean ok = cmd.onCommand(p, bukkitCmd, "tntownchat", new String[]{"town"});
		assertTrue(ok);
		verify(townService.getOrCreateResident(p)).setChatChannel(ChatChannel.TOWN);
		assertTrue(messages.stream().anyMatch(s -> s.contains("Chat channel set to")));
	}

	@Test
	void chatToggle_unknown_channel_shows_error() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		ChatToggleCommand cmd = new ChatToggleCommand(townService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("tntownchat");
		boolean ok = cmd.onCommand(p, bukkitCmd, "tntownchat", new String[]{"nope"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.contains("Unknown channel.")));
	}

	@Test
	void chatToggle_non_player_rejected() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		ChatToggleCommand cmd = new ChatToggleCommand(townService);
		org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("tntownchat");
		boolean ok = cmd.onCommand(sender, bukkitCmd, "tntownchat", new String[]{"global"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("Players only.")));
	}

	@Test
	void chatToggle_tab_complete_suggests_channels() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		ChatToggleCommand cmd = new ChatToggleCommand(townService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Command bukkitCmd = mock(Command.class);
		java.util.List<String> a = cmd.onTabComplete(p, bukkitCmd, "tntownchat", new String[]{"g"});
		assertTrue(a.contains("global"));
		java.util.List<String> b = cmd.onTabComplete(p, bukkitCmd, "tntownchat", new String[]{"global","x"});
		assertTrue(b.isEmpty());
	}
}
