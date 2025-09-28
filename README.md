# TNtowns (Paper 1.21.4)

TNtowns is a territory, economy, and social-structure plugin inspired by Towny, tailored for MMO-style servers. It implements Residents → Towns → Nations; chunk claims; banks and upkeep; diplomacy; plots; jail/outlaw; chat channels; territory caps with paid expansion; and rich command tab-completion. It integrates with Vault for economy and declares soft-depends for MMOItems/MMOCore/Essentials.

## Requirements
- PaperMC 1.21.4 (or compatible)
- Vault + Economy provider (EssentialsX Economy, CMI, etc.)
- Java 21

## Build & Install
- Build: `mvn package`
- Copy `target/TNtowns-*.jar` to your server `plugins/` and start the server

## Quick Start
1. Create a town: `/tntown create <name>`
2. Claim land: stand in a chunk and `/tntown claim`
3. Invite residents: `/tntown invite <player>` (mayor only)
4. Create a nation: mayor of a town runs `/nation create <name>`
5. Add towns: `/nation addtown <townId>` (townId is auto-generated, shown to the mayor when created)
6. Manage relations: `/nation ally <nationId>`, `/nation enemy <nationId>`
7. Expand nation territory: `/nation expand` (spends from members fairly; see Territory System)

---

## Concepts
- Resident: a player entry with UUID, name, role in a town, chat channel, and jail state.
- Town: an organization with an auto-generated id, name, mayor, residents, claimed chunks, bank, tax per resident, relations, outlaw list, optional jail location, and PVP toggle.
- Nation: an organization over towns with an auto-generated id, name, capital town, member towns, nation bank, per-member tax, diplomacy (ally/enemy), and per-player nation bank credits (deposits/withdrawals tracked per member).
- Claim: a 16×16 chunk identified by `world:x:z` owned by a town. Plots (ownership/for-sale flags) can be attached to a claim.
- Plot: ownership metadata for a claimed chunk, optionally for sale at a price.
- Nation Bank + Member Credits: Nation bank is shared; each member’s deposit to the nation is tracked. Withdrawals are limited to what a player has deposited.
- Nation Tax: A configurable per-member tax which, when collected, moves that amount from every member’s credit (can go negative) to the nation bank.
- Territory System: Nation claim graph is partitioned into connected components (4-neighborhood adjacency by chunk). Your max territories = `base_slots + extra_slots`. Buying extra slots costs follow a configurable progression.

---

## Commands
All commands have tab-completion for subcommands and key arguments.

### Town (`/tntown`)
- `create <name>`: Create a town (ID is auto-generated). Cost: `costs.create_town`.
- `claim` / `unclaim`: Claim/unclaim the current chunk. Claim respects nation territory cap rules; unclaim refunds `costs.unclaim_refund` to player. Claim cost: `costs.claim_chunk`.
- `bank deposit <amount>` / `bank withdraw <amount>`: Deposit to/withdraw from town bank (withdraw: mayor only).
- `invite <player>` / `accept`: Invite a player to your town; invited player accepts.
- `leave`: Leave your town (mayor must transfer first).
- `kick <player>`: Remove a resident (mayor only).
- `settax <amount>`: Set per-resident daily upkeep for the town (mayor).
- `setmayor <player>`: Transfer mayorship.
- `setjail`: Set jail location to your position (mayor).
- `jail <player>`: Toggle jail for a player in your town (mayor).
- `outlaw <player>`: Toggle outlaw status (no protection for them in your town; mayor).

### Nation (`/nation`)
- `create <name>`: Create a nation (ID is auto-generated).
- `addtown <townId>`: Add an existing town to your nation.
- `ally <nationId>` / `enemy <nationId>`: Set relations for diplomacy and protections.
- `bank` → shows nation bank and your personal deposit credit.
- `bank deposit <amount>` / `bank withdraw <amount>`: Deposit increases nation bank and your personal credit; withdrawals are limited by your credit and bank balance.
- `expand`: Purchases an additional nation territory slot, withdrawing the cost evenly across members’ credits (credits can go negative). Also debits nation bank by the same amount.
- `accept`: Accept a pending nation invite outside of the admin path.
- `admin invite <player>`: Invite a player to the nation (if they aren’t already in any nation).
- `admin kick <player>`: Remove a player from the nation roster.
- `admin rank <player> <E1-E9>`: Set the member’s nation rank (simple E1–E9 ladder).
- `admin bank list`: Show all members and their nation bank credits.
- `admin tax set <amount>` / `admin tax get`: Set/get the per-member tax value for the nation.
- `admin tax debug gettaxes`: Collect per-member tax from every member into the nation bank; members’ credits go negative if needed.

### Chat & Admin Utilities
- `/tntownchat <global|local|town|nation>`: Switch chat channel.
- `/plot <forsale <price>|buy|setowner <player>>`: Manage plot ownership and sales for the current claimed chunk (mayor or `tntowns.plot.admin` can set owner). 

