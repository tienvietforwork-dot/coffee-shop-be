package com.coffeeshop.security;

/**
 * Permission codes (= permissions.code, one per screen like MES MENU_CD) and the
 * {@code @PreAuthorize} expressions built from them. An API shared by several screens accepts
 * any of their permissions ("hasAnyAuthority").
 * Keep in sync with database/seed.sql and the frontend src/lib/perm.ts.
 */
public final class Perm {

    private Perm() {
    }

    public static final String DASHBOARD = "dashboard";
    public static final String ORDERS = "orders";
    public static final String ORDERS_REFUND = "orders.refund";
    public static final String POS = "pos";
    public static final String SHIPMENTS = "shipments";
    public static final String INCIDENTS = "incidents";
    public static final String INCIDENTS_RESOLVE = "incidents.resolve";
    public static final String MENU = "menu";
    public static final String MENU_EDIT = "menu.edit";
    public static final String CATEGORIES = "categories";
    public static final String MATERIALS = "materials";
    public static final String MATERIALS_EDIT = "materials.edit";
    public static final String STOCK = "stock";
    public static final String STOCK_HISTORY = "stock-history";
    public static final String TABLES = "tables";
    public static final String TABLES_EDIT = "tables.edit";
    public static final String CRM_CUSTOMERS = "crm.customers";
    public static final String PROMOTIONS = "promotions";
    public static final String STAFF = "staff";
    public static final String USERS = "users";
    public static final String ROLES = "roles";
    public static final String ACCOUNT = "account";

    // ---- @PreAuthorize expressions ---------------------------------------
    public static final String CAN_DASHBOARD = "hasAuthority('dashboard')";
    /** Menu / categories / recipes are read by the menu screens, the counter and promotion forms. */
    public static final String CAN_READ_CATALOG = "hasAnyAuthority('menu','categories','pos','promotions')";
    public static final String CAN_MENU_STATUS = "hasAuthority('menu')";
    public static final String CAN_MENU_EDIT = "hasAuthority('menu.edit')";
    public static final String CAN_READ_MATERIALS = "hasAnyAuthority('materials','stock','stock-history','menu')";
    public static final String CAN_MATERIALS_EDIT = "hasAuthority('materials.edit')";
    public static final String CAN_STOCK = "hasAuthority('stock')";
    public static final String CAN_STOCK_HISTORY = "hasAnyAuthority('stock-history','materials')";
    public static final String CAN_INVENTORY_ALERTS = "hasAnyAuthority('dashboard','materials','stock')";
    public static final String CAN_READ_TABLES = "hasAnyAuthority('tables','pos')";
    public static final String CAN_TABLES_EDIT = "hasAuthority('tables.edit')";
    public static final String CAN_ORDERS = "hasAuthority('orders')";
    public static final String CAN_READ_ORDERS = "hasAnyAuthority('orders','pos','shipments','incidents','dashboard')";
    public static final String CAN_POS = "hasAuthority('pos')";
    public static final String CAN_COLLECT = "hasAnyAuthority('orders','pos')";
    public static final String CAN_REFUND = "hasAuthority('orders.refund')";
    public static final String CAN_SHIPMENTS = "hasAuthority('shipments')";
    public static final String CAN_INCIDENTS = "hasAnyAuthority('incidents','orders')";
    public static final String CAN_RESOLVE_INCIDENTS = "hasAuthority('incidents.resolve')";
    public static final String CAN_LOOKUP_CUSTOMER = "hasAnyAuthority('pos','orders','crm.customers')";
    public static final String CAN_READ_STAFF = "hasAnyAuthority('staff','users')";
    public static final String CAN_STAFF = "hasAuthority('staff')";
    public static final String CAN_USERS = "hasAuthority('users')";
    public static final String CAN_READ_ROLES = "hasAnyAuthority('roles','users')";
    public static final String CAN_ROLES = "hasAuthority('roles')";
    public static final String CAN_ACCOUNT = "hasAuthority('account')";
}
