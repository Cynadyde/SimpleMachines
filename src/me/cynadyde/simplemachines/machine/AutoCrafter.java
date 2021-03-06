package me.cynadyde.simplemachines.machine;

import me.cynadyde.simplemachines.SimpleMachinesPlugin;
import me.cynadyde.simplemachines.transfer.OutputPolicy;
import me.cynadyde.simplemachines.transfer.TransferScheme;
import me.cynadyde.simplemachines.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.stream.Collectors;

public class AutoCrafter implements Listener {

    private final Map<List<ItemStack>, Recipe> recipeCache; // this is never emptied...
    private final SimpleMachinesPlugin plugin;

    public AutoCrafter(SimpleMachinesPlugin plugin) {
        this.recipeCache = new HashMap<>();
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        if (isAutoCrafterMachine(event.getBlock())) {
            event.setCancelled(true);

            /* have to run this code next tick so that the inventory
               isn't overwritten by the event trying to cancel itself. */
            final Block machine = event.getBlock();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> doAutoCraft(machine), 0L);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if (holder instanceof BlockState) {

            final Block machine = ((BlockState) holder).getBlock();
            if (isAutoCrafterMachine(machine)) {
                event.setCancelled(true);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> doAutoCraft(machine), 0L);
            }
        }
    }

    public boolean isAutoCrafterMachine(Block block) {
        return block.getType() == Material.DROPPER
                && block.getRelative(BlockFace.DOWN).getType() == Material.CRAFTING_TABLE;
    }

    public void doAutoCraft(Block block) {
        if (isAutoCrafterMachine(block)) {
            Dropper dropper = ((Dropper) block.getState());
            OutputPolicy output = TransferScheme.ofHolder(dropper).OUTPUT;

            ItemStack[] contents = dropper.getInventory().getContents();
            ItemStack[] ingredients = calcIngredients(contents, output);
            if (ingredients != null) {

                Recipe recipe = getBestRecipeMatch(ingredients);
                if (recipe != null) {
                    ItemStack result = recipe.getResult();
                    ItemUtils.dropFromDropper(dropper, result);
                    dropper.getInventory().setContents(calcLeftovers(contents, ingredients));
                }
            }
        }
    }

    private ItemStack[] calcIngredients(ItemStack[] contents, OutputPolicy output) {
        if (contents.length != 9) {
            throw new IllegalArgumentException("contents array must be of length 9");
        }
        boolean notEmpty = false;
        ItemStack[] ingredients = new ItemStack[contents.length];

        /* either everything is able to be pulled out or nothing will be...
            this protects against crafting unexpected recipes from partial ingredients */
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (!ItemUtils.isEmpty(item)) {
                if (!output.testSlot(item)) {
                    return null;
                }
                else {
                    notEmpty = true;
                    ItemStack ingredient = item.clone();
                    ingredient.setAmount(1);
                    ingredients[i] = ingredient;
                }
            }
        }
        return notEmpty ? ingredients : null;
    }

    private ItemStack[] calcLeftovers(ItemStack[] contents, ItemStack[] consumed) {
        if (contents.length != 9) {
            throw new IllegalArgumentException("contents array must be of length 9");
        }
        if (consumed.length != 9) {
            throw new IllegalArgumentException("taken array must be of length 9");
        }
        ItemStack[] results = new ItemStack[contents.length];
        for (int i = 0; i < results.length; i++) {

            ItemStack c = contents[i];
            if (c != null && !c.getType().isAir() && c.getAmount() > 0) {

                /* If a content item becomes a different item after crafting but the
                    stack hasn't been depleted, the different item will be destroyed. */
                ItemStack item = c.clone();
                int taken = consumed[i] == null ? 0 : consumed[i].getAmount();
                item.setAmount(item.getAmount() - taken);
                if (ItemUtils.isEmpty(item)) {
                    Material remainder = c.getType().getCraftingRemainingItem();
                    item = remainder != null ? new ItemStack(remainder, c.getAmount()) : null;
                }
                results[i] = item;
            }
        }
        return results;
    }

    private Recipe getBestRecipeMatch(ItemStack[] ingredients) {
        /* yeah, for some reason bukkit doesn't actually
            have a method for this, so here it goes... */
        if (ingredients.length != 9) {
            throw new IllegalArgumentException("ingredients array must be of length 9");
        }
        if (Arrays.stream(ingredients).allMatch(Objects::isNull)) {
            return null;
        }
        List<ItemStack> ingredientsKey = Arrays.stream(ingredients)
                .map(item -> {
                    if (item == null) {
                        return null;
                    }
                    else {
                        ItemStack result = item.clone();
                        result.setAmount(1);
                        return result;
                    }
                })
                .collect(Collectors.toList());

        Recipe cachedRecipe = recipeCache.get(ingredientsKey);
        if (cachedRecipe != null) {
            return cachedRecipe;
        }

        int usedRows;
        int usedCols;
        {
            List<Integer> xList = new ArrayList<>();
            List<Integer> yList = new ArrayList<>();

            for (int i = 0; i <= 8; i++) {
                if (ingredients[i] != null) {
                    xList.add(i % 3);
                    yList.add(i / 3);
                }
            }
            usedRows = 1 + Collections.max(yList) - Collections.min(yList);
            usedCols = 1 + Collections.max(xList) - Collections.min(xList);
        }

        Recipe recipe = null;
        boolean foundRecipe = false;
        Iterator<Recipe> recipes = plugin.getServer().recipeIterator();

        recipeSearch:
        while (recipes.hasNext()) {
            recipe = recipes.next();

            if (recipe instanceof ShapelessRecipe) {

                List<ItemStack> unmatched = new ArrayList<>();
                for (ItemStack ingredient : ingredients) {
                    if (ingredient != null && !ingredient.getType().isAir() && ingredient.getAmount() > 0) {
                        unmatched.add(ingredient);
                    }
                }

                testRecipe:
                for (RecipeChoice choice : ((ShapelessRecipe) recipe).getChoiceList()) {
                    for (int i = 0; i < unmatched.size(); i++) {
                        ItemStack match = unmatched.get(i);
                        if (choice.test(match)) {
                            unmatched.remove(i);
                            continue testRecipe;
                        }
                    }
                    // if no matches were found for a recipe choice, we end up here
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
                if (rows != usedRows) continue;

                int cols = shape[0].length(); // recipe shapes are forced to be rectangular
                if (cols != usedCols) continue;

                // since the recipe shape can be smaller than the 3x3 grid, exhaust possible sub-grids in testing
                for (int top = 0; top <= 3 - rows; top++) {

                    testRecipeSubGrid:
                    for (int left = 0; left <= 3 - cols; left++) {

                        Set<Integer> unmatched = new HashSet<>();
                        for (int i = 0; i < ingredients.length; i++) {
                            if (ingredients[i] != null) {
                                unmatched.add(i);
                            }
                        }
                        for (int r = 0; r < rows; r++) {
                            int row = top + r;
                            for (int c = 0; c < cols; c++) {
                                int col = left + c;

                                RecipeChoice choice = choiceMap.get(shape[r].charAt(c));
                                if (choice != null) {
                                    int i = row * 3 + col;
                                    ItemStack ingredient = ingredients[i];
                                    if (ingredient == null || !choice.test(ingredient)) {
                                        continue testRecipeSubGrid;
                                    }
                                    unmatched.remove(i);
                                }
                            }
                        }
                        if (unmatched.isEmpty()) {
                            foundRecipe = true;
                            break recipeSearch;
                        }
                    }
                }
            }
        }
        if (foundRecipe) {
            recipeCache.put(ingredientsKey, recipe);
            return recipe;
        }
        return null;
    }
}
