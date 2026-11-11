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
    public static final String CASH_SHIFTS = "cash-shifts";
    public static final String WORK_SHIFTS = "work-shifts";
    public static final String AUDIT_LOGS = "audit-logs";
    public static final String PAYMENT_RECONCILIATION = "payment-reconciliation";
    public static final String DELIVERY_ZONES = "delivery-zones";
    /** người giao: chỉ thấy và cập nhật đơn giao được phân công cho mình */
    public static final String MY_DELIVERIES = "my-deliveries";
    /** lịch hẹn giao + thứ tự pha chế ưu tiên */
    public static final String DELIVERY_SCHEDULE = "delivery-schedule";
    /** gói giao cà phê định kỳ */
    public static final String SUBSCRIPTIONS = "subscriptions";
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
    public static final String CAN_READ_MATERIALS = "hasAnyAuthority('materials','menu')";
    public static final String CAN_MATERIALS_EDIT = "hasAuthority('materials.edit')";
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
    public static final String CAN_MY_DELIVERIES = "hasAuthority('my-deliveries')";
    /** đổi trạng thái / xem tuyến: quản lý giao hàng hoặc người giao (đơn của mình — kiểm tra trong ShipmentService) */
    public static final String CAN_DELIVER = "hasAnyAuthority('shipments','my-deliveries')";
    public static final String CAN_DELIVERY_SCHEDULE = "hasAnyAuthority('delivery-schedule','orders')";
    public static final String CAN_SUBSCRIPTIONS = "hasAuthority('subscriptions')";
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
