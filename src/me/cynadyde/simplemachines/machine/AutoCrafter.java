package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.transfer.OutputPolicy;
import me.cynadyde.simplemachines.transfer.TransferSourcePolicy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.stream.Collectors;

public class AutoCrafter implements Listener {

    private final Map<List<Material>, Recipe> recipeCache;
    private final SimpleMachinesPlugin plugin;

    public AutoCrafter(SimpleMachinesPlugin plugin) {
        this.recipeCache = new HashMap<>();
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (isAutoCraftMachine(block)) {

            event.setCancelled(true);
            // have to run this code next tick so that the dispensed item is put back (due to event cancel)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> handleAutoCraft(block), 1L);
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
            TransferSourcePolicy sourcePolicy = TransferSourcePolicy.ofInventory(dispenser.getInventory());
            OutputPolicy output = sourcePolicy.OUTPUT;

            ItemStack[] contents = dispenser.getInventory().getContents();
            ItemStack[] ingredients = takeIngredients(contents, output);

            if (Arrays.stream(ingredients).anyMatch(Objects::nonNull)) {

                ItemStack result = getCraftingResult(ingredients);
                if (result != null) {
                    List<ItemStack> spill = new ArrayList<>();

                    Block below = block.getRelative(BlockFace.DOWN, 2);
                    Location dest = below.getLocation().add(0.5, 0.5, 0.5);

                    if (below.getState() instanceof Container) {
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
                    }
                    dispenser.getInventory().setContents(contents);
                }
            }
        }
    }

    public ItemStack[] takeIngredients(ItemStack[] items, OutputPolicy output) {

        ItemStack[] ingredients = new ItemStack[items.length];
        // either everything is able to be pulled out or nothing will be...
        // this protects against crafting unexpected recipes from partial ingredients
        for (ItemStack item : items) {
            if (item != null && output == OutputPolicy.MIN_ONE && item.getAmount() == 1 && item.getMaxStackSize() > 1) {
                return ingredients;
            }
        }
        // mutate the items array, taking one of each
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null) {
                ItemStack ingredient = item.clone();

                item.setAmount(item.getAmount() - 1);
                ingredient.setAmount(1);
                ingredients[i] = item;
            }
        }
        return ingredients;
    }

    public ItemStack getCraftingResult(ItemStack[] ingredients) {
        if (ingredients.length != 9) {
            throw new IllegalArgumentException("ingredients array must be of length 9");
        }
        if (Arrays.stream(ingredients).allMatch(Objects::isNull)) {
            return null;
        }
        List<Material> materials = Arrays.stream(ingredients)
                .map(item -> item == null ? null : item.getType())
                .collect(Collectors.toList());

        Recipe cachedRecipe = recipeCache.get(materials);
        if (cachedRecipe != null) {
            return cachedRecipe.getResult();
        }

        Recipe recipe = null;
        boolean foundRecipe = false;
        Iterator<Recipe> recipes = plugin.getServer().recipeIterator();

        recipeSearch:
        while (recipes.hasNext()) {
            recipe = recipes.next();

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
                    foundRecipe = true;
                    break;
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
                        foundRecipe = true;
                        break recipeSearch;
                    }
                }
            }
        }
        if (foundRecipe) {
            recipeCache.put(materials, recipe);
            return recipe.getResult();
        }
        return null;
    }
}
