/*
 *
 *                         Copyright (c) 2016-2019
 *                SparklingComet @ http://shanerx.org
 *               KillerOfPie @ http://killerofpie.github.io
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *                http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  NOTICE: All modifications made by others to the source code belong
 *  to the respective contributor. No contributor should be held liable for
 *  any damages of any kind, whether be material or moral, which were
 *  caused by their contribution(s) to the project. See the full License for more information.
 *
 */

package org.shanerx.tradeshop.commands.commandrunners;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.shanerx.tradeshop.TradeShop;
import org.shanerx.tradeshop.commands.CommandPass;
import org.shanerx.tradeshop.data.config.Message;
import org.shanerx.tradeshop.item.ShopItemSide;
import org.shanerx.tradeshop.item.ShopItemStack;
import org.shanerx.tradeshop.item.ShopItemStackSettingKeys;
import org.shanerx.tradeshop.player.Permissions;
import org.shanerx.tradeshop.player.ShopRole;
import org.shanerx.tradeshop.player.ShopUser;
import org.shanerx.tradeshop.shop.Shop;
import org.shanerx.tradeshop.utils.objects.ObjectHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of GUICommand for the `edit` command
 *
 * @since 2.3.0
 */
public class EditCommand extends GUICommand {

    private Shop shop;
    private InventoryGui mainMenu,
            userEdit,
            costEdit,
            productEdit;
    private List<ShopItemStack> costItems,
            productItems;
    private List<Boolean> costItemsRemoval,
            productItemsRemoval;
    public final ItemStack TRUE_ITEM = new ItemStack(Material.EMERALD_BLOCK);
    public final ItemStack FALSE_ITEM = new ItemStack(Material.REDSTONE_BLOCK);


    public EditCommand(TradeShop instance, CommandPass command) {
        super(instance, command);
    }

    /**
     * Opens a GUI allowing the player to edit the shop
     */
    public void edit() {
        shop = findShop();

        if (shop == null)
            return;

        if (!(shop.getUsersUUID(ShopRole.MANAGER, ShopRole.OWNER).contains(pSender.getUniqueId())
                || Permissions.isAdminEnabled(pSender))) {
            command.sendMessage(Message.NO_SHOP_PERMISSION.getPrefixed());
            return;
        }

        mainMenu = new InventoryGui(plugin, "Edit Menu-" + shop.getShopLocationAsSL().serialize(), MENU_LAYOUT);

        mainMenu.setFiller(new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1));

        // ShopUser edit menu, currently can only change/remove. Adding only available through commands
        mainMenu.addElement(new StaticGuiElement('a', new ItemStack(Material.PLAYER_HEAD), click -> {
            userEdit = new InventoryGui(plugin, "Edit Users", EDIT_LAYOUT);
            Set<ShopUser> shopUsers = new HashSet<>();
            GuiElementGroup userGroup = new GuiElementGroup('g');

            // Previous page
            userEdit.addElement(PREV_BUTTON);

            // Next page
            userEdit.addElement(NEXT_BUTTON);

            // Cancel and Back
            userEdit.addElement(CANCEL_BUTTON);

            userEdit.addElement(new StaticGuiElement('a', new ItemStack(Material.BLUE_STAINED_GLASS_PANE), " "));

            // Owner added separately as it is not editable
            userGroup.addElement(new StaticGuiElement('e', shop.getOwner().getHead(), shop.getOwner().getName(), "Position: " + shop.getOwner().getRole().toString()));

            if (shop.getOwner().getUUID().equals(pSender.getUniqueId())) {

                // Save and Back
                userEdit.addElement(new StaticGuiElement('s', new ItemStack(Material.ANVIL), click3 -> {
                    shop.updateShopUsers(shopUsers);
                    InventoryGui.goBack(pSender);
                    return true;
                }, "Save Changes"));

                for (ShopUser user : shop.getUsers(ShopRole.MANAGER, ShopRole.MEMBER)) {
                    shopUsers.add(user);
                    userGroup.addElement(new GuiStateElement('e',
                            user.getRole().toString(),
                            new GuiStateElement.State(change -> {
                                shopUsers.remove(user);
                                user.setRole(ShopRole.MANAGER);
                                shopUsers.add(user);
                            },
                                    "MANAGER",
                                    user.getHead(),
                                    user.getName(),
                                    "Position: MANAGER",
                                    "Click here to change to Member."),
                            new GuiStateElement.State(change -> {
                                shopUsers.remove(user);
                                user.setRole(ShopRole.MEMBER);
                                shopUsers.add(user);
                            },
                                    "MEMBER",
                                    user.getHead(),
                                    user.getName(),
                                    "Position: MEMBER",
                                    "Click here to remove this player."),
                            new GuiStateElement.State(change -> {
                                shopUsers.remove(user);
                                user.setRole(ShopRole.SHOPPER);
                                shopUsers.add(user);
                            },
                                    "SHOPPER",
                                    new ItemStack(Material.BARRIER),
                                    user.getName(),
                                    "Position: NONE",
                                    "Click here to add the player as a Manager.")
                    ));

                }
            } else {
                for (ShopUser user : shop.getUsers(ShopRole.MANAGER, ShopRole.MEMBER)) {
                    userGroup.addElement(new StaticGuiElement('e', user.getHead(), user.getName(), "Position: " + user.getRole().toString()));
                }
            }
            userEdit.addElement(userGroup);
            userEdit.show(pSender);
            return true;
        }, "Edit Shop Users"));

