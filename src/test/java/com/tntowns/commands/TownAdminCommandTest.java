package com.tntowns.commands;

import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TownAdminCommandTest {

	@Test
	void townAdmin_pvp_on_sets_flag() {
		TownService townService = mock(TownService.class);
		TownAdminCommand cmd = new TownAdminCommand(townService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p.hasPermission("tntowns.admin")).thenReturn(true);
		Chunk c = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(c);
		Town owner = new Town(); owner.setId("1");
		when(townService.findTownByChunk(c)).thenReturn(Optional.of(owner));
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("townadmin");
		boolean ok = cmd.onCommand(p, bukkitCmd, "townadmin", new String[]{"pvp", "on"});
		assertTrue(ok);
		assertTrue(owner.isPvpEnabled());
		assertTrue(messages.stream().anyMatch(s -> s.contains("Set town PVP to")));
	}

	@Test
	void townAdmin_pvp_on_requires_permission() {
		TownService townService = mock(TownService.class);
		TownAdminCommand cmd = new TownAdminCommand(townService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p.hasPermission("tntowns.admin")).thenReturn(false);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("townadmin");
		boolean ok = cmd.onCommand(p, bukkitCmd, "townadmin", new String[]{"pvp", "on"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cNo permission.")));
	}

	@Test
	void townAdmin_usage_and_wilderness_and_pvp_off_and_unknown_and_nonplayer_and_tab() {
		TownService townService = mock(TownService.class);
		TownAdminCommand cmd = new TownAdminCommand(townService);
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("townadmin");
		// Non-player
		org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
		final java.util.List<String> m1 = new java.util.ArrayList<>(); doAnswer(inv -> { m1.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());
		assertTrue(cmd.onCommand(sender, bukkitCmd, "townadmin", new String[]{}));
		assertTrue(m1.stream().anyMatch(s -> s.equals("Players only.")));
		// Usage
		Player p = mock(Player.class, RETURNS_DEEP_STUBS); when(p.hasPermission("tntowns.admin")).thenReturn(true);
		final java.util.List<String> m2 = new java.util.ArrayList<>(); doAnswer(inv -> { m2.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		assertTrue(cmd.onCommand(p, bukkitCmd, "townadmin", new String[]{}));
		assertTrue(m2.stream().anyMatch(s -> s.contains("Usage:")));
		// Wilderness
		java.util.List<String> m3 = new java.util.ArrayList<>(); doAnswer(inv -> { m3.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		org.bukkit.Chunk c = mock(org.bukkit.Chunk.class, RETURNS_DEEP_STUBS); when(p.getLocation().getChunk()).thenReturn(c);
		when(townService.findTownByChunk(c)).thenReturn(java.util.Optional.empty());
		assertTrue(cmd.onCommand(p, bukkitCmd, "townadmin", new String[]{"pvp","on"}));
		assertTrue(m3.stream().anyMatch(s -> s.equals("§cThis chunk is wilderness.")));
		// pvp off
		Town t = new Town(); t.setId("1"); when(townService.findTownByChunk(c)).thenReturn(java.util.Optional.of(t));
		java.util.List<String> m4 = new java.util.ArrayList<>(); doAnswer(inv -> { m4.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		assertTrue(cmd.onCommand(p, bukkitCmd, "townadmin", new String[]{"pvp","off"}));
		assertFalse(t.isPvpEnabled());
		// unknown sub
		java.util.List<String> m5 = new java.util.ArrayList<>(); doAnswer(inv -> { m5.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		assertTrue(cmd.onCommand(p, bukkitCmd, "townadmin", new String[]{"nope"}));
		assertTrue(m5.stream().anyMatch(s -> s.equals("§cUnknown subcommand.")));
		// tab complete
		java.util.List<String> tc1 = cmd.onTabComplete(p, bukkitCmd, "townadmin", new String[]{"p"});
		org.junit.jupiter.api.Assertions.assertTrue(tc1.contains("pvp"));
		java.util.List<String> tc2 = cmd.onTabComplete(p, bukkitCmd, "townadmin", new String[]{"pvp","o"});
		org.junit.jupiter.api.Assertions.assertTrue(tc2.contains("on") || tc2.contains("off"));
	}
}
