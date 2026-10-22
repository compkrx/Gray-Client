# Gray Client feature coverage

This maps the uploaded feature catalog to the current source. The catalog’s closing recommendation about release policy is background text, not a module request. A listed catalog idea is not automatically an implemented module, a promise of server compatibility, or a claim about another client. Build and live-game validation are reported separately in the README/release notes.

## Current module registry

The 34 registered modules are:

- Combat: SilentAura, AimAssist, TriggerBot, AutoClicker, HitSelect, WTap, Velocity, CrystalAssist, CrystalAura, MaceBot, AutoWeapon, ShieldBreaker, BowRelease.
- Player: AutoSprint, AutoEat, AutoTotem, AutoRespawn, AutoTool, AutoReplenish, FastPlace.
- Farming: AutoFarm, Nuker, VeinMiner, OreFinder, AutoPlant, AutoFish, AutoShear, AutoBreed, BonemealAura.
- Visual: Fullbright, ESP, HUD, InfoHUD, Zoom.

OreFinder reports nearby exposed ore from loaded chunks. It is not XRay. AutoPlant is an additional planting-only helper; AutoFarm also replants. Aliases in the catalog are mapped to their actual implementation, rather than counted as extra modules.

## Good candidates for a later increment

- Information HUD expansion: ping, saturation, per-piece armor display, configurable widget placement and colors. InfoHUD already supplies coordinates, direction, FPS, horizontal speed, selected equipment durability, block name and a numeric light sample.
- Local presentation expansion: view-bobbing controls and customizable HUD placement. Toggle Zoom is already included.
- Farming expansion: more crop/animal policies, storage workflows and validated pathfinding integrations. AutoFish, AutoShear, AutoBreed and BonemealAura are already included.
- Movement/input: AutoWalk, ToggleSneak and conservative AntiAFK with explicit ownership and release of synthetic input.
- Inventory: item filters, container transfers and restock profiles with correct container synchronization.

These are implementation candidates, not available switches. Larger systems such as template building, pathfinding, replay, profiles and projectile prediction need their own design and testing. A client mod cannot grant permissions, authoritative flight/reach, hidden server data, server-side vein mining or anticheat compatibility.

## Status key

- **Present**: an implementation exists in this source; read its limits.
- **Partial**: a related feature exists, but the catalog entry is broader.
- **Missing**: no implementation is included.
- **Separate**: requires new integration, service, launcher or architecture work.

## 1. Client Framework And User Interface

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| ClickGUI / module menu | Present | Gray menu, native widgets, categories, toggles and paged settings. |
| TabGUI / keyboard navigation | Partial | Native widgets support focus/keyboard navigation; no separate TabGUI overlay. |
| Module search / Navigator | Partial | Searches module names and descriptions; no independent setting navigator. |
| Per-module keybinds | Present | Uses Minecraft Controls; toggle bindings start unbound. |
| Persistent settings | Partial | Boolean/numeric values persist. Enabled toggles reset off, except HUD. |
| Profiles / AutoConfig | Missing | Not implemented. |
| Per-module settings panels | Present | Boolean and numeric values; long setting lists paginate. |
| Active-module list / HackList | Present | HUD can list enabled modules. |
| Client watermark | Present | Gray Client HUD watermark. |
| Customizable HUD editor | Missing | Not implemented. |
| Panic / Disable Client | Present | Disable all button in the menu; no dedicated panic key. |
| Category filters and module aliases | Partial | Category filters exist; arbitrary aliases do not. |
| In-game commands for module control and configuration. | Missing | Not implemented. |
| Client update / multi-version launcher support. | Separate | Requires distribution/update and version-management architecture. |
| Built-in FPS optimizations and rendering/chunk-loading performance options. | Missing | Not implemented. |
| Custom skin changer. | Missing | Not implemented. |
| Cosmetics | Separate | Requires new render/assets infrastructure and possibly a cosmetic service. |
| Friends / social presence | Separate | Friend/social services and Discord integration are not bundled. |
| Cross-server chat and built-in voice chat (Feather/Dawn). | Separate | Requires network/service integration and compatible peers. |
| Client-hosted worlds/server hosting and single-player world backups (Feather/Dawn features). | Separate | Hosting needs server/service orchestration; local backup UI is also absent. |
| HUD and game profiles associated with specific servers. | Missing | Not implemented. |
| Optional replay / gameplay recording integration where supported by the client/version. | Separate | Requires recording/replay integration, not an existing module toggle. |
| Built-in mod manager/library for the client's own bundled mods. | Separate | Requires dependency/distribution management beyond this Fabric mod. |

