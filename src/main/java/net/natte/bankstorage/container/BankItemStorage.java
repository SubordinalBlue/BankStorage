package net.natte.bankstorage.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.natte.bankstorage.BankStorage;
import net.natte.bankstorage.item.BankItem;
import net.natte.bankstorage.item.CachedBankStorage;
import net.natte.bankstorage.options.BankOptions;
import net.natte.bankstorage.screen.BankScreenHandler;

public class BankItemStorage extends SimpleInventory implements NamedScreenHandlerFactory {

    public BankOptions options;

    public BankType type;

    private Text displayName;

    public int selectedItemSlot = 0;

    public UUID uuid;

    public Random random;

    public BankItemStorage(BankType type, UUID uuid) {
        super(type.rows * type.cols);
        this.type = type;
        this.options = new BankOptions();
        this.uuid = uuid;
        this.random = new Random();
        // this.inventory = DefaultedList.ofSize(this.rows * this.cols,
        // ItemStack.EMPTY);
    }

    public BankItemStorage withDisplayName(Text displayName) {
        this.displayName = displayName;
        return this;
    }

    public BankItemStorage asType(BankType type) {
        if (this.type != type) {

            return changeType(type);
        }
        return this;
    }

    public BankItemStorage changeType(BankType type) {
        BankStorage.LOGGER.info("Upgrading bank from " + this.type.getName() + " to " + type.getName() + " uuid " + this.uuid);
        BankItemStorage newBankItemStorage = new BankItemStorage(type, this.uuid).withDisplayName(displayName);
        for (int i = 0; i < this.stacks.size(); ++i) {
            newBankItemStorage.stacks.set(i, this.stacks.get(i));
        }
        return newBankItemStorage;
    }

    @Override
    public BankScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        // return GenericContainerScreenHandler.createGeneric9x1(syncId,
        // playerInventory, (Inventory) this);
        System.out.println("opening type " + this.type.getName());
        System.out.println("i have slots " + this.stacks.size());
        return new BankScreenHandler(syncId, playerInventory, this, this.type);

    }

    @Override
    public Text getDisplayName() {
        return displayName;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.uuid != null)
            CachedBankStorage.bankRequestQueue.add(this.uuid);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return super.getStack(slot);
        // return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return super.removeStack(slot, amount);
        // return this.inventory.get(slot).split(amount);

    }

    @Override
    public int getMaxCountPerStack() {
        return 64 * type.slotStorageMultiplier;
    }

    public int getStorageMultiplier() {
        return type.slotStorageMultiplier;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return super.isValid(slot, stack);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return super.removeStack(slot);
        // ItemStack itemStack = this.inventory.get(slot);
        // this.inventory.set(slot, ItemStack.EMPTY);
        // return itemStack;
    }

    @Override
    public void setStack(int slot, ItemStack itemStack) {
        super.setStack(slot, itemStack);
        // this.inventory.set(slot, itemStack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity playerEntity) {
        return true;
    }

    // same format as vanilla except itemstack count and slot saved as int instead
    // of byte
    public NbtCompound saveToNbt() {
        NbtCompound nbtCompound = new NbtCompound();

        // ("selectedItemSlot", this.selectedItemSlot);
        nbtCompound.putUuid("uuid", this.uuid);
        nbtCompound.putInt("selectedItemSlot", this.selectedItemSlot);
        nbtCompound.put("options", this.options.asNbt());

        nbtCompound.putString("type", this.type.getName());

        NbtList nbtList = new NbtList();
        for (int i = 0; i < this.stacks.size(); ++i) {
            ItemStack itemStack = this.stacks.get(i);
            if (itemStack.isEmpty())
                continue;
            NbtCompound itemNbtCompound = new NbtCompound();
            itemNbtCompound.putInt("Slot", i);

            Identifier identifier = Registries.ITEM.getId(itemStack.getItem());
            itemNbtCompound.putString("id", identifier == null ? "minecraft:air" : identifier.toString());
            itemNbtCompound.putInt("Count", itemStack.getCount());
            if (itemStack.getNbt() != null) {
                itemNbtCompound.put("tag", itemStack.getNbt().copy());
            }

            nbtList.add(itemNbtCompound);
        }
        nbtCompound.put("Items", nbtList);
        return nbtCompound;
    }

    // same format as vanilla except itemstack count and slot saved as int instead
    // of byte
    public static BankItemStorage createFromNbt(NbtCompound nbtCompound) {

        UUID uuid = nbtCompound.getUuid("uuid");
        System.out.println("reading " + uuid);
        System.out.println("has type " + nbtCompound.getString("type"));
        BankItemStorage bankItemStorage = new BankItemStorage(
                BankStorage.getBankTypeFromName(nbtCompound.getString("type")),
                uuid);

        bankItemStorage.selectedItemSlot = nbtCompound.getInt("selectedItemSlot");

        bankItemStorage.options = BankOptions.fromNbt(nbtCompound.getCompound("options"));

        Inventories.readNbt(nbtCompound, bankItemStorage.stacks);
        NbtList nbtList = nbtCompound.getList("Items", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbt = nbtList.getCompound(i);
            int j = nbt.getInt("Slot");
            if (j < 0 || j >= bankItemStorage.stacks.size())
                continue;

            ItemStack itemStack = Registries.ITEM.get(new Identifier(nbt.getString("id"))).getDefaultStack();
            itemStack.setCount(nbt.getInt("Count"));
            if (nbt.contains("tag", NbtElement.COMPOUND_TYPE)) {
                itemStack.setNbt(nbt.getCompound("tag"));
            }
            bankItemStorage.stacks.set(j, itemStack);
        }

        return bankItemStorage;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return !(stack.getItem() instanceof BankItem) && super.canInsert(stack);
    }

    @Override
    public ItemStack addStack(ItemStack stack) {
        return super.addStack(stack);
    }

    public List<ItemStack> getUniqueItems() {

        List<ItemStack> items = new ArrayList<>();

        for (ItemStack itemStack : this.stacks) {
            if (itemStack.isEmpty())
                continue;
            boolean contains = false;
            for (ItemStack stack : items) {
                if (ItemStack.canCombine(itemStack, stack)) {
                    stack.increment(itemStack.getCount());
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                items.add(itemStack);
            }
        }
        return items;
    }

    public List<ItemStack> getBlockItems() {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : this.stacks) {
            // if(stack.getItem)
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem)
                items.add(stack);
        }
        return items;
    }

    public ItemStack getSelectedItem() {

        List<ItemStack> items = getBlockItems();
        return items.isEmpty() ? ItemStack.EMPTY : items.get(this.options.selectedItemSlot);

    }

    public ItemStack getRandomItem() {

        List<ItemStack> items = getBlockItems();
        return items.isEmpty() ? ItemStack.EMPTY : items.get(this.random.nextInt(items.size()));

    }

    public ItemStack chooseItemToPlace() {
        List<ItemStack> items = getBlockItems();
        if (items.isEmpty())
            return ItemStack.EMPTY;
        return switch (this.options.buildMode) {
            case NONE -> ItemStack.EMPTY;
            case NORMAL -> items.get(this.options.selectedItemSlot);
            case RANDOM -> items.get(this.random.nextInt(items.size()));
        };
    }
}
