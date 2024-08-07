package me.pikamug.quests.listeners;

import lol.pyr.znpcsplus.api.event.NpcInteractEvent;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.api.npc.Npc;
import me.pikamug.quests.BukkitQuestsPlugin;
import me.pikamug.quests.enums.ObjectiveType;
import me.pikamug.quests.player.BukkitQuestProgress;
import me.pikamug.quests.player.Quester;
import me.pikamug.quests.quests.BukkitQuest;
import me.pikamug.quests.quests.Quest;
import me.pikamug.quests.quests.components.BukkitStage;
import me.pikamug.quests.util.BukkitItemUtil;
import me.pikamug.quests.util.BukkitLang;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for ZNPCsPlus 2.x+
 */
public class BukkitZnpcsApiListener implements Listener {

    private final BukkitQuestsPlugin plugin;

    public BukkitZnpcsApiListener(final BukkitQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCInteract(final NpcInteractEvent event) {
        if (plugin.getDependencies().getZnpcsPlusApi() == null) {
            return;
        }
        // Needed because the NpcInteractEvent is fired async
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getClickType().equals(InteractionType.LEFT_CLICK)) {
                if (plugin.getQuestFactory().getSelectingNpcs().contains(event.getPlayer().getUniqueId())) {
                    if (event.getNpc() == null) {
                        plugin.getLogger().severe("ZNPCsPlus was null while selecting by left-click");
                        return;
                    }
                    event.getPlayer().acceptConversationInput(String.valueOf(event.getNpc().getUuid()));
                }
            } else if (event.getClickType().equals(InteractionType.RIGHT_CLICK)) {
                if (plugin.getQuestFactory().getSelectingNpcs().contains(event.getPlayer().getUniqueId())) {
                    if (event.getNpc() == null) {
                        plugin.getLogger().severe("ZNPCsPlus was null while selecting by right-click");
                        return;
                    }
                    event.getPlayer().acceptConversationInput(String.valueOf(event.getNpc().getUuid()));
                }
                if (!event.getPlayer().isConversing()) {
                    final Player player = event.getPlayer();
                    final Quester quester = plugin.getQuester(player.getUniqueId());
                    for (final Quest quest : quester.getCurrentQuests().keySet()) {
                        final BukkitStage currentStage = (BukkitStage) quester.getCurrentStage(quest);
                        if (currentStage.containsObjective(ObjectiveType.DELIVER_ITEM)) {
                            final ItemStack hand = player.getItemInHand();
                            int currentIndex = -1;
                            final LinkedList<Integer> matches = new LinkedList<>();
                            int reasonCode = 0;
                            for (final ItemStack is : currentStage.getItemsToDeliver()) {
                                currentIndex++;
                                reasonCode = BukkitItemUtil.compareItems(is, hand, true);
                                if (reasonCode == 0) {
                                    matches.add(currentIndex);
                                }
                            }
                            final Npc clicked = event.getNpc();
                            if (!matches.isEmpty()) {
                                for (final Integer match : matches) {
                                    final UUID uuid = currentStage.getItemDeliveryTargets().get(match);
                                    if (uuid.equals(clicked.getUuid())) {
                                        quester.deliverToNPC(quest, uuid, hand);
                                        return;
                                    }
                                }
                            } else if (!hand.getType().equals(Material.AIR)) {
                                for (final UUID uuid : currentStage.getItemDeliveryTargets()) {
                                    if (uuid.equals(clicked.getUuid())) {
                                        String text = "";
                                        final boolean hasMeta = hand.getItemMeta() != null;
                                        if (hasMeta) {
                                            text += ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC
                                                    + (hand.getItemMeta().hasDisplayName() ? hand.getItemMeta().getDisplayName()
                                                    + ChatColor.GRAY + " (" : "");
                                        }
                                        text += ChatColor.AQUA + "<item>" + (hand.getDurability() != 0 ? (":" + ChatColor.BLUE
                                                + hand.getDurability()) : "") + ChatColor.GRAY;
                                        if (hasMeta) {
                                            text += (hand.getItemMeta().hasDisplayName() ? ")" : "");
                                        }
                                        text += " x " + ChatColor.DARK_AQUA + hand.getAmount() + ChatColor.GRAY;
                                        if (plugin.getConfigSettings().canTranslateNames() && !hasMeta
                                                && !hand.getItemMeta().hasDisplayName()) {
                                            plugin.getLocaleManager().sendMessage(player, BukkitLang
                                                    .get(player, "questInvalidDeliveryItem").replace("<item>", text), hand
                                                    .getType(), hand.getDurability(), null);
                                        } else {
                                            player.sendMessage(BukkitLang.get(player, "questInvalidDeliveryItem")
                                                    .replace("<item>", text).replace("<item>", BukkitItemUtil.getName(hand)));
                                        }
                                        switch (reasonCode) {
                                            case 1:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "one item is null"));
                                                break;
                                            case 0:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "ERROR"));
                                                break;
                                            case -1:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "name"));
                                                break;
                                            case -2:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "amount"));
                                                break;
                                            case -3:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "durability"));
                                                break;
                                            case -4:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "display name or lore"));
                                                break;
                                            case -5:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "enchantments"));
                                                break;
                                            case -6:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "stored enchants"));
                                                break;
                                            case -7:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "item flags"));
                                                break;
                                            case -8:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "book data"));
                                                break;
                                            case -9:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "potion type"));
                                                break;
                                            case -10:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "fish variant"));
                                                break;
                                            default:
                                                player.sendMessage(ChatColor.GRAY + BukkitLang.get(player, "difference")
                                                        .replace("<data>", "unknown"));
                                        }
                                        if (hasMeta) {
                                            if (hand.getType().equals(Material.ENCHANTED_BOOK)) {
                                                final EnchantmentStorageMeta esMeta = (EnchantmentStorageMeta) hand.getItemMeta();
                                                if (esMeta.hasStoredEnchants()) {
                                                    for (final Map.Entry<Enchantment, Integer> e : esMeta.getStoredEnchants()
                                                            .entrySet()) {
                                                        final HashMap<Enchantment, Integer> single = new HashMap<>();
                                                        single.put(e.getKey(), e.getValue());
                                                        plugin.getLocaleManager().sendMessage(player, ChatColor.GRAY + "\u2515 "
                                                                + ChatColor.DARK_GREEN + "<enchantment> <level>\n", single);
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    boolean hasObjective = false;
                    for (final Quest quest : quester.getCurrentQuests().keySet()) {
                        if (!quester.meetsCondition(quest, true)) {
                            continue;
                        }
                        if (quester.getCurrentStage(quest).containsObjective(ObjectiveType.TALK_TO_NPC)) {
                            if (quester.getCurrentStage(quest).getNpcsToInteract().contains(event.getNpc().getUuid())) {
                                final int npcIndex = quester.getCurrentStage(quest).getNpcsToInteract().indexOf(event.getNpc()
                                        .getUuid());
                                if (quester.getQuestProgressOrDefault(quest) != null && npcIndex > -1
                                        && !((BukkitQuestProgress) quester.getQuestProgressOrDefault(quest)).npcsInteracted.get(npcIndex)) {
                                    hasObjective = true;
                                }
                                quester.interactWithNPC(quest, event.getNpc().getUuid());
                            }
                        }
                    }
                    if (hasObjective || !plugin.getQuestNpcUuids().contains(event.getNpc().getUuid())) {
                        return;
                    }
                    boolean hasAtLeastOneGUI = false;
                    final LinkedList<Quest> npcQuests = new LinkedList<>();
                    for (final Quest quest : plugin.getLoadedQuests()) {
                        final BukkitQuest bukkitQuest = (BukkitQuest) quest;
                        if (quester.getCurrentQuests().containsKey(bukkitQuest)) {
                            continue;
                        }
                        if (bukkitQuest.getNpcStart() != null && bukkitQuest.getNpcStart().equals(event.getNpc().getUuid())) {
                            if (plugin.getConfigSettings().canIgnoreLockedQuests()
                                    && (!quester.getCompletedQuests().contains(bukkitQuest)
                                    || bukkitQuest.getPlanner().getCooldown() > -1)) {
                                if (bukkitQuest.testRequirements(quester)) {
                                    npcQuests.add(bukkitQuest);
                                    if (bukkitQuest.getGUIDisplay() != null) {
                                        hasAtLeastOneGUI = true;
                                    }
                                }
                            } else if (!quester.getCompletedQuests().contains(bukkitQuest) || bukkitQuest.getPlanner().getCooldown() > -1) {
                                npcQuests.add(bukkitQuest);
                                if (bukkitQuest.getGUIDisplay() != null) {
                                    hasAtLeastOneGUI = true;
                                }
                            }
                        }
                    }
                    if (npcQuests.size() == 1) {
                        final BukkitQuest quest = (BukkitQuest) npcQuests.get(0);
                        if (quester.canAcceptOffer(quest, true)) {
                            quester.setQuestIdToTake(quest.getId());
                            if (!plugin.getConfigSettings().canConfirmAccept()) {
                                quester.takeQuest(quest, false);
                            } else {
                                if (quest.getGUIDisplay() != null) {
                                    quester.showGUIDisplay(event.getNpc().getUuid(), npcQuests);
                                } else {
                                    for (final String msg : extracted(quester).split("<br>")) {
                                        player.sendMessage(msg);
                                    }
                                    plugin.getConversationFactory().buildConversation(player).begin();
                                }
                            }
                        }
                    } else if (npcQuests.size() > 1) {
                        if (hasAtLeastOneGUI) {
                            quester.showGUIDisplay(event.getNpc().getUuid(), npcQuests);
                        } else {
                            final Conversation c = plugin.getNpcConversationFactory().buildConversation(player);
                            c.getContext().setSessionData("npcQuests", npcQuests);
                            //c.getContext().setSessionData("npc", event.getNpc().getGameProfile().getName());
                            c.getContext().setSessionData("npc", plugin.getDependencies().getNpcName(event.getNpc().getUuid()));
                            c.begin();
                        }
                    } else {
                        BukkitLang.send(player, ChatColor.YELLOW + BukkitLang.get(player, "noMoreQuest"));
                    }
                }
            }
        });
    }

    private String extracted(final Quester quester) {
        final Quest quest = plugin.getQuestById(quester.getQuestIdToTake());
        return MessageFormat.format("{0}- {1}{2}{3} -\n\n{4}{5}\n", ChatColor.GOLD, ChatColor.DARK_PURPLE,
                quest.getName(), ChatColor.GOLD, ChatColor.RESET, quest.getDescription());
    }
}