## 2. Pvp And Combat

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| AimAssist | Present | Smooth aim while attack is held; configured FOV and speed. |
| Aimbot | Missing | Not implemented. |
| BowAimbot / projectile aim prediction | Missing | Not implemented. |
| AutoBow | Partial | BowRelease releases a bow already being drawn; no auto-aim or automatic redraw. |
| KillAura / SilentAura | Present | SilentAura targets eligible nearby living entities. |
| ClickAura | Partial | AutoClicker attacks the crosshair target under held attack; no separate nearby click-aura. |
| MultiAura | Missing | Not implemented. |
| TriggerBot | Present | Crosshair living-target attacks with attack cooldown gating. |
| AutoClicker | Present | Repeated attacks at crosshair entities while attack is held. |
| AutoShoot | Missing | Not implemented. |
| AutoRod | Missing | Not implemented. |
| AutoWeapon / AutoSword | Present | Selects the first hotbar sword, otherwise axe; does not rank all weapon attributes. |
| AutoArmor | Missing | Not implemented. |
| AutoTotem / Offhand manager | Partial | Totem-only offhand management; no generalized offhand policy. |
| AutoBuff / AutoPotion | Missing | Not implemented. |
| AutoSoup | Missing | Not implemented. |
| AutoGapple / AutoGap | Partial | AutoEat can allow gapples as food; no health-triggered gapple module. |
| AutoEat | Partial | Hotbar food and hunger threshold; no generalized healing/health consumable controller. |
| AutoMend | Missing | Not implemented. |
| Criticals | Missing | Not implemented. |
| End Crystal Aura | Present | CrystalAura placement and detonation; requires items and valid placement. |
| Crystal Assist | Present | Held-input crystal place/break assistance. |
| AnchorAura | Missing | Not implemented. |
| BedAura | Missing | Not implemented. |
| MaceAura / MaceKill / MaceBot | Present | MaceBot falling-target attacks and temporary hotbar selection. |
| SpearKill | Missing | Not implemented. |
| AutoEXP / EXPThrower | Missing | Not implemented. |
| BowSpam | Missing | Not implemented. |
| ArrowDodge / AutoDodge | Missing | Not implemented. |
| AutoPearl / EasyPearl | Missing | Not implemented. |
| AutoWindCharge | Missing | Not implemented. |
| AutoShieldBreaker | Present | ShieldBreaker selects a hotbar axe against a blocking target. |
| AutoTrap | Missing | Not implemented. |
| AutoWeb / SelfWeb | Missing | Not implemented. |
| AutoAnvil / SelfAnvil | Missing | Not implemented. |
| SelfTrap | Missing | Not implemented. |
| Burrow | Missing | Not implemented. |
| Surround | Missing | Not implemented. |
| AutoCity | Missing | Not implemented. |
| Quiver | Missing | Not implemented. |
| AutoRespawn | Present | Requests respawn after death. |
| AutoLeave / AutoLog | Missing | Not implemented. |
| AutoWeapon / target filters | Partial | Shared living-target filters and some per-module player/mob toggles; not arbitrary entity filters. |
| TargetLock | Missing | Not implemented. |
| Hitbox adjustment | Missing | Not implemented. |
| Reach | Missing | Not implemented. |
| AntiKnockback / Velocity | Partial | Velocity handles incoming entity-velocity packets; not every knockback source. |
| SuperKnockback | Partial | WTap provides a sprint reset; no separate SuperKnockback module. |
| WTap | Present | Sprint reset around attacks generated through the shared attack helper. |
| KeepSprint | Missing | Not implemented. |
| SwordBlock | Missing | Not implemented. |
| NoMissCooldown / BreakDelay | Missing | Not implemented. |
| HitSelect | Present | Cooldown and optional hurt-time gate for the automatic attack helpers. |
| Backtrack | Missing | Not implemented. |
| FakeLag / Blink-style combat delay | Missing | Not implemented. |
| PingSpoof | Missing | Not implemented. |
| TimerRange / TickBase | Missing | Not implemented. |
| AntiInvis | Missing | Not implemented. |
| AntiBot | Missing | Not implemented. |
| FightBot | Missing | Not implemented. |
| AutoEXP / AutoBuff / healing-item settings | Missing | Not implemented. |

