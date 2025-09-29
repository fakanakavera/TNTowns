package com.tntowns.commands;

import com.tntowns.TNtownsPlugin;
import com.tntowns.model.Plot;
import com.tntowns.model.Resident;
import com.tntowns.model.Town;
import com.tntowns.service.PlotService;
import com.tntowns.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlotCommandTest {

	@Test
	void plot_forsale_sets_price() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));
		Plot plot = new Plot();
		when(plotService.getOrCreate(chunk)).thenReturn(plot);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");

		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"forsale", "250"});
		assertTrue(ok);
		assertTrue(plot.isForSale());
		assertEquals(250.0, plot.getPrice());
	}

	@Test
	void plot_buy_insufficient_funds() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));
		Plot plot = new Plot(); plot.setForSale(true); plot.setPrice(300.0);
		when(plotService.getOrCreate(chunk)).thenReturn(plot);

		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		when(plugin.getEconomy()).thenReturn(eco);
		when(eco.getBalance(p)).thenReturn(100.0);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");

		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"buy"});
		assertTrue(ok);
		verify(eco, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cInsufficient funds.")));
	}

	@Test
	void plot_buy_succeeds() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));
		Plot plot = new Plot(); plot.setForSale(true); plot.setPrice(300.0);
		when(plotService.getOrCreate(chunk)).thenReturn(plot);

		net.milkbowl.vault.economy.Economy eco = mock(net.milkbowl.vault.economy.Economy.class);
		when(plugin.getEconomy()).thenReturn(eco);
		when(eco.getBalance(p)).thenReturn(500.0);

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");

		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"buy"});
		assertTrue(ok);
		verify(eco).withdrawPlayer(p, 300.0);
		assertEquals(p.getUniqueId().toString(), plot.getOwnerUuid());
		assertFalse(plot.isForSale());
	}

	@Test
	void plot_setowner_requires_mayor_or_perm() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);

		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(townService.isMayor(res, t)).thenReturn(false);
		when(p.hasPermission("tntowns.plot.admin")).thenReturn(false);
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));

		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");

		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"setowner", "Any"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cOnly mayor can set plot owner.")));
	}

	@Test
	void plot_usage_and_unknown_sub() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");
		boolean usage = cmd.onCommand(p, bukkitCmd, "plot", new String[]{});
		boolean unknown = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"nope"});
		assertTrue(usage && unknown);
		assertTrue(messages.stream().anyMatch(s -> s.contains("Usage:")));
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cUnknown subcommand.")));
	}

	@Test
	void plot_setowner_success() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Player target = mock(Player.class, RETURNS_DEEP_STUBS); when(target.getName()).thenReturn("Bob");
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(townService.isMayor(res, t)).thenReturn(true);
		when(p.getServer().getPlayerExact("Bob")).thenReturn(target);
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));
		Plot plot = new Plot(); when(plotService.getOrCreate(chunk)).thenReturn(plot);
		final java.util.List<String> messages = new java.util.ArrayList<>(); doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");
		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"setowner","Bob"});
		assertTrue(ok);
		assertEquals(target.getUniqueId().toString(), plot.getOwnerUuid());
		assertTrue(messages.stream().anyMatch(s -> s.contains("Set plot owner to")));
	}

	@Test
	void plot_forsale_invalid_price() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		Chunk chunk = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(chunk);
		when(townService.findTownByChunk(chunk)).thenReturn(Optional.of(t));
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");
		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"forsale","abc"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cInvalid price.")));
	}

	@Test
	void plot_requires_in_town_and_correct_owner() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.empty());
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");
		boolean notown = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"buy"});
		assertTrue(notown);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cYou are not in a town.")));
		// now in town but wrong owner
		messages.clear();
		Town t = new Town(); t.setId("1");
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		Chunk ch = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(ch);
		when(townService.findTownByChunk(ch)).thenReturn(Optional.empty());
		boolean wrong = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"buy"});
		assertTrue(wrong);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cYour town must own this chunk.")));
	}

	@Test
	void plot_setowner_player_not_found() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		Resident res = new Resident(UUID.randomUUID(), "A");
		Town t = new Town(); t.setId("1");
		when(townService.getOrCreateResident(p)).thenReturn(res);
		when(townService.getResidentTown(res)).thenReturn(Optional.of(t));
		when(townService.isMayor(res, t)).thenReturn(true);
		Chunk ch = mock(Chunk.class, RETURNS_DEEP_STUBS);
		when(p.getLocation().getChunk()).thenReturn(ch);
		when(townService.findTownByChunk(ch)).thenReturn(Optional.of(t));
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");
		// Ensure player lookup returns null to trigger early return
		when(p.getServer().getPlayerExact("Bob")).thenReturn(null);
		boolean ok = cmd.onCommand(p, bukkitCmd, "plot", new String[]{"setowner","Bob"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("§cPlayer not found.")));
	}

	@Test
	void plot_non_player_rejected_and_tab_complete() {
		TNtownsPlugin plugin = mock(TNtownsPlugin.class, RETURNS_DEEP_STUBS);
		TownService townService = mock(TownService.class);
		PlotService plotService = mock(PlotService.class);
		PlotCommand cmd = new PlotCommand(plugin, townService, plotService);
		org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
		final java.util.List<String> messages = new java.util.ArrayList<>();
		doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());
		Command bukkitCmd = mock(Command.class); when(bukkitCmd.getName()).thenReturn("plot");
		boolean ok = cmd.onCommand(sender, bukkitCmd, "plot", new String[]{"buy"});
		assertTrue(ok);
		assertTrue(messages.stream().anyMatch(s -> s.equals("Players only.")));
		// tab complete basics
		Player p = mock(Player.class, RETURNS_DEEP_STUBS);
		java.util.List<String> root = cmd.onTabComplete(p, bukkitCmd, "plot", new String[]{"f"});
		assertTrue(root.contains("forsale"));
	}
}
