package com.tntowns.commands;

import com.tntowns.InviteManager;
import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Nation;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.NationService;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NationAdminCommandTest {

	private Nation baseNation() {
		Nation n = new Nation(); n.setId("N1");
		return n;
	}

	private Town baseTown() {
		Town t = new Town(); t.setId("1"); t.setNationId("N1"); t.setMayorUuid(UUID.randomUUID().toString());
		return t;
	}

	@Test
	void nation_admin_invite_and_accept() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		NationService nationService = mock(NationService.class);
		NationAdminCommand cmd = new NationAdminCommand(townService, nationService);
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		InviteManager im = new InviteManager();
		try (MockedStatic<TNtownsPlugin> mocked = mockStatic(TNtownsPlugin.class)) {
			mocked.when(TNtownsPlugin::getInstance).thenReturn(plugin);
			when(plugin.getInviteManager()).thenReturn(im);
			Nation n = baseNation();
			Town t = baseTown();
			Player p = mock(Player.class, RETURNS_DEEP_STUBS);
			Resident res = new Resident(UUID.randomUUID(), "A");
			when(townService.getOrCreateResident(p)).thenReturn(res);
			when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
			when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
			when(p.getServer().getPlayerExact("Bob")).thenReturn(p); when(p.getName()).thenReturn("Bob");
			Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nationadmin");
			final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
			boolean inv = cmd.onCommand(p, bukkitCmd, "nationadmin", new String[]{"invite", "Bob"});
			im.invite(p.getUniqueId(), "N1");
			boolean acc = cmd.onCommand(p, bukkitCmd, "nationadmin", new String[]{"accept"});
			assertTrue(inv && acc);
		}
	}

	@Test
	void nation_admin_kick_and_rank_and_bank_list() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		NationService nationService = mock(NationService.class);
		Nation n = baseNation();
		Town t = baseTown();
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
		when(nationService.isAdminOrKingTownMayor(n, t, p.getUniqueId())).thenReturn(true);
		Player target = mock(Player.class, RETURNS_DEEP_STUBS); when(p.getServer().getPlayerExact("Bob")).thenReturn(target); when(target.getName()).thenReturn("Bob");
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean kick = new NationCommand(townService, nationService).onCommand(p, bukkitCmd, "nation", new String[]{"admin","kick","Bob"});
		boolean rank = new NationCommand(townService, nationService).onCommand(p, bukkitCmd, "nation", new String[]{"admin","rank","Bob","E3"});
		boolean bank = new NationCommand(townService, nationService).onCommand(p, bukkitCmd, "nation", new String[]{"admin","bank"});
		assertTrue(kick && rank && bank);
		assertTrue(messages.stream().anyMatch(s -> s.contains("Kicked") || s.contains("Set nation rank") || s.contains("Nation Bank Info:")));
	}

	@Test
	void nation_admin_onTabComplete_suggestions() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		NationService nationService = mock(NationService.class);
		NationAdminCommand cmd = new NationAdminCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Command bukkitCmd = mock(Command.class);
		java.util.List<String> root = cmd.onTabComplete(p, bukkitCmd, "nationadmin", new String[]{"i"});
		assertTrue(root.contains("invite"));
		java.util.List<String> bank = cmd.onTabComplete(p, bukkitCmd, "nationadmin", new String[]{"bank",""});
		assertTrue(bank.contains("list"));
		// rank list E1..E9
		java.util.List<String> ranks = cmd.onTabComplete(p, bukkitCmd, "nationadmin", new String[]{"rank","Bob","E"});
		assertTrue(ranks.stream().anyMatch(r -> r.startsWith("E")));
	}

	@Test
	void nation_admin_tax_and_expand_and_permissions() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		try (MockedStatic<TNtownsPlugin> mocked = mockStatic(TNtownsPlugin.class)) {
			mocked.when(TNtownsPlugin::getInstance).thenReturn(plugin);
			FileConfiguration cfg = mock(FileConfiguration.class);
			when(plugin.getConfig()).thenReturn(cfg);
			when(cfg.getInt("nation.territory.base_slots", 1)).thenReturn(1);
			when(cfg.getDoubleList("nation.territory.expansion_costs")).thenReturn(java.util.Arrays.asList(100.0, 200.0));
			Nation n = baseNation(); n.setBankBalance(1000.0);
			Town t = baseTown();
			Player p = mock(Player.class, RETURNS_DEEP_STUBS);
			Resident res = new Resident(UUID.randomUUID(), "A");
			when(townService.getOrCreateResident(p)).thenReturn(res);
			when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
			when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
			when(nationService.isAdminOrKingTownMayor(n, t, p.getUniqueId())).thenReturn(true);
			final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
			Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
			boolean set = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin","tax","set","15"});
			boolean get = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin","tax","get"});
			boolean expand = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin","expand"});
			boolean give = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin","permission","give","Bob"});
			boolean take = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin","permission","take","Bob"});
			assertTrue(set && get && expand && give && take);
		}
	}

	@Test
	void nation_admin_usage_and_unknown_and_nonplayer() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		// Non-player
		org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
		final java.util.List<String> msgs1 = new java.util.ArrayList<>(); doAnswer(inv -> { msgs1.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		assertTrue(cmd.onCommand(sender, bukkitCmd, "nation", new String[]{"admin"}));
		assertTrue(msgs1.stream().anyMatch(s -> s.equals("Players only.")));
		// Player but not in town
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		final java.util.List<String> msgs2 = new java.util.ArrayList<>(); doAnswer(inv -> { msgs2.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Resident res = new Resident(java.util.UUID.randomUUID(), "A");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.empty());
		assertTrue(cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin"}));
		assertTrue(msgs2.stream().anyMatch(s -> s.equals("§cYou are not in a nation.")) || msgs2.stream().anyMatch(s -> s.equals("§cYou are not in a town.")));
		// Unknown admin sub
		Town t = baseTown(); Nation n = baseNation();
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
		when(nationService.isAdminOrKingTownMayor(n, t, p.getUniqueId())).thenReturn(true);
		final java.util.List<String> msgs3 = new java.util.ArrayList<>(); doAnswer(inv -> { msgs3.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		assertTrue(cmd.onCommand(p, bukkitCmd, "nation", new String[]{"admin","nope"}));
		assertTrue(msgs3.stream().anyMatch(s -> s.equals("§cUnknown admin subcommand.")));
	}

	@Test
	void nation_admin_tab_complete_covers_paths() {
		TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		// root subs
		java.util.List<String> s1 = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"a"});
		assertTrue(s1.stream().anyMatch(v -> v.startsWith("a")));
		// admin subs
		java.util.List<String> s2 = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"admin","b"});
		assertTrue(s2.contains("bank") || s2.contains("ban"));
		// admin rank player list and rank list (may be empty when no online players)
		//java.util.List<String> rs = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"admin","rank","b"});
		java.util.List<String> ranks = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"admin","rank","Bob","E"});
		assertTrue(ranks.stream().anyMatch(v -> v.startsWith("E")));
		// admin bank list
		java.util.List<String> bl = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"admin","bank",""});
		assertTrue(bl.contains("list"));
		// admin tax suggest
		java.util.List<String> tax = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"admin","tax",""});
		assertTrue(tax.contains("set") && tax.contains("get") && tax.contains("debug"));
		java.util.List<String> taxdbg = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"admin","tax","debug",""});
		assertTrue(taxdbg.contains("gettaxes"));
	}
}