## 3. Movement And Mobility

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| AutoSprint / ToggleSprint | Partial | AutoSprint exists; no separate toggle-sprint mode. |
| ToggleSneak / Sneak | Missing | Not implemented. |
| AutoWalk / Follow | Missing | Not implemented. |
| AutoSwim | Missing | Not implemented. |
| AirJump | Missing | Not implemented. |
| HighJump | Missing | Not implemented. |
| LongJump | Missing | Not implemented. |
| Speed | Missing | Not implemented. |
| Strafe | Missing | Not implemented. |
| TargetStrafe | Missing | Not implemented. |
| Step | Missing | Not implemented. |
| ReverseStep | Missing | Not implemented. |
| Spider | Missing | Not implemented. |
| Flight / Fly | Missing | Not implemented. |
| ElytraFly / ElytraBoost | Missing | Not implemented. |
| ElytraTarget | Missing | Not implemented. |
| ElytraRecast | Missing | Not implemented. |
| ExtendedFirework | Missing | Not implemented. |
| TridentBoost | Missing | Not implemented. |
| VehicleBoost / VehicleControl | Missing | Not implemented. |
| BoatFly | Missing | Not implemented. |
| EntityControl | Missing | Not implemented. |
| Glide | Missing | Not implemented. |
| Jetpack | Missing | Not implemented. |
| Dolphin / SnowShoe | Missing | Not implemented. |
| NoFall | Missing | Not implemented. |
| NoSlow / NoSlowdown | Missing | Not implemented. |
| NoWeb | Missing | Not implemented. |
| NoJumpDelay | Missing | Not implemented. |
| NoLevitation / AntiLevitation | Missing | Not implemented. |
| AntiVoid | Missing | Not implemented. |
| AntiBounce | Missing | Not implemented. |
| AntiEntityPush / NoPush | Missing | Not implemented. |
| AntiWaterPush | Missing | Not implemented. |
| AvoidHazards | Missing | Not implemented. |
| SafeWalk / Eagle | Missing | Not implemented. |
| Parkour | Missing | Not implemented. |
| GUI Move / InventoryMove | Missing | Not implemented. |
| Blink | Missing | Not implemented. |
| ClickTP / Teleport | Missing | Not implemented. |
| NoClip / CameraNoClip | Missing | Not implemented. |
| Phase | Missing | Not implemented. |
| LiquidWalk / Jesus | Missing | Not implemented. |
| Anchor | Missing | Not implemented. |
| BlockWalk | Missing | Not implemented. |
| Freeze | Missing | Not implemented. |
| SnapTap | Missing | Not implemented. |
| NoPose | Missing | Not implemented. |
| AutoDodge | Missing | Not implemented. |

