package filexstudios.mc.teleporter;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Teleporter extends JavaPlugin implements Listener {

    private Map<String, Long> cooldowns = new HashMap<>();
    private Map<String, Boolean> playersMoves = new HashMap<>();
    private Map<String, Integer> playersParticleTime = new HashMap<>();
    private static long COOLDOWN_DURATION = 10000; // 10 seconds in milliseconds
    private FileConfiguration config = null;
    private Material tp_material = Material.getMaterial("BLAZE_ROD");
    private int config_version = 1;
    private String display_name = "Teleporter";
    private String seted_text = "USTAWIONY";
    private Long teleport_wait_time = 100L;

    @Override
    public void onEnable() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {

        } else {
            this.saveDefaultConfig();
        }
        config = this.getConfig();
        COOLDOWN_DURATION = config.getLong("cooldown_duration");
        tp_material = Material.getMaterial(config.getString("teleporter_material"));
        display_name = config.getString("display_name");
        seted_text = config.getString("teleporter_set_text");
        teleport_wait_time = config.getLong("teleport_wait_time");

        Bukkit.getPluginManager().registerEvents(this, this);
        LoadRecepture();
    }
    public void LoadRecepture(){
        ItemStack teleporter = new ItemStack(tp_material, 1);
        ItemMeta teleporterMeta = teleporter.getItemMeta();
        teleporterMeta.setDisplayName(display_name);
        String fxtype = "teleporter";
        NamespacedKey key_tp = new NamespacedKey(this, "fxtype");
        PersistentDataContainer container_tp = teleporterMeta.getPersistentDataContainer();
        container_tp.set(key_tp, PersistentDataType.STRING, fxtype);
        teleporterMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        teleporterMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        teleporterMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        teleporter.setItemMeta(teleporterMeta);
        NamespacedKey key = new NamespacedKey(this, "teleporter");
        ShapedRecipe recipe = new ShapedRecipe(key, teleporter);

        //YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("/config.yml"));
        List<String> shape = config.getStringList("teleporter_recipe.shape");
        Map<String, Object> ingredientsConfig = config.getConfigurationSection("teleporter_recipe.ingredients").getValues(false);

        Map<String, String> ingredients = new HashMap<>();
        for (Map.Entry<String, Object> entry : ingredientsConfig.entrySet()) {
            ingredients.put(entry.getKey(), entry.getValue().toString());
        }

        // teraz możesz użyć zmiennej ingredients jako mapy typu Map<String, String>
        List<String> shape2 = config.getStringList("teleporter_recipe.shape");
        if (shape2.size() > 0){
            if (shape2.size() > 1){
                if (shape2.size() > 2){
                    recipe.shape(shape2.get(0), shape2.get(1), shape2.get(2));
                } else {
                    recipe.shape(shape2.get(0), shape2.get(1));
                }
            } else {
                recipe.shape(shape2.get(0));
            }
        } else {
            return;
        }
        for (int y = 0; y < shape.size(); y++) {
            for (int x = 0; x < shape.get(y).length(); x++) {
                String ingredient = ingredients.get(shape.get(y).substring(x, x+1));
                // tutaj możesz użyć zmiennej ingredient, aby dodać składnik do receptury

                // konwertujemy nazwę na obiekt Material
                Material material = Material.getMaterial(ingredient);
                recipe.setIngredient(shape.get(y).charAt(x), material);
            }
        }
        Bukkit.addRecipe(recipe);



    }

    public void RepairConfig(){

        System.out.println("Repairing...");

        InputStream originalConfigStream = getClass().getResourceAsStream("/config.yml");
        YamlConfiguration originalConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(originalConfigStream));

// ładujemy plik konfiguracyjny z dysku serwera
        File serverConfigFile = new File("config.yml");
        YamlConfiguration serverConfig = YamlConfiguration.loadConfiguration(serverConfigFile);

// uzyskujemy sekcję główną orginalnego pliku konfiguracyjnego
        ConfigurationSection originalRootSection = originalConfig.getRoot();