        mainMenu.addElement(new StaticGuiElement('b', new ItemStack(Material.GOLD_NUGGET), click -> {
            costEdit = new InventoryGui(plugin, "Edit Costs", EDIT_LAYOUT);
            costItems = new ArrayList<>();
            costItemsRemoval = new ArrayList<>();
            for (ShopItemStack item : shop.getSideList(ShopItemSide.COST)) {
                costItems.add(item.clone());
                costItemsRemoval.add(false);
            }
            GuiElementGroup costGroup = new GuiElementGroup('g');

            // Previous page
            costEdit.addElement(PREV_BUTTON);

            // Next page
            costEdit.addElement(NEXT_BUTTON);

            // Cancel and Back
            costEdit.addElement(CANCEL_BUTTON);

            // Save and Back
            costEdit.addElement(new StaticGuiElement('s', new ItemStack(Material.ANVIL), click3 -> {
                for (int i = costItems.size() - 1; i >= 0; i--) {
                    if (costItemsRemoval.get(i))
                        costItems.remove(i);
                }
                shop.updateSide(ShopItemSide.COST, costItems);
                InventoryGui.goBack(pSender);
                return true;
            }, "Save Changes"));

            costEdit.addElement(new StaticGuiElement('a', new ItemStack(Material.YELLOW_STAINED_GLASS_PANE), " "));

            for (int i = 0; i < costItems.size(); i++) {
                costGroup.addElement(shopItemEditMenu(i, true));
            }

            costEdit.addElement(costGroup);
            costEdit.show(pSender);
            return true;
        }, "Edit Shop Costs"));

        mainMenu.addElement(new StaticGuiElement('c', new ItemStack(Material.GRASS_BLOCK), click -> {
            productEdit = new InventoryGui(plugin, "Edit Products", EDIT_LAYOUT);
            productItems = new ArrayList<>();
            productItemsRemoval = new ArrayList<>();
            for (ShopItemStack item : shop.getSideList(ShopItemSide.PRODUCT)) {
                productItems.add(item.clone());
                productItemsRemoval.add(false);
            }
            GuiElementGroup productGroup = new GuiElementGroup('g');

            // Previous page
            productEdit.addElement(PREV_BUTTON);

            // Next page
            productEdit.addElement(NEXT_BUTTON);

            // Cancel and Back
            productEdit.addElement(CANCEL_BUTTON);

            // Save and Back
            productEdit.addElement(new StaticGuiElement('s', new ItemStack(Material.ANVIL), click3 -> {
                for (int i = productItems.size() - 1; i >= 0; i--) {
                    if (productItemsRemoval.get(i))
                        productItems.remove(i);
                }
                shop.updateSide(ShopItemSide.PRODUCT, productItems);
                InventoryGui.goBack(pSender);
                return true;
            }, "Save Changes"));

            productEdit.addElement(new StaticGuiElement('a', new ItemStack(Material.YELLOW_STAINED_GLASS_PANE), " "));

            for (int i = 0; i < productItems.size(); i++) {
                productGroup.addElement(shopItemEditMenu(i, false));
            }

            productEdit.addElement(productGroup);
            productEdit.show(pSender);
            return true;
        }, "Edit Shop Products"));