## 4. Farming, World Interaction, Building And Mining

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| AutoFarm | Present | Mature crops, replanting, stem-grown gourds and upper sugar cane in reach. |
| AutoFish | Present | Reacts to a splash near the player-owned bobber, reels after a delay and optionally recasts; requires a rod. |
| BonemealAura | Present | Applies hotbar bone meal to nearby fertilizable plants; crops-only mode is available. |
| AutoBreed | Present | Feeds eligible visible adult animals matching hotbar breeding food, with retry and delay controls. |
| AutoShear | Present | Uses hotbar shears on visible, eligible nearby sheep and restores the selected slot. |
| AutoMobHeal | Missing | Not implemented. |
| AutoOminousBottle | Missing | Not implemented. |
| TreeBot | Partial | VeinMiner can include matching connected logs within reach; no pathfinding/tree-farm bot. |
| AutoMine | Partial | Nuker/VeinMiner mine nearby blocks; no selected-resource pathfinding miner. |
| Excavator | Missing | Not implemented. |
| InfinityMiner | Separate | Requires pathfinding and long-running inventory/mining orchestration. |
| Tunneller / HighwayBuilder | Missing | Not implemented. |
| VeinMiner | Present | Bounded connected matching ore/log group; normal mining, held attack, reach limited. |
| Nuker | Present | Nearby normal block-breaking progress; does not make server mining instantaneous. |
| Legit Nuker | Partial | Nuker uses normal breaking progress, range and delay controls; no separate named mode. |
| SpeedNuker | Missing | Not implemented. |
| FastBreak | Missing | Not implemented. |
| PacketMine | Missing | Not implemented. |
| NoSlowBreak / InstantRebreak | Missing | Not implemented. |
| AutoTool | Present | Chooses the fastest hotbar mining tool and restores the prior slot. |
| FastPlace | Present | Repeats normal held-use block placement at a configurable interval. |
| AirPlace | Missing | Not implemented. |
| Scaffold / ScaffoldWalk | Missing | Not implemented. |
| AutoBuild | Missing | Not implemented. |
| BuildRandom / RandomPlace | Missing | Not implemented. |
| TemplateTool | Missing | Not implemented. |
| InstantBunker | Missing | Not implemented. |
| AutoSign | Missing | Not implemented. |
| AutoSmelter | Missing | Not implemented. |
| AutoBrewer | Missing | Not implemented. |
| AutoCrafter | Missing | Not implemented. |
| AutoMount | Missing | Not implemented. |
| AutoNametag | Missing | Not implemented. |
| AutoLibrarian | Missing | Not implemented. |
| EChestFarmer | Missing | Not implemented. |
| StashFinder | Missing | Not implemented. |
| SpawnProofer | Missing | Not implemented. |
| LiquidFiller | Missing | Not implemented. |
| LiquidPlace / Liquids | Missing | Not implemented. |
| TileBreaker | Missing | Not implemented. |
| BedDefender / objective-block helper | Missing | Not implemented. |
| BlockTrap | Missing | Not implemented. |
| HoleFiller | Missing | Not implemented. |
| BlockIn | Missing | Not implemented. |
| ProjectilePuncher | Missing | Not implemented. |
| Extinguish | Missing | Not implemented. |
| AutoReplenish | Present | Swaps matching inventory stacks into low or remembered depleted hotbar slots. |
| AutoDeposit | Missing | Not implemented. |
| AutoShop | Separate | Requires explicit supported server/menu protocols. |
| ChestStealer / AutoSteal | Missing | Not implemented. |
| ChestCleaner | Missing | Not implemented. |
| InventoryCleaner | Missing | Not implemented. |
| AutoDrop | Missing | Not implemented. |
| Restock | Partial | AutoReplenish covers matching inventory-to-hotbar swaps; no container restock profiles. |

