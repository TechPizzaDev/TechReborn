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

package techreborn.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import reborncore.client.models.ModelCompound;
import reborncore.client.models.RebornModelRegistry;
import reborncore.common.powerSystem.ExternalPowerSystems;
import reborncore.common.powerSystem.forge.ForgePowerItemManager;
import reborncore.common.util.WorldUtils;
import techreborn.TechReborn;
import techreborn.events.TRRecipeHandler;
import techreborn.init.ModSounds;
import techreborn.init.TRContent;
import techreborn.items.tool.ItemTreeTap;
import techreborn.items.tool.basic.ItemElectricTreetap;

import java.util.Random;

/**
 * Created by modmuss50 on 19/02/2016.
 */
public class BlockRubberLog extends Block {

	public static DirectionProperty SAP_SIDE = DirectionProperty.create("sapside", EnumFacing.Plane.HORIZONTAL);
	public static BooleanProperty HAS_SAP = BooleanProperty.create("hassap");

	public BlockRubberLog() {
		super(Block.Properties.create(Material.WOOD).hardnessAndResistance(2f).sound(SoundType.WOOD).tickRandomly());
		this.setDefaultState(this.getDefaultState().with(SAP_SIDE, EnumFacing.NORTH).with(HAS_SAP, false));
		((BlockFire) Blocks.FIRE).setFireInfo(this, 5, 5);
		RebornModelRegistry.registerModel(new ModelCompound(TechReborn.MOD_ID, this));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder) {
		SAP_SIDE = DirectionProperty.create("sapside", EnumFacing.Plane.HORIZONTAL);
		HAS_SAP = BooleanProperty.create("hassap");
		builder.add(SAP_SIDE, HAS_SAP);
	}

	@Override
	public boolean isIn(Tag<Block> tagIn) {
		return tagIn == BlockTags.LOGS;
	}

	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		int i = 4;
		int j = i + 1;
		if (worldIn.isAreaLoaded(pos.add(-j, -j, -j), pos.add(j, j, j))) {
			for (BlockPos blockpos : BlockPos.getAllInBox(pos.add(-i, -i, -i), pos.add(i, i, i))) {
				IBlockState state1 = worldIn.getBlockState(blockpos);
				if (state1.getBlock() instanceof BlockLeaves) {
					state1.getBlock().tick(state1, worldIn, pos, worldIn.getRandom());
					state1.getBlock().randomTick(state1, worldIn, pos, worldIn.getRandom());
				}
			}
		}
	}

	@Override
	public void tick(IBlockState state, World worldIn, BlockPos pos, Random random) {
		super.tick(state, worldIn, pos, random);
		if (!state.get(HAS_SAP)) {
			if (random.nextInt(50) == 0) {
				EnumFacing facing = EnumFacing.byHorizontalIndex(random.nextInt(4));
				if (worldIn.getBlockState(pos.down()).getBlock() == this
						&& worldIn.getBlockState(pos.up()).getBlock() == this) {
					worldIn.setBlockState(pos, state.with(HAS_SAP, true).with(SAP_SIDE, facing));
				}
			}
		}
	}

	@Override
	public boolean onBlockActivated(IBlockState state, World worldIn, BlockPos pos, EntityPlayer playerIn,
			EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		super.onBlockActivated(state, worldIn, pos, playerIn, hand, side, hitX, hitY, hitZ);
		ItemStack stack = playerIn.getHeldItem(EnumHand.MAIN_HAND);
		if (stack.isEmpty()) {
			return false;
		}
		ForgePowerItemManager capEnergy = null;
		if (stack.getItem() instanceof ItemElectricTreetap) {
			capEnergy = new ForgePowerItemManager(stack);
		}
		if ((capEnergy != null && capEnergy.getEnergyStored() > 20) || stack.getItem() instanceof ItemTreeTap) {
			if (state.get(HAS_SAP) && state.get(SAP_SIDE) == side) {
				worldIn.setBlockState(pos, state.with(HAS_SAP, false).with(SAP_SIDE, EnumFacing.byHorizontalIndex(0)));
				worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.SAP_EXTRACT, SoundCategory.BLOCKS,
						0.6F, 1F);
				if (!worldIn.isRemote) {
					if (capEnergy != null) {
						capEnergy.extractEnergy(20, false);

						ExternalPowerSystems.requestEnergyFromArmor(capEnergy, playerIn);
					} else {
						playerIn.getHeldItem(EnumHand.MAIN_HAND).damageItem(1, playerIn);
					}
					if (!playerIn.inventory.addItemStackToInventory(TRContent.Parts.SAP.getStack())) {
						WorldUtils.dropItem(TRContent.Parts.SAP.getStack(), worldIn, pos.offset(side));
					}
					if (playerIn instanceof EntityPlayerMP) {
						TRRecipeHandler.unlockTRRecipes((EntityPlayerMP) playerIn);
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void getDrops(IBlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune) {
		drops.add(new ItemStack(this));
		if (state.get(HAS_SAP)) {
			if (new Random().nextInt(4) == 0) {
				drops.add(TRContent.Parts.SAP.getStack());
			}
		}
	}
}
