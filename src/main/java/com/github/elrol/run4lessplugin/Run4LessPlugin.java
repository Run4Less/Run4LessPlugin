package com.github.elrol.run4lessplugin;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.FriendChatManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;

@PluginDescriptor(
        name = "Bone Running Plugin",
        description = "A plugin made for Bone Running, commissioned by the Run4Less group",
        tags = {"run4less", "menu", "running", "bone"},
        loadWhenOutdated = true,
        enabledByDefault = false
)
public class Run4LessPlugin extends Plugin {
    @Inject
    private Run4LessConfig config;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private FriendChatManager friendChatManager;

    @Inject
    private Run4LessOverlay run4LessOverlay;

    @Inject
    private Run4LessCCOverlay run4LessCCOverlay;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ClientThread clientThread;

    @Inject
    private Provider<MenuManager> menuManager;

    @Inject
    private ConfigManager configManager;

    private NavigationButton panel;

    private boolean isRunner = false;
    private boolean isHost = false;

    private final ArrayListMultimap<String, Integer> indexes = ArrayListMultimap.create();
    public static RunnerStats stats = RunnerStats.load();
    public static final String setClient = "Set as Client";
    boolean shouldSpam = true;

    @Override
    protected void startUp() throws Exception {
        URL img = new URL("https://i.imgur.com/5NtdRId.png");
        BufferedImage image = ImageIO.read(img);

        run4LessCCOverlay.setFCManager(friendChatManager);
        if(client != null) menuManager.get().addPlayerMenuItem(setClient);
        if(config.splitCCEnabled() && config.ccLines() > 0){
            overlayManager.add(run4LessCCOverlay);
        }
        panel = NavigationButton.builder()
                .tooltip("Run4Less")
                .icon(image)
                .priority(10)
                .panel(new Run4LessPanel())
                .build();
        clientToolbar.addNavigation(panel);

        super.startUp();
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(run4LessOverlay);
        overlayManager.remove(run4LessCCOverlay);
        clientToolbar.removeNavigation(panel);
        super.shutDown();
    }