## 5. Visuals, Esp, Camera And World Information

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| ESP / EntityESP | Present | Client glow-outline ESP with range and entity-category settings. |
| PlayerESP | Partial | Player highlighting is a setting of ESP, rather than a separate module. |
| MobESP / MobOverlay | Partial | Hostile/passive highlighting is a setting of ESP; no separate mob overlay renderer. |
| MobSpawnESP | Missing | Not implemented. |
| ItemESP | Missing | Not implemented. |
| ChestESP / StorageESP | Missing | Not implemented. |
| BlockESP | Missing | Not implemented. |
| HoleESP | Missing | Not implemented. |
| VoidESP | Missing | Not implemented. |
| BarrierESP | Missing | Not implemented. |
| OpenWaterESP | Missing | Not implemented. |
| PortalESP | Missing | Not implemented. |
| ProphuntESP | Missing | Not implemented. |
| ProtectionZones | Missing | Not implemented. |
| Radar | Missing | Not implemented. |
| Tracers | Missing | Not implemented. |
| Trajectories | Missing | Not implemented. |
| Breadcrumbs / Trail | Missing | Not implemented. |
| Waypoints / Markers | Missing | Not implemented. |
| NewChunks | Missing | Not implemented. |
| LogoffSpots / LogoutSpots | Missing | Not implemented. |
| BaseFinder | Missing | Not implemented. |
| PlayerFinder | Missing | Not implemented. |
| CaveFinder | Missing | Not implemented. |
| XRay / WallHack | Missing | Not implemented. |
| TrueSight | Missing | Not implemented. |
| Fullbright / Brightness | Present | Client night vision; preserves a preexisting effect. |
| LightLevelOverlay | Partial | InfoHUD displays a numeric light sample at the exposed targeted face or player position; no per-block floor overlay. |
| BlockOverlay / BlockOutline / BlockSelection | Missing | Not implemented. |
| BlockIndicator | Partial | InfoHUD can display the targeted block name; no expanded block-properties panel. |
| Freecam | Missing | Not implemented. |
| FreeLook / Freelook | Missing | Not implemented. |
| POV / RemoteView | Missing | Not implemented. |
| CameraNoClip / CameraClip | Missing | Not implemented. |
| CameraDistance | Missing | Not implemented. |
| SmoothCamera | Missing | Not implemented. |
| AutoF5 / QuickPerspectiveSwap / AutoPerspective | Missing | Not implemented. |
| Zoom | Present | Toggle lower FOV, configurable 30–90; restores the previous FOV when disabled. |
| FOV Changer / NoFOV | Missing | Not implemented. |
| Aspect | Missing | Not implemented. |
| ViewModel / HandView | Missing | Not implemented. |
| SilentHotbar | Missing | Not implemented. |
| Chams / ItemChams | Missing | Not implemented. |
| CrystalView | Missing | Not implemented. |
| Hitboxes | Missing | Not implemented. |
| Nametags | Missing | Not implemented. |
| HealthTags / DamageIndicator | Missing | Not implemented. |
| MobOwners | Missing | Not implemented. |
| ItemTags / Tooltips | Missing | Not implemented. |
| AntiBlind | Missing | Not implemented. |
| NoRender | Missing | Not implemented. |
| NoFireOverlay | Missing | Not implemented. |
| NoFog / CustomFog | Missing | Not implemented. |
| NoWeather / WeatherChanger | Missing | Not implemented. |
| NoHurtcam | Missing | Not implemented. |
| NoBob | Missing | Not implemented. |
| NoSwing | Missing | Not implemented. |
| NoOverlay | Missing | Not implemented. |
| NoPumpkin / NoShieldOverlay | Missing | Not implemented. |
| AntiWobble | Missing | Not implemented. |
| CustomAmbience / TimeChanger | Missing | Not implemented. |
| Animations | Missing | Not implemented. |
| ItemPhysics | Missing | Not implemented. |
| Particles / DamageParticles / HitFX | Missing | Not implemented. |
| JumpEffect | Missing | Not implemented. |
| TotemEffect | Missing | Not implemented. |
| PotionFX | Missing | Not implemented. |
| Hats / Wings | Missing | Not implemented. |
| SkinChanger / SkinDerp | Missing | Not implemented. |
| CombineMobs / BossStack | Missing | Not implemented. |

