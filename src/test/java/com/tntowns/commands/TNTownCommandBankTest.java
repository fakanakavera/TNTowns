package com.tntowns.commands;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.TownService;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TNTownCommandBankTest {

	private static class Fixture {
		final TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		final TownService townService = mock(TownService.class);
		final TNTownCommand cmd = new TNTownCommand(plugin, townService);
		final Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		final Resident res = new Resident(UUID.randomUUID(), "A");
		final Town town = new Town();
		final net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		final Command bukkitCmd = mock(Command.class);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		Fixture() {
			when(bukkitCmd.getName()).thenReturn("tntown");
			when(p.hasPermission("tntowns.use")).thenReturn(true);
			when(townService.getOrCreateResident(p)).thenReturn(res);
			when(townService.getResidentTown(res)).thenReturn(Optional.of(town));
			when(plugin.getEconomy()).thenReturn(eco);
			doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		}
	}

	@Test
	void bank_usage_when_only_bank() {
		Fixture f = new Fixture();
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.contains("Town Bank:")));
	}

	@Test
	void bank_missing_amount_prompts() {
		Fixture f = new Fixture();
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","deposit"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.equals("§eProvide an amount.")));
	}

	@Test
	void bank_deposit_invalid_amount() {
		Fixture f = new Fixture();
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","deposit","abc"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.equals("§cInvalid amount.")));
	}

	@Test
	void bank_deposit_negative_amount_rejected() {
		Fixture f = new Fixture();
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","deposit","-5"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.equals("§cAmount must be positive.")));
	}

	@Test
	void bank_deposit_insufficient_player_funds() {
		Fixture f = new Fixture();
		when(f.eco.getBalance(f.p)).thenReturn(10.0);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","deposit","50"});
		assertTrue(ok);
		verify(f.eco, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
		assertTrue(f.messages.stream().anyMatch(s -> s.equals("§cInsufficient funds.")));
	}

	@Test
	void bank_deposit_success() {
		Fixture f = new Fixture();
		when(f.eco.getBalance(f.p)).thenReturn(200.0);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","deposit","50"});
		assertTrue(ok);
		verify(f.eco).withdrawPlayer(f.p, 50.0);
		assertTrue(f.messages.stream().anyMatch(s -> s.contains("Deposited $50.0")));
	}

	@Test
	void bank_withdraw_not_mayor() {
		Fixture f = new Fixture();
		when(f.townService.isMayor(f.res, f.town)).thenReturn(false);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","withdraw","10"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.equals("§cOnly the mayor can withdraw.")));
	}

	@Test
	void bank_withdraw_insufficient_bank() {
		Fixture f = new Fixture();
		when(f.townService.isMayor(f.res, f.town)).thenReturn(true);
		when(f.townService.withdraw(f.town, 10.0)).thenReturn(false);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","withdraw","10"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.equals("§cTown bank has insufficient funds.")));
	}

	@Test
	void bank_withdraw_success() {
		Fixture f = new Fixture();
		when(f.townService.isMayor(f.res, f.town)).thenReturn(true);
		when(f.townService.withdraw(f.town, 10.0)).thenReturn(true);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"bank","withdraw","10"});
		assertTrue(ok);
		verify(f.eco).depositPlayer(f.p, 10.0);
		assertTrue(f.messages.stream().anyMatch(s -> s.contains("Withdrew $10.0")));
	}
}