    @Subscribe(priority = -2)
    public void onChatMessage(ChatMessage message) {
        FriendsChatManager manager = client.getFriendsChatManager();
        if (manager != null && manager.getOwner().equalsIgnoreCase("Run4less")) {
            System.out.println(message.getMessage());
            if (message.getMessage().toLowerCase().contains("!bones ")) {
                System.out.println("Ran bones command");
                String cmd = message.getMessage().toLowerCase().split("!bones ")[1];
                String[] temp = cmd.split(" ");
                int rate = 0;
                if (temp[0].equalsIgnoreCase("afk")) rate = 20000;
                else if (temp[0].equalsIgnoreCase("tick")) rate = 18000;
                else
                    client.addChatMessage(message.getType(), message.getName(), "Invalid argument [" + temp[0] + "]. Options are [tick/afk]", message.getSender());

                int qty = Integer.parseInt(temp[1]);
                if (qty <= 0) {
                    client.addChatMessage(message.getType(), message.getName(), "Invalid argument [" + temp[1] + "]. Options must be greater then 0", message.getSender());
                    qty = 0;
                }
                DecimalFormat formatter = new DecimalFormat("#,###");
                String price = formatter.format(Math.round(((float) qty / 26F) * rate));
                client.addChatMessage(message.getType(), message.getName(), temp[0] + "ing " + temp[1] + " bones would be " + price, message.getSender());
                System.out.println(temp[0] + "ing " + temp[1] + " bones would be " + price);
            }
            if (message.getMessage().equalsIgnoreCase("accepted trade.") && config.enableStats()) {
                Widget tradingWith = client.getWidget(334, 30);
                if (tradingWith != null) {
                    String rsn = tradingWith.getText().replace("Trading with:<br>", "");
                    Widget partnerTrades = client.getWidget(334, 29);
                    Widget offeredTrades = client.getWidget(334, 28);

                    if (partnerTrades != null && offeredTrades != null) {
                        int i = 0;
                        int coins = 0;
                        int notes = 0;
                        int qty = 0;
                        String bones = "";
                        String noted = "";
                        for (Widget w : partnerTrades.getChildren()) {
                            String text = w.getText();
                            if (text.startsWith("Coins")) {
                                if (text.contains("(")) text = text.split("[(]")[1];
                                else text = text.split("<col=ffffff> x <col=ffff00>")[1];
                                text = text.replace(",", "").replace(")", "");
                                coins += Integer.parseInt(text);
                            } else if (text.toLowerCase().contains("bones") && text.contains("<col=ffffff> x <col=ffff00>")) {
                                String[] temp = text.split("<col=ffffff> x <col=ffff00>");
                                noted = temp[0];
                                notes = Integer.parseInt(temp[1]);
                            } else if (text.toLowerCase().contains("bones")) {
                                qty--;
                            }
                        }
                        for (Widget w : offeredTrades.getChildren()) {
                            if (w == null) continue;
                            System.out.println("[" + i++ + "]:" + w.getText());
                            String s = w.getText().toLowerCase();
                            if (s.contains("bones") && !s.contains("<col=ffffff> x <col=ffff00>")) {
                                bones = w.getText();
                                qty++;
                            }
                        }
                        stats.addRun(rsn, bones, qty, coins, notes, noted);
                    }
                }
            }
            String sender = message.getName();
            if (!sender.isEmpty()) {
                FriendsChatMember p = manager.findByName(sender);
                if (p != null) {
                    FriendsChatRank rank = p.getRank();
                    if (rank != FriendsChatRank.UNRANKED && message.getMessage().contains("@runner") && isRunner)
                        TimedNotifier.init("Bone Runner Requested", 30, overlayManager);
                }
            }
        }
        if (config.splitCCEnabled() && config.ccLines() > 0) {
            final Widget chat = client.getWidget(WidgetInfo.CHATBOX_TRANSPARENT_LINES);
            if (message.getType().equals(ChatMessageType.FRIENDSCHAT) && chat != null && !chat.isHidden()) {
                run4LessCCOverlay.init(config.ccLines(), chat.getWidth(), message);
            }
        }
        if (config.filterTradeEnabled() && message.getType().equals(ChatMessageType.TRADE)) {
            removeMessage(message);
        }
        if (config.spamTrade() && message.getType().equals(ChatMessageType.TRADEREQ) && shouldSpam){
            shouldSpam = false;
            for (int i = 0; i < 7; i++) {
                client.addChatMessage(message.getType(), message.getName(), message.getMessage(), message.getSender());
            }
            shouldSpam = true;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event){
        if(event.getGroup().equals("run4less")){
            if(config.splitCCEnabled()) {
                final Widget chat = client.getWidget(WidgetInfo.CHATBOX_TRANSPARENT_LINES);
                if(chat != null) run4LessCCOverlay.init(config.ccLines(), chat.getWidth());
                overlayManager.add(run4LessCCOverlay);
            } else {
                overlayManager.remove(run4LessCCOverlay);
            }
        }
    }

    @Subscribe
    public void onFriendsChatChanged(FriendsChatChanged event){
        clientThread.invokeLater(() -> {
            FriendsChatManager manager = client.getFriendsChatManager();
            Player player = client.getLocalPlayer();
            if(manager != null && player != null && manager.getOwner().equalsIgnoreCase("Run4less")){
                FriendsChatRank rank = manager.findByName(player.getName()).getRank();
                if(rank.equals(FriendsChatRank.FRIEND)){
                    isHost = true;
                    overlayManager.add(run4LessOverlay);
                    return;
                } else if(!rank.equals(FriendsChatRank.OWNER)) {
                    isRunner = true;
                    overlayManager.add(run4LessOverlay);
                    return;
                } else {
                    isRunner = false;
                    isHost = false;
                }
            }
            overlayManager.remove(run4LessOverlay);
        });
    }

    @Subscribe
    public void onClientTick(final ClientTick clientTick) {
        if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()) return;
        if (config.offerAllEnabled()) {
                final MenuEntry[] menuEntries = client.getMenuEntries();
            int index = 0;
            indexes.clear();
            for (MenuEntry entry : menuEntries) {
                final String option = Text.removeTags(entry.getOption()).toLowerCase();
                indexes.put(option, index++);
            }

            index = 0;
            for (MenuEntry menuEntry : menuEntries) {
                index++;
                final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
                final String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();

                if (option.equals("offer")) {
                    final int i = index(indexes, menuEntries, index, option, target);
                    final int id = index(indexes, menuEntries, i, "offer-all", target);

                    if (i >= 0 && id >= 0) {
                        final MenuEntry entry = menuEntries[id];
                        menuEntries[id] = menuEntries[i];
                        menuEntries[i] = entry;

                        client.setMenuEntries(menuEntries);

                        indexes.clear();
                        int idx = 0;
                        for (MenuEntry e : menuEntries) {
                            final String o = Text.removeTags(e.getOption()).toLowerCase();
                            indexes.put(o, idx++);
                        }
                    }
                }
            }
        }
    }

    public static int index(final ArrayListMultimap<String, Integer> optionIndexes, final MenuEntry[] entries, final int limit, final String option, final String target) {
        List<Integer> indexes = optionIndexes.get(option);
        for (int i = indexes.size() - 1; i >= 0; --i) {
            final int idx = indexes.get(i);
            MenuEntry entry = entries[idx];
            String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();
            if (idx <= limit && entryTarget.equals(target))
                return idx;
        }
        return -1;
    }

    @Subscribe
    public void onPlayerMenuOptionClicked(PlayerMenuOptionClicked event){
        if (event.getMenuOption().equals(setClient)){
            configManager.setConfiguration("run4less", "clientName", Text.removeTags(event.getMenuTarget()));
        }
    }

    private void removeMessage(ChatMessage msg){
        ChatLineBuffer ccInfoBuffer = client.getChatLineMap().get(ChatMessageType.TRADE.getType());
        if (ccInfoBuffer != null) {
                ccInfoBuffer.removeMessageNode(msg.getMessageNode());
        }
    }

    @Provides
    Run4LessConfig getConfig(ConfigManager configManager){
        return configManager.getConfig(Run4LessConfig.class);
    }
}