## 6. Hud, Pvp Statistics And Qol Displays

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| FPS Counter | Present | InfoHUD displays the client-reported FPS. |
| CPS Counter | Missing | Not implemented. |
| Keystrokes | Missing | Not implemented. |
| Mousestrokes | Missing | Not implemented. |
| Coordinates | Present | InfoHUD displays current block X/Y/Z coordinates; no dimension/location metadata. |
| Direction HUD | Present | InfoHUD displays the horizontal cardinal facing direction. |
| Ping Display | Missing | Not implemented. |
| TPS Display | Missing | Not implemented. |
| Memory / System Resources | Missing | Not implemented. |
| Speed Meter | Present | InfoHUD displays horizontal velocity in blocks per second. |
| Time / Stopwatch / Playtime | Missing | Not implemented. |
| Server Address | Missing | Not implemented. |
| Reach Display | Missing | Not implemented. |
| Combo Counter / Combo Display | Missing | Not implemented. |
| PvP Info | Missing | Not implemented. |
| Armor Status / Armor Bar | Partial | InfoHUD can show remaining durability for the lowest equipped item; no armor-point/bar or per-slot layout. |
| Potion Effects | Missing | Not implemented. |
| Hearts / Health HUD | Missing | Not implemented. |
| Item Counter / Item Tracker | Missing | Not implemented. |
| Item Despawn | Missing | Not implemented. |
| Loot Beams | Missing | Not implemented. |
| Attack Indicator | Missing | Not implemented. |
| Hit Indicator / Jump Reset Indicator | Missing | Not implemented. |
| Saturation | Missing | Not implemented. |
| UHC Overlay | Missing | Not implemented. |
| Scoreboard / Tablist / BetterTab | Missing | Not implemented. |
| Boss Bar | Missing | Not implemented. |
| Custom F3 / Debug HUD | Missing | Not implemented. |
| Custom Crosshair | Missing | Not implemented. |
| Direction / Team Tracker | Missing | Not implemented. |
| Player Model HUD | Missing | Not implemented. |
| Death Info | Missing | Not implemented. |
| Item Info | Missing | Not implemented. |
| Shulker Tooltips | Missing | Not implemented. |
| Pack Display / Pack Organizer | Missing | Not implemented. |
| Subtitles / Sound Filters | Missing | Not implemented. |
| Custom Chat | Missing | Not implemented. |
| Title Tweaker | Missing | Not implemented. |
| Toast Control / Autohide HUD | Missing | Not implemented. |
| Dark Mode | Partial | Gray Client menu is charcoal gray; vanilla screens are not globally reskinned. |
| UI Scaling | Partial | Uses Minecraft GUI scale with adaptive layout; no independent UI-scale slider. |
| FOV / Motion Blur / Color Saturation | Missing | Not implemented. |
| Custom Advancements Screen | Missing | Not implemented. |
| Screenshot Tools | Missing | Not implemented. |
| Scrollable Tooltips | Missing | Not implemented. |
| Drop Prevention | Missing | Not implemented. |
| Item Physics / Glint | Missing | Not implemented. |
| Tier Tagger | Separate | Requires a supported external ranking source. |
| Hypixel / Skyblock HUD helpers | Separate | Requires a separately defined server/game-mode integration. |

