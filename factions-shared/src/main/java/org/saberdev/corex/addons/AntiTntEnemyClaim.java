package org.saberdev.corex.addons;

import com.cryptomorin.xseries.XMaterial;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.saberdev.corex.CoreAddon;
import org.saberdev.corex.CoreX;

@CoreAddon(configVariable = "Anti-TNT-Enemy-Claim")
public class AntiTntEnemyClaim implements Listener {

    private static final String META_FACTION_ID = "saber-tnt-source-faction";

    private final Material tntMaterial = XMaterial.TNT.parseMaterial();

    public AntiTntEnemyClaim() {
        if (!CoreX.handleFeatureRegistry("Anti-TNT-Enemy-Claim")) return;
        Bukkit.getScheduler().runTaskTimer(FactionsPlugin.getInstance(), this::scan, 5L, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed) {
            tagFromIgniter(entity, ((TNTPrimed) entity).getSource());
        } else if (isTntItem(entity)) {
            tagFromThrower((Item) entity);
        } else {
            return;
        }
        if (shouldDespawn(entity)) {
            entity.remove();
        }
    }

    private void scan() {
        for (World world : Bukkit.getWorlds()) {
            for (TNTPrimed tnt : world.getEntitiesByClass(TNTPrimed.class)) {
                if (shouldDespawn(tnt)) tnt.remove();
            }
            if (tntMaterial == null) continue;
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getItemStack().getType() != tntMaterial) continue;
                if (shouldDespawn(item)) item.remove();
            }
        }
    }

    private boolean isTntItem(Entity entity) {
        return tntMaterial != null
                && entity instanceof Item
                && ((Item) entity).getItemStack().getType() == tntMaterial;
    }

    private void tagFromIgniter(Entity entity, Entity source) {
        if (!(source instanceof Player)) return;
        FPlayer fp = FPlayers.getInstance().getByPlayer((Player) source);
        if (fp == null || !fp.hasFaction()) return;
        entity.setMetadata(META_FACTION_ID,
                new FixedMetadataValue(FactionsPlugin.getInstance(), fp.getFactionId()));
    }

    private void tagFromThrower(Item item) {
        if (item.getThrower() == null) return;
        FPlayer fp = FPlayers.getInstance().getById(item.getThrower().toString());
        if (fp == null || !fp.hasFaction()) return;
        item.setMetadata(META_FACTION_ID,
                new FixedMetadataValue(FactionsPlugin.getInstance(), fp.getFactionId()));
    }

    private boolean shouldDespawn(Entity entity) {
        if (!entity.hasMetadata(META_FACTION_ID)) return false;
        Faction at = Board.getInstance().getFactionAt(FLocation.wrap(entity.getLocation()));
        if (!at.isNormal()) return false;
        String factionId = entity.getMetadata(META_FACTION_ID).get(0).asString();
        Faction source = Factions.getInstance().getFactionById(factionId);
        if (source == null) return false;
        return source.getRelationTo(at) == Relation.ENEMY;
    }
}