package net.natte.bankstorage.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackSet;
import net.natte.bankstorage.options.BankOptions;

public class CachedBankStorage {

    public static Map<UUID, CachedBankStorage> BANK_CACHE = new HashMap<>();

    // public static List<ItemStack> bankRequestQueue = new ArrayList<>();
    public static Set<ItemStack> bankRequestQueue = ItemStackSet.create();

    public List<ItemStack> items;

    public int selectedItemSlot;
    public UUID uuid;
    public ItemStack bankItemStack;
    
    public BankOptions options;

    public CachedBankStorage() {
        this.items = new ArrayList<>();
        this.selectedItemSlot = 0;
        this.options = new BankOptions();
    }

    public CachedBankStorage(List<ItemStack> items, int selectedItemSlot, UUID uuid, BankOptions options, ItemStack bankItemStack) {
        this.items = items;
        this.selectedItemSlot = selectedItemSlot;
        this.uuid = uuid;
        this.options = options;
        this.bankItemStack = bankItemStack;
    }

    public ItemStack getSelectedItem() {
        if (this.selectedItemSlot > this.items.size())
            return ItemStack.EMPTY;
        return this.items.get(this.selectedItemSlot);
    }

    @Nullable
    public static CachedBankStorage getBankStorage(ItemStack itemStack) {
        if (!itemStack.hasNbt())
            return null;
        if (!itemStack.getNbt().contains(BankItem.UUID_KEY))
            return null;

        UUID uuid = itemStack.getNbt().getUuid(BankItem.UUID_KEY);

        CachedBankStorage bankStorage = BANK_CACHE.get(uuid);

        if (bankStorage == null) {
            bankRequestQueue.add(itemStack);
        // // RequestBankStorage.requestC2S(itemStack);
        // PacketByteBuf buf = PacketByteBufs.create();
        // buf.writeItemStack(itemStack);
        // ClientPlayNetworking.send(RequestBankStorage.C2S_PACKET_ID, buf);

        }

        return bankStorage;
    }
}
