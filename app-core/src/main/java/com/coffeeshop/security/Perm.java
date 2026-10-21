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
    public static final String SUPPLIERS = "suppliers";
    public static final String PURCHASE_ORDERS = "purchase-orders";
    public static final String SUPPLIER_RETURNS = "supplier-returns";
    public static final String SUPPLIER_DEBTS = "supplier-debts";
    public static final String STOCKTAKES = "stocktakes";
    public static final String INVENTORY_REPORT = "inventory-report";
    public static final String COFFEE_COSTS = "coffee-costs";
    public static final String CASH_SHIFTS = "cash-shifts";
    public static final String WORK_SHIFTS = "work-shifts";
    public static final String AUDIT_LOGS = "audit-logs";
    public static final String PAYMENT_RECONCILIATION = "payment-reconciliation";
    public static final String DELIVERY_ZONES = "delivery-zones";
    public static final String INVOICES = "invoices";
    public static final String NOTIFICATIONS = "notifications";
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
    /** Suppliers are picked on purchase orders and on loose imports (Nhập / xuất kho). */
    public static final String CAN_READ_SUPPLIERS = "hasAnyAuthority('suppliers','purchase-orders','stock','supplier-returns','supplier-debts')";
    public static final String CAN_SUPPLIERS = "hasAuthority('suppliers')";
    /** Staff with Nhập / xuất kho see the orders waiting for goods and receive them. */
    public static final String CAN_READ_PURCHASE_ORDERS = "hasAnyAuthority('purchase-orders','stock')";
    public static final String CAN_PURCHASE_ORDERS = "hasAuthority('purchase-orders')";
    public static final String CAN_RECEIVE_GOODS = "hasAnyAuthority('purchase-orders','stock')";
    public static final String CAN_READ_RECEIPTS = "hasAnyAuthority('purchase-orders','stock','stock-history','supplier-returns','supplier-debts')";
    /** Hủy phiếu nhập: the manager (người lập phiếu đặt hàng). */
    public static final String CAN_CANCEL_RECEIPT = "hasAuthority('purchase-orders')";
    public static final String CAN_SUPPLIER_RETURNS = "hasAuthority('supplier-returns')";
    public static final String CAN_SUPPLIER_DEBTS = "hasAuthority('supplier-debts')";
    public static final String CAN_STOCKTAKE = "hasAuthority('stocktakes')";
    public static final String CAN_INVENTORY_REPORT = "hasAuthority('inventory-report')";
    public static final String CAN_COFFEE_COSTS = "hasAuthority('coffee-costs')";
    /** changing a selling price from the giá vốn screen is a menu edit */
    public static final String CAN_COFFEE_COSTS_EDIT = "hasAuthority('coffee-costs') and hasAuthority('menu.edit')";
    /** thu ngân mở / chốt ca ở quầy */
    public static final String CAN_CASH_SHIFTS = "hasAnyAuthority('cash-shifts','pos')";
    public static final String CAN_WORK_SHIFTS = "hasAuthority('work-shifts')";
    /** lịch ca is read by the manager screen and staff management */
    public static final String CAN_READ_WORK_SHIFTS = "hasAnyAuthority('work-shifts','staff')";
    public static final String CAN_AUDIT_LOGS = "hasAuthority('audit-logs')";
    public static final String CAN_PAYMENT_RECONCILIATION = "hasAuthority('payment-reconciliation')";
    public static final String CAN_DELIVERY_ZONES = "hasAuthority('delivery-zones')";
    /** thu ngân also adds the buyer's company details at the counter */
    public static final String CAN_INVOICES = "hasAnyAuthority('invoices','pos')";
    public static final String CAN_NOTIFICATIONS = "hasAuthority('notifications')";
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
