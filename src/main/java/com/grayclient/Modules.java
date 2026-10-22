package com.grayclient;

import java.util.List;

public class Modules {
    // combat
    public static final CombatModules.SilentAura AURA = new CombatModules.SilentAura();
    public static final CombatModules.AimAssist AIM = new CombatModules.AimAssist();
    public static final CombatModules.TriggerBot TRIGGER = new CombatModules.TriggerBot();
    public static final CombatModules.AutoClicker CLICKER = new CombatModules.AutoClicker();
    public static final CombatModules.HitSelect HIT_SELECT = new CombatModules.HitSelect();
    public static final CombatModules.WTap WTAP = new CombatModules.WTap();
    public static final CombatModules.Velocity VELOCITY = new CombatModules.Velocity();
    public static final CombatModules.CrystalAssist CRYSTAL_ASSIST = new CombatModules.CrystalAssist();
    public static final CombatModules.CrystalAura CRYSTAL_AURA = new CombatModules.CrystalAura();
    public static final CombatModules.MaceBot MACE = new CombatModules.MaceBot();
    public static final CombatModules.AutoWeapon AUTO_WEAPON = new CombatModules.AutoWeapon();
    public static final CombatModules.ShieldBreaker SHIELD_BREAKER = new CombatModules.ShieldBreaker();
    public static final CombatModules.BowRelease BOW_RELEASE = new CombatModules.BowRelease();
    // player
    public static final UtilityModules.AutoSprint AUTO_SPRINT = new UtilityModules.AutoSprint();
    public static final UtilityModules.AutoEat AUTO_EAT = new UtilityModules.AutoEat();
    public static final UtilityModules.AutoTotem AUTO_TOTEM = new UtilityModules.AutoTotem();
    public static final UtilityModules.AutoRespawn AUTO_RESPAWN = new UtilityModules.AutoRespawn();
    public static final UtilityModules.AutoTool AUTO_TOOL = new UtilityModules.AutoTool();
    // farming
    public static final FarmModules.AutoFarm AUTO_FARM = new FarmModules.AutoFarm();
    public static final FarmModules.Nuker NUKER = new FarmModules.Nuker();
    public static final SurvivalModules.VeinMiner VEIN_MINER = new SurvivalModules.VeinMiner();
    public static final SurvivalModules.OreFinder ORE_FINDER = new SurvivalModules.OreFinder();
    public static final SurvivalModules.AutoPlant AUTO_PLANT = new SurvivalModules.AutoPlant();
    public static final SurvivalModules.AutoReplenish AUTO_REPLENISH = new SurvivalModules.AutoReplenish();
    public static final SurvivalModules.FastPlace FAST_PLACE = new SurvivalModules.FastPlace();
    public static final AdditionalFarmModules.AutoFish AUTO_FISH = new AdditionalFarmModules.AutoFish();
    public static final AdditionalFarmModules.AutoShear AUTO_SHEAR = new AdditionalFarmModules.AutoShear();
    public static final AdditionalFarmModules.AutoBreed AUTO_BREED = new AdditionalFarmModules.AutoBreed();
    public static final AdditionalFarmModules.BonemealAura BONEMEAL = new AdditionalFarmModules.BonemealAura();
    // visual
    public static final VisualModules.Fullbright FULLBRIGHT = new VisualModules.Fullbright();
    public static final VisualModules.Esp ESP = new VisualModules.Esp();
    public static final VisualModules.Hud HUD = new VisualModules.Hud();
    public static final InfoModules.InfoHUD INFO_HUD = new InfoModules.InfoHUD();
    public static final InfoModules.Zoom ZOOM = new InfoModules.Zoom();

    public static final List<Module> ALL = List.of(
            AURA, AIM, TRIGGER, CLICKER, HIT_SELECT, WTAP, VELOCITY, CRYSTAL_ASSIST, CRYSTAL_AURA, MACE, AUTO_WEAPON,
            SHIELD_BREAKER, BOW_RELEASE,
            AUTO_SPRINT, AUTO_EAT, AUTO_TOTEM, AUTO_RESPAWN, AUTO_TOOL, AUTO_REPLENISH, FAST_PLACE,
            AUTO_FARM, NUKER, VEIN_MINER, ORE_FINDER, AUTO_PLANT,
            AUTO_FISH, AUTO_SHEAR, AUTO_BREED, BONEMEAL,
            FULLBRIGHT, ESP, HUD, INFO_HUD, ZOOM);
}
