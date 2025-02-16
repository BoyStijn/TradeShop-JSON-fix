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

package org.shanerx.tradeshop.framework.events;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.shanerx.tradeshop.objects.Shop;
import org.shanerx.tradeshop.objects.ShopItemStack;

import java.util.List;

/**
 * This class represents the event which is fired when a player attempts to perform a transaction with a shop.
 * Note: This event is fired BEFORE all the necessary conditions for the transaction are checked, and it is fired JUST BEFORE the checks happen.
 * This makes it possible to interact or cancel with the trade before the trade item and inventory processing takes place by using {@link Cancellable}.
 */
public class PlayerPreTradeEvent extends PlayerInteractEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Shop shop;
    private List<ShopItemStack> product;
    private List<ShopItemStack> cost;
	private Block clickedBlock;
	private boolean cancelled;

	/**
	 * Constructor for the object.
	 * @param who The {@link Player} object representing the player who is attempting the trade.
	 * @param cost The object representing the items which are being traded.
	 * @param product The object representing the items being traded for.
	 * @param shop The object representing the shop at which the trade takes place.
	 * @param clickedBlock The {@link Block} that was clicked, ie. the sign.
	 * @param clickedFace  The {@link BlockFace} object representing the face of the block that was clicked.
	 */
    public PlayerPreTradeEvent(Player who, List<ShopItemStack> cost, List<ShopItemStack> product, Shop shop, Block clickedBlock, BlockFace clickedFace) {
		super(who, Action.RIGHT_CLICK_BLOCK, null, shop.getShopSign().getBlock(), clickedFace);
		this.shop = shop;
		this.product = product;
		this.cost = cost;
		this.clickedBlock = clickedBlock;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Returns the {@link Shop} object representing the player shop this event is about.
	 * @return the shop.
	 */
	public Shop getShop() {
		return shop;
	}

	/**
	 * The items that are being bought from the shop by the player.
	 * @return A {@link List} which contains the {@link org.bukkit.inventory.ItemStack} objects which represent the items.
	 */
    public List<ShopItemStack> getProduct() {
		return product;
	}

	/**
	 * The items that are being paid to the shop by the player.
	 * @return A {@link List} which contains the {@link org.bukkit.inventory.ItemStack} objects which represent the items.
	 */
    public List<ShopItemStack> getCost() {
		return cost;
	}
	
	/**
	 * Returns whether or not the event has been cancelled.
	 * @return true if the event is being cancelled.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * Choose whether or not to cancel the event.
	 * @param cancelled true if the event should be cancelled.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
