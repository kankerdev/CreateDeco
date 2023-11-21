package com.github.talrey.createdeco.blocks.block_entities;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ShippingContainerBlockEntity extends ItemVaultBlockEntity {
    public ShippingContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ShippingContainerBlockEntity getControllerBE() {
        BlockEntity be = super.getControllerBE();
        if (be instanceof ShippingContainerBlockEntity sc) return sc;

        return null; // could be dangerous?
    }

    @Override
    public void updateConnectivity() {
        super.updateConnectivity();
    }

    /*extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory, SidedStorageBlockEntity {
    protected Storage<ItemVariant> itemCapability;

    protected ItemStackHandler inventory;
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected int radius;
    protected int length;
    protected Direction.Axis axis;

    protected boolean recalculateComparatorsNextTick = false;


    public ShippingContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        inventory = new ItemStackHandler(AllConfigs.server().logistics.vaultCapacity.get()) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                recalculateComparatorsNextTick = true;
            }
        };

        itemCapability = null;
        radius = 1;
        length = 1;
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide())
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    protected void updateComparators() {
        recalculateComparatorsNextTick = false;

        ShippingContainerBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return;

        level.blockEntityChanged(controllerBE.worldPosition);

        BlockPos pos = controllerBE.getBlockPos();
        for (int y = 0; y < controllerBE.radius; y++) {
            for (int z = 0; z < (controllerBE.axis == Direction.Axis.X ? controllerBE.radius : controllerBE.length); z++) {
                for (int x = 0; x < (controllerBE.axis == Direction.Axis.Z ? controllerBE.radius : controllerBE.length); x++) {
                    level.updateNeighbourForOutputSignal(pos.offset(x, y, z), getBlockState().getBlock());
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
            onPositionChanged();
            return;
        }

        if (updateConnectivity)
            updateConnectivity();

        if (recalculateComparatorsNextTick)
            updateComparators();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ShippingContainerBlockEntity getControllerBE() {
        if (isController())
            return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof ShippingContainerBlockEntity)
            return (ShippingContainerBlockEntity) blockEntity;
        return null;
    }

    public void removeController(boolean keepContents) {
        if (level.isClientSide())
            return;
        updateConnectivity = true;
        controller = null;
        radius = 1;
        length = 1;

        BlockState state = getBlockState();
        if (ShippingContainerBlock.isVault(state)) {
            state = state.setValue(ShippingContainerBlock.LARGE, false);
            getLevel().setBlock(worldPosition, state, 22);
        }

        itemCapability = null;
        setChanged();
        sendData();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        itemCapability = null;
        setChanged();
        sendData();
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = radius;
        int prevLength = length;

        updateConnectivity = compound.contains("Uninitialized");
        controller = null;
        lastKnownPos = null;

        if (compound.contains("LastKnownPos"))
            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));
        if (compound.contains("Controller"))
            controller = NbtUtils.readBlockPos(compound.getCompound("Controller"));

        if (isController()) {
            radius = compound.getInt("Size");
            length = compound.getInt("Length");
        }

        if (!clientPacket) {
            inventory.deserializeNBT(compound.getCompound("Inventory"));
            return;
        }

        boolean changeOfController =
                controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
        if (hasLevel() && (changeOfController || prevSize != radius || prevLength != length))
            level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        if (updateConnectivity)
            compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (!isController())
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        if (isController()) {
            compound.putInt("Size", radius);
            compound.putInt("Length", length);
        }

        super.write(compound, clientPacket);

        if (!clientPacket) {
            compound.putString("StorageType", "CombinedInv");
            compound.put("Inventory", inventory.serializeNBT());
        }
    }

    public ItemStackHandler getInventoryOfBlock() {
        return inventory;
    }

    public void applyInventoryToBlock(ItemStackHandler handler) {
        for (int i = 0; i < inventory.getSlotCount(); i++)
            inventory.setStackInSlot(i, i < handler.getSlotCount() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
        initCapability();
        return itemCapability;
    }

    private void initCapability() {
        if (itemCapability != null)
            return;
        if (!isController()) {
            ShippingContainerBlockEntity controllerBE = getControllerBE();
            if (controllerBE == null)
                return;
            controllerBE.initCapability();
            itemCapability = controllerBE.itemCapability;
            return;
        }

        boolean alongZ = ShippingContainerBlock.getVaultBlockAxis(getBlockState()) == Direction.Axis.Z;
        ItemStackHandler[] invs = new ItemStackHandler[length * radius * radius];
        for (int yOffset = 0; yOffset < length; yOffset++) {
            for (int xOffset = 0; xOffset < radius; xOffset++) {
                for (int zOffset = 0; zOffset < radius; zOffset++) {
                    BlockPos vaultPos = alongZ ? worldPosition.offset(xOffset, zOffset, yOffset)
                            : worldPosition.offset(yOffset, xOffset, zOffset);
                    ShippingContainerBlockEntity vaultAt =
                            ConnectivityHandler.partAt(BlockRegistry.SHIPPING_CONTAINER_ENTITIES.get(), level, vaultPos);
                    invs[yOffset * radius * radius + xOffset * radius + zOffset] =
                            vaultAt != null ? vaultAt.inventory : new ItemStackHandler();
                }
            }
        }

        CombinedStorage<ItemVariant, ItemStackHandler> combinedInvWrapper = new CombinedStorage<>(List.of(invs));
        itemCapability = combinedInvWrapper;
    }

    public static int getMaxLength(int radius) {
        return radius * 3;
    }

    @Override
    public void preventConnectivityUpdate() { updateConnectivity = false; }

    // fabric: see comment in FluidTankItem
    public void queueConnectivityUpdate() {
        updateConnectivity = true;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (ShippingContainerBlock.isVault(state)) { // safety
            level.setBlock(getBlockPos(), state.setValue(ShippingContainerBlock.LARGE, radius > 2), 6);
        }
        itemCapability = null;
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() { return getMainAxisOf(this); }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Y) return getMaxWidth();
        return getMaxLength(width);
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() { return length; }

    @Override
    public int getWidth() { return radius; }

    @Override
    public void setHeight(int height) { this.length = height; }

    @Override
    public void setWidth(int width) { this.radius = width; }

    @Override
    public boolean hasInventory() { return true; }
    */
}