        mainMenu.show(pSender);
    }


    //region Util Methods
    //------------------------------------------------------------------------------------------------------------------

    private StaticGuiElement shopItemEditMenu(int index, boolean isCost) {
        ShopItemStack item = (isCost ? costItems : productItems).get(index).clone();
        ItemStack tempStack = item.getItemStack();
        ItemMeta tempMeta = tempStack.getItemMeta();
        List<String> newLore = new ArrayList<>();
        newLore.add(colorize("&8Amount &7» &f" + item.getAmount()));

        if (tempMeta != null && tempMeta.hasLore()) {
            newLore.add("");
            newLore.addAll(tempMeta.getLore());
        }

        tempMeta.setLore(newLore);
        tempStack.setItemMeta(tempMeta);

        return new StaticGuiElement('e', tempStack, click2 -> {
            InventoryGui itemEdit = new InventoryGui(plugin, "Edit Cost Item", ITEM_LAYOUT);
            GuiElementGroup itemGroup = new GuiElementGroup('g');

            // Cancel and Back
            itemEdit.addElement(CANCEL_BUTTON);

            // Save and Back
            itemEdit.addElement(new StaticGuiElement('s', new ItemStack(Material.ANVIL), click3 -> {
                (isCost ? costItems : productItems).set(index, item);
                InventoryGui.goBack(pSender);
                return true;
            }, "Save Changes"));

            itemEdit.addElement(new StaticGuiElement('a', new ItemStack(Material.YELLOW_STAINED_GLASS_PANE), " "));

            itemGroup.addElement(new GuiStateElement('e',
                    (isCost ? costItemsRemoval : productItemsRemoval).get(index) + "",
                    new GuiStateElement.State(change -> (isCost ? costItemsRemoval : productItemsRemoval).set(index, true),
                            "true",
                            new ItemStack(Material.BARRIER),
                            item.getItemName()),
                    new GuiStateElement.State(change -> (isCost ? costItemsRemoval : productItemsRemoval).set(index, false),
                            "false",
                            item.getItemStack())

            ));

            //Add new item settings below
            if (item.getItemStack().getItemMeta() instanceof Damageable) {
                itemGroup.addElement(numericalOption(ShopItemStackSettingKeys.COMPARE_DURABILITY, item, new ItemStack[]{
                        FALSE_ITEM, new ItemStack(Material.IRON_BLOCK), TRUE_ITEM, new ItemStack(Material.GOLD_BLOCK)
                }));
            }

            if (tempMeta instanceof BookMeta) {
                itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_BOOK_AUTHOR, item, TRUE_ITEM, FALSE_ITEM));
                itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_BOOK_PAGES, item, TRUE_ITEM, FALSE_ITEM));
            }

            if (tempMeta instanceof FireworkMeta) {
                itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_FIREWORK_DURATION, item, TRUE_ITEM, FALSE_ITEM));
                itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_FIREWORK_EFFECTS, item, TRUE_ITEM, FALSE_ITEM));
            }

            if (tempStack.getType().toString().endsWith("SHULKER_BOX")) {
                itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_SHULKER_INVENTORY, item, TRUE_ITEM, FALSE_ITEM));
            }

            if (plugin.getVersion().isAtLeast(1, 17) && tempStack.getType().equals(Material.BUNDLE)) {
                itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_BUNDLE_INVENTORY, item, TRUE_ITEM, FALSE_ITEM));
            }

            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_NAME, item, TRUE_ITEM, FALSE_ITEM));
            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_LORE, item, TRUE_ITEM, FALSE_ITEM));
            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_UNBREAKABLE, item, TRUE_ITEM, FALSE_ITEM));
            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_ENCHANTMENTS, item, TRUE_ITEM, FALSE_ITEM));
            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_ITEM_FLAGS, item, TRUE_ITEM, FALSE_ITEM));
            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_CUSTOM_MODEL_DATA, item, TRUE_ITEM, FALSE_ITEM));
            itemGroup.addElement(booleanOption(ShopItemStackSettingKeys.COMPARE_ATTRIBUTE_MODIFIER, item, TRUE_ITEM, FALSE_ITEM));


            itemEdit.addElement(itemGroup);
            itemEdit.show(pSender);
            return true;
        });
    }

    private GuiStateElement numericalOption(ShopItemStackSettingKeys setting, ShopItemStack tempItem, ItemStack[] indexedTempItem) {

        return new GuiStateElement('e',
                String.valueOf(tempItem.getShopSettingAsInteger(setting)),
                new GuiStateElement.State(change -> {
                    tempItem.setShopSettings(setting, new ObjectHolder<>(2));
                },
                        "2",
                        indexedTempItem[3],
                        setting.makeReadable(),
                        "State: >="),

                new GuiStateElement.State(change -> {
                    tempItem.setShopSettings(setting, new ObjectHolder<>(-1));
                },
                        "-1",
                        indexedTempItem[0],
                        setting.makeReadable(),
                        "State: False"),

                new GuiStateElement.State(change -> {
                    tempItem.setShopSettings(setting, new ObjectHolder<>(0));
                },
                        "0",
                        indexedTempItem[1],
                        setting.makeReadable(),
                        "State: <="),

                new GuiStateElement.State(change -> {
                    tempItem.setShopSettings(setting, new ObjectHolder<>(1));
                },
                        "1",
                        indexedTempItem[2],
                        setting.makeReadable(),
                        "State: =="

                ));
    }

    private GuiStateElement booleanOption(ShopItemStackSettingKeys setting, ShopItemStack tempItem, ItemStack trueTempItem, ItemStack falseTempItem) {
        return new GuiStateElement('e',
                String.valueOf(tempItem.getShopSettingAsBoolean(setting)),
                new GuiStateElement.State(change -> {
                    tempItem.setShopSettings(setting, new ObjectHolder<>(true));
                },
                        "true",
                        trueTempItem,
                        setting.makeReadable(),
                        "State: True"),
                new GuiStateElement.State(change -> {
                    tempItem.setShopSettings(setting, new ObjectHolder<>(false));
                },
                        "false",
                        falseTempItem,
                        setting.makeReadable(),
                        "State: False"
                ));
    }

    //------------------------------------------------------------------------------------------------------------------
    //endregion
}