> Note: Legacy `/tntownadmin` was removed to avoid confusion. Town pvp toggling exists in code but is not currently exposed as a registered command.

---

## Protections & Rules
- Build/Break/Interact:
  - Allowed for town members within their town claims.
  - Allowed for plot owners in the claimed chunk.
  - Allies can build if `protection.allow_allies_build` is true.
  - Cancelled elsewhere (wilderness is allowed by default).
- PvP:
  - Inside town claims, PvP is blocked if the town’s `pvpEnabled` is false.
  - Allies and same-town members can’t damage each other inside borders.
- Outlaws: Outlaws have no protection within the town that marked them.
- Jail: Jailed residents are kept at the town’s jail position; attempts to leave teleport them back.

---

## Territory System (Nations)
- Nation claims are the union of all member towns’ claimed chunks.
- Territories are defined as connected components of claimed chunks using 4-direction adjacency.
- The maximum number of territories is `nation.territory.base_slots + extraTerritorySlots`.
- `/nation expand` buys +1 extra slot at cost taken from the nation bank and split evenly across all nation members’ credits. Members’ credits can go negative.

---

## Economy & Upkeep
- Town upkeep: A scheduled task runs every `scheduler.upkeep_interval_minutes` to charge each town `taxPerResident * residentCount`. If the town bank can’t pay, the town is disbanded (residents removed, claims cleared).
- Nation bank: Tracks a shared balance plus per-member deposits. Players can only withdraw up to their personal deposit total.
- Nation tax: A per-member amount configured per nation. `admin tax debug gettaxes` collects from all members into nation bank; member credits can go negative.

---

## Configuration (`config.yml`)
```yml
costs:
  create_town: 500.0
  claim_chunk: 50.0
  unclaim_refund: 25.0

taxes:
  upkeep_per_resident_daily: 10.0

scheduler:
  upkeep_interval_minutes: 1440

chat:
  prefix: "&6[\uE000TNtowns&6] &r"

protection:
  allow_allies_build: false

nation:
  territory:
    base_slots: 1
    expansion_costs:
      - 1000.0
      - 2500.0
      - 5000.0
      - 10000.0
```

### Notes
- Claim/unclaim/town creation costs use Vault economy.
- Nation expansion consumes the next entry in `expansion_costs` (final value repeats when the list is exceeded).

---

## Data Storage (plugin data folder)
- `residents.yml`: UUID → { name, role, townId, chat, jailed }
- `towns.yml`: townId → { name, mayorUuid, nationId, bankBalance, taxPerResident, residentUuids[], claimKeys[], allyTownIds[], enemyTownIds[], outlawUuids[], jail: { world,x,y,z }, pvpEnabled }
- `nations.yml`: nationId → { name, kingTownId, bankBalance, perMemberTax, townIds[], allyNationIds[], enemyNationIds[], memberDeposits{ uuid: amount }, memberRanks{ uuid: rank } }
- `plots.yml`: `world:x:z` → { ownerUuid, forSale, price }
- `meta.yml`: `nextTownId`, `nextNationId`

---

## Permissions
- `tntowns.use` (default: true): Use general commands
- `tntowns.claim` (default: false): Non-mayor claim permission
- `tntowns.unclaim` (default: false): Non-mayor unclaim permission
- `tntowns.invite` (default: false): Town invite if not mayor
- `tntowns.admin` (default: op): Recommended gate for nation admin usage (not strictly enforced by every subcommand)
- `tntowns.plot.admin` (default: op): Override for plot owner setting

---

## Tab Completion
All registered commands provide subcommand and argument suggestions:
- `/tntown` → subcommands; player names for invite/kick/jail/outlaw/setmayor; `bank` actions.
- `/nation` → subcommands; under `admin`, suggests `invite|kick|rank|bank|tax` and player names/ranks; `bank` suggests `deposit|withdraw`.
- `/tntownchat` → `global|local|town|nation`.
- `/plot` → `forsale|buy|setowner` (+ player names).

---

## Event Hooks & Safety
- Block place/break, interact: cancelled where protections apply.
- PvP: cancelled per town PVP toggle and ally/same-town logic inside borders.
- Player join: Residents auto-registered.

---

## Integrations (Soft-dep)
- Vault: Required for all economic operations.
- MMOItems/MMOCore/Essentials/TNauctionhouse: Declared as soft-dep for future integrations. Current build operates without them.

---

## Roadmap / Extension Ideas
- Per-flag permissions (build/break/interact) with friend exceptions per plot
- Rank-gated nation admin actions (min rank to kick/invite, etc.)
- Border PvP and war/raid systems
- Dynmap/BlueMap overlays for claims and borders
- GUI management (chest GUIs) for towns/nations

---

## Support
Please open issues with logs and steps to reproduce. Include your `config.yml` and relevant YAMLs from the data folder if the issue involves data.
