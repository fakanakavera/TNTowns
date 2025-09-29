package com.tntowns.commands;

import com.tntowns.InviteManager;
import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Nation;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.NationService;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NationCommandTest {

	@Test
	void nation_info_when_in_nation_shows_info() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1"); t.setNationId("N1");
		Nation n = new Nation(); n.setId("N1"); n.setName("Alpha");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n));

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.contains("Nation Info:")));
	}

	@Test
	void nation_create_requires_mayor() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(townService.isMayor(res, t)).thenReturn(false);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"create", "NName"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cOnly the mayor can create a nation.")));
	}

	@Test
	void nation_addtown_happy_path() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town my = new Town(); my.setId("1"); my.setNationId("N1");
		Town other = new Town(); other.setId("2");
		Nation n = new Nation(); n.setId("N1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(my));
		when(townService.getTownById("2")).thenReturn(Optional.of(other));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"addtown", "2"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§aTown added to nation.")));
	}

	@Test
	void nation_ally_and_enemy() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town my = new Town(); my.setId("1"); my.setNationId("N1");
		Nation n1 = new Nation(); n1.setId("N1");
		Nation n2 = new Nation(); n2.setId("N2");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(my));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n1));
		when(nationService.getNationById("N2")).thenReturn(Optional.of(n2));
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean a = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"ally", "N2"});
		boolean e = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"enemy", "N2"});
		assertTrue(a && e);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§aUpdated relations.")));
	}

	@Test
	void nation_bank_deposit_and_withdraw_and_transfer() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		try (MockedStatic<TNtownsPlugin> mocked = mockStatic(TNtownsPlugin.class)) {
			mocked.when(TNtownsPlugin::getInstance).thenReturn(plugin);
			net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
			when(plugin.getEconomy()).thenReturn(eco);
			Player p = mock(Player.class, RETURNS_DEEP_STUBS);
			Player q = mock(Player.class, RETURNS_DEEP_STUBS);
			when(p.getServer().getPlayerExact("Bob")).thenReturn(q);
			when(q.getName()).thenReturn("Bob");
			Resident res = new Resident(UUID.randomUUID(), "A");
			Town my = new Town(); my.setId("1"); my.setNationId("N1");
			Nation n1 = new Nation(); n1.setId("N1");
			when(townService.getOrCreateResident(p)).thenReturn(res);
			when(townService.getResidentTown(res)).thenReturn(Optional.of(my));
			when(nationService.getNationById("N1")).thenReturn(Optional.of(n1));
			when(eco.getBalance(p)).thenReturn(1000.0);
			// deposit 100
			Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
			boolean dep = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"bank", "deposit", "100"});
			// withdraw 50
			when(nationService.getMemberDeposit(n1, p.getUniqueId())).thenReturn(100.0);
			when(nationService.withdraw(n1, p.getUniqueId(), 50.0)).thenReturn(true);
			boolean wit = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"bank", "withdraw", "50"});
			// transfer 25 to Bob
			when(nationService.getMemberDeposit(n1, p.getUniqueId())).thenReturn(50.0);
			when(nationService.withdraw(n1, p.getUniqueId(), 25.0)).thenReturn(true);
			boolean tr = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"bank", "transfer", "Bob", "25"});
			assertTrue(dep && wit && tr);
		}
	}

	@Test
	void nation_accept_uses_invite_manager() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		InviteManager im = new InviteManager();
		try (MockedStatic<TNtownsPlugin> mocked = mockStatic(TNtownsPlugin.class)) {
			mocked.when(TNtownsPlugin::getInstance).thenReturn(plugin);
			when(plugin.getInviteManager()).thenReturn(im);
			Player p = mock(Player.class, RETURNS_DEEP_STUBS);
			Resident res = new Resident(UUID.randomUUID(), "A");
			Town my = new Town(); my.setId("1");
			when(townService.getOrCreateResident(p)).thenReturn(res);
			when(townService.getResidentTown(res)).thenReturn(Optional.of(my));
			Nation n1 = new Nation(); n1.setId("n1");
			when(nationService.getNationById("n1")).thenReturn(Optional.of(n1));
			im.invite(p.getUniqueId(), "N1"); // InviteManager lowercases to n1
			final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
			Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
			boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"accept"});
			assertTrue(ok);
			assertTrue(messages.stream().anyMatch(s -> s.equals("§aJoined nation.")));
		}
	}

	@Test
	void nation_not_in_nation_shows_error() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1"); // no nation
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cYou are not in a nation.")));
	}

	@Test
	void nation_create_success_for_mayor() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(townService.isMayor(res, t)).thenReturn(true);
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"create","My Nation"});
		assertTrue(ok);
		verify(nationService).createNation("My Nation", t);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§aNation created.")));
	}

	@Test
	void nation_bank_show_own_deposit() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1"); t.setNationId("N1");
		com.tntowns.model.Nation n = new com.tntowns.model.Nation(); n.setId("N1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
		when(nationService.getMemberDeposit(n, p.getUniqueId())).thenReturn(12.34);
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"bank"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.contains("Your nation bank deposit: §e$12.34") || s.contains("$12.34")));
	}

	@Test
	void nation_bank_invalid_amount_message() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1"); t.setNationId("N1");
		com.tntowns.model.Nation n = new com.tntowns.model.Nation(); n.setId("N1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(nationService.getNationById("N1")).thenReturn(Optional.of(n));
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("nation");
		boolean ok = cmd.onCommand(p, bukkitCmd, "nation", new String[]{"bank","deposit","abc"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cInvalid amount.")));
	}

	@Test
	void nation_tab_complete_bank_suggestions() {
		TownService townService = mock(TownService.class);
		NationService nationService = mock(NationService.class);
		NationCommand cmd = new NationCommand(townService, nationService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		java.util.List<String> root = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"b"});
		assertTrue(root.contains("bank"));
		java.util.List<String> bank = cmd.onTabComplete(p, mock(Command.class), "nation", new String[]{"bank","d"});
		assertTrue(bank.contains("deposit"));
	}
}
