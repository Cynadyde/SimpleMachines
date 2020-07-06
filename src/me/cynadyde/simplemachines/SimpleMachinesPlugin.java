package me.cynadyde.simplemachines;

import org.apache.commons.lang.ArrayUtils;
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

            Dispenser dispenser = ((Dispenser) block.getState());
            ItemStack[] contents = dispenser.getInventory().getContents();
            ItemStack[] ingredients = airFilteredOut(contents);

            if (moreThanOneOfEach(ingredients)) {
                System.out.println("got ingredients: " + Arrays.asList(ingredients));

                ItemStack result = crafted(ingredients);
                if (result != null) {

                    int amount = findLeastCommonAmount(ingredients) - 1; // keep 1 of each in the auto-crafter
                    // make sure new result amount isn't greater than the max stack size
                    int overflow = Math.max(0, (result.getAmount() * amount) - result.getMaxStackSize());
                    if (overflow > 0) {
                        amount -= Math.ceil(overflow / (double) result.getAmount());
                    }
                    result.setAmount(result.getAmount() * amount);
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
                            below.getWorld().dropItem(dest, spilled);
                        }
//                        below.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dest, 10, 0.02, 0.04, 0.02, 0.02 /*speed*/);
//                        below.getWorld().playSound(dest, Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    }

                    ingredients = dispenser.getInventory().getContents();
                    removeAmountFromEachNonAir(ingredients, amount);
//                    dispenser.getInventory().setContents();
                }
            }
        }
    }

    public ItemStack[] airFilteredOut(ItemStack[] items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            result[i] = isAirFilter(item) ? null : item;
        }
        return result;
    }

    public boolean isAirFilter(ItemStack item) {
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = ChatColor.stripColor(meta.getDisplayName().trim()).toLowerCase();
                return name.equals("minecraft:air") || name.equals("air");
            }
        }
        return false;
    }

    public boolean moreThanOneOfEach(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && item.getAmount() <= 1) {
                return false;
            }
        }
        return true;
    }

    public int findLeastCommonAmount(ItemStack[] items) {
        int result = Integer.MAX_VALUE;
        for (ItemStack item : items) {
            if (item != null) {
                if (item.getAmount() < result) {
                    result = item.getAmount();
                }
            }
        }
        return result;
    }

    public void removeAmountFromEachNonAir(ItemStack[] items, int amount) {
        for (ItemStack item : items) {
            if (item != null && !isAirFilter(item)) {
                item.setAmount(item.getAmount() - amount);
            }
        }
    }

    public ItemStack crafted(ItemStack[] ingredients) {
        if (ingredients.length != 9) {
            throw new IllegalArgumentException("ingredients array must be of length 9");
        }
        if (Arrays.stream(ingredients).allMatch(Objects::isNull)) {
            return null;
        }
        Iterator<Recipe> recipes = getServer().recipeIterator();

        recipeSearch:
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();

            if (recipe instanceof ShapelessRecipe) {
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
                Map<Character, RecipeChoice> choiceMap = ((ShapedRecipe) recipe).getChoiceMap();
                String[] shape = ((ShapedRecipe) recipe).getShape();
                int rows = shape.length;
                int cols = shape[0].length(); // recipe shapes are forced to be rectangular

                // since the recipe shape can be smaller than the 3x3 grid, exhaust possible sub-grids in testing
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
        return null;
    }
}