## 7. Player, Inventory, Chat And Miscellaneous Automation

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| AntiAFK | Missing | Not implemented. |
| AntiHunger | Missing | Not implemented. |
| AutoReconnect / Reconnect | Missing | Not implemented. |
| AutoQueue | Separate | Requires a specific supported server queue protocol. |
| AutoChatGame | Separate | Requires specific game/chat formats. |
| AutoText / Macros | Missing | Not implemented. |
| BetterChat / FancyChat | Missing | Not implemented. |
| AntiSpam | Missing | Not implemented. |
| AutoComplete | Missing | Not implemented. |
| ChatTranslator | Separate | Requires translation provider, privacy policy and service configuration. |
| NoChatReports | Missing | Not implemented. |
| NoTelemetry | Missing | Not implemented. |
| InfiniChat | Missing | Not implemented. |
| Spammer / LogSpammer | Missing | Not implemented. |
| MessageAura | Missing | Not implemented. |
| BookBot / BookHack | Missing | Not implemented. |
| Notebot | Missing | Not implemented. |
| AntiBot / AntiStaff | Missing | Not implemented. |
| AntiCheatDetect / FlagCheck | Missing | Not implemented. |
| AntiExploit | Missing | Not implemented. |
| BetterInventory / InventoryTweaks / ItemScroller | Missing | Not implemented. |
| MiddleClickAction / MiddleClickExtra | Missing | Not implemented. |
| AutoAccount / AltManager | Separate | Requires account/authentication architecture; no credentials are collected here. |
| NameProtect / NickHider | Missing | Not implemented. |
| TextFieldProtect | Missing | Not implemented. |
| Notifier | Missing | Not implemented. |
| ReportHelper | Missing | Not implemented. |
| DiscordRPC / DiscordPresence | Separate | Requires a Discord presence integration. |
| Voice Chat | Separate | Requires compatible peers, networking and audio integration. |
| Friend list / Middle Click Friends | Missing | Not implemented. |
| Teams | Missing | Not implemented. |
| InventoryTracker | Missing | Not implemented. |
| PotionSaver | Missing | Not implemented. |
| AutoDisable | Partial | Disconnect cleanup turns modules off; configurable trigger rules are absent. |
| GUICloser | Missing | Not implemented. |
| PacketLogger / DebugRecorder | Missing | Not implemented. |
| PacketCanceller / SoundBlocker | Missing | Not implemented. |
| BetterBeacons | Missing | Not implemented. |
| Last Server / ServerFinder | Missing | Not implemented. |
| Server cleanup / log cleanup | Missing | Not implemented. |
| AutoMount / entity interaction helpers | Missing | Not implemented. |
| FakePlayer | Missing | Not implemented. |

## 8. Advanced / Exploit-Like Features Found In Some Hacked Clients

| Catalog feature | Status | Actual coverage or limit |
| --- | --- | --- |
| GhostHand / HandNoClip | Missing | Not implemented. |
| NoMiningTrace / NoInteract / NoBlockInteract / NoEntityInteract | Missing | Not implemented. |
| MultiActions / Multitask | Missing | Not implemented. |
| AbortBreaking | Missing | Not implemented. |
| AntiReducedDebugInfo | Missing | Not implemented. |
| Clip | Missing | Not implemented. |
| Damage | Missing | Not implemented. |
| Dupe | Missing | Not implemented. |
| ServerCrasher / NocomCrash / CrashChest / Kaboom | Missing | Not implemented. |
| Kick / VehicleOneHit / MountBypass | Missing | Not implemented. |
| Disabler / ResetVL / YesCheat+ | Missing | Not implemented. |
| Plugins / NameCollector | Missing | Not implemented. |
| MoreCarry / XCarry | Missing | Not implemented. |
| PortalMenu / SleepWalker / TimeShift | Missing | Not implemented. |
| NoRotateSet / NoSlotSet / PotionSpoof | Missing | Not implemented. |
| ForceOP / OP-Sign / ItemGenerator / KillPotion / TrollPotion | Missing | Not implemented. |
| VanillaSpoof / ServerSpoof / YggdrasilSignatureFix | Missing | Not implemented. |
| FastUse / FastEXP | Missing | Not implemented. |
| Timer | Missing | Not implemented. |

## Maintenance

Check `src/main/java/com/grayclient/Modules.java` before changing an implementation claim. Update this matrix when modules are added, renamed or removed. Do not count aliases, reference-client features or empty placeholders as implemented modules.
