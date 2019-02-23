/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package techreborn.tiles.machine.tier1;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import reborncore.api.IToolDrop;
import reborncore.common.powerSystem.TilePowerAcceptor;
import reborncore.common.registration.RebornRegister;
import reborncore.common.registration.config.ConfigRegistry;
import reborncore.common.util.WorldUtils;
import techreborn.TechReborn;
import techreborn.blocks.tier1.BlockPlayerDetector;
import techreborn.init.TRContent;
import techreborn.init.TRTileEntities;

import java.util.Iterator;

@RebornRegister(TechReborn.MOD_ID)
public class TilePlayerDectector extends TilePowerAcceptor implements IToolDrop {

	@ConfigRegistry(config = "machines", category = "player_detector", key = "PlayerDetectorMaxInput", comment = "Player Detector Max Input (Value in EU)")
	public static int maxInput = 32;
	@ConfigRegistry(config = "machines", category = "player_detector", key = "PlayerDetectorMaxEnergy", comment = "Player Detector Max Energy (Value in EU)")
	public static int maxEnergy = 10000;
	@ConfigRegistry(config = "machines", category = "player_detector", key = "PlayerDetectorEUPerSecond", comment = "Player Detector Energy Consumption per second (Value in EU)")
	public static int euPerTick = 10;

	public String owenerUdid = "";
	boolean redstone = false;

	public TilePlayerDectector() {
		super(TRTileEntities.PLAYER_DETECTOR);
	}
	
	public boolean isProvidingPower() {
		return redstone;
	}

	// TilePowerAcceptor
	@Override
	public void tick() {
		super.tick();
		if (!world.isRemote && world.getGameTime() % 20 == 0) {
			boolean lastRedstone = redstone;
			redstone = false;
			if (canUseEnergy(euPerTick)) {
				Iterator<EntityPlayer> tIterator = super.world.playerEntities.iterator();
				while (tIterator.hasNext()) {
					EntityPlayer player = tIterator.next();
					if (player.getDistanceSq((double) super.getPos().getX() + 0.5D,
						(double) super.getPos().getY() + 0.5D, (double) super.getPos().getZ() + 0.5D) <= 256.0D) {
						String type = world.getBlockState(pos).get(BlockPlayerDetector.TYPE);
						if (type.equals("all")) {// ALL
							redstone = true;
						} else if (type.equals("others")) {// Others
							if (!owenerUdid.isEmpty() && !owenerUdid.equals(player.getUniqueID().toString())) {
								redstone = true;
							}
						} else {// You
							if (!owenerUdid.isEmpty() && owenerUdid.equals(player.getUniqueID().toString())) {
								redstone = true;
							}
						}
					}
				}
				useEnergy(euPerTick);
			}
			if (lastRedstone != redstone) {
				WorldUtils.updateBlock(world, pos);
				world.notifyNeighborsOfStateChange(pos, world.getBlockState(pos).getBlock());
			}
		}
	}
	
	@Override
	public double getBaseMaxPower() {
		return maxEnergy;
	}

	@Override
	public boolean canAcceptEnergy(EnumFacing direction) {
		return true;
	}

	@Override
	public boolean canProvideEnergy(EnumFacing direction) {
		return false;
	}

	@Override
	public double getBaseMaxOutput() {
		return 0;
	}

	@Override
	public double getBaseMaxInput() {
		return maxInput;
	}

	@Override
	public void read(NBTTagCompound tag) {
		super.read(tag);
		owenerUdid = tag.getString("ownerID");
	}

	@Override
	public NBTTagCompound write(NBTTagCompound tag) {
		super.write(tag);
		tag.putString("ownerID", owenerUdid);
		return tag;
	}

	// IToolDrop
	@Override
	public ItemStack getToolDrop(EntityPlayer p0) {
		return TRContent.Machine.PLAYER_DETECTOR.getStack();
	}
}
