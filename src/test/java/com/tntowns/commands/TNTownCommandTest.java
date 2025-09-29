package com.tntowns.commands;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TNTownCommandTest {

	@Test
	void tntownCreate_withoutName_sendsUsageMessage() {
		// Mocks
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);

		Player player = mock(Player.class, RETURNS_DEEP_STUBS);
		when(player.hasPermission("tntowns.use")).thenReturn(true);

		// Capture sent messages
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(player).sendMessage(Mockito.anyString());

		Command bukkitCmd = mock(Command.class);
		when(bukkitCmd.getName()).thenReturn("tntown");

		boolean result = cmd.onCommand(player, bukkitCmd, "tntown", new String[]{"create"});

		assertTrue(result);
		assertFalse(messages.isEmpty());
		assertEquals("§eUsage: /tntown create <name>", messages.get(0));
	}

	@Test
	void tntownCreate_withName_succeeds_and_charges_cost() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);

		// Config: cost 500
		FileConfiguration cfg = mock(FileConfiguration.class);
		when(plugin.getConfig()).thenReturn(cfg);
		when(cfg.getDouble("costs.create_town", 500.0)).thenReturn(500.0);

		// Economy has enough, and will be charged
		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		when(plugin.getEconomy()).thenReturn(eco);

		Player p1 = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p1.hasPermission("tntowns.use")).thenReturn(true);
		when(eco.getBalance(p1)).thenReturn(1000.0);

		// Not already in a town
		when(townService.getOrCreateResident(p1)).thenReturn(new Resident(UUID.randomUUID(), "Alice"));

		// Town creation
		Town created = new Town();
		created.setId("1");
		created.setName("testtown");
		when(townService.isTownNameTaken("testtown")).thenReturn(false);
		when(townService.createTown(p1, "testtown")).thenReturn(created);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p1).sendMessage(Mockito.anyString());

		Command bukkitCmd = mock(Command.class);
		when(bukkitCmd.getName()).thenReturn("tntown");

		boolean result = cmd.onCommand(p1, bukkitCmd, "tntown", new String[]{"create", "testtown"});

		assertTrue(result);
		verify(eco).withdrawPlayer(p1, 500.0);
		assertTrue(messages.stream().anyMatch(s -> s.contains("§aCreated town §e")));
	}

	@Test
	void tntownCreate_duplicateName_rejected() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);

		FileConfiguration cfg = mock(FileConfiguration.class);
		when(plugin.getConfig()).thenReturn(cfg);
		when(cfg.getDouble("costs.create_town", 500.0)).thenReturn(500.0);

		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		when(plugin.getEconomy()).thenReturn(eco);

		Player p2 = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p2.hasPermission("tntowns.use")).thenReturn(true);
		when(eco.getBalance(p2)).thenReturn(1000.0);

		// Not already in a town
		when(townService.getOrCreateResident(p2)).thenReturn(new Resident(UUID.randomUUID(), "Bob"));

		// First town exists with this name
		when(townService.isTownNameTaken("testtown")).thenReturn(true);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p2).sendMessage(Mockito.anyString());

		Command bukkitCmd = mock(Command.class);
		when(bukkitCmd.getName()).thenReturn("tntown");

		boolean result = cmd.onCommand(p2, bukkitCmd, "tntown", new String[]{"create", "testtown"});

		assertTrue(result);
		verify(eco, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cA town with that name already exists.")));
	}

	@Test
	void tntownCreate_insufficientFunds_rejected() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);

		FileConfiguration cfg = mock(FileConfiguration.class);
		when(plugin.getConfig()).thenReturn(cfg);
		when(cfg.getDouble("costs.create_town", 500.0)).thenReturn(500.0);

		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		when(plugin.getEconomy()).thenReturn(eco);

		Player p3 = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p3.hasPermission("tntowns.use")).thenReturn(true);
		when(eco.getBalance(p3)).thenReturn(100.0); // less than cost

		when(townService.getOrCreateResident(p3)).thenReturn(new Resident(UUID.randomUUID(), "Carl"));
		when(townService.isTownNameTaken("testtown")) .thenReturn(false);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p3).sendMessage(Mockito.anyString());

		Command bukkitCmd = mock(Command.class);
		when(bukkitCmd.getName()).thenReturn("tntown");

		boolean result = cmd.onCommand(p3, bukkitCmd, "tntown", new String[]{"create", "testtown"});

		assertTrue(result);
		verify(eco, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cYou need $500.0 to create a town.")));
	}

	@Test
	void tntownCreate_alreadyInTown_rejected() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);

		FileConfiguration cfg = mock(FileConfiguration.class);
		when(plugin.getConfig()).thenReturn(cfg);
		when(cfg.getDouble("costs.create_town", 500.0)).thenReturn(500.0);

		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		when(plugin.getEconomy()).thenReturn(eco);

		Player p4 = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p4.hasPermission("tntowns.use")).thenReturn(true);
		when(eco.getBalance(p4)).thenReturn(1000.0);

		Resident r = new Resident(UUID.randomUUID(), "Dana");
		r.setTownId("1");
		when(townService.getOrCreateResident(p4)).thenReturn(r);
		when(townService.isTownNameTaken("testtown")).thenReturn(false);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p4).sendMessage(Mockito.anyString());

		Command bukkitCmd = mock(Command.class);
		when(bukkitCmd.getName()).thenReturn("tntown");

		boolean result = cmd.onCommand(p4, bukkitCmd, "tntown", new String[]{"create", "testtown"});

		assertTrue(result);
		verify(eco, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
		verify(townService, never()).createTown(any(), anyString());
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cYou are already in a town.")));
	}

	@Test
	void tntownCreate_noPermission_rejected() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);

		Player p5 = mock(Player.class, RETURNS_DEEP_STUBS);
		when(p5.hasPermission("tntowns.use")).thenReturn(false);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p5).sendMessage(Mockito.anyString());

		Command bukkitCmd = mock(Command.class);
		when(bukkitCmd.getName()).thenReturn("tntown");

		boolean result = cmd.onCommand(p5, bukkitCmd, "tntown", new String[]{"create", "testtown"});

		assertTrue(result);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cYou do not have permission.")));
	}

	@Test
	void tntown_help_and_info_and_compass() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS); when(p.hasPermission("tntowns.use")).thenReturn(true);
		Resident res = new Resident(java.util.UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1"); t.setName("Alpha"); t.setMayorUuid(java.util.UUID.randomUUID().toString());
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(java.util.Optional.of(t));
		when(townService.getTownCompassLocation(t, p.getServer())).thenReturn(java.util.Optional.of(mock(org.bukkit.Location.class)));
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("tntown");
		assertTrue(cmd.onCommand(p, bukkitCmd, "tntown", new String[]{"help"}));
		assertTrue(messages.stream().anyMatch(s -> s.contains("TNtowns:")));
		messages.clear();
		assertTrue(cmd.onCommand(p, bukkitCmd, "tntown", new String[]{}));
		assertTrue(messages.stream().anyMatch(s -> s.contains("Town Info:")));
		messages.clear();
		assertTrue(cmd.onCommand(p, bukkitCmd, "tntown", new String[]{"compass"}));
		assertTrue(messages.stream().anyMatch(s -> s.contains("Compass now points")));
	}

	@Test
	void tntown_invite_accept_leave_kick_settax_setmayor_setjail_jail_outlaw() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		TNTownCommand cmd = new TNTownCommand(plugin, townService);
		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class); when(plugin.getEconomy()).thenReturn(eco);
		com.tntowns.InviteManager im = new com.tntowns.InviteManager(); when(plugin.getInviteManager()).thenReturn(im);
		when(plugin.getNationService()).thenReturn(mock(com.tntowns.service.NationService.class, RETURNS_DEEP_STUBS));
		// players
		Player mayor = mock(Player.class, RETURNS_DEEP_STUBS); when(mayor.getName()).thenReturn("Mayor"); when(mayor.hasPermission("tntowns.use")).thenReturn(true);
		Player bob = mock(Player.class, RETURNS_DEEP_STUBS); when(bob.getName()).thenReturn("Bob");
		java.util.UUID bobId = java.util.UUID.randomUUID(); when(bob.getUniqueId()).thenReturn(bobId);
		when(mayor.getServer().getPlayerExact("Bob")).thenReturn(bob);
		// residents and town
		Resident mayorRes = new Resident(java.util.UUID.randomUUID(), "Mayor");
		Town town = new Town(); town.setId("1"); town.setName("Alfa"); town.setMayorUuid(mayorRes.getUuid().toString());
		town.setResidentUuids(new java.util.HashSet<>(java.util.Arrays.asList(mayorRes.getUuid().toString())));
		when(townService.getOrCreateResident(mayor)).thenReturn(mayorRes);
		when(townService.getResidentTown(mayorRes)).thenReturn(java.util.Optional.of(town));
		when(townService.isMayor(mayorRes, town)).thenReturn(true);
		doReturn(java.util.Optional.of(new Resident(bobId, "Bob"))).when(townService).getResident(bobId);
		// economy & config for miscellaneous commands
		org.bukkit.configuration.file.FileConfiguration cfg = mock(org.bukkit.configuration.file.FileConfiguration.class);
		when(plugin.getConfig()).thenReturn(cfg);
		when(cfg.getDouble("costs.claim_chunk", 50.0)).thenReturn(50.0);
		// messages capture
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(mayor).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("tntown");
		// invite Bob
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"invite","Bob"}));
		// accept on Bob
		im.invite(bobId, town.getId());
		when(townService.getOrCreateResident(bob)).thenReturn(new Resident(bobId, "Bob"));
		assertTrue(cmd.onCommand(bob, bukkitCmd, "tntown", new String[]{"accept"}));
		// set tax
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"settax","10"}));
		// set mayor to Bob
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"setmayor","Bob"}));
		// setjail
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"setjail"}));
		// jail toggle Bob
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"jail","Bob"}));
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"jail","Bob"}));
		// outlaw toggle
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"outlaw","Bob"}));
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"outlaw","Bob"}));
		// leave variants: non-mayor path not tested here (covered by kick), but ensure command returns
		Resident nonMayor = new Resident(java.util.UUID.randomUUID(), "Non");
		when(townService.getOrCreateResident(bob)).thenReturn(nonMayor);
		when(townService.getResidentTown(nonMayor)).thenReturn(java.util.Optional.of(town));
		when(townService.isMayor(nonMayor, town)).thenReturn(false);
		assertTrue(cmd.onCommand(bob, bukkitCmd, "tntown", new String[]{"leave"}));
		// kick path by mayor after reinvite
		im.invite(bobId, town.getId());
		Resident bobRes = new Resident(bobId, "Bob"); bobRes.setTownId(town.getId());
		when(townService.getOrCreateResident(bob)).thenReturn(bobRes);
		when(townService.getResidentTown(bobRes)).thenReturn(java.util.Optional.of(town));
		if (!town.getResidentUuids().contains(bobId.toString())) town.getResidentUuids().add(bobId.toString());
		assertTrue(cmd.onCommand(mayor, bukkitCmd, "tntown", new String[]{"kick","Bob"}));
	}
}
