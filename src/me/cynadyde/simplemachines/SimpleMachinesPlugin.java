package me.cynadyde.simplemachines;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleMachinesPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (isAutoCraftMachine(block)) {

            event.setCancelled(true);
            // have to run this code next tick so that the dispensed item is put back (due to event cancel)
            getServer().getScheduler().runTaskLater(this, () -> handleAutoCraft(block), 1L);
        }
    }

    public boolean isAutoCraftMachine(Block block) {
        return block.getType() == Material.DISPENSER
                && ((Directional) block.getBlockData()).getFacing() == BlockFace.DOWN
                && block.getRelative(BlockFace.DOWN).getType() == Material.CRAFTING_TABLE;
    }

    public void handleAutoCraft(Block block) {
        if (isAutoCraftMachine(block)) {

            ItemStack[] ingredients = ((Dispenser) block.getState()).getInventory().getContents();
            filterOutAir(ingredients);

            if (moreThanOneOfEach(ingredients)) {
                System.out.println("got ingredients: " + Arrays.asList(ingredients));

                ItemStack result = crafted(ingredients);
                if (result != null) {

                    System.out.println("got a result! " + result);

                    List<ItemStack> spill = new ArrayList<>();
                    Block below = block.getRelative(BlockFace.DOWN, 2);
                    Location dest = below.getLocation().add(0.5, 0.9, 0.5);

                    if (below.getState() instanceof Container) {

                        System.out.println("found container under the auto-crafter!");

                        // attempts to add the items to the container, returning anything that didn't fit
                        spill.addAll(((Container) below.getState()).getInventory().addItem(result).values());
                    }
                    else {
                        spill.add(result);
                    }
                    if (!spill.isEmpty()) {
                        for (ItemStack spilled : spill) {
                            below.getWorld().dropItemNaturally(dest, spilled);
                        }
                        below.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dest, 10, 0.02, 0.04, 0.02, 0.02 /*speed*/);
                        below.getWorld().playSound(dest, Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    public void filterOutAir(ItemStack[] items) {
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String name = ChatColor.stripColor(meta.getDisplayName().trim()).toLowerCase();
                    if (name.equals("minecraft:air") || name.equals("air")) {
                        items[i] = null;
                    }
                }
            }
        }
    }

    public boolean moreThanOneOfEach(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && item.getAmount() <= 1) {
                return false;
            }
        }
        return true;
    }

    public ItemStack crafted(ItemStack[] ingredients) {
        if (ingredients.length != 9) {
            throw new IllegalArgumentException("ingredients array must be of length 9");
        }
        if (Arrays.stream(ingredients).allMatch(Objects::isNull)) {
            return null;
        }
        Iterator<Recipe> recipes = getServer().recipeIterator();
        int searched = 0;

        recipeSearch:
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();


            if (recipe instanceof ShapelessRecipe) {
                searched += 1;
                if (recipe.getResult().getType() == Material.STICK) {
                    System.out.println("testing recipe: (" + searched + ") " + ((ShapelessRecipe) recipe).getKey());
                }

                List<ItemStack> unmatched = new ArrayList<>(Arrays.asList(ingredients));

                testRecipe:
                for (RecipeChoice choice : ((ShapelessRecipe) recipe).getChoiceList()) {
                    for (int i = 0; i < unmatched.size(); i++) {
                        ItemStack match = unmatched.get(i);
                        if (match != null && choice.test(match)) {
                            unmatched.remove(i);
                            continue testRecipe;
                        }
                    }
                    continue recipeSearch;
                }
                if (unmatched.isEmpty()) {
                    return recipe.getResult();
                }
            }
            else if (recipe instanceof ShapedRecipe) {
                searched += 1;
                boolean verbose = false;
                if (recipe.getResult().getType() == Material.STICK) {
                    System.out.println("testing recipe: (" + searched + ") " + ((ShapedRecipe) recipe).getKey());
                    verbose = true;

//                    System.out.println(Arrays.toString(((ShapedRecipe) recipe).getShape()));
//                    System.out.println("{" + ((ShapedRecipe) recipe).getChoiceMap().entrySet().stream().map(e -> e.getKey().toString() + e.getValue().toString()).collect(Collectors.joining(", ")) + "}");
                }
                Map<Character, RecipeChoice> choiceMap = ((ShapedRecipe) recipe).getChoiceMap();
                String[] shape = ((ShapedRecipe) recipe).getShape();
                int rows = shape.length;
                int cols = shape[0].length();

                // since the recipe shape can be smaller than the 3x3 grid, exhaust the possible sub grids in testing
                for (int top = 0; top <= 3 - rows; top++) {

                    testRecipeSubGrid:
                    for (int left = 0; left <= 3 - cols; left++) {

                        for (int r = 0; r < rows; r++) {
                            int row = top + r;
                            for (int c = 0; c < cols; c++) {
                                int col = left + c;

                                RecipeChoice choice = choiceMap.get(shape[r].charAt(c));
                                if (choice != null) {
                                    ItemStack ingredient = ingredients[row * 3 + col];
                                    if (ingredient == null || !choice.test(ingredient)) {
                                        continue testRecipeSubGrid;
                                    }
                                }
                            }
                        }
                        return recipe.getResult();
                    }
                }
            }
        }

        System.out.println("tested " + searched + " recipes...");

        return null;
    }
}
