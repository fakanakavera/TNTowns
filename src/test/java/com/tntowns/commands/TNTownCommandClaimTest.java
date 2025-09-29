package com.tntowns.commands;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.NationService;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TNTownCommandClaimTest {

	private static class Fixture {
		final TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		final TownService townService = mock(TownService.class, RETURNS_DEEP_STUBS);
		final NationService nationService = mock(NationService.class, RETURNS_DEEP_STUBS);
		final TNTownCommand cmd = new TNTownCommand(plugin, townService);
		final Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		final Resident res = new Resident(UUID.randomUUID(), "A");
		final Town town = new Town();
		final Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		final net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		final Command bukkitCmd = mock(Command.class);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		final FileConfiguration cfg = mock(FileConfiguration.class);
		Fixture() {
			when(bukkitCmd.getName()).thenReturn("tntown");
			when(p.hasPermission("tntowns.use")).thenReturn(true);
			when(townService.getOrCreateResident(p)).thenReturn(res);
			when(townService.getResidentTown(res)).thenReturn(Optional.of(town));
			town.setId("1");
			when(p.getLocation().getChunk()).thenReturn(chunk);
			when(plugin.getEconomy()).thenReturn(eco);
			when(plugin.getNationService()).thenReturn(nationService);
			when(plugin.getConfig()).thenReturn(cfg);
			doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		}
	}

	@Test
	void claim_requires_funds_and_unclaimed() {
		Fixture f = new Fixture();
		when(f.townService.isMayor(f.res, f.town)).thenReturn(true);
		when(f.townService.isChunkClaimed(f.chunk)).thenReturn(false);
		when(f.eco.getBalance(f.p)).thenReturn(10.0);
		when(f.cfg.getDouble("costs.claim_chunk", 50.0)).thenReturn(50.0);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"claim"});
		assertTrue(ok);
		assertTrue(f.messages.stream().anyMatch(s -> s.contains("You need $50.0")));
	}

	@Test
	void claim_success_withdraws() {
		Fixture f = new Fixture();
		when(f.townService.isMayor(f.res, f.town)).thenReturn(true);
		when(f.townService.isChunkClaimed(f.chunk)).thenReturn(false);
		when(f.townService.claimChunk(f.p, f.town, f.chunk)).thenReturn(true);
		when(f.eco.getBalance(f.p)).thenReturn(100.0);
		when(f.cfg.getDouble("costs.claim_chunk", 50.0)).thenReturn(50.0);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"claim"});
		assertTrue(ok);
		verify(f.eco).withdrawPlayer(f.p, 50.0);
		assertTrue(f.messages.stream().anyMatch(s -> s.contains("Claimed chunk")));
	}

	@Test
	void unclaim_requires_ownership_and_refunds() {
		Fixture f = new Fixture();
		when(f.townService.isMayor(f.res, f.town)).thenReturn(true);
		when(f.townService.findTownByChunk(f.chunk)).thenReturn(Optional.of(f.town));
		when(f.townService.unclaimChunk(f.town, f.chunk)).thenReturn(true);
		when(f.cfg.getDouble("costs.unclaim_refund", 25.0)).thenReturn(25.0);
		boolean ok = f.cmd.onCommand(f.p, f.bukkitCmd, "tntown", new String[]{"unclaim"});
		assertTrue(ok);
		verify(f.eco).depositPlayer(f.p, 25.0);
		assertTrue(f.messages.stream().anyMatch(s -> s.contains("Unclaimed chunk.")));
	}
}
