package com.ticketer.dtos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.ComboItem;
import com.ticketer.models.ComboSlot;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;

public class DtoMapper {

    public static SettingsDto toSettingsDto(Settings settings) {
        if (settings == null) return null;
        return new SettingsDto(settings.getTax(), settings.getHours());
    }

    public static BaseItemDto toBaseItemDto(BaseItem item) {
        return new BaseItemDto(item.getId(), item.getName(), item.getPrice(),
                item.isAvailable(), item.isKitchen());
    }

    public static ItemDto toItemDto(MenuItem menuItem, BaseItem baseItem, List<BaseItem> sideOptions) {
        List<BaseItemDto> sideOptionDtos = sideOptions == null ? null
                : sideOptions.stream().map(DtoMapper::toBaseItemDto).collect(Collectors.toList());
        return new ItemDto(
                baseItem.getId(),
                baseItem.getName(),
                baseItem.getPrice(),
                baseItem.isAvailable(),
                baseItem.isKitchen(),
                menuItem.getSideSources(),
                sideOptionDtos);
    }

    public static ComboSlotDto toComboSlotDto(ComboSlot slot, Map<UUID, BaseItem> baseItems) {
        List<BaseItemDto> options = slot.getOptionOrder().stream()
                .map(baseItems::get)
                .filter(Objects::nonNull)
                .map(DtoMapper::toBaseItemDto)
                .collect(Collectors.toList());
        return new ComboSlotDto(slot.getId(), slot.getName(), options, slot.getOptionOrder(),
                slot.isRequired());
    }

    public static ComboItemDto toComboItemDto(ComboItem combo, Map<UUID, BaseItem> baseItems) {
        List<BaseItemDto> components = combo.getComponents().stream()
                .map(baseItems::get)
                .filter(Objects::nonNull)
                .map(DtoMapper::toBaseItemDto)
                .collect(Collectors.toList());
        List<ComboSlotDto> slots = combo.getSlots().stream()
                .map(slot -> toComboSlotDto(slot, baseItems))
                .collect(Collectors.toList());
        return new ComboItemDto(
                combo.getId(), combo.getName(), combo.getCategory(),
                components, slots, combo.getPrice(), combo.isAvailable(), combo.isKitchen());
    }

    public static CategoryDto toCategoryDto(CategoryEntry entry, Menu menu) {
        List<ItemDto> items = entry.getItems().stream()
                .map(menuItem -> {
                    BaseItem baseItem = menu.getBaseItem(menuItem.getBaseItemId());
                    if (baseItem == null) return null;
                    List<BaseItem> sideOptions = menu.getSideOptions(menuItem.getSideSources());
                    return toItemDto(menuItem, baseItem, sideOptions);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CategoryDto(entry.isVisible(), items);
    }

    public static MenuDto toMenuDto(Menu menu) {
        List<BaseItemDto> baseItemDtos = menu.getBaseItems().values().stream()
                .map(DtoMapper::toBaseItemDto)
                .collect(Collectors.toList());

        Map<String, CategoryDto> catDtos = new LinkedHashMap<>();
        for (Map.Entry<String, CategoryEntry> e : menu.getCategories().entrySet()) {
            catDtos.put(e.getKey(), toCategoryDto(e.getValue(), menu));
        }

        List<ComboItemDto> comboDtos = menu.getCombos().values().stream()
                .map(combo -> toComboItemDto(combo, menu.getBaseItems()))
                .collect(Collectors.toList());

        return new MenuDto(baseItemDtos, catDtos, comboDtos, menu.getCategoryOrder());
    }

    public static OrderItemDto toOrderItemDto(OrderItem item) {
        if (OrderItem.TYPE_COMBO.equals(item.getType())) {
            List<ComboSlotSelectionDto> slotDtos = null;
            if (item.getSlotSelections() != null) {
                slotDtos = item.getSlotSelections().stream()
                        .map(s -> new ComboSlotSelectionDto(
                                s.getSlotId(), s.getSelectedBaseItemId(), s.getSelectedName()))
                        .collect(Collectors.toList());
            }
            return new OrderItemDto(
                    item.getType(), item.getName(), null,
                    item.getMainPrice(), item.getSidePrice(), item.getComment(),
                    item.getComboId(), slotDtos);
        }
        return new OrderItemDto(
                item.getType(), item.getName(), item.getSelectedSide(),
                item.getMainPrice(), item.getSidePrice(), item.getComment(),
                null, null);
    }

    public static OrderDto toOrderDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(DtoMapper::toOrderItemDto)
                .collect(Collectors.toList());
        return new OrderDto(items, order.getSubtotal(), order.getTotal(), order.getTax(),
                order.getTaxRate(), order.getComment());
    }

    public static TicketDto toTicketDto(Ticket ticket) {
        if (ticket == null) return null;
        List<OrderDto> orders = ticket.getOrders().stream()
                .map(DtoMapper::toOrderDto)
                .collect(Collectors.toList());

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        java.time.ZoneId zone = java.time.ZoneId.of("UTC");

        return new TicketDto(
                ticket.getId(),
                ticket.getTableNumber(),
                orders,
                ticket.getSubtotal(),
                ticket.getTotal(),
                ticket.getTax(),
                ticket.getStatus(),
                ticket.getCreatedAt() != null
                        ? ticket.getCreatedAt().atZone(zone).format(formatter) : null,
                ticket.getClosedAt() != null
                        ? ticket.getClosedAt().atZone(zone).format(formatter) : null,
                ticket.getComment());
    }
}