// iterujemy przez wszystkie klucze w sekcji głównej orginalnego pliku konfiguracyjnego
        for (String key : originalRootSection.getKeys(true)) {
            // sprawdzamy, czy dany klucz istnieje w pliku konfiguracyjnym serwera
            if (!serverConfig.contains(key)) {
                // jeśli klucz nie istnieje, dodajemy go do pliku konfiguracyjnego serwera z odpowiednią wartością z orginalnego pliku konfiguracyjnego
                serverConfig.set(key, originalConfig.get(key));
            }
        }
        try {
            serverConfig.save(serverConfigFile);
            System.out.println("Repaired!");
        } catch (IOException e) {

        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("teleporter") && args[0].equals("give")) {
            // Sprawdzamy, czy nadawcą polecenia jest gracz
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.RED + config.getString("only_by_player"));
                return true;
            }

            Player player = (Player) sender;

            // Tworzymy nowy przedmiot o nazwie "Teleporter"
            ItemStack teleporter = new ItemStack(tp_material, 1);
            ItemMeta teleporterMeta = teleporter.getItemMeta();
            teleporterMeta.setDisplayName(display_name);
            String fxtype = "teleporter";
            NamespacedKey key_tp = new NamespacedKey(this, "fxtype");
            PersistentDataContainer container_tp = teleporterMeta.getPersistentDataContainer();
            container_tp.set(key_tp, PersistentDataType.STRING, fxtype);
            teleporterMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
            teleporterMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            teleporterMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            teleporter.setItemMeta(teleporterMeta);

            // Dodajemy przedmiot do ekwipunku gracza
            player.getInventory().addItem(teleporter);
            player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + config.getString("received_teleporter"));

            return true;
        }
        if (command.getName().equalsIgnoreCase("teleporter") && args[0].equals("reload")) {
            reloadConfig();
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.RED + config.getString("only_by_player"));
                return true;
            }

            Player player = (Player) sender;
            config = this.getConfig();
            COOLDOWN_DURATION = config.getLong("cooldown_duration");
            tp_material = Material.getMaterial(config.getString("teleporter_material"));
            display_name = config.getString("display_name");
            seted_text = config.getString("seted_text");
            teleport_wait_time = config.getLong("teleport_wait_time");

            NamespacedKey keytest = new NamespacedKey(this, "teleporter");
            Bukkit.removeRecipe(keytest);
            LoadRecepture();
            Set<OfflinePlayer> operators = Bukkit.getOperators();

            for (OfflinePlayer operator : operators) {
                if (operator.isOnline()) {
                    Player onlineOperator = operator.getPlayer();
                    onlineOperator.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.YELLOW + config.getString("plugin_reloaded"));
                }
            }
            return true;
        }

        return false;


    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        return Arrays.asList("reload", "give");
    }



    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if the player has moved by at least 1 block in any direction
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            playersMoves.put(player.getName(), true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        ItemStack item_test = player.getInventory().getItemInMainHand();
        ItemMeta meta_test = item_test.getItemMeta();
        String type_test_str = null;
        if (!(meta_test == null)){
            PersistentDataContainer container_test = meta_test.getPersistentDataContainer();
            NamespacedKey key_test = new NamespacedKey(this, "fxtype");
            type_test_str = container_test.get(key_test, PersistentDataType.STRING);
        }


        // Sprawdzamy, czy gracz kliknął prawym przyciskiem myszy z przedmiotem "Teleporter"
        Long lastUse = cooldowns.get(player.getName());
        long currentTime = System.currentTimeMillis();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (item == null || item.getType() != tp_material) {

                return;
            } else {
                if (type_test_str == null) {
                    return;

                } else {
                    if (!type_test_str.equals("teleporter")) {
                        return;
                    } else {


                        if (lastUse != null && currentTime - lastUse < COOLDOWN_DURATION) {
                            // Player is still on cooldown
                            long remaining = COOLDOWN_DURATION - (currentTime - lastUse);
                            long seconds = remaining / 1000;
                            player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.RED + config.getString("wait_time_info").replace("%s", String.valueOf(seconds)));
                            return;
                        }
                    }
                }
            }

            cooldowns.put(player.getName(), currentTime);

            ItemStack item2 = player.getInventory().getItemInMainHand();
            ItemMeta meta = item2.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(this, "tpcoords");
            String coordsString = container.get(key, PersistentDataType.STRING);

            if (coordsString != null) {
                String[] parts = coordsString.split(";");
                double x = Double.parseDouble(parts[0].split(":")[1].replace(",", "."));
                double y = Double.parseDouble(parts[1].split(":")[1].replace(",", "."));
                double z = Double.parseDouble(parts[2].split(":")[1].replace(",", "."));
                World world = Bukkit.getWorld(parts[3].split(":")[1].replace(",", "."));
                Location location = new Location(world, x, y, z);
                playersParticleTime.put(player.getName(), 0);
                int taskId2 = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                    for (int r = 0; r < 20; r++){
                        double angle = playersParticleTime.get(player.getName()) * Math.PI / 45;
                        double x2 = 0.5 * Math.cos(angle);
                        double y2 = 1.0 / 180 * playersParticleTime.get(player.getName());
                        double z2 = 0.5 * Math.sin(angle);
                        player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation().add(x2, y2, z2), 1, new Particle.DustOptions(Color.YELLOW, 1));
                        playersParticleTime.put(player.getName(), playersParticleTime.get(player.getName()) + 1);
                        if (1.0 / 180 * playersParticleTime.get(player.getName()) > 2){
                            playersParticleTime.put(player.getName(), 0);
                        }
                    }
                }, 0, 1).getTaskId();
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Bukkit.getScheduler().cancelTask(taskId2);
                }, 100);
                playersMoves.put(player.getName(), false);
                player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + config.getString("teleporting_info").replace("%s", String.valueOf(teleport_wait_time / 20)));
                if (false){
                    if (teleport_wait_time / 20 < 2) {
                        player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + "Teleportowanie... (Zaczekaj " + teleport_wait_time / 20 + " sekundę)");
                    }
                    if (teleport_wait_time / 20 > 1 && teleport_wait_time / 20 < 5) {
                        player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + "Teleportowanie... (Zaczekaj " + teleport_wait_time / 20 + " sekundy)");
                    }
                    if (teleport_wait_time / 20 > 4) {
                        player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + "Teleportowanie... (Zaczekaj " + teleport_wait_time / 20 + " sekund)");
                    }
                }
                item.setAmount(item.getAmount() - 1);
                BukkitScheduler scheduler = Bukkit.getScheduler();
                int taskId = scheduler.scheduleSyncDelayedTask(this, () -> {
                    if (!playersMoves.get(player.getName())) {
                        player.teleport(location);
                        player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + config.getString("teleported"));
                    } else {
                        if (config.getBoolean("cancel_tp_when_player_moves")){
                            player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.RED + config.getString("teleport_canceled"));
                        } else {
                            player.teleport(location);
                            player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + config.getString("teleported"));
                        }
                    }
                    }, teleport_wait_time);

            } else {
                // Tworzymy nowe metadane z niestandardowymi danymi
                Location location = player.getLocation();
                String coordsString2 = String.format("x:%.2f;y:%.2f;z:%.2f;world:%s", location.getX(), location.getY(), location.getZ(), location.getWorld().getName());
                NamespacedKey key2 = new NamespacedKey(this, "tpcoords");
                PersistentDataContainer container2 = meta.getPersistentDataContainer();
                container.set(key2, PersistentDataType.STRING, coordsString2);
                meta.setDisplayName(display_name + " [" + seted_text + "]");
                meta.setLore(Collections.singletonList(coordsString2));


                // Tworzymy nowy przedmiot o nazwie "Teleporter"
                ItemStack teleporter = new ItemStack(tp_material, 1);
                teleporter.setItemMeta(meta);

                // Dodajemy przedmiot do ekwipunku gracza
                item.setAmount(item.getAmount() - 1);
                player.getInventory().addItem(teleporter);
                player.sendMessage(ChatColor.AQUA + "<<" + display_name + ">> " + ChatColor.GREEN + config.getString("teleportation_location_set"));
            }
        }
    }
}

