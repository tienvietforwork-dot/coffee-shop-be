--
-- PostgreSQL database dump
--

\restrict iqB58SmG5gGgs1U7b6vtLFXE7eLi2xxLn5DsnvcmsL0eZC0pFmsvs9KrCHZkXXA

-- Dumped from database version 18.6 (4e955f5)
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: legacy_backup; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA legacy_backup;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: categories; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.categories (
    id bigint NOT NULL,
    name character varying(150) NOT NULL
);


--
-- Name: categories_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.categories_id_seq OWNED BY legacy_backup.categories.id;


--
-- Name: daily_revenue; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.daily_revenue (
    id bigint NOT NULL,
    revenue_date date NOT NULL,
    total_revenue numeric(16,2) DEFAULT 0 NOT NULL,
    total_orders integer DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: daily_revenue_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.daily_revenue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: daily_revenue_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.daily_revenue_id_seq OWNED BY legacy_backup.daily_revenue.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: material_transactions; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.material_transactions (
    id bigint NOT NULL,
    material_id bigint NOT NULL,
    type character varying(10) NOT NULL,
    quantity numeric(14,3) NOT NULL,
    reason character varying(500),
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: material_transactions_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.material_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.material_transactions_id_seq OWNED BY legacy_backup.material_transactions.id;


--
-- Name: materials; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.materials (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    unit character varying(20) NOT NULL,
    quantity_in_stock numeric(14,3) DEFAULT 0 NOT NULL,
    min_threshold numeric(14,3) DEFAULT 0 NOT NULL,
    unit_price numeric(14,2) DEFAULT 0 NOT NULL
);


--
-- Name: materials_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.materials_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: materials_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.materials_id_seq OWNED BY legacy_backup.materials.id;


--
-- Name: order_items; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.order_items (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    subtotal numeric(14,2) NOT NULL
);


--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.order_items_id_seq OWNED BY legacy_backup.order_items.id;


--
-- Name: orders; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.orders (
    id bigint NOT NULL,
    order_code character varying(30) NOT NULL,
    customer_name character varying(150),
    customer_phone character varying(30),
    order_type character varying(20) DEFAULT 'DINE_IN'::character varying NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    total_amount numeric(14,2) DEFAULT 0 NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.orders_id_seq OWNED BY legacy_backup.orders.id;


--
-- Name: payments; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.payments (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    method character varying(20) DEFAULT 'CASH'::character varying NOT NULL,
    amount numeric(14,2) NOT NULL,
    status character varying(20) DEFAULT 'UNPAID'::character varying NOT NULL,
    paid_at timestamp without time zone
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.payments_id_seq OWNED BY legacy_backup.payments.id;


--
-- Name: product_materials; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.product_materials (
    product_id bigint NOT NULL,
    material_id bigint NOT NULL,
    quantity_required numeric(14,3) NOT NULL
);


--
-- Name: products; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.products (
    id bigint NOT NULL,
    name character varying(200) NOT NULL,
    category_id bigint,
    price numeric(12,2) DEFAULT 0 NOT NULL,
    image_url character varying(500),
    description character varying(2000),
    active boolean DEFAULT true NOT NULL
);


--
-- Name: products_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.products_id_seq OWNED BY legacy_backup.products.id;


--
-- Name: shipments; Type: TABLE; Schema: legacy_backup; Owner: -
--

CREATE TABLE legacy_backup.shipments (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    shipper_id bigint,
    address character varying(500),
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    note character varying(1000),
    delivered_at timestamp without time zone
);


--
-- Name: shipments_id_seq; Type: SEQUENCE; Schema: legacy_backup; Owner: -
--

CREATE SEQUENCE legacy_backup.shipments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shipments_id_seq; Type: SEQUENCE OWNED BY; Schema: legacy_backup; Owner: -
--

ALTER SEQUENCE legacy_backup.shipments_id_seq OWNED BY legacy_backup.shipments.id;


--
-- Name: background_service_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_service_logs (
    id bigint NOT NULL,
    service character varying(100) NOT NULL,
    check_time timestamp without time zone NOT NULL,
    duration_ms integer,
    msg text,
    next_run_time timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: background_service_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.background_service_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: background_service_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.background_service_logs_id_seq OWNED BY public.background_service_logs.id;


--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cart_items (
    id bigint NOT NULL,
    cart_id bigint NOT NULL,
    coffee_id bigint NOT NULL,
    quantity integer NOT NULL,
    note character varying(255),
    subtotal numeric(12,2) NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT cart_items_quantity_check CHECK ((quantity > 0)),
    CONSTRAINT cart_items_subtotal_check CHECK ((subtotal >= (0)::numeric))
);


--
-- Name: cart_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cart_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cart_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cart_items_id_seq OWNED BY public.cart_items.id;


--
-- Name: carts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carts (
    id bigint NOT NULL,
    session_code character varying(64) NOT NULL,
    table_id bigint,
    customer_id bigint,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT carts_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'ORDERED'::character varying, 'ABANDONED'::character varying])::text[])))
);


--
-- Name: carts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.carts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: carts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.carts_id_seq OWNED BY public.carts.id;


--
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    display_order integer DEFAULT 0 NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;


--
-- Name: coffees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coffees (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    image_url character varying(500),
    price numeric(12,2) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    image_id bigint,
    auto_sold_out boolean DEFAULT false NOT NULL,
    CONSTRAINT coffees_price_check CHECK ((price >= (0)::numeric)),
    CONSTRAINT coffees_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'SOLD_OUT'::character varying, 'HIDDEN'::character varying, 'DISCONTINUED'::character varying])::text[])))
);


--
-- Name: COLUMN coffees.image_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coffees.image_id IS 'ảnh tải lên; image_url chỉ dùng cho link ngoài';


--
-- Name: COLUMN coffees.auto_sold_out; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.coffees.auto_sold_out IS 'SOLD_OUT do hết nguyên liệu (tự mở bán lại khi đủ kho)';


--
-- Name: coffees_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.coffees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: coffees_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.coffees_id_seq OWNED BY public.coffees.id;


--
-- Name: customer_addresses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_addresses (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    label character varying(30),
    recipient_name character varying(100) NOT NULL,
    recipient_phone character varying(15) NOT NULL,
    address character varying(255) NOT NULL,
    is_default boolean DEFAULT false NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: customer_addresses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_addresses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_addresses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_addresses_id_seq OWNED BY public.customer_addresses.id;


--
-- Name: customer_group_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_group_members (
    id bigint NOT NULL,
    group_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    is_manual boolean DEFAULT false NOT NULL,
    added_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: customer_group_members_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_group_members_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_group_members_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_group_members_id_seq OWNED BY public.customer_group_members.id;


--
-- Name: customer_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_groups (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    criteria_type character varying(30),
    criteria_rule jsonb,
    recalculated_at timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT customer_groups_criteria_type_check CHECK (((criteria_type)::text = ANY ((ARRAY['SPENDING'::character varying, 'FREQUENCY'::character varying, 'RECENCY'::character varying, 'FAVORITE_COFFEE'::character varying, 'MANUAL'::character varying])::text[])))
);


--
-- Name: customer_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_groups_id_seq OWNED BY public.customer_groups.id;


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id bigint NOT NULL,
    full_name character varying(100),
    phone character varying(15) NOT NULL,
    email character varying(100),
    loyalty_points integer DEFAULT 0 NOT NULL,
    registered_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT customers_loyalty_points_check CHECK ((loyalty_points >= 0))
);


--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;


--
-- Name: dining_tables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dining_tables (
    id bigint NOT NULL,
    table_no character varying(10) NOT NULL,
    qr_code character varying(255) NOT NULL,
    area character varying(50),
    capacity integer,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT dining_tables_capacity_check CHECK ((capacity > 0)),
    CONSTRAINT dining_tables_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'OCCUPIED'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: dining_tables_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dining_tables_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dining_tables_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dining_tables_id_seq OWNED BY public.dining_tables.id;


--
-- Name: feedbacks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feedbacks (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    topic character varying(50),
    category character varying(30),
    sentiment character varying(20),
    content text NOT NULL,
    status character varying(20) DEFAULT 'NEW'::character varying NOT NULL,
    escalated boolean DEFAULT false NOT NULL,
    resolution text,
    satisfaction character varying(20),
    closed_at timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT feedbacks_category_check CHECK (((category)::text = ANY ((ARRAY['COMPLAINT'::character varying, 'SUGGESTION'::character varying, 'PRAISE'::character varying])::text[]))),
    CONSTRAINT feedbacks_satisfaction_check CHECK (((satisfaction)::text = ANY ((ARRAY['SATISFIED'::character varying, 'UNSATISFIED'::character varying])::text[]))),
    CONSTRAINT feedbacks_sentiment_check CHECK (((sentiment)::text = ANY ((ARRAY['POSITIVE'::character varying, 'NEUTRAL'::character varying, 'NEGATIVE'::character varying])::text[]))),
    CONSTRAINT feedbacks_status_check CHECK (((status)::text = ANY ((ARRAY['NEW'::character varying, 'IN_PROGRESS'::character varying, 'ESCALATED'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: feedbacks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.feedbacks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: feedbacks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.feedbacks_id_seq OWNED BY public.feedbacks.id;


--
-- Name: images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.images (
    id bigint NOT NULL,
    content_type character varying(50) NOT NULL,
    size_bytes integer NOT NULL,
    data bytea NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT images_size_bytes_check CHECK ((size_bytes > 0))
);


--
-- Name: images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.images_id_seq OWNED BY public.images.id;


--
-- Name: incidents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incidents (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    handled_by bigint,
    type character varying(30) NOT NULL,
    severity character varying(20),
    reason character varying(255),
    description text,
    resolution text,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    reported_at timestamp without time zone DEFAULT now() NOT NULL,
    resolved_at timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT incidents_severity_check CHECK (((severity)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[]))),
    CONSTRAINT incidents_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'RESOLVED'::character varying])::text[]))),
    CONSTRAINT incidents_type_check CHECK (((type)::text = ANY ((ARRAY['WRONG_ITEM'::character varying, 'QUALITY'::character varying, 'LATE'::character varying, 'SPILLED'::character varying, 'MISSING_ITEM'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: incidents_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.incidents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: incidents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.incidents_id_seq OWNED BY public.incidents.id;


--
-- Name: interactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.interactions (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    channel character varying(20),
    content text,
    note text,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    remind_at timestamp without time zone,
    closed_at timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT interactions_channel_check CHECK (((channel)::text = ANY ((ARRAY['CALL'::character varying, 'CHAT'::character varying, 'EMAIL'::character varying, 'IN_STORE'::character varying, 'NOTE'::character varying])::text[]))),
    CONSTRAINT interactions_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'FOLLOW_UP'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: interactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.interactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: interactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.interactions_id_seq OWNED BY public.interactions.id;


--
-- Name: loyalty_point_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loyalty_point_history (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    order_id bigint,
    points_change integer NOT NULL,
    reason character varying(100),
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: loyalty_point_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.loyalty_point_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: loyalty_point_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.loyalty_point_history_id_seq OWNED BY public.loyalty_point_history.id;


--
-- Name: material_batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material_batches (
    id bigint NOT NULL,
    material_id bigint NOT NULL,
    import_quantity numeric(12,2) NOT NULL,
    remaining_quantity numeric(12,2) NOT NULL,
    unit_cost numeric(12,2),
    expiry_date date,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    ready_at timestamp without time zone,
    expires_at timestamp without time zone,
    CONSTRAINT material_batches_check CHECK ((remaining_quantity <= import_quantity)),
    CONSTRAINT material_batches_import_quantity_check CHECK ((import_quantity > (0)::numeric)),
    CONSTRAINT material_batches_remaining_quantity_check CHECK ((remaining_quantity >= (0)::numeric)),
    CONSTRAINT material_batches_status_check CHECK (((status)::text = ANY ((ARRAY['PREPARING'::character varying, 'AVAILABLE'::character varying, 'DEPLETED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT material_batches_unit_cost_check CHECK ((unit_cost >= (0)::numeric))
);


--
-- Name: material_batches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.material_batches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material_batches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.material_batches_id_seq OWNED BY public.material_batches.id;


--
-- Name: material_components; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material_components (
    id bigint NOT NULL,
    material_id bigint NOT NULL,
    component_id bigint NOT NULL,
    quantity numeric(12,2) NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT material_components_check CHECK ((material_id <> component_id)),
    CONSTRAINT material_components_quantity_check CHECK ((quantity > (0)::numeric))
);


--
-- Name: material_components_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.material_components_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material_components_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.material_components_id_seq OWNED BY public.material_components.id;


--
-- Name: material_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material_transactions (
    id bigint NOT NULL,
    batch_id bigint NOT NULL,
    staff_id bigint,
    order_item_id bigint,
    type character varying(20) NOT NULL,
    quantity numeric(12,2) NOT NULL,
    note character varying(255),
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    produced_batch_id bigint,
    CONSTRAINT material_transactions_type_check CHECK (((type)::text = ANY ((ARRAY['IMPORT'::character varying, 'EXPORT'::character varying, 'SALE'::character varying, 'ADJUSTMENT'::character varying, 'PRODUCTION_USE'::character varying, 'PRODUCE'::character varying])::text[])))
);


--
-- Name: material_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.material_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: material_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.material_transactions_id_seq OWNED BY public.material_transactions.id;


--
-- Name: materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materials (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    unit character varying(20) NOT NULL,
    stock_quantity numeric(12,2) DEFAULT 0 NOT NULL,
    min_stock numeric(12,2) DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    kind character varying(20) DEFAULT 'RAW'::character varying NOT NULL,
    yield_quantity numeric(12,2),
    prep_minutes integer,
    shelf_life_minutes integer,
    instructions text,
    CONSTRAINT materials_kind_check CHECK (((kind)::text = ANY ((ARRAY['RAW'::character varying, 'PREPARED'::character varying])::text[]))),
    CONSTRAINT materials_min_stock_check CHECK ((min_stock >= (0)::numeric)),
    CONSTRAINT materials_prep_minutes_check CHECK ((prep_minutes >= 0)),
    CONSTRAINT materials_shelf_life_minutes_check CHECK ((shelf_life_minutes > 0)),
    CONSTRAINT materials_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT materials_stock_quantity_check CHECK ((stock_quantity >= (0)::numeric)),
    CONSTRAINT materials_yield_quantity_check CHECK ((yield_quantity > (0)::numeric))
);


--
-- Name: COLUMN materials.instructions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.materials.instructions IS 'PREPARED: hướng dẫn làm 1 mẻ (định chuẩn)';


--
-- Name: materials_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.materials_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: materials_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.materials_id_seq OWNED BY public.materials.id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    coffee_id bigint NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    line_total numeric(12,2) NOT NULL,
    note character varying(255),
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT order_items_line_total_check CHECK ((line_total >= (0)::numeric)),
    CONSTRAINT order_items_quantity_check CHECK ((quantity > 0)),
    CONSTRAINT order_items_unit_price_check CHECK ((unit_price >= (0)::numeric))
);


--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id bigint NOT NULL,
    order_code character varying(30) NOT NULL,
    customer_id bigint,
    staff_id bigint,
    table_id bigint,
    cart_id bigint,
    promotion_id bigint,
    order_type character varying(20) NOT NULL,
    channel character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    subtotal numeric(12,2) DEFAULT 0 NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0 NOT NULL,
    total_amount numeric(12,2) DEFAULT 0 NOT NULL,
    note text,
    cancel_reason text,
    pickup_time timestamp without time zone,
    ordered_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT orders_channel_check CHECK (((channel)::text = ANY ((ARRAY['COUNTER'::character varying, 'QR'::character varying, 'ONLINE'::character varying])::text[]))),
    CONSTRAINT orders_discount_amount_check CHECK ((discount_amount >= (0)::numeric)),
    CONSTRAINT orders_order_type_check CHECK (((order_type)::text = ANY ((ARRAY['DINE_IN'::character varying, 'TAKE_AWAY'::character varying, 'DELIVERY'::character varying])::text[]))),
    CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'PREPARING'::character varying, 'READY'::character varying, 'DELIVERING'::character varying, 'COMPLETED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT orders_subtotal_check CHECK ((subtotal >= (0)::numeric)),
    CONSTRAINT orders_total_amount_check CHECK ((total_amount >= (0)::numeric))
);


--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    method character varying(20) NOT NULL,
    amount numeric(12,2) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    transaction_code character varying(100),
    paid_at timestamp without time zone,
    refund_amount numeric(12,2) DEFAULT 0 NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT payments_amount_check CHECK ((amount >= (0)::numeric)),
    CONSTRAINT payments_check CHECK ((refund_amount <= amount)),
    CONSTRAINT payments_method_check CHECK (((method)::text = ANY ((ARRAY['CASH'::character varying, 'BANK_TRANSFER'::character varying, 'E_WALLET'::character varying, 'CARD'::character varying])::text[]))),
    CONSTRAINT payments_refund_amount_check CHECK ((refund_amount >= (0)::numeric)),
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    id bigint NOT NULL,
    code character varying(60) NOT NULL,
    name character varying(150) NOT NULL,
    path character varying(100),
    module character varying(30) NOT NULL,
    description character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT permissions_module_check CHECK (((module)::text = ANY ((ARRAY['core'::character varying, 'crm'::character varying, 'promotions'::character varying, 'stats'::character varying, 'system'::character varying, 'customer'::character varying])::text[])))
);


--
-- Name: permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.permissions_id_seq OWNED BY public.permissions.id;


--
-- Name: product_bundle_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_bundle_items (
    id bigint NOT NULL,
    bundle_id bigint NOT NULL,
    coffee_id bigint NOT NULL,
    quantity integer DEFAULT 1 NOT NULL,
    optional boolean DEFAULT false NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT product_bundle_items_display_order_check CHECK ((display_order >= 0)),
    CONSTRAINT product_bundle_items_quantity_check CHECK ((quantity > 0))
);


--
-- Name: product_bundle_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_bundle_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_bundle_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_bundle_items_id_seq OWNED BY public.product_bundle_items.id;


--
-- Name: product_bundles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_bundles (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    bundle_price numeric(12,2) NOT NULL,
    starts_at timestamp without time zone,
    ends_at timestamp without time zone,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT product_bundles_bundle_price_check CHECK ((bundle_price >= (0)::numeric)),
    CONSTRAINT product_bundles_check CHECK (((ends_at IS NULL) OR (starts_at IS NULL) OR (ends_at >= starts_at))),
    CONSTRAINT product_bundles_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'PAUSED'::character varying])::text[])))
);


--
-- Name: product_bundles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_bundles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_bundles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_bundles_id_seq OWNED BY public.product_bundles.id;


--
-- Name: product_placement_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_placement_rules (
    id bigint NOT NULL,
    coffee_id bigint NOT NULL,
    section character varying(30) NOT NULL,
    title character varying(150),
    display_order integer DEFAULT 0 NOT NULL,
    starts_at timestamp without time zone,
    ends_at timestamp without time zone,
    active boolean DEFAULT true NOT NULL,
    publication_status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT product_placement_rules_check CHECK (((ends_at IS NULL) OR (starts_at IS NULL) OR (ends_at >= starts_at))),
    CONSTRAINT product_placement_rules_display_order_check CHECK ((display_order >= 0)),
    CONSTRAINT product_placement_rules_publication_status_check CHECK (((publication_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying])::text[]))),
    CONSTRAINT product_placement_rules_section_check CHECK (((section)::text = ANY ((ARRAY['FEATURED'::character varying, 'BEST_SELLER'::character varying, 'NEW_ARRIVAL'::character varying, 'RECOMMENDED'::character varying, 'HOME'::character varying])::text[])))
);


--
-- Name: product_placement_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_placement_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_placement_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_placement_rules_id_seq OWNED BY public.product_placement_rules.id;


--
-- Name: promotion_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotion_items (
    id bigint NOT NULL,
    promotion_id bigint NOT NULL,
    coffee_id bigint NOT NULL,
    custom_discount numeric(12,2),
    sale_price numeric(12,2),
    max_quantity integer,
    sold_quantity integer DEFAULT 0 NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT promotion_items_check CHECK (((max_quantity IS NULL) OR (sold_quantity <= max_quantity))),
    CONSTRAINT promotion_items_custom_discount_check CHECK ((custom_discount >= (0)::numeric)),
    CONSTRAINT promotion_items_max_quantity_check CHECK ((max_quantity > 0)),
    CONSTRAINT promotion_items_sale_price_check CHECK ((sale_price >= (0)::numeric)),
    CONSTRAINT promotion_items_sold_quantity_check CHECK ((sold_quantity >= 0))
);


--
-- Name: promotion_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.promotion_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promotion_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.promotion_items_id_seq OWNED BY public.promotion_items.id;


--
-- Name: promotions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promotions (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    apply_scope character varying(10) NOT NULL,
    discount_percent numeric(5,2),
    min_order_amount numeric(12,2),
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT promotions_apply_scope_check CHECK (((apply_scope)::text = ANY ((ARRAY['ORDER'::character varying, 'COFFEE'::character varying])::text[]))),
    CONSTRAINT promotions_check CHECK ((end_date > start_date)),
    CONSTRAINT promotions_discount_percent_check CHECK (((discount_percent > (0)::numeric) AND (discount_percent <= (100)::numeric))),
    CONSTRAINT promotions_min_order_amount_check CHECK ((min_order_amount >= (0)::numeric)),
    CONSTRAINT promotions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'PAUSED'::character varying, 'ENDED'::character varying])::text[])))
);


--
-- Name: promotions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.promotions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promotions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.promotions_id_seq OWNED BY public.promotions.id;


--
-- Name: recipe_materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_materials (
    id bigint NOT NULL,
    recipe_id bigint NOT NULL,
    material_id bigint NOT NULL,
    quantity numeric(10,2) NOT NULL,
    note character varying(255),
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT recipe_materials_quantity_check CHECK ((quantity > (0)::numeric))
);


--
-- Name: recipe_materials_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recipe_materials_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recipe_materials_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recipe_materials_id_seq OWNED BY public.recipe_materials.id;


--
-- Name: recipe_steps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_steps (
    id bigint NOT NULL,
    recipe_id bigint NOT NULL,
    step_no integer NOT NULL,
    instruction text NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT recipe_steps_step_no_check CHECK ((step_no > 0))
);


--
-- Name: recipe_steps_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recipe_steps_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recipe_steps_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recipe_steps_id_seq OWNED BY public.recipe_steps.id;


--
-- Name: recipes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipes (
    id bigint NOT NULL,
    coffee_id bigint NOT NULL,
    brew_method character varying(30),
    version integer DEFAULT 1 NOT NULL,
    description text,
    brew_time_min integer,
    is_active boolean DEFAULT true NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT recipes_brew_method_check CHECK (((brew_method)::text = ANY ((ARRAY['PHIN'::character varying, 'MACHINE'::character varying, 'COLD_BREW'::character varying, 'POUR_OVER'::character varying])::text[]))),
    CONSTRAINT recipes_brew_time_min_check CHECK ((brew_time_min >= 0)),
    CONSTRAINT recipes_version_check CHECK ((version > 0))
);


--
-- Name: recipes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recipes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recipes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recipes_id_seq OWNED BY public.recipes.id;


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    id bigint NOT NULL,
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: role_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.role_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: role_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.role_permissions_id_seq OWNED BY public.role_permissions.id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    code character varying(30) NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(255),
    active boolean DEFAULT true NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT roles_code_check CHECK (((code)::text = ANY ((ARRAY['ADMIN'::character varying, 'STAFF'::character varying, 'CUSTOMER'::character varying])::text[])))
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- Name: shipments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shipments (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    address_id bigint NOT NULL,
    carrier character varying(50),
    tracking_code character varying(100),
    shipping_fee numeric(12,2) DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    booked_at timestamp without time zone,
    driver_accepted_at timestamp without time zone,
    delivered_at timestamp without time zone,
    note text,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT shipments_shipping_fee_check CHECK ((shipping_fee >= (0)::numeric)),
    CONSTRAINT shipments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'BOOKED'::character varying, 'DRIVER_ACCEPTED'::character varying, 'DELIVERING'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: shipments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.shipments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shipments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shipments_id_seq OWNED BY public.shipments.id;


--
-- Name: staff; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff (
    id bigint NOT NULL,
    full_name character varying(100) NOT NULL,
    "position" character varying(50),
    phone character varying(15) NOT NULL,
    email character varying(100),
    hire_date date,
    work_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT staff_work_status_check CHECK (((work_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ON_LEAVE'::character varying, 'RESIGNED'::character varying])::text[])))
);


--
-- Name: staff_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.staff_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: staff_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.staff_id_seq OWNED BY public.staff.id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: user_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_roles_id_seq OWNED BY public.user_roles.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(150),
    email character varying(150),
    phone character varying(30),
    active boolean DEFAULT true NOT NULL,
    staff_id bigint,
    customer_id bigint,
    last_login_at timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50)
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vouchers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vouchers (
    id bigint NOT NULL,
    code character varying(30) NOT NULL,
    customer_id bigint,
    order_id bigint,
    incident_id bigint,
    discount_value numeric(12,2) NOT NULL,
    min_order_value numeric(12,2) DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'ISSUED'::character varying NOT NULL,
    issued_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone,
    created_by character varying(50) DEFAULT 'system'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    updated_at timestamp without time zone,
    del_flag boolean DEFAULT false NOT NULL,
    del_user character varying(50),
    CONSTRAINT vouchers_check CHECK (((expires_at IS NULL) OR (expires_at > issued_at))),
    CONSTRAINT vouchers_discount_value_check CHECK ((discount_value > (0)::numeric)),
    CONSTRAINT vouchers_min_order_value_check CHECK ((min_order_value >= (0)::numeric)),
    CONSTRAINT vouchers_status_check CHECK (((status)::text = ANY ((ARRAY['ISSUED'::character varying, 'USED'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying])::text[])))
);


--
-- Name: vouchers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vouchers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vouchers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vouchers_id_seq OWNED BY public.vouchers.id;


--
-- Name: categories id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.categories ALTER COLUMN id SET DEFAULT nextval('legacy_backup.categories_id_seq'::regclass);


--
-- Name: daily_revenue id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.daily_revenue ALTER COLUMN id SET DEFAULT nextval('legacy_backup.daily_revenue_id_seq'::regclass);


--
-- Name: material_transactions id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.material_transactions ALTER COLUMN id SET DEFAULT nextval('legacy_backup.material_transactions_id_seq'::regclass);


--
-- Name: materials id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.materials ALTER COLUMN id SET DEFAULT nextval('legacy_backup.materials_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.order_items ALTER COLUMN id SET DEFAULT nextval('legacy_backup.order_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.orders ALTER COLUMN id SET DEFAULT nextval('legacy_backup.orders_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.payments ALTER COLUMN id SET DEFAULT nextval('legacy_backup.payments_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.products ALTER COLUMN id SET DEFAULT nextval('legacy_backup.products_id_seq'::regclass);


--
-- Name: shipments id; Type: DEFAULT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.shipments ALTER COLUMN id SET DEFAULT nextval('legacy_backup.shipments_id_seq'::regclass);


--
-- Name: background_service_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_service_logs ALTER COLUMN id SET DEFAULT nextval('public.background_service_logs_id_seq'::regclass);


--
-- Name: cart_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items ALTER COLUMN id SET DEFAULT nextval('public.cart_items_id_seq'::regclass);


--
-- Name: carts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts ALTER COLUMN id SET DEFAULT nextval('public.carts_id_seq'::regclass);


--
-- Name: categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);


--
-- Name: coffees id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coffees ALTER COLUMN id SET DEFAULT nextval('public.coffees_id_seq'::regclass);


--
-- Name: customer_addresses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses ALTER COLUMN id SET DEFAULT nextval('public.customer_addresses_id_seq'::regclass);


--
-- Name: customer_group_members id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_group_members ALTER COLUMN id SET DEFAULT nextval('public.customer_group_members_id_seq'::regclass);


--
-- Name: customer_groups id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_groups ALTER COLUMN id SET DEFAULT nextval('public.customer_groups_id_seq'::regclass);


--
-- Name: customers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers ALTER COLUMN id SET DEFAULT nextval('public.customers_id_seq'::regclass);


--
-- Name: dining_tables id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dining_tables ALTER COLUMN id SET DEFAULT nextval('public.dining_tables_id_seq'::regclass);


--
-- Name: feedbacks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedbacks ALTER COLUMN id SET DEFAULT nextval('public.feedbacks_id_seq'::regclass);


--
-- Name: images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.images ALTER COLUMN id SET DEFAULT nextval('public.images_id_seq'::regclass);


--
-- Name: incidents id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents ALTER COLUMN id SET DEFAULT nextval('public.incidents_id_seq'::regclass);


--
-- Name: interactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interactions ALTER COLUMN id SET DEFAULT nextval('public.interactions_id_seq'::regclass);


--
-- Name: loyalty_point_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_point_history ALTER COLUMN id SET DEFAULT nextval('public.loyalty_point_history_id_seq'::regclass);


--
-- Name: material_batches id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_batches ALTER COLUMN id SET DEFAULT nextval('public.material_batches_id_seq'::regclass);


--
-- Name: material_components id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_components ALTER COLUMN id SET DEFAULT nextval('public.material_components_id_seq'::regclass);


--
-- Name: material_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions ALTER COLUMN id SET DEFAULT nextval('public.material_transactions_id_seq'::regclass);


--
-- Name: materials id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materials ALTER COLUMN id SET DEFAULT nextval('public.materials_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions ALTER COLUMN id SET DEFAULT nextval('public.permissions_id_seq'::regclass);


--
-- Name: product_bundle_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_bundle_items ALTER COLUMN id SET DEFAULT nextval('public.product_bundle_items_id_seq'::regclass);


--
-- Name: product_bundles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_bundles ALTER COLUMN id SET DEFAULT nextval('public.product_bundles_id_seq'::regclass);


--
-- Name: product_placement_rules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_placement_rules ALTER COLUMN id SET DEFAULT nextval('public.product_placement_rules_id_seq'::regclass);


--
-- Name: promotion_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_items ALTER COLUMN id SET DEFAULT nextval('public.promotion_items_id_seq'::regclass);


--
-- Name: promotions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotions ALTER COLUMN id SET DEFAULT nextval('public.promotions_id_seq'::regclass);


--
-- Name: recipe_materials id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_materials ALTER COLUMN id SET DEFAULT nextval('public.recipe_materials_id_seq'::regclass);


--
-- Name: recipe_steps id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_steps ALTER COLUMN id SET DEFAULT nextval('public.recipe_steps_id_seq'::regclass);


--
-- Name: recipes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipes ALTER COLUMN id SET DEFAULT nextval('public.recipes_id_seq'::regclass);


--
-- Name: role_permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions ALTER COLUMN id SET DEFAULT nextval('public.role_permissions_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- Name: shipments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments ALTER COLUMN id SET DEFAULT nextval('public.shipments_id_seq'::regclass);


--
-- Name: staff id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff ALTER COLUMN id SET DEFAULT nextval('public.staff_id_seq'::regclass);


--
-- Name: user_roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles ALTER COLUMN id SET DEFAULT nextval('public.user_roles_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: vouchers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vouchers ALTER COLUMN id SET DEFAULT nextval('public.vouchers_id_seq'::regclass);


--
-- Data for Name: categories; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.categories (id, name) FROM stdin;
1	Coffee
2	Tea
3	Pastry
\.


--
-- Data for Name: daily_revenue; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.daily_revenue (id, revenue_date, total_revenue, total_orders, updated_at) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	20260916034100	init schema	SQL	V20260916034100__init_schema.sql	-174097122	neondb_owner	2026-09-16 03:41:17.857887	973	t
2	20260916034102	seed data	SQL	V20260916034102__seed_data.sql	1026817357	neondb_owner	2026-09-16 03:41:19.466562	777	t
\.


--
-- Data for Name: material_transactions; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.material_transactions (id, material_id, type, quantity, reason, created_by, created_at) FROM stdin;
\.


--
-- Data for Name: materials; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.materials (id, name, unit, quantity_in_stock, min_threshold, unit_price) FROM stdin;
1	Coffee Beans	kg	20.000	5.000	220000.00
2	Fresh Milk	l	15.000	5.000	32000.00
3	Sugar	kg	10.000	2.000	18000.00
4	Tea Leaves	kg	8.000	2.000	150000.00
5	Flour	kg	12.000	3.000	15000.00
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.order_items (id, order_id, product_id, quantity, unit_price, subtotal) FROM stdin;
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.orders (id, order_code, customer_name, customer_phone, order_type, status, total_amount, created_by, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.payments (id, order_id, method, amount, status, paid_at) FROM stdin;
\.


--
-- Data for Name: product_materials; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.product_materials (product_id, material_id, quantity_required) FROM stdin;
1	1	0.020
1	3	0.005
2	1	0.020
2	2	0.100
2	3	0.005
3	4	0.010
3	2	0.100
4	5	0.080
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.products (id, name, category_id, price, image_url, description, active) FROM stdin;
1	Black Coffee	1	25000.00	\N	Traditional Vietnamese black coffee	t
2	Milk Coffee	1	30000.00	\N	Coffee with fresh milk	t
3	Milk Tea	2	28000.00	\N	Milk tea with tea leaves	t
4	Croissant	3	20000.00	\N	Butter croissant	t
\.


--
-- Data for Name: shipments; Type: TABLE DATA; Schema: legacy_backup; Owner: -
--

COPY legacy_backup.shipments (id, order_id, shipper_id, address, status, note, delivered_at) FROM stdin;
\.


--
-- Data for Name: background_service_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.background_service_logs (id, service, check_time, duration_ms, msg, next_run_time, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	CoffeeStatusBackgroundService	2026-10-07 06:49:37.859513	281	Không đổi trạng thái món nào	2026-10-07 06:54:38.150595	Service	2026-10-07 06:49:38.162017	\N	\N	f	\N
2	CoffeeStatusBackgroundService	2026-10-07 06:49:50.808691	136	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:50.945035	\N	\N	f	\N
3	CoffeeStatusBackgroundService	2026-10-07 06:49:51.472395	122	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:51.595625	\N	\N	f	\N
4	CoffeeStatusBackgroundService	2026-10-07 06:49:52.125107	121	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:52.248671	\N	\N	f	\N
5	CoffeeStatusBackgroundService	2026-10-07 06:49:52.786305	120	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:52.907628	\N	\N	f	\N
6	CoffeeStatusBackgroundService	2026-10-07 06:49:53.447534	122	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:53.577621	\N	\N	f	\N
7	CoffeeStatusBackgroundService	2026-10-07 06:49:54.112605	124	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:54.238709	\N	\N	f	\N
8	CoffeeStatusBackgroundService	2026-10-07 06:49:54.857646	249	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:55.107667	\N	\N	f	\N
9	CoffeeStatusBackgroundService	2026-10-07 06:49:55.507379	130	Không đổi trạng thái món nào	\N	admin	2026-10-07 06:49:55.641607	\N	\N	f	\N
10	CoffeeStatusBackgroundService	2026-10-07 06:54:38.346529	355	Không đổi trạng thái món nào	2026-10-07 06:59:38.710444	Service	2026-10-07 06:54:38.721734	\N	\N	f	\N
11	CoffeeStatusBackgroundService	2026-10-07 06:59:38.843657	694	Không đổi trạng thái món nào	2026-10-07 07:04:39.575417	Service	2026-10-07 06:59:39.590254	\N	\N	f	\N
12	CoffeeStatusBackgroundService	2026-10-07 07:04:39.705718	207	Không đổi trạng thái món nào	2026-10-07 07:09:39.915916	Service	2026-10-07 07:04:39.91942	\N	\N	f	\N
13	CoffeeStatusBackgroundService	2026-10-07 07:08:31.025681	653	Không đổi trạng thái món nào	2026-10-07 07:13:31.682388	Service	2026-10-07 07:08:31.694922	\N	\N	f	\N
14	CoffeeStatusBackgroundService	2026-10-07 07:18:37.548189	1290	Không đổi trạng thái món nào	2026-10-07 07:23:38.849116	Service	2026-10-07 07:18:38.870546	\N	\N	f	\N
15	CoffeeStatusBackgroundService	2026-10-07 07:18:56.520682	223	Cold Brew truyền thống → SOLD_OUT	\N	admin	2026-10-07 07:18:56.74666	\N	\N	f	\N
16	CoffeeStatusBackgroundService	2026-10-07 07:18:57.78613	165	Cold Brew cam sả → SOLD_OUT	\N	admin	2026-10-07 07:18:57.953545	\N	\N	f	\N
17	CoffeeStatusBackgroundService	2026-10-07 07:19:08.979738	120	Không đổi trạng thái món nào	\N	admin	2026-10-07 07:19:09.102312	\N	\N	f	\N
18	CoffeeStatusBackgroundService	2026-10-07 07:20:58.365602	281	Không đổi trạng thái món nào	2026-10-07 07:25:58.650234	Service	2026-10-07 07:20:58.660057	\N	\N	f	\N
19	CoffeeStatusBackgroundService	2026-10-07 07:22:45.406769	299	Cold Brew cam sả → AVAILABLE, Cold Brew truyền thống → AVAILABLE	\N	admin	2026-10-07 07:22:45.713467	\N	\N	f	\N
20	CoffeeStatusBackgroundService	2026-10-07 07:25:58.830905	450	Không đổi trạng thái món nào	2026-10-07 07:30:59.291437	Service	2026-10-07 07:25:59.294731	\N	\N	f	\N
21	CoffeeStatusBackgroundService	2026-10-07 07:30:59.399814	255	Không đổi trạng thái món nào	2026-10-07 07:35:59.659866	Service	2026-10-07 07:30:59.664732	\N	\N	f	\N
22	CoffeeStatusBackgroundService	2026-10-07 07:34:12.816214	263	Không đổi trạng thái món nào	2026-10-07 07:39:13.082966	Service	2026-10-07 07:34:13.097718	\N	\N	f	\N
23	CoffeeStatusBackgroundService	2026-10-07 07:39:13.240865	365	Không đổi trạng thái món nào	2026-10-07 07:44:13.626738	Service	2026-10-07 07:39:13.632249	\N	\N	f	\N
24	CoffeeStatusBackgroundService	2026-10-07 07:44:14.48414	299	Không đổi trạng thái món nào	2026-10-07 07:49:14.7836	Service	2026-10-07 07:44:14.796308	\N	\N	f	\N
25	CoffeeStatusBackgroundService	2026-10-07 07:48:16.410813	259	Cà phê đen đá → SOLD_OUT	\N	admin	2026-10-07 07:48:16.686591	\N	\N	f	\N
26	CoffeeStatusBackgroundService	2026-10-07 07:48:17.861526	165	Cà phê sữa đá → SOLD_OUT	\N	admin	2026-10-07 07:48:18.022006	\N	\N	f	\N
27	CoffeeStatusBackgroundService	2026-10-07 07:48:19.368417	166	Bạc xỉu → SOLD_OUT	\N	admin	2026-10-07 07:48:19.531044	\N	\N	f	\N
28	CoffeeStatusBackgroundService	2026-10-07 07:48:21.104718	232	Cà phê muối → SOLD_OUT	\N	admin	2026-10-07 07:48:21.339088	\N	\N	f	\N
29	CoffeeStatusBackgroundService	2026-10-07 07:48:22.570513	231	Cà phê trứng → SOLD_OUT	\N	admin	2026-10-07 07:48:22.801447	\N	\N	f	\N
30	CoffeeStatusBackgroundService	2026-10-07 07:48:24.316934	237	Cà phê dừa → SOLD_OUT	\N	admin	2026-10-07 07:48:24.555189	\N	\N	f	\N
31	CoffeeStatusBackgroundService	2026-10-07 07:48:26.12624	244	Cold Brew cam sả → SOLD_OUT	\N	admin	2026-10-07 07:48:26.366566	\N	\N	f	\N
32	CoffeeStatusBackgroundService	2026-10-07 07:48:59.075716	215	Không đổi trạng thái món nào	\N	admin	2026-10-07 07:48:59.295851	\N	\N	f	\N
33	CoffeeStatusBackgroundService	2026-10-07 07:48:59.717291	374	Bạc xỉu → AVAILABLE, Cà phê dừa → AVAILABLE, Cà phê muối → AVAILABLE, Cà phê sữa đá → AVAILABLE, Cà phê trứng → AVAILABLE	\N	admin	2026-10-07 07:49:00.094597	\N	\N	f	\N
34	CoffeeStatusBackgroundService	2026-10-07 07:49:00.401312	180	Không đổi trạng thái món nào	\N	admin	2026-10-07 07:49:00.592068	\N	\N	f	\N
35	CoffeeStatusBackgroundService	2026-10-07 07:49:01.064142	230	Cà phê đen đá → AVAILABLE	\N	admin	2026-10-07 07:49:01.305231	\N	\N	f	\N
36	CoffeeStatusBackgroundService	2026-10-07 07:49:14.938631	229	Không đổi trạng thái món nào	2026-10-07 07:54:15.170122	Service	2026-10-07 07:49:15.171123	\N	\N	f	\N
37	CoffeeStatusBackgroundService	2026-10-07 07:54:15.299619	283	Không đổi trạng thái món nào	2026-10-07 07:59:15.594116	Service	2026-10-07 07:54:15.596692	\N	\N	f	\N
38	CoffeeStatusBackgroundService	2026-10-07 07:59:15.700103	256	Không đổi trạng thái món nào	2026-10-07 08:04:15.95976	Service	2026-10-07 07:59:15.963706	\N	\N	f	\N
39	CoffeeStatusBackgroundService	2026-10-07 08:04:16.074064	335	Không đổi trạng thái món nào	2026-10-07 08:09:16.422983	Service	2026-10-07 08:04:16.428198	\N	\N	f	\N
40	CoffeeStatusBackgroundService	2026-10-07 08:09:16.531663	253	Không đổi trạng thái món nào	2026-10-07 08:14:16.79542	Service	2026-10-07 08:09:16.79542	\N	\N	f	\N
41	CoffeeStatusBackgroundService	2026-10-07 08:14:16.892963	247	Không đổi trạng thái món nào	2026-10-07 08:19:17.146557	Service	2026-10-07 08:14:17.146557	\N	\N	f	\N
42	CoffeeStatusBackgroundService	2026-10-07 08:19:17.23631	256	Không đổi trạng thái món nào	2026-10-07 08:24:17.49007	Service	2026-10-07 08:19:17.500233	\N	\N	f	\N
43	CoffeeStatusBackgroundService	2026-10-07 08:24:17.60902	358	Không đổi trạng thái món nào	2026-10-07 08:29:17.973148	Service	2026-10-07 08:24:17.975637	\N	\N	f	\N
44	CoffeeStatusBackgroundService	2026-10-07 08:29:18.086638	329	Không đổi trạng thái món nào	2026-10-07 08:34:18.42411	Service	2026-10-07 08:29:18.427144	\N	\N	f	\N
45	CoffeeStatusBackgroundService	2026-10-07 08:34:18.551424	451	Không đổi trạng thái món nào	2026-10-07 08:39:19.205792	Service	2026-10-07 08:34:19.211748	\N	\N	f	\N
46	CoffeeStatusBackgroundService	2026-10-07 08:39:19.358086	404	Không đổi trạng thái món nào	2026-10-07 08:44:19.767144	Service	2026-10-07 08:39:19.769141	\N	\N	f	\N
47	CoffeeStatusBackgroundService	2026-10-07 08:44:19.892214	431	Không đổi trạng thái món nào	2026-10-07 08:49:20.336065	Service	2026-10-07 08:44:20.342071	\N	\N	f	\N
48	CoffeeStatusBackgroundService	2026-10-07 08:49:20.479576	429	Không đổi trạng thái món nào	2026-10-07 08:54:20.917383	Service	2026-10-07 08:49:20.924463	\N	\N	f	\N
49	CoffeeStatusBackgroundService	2026-10-07 08:54:21.092185	620	Không đổi trạng thái món nào	2026-10-07 08:59:21.728762	Service	2026-10-07 08:54:21.737812	\N	\N	f	\N
50	CoffeeStatusBackgroundService	2026-10-07 08:54:55.754945	3000	Không đổi trạng thái món nào	2026-10-07 08:59:58.755728	Service	2026-10-07 08:54:59.059133	\N	\N	f	\N
51	CoffeeStatusBackgroundService	2026-10-07 08:59:22.071315	475	Không đổi trạng thái món nào	2026-10-07 09:04:22.579966	Service	2026-10-07 08:59:22.588149	\N	\N	f	\N
52	CoffeeStatusBackgroundService	2026-10-07 08:59:59.676174	288	Không đổi trạng thái món nào	2026-10-07 09:04:59.96545	Service	2026-10-07 08:59:59.965884	\N	\N	f	\N
53	CoffeeStatusBackgroundService	2026-10-07 09:04:22.761574	344	Không đổi trạng thái món nào	2026-10-07 09:09:23.121635	Service	2026-10-07 09:04:23.125475	\N	\N	f	\N
54	CoffeeStatusBackgroundService	2026-10-07 09:05:00.062182	297	Không đổi trạng thái món nào	2026-10-07 09:10:00.359846	Service	2026-10-07 09:05:00.36032	\N	\N	f	\N
55	CoffeeStatusBackgroundService	2026-10-07 09:09:23.245701	266	Không đổi trạng thái món nào	2026-10-07 09:14:23.529716	Service	2026-10-07 09:09:23.538232	\N	\N	f	\N
56	CoffeeStatusBackgroundService	2026-10-07 09:10:00.366162	292	Không đổi trạng thái món nào	2026-10-07 09:15:00.659311	Service	2026-10-07 09:10:00.660028	\N	\N	f	\N
57	CoffeeStatusBackgroundService	2026-10-07 09:14:23.669813	253	Không đổi trạng thái món nào	2026-10-07 09:19:23.932023	Service	2026-10-07 09:14:23.938151	\N	\N	f	\N
58	CoffeeStatusBackgroundService	2026-10-07 09:19:24.057484	251	Không đổi trạng thái món nào	2026-10-07 09:24:24.316873	Service	2026-10-07 09:19:24.324237	\N	\N	f	\N
59	CoffeeStatusBackgroundService	2026-10-07 09:24:24.430406	252	Không đổi trạng thái món nào	2026-10-07 09:29:24.69601	Service	2026-10-07 09:24:24.700537	\N	\N	f	\N
60	CoffeeStatusBackgroundService	2026-10-07 09:29:24.817295	243	Không đổi trạng thái món nào	2026-10-07 09:34:25.070019	Service	2026-10-07 09:29:25.072262	\N	\N	f	\N
61	CoffeeStatusBackgroundService	2026-10-07 09:34:25.183312	253	Không đổi trạng thái món nào	2026-10-07 09:39:25.443489	Service	2026-10-07 09:34:25.443489	\N	\N	f	\N
62	CoffeeStatusBackgroundService	2026-10-07 09:39:25.559607	249	Không đổi trạng thái món nào	2026-10-07 09:44:25.814885	Service	2026-10-07 09:39:25.816875	\N	\N	f	\N
63	CoffeeStatusBackgroundService	2026-10-07 09:44:25.965025	748	Không đổi trạng thái món nào	2026-10-07 09:49:26.756258	Service	2026-10-07 09:44:26.781779	\N	\N	f	\N
64	CoffeeStatusBackgroundService	2026-10-07 09:49:27.102474	769	Không đổi trạng thái món nào	2026-10-07 09:54:27.889126	Service	2026-10-07 09:49:27.894681	\N	\N	f	\N
65	CoffeeStatusBackgroundService	2026-10-07 09:54:28.050254	521	Không đổi trạng thái món nào	2026-10-07 09:59:28.592354	Service	2026-10-07 09:54:28.60409	\N	\N	f	\N
66	CoffeeStatusBackgroundService	2026-10-07 09:59:28.769494	537	Không đổi trạng thái món nào	2026-10-07 10:04:29.328573	Service	2026-10-07 09:59:29.339043	\N	\N	f	\N
67	CoffeeStatusBackgroundService	2026-10-07 10:04:29.4939	404	Không đổi trạng thái món nào	2026-10-07 10:09:29.916959	Service	2026-10-07 10:04:29.926634	\N	\N	f	\N
68	CoffeeStatusBackgroundService	2026-10-07 10:09:30.10112	405	Không đổi trạng thái món nào	2026-10-07 10:14:30.524613	Service	2026-10-07 10:09:30.531158	\N	\N	f	\N
69	CoffeeStatusBackgroundService	2026-10-07 10:14:30.665765	371	Không đổi trạng thái món nào	2026-10-07 10:19:31.051222	Service	2026-10-07 10:14:31.054735	\N	\N	f	\N
70	CoffeeStatusBackgroundService	2026-10-07 10:19:31.189273	325	Không đổi trạng thái món nào	2026-10-07 10:24:31.523866	Service	2026-10-07 10:19:31.524875	\N	\N	f	\N
71	CoffeeStatusBackgroundService	2026-10-07 10:24:31.635487	295	Không đổi trạng thái món nào	2026-10-07 10:29:31.936817	Service	2026-10-07 10:24:31.937834	\N	\N	f	\N
72	CoffeeStatusBackgroundService	2026-10-07 10:29:32.182048	581	Không đổi trạng thái món nào	2026-10-07 10:34:32.79712	Service	2026-10-07 10:29:32.809496	\N	\N	f	\N
73	CoffeeStatusBackgroundService	2026-10-07 10:34:32.998916	352	Không đổi trạng thái món nào	2026-10-07 10:39:33.363845	Service	2026-10-07 10:34:33.369881	\N	\N	f	\N
74	CoffeeStatusBackgroundService	2026-10-07 10:39:33.512155	320	Không đổi trạng thái món nào	2026-10-07 10:44:33.837891	Service	2026-10-07 10:39:33.839824	\N	\N	f	\N
75	CoffeeStatusBackgroundService	2026-10-07 10:44:34.120529	662	Không đổi trạng thái món nào	2026-10-07 10:49:34.805088	Service	2026-10-07 10:44:34.812288	\N	\N	f	\N
76	CoffeeStatusBackgroundService	2026-10-07 10:49:35.009471	452	Không đổi trạng thái món nào	2026-10-07 10:54:35.4677	Service	2026-10-07 10:49:35.4707	\N	\N	f	\N
77	CoffeeStatusBackgroundService	2026-10-07 10:54:35.60548	317	Không đổi trạng thái món nào	2026-10-07 10:59:35.932604	Service	2026-10-07 10:54:35.93569	\N	\N	f	\N
78	CoffeeStatusBackgroundService	2026-10-07 10:59:36.066445	321	Không đổi trạng thái món nào	2026-10-07 11:04:36.398505	Service	2026-10-07 10:59:36.400981	\N	\N	f	\N
79	CoffeeStatusBackgroundService	2026-10-07 11:04:36.545917	357	Không đổi trạng thái món nào	2026-10-07 11:09:36.909456	Service	2026-10-07 11:04:36.910777	\N	\N	f	\N
80	CoffeeStatusBackgroundService	2026-10-07 11:09:37.088471	480	Không đổi trạng thái món nào	2026-10-07 11:14:37.591829	Service	2026-10-07 11:09:37.602568	\N	\N	f	\N
81	CoffeeStatusBackgroundService	2026-10-07 11:14:37.791973	537	Không đổi trạng thái món nào	2026-10-07 11:19:38.341383	Service	2026-10-07 11:14:38.344384	\N	\N	f	\N
82	CoffeeStatusBackgroundService	2026-10-07 11:19:38.497689	411	Không đổi trạng thái món nào	2026-10-07 11:24:38.930144	Service	2026-10-07 11:19:38.936628	\N	\N	f	\N
83	CoffeeStatusBackgroundService	2026-10-07 11:24:39.14963	465	Không đổi trạng thái món nào	2026-10-07 11:29:39.628974	Service	2026-10-07 11:24:39.637096	\N	\N	f	\N
84	CoffeeStatusBackgroundService	2026-10-07 11:29:39.800132	349	Không đổi trạng thái món nào	2026-10-07 11:34:40.157135	Service	2026-10-07 11:29:40.159997	\N	\N	f	\N
85	CoffeeStatusBackgroundService	2026-10-07 11:34:40.337553	1198	Không đổi trạng thái món nào	2026-10-07 11:39:41.557547	Service	2026-10-07 11:34:41.580596	\N	\N	f	\N
86	CoffeeStatusBackgroundService	2026-10-07 11:39:41.879791	840	Không đổi trạng thái món nào	2026-10-07 11:44:42.754966	Service	2026-10-07 11:39:42.772692	\N	\N	f	\N
87	CoffeeStatusBackgroundService	2026-10-07 11:44:42.972067	946	Không đổi trạng thái món nào	2026-10-07 11:49:43.975779	Service	2026-10-07 11:44:43.996319	\N	\N	f	\N
88	CoffeeStatusBackgroundService	2026-10-07 11:49:44.199847	563	Không đổi trạng thái món nào	2026-10-07 11:54:44.790346	Service	2026-10-07 11:49:44.800909	\N	\N	f	\N
89	CoffeeStatusBackgroundService	2026-10-07 11:54:44.964249	747	Không đổi trạng thái món nào	2026-10-07 11:59:45.74655	Service	2026-10-07 11:54:45.761575	\N	\N	f	\N
90	CoffeeStatusBackgroundService	2026-10-07 11:57:08.010309	5092	Không đổi trạng thái món nào	2026-10-07 12:02:13.202728	Service	2026-10-07 11:57:13.210803	\N	\N	f	\N
91	CoffeeStatusBackgroundService	2026-10-07 11:59:45.944731	501	Không đổi trạng thái món nào	2026-10-07 12:04:46.475752	Service	2026-10-07 11:59:46.487791	\N	\N	f	\N
92	CoffeeStatusBackgroundService	2026-10-07 12:02:13.808097	499	Không đổi trạng thái món nào	2026-10-07 12:07:14.308122	Service	2026-10-07 12:02:14.403264	\N	\N	f	\N
93	CoffeeStatusBackgroundService	2026-10-07 12:04:46.67467	656	Không đổi trạng thái món nào	2026-10-07 12:09:47.363038	Service	2026-10-07 12:04:47.384725	\N	\N	f	\N
94	CoffeeStatusBackgroundService	2026-10-07 12:07:14.410508	299	Không đổi trạng thái món nào	2026-10-07 12:12:14.710448	Service	2026-10-07 12:07:14.710981	\N	\N	f	\N
95	CoffeeStatusBackgroundService	2026-10-07 12:09:47.603337	562	Không đổi trạng thái món nào	2026-10-07 12:14:48.204907	Service	2026-10-07 12:09:48.216539	\N	\N	f	\N
96	CoffeeStatusBackgroundService	2026-10-07 12:14:48.397017	787	Không đổi trạng thái món nào	2026-10-07 12:19:49.222567	Service	2026-10-07 12:14:49.238973	\N	\N	f	\N
97	CoffeeStatusBackgroundService	2026-10-07 12:19:49.466708	699	Không đổi trạng thái món nào	2026-10-07 12:24:50.187788	Service	2026-10-07 12:19:50.202645	\N	\N	f	\N
98	CoffeeStatusBackgroundService	2026-10-07 12:24:50.371071	575	Không đổi trạng thái món nào	2026-10-07 12:29:50.984712	Service	2026-10-07 12:24:50.993755	\N	\N	f	\N
99	CoffeeStatusBackgroundService	2026-10-07 12:29:51.166556	835	Không đổi trạng thái món nào	2026-10-07 12:34:52.070835	Service	2026-10-07 12:29:52.088239	\N	\N	f	\N
100	CoffeeStatusBackgroundService	2026-10-07 12:34:52.281041	680	Không đổi trạng thái món nào	2026-10-07 12:39:53.005632	Service	2026-10-07 12:34:53.023188	\N	\N	f	\N
101	CoffeeStatusBackgroundService	2026-10-07 12:39:53.19856	688	Không đổi trạng thái món nào	2026-10-07 12:44:53.916935	Service	2026-10-07 12:39:53.934591	\N	\N	f	\N
102	CoffeeStatusBackgroundService	2026-10-07 12:44:54.405794	1427	Không đổi trạng thái món nào	2026-10-07 12:49:55.889605	Service	2026-10-07 12:44:55.902947	\N	\N	f	\N
103	CoffeeStatusBackgroundService	2026-10-07 12:49:56.053956	459	Không đổi trạng thái món nào	2026-10-07 12:54:56.519525	Service	2026-10-07 12:49:56.535488	\N	\N	f	\N
104	CoffeeStatusBackgroundService	2026-10-07 12:54:56.671408	407	Không đổi trạng thái món nào	2026-10-07 12:59:57.091016	Service	2026-10-07 12:54:57.101752	\N	\N	f	\N
105	CoffeeStatusBackgroundService	2026-10-07 12:57:05.154854	2998	Không đổi trạng thái món nào	2026-10-07 13:02:08.154503	Service	2026-10-07 12:57:08.255818	\N	\N	f	\N
106	CoffeeStatusBackgroundService	2026-10-07 12:59:57.245447	413	Không đổi trạng thái món nào	2026-10-07 13:04:57.68174	Service	2026-10-07 12:59:57.703865	\N	\N	f	\N
107	CoffeeStatusBackgroundService	2026-10-07 13:02:09.854589	398	Không đổi trạng thái món nào	2026-10-07 13:07:10.253569	Service	2026-10-07 13:02:10.254237	\N	\N	f	\N
108	CoffeeStatusBackgroundService	2026-10-07 13:04:57.861824	446	Không đổi trạng thái món nào	2026-10-07 13:09:58.319285	Service	2026-10-07 13:04:58.332922	\N	\N	f	\N
109	CoffeeStatusBackgroundService	2026-10-07 13:07:10.261477	197	Không đổi trạng thái món nào	2026-10-07 13:12:10.45961	Service	2026-10-07 13:07:10.553346	\N	\N	f	\N
110	CoffeeStatusBackgroundService	2026-10-07 13:09:58.448762	402	Không đổi trạng thái món nào	2026-10-07 13:14:58.867164	Service	2026-10-07 13:09:58.876015	\N	\N	f	\N
111	CoffeeStatusBackgroundService	2026-10-07 13:12:10.560619	197	Không đổi trạng thái món nào	2026-10-07 13:17:10.758153	Service	2026-10-07 13:12:10.75859	\N	\N	f	\N
112	CoffeeStatusBackgroundService	2026-10-07 13:14:59.028878	325	Không đổi trạng thái món nào	2026-10-07 13:19:59.361737	Service	2026-10-07 13:14:59.361737	\N	\N	f	\N
113	CoffeeStatusBackgroundService	2026-10-07 13:17:10.863529	98	Không đổi trạng thái món nào	2026-10-07 13:22:10.962747	Service	2026-10-07 13:17:11.053502	\N	\N	f	\N
114	CoffeeStatusBackgroundService	2026-10-07 13:19:59.534898	315	Không đổi trạng thái món nào	2026-10-07 13:24:59.851145	Service	2026-10-07 13:19:59.867077	\N	\N	f	\N
115	CoffeeStatusBackgroundService	2026-10-07 13:24:59.969504	412	Không đổi trạng thái món nào	2026-10-07 13:30:00.421206	Service	2026-10-07 13:25:00.429506	\N	\N	f	\N
116	CoffeeStatusBackgroundService	2026-10-07 13:30:00.531731	348	Không đổi trạng thái món nào	2026-10-07 13:35:00.898898	Service	2026-10-07 13:30:00.898898	\N	\N	f	\N
117	CoffeeStatusBackgroundService	2026-10-07 13:35:01.013215	335	Không đổi trạng thái món nào	2026-10-07 13:40:01.35689	Service	2026-10-07 13:35:01.357486	\N	\N	f	\N
118	CoffeeStatusBackgroundService	2026-10-07 13:40:01.453237	302	Không đổi trạng thái món nào	2026-10-07 13:45:01.775928	Service	2026-10-07 13:40:01.775928	\N	\N	f	\N
119	CoffeeStatusBackgroundService	2026-10-07 13:45:01.902878	320	Không đổi trạng thái món nào	2026-10-07 13:50:02.215837	Service	2026-10-07 13:45:02.238355	\N	\N	f	\N
120	CoffeeStatusBackgroundService	2026-10-07 13:50:02.390247	376	Không đổi trạng thái món nào	2026-10-07 13:55:02.788796	Service	2026-10-07 13:50:02.805102	\N	\N	f	\N
121	CoffeeStatusBackgroundService	2026-10-07 13:55:02.959628	390	Không đổi trạng thái món nào	2026-10-07 14:00:03.36855	Service	2026-10-07 13:55:03.374138	\N	\N	f	\N
122	CoffeeStatusBackgroundService	2026-10-07 14:00:03.501046	258	Không đổi trạng thái món nào	2026-10-07 14:05:03.767419	Service	2026-10-07 14:00:03.770765	\N	\N	f	\N
123	CoffeeStatusBackgroundService	2026-10-07 14:05:03.878277	347	Không đổi trạng thái món nào	2026-10-07 14:10:04.24918	Service	2026-10-07 14:05:04.24918	\N	\N	f	\N
124	CoffeeStatusBackgroundService	2026-10-07 14:10:04.396698	348	Không đổi trạng thái món nào	2026-10-07 14:15:04.757919	Service	2026-10-07 14:10:04.775824	\N	\N	f	\N
125	CoffeeStatusBackgroundService	2026-10-07 14:15:04.891923	346	Không đổi trạng thái món nào	2026-10-07 14:20:05.24727	Service	2026-10-07 14:15:05.250231	\N	\N	f	\N
126	CoffeeStatusBackgroundService	2026-10-07 14:20:05.34629	261	Không đổi trạng thái món nào	2026-10-07 14:25:05.619698	Service	2026-10-07 14:20:05.630347	\N	\N	f	\N
127	CoffeeStatusBackgroundService	2026-10-07 14:25:05.741953	262	Không đổi trạng thái món nào	2026-10-07 14:30:06.023594	Service	2026-10-07 14:25:06.033437	\N	\N	f	\N
128	CoffeeStatusBackgroundService	2026-10-07 14:27:27.172499	4494	Không đổi trạng thái món nào	2026-10-07 14:32:31.667377	Service	2026-10-07 14:27:31.775176	\N	\N	f	\N
129	CoffeeStatusBackgroundService	2026-10-07 14:30:06.135702	386	Không đổi trạng thái món nào	2026-10-07 14:35:06.538166	Service	2026-10-07 14:30:06.538166	\N	\N	f	\N
130	CoffeeStatusBackgroundService	2026-10-07 14:32:32.283581	484	Không đổi trạng thái món nào	2026-10-07 14:37:32.768332	Service	2026-10-07 14:32:32.768855	\N	\N	f	\N
131	CoffeeStatusBackgroundService	2026-10-07 14:35:06.655628	360	Không đổi trạng thái món nào	2026-10-07 14:40:07.027285	Service	2026-10-07 14:35:07.029823	\N	\N	f	\N
132	CoffeeStatusBackgroundService	2026-10-07 14:37:32.775972	395	Không đổi trạng thái món nào	2026-10-07 14:42:33.171537	Service	2026-10-07 14:37:33.171961	\N	\N	f	\N
133	CoffeeStatusBackgroundService	2026-10-07 14:40:07.164703	351	Không đổi trạng thái món nào	2026-10-07 14:45:07.527622	Service	2026-10-07 14:40:07.527622	\N	\N	f	\N
134	CoffeeStatusBackgroundService	2026-10-07 14:45:07.654317	364	Không đổi trạng thái món nào	2026-10-07 14:50:08.024832	Service	2026-10-07 14:45:08.030849	\N	\N	f	\N
135	CoffeeStatusBackgroundService	2026-10-07 14:50:08.159705	323	Không đổi trạng thái món nào	2026-10-07 14:55:08.506248	Service	2026-10-07 14:50:08.506248	\N	\N	f	\N
136	CoffeeStatusBackgroundService	2026-10-07 14:55:08.631408	344	Không đổi trạng thái món nào	2026-10-07 15:00:08.994793	Service	2026-10-07 14:55:08.994793	\N	\N	f	\N
137	CoffeeStatusBackgroundService	2026-10-07 15:00:09.128114	267	Không đổi trạng thái món nào	2026-10-07 15:05:09.401085	Service	2026-10-07 15:00:09.402091	\N	\N	f	\N
138	CoffeeStatusBackgroundService	2026-10-07 15:05:09.527747	359	Không đổi trạng thái món nào	2026-10-07 15:10:09.893396	Service	2026-10-07 15:05:09.900723	\N	\N	f	\N
139	CoffeeStatusBackgroundService	2026-10-07 15:10:10.024416	313	Không đổi trạng thái món nào	2026-10-07 15:15:10.357334	Service	2026-10-07 15:10:10.366305	\N	\N	f	\N
140	CoffeeStatusBackgroundService	2026-10-07 15:15:10.491152	325	Không đổi trạng thái món nào	2026-10-07 15:20:10.823807	Service	2026-10-07 15:15:10.823807	\N	\N	f	\N
141	CoffeeStatusBackgroundService	2026-10-07 15:20:10.930246	298	Không đổi trạng thái món nào	2026-10-07 15:25:11.243632	Service	2026-10-07 15:20:11.25773	\N	\N	f	\N
142	CoffeeStatusBackgroundService	2026-10-07 15:25:11.385515	323	Không đổi trạng thái món nào	2026-10-07 15:30:11.744127	Service	2026-10-07 15:25:11.750743	\N	\N	f	\N
143	CoffeeStatusBackgroundService	2026-10-07 15:30:11.871753	349	Không đổi trạng thái món nào	2026-10-07 15:35:12.224939	Service	2026-10-07 15:30:12.235571	\N	\N	f	\N
144	CoffeeStatusBackgroundService	2026-10-07 15:35:12.339564	271	Không đổi trạng thái món nào	2026-10-07 15:40:12.617213	Service	2026-10-07 15:35:12.625782	\N	\N	f	\N
145	CoffeeStatusBackgroundService	2026-10-07 15:40:12.730395	278	Không đổi trạng thái món nào	2026-10-07 15:45:13.021132	Service	2026-10-07 15:40:13.030238	\N	\N	f	\N
146	CoffeeStatusBackgroundService	2026-10-07 15:45:13.13515	278	Không đổi trạng thái món nào	2026-10-07 15:50:13.416984	Service	2026-10-07 15:45:13.416984	\N	\N	f	\N
147	CoffeeStatusBackgroundService	2026-10-07 15:50:13.520452	281	Không đổi trạng thái món nào	2026-10-07 15:55:13.823442	Service	2026-10-07 15:50:13.823442	\N	\N	f	\N
148	CoffeeStatusBackgroundService	2026-10-07 15:55:13.930915	281	Không đổi trạng thái món nào	2026-10-07 16:00:14.216441	Service	2026-10-07 15:55:14.230903	\N	\N	f	\N
149	CoffeeStatusBackgroundService	2026-10-07 16:00:14.335779	245	Không đổi trạng thái món nào	2026-10-07 16:05:14.58797	Service	2026-10-07 16:00:14.600666	\N	\N	f	\N
150	CoffeeStatusBackgroundService	2026-10-07 16:05:14.694679	315	Không đổi trạng thái món nào	2026-10-07 16:10:15.015737	Service	2026-10-07 16:05:15.024282	\N	\N	f	\N
151	CoffeeStatusBackgroundService	2026-10-07 16:10:15.12339	374	Không đổi trạng thái món nào	2026-10-07 16:15:15.508553	Service	2026-10-07 16:10:15.517307	\N	\N	f	\N
152	CoffeeStatusBackgroundService	2026-10-07 16:15:15.643639	293	Không đổi trạng thái món nào	2026-10-07 16:20:15.937405	Service	2026-10-07 16:15:15.943362	\N	\N	f	\N
153	CoffeeStatusBackgroundService	2026-10-07 16:20:16.052082	347	Không đổi trạng thái món nào	2026-10-07 16:25:16.412626	Service	2026-10-07 16:20:16.415678	\N	\N	f	\N
154	CoffeeStatusBackgroundService	2026-10-07 16:25:16.532754	379	Không đổi trạng thái món nào	2026-10-07 16:30:16.927513	Service	2026-10-07 16:25:16.927513	\N	\N	f	\N
155	CoffeeStatusBackgroundService	2026-10-07 16:30:17.04287	286	Không đổi trạng thái món nào	2026-10-07 16:35:17.328468	Service	2026-10-07 16:30:17.338983	\N	\N	f	\N
156	CoffeeStatusBackgroundService	2026-10-07 16:35:17.466525	320	Không đổi trạng thái món nào	2026-10-07 16:40:17.799382	Service	2026-10-07 16:35:17.800886	\N	\N	f	\N
157	CoffeeStatusBackgroundService	2026-10-07 16:40:17.918464	371	Không đổi trạng thái món nào	2026-10-07 16:45:18.310251	Service	2026-10-07 16:40:18.320004	\N	\N	f	\N
158	CoffeeStatusBackgroundService	2026-10-07 16:45:18.445915	327	Không đổi trạng thái món nào	2026-10-07 16:50:18.775418	Service	2026-10-07 16:45:18.781052	\N	\N	f	\N
159	CoffeeStatusBackgroundService	2026-10-07 16:50:18.899901	399	Không đổi trạng thái món nào	2026-10-07 16:55:19.311395	Service	2026-10-07 16:50:19.316545	\N	\N	f	\N
160	CoffeeStatusBackgroundService	2026-10-07 16:55:19.438301	340	Không đổi trạng thái món nào	2026-10-07 17:00:19.783446	Service	2026-10-07 16:55:19.783446	\N	\N	f	\N
161	CoffeeStatusBackgroundService	2026-10-07 17:00:19.911233	306	Không đổi trạng thái món nào	2026-10-07 17:05:20.213372	Service	2026-10-07 17:00:20.229607	\N	\N	f	\N
162	CoffeeStatusBackgroundService	2026-10-07 17:05:20.357399	437	Không đổi trạng thái món nào	2026-10-07 17:10:20.803165	Service	2026-10-07 17:05:20.815677	\N	\N	f	\N
163	CoffeeStatusBackgroundService	2026-10-07 17:10:20.945893	409	Không đổi trạng thái món nào	2026-10-07 17:15:21.358947	Service	2026-10-07 17:10:21.368592	\N	\N	f	\N
164	CoffeeStatusBackgroundService	2026-10-07 17:15:21.477555	266	Không đổi trạng thái món nào	2026-10-07 17:20:21.747189	Service	2026-10-07 17:15:21.75474	\N	\N	f	\N
165	CoffeeStatusBackgroundService	2026-10-07 17:20:21.849702	297	Không đổi trạng thái món nào	2026-10-07 17:25:22.161297	Service	2026-10-07 17:20:22.161297	\N	\N	f	\N
166	CoffeeStatusBackgroundService	2026-10-07 17:25:22.291245	284	Không đổi trạng thái món nào	2026-10-07 17:30:22.569669	Service	2026-10-07 17:25:22.592179	\N	\N	f	\N
167	CoffeeStatusBackgroundService	2026-10-07 17:30:22.695954	287	Không đổi trạng thái món nào	2026-10-07 17:35:22.997665	Service	2026-10-07 17:30:23.000117	\N	\N	f	\N
168	CoffeeStatusBackgroundService	2026-10-07 17:35:23.11717	254	Không đổi trạng thái món nào	2026-10-07 17:40:23.389408	Service	2026-10-07 17:35:23.389408	\N	\N	f	\N
169	CoffeeStatusBackgroundService	2026-10-07 17:40:23.49175	283	Không đổi trạng thái món nào	2026-10-07 17:45:23.794785	Service	2026-10-07 17:40:23.796165	\N	\N	f	\N
170	CoffeeStatusBackgroundService	2026-10-07 17:45:23.904069	304	Không đổi trạng thái món nào	2026-10-07 17:50:24.211733	Service	2026-10-07 17:45:24.211733	\N	\N	f	\N
171	CoffeeStatusBackgroundService	2026-10-07 17:50:24.318692	263	Không đổi trạng thái món nào	2026-10-07 17:55:24.592876	Service	2026-10-07 17:50:24.595168	\N	\N	f	\N
172	CoffeeStatusBackgroundService	2026-10-07 17:55:24.724903	311	Không đổi trạng thái món nào	2026-10-07 18:00:25.044088	Service	2026-10-07 17:55:25.050831	\N	\N	f	\N
173	CoffeeStatusBackgroundService	2026-10-07 18:00:25.168007	238	Không đổi trạng thái món nào	2026-10-07 18:05:25.41939	Service	2026-10-07 18:00:25.41939	\N	\N	f	\N
174	CoffeeStatusBackgroundService	2026-10-07 18:05:25.537596	451	Không đổi trạng thái món nào	2026-10-07 18:10:26.002855	Service	2026-10-07 18:05:26.010186	\N	\N	f	\N
175	CoffeeStatusBackgroundService	2026-10-07 18:10:26.154005	289	Không đổi trạng thái món nào	2026-10-07 18:15:26.454177	Service	2026-10-07 18:10:26.470137	\N	\N	f	\N
176	CoffeeStatusBackgroundService	2026-10-07 18:15:26.579452	262	Không đổi trạng thái món nào	2026-10-07 18:20:26.845258	Service	2026-10-07 18:15:26.849385	\N	\N	f	\N
177	CoffeeStatusBackgroundService	2026-10-07 18:20:26.95445	298	Không đổi trạng thái món nào	2026-10-07 18:25:27.258593	Service	2026-10-07 18:20:27.258593	\N	\N	f	\N
178	CoffeeStatusBackgroundService	2026-10-07 18:25:27.355523	288	Không đổi trạng thái món nào	2026-10-07 18:30:27.688621	Service	2026-10-07 18:25:27.690628	\N	\N	f	\N
179	CoffeeStatusBackgroundService	2026-10-07 18:30:27.79843	285	Không đổi trạng thái món nào	2026-10-07 18:35:28.104397	Service	2026-10-07 18:30:28.107904	\N	\N	f	\N
180	CoffeeStatusBackgroundService	2026-10-07 18:35:28.220479	312	Không đổi trạng thái món nào	2026-10-07 18:40:28.548006	Service	2026-10-07 18:35:28.556945	\N	\N	f	\N
181	CoffeeStatusBackgroundService	2026-10-07 18:40:28.679216	365	Không đổi trạng thái món nào	2026-10-07 18:45:29.081316	Service	2026-10-07 18:40:29.082826	\N	\N	f	\N
182	CoffeeStatusBackgroundService	2026-10-07 18:45:29.207135	285	Không đổi trạng thái món nào	2026-10-07 18:50:29.49574	Service	2026-10-07 18:45:29.49574	\N	\N	f	\N
183	CoffeeStatusBackgroundService	2026-10-07 18:50:29.602687	296	Không đổi trạng thái món nào	2026-10-07 18:55:29.911662	Service	2026-10-07 18:50:29.920063	\N	\N	f	\N
184	CoffeeStatusBackgroundService	2026-10-07 18:55:30.048106	286	Không đổi trạng thái món nào	2026-10-07 19:00:30.351321	Service	2026-10-07 18:55:30.351321	\N	\N	f	\N
185	CoffeeStatusBackgroundService	2026-10-07 19:00:30.46762	295	Không đổi trạng thái món nào	2026-10-07 19:05:30.776294	Service	2026-10-07 19:00:30.780305	\N	\N	f	\N
186	CoffeeStatusBackgroundService	2026-10-07 19:05:30.897	299	Không đổi trạng thái món nào	2026-10-07 19:10:31.208741	Service	2026-10-07 19:05:31.214034	\N	\N	f	\N
187	CoffeeStatusBackgroundService	2026-10-07 19:10:31.326836	343	Không đổi trạng thái món nào	2026-10-07 19:15:31.676963	Service	2026-10-07 19:10:31.682527	\N	\N	f	\N
188	CoffeeStatusBackgroundService	2026-10-07 19:15:31.803859	302	Không đổi trạng thái món nào	2026-10-07 19:20:32.105825	Service	2026-10-07 19:15:32.129686	\N	\N	f	\N
189	CoffeeStatusBackgroundService	2026-10-07 19:20:32.266496	320	Không đổi trạng thái món nào	2026-10-07 19:25:32.594205	Service	2026-10-07 19:20:32.596768	\N	\N	f	\N
190	CoffeeStatusBackgroundService	2026-10-07 19:25:32.702498	308	Không đổi trạng thái món nào	2026-10-07 19:30:33.020478	Service	2026-10-07 19:25:33.020478	\N	\N	f	\N
191	CoffeeStatusBackgroundService	2026-10-07 19:30:33.133016	325	Không đổi trạng thái món nào	2026-10-07 19:35:33.46487	Service	2026-10-07 19:30:33.46487	\N	\N	f	\N
192	CoffeeStatusBackgroundService	2026-10-07 19:35:33.597108	241	Không đổi trạng thái món nào	2026-10-07 19:40:33.847636	Service	2026-10-07 19:35:33.847636	\N	\N	f	\N
193	CoffeeStatusBackgroundService	2026-10-07 19:40:33.950531	328	Không đổi trạng thái món nào	2026-10-07 19:45:34.297883	Service	2026-10-07 19:40:34.313025	\N	\N	f	\N
194	CoffeeStatusBackgroundService	2026-10-07 19:45:34.450116	312	Không đổi trạng thái món nào	2026-10-07 19:50:34.763249	Service	2026-10-07 19:45:34.777524	\N	\N	f	\N
195	CoffeeStatusBackgroundService	2026-10-07 19:50:34.90383	354	Không đổi trạng thái món nào	2026-10-07 19:55:35.268585	Service	2026-10-07 19:50:35.282307	\N	\N	f	\N
196	CoffeeStatusBackgroundService	2026-10-07 19:55:35.4234	284	Không đổi trạng thái món nào	2026-10-07 20:00:35.70768	Service	2026-10-07 19:55:35.718707	\N	\N	f	\N
197	CoffeeStatusBackgroundService	2026-10-07 20:00:35.83037	449	Không đổi trạng thái món nào	2026-10-07 20:05:36.292452	Service	2026-10-07 20:00:36.292452	\N	\N	f	\N
198	CoffeeStatusBackgroundService	2026-10-07 20:05:36.451234	258	Không đổi trạng thái món nào	2026-10-07 20:10:36.718864	Service	2026-10-07 20:05:36.725682	\N	\N	f	\N
199	CoffeeStatusBackgroundService	2026-10-07 20:10:36.834035	340	Không đổi trạng thái món nào	2026-10-07 20:15:37.194518	Service	2026-10-07 20:10:37.197338	\N	\N	f	\N
200	CoffeeStatusBackgroundService	2026-10-07 20:15:37.320565	342	Không đổi trạng thái món nào	2026-10-07 20:20:37.671558	Service	2026-10-07 20:15:37.6868	\N	\N	f	\N
201	CoffeeStatusBackgroundService	2026-10-07 20:20:37.820129	463	Không đổi trạng thái món nào	2026-10-07 20:25:38.315897	Service	2026-10-07 20:20:38.329301	\N	\N	f	\N
202	CoffeeStatusBackgroundService	2026-10-07 20:25:38.451816	294	Không đổi trạng thái món nào	2026-10-07 20:30:38.75468	Service	2026-10-07 20:25:38.75468	\N	\N	f	\N
203	CoffeeStatusBackgroundService	2026-10-07 20:30:38.86638	357	Không đổi trạng thái món nào	2026-10-07 20:35:39.230318	Service	2026-10-07 20:30:39.230318	\N	\N	f	\N
204	CoffeeStatusBackgroundService	2026-10-07 20:35:39.362041	259	Không đổi trạng thái món nào	2026-10-07 20:40:39.632135	Service	2026-10-07 20:35:39.641599	\N	\N	f	\N
205	CoffeeStatusBackgroundService	2026-10-07 20:40:39.755133	323	Không đổi trạng thái món nào	2026-10-07 20:45:40.087681	Service	2026-10-07 20:40:40.087681	\N	\N	f	\N
206	CoffeeStatusBackgroundService	2026-10-07 20:45:40.216282	333	Không đổi trạng thái món nào	2026-10-07 20:50:40.550618	Service	2026-10-07 20:45:40.550618	\N	\N	f	\N
207	CoffeeStatusBackgroundService	2026-10-07 20:50:40.681286	312	Không đổi trạng thái món nào	2026-10-07 20:55:41.014554	Service	2026-10-07 20:50:41.025651	\N	\N	f	\N
208	CoffeeStatusBackgroundService	2026-10-07 20:55:41.16605	356	Không đổi trạng thái món nào	2026-10-07 21:00:41.531397	Service	2026-10-07 20:55:41.549762	\N	\N	f	\N
209	CoffeeStatusBackgroundService	2026-10-07 21:00:41.732682	344	Không đổi trạng thái món nào	2026-10-07 21:05:42.101362	Service	2026-10-07 21:00:42.111358	\N	\N	f	\N
210	CoffeeStatusBackgroundService	2026-10-07 21:05:42.256069	299	Không đổi trạng thái món nào	2026-10-07 21:10:42.564045	Service	2026-10-07 21:05:42.568799	\N	\N	f	\N
211	CoffeeStatusBackgroundService	2026-10-07 21:10:42.668898	316	Không đổi trạng thái món nào	2026-10-07 21:15:43.000982	Service	2026-10-07 21:10:43.000982	\N	\N	f	\N
212	CoffeeStatusBackgroundService	2026-10-07 21:15:43.13366	278	Không đổi trạng thái món nào	2026-10-07 21:20:43.414412	Service	2026-10-07 21:15:43.421648	\N	\N	f	\N
213	CoffeeStatusBackgroundService	2026-10-07 21:20:43.528769	264	Không đổi trạng thái món nào	2026-10-07 21:25:43.80149	Service	2026-10-07 21:20:43.809181	\N	\N	f	\N
214	CoffeeStatusBackgroundService	2026-10-07 21:25:43.903755	260	Không đổi trạng thái món nào	2026-10-07 21:30:44.171093	Service	2026-10-07 21:25:44.171093	\N	\N	f	\N
215	CoffeeStatusBackgroundService	2026-10-07 21:30:44.276546	315	Không đổi trạng thái món nào	2026-10-07 21:35:44.590498	Service	2026-10-07 21:30:44.605941	\N	\N	f	\N
216	CoffeeStatusBackgroundService	2026-10-07 21:35:44.723871	243	Không đổi trạng thái món nào	2026-10-07 21:40:44.977146	Service	2026-10-07 21:35:44.986487	\N	\N	f	\N
217	CoffeeStatusBackgroundService	2026-10-07 21:40:45.07679	292	Không đổi trạng thái món nào	2026-10-07 21:45:45.378628	Service	2026-10-07 21:40:45.378628	\N	\N	f	\N
218	CoffeeStatusBackgroundService	2026-10-07 21:45:45.478218	276	Không đổi trạng thái món nào	2026-10-07 21:50:45.747288	Service	2026-10-07 21:45:45.76894	\N	\N	f	\N
219	CoffeeStatusBackgroundService	2026-10-07 21:50:45.908726	306	Không đổi trạng thái món nào	2026-10-07 21:55:46.226157	Service	2026-10-07 21:50:46.231259	\N	\N	f	\N
220	CoffeeStatusBackgroundService	2026-10-07 21:55:46.326618	341	Không đổi trạng thái món nào	2026-10-07 22:00:46.676738	Service	2026-10-07 21:55:46.680335	\N	\N	f	\N
221	CoffeeStatusBackgroundService	2026-10-07 22:00:46.779964	285	Không đổi trạng thái món nào	2026-10-07 22:05:47.079834	Service	2026-10-07 22:00:47.08585	\N	\N	f	\N
222	CoffeeStatusBackgroundService	2026-10-07 22:05:47.187629	305	Không đổi trạng thái món nào	2026-10-07 22:10:47.501184	Service	2026-10-07 22:05:47.501184	\N	\N	f	\N
223	CoffeeStatusBackgroundService	2026-10-07 22:10:47.614114	341	Không đổi trạng thái món nào	2026-10-07 22:15:47.96027	Service	2026-10-07 22:10:47.96027	\N	\N	f	\N
224	CoffeeStatusBackgroundService	2026-10-07 22:15:48.092173	296	Không đổi trạng thái món nào	2026-10-07 22:20:48.38921	Service	2026-10-07 22:15:48.392098	\N	\N	f	\N
225	CoffeeStatusBackgroundService	2026-10-07 22:20:48.501727	329	Không đổi trạng thái món nào	2026-10-07 22:25:48.848596	Service	2026-10-07 22:20:48.848596	\N	\N	f	\N
226	CoffeeStatusBackgroundService	2026-10-07 22:25:48.976521	284	Không đổi trạng thái món nào	2026-10-07 22:30:49.276214	Service	2026-10-07 22:25:49.276214	\N	\N	f	\N
227	CoffeeStatusBackgroundService	2026-10-07 22:30:49.408284	305	Không đổi trạng thái món nào	2026-10-07 22:35:49.72672	Service	2026-10-07 22:30:49.740079	\N	\N	f	\N
228	CoffeeStatusBackgroundService	2026-10-07 22:35:49.865825	337	Không đổi trạng thái món nào	2026-10-07 22:40:50.214147	Service	2026-10-07 22:35:50.230496	\N	\N	f	\N
229	CoffeeStatusBackgroundService	2026-10-07 22:40:50.364791	312	Không đổi trạng thái món nào	2026-10-07 22:45:50.695573	Service	2026-10-07 22:40:50.703102	\N	\N	f	\N
230	CoffeeStatusBackgroundService	2026-10-07 22:45:50.820794	313	Không đổi trạng thái món nào	2026-10-07 22:50:51.134858	Service	2026-10-07 22:45:51.134858	\N	\N	f	\N
231	CoffeeStatusBackgroundService	2026-10-07 22:50:51.267083	314	Không đổi trạng thái món nào	2026-10-07 22:55:51.581474	Service	2026-10-07 22:50:51.592953	\N	\N	f	\N
232	CoffeeStatusBackgroundService	2026-10-07 22:55:51.705298	301	Không đổi trạng thái món nào	2026-10-07 23:00:52.015882	Service	2026-10-07 22:55:52.021871	\N	\N	f	\N
233	CoffeeStatusBackgroundService	2026-10-07 23:00:52.134423	311	Không đổi trạng thái món nào	2026-10-07 23:05:52.458101	Service	2026-10-07 23:00:52.462143	\N	\N	f	\N
234	CoffeeStatusBackgroundService	2026-10-07 23:05:52.56971	325	Không đổi trạng thái món nào	2026-10-07 23:10:52.904487	Service	2026-10-07 23:05:52.91523	\N	\N	f	\N
235	CoffeeStatusBackgroundService	2026-10-07 23:10:53.040289	351	Không đổi trạng thái món nào	2026-10-07 23:15:53.402385	Service	2026-10-07 23:10:53.402385	\N	\N	f	\N
236	CoffeeStatusBackgroundService	2026-10-07 23:15:53.546265	311	Không đổi trạng thái món nào	2026-10-07 23:20:53.855308	Service	2026-10-07 23:15:53.864581	\N	\N	f	\N
237	CoffeeStatusBackgroundService	2026-10-07 23:20:54.007632	385	Không đổi trạng thái món nào	2026-10-07 23:25:54.408845	Service	2026-10-07 23:20:54.418589	\N	\N	f	\N
238	CoffeeStatusBackgroundService	2026-10-07 23:25:54.533696	319	Không đổi trạng thái món nào	2026-10-07 23:30:54.864397	Service	2026-10-07 23:25:54.87699	\N	\N	f	\N
239	CoffeeStatusBackgroundService	2026-10-07 23:30:54.990901	285	Không đổi trạng thái món nào	2026-10-07 23:35:55.275315	Service	2026-10-07 23:30:55.288127	\N	\N	f	\N
240	CoffeeStatusBackgroundService	2026-10-07 23:35:55.389081	328	Không đổi trạng thái món nào	2026-10-07 23:40:55.732346	Service	2026-10-07 23:35:55.737245	\N	\N	f	\N
241	CoffeeStatusBackgroundService	2026-10-07 23:40:55.894794	360	Không đổi trạng thái món nào	2026-10-07 23:45:56.261589	Service	2026-10-07 23:40:56.261589	\N	\N	f	\N
242	CoffeeStatusBackgroundService	2026-10-07 23:45:56.391389	314	Không đổi trạng thái món nào	2026-10-07 23:50:56.711226	Service	2026-10-07 23:45:56.711226	\N	\N	f	\N
243	CoffeeStatusBackgroundService	2026-10-07 23:50:56.819231	383	Không đổi trạng thái món nào	2026-10-07 23:55:57.212499	Service	2026-10-07 23:50:57.214791	\N	\N	f	\N
244	CoffeeStatusBackgroundService	2026-10-07 23:55:57.325717	331	Không đổi trạng thái món nào	2026-10-08 00:00:57.664055	Service	2026-10-07 23:55:57.664055	\N	\N	f	\N
245	CoffeeStatusBackgroundService	2026-10-08 00:00:57.785904	324	Không đổi trạng thái món nào	2026-10-08 00:05:58.117642	Service	2026-10-08 00:00:58.133491	\N	\N	f	\N
246	CoffeeStatusBackgroundService	2026-10-08 00:05:58.264536	282	Không đổi trạng thái món nào	2026-10-08 00:10:58.563201	Service	2026-10-08 00:05:58.570261	\N	\N	f	\N
247	CoffeeStatusBackgroundService	2026-10-08 00:10:58.680411	341	Không đổi trạng thái món nào	2026-10-08 00:15:59.0342	Service	2026-10-08 00:10:59.043204	\N	\N	f	\N
248	CoffeeStatusBackgroundService	2026-10-08 00:15:59.144591	313	Không đổi trạng thái món nào	2026-10-08 00:20:59.461834	Service	2026-10-08 00:15:59.470125	\N	\N	f	\N
249	CoffeeStatusBackgroundService	2026-10-08 00:20:59.570406	292	Không đổi trạng thái món nào	2026-10-08 00:25:59.870609	Service	2026-10-08 00:20:59.876764	\N	\N	f	\N
250	CoffeeStatusBackgroundService	2026-10-08 00:25:59.972684	290	Không đổi trạng thái món nào	2026-10-08 00:31:00.283579	Service	2026-10-08 00:26:00.294091	\N	\N	f	\N
251	CoffeeStatusBackgroundService	2026-10-08 00:31:00.438474	203	Không đổi trạng thái món nào	2026-10-08 00:36:00.641742	Service	2026-10-08 00:31:00.643749	\N	\N	f	\N
252	CoffeeStatusBackgroundService	2026-10-08 00:36:00.729674	233	Không đổi trạng thái món nào	2026-10-08 00:41:00.974015	Service	2026-10-08 00:36:00.974015	\N	\N	f	\N
253	CoffeeStatusBackgroundService	2026-10-08 00:41:01.083723	275	Không đổi trạng thái món nào	2026-10-08 00:46:01.374664	Service	2026-10-08 00:41:01.377664	\N	\N	f	\N
254	CoffeeStatusBackgroundService	2026-10-08 00:46:01.495286	381	Không đổi trạng thái món nào	2026-10-08 00:51:01.895844	Service	2026-10-08 00:46:01.895844	\N	\N	f	\N
255	CoffeeStatusBackgroundService	2026-10-08 00:51:02.019562	316	Không đổi trạng thái món nào	2026-10-08 00:56:02.349777	Service	2026-10-08 00:51:02.360881	\N	\N	f	\N
256	CoffeeStatusBackgroundService	2026-10-08 00:56:02.472628	310	Không đổi trạng thái món nào	2026-10-08 01:01:02.80866	Service	2026-10-08 00:56:02.815306	\N	\N	f	\N
257	CoffeeStatusBackgroundService	2026-10-08 01:01:02.930906	323	Không đổi trạng thái món nào	2026-10-08 01:06:03.260695	Service	2026-10-08 01:01:03.262439	\N	\N	f	\N
258	CoffeeStatusBackgroundService	2026-10-08 01:06:03.382556	319	Không đổi trạng thái món nào	2026-10-08 01:11:03.709948	Service	2026-10-08 01:06:03.714486	\N	\N	f	\N
259	CoffeeStatusBackgroundService	2026-10-08 01:11:03.82172	346	Không đổi trạng thái món nào	2026-10-08 01:16:04.182288	Service	2026-10-08 01:11:04.185882	\N	\N	f	\N
260	CoffeeStatusBackgroundService	2026-10-08 01:16:04.294573	407	Không đổi trạng thái món nào	2026-10-08 01:21:04.704677	Service	2026-10-08 01:16:04.706678	\N	\N	f	\N
261	CoffeeStatusBackgroundService	2026-10-08 01:19:02.569146	5698	Không đổi trạng thái món nào	2026-10-08 01:24:08.268541	Service	2026-10-08 01:19:08.365739	\N	\N	f	\N
262	CoffeeStatusBackgroundService	2026-10-08 01:21:04.806872	343	Không đổi trạng thái món nào	2026-10-08 01:26:05.170767	Service	2026-10-08 01:21:05.179756	\N	\N	f	\N
263	CoffeeStatusBackgroundService	2026-10-08 01:24:09.165012	503	Không đổi trạng thái món nào	2026-10-08 01:29:09.668453	Service	2026-10-08 01:24:09.668945	\N	\N	f	\N
264	CoffeeStatusBackgroundService	2026-10-08 01:26:05.292623	324	Không đổi trạng thái món nào	2026-10-08 01:31:05.630889	Service	2026-10-08 01:26:05.646575	\N	\N	f	\N
265	CoffeeStatusBackgroundService	2026-10-08 01:29:09.683267	385	Không đổi trạng thái món nào	2026-10-08 01:34:10.068823	Service	2026-10-08 01:29:10.069273	\N	\N	f	\N
266	CoffeeStatusBackgroundService	2026-10-08 01:31:05.776082	374	Không đổi trạng thái món nào	2026-10-08 01:36:06.151777	Service	2026-10-08 01:31:06.167446	\N	\N	f	\N
267	CoffeeStatusBackgroundService	2026-10-08 01:36:06.289211	339	Không đổi trạng thái món nào	2026-10-08 01:41:06.6538	Service	2026-10-08 01:36:06.6538	\N	\N	f	\N
268	CoffeeStatusBackgroundService	2026-10-08 01:41:06.843735	851	Không đổi trạng thái món nào	2026-10-08 01:46:07.74201	Service	2026-10-08 01:41:07.78674	\N	\N	f	\N
269	CoffeeStatusBackgroundService	2026-10-08 01:46:07.990452	295	Không đổi trạng thái món nào	2026-10-08 01:51:08.294196	Service	2026-10-08 01:46:08.299195	\N	\N	f	\N
270	CoffeeStatusBackgroundService	2026-10-08 01:51:08.396595	256	Không đổi trạng thái món nào	2026-10-08 01:56:08.664689	Service	2026-10-08 01:51:08.664689	\N	\N	f	\N
271	CoffeeStatusBackgroundService	2026-10-08 01:56:08.780124	277	Không đổi trạng thái món nào	2026-10-08 02:01:09.07012	Service	2026-10-08 01:56:09.072037	\N	\N	f	\N
272	CoffeeStatusBackgroundService	2026-10-08 02:01:09.171485	301	Không đổi trạng thái món nào	2026-10-08 02:06:09.490751	Service	2026-10-08 02:01:09.500685	\N	\N	f	\N
273	CoffeeStatusBackgroundService	2026-10-08 02:06:09.626182	276	Không đổi trạng thái món nào	2026-10-08 02:11:09.904374	Service	2026-10-08 02:06:09.91325	\N	\N	f	\N
274	CoffeeStatusBackgroundService	2026-10-08 02:11:10.005271	270	Không đổi trạng thái món nào	2026-10-08 02:16:10.305262	Service	2026-10-08 02:11:10.305262	\N	\N	f	\N
275	CoffeeStatusBackgroundService	2026-10-08 02:16:10.418568	293	Không đổi trạng thái món nào	2026-10-08 02:21:10.721716	Service	2026-10-08 02:16:10.73024	\N	\N	f	\N
276	CoffeeStatusBackgroundService	2026-10-08 02:21:10.838403	307	Không đổi trạng thái món nào	2026-10-08 02:26:11.159638	Service	2026-10-08 02:21:11.166109	\N	\N	f	\N
277	CoffeeStatusBackgroundService	2026-10-08 02:26:11.271038	274	Không đổi trạng thái món nào	2026-10-08 02:31:11.550974	Service	2026-10-08 02:26:11.552338	\N	\N	f	\N
278	CoffeeStatusBackgroundService	2026-10-08 02:31:11.654963	333	Không đổi trạng thái món nào	2026-10-08 02:36:11.99859	Service	2026-10-08 02:31:12.004588	\N	\N	f	\N
279	CoffeeStatusBackgroundService	2026-10-08 02:36:12.111498	282	Không đổi trạng thái món nào	2026-10-08 02:41:12.405842	Service	2026-10-08 02:36:12.410085	\N	\N	f	\N
280	CoffeeStatusBackgroundService	2026-10-08 02:41:12.531489	340	Không đổi trạng thái món nào	2026-10-08 02:46:12.89076	Service	2026-10-08 02:41:12.897747	\N	\N	f	\N
281	CoffeeStatusBackgroundService	2026-10-08 02:46:12.999999	329	Không đổi trạng thái món nào	2026-10-08 02:51:13.338253	Service	2026-10-08 02:46:13.348291	\N	\N	f	\N
282	CoffeeStatusBackgroundService	2026-10-08 02:51:13.474146	328	Không đổi trạng thái món nào	2026-10-08 02:56:13.815674	Service	2026-10-08 02:51:13.822678	\N	\N	f	\N
283	CoffeeStatusBackgroundService	2026-10-08 02:56:13.943757	332	Không đổi trạng thái món nào	2026-10-08 03:01:14.285803	Service	2026-10-08 02:56:14.290809	\N	\N	f	\N
284	CoffeeStatusBackgroundService	2026-10-08 03:01:14.386717	351	Không đổi trạng thái món nào	2026-10-08 03:06:14.7494	Service	2026-10-08 03:01:14.753722	\N	\N	f	\N
285	CoffeeStatusBackgroundService	2026-10-08 03:06:14.877602	288	Không đổi trạng thái món nào	2026-10-08 03:11:15.183245	Service	2026-10-08 03:06:15.200232	\N	\N	f	\N
286	CoffeeStatusBackgroundService	2026-10-08 03:11:15.314335	335	Không đổi trạng thái món nào	2026-10-08 03:16:15.658609	Service	2026-10-08 03:11:15.66608	\N	\N	f	\N
287	CoffeeStatusBackgroundService	2026-10-08 03:16:15.790223	284	Không đổi trạng thái món nào	2026-10-08 03:21:16.082395	Service	2026-10-08 03:16:16.088899	\N	\N	f	\N
288	CoffeeStatusBackgroundService	2026-10-08 03:21:16.205291	263	Không đổi trạng thái món nào	2026-10-08 03:26:16.480867	Service	2026-10-08 03:21:16.485863	\N	\N	f	\N
289	CoffeeStatusBackgroundService	2026-10-08 03:26:16.594281	297	Không đổi trạng thái món nào	2026-10-08 03:31:16.900304	Service	2026-10-08 03:26:16.903303	\N	\N	f	\N
290	CoffeeStatusBackgroundService	2026-10-08 03:31:17.010227	389	Không đổi trạng thái món nào	2026-10-08 03:36:17.413432	Service	2026-10-08 03:31:17.421788	\N	\N	f	\N
291	CoffeeStatusBackgroundService	2026-10-08 03:36:17.53054	348	Không đổi trạng thái món nào	2026-10-08 03:41:17.898305	Service	2026-10-08 03:36:17.90793	\N	\N	f	\N
292	CoffeeStatusBackgroundService	2026-10-08 03:41:18.00491	283	Không đổi trạng thái món nào	2026-10-08 03:46:18.299386	Service	2026-10-08 03:41:18.304326	\N	\N	f	\N
293	CoffeeStatusBackgroundService	2026-10-08 03:46:18.410793	285	Không đổi trạng thái món nào	2026-10-08 03:51:18.700909	Service	2026-10-08 03:46:18.706967	\N	\N	f	\N
294	CoffeeStatusBackgroundService	2026-10-08 03:51:18.819628	307	Không đổi trạng thái món nào	2026-10-08 03:56:19.141217	Service	2026-10-08 03:51:19.151729	\N	\N	f	\N
295	CoffeeStatusBackgroundService	2026-10-08 03:56:19.29601	372	Không đổi trạng thái món nào	2026-10-08 04:01:19.680608	Service	2026-10-08 03:56:19.686604	\N	\N	f	\N
296	CoffeeStatusBackgroundService	2026-10-08 04:01:19.802257	264	Không đổi trạng thái món nào	2026-10-08 04:06:20.074623	Service	2026-10-08 04:01:20.084531	\N	\N	f	\N
297	CoffeeStatusBackgroundService	2026-10-08 04:06:20.205625	391	Không đổi trạng thái món nào	2026-10-08 04:11:20.611685	Service	2026-10-08 04:06:20.6218	\N	\N	f	\N
298	CoffeeStatusBackgroundService	2026-10-08 04:11:20.746981	370	Không đổi trạng thái món nào	2026-10-08 04:16:21.137069	Service	2026-10-08 04:11:21.1483	\N	\N	f	\N
299	CoffeeStatusBackgroundService	2026-10-08 04:16:21.259124	352	Không đổi trạng thái món nào	2026-10-08 04:21:21.617023	Service	2026-10-08 04:16:21.623127	\N	\N	f	\N
300	CoffeeStatusBackgroundService	2026-10-08 04:21:21.731957	306	Không đổi trạng thái món nào	2026-10-08 04:26:22.047819	Service	2026-10-08 04:21:22.053817	\N	\N	f	\N
301	CoffeeStatusBackgroundService	2026-10-08 04:26:22.156468	328	Không đổi trạng thái món nào	2026-10-08 04:31:22.497288	Service	2026-10-08 04:26:22.506286	\N	\N	f	\N
302	CoffeeStatusBackgroundService	2026-10-08 04:31:22.611557	281	Không đổi trạng thái món nào	2026-10-08 04:36:22.901384	Service	2026-10-08 04:31:22.906411	\N	\N	f	\N
303	CoffeeStatusBackgroundService	2026-10-08 04:36:23.010055	265	Không đổi trạng thái món nào	2026-10-08 04:41:23.284609	Service	2026-10-08 04:36:23.292437	\N	\N	f	\N
304	CoffeeStatusBackgroundService	2026-10-08 04:41:23.400034	259	Không đổi trạng thái món nào	2026-10-08 04:46:23.671129	Service	2026-10-08 04:41:23.676128	\N	\N	f	\N
305	CoffeeStatusBackgroundService	2026-10-08 04:46:23.780948	245	Không đổi trạng thái món nào	2026-10-08 04:51:24.029022	Service	2026-10-08 04:46:24.031508	\N	\N	f	\N
306	CoffeeStatusBackgroundService	2026-10-08 04:51:24.126915	311	Không đổi trạng thái món nào	2026-10-08 04:56:24.446234	Service	2026-10-08 04:51:24.455893	\N	\N	f	\N
307	CoffeeStatusBackgroundService	2026-10-08 04:56:24.559655	346	Không đổi trạng thái món nào	2026-10-08 05:01:24.916771	Service	2026-10-08 04:56:24.932607	\N	\N	f	\N
308	CoffeeStatusBackgroundService	2026-10-08 05:01:25.078162	478	Không đổi trạng thái món nào	2026-10-08 05:06:25.564446	Service	2026-10-08 05:01:25.573104	\N	\N	f	\N
309	CoffeeStatusBackgroundService	2026-10-08 05:06:25.708997	289	Không đổi trạng thái món nào	2026-10-08 05:11:26.010753	Service	2026-10-08 05:06:26.01775	\N	\N	f	\N
310	CoffeeStatusBackgroundService	2026-10-08 05:11:26.119635	245	Không đổi trạng thái món nào	2026-10-08 05:16:26.368984	Service	2026-10-08 05:11:26.372672	\N	\N	f	\N
311	CoffeeStatusBackgroundService	2026-10-08 05:16:26.473412	244	Không đổi trạng thái món nào	2026-10-08 05:21:26.718501	Service	2026-10-08 05:16:26.723285	\N	\N	f	\N
312	CoffeeStatusBackgroundService	2026-10-08 05:21:26.822513	255	Không đổi trạng thái món nào	2026-10-08 05:26:27.091685	Service	2026-10-08 05:21:27.096688	\N	\N	f	\N
313	CoffeeStatusBackgroundService	2026-10-08 05:26:27.198778	296	Không đổi trạng thái món nào	2026-10-08 05:31:27.509614	Service	2026-10-08 05:26:27.516031	\N	\N	f	\N
314	CoffeeStatusBackgroundService	2026-10-08 05:31:27.632611	287	Không đổi trạng thái món nào	2026-10-08 05:36:27.925507	Service	2026-10-08 05:31:27.929507	\N	\N	f	\N
315	CoffeeStatusBackgroundService	2026-10-08 05:36:28.014558	329	Không đổi trạng thái món nào	2026-10-08 05:41:28.35387	Service	2026-10-08 05:36:28.360713	\N	\N	f	\N
316	CoffeeStatusBackgroundService	2026-10-08 05:41:28.460726	309	Không đổi trạng thái món nào	2026-10-08 05:46:28.781933	Service	2026-10-08 05:41:28.789653	\N	\N	f	\N
317	CoffeeStatusBackgroundService	2026-10-08 05:46:28.902815	265	Không đổi trạng thái món nào	2026-10-08 05:51:29.169192	Service	2026-10-08 05:46:29.17819	\N	\N	f	\N
318	CoffeeStatusBackgroundService	2026-10-08 05:51:29.285499	277	Không đổi trạng thái món nào	2026-10-08 05:56:29.575302	Service	2026-10-08 05:51:29.581651	\N	\N	f	\N
319	CoffeeStatusBackgroundService	2026-10-08 05:56:29.681185	343	Không đổi trạng thái món nào	2026-10-08 06:01:30.030211	Service	2026-10-08 05:56:30.03421	\N	\N	f	\N
320	CoffeeStatusBackgroundService	2026-10-08 06:01:30.171934	352	Không đổi trạng thái món nào	2026-10-08 06:06:30.538703	Service	2026-10-08 06:01:30.544562	\N	\N	f	\N
321	CoffeeStatusBackgroundService	2026-10-08 06:06:30.644542	295	Không đổi trạng thái món nào	2026-10-08 06:11:30.971115	Service	2026-10-08 06:06:30.987144	\N	\N	f	\N
322	CoffeeStatusBackgroundService	2026-10-08 06:11:32.215624	246	Không đổi trạng thái món nào	2026-10-08 06:16:32.471843	Service	2026-10-08 06:11:32.476841	\N	\N	f	\N
323	CoffeeStatusBackgroundService	2026-10-08 06:16:32.58272	295	Không đổi trạng thái món nào	2026-10-08 06:21:32.887256	Service	2026-10-08 06:16:32.893254	\N	\N	f	\N
324	CoffeeStatusBackgroundService	2026-10-08 06:21:32.993502	294	Không đổi trạng thái món nào	2026-10-08 06:26:33.31266	Service	2026-10-08 06:21:33.316163	\N	\N	f	\N
325	CoffeeStatusBackgroundService	2026-10-08 06:26:33.41049	354	Không đổi trạng thái món nào	2026-10-08 06:31:33.785347	Service	2026-10-08 06:26:33.797423	\N	\N	f	\N
326	CoffeeStatusBackgroundService	2026-10-08 06:31:33.923753	415	Không đổi trạng thái món nào	2026-10-08 06:36:34.349267	Service	2026-10-08 06:31:34.352801	\N	\N	f	\N
327	CoffeeStatusBackgroundService	2026-10-08 06:36:34.446152	341	Không đổi trạng thái món nào	2026-10-08 06:41:34.802055	Service	2026-10-08 06:36:34.813167	\N	\N	f	\N
328	CoffeeStatusBackgroundService	2026-10-08 06:41:34.928433	338	Không đổi trạng thái món nào	2026-10-08 06:46:35.282155	Service	2026-10-08 06:41:35.293396	\N	\N	f	\N
329	CoffeeStatusBackgroundService	2026-10-08 06:46:35.397117	298	Không đổi trạng thái món nào	2026-10-08 06:51:35.697664	Service	2026-10-08 06:46:35.702666	\N	\N	f	\N
330	CoffeeStatusBackgroundService	2026-10-08 06:51:35.812478	251	Không đổi trạng thái món nào	2026-10-08 06:56:36.069248	Service	2026-10-08 06:51:36.072249	\N	\N	f	\N
331	CoffeeStatusBackgroundService	2026-10-08 06:56:36.171191	324	Không đổi trạng thái món nào	2026-10-08 07:01:36.503271	Service	2026-10-08 06:56:36.509781	\N	\N	f	\N
332	CoffeeStatusBackgroundService	2026-10-08 07:01:36.637436	377	Không đổi trạng thái món nào	2026-10-08 07:06:37.029007	Service	2026-10-08 07:01:37.036001	\N	\N	f	\N
333	CoffeeStatusBackgroundService	2026-10-08 07:06:37.15425	325	Không đổi trạng thái món nào	2026-10-08 07:11:37.490282	Service	2026-10-08 07:06:37.500842	\N	\N	f	\N
334	CoffeeStatusBackgroundService	2026-10-08 07:11:37.615215	250	Không đổi trạng thái món nào	2026-10-08 07:16:37.87262	Service	2026-10-08 07:11:37.876046	\N	\N	f	\N
335	CoffeeStatusBackgroundService	2026-10-08 07:16:37.967931	291	Không đổi trạng thái món nào	2026-10-08 07:21:38.259423	Service	2026-10-08 07:16:38.268436	\N	\N	f	\N
336	CoffeeStatusBackgroundService	2026-10-08 07:21:38.413121	284	Không đổi trạng thái món nào	2026-10-08 07:26:38.704894	Service	2026-10-08 07:21:38.708897	\N	\N	f	\N
337	CoffeeStatusBackgroundService	2026-10-08 07:26:38.815146	307	Không đổi trạng thái món nào	2026-10-08 07:31:39.129554	Service	2026-10-08 07:26:39.134632	\N	\N	f	\N
338	CoffeeStatusBackgroundService	2026-10-08 07:31:39.25645	277	Không đổi trạng thái món nào	2026-10-08 07:36:39.540375	Service	2026-10-08 07:31:39.543473	\N	\N	f	\N
339	CoffeeStatusBackgroundService	2026-10-08 07:36:39.651309	427	Không đổi trạng thái món nào	2026-10-08 07:41:40.092486	Service	2026-10-08 07:36:40.098513	\N	\N	f	\N
340	CoffeeStatusBackgroundService	2026-10-08 07:41:40.237419	533	Không đổi trạng thái món nào	2026-10-08 07:46:40.80705	Service	2026-10-08 07:41:40.827369	\N	\N	f	\N
341	CoffeeStatusBackgroundService	2026-10-08 07:46:40.979831	402	Không đổi trạng thái món nào	2026-10-08 07:51:41.392535	Service	2026-10-08 07:46:41.401804	\N	\N	f	\N
342	CoffeeStatusBackgroundService	2026-10-08 07:50:31.188181	606	Bạc xỉu → SOLD_OUT, Cà phê dừa → SOLD_OUT, Cà phê muối → SOLD_OUT, Cà phê sữa đá → SOLD_OUT, Cà phê trứng → SOLD_OUT, Cà phê đen đá → SOLD_OUT	\N	system	2026-10-08 07:50:31.794696	\N	\N	f	\N
343	CoffeeStatusBackgroundService	2026-10-08 07:51:41.516493	322	Không đổi trạng thái món nào	2026-10-08 07:56:41.850893	Service	2026-10-08 07:51:41.854905	\N	\N	f	\N
344	CoffeeStatusBackgroundService	2026-10-08 07:56:41.958919	340	Không đổi trạng thái món nào	2026-10-08 08:01:42.311868	Service	2026-10-08 07:56:42.323279	\N	\N	f	\N
345	CoffeeStatusBackgroundService	2026-10-08 08:01:42.476481	305	Không đổi trạng thái món nào	2026-10-08 08:06:42.796713	Service	2026-10-08 08:01:42.801866	\N	\N	f	\N
346	CoffeeStatusBackgroundService	2026-10-08 08:06:42.904975	452	Không đổi trạng thái món nào	2026-10-08 08:11:43.372683	Service	2026-10-08 08:06:43.378864	\N	\N	f	\N
347	CoffeeStatusBackgroundService	2026-10-08 08:11:43.486056	380	Không đổi trạng thái món nào	2026-10-08 08:16:43.878402	Service	2026-10-08 08:11:43.880908	\N	\N	f	\N
348	CoffeeStatusBackgroundService	2026-10-08 08:16:43.989485	359	Không đổi trạng thái món nào	2026-10-08 08:21:44.351062	Service	2026-10-08 08:16:44.353575	\N	\N	f	\N
349	CoffeeStatusBackgroundService	2026-10-08 08:21:44.461104	369	Không đổi trạng thái món nào	2026-10-08 08:26:44.836028	Service	2026-10-08 08:21:44.84391	\N	\N	f	\N
350	CoffeeStatusBackgroundService	2026-10-08 08:26:44.961653	368	Không đổi trạng thái món nào	2026-10-08 08:31:45.339053	Service	2026-10-08 08:26:45.349127	\N	\N	f	\N
351	CoffeeStatusBackgroundService	2026-10-08 08:31:45.497142	812	Không đổi trạng thái món nào	2026-10-08 08:36:46.328236	Service	2026-10-08 08:31:46.338365	\N	\N	f	\N
352	CoffeeStatusBackgroundService	2026-10-08 08:36:46.460075	387	Không đổi trạng thái món nào	2026-10-08 08:41:46.864729	Service	2026-10-08 08:36:46.870364	\N	\N	f	\N
353	CoffeeStatusBackgroundService	2026-10-08 08:41:46.991528	318	Không đổi trạng thái món nào	2026-10-08 08:46:47.315207	Service	2026-10-08 08:41:47.318398	\N	\N	f	\N
354	CoffeeStatusBackgroundService	2026-10-08 08:46:47.565397	561	Không đổi trạng thái món nào	2026-10-08 08:51:48.142738	Service	2026-10-08 08:46:48.151727	\N	\N	f	\N
355	CoffeeStatusBackgroundService	2026-10-08 08:51:48.299232	499	Không đổi trạng thái món nào	2026-10-08 08:56:48.809285	Service	2026-10-08 08:51:48.816747	\N	\N	f	\N
356	CoffeeStatusBackgroundService	2026-10-08 08:56:48.9553	378	Không đổi trạng thái món nào	2026-10-08 09:01:49.343821	Service	2026-10-08 08:56:49.34883	\N	\N	f	\N
357	CoffeeStatusBackgroundService	2026-10-08 09:01:49.473716	337	Không đổi trạng thái món nào	2026-10-08 09:06:49.817469	Service	2026-10-08 09:01:49.820624	\N	\N	f	\N
358	CoffeeStatusBackgroundService	2026-10-08 09:06:49.95082	347	Không đổi trạng thái món nào	2026-10-08 09:11:50.305139	Service	2026-10-08 09:06:50.307139	\N	\N	f	\N
359	CoffeeStatusBackgroundService	2026-10-08 09:11:50.417682	290	Không đổi trạng thái món nào	2026-10-08 09:16:50.714923	Service	2026-10-08 09:11:50.720068	\N	\N	f	\N
360	CoffeeStatusBackgroundService	2026-10-08 09:16:50.840521	493	Không đổi trạng thái món nào	2026-10-08 09:21:51.339806	Service	2026-10-08 09:16:51.345376	\N	\N	f	\N
361	CoffeeStatusBackgroundService	2026-10-08 09:21:51.472314	358	Không đổi trạng thái món nào	2026-10-08 09:26:51.849823	Service	2026-10-08 09:21:51.849823	\N	\N	f	\N
362	CoffeeStatusBackgroundService	2026-10-08 09:26:52.010263	342	Không đổi trạng thái món nào	2026-10-08 09:31:52.360505	Service	2026-10-08 09:26:52.370933	\N	\N	f	\N
363	CoffeeStatusBackgroundService	2026-10-08 09:31:52.494813	317	Không đổi trạng thái món nào	2026-10-08 09:36:52.81901	Service	2026-10-08 09:31:52.822272	\N	\N	f	\N
364	CoffeeStatusBackgroundService	2026-10-08 09:36:52.945971	369	Không đổi trạng thái món nào	2026-10-08 09:41:53.323621	Service	2026-10-08 09:36:53.326634	\N	\N	f	\N
365	CoffeeStatusBackgroundService	2026-10-08 09:41:53.44268	355	Không đổi trạng thái món nào	2026-10-08 09:46:53.813913	Service	2026-10-08 09:41:53.820799	\N	\N	f	\N
366	CoffeeStatusBackgroundService	2026-10-08 09:46:53.947095	450	Không đổi trạng thái món nào	2026-10-08 09:51:54.406607	Service	2026-10-08 09:46:54.419151	\N	\N	f	\N
367	CoffeeStatusBackgroundService	2026-10-08 09:51:54.532195	304	Không đổi trạng thái món nào	2026-10-08 09:56:54.844064	Service	2026-10-08 09:51:54.849476	\N	\N	f	\N
368	CoffeeStatusBackgroundService	2026-10-08 09:56:54.945817	284	Không đổi trạng thái món nào	2026-10-08 10:01:55.235772	Service	2026-10-08 09:56:55.238771	\N	\N	f	\N
369	CoffeeStatusBackgroundService	2026-10-08 10:01:55.347398	327	Không đổi trạng thái món nào	2026-10-08 10:06:55.679307	Service	2026-10-08 10:01:55.682759	\N	\N	f	\N
370	CoffeeStatusBackgroundService	2026-10-08 10:06:55.809953	237	Không đổi trạng thái món nào	2026-10-08 10:11:56.052498	Service	2026-10-08 10:06:56.055502	\N	\N	f	\N
371	CoffeeStatusBackgroundService	2026-10-08 10:11:56.160999	358	Không đổi trạng thái món nào	2026-10-08 10:16:56.52928	Service	2026-10-08 10:11:56.536281	\N	\N	f	\N
372	CoffeeStatusBackgroundService	2026-10-08 10:16:56.652031	299	Không đổi trạng thái món nào	2026-10-08 10:21:56.956394	Service	2026-10-08 10:16:56.960394	\N	\N	f	\N
373	CoffeeStatusBackgroundService	2026-10-08 10:21:57.075648	295	Không đổi trạng thái món nào	2026-10-08 10:26:57.38026	Service	2026-10-08 10:21:57.383859	\N	\N	f	\N
374	CoffeeStatusBackgroundService	2026-10-08 10:26:57.499234	291	Không đổi trạng thái món nào	2026-10-08 10:31:57.796484	Service	2026-10-08 10:26:57.801483	\N	\N	f	\N
375	CoffeeStatusBackgroundService	2026-10-08 10:31:57.918005	344	Không đổi trạng thái món nào	2026-10-08 10:36:58.270663	Service	2026-10-08 10:31:58.27527	\N	\N	f	\N
376	CoffeeStatusBackgroundService	2026-10-08 10:36:58.386946	293	Không đổi trạng thái món nào	2026-10-08 10:41:58.688607	Service	2026-10-08 10:36:58.69312	\N	\N	f	\N
377	CoffeeStatusBackgroundService	2026-10-08 10:41:58.814943	317	Không đổi trạng thái món nào	2026-10-08 10:46:59.136953	Service	2026-10-08 10:41:59.138983	\N	\N	f	\N
378	CoffeeStatusBackgroundService	2026-10-08 10:46:59.248164	344	Không đổi trạng thái món nào	2026-10-08 10:51:59.600809	Service	2026-10-08 10:46:59.608276	\N	\N	f	\N
379	CoffeeStatusBackgroundService	2026-10-08 10:51:59.715841	267	Không đổi trạng thái món nào	2026-10-08 10:56:59.988407	Service	2026-10-08 10:51:59.992138	\N	\N	f	\N
380	CoffeeStatusBackgroundService	2026-10-08 10:57:00.11779	465	Không đổi trạng thái món nào	2026-10-08 11:02:00.592953	Service	2026-10-08 10:57:00.600937	\N	\N	f	\N
381	CoffeeStatusBackgroundService	2026-10-08 11:02:00.719458	327	Không đổi trạng thái món nào	2026-10-08 11:07:01.051785	Service	2026-10-08 11:02:01.054783	\N	\N	f	\N
382	CoffeeStatusBackgroundService	2026-10-08 11:07:01.183649	447	Không đổi trạng thái món nào	2026-10-08 11:12:01.643373	Service	2026-10-08 11:07:01.653036	\N	\N	f	\N
383	CoffeeStatusBackgroundService	2026-10-08 11:12:01.773009	440	Không đổi trạng thái món nào	2026-10-08 11:17:02.24199	Service	2026-10-08 11:12:02.25907	\N	\N	f	\N
384	CoffeeStatusBackgroundService	2026-10-08 11:17:02.401864	404	Không đổi trạng thái món nào	2026-10-08 11:22:02.821156	Service	2026-10-08 11:17:02.830973	\N	\N	f	\N
385	CoffeeStatusBackgroundService	2026-10-08 11:22:02.964479	365	Không đổi trạng thái món nào	2026-10-08 11:27:03.340187	Service	2026-10-08 11:22:03.34641	\N	\N	f	\N
386	CoffeeStatusBackgroundService	2026-10-08 11:27:03.488791	445	Không đổi trạng thái món nào	2026-10-08 11:32:03.941616	Service	2026-10-08 11:27:03.947643	\N	\N	f	\N
387	CoffeeStatusBackgroundService	2026-10-08 11:32:04.056009	330	Không đổi trạng thái món nào	2026-10-08 11:37:04.39703	Service	2026-10-08 11:32:04.402808	\N	\N	f	\N
388	CoffeeStatusBackgroundService	2026-10-08 11:37:04.537956	387	Không đổi trạng thái món nào	2026-10-08 11:42:04.94406	Service	2026-10-08 11:37:04.972566	\N	\N	f	\N
389	CoffeeStatusBackgroundService	2026-10-08 11:42:05.152414	419	Không đổi trạng thái món nào	2026-10-08 11:47:05.581776	Service	2026-10-08 11:42:05.583775	\N	\N	f	\N
390	CoffeeStatusBackgroundService	2026-10-08 11:47:05.708008	734	Không đổi trạng thái món nào	2026-10-08 11:52:06.46712	Service	2026-10-08 11:47:06.476798	\N	\N	f	\N
391	CoffeeStatusBackgroundService	2026-10-08 11:52:06.62613	355	Không đổi trạng thái món nào	2026-10-08 11:57:06.994769	Service	2026-10-08 11:52:07.001903	\N	\N	f	\N
392	CoffeeStatusBackgroundService	2026-10-08 11:57:07.13899	369	Không đổi trạng thái món nào	2026-10-08 12:02:07.521667	Service	2026-10-08 11:57:07.532653	\N	\N	f	\N
393	CoffeeStatusBackgroundService	2026-10-08 12:02:07.688443	481	Không đổi trạng thái món nào	2026-10-08 12:07:08.176906	Service	2026-10-08 12:02:08.181387	\N	\N	f	\N
394	CoffeeStatusBackgroundService	2026-10-08 12:07:08.293973	299	Không đổi trạng thái món nào	2026-10-08 12:12:08.608138	Service	2026-10-08 12:07:08.614443	\N	\N	f	\N
395	CoffeeStatusBackgroundService	2026-10-08 12:12:08.744186	459	Không đổi trạng thái món nào	2026-10-08 12:17:09.211102	Service	2026-10-08 12:12:09.214101	\N	\N	f	\N
396	CoffeeStatusBackgroundService	2026-10-08 12:17:09.324885	425	Không đổi trạng thái món nào	2026-10-08 12:22:09.76227	Service	2026-10-08 12:17:09.771381	\N	\N	f	\N
397	CoffeeStatusBackgroundService	2026-10-08 12:22:09.878012	403	Không đổi trạng thái món nào	2026-10-08 12:27:10.296439	Service	2026-10-08 12:22:10.305904	\N	\N	f	\N
398	CoffeeStatusBackgroundService	2026-10-08 12:27:10.422535	327	Không đổi trạng thái món nào	2026-10-08 12:32:10.755772	Service	2026-10-08 12:27:10.76177	\N	\N	f	\N
399	CoffeeStatusBackgroundService	2026-10-08 12:32:10.874662	411	Không đổi trạng thái món nào	2026-10-08 12:37:11.295454	Service	2026-10-08 12:32:11.301532	\N	\N	f	\N
400	CoffeeStatusBackgroundService	2026-10-08 12:37:11.430723	418	Không đổi trạng thái món nào	2026-10-08 12:42:11.859774	Service	2026-10-08 12:37:11.866293	\N	\N	f	\N
401	CoffeeStatusBackgroundService	2026-10-08 12:42:11.986204	375	Không đổi trạng thái món nào	2026-10-08 12:47:12.370344	Service	2026-10-08 12:42:12.377798	\N	\N	f	\N
402	CoffeeStatusBackgroundService	2026-10-08 12:47:12.500949	437	Không đổi trạng thái món nào	2026-10-08 12:52:12.951034	Service	2026-10-08 12:47:12.952077	\N	\N	f	\N
403	CoffeeStatusBackgroundService	2026-10-08 12:52:13.063944	592	Không đổi trạng thái món nào	2026-10-08 12:57:13.662851	Service	2026-10-08 12:52:13.666327	\N	\N	f	\N
404	CoffeeStatusBackgroundService	2026-10-08 12:57:13.798155	365	Không đổi trạng thái món nào	2026-10-08 13:02:14.172611	Service	2026-10-08 12:57:14.17661	\N	\N	f	\N
405	CoffeeStatusBackgroundService	2026-10-08 13:02:14.296391	377	Không đổi trạng thái món nào	2026-10-08 13:07:14.695067	Service	2026-10-08 13:02:14.695067	\N	\N	f	\N
406	CoffeeStatusBackgroundService	2026-10-08 13:07:14.870113	353	Không đổi trạng thái món nào	2026-10-08 13:12:15.23117	Service	2026-10-08 13:07:15.235263	\N	\N	f	\N
407	CoffeeStatusBackgroundService	2026-10-08 13:12:15.357969	266	Không đổi trạng thái món nào	2026-10-08 13:17:15.628836	Service	2026-10-08 13:12:15.633639	\N	\N	f	\N
408	CoffeeStatusBackgroundService	2026-10-08 13:17:15.7863	334	Không đổi trạng thái món nào	2026-10-08 13:22:16.14254	Service	2026-10-08 13:17:16.148552	\N	\N	f	\N
409	CoffeeStatusBackgroundService	2026-10-08 13:22:16.273292	295	Không đổi trạng thái món nào	2026-10-08 13:27:16.573776	Service	2026-10-08 13:22:16.573776	\N	\N	f	\N
410	CoffeeStatusBackgroundService	2026-10-08 13:27:16.693888	325	Không đổi trạng thái món nào	2026-10-08 13:32:17.07204	Service	2026-10-08 13:27:17.080218	\N	\N	f	\N
411	CoffeeStatusBackgroundService	2026-10-08 13:32:17.199327	412	Không đổi trạng thái món nào	2026-10-08 13:37:17.629771	Service	2026-10-08 13:32:17.629771	\N	\N	f	\N
412	CoffeeStatusBackgroundService	2026-10-08 13:37:17.743008	267	Không đổi trạng thái món nào	2026-10-08 13:42:18.028165	Service	2026-10-08 13:37:18.028797	\N	\N	f	\N
413	CoffeeStatusBackgroundService	2026-10-08 13:42:18.152041	237	Không đổi trạng thái món nào	2026-10-08 13:47:18.391059	Service	2026-10-08 13:42:18.391059	\N	\N	f	\N
414	CoffeeStatusBackgroundService	2026-10-08 13:47:18.517041	295	Không đổi trạng thái món nào	2026-10-08 13:52:18.816076	Service	2026-10-08 13:47:18.817721	\N	\N	f	\N
415	CoffeeStatusBackgroundService	2026-10-08 13:52:18.914017	339	Không đổi trạng thái món nào	2026-10-08 13:57:19.260054	Service	2026-10-08 13:52:19.260054	\N	\N	f	\N
416	CoffeeStatusBackgroundService	2026-10-08 13:57:19.408843	329	Không đổi trạng thái món nào	2026-10-08 14:02:19.748544	Service	2026-10-08 13:57:19.748544	\N	\N	f	\N
417	CoffeeStatusBackgroundService	2026-10-08 14:02:19.856835	252	Không đổi trạng thái món nào	2026-10-08 14:07:20.109198	Service	2026-10-08 14:02:20.13342	\N	\N	f	\N
418	CoffeeStatusBackgroundService	2026-10-08 14:07:20.251054	326	Không đổi trạng thái món nào	2026-10-08 14:12:20.58389	Service	2026-10-08 14:07:20.588604	\N	\N	f	\N
419	CoffeeStatusBackgroundService	2026-10-08 14:12:20.699351	335	Không đổi trạng thái món nào	2026-10-08 14:17:21.045633	Service	2026-10-08 14:12:21.047925	\N	\N	f	\N
420	CoffeeStatusBackgroundService	2026-10-08 14:17:21.176373	378	Không đổi trạng thái món nào	2026-10-08 14:22:21.571046	Service	2026-10-08 14:17:21.573572	\N	\N	f	\N
421	CoffeeStatusBackgroundService	2026-10-08 14:22:21.689819	332	Không đổi trạng thái món nào	2026-10-08 14:27:22.023779	Service	2026-10-08 14:22:22.03048	\N	\N	f	\N
422	CoffeeStatusBackgroundService	2026-10-08 14:27:22.154854	298	Không đổi trạng thái món nào	2026-10-08 14:32:22.458648	Service	2026-10-08 14:27:22.462663	\N	\N	f	\N
423	CoffeeStatusBackgroundService	2026-10-08 14:32:22.590529	294	Không đổi trạng thái món nào	2026-10-08 14:37:22.89124	Service	2026-10-08 14:32:22.899046	\N	\N	f	\N
424	CoffeeStatusBackgroundService	2026-10-08 14:37:23.027971	368	Không đổi trạng thái món nào	2026-10-08 14:42:23.406635	Service	2026-10-08 14:37:23.416678	\N	\N	f	\N
425	CoffeeStatusBackgroundService	2026-10-08 14:42:23.543877	396	Không đổi trạng thái món nào	2026-10-08 14:47:23.937393	Service	2026-10-08 14:42:23.951107	\N	\N	f	\N
426	CoffeeStatusBackgroundService	2026-10-08 14:47:24.084709	384	Không đổi trạng thái món nào	2026-10-08 14:52:24.480038	Service	2026-10-08 14:47:24.485211	\N	\N	f	\N
427	CoffeeStatusBackgroundService	2026-10-08 14:52:24.63547	331	Không đổi trạng thái món nào	2026-10-08 14:57:24.97202	Service	2026-10-08 14:52:24.975798	\N	\N	f	\N
428	CoffeeStatusBackgroundService	2026-10-08 14:57:25.11425	322	Không đổi trạng thái món nào	2026-10-08 15:02:25.440046	Service	2026-10-08 14:57:25.440046	\N	\N	f	\N
429	CoffeeStatusBackgroundService	2026-10-08 15:02:25.591494	416	Không đổi trạng thái món nào	2026-10-08 15:07:26.015547	Service	2026-10-08 15:02:26.015547	\N	\N	f	\N
430	CoffeeStatusBackgroundService	2026-10-08 15:07:26.141792	330	Không đổi trạng thái món nào	2026-10-08 15:12:26.480891	Service	2026-10-08 15:07:26.486492	\N	\N	f	\N
431	CoffeeStatusBackgroundService	2026-10-08 15:12:26.615094	438	Không đổi trạng thái món nào	2026-10-08 15:17:27.057376	Service	2026-10-08 15:12:27.057376	\N	\N	f	\N
432	CoffeeStatusBackgroundService	2026-10-08 15:17:27.187958	323	Không đổi trạng thái món nào	2026-10-08 15:22:27.504915	Service	2026-10-08 15:17:27.520812	\N	\N	f	\N
433	CoffeeStatusBackgroundService	2026-10-08 15:22:27.660887	425	Không đổi trạng thái món nào	2026-10-08 15:27:28.097085	Service	2026-10-08 15:22:28.112233	\N	\N	f	\N
434	CoffeeStatusBackgroundService	2026-10-08 15:27:28.262796	325	Không đổi trạng thái món nào	2026-10-08 15:32:28.59562	Service	2026-10-08 15:27:28.59562	\N	\N	f	\N
435	CoffeeStatusBackgroundService	2026-10-08 15:32:28.706559	314	Không đổi trạng thái món nào	2026-10-08 15:37:29.032422	Service	2026-10-08 15:32:29.036167	\N	\N	f	\N
436	CoffeeStatusBackgroundService	2026-10-08 15:37:29.167692	377	Không đổi trạng thái món nào	2026-10-08 15:42:29.553986	Service	2026-10-08 15:37:29.553986	\N	\N	f	\N
437	CoffeeStatusBackgroundService	2026-10-08 15:42:29.701346	420	Không đổi trạng thái món nào	2026-10-08 15:47:30.117683	Service	2026-10-08 15:42:30.129875	\N	\N	f	\N
438	CoffeeStatusBackgroundService	2026-10-08 15:47:30.258143	347	Không đổi trạng thái món nào	2026-10-08 15:52:30.610823	Service	2026-10-08 15:47:30.614986	\N	\N	f	\N
439	CoffeeStatusBackgroundService	2026-10-08 15:52:30.745296	368	Không đổi trạng thái món nào	2026-10-08 15:57:31.125279	Service	2026-10-08 15:52:31.134786	\N	\N	f	\N
440	CoffeeStatusBackgroundService	2026-10-08 15:57:31.284684	464	Không đổi trạng thái món nào	2026-10-08 16:02:31.760481	Service	2026-10-08 15:57:31.760481	\N	\N	f	\N
441	CoffeeStatusBackgroundService	2026-10-08 16:02:31.911383	403	Không đổi trạng thái món nào	2026-10-08 16:07:32.324464	Service	2026-10-08 16:02:32.327597	\N	\N	f	\N
442	CoffeeStatusBackgroundService	2026-10-08 16:07:32.438579	384	Không đổi trạng thái món nào	2026-10-08 16:12:32.834852	Service	2026-10-08 16:07:32.834852	\N	\N	f	\N
443	CoffeeStatusBackgroundService	2026-10-08 16:12:32.969816	429	Không đổi trạng thái món nào	2026-10-08 16:17:33.409999	Service	2026-10-08 16:12:33.411548	\N	\N	f	\N
444	CoffeeStatusBackgroundService	2026-10-08 16:17:33.54788	309	Không đổi trạng thái món nào	2026-10-08 16:22:33.867467	Service	2026-10-08 16:17:33.874971	\N	\N	f	\N
445	CoffeeStatusBackgroundService	2026-10-08 16:22:33.97749	303	Không đổi trạng thái món nào	2026-10-08 16:27:34.299529	Service	2026-10-08 16:22:34.307039	\N	\N	f	\N
446	CoffeeStatusBackgroundService	2026-10-08 16:27:34.400188	318	Không đổi trạng thái món nào	2026-10-08 16:32:34.728824	Service	2026-10-08 16:27:34.733792	\N	\N	f	\N
447	CoffeeStatusBackgroundService	2026-10-08 16:32:34.829914	244	Không đổi trạng thái món nào	2026-10-08 16:37:35.083229	Service	2026-10-08 16:32:35.083229	\N	\N	f	\N
448	CoffeeStatusBackgroundService	2026-10-08 16:37:35.180146	280	Không đổi trạng thái món nào	2026-10-08 16:42:35.464325	Service	2026-10-08 16:37:35.464325	\N	\N	f	\N
449	CoffeeStatusBackgroundService	2026-10-08 16:42:35.591591	500	Không đổi trạng thái món nào	2026-10-08 16:47:36.103311	Service	2026-10-08 16:42:36.11046	\N	\N	f	\N
450	CoffeeStatusBackgroundService	2026-10-08 16:47:36.212982	290	Không đổi trạng thái món nào	2026-10-08 16:52:36.509811	Service	2026-10-08 16:47:36.509811	\N	\N	f	\N
451	CoffeeStatusBackgroundService	2026-10-08 16:52:36.624052	310	Không đổi trạng thái món nào	2026-10-08 16:57:36.940965	Service	2026-10-08 16:52:36.953311	\N	\N	f	\N
452	CoffeeStatusBackgroundService	2026-10-08 16:57:37.09908	352	Không đổi trạng thái món nào	2026-10-08 17:02:37.452812	Service	2026-10-08 16:57:37.452812	\N	\N	f	\N
453	CoffeeStatusBackgroundService	2026-10-08 17:00:31.134413	456	Americano → SOLD_OUT, Cold Brew truyền thống → SOLD_OUT	\N	system	2026-10-08 17:00:31.590275	\N	\N	f	\N
454	CoffeeStatusBackgroundService	2026-10-08 17:02:37.607237	319	Không đổi trạng thái món nào	2026-10-08 17:07:37.937242	Service	2026-10-08 17:02:37.937242	\N	\N	f	\N
455	CoffeeStatusBackgroundService	2026-10-08 17:07:38.066998	336	Không đổi trạng thái món nào	2026-10-08 17:12:38.416315	Service	2026-10-08 17:07:38.416315	\N	\N	f	\N
456	CoffeeStatusBackgroundService	2026-10-08 17:12:38.558677	348	Không đổi trạng thái món nào	2026-10-08 17:17:38.917776	Service	2026-10-08 17:12:38.917776	\N	\N	f	\N
457	CoffeeStatusBackgroundService	2026-10-08 17:17:39.04185	316	Không đổi trạng thái món nào	2026-10-08 17:22:39.367488	Service	2026-10-08 17:17:39.367488	\N	\N	f	\N
458	CoffeeStatusBackgroundService	2026-10-08 17:22:39.478235	390	Không đổi trạng thái món nào	2026-10-08 17:27:39.878544	Service	2026-10-08 17:22:39.878544	\N	\N	f	\N
459	CoffeeStatusBackgroundService	2026-10-08 17:27:39.991443	307	Không đổi trạng thái món nào	2026-10-08 17:32:40.299221	Service	2026-10-08 17:27:40.312254	\N	\N	f	\N
460	CoffeeStatusBackgroundService	2026-10-08 17:32:40.408698	268	Không đổi trạng thái món nào	2026-10-08 17:37:40.688351	Service	2026-10-08 17:32:40.698987	\N	\N	f	\N
461	CoffeeStatusBackgroundService	2026-10-08 17:37:40.836607	338	Không đổi trạng thái món nào	2026-10-08 17:42:41.174899	Service	2026-10-08 17:37:41.174899	\N	\N	f	\N
462	CoffeeStatusBackgroundService	2026-10-08 17:42:41.28597	284	Không đổi trạng thái món nào	2026-10-08 17:47:41.568932	Service	2026-10-08 17:42:41.568932	\N	\N	f	\N
463	CoffeeStatusBackgroundService	2026-10-08 17:47:41.667507	356	Không đổi trạng thái món nào	2026-10-08 17:52:42.047498	Service	2026-10-08 17:47:42.047498	\N	\N	f	\N
464	CoffeeStatusBackgroundService	2026-10-08 17:52:42.20339	237	Không đổi trạng thái món nào	2026-10-08 17:57:42.450924	Service	2026-10-08 17:52:42.459188	\N	\N	f	\N
465	CoffeeStatusBackgroundService	2026-10-08 17:57:42.628442	368	Không đổi trạng thái món nào	2026-10-08 18:02:43.015061	Service	2026-10-08 17:57:43.030806	\N	\N	f	\N
466	CoffeeStatusBackgroundService	2026-10-08 18:02:43.160806	321	Không đổi trạng thái món nào	2026-10-08 18:07:43.488497	Service	2026-10-08 18:02:43.495916	\N	\N	f	\N
467	CoffeeStatusBackgroundService	2026-10-08 18:07:43.610109	316	Không đổi trạng thái món nào	2026-10-08 18:12:43.93858	Service	2026-10-08 18:07:43.93858	\N	\N	f	\N
468	CoffeeStatusBackgroundService	2026-10-08 18:12:44.044898	339	Không đổi trạng thái món nào	2026-10-08 18:17:44.409019	Service	2026-10-08 18:12:44.418449	\N	\N	f	\N
469	CoffeeStatusBackgroundService	2026-10-08 18:17:44.522002	290	Không đổi trạng thái món nào	2026-10-08 18:22:44.80874	Service	2026-10-08 18:17:44.80874	\N	\N	f	\N
470	CoffeeStatusBackgroundService	2026-10-08 18:22:44.95711	367	Không đổi trạng thái món nào	2026-10-08 18:27:45.331003	Service	2026-10-08 18:22:45.333891	\N	\N	f	\N
471	CoffeeStatusBackgroundService	2026-10-08 18:27:45.469614	407	Không đổi trạng thái món nào	2026-10-08 18:32:45.892155	Service	2026-10-08 18:27:45.892155	\N	\N	f	\N
472	CoffeeStatusBackgroundService	2026-10-08 18:32:46.023055	322	Không đổi trạng thái món nào	2026-10-08 18:37:46.340872	Service	2026-10-08 18:32:46.354971	\N	\N	f	\N
473	CoffeeStatusBackgroundService	2026-10-08 18:37:46.485691	319	Không đổi trạng thái món nào	2026-10-08 18:42:46.806594	Service	2026-10-08 18:37:46.806594	\N	\N	f	\N
474	CoffeeStatusBackgroundService	2026-10-08 18:42:46.927984	334	Không đổi trạng thái món nào	2026-10-08 18:47:47.265021	Service	2026-10-08 18:42:47.265021	\N	\N	f	\N
475	CoffeeStatusBackgroundService	2026-10-08 18:47:47.387637	345	Không đổi trạng thái món nào	2026-10-08 18:52:47.739726	Service	2026-10-08 18:47:47.747489	\N	\N	f	\N
476	CoffeeStatusBackgroundService	2026-10-08 18:52:47.867388	361	Không đổi trạng thái món nào	2026-10-08 18:57:48.233527	Service	2026-10-08 18:52:48.246558	\N	\N	f	\N
477	CoffeeStatusBackgroundService	2026-10-08 18:57:48.357738	268	Không đổi trạng thái món nào	2026-10-08 19:02:48.628692	Service	2026-10-08 18:57:48.628692	\N	\N	f	\N
478	CoffeeStatusBackgroundService	2026-10-08 19:02:48.766167	346	Không đổi trạng thái món nào	2026-10-08 19:07:49.124279	Service	2026-10-08 19:02:49.134941	\N	\N	f	\N
479	CoffeeStatusBackgroundService	2026-10-08 19:07:49.244901	305	Không đổi trạng thái món nào	2026-10-08 19:12:49.547424	Service	2026-10-08 19:07:49.564905	\N	\N	f	\N
480	CoffeeStatusBackgroundService	2026-10-08 19:12:49.657433	305	Không đổi trạng thái món nào	2026-10-08 19:17:49.970966	Service	2026-10-08 19:12:49.973352	\N	\N	f	\N
481	CoffeeStatusBackgroundService	2026-10-08 19:17:50.083799	392	Không đổi trạng thái món nào	2026-10-08 19:22:50.491282	Service	2026-10-08 19:17:50.498507	\N	\N	f	\N
482	CoffeeStatusBackgroundService	2026-10-08 19:22:50.621517	315	Không đổi trạng thái món nào	2026-10-08 19:27:50.941922	Service	2026-10-08 19:22:50.956602	\N	\N	f	\N
483	CoffeeStatusBackgroundService	2026-10-08 19:27:51.084977	401	Không đổi trạng thái món nào	2026-10-08 19:32:51.501038	Service	2026-10-08 19:27:51.505243	\N	\N	f	\N
484	CoffeeStatusBackgroundService	2026-10-08 19:32:51.660082	423	Không đổi trạng thái món nào	2026-10-08 19:37:52.090527	Service	2026-10-08 19:32:52.095416	\N	\N	f	\N
485	CoffeeStatusBackgroundService	2026-10-08 19:37:52.257738	354	Không đổi trạng thái món nào	2026-10-08 19:42:52.625244	Service	2026-10-08 19:37:52.626777	\N	\N	f	\N
486	CoffeeStatusBackgroundService	2026-10-08 19:42:52.749416	378	Không đổi trạng thái món nào	2026-10-08 19:47:53.148081	Service	2026-10-08 19:42:53.153089	\N	\N	f	\N
487	CoffeeStatusBackgroundService	2026-10-08 19:47:53.276151	354	Không đổi trạng thái món nào	2026-10-08 19:52:53.656463	Service	2026-10-08 19:47:53.657472	\N	\N	f	\N
488	CoffeeStatusBackgroundService	2026-10-08 19:52:53.783861	304	Không đổi trạng thái món nào	2026-10-08 19:57:54.100247	Service	2026-10-08 19:52:54.100247	\N	\N	f	\N
489	CoffeeStatusBackgroundService	2026-10-08 19:57:54.236949	303	Không đổi trạng thái món nào	2026-10-08 20:02:54.546565	Service	2026-10-08 19:57:54.568221	\N	\N	f	\N
490	CoffeeStatusBackgroundService	2026-10-08 20:02:54.678646	340	Không đổi trạng thái món nào	2026-10-08 20:07:55.031667	Service	2026-10-08 20:02:55.031667	\N	\N	f	\N
491	CoffeeStatusBackgroundService	2026-10-08 20:07:55.157903	368	Không đổi trạng thái món nào	2026-10-08 20:12:55.534985	Service	2026-10-08 20:07:55.541381	\N	\N	f	\N
492	CoffeeStatusBackgroundService	2026-10-08 20:12:55.667305	304	Không đổi trạng thái món nào	2026-10-08 20:17:55.977068	Service	2026-10-08 20:12:55.984339	\N	\N	f	\N
493	CoffeeStatusBackgroundService	2026-10-08 20:17:56.079879	297	Không đổi trạng thái món nào	2026-10-08 20:22:56.379765	Service	2026-10-08 20:17:56.381788	\N	\N	f	\N
494	CoffeeStatusBackgroundService	2026-10-08 20:22:56.505939	319	Không đổi trạng thái món nào	2026-10-08 20:27:56.822099	Service	2026-10-08 20:22:56.822099	\N	\N	f	\N
495	CoffeeStatusBackgroundService	2026-10-08 20:27:56.95684	385	Không đổi trạng thái món nào	2026-10-08 20:32:57.347394	Service	2026-10-08 20:27:57.349867	\N	\N	f	\N
496	CoffeeStatusBackgroundService	2026-10-08 20:32:57.457534	302	Không đổi trạng thái món nào	2026-10-08 20:37:57.778854	Service	2026-10-08 20:32:57.78064	\N	\N	f	\N
497	CoffeeStatusBackgroundService	2026-10-08 20:37:57.887524	350	Không đổi trạng thái món nào	2026-10-08 20:42:58.251537	Service	2026-10-08 20:37:58.251537	\N	\N	f	\N
498	CoffeeStatusBackgroundService	2026-10-08 20:42:58.364533	244	Không đổi trạng thái món nào	2026-10-08 20:47:58.614525	Service	2026-10-08 20:42:58.618269	\N	\N	f	\N
499	CoffeeStatusBackgroundService	2026-10-08 20:47:58.708451	343	Không đổi trạng thái món nào	2026-10-08 20:52:59.073213	Service	2026-10-08 20:47:59.081707	\N	\N	f	\N
500	CoffeeStatusBackgroundService	2026-10-08 20:52:59.18793	292	Không đổi trạng thái món nào	2026-10-08 20:57:59.489591	Service	2026-10-08 20:52:59.495081	\N	\N	f	\N
501	CoffeeStatusBackgroundService	2026-10-08 20:57:59.625791	374	Không đổi trạng thái món nào	2026-10-08 21:03:00.015999	Service	2026-10-08 20:58:00.023985	\N	\N	f	\N
502	CoffeeStatusBackgroundService	2026-10-08 21:03:00.154927	361	Không đổi trạng thái món nào	2026-10-08 21:08:00.552866	Service	2026-10-08 21:03:00.559392	\N	\N	f	\N
503	CoffeeStatusBackgroundService	2026-10-08 21:08:00.676216	364	Không đổi trạng thái món nào	2026-10-08 21:13:01.050552	Service	2026-10-08 21:08:01.056202	\N	\N	f	\N
504	CoffeeStatusBackgroundService	2026-10-08 21:13:01.185349	340	Không đổi trạng thái món nào	2026-10-08 21:18:01.535642	Service	2026-10-08 21:13:01.546047	\N	\N	f	\N
505	CoffeeStatusBackgroundService	2026-10-08 21:18:01.678364	254	Không đổi trạng thái món nào	2026-10-08 21:23:01.937654	Service	2026-10-08 21:18:01.945121	\N	\N	f	\N
506	CoffeeStatusBackgroundService	2026-10-08 21:23:02.042892	445	Không đổi trạng thái món nào	2026-10-08 21:28:02.51094	Service	2026-10-08 21:23:02.516703	\N	\N	f	\N
507	CoffeeStatusBackgroundService	2026-10-08 21:28:02.64024	351	Không đổi trạng thái món nào	2026-10-08 21:33:03.001284	Service	2026-10-08 21:28:03.004002	\N	\N	f	\N
508	CoffeeStatusBackgroundService	2026-10-08 21:33:03.155658	376	Không đổi trạng thái món nào	2026-10-08 21:38:03.529268	Service	2026-10-08 21:33:03.529268	\N	\N	f	\N
509	CoffeeStatusBackgroundService	2026-10-08 21:38:03.668544	359	Không đổi trạng thái món nào	2026-10-08 21:43:04.034974	Service	2026-10-08 21:38:04.034974	\N	\N	f	\N
510	CoffeeStatusBackgroundService	2026-10-08 21:43:04.150961	280	Không đổi trạng thái món nào	2026-10-08 21:48:04.441635	Service	2026-10-08 21:43:04.447363	\N	\N	f	\N
511	CoffeeStatusBackgroundService	2026-10-08 21:48:04.560639	416	Không đổi trạng thái món nào	2026-10-08 21:53:04.986771	Service	2026-10-08 21:48:04.993329	\N	\N	f	\N
512	CoffeeStatusBackgroundService	2026-10-08 21:53:05.112765	370	Không đổi trạng thái món nào	2026-10-08 21:58:05.485386	Service	2026-10-08 21:53:05.485386	\N	\N	f	\N
513	CoffeeStatusBackgroundService	2026-10-08 21:58:05.61058	385	Không đổi trạng thái món nào	2026-10-08 22:03:06.011554	Service	2026-10-08 21:58:06.016404	\N	\N	f	\N
514	CoffeeStatusBackgroundService	2026-10-08 22:03:06.141324	286	Không đổi trạng thái món nào	2026-10-08 22:08:06.442322	Service	2026-10-08 22:03:06.442322	\N	\N	f	\N
515	CoffeeStatusBackgroundService	2026-10-08 22:08:06.579943	409	Không đổi trạng thái món nào	2026-10-08 22:13:06.996383	Service	2026-10-08 22:08:07.000671	\N	\N	f	\N
516	CoffeeStatusBackgroundService	2026-10-08 22:13:07.110901	322	Không đổi trạng thái món nào	2026-10-08 22:18:07.443204	Service	2026-10-08 22:13:07.443204	\N	\N	f	\N
517	CoffeeStatusBackgroundService	2026-10-08 22:18:07.571117	338	Không đổi trạng thái món nào	2026-10-08 22:23:07.91922	Service	2026-10-08 22:18:07.935029	\N	\N	f	\N
518	CoffeeStatusBackgroundService	2026-10-08 22:23:08.063888	370	Không đổi trạng thái món nào	2026-10-08 22:28:08.454635	Service	2026-10-08 22:23:08.457734	\N	\N	f	\N
519	CoffeeStatusBackgroundService	2026-10-08 22:28:08.581554	313	Không đổi trạng thái món nào	2026-10-08 22:33:08.894496	Service	2026-10-08 22:28:08.902953	\N	\N	f	\N
520	CoffeeStatusBackgroundService	2026-10-08 22:33:09.018469	430	Không đổi trạng thái món nào	2026-10-08 22:38:09.454976	Service	2026-10-08 22:33:09.45698	\N	\N	f	\N
521	CoffeeStatusBackgroundService	2026-10-08 22:38:09.561319	253	Không đổi trạng thái món nào	2026-10-08 22:43:09.816805	Service	2026-10-08 22:38:09.816805	\N	\N	f	\N
522	CoffeeStatusBackgroundService	2026-10-08 22:43:09.911849	343	Không đổi trạng thái món nào	2026-10-08 22:48:10.27144	Service	2026-10-08 22:43:10.274595	\N	\N	f	\N
523	CoffeeStatusBackgroundService	2026-10-08 22:48:10.367147	327	Không đổi trạng thái món nào	2026-10-08 22:53:10.7039	Service	2026-10-08 22:48:10.706469	\N	\N	f	\N
524	CoffeeStatusBackgroundService	2026-10-08 22:53:10.817005	353	Không đổi trạng thái món nào	2026-10-08 22:58:11.187336	Service	2026-10-08 22:53:11.190409	\N	\N	f	\N
525	CoffeeStatusBackgroundService	2026-10-08 22:58:11.303506	266	Không đổi trạng thái món nào	2026-10-08 23:03:11.572407	Service	2026-10-08 22:58:11.589386	\N	\N	f	\N
526	CoffeeStatusBackgroundService	2026-10-08 23:03:11.717183	390	Không đổi trạng thái món nào	2026-10-08 23:08:12.110685	Service	2026-10-08 23:03:12.110685	\N	\N	f	\N
527	CoffeeStatusBackgroundService	2026-10-08 23:08:12.237508	330	Không đổi trạng thái món nào	2026-10-08 23:13:12.566132	Service	2026-10-08 23:08:12.566132	\N	\N	f	\N
528	CoffeeStatusBackgroundService	2026-10-08 23:13:12.717507	333	Không đổi trạng thái món nào	2026-10-08 23:18:13.056167	Service	2026-10-08 23:13:13.058169	\N	\N	f	\N
529	CoffeeStatusBackgroundService	2026-10-08 23:18:13.155751	199	Không đổi trạng thái món nào	2026-10-08 23:23:13.357154	Service	2026-10-08 23:18:13.357154	\N	\N	f	\N
530	CoffeeStatusBackgroundService	2026-10-08 23:23:13.508336	377	Không đổi trạng thái món nào	2026-10-08 23:28:13.905841	Service	2026-10-08 23:23:13.905841	\N	\N	f	\N
531	CoffeeStatusBackgroundService	2026-10-08 23:28:14.045183	316	Không đổi trạng thái món nào	2026-10-08 23:33:14.387398	Service	2026-10-08 23:28:14.394909	\N	\N	f	\N
532	CoffeeStatusBackgroundService	2026-10-08 23:33:14.500897	246	Không đổi trạng thái món nào	2026-10-08 23:38:14.757865	Service	2026-10-08 23:33:14.759438	\N	\N	f	\N
533	CoffeeStatusBackgroundService	2026-10-08 23:38:14.869339	294	Không đổi trạng thái món nào	2026-10-08 23:43:15.168106	Service	2026-10-08 23:38:15.168106	\N	\N	f	\N
534	CoffeeStatusBackgroundService	2026-10-08 23:43:15.274555	355	Không đổi trạng thái món nào	2026-10-08 23:48:15.645954	Service	2026-10-08 23:43:15.658819	\N	\N	f	\N
535	CoffeeStatusBackgroundService	2026-10-08 23:48:15.786875	382	Không đổi trạng thái món nào	2026-10-08 23:53:16.183241	Service	2026-10-08 23:48:16.183241	\N	\N	f	\N
536	CoffeeStatusBackgroundService	2026-10-08 23:53:16.317899	400	Không đổi trạng thái món nào	2026-10-08 23:58:16.723196	Service	2026-10-08 23:53:16.723196	\N	\N	f	\N
537	CoffeeStatusBackgroundService	2026-10-08 23:58:16.852562	380	Không đổi trạng thái món nào	2026-10-09 00:03:17.23926	Service	2026-10-08 23:58:17.243273	\N	\N	f	\N
538	CoffeeStatusBackgroundService	2026-10-09 00:03:17.372508	401	Không đổi trạng thái món nào	2026-10-09 00:08:17.780203	Service	2026-10-09 00:03:17.783756	\N	\N	f	\N
539	CoffeeStatusBackgroundService	2026-10-09 00:08:17.897007	402	Không đổi trạng thái món nào	2026-10-09 00:13:18.309221	Service	2026-10-09 00:08:18.32694	\N	\N	f	\N
540	CoffeeStatusBackgroundService	2026-10-09 00:13:18.468419	369	Không đổi trạng thái món nào	2026-10-09 00:18:18.853382	Service	2026-10-09 00:13:18.853382	\N	\N	f	\N
541	CoffeeStatusBackgroundService	2026-10-09 00:18:19.020215	406	Không đổi trạng thái món nào	2026-10-09 00:23:19.429625	Service	2026-10-09 00:18:19.429625	\N	\N	f	\N
542	CoffeeStatusBackgroundService	2026-10-09 00:23:19.548559	320	Không đổi trạng thái món nào	2026-10-09 00:28:19.87556	Service	2026-10-09 00:23:19.879895	\N	\N	f	\N
543	CoffeeStatusBackgroundService	2026-10-09 00:28:20.039132	365	Không đổi trạng thái món nào	2026-10-09 00:33:20.41751	Service	2026-10-09 00:28:20.42486	\N	\N	f	\N
544	CoffeeStatusBackgroundService	2026-10-09 00:33:20.586461	402	Không đổi trạng thái món nào	2026-10-09 00:38:20.990769	Service	2026-10-09 00:33:20.990769	\N	\N	f	\N
545	CoffeeStatusBackgroundService	2026-10-09 00:38:21.134089	327	Không đổi trạng thái món nào	2026-10-09 00:43:21.466689	Service	2026-10-09 00:38:21.477435	\N	\N	f	\N
546	CoffeeStatusBackgroundService	2026-10-09 00:43:21.612589	293	Không đổi trạng thái món nào	2026-10-09 00:48:21.91105	Service	2026-10-09 00:43:21.919336	\N	\N	f	\N
547	CoffeeStatusBackgroundService	2026-10-09 00:48:22.042528	473	Không đổi trạng thái món nào	2026-10-09 00:53:22.53194	Service	2026-10-09 00:48:22.536488	\N	\N	f	\N
548	CoffeeStatusBackgroundService	2026-10-09 00:53:22.671887	488	Không đổi trạng thái món nào	2026-10-09 00:58:23.173138	Service	2026-10-09 00:53:23.178127	\N	\N	f	\N
549	CoffeeStatusBackgroundService	2026-10-09 00:58:23.317993	475	Không đổi trạng thái món nào	2026-10-09 01:03:23.813251	Service	2026-10-09 00:58:23.819252	\N	\N	f	\N
550	CoffeeStatusBackgroundService	2026-10-09 01:03:23.939906	471	Không đổi trạng thái món nào	2026-10-09 01:08:24.42851	Service	2026-10-09 01:03:24.437218	\N	\N	f	\N
551	CoffeeStatusBackgroundService	2026-10-09 01:08:24.57806	452	Không đổi trạng thái món nào	2026-10-09 01:13:25.039507	Service	2026-10-09 01:08:25.044509	\N	\N	f	\N
552	CoffeeStatusBackgroundService	2026-10-09 01:13:25.185846	314	Không đổi trạng thái món nào	2026-10-09 01:18:25.509219	Service	2026-10-09 01:13:25.514217	\N	\N	f	\N
553	CoffeeStatusBackgroundService	2026-10-09 01:18:25.62819	338	Không đổi trạng thái món nào	2026-10-09 01:23:25.976373	Service	2026-10-09 01:18:25.982222	\N	\N	f	\N
554	CoffeeStatusBackgroundService	2026-10-09 01:23:26.11995	389	Không đổi trạng thái món nào	2026-10-09 01:28:26.514831	Service	2026-10-09 01:23:26.518333	\N	\N	f	\N
555	CoffeeStatusBackgroundService	2026-10-09 01:28:26.638218	331	Không đổi trạng thái món nào	2026-10-09 01:33:26.978034	Service	2026-10-09 01:28:26.980971	\N	\N	f	\N
556	CoffeeStatusBackgroundService	2026-10-09 01:33:27.109168	414	Không đổi trạng thái món nào	2026-10-09 01:38:27.532406	Service	2026-10-09 01:33:27.54426	\N	\N	f	\N
557	CoffeeStatusBackgroundService	2026-10-09 01:38:27.690363	484	Không đổi trạng thái món nào	2026-10-09 01:43:28.189546	Service	2026-10-09 01:38:28.189546	\N	\N	f	\N
558	CoffeeStatusBackgroundService	2026-10-09 01:43:28.343592	313	Không đổi trạng thái món nào	2026-10-09 01:48:28.663109	Service	2026-10-09 01:43:28.665278	\N	\N	f	\N
559	CoffeeStatusBackgroundService	2026-10-09 01:48:28.78538	375	Không đổi trạng thái món nào	2026-10-09 01:53:29.169438	Service	2026-10-09 01:48:29.174397	\N	\N	f	\N
560	CoffeeStatusBackgroundService	2026-10-09 01:53:29.303846	328	Không đổi trạng thái món nào	2026-10-09 01:58:29.637434	Service	2026-10-09 01:53:29.642436	\N	\N	f	\N
561	CoffeeStatusBackgroundService	2026-10-09 01:58:29.762002	481	Không đổi trạng thái món nào	2026-10-09 02:03:30.253199	Service	2026-10-09 01:58:30.261439	\N	\N	f	\N
562	CoffeeStatusBackgroundService	2026-10-09 02:03:30.403759	420	Không đổi trạng thái món nào	2026-10-09 02:08:30.836142	Service	2026-10-09 02:03:30.84534	\N	\N	f	\N
563	CoffeeStatusBackgroundService	2026-10-09 02:08:30.983855	296	Không đổi trạng thái món nào	2026-10-09 02:13:31.288586	Service	2026-10-09 02:08:31.294862	\N	\N	f	\N
564	CoffeeStatusBackgroundService	2026-10-09 02:13:31.423474	341	Không đổi trạng thái món nào	2026-10-09 02:18:31.778274	Service	2026-10-09 02:13:31.834187	\N	\N	f	\N
565	CoffeeStatusBackgroundService	2026-10-09 02:18:31.956413	327	Không đổi trạng thái món nào	2026-10-09 02:23:32.289357	Service	2026-10-09 02:18:32.289357	\N	\N	f	\N
566	CoffeeStatusBackgroundService	2026-10-09 02:23:32.422836	366	Không đổi trạng thái món nào	2026-10-09 02:28:32.822431	Service	2026-10-09 02:23:32.825608	\N	\N	f	\N
567	CoffeeStatusBackgroundService	2026-10-09 02:28:32.973663	378	Không đổi trạng thái món nào	2026-10-09 02:33:33.358576	Service	2026-10-09 02:28:33.358576	\N	\N	f	\N
568	CoffeeStatusBackgroundService	2026-10-09 02:33:33.566713	372	Không đổi trạng thái món nào	2026-10-09 02:38:33.945808	Service	2026-10-09 02:33:33.950328	\N	\N	f	\N
569	CoffeeStatusBackgroundService	2026-10-09 02:38:34.070168	366	Không đổi trạng thái món nào	2026-10-09 02:43:34.454566	Service	2026-10-09 02:38:34.465998	\N	\N	f	\N
570	CoffeeStatusBackgroundService	2026-10-09 02:43:34.576146	418	Không đổi trạng thái món nào	2026-10-09 02:48:35.008319	Service	2026-10-09 02:43:35.008319	\N	\N	f	\N
571	CoffeeStatusBackgroundService	2026-10-09 02:48:35.165594	615	Không đổi trạng thái món nào	2026-10-09 02:53:35.807878	Service	2026-10-09 02:48:35.820009	\N	\N	f	\N
572	CoffeeStatusBackgroundService	2026-10-09 02:53:35.953896	427	Không đổi trạng thái món nào	2026-10-09 02:58:36.391936	Service	2026-10-09 02:53:36.399937	\N	\N	f	\N
573	CoffeeStatusBackgroundService	2026-10-09 02:58:36.543266	340	Không đổi trạng thái món nào	2026-10-09 03:03:36.894664	Service	2026-10-09 02:58:36.897393	\N	\N	f	\N
574	CoffeeStatusBackgroundService	2026-10-09 03:03:37.007482	368	Không đổi trạng thái món nào	2026-10-09 03:08:37.38786	Service	2026-10-09 03:03:37.404946	\N	\N	f	\N
575	CoffeeStatusBackgroundService	2026-10-09 03:08:37.559818	716	Không đổi trạng thái món nào	2026-10-09 03:13:38.290997	Service	2026-10-09 03:08:38.307403	\N	\N	f	\N
576	CoffeeStatusBackgroundService	2026-10-09 03:13:38.445326	594	Không đổi trạng thái món nào	2026-10-09 03:18:39.056433	Service	2026-10-09 03:13:39.063607	\N	\N	f	\N
577	CoffeeStatusBackgroundService	2026-10-09 03:18:39.186223	382	Không đổi trạng thái món nào	2026-10-09 03:23:39.574701	Service	2026-10-09 03:18:39.581693	\N	\N	f	\N
578	CoffeeStatusBackgroundService	2026-10-09 03:23:39.705833	315	Không đổi trạng thái món nào	2026-10-09 03:28:40.029423	Service	2026-10-09 03:23:40.032229	\N	\N	f	\N
579	CoffeeStatusBackgroundService	2026-10-09 03:28:40.138794	302	Không đổi trạng thái món nào	2026-10-09 03:33:40.445579	Service	2026-10-09 03:28:40.448584	\N	\N	f	\N
580	CoffeeStatusBackgroundService	2026-10-09 03:33:40.559501	347	Không đổi trạng thái món nào	2026-10-09 03:38:40.917728	Service	2026-10-09 03:33:40.921774	\N	\N	f	\N
581	CoffeeStatusBackgroundService	2026-10-09 03:38:41.033675	512	Không đổi trạng thái món nào	2026-10-09 03:43:41.584814	Service	2026-10-09 03:38:41.595031	\N	\N	f	\N
582	CoffeeStatusBackgroundService	2026-10-09 03:43:41.736215	492	Không đổi trạng thái món nào	2026-10-09 03:48:42.237962	Service	2026-10-09 03:43:42.241977	\N	\N	f	\N
583	CoffeeStatusBackgroundService	2026-10-09 03:48:42.362553	434	Không đổi trạng thái món nào	2026-10-09 03:53:42.804159	Service	2026-10-09 03:48:42.809711	\N	\N	f	\N
584	CoffeeStatusBackgroundService	2026-10-09 03:53:42.934572	347	Không đổi trạng thái món nào	2026-10-09 03:58:43.288009	Service	2026-10-09 03:53:43.291145	\N	\N	f	\N
585	CoffeeStatusBackgroundService	2026-10-09 03:58:43.408658	350	Không đổi trạng thái món nào	2026-10-09 04:03:43.765244	Service	2026-10-09 03:58:43.768244	\N	\N	f	\N
586	CoffeeStatusBackgroundService	2026-10-09 04:03:43.923051	344	Không đổi trạng thái món nào	2026-10-09 04:08:44.270746	Service	2026-10-09 04:03:44.278299	\N	\N	f	\N
587	CoffeeStatusBackgroundService	2026-10-09 04:08:44.469863	454	Không đổi trạng thái món nào	2026-10-09 04:13:44.93501	Service	2026-10-09 04:08:44.94001	\N	\N	f	\N
588	CoffeeStatusBackgroundService	2026-10-09 04:13:45.04325	291	Không đổi trạng thái món nào	2026-10-09 04:18:45.340884	Service	2026-10-09 04:13:45.344842	\N	\N	f	\N
589	CoffeeStatusBackgroundService	2026-10-09 04:18:45.456721	347	Không đổi trạng thái món nào	2026-10-09 04:23:45.810837	Service	2026-10-09 04:18:45.814397	\N	\N	f	\N
590	CoffeeStatusBackgroundService	2026-10-09 04:23:45.931636	390	Không đổi trạng thái món nào	2026-10-09 04:28:46.336198	Service	2026-10-09 04:23:46.344279	\N	\N	f	\N
591	CoffeeStatusBackgroundService	2026-10-09 04:28:46.490506	374	Không đổi trạng thái món nào	2026-10-09 04:33:46.872274	Service	2026-10-09 04:28:46.875337	\N	\N	f	\N
592	CoffeeStatusBackgroundService	2026-10-09 04:33:46.988628	345	Không đổi trạng thái món nào	2026-10-09 04:38:47.343631	Service	2026-10-09 04:33:47.347561	\N	\N	f	\N
593	CoffeeStatusBackgroundService	2026-10-09 04:38:47.458544	345	Không đổi trạng thái món nào	2026-10-09 04:43:47.816616	Service	2026-10-09 04:38:47.823522	\N	\N	f	\N
594	CoffeeStatusBackgroundService	2026-10-09 04:43:47.955626	326	Không đổi trạng thái món nào	2026-10-09 04:48:48.287908	Service	2026-10-09 04:43:48.291271	\N	\N	f	\N
595	CoffeeStatusBackgroundService	2026-10-09 04:48:48.419985	349	Không đổi trạng thái món nào	2026-10-09 04:53:48.774571	Service	2026-10-09 04:48:48.774571	\N	\N	f	\N
596	CoffeeStatusBackgroundService	2026-10-09 04:53:48.952014	397	Không đổi trạng thái món nào	2026-10-09 04:58:49.358367	Service	2026-10-09 04:53:49.36101	\N	\N	f	\N
597	CoffeeStatusBackgroundService	2026-10-09 04:58:49.490062	330	Không đổi trạng thái món nào	2026-10-09 05:03:49.82601	Service	2026-10-09 04:58:49.82801	\N	\N	f	\N
598	CoffeeStatusBackgroundService	2026-10-09 05:03:49.961702	376	Không đổi trạng thái món nào	2026-10-09 05:08:50.346942	Service	2026-10-09 05:03:50.346942	\N	\N	f	\N
599	CoffeeStatusBackgroundService	2026-10-09 05:08:50.467625	260	Không đổi trạng thái món nào	2026-10-09 05:13:50.735306	Service	2026-10-09 05:08:50.735306	\N	\N	f	\N
600	CoffeeStatusBackgroundService	2026-10-09 05:13:50.833328	309	Không đổi trạng thái món nào	2026-10-09 05:18:51.150251	Service	2026-10-09 05:13:51.161606	\N	\N	f	\N
601	CoffeeStatusBackgroundService	2026-10-09 05:18:51.302585	340	Không đổi trạng thái món nào	2026-10-09 05:23:51.638495	Service	2026-10-09 05:18:51.653027	\N	\N	f	\N
602	CoffeeStatusBackgroundService	2026-10-09 05:23:51.774141	361	Không đổi trạng thái món nào	2026-10-09 05:28:52.148682	Service	2026-10-09 05:23:52.156101	\N	\N	f	\N
603	CoffeeStatusBackgroundService	2026-10-09 05:28:52.872991	331	Không đổi trạng thái món nào	2026-10-09 05:33:53.213893	Service	2026-10-09 05:28:53.213893	\N	\N	f	\N
604	CoffeeStatusBackgroundService	2026-10-09 05:33:53.336763	339	Không đổi trạng thái món nào	2026-10-09 05:38:53.683273	Service	2026-10-09 05:33:53.686697	\N	\N	f	\N
605	CoffeeStatusBackgroundService	2026-10-09 05:38:53.80925	339	Không đổi trạng thái món nào	2026-10-09 05:43:54.151245	Service	2026-10-09 05:38:54.155877	\N	\N	f	\N
606	CoffeeStatusBackgroundService	2026-10-09 05:43:54.279127	261	Không đổi trạng thái món nào	2026-10-09 05:48:54.5389	Service	2026-10-09 05:43:54.5389	\N	\N	f	\N
607	CoffeeStatusBackgroundService	2026-10-09 05:48:54.653776	293	Không đổi trạng thái món nào	2026-10-09 05:53:54.965655	Service	2026-10-09 05:48:54.968702	\N	\N	f	\N
608	CoffeeStatusBackgroundService	2026-10-09 05:53:55.072649	296	Không đổi trạng thái món nào	2026-10-09 05:58:55.383231	Service	2026-10-09 05:53:55.383231	\N	\N	f	\N
609	CoffeeStatusBackgroundService	2026-10-09 05:58:55.500785	299	Không đổi trạng thái món nào	2026-10-09 06:03:55.805421	Service	2026-10-09 05:58:55.813007	\N	\N	f	\N
610	CoffeeStatusBackgroundService	2026-10-09 06:03:55.932319	296	Không đổi trạng thái món nào	2026-10-09 06:08:56.237611	Service	2026-10-09 06:03:56.239616	\N	\N	f	\N
611	CoffeeStatusBackgroundService	2026-10-09 06:08:56.339351	271	Không đổi trạng thái món nào	2026-10-09 06:13:56.61191	Service	2026-10-09 06:08:56.620305	\N	\N	f	\N
612	CoffeeStatusBackgroundService	2026-10-09 06:13:57.841701	313	Không đổi trạng thái món nào	2026-10-09 06:18:58.161269	Service	2026-10-09 06:13:58.163276	\N	\N	f	\N
613	CoffeeStatusBackgroundService	2026-10-09 06:18:58.275212	302	Không đổi trạng thái món nào	2026-10-09 06:23:58.585514	Service	2026-10-09 06:18:58.587516	\N	\N	f	\N
614	CoffeeStatusBackgroundService	2026-10-09 06:23:58.709102	383	Không đổi trạng thái món nào	2026-10-09 06:28:59.099338	Service	2026-10-09 06:23:59.103348	\N	\N	f	\N
615	CoffeeStatusBackgroundService	2026-10-09 06:28:59.233393	297	Không đổi trạng thái món nào	2026-10-09 06:33:59.533286	Service	2026-10-09 06:28:59.540941	\N	\N	f	\N
616	CoffeeStatusBackgroundService	2026-10-09 06:33:59.645196	291	Không đổi trạng thái món nào	2026-10-09 06:38:59.946859	Service	2026-10-09 06:33:59.948453	\N	\N	f	\N
617	CoffeeStatusBackgroundService	2026-10-09 06:39:00.067873	326	Không đổi trạng thái món nào	2026-10-09 06:44:00.397608	Service	2026-10-09 06:39:00.402666	\N	\N	f	\N
618	CoffeeStatusBackgroundService	2026-10-09 06:44:00.522077	335	Không đổi trạng thái món nào	2026-10-09 06:49:00.85869	Service	2026-10-09 06:44:00.85869	\N	\N	f	\N
619	CoffeeStatusBackgroundService	2026-10-09 06:49:00.986324	340	Không đổi trạng thái món nào	2026-10-09 06:54:01.325303	Service	2026-10-09 06:49:01.335729	\N	\N	f	\N
620	CoffeeStatusBackgroundService	2026-10-09 06:54:01.440044	297	Không đổi trạng thái món nào	2026-10-09 06:59:01.740062	Service	2026-10-09 06:54:01.740062	\N	\N	f	\N
621	CoffeeStatusBackgroundService	2026-10-09 06:59:01.873098	336	Không đổi trạng thái món nào	2026-10-09 07:04:02.213802	Service	2026-10-09 06:59:02.215803	\N	\N	f	\N
622	CoffeeStatusBackgroundService	2026-10-09 07:04:02.364618	518	Không đổi trạng thái món nào	2026-10-09 07:09:02.896438	Service	2026-10-09 07:04:02.907151	\N	\N	f	\N
623	CoffeeStatusBackgroundService	2026-10-09 07:09:03.061423	362	Không đổi trạng thái món nào	2026-10-09 07:14:03.432259	Service	2026-10-09 07:09:03.432259	\N	\N	f	\N
624	CoffeeStatusBackgroundService	2026-10-09 07:14:03.556706	398	Không đổi trạng thái món nào	2026-10-09 07:19:03.966303	Service	2026-10-09 07:14:03.971432	\N	\N	f	\N
625	CoffeeStatusBackgroundService	2026-10-09 07:19:04.090794	430	Không đổi trạng thái món nào	2026-10-09 07:24:04.535685	Service	2026-10-09 07:19:04.535685	\N	\N	f	\N
626	CoffeeStatusBackgroundService	2026-10-09 07:24:04.667697	353	Không đổi trạng thái món nào	2026-10-09 07:29:05.030053	Service	2026-10-09 07:24:05.035469	\N	\N	f	\N
627	CoffeeStatusBackgroundService	2026-10-09 07:29:05.179156	368	Không đổi trạng thái món nào	2026-10-09 07:34:05.556278	Service	2026-10-09 07:29:05.558286	\N	\N	f	\N
628	CoffeeStatusBackgroundService	2026-10-09 07:34:05.687184	335	Không đổi trạng thái món nào	2026-10-09 07:39:06.029435	Service	2026-10-09 07:34:06.031442	\N	\N	f	\N
629	CoffeeStatusBackgroundService	2026-10-09 07:39:06.156184	428	Không đổi trạng thái món nào	2026-10-09 07:44:06.599032	Service	2026-10-09 07:39:06.607659	\N	\N	f	\N
630	CoffeeStatusBackgroundService	2026-10-09 07:44:06.746458	326	Không đổi trạng thái món nào	2026-10-09 07:49:07.078221	Service	2026-10-09 07:44:07.08174	\N	\N	f	\N
631	CoffeeStatusBackgroundService	2026-10-09 07:49:07.183534	321	Không đổi trạng thái món nào	2026-10-09 07:54:07.512912	Service	2026-10-09 07:49:07.516968	\N	\N	f	\N
632	CoffeeStatusBackgroundService	2026-10-09 07:54:07.64518	292	Không đổi trạng thái món nào	2026-10-09 07:59:07.945034	Service	2026-10-09 07:54:07.948028	\N	\N	f	\N
633	CoffeeStatusBackgroundService	2026-10-09 07:59:08.139895	426	Không đổi trạng thái món nào	2026-10-09 08:04:08.57222	Service	2026-10-09 07:59:08.574628	\N	\N	f	\N
634	CoffeeStatusBackgroundService	2026-10-09 08:04:08.691254	328	Không đổi trạng thái món nào	2026-10-09 08:09:09.025593	Service	2026-10-09 08:04:09.028724	\N	\N	f	\N
635	CoffeeStatusBackgroundService	2026-10-09 08:09:09.132493	328	Không đổi trạng thái món nào	2026-10-09 08:14:09.468103	Service	2026-10-09 08:09:09.471103	\N	\N	f	\N
636	CoffeeStatusBackgroundService	2026-10-09 08:14:09.612482	381	Không đổi trạng thái món nào	2026-10-09 08:19:10.002262	Service	2026-10-09 08:14:10.006262	\N	\N	f	\N
637	CoffeeStatusBackgroundService	2026-10-09 08:19:10.132024	338	Không đổi trạng thái món nào	2026-10-09 08:24:10.478841	Service	2026-10-09 08:19:10.484145	\N	\N	f	\N
638	CoffeeStatusBackgroundService	2026-10-09 08:24:10.615575	532	Không đổi trạng thái món nào	2026-10-09 08:29:11.160482	Service	2026-10-09 08:24:11.170457	\N	\N	f	\N
639	CoffeeStatusBackgroundService	2026-10-09 08:29:11.324905	337	Không đổi trạng thái món nào	2026-10-09 08:34:11.669599	Service	2026-10-09 08:29:11.671728	\N	\N	f	\N
640	CoffeeStatusBackgroundService	2026-10-09 08:34:11.79832	376	Không đổi trạng thái món nào	2026-10-09 08:39:12.182126	Service	2026-10-09 08:34:12.184183	\N	\N	f	\N
641	CoffeeStatusBackgroundService	2026-10-09 08:39:12.322744	382	Không đổi trạng thái món nào	2026-10-09 08:44:12.71376	Service	2026-10-09 08:39:12.718276	\N	\N	f	\N
642	CoffeeStatusBackgroundService	2026-10-09 08:44:12.846075	407	Không đổi trạng thái món nào	2026-10-09 08:49:13.26142	Service	2026-10-09 08:44:13.26142	\N	\N	f	\N
643	CoffeeStatusBackgroundService	2026-10-09 08:49:13.414263	389	Không đổi trạng thái món nào	2026-10-09 08:54:13.811529	Service	2026-10-09 08:49:13.815533	\N	\N	f	\N
644	CoffeeStatusBackgroundService	2026-10-09 08:54:13.948887	617	Không đổi trạng thái món nào	2026-10-09 08:59:14.570368	Service	2026-10-09 08:54:14.572375	\N	\N	f	\N
645	CoffeeStatusBackgroundService	2026-10-09 08:59:14.71035	395	Không đổi trạng thái món nào	2026-10-09 09:04:15.115273	Service	2026-10-09 08:59:15.127273	\N	\N	f	\N
646	CoffeeStatusBackgroundService	2026-10-09 09:04:15.319716	595	Không đổi trạng thái món nào	2026-10-09 09:09:15.925973	Service	2026-10-09 09:04:15.929973	\N	\N	f	\N
647	CoffeeStatusBackgroundService	2026-10-09 09:09:16.072366	353	Không đổi trạng thái món nào	2026-10-09 09:14:16.435852	Service	2026-10-09 09:09:16.441043	\N	\N	f	\N
648	CoffeeStatusBackgroundService	2026-10-09 09:14:16.562502	361	Không đổi trạng thái món nào	2026-10-09 09:19:16.937975	Service	2026-10-09 09:14:16.940975	\N	\N	f	\N
649	CoffeeStatusBackgroundService	2026-10-09 09:19:17.039041	297	Không đổi trạng thái món nào	2026-10-09 09:24:17.339732	Service	2026-10-09 09:19:17.343387	\N	\N	f	\N
650	CoffeeStatusBackgroundService	2026-10-09 09:24:17.469465	364	Không đổi trạng thái món nào	2026-10-09 09:29:17.842737	Service	2026-10-09 09:24:17.844868	\N	\N	f	\N
651	CoffeeStatusBackgroundService	2026-10-09 09:29:17.958571	451	Không đổi trạng thái món nào	2026-10-09 09:34:18.423933	Service	2026-10-09 09:29:18.433491	\N	\N	f	\N
652	CoffeeStatusBackgroundService	2026-10-09 09:34:18.589279	299	Không đổi trạng thái món nào	2026-10-09 09:39:18.893088	Service	2026-10-09 09:34:18.89612	\N	\N	f	\N
653	CoffeeStatusBackgroundService	2026-10-09 09:39:19.030456	464	Không đổi trạng thái món nào	2026-10-09 09:44:19.500214	Service	2026-10-09 09:39:19.503195	\N	\N	f	\N
654	CoffeeStatusBackgroundService	2026-10-09 09:44:19.630792	310	Không đổi trạng thái món nào	2026-10-09 09:49:19.956112	Service	2026-10-09 09:44:19.958594	\N	\N	f	\N
655	CoffeeStatusBackgroundService	2026-10-09 09:49:20.097166	496	Không đổi trạng thái món nào	2026-10-09 09:54:20.605832	Service	2026-10-09 09:49:20.612338	\N	\N	f	\N
656	CoffeeStatusBackgroundService	2026-10-09 09:54:20.746221	454	Không đổi trạng thái món nào	2026-10-09 09:59:21.205503	Service	2026-10-09 09:54:21.212506	\N	\N	f	\N
657	CoffeeStatusBackgroundService	2026-10-09 09:59:21.38393	519	Không đổi trạng thái món nào	2026-10-09 10:04:21.918194	Service	2026-10-09 09:59:21.928081	\N	\N	f	\N
658	CoffeeStatusBackgroundService	2026-10-09 10:04:22.067987	476	Không đổi trạng thái món nào	2026-10-09 10:09:22.553522	Service	2026-10-09 10:04:22.558518	\N	\N	f	\N
659	CoffeeStatusBackgroundService	2026-10-09 10:09:22.673519	328	Không đổi trạng thái món nào	2026-10-09 10:14:23.00999	Service	2026-10-09 10:09:23.01387	\N	\N	f	\N
660	CoffeeStatusBackgroundService	2026-10-09 10:14:23.13983	338	Không đổi trạng thái món nào	2026-10-09 10:19:23.479275	Service	2026-10-09 10:14:23.479275	\N	\N	f	\N
661	CoffeeStatusBackgroundService	2026-10-09 10:19:23.595592	248	Không đổi trạng thái món nào	2026-10-09 10:24:23.85105	Service	2026-10-09 10:19:23.85105	\N	\N	f	\N
662	CoffeeStatusBackgroundService	2026-10-09 10:24:23.957454	284	Không đổi trạng thái món nào	2026-10-09 10:29:24.246835	Service	2026-10-09 10:24:24.246835	\N	\N	f	\N
663	CoffeeStatusBackgroundService	2026-10-09 10:29:24.360506	324	Không đổi trạng thái món nào	2026-10-09 10:34:24.690727	Service	2026-10-09 10:29:24.690727	\N	\N	f	\N
664	CoffeeStatusBackgroundService	2026-10-09 10:34:24.812566	404	Không đổi trạng thái món nào	2026-10-09 10:39:25.232707	Service	2026-10-09 10:34:25.237705	\N	\N	f	\N
665	CoffeeStatusBackgroundService	2026-10-09 10:39:25.354205	349	Không đổi trạng thái món nào	2026-10-09 10:44:25.716913	Service	2026-10-09 10:39:25.720909	\N	\N	f	\N
666	CoffeeStatusBackgroundService	2026-10-09 10:44:25.84626	358	Không đổi trạng thái món nào	2026-10-09 10:49:26.207826	Service	2026-10-09 10:44:26.228216	\N	\N	f	\N
667	CoffeeStatusBackgroundService	2026-10-09 10:49:26.418253	363	Không đổi trạng thái món nào	2026-10-09 10:54:26.798791	Service	2026-10-09 10:49:26.810386	\N	\N	f	\N
668	CoffeeStatusBackgroundService	2026-10-09 10:54:26.937614	499	Không đổi trạng thái món nào	2026-10-09 10:59:27.457724	Service	2026-10-09 10:54:27.480526	\N	\N	f	\N
669	CoffeeStatusBackgroundService	2026-10-09 10:59:27.64873	383	Không đổi trạng thái món nào	2026-10-09 11:04:28.037608	Service	2026-10-09 10:59:28.039623	\N	\N	f	\N
670	CoffeeStatusBackgroundService	2026-10-09 11:04:28.182356	420	Không đổi trạng thái món nào	2026-10-09 11:09:28.608459	Service	2026-10-09 11:04:28.611933	\N	\N	f	\N
671	CoffeeStatusBackgroundService	2026-10-09 11:09:28.761059	444	Không đổi trạng thái món nào	2026-10-09 11:14:29.210803	Service	2026-10-09 11:09:29.213284	\N	\N	f	\N
672	CoffeeStatusBackgroundService	2026-10-09 11:14:29.401135	397	Không đổi trạng thái món nào	2026-10-09 11:19:29.809328	Service	2026-10-09 11:14:29.818886	\N	\N	f	\N
673	CoffeeStatusBackgroundService	2026-10-09 11:19:29.969228	427	Không đổi trạng thái món nào	2026-10-09 11:24:30.401227	Service	2026-10-09 11:19:30.405299	\N	\N	f	\N
674	CoffeeStatusBackgroundService	2026-10-09 11:24:30.547734	386	Không đổi trạng thái món nào	2026-10-09 11:29:30.950067	Service	2026-10-09 11:24:30.953081	\N	\N	f	\N
675	CoffeeStatusBackgroundService	2026-10-09 11:29:31.099101	473	Không đổi trạng thái món nào	2026-10-09 11:34:31.577016	Service	2026-10-09 11:29:31.580015	\N	\N	f	\N
676	CoffeeStatusBackgroundService	2026-10-09 11:34:31.706342	415	Không đổi trạng thái món nào	2026-10-09 11:39:32.132439	Service	2026-10-09 11:34:32.14345	\N	\N	f	\N
677	CoffeeStatusBackgroundService	2026-10-09 11:39:32.289575	414	Không đổi trạng thái món nào	2026-10-09 11:44:32.713384	Service	2026-10-09 11:39:32.721891	\N	\N	f	\N
678	CoffeeStatusBackgroundService	2026-10-09 11:44:32.879277	376	Không đổi trạng thái món nào	2026-10-09 11:49:33.266335	Service	2026-10-09 11:44:33.270333	\N	\N	f	\N
679	CoffeeStatusBackgroundService	2026-10-09 11:49:33.384886	361	Không đổi trạng thái món nào	2026-10-09 11:54:33.750104	Service	2026-10-09 11:49:33.752651	\N	\N	f	\N
680	CoffeeStatusBackgroundService	2026-10-09 11:54:33.893093	515	Không đổi trạng thái món nào	2026-10-09 11:59:34.432428	Service	2026-10-09 11:54:34.450775	\N	\N	f	\N
681	CoffeeStatusBackgroundService	2026-10-09 11:59:34.620717	371	Không đổi trạng thái món nào	2026-10-09 12:04:34.99689	Service	2026-10-09 11:59:35.005414	\N	\N	f	\N
682	CoffeeStatusBackgroundService	2026-10-09 12:04:35.119241	362	Không đổi trạng thái món nào	2026-10-09 12:09:35.491958	Service	2026-10-09 12:04:35.501905	\N	\N	f	\N
683	CoffeeStatusBackgroundService	2026-10-09 12:09:35.634527	370	Không đổi trạng thái món nào	2026-10-09 12:14:36.012353	Service	2026-10-09 12:09:36.014353	\N	\N	f	\N
684	CoffeeStatusBackgroundService	2026-10-09 12:14:36.137499	395	Không đổi trạng thái món nào	2026-10-09 12:19:36.540807	Service	2026-10-09 12:14:36.543314	\N	\N	f	\N
685	CoffeeStatusBackgroundService	2026-10-09 12:19:36.6777	386	Không đổi trạng thái món nào	2026-10-09 12:24:37.07596	Service	2026-10-09 12:19:37.077961	\N	\N	f	\N
686	CoffeeStatusBackgroundService	2026-10-09 12:24:37.191185	376	Không đổi trạng thái món nào	2026-10-09 12:29:37.578761	Service	2026-10-09 12:24:37.585492	\N	\N	f	\N
687	CoffeeStatusBackgroundService	2026-10-09 12:29:37.723281	380	Không đổi trạng thái món nào	2026-10-09 12:34:38.108557	Service	2026-10-09 12:29:38.111562	\N	\N	f	\N
688	CoffeeStatusBackgroundService	2026-10-09 12:34:38.223597	340	Không đổi trạng thái món nào	2026-10-09 12:39:38.567956	Service	2026-10-09 12:34:38.571212	\N	\N	f	\N
689	CoffeeStatusBackgroundService	2026-10-09 12:39:38.687401	399	Không đổi trạng thái món nào	2026-10-09 12:44:39.095337	Service	2026-10-09 12:39:39.098338	\N	\N	f	\N
690	CoffeeStatusBackgroundService	2026-10-09 12:44:39.253678	517	Không đổi trạng thái món nào	2026-10-09 12:49:39.780195	Service	2026-10-09 12:44:39.784755	\N	\N	f	\N
691	CoffeeStatusBackgroundService	2026-10-09 12:49:39.934741	427	Không đổi trạng thái món nào	2026-10-09 12:54:40.374108	Service	2026-10-09 12:49:40.379254	\N	\N	f	\N
692	CoffeeStatusBackgroundService	2026-10-09 12:54:40.529084	444	Không đổi trạng thái món nào	2026-10-09 12:59:40.97761	Service	2026-10-09 12:54:40.981553	\N	\N	f	\N
693	CoffeeStatusBackgroundService	2026-10-09 12:59:41.114142	362	Không đổi trạng thái món nào	2026-10-09 13:04:41.480931	Service	2026-10-09 12:59:41.484434	\N	\N	f	\N
694	CoffeeStatusBackgroundService	2026-10-09 13:04:41.630723	407	Không đổi trạng thái món nào	2026-10-09 13:09:42.050177	Service	2026-10-09 13:04:42.054177	\N	\N	f	\N
695	CoffeeStatusBackgroundService	2026-10-09 13:09:42.198093	442	Không đổi trạng thái món nào	2026-10-09 13:14:42.6548	Service	2026-10-09 13:09:42.66385	\N	\N	f	\N
696	CoffeeStatusBackgroundService	2026-10-09 13:14:42.794846	402	Không đổi trạng thái món nào	2026-10-09 13:19:43.206105	Service	2026-10-09 13:14:43.213121	\N	\N	f	\N
697	CoffeeStatusBackgroundService	2026-10-09 13:19:43.34009	350	Không đổi trạng thái món nào	2026-10-09 13:24:43.700195	Service	2026-10-09 13:19:43.708214	\N	\N	f	\N
698	CoffeeStatusBackgroundService	2026-10-09 13:24:43.830916	472	Không đổi trạng thái món nào	2026-10-09 13:29:44.311783	Service	2026-10-09 13:24:44.321278	\N	\N	f	\N
699	CoffeeStatusBackgroundService	2026-10-09 13:29:44.458981	412	Không đổi trạng thái món nào	2026-10-09 13:34:44.876204	Service	2026-10-09 13:29:44.878682	\N	\N	f	\N
700	CoffeeStatusBackgroundService	2026-10-09 13:34:44.990362	407	Không đổi trạng thái món nào	2026-10-09 13:39:45.404679	Service	2026-10-09 13:34:45.406679	\N	\N	f	\N
701	CoffeeStatusBackgroundService	2026-10-09 13:39:45.511778	405	Không đổi trạng thái món nào	2026-10-09 13:44:45.920635	Service	2026-10-09 13:39:45.922921	\N	\N	f	\N
702	CoffeeStatusBackgroundService	2026-10-09 13:44:46.061276	446	Không đổi trạng thái món nào	2026-10-09 13:49:46.511407	Service	2026-10-09 13:44:46.513953	\N	\N	f	\N
703	CoffeeStatusBackgroundService	2026-10-09 13:49:46.643717	353	Không đổi trạng thái món nào	2026-10-09 13:54:47.008651	Service	2026-10-09 13:49:47.012834	\N	\N	f	\N
704	CoffeeStatusBackgroundService	2026-10-09 13:54:47.179335	456	Không đổi trạng thái món nào	2026-10-09 13:59:47.643565	Service	2026-10-09 13:54:47.649936	\N	\N	f	\N
705	CoffeeStatusBackgroundService	2026-10-09 13:59:47.80429	394	Không đổi trạng thái món nào	2026-10-09 14:04:48.205388	Service	2026-10-09 13:59:48.209387	\N	\N	f	\N
706	CoffeeStatusBackgroundService	2026-10-09 14:04:48.385221	338	Không đổi trạng thái món nào	2026-10-09 14:09:48.728348	Service	2026-10-09 14:04:48.732783	\N	\N	f	\N
707	CoffeeStatusBackgroundService	2026-10-09 14:09:48.858631	363	Không đổi trạng thái món nào	2026-10-09 14:14:49.226836	Service	2026-10-09 14:09:49.228836	\N	\N	f	\N
708	CoffeeStatusBackgroundService	2026-10-09 14:14:49.339664	350	Không đổi trạng thái món nào	2026-10-09 14:19:49.699724	Service	2026-10-09 14:14:49.706241	\N	\N	f	\N
709	CoffeeStatusBackgroundService	2026-10-09 14:19:49.838799	357	Không đổi trạng thái món nào	2026-10-09 14:24:50.198933	Service	2026-10-09 14:19:50.201957	\N	\N	f	\N
710	CoffeeStatusBackgroundService	2026-10-09 14:24:50.329955	358	Không đổi trạng thái món nào	2026-10-09 14:29:50.698573	Service	2026-10-09 14:24:50.703578	\N	\N	f	\N
711	CoffeeStatusBackgroundService	2026-10-09 14:29:50.823196	373	Không đổi trạng thái món nào	2026-10-09 14:34:51.204056	Service	2026-10-09 14:29:51.2116	\N	\N	f	\N
712	CoffeeStatusBackgroundService	2026-10-09 14:34:51.32098	410	Không đổi trạng thái món nào	2026-10-09 14:39:51.740755	Service	2026-10-09 14:34:51.746755	\N	\N	f	\N
713	CoffeeStatusBackgroundService	2026-10-09 14:39:51.927351	400	Không đổi trạng thái món nào	2026-10-09 14:44:52.333749	Service	2026-10-09 14:39:52.336749	\N	\N	f	\N
714	CoffeeStatusBackgroundService	2026-10-09 14:44:52.448199	292	Không đổi trạng thái món nào	2026-10-09 14:49:52.745823	Service	2026-10-09 14:44:52.751575	\N	\N	f	\N
715	CoffeeStatusBackgroundService	2026-10-09 14:49:52.865063	514	Không đổi trạng thái món nào	2026-10-09 14:54:53.385458	Service	2026-10-09 14:49:53.391459	\N	\N	f	\N
716	CoffeeStatusBackgroundService	2026-10-09 14:54:53.520647	331	Không đổi trạng thái món nào	2026-10-09 14:59:53.864717	Service	2026-10-09 14:54:53.867725	\N	\N	f	\N
717	CoffeeStatusBackgroundService	2026-10-09 14:59:54.016725	514	Không đổi trạng thái món nào	2026-10-09 15:04:54.545117	Service	2026-10-09 14:59:54.555118	\N	\N	f	\N
718	CoffeeStatusBackgroundService	2026-10-09 15:04:54.705654	315	Không đổi trạng thái món nào	2026-10-09 15:09:55.030499	Service	2026-10-09 15:04:55.034496	\N	\N	f	\N
719	CoffeeStatusBackgroundService	2026-10-09 15:09:55.165081	517	Không đổi trạng thái món nào	2026-10-09 15:14:55.691671	Service	2026-10-09 15:09:55.701601	\N	\N	f	\N
720	CoffeeStatusBackgroundService	2026-10-09 15:14:55.863857	430	Không đổi trạng thái món nào	2026-10-09 15:19:56.300999	Service	2026-10-09 15:14:56.303457	\N	\N	f	\N
721	CoffeeStatusBackgroundService	2026-10-09 15:19:56.467521	463	Không đổi trạng thái món nào	2026-10-09 15:24:56.956917	Service	2026-10-09 15:19:56.963214	\N	\N	f	\N
722	CoffeeStatusBackgroundService	2026-10-09 15:24:57.108687	369	Không đổi trạng thái món nào	2026-10-09 15:29:57.4795	Service	2026-10-09 15:24:57.4805	\N	\N	f	\N
723	CoffeeStatusBackgroundService	2026-10-09 15:29:57.589884	497	Không đổi trạng thái món nào	2026-10-09 15:34:58.093607	Service	2026-10-09 15:29:58.096451	\N	\N	f	\N
724	CoffeeStatusBackgroundService	2026-10-09 15:34:58.222564	398	Không đổi trạng thái món nào	2026-10-09 15:39:58.632663	Service	2026-10-09 15:34:58.636676	\N	\N	f	\N
725	CoffeeStatusBackgroundService	2026-10-09 15:39:58.781192	434	Không đổi trạng thái món nào	2026-10-09 15:44:59.229475	Service	2026-10-09 15:39:59.235498	\N	\N	f	\N
726	CoffeeStatusBackgroundService	2026-10-09 15:44:59.364338	324	Không đổi trạng thái món nào	2026-10-09 15:49:59.69515	Service	2026-10-09 15:44:59.704536	\N	\N	f	\N
727	CoffeeStatusBackgroundService	2026-10-09 15:49:59.847143	447	Không đổi trạng thái món nào	2026-10-09 15:55:00.307114	Service	2026-10-09 15:50:00.322393	\N	\N	f	\N
728	CoffeeStatusBackgroundService	2026-10-09 15:55:00.462406	354	Không đổi trạng thái món nào	2026-10-09 16:00:00.822947	Service	2026-10-09 15:55:00.828947	\N	\N	f	\N
729	CoffeeStatusBackgroundService	2026-10-09 16:00:00.936774	205	Không đổi trạng thái món nào	2026-10-09 16:05:01.148684	Service	2026-10-09 16:00:01.151698	\N	\N	f	\N
730	CoffeeStatusBackgroundService	2026-10-09 16:05:01.254448	402	Không đổi trạng thái món nào	2026-10-09 16:10:01.662187	Service	2026-10-09 16:05:01.665734	\N	\N	f	\N
731	CoffeeStatusBackgroundService	2026-10-09 16:10:01.794295	333	Không đổi trạng thái món nào	2026-10-09 16:15:02.134784	Service	2026-10-09 16:10:02.141777	\N	\N	f	\N
732	CoffeeStatusBackgroundService	2026-10-09 16:15:02.256751	262	Không đổi trạng thái món nào	2026-10-09 16:20:02.525821	Service	2026-10-09 16:15:02.536348	\N	\N	f	\N
733	CoffeeStatusBackgroundService	2026-10-09 16:20:02.66577	316	Không đổi trạng thái món nào	2026-10-09 16:25:02.995662	Service	2026-10-09 16:20:03.005676	\N	\N	f	\N
734	CoffeeStatusBackgroundService	2026-10-09 16:25:03.134198	389	Không đổi trạng thái món nào	2026-10-09 16:30:03.531056	Service	2026-10-09 16:25:03.534063	\N	\N	f	\N
735	CoffeeStatusBackgroundService	2026-10-09 16:30:03.652942	289	Không đổi trạng thái món nào	2026-10-09 16:35:03.947313	Service	2026-10-09 16:30:03.949316	\N	\N	f	\N
736	CoffeeStatusBackgroundService	2026-10-09 16:35:04.063582	339	Không đổi trạng thái món nào	2026-10-09 16:40:04.407162	Service	2026-10-09 16:35:04.409357	\N	\N	f	\N
737	CoffeeStatusBackgroundService	2026-10-09 16:40:04.518595	259	Không đổi trạng thái món nào	2026-10-09 16:45:04.7919	Service	2026-10-09 16:40:04.800915	\N	\N	f	\N
738	CoffeeStatusBackgroundService	2026-10-09 16:45:04.914673	579	Không đổi trạng thái món nào	2026-10-09 16:50:05.501576	Service	2026-10-09 16:45:05.506011	\N	\N	f	\N
739	CoffeeStatusBackgroundService	2026-10-09 16:50:05.618476	329	Không đổi trạng thái món nào	2026-10-09 16:55:05.96223	Service	2026-10-09 16:50:05.966229	\N	\N	f	\N
740	CoffeeStatusBackgroundService	2026-10-09 16:55:06.075021	265	Không đổi trạng thái món nào	2026-10-09 17:00:06.355183	Service	2026-10-09 16:55:06.363187	\N	\N	f	\N
741	CoffeeStatusBackgroundService	2026-10-09 17:00:06.469267	270	Không đổi trạng thái món nào	2026-10-09 17:05:06.746831	Service	2026-10-09 17:00:06.750832	\N	\N	f	\N
742	CoffeeStatusBackgroundService	2026-10-09 17:05:06.855168	261	Không đổi trạng thái món nào	2026-10-09 17:10:07.123993	Service	2026-10-09 17:05:07.128007	\N	\N	f	\N
743	CoffeeStatusBackgroundService	2026-10-09 17:10:07.230918	338	Không đổi trạng thái món nào	2026-10-09 17:15:07.574244	Service	2026-10-09 17:10:07.579274	\N	\N	f	\N
744	CoffeeStatusBackgroundService	2026-10-09 17:15:07.734477	381	Không đổi trạng thái món nào	2026-10-09 17:20:08.116472	Service	2026-10-09 17:15:08.122316	\N	\N	f	\N
745	CoffeeStatusBackgroundService	2026-10-09 17:20:08.238782	325	Không đổi trạng thái món nào	2026-10-09 17:25:08.575768	Service	2026-10-09 17:20:08.582331	\N	\N	f	\N
746	CoffeeStatusBackgroundService	2026-10-09 17:25:08.731309	353	Không đổi trạng thái món nào	2026-10-09 17:30:09.089611	Service	2026-10-09 17:25:09.09211	\N	\N	f	\N
747	CoffeeStatusBackgroundService	2026-10-09 17:30:09.211359	257	Không đổi trạng thái món nào	2026-10-09 17:35:09.477278	Service	2026-10-09 17:30:09.480858	\N	\N	f	\N
748	CoffeeStatusBackgroundService	2026-10-09 17:35:09.586516	266	Không đổi trạng thái món nào	2026-10-09 17:40:09.864019	Service	2026-10-09 17:35:09.869073	\N	\N	f	\N
749	CoffeeStatusBackgroundService	2026-10-09 17:40:09.966144	299	Không đổi trạng thái món nào	2026-10-09 17:45:10.269367	Service	2026-10-09 17:40:10.270373	\N	\N	f	\N
750	CoffeeStatusBackgroundService	2026-10-09 17:45:10.37208	311	Không đổi trạng thái món nào	2026-10-09 17:50:10.685077	Service	2026-10-09 17:45:10.691086	\N	\N	f	\N
751	CoffeeStatusBackgroundService	2026-10-09 17:50:10.806164	354	Không đổi trạng thái món nào	2026-10-09 17:55:11.167761	Service	2026-10-09 17:50:11.170781	\N	\N	f	\N
752	CoffeeStatusBackgroundService	2026-10-09 17:55:11.298677	344	Không đổi trạng thái món nào	2026-10-09 18:00:11.652248	Service	2026-10-09 17:55:11.656964	\N	\N	f	\N
753	CoffeeStatusBackgroundService	2026-10-09 18:00:11.78889	207	Không đổi trạng thái món nào	2026-10-09 18:05:11.997413	Service	2026-10-09 18:00:11.997413	\N	\N	f	\N
754	CoffeeStatusBackgroundService	2026-10-09 18:05:12.088064	268	Không đổi trạng thái món nào	2026-10-09 18:10:12.368045	Service	2026-10-09 18:05:12.373064	\N	\N	f	\N
755	CoffeeStatusBackgroundService	2026-10-09 18:10:12.480905	295	Không đổi trạng thái món nào	2026-10-09 18:15:12.787521	Service	2026-10-09 18:10:12.796091	\N	\N	f	\N
756	CoffeeStatusBackgroundService	2026-10-09 18:15:12.912484	322	Không đổi trạng thái món nào	2026-10-09 18:20:13.235721	Service	2026-10-09 18:15:13.242222	\N	\N	f	\N
757	CoffeeStatusBackgroundService	2026-10-09 18:20:13.362843	355	Không đổi trạng thái món nào	2026-10-09 18:25:13.730299	Service	2026-10-09 18:20:13.733846	\N	\N	f	\N
758	CoffeeStatusBackgroundService	2026-10-09 18:25:13.848817	284	Không đổi trạng thái món nào	2026-10-09 18:30:14.139593	Service	2026-10-09 18:25:14.141243	\N	\N	f	\N
759	CoffeeStatusBackgroundService	2026-10-09 18:30:14.241688	350	Không đổi trạng thái món nào	2026-10-09 18:35:14.599162	Service	2026-10-09 18:30:14.601219	\N	\N	f	\N
760	CoffeeStatusBackgroundService	2026-10-09 18:35:14.718615	253	Không đổi trạng thái món nào	2026-10-09 18:40:15.011198	Service	2026-10-09 18:35:15.070234	\N	\N	f	\N
761	CoffeeStatusBackgroundService	2026-10-09 18:40:15.165483	299	Không đổi trạng thái món nào	2026-10-09 18:45:15.479335	Service	2026-10-09 18:40:15.4909	\N	\N	f	\N
762	CoffeeStatusBackgroundService	2026-10-09 18:45:15.620665	252	Không đổi trạng thái món nào	2026-10-09 18:50:15.873137	Service	2026-10-09 18:45:15.874672	\N	\N	f	\N
763	CoffeeStatusBackgroundService	2026-10-09 18:50:15.96963	298	Không đổi trạng thái món nào	2026-10-09 18:55:16.276803	Service	2026-10-09 18:50:16.280804	\N	\N	f	\N
764	CoffeeStatusBackgroundService	2026-10-09 18:55:16.395604	243	Không đổi trạng thái món nào	2026-10-09 19:00:16.643563	Service	2026-10-09 18:55:16.645563	\N	\N	f	\N
765	CoffeeStatusBackgroundService	2026-10-09 19:00:16.736101	244	Không đổi trạng thái món nào	2026-10-09 19:05:16.98897	Service	2026-10-09 19:00:16.991592	\N	\N	f	\N
766	CoffeeStatusBackgroundService	2026-10-09 19:05:17.086793	244	Không đổi trạng thái món nào	2026-10-09 19:10:17.365173	Service	2026-10-09 19:05:17.374726	\N	\N	f	\N
767	CoffeeStatusBackgroundService	2026-10-09 19:10:17.471459	284	Không đổi trạng thái món nào	2026-10-09 19:15:17.763971	Service	2026-10-09 19:10:17.768988	\N	\N	f	\N
768	CoffeeStatusBackgroundService	2026-10-09 19:15:17.863769	233	Không đổi trạng thái món nào	2026-10-09 19:20:18.098854	Service	2026-10-09 19:15:18.103902	\N	\N	f	\N
769	CoffeeStatusBackgroundService	2026-10-09 19:20:18.212343	269	Không đổi trạng thái món nào	2026-10-09 19:25:18.491151	Service	2026-10-09 19:20:18.493754	\N	\N	f	\N
770	CoffeeStatusBackgroundService	2026-10-09 19:25:18.588788	385	Không đổi trạng thái món nào	2026-10-09 19:30:18.982422	Service	2026-10-09 19:25:18.986426	\N	\N	f	\N
771	CoffeeStatusBackgroundService	2026-10-09 19:30:19.095839	276	Không đổi trạng thái món nào	2026-10-09 19:35:19.386237	Service	2026-10-09 19:30:19.394421	\N	\N	f	\N
772	CoffeeStatusBackgroundService	2026-10-09 19:35:19.494849	400	Không đổi trạng thái món nào	2026-10-09 19:40:19.911482	Service	2026-10-09 19:35:19.923018	\N	\N	f	\N
773	CoffeeStatusBackgroundService	2026-10-09 19:40:20.0498	267	Không đổi trạng thái món nào	2026-10-09 19:45:20.325146	Service	2026-10-09 19:40:20.328485	\N	\N	f	\N
774	CoffeeStatusBackgroundService	2026-10-09 19:45:20.428601	281	Không đổi trạng thái món nào	2026-10-09 19:50:20.710794	Service	2026-10-09 19:45:20.715809	\N	\N	f	\N
775	CoffeeStatusBackgroundService	2026-10-09 19:50:20.889627	331	Không đổi trạng thái món nào	2026-10-09 19:55:21.227298	Service	2026-10-09 19:50:21.229804	\N	\N	f	\N
776	CoffeeStatusBackgroundService	2026-10-09 19:55:21.334401	325	Không đổi trạng thái món nào	2026-10-09 20:00:21.671829	Service	2026-10-09 19:55:21.678429	\N	\N	f	\N
777	CoffeeStatusBackgroundService	2026-10-09 20:00:21.789311	252	Không đổi trạng thái món nào	2026-10-09 20:05:22.050002	Service	2026-10-09 20:00:22.056656	\N	\N	f	\N
778	CoffeeStatusBackgroundService	2026-10-09 20:05:22.161267	338	Không đổi trạng thái món nào	2026-10-09 20:10:22.506424	Service	2026-10-09 20:05:22.508884	\N	\N	f	\N
779	CoffeeStatusBackgroundService	2026-10-09 20:10:22.607338	268	Không đổi trạng thái món nào	2026-10-09 20:15:22.882236	Service	2026-10-09 20:10:22.884246	\N	\N	f	\N
780	CoffeeStatusBackgroundService	2026-10-09 20:15:22.987288	257	Không đổi trạng thái món nào	2026-10-09 20:20:23.245979	Service	2026-10-09 20:15:23.252349	\N	\N	f	\N
781	CoffeeStatusBackgroundService	2026-10-09 20:20:23.351071	255	Không đổi trạng thái món nào	2026-10-09 20:25:23.612981	Service	2026-10-09 20:20:23.615933	\N	\N	f	\N
782	CoffeeStatusBackgroundService	2026-10-09 20:25:23.712395	315	Không đổi trạng thái món nào	2026-10-09 20:30:24.037029	Service	2026-10-09 20:25:24.044405	\N	\N	f	\N
783	CoffeeStatusBackgroundService	2026-10-09 20:30:24.158992	316	Không đổi trạng thái món nào	2026-10-09 20:35:24.483728	Service	2026-10-09 20:30:24.486728	\N	\N	f	\N
784	CoffeeStatusBackgroundService	2026-10-09 20:35:24.604152	289	Không đổi trạng thái món nào	2026-10-09 20:40:24.918226	Service	2026-10-09 20:35:24.971365	\N	\N	f	\N
785	CoffeeStatusBackgroundService	2026-10-09 20:40:25.15049	276	Không đổi trạng thái món nào	2026-10-09 20:45:25.434276	Service	2026-10-09 20:40:25.436276	\N	\N	f	\N
786	CoffeeStatusBackgroundService	2026-10-09 20:45:25.551002	286	Không đổi trạng thái món nào	2026-10-09 20:50:25.838595	Service	2026-10-09 20:45:25.844599	\N	\N	f	\N
787	CoffeeStatusBackgroundService	2026-10-09 20:50:25.959755	285	Không đổi trạng thái món nào	2026-10-09 20:55:26.250552	Service	2026-10-09 20:50:26.25455	\N	\N	f	\N
788	CoffeeStatusBackgroundService	2026-10-09 20:55:26.349986	306	Không đổi trạng thái món nào	2026-10-09 21:00:26.666246	Service	2026-10-09 20:55:26.670222	\N	\N	f	\N
789	CoffeeStatusBackgroundService	2026-10-09 21:00:26.772156	213	Không đổi trạng thái món nào	2026-10-09 21:05:26.98915	Service	2026-10-09 21:00:26.992155	\N	\N	f	\N
790	CoffeeStatusBackgroundService	2026-10-09 21:05:27.084292	271	Không đổi trạng thái món nào	2026-10-09 21:10:27.368905	Service	2026-10-09 21:05:27.374888	\N	\N	f	\N
791	CoffeeStatusBackgroundService	2026-10-09 21:10:27.477617	245	Không đổi trạng thái món nào	2026-10-09 21:15:27.729213	Service	2026-10-09 21:10:27.732242	\N	\N	f	\N
792	CoffeeStatusBackgroundService	2026-10-09 21:15:27.829279	276	Không đổi trạng thái món nào	2026-10-09 21:20:28.106953	Service	2026-10-09 21:15:28.114215	\N	\N	f	\N
793	CoffeeStatusBackgroundService	2026-10-09 21:20:28.220435	267	Không đổi trạng thái món nào	2026-10-09 21:25:28.499348	Service	2026-10-09 21:20:28.502358	\N	\N	f	\N
794	CoffeeStatusBackgroundService	2026-10-09 21:25:28.59634	260	Không đổi trạng thái món nào	2026-10-09 21:30:28.861168	Service	2026-10-09 21:25:28.864168	\N	\N	f	\N
795	CoffeeStatusBackgroundService	2026-10-09 21:30:28.959522	286	Không đổi trạng thái món nào	2026-10-09 21:35:29.255954	Service	2026-10-09 21:30:29.262501	\N	\N	f	\N
796	CoffeeStatusBackgroundService	2026-10-09 21:35:29.376289	321	Không đổi trạng thái món nào	2026-10-09 21:40:29.706407	Service	2026-10-09 21:35:29.713507	\N	\N	f	\N
797	CoffeeStatusBackgroundService	2026-10-09 21:40:29.821007	275	Không đổi trạng thái món nào	2026-10-09 21:45:30.102899	Service	2026-10-09 21:40:30.108921	\N	\N	f	\N
798	CoffeeStatusBackgroundService	2026-10-09 21:45:30.203929	289	Không đổi trạng thái món nào	2026-10-09 21:50:30.495944	Service	2026-10-09 21:45:30.505938	\N	\N	f	\N
799	CoffeeStatusBackgroundService	2026-10-09 21:50:30.622648	277	Không đổi trạng thái món nào	2026-10-09 21:55:30.906414	Service	2026-10-09 21:50:30.909779	\N	\N	f	\N
800	CoffeeStatusBackgroundService	2026-10-09 21:55:31.00819	291	Không đổi trạng thái món nào	2026-10-09 22:00:31.32068	Service	2026-10-09 21:55:31.346493	\N	\N	f	\N
801	CoffeeStatusBackgroundService	2026-10-09 22:00:31.485114	255	Không đổi trạng thái món nào	2026-10-09 22:05:31.745376	Service	2026-10-09 22:00:31.747844	\N	\N	f	\N
802	CoffeeStatusBackgroundService	2026-10-09 22:05:31.843434	268	Không đổi trạng thái món nào	2026-10-09 22:10:32.121555	Service	2026-10-09 22:05:32.139427	\N	\N	f	\N
803	CoffeeStatusBackgroundService	2026-10-09 22:10:32.24363	284	Không đổi trạng thái món nào	2026-10-09 22:15:32.534045	Service	2026-10-09 22:10:32.539122	\N	\N	f	\N
804	CoffeeStatusBackgroundService	2026-10-09 22:15:32.631572	279	Không đổi trạng thái món nào	2026-10-09 22:20:32.912625	Service	2026-10-09 22:15:32.919624	\N	\N	f	\N
805	CoffeeStatusBackgroundService	2026-10-09 22:20:33.030885	262	Không đổi trạng thái món nào	2026-10-09 22:25:33.300355	Service	2026-10-09 22:20:33.304356	\N	\N	f	\N
806	CoffeeStatusBackgroundService	2026-10-09 22:25:33.407948	248	Không đổi trạng thái món nào	2026-10-09 22:30:33.665047	Service	2026-10-09 22:25:33.669063	\N	\N	f	\N
807	CoffeeStatusBackgroundService	2026-10-09 22:30:33.770103	249	Không đổi trạng thái món nào	2026-10-09 22:35:34.029278	Service	2026-10-09 22:30:34.033278	\N	\N	f	\N
808	CoffeeStatusBackgroundService	2026-10-09 22:35:34.139228	256	Không đổi trạng thái món nào	2026-10-09 22:40:34.398828	Service	2026-10-09 22:35:34.400828	\N	\N	f	\N
809	CoffeeStatusBackgroundService	2026-10-09 22:40:34.491034	244	Không đổi trạng thái món nào	2026-10-09 22:45:34.73882	Service	2026-10-09 22:40:34.742817	\N	\N	f	\N
810	CoffeeStatusBackgroundService	2026-10-09 22:45:34.84353	306	Không đổi trạng thái món nào	2026-10-09 22:50:35.152323	Service	2026-10-09 22:45:35.159205	\N	\N	f	\N
811	CoffeeStatusBackgroundService	2026-10-09 22:50:35.258116	247	Không đổi trạng thái món nào	2026-10-09 22:55:35.512375	Service	2026-10-09 22:50:35.51541	\N	\N	f	\N
812	CoffeeStatusBackgroundService	2026-10-09 22:55:35.613186	336	Không đổi trạng thái món nào	2026-10-09 23:00:35.963499	Service	2026-10-09 22:55:35.973038	\N	\N	f	\N
813	CoffeeStatusBackgroundService	2026-10-09 23:00:36.097874	306	Không đổi trạng thái món nào	2026-10-09 23:05:36.412386	Service	2026-10-09 23:00:36.416386	\N	\N	f	\N
814	CoffeeStatusBackgroundService	2026-10-09 23:05:36.517045	308	Không đổi trạng thái món nào	2026-10-09 23:10:36.829729	Service	2026-10-09 23:05:36.832792	\N	\N	f	\N
815	CoffeeStatusBackgroundService	2026-10-09 23:10:36.958626	421	Không đổi trạng thái món nào	2026-10-09 23:15:37.393159	Service	2026-10-09 23:10:37.400171	\N	\N	f	\N
816	CoffeeStatusBackgroundService	2026-10-09 23:15:37.513717	343	Không đổi trạng thái món nào	2026-10-09 23:20:37.859255	Service	2026-10-09 23:15:37.863292	\N	\N	f	\N
817	CoffeeStatusBackgroundService	2026-10-09 23:20:37.960272	346	Không đổi trạng thái món nào	2026-10-09 23:25:38.344782	Service	2026-10-09 23:20:38.398926	\N	\N	f	\N
818	CoffeeStatusBackgroundService	2026-10-09 23:25:38.540575	266	Không đổi trạng thái món nào	2026-10-09 23:30:38.811426	Service	2026-10-09 23:25:38.816999	\N	\N	f	\N
819	CoffeeStatusBackgroundService	2026-10-09 23:30:38.937225	222	Không đổi trạng thái món nào	2026-10-09 23:35:39.162248	Service	2026-10-09 23:30:39.16509	\N	\N	f	\N
820	CoffeeStatusBackgroundService	2026-10-09 23:35:39.269684	334	Không đổi trạng thái món nào	2026-10-09 23:40:39.615312	Service	2026-10-09 23:35:39.621614	\N	\N	f	\N
821	CoffeeStatusBackgroundService	2026-10-09 23:40:39.733167	286	Không đổi trạng thái món nào	2026-10-09 23:45:40.028216	Service	2026-10-09 23:40:40.036217	\N	\N	f	\N
822	CoffeeStatusBackgroundService	2026-10-09 23:45:40.161704	261	Không đổi trạng thái món nào	2026-10-09 23:50:40.424848	Service	2026-10-09 23:45:40.430406	\N	\N	f	\N
823	CoffeeStatusBackgroundService	2026-10-09 23:50:40.535855	303	Không đổi trạng thái món nào	2026-10-09 23:55:40.847992	Service	2026-10-09 23:50:40.852538	\N	\N	f	\N
824	CoffeeStatusBackgroundService	2026-10-09 23:55:40.980858	306	Không đổi trạng thái món nào	2026-10-10 00:00:41.294311	Service	2026-10-09 23:55:41.300324	\N	\N	f	\N
825	CoffeeStatusBackgroundService	2026-10-10 00:00:41.424299	241	Không đổi trạng thái món nào	2026-10-10 00:05:41.669294	Service	2026-10-10 00:00:41.673809	\N	\N	f	\N
826	CoffeeStatusBackgroundService	2026-10-10 00:05:41.775146	239	Không đổi trạng thái món nào	2026-10-10 00:10:42.019203	Service	2026-10-10 00:05:42.023204	\N	\N	f	\N
827	CoffeeStatusBackgroundService	2026-10-10 00:10:42.125534	321	Không đổi trạng thái món nào	2026-10-10 00:15:42.453108	Service	2026-10-10 00:10:42.455117	\N	\N	f	\N
828	CoffeeStatusBackgroundService	2026-10-10 00:15:42.555219	298	Không đổi trạng thái món nào	2026-10-10 00:20:42.854998	Service	2026-10-10 00:15:42.857488	\N	\N	f	\N
829	CoffeeStatusBackgroundService	2026-10-10 00:20:42.959967	298	Không đổi trạng thái món nào	2026-10-10 00:25:43.268614	Service	2026-10-10 00:20:43.272999	\N	\N	f	\N
830	CoffeeStatusBackgroundService	2026-10-10 00:25:43.390304	310	Không đổi trạng thái món nào	2026-10-10 00:30:43.709224	Service	2026-10-10 00:25:43.717525	\N	\N	f	\N
831	CoffeeStatusBackgroundService	2026-10-10 00:30:43.827887	284	Không đổi trạng thái món nào	2026-10-10 00:35:44.117208	Service	2026-10-10 00:30:44.120219	\N	\N	f	\N
832	CoffeeStatusBackgroundService	2026-10-10 00:35:44.42059	276	Không đổi trạng thái món nào	2026-10-10 00:40:44.704163	Service	2026-10-10 00:35:44.707039	\N	\N	f	\N
833	CoffeeStatusBackgroundService	2026-10-10 00:40:44.80361	265	Không đổi trạng thái món nào	2026-10-10 00:45:45.072744	Service	2026-10-10 00:40:45.075748	\N	\N	f	\N
834	CoffeeStatusBackgroundService	2026-10-10 00:45:45.163707	250	Không đổi trạng thái món nào	2026-10-10 00:50:45.416421	Service	2026-10-10 00:45:45.42142	\N	\N	f	\N
835	CoffeeStatusBackgroundService	2026-10-10 00:50:45.534387	244	Không đổi trạng thái món nào	2026-10-10 00:55:45.79018	Service	2026-10-10 00:50:45.794183	\N	\N	f	\N
836	CoffeeStatusBackgroundService	2026-10-10 00:55:45.912531	352	Không đổi trạng thái món nào	2026-10-10 01:00:46.273362	Service	2026-10-10 00:55:46.278363	\N	\N	f	\N
837	CoffeeStatusBackgroundService	2026-10-10 01:00:46.388492	243	Không đổi trạng thái món nào	2026-10-10 01:05:46.637102	Service	2026-10-10 01:00:46.643113	\N	\N	f	\N
838	CoffeeStatusBackgroundService	2026-10-10 01:05:46.740475	269	Không đổi trạng thái món nào	2026-10-10 01:10:47.018022	Service	2026-10-10 01:05:47.024025	\N	\N	f	\N
839	CoffeeStatusBackgroundService	2026-10-10 01:10:47.121286	270	Không đổi trạng thái món nào	2026-10-10 01:15:47.397316	Service	2026-10-10 01:10:47.401333	\N	\N	f	\N
840	CoffeeStatusBackgroundService	2026-10-10 01:15:47.495498	256	Không đổi trạng thái món nào	2026-10-10 01:20:47.753699	Service	2026-10-10 01:15:47.761442	\N	\N	f	\N
841	CoffeeStatusBackgroundService	2026-10-10 01:20:47.859469	246	Không đổi trạng thái món nào	2026-10-10 01:25:48.110412	Service	2026-10-10 01:20:48.112413	\N	\N	f	\N
842	CoffeeStatusBackgroundService	2026-10-10 01:25:48.203116	254	Không đổi trạng thái món nào	2026-10-10 01:30:48.46183	Service	2026-10-10 01:25:48.470464	\N	\N	f	\N
843	CoffeeStatusBackgroundService	2026-10-10 01:30:48.573056	254	Không đổi trạng thái món nào	2026-10-10 01:35:48.83424	Service	2026-10-10 01:30:48.838239	\N	\N	f	\N
844	CoffeeStatusBackgroundService	2026-10-10 01:35:48.935007	261	Không đổi trạng thái món nào	2026-10-10 01:40:49.207623	Service	2026-10-10 01:35:49.212655	\N	\N	f	\N
845	CoffeeStatusBackgroundService	2026-10-10 01:40:49.314108	344	Không đổi trạng thái món nào	2026-10-10 01:45:49.673082	Service	2026-10-10 01:40:49.680093	\N	\N	f	\N
846	CoffeeStatusBackgroundService	2026-10-10 01:45:49.795213	273	Không đổi trạng thái món nào	2026-10-10 01:50:50.069437	Service	2026-10-10 01:45:50.074438	\N	\N	f	\N
847	CoffeeStatusBackgroundService	2026-10-10 01:50:50.17446	293	Không đổi trạng thái món nào	2026-10-10 01:55:50.480134	Service	2026-10-10 01:50:50.488134	\N	\N	f	\N
848	CoffeeStatusBackgroundService	2026-10-10 01:55:50.601028	241	Không đổi trạng thái món nào	2026-10-10 02:00:50.850366	Service	2026-10-10 01:55:50.854917	\N	\N	f	\N
849	CoffeeStatusBackgroundService	2026-10-10 02:00:50.949888	353	Không đổi trạng thái món nào	2026-10-10 02:05:51.316769	Service	2026-10-10 02:00:51.320753	\N	\N	f	\N
850	CoffeeStatusBackgroundService	2026-10-10 02:05:51.443206	348	Không đổi trạng thái món nào	2026-10-10 02:10:51.796707	Service	2026-10-10 02:05:51.799709	\N	\N	f	\N
851	CoffeeStatusBackgroundService	2026-10-10 02:10:51.910705	282	Không đổi trạng thái món nào	2026-10-10 02:15:52.199356	Service	2026-10-10 02:10:52.201364	\N	\N	f	\N
852	CoffeeStatusBackgroundService	2026-10-10 02:15:52.351625	269	Không đổi trạng thái món nào	2026-10-10 02:20:52.621697	Service	2026-10-10 02:15:52.624715	\N	\N	f	\N
853	CoffeeStatusBackgroundService	2026-10-10 02:20:52.739047	306	Không đổi trạng thái món nào	2026-10-10 02:25:53.049798	Service	2026-10-10 02:20:53.053229	\N	\N	f	\N
854	CoffeeStatusBackgroundService	2026-10-10 02:25:53.172357	298	Không đổi trạng thái món nào	2026-10-10 02:30:53.476996	Service	2026-10-10 02:25:53.481921	\N	\N	f	\N
855	CoffeeStatusBackgroundService	2026-10-10 02:30:53.594534	341	Không đổi trạng thái món nào	2026-10-10 02:35:53.943538	Service	2026-10-10 02:30:53.946557	\N	\N	f	\N
856	CoffeeStatusBackgroundService	2026-10-10 02:35:54.101377	273	Không đổi trạng thái món nào	2026-10-10 02:40:54.379437	Service	2026-10-10 02:35:54.381438	\N	\N	f	\N
857	CoffeeStatusBackgroundService	2026-10-10 02:40:54.478457	241	Không đổi trạng thái món nào	2026-10-10 02:45:54.724029	Service	2026-10-10 02:40:54.726416	\N	\N	f	\N
858	CoffeeStatusBackgroundService	2026-10-10 02:45:54.829485	270	Không đổi trạng thái món nào	2026-10-10 02:50:55.1008	Service	2026-10-10 02:45:55.104814	\N	\N	f	\N
859	CoffeeStatusBackgroundService	2026-10-10 02:50:55.204603	245	Không đổi trạng thái món nào	2026-10-10 02:55:55.46474	Service	2026-10-10 02:50:55.474833	\N	\N	f	\N
860	CoffeeStatusBackgroundService	2026-10-10 02:55:55.596952	246	Không đổi trạng thái món nào	2026-10-10 03:00:55.848771	Service	2026-10-10 02:55:55.851489	\N	\N	f	\N
861	CoffeeStatusBackgroundService	2026-10-10 03:00:55.939145	264	Không đổi trạng thái món nào	2026-10-10 03:05:56.213112	Service	2026-10-10 03:00:56.222265	\N	\N	f	\N
862	CoffeeStatusBackgroundService	2026-10-10 03:05:56.406158	262	Không đổi trạng thái món nào	2026-10-10 03:10:56.679302	Service	2026-10-10 03:05:56.683307	\N	\N	f	\N
863	CoffeeStatusBackgroundService	2026-10-10 03:10:56.786652	321	Không đổi trạng thái món nào	2026-10-10 03:15:57.115891	Service	2026-10-10 03:10:57.121228	\N	\N	f	\N
864	CoffeeStatusBackgroundService	2026-10-10 03:15:57.222053	243	Không đổi trạng thái món nào	2026-10-10 03:20:57.465307	Service	2026-10-10 03:15:57.468431	\N	\N	f	\N
865	CoffeeStatusBackgroundService	2026-10-10 03:20:57.560534	244	Không đổi trạng thái món nào	2026-10-10 03:25:57.816147	Service	2026-10-10 03:20:57.82215	\N	\N	f	\N
866	CoffeeStatusBackgroundService	2026-10-10 03:25:57.917624	357	Không đổi trạng thái món nào	2026-10-10 03:30:58.279749	Service	2026-10-10 03:25:58.28239	\N	\N	f	\N
867	CoffeeStatusBackgroundService	2026-10-10 03:30:58.400374	285	Không đổi trạng thái món nào	2026-10-10 03:35:58.693924	Service	2026-10-10 03:30:58.69992	\N	\N	f	\N
868	CoffeeStatusBackgroundService	2026-10-10 03:35:58.863255	378	Không đổi trạng thái món nào	2026-10-10 03:40:59.246575	Service	2026-10-10 03:35:59.248575	\N	\N	f	\N
869	CoffeeStatusBackgroundService	2026-10-10 03:40:59.362869	319	Không đổi trạng thái món nào	2026-10-10 03:45:59.687098	Service	2026-10-10 03:40:59.691092	\N	\N	f	\N
870	CoffeeStatusBackgroundService	2026-10-10 03:45:59.811483	433	Không đổi trạng thái món nào	2026-10-10 03:51:00.271604	Service	2026-10-10 03:46:00.288897	\N	\N	f	\N
871	CoffeeStatusBackgroundService	2026-10-10 03:51:00.409202	287	Không đổi trạng thái món nào	2026-10-10 03:56:00.700579	Service	2026-10-10 03:51:00.70497	\N	\N	f	\N
872	CoffeeStatusBackgroundService	2026-10-10 03:56:00.84037	384	Không đổi trạng thái món nào	2026-10-10 04:01:01.235037	Service	2026-10-10 03:56:01.239572	\N	\N	f	\N
873	CoffeeStatusBackgroundService	2026-10-10 04:01:01.345126	261	Không đổi trạng thái món nào	2026-10-10 04:06:01.608888	Service	2026-10-10 04:01:01.613887	\N	\N	f	\N
874	CoffeeStatusBackgroundService	2026-10-10 04:06:01.723227	298	Không đổi trạng thái món nào	2026-10-10 04:11:02.032068	Service	2026-10-10 04:06:02.035348	\N	\N	f	\N
875	CoffeeStatusBackgroundService	2026-10-10 04:11:02.142649	233	Không đổi trạng thái món nào	2026-10-10 04:16:02.380931	Service	2026-10-10 04:11:02.383308	\N	\N	f	\N
876	CoffeeStatusBackgroundService	2026-10-10 04:16:02.478066	330	Không đổi trạng thái món nào	2026-10-10 04:21:02.811646	Service	2026-10-10 04:16:02.818666	\N	\N	f	\N
877	CoffeeStatusBackgroundService	2026-10-10 04:21:02.928804	279	Không đổi trạng thái món nào	2026-10-10 04:26:03.21451	Service	2026-10-10 04:21:03.216987	\N	\N	f	\N
878	CoffeeStatusBackgroundService	2026-10-10 04:26:03.318475	298	Không đổi trạng thái món nào	2026-10-10 04:31:03.621581	Service	2026-10-10 04:26:03.625608	\N	\N	f	\N
879	CoffeeStatusBackgroundService	2026-10-10 04:31:03.739394	275	Không đổi trạng thái món nào	2026-10-10 04:36:04.019102	Service	2026-10-10 04:31:04.021106	\N	\N	f	\N
880	CoffeeStatusBackgroundService	2026-10-10 04:36:04.140476	403	Không đổi trạng thái món nào	2026-10-10 04:41:04.550839	Service	2026-10-10 04:36:04.555824	\N	\N	f	\N
881	CoffeeStatusBackgroundService	2026-10-10 04:41:04.675734	297	Không đổi trạng thái món nào	2026-10-10 04:46:04.981986	Service	2026-10-10 04:41:04.991118	\N	\N	f	\N
882	CoffeeStatusBackgroundService	2026-10-10 04:46:05.109835	287	Không đổi trạng thái món nào	2026-10-10 04:51:05.39759	Service	2026-10-10 04:46:05.40059	\N	\N	f	\N
883	CoffeeStatusBackgroundService	2026-10-10 04:51:05.51287	305	Không đổi trạng thái món nào	2026-10-10 04:56:05.821587	Service	2026-10-10 04:51:05.822584	\N	\N	f	\N
884	CoffeeStatusBackgroundService	2026-10-10 04:56:05.916325	260	Không đổi trạng thái món nào	2026-10-10 05:01:06.179783	Service	2026-10-10 04:56:06.182873	\N	\N	f	\N
885	CoffeeStatusBackgroundService	2026-10-10 05:01:06.299021	298	Không đổi trạng thái món nào	2026-10-10 05:06:06.608111	Service	2026-10-10 05:01:06.612859	\N	\N	f	\N
886	CoffeeStatusBackgroundService	2026-10-10 05:06:06.721382	258	Không đổi trạng thái món nào	2026-10-10 05:11:06.992591	Service	2026-10-10 05:06:06.99674	\N	\N	f	\N
887	CoffeeStatusBackgroundService	2026-10-10 05:11:07.111924	302	Không đổi trạng thái món nào	2026-10-10 05:16:07.425017	Service	2026-10-10 05:11:07.4307	\N	\N	f	\N
888	CoffeeStatusBackgroundService	2026-10-10 05:16:07.550372	255	Không đổi trạng thái món nào	2026-10-10 05:21:07.805961	Service	2026-10-10 05:16:07.808966	\N	\N	f	\N
889	CoffeeStatusBackgroundService	2026-10-10 05:21:07.911705	347	Không đổi trạng thái món nào	2026-10-10 05:26:08.268196	Service	2026-10-10 05:21:08.274194	\N	\N	f	\N
890	CoffeeStatusBackgroundService	2026-10-10 05:26:08.368304	309	Không đổi trạng thái món nào	2026-10-10 05:31:08.685525	Service	2026-10-10 05:26:08.688108	\N	\N	f	\N
891	CoffeeStatusBackgroundService	2026-10-10 05:31:08.789027	239	Không đổi trạng thái món nào	2026-10-10 05:36:09.036297	Service	2026-10-10 05:31:09.040955	\N	\N	f	\N
892	CoffeeStatusBackgroundService	2026-10-10 05:36:09.147393	329	Không đổi trạng thái món nào	2026-10-10 05:41:09.484437	Service	2026-10-10 05:36:09.490438	\N	\N	f	\N
893	CoffeeStatusBackgroundService	2026-10-10 05:41:09.593064	286	Không đổi trạng thái món nào	2026-10-10 05:46:09.889598	Service	2026-10-10 05:41:09.899727	\N	\N	f	\N
894	CoffeeStatusBackgroundService	2026-10-10 05:46:10.018406	234	Không đổi trạng thái món nào	2026-10-10 05:51:10.253361	Service	2026-10-10 05:46:10.258374	\N	\N	f	\N
895	CoffeeStatusBackgroundService	2026-10-10 05:51:10.369273	296	Không đổi trạng thái món nào	2026-10-10 05:56:10.670409	Service	2026-10-10 05:51:10.675414	\N	\N	f	\N
896	CoffeeStatusBackgroundService	2026-10-10 05:56:10.782523	235	Không đổi trạng thái món nào	2026-10-10 06:01:11.024792	Service	2026-10-10 05:56:11.028963	\N	\N	f	\N
897	CoffeeStatusBackgroundService	2026-10-10 06:01:11.136741	284	Không đổi trạng thái món nào	2026-10-10 06:06:11.426687	Service	2026-10-10 06:01:11.430686	\N	\N	f	\N
898	CoffeeStatusBackgroundService	2026-10-10 06:06:11.541003	291	Không đổi trạng thái món nào	2026-10-10 06:11:11.835584	Service	2026-10-10 06:06:11.837583	\N	\N	f	\N
899	CoffeeStatusBackgroundService	2026-10-10 06:11:11.948793	304	Không đổi trạng thái món nào	2026-10-10 06:16:12.258425	Service	2026-10-10 06:11:12.261432	\N	\N	f	\N
900	CoffeeStatusBackgroundService	2026-10-10 06:16:13.47538	298	Không đổi trạng thái món nào	2026-10-10 06:21:13.779596	Service	2026-10-10 06:16:13.790605	\N	\N	f	\N
901	CoffeeStatusBackgroundService	2026-10-10 06:21:13.915424	374	Không đổi trạng thái món nào	2026-10-10 06:26:14.295226	Service	2026-10-10 06:21:14.299192	\N	\N	f	\N
902	CoffeeStatusBackgroundService	2026-10-10 06:26:14.416689	280	Không đổi trạng thái món nào	2026-10-10 06:31:14.702168	Service	2026-10-10 06:26:14.70717	\N	\N	f	\N
903	CoffeeStatusBackgroundService	2026-10-10 06:31:14.831653	236	Không đổi trạng thái món nào	2026-10-10 06:36:15.075058	Service	2026-10-10 06:31:15.080065	\N	\N	f	\N
904	CoffeeStatusBackgroundService	2026-10-10 06:36:15.180703	246	Không đổi trạng thái món nào	2026-10-10 06:41:15.434767	Service	2026-10-10 06:36:15.443102	\N	\N	f	\N
905	CoffeeStatusBackgroundService	2026-10-10 06:41:15.559949	245	Không đổi trạng thái món nào	2026-10-10 06:46:15.814526	Service	2026-10-10 06:41:15.820536	\N	\N	f	\N
906	CoffeeStatusBackgroundService	2026-10-10 06:46:15.9125	252	Không đổi trạng thái món nào	2026-10-10 06:51:16.165036	Service	2026-10-10 06:46:16.168614	\N	\N	f	\N
907	CoffeeStatusBackgroundService	2026-10-10 06:51:16.266085	295	Không đổi trạng thái món nào	2026-10-10 06:56:16.592044	Service	2026-10-10 06:51:16.594048	\N	\N	f	\N
908	CoffeeStatusBackgroundService	2026-10-10 06:56:16.753878	251	Không đổi trạng thái món nào	2026-10-10 07:01:17.015598	Service	2026-10-10 06:56:17.020622	\N	\N	f	\N
909	CoffeeStatusBackgroundService	2026-10-10 07:01:17.11948	220	Không đổi trạng thái món nào	2026-10-10 07:06:17.342646	Service	2026-10-10 07:01:17.344644	\N	\N	f	\N
910	CoffeeStatusBackgroundService	2026-10-10 07:06:17.452646	405	Không đổi trạng thái món nào	2026-10-10 07:11:17.865047	Service	2026-10-10 07:06:17.867191	\N	\N	f	\N
911	CoffeeStatusBackgroundService	2026-10-10 07:11:17.960276	249	Không đổi trạng thái món nào	2026-10-10 07:16:18.219786	Service	2026-10-10 07:11:18.222785	\N	\N	f	\N
912	CoffeeStatusBackgroundService	2026-10-10 07:16:18.317907	236	Không đổi trạng thái món nào	2026-10-10 07:21:18.554868	Service	2026-10-10 07:16:18.556846	\N	\N	f	\N
913	CoffeeStatusBackgroundService	2026-10-10 07:21:18.664038	267	Không đổi trạng thái món nào	2026-10-10 07:26:18.944071	Service	2026-10-10 07:21:18.961049	\N	\N	f	\N
914	CoffeeStatusBackgroundService	2026-10-10 07:26:19.121527	285	Không đổi trạng thái món nào	2026-10-10 07:31:19.411902	Service	2026-10-10 07:26:19.417904	\N	\N	f	\N
915	CoffeeStatusBackgroundService	2026-10-10 07:31:19.573302	284	Không đổi trạng thái món nào	2026-10-10 07:36:19.863732	Service	2026-10-10 07:31:19.866816	\N	\N	f	\N
916	CoffeeStatusBackgroundService	2026-10-10 07:36:19.98543	286	Không đổi trạng thái món nào	2026-10-10 07:41:20.287846	Service	2026-10-10 07:36:20.300061	\N	\N	f	\N
917	CoffeeStatusBackgroundService	2026-10-10 07:41:20.466015	288	Không đổi trạng thái món nào	2026-10-10 07:46:20.765709	Service	2026-10-10 07:41:20.772406	\N	\N	f	\N
918	CoffeeStatusBackgroundService	2026-10-10 07:46:20.881125	456	Không đổi trạng thái món nào	2026-10-10 07:51:21.339184	Service	2026-10-10 07:46:21.344201	\N	\N	f	\N
919	CoffeeStatusBackgroundService	2026-10-10 07:51:21.462671	288	Không đổi trạng thái món nào	2026-10-10 07:56:21.755128	Service	2026-10-10 07:51:21.76071	\N	\N	f	\N
920	CoffeeStatusBackgroundService	2026-10-10 07:56:21.879887	271	Không đổi trạng thái món nào	2026-10-10 08:01:22.156068	Service	2026-10-10 07:56:22.1601	\N	\N	f	\N
921	CoffeeStatusBackgroundService	2026-10-10 08:01:22.276332	325	Không đổi trạng thái món nào	2026-10-10 08:06:22.605597	Service	2026-10-10 08:01:22.609647	\N	\N	f	\N
922	CoffeeStatusBackgroundService	2026-10-10 08:06:22.72892	306	Không đổi trạng thái món nào	2026-10-10 08:11:23.040809	Service	2026-10-10 08:06:23.04633	\N	\N	f	\N
923	CoffeeStatusBackgroundService	2026-10-10 08:11:23.161681	312	Không đổi trạng thái món nào	2026-10-10 08:16:23.481872	Service	2026-10-10 08:11:23.488901	\N	\N	f	\N
924	CoffeeStatusBackgroundService	2026-10-10 08:16:23.620419	318	Không đổi trạng thái món nào	2026-10-10 08:21:23.938855	Service	2026-10-10 08:16:23.940854	\N	\N	f	\N
925	CoffeeStatusBackgroundService	2026-10-10 08:21:24.054495	273	Không đổi trạng thái món nào	2026-10-10 08:26:24.333642	Service	2026-10-10 08:21:24.338641	\N	\N	f	\N
926	CoffeeStatusBackgroundService	2026-10-10 08:26:24.468897	266	Không đổi trạng thái món nào	2026-10-10 08:31:24.741983	Service	2026-10-10 08:26:24.751286	\N	\N	f	\N
927	CoffeeStatusBackgroundService	2026-10-10 08:31:24.927507	338	Không đổi trạng thái món nào	2026-10-10 08:36:25.273541	Service	2026-10-10 08:31:25.275538	\N	\N	f	\N
928	CoffeeStatusBackgroundService	2026-10-10 08:36:25.379844	243	Không đổi trạng thái món nào	2026-10-10 08:41:25.627048	Service	2026-10-10 08:36:25.632025	\N	\N	f	\N
929	CoffeeStatusBackgroundService	2026-10-10 08:41:25.731131	247	Không đổi trạng thái món nào	2026-10-10 08:46:25.991695	Service	2026-10-10 08:41:26.003256	\N	\N	f	\N
930	CoffeeStatusBackgroundService	2026-10-10 08:46:26.096587	238	Không đổi trạng thái món nào	2026-10-10 08:51:26.334897	Service	2026-10-10 08:46:26.345168	\N	\N	f	\N
931	CoffeeStatusBackgroundService	2026-10-10 08:51:26.452212	300	Không đổi trạng thái món nào	2026-10-10 08:56:26.759351	Service	2026-10-10 08:51:26.762901	\N	\N	f	\N
932	CoffeeStatusBackgroundService	2026-10-10 08:56:26.88473	290	Không đổi trạng thái món nào	2026-10-10 09:01:27.181337	Service	2026-10-10 08:56:27.18433	\N	\N	f	\N
933	CoffeeStatusBackgroundService	2026-10-10 09:01:27.293226	276	Không đổi trạng thái món nào	2026-10-10 09:06:27.57966	Service	2026-10-10 09:01:27.583662	\N	\N	f	\N
934	CoffeeStatusBackgroundService	2026-10-10 09:06:27.696782	302	Không đổi trạng thái món nào	2026-10-10 09:11:28.005519	Service	2026-10-10 09:06:28.007528	\N	\N	f	\N
935	CoffeeStatusBackgroundService	2026-10-10 09:11:28.143088	411	Không đổi trạng thái món nào	2026-10-10 09:16:28.568036	Service	2026-10-10 09:11:28.577051	\N	\N	f	\N
936	CoffeeStatusBackgroundService	2026-10-10 09:16:28.754655	264	Không đổi trạng thái món nào	2026-10-10 09:21:29.02522	Service	2026-10-10 09:16:29.029217	\N	\N	f	\N
937	CoffeeStatusBackgroundService	2026-10-10 09:21:29.142061	244	Không đổi trạng thái món nào	2026-10-10 09:26:29.392768	Service	2026-10-10 09:21:29.396767	\N	\N	f	\N
938	CoffeeStatusBackgroundService	2026-10-10 09:26:29.511971	235	Không đổi trạng thái món nào	2026-10-10 09:31:29.753941	Service	2026-10-10 09:26:29.757575	\N	\N	f	\N
939	CoffeeStatusBackgroundService	2026-10-10 09:31:29.860921	278	Không đổi trạng thái món nào	2026-10-10 09:36:30.145903	Service	2026-10-10 09:31:30.148201	\N	\N	f	\N
940	CoffeeStatusBackgroundService	2026-10-10 09:36:30.240696	261	Không đổi trạng thái món nào	2026-10-10 09:41:30.508367	Service	2026-10-10 09:36:30.513386	\N	\N	f	\N
941	CoffeeStatusBackgroundService	2026-10-10 09:41:30.60824	266	Không đổi trạng thái món nào	2026-10-10 09:46:30.883602	Service	2026-10-10 09:41:30.885603	\N	\N	f	\N
942	CoffeeStatusBackgroundService	2026-10-10 09:46:31.005799	304	Không đổi trạng thái món nào	2026-10-10 09:51:31.316176	Service	2026-10-10 09:46:31.326326	\N	\N	f	\N
943	CoffeeStatusBackgroundService	2026-10-10 09:51:31.461287	360	Không đổi trạng thái món nào	2026-10-10 09:56:31.82674	Service	2026-10-10 09:51:31.83078	\N	\N	f	\N
944	CoffeeStatusBackgroundService	2026-10-10 09:56:31.996917	422	Không đổi trạng thái món nào	2026-10-10 10:01:32.430445	Service	2026-10-10 09:56:32.434443	\N	\N	f	\N
945	CoffeeStatusBackgroundService	2026-10-10 10:01:32.552741	343	Không đổi trạng thái món nào	2026-10-10 10:06:32.902357	Service	2026-10-10 10:01:32.911405	\N	\N	f	\N
946	CoffeeStatusBackgroundService	2026-10-10 10:06:33.037813	305	Không đổi trạng thái món nào	2026-10-10 10:11:33.351538	Service	2026-10-10 10:06:33.357568	\N	\N	f	\N
947	CoffeeStatusBackgroundService	2026-10-10 10:11:33.47946	294	Không đổi trạng thái món nào	2026-10-10 10:16:33.783767	Service	2026-10-10 10:11:33.787786	\N	\N	f	\N
948	CoffeeStatusBackgroundService	2026-10-10 10:16:33.903388	268	Không đổi trạng thái món nào	2026-10-10 10:21:34.173603	Service	2026-10-10 10:16:34.176111	\N	\N	f	\N
949	CoffeeStatusBackgroundService	2026-10-10 10:21:34.343477	284	Không đổi trạng thái món nào	2026-10-10 10:26:34.633841	Service	2026-10-10 10:21:34.638034	\N	\N	f	\N
950	CoffeeStatusBackgroundService	2026-10-10 10:26:34.741304	400	Không đổi trạng thái món nào	2026-10-10 10:31:35.147046	Service	2026-10-10 10:26:35.149067	\N	\N	f	\N
951	CoffeeStatusBackgroundService	2026-10-10 10:31:35.25538	303	Không đổi trạng thái món nào	2026-10-10 10:36:35.567724	Service	2026-10-10 10:31:35.572125	\N	\N	f	\N
952	CoffeeStatusBackgroundService	2026-10-10 10:36:35.688511	365	Không đổi trạng thái món nào	2026-10-10 10:41:36.064244	Service	2026-10-10 10:36:36.068249	\N	\N	f	\N
953	CoffeeStatusBackgroundService	2026-10-10 10:41:36.200998	309	Không đổi trạng thái món nào	2026-10-10 10:46:36.517298	Service	2026-10-10 10:41:36.523268	\N	\N	f	\N
954	CoffeeStatusBackgroundService	2026-10-10 10:46:36.662872	315	Không đổi trạng thái món nào	2026-10-10 10:51:36.988366	Service	2026-10-10 10:46:36.994021	\N	\N	f	\N
955	CoffeeStatusBackgroundService	2026-10-10 10:51:37.096412	286	Không đổi trạng thái món nào	2026-10-10 10:56:37.393689	Service	2026-10-10 10:51:37.40473	\N	\N	f	\N
956	CoffeeStatusBackgroundService	2026-10-10 10:56:37.554308	449	Không đổi trạng thái món nào	2026-10-10 11:01:38.010553	Service	2026-10-10 10:56:38.016525	\N	\N	f	\N
957	CoffeeStatusBackgroundService	2026-10-10 11:01:38.135464	419	Không đổi trạng thái món nào	2026-10-10 11:06:38.561226	Service	2026-10-10 11:01:38.565226	\N	\N	f	\N
958	CoffeeStatusBackgroundService	2026-10-10 11:06:38.686376	269	Không đổi trạng thái món nào	2026-10-10 11:11:38.959469	Service	2026-10-10 11:06:38.961645	\N	\N	f	\N
959	CoffeeStatusBackgroundService	2026-10-10 11:11:39.076555	286	Không đổi trạng thái món nào	2026-10-10 11:16:39.376301	Service	2026-10-10 11:11:39.384305	\N	\N	f	\N
960	CoffeeStatusBackgroundService	2026-10-10 11:16:39.527159	452	Không đổi trạng thái món nào	2026-10-10 11:21:39.986859	Service	2026-10-10 11:16:39.992867	\N	\N	f	\N
961	CoffeeStatusBackgroundService	2026-10-10 11:21:40.128087	355	Không đổi trạng thái món nào	2026-10-10 11:26:40.488117	Service	2026-10-10 11:21:40.490117	\N	\N	f	\N
962	CoffeeStatusBackgroundService	2026-10-10 11:26:40.598984	338	Không đổi trạng thái món nào	2026-10-10 11:31:40.943721	Service	2026-10-10 11:26:40.950721	\N	\N	f	\N
963	CoffeeStatusBackgroundService	2026-10-10 11:31:41.133751	339	Không đổi trạng thái món nào	2026-10-10 11:36:41.479639	Service	2026-10-10 11:31:41.507561	\N	\N	f	\N
964	CoffeeStatusBackgroundService	2026-10-10 11:36:41.659031	314	Không đổi trạng thái món nào	2026-10-10 11:41:41.977622	Service	2026-10-10 11:36:41.981161	\N	\N	f	\N
965	CoffeeStatusBackgroundService	2026-10-10 11:41:42.090654	322	Không đổi trạng thái món nào	2026-10-10 11:46:42.422316	Service	2026-10-10 11:41:42.426317	\N	\N	f	\N
966	CoffeeStatusBackgroundService	2026-10-10 11:46:42.554801	248	Không đổi trạng thái món nào	2026-10-10 11:51:42.803999	Service	2026-10-10 11:46:42.806999	\N	\N	f	\N
967	CoffeeStatusBackgroundService	2026-10-10 11:51:42.921282	358	Không đổi trạng thái món nào	2026-10-10 11:56:43.284222	Service	2026-10-10 11:51:43.287225	\N	\N	f	\N
968	CoffeeStatusBackgroundService	2026-10-10 11:56:43.397124	293	Không đổi trạng thái món nào	2026-10-10 12:01:43.693674	Service	2026-10-10 11:56:43.695864	\N	\N	f	\N
969	CoffeeStatusBackgroundService	2026-10-10 12:01:43.810683	339	Không đổi trạng thái món nào	2026-10-10 12:06:44.157446	Service	2026-10-10 12:01:44.162481	\N	\N	f	\N
970	CoffeeStatusBackgroundService	2026-10-10 12:06:44.299526	417	Không đổi trạng thái món nào	2026-10-10 12:11:44.726189	Service	2026-10-10 12:06:44.731739	\N	\N	f	\N
971	CoffeeStatusBackgroundService	2026-10-10 12:11:44.87296	323	Không đổi trạng thái món nào	2026-10-10 12:16:45.200382	Service	2026-10-10 12:11:45.20285	\N	\N	f	\N
972	CoffeeStatusBackgroundService	2026-10-10 12:16:45.315398	283	Không đổi trạng thái món nào	2026-10-10 12:21:45.613914	Service	2026-10-10 12:16:45.621938	\N	\N	f	\N
973	CoffeeStatusBackgroundService	2026-10-10 12:21:45.749645	264	Không đổi trạng thái món nào	2026-10-10 12:26:46.020751	Service	2026-10-10 12:21:46.02216	\N	\N	f	\N
974	CoffeeStatusBackgroundService	2026-10-10 12:26:46.130779	268	Không đổi trạng thái món nào	2026-10-10 12:31:46.413337	Service	2026-10-10 12:26:46.419868	\N	\N	f	\N
975	CoffeeStatusBackgroundService	2026-10-10 12:31:46.521814	274	Không đổi trạng thái món nào	2026-10-10 12:36:46.799616	Service	2026-10-10 12:31:46.806618	\N	\N	f	\N
976	CoffeeStatusBackgroundService	2026-10-10 12:36:46.90374	260	Không đổi trạng thái món nào	2026-10-10 12:41:47.175656	Service	2026-10-10 12:36:47.179763	\N	\N	f	\N
977	CoffeeStatusBackgroundService	2026-10-10 12:41:47.272311	233	Không đổi trạng thái món nào	2026-10-10 12:46:47.508817	Service	2026-10-10 12:41:47.510798	\N	\N	f	\N
978	CoffeeStatusBackgroundService	2026-10-10 12:46:47.601428	256	Không đổi trạng thái món nào	2026-10-10 12:51:47.858449	Service	2026-10-10 12:46:47.861554	\N	\N	f	\N
979	CoffeeStatusBackgroundService	2026-10-10 12:51:47.962196	285	Không đổi trạng thái món nào	2026-10-10 12:56:48.25693	Service	2026-10-10 12:51:48.262979	\N	\N	f	\N
980	CoffeeStatusBackgroundService	2026-10-10 12:56:48.406775	372	Không đổi trạng thái món nào	2026-10-10 13:01:48.786411	Service	2026-10-10 12:56:48.788562	\N	\N	f	\N
981	CoffeeStatusBackgroundService	2026-10-10 13:01:48.917729	395	Không đổi trạng thái món nào	2026-10-10 13:06:49.320264	Service	2026-10-10 13:01:49.323273	\N	\N	f	\N
982	CoffeeStatusBackgroundService	2026-10-10 13:06:49.424544	257	Không đổi trạng thái món nào	2026-10-10 13:11:49.689864	Service	2026-10-10 13:06:49.697746	\N	\N	f	\N
983	CoffeeStatusBackgroundService	2026-10-10 13:11:49.818756	259	Không đổi trạng thái món nào	2026-10-10 13:16:50.093109	Service	2026-10-10 13:11:50.09811	\N	\N	f	\N
984	CoffeeStatusBackgroundService	2026-10-10 13:16:50.21617	389	Không đổi trạng thái món nào	2026-10-10 13:21:50.609573	Service	2026-10-10 13:16:50.611572	\N	\N	f	\N
985	CoffeeStatusBackgroundService	2026-10-10 13:21:50.861904	300	Không đổi trạng thái món nào	2026-10-10 13:26:51.167366	Service	2026-10-10 13:21:51.172327	\N	\N	f	\N
986	CoffeeStatusBackgroundService	2026-10-10 13:26:51.30301	296	Không đổi trạng thái món nào	2026-10-10 13:31:51.611915	Service	2026-10-10 13:26:51.614934	\N	\N	f	\N
987	CoffeeStatusBackgroundService	2026-10-10 13:31:51.729111	412	Không đổi trạng thái món nào	2026-10-10 13:36:52.149408	Service	2026-10-10 13:31:52.155466	\N	\N	f	\N
988	CoffeeStatusBackgroundService	2026-10-10 13:36:52.263199	280	Không đổi trạng thái món nào	2026-10-10 13:41:52.546603	Service	2026-10-10 13:36:52.548604	\N	\N	f	\N
989	CoffeeStatusBackgroundService	2026-10-10 13:41:52.635572	290	Không đổi trạng thái món nào	2026-10-10 13:46:52.932983	Service	2026-10-10 13:41:52.936995	\N	\N	f	\N
990	CoffeeStatusBackgroundService	2026-10-10 13:46:53.053099	401	Không đổi trạng thái món nào	2026-10-10 13:51:53.458409	Service	2026-10-10 13:46:53.461415	\N	\N	f	\N
991	CoffeeStatusBackgroundService	2026-10-10 13:51:53.581509	350	Không đổi trạng thái món nào	2026-10-10 13:56:53.93712	Service	2026-10-10 13:51:53.941121	\N	\N	f	\N
992	CoffeeStatusBackgroundService	2026-10-10 13:56:54.071362	446	Không đổi trạng thái món nào	2026-10-10 14:01:54.52554	Service	2026-10-10 13:56:54.528569	\N	\N	f	\N
993	CoffeeStatusBackgroundService	2026-10-10 14:01:54.647645	277	Không đổi trạng thái món nào	2026-10-10 14:06:54.934075	Service	2026-10-10 14:01:54.937386	\N	\N	f	\N
994	CoffeeStatusBackgroundService	2026-10-10 14:06:55.059197	325	Không đổi trạng thái món nào	2026-10-10 14:11:55.393218	Service	2026-10-10 14:06:55.3996	\N	\N	f	\N
995	CoffeeStatusBackgroundService	2026-10-10 14:11:55.54202	290	Không đổi trạng thái món nào	2026-10-10 14:16:55.839874	Service	2026-10-10 14:11:55.843874	\N	\N	f	\N
996	CoffeeStatusBackgroundService	2026-10-10 14:16:55.978055	347	Không đổi trạng thái món nào	2026-10-10 14:21:56.33109	Service	2026-10-10 14:16:56.334091	\N	\N	f	\N
997	CoffeeStatusBackgroundService	2026-10-10 14:21:56.472905	327	Không đổi trạng thái món nào	2026-10-10 14:26:56.808786	Service	2026-10-10 14:21:56.81279	\N	\N	f	\N
998	CoffeeStatusBackgroundService	2026-10-10 14:26:56.912348	308	Không đổi trạng thái món nào	2026-10-10 14:31:57.231373	Service	2026-10-10 14:26:57.238385	\N	\N	f	\N
999	CoffeeStatusBackgroundService	2026-10-10 14:31:57.357614	313	Không đổi trạng thái món nào	2026-10-10 14:36:57.67506	Service	2026-10-10 14:31:57.67706	\N	\N	f	\N
1000	CoffeeStatusBackgroundService	2026-10-10 14:36:57.799176	282	Không đổi trạng thái món nào	2026-10-10 14:41:58.088081	Service	2026-10-10 14:36:58.092092	\N	\N	f	\N
1001	CoffeeStatusBackgroundService	2026-10-10 14:41:58.228043	369	Không đổi trạng thái món nào	2026-10-10 14:46:58.606884	Service	2026-10-10 14:41:58.612228	\N	\N	f	\N
1002	CoffeeStatusBackgroundService	2026-10-10 14:46:58.753082	318	Không đổi trạng thái món nào	2026-10-10 14:51:59.072233	Service	2026-10-10 14:46:59.075515	\N	\N	f	\N
1003	CoffeeStatusBackgroundService	2026-10-10 14:51:59.181158	414	Không đổi trạng thái món nào	2026-10-10 14:56:59.60838	Service	2026-10-10 14:51:59.610482	\N	\N	f	\N
1004	CoffeeStatusBackgroundService	2026-10-10 14:56:59.726059	259	Không đổi trạng thái món nào	2026-10-10 15:01:59.988844	Service	2026-10-10 14:56:59.992612	\N	\N	f	\N
1005	CoffeeStatusBackgroundService	2026-10-10 15:02:00.110174	394	Không đổi trạng thái món nào	2026-10-10 15:07:00.513567	Service	2026-10-10 15:02:00.519567	\N	\N	f	\N
1006	CoffeeStatusBackgroundService	2026-10-10 15:07:00.64282	462	Không đổi trạng thái món nào	2026-10-10 15:12:01.116383	Service	2026-10-10 15:07:01.123351	\N	\N	f	\N
1007	CoffeeStatusBackgroundService	2026-10-10 15:12:01.258538	336	Không đổi trạng thái món nào	2026-10-10 15:17:01.600595	Service	2026-10-10 15:12:01.609732	\N	\N	f	\N
1008	CoffeeStatusBackgroundService	2026-10-10 15:17:01.74822	378	Không đổi trạng thái món nào	2026-10-10 15:22:02.13277	Service	2026-10-10 15:17:02.134767	\N	\N	f	\N
1009	CoffeeStatusBackgroundService	2026-10-10 15:22:02.270276	314	Không đổi trạng thái món nào	2026-10-10 15:27:02.591074	Service	2026-10-10 15:22:02.594603	\N	\N	f	\N
1010	CoffeeStatusBackgroundService	2026-10-10 15:27:02.709969	326	Không đổi trạng thái món nào	2026-10-10 15:32:03.046499	Service	2026-10-10 15:27:03.0515	\N	\N	f	\N
1011	CoffeeStatusBackgroundService	2026-10-10 15:32:03.167526	341	Không đổi trạng thái món nào	2026-10-10 15:37:03.515551	Service	2026-10-10 15:32:03.521445	\N	\N	f	\N
1012	CoffeeStatusBackgroundService	2026-10-10 15:37:03.633233	328	Không đổi trạng thái món nào	2026-10-10 15:42:03.969347	Service	2026-10-10 15:37:03.974347	\N	\N	f	\N
1013	CoffeeStatusBackgroundService	2026-10-10 15:42:04.092916	312	Không đổi trạng thái món nào	2026-10-10 15:47:04.409349	Service	2026-10-10 15:42:04.413389	\N	\N	f	\N
1014	CoffeeStatusBackgroundService	2026-10-10 15:47:04.529694	329	Không đổi trạng thái món nào	2026-10-10 15:52:04.86428	Service	2026-10-10 15:47:04.867281	\N	\N	f	\N
1015	CoffeeStatusBackgroundService	2026-10-10 15:52:04.983053	305	Không đổi trạng thái món nào	2026-10-10 15:57:05.293482	Service	2026-10-10 15:52:05.295481	\N	\N	f	\N
1016	CoffeeStatusBackgroundService	2026-10-10 15:57:05.392453	283	Không đổi trạng thái món nào	2026-10-10 16:02:05.682821	Service	2026-10-10 15:57:05.687818	\N	\N	f	\N
1017	CoffeeStatusBackgroundService	2026-10-10 16:02:05.802607	322	Không đổi trạng thái món nào	2026-10-10 16:07:06.1337	Service	2026-10-10 16:02:06.136776	\N	\N	f	\N
1018	CoffeeStatusBackgroundService	2026-10-10 16:07:06.251316	315	Không đổi trạng thái món nào	2026-10-10 16:12:06.573208	Service	2026-10-10 16:07:06.576221	\N	\N	f	\N
1019	CoffeeStatusBackgroundService	2026-10-10 16:12:06.670186	320	Không đổi trạng thái món nào	2026-10-10 16:17:06.998222	Service	2026-10-10 16:12:07.003725	\N	\N	f	\N
1020	CoffeeStatusBackgroundService	2026-10-10 16:17:07.105486	334	Không đổi trạng thái món nào	2026-10-10 16:22:07.445005	Service	2026-10-10 16:17:07.448005	\N	\N	f	\N
1021	CoffeeStatusBackgroundService	2026-10-10 16:22:07.558755	280	Không đổi trạng thái món nào	2026-10-10 16:27:07.844129	Service	2026-10-10 16:22:07.848125	\N	\N	f	\N
1022	CoffeeStatusBackgroundService	2026-10-10 16:27:07.974729	361	Không đổi trạng thái món nào	2026-10-10 16:32:08.342758	Service	2026-10-10 16:27:08.350498	\N	\N	f	\N
1023	CoffeeStatusBackgroundService	2026-10-10 16:32:08.444876	249	Không đổi trạng thái món nào	2026-10-10 16:37:08.705448	Service	2026-10-10 16:32:08.70946	\N	\N	f	\N
1024	CoffeeStatusBackgroundService	2026-10-10 16:37:08.846825	458	Không đổi trạng thái món nào	2026-10-10 16:42:09.309771	Service	2026-10-10 16:37:09.311764	\N	\N	f	\N
1025	CoffeeStatusBackgroundService	2026-10-10 16:42:09.421028	328	Không đổi trạng thái món nào	2026-10-10 16:47:09.757833	Service	2026-10-10 16:42:09.762345	\N	\N	f	\N
1026	CoffeeStatusBackgroundService	2026-10-10 16:47:09.87817	301	Không đổi trạng thái món nào	2026-10-10 16:52:10.183074	Service	2026-10-10 16:47:10.185143	\N	\N	f	\N
1027	CoffeeStatusBackgroundService	2026-10-10 16:52:10.283152	324	Không đổi trạng thái món nào	2026-10-10 16:57:10.616886	Service	2026-10-10 16:52:10.620797	\N	\N	f	\N
1028	CoffeeStatusBackgroundService	2026-10-10 16:57:10.738464	323	Không đổi trạng thái món nào	2026-10-10 17:02:11.06657	Service	2026-10-10 16:57:11.070583	\N	\N	f	\N
1029	CoffeeStatusBackgroundService	2026-10-10 17:00:33.31221	253	Không đổi trạng thái món nào	\N	system	2026-10-10 17:00:33.566176	\N	\N	f	\N
1030	CoffeeStatusBackgroundService	2026-10-10 17:02:11.196762	312	Không đổi trạng thái món nào	2026-10-10 17:07:11.513704	Service	2026-10-10 17:02:11.515344	\N	\N	f	\N
1031	CoffeeStatusBackgroundService	2026-10-10 17:07:11.628116	347	Không đổi trạng thái món nào	2026-10-10 17:12:11.983315	Service	2026-10-10 17:07:11.990408	\N	\N	f	\N
1032	CoffeeStatusBackgroundService	2026-10-10 17:12:12.110279	406	Không đổi trạng thái món nào	2026-10-10 17:17:12.5259	Service	2026-10-10 17:12:12.531899	\N	\N	f	\N
1033	CoffeeStatusBackgroundService	2026-10-10 17:17:12.689157	400	Không đổi trạng thái món nào	2026-10-10 17:22:13.101072	Service	2026-10-10 17:17:13.106259	\N	\N	f	\N
1034	CoffeeStatusBackgroundService	2026-10-10 17:22:13.264203	457	Không đổi trạng thái món nào	2026-10-10 17:27:13.724798	Service	2026-10-10 17:22:13.727017	\N	\N	f	\N
1035	CoffeeStatusBackgroundService	2026-10-10 17:27:13.871549	437	Không đổi trạng thái món nào	2026-10-10 17:32:14.320085	Service	2026-10-10 17:27:14.324084	\N	\N	f	\N
1036	CoffeeStatusBackgroundService	2026-10-10 17:32:14.45351	378	Không đổi trạng thái món nào	2026-10-10 17:37:14.838486	Service	2026-10-10 17:32:14.845501	\N	\N	f	\N
1037	CoffeeStatusBackgroundService	2026-10-10 17:37:15.013139	361	Không đổi trạng thái món nào	2026-10-10 17:42:15.387331	Service	2026-10-10 17:37:15.394902	\N	\N	f	\N
1038	CoffeeStatusBackgroundService	2026-10-10 17:42:15.598941	280	Không đổi trạng thái món nào	2026-10-10 17:47:15.886686	Service	2026-10-10 17:42:15.895938	\N	\N	f	\N
1039	CoffeeStatusBackgroundService	2026-10-10 17:47:16.015914	289	Không đổi trạng thái món nào	2026-10-10 17:52:16.314335	Service	2026-10-10 17:47:16.332548	\N	\N	f	\N
1040	CoffeeStatusBackgroundService	2026-10-10 17:52:16.459687	338	Không đổi trạng thái món nào	2026-10-10 17:57:16.807062	Service	2026-10-10 17:52:16.812111	\N	\N	f	\N
1041	CoffeeStatusBackgroundService	2026-10-10 17:57:16.907337	245	Không đổi trạng thái món nào	2026-10-10 18:02:17.16222	Service	2026-10-10 17:57:17.166635	\N	\N	f	\N
1042	CoffeeStatusBackgroundService	2026-10-10 18:02:17.319492	376	Không đổi trạng thái món nào	2026-10-10 18:07:17.702355	Service	2026-10-10 18:02:17.707356	\N	\N	f	\N
1043	CoffeeStatusBackgroundService	2026-10-10 18:07:17.832392	370	Không đổi trạng thái món nào	2026-10-10 18:12:18.214984	Service	2026-10-10 18:07:18.220984	\N	\N	f	\N
1044	CoffeeStatusBackgroundService	2026-10-10 18:12:18.336536	380	Không đổi trạng thái món nào	2026-10-10 18:17:18.737713	Service	2026-10-10 18:12:18.748266	\N	\N	f	\N
1045	CoffeeStatusBackgroundService	2026-10-10 18:17:18.850793	302	Không đổi trạng thái món nào	2026-10-10 18:22:19.160582	Service	2026-10-10 18:17:19.166065	\N	\N	f	\N
1046	CoffeeStatusBackgroundService	2026-10-10 18:22:19.276636	322	Không đổi trạng thái món nào	2026-10-10 18:27:19.607421	Service	2026-10-10 18:22:19.612782	\N	\N	f	\N
1047	CoffeeStatusBackgroundService	2026-10-10 18:27:19.717842	258	Không đổi trạng thái món nào	2026-10-10 18:32:19.986315	Service	2026-10-10 18:27:19.990759	\N	\N	f	\N
1048	CoffeeStatusBackgroundService	2026-10-10 18:32:20.124592	483	Không đổi trạng thái món nào	2026-10-10 18:37:20.6159	Service	2026-10-10 18:32:20.622924	\N	\N	f	\N
1049	CoffeeStatusBackgroundService	2026-10-10 18:37:20.745177	336	Không đổi trạng thái món nào	2026-10-10 18:42:21.085491	Service	2026-10-10 18:37:21.088964	\N	\N	f	\N
1050	CoffeeStatusBackgroundService	2026-10-10 18:42:21.196769	397	Không đổi trạng thái món nào	2026-10-10 18:47:21.603055	Service	2026-10-10 18:42:21.608041	\N	\N	f	\N
1051	CoffeeStatusBackgroundService	2026-10-10 18:47:21.745096	264	Không đổi trạng thái món nào	2026-10-10 18:52:22.0125	Service	2026-10-10 18:47:22.019819	\N	\N	f	\N
1052	CoffeeStatusBackgroundService	2026-10-10 18:52:22.149814	382	Không đổi trạng thái món nào	2026-10-10 18:57:22.536036	Service	2026-10-10 18:52:22.541065	\N	\N	f	\N
1053	CoffeeStatusBackgroundService	2026-10-10 18:57:22.639545	261	Không đổi trạng thái món nào	2026-10-10 19:02:22.910069	Service	2026-10-10 18:57:22.914084	\N	\N	f	\N
1054	CoffeeStatusBackgroundService	2026-10-10 19:02:23.039997	406	Không đổi trạng thái món nào	2026-10-10 19:07:23.451524	Service	2026-10-10 19:02:23.458872	\N	\N	f	\N
1055	CoffeeStatusBackgroundService	2026-10-10 19:07:23.575932	326	Không đổi trạng thái món nào	2026-10-10 19:12:23.909518	Service	2026-10-10 19:07:23.918518	\N	\N	f	\N
1056	CoffeeStatusBackgroundService	2026-10-10 19:12:24.054214	438	Không đổi trạng thái món nào	2026-10-10 19:17:24.496301	Service	2026-10-10 19:12:24.501301	\N	\N	f	\N
1057	CoffeeStatusBackgroundService	2026-10-10 19:17:24.605685	342	Không đổi trạng thái món nào	2026-10-10 19:22:24.950206	Service	2026-10-10 19:17:24.958452	\N	\N	f	\N
1058	CoffeeStatusBackgroundService	2026-10-10 19:22:25.096695	396	Không đổi trạng thái món nào	2026-10-10 19:27:25.499234	Service	2026-10-10 19:22:25.503238	\N	\N	f	\N
1059	CoffeeStatusBackgroundService	2026-10-10 19:27:25.639474	432	Không đổi trạng thái món nào	2026-10-10 19:32:26.076204	Service	2026-10-10 19:27:26.078204	\N	\N	f	\N
1060	CoffeeStatusBackgroundService	2026-10-10 19:32:26.190904	334	Không đổi trạng thái món nào	2026-10-10 19:37:26.528633	Service	2026-10-10 19:32:26.530652	\N	\N	f	\N
1061	CoffeeStatusBackgroundService	2026-10-10 19:37:26.657748	375	Không đổi trạng thái món nào	2026-10-10 19:42:27.056522	Service	2026-10-10 19:37:27.075866	\N	\N	f	\N
1062	CoffeeStatusBackgroundService	2026-10-10 19:42:27.245655	326	Không đổi trạng thái món nào	2026-10-10 19:47:27.579995	Service	2026-10-10 19:42:27.583009	\N	\N	f	\N
1063	CoffeeStatusBackgroundService	2026-10-10 19:47:27.721272	373	Không đổi trạng thái món nào	2026-10-10 19:52:28.107089	Service	2026-10-10 19:47:28.11672	\N	\N	f	\N
1064	CoffeeStatusBackgroundService	2026-10-10 19:52:28.249664	248	Không đổi trạng thái món nào	2026-10-10 19:57:28.500824	Service	2026-10-10 19:52:28.501848	\N	\N	f	\N
1065	CoffeeStatusBackgroundService	2026-10-10 19:57:28.64491	342	Không đổi trạng thái món nào	2026-10-10 20:02:29.006014	Service	2026-10-10 19:57:29.008014	\N	\N	f	\N
1066	CoffeeStatusBackgroundService	2026-10-10 20:02:29.104727	279	Không đổi trạng thái món nào	2026-10-10 20:07:29.388443	Service	2026-10-10 20:02:29.391091	\N	\N	f	\N
1067	CoffeeStatusBackgroundService	2026-10-10 20:07:29.498721	339	Không đổi trạng thái món nào	2026-10-10 20:12:29.8525	Service	2026-10-10 20:07:29.862529	\N	\N	f	\N
1068	CoffeeStatusBackgroundService	2026-10-10 20:12:29.974887	342	Không đổi trạng thái món nào	2026-10-10 20:17:30.319461	Service	2026-10-10 20:12:30.321472	\N	\N	f	\N
1069	CoffeeStatusBackgroundService	2026-10-10 20:17:30.449286	330	Không đổi trạng thái món nào	2026-10-10 20:22:30.791623	Service	2026-10-10 20:17:30.794633	\N	\N	f	\N
1070	CoffeeStatusBackgroundService	2026-10-10 20:22:30.88984	250	Không đổi trạng thái món nào	2026-10-10 20:27:31.148425	Service	2026-10-10 20:22:31.152425	\N	\N	f	\N
1071	CoffeeStatusBackgroundService	2026-10-10 20:27:31.280984	346	Không đổi trạng thái món nào	2026-10-10 20:32:31.634517	Service	2026-10-10 20:27:31.637517	\N	\N	f	\N
1072	CoffeeStatusBackgroundService	2026-10-10 20:32:31.745231	272	Không đổi trạng thái món nào	2026-10-10 20:37:32.020682	Service	2026-10-10 20:32:32.022989	\N	\N	f	\N
1073	CoffeeStatusBackgroundService	2026-10-10 20:37:32.120479	272	Không đổi trạng thái món nào	2026-10-10 20:42:32.399276	Service	2026-10-10 20:37:32.405444	\N	\N	f	\N
1074	CoffeeStatusBackgroundService	2026-10-10 20:42:32.518375	343	Không đổi trạng thái món nào	2026-10-10 20:47:32.869572	Service	2026-10-10 20:42:32.875088	\N	\N	f	\N
1075	CoffeeStatusBackgroundService	2026-10-10 20:47:33.016405	447	Không đổi trạng thái món nào	2026-10-10 20:52:33.47033	Service	2026-10-10 20:47:33.47833	\N	\N	f	\N
1076	CoffeeStatusBackgroundService	2026-10-10 20:52:33.615628	392	Không đổi trạng thái món nào	2026-10-10 20:57:34.026951	Service	2026-10-10 20:52:34.038016	\N	\N	f	\N
1077	CoffeeStatusBackgroundService	2026-10-10 20:57:34.181818	432	Không đổi trạng thái món nào	2026-10-10 21:02:34.617528	Service	2026-10-10 20:57:34.621488	\N	\N	f	\N
1078	CoffeeStatusBackgroundService	2026-10-10 21:02:34.749034	365	Không đổi trạng thái món nào	2026-10-10 21:07:35.120727	Service	2026-10-10 21:02:35.124729	\N	\N	f	\N
1079	CoffeeStatusBackgroundService	2026-10-10 21:07:35.255475	445	Không đổi trạng thái món nào	2026-10-10 21:12:35.706436	Service	2026-10-10 21:07:35.710403	\N	\N	f	\N
1080	CoffeeStatusBackgroundService	2026-10-10 21:12:35.868885	346	Không đổi trạng thái món nào	2026-10-10 21:17:36.22352	Service	2026-10-10 21:12:36.228517	\N	\N	f	\N
1081	CoffeeStatusBackgroundService	2026-10-10 21:17:36.336429	220	Không đổi trạng thái món nào	2026-10-10 21:22:36.560146	Service	2026-10-10 21:17:36.561217	\N	\N	f	\N
1082	CoffeeStatusBackgroundService	2026-10-10 21:22:36.665793	385	Không đổi trạng thái món nào	2026-10-10 21:27:37.055295	Service	2026-10-10 21:22:37.058544	\N	\N	f	\N
1083	CoffeeStatusBackgroundService	2026-10-10 21:27:37.17125	254	Không đổi trạng thái món nào	2026-10-10 21:32:37.426582	Service	2026-10-10 21:27:37.426582	\N	\N	f	\N
1084	CoffeeStatusBackgroundService	2026-10-10 21:32:37.539099	350	Không đổi trạng thái món nào	2026-10-10 21:37:37.897269	Service	2026-10-10 21:32:37.901277	\N	\N	f	\N
1085	CoffeeStatusBackgroundService	2026-10-10 21:37:38.02566	308	Không đổi trạng thái món nào	2026-10-10 21:42:38.338859	Service	2026-10-10 21:37:38.344872	\N	\N	f	\N
1086	CoffeeStatusBackgroundService	2026-10-10 21:42:38.490012	369	Không đổi trạng thái món nào	2026-10-10 21:47:38.87008	Service	2026-10-10 21:42:38.879631	\N	\N	f	\N
1087	CoffeeStatusBackgroundService	2026-10-10 21:47:39.015206	364	Không đổi trạng thái món nào	2026-10-10 21:52:39.385976	Service	2026-10-10 21:47:39.389495	\N	\N	f	\N
1088	CoffeeStatusBackgroundService	2026-10-10 21:52:39.52234	368	Không đổi trạng thái món nào	2026-10-10 21:57:39.896385	Service	2026-10-10 21:52:39.901688	\N	\N	f	\N
1089	CoffeeStatusBackgroundService	2026-10-10 21:57:40.042964	353	Không đổi trạng thái món nào	2026-10-10 22:02:40.404055	Service	2026-10-10 21:57:40.411737	\N	\N	f	\N
1090	CoffeeStatusBackgroundService	2026-10-10 22:02:40.542085	333	Không đổi trạng thái món nào	2026-10-10 22:07:40.880608	Service	2026-10-10 22:02:40.884607	\N	\N	f	\N
1091	CoffeeStatusBackgroundService	2026-10-10 22:07:41.001078	360	Không đổi trạng thái món nào	2026-10-10 22:12:41.37113	Service	2026-10-10 22:07:41.376119	\N	\N	f	\N
1092	CoffeeStatusBackgroundService	2026-10-10 22:12:41.501003	317	Không đổi trạng thái món nào	2026-10-10 22:17:41.823175	Service	2026-10-10 22:12:41.825178	\N	\N	f	\N
1093	CoffeeStatusBackgroundService	2026-10-10 22:17:41.956096	406	Không đổi trạng thái món nào	2026-10-10 22:22:42.369421	Service	2026-10-10 22:17:42.372947	\N	\N	f	\N
1094	CoffeeStatusBackgroundService	2026-10-10 22:22:42.496484	335	Không đổi trạng thái món nào	2026-10-10 22:27:42.837319	Service	2026-10-10 22:22:42.840318	\N	\N	f	\N
1095	CoffeeStatusBackgroundService	2026-10-10 22:27:42.963958	410	Không đổi trạng thái món nào	2026-10-10 22:32:43.382128	Service	2026-10-10 22:27:43.385264	\N	\N	f	\N
1096	CoffeeStatusBackgroundService	2026-10-10 22:32:43.510447	353	Không đổi trạng thái món nào	2026-10-10 22:37:43.866839	Service	2026-10-10 22:32:43.870945	\N	\N	f	\N
1097	CoffeeStatusBackgroundService	2026-10-10 22:37:44.000079	322	Không đổi trạng thái món nào	2026-10-10 22:42:44.332306	Service	2026-10-10 22:37:44.341302	\N	\N	f	\N
1098	CoffeeStatusBackgroundService	2026-10-10 22:42:44.477193	447	Không đổi trạng thái món nào	2026-10-10 22:47:44.931524	Service	2026-10-10 22:42:44.937794	\N	\N	f	\N
1099	CoffeeStatusBackgroundService	2026-10-10 22:47:45.065029	349	Không đổi trạng thái món nào	2026-10-10 22:52:45.425428	Service	2026-10-10 22:47:45.430428	\N	\N	f	\N
1100	CoffeeStatusBackgroundService	2026-10-10 22:52:45.549353	286	Không đổi trạng thái món nào	2026-10-10 22:57:45.841774	Service	2026-10-10 22:52:45.850741	\N	\N	f	\N
1101	CoffeeStatusBackgroundService	2026-10-10 22:57:45.992394	398	Không đổi trạng thái món nào	2026-10-10 23:02:46.401094	Service	2026-10-10 22:57:46.404942	\N	\N	f	\N
1102	CoffeeStatusBackgroundService	2026-10-10 23:02:46.531138	371	Không đổi trạng thái món nào	2026-10-10 23:07:46.912242	Service	2026-10-10 23:02:46.918244	\N	\N	f	\N
1103	CoffeeStatusBackgroundService	2026-10-10 23:07:47.054904	309	Không đổi trạng thái món nào	2026-10-10 23:12:47.375205	Service	2026-10-10 23:07:47.385404	\N	\N	f	\N
1104	CoffeeStatusBackgroundService	2026-10-10 23:12:47.528707	310	Không đổi trạng thái món nào	2026-10-10 23:17:47.84283	Service	2026-10-10 23:12:47.845834	\N	\N	f	\N
1105	CoffeeStatusBackgroundService	2026-10-10 23:17:47.988674	370	Không đổi trạng thái món nào	2026-10-10 23:22:48.364799	Service	2026-10-10 23:17:48.372838	\N	\N	f	\N
1106	CoffeeStatusBackgroundService	2026-10-10 23:22:48.488682	293	Không đổi trạng thái món nào	2026-10-10 23:27:48.790581	Service	2026-10-10 23:22:48.795101	\N	\N	f	\N
1107	CoffeeStatusBackgroundService	2026-10-10 23:27:48.892346	256	Không đổi trạng thái món nào	2026-10-10 23:32:49.156356	Service	2026-10-10 23:27:49.162317	\N	\N	f	\N
1108	CoffeeStatusBackgroundService	2026-10-10 23:32:49.297912	385	Không đổi trạng thái món nào	2026-10-10 23:37:49.689747	Service	2026-10-10 23:32:49.693748	\N	\N	f	\N
1109	CoffeeStatusBackgroundService	2026-10-10 23:37:49.805888	314	Không đổi trạng thái món nào	2026-10-10 23:42:50.133923	Service	2026-10-10 23:37:50.137913	\N	\N	f	\N
1110	CoffeeStatusBackgroundService	2026-10-10 23:42:50.251736	309	Không đổi trạng thái món nào	2026-10-10 23:47:50.563726	Service	2026-10-10 23:42:50.621267	\N	\N	f	\N
1111	CoffeeStatusBackgroundService	2026-10-10 23:47:50.758076	356	Không đổi trạng thái món nào	2026-10-10 23:52:51.122107	Service	2026-10-10 23:47:51.127634	\N	\N	f	\N
1112	CoffeeStatusBackgroundService	2026-10-10 23:52:51.254527	356	Không đổi trạng thái món nào	2026-10-10 23:57:51.618429	Service	2026-10-10 23:52:51.621429	\N	\N	f	\N
1113	CoffeeStatusBackgroundService	2026-10-10 23:57:51.729424	364	Không đổi trạng thái món nào	2026-10-11 00:02:52.102881	Service	2026-10-10 23:57:52.1093	\N	\N	f	\N
1114	CoffeeStatusBackgroundService	2026-10-11 00:02:52.23816	328	Không đổi trạng thái món nào	2026-10-11 00:07:52.571239	Service	2026-10-11 00:02:52.573731	\N	\N	f	\N
1115	CoffeeStatusBackgroundService	2026-10-11 00:07:52.689576	295	Không đổi trạng thái món nào	2026-10-11 00:12:52.996481	Service	2026-10-11 00:07:53.006009	\N	\N	f	\N
1116	CoffeeStatusBackgroundService	2026-10-11 00:12:53.246947	455	Không đổi trạng thái món nào	2026-10-11 00:17:53.709844	Service	2026-10-11 00:12:53.712841	\N	\N	f	\N
1117	CoffeeStatusBackgroundService	2026-10-11 00:17:53.846972	375	Không đổi trạng thái món nào	2026-10-11 00:22:54.228499	Service	2026-10-11 00:17:54.232005	\N	\N	f	\N
1118	CoffeeStatusBackgroundService	2026-10-11 00:22:54.353395	449	Không đổi trạng thái món nào	2026-10-11 00:27:54.813211	Service	2026-10-11 00:22:54.819227	\N	\N	f	\N
1119	CoffeeStatusBackgroundService	2026-10-11 00:27:54.947734	333	Không đổi trạng thái món nào	2026-10-11 00:32:55.287688	Service	2026-10-11 00:27:55.291727	\N	\N	f	\N
1120	CoffeeStatusBackgroundService	2026-10-11 00:32:55.416536	352	Không đổi trạng thái món nào	2026-10-11 00:37:55.778539	Service	2026-10-11 00:32:55.784545	\N	\N	f	\N
1121	CoffeeStatusBackgroundService	2026-10-11 00:37:55.892149	386	Không đổi trạng thái món nào	2026-10-11 00:42:56.285118	Service	2026-10-11 00:37:56.292626	\N	\N	f	\N
1122	CoffeeStatusBackgroundService	2026-10-11 00:42:56.4465	418	Không đổi trạng thái món nào	2026-10-11 00:47:56.875881	Service	2026-10-11 00:42:56.879884	\N	\N	f	\N
1123	CoffeeStatusBackgroundService	2026-10-11 00:47:57.002023	324	Không đổi trạng thái món nào	2026-10-11 00:52:57.33317	Service	2026-10-11 00:47:57.348701	\N	\N	f	\N
1124	CoffeeStatusBackgroundService	2026-10-11 00:52:57.520799	405	Không đổi trạng thái món nào	2026-10-11 00:57:57.935499	Service	2026-10-11 00:52:57.940496	\N	\N	f	\N
1125	CoffeeStatusBackgroundService	2026-10-11 00:57:58.071883	405	Không đổi trạng thái món nào	2026-10-11 01:02:58.482577	Service	2026-10-11 00:57:58.484991	\N	\N	f	\N
1126	CoffeeStatusBackgroundService	2026-10-11 01:02:58.621343	371	Không đổi trạng thái món nào	2026-10-11 01:07:58.998837	Service	2026-10-11 01:02:59.002837	\N	\N	f	\N
1127	CoffeeStatusBackgroundService	2026-10-11 01:07:59.136817	361	Không đổi trạng thái món nào	2026-10-11 01:12:59.506621	Service	2026-10-11 01:07:59.512325	\N	\N	f	\N
1128	CoffeeStatusBackgroundService	2026-10-11 01:12:59.630511	375	Không đổi trạng thái món nào	2026-10-11 01:18:00.012515	Service	2026-10-11 01:13:00.021527	\N	\N	f	\N
1129	CoffeeStatusBackgroundService	2026-10-11 01:18:00.186731	333	Không đổi trạng thái món nào	2026-10-11 01:23:00.522958	Service	2026-10-11 01:18:00.525983	\N	\N	f	\N
1130	CoffeeStatusBackgroundService	2026-10-11 01:23:00.63258	225	Không đổi trạng thái món nào	2026-10-11 01:28:00.861006	Service	2026-10-11 01:23:00.864998	\N	\N	f	\N
1131	CoffeeStatusBackgroundService	2026-10-11 01:28:00.969585	382	Không đổi trạng thái món nào	2026-10-11 01:33:01.356853	Service	2026-10-11 01:28:01.359004	\N	\N	f	\N
1132	CoffeeStatusBackgroundService	2026-10-11 01:33:01.486158	412	Không đổi trạng thái món nào	2026-10-11 01:38:01.908817	Service	2026-10-11 01:33:01.914679	\N	\N	f	\N
1133	CoffeeStatusBackgroundService	2026-10-11 01:38:02.029407	299	Không đổi trạng thái món nào	2026-10-11 01:43:02.337093	Service	2026-10-11 01:38:02.33871	\N	\N	f	\N
1134	CoffeeStatusBackgroundService	2026-10-11 01:43:02.448552	410	Không đổi trạng thái món nào	2026-10-11 01:48:02.865305	Service	2026-10-11 01:43:02.869704	\N	\N	f	\N
1135	CoffeeStatusBackgroundService	2026-10-11 01:48:03.01353	359	Không đổi trạng thái món nào	2026-10-11 01:53:03.377694	Service	2026-10-11 01:48:03.380153	\N	\N	f	\N
1136	CoffeeStatusBackgroundService	2026-10-11 01:53:03.498154	360	Không đổi trạng thái món nào	2026-10-11 01:58:03.866592	Service	2026-10-11 01:53:03.871927	\N	\N	f	\N
1137	CoffeeStatusBackgroundService	2026-10-11 01:58:04.017292	448	Không đổi trạng thái món nào	2026-10-11 02:03:04.504989	Service	2026-10-11 01:58:04.508994	\N	\N	f	\N
1138	CoffeeStatusBackgroundService	2026-10-11 02:03:04.624076	330	Không đổi trạng thái món nào	2026-10-11 02:08:04.959393	Service	2026-10-11 02:03:04.960376	\N	\N	f	\N
1139	CoffeeStatusBackgroundService	2026-10-11 02:08:05.093838	392	Không đổi trạng thái món nào	2026-10-11 02:13:05.495752	Service	2026-10-11 02:08:05.506302	\N	\N	f	\N
1140	CoffeeStatusBackgroundService	2026-10-11 02:13:05.653402	326	Không đổi trạng thái món nào	2026-10-11 02:18:05.985191	Service	2026-10-11 02:13:05.987707	\N	\N	f	\N
1141	CoffeeStatusBackgroundService	2026-10-11 02:18:06.115741	468	Không đổi trạng thái món nào	2026-10-11 02:23:06.58906	Service	2026-10-11 02:18:06.59206	\N	\N	f	\N
1142	CoffeeStatusBackgroundService	2026-10-11 02:23:06.699512	297	Không đổi trạng thái món nào	2026-10-11 02:28:07.005076	Service	2026-10-11 02:23:07.012109	\N	\N	f	\N
1143	CoffeeStatusBackgroundService	2026-10-11 02:28:07.141929	384	Không đổi trạng thái món nào	2026-10-11 02:33:07.537682	Service	2026-10-11 02:28:07.548789	\N	\N	f	\N
1144	CoffeeStatusBackgroundService	2026-10-11 02:33:07.692616	308	Không đổi trạng thái món nào	2026-10-11 02:38:08.003831	Service	2026-10-11 02:33:08.007331	\N	\N	f	\N
1145	CoffeeStatusBackgroundService	2026-10-11 02:38:08.112871	332	Không đổi trạng thái món nào	2026-10-11 02:43:08.457741	Service	2026-10-11 02:38:08.468764	\N	\N	f	\N
1146	CoffeeStatusBackgroundService	2026-10-11 02:43:08.60133	513	Không đổi trạng thái món nào	2026-10-11 02:48:09.121834	Service	2026-10-11 02:43:09.126833	\N	\N	f	\N
1147	CoffeeStatusBackgroundService	2026-10-11 02:48:09.257056	372	Không đổi trạng thái món nào	2026-10-11 02:53:09.634928	Service	2026-10-11 02:48:09.63794	\N	\N	f	\N
1148	CoffeeStatusBackgroundService	2026-10-11 02:53:09.782352	353	Không đổi trạng thái món nào	2026-10-11 02:58:10.141887	Service	2026-10-11 02:53:10.145425	\N	\N	f	\N
1149	CoffeeStatusBackgroundService	2026-10-11 02:58:10.282862	393	Không đổi trạng thái món nào	2026-10-11 03:03:10.682996	Service	2026-10-11 02:58:10.688994	\N	\N	f	\N
1150	CoffeeStatusBackgroundService	2026-10-11 03:03:10.833146	340	Không đổi trạng thái món nào	2026-10-11 03:08:11.177728	Service	2026-10-11 03:03:11.181347	\N	\N	f	\N
1151	CoffeeStatusBackgroundService	2026-10-11 03:08:11.308637	424	Không đổi trạng thái món nào	2026-10-11 03:13:11.73785	Service	2026-10-11 03:08:11.739861	\N	\N	f	\N
1152	CoffeeStatusBackgroundService	2026-10-11 03:13:11.876764	282	Không đổi trạng thái món nào	2026-10-11 03:18:12.169283	Service	2026-10-11 03:13:12.177297	\N	\N	f	\N
1153	CoffeeStatusBackgroundService	2026-10-11 03:18:12.302124	294	Không đổi trạng thái món nào	2026-10-11 03:23:12.603015	Service	2026-10-11 03:18:12.611632	\N	\N	f	\N
1154	CoffeeStatusBackgroundService	2026-10-11 03:23:12.735713	353	Không đổi trạng thái món nào	2026-10-11 03:28:13.100628	Service	2026-10-11 03:23:13.105654	\N	\N	f	\N
1155	CoffeeStatusBackgroundService	2026-10-11 03:28:13.246856	639	Không đổi trạng thái món nào	2026-10-11 03:33:13.898162	Service	2026-10-11 03:28:13.907197	\N	\N	f	\N
1156	CoffeeStatusBackgroundService	2026-10-11 03:33:14.053548	347	Không đổi trạng thái món nào	2026-10-11 03:38:14.408113	Service	2026-10-11 03:33:14.41527	\N	\N	f	\N
1157	CoffeeStatusBackgroundService	2026-10-11 03:38:14.550017	513	Không đổi trạng thái món nào	2026-10-11 03:43:15.074181	Service	2026-10-11 03:38:15.081314	\N	\N	f	\N
1158	CoffeeStatusBackgroundService	2026-10-11 03:43:15.226712	276	Không đổi trạng thái món nào	2026-10-11 03:48:15.505468	Service	2026-10-11 03:43:15.507978	\N	\N	f	\N
1159	CoffeeStatusBackgroundService	2026-10-11 03:48:15.616467	331	Không đổi trạng thái món nào	2026-10-11 03:53:15.955961	Service	2026-10-11 03:48:15.976142	\N	\N	f	\N
1160	CoffeeStatusBackgroundService	2026-10-11 03:53:16.09684	391	Không đổi trạng thái món nào	2026-10-11 03:58:16.492707	Service	2026-10-11 03:53:16.506663	\N	\N	f	\N
1161	CoffeeStatusBackgroundService	2026-10-11 03:58:16.634186	376	Không đổi trạng thái món nào	2026-10-11 04:03:17.014342	Service	2026-10-11 03:58:17.016338	\N	\N	f	\N
1162	CoffeeStatusBackgroundService	2026-10-11 04:03:17.127694	416	Không đổi trạng thái món nào	2026-10-11 04:08:17.560184	Service	2026-10-11 04:03:17.567777	\N	\N	f	\N
1163	CoffeeStatusBackgroundService	2026-10-11 04:08:17.704196	315	Không đổi trạng thái món nào	2026-10-11 04:13:18.025823	Service	2026-10-11 04:08:18.028878	\N	\N	f	\N
1164	CoffeeStatusBackgroundService	2026-10-11 04:13:18.130009	328	Không đổi trạng thái món nào	2026-10-11 04:18:18.468045	Service	2026-10-11 04:13:18.474045	\N	\N	f	\N
1165	CoffeeStatusBackgroundService	2026-10-11 04:18:18.612323	334	Không đổi trạng thái món nào	2026-10-11 04:23:18.95024	Service	2026-10-11 04:18:18.956238	\N	\N	f	\N
1166	CoffeeStatusBackgroundService	2026-10-11 04:23:19.084759	376	Không đổi trạng thái món nào	2026-10-11 04:28:19.466248	Service	2026-10-11 04:23:19.470357	\N	\N	f	\N
\.


--
-- Data for Name: cart_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cart_items (id, cart_id, coffee_id, quantity, note, subtotal, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	1	1	6	\N	234000.00	admin	2026-10-07 09:02:02.888585	admin	2026-10-07 09:04:36.927687	f	\N
3	1	4	1	\N	52000.00	admin	2026-10-07 09:06:05.003571	guest	2026-10-07 09:06:18.655796	t	guest
2	1	2	1	\N	35000.00	admin	2026-10-07 09:02:07.554314	admin	2026-10-07 09:19:32.859212	f	\N
\.


--
-- Data for Name: carts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.carts (id, session_code, table_id, customer_id, status, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	567531f717dc43fc85773a0632dd4f9b	\N	\N	ABANDONED	admin	2026-10-07 09:02:01.592075	system	2026-10-08 10:00:00.388623	f	\N
\.


--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.categories (id, name, description, display_order, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	Cà phê Việt	Phin truyền thống, đậm vị Robusta Buôn Ma Thuột	1	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	Espresso	Pha máy từ hạt Arabica rang vừa	2	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	Cold Brew	Ủ lạnh 18 giờ, êm và ít đắng	3	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	Cà phê đặc biệt	Công thức riêng của Coffeeholic	4	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: coffees; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.coffees (id, category_id, name, image_url, price, description, status, created_by, created_at, updated_by, updated_at, del_flag, del_user, image_id, auto_sold_out) FROM stdin;
4	2	Latte	https://images.unsplash.com/photo-1541167760496-1628856ab772?w=800&q=80	52000.00	Espresso với nhiều sữa tươi đánh nóng	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	f
5	2	Cappuccino	https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=800&q=80	49000.00	Espresso, sữa nóng và lớp bọt mịn	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	f
7	2	Espresso	https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=800&q=80	35000.00	Shot espresso Arabica Cầu Đất	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	f
13	1	ZZ test ảnh (xóa)	\N	1000.00	\N	HIDDEN	admin	2026-10-07 03:33:06.546425	admin	2026-10-07 03:33:08.467726	t	admin	\N	f
3	1	Cà phê đen đá	https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&q=80	29000.00	Phin Robusta nguyên chất, đậm và thơm	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 07:50:31.355392	f	\N	\N	t
2	1	Cà phê sữa đá	https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80	35000.00	Phin Robusta cùng sữa đặc, ly cà phê quen thuộc	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 07:50:31.356896	f	\N	\N	t
1	1	Bạc xỉu	https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=800&q=80	39000.00	Nhiều sữa, ít cà phê, ngọt dịu	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 07:50:31.356896	f	\N	\N	t
8	3	Cold Brew cam sả	https://images.unsplash.com/photo-1499961024600-ad094db305cc?w=800&q=80	55000.00	Cold brew cùng cam vàng và sả tươi	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-07 07:48:26.286071	f	\N	\N	t
12	4	Cà phê muối	https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=800&q=80	45000.00	Kem muối Huế béo mặn trên nền cà phê phin	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 07:50:31.356896	f	\N	\N	t
11	4	Cà phê trứng	https://images.unsplash.com/photo-1534778101976-62847782c213?w=800&q=80	49000.00	Kem trứng đánh bông kiểu Hà Nội	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 07:50:31.356896	f	\N	\N	t
10	4	Cà phê dừa	https://images.unsplash.com/photo-1525803377221-4f6ccdaa5133?w=800&q=80	52000.00	Cốt dừa đá xay và cà phê phin	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 07:50:31.356896	f	\N	\N	t
6	2	Americano	https://images.unsplash.com/photo-1551030173-122aabc4489c?w=800&q=80	39000.00	Espresso pha loãng, thanh và nhẹ	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 17:00:31.432495	f	\N	\N	t
9	3	Cold Brew truyền thống	https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=800&q=80	45000.00	Ủ lạnh 18 giờ, hậu vị chocolate	SOLD_OUT	system	2026-10-06 06:37:15.880553	system	2026-10-08 17:00:31.432495	f	\N	\N	t
\.


--
-- Data for Name: customer_addresses; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customer_addresses (id, customer_id, label, recipient_name, recipient_phone, address, is_default, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: customer_group_members; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customer_group_members (id, group_id, customer_id, is_manual, added_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: customer_groups; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customer_groups (id, name, description, criteria_type, criteria_rule, recalculated_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (id, full_name, phone, email, loyalty_points, registered_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	Đặng Hoài Nam	0912345678	nam.dh@gmail.com	42	2026-10-06 06:37:15.880553	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	Bùi Ngọc Lan	0987654321	\N	15	2026-10-06 06:37:15.880553	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: dining_tables; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.dining_tables (id, table_no, qr_code, area, capacity, status, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	B01	tbl-b01-7f3k2m	Tầng trệt	2	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	B02	tbl-b02-9q4x1p	Tầng trệt	4	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	B03	tbl-b03-2h8n5r	Tầng trệt	4	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	B04	tbl-b04-6w1c7v	Sân vườn	6	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
5	B05	tbl-b05-3j9t4y	Sân vườn	4	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
6	B06	tbl-b06-8d2z6s	Lầu 1	2	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: feedbacks; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.feedbacks (id, customer_id, topic, category, sentiment, content, status, escalated, resolution, satisfaction, closed_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: images; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.images (id, content_type, size_bytes, data, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	image/webp	8780	\\x524946464422000057454250565038580a00000020000000af040083030049434350c8010000000001c800000000043000006d6e74725247422058595a2007e00001000100000000000061637370000000000000000000000000000000000000000000000000000000010000f6d6000100000000d32d0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000964657363000000f0000000247258595a00000114000000146758595a00000128000000146258595a0000013c00000014777470740000015000000014725452430000016400000028675452430000016400000028625452430000016400000028637072740000018c0000003c6d6c756300000000000000010000000c656e5553000000080000001c007300520047004258595a200000000000006fa2000038f50000039058595a2000000000000062990000b785000018da58595a2000000000000024a000000f840000b6cf58595a20000000000000f6d6000100000000d32d706172610000000000040000000266660000f2a700000d59000013d000000a5b00000000000000006d6c756300000000000000010000000c656e5553000000200000001c0047006f006f0067006c006500200049006e0063002e002000320030003100365650382056200000f049019d012ab00484033e5128934723a2a2212051f820700a09696ee1165f01ff41fbffcc3704fb1ffffffff4b10ffffff301ffff87ff11ffffe523807ffeee27ad5f806eeda74589c6a5f57d1ff72f5f8b6feab79aad29ee89f3a9fdf7f503dc57e9af60be73be603ceb3d2dff86f500fd8eeb4df411e98dc8e3f2a7fa9edf7fdfe438897f6af9f8ed276b8de48200c9b95aea0078b06843eb7e014047c6d439c5e22ffe58da8738bc45ffcb1b50e71789c6a15d1b50e71788bff9636b309807589ff66c59ad9255310fd350a2c55fed5cab894d08afcf7c1f215c33ff6f5f27c1206fd9631c00495b2b9030cb34402688e7a6adc197f89eaabdc1de624adefe34821fc9d48081fee1ca57893adc6312e52a7d010d0116a553411dc93ee2aa407b3b01aebb4dbefd5a23e3d03632d9ec1865927548f91edbfff8b3aa1310695e1d25a062422df55da0ce2d385323d1ce48e73ead54738d63581de59801008686f83c60978a37dc28fa46062037f3b622e022d3c2c4682145223c9bb7001256cae40c32cd1009a3986d662a1881b0660021537dab33b9b1d36f2bf62edefc5685ba0732b2b9822bf2c2a6a0ac2b1b1020310a2c57fee7d59fe352376f54e29fcce838c17dfa40b75421cc58c559e44517c922f2938fffc37bc7a69602d351540273451871bc88a3fb1cccc06c36aadfafa6f7e98029472213b0a37dc28fa45d1b5104472313e064abe16a917e5b599c38081d195c7acc2a55323d2ca3391ead0e806ff5f7709a3c3b255b44e5c8bffdf778fc7a759d74ec35103fb75453c8a02fefb04946e6a9cb79208ff5cdbd5c0d3ecb946c5abb95f1949fb51236be2a0d8a5645010791601bfe3c900eb11fedf55da0cde9337407a4af7a2ae2817fdf96cde48b7d7d37bf4c014a39109d851bee147d22e8da882239189f03255f0b548bf2daccdf1d0e7796124744e578379225093f4748afcb2e3e753991af96f455c440d7559a45ce36759e20be4bb7df988c522cb02fefb04946e6a9cb79208ff5cdbd5c0d3ecb946c5abb95f1677da896609fe1bc90a9a801c9bea1d3707750ed1af5f845599bea804e68a30e3791147aa81cc47b57dd139f1602388835ff1e070ffb572ae2533ff9637d967007ca0a94dbfbf3639e9ac29f96e7d5917598513fa4f4b28ce47ab7ae8df623acb06a851de4451ea9746fb11d570f7ca4a4802e71685515761d14dedb45341b07c5fd91f24b02f402bd9491c0de577eaad66cf5eb6785854ff4a9b257bceeab3601bfe3c900eb11fedf55da01615ed513f53e5df3c19800d04aad03718ba1e2dcac2b88eaacb6d1229bbd0f2cc52e8daf8c4bca5cdf93efd32aa55247395ad5778a66495480f476a109b7dec69e3100dfebeee12cf181d172330c8bffdf778c27a924d5ffd0aa10e6216b40bd62631955dd2ea8a220219f16bdcd5a710896601a83875c90c998b1b19eba31a50000c0756e06f2c14002f9614af7cb79dda01615ed513f53e5df3c19800d04aad03718b9ff70dffb78b5770820c5f55affdf9b1ce2f28027797b4c5955a91fa1f212e4d111a086b77df0d406370ad2a99ff70d3257734e49fa2c4058321ec2b0cf56c95025a749b340ea6895ecaa688ba90882a53f9c206cd4016e2a8e168efaaf605a410906c8008c2c866ac054666192e45ffefbbc613d4926afff1d62c63429ed8522b58ededeb7ad169dd5b3ac02eee1b1e138d841eaeadc5ba9dd4912d750202bf0a40169dcfb524dbfc430ec3d57b00809e3de5c9d1f82ca41f5c3af0f384175942c7baf61afe5c2dcaae46fe3b83535d835c26b92824f418b1b19eba31a504027a29a0164bc4e60176d45d523c0004dd695e195716705e5208b4d7662e218c23a121e78985d07d2d0ba59294902ffee429ebe1e6aaad9ca560b4b77168846d9a10e32fab4d2bff819051b6319e4354ad5693e7b9e31652da2ada272fd417e8e5659c4a27fd14d6c903753bd2977420cc5240ba682e8ccef1b4bc750aee7efc64ebe14900b36be903b4a0ad13dee4655116c8ec4b697257b0ccb3d4891eb8ceb6965a041e230a5762e52096aa5afd3b5de5434f60e46e817cf6d56423894cffe968ab58a3ba413027ffefbea8a73798f25b6a5c9fa9205d34ebd1f4d0d4584b44e8065e008752292245cb1e5d275f46f3f3b1f61232b207abc05da4daf784206c85bf8e3ac598bc4595e9f71a4a34afdbcdf88e45a6bac5a5dbbaf76b95853a55ac1c4dbf11a5d5400eb6aae8df623aa81fdc2eadf14100d41c35c2b549242995535d91d5c99daa00e23585a91396ddee59315001b2d2ea597cb38a60da3d93da3402789ba3757971c8897e4339e42456f84bda3a5259aad7082ff0d1ead4612228a7dc9a4f863712cac6c0fbc36adff7e52328f9336e65f82974883c5d5011e3a4f2cac392e21af450ec997be67f6bbd8e1b491b86cf2280864611b06c8a2796f0accee4f56f9d06b77c34caa5b4caa9545e2c62aec4e24977d722f55c8e705308c0e7b450ca680592f166889be3fd2a45aff39822ecfd59e0d077ba8386baacd22e718aa9b8282894ff7afba068be46e12222524222f5100178bf1e4d019bb7aa73a89c3a0e3068728d7fa9ff7e52328f933715e98b7540062ccffe950346b36f60b7579ff917fa3ca47c3c6854f961453608f5556f6cd195d44280a4f5f55affdf9b1ce2f2885527168b8f75c8bd57239c14c23039e4539b9aa457e67ae8db8ff4a916bfce608bb3f59a2ffa2e4661917ffbeef1796cb6d4ba18806a0e1b2501e963c81dc990353a0734779c49e3479a990b2fc5d65f3e4df18521bf98b0a9f7c3ce19a77452db500e0363239c908bafc22accdf53c2b0a280872280864611b06c8a037b4923b7e4f61881d555be1a6552da6554aa2f13980c41b5b57e96e92abaa02cbe64c0bd7dc7473baacd809b63a6fc5cd0c0e62361292f0d7179f256d139722fff7dde2f2d96da9743100d41c364a03dcb4ac555dd2ea8a220219f16bdcd5a329f0bb3fbe741c60d0e51aff53fefca4651f266e2bd074ec7b48928b00eb11fedf55da016144fe93d257bd15711052e7391f24eb0bd4954c27c7998a6a910ba106b06821add50018b5cd0872215f0b548bf31fedea03c00a8ccf2ee46c2a8e891bee3b94d7fdf965c7cea73235f2de8ab8881aeab348b9c6291eadf03988b6f23148000301d616cf23d9ff125635e2ef3badca1f65ca362d5dcaf8b3bfbfdd2e29fcd037154ef96e47b6a00081175404a068d66dec16eaf3ff22ff47948f878d0a9f2c28a0870893aded9a32b91ed4bb8d7451b231e6a7fcb220f2c5ea7910af85aa45f96ebb13dd9e6e542c0248e891bec8c232077d60f32f13969ac8f5de9ff45c8cc322fff7dde2f2d9fb789ba8d7f93ce80219b257bcf06822b5714440433e2d7b9ab4653e1767f7ce838c175d1f91a2bf291947c99b8af41d3b1e6b4cbffcbc6a018df9b4a6e8ae11d9f9a3ca47c3c68457e5b34986158ea09f8d6220755725574b1b243299ffcb1becb3803e5054a6dfdf9b1cf4d614fcb74a706821affbf33d748882d73700eafff7dde3792cb735059347aa5d1bec4755c3df29292015ce71578bcf934071bc6a5533fe8df647c92c0bd00af6524703795dfaab59b3d7ad9e16153ef879c334ee8a5b6e6554e78b78d81e7c8441add526e8740fbb5ecf3ee2ac738beafa33b7af95c8db455806ba9fe955c68dac620000fe4b59ffd2cb5cae105bffc54d99ea123d9ab6ee7cd0a257f45f0f0ddcb8df4b32a8845a01f414bd47d2e8cb41e8760d520a147dc2e3a5b58d493764ca45ca4c2025818fd3bed01a7a28755e43e17456e39af012fb89601db1149427b62557e389d53bcbd302431b051ae1b2776cc47040c866d0a6890c3a4b655873ae87a347d432457471ca5ec89ca5e87dedab7fa87c652123044dc0247b31f6ee03bed48f7d9f0b49b39fd4f82f17b7b26d6443739c23b4f6e2c319600f1d2b7968c05beb27025351a8697125dbc6c0e5663d3337af23eb4ed31c7865b889ae73c3795a5da35927772f4c1b8c2c305ca688aaa0fccd0becee15e0cc79b7bdddbfd9fbe9c9c9422c05b8a90b1ff724ea654fc1d7f6c49a3165b58ef9de9bf9132209d925b61d867a91d5efaa8c92419db7ab156a5c690bf9ebe27329063008629ffac5e8efea2a3a57227dceb1e8316949dd902454d8369f7769630d42143cd134d4ba395a102dbfcc0e11f3d8707dc013d506874f62b039397145c7292cc5469c14f84b6c219633a72187948007d8f993b85499145724a00e350036e9ae691f2fba2c01b947442d1d299c307933be2bc198f35d8165841cb4df26497843fc5cd2feeda65e01a6717c71d7f6c6abc4d8e405e30994b0d11108fef1d64a3e6c5f8c819b5f7df086cc3f00c5029ed428c83cb32fdde77a290630d630f81cd96d46e2287c777e468f144f0ca76c9f129d0cc7d13b5212061772879a3e68842d93662f89a05b7f981d49628670ed285cc16278ca42fe3f24fa2e394722f9f286728c8604dbaa361430f28fdc9fb60676f43f6e226b9f4a830dc35e02552b32f108ae0f42ecc58a034a670c2994ef8af0663cd7605961072d37c994d5c45ad00560d7cffdc375e41573e7b5e46fb811089a93a29330a280177adb35cda51bfe6d99138ad0e1bae70d06def76d651ef24b38c81bf9e937f8f22ba3b0bee1d985da10b69313c777e468f1423669020a01bf8663e89da909030bb943b31ff344216c9b31c65ac28a05815bfcc0ea4b1433876942e60b13c656f0c588fd8dada0b917cf943394643026dd51b0a187948046cc8f528369b58628de4d14572c660a28b35927754b4ea4fa0d92f7eb961dc930d6d96c7e0fdcf32970c5b81331432ae4e4b911adad64e7bf9cf42c60b9f1700b54e66135e46fb811089a93a29330a280177adb35c664947cd8bf190336befbe10dd87e1b77a623b93a603778a8323ea2af9f0e86781ac61f039b2da8dc0dd28f4613ea09014bfb36968ac2443d18193b1b93eb31f8dfff5daef30ed06414c01f4b45dbec9aa119663ae2dfc9c919078c92779fa191d4929fef44935004b6a325268270f291ceb89c5800702976f246e8b199685e8058ce834324c2ed09d49f41b25efd72c3b9261adb2d8fc1fb9e652e18b7026628655c8f5b95f18035073745b6626ce3613a202551d6341aebf1c546461a471ee97c913844052814e76630054e75fe03396fba597347b446980ddded87a4e011747071ca52af83c614e4d151cd1ab025fb0297f66d2d1584887a3032763727d663f1bffebcd85392316b6b9a61c982c6fd16b371cdce1730589e32b7862c47ec6d6d05c8be7ca19ca3218136ea8d850c3ca473ab3ee079bcdd9379c9ec5c361d22c768c03f87e86d51e9b1c44a60d0ee4986b6cb63f07ee7994b862dc0998a195723d6e8e6b57835073745cec910fa5be4b2c8dbca64a4b0869b8bec041f31f07ea47950a91b2cd95b5ccd4a7cf1ee9825713d22e8ac63ddcc13f5061864cfb8d7b07264fb6d047e9003c2c850f44977670e7278cdbca8951dbc16acc3180c70decea83bc0138ab901bb9497d56fbc1da57fad3081d57f4c45fe03396fba597347b3de7998416de094b64b21b1868ef53654f3817baf93f6637a5b7131811607e06a87b146753d51ff87d9ab5efdf18b83e5a6dfa03ea07d8c6914a3ce11dfedde3548db1dfc63ee9b319d950f488cb5eec77c64fdfa74ab66af166706d4c5997d0e27467b9657ecf33cdae7672353d3f4e7e5005adfd96148b2fdfa02a7a845fbd4c1fce496ddb6c23ae296fbaa240beb42c423752c3014bfb3698e24a876beb956329ca01196828dcc0b18801c4fca5ebda8d9050fedca840c43aca5e3bbb0cfb4307ef510dce846302d5958b577c50dd6641acade0e9be5bd2eca18f7caf4e0eb454a89306de2964995264557e788c020e9b03378ad4c8e8128f2f05eb014b4c981271687fdfaa7b42e2f30eed4171d363b60b77153cf6ef4d501f083282fb424c86687443ec495d804ff62982fd896b9cabd26740e01eb60a828cfc7723a288b5d03e729b52e2dc1140e45738bea66662857818cd265d7a6ed4753f0efffd90bcf58753e89c5d46b88c6cd50e9f6b549694510fd18193b1b93eb31f8dfff5e6c29c918b5b4de8ba5eb04f0659f1723b77901396cbc2a204773ab461b59975a73056f8921ffe383e472a3f11bf1be5937f4b1ccb77652493c6401e0778b4382cd474276981f8699ebcba856d528d0474d61761d956b0bc91d18e9a3bcc5cd13bce88ebc9884aae21f99515efe389a9c301953e4c15c9f4990a8cfa2940ef5b0a62048e100164f8e3c38890b66887837a136873531936d5cd2137a8b0ac22de1a8e37c77aa66f9139f115f453538885a7f0484bad95e7e14ed0004567b0cbb94c422e205688a2e2d6a250ea00e0015807d9859872351cda098b24a2c97cc0a450df649c7814212760c08c637ad84b5ba892d8dcc88d0967d6aceca5e6e795ac6739cfda9989ee8bd7d7f12a5ed3764fac2e1ae192aae094059c6afa35bfd0469de53715cc07b856df6c43b10a6a640ce0f72079ffcb3d2e83dbef5c288a3fe9000795362425c169a3ec7b5ce289bb8772104579c9b7bcf049f4dca89092ac4c400e633553af4b0663e41b00f496f0f1e11c9f72caf73126d41fe87fe4f98739a2c312953c4e8d6a7e9bf14b6cbd3a0c169680309b28574b9eeba0017f0cfb1e90667e40b897b2d5db195e8c80d27f14ffaeea9cc2d884c72ee809f7c7fd72d9a0d8040c4e18e4f48244832c793ea8d0df75c7cc9875a37fe4ea5df71881f7a736681d6fb7c496ff6a9e69dd2fb4120ab65113ae4ab883ecf8ac02e1c8329c87d857183b6f40bfb7d166d694c8870226f6e8daf0520c1d04c31b03892afdf4d2603f9c843d7dce41463793d450f42fcc13782d650a16c69f8fdd396e091db9ec0b7626fdc0f379bb0c591386d52ecad2a06864985da13a93e834135020d1543e50efa33e2604ef214dab4f5434ed9ba3cfd93f95f69d09e679e4513cfc4059f5ee9ddddece1243f80e3f85c984c59b7219da32b19aceeb4807e63cda79b01fc30104d6ebc85dd01968478d700a61c89b0de863fa90414f4b965306d4077345944d6196ee74ae2633a3fb0da059cf95999562736c3b2fc0e832d84984ff5366b8bf666b1a98c3067c92880420ea7bdfed17af1a20745ed64b55a01d3882efbb70003198474ea5f9c5b0f0e8bad6a04e86144d1cb4166e5d33438c904697cd1d13b91fafd1e2ee3b8f9b7baf16ff2ae68174143ab7bd8994bdb3d0765c7b1bea0569108470bd8faa1bbd6374005f73d52c9bcd206170d9389a976dc1c358f9aac7f80e8b298fe4defa3296bb485ffea830c76d2a77895c2fed8ae8004c1f7766b300d3110d7a077056253405e88e2e72bfe9ab58edc8706d0d29dd3e80f1eeb50da3b0085a2c42622116e9538582b2313443de2afdcde253687bfcc7df0929955f7b63891534883a7207ea9c3f2f27bd33e730344cf08298e7fd39abcba46357d609edc9d6d3403fc37460e9f122dcdbbd2c814493084806c9daf8e5b989f5efeef9f3bdd55fd11a03b46e202c08f18dbc00bffe4983d0a5a1c13a8f972cb07f9c813ea4b77b24b9c61d9b2ff4a5e70d90e566cf519f16c103236108fd25d6b9c1d8ed123c6039fcf6f39b9cb4943c47caef77f37fc334e3fceda63e6767e0cb78ae7d59742751c17e5acd37c640920f07355e8741ab938d685504f51816845ab47fec0ca58abc6abb04e46c75eb13ffddd98a9f74e6382f6badbf1624a04f532dfd381ab90708acf5c52a2f119c7c7db40518e8e5efef4a80241ad00f275fce79966cbf43ece880954758d06bafe5b08e1f2b243999dffe16ff9184220bcc06ede26bf9433ba1fda14710ce5f11e63edd797f6af2636601ed46d84e49acfa9a548d3ddb205ea95736bd27201b84d22a183343f430df4edff129d5d44fb328e55222537dca42c050940f753489d8f4ba954047c029bdf0ea10e03790b341ca4f064031a81abd32f14d0ab4d5e14a5a2deed34c95c4c4250ddc241cdc1d5732df0cd8d2799afa318937819e8609e89c44213d8e8d10482532a83cf09966cf10919ff5f47b976d187f9f53f07125eae99caf2477e5c3062c148b300f836ce316665b2b01c6dcbae49714d9d0c42351634600caed0923018039a73bbaceb9600b35e4e15648dbe7bf20af827f2eafe9d9e9079db387f4a561378261c7c2659cc762b0c978ce9abf8cf467f22282c25d0f9035ed1de12b1f8cb4631707dea656e871756c0e8d0df32c3815f8f11bf94c97c0cec28f1542ba6b89b8ad793c395d6acdc2c284a0cc3a23e998136280619789c03aed5cee295ed59f321e7169f7cc114ed4a2d01b81ef473cc98f58af1809351275c6bcde52aeacb5077f5d501ea713d053ec22e07371061bb007abc8caa698c01d533f2d0ac31e7921dd9b208d9ef40b859cd5f002e9b1c174d1729328d25ebe9e100a3855238585cf5634c88fe90569eabdfce4b6267bed2ba05be1f6d0f4a238d15d4b362111e37f9a009e870086060e6cb6a37037032d2bddfe01e2438f379aebf5722f784005dd090f24b5085ed4ef7b62f09cbd1696e801e45e0ddbedbbc8c1bc9c445a5c8b8aee954cdc4ff3c460b2d4e08e9d182e3ff2a5bb5d2fbb6c8a04ee25e54403b6cb41fc0be25f6ce437242b99e81b599f4da0bda60e06d84c96f0ada875d80b185465fb4d792b301a49d438aef40f5836fc3be7eebebed77159b65d629786a37f0b71c36df70a79b464a70bf61e9bc89bf552562f33b33f19226e476c66c0415528dc764bcf03543cd4bd510b41d31967d2fba963028a06c34fa7b30329de329bf8b40ff13f0f623c0e9c7f6bf024a3085da609db1e01bb68eea60efaca98fbee8edd449bcccb7e1946499233bd43ee6ad4cc8920b20b6c9d017f66c90328325ec99c14ce9155623f67cf2045fd1fd7dd2327277a0a526bb4d8aeff25e05eb13f6e7cbf0968b209c95b0af61cbce06ebaf8cb36a57d5e649409bdc06540c03187a354d520aa58e6c64ae9152d2badd5ac1acd0d43f24749aec79f4dcfca7009f943f9602c24214ad00e864dbf69fb1f493e9a43e06ac097fffc69ada2c71870080a6771acc7f10730f306e9eb57879f50759ac980c7de162b1e16bc340607d186aa88ded2a66642cd2c0bfe3ed0ece0fe5e25ad0f4d560cb3d2fb269b67ad061c0d1134dfb51247061970c658dcacbcbc3682ef93405b9e40f27f4502131f807d06cbe87381106db1b08a72899ade8961c6d75b3915018b221edee2b0f4a8e711f837993dc9ba66e993b2b13f8e24b278384c0fc9291fd716fe4e48c83e1f62c4a430cbabd57bd23cb1a0811b4bfc9848c55f3a606761c86810af6389d5d031745c5ba431fab9589eba90eb17ba9da27a37339a0c0cc86e74d70960b12d672a7e9da0bbfd2700e51302c11085facf30344d2eeaa0209049c38753068f8ddc410f1838a5e7cc6419819de5d9b6bdeff03410eac1e9c596771e546c3e6fa90103b70919cba3c97be29d0d3ef466624ec8e7cf1c74d10fd659b9255614810415efb649fe0f26d5a029eb8118e23914d52acb4d8b21712d852b7f068a3f61cdcc0b60ee9dd8a4bab3f6525ee564f38088b854bd7ca8b86cbe65ad575915612fc1dd237fd4d34b7e8fb3b7c34635b3361be319ebbd0486bb45c497d087cd19d8762b3da14f9b41df62403626cf770c434c4477cb815197a89bc9cedb234d623ffd69a620e650ea14659eb1ee093824d7f52605c2bf441694fac8d184ce7e866d27e14545009542f8c000c2efaebdad79dcb220fcd74d2c8352ad333728f31cecf41d503f7f6fd78e396ed5f4925847df59a3b709c9cd47e7d0c9ff086b8b14f028bbb4601fc3f436a8f4d8e226e6094ea63a1298720a8310aec9db2c5687a00eb6179227d02ad20e8ae66ed67ff943d53a033661ad50e736363ef2c4c2e8849d5a319e8045fdffd77e9bbd207f709b0623ddb33aed4b98c9d7c7e88668a8ba5311d02551d6341aebf1c522197f49f4a14c31624538d6f1f4ddfc62aa59b4b2caa2c423cf79a15ebed73b4729d12380dfb4a02181839b2da8dc0dd28f4613ea09014c2a87c6e8966bd396fbbbfcf388fc1bd993da8e8493b2b13f8e24b278384c103a10f82e60b13c656f0c588fd8de421e093e45db50d390d7a139a116c03967e34aab3cf7fb7618b2270daa2df82f77012a959978845707a17662c506625a368b82115f34adca1606817e8c91fa5326f362d1e113a6e9e659b2fd474e532b1ba15761c90e3877602dfd293db4d0e6f544a9ce405c1ed2ba05be1f6968ab0e23555de2f92af66c06ff33ef06098ffb3b2cbff26b4c8375953b01014cc3056bf0b8dc0a842936fd17bffa24497bf0b334d3b326987260b3a4fb33a6a5c50b1fa8343afaf03dee714f64d7500604bb6a1a718269477d9eab7367baa35b85e13c8646e8b1b6ba27a818f2e8c7cbee8b006e51d10b2a4431b8a4e9b67a52e18b70266286c188f76ccf12181faf49f3d5e30e19ee059bf2488307c9d1ac596d63be7602dfcb3e0df97fe9104cfe6d396e32cb2a8b108f3de68f6c906d9ff426d30baf5e1b997f3478c73ae4a1460ac172627770ca77bab6ebcab9baa6d7bfcf388fc1bd993da8e80ec28bcf25dadcb917817a48792d8c057236323922c0325c729257e1a70093631e8d2f8e30492c23dfd9a031cc183852dc57e1e14e3f1d2e5b93d262f7cd26206f30d6d96c8d82028223064f1800c4afd1923f4a64de6c5a3c2283ff46874caf393b7e99b60d7f3bde9491cb53117cf21a5e97c913844052c6a9ce405c1ed2ba05bd8fdfe1c2a8136fbce31d430a59ba6ef33ef0609f44eb9bbf250a30560b9313bb8653c2d9405ec4f90e8d141582f69f03170ee0d580d49ae2fde03e98cfae480933a6a5c4b81be16bebc0f7b9c53bbe05c8be7ca19ca3218136ea8dea441b55cf1aeaad83983070a5b8afc3c29c7e3a5c53516152db4697b4627f84bd146ada4e9b67a52e18b70266286c188f8163aa76bc1cd9bbe6cf578c3867b8166fc9220c1d80346ecc4173ce7602dfcb3e0fd48f29cd8ab6997fb6b07aa08c8d54b6968ab0e2171791b9fd65fee947aba9977d9a34b9460c804e17ae5dd8b8653c2d9405ec4f90e8d141582f32cc7e3b61ec3acea3a124ecac4fe386e97b59165e921e4b60454d52718f35bcaaff1a215248be7ca19ca3218136ea8d850c3ca4023684a1d85eaccc18382864519fe2c1ae2dbdc6491760015da7f2707724c35b65b1f83f73cca5c316e04cc50cab9392e446b69dabaec6a87651b4618ffc17d80961a28b2bea39d83ce7602dfcb3e0fd48f29cd8066a187973ebca14b552da4ecfef4c476f33bfe0dca6e3daf978188b48ef35b6c05ba66ac097ec0a5fd9b4b4561221e8c0c9d8dc9f598fc6fffaed7c23c4ec28635b626f6f53d91953573f893115354be70d1936837b57b9c53bbe05c8be7ca19ca3218136ea8d850c3ca473ae2715ffab2a3f3e864ff8435c58a5b89a614d439552d1e11516c17ed020568f57b4742ab804506461c7a67434f40e4ae2c0592cb3a5e6827aeee63536794ecea7e9c55765007880000	admin	2026-10-07 03:00:51.475859	\N	\N	t	claude-test
2	image/webp	46738	\\x524946468ab6000057454250565038580a00000020000000af040024020049434350c8010000000001c800000000043000006d6e74725247422058595a2007e00001000100000000000061637370000000000000000000000000000000000000000000000000000000010000f6d6000100000000d32d0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000964657363000000f0000000247258595a00000114000000146758595a00000128000000146258595a0000013c00000014777470740000015000000014725452430000016400000028675452430000016400000028625452430000016400000028637072740000018c0000003c6d6c756300000000000000010000000c656e5553000000080000001c007300520047004258595a200000000000006fa2000038f50000039058595a2000000000000062990000b785000018da58595a2000000000000024a000000f840000b6cf58595a20000000000000f6d6000100000000d32d706172610000000000040000000266660000f2a700000d59000013d000000a5b00000000000000006d6c756300000000000000010000000c656e5553000000200000001c0047006f006f0067006c006500200049006e0063002e00200032003000310036565038209cb400003011039d012ab00425023e51268f4623a2272623740b08e00a09674b51cb1e42d82c508b81b9ce861ee23d961eac873d53541f48d26df15e9cb9370e629a7951f2de596f8fe9fb9c97aaef379fb83fb69eec9ff87f68fdfc7f4ff508fe71fe67d6bbfecfb3fff7effa3ffffdc8bf663d3a7d9c7fb7ffe4fdcff6d2ffffffff5c0fb29f8dff06be5bfcaff7dfc79fee9eadf952f2afefdfe57fc8ff75ffdbfe73e517fa3e7cfff43ea7ff1dfb61f69fee5fe57fd97f68fdd6fb97f729fe37f747faafb6bf98ff31fe7bf2b7fcb7c847e35fc87fb67f75fdbefec3fbc9c023f8dfedfd42fd5cf997f8afef9fe87fe77f91fde2f69afe07fc47edaff85fffff2f7e85fd8ffbeff6cfdadff09fffff003f8e7f31ff09fdbffc4ffb6ff0ffffffe77ddfff17feff9327deffcfffbeff47f94bf605fcaffa97fb0ff09fe8bff97f94fffffffff18bf95ffadfe47fd1fed8fb6bfcf7fc2ffceff2bfe8fff77fa6fffffffff417f94ff4fff69fdf7fd27fe9ff33ffffffcfdea7ffcff99f027f747ffc7fbef84cfdaefff1fefca2982072e14762ba51272c5dd4d4324a53ae76b984789dd0cba3bf6c11faf42d20f83df2fa91e11a94c16acf95da011f40d825b8e391d585eee6d8bb3ac009e9d3c5ef11e33f773ce801d4cd961df3ff150c84c1cc663f98cb08b8e00fdc44257daa53c40510b706ceafa37ba1a90b34faf721abe938f295ebac87a3bea961394fcacafd722b9d14d895c22c7617b6a6ac99fa7043981095e3672ee6a6ee0f74710ff973e6e4921cb1cd1a2a0765995b396ac3edcb9b65a96dcc682b4feaab3efb52db6c0a79c2209eb0af8421f302f8a4be08dcd1a7e3b308d3d32f7ebc83b8fa0ce91ab104282f28036663d65c251022e0c35df82d48bde407eb294c98965473451ca4d27ffb43a5bae4778b0844942590ae8ef2a0f799a74f8eede2c813205d8df077845760e4477b35e3a4fa0e60ed9559da0c2e540174d2623d262db0112a166acae451759fdf49c400827bb7623d3ee32c80a0e967e87cfc4e584c11d676d280949fc26a44e567b519b8448a00369b1d1db47a3ee646020f600dbdf6fa326b9a3103d414ce06193a02eaa0b0b1ff16d2b60f18428e2f0a0d095b125253f648b9bf1d472649834a6a9a901308798c74a2eeaf8f917a0ca1afbb70c0c6d7ab2f67a0cc72aa3f3a2966d3e3fce5557feabfe94e249cea8a70c8cf0f069c0136c792231b11e908851c721b44ac0ee488734f69f7dbcb4d0f5b6a0872a69f2a2c8458041471b5736ea62b51a9ded6fa78106214d3f1879175eb663772371b9d6de4ba87d88fb55a506fc6f079d60a1f8fac74616980e0e6f3f73a9b2c1c989db70833024499828a15d7404d95794acdf138ba9531bcc6646cd039f4a4ee54d77f35b130d6494b6367515da1eddf9ee1706c3f3dee903e916f70cf3eb8ed336d8f1ccc698942504f6e7386e42289d35f41c7c0d4b09eb67b732fe63c20fc098bf1e025208d610da254090979835da14b6509549bbfcb01f64e44bc5b7b8ca2da8d534bc97db8ae150a9919e65628922e40e7a7a752ffa2819aa6039c25e293b2e1f0881f2ddb4c2577f987f57fee51ce2fde1d74c842298644879ec66e13c48d2ebae7ee8b40b24411e5a134222eb45c75b2b55ce97b16247d4720a56ccd9f2b15f34827d8f128492483bf3da4720132909a1744f470d7887a9f545959269c42be4e1ec1a4bd4eccc410457de0d4a62674110c4868292f5503b57c44f3e836752916ed286a83e78390ccc27547849d5fd3c59458615c13fa462c67b63a653c88eaf284b7d6e76a0bbc0d0718151fef888f6b1967034f65c0d67df0ac2f38f0237469c18f6efbd91283ae06163b2f9435d2571b32feb264dd999e971d6928258dca5977c0fc6518c91b88c49d9faf462a75f49b0d437ff6e030bcbbfd6bbdc5ac7a7344a5d9f2bdaf907ae8d36385002e482ce7dc62c5e243cae181a38827454dde1432541dc3a59cc938df9825bcbc8ff7fff458af15b58017d1e66131d24603e8e7de47ac443f46c7608b7462687138b3648844d85745932caf86f0e571739f65d0fd277a66136972d2f03717bf64356d0dc6b11c2508f4cb4e234dab6e5c9212a83b0e43330592fdef72d27ae68170341c4717919ced2a78d3d6a1e0155adbf54f7636d9cc5d121c7bd4cc566839abed69957c79587a0fc195aa82f07755cdd3de849934648225bbbec3470c553ce3e8afa9223a9d9ef56966d411a0c44f0d2ae3cfc5cde813616cec67146240f391e5cc3a8ae483b0c063d9384021883e87014656726a03e4a96364821eb09ebb70543faf9fa285c8b436303a0656be562278ae840fbd085616e56a9dc3fbbadf6660ae029db90667007164a2877508a4c83530ec5ebbe652bab0a7f7327069988cefc1c7ecd7ca0184aee5f5bda27c383e34b515d2e3296029b2a70c29cd30ced4f2d3206ab7e09851ac38e21f90a41fd81afe9d00f194f4a6821794f9fefa99b7fa0202b34ea2149437a2b0e074af0bb34bae4a47beed9630cc1cbe92f7f6d0339ec14a6d80d18f0ca3d31756c50020fb2ab8637a6a3488367e858e1d8c4d3a4080756ed9d7d2c5d45eff6a72ff0408346a8c3bf644d9f9149339c238b4cc05a1431c99e3fe7efbf579294264c0c28de79a0781aa6fdc9c194f90cd0606cde56ff6d76e4432e7786df1f5f1160c74098416ce0a99f98c1185ce60e1831ff428b35d92a8d94c2aedc5d072f24d0b09d423cc70e29861a0c43bb0acdfe39553e713c520ef421a88167f96e032dee90aa9ab5e4dbd66fdabfda18c443b94b5d9ba67eb04ddf4c4bde9ffb4addc3176374a0a5d34c016e44d981d155ae56fd2cb8ebc51032bf39ce42541bcca8414210264725833ca8ae921d7a7185d3dd62192f455e01db67d78843646a703ccb5ac21dae6dd7ff5afcc0c6543140ac39276d77042b3cad965445436c6a81a7877c30c0ef045ec6d5384e101f644b18625f8491ae0a944921fb935bae73c6787f8728c991fc58515835881f1e6943210e2a86f8da6575ba9a4457dfb7a877f9c05a39f5387e7f245f209e1931da7abeb7af462e4271a7b344dac6bde635ec27806ea49c0e2ac53496de793eb971c61da2a33d6b36abab430e8980d070997ff79d31edd7d61e1c1b714c2363a2237d7da7a0d7792526498110524cf2de55434f3748dee1ac422d1f5ba02bc28804f755bf556a44c9a4178bf68e138f5e46ca3419b63cff1ea41aa62ad8dc29d6cef39be73f1b1bf5f7ccab7939d54b86f2deaedf76eadbc061d4acaf7c37923cb115f07f63a2a94964518db18b669a69dd16f6a66c2924b0105d9e884b70353daa8a20a99e214529ca417a0c8a63cfe2fba2f444ffcfd023e84e0c67e0dd973ead61a4b731252156691da61afee23c27efba71ecb3a786ad5fbfa6e52d14b53a5d8c7392602ed9416a2d0598936d798b3a103fb76ee76bf3fba6ad4d659f941183d5f9d54074d2c3222ad43907377ba9a2af5037a71fd2b5fce645d29648911339f9eae055d78382668af6546aa71bf05aab1d4397d2788b9dfe937ef4692ff5285ebb9f58181d1dfd77690bbd9fd45e0fba777b4b9777f8c547c26ea6e3a07c7df8722cd69d97a991777493f1c0b74956fee391407dcb714f5c383966450ae7551f5ddeed03996ed509971f1498b1abf1fcf6753d9914fcc68a8784b1d39cb1458f11da888be4f80e45b8902e90fb5541543848569bb3f47257632ba0c19717ed2453d79905c92725784ef25037f7f95b4b2f6264a484d86bac73e9941090546e15817012e49542c7dca49f2f0f4dda2d78cf6afa177b4f24e1466ef625055c89eed59bf60b83db4a0069570e76bac8a1833eeb025e06f2409dc77440569c924948b6b7405252ed734b9fa93e927749454f093af97898dfdf6cba835311a9008f3fd7bfc532a8a78f6f7962832de60bbd20b3aecc7737441204a07bcc1a0c6757e356f4dd87c7d2c9a8f20141dc463c46772b9ccfb5fc19428a3969b7a29617fb54e48a0a43a60eb7144755e8f8749e985612f72af38e5d597d848f04442d54b77e636d1d0a009dc5a41452a5a944dfacc0d2dcc801b490c6d7b32bf18320aaae1faeb004f04d86f54d963e849996b58bfbdb604658770bced46707b66bab22e0b67f46037dce7003d0e20a313a4c6786aa7c439632d1203f3fe25ab817d5f1cd44b0a2351ef3902e1fae8a87810129ad2374a0d5c208440e8a11198ebd207197fd7379e3346b2a6eba32302f7b3bef1c1dda2ee12f639c37f1ebc284cbc38af7764289a43b6a49ae63cef2c6e732db6e5bc2abe37ba6696832d38af952f937a2d4d2e465b14b21af455da169ec351287843555def62737679bd84227059a35368c3e7843e8795e032413cc42caf2575b974ddecf1bfc8cac0742e500e4759de19b7aa68eeb7d9a820da1bc99a47c106c443c36c4ec09f86af5c7d203022b8d949e62642d65d33207ca2b2610735734886d54f5ecdd165c28f104b3d9b2176536ea95a1c872e52d8b49f6487bc26f574c738e3943a732d2d03564d932abcfe9014b344c7389e9544f4e169ef8815b2dcfa701f5292822ea7376c5014d729569461cd301347451bb4d92370c1d32f6d34e0d41df72f91db0f9a41cb61e5004b5d7dd1506f3dba03244b21d4b23149eacf2138622ce4f801a4b40a847d27b4bd7d36c128303bf73fd2565e83c3f8b487af10faad71c6a233d9812c71f3a6356d4e39ba46e7d09efca61556b587a2e46f9f46d95cddbba7da99be499bc66cc37beaafd2d582d8a34f5cdc9cc7ecf7aa6f0a19ebcd6a9fda178322f0cc113bf93e956344382380f576a3d45854a32d8934ccbd99f64779f8a8ba61ce3b263c0a42df0b58a955302ea9a38b35f856115ff1e7fa5cebcc5c47f9223ba80faf6def6ca49e68a02e776bccf33fa79a959a6a3a007c0b3542f8eef20bc08a14fe50739a40c4d33efc62cc3377be887e206dcbb60639025d801f1daffdd5b0a1a6075ca48d207d612f43aac2a2880fc2f5fb62c346579a3f6937ebd6841ab9633365bca97b53a5e720a637ed63e0c5714e2ae86fc789e77c799637b371aa228173123c1fc0c583fc82100dc041ad7c43af211b2968a89bc5f7db822090abc59d22c2d27208822aacf38b50d007b4043a0070561ef9b40073a398584b3c91f72119d70548539a162634da81986372e9546a5db70abda1078b3eec14c5bcded3ccb53739027a6809853af71f9e67c8d9b8d6b1aeecc82073df48d5718c77799cbfa6113e16ec1e8cf3af3a17a0cd55d245491b046d3ab34cc2fc940288ce0de3de61c0415e40b836b94b0147d1a9029d236ee6cbbfa30eb98c9ebc66eaa19b0ad8ca4d796a37a9230c4f5feda534d0af3b64c490ce22a989d10089817d63a76590b181a7803ab0c5b3310780cd23c4573a7690855bda91878290014ffc078dbce2b7416e407028d460788b3fe414445d1c8d33d3c2c132b7e7664c461c1341a5dcbca61dfed1bc55264534fd43f61e01df39411b8823c263fd0dc3201ada8dcda411e58f70eefffb4f3dd6ca4fae1e03e65243ff9316ac58393213ed9ec9971d9408aa472eff3552d33ddb885014997ae2cd801d1b2e7fab84b36de01201b031e66c6535c8ec0d763b5d350db74948e1605417082484ba35c6669978b2a4699cc4ae36f1b8457565476099dbe38f03bd808339641eed36f8ec32ad1fb454e2054bbc18b945c1c66643f70378cfd31130d0a6b1d85746663c75ec0ccbca1383aca927c9151cdac221d92c4cc6ee073487ddedc0dc006d82e78654572ec0bcda42b4db6747e11f83f947229084bff5ddd1c8a42987c2bc9883f9fbf061985a5374994dccfe997b8939fd65bfe1ec9da2fcec9131f96c5df26252f57cc81eb075aaaf36f12eb5f29c3cdb501c3c1fdd7f9dd468f918fc45fbf42e8cc3c18b60654af964eaf0f18bbf2a0a9fd806b05838edc5cc49217cdfd9b009700268713af5911ee31f6255a12fa368fe21ee8afff3e4bf1a27f5203ff8320bd2df4870637882a2dcbacb1b75b90421cf82d19b2add998410a066877c2583479a2297b909e3840484f6e9d7ffeb4e88f40e73e1a349694418d0cb7aabc767c67f6e8f8cc5ad73cfd9207f1f32bfae09fd2297503403634ed3f8bb6bcee93fc4226e983ca088f089d226f3ddbdeda52a3887899128b3d6c16c576947941fc40e53482dd80769739c5a6d122925d962131039969da93056b76a71eb9663fa5bdd965634a42602024ed9610cad9c6354e1fb6c8ccce79f3c77c290c394a729e906937341faf90de9c4e9df5193e38d6c7042630ed29ac67eff123c59d6300d402879828db17042663d30e2711ef295f379e76c86da2bef63f10ac5711d9e1a739a35998787d7ac0c74ffec8b751850237c3a8f70619484658ad96165ba3887479f078368c088434ccfb69d630d0910dac3342139a96b93a61d3cf491a7daa83e431a4e167525492c51a6eb75691dbb2d037ed962ab1371d6b7c3fd8bc81455be62dc78d3772919e2e0cdbb4b49cc504c702a1099aae420e4294514f21a0cd0635fc478a9d6ed393a08147297ce44bf2e7df4a1ad4c417ca4c6139986e7a3032395c541a86cac3e152f9eaabf132fe404942a8edcb8742f22fcdcae9636d79bf97fd59951a47dc9a4abbc045fc343b024d1d934733434bba30753bb5156c8eec05a82db0604b4bb649c26e5df6fd41adff1bce0040375bf6d18ff20bb1d197cad44f2308a985f97b09175979310694c371603499c8b1ecb3c9914df8f585d2500c74d906e1b9566f523da648ddb4343e6c2b11afdf0959fd5d0ad037def03515ab3ac70dac2931882eb3bbf6ad24715278d0d0930eb96fb18cce94ec89178f542b379b49d56ad3ef74dcba3b01e9b4e0e521e57ae6ceff457fcc195b88332b9e5e326eed4ed422f2a1baaaffffffedd85828e9651a7ca1d4c868fa28defc312f0f5a920979cab9292c9e68f6a6581b412c8a3bd0d05b931e2870a180a3ce343ebcbbc7aeb0389622142fd36f286637f4205b22e6091b01b4f5ae13124a327ea1d620f13a87ee8464fc5e11451843496eb48e97947f8ebc5fe1c1c3f09d88a9dc7eefea48465d86e821abec8f6e9d4a8434bdfb9707d695b4aee8a3c0886659f69a53933cfc256a23b75b45d3aee2a75c7ff13d0804e928603e2c026867f4c03072bf141533130caf8eb30f14c9090dcd261ac6564e7a63e614036e44486c1eea901eea3a45fa22cbe47d6f427567f03e605d5002b2b14701c4d369dc777f23d9576baa30c1cf649b4b9721d1b4c85ccd621dbcb4aad956a7dd716dae72bd2f1f45591a280a5723353d86e1d534525ebf9a3410fa85a01172c105291485cedd11bd3c3985d0478f83b9c70cdd1b095ddd318136e654a984fcd0577056a07b2249c518be5bb1d8811269e468b7583fa3c607a3c267e5ff50d9541d7ba638367da79bebfe6541bb0a9e3f834d68d10cb5b2be7a9ff02bc5ae59930ac01bd30a1536956fb023d00ec36332dd76aec8227b3cccddb53a79fa23953ca39f82f81032a135497c9fd3ead3449d6ef4e2d49fcf58644a5a6977ecdd681f432039b421b3d3a6ecb40d7c9180906c7eebb7ced6e6105bb318ffdee82518dc22d13b89f681148c35e9d3cb9144d2bdf2f047df05ae3d6f8ef87a62beecf4cfcb9f41ffb5a67c293496f8508b861ba55fb65115257347571dc9b7dfd1d081dfac8f477031c147ad339a9b5aa0b8b7d6853f174321393466f0a1b833331cb5e018270b269a6b62e69a8c33ee53cedd6037452858084f185cf1dc077cd338572df90af32d03638500e65a06c705deceedccd6d61c6c5d2316ef00ce64f9da5b1d8dfc64c9de7d6b6a51e511fcd98cfd53600470f43dbcd573c3908a54a454d956223712a2da72f68fee1546572b8093ca88be69e7a079b498cd16e90fef3a06dc69177df098d55230e824399b7e67be17beca47a4cd2e8cc84146eebcde46f1cf9ba5423b2c96856b357e8d4261eae8e0861d7200c9c818a4143965d53fa3843427a0ded2d62e465399c527cac47308820778ae08ffdc6ff9588f9a389e837f4cbd4b14871f32065c9ae7ad06c01be899ffb7fa09256bf198a0687ff4afaf615d1f9e546bd2d2969e1eaf96ce639a0082ddf835ce66e6c91127cfe2be2af2c3ce5fa956dd4ab00028f429c92ce647745d0bb8f2a50157d19f59e317e0a26bb646760eb2e2257c3b5727f75fe8627df7f94e5477ee229ee829486df014a7af588347e52e7154148393eead4aab7d93142e6d8473e7cb79b700874c2efe761921cc323cbe79ac65708c4abff8483c1903e815c8c49dc989b9e4e6544bdc03f1dda5c341076f2a8f470af269aa2441f48bef803818aa3bddf797b3c386fa46f09dda3156f37992aff64db9c5ee1f1f22b091f8e4063c10751946fd6b528ea8a4f8d09a330eae595844f6191cd659a38fddd68000e752393bfc356401cd944a432c29d9d94751e3bb4b7aef1af94ab97e2102e9933500eaa6ca8610c662f02301051ffd5816b63989d72b671de01757bcd44c8b31dea389003b43d4065f7e9e0976a375eb6ffcd045050ab9d6698e29481c7f5c7eb4319a9ec9a22d6bfd0ec6bce9e1671a15aea063eea2b977ca5defc639fbdb6855028fd7c3d319620f5648b9c2da16f175e396c2d8d904070e6cb10fa00d9810c3a9efab5970fea6303a2b9ca000a8a1663920779601f7eadd86af948f086a26ea1649fad0e45768522d6461a5865dda788ed358c6f8d17cb4b3f277500b5ea24ccfc4a38579ba2aba0cb08c826e715517a6b1929004cef6702d07fef315caa37379cb80000fefbfd6ce259648a37682d5440d25ca004071798ede3b17b651a375cdcdb897e2615c4d7d980a597ee69e46aa5e06301a6ca4b8dcdb9d4c06c235de9ae72aedd90ecd406f1a02d5066593ac0db8c82bbd2513807a1350e441da48b5e75e1eece13691d66d5dcf5b3692dd4b059547c0f85bf26d1d8dd3c4c317b59c2720c843ec30783037eedb0cea01e10eafd5318718bd8faf15ca989a99a95566dad52a35d04d3f87c99ced094bb526a141e1cf20c036293d3daf01be8b2251c4a798ddc5f0c15d147bf52a44e438df6ceceae26874eef0969ab56cd78516011d4ef4eebaf895a6af43d8ab94115ce18f49cfc9bbd2d3544836bf144f3c448a06f61114dab32ebcb94c37ec5be5e81df03a7d9e2d21e1310b3a58b268b7ab793fe3b0b0a33793c1ce7777a7d553754cb646340c76756b0d919f1be0c15f364d34beb5b10bb8d14091134494af35897c05025de390cb8ec9cc71d70b81e4b3d967b3e334e3e87a4c566f9df678a36d29bd6149d25117fd9ad52e6894a62dcc37e56f9d097497e3d8e82f7ac60f09db99d221dc69b0d4b603bab5729165c4da9cca862cf932b3aa1c8e545d8a84b58ad12caa08f7fbde27cd3e5828ea7a1af632816f731117a9123c76dfa9cda31c331d349959e523a157f3055eb9f746b62bee4959d22dea29a1b614e7df13c0d547c99f92cebf58336f27d7efba96431c40387bb0f7bda692f97e337d6514d79d2b836a86a782bc592f616856370f3d8a33679f4f5da1b85a656eacde8f19039856719a1106eba939d85c6f5b72aba03650cf7bc293f3e761839c3f54820c2fd595419a41ddcaa25409e12940f186d586410c026253a068289dc55b15d681dca1deee3533fbb9f0fae5125d88fc82cf98d309fe67df9546b34b66eecc8bbb4509eb341953d2ec0b06688eb6e53b50fe3bc13a0af111692669cd866f9d82a57353e717e258903d4ba08c001e9a442db705db69b3c7332207335e10edd4143a6a6798f715aadc1f1349b1c8dba27987249f339f61351e6e8638c22bb48f20ef548bbc8bdb3bd6e7a054894c37d0e34c0bf1460b984b51649d9ecb28188b9ce628fcecb0cf58d6c0dc82abbf7c9467769438d3474126fff529a5fe4d133fd105a0f4eee15478a95364952313e3c236ebd20c47a797e942785b738d6ae0ff9126bc66fd884d8e087f9a01b0baaaa1e0f391ee9f2d2f46dcf0127287f9d4d331eaaad363c91bd65066c896246244c7942f404749cc0cd185fb430342161b1f5429926365f3c34495fd1e9ccb0ded96f8ec5a6acf1c00c89146eef08ed1987651fb6df792459a2254fe9c9f9f3dadf64409c20e9d16bd4898b0070c326104b4865491ccb5f05f28a5f1dbed795a58e92fc28f914337e4982cd3c3801c63737eed5580781a0a41caa3c860a340ed41550b175bed54bd594028faa46025e699ff891e23308fba306299c53a262f7385400223cd0e0222fb2b12c3c658fd4a547eb22ed252fcfcce3b85f5819ecb4810f6108fe6813a1cdc8432a3e94cdac1913046ec5916aeb2d09400e4335377c1b23dae35e33cc20807e9f28339f47676d09e10e9d2468614557721723dca4e4bda4ac64b456b98cd643ce3bd93e3ab765e5afa78e951ef3e13ffbdcda52cc4615364705f8bd6f7dc04c09429fb34798687a59aa89615624c023774c98770a61c50b0284c824bb59bce6040ca0f64d350b532d10e16372a76c14e75c9dda77e1d70203b9966adf64465d42155c1101296b7c9223747f685dc5d63dbe51a7cb244704b30448292e56589cccf548566294d8927d52324edbfab4c5a8ed5a5997d6d69930257f28381504804d7a811ef5993dd3f608224cebe0c4beea7832222c3b3f3eec106cb95952d157699c0cbe301ea143f37bef002121e408aad14b6cb02e91b0bec08a28dcd493fb12cf8a0080a720e8438dd747868b275b0946610e840b5ba4d80794cedb17aa7260d5c7191e92f69762d630d01fb36a0432e0ce73d569523aea589d80bcfb05f9dbde6b0d513bde646c1a0f087bd94dc40c8601d3da3ac27b5e063612a2d4ec63ed7dc942451459a992092d24f73a968b2e7212ed45b0dbfadd7283a7f38f0ea43ebecf73ca51ec9bad4a1a4ac0240e4b0e094138d0f0525c8c89ca5dca674c521e69079b5b9da2f42eae648c0568b6d6e0aaf3479146ea45b7af94969ee0cc15cf9658bdf243328733df60cc1a2ad1c4ef1b888ff2aa755563dbb3258f3c5e1ad368e9043536aed6fafeea836abdb22370fc98e31578fc27e08d1279e99b5dd2e66b6d4f4edfe9377ab18f33afb6a7802029b65b06ae59bec108ee26341518351e2f30f696b40d00c5ec4acd5d18a43919bf02400f1843b491999462a90010dc0ba257b4d1b53ac50543b6c3c1c90f49d3ce06bc05811cfd6f20e494cc0795806e073559d72a6641f85af35f28c0d813becf5c8eb0fe627a28656c61282976c18b3b5dc8137f9f8de590e6f56aa7bdc2a5100420a1f1a06e01a384a76b3d784bf6e02b4bf15a3e95db3904975c1dd8e832ab0514024056bacc9ba44bb8586f5bb2140589a54852a663a803108ca5f6e160a4554c3acff7d7466ac4fb2c617af2a6af1ef895c08026686d581ac5959cf8912849311e6296c1dcc7c3d53e9889f3464032fb8f75d7cb699d3e9be6e34e7dceef720a4753e1f28127507f5722f647eb022aec4914beac91b4b8c404ba47b37455984d1d16505abc61af4a7ad15f5cb4f1dfbf3e66732cd8c3739e8d01e1a71203fff3bde867d607457de00073d811cad458461c9287e0a082a58ab69009546b53d3aca59eed5c995ec018e8b6121d8bb3d046e4cdae8e8858cbf7cf28ac656cc202faf867f0096b0ea1ecd8c267694d88de34febc2bd4f5a4c3a61d2b496a03734f83600e87df27fd66b7c031632a260dd07be284ea5243491858153b871ac47a5c37d59d8fc992db1401abead45d5f4a813338fcbb91f04a14da4ecc5703c3ca21c7a6fbd41a52ed596adafab25804b8fbfcc923fc4072d211ea0c4ae443801a16a797cd1cecf1eace5972ae9868ff748d694ea8c81cb436e77a86d625eab0e807d8644edffb4c5f3ef8d9c53ed8e7fe0853ddacead8367483852112cc99426f44197f337039b3dcfc2b14660b288e2c6d4bbc276bed04f8154d094396024fe9c02dde9bdb104e4792580df00231252571b3f1eb1244c4e27fe6955d17f2e4ff71507392e9110fb61d6086eb238abaef980ee5d04e87ea9c31bc19dbd7822feb3dbc98d864bbff70d360db4aa0125a43b11eefbbce5a51f0ba00b1fdf901ffb5cab2438f58c1daa647fbe3e8f38b49ae082783497d8f08a75bbe518cef6dda6b123282a36b202c6cc1c25a64f8603cf23eff00a4ff0657cd3cfef95725ccfa99e6ce69c37f4b8bd102276ae2419e454c6d7db14bf1ab3c7495a0098512790a6d244bb7adb0914ea8034da061f60fc9057eb9ddb97139bc4420d1ad761d85047f55063cd2b62e048b941ea9dd07e7e01e000aa392530f9b5cb8d5bb2e05e2f915b436d9440e53907802b2032d5a70d116332facd1cbbbdc71dcc46cd53e697f13bb61c1800ff4565768dfc053c169bb91aa1f1088cfba50d40f635bf67fd09bceefa120865000074b77504afc693ced9c7bf6343fdf383a7effdbf26d3a521a83138cff906a2f6f547f1c4aa9ea5366f7670c37589127c29c01187958f8dd630790952831753094000008ed9c7b50b1694f9d640fee457fc73a630ca408a4ddb713775e01bc414823b4e101ba3bcecefa25cc151ac07724a4537133ac5bb0c38a72215c13fc1832f5c1bf11dbdc30fad173fcbc9457df82edc9436c2fd35c183fd9e65ff6081e172dae21455c0fb3b55e8ef779158dfd7ceca478b7d6ef10998b57f019108c097e86b9fff9cc461ca0cb23b98acbc93762491e00181287f7787df18f2420f6d2ba5c4f0921835b9aaa23822b5168b9293ce1fbcb3121eb5efa817c2c428542daba9f1aef75f8ab96d2f18698f7a5bd385dad58c33ceb60017848b493c3025048eff75314f847982c6341ca33e7ba0ea987ca3a972621e7a996c589915b0f1518ea2586bc497f850fb9865363318d650902871926e44624bda5d990d6762bd192bfb6bb36c65f62b8f278e4dfdab1237955dd717bbf0d9dd0646f932cf47a2fb6c048495f6ee2bbe3ee105b625bc9befb9c6175394d3f09032fd51b3d531034d0cf6b4a0bca8db421b06237e96cf0148cd58b5d30dfe61bf21c9f0bb26e98638637ddc27fe33445d8ffb393dff0cbff741a542508ad8ab0498b00335afe2242a75a96a577324bad5c8995eaa30bb3f77a58c0762009ecc0b1cad9f6bf0e44289a362cf5c0f07d71d854cddac3f9404ccb45ab64d4a8730afdb2563fffed4d5d03530f84c7561e84afa113f935d3fcc1b23f0dad3032cad614b35d90990f7ff098de6b440a65c81bf0b2f817014459cfee4effe25a7c13ac0b0913ce49abc3484599e29cda57e4a409808c511a8ce2c6fe21e34422f073d6c2165b942b87f3f11010c5977fe5bc970c5d55e624369bf118f2db7f2590ee98695c665c91ada92a508d020951a6b84a7d54001d88809c44c7bbeef40b9c70388751fcb11e66ff78924edd34669025e5b6b3a7bd22cd6974554f3d008a5454871184f391045fb1be86341a15af290fb1f1bf5c7d5f6961166e25eb5799e59fb4f8831668a6f840a9cbfb1ae6f97765b914f539104888d3ecb3120e45c5f4c86ee0242c580127b5aca6da73d43e0621703fd216e8d9c4562cfc74b036cbf601b82966cf7818d6e7e3d15a0ede35fb5da348559b0e751366298bb889bf6a2b324ea4ba357c652e4f9f69cd2cdbdd33b5e9ae076324b254f729b6c67fdeab758007b780003e0f4a7e388e363d2f909a9b202ed4af600b9a14a1417de248cc59c393ee025b9658e7dfbe58515685283a37674ac55a257ea31c8e101944691845ae56695bc36f9593b366e609e13eda2bbad194c8bef641179e1da3f7775455af2a19d7bc13341abefc6df4bddeffa9e8c8b0aa3b277608b9f6df275330608e40013dd6b71ffee464ecd62885dd061ce64eac3339fdbc73ea47fded666d4f299d3d2f9c2190653d16331e8c2fb09cf80b4a50e00c76ca451d84c27a49628b2d62dbc8d254f8095cf253c1a771282c82c9b9a088e27adef9429b7502591ed6662bfc182a0c4bfdcc70ec4ca23fdfafb13146b86e6875c55b03ef63e8d223f9cd25be09ecb440f83cf8c56eb1287a72548e9271f81fb7362ceeea9f5b5192e90a931337fec9c37216be68e2fbbe0841c06fa4b30bbedf8ec94d1a4355cc70896b27cf5b6d16290ed68f96a9c14182a338ddb7dab713c0649caca8e5c214f96edfd71685b6ed4eebbfadc024b08c65e79371b72d418abd422a248ac86f0b578e6a55bcb1d7a175db084a0bd0c90012aa9397f6d95e3b2a6130c73ceaeaaea06ccd0d24ccb5be4f98a3ba376a924eb754c364664632ee823ec9ea5bafcb07d4fe5939502c9666dbf526359fd27f6c8007c33c1114fb991c5a00fd4eec422d981c093a88e397c1be027cd9bd1a880552492347a2c622a367444d28dbf2df9d2782c55769e3ca12d73842d0510eec1b6bb4ed3caf1c08e83725e567df7f658ab3234031bfaec633dea8da49d55e0703ff949fb392c02fd799907ca89d4f61e5924351df2f1e8fae1901939006300c1b2829b35053f26a736db0e5a9d42efb3463f226a4ca172b83f5a85b07415234c7713491dab20734d755d9ed7546b32d3a46ff7823cbb7269f71317f929132b6862872bebbdfcb46bd9157932493861a27640ec6ff5339d682b739a6e4e0bb529af4029bea4611feeb3073528cf0dc181a43b5853c15c7e0bf0c96d9346c8ac324f798164fff4349e1d4fb53a4937584bd75d6325bf5ca166aae7df0b3d3b26c02da45153d4593b6170f52b20beb8b9d4c85cdc7c3896bbe2cc6eaeb9e0d7bf94c2265f8303db389a95e826ec5b73c86b1107fe0a09f571acb53d542c6ba4931d85007531af9decc36e2e6400423009e5f90ed16955c92e6bd345c1c8b772dbbe5e7cdb4fabf5283332e78f96a767d240443abf2470778d9920b5f3cbe9e876e96ec906d38afc17a80b078d01b234a034504663395916ed2ba9e1ae02c7b5d5a83679bf0183060a3f09ca1de85f70dd2602296421d4742d0f478621962aa079b63adf42321cbb25e29e42ac3358edb6a342e19601f1740fc7108a351c6c7861126284623c75ed93192f7980c58c3b8a754f5103af73d39d666190cf4cb49539e0b1f8b8aa47d4d879e87f8145320bb8cd884d3f44960ad6af81e78d71e4689dbcd80c0193b4516af5f462007edb482de66bc8fed5459d354c069f0a94c195e9e4186bed4461e4bc59dfc95c8f213bd96643778475bcd7c973126d9cbca33bea53b42951152116e024883ecb551b82a62b9c83e99e451f10b1ee06166f69bcdbafa78557a03a0fd1566989650d7dc5b85e009097a07933e2a09e8e8b9a7f98a9d0b5ec447e6d8aae1494c071280c4a02b5e049cc61eba37dac2f5325c9829ba1626e777d5f7eb04970018bd47c75795e9e2596da513a66e2795ab88ebb39a2ffcdc6922f82c3e08a300eb29b0c3c18ba70432057393dacbd1ccc0a2c07869f45ce3c69ea87aaf207b20116459bff92e47acdb31c48ea454b5f28c8bc19ee6a4b4ce9b36b344ea1db5b9c0c0eb69e8aac8f1bc364462b42be3abc0013e301f3c2574b1a802233cc76b14e2446026aa6deadf84ce9007c445664ab4922110689886bb3b85ffa09cbb55a2719c7199c66a013a0e7c7ffcf0951af176f4f3b8f2dc07ebb1cf9a6e1ec1ac3f02bc9cd67cfdf76d2d262a544fac491bf773b0c18b9dd6807a4bee677b4a5587101f3e0e05af87f7f1fd7f5e7c53e98691f1ba4452f3050c9e3d8e13f3f10b0ff56d3aa63d0680326d13f167c82abf0040d150c3a7d41e3b43f0c0d2d902dd8d9f5b6f859dd72581e85f4be2b527224ce2bc8cedcf0bb1e241990e95c0193b6f8e51f7c19dd82fd9e0601720334d4954237023a2b5f0942e9ecd8157beafdbe9d0b4e0f9bc69adae242718587762645a1b3b68b547a907ef43040f1e1e56f74c900514fde72311e7dbee546e4c04246bd9276458fc290b852885ba2798f3311625a2605c874250d1ab43dee89b920507d24fba059912c966f7007f0ab26020539f583b53b0cda9e5f66a466f07dc23502fbd225a8ef23ca9ef92d5ac97a39364447ab0b2cdf3e4cf2d8e4610d8d3a04fccf67af02f7c690e770ee4b657a2ad74a1cf068de36f769d53bd74cddba68d322719744dbdea40eb3045fa6fd0c370d0542079558932008e480b391e224c0e055a588cb248b8862229eaf2a2f47b013ba15d2f94e23d2b803f511399e4d8b9be0ca41dd4683e29f4762c57f821421c4411fe5f299c4ea39ef375a2fdbe6712c775b9b81808be40a0e7e99a56469c34c738b8b54241778a9e4f52e99d78cad27364bed279034fd0374a302dc311d9aa6ae12c8a3afcb94f7687991a9894584efdc690e6e9d5a231677c13dd8af426f1bf1ff7d536ad9f5cc7b7c2b4daedca03b6e92a49d2a6ff87b870a2d2ccffbf76d16ea8fce8d7326817c48802c90c27b4c0d78e150c11fbe687d0ccc6abf435d298bc72ecddb6699be20ab77f1b77d71c33c324a294407771716ed40ef4b30a478400cb44103f933f227f0c4905d10e939ca432c029b64e67a47777a9c7354ad8a32e693a894f250c399cd43a1a6a826c324c5ab9dbf9dbf5a1e69f04c3dc9683f7a731331cdb85161ff314f1dcc88c17543ccf0c26cd7418dfd69617cd2ee15fbb1df5afab86fe1e30cc78ac12d6ffd321abed30b41ce58e4e04849a437749c8e713e6dec9abba1291c066da4a9fcdd9821d0c5a3d2458aa9017501c9e67a3117409fea9f79e8694fd190a555ba8988776e896ec559cb631d4d378ad70e03e0de091c06cdb83db14f0597a5302d97cf13a75e2fc5400dbef105bcac462beb36526f53ccac20b1af8aac5b081936b7c1e225ec64299e372e9776a8cdcd6d2333cb803ecfee014fc39b95e68d5c46caa4a6b2e65e202eb99d9448fc5a2f0baf62e9e94b077511be19ed01752d069c32e62311c37fa32ba8658237e08745eae01c0d121e51d006785c980e31d04de77487c1f7884af9f093f7115ee009cf3f7c800139053659416c0d73a9c544dc38205da99743fdb7cb73fdbfb4bc082ded42cb17022f441bc9affbac223a19166d0ae586179d816e54a879e1ef85d06420e688a28de07af15b644c9e4475fa021bbe69466059ec68fc5d72c68b5052bffe7f3c444e04a2a8ad0013f48f80edb3e4e7d8b16589072c4a3d04ab6ab2de2c285c6516132f86c4f5abfb85576e5ceec9cf6fb361a53ae3c30ebe91e483744f3cf3cc6b9cd940e3d12271e3c25b6df052a662813bd9d51c8767058483db5f39acab92f548550156dc4a427040fca2f1e0486c581cd02207a3cb4ce35e2065c5286cff3db0ead17f740844a99c64ccd236c8dbd18c5c1e96418b934c9aad58677b73a3b6aa2943eafd77bebf36190d30607a21c88503c17a5a73dd03e297ebfe02aed40f5d5fba35f4e930c789187ee9c47396adc27dd49c1e1cad6dcc4a0c158cb575c1e63fb6984b347f63fd63fca5faec9b5fb498f31869e8209c791dd51d65c2ded8142ee405f5ce0f633b7fc52f0ee1e91ce080a095671ed2f830f8a4baea0ba2279d881216d3d60a5a1fd71bafd089a7ee9d69b5c9b418da8a286800796a91a51ea56893839a49d11f5b9e4298599d3e628deefb1c5c9825dbd4c1b385fdf3ffc2febd06f11739d6602fbb8a09f674ce1e7707424af15a7ec865e178e3af80542ceb60a31615d3eadd0a3bbfc261987ddd9b6413471c28e6277ad0ab05889705f0b976bdaf9b82108a089b49a61fb037cdc00017c62b78aee49e6a485666a5dd0f7627e27a92e18963393316823acefe2e92ec7c4266a94606d28bd250d3c2de1bf38f2471211b2f56a36bc55dd0f6318f33c6aaf501b775874cdb83089c63c95751163daeb7a00c85eb184a8bbaaf0b9e7438f41524b8faf5802db6a9bf5be65f7e73a9836cadf92d1cedd328dcf0a27fc6a7acf0d8536ece696abbf7e58191733b378aac0dc94fcb66424e46ea8795fe712273e5a443b0cdbf3ed550620aa709683ac7d35cc5765630339840de2feef87de0e3f9dbe9d31f8fdd025513f689c891fc6879b98c9abd4a804e90907879fff785ef1d6997e4c5fa985f43942bbffb7159848cfd6106592fc56f9275b131cd5fc4bdb8ca7b23490023e7ade9f0a27f5db2748bfd7a21e58c90720afc940dffc09a06ef9b9e6158e4fa39fd6efc1b4014f2538fd186f785446c11a83eb7d37438dafbd272204af0c05c0fb9ea27468a1fdeab087cd0ec3cb374c554135c8703922ea7d68fa9b89d91ac72f422ea70dd89f868c9faf4d007c0e9a9a212f910ed003ab73e9589cadfc405848819ca5d9df806a2ef57d7b17158c9efde5f9a0a10577dfcd2d8c17134237f54f1abda7bcb0b2e77abc72f41457402715e6f3c237147c92c98e350e949777c3f1569cc79a655dc0d73c3fd60c2d6372cfcb275c90c0f60d61b30007e79c811bf0f1a3872b93787a40e03859955e847be4fba829cf93f19065c75308e1db20a95d03a11de5143f92794d450f2dad3de14efb45371afbce1ce7d293f74a10e4c2fac157f4f5e7af79f048d0d073ff9b5bae2d2aaac338c8c8dd3ab77f16a2c744ce5aa2053483261a888d521f45fdbad3d4b5778ff706cff49216f71d697e506af6e04641ca55f7ad24226f969d924fd191e08bc758c6a55280b9c0c9422b67d70f8a1ebebf22e11e47724e6958752d37438c48546dbfbb09267b2038c45712ac1233bcdd074bd0e9002434b6c637163ef3c494d25ad8a714aca8be84a71d368f1c193cb8d48aba2799fbc90ece62d1ad1ebd5c5c2b31109e1fced5b8f0bfec09150862d73d658041d7300716568795c28a2e2d353e6b9b22affe05970e6fa31ada1b83ca88f3c7cd83f907ed0398fc74b6ba211854980da412f2bcfa03fd0dded33d20adaf5ae29c72b678de693c82d6482cfa8e6001e1a3da7ae9859f565d4cecede229df746d31ce34f12b0d2c1e091ce829946ea95e9e4f02a0b17a7528803e0bfe7cbb13e94e24a7f06053fc0badf96f3db2d0306d14d4ab1c94fe448a2ce346eb23fe59c23cbae1ec60161379f37be9d58e00a358fd85a58d2c1dd6320eddd2006ef9e7f4c671cf1d5d0f8239f9b7e0ef9b7b196dd1b543583185c6a36e3042f83ad6a6113e96fb1c95915ebc91b8a39bc31057b5e14e5c757d660e39aa7a1cd138e32bbcafd1c857c720ac118b4d6132f03217654c580b8288b801498b525d0b00dab1e1230f58df728b8ba565f2a72d4a53ca95344709511a40ff52ea1bc412897052e5de490362c720fc570907ecab3c8952e42234f95c335c55b84aa398c670aa40c63adaec31c5538620d673b575bc144b9bff01df55216218458bcb14c7e1c4223078b1c37004b4ff0de4c900ca31cefc513a1b61be93e82caac6b28393fb2a94996ba004104d80c3c1484bc8ca2cb53f20f62d82a9078840551f2a6fe2d73f261f03cc301778d50928650785288830e82d0c83406f747977db79c5074db86bab4919f7d5a9605e818e19f202c0311d089288cf6a61f0b8f184f1a093e82d3ce2c30e856f2d450b542039f435bb0a7345095df24d8aa99846b1da5e168b36bb23d49e60f35c41fac7501e3f345255de7c9655cebe7cc0726ec64242c9c30d0b153929551ba68dfbf0acc344df55ab3a9e99039e7946cbb7b5096d036c7a28777eaaf00b15929d5ed09e9feffb134ca235135cf7cd81392eca66987eb6b616ebaf4c801a8066120419cbcd5b7c977c4394c44b23f310bfbc011394af2de66d320d754394739ce4d68106be28273cd040f30ed50e8468a8ff2479e002673cc05810a4e328f720ab550493e4c5c339452baf82b3dd87a95b2ed753dfa1bad49dc06febc6225b7350555fb88a67483520f642ea0824e60e6c2d0258fe65eac32f29677f01d6d80aad68d0daf6c3bcc5a1ceca465cc54421ba9761d1ce7dc51b3fa20ce78458a1f872782d28a5fe304f508c6f12fe2799d64f7df04c452dd75bf3ff893a4bc52f5b506128065d70156899202353a1767641e028bc54a262b30829f2bf8ce9c6645bb3b85c9976056680e372ba7888f355489fec204afcc860b22266867e91c43c503bc50777cd9a116ea92b65c38a003ce875401e0352c1e91827a523ab850f3aeea6538331aea1f5adf0d7aca65705821cbf0214f3fb281d3ad555221844b62f0677f4f72a787532996996aa8f0330277025812298d76ca041c26405b8d425696e7a7b5621a90a0821f66682f9f2556ef2cfa91f5e58ea03eff20869e6fefa205c30aabb539242ab8b44a68757baa73be2c9685cb228b8917ead4cad446b64ba13e7eef356e847f43239d42bf51e6d506497ba665613db66f0542fe8c63a3074c5ff955c0f61b81ccf4a7caabfb46afaf489fc962f12dcd29a75922223825b83c7b3c4ce0c5c47ff364f733b8e039ce5701d795ab3afb785332b7107b32d06fed331b10521f6cc988f3a9573e688e856f84a362066d004c9f73d9954a582fba116ece6d3e2d33a46a1b1fe9b008a4c7eb146369ecf3b77cf22ea1a0f2183f21eea40b48d303ebd85b1c57ee4afff0ad0f05160cea9090a4924595a8e04792879e86b81b88ce7f16003277bcf1d1fa4b6de77fbd0c98ef8b2fe562ba3675ab4b1eb0b43e80ffdc91d9135df38bf4dd026a111cc1a8931f96819b346a1fd90c4c77728a190462aa54576239f59e5c53ffc590a19b74b207a8b70731f33e78133546c25a0a806210dc0f28ba840f6258fe2d4e6a9845d6f2cc775573e30f58e670a686b89dfdbaacd8db2a27428e74bc2ab41fdca8dc76bf65fc76eca3d265fc9b19363b6da35c4127c1dbc1379c9968b07f3e896969de906fc23cbf941f067570993acdebd8001aba51ec758531ce6464a2313398f79d5273d663308c087773f7a8272bf533c73ae1194746d0c1bd4c0c4b1f2b6de8400e3eac3b2671b4a7bb5c885e62d69ebd7f546d26fc590948f2a0daac69bf4472a6963ed738b781cd41ab8ec63f0f27867f746297c51a5911aa1a209771c234704a8e6608beb07a825fb72a0df63d24d65a6c1a7730367e586da44fb188a54ef87979db390c8b9f6372a9dab566578a480df4473d4e9a9b4b0daad9a24dc27f1f9420b07e4d664aec7783b83eaf9cf5c8d10eb5afa972483e8b1b4f091ccebe6e14c192324fb64fa82fd4b6ebb845f5217626000a3cc7abb1a994cbfb584b6ea12f07d5fdc2c62668cc97f61f27a8445e5290ef0a4c80e69c24509da9d47d8d2010b42b627ed3e51434c4b5188c3c48fbc3292f152fe40b4dbc3a2516a8f5e519f8674e4ea3a349676b666381fd3b17dbb7dca6f53569d55494c7a9a37abd02dc6e7d50a029962523111997f760b5291d90ebf86690ab92fdb53907750d146cc60fc8e92fdca8258b3b9f2251c3c3ead94eca9a2a749b18eaa9b0a54aba8109595254c1b171923e1ceb2b9ed0718df5ff5d18841f862667b281ce9ccbdd3019485ba66a26935b78e76cd4fd3bc7c3654a8ff49dba58132140bc85f205b2642d9a5da7ee8e80e69bb68823889bed2d9d63e4834b6b333c022b936276699e1b12938d4c6e5f6713d8646f1685842da1776f54c4b78b4095e622c5fa7550be350dae6c0c8f273c3f24d9acfe05465b5ed3d0739e00db27cdd18de14aebf5c6813c1d06dac15a63fb8ba5b5700119315357147f35f7d989f7b058ec1f39b3e91629fba5b0ecfee89da18c1553012d1d93600e9dec5bb0c06b82da38ff7f7250113a6dfff883ecb997b5619258dafd5876ababc598791389533d2ef0e25d0b0a4078d2abc3bb165e476ca57d236921bcbd0e6ce8f977d043645831354ee6380f43d9459ccd4b6ec9b136b2bcc5b421a48a50db90e1e494f39c4aabb09c57de7d60c93323673b573d1b372f285d5b11db4faed52c4bdcdb259adba8339185f6e8fec4d1e1f46fa0db9bc5920a4d74f408dad7ff7ba1a5d7460800505ce990e661bba4d6e40c164b432600456ef637044a369f4f938b172342045d998b31cee3eda1ca2400e1d42a65bdc0851b20ef4cb6380e2f1398b69023695f2c11b3411cf539250f058e6bc2f053730a7e8c417d5fc2abab3c141cca7925358295b747a8dd3a0e9a0c6d6d54c7eda56137b10c52644e51742373cf82212c124a43eed004e3cce73075e9fd090140a95116f9c4dae5e35eff87a8e687b6d30dca86774fb74a249c9554e36f25db1e3f2263222e21aec8b32a851c86a09440858de93dd17de98d9798407fda2ed70137127457ed109adc0cae711b7c749ee2fbbb49787e7832359a49b9758123d39ab92698c75eeb8e835cb6945fbd79e96bffe09143ef4633146bf60cd88f09eb9e55360e54eb6a8d44ff0a4132a91a7650572218bc2a8514f2e4fc3cf0e63c7b48bf34f33a7d0109614b2643dd428957e19fe665509c32922092bbe2e6515bd3c079f6a85f8e5bda25e4ff3d830c80a1b1bb7b08af4bdd3fd1d5e1fd68e2d5670da762a10ae3a85f9b8a9ec51bbf6e3ae418c54758dab128458b51b8fc75c3ffc989eae69fd6fc5468eaf8a77b393de3fa1bcee8c4de1546137869c84037829211f1d8bb1acb66dc783ff7350c8b7e1b5ab7e4d3d44deab7c71210777f367476fc108c6965e7ab0e0ebfbccd44f4d1027bc8a9685c951cf05fdb7db1d2a111511aade042bcdee75922a4d167a6a97108642cd3dbeb72323abaa779ad1d6fec6699eabec766a6c43e412c1151eb917d3d7cb080021cca289bd2df4eb9644c8ddc88de060fb537b03438395ab931e9c92d88bda811cd64d356eadd599388589a67ab2ad42d9754d9dff11f993bb31452b792e32c9660f809c9c7b9a2aa90445b97a1fa228c08fb6b3bb3d24e3fc54560595e4ee3b09d970f98de107bf9942c223bb35b8ad1842566bbbbc0c83fc93637e5994c2bb5a9b8133ad538e896d8de7c97dc957c942787c3fdbed42f07d2fc2d92091966e84042b5beadf34f86f1d1463d4fef3a4aebfe073f44b684dcfaf085a2753f6892e2501f23047628205cd199039e31136818dce0af5999c4fc213c2dc35f4d973e57aa4066f311849df2155b802bc9750b58eb2dde95ea4326262951c443b6d9322d1f55aa527fc441d8b6abd34b5b4c8ae8fa7d7316b325e7a9188df89831b974bd7b2965f93bd6bf5f5c3271961a77717200ba1628b91af4821b3e9590b88a884273a8db784b87f46e20b720a6daab4fb60913f5aa9c3b28dad358d4812a61a74844a6e88785ce904ffbdeda223e3cf84bfabaf4f23365711a0bbaeba21d0cd840d5c11c1695fd926a6cd5599dd2875c385901ec1cf5c217fff52d50389fc0bc588d7cb188260c200eca07339cfdad989fdb42bfe0253e31487050b340261d83e272a0524918b6dbd21ac6cd6a26ad2c3a80b9382f55de0046800a3421d7b8e9f9fc4dbbbf880fe4d932cd021335f0bf40567d2a54e87d7c18eac97ce00e0cfb1b3ae27fb8fe257fe74ce46678bf189c0aa510aad2154d20672c57a5d70fe222475f6dbfe16cc56fabb54bba5fc320ddd48f8b0eaf380c09afc4b7e9410b580e2995b9cb6c29a2bac135ed83c0df2ae288b03f41afe9c7f03f8eb459e3d59394e210000ba9986d5303a8e22401b470fd19cd60740ba457571c98993ceedda70b7510754e79deacfed99f0e1ff3fcfcb9e2c8039aa0776bbf5ce99f7d36a5dd2df41288250d18ff2e87d6e7bc3249723e074aefcb77092b4c7339edf7aa2afe70b27245cdb0b8be65282f42492fc9a409b76c72e17674ebac41fb56c3c1de0c49924f18abd1c7be887ca5c00a893fe790c940b0429de326550a4746e59b99d2de24489d6a4f7db56337561e20d213ff07ab5f5482e3e6989cad1327b8a229964b99ce200ce6f0cecd2960a244b9444da86e45de21e6651bee5291eac1af4ae6636452f0cd3d1887ba3286111e17c346a3c50c4931ec7ec188a01a96a8bf0e2ef417af38b41397af9710f98a15672e6c2c25e928fd4906f7003ac524d3d03df901441459c3249ddf575698aef4fcff2427ec01c76a43fd204277c635d95f6934e09d7afc4ca5fe7d9d6f31ff41529a5e7fc90e9f95fd529445ecc7fa95127e8ca00f1371268afe8c5aa9fffa73fc22eb3455eb829f282460c1b9c04012fd074f877bd61b41250bd9f603b423ad5a2ac4c5928171c813f2230906d7cbe44c2c4a74369f07bc093ab6abbc63257945c1d2331d64e3fa9d243101e5ff2cfed359c3a504f77f94f4db44fac0ce17ea2a1a5b14807bf44f40ae76e9b29cb576e244b03ba66f687df020b1c5c22bd1ce9ec5460712b1cab023461badf5a82cf1ee4430d61ae69de153bbbe0108884cd325b61e3369e9f9a682acca08761849e2dab33ba1830c7e6a1b248f535ceee49cd767967cd8ca0091e5a307b1f2265c1d94a58e82e56153b27e312a2720bd3edab43829a6f1225ce37d1a29078c579b7a433903b9aa0ab99021171977cb4917b77326e1284903cfacde459d2b922116f153d2f92a248ecd3841351faa86d91b8d4265539454d3f9cec949528aabfec21d27fdb409ab4e5d4182b69b53a604bdf3606c06a0dd1750c2618fb779d8e134035875a0a98845852bc2bba6c55fe48a9973db7c862e36c1678239d1756353bcd914688258440b1b70bca396a98b4e9e5a49bf9cbaad7b4555f587b0827eb87321dfff5657aa265e6e11d90ac31eb71e3a049a8ec4013988ca930b62adfa69dcfc04b05f3c7c52057e7bee7637de9db5f1b42e6f5c03bbd2c79901c847044e99734ef50900d29d42122246eb4975eb2e37be2f6e22837cc17e14be8db0bdd93bf69fc3eda3cf99d2cedb410fe3c74629718ecdc8a4afef0f51009ae17f94975a017780ec80ca1f2bd89667c59ac2aed710ae87b375fb62e1705cd47ada624406a327de873a85d64b8a1a8d34245d8e4177fdf183edc5b55cf6c278d08061fc896a97be4ebc72515ec7aa9fdc22bbc4bcb736e64cc8ca6a38594d03faa2900c2780e2035d7d655461ac6721474a1715588c5be0aaef58a1b68fffd714039fbad59dd5016dd82cbad56e4cc193fb420e96f4731e207c987505366adce67048e1a39332ba3153b3d6debfcf011d9f78578cef39f2367b9fd1bfc55a8e1e115850410d377f94dfc36643c59b74241f65c317ef550d471816961acca38f6fc9b35d6cfe9db715a4dac051d6c52fc8d8d2120926ea24813a31d162c7b5f85c17484f132ec9fd6d4e4be348ac1d301c54b41b81260e298943281a3d8a25acb55366a869aed801161f78645659d690459e34730a064b14332cde1a73f99fa1f0bf92f96af4684d81818d3f163b8a7a695ab23d6370922fba2ab195ab3be94702975dd72983ae42d4c479ff3a4b3e4677f1bc5f5ed6a30ddc8e77aa6c380735670cd5c8c3e3f0763b6e50bda3f27eb33784aec3da498532be6e2da26ab41b7628e07f4282c6ecfc4e04f9facf17d722bb67f4ce620187041ef3a8375aac6e48bfeae04d261818c4f0e5aa2cc1fd157c752eaa626d5145582f8cc55390bb59867e595c1ac632444577ac1a575fd11b7427b01bacafc71808963aed18e429e4b275445851f4b769a8a08a2a5aac516ccb00da963489c551ae90fc410674197590fa61f487951f732309eab793589b814176d7d5ba76e4a43d0a02417dda2af2b9cb69e7d59323bfd1cad2cdb3e40e8d1fd79a9d0d799f2ced718b6d6024e379e1e56b29205a3120f9e5b676cf8b6cc2a3be15335d71251669dfd0ac06d1920d36fd2c41cd25fe07db3cbcdcd258dfdc4515595513e3207a4afedb27c0820cfd08f60cdde51ab95a5387db481b4766a0e5ea8dd83df09ddaaf1ed86f99ba03f586eb6b5aab561368297189345a678c636325507bc89f160b55642cf2bcb38a8a7c2ef9981fa426177683313feea4cf032de9ccaa7c75037622f253f1e4634bdffde8b481fd63c490609d16b56c25c56091c83753c8e1fc61b731c3768089a5b3794ec3119a9756d1564bee36fd5e686fc3147fcf6e653f38cbc3d32b947f9d56837d05a1dff575dd4170f351bee1226a1e1d8aa2b9dd82e694ea44906009d85a0495da21eb61d35f7d92a1ff20e0db295aec65b6c4ef0f934b1dfb4ac0b382a211e87e580e8b7cd51be878237b92d8dbe4c0b75ceaff87e77c0923c25ec1b19005fd291d2049a8cda4fe27c132ef6e04a9b8c30ad999820f46d86f2805a749b9825421bb190bea3c9be9b5013d8c99c1629e0a63d2cb5d9cc885e94770e5caf41d097c48de3ecba597d341446f31f2fd15c11ea3bb8695fa9970db91609132b52a0db4464cddd0ee260a1647a5ecd07cc5f4e38642c6c6b702e3fe6a02ff5578931ec79a4e61d0c89645702e79c31447fc7a3301ffbefca31432fe24ed5f82157563a11cc19f5df009b4b8cdade86cd61a5a178d0ffbef20e09859ee86fba1c850322157d3d625ab2e0217145020d7e02709e4e2afacf0d2370c56557d7dcc735d82a903bb91b435d654bf0c4464cddc557673536ef431bfdef858a0e83a4488c2331eb002a7096e0893fa6a583f1fa3fdd1a9c83a834d8665d0e3c88ae409cbeceec55ef6188f8d95e94e457578ea67c7d0f714cbdc5b0678e589f5be2648c0c599fbc86362c94b83f5440ded0d363cc88286f6fcf73cd1abcf126f58a026785e08953e6c7cd19bc252c0eb7747a499f82f4e27674cfdfef71af6dd8979489147bdd706dd4d4cec48ebbf82065861dc695c8e345696438b10f24c2f62c015f0102225325b34e0959cdefc8183b17a66b299eaabc3c50560f23628a962114189c7d87743a648d44a96cd951fe57dc769dccae38311aada897401b15c16b2182f2f9a7b3cfc1d494716568d38e0497851124b007c72469a46676c2d7ff4525b13795689a227a0fdf0dcf9acdd65ab971e7352bdb65ccd155090b02b3d913a7f8c0ec16865be9bdaa01730c98ed1727451e9c6be9ac204e899495f129264dffc532ffff03d654b74dbce7ec08b703d2087f5914fbac0be8a4191574e34ec190baf580c6308a03828ed402d97ef2365ced91838ed5e337d3c0b91e6943513032d858278856eb227e8d6922c6a39345891dd24340b6f6c9cae8b70fef4fad5a349e9256bc90a7a164206c42347f253e3fca54d483609670b0ffaac44fed1e7342e199905cad9160c6f315da13dc290c77241dc8be099008822736a66cf5db4f9a3a02a3820663b8bde9043de5680fe5c75bdd65aac6fbb85ad3bca22b6d2883a5e598a1c33d5ba5e5842ca94112f946f7e39600595684697a3e82562d5ded64261176c63ad38061824d4c3c3b24bc81b0e5119a50c93104bbfa010051ba630a6f9d891f3a76d5887c85971238f7c8856f967fbb7b152e2d7efc968ffe17a36482190ff46ac11ba921cc54b7eafa13608c237a572f199248ce0b3a0225495ebce048a7539b1620267ecc6f2a69e910d6228fb6c03d8736d180b0e82769534467a4c2c7d154892a6b6577c037cd909f244d69051ceebb4a3fed041a50b2ea2c7477e186ef1e8c26c73818bb421beb15e6ac7374223d826940494300e85866be4e58c9d47d8236de937d8a021bc7934ef5f8e7dda4f3e174bc0453b54df9f79e01eb48abfc51d5cf12244ba6e197293463f2d15bb4cb32f015d75b31e832e14580dc91f33929c031d397fa8b618a46394449c98bb87929c0424f6f307d644581abf2e0eeee639753b78aa2f00bbab874b131cb83deccbd0ed2d9fca2d1213e92afd3fe048aab280df9367136475c5a3ed58ff0b26fa796ae2076fea8ca90efe008948259f53195818c572d4820548905667dd81e117a5559b3d3f89112534857fdd3f60f589dd05a78012fd91155d7421d11b5cac70e2f346b2f3f7c64614e2e201e20cc1089761a9b7aca32daeb2f125acb42bab989b778b00c10bf6ad16e3a4419d822e80dd70eb26944f184a55938843c9b3bbf506a3659635e0d63d31d93a383d77bbf94756e6146c83b7bf2b78a5b71a0f5414fc8ac5e8219b76076e7854e660b54d7cd907b657eab07369a19f727c7391ce60c0f368f3ca3205926147f36587fffc61694ef88e5dcfc28d20627f984fdb19b6fc3707298f6c5d60e4e54e1fd442754b494ba3ac23fdb4e812369233354c938dcec41e34e92762e3acc20d1dacb2babad710a012ae10c466f1b03dff1d431abbfa38d6302d973a235ca0167db15884705acb7f037fcfde32783ddeb42d6d88a6b761c80677548d45f8ce4208a473aaccb29a7fff08d38bf3b251c5ed6581ab9d8000de5f7fe6f9533b361e0904f78e3e91428903cbb79509bfd1c713e4e82bded717b1fa389c065a2dcbc843f07da106b2a8903aa3fd55f7dbd0d58a3ce400ca8045daeab2b3933b23490f1737e4490b4e2185363d93c39bf7acc06c799b4695ba52c0a6f02d109a93205c26a80022b4a8b44e1c7785a7126d0901d44cd00a6edf87e2c3ed7f73c6fee265a7b890e3d93b6aa449de4bebdf1ce55698261992f4c1a00d47e3be48a3888e53a376967501b8d75320deed1c03dce33f5b9d3193ca503315ad85322eeb8f45f85f5239099857c696785fb91be55b510008fa25b71395e4e630d6cd32e8c10b2a55013fa92722a1909909b47a5f7e7b44760d246d9acf97804f1b470858da9a198a05361f5509382290ce0b62f5537319398b1dfacf3c880dc6706ce4d63c49a83c7eb27776a2f792d19f0e43b1045653452d7f0f995e2ae6d4e973449f6f0ffe915e21a4a090d525459c34065805a6581ba4532004b07dba4ea3ec10259fc69f5eca492ad170d522068c336274f35400a4331219711c507f3fc29ee9d0cec6c63a53d7c10c6b5eeb9797011025cb3242c9a0df16edfe13ace2298a9136d4963b415160bdcb25a7dbd2fde0a0461f120ecbf23d4cfbaefc49f590de3d17f982084b0b9d61ba1ac61410ca5fbdef42ac94a429703e92a22ad0393e13e7110f5983984e855a12591c741587827f1a6ef60b777222a2c5751fd52d282043b7879e6049bec5c1e4dd622e9d659cc9a066e07c7b564cd25bd130f8af7040316c7b26cf344505da2d1e7ea207bacd260a0352bda01a3891f0c9ef156b921e7e0caa34bb8eb87d31b1e98830ccf49096e607008e3bc42ce67640d9063bf4a8082815f56cf995408dea62ebb5d40a68aaa5c24492ecf83c133ab663c1db470e75328eb30ecc6f6f079ca28a4c55402fdb169ff8aae1d99c2ead53a48b64c4e921e11f7a7e00bbaac0c56dfba9f31aeac62ae972cd243202a6f5c8abebc2169b8ce867d0dbfda525eea429e579a563fc9cb14843f057ed6cf3c4105a8e555abf48c677e8ea6372526303acaa22e3f4fc00e4e8c6f2334ad7ba542ad0d4d6a8345a349f88dda91c15e33252b551101fbc31cbdd542c532b0d512b0c24af0afc8fc1d3d883393a2eebdfe915ec75fe6a3164c400c098448c52b18b078b31cdd853ed9015e9cbaf20b33e7d14d667338f87ea78262909a2a3ae4f86d34caff79ce5c6ab2dcda115ffbd28cfac36ecb6d8750ccfc1b218d4e5bff3463832b513e57debc49efd7272ef72945aeb0dcce22b343335c478dc5f01d0627911af9f5c84be71fc419ae67e99d06bc3c891d2fca26926a338dace46d86006bee450bac978e98e9892ae7baaef21605c8b5c89dd0b9848d8d445e599ad68ccc57abcf8eb2b5ca10a6f24071a80e5890aaa2125b90d8a995192defc5d7f4031e296fe7bd31ec0e880026fa31b4d848b72b6d2154561fcfed8955fbe9049860a5c7a5142e82abec8ed1bf705a3329c87706bb30df784e77a22aae8d2586fb1e9e4c9ec3b55d24baa421b3e7c92508373600c96d458eaaaa8ce6afef420a0c1c3014fe07cc6c721cdb1d268f91abd9b47b2a9f68e91aa26d3658f28e5b7792a752c462d5f019b3d816d44c3272ed12ec58410918d39006d999fb73bd3e34d2cb03e82160494cc10a1c1e1324e2e8912688ced88d6c69efd12643f9d64c55ef065dd4aa2d10e756da7f6d755f08eda7def0edf66558c946bcdb1f28675cc0c850e5eee1c774fdf21828f9b37614e522495396bb76748b84aeef68f8f83a91590b8e6b19d8b3a0c35e1479e5944b16b198327db07efc7ec8e557a843142e0a25149b4a3ba0053043d83c2c2959deff1e3525fc58085ddd7f8e400dbfcdad6d9a20fb4e370ccff70cc90b3f690d95bf711984f8d5cb928a327b4bd3d6384f2658cfd42fd98a5a72ff6ca8b7920345e8bee431c9c0a8672841739318e6a07f363e00d4f9d99a57d1d16bd0c6586b45ff8c379f1380e321e14807140cd41ee60d687ac8a9ad96bc5583798a496d3cea6e3b833c8bddc4b0f4a83271aa926b934c4d060ec6519b33988eefdc3d1b1f4b72e68bde5db6167ece9f4b064606dcab2ee45c66cee5410432a26750017f33f18a4f026b62f64bcbadf744583dfccfed7b70b1bb4c8cb28d0f1338d8cb8171e1efd4cf49c80ffaac311e5fe2d4fb2637421f927748c65030006839846609ef2d37321933fba5a4cf244bcf86d4e18b667f2aa5e134f0462befcfe79c106353ca621840ee97d188aa4e3e08deb849613dfc24bc5a27359bb2684c9bcf47d6689cc126bddddf2265daef5258bacd3330d2ee53bb3985ef9a0082248556a5ed110c43bfaab95c997f409c3c7a5859f965432d826f031043df5bfa1f2323d87187950e8338c5d9bafc89960e3ccac92350fb847d3fc6772af01306d7e4108f6569d3b4af93a51166464b979efd0e5b9c25c0e662a0f9e2af800aef70571e923347b5673578d3ca1a7e3903cd2ae9f7fe1d5af85c7fcc9bd6378f8e64d81bb8be371715496fa578d6d45efd819ecde6cad7c14c893292cdeb4b6ccf584acaa58002d505a1c5176565d5d73e2a2a4aad7abfaa174b6e045da8e0f6ba3d62edf6e21f0b6d52a24e86637026e2498d4d76ef6f8066e0e12185135e302cbadf42cb2f7490f7380edba115c9174b2add5d5fe41ecf870c4cc23fdd8dcafd1328ee429f30b1c2c23ffccbba20f36f4f2008dbebec9c9c8699ca7cab8b6e4ae2f14b5ad781244b6cbe5a134b6695ac023152555388a2e9321ee009d6168685fb9c20a99e6061ee0d81ddf21f2e64cbd02874125bc3867ee38073c5d3f4f94aa4402cfed2ab71cfe28e714be1d850eb621b068e99d9830ee9d637eb395dfb47cd71bf84f8fc04854faea613c9681f84db82738e4a65e0e0f107c7925660c209e74298716389266ed86bb7d7f9bb6d6d9ed8ec9b6dfe856301c1a1300ea34e135b0da4750c6033495338357836ac714c56662910cc6020a13c4ee756ee385f27f656afaedded5e69e956cc6f70431e184166eace326a3bcd2a33f13f2b19de274a7d1dc5e2437bc6af00b36ac0965b2a52de107b9bcaa69a7abf9a9d1ab7539d2624bf262f868e34e2a9120fd92e96f5494c625572c4e7a8f75cb1ce74b34fdd5b094e30adea3d073c1da6cb018d01aecf0f69b599b8b62a29641c535a056fdee948889d5acf3921cc1565eb4471f11d07614c16ef704c4f230bd19ec4fc360940355458dc9c90d23203dc0ef1ce104c07eaf896e93781b74e46b8666f0b7b7ff363acfce62b35afd92c05b7245af0a93a7e40dfbfa7ad02a205812d7d480d03c18b64d207a016a9495a591178761d636bf13d9f3a682b447d061d4456cb134b22bfe422f3e29fd7c1896b988b678ae60bed43f20001c40498481c746633d93651000601f8c03e8d51e9cefd083324a007203de1db86ef59aeb17f1827e412627ba94a7f5efd16e324d925fed0ab93910ba5f2e55b82c1d015927316b8f4564bd9f9e3f109638f2d0467e5ac7fe8af37ef07eaf36943e0d4a55fdc7496851f69a04f7ff73b7c42c1d392d9386a58c635726cfbb017333364f96aa58858e7294284f3e9fcae10f0179510cbb4b38b1910091d0a91face3eccc6930813ea2483997bc9ab647db58a3bb69015bf94b78f1c46366db0d5a2384d4b670b66b2ec21ef1848448b35f270180afe7a4c56eb479cc3cc01490f34b7da3d6297aaf9611c004c3f0e8d7f1353b6ff4dca521ac24540ef0fbe0f26deaae2148f89121f9c3ae00dbdeee7105d2ed3f05de47d3c46c2f7b7ab2dab3daf51f66eeb22b897cfe2c8d658d4947ed29795429b1a07625d8a719ed081c033482854ec03ab0d860cd11e16425096bdd62afd4a90f7743db527a87d4ac051c7dcd66126fb588059a96f9a06d51cb4c3c467362da602c666167224198db50e545dcec019d7a6e306f1bdc152f93feace1061272953e7dd4ed270c061acde956cd629730d8669326be3921aded6539cb04ef942c0c22aab58360fbf79871f7cae8dd5a6de8699143f1e60cac38b06356affac15b433052cf616cf22369578cdd13e6a46190b4b7922b0c36a8b463bbc73a69d70921b3f1de68c6485ed490b3d4725e9423668f7b64540a907c298945450d7cf2a852625b3f84f019ce94fe44ef2531a9167708bc3d5c134e845469271254674c751053ac6cbe77b0b22c56dbefe78f3d9a9d40b90bacaee8dad30e1f248bf89c9a8ebb50acc7b3203d74d822aceafb5a2c4def064fea1896f8e4e9e79859996cb858a0fc144e674245d87a75401d9c65200e976694ea8b057e3cf81a4a885dcb7f6ff9fcdfd910775ffff5b439dfead8672a0ee4433aa0d16cc9afc8dedefb1903d3bccd0a68a832d30912eb41ac543c80b7274be5fdf2deb57e7d4ceddf41dc777a61a6f90f3a9b587d47df463990e736726e8648bd61abe59f8dbad010fefd0e1a2ca870d9b28534a32f9983c95149c8f192aee1577fdc75a5fb11724b4b1330e30eeb6b41f533c6c93cd4572949a98597840d933b084736d3226315de8310010a381208d9171d3e705d2995cf55db511ebff75dc7a0c2a5aebffb250a24d2a8cc654a01042ae75f6951f4e12bec20016e422b69732a4f11525edbe9fd6e969f6c5c6f10a054cacf03f2a0273cd2fcee367201c6d41c455bf309ffa9c2af7df2cd0a67ceba7e475b838cc406e6ceb6dbdd1bb8a420f886251ee8ee4e62a0c700543349631b08b20675dbafd318f79579e3c62044a8733bd21ebccbf5a0cd87d42ea4936f01f841feb90f1cf33f0f67df74b56b298603f40755d7fd0a4b762f3185379a591a3ce971ca1706dc528f3964df9605679e4583a3681e0ce68fda62ad68c35914cf7c85b8a35c5235017f2b219c18c19ce211119830842ce63eb91ec42360caf2141545c042c17c3118f0127b09ffe379becc696309b6725e5c5d478bcb88fb3cb4a166bfd466aeb57a90be7fb1efea72e62be19e1cace348c1c9f2967812229ecee2a83e07cef084fb49816fdfc618179dbbf9a72a1cf6aebb5657f133db4997d71fe3576983013ba0e625f9d03a34ea2c8ab80c93934686242cdb5477813e40f33daed83f819522777f3c4f36c77eaecd3db0dd25fb7f05f146ee1bdd4e769596df8d687cb5314042c6d3d887eb608adf576211bfcbbe17203ebfc02d4ee1bdfd31cc2af3d8d97d5abd57d66578d310de657f4d283d8322b9b9d1e961b6c699a2dbbfc5826c8bfac85af4db853b1088296b4c8ec2f5fecf9f02c6f99d976f176a8bbc1606c2f299ce8a19281bfbeab09a334422c395e9d41b59e001275cd2fe4a7a8f94b3af63244a5ed9861fdc37b5fe33efcc095fa4eadf5935333ded7d6d1d49c54659898bc8e543bfa4b283379c765a9e758ee08c1f701c543ad9ed8834db39cb6ec7d46a45a36d8e01d9d459258a70a7796bc99dd6cbbc45527ba73cd7722b09884a2a0af754924917319639351326f1f3543eae85af060d56cf5dc9a67b317d3176c12ebb074a627902458e1d41287e22daa3f404c3983698090ae808f577ad9c8caca34b04a2a8226945619b0b848322f89f86e36d3109b8b1d533ab604ce76dea17d0e6b1a444634a657b233f7c8a3f28083a01715e49a486351ae4915c1d9e8845fc71ff20c2b5f1307f678c76e37cf293fe046d0b2e697ea04660edf0fc103d3fd0a520b5b6395e7692f3ea73bbf15ebd8f873516c8a0d9aa7b43d86dbd1ad5d15278566dd82becd42977d4bc9762daeafe46d0806994465d9c2c234620bd832e8c1c7702c58f8d537f8397f916f3f8ab6d66ef2e807c2d27e643c44c2e2b7b1f18606d0f8643dd3107f398037f70cc8cedfeb3591aa2384f5e8520e0181d78df5187eb2a6907621ab71c97fd6e621217611463bfcb0f9676908ff81f3710c969a197bea7213526adb07780c8fe0fd82d60e2bf974f0d1e00f798894cca16700edf9de00be640504b880337376e27d863700f942b6e3b3820bfcc826a6f4c7dc7d473d6feb98eaf866cf893434eb9b4866475b4b3e2585595ac0aaa913df9736693cc77e732fd17c01c407c782fce9bf60e3c69aa9826b508163ea5fa4019c1bfccf1d7c808bb21215c358d0682684b8f6722d3dbf2893a74484579541360da057786cf62b06f12854509ff1506ec215f753b8f8b98047c35f763b898b4557229709dcabea72340916091234f343fc27a25ac8c0143d493dc817768b25fa418c7d3cbce38a142169545813b5ae9d1ebae42bad23aed88c1ed0dc662fc2894b9d6543f0ec916fbb7bddded72d1782db1284c7e2705d5c8260553ec454645553b530d86c19c92f162324c14f2a5f6e36b887bf11108bb47521cdf94f7ab95ab6ae903b27c6863c5e7f87e66744db3ced81e7069e3c41b2ad1d4fa381388b90b8af51a475c08e9a2ef9101a16b5a58c72febadd62bcc8384c4cc07685cde33f9d7b1df1f27b2cd9dcb99ccf7ca338d861d35ac36841f54db553f5329ad14c2b5f2b08a65d9cb8b5286d20d547f60f259044818c3ef3c17f33466708e7c0be1559621b0dfdd1a199829802a54cbbec90b2565ce1d855fb9cc06b7614c5d2ad601cb4092d6da8ec098aa41ee142463c04170821627f31f812df34feed28b99bea8ea4a64af212b0fc4eaffdc1de6a776e0c345ad7d006f4f2dcb9066d3daf7b9b43a7809e917ae14cc913a884f0ae6851093efc09e9d52ad03e21f4a9318bd04569119e86ce534e16b85d4baca5f9e3aed9a8501c9ff582929dae5c9a1e0a95a5fbbfc73c027c041dbbb7689b01f9bc5792af7680a88fca6ac3997d1cb3cb9f5ec6d3697ccb00668fdcca80bdc0043385e223237e53f7591edb940a6c99e33b37a3855ccb5317a5b0086688549770e147cc16ddfd7be5530affcc4a6ca74806cd3fdf88515332f18cedce7307604f962275a9cdc30394f2e81aaa5af18a17ce5f1d898193f73399fdeb6144dbbff5ea9a7aa7a408893a99155e3e018fbcdac5dd37ea07e1c965dcba373314c606ea5493fa2e14058330c0d6719c237829549e99db7a1b8ccf52a1839d38f0719fefe3dfd92485d36ac18e66d5b1dbfeb7fbe9b5ed836c223c7fb1fd789aca10e997cb1eb7ee4091984a2965277f501cc8bd19cec519dcffa69086b4b5a07f498ccb1a44d299f5ecbf4fc0c75a4a90f8e32951d83443a7fbfa1afeff2cd5cc286539a400cd995de386b53fb21765add41b2cf5bd217ab3a2dce2ac0ddc43b3ba49aa4308b092f5c57b6bde4638067dca53c90455e72699a6dd22660ad8e81cb96df51df599353f7ca5cd43a13a4d34b190ecbc2bd39413836130cff9a261754b2f3b2f25bb62c646c2fd6fd4cafb3ebd9fa9b96b6666088d4663aa00f35619a5dca44fbcb049630e3bf4ab57d2e1b687e5f5d57c80c07d445c901dbddca9bf88bebd87b4b5f9644968c87f54c8f1958a5080f2502a91730424e19c562a0d65b51f21a412f221410c8baa53f88047534bfc54c622b852c85d1f16d81817ac7a6e439fb4e324a83f72afea79fcbbd8d1a5febeab83e19dfd8b2d8963ee8294f8eb92d428de11829bd38569ea97733479a9c6c63f996a116eb613cab98c992c2e4a310de88b0e915d6543667de2b9d3bc0cf3f8e58e3bfe3fc634e8db7ace0d970c2f5fc09092063069d80435151745187fa2092dd6f7c3de4b82e0112eca67ea05413e7abc703f57486c191bb7e54f91c0638256507e0e8c6fdae79e2d01adfae447ef0b26dc11643cb79f05ae2e807e46cfa973dae51de89cfed0a2e134d39225c37eadce8aa9db5453308cc2940a548e4747fef84b9cf11de4da2ed2847037a66a25fe6ed7350b06b9f72936ee5770d986b01768d9ab1199c7fbc8d33e12aec12bb6308bf87b258d2aeb5f24c41e338cc241bfa57faf0faa098fba8380d56ebdf559ba873c5fa7d98206835c3621eadb1d1ae7e3bfe3e189aab90429d4bd4aac75db04b7700a1d5497a7904bdccb302a8e66fcfe02b135f3e2e7687c0ae23664968cc89f3ba4b10997b1b0d2bc5ae837924f724db7ddd4a555e1fc3bcbfe1ab653a6d46fd35e48fa138b60ab29db12034d78a8744b49cded0e641f8ab691769ddd3e994c48b18e58139704bb57b5a9a2883faf6df1dd5493f305a848c2d6ec5ed287e99a83bc7acc7aa5aa65eb9122784bbc5e68fdd6373ffd8f936957c258a826bf59d20bf83ab45adf83e46a10486c41238bf30d008897d87c977d79b553a665e67928ec246f5f3f6631a94d1bb9e1e1e85166f1e90c00891f6eaa8543ed90e0111903d4a3b649634182132bf6b7f66265d76debf262039ef263055e990f9238b39f2151975992ae7fb77821ee41936b02983d6e126d7c548f8f27a8da08dbbc92fcdc042396008ae8a6253d348ec85f8fe853be721bb9f693557cc7b869e0d84052f50fa93d590f40b1f5f1a0bf550d6a965426f6f61f4c9ab196e5aa5ad55849cdda0ac21a4ff4bbb072344184bf38e8a45a6485daf62a31498378d30fca9b3c4269abff53f09ef316f2077a7258ce918616017da6cc33c9e4bcf3d6edba3583b977b5bba1fcd2f32d46c26277859d49cf9e9718a2c7f6dc4fc3f98f5d8f42f8a794831dc7020e9d01f4dac8643e1f2f88ee76515f1e94bf387eaa104aa57d48b1caafa45ffd807225a90317b6c36e8b89a6a36cd308c327fa5bebdde5b5d617cffea90a163e002f3f5597d4de7cd8887fe03498011f470adf188fab07cf9daa6a2a66c77183458946f89066db2f4b0781c1f793e7d2299c39f63090d3786d3b8034cbd907703e9ea3c72dd6ff569a127ec03f91f3e93b198276e2415bb8a4052a12adec8cf9a5478049c752ac8f0f2fa4e15342a2a4b23c9eac77b9b52c5d6ced38d0b2c9efed2fab8fd86818f34022586612332572753ea41fd11b06be6d44b07ef40a907a5a88ba52fb86a14c8122a9ae82ccb6b62fcba544ea738a4b950667a7301314b502b89d5c77ba9670e11319dbd3dfb7c61536317a2e63f41e31da18f62d58a6b1659c794e69fd88482d83aa89d6dcdeaeb7a717e9528003f3f1ef185e90113902cc558df3437314d7709f7646587dca21093407b72b5700aa0e4fce817d29568ec94e7d37da7294b4d5527e9b2447d316defae1c2f00038deb87ded0da6484641b5bab0f227f6ad59e2a2320b1549123f8f41245481b016eb0d78556790e70f94d673a0ec4a40029fe7b1a89f53f9223d1767785d3c231cbca538b72253f1123b7fe1645336979fd05a69d4f81590f3a2cc7b3b31027c2e477037d3c1b1540089bb914c3083d0a02a7a7d5f04bc725b8bc90eadc23227cf9a108de6beffa953408c0b49ee821588d0343086f9ac2dd822549a5d2cc475dd42d83a23edb5a5bc1e8110ab5b33ec98e4c976149f0f74e0c2df1098fdb810f0a518a641f0a4ec3be361472f1e678db1559ba7b14381fbfcd3ae63cfd5c6362e08d91411843373589c071d173f38cb8a0b31ab3be8cc6f7e52d954475db191337e8d2a56acfa4c5e1a6784d2cd597d2f0d60b6754962ddb34ff44fb5ceb85e4254482806f005df2c6e3fb9383b3bc4459f2fc846dcfea3703b73cd64b2e9ce6553242490537f891947a24bdcc480429d67c4865b289df538c298a2b161972bd606944f264b8e52a23d316f65aa9915feb2e3d9c74cc0125c1040ae3258be3287529ad90bd66129a42b9e2e9f5ffff7de0c2feb17340b9dbd6fdf69a318bf9732ad426b4dc0fc13abe68a517d5c8bf8a2023dd0eaa3359b9fdc7cba708da20a4f3c8e1e90380d991da4a00acd96300493972ddb095707e3abe4337e1cf380c628ad8b8f7737da98ed3b560d2737b352df7eb98f7e9a4724b9614eae0dedd98c43d35967a78e46952d2ae53b2e580d3c38f572955d1ddc3f18ddf9b420e2a8cc61e028517bd38e6de4bfc0cfa4375d543b9126f256492e705f460d70456634084b56258591937dfe118874f01e674e158c5d4cb14d0b3fa98d107b2ebb7361e0755a883d71c53abb89c9b699982edbc54695fa99d503ef8b7b1cddfd4dd7c18f9d045fbd03234d92462fd59919e95802c92d5aa0b3467949365aa1b8b0d530a280f81033a53639b69efa6cbfca0e79fff62e979441e738127609a13910d833cd4ff9ec26c8b34f56bba87bf3c52de27a3046905773cea1200c840e58469b9e2415c5a8f942a82df4d40d755eb4490d29c8e1e7b92cc126c33c9dbd4008246eed765567ae8d7b18b1f04bdf910dd3fddde41e5cc9630874fe79c91eb1c620788b296d8fbe2d9b54062a17db71550c66d4a4667d9d528edc5cfa776fb6f0ac3cbc1589e87286a35ee402ad33ff918400a4de061a3a84707a40acdbccffedf9ec2a51827c19f052ab65bb6f66a5f647b4eeaefe4395d112ea74d3a216ff13bd8aaf6ffb96779d3dafbf251d18f01e2260319db207e7c2a357e853be7281616a90b46280c452a20e78352dbe8a7439e63a093268c0cec3f6c53ab04aa2b9555227ed207ffdd43a9e068ae7fef87880ccfb801b2682005db609688274fa46faef5c5b16c3e59add991c659738063e1b17261899f63602feb64b344eb55ab22ec40ebda2be31b52efa215a8b8ec14ebe0af2c6c1d122f2ab2b300b893326be2b952426cb952e61acf2b7ac1e63974fb246ddf26078a4cd0c39762fc6a76ddad5ae94191d15295b257b010d9e031c7b19952beb8986f9cd36f991c282baac015fac2948a8da87af96e20272b44b59a464829e713ea19f10fae6261282f4d1d857e1e09168b316830b939993c23735ac21ba8c04d73fd54aab4cd5f2dca033e74714ba579b1f8114e520a2e20a0140846a8405d9f8e22cd6401f6e80b8b1a2352997e843fbfab9f02dd5ea96c71a36dbe0124dcdefbe47e8196d8bd01b8c842a710cddf8a6c23bf8d1af9a7f4fcd6da21b0ba1d196d8d6e9da5b76d9cc18a80a0bffdcddd99afc8f5ea5a6e1e1dd3b7096a622aeef3768416ca770bcc60ff7a4f91d479144bdbae716a6fc5dc54f5d454b223914a91c74b669a84099436d1e23ffab71650b2ac16b958e7a688bc1a4ddca244e06c64cb890c95fbcef843a92cad1b732f32928142b53db92349989a53c2b19bee64da44eccd64f4dc31de3ff0ecae193d45ccc0732a3e7248f985f4eb686b6b173da64c35df59c8d5b0261ab3e3900313510ef27c1c743f5adbbfca6ffa7725ba719988cbfbbd90e927bf8e6fb7f38ad28408d13812045dea56f3207d9a899c28c677cb8aca85cbfede13e3b567595f9acdbf28776543960e1ac7267665acdf613533395957c05ae8d0a233ace5ea5c3b97c346f1937f29573b2f523c621e45e4ea88931bdcc0d7c130bfca42db2983ad638be469e5d43e83cdf86e3cb69fd59d4c9991437bc4bf877f1b128c2614967f9e0e15cf3ecd5a2adee5465f04118001d7e9acdda0cc1041526ab608fb4cd2f9d783422e4a20a4a3de1baeb12a1de59f7d261c36ed256e62f5035015efc444dd9b6b5fdd45e8607afa4114978ae59cffd9ce0c85ad21518e97f53e19c7a13eb0755a16dca7e0f4a3b33b726becf25a4130a667dec40474029f73163fbb262c9df473ed91c6d18fba08d4bcb7a8dfa7df6e450d622d867ed4ab5746a5f540498fd0c44d2d6bfba9df418c85b4d89cb77df68001bbb817085e095767d42f36b2c15cd1275bf803cb968a2f65aa887aa9afda461bcf1f59d0a945653a7a8155e7f8338b2609f79059dcf7661a499616031f1c94d9086013da5375d945ddda51a489f60ae12e660aa9938f53ba9f9132d97e694a50e0a1f185de12aeb4ba30df403f18be43e3fb6f902799f8052ac5998cbfeaad6238805b96e8368c919e43ea6bbbc4e5e6589fdd2d8866e7cd248979fc986c42f87f21aaca2b8adb875ea0120bbd9d2d412e67116bf675186e4a3179cb7e626d80e669c246a5b3aeb2915e2dce1e7d42065bc4ba08ce792494f07095a15c5bf028c1a982fea27011db6f1faa30a74f37f9e7429d191eb8d58fe426a71fe0b792725fc431138aafd4b0a01eee85b7e27624d70c3121a08e614b9d3e2b237e5b0fa922f90090f38f9f3ec5f41389be58191041094f0c17a651783ecb25f563bf4cb2de0ea14cb90a0cf1b9d33708b3ece5a3dbc203647feb9b59355bc061cb81afa0dcf918e4466225f115985b2e9c4dcf7c8133ad9708eec6c0efac54ef8d0a52ec83d71a2624de8a256c78bd30e054089d37a0496e02db708a5a5701b558977010bf987c0cc29e4973ad6d4f7b9319f11a5b4309bfde3158ec1beeefa25ac6ae59fbe95fd68ffaf69b8352a2357c45b691518699d0cde3342d6cabe4cbf42db4993d1c92a7cd00e409fb546b7fef8b506e8eb44bc095a628c7d7c5ba385d7854d8fdf047cda5c4a04c3aced4be3eca72ac120564ed181517ae14b105da949205fca2e48cdd72ed776fc896a0fe155baf277c0fd0c8dc28456f40970f5343af3994b5c9a05231b1ddc86991aaf6adf485653ca9d71c1291a8b1957fc5c2e8f70411a4e4b8b093bc231276b84d00a589686e45b0158b239402d2766b784836647cbee121ab7877c982627d813c982a30610f2603fe9c9249e63960d5eacb418b223cb98b913ce94a333922a9c28ff93a985aacd96a359ca6693546e24c6589bc72b6519aeb0143bcb0656440a615a6bf1cc9e3711c205a87dc4da42e2347d7b73067f02cd91e32398bd0a6e5583acaa1e5b552e865001eebc43fd24ee4594cf205e60455248b0e2f582cb86a23793361efea8c9e9159466a677fecf2d7fd7233327151e3de0e90bc1b8648838a3b46fbe338f1e8306937689b8ec6d69973a564e671825690b81ed62b8962f172a593b789399f7d7e40c7f229a1304d8efcf4f6de9f7669a44f894ac6fec758e6af647341a1e63b4051c7b60f2364adc59aff7f03f263e3b2f9eb38dd6512a28443a886f88d9c3d975c6bf22be14235c0e04218e408cb264b299f174e8717759f3000ca28f69255cfe4a65db6a9bc021e5c8e6712ebc1a6bd8ceed89581da8dc31579a883c5a3286de346ab91ae3dbe9160e02277d3006b2d1d531278436aee197070009ac3e773f7dc59913472a1af457bbddf50963949f2674aa15ceb4480a636d9ff30d003e3f00a6f4f2f87afdfb4da3ef2e7d0181277001cf8a5125a13d57c6ab4f3c2fe4086a04661552638540bf481170a798d8a4e6c1b88f600938df7245668cfd00dc3b62b5f334f8c8014b4bcb7f285330a0002175e936e9194778a0c0418d056d7fd8b6cf6031f545bf9a502eba58f6987b78dd3fea3900a7a246598df1bfda9988e1835e2c482f613eea082e7e13f704e348c5d6f5dcc1e5f01dc60da52cbe60526c67ac13eb9ea952930091f3ff2b9db478ed7b0ae167709ad3a85c78fc8f9baf756cc44d033af581d8e8cea7adf148251e11e099d36efa70e67db36d6ad5455b109c728303a8c9c7c2c3506fc8d844d4e0628e8af07e986202e00a27c89292979c85d0310fa0ce4a0ed1ad0d5c2ccf3ab69efa7c44706dcd4440834c6e4c04d3410265e3f0d665ea381fc17e7565422a406520375eb50d480c261fc5a056e81166495fae10632c7eca962d059c6f31451368d5f063cb8246f7d51452bb399bb98768bb209a307fbde0e1da4584202aa435eb6f78c3f098700777ba568faf45b30977ac7acffcdead3fa4291a679183297d3474f623c9a2ceeafdf71716bac8db3d3f9017f12e3a6a5131b63a10798947eb195c8e1fe27592ced19910e13d1c60031d5465281921229798610ed275b9fb313b41035bff5ff944d0e917ea1b9a0e33d718aab0be3edd14b67e1caf4f398afa9b520cf6caf66d2ceb0320aa58c36bbd153f16efe703dcbadebad96020271bbde9084224d0b0a4ec5513adb34a97f58658b076b6c4aee22cece0ec5e7ffda3d2c59f0d912e04dc84b5a883ec44e0ab57334a18c1760fbb4044ee32ad57184dd159c047bfca8fd69f01ef838c5e10564cd30799c7eebb4053acd7474a6284a73f314343809fc6df0bcd0427c467f9fe495fe8376271489e5106b82a7822c3c96b01d76066b717b5efa28989b7041cdfa141b7b2b14b219db80c955cb091a7306f4d942721e02b9f02ac108e914c5a68b83417d2d00767599777c2beebae8e38d400d72e7a1e64d3f8d693394e8313a93c2222920d62918b5750142a0f7dc9f4dde7cc7ad80c8bce7e6bc2878f96ab44bc776ecdad6b3a09d08c4c75b1e0ed5843d4786a7abbd45e7e55514bfa0e02d2efe626b7ef997e8df5a780307cb5e2f272c6e56d08b76060fc38ab101f81d6e4c1e183e23f3dc93853ba70efdd05b11d5f436d3d13eab83a061d7d99c43253078d9417babf99962ab495c0289b1755eb8b7346e3815c32dafe1066401e891d4af9469f53defd5a21031c0edf4bacdabeaa59a820e50bf23c8a95ce6a32076e130fc9395a22ab9798a7e384662eafb3bc53d7ea860649ad5160bd4edd62bf2bf51054b8bc9ad5f416295df6bcf2b02c3be3e60fc9464cc3e9facf0da996517afd39f1884d30b124fc297391f2145acb78980ec2d425b915a77e6fd9474e80e4093a644cba9370c7df262092a3307f56d0e25fb033be7a75b157ce4bf2005b4866d6187d5edaa2a31401743519d4be6f65eedb3a76077ada929920e89cf217cb7420abfc3c8f356386d8e6e4b539de242b829cd45437bf2fd82ba9b7589d53e829aea419f5ab195c5083f9243d5eea14f7c3d74abbb4113b15fbde60f38c79873c0102ccee36f7efd696b20c06e8b99b472120c655e4054a573598f178ecf4c41c6c6d104f92a7030caa001eb699d9a795d87dd5c5919b68976bab58531b34dc4b52aa04013637b6855ff9266b4319fcf63ef882f0dec5f76878b028b46211602332b23202831b56d6d1bf7802d9dc34ecb78f9297c59e285d156395a4d92b6bfdc3a5709f64aef341d5a34b8408f2d2172f16b60535cda66915104ff8c26ab14264ec1d660003ea430e47725358afe9c1927e5aa839d934760eca63abd11a41b451c5b4304bfd42a6b7a44232f574a5d458b9b1818c6370cb59ef730fbba9e0a105fb2dcb0d3504db3321f17e4388cd977b47a4b08792ffea14d5754205383a5a5180ee58734efab4685142c59a0d8afb3a97f5c83975e91e0b11b1ade93f846863b7b9df8430ab44eb1e87bd58b32a175fe64a28734f9a6d545fe853a293a2b40994a2e30fd40972cffb2e2b0fe3795c49a473b96e811240d2f37e3c456485ce88992f1202fcc4f4459cf6c6fa687ab6298ee2a4f45b65cda7b09facccdd50e1351d71654e3c8e4d1cd1533d9dd81665e4a6c48be84b37fd0258da051e80c44a9e5cae5cbba5f350b404adeb17b8074fa1464a519d5b9eda88a300cdc8c9095b375f5493d7ff390afdc185f620a17cd1987d21007c3d30d8b3ed932ba002d0cc937467b8a25b06a02dfc44679624197a7e7cf4c4a5101483fa5b303e4e523b333a12d9303d571c583d18ceaff7bc32588d8d9dc3ff230c493ed05b28f0ba93c3cedc4a640ef6d8da758df00672072f27cb3abcf75c3e5dbcbc4f77d88e316f72114e4687c0453ce2295cb27f18e2c9bb356423ff91ddabe3a7fd796f3bfbef6f1de42d939b4050e33cc76e6a4b32dcf831bae5fca12ce60e515f0ff1c4e776ed6ea0b92fb4c069031e65df250ffa1f26f7c13d8ddd87951962109c7ba4e5128a653d572ea7edde1cf7540932e0fee76d6daf7a8e3a3450dda82dd8a6a89d6d616e6a5985def6e6786d3c421a4c3cdd58cd46fe35b86e1aebc04bd54eec4a7d88ed13f006829ef9d85cc6a04aa4ff8e71109adc212043da905bdb006b06814f5e307a148886313998912c2ebc0ebd90df3ea0312bebfb39853305e82a24f761ee4b5ffe4b10db4d195a208afc6b14adb0f8546c5db5adb50bdfee812a85487a44086d5e6b86960c9f1ff4dc8bc7a2250c3178f6bd67e00fb12bc393fe03099d80ec9c98629abe5107aa25641bac66cf62d76ce67533900abf0aac16338ee75ddf7fe69dd1881d7f3526bdf10bf8f938de49e6834b5cb5b1e3bae8d97ab09900616e782d4dbff61dc8a008ce6f4488a051842495960b8ce48017d563d9dddbb39751755f4ecdff13ddcf16ae499e1547c7d6a05b4dce2f48200127be0320705d231d469bb1394093fdb9f2aff93522c0b7079ef9ca98d4f02ae8afcf308bbc769946a9760190fa816fc2893bccf0ffd4a5ca7396912ff383a523a8c02742dd25dd81814202ca7da847a4934e70628058df97be2b32666ab67a2f42240b7bdd4210167887d78377a708fffa95245bcfd800f4a5b85e6f3a37c4e7230dd33e82da4c7c8941838e13e8ea6596f825946d937018c38a0837ca958542c73998c7f86b88b268d8a42ce0efe274f4f971e3dc082aa94bf8dac03a288d2188d92672783f113841f34b964a396df2a32ed14000977ccd4f04ef1f45783c9263e98846b68a9a2b42f13ab5039d6b40a9b7a93ddcb622a2139fea312f8dd93746a9d77e7fb50bfed073b5d79d7d96f581c4d77b54395f7badc3ff157d290f13c137b49364c754783f43b5277ef7145a58f3ee05c545ee7206fcc3b3e9bffbef6c4e44544ab4bd2ece265ea3d516257cf845e3b092ece1746bac6e355b3691e51a52f6df4b712b6b1e971d2e11be2339c80b93234cb17ed97c38612e5b8de97bfe92d4f2721674f39be5bb44ad18765fe7c20391230b5b281559b3e81a24eb4a08161b11aae4b4338bef119e2fda1691166b95d74363ba0d079fa7cf7f4cb3942083b93dc9b673840070b5f25346be3c541138546c5603c1675790986d0b5c26b2724bf2ca0f65f7606a479b6fefab8a77194967a47f73745d9e75e425b232a25ed0c9bc2aed8968aaf966958f658f7dd8135cbf9d210aed5248e954b578cb59e2ac973dcce729fef176ed16d6fee7657a64ce3b692c43422935f3293e94ac0ecce2476721e8e61e0d104e068bbb6153fe6c940268eda6f157bca4edff56fe6cd263daef5d9f8fc3ef32510b7078a8b379a79b3619935b34deebcc95270c52255549b6418b390e48906af1144729babbefaa5566746e5718a83f27d77559fa74eb3b315c3098003adfb5cc9b1c509133c569ed18f46fe380afd34dd01061c010a4ce95e9e40c6a71189dbc1fe229ab28f1fd437dfa45dd33f304449a98470a269e038c4ac09c8e901df65f1769bf53bdf16c0b63c125524bd1cdb55a1d67d53f9d5b6d7524801c63cd8aa26fb3be021e4efcac81eb909f2378d96ba1ef1f7ce3a5f400138a8aee04144e568a27d2d87ff155e7ab2799bbf4c68fbcc49ba2cb8a5c47013faa0aa1c1d8389ba9ca258bab55c6b6ce5b15a1f636c7f19a3bc73c7bc9bba2e32481d93e0e0fa9ae51fe7579f06415abd2a6192527c1653b654e8431a7b610013966f5ce06c45a51b5b46fd201c4958c247e53600faeca20c0887c5ecd3f64d94041823eec55f166391f847c3f706dd38dd6bfe20dddb19ae37dff46698d93615dd8cbbe7bb54c0e3ae2f7708b94227d24375bb2e9b776f6946b4aa0bfb641f3b23509bd35fe70094b2841da9b49fe05851f0452d93048cf2178c870464958c1364479cce38975de10e17145a814d6f80c52047f89d834f6b02f54f4382f7ccf0161519cf8fba76ae0b6dc7ca8a7517a6ab48272b6e37baf45c20c5337cbf409e39dde2c164508b5d50ce87f19eff0d2122ce986f261b481d743609fdb6c4a11f94a5b6a66b794e4bb260b98f056c26a3eca80d70c0e1a478aa6e4af8f81cc6d609a78d9eb34faf85ddfbfdb68617cecf56a9121053dc8768f6cd914f7c46f919523d25caa1096d1574243d3f9f36c15cf5e593187f32d8bb037728c449534a87d23522e85077b50c9935ebb821b84b381eb0f26f70b10a18426f120c994b900968d4dff7934369c29f983004f8c8cd758bbe94b4d6a34848c450203fc2fcba9369e610242463888db5cc340459f73390bbca3ffc5ebd4d4ebbb793c0d41c9dd782bf6bbc33792e069c2eda30da55fc63346acfbb16fec8dd71abd44917aa5423b5f548898a575089e56aba90ae6f4bb3bacb917560dc771aa637a2af9243763385baefffcbc31fd2a8106cca75a61bf1921b3dcd51419c4bcc6170126113537bb4071119e603d72e4b6375658180890711f71803dc73fd8c4b9bc37c3066a63a43032e509ba894f6f83b328ada5e22ad95553249eda24fe7f4cbec0347ea4eb5b744548a695e46818c5742399afd756f94c986c5e08762051e32e12b1ad2f31829e000237ca0792981206e9550051c138631b36feaefa5e2cde1b9ce55c891469057083a1e3dff76b76810b36634c4bec49ee215ba20d885d16d722090019d996ddcffa1b0599463b72f3981b54f975e7fe8297de24185bb12703cfe4f5d583925299b35aafce98a8860a08ea1054c47b6f318d376eb7af244be468d52b2cc52e24a1aa192b97013e8ad6b760b7f0362753b6d069a9a050b9b35c279d018c0dcf7cca61e49a5ea7fe8fa66de8a2c76533f94b0f59624ebe4bd8ef1b18e22e7ccfb4a34180631178dc51deb6ce3e553e27d99bae47c6c3424535fdbba35a16451af692c15ed88c1f24a6d24f69a2790630a2120c110b9bddc8230ada5da81d802e20f8603d80c42203efd52a0e97a91a59c29acffa66b72d73b186aef3327519602a54143deb91d2fc9ce3218eef9b4a2e7dd296e8745bcdd6dedc3b93f405a501e2de2560890c501e33f05855bbf6903e42f0109e29a85cf6a9750911cbd6bf5fb6bdcb98df2cfbdc0bca8f6d4c2a284230e941a693aa42db2229a636914560011e43ffe316e2a7b1e15212a2eb7aced0abdc8d14fc65119e24648dfd3af8251bea83bceaa8671f4db396c16ad851cec281caa236601072e9dba0e10c906d22d6e8b3600a123c505c1aa61bc59c56b45ace605afb59d031044474360fca0d9c0e513e3a7af0f70fad12f87f96a7415948a133ddd37edab7b40826f5ca3d6a208955f52293d7bb8ca17a6813a3a4d2c13de44cd2cf2e61847349900c5b829ae7491beb2e2a0fb58a83a0034b004f1b1d71f497221dc83bcd17656fee0484885b297558ccd289962feed2e777ba71a0eba6197d966fe9bc9b3f0ba21dbfa295969f4b5d7cc461b0b5f926278540dd49bb52ca741cfedecc4859cffce62fd8317d65effa9cfef9e99dfa7ee5f373f7fbe9f34d874fb783f5d489c01d9fe56dcec122f4ebf78f77e535ac92f0959753a9caba41568dd32ea56a285ea8acad75c8e02c5cf2980cf1844a4a63699c580cfbe662d2d67f7dfe11f76ae8d02815f71fe5c9281428f2ccfdf6485729139e65e297ef2456422b42b546df085b09c870c21f2bd1aba4c37965a4364b21e80bcf5b51da1ccb6a889d4610269a8006925b4244d79255b3b86fc033f98d6d114561d0663d9777577daf5079f907db206e0cc693e3a57eef8f8108aee597e4c67335a4cea2900235e0d5dc4d68e2f657d70441f24311065064a404f5c58ae4e47267b7672d72763ceed652cba0921924fcacabd97c769360bc56a7382a5eee3afef221954052213ba338810d125fb13182a08a0aa793917b021f436d5c6d6160dc5b2057a7f451b62a986709c0afaa8b7762ffea28e581b946f92ffd8da7ad1e3bd5c896645ec1fe0ff7fe8ce57e37e59d86cf1988414bc28a301308c9aca2c1e037fbc16df2b658834a51fd4eba681e6602697c1a24740b96f151ac1c64f0e0a229c03f70f04c7e86dfea70026c77669709bc52c2c57c85cec4e6a354e7c8ac075a96c30bbef48424388deffdf9524619120b9c163443db4bc82dd27f689178d69ec6621dcce4a6602622db07b5a310a7ce8a60cbebe0c19e3beebef46857ee59f8b34a9d3c73ef78fcb8ff75e85177e2a5dd95fd6fe83bcdbb285c1f190039cb49d81ce004d0a3de4a7f03e0533f57c235467ec1e12f95846e4d8658e435423506ab4d4748ce6d53c521f4eedb6be9dc9c9c487f6e42f670150490ed0fda679f1b86a4cd8599e1232bbe7f7e7cf707a3be59906424091f6e48241ac6dce4155b9e489ef7207030a4b3fa42b872abb54241a744ecd852b0392c8dab7d56e25acdc563779d632df84400cad054e9713a480142f767299a1c565424d0e31b52280ec3c569ee661b88bdda4c06a02e26c2a2374b4ede681b7a6e5e4c5b2a9b453e2ec84f8e05331706f66b7cbcbf6a08ffbf1ce5ec8ae81ed0c2a5f182e3ee1f0da73d3eff43e5239947de2ce22a20c5ac2bb67137087c0a70bb327c00023c615039dd10eb5ffe9b8215faba8c872f77b01e919e30bc736f935fe1542905219fe010c08cf10dbc5fbc39ceac78d10e4d0372b00f4b2537156060d53a5443f02431eb3c2d79d3ff6749c7c7603a96641da1a04da7393ea62f684be10d052cac7099b7b2d39066a7298d0585e88942920f082ae9c035ed84c2d3d38b0ff797446535de35859d3d36d8e2fb1034a2044299c6939fc995a8ae11066ced3f590f64e0f05b674e5db0f6f756c401c43bd69ecfb465692ae48fbfcf3dee50b70154cf117d6c89df2eed69760d0d0d7c1a0ac087d723156708c8ed7ed7369716131552a08d3cb6bfd499b59b1b73e67166a95ac7d6376783b256cd14d87dc56999b7b6bd4999d09d71ccb1681e4a2c9a7c56f5c0a240a10006f39c0e389044ce68a0748a32a0c7a5061e4a2a8fdd5b17dd2ba5bfff2e3957d61958d4ae2eba93f0216403712c32a4a1a4d0df43dca9d381db97c601f00139c37d3b6460fb4344a7e678049b80822a80ec69795df5d4d37f5bc81df76abe1ad137af8977f7d6bcf1c141334e50f798450f2f3e4e1dfcb163e264ce95a135190593ce03b95e0c4626c66ccb4be4476fd058e0936ae026bc0f1a5d8997d545d5eb0492b3b10e4a6bb45c496aa229c867ce483ac2c21c4402682e6aed48882f141fb81486845c0e0bcba3b9c16592e6b81f0cc2aac7f684172930009e587f77df3412147754a33ca6e751eda31d2c6b259a9480675375194ef3e9c42dd71333ad99cc73cfef2f0cfa512d40fca8bb0cfe688360be3064d43a294ca036561dc089a27e912252c6157c3ff753e2b1bf435623b05e4f4502f0344d97d866bed7de878016630bc52107470103e2170aaa049746a9a7df7f3b1128a26711c47113e3437ff792666c503c6535326e962a5150dd939e68ef4f503dad87493a5296025ce7b0588896b57b321a55024ae197139df19370c5dd75305517464465c76624019c707005986b325f093d252906686c775ac36b3642a41f2dbe246d61ad9b4c3f8534357113b30339fd0f58570f132aaaeec92eb87ee061774ec14164422359a185a5a0bf7bb4d708c192232f5b4b3e72ce6e9a1d8121d7c901a82f6ccaf8580ce87576462dd52d57d680f459418c2fc40713a77d6752a958b893abc288ef1a546cf453b8b058c7cf4bf624472091ca54f01a55b7983ed87355258ac3728b72276ba55861e8fc66b07df8923402eea9040b72e70bdff74d086231ba0b3572eb740c27685d39fdc03fa246d79b1982a1c52cf4cc4005c05981deb5b16f9aa8020024a86b3780570177d99bb53e910ddb4bddc216b2a5fc302ed120ed2cbb2d7be8b8377063da371e4877bd3b570e65f1685d0a2400c9b1b35f225f87dea7c8f2263149df658095d38cb2582b3170e18c7fbecf013c02da8ab0b693911a19e97f85ba3b32c54ca9366f3583f7360000000016e00f44b09de365329b9a965989be45e88b0713fd50036f589a42ef2647f4a6bc7d83a952774cd6fd7891c54ce24211e42bfff25f63fa1ef37430d2280d5b816fd812476b48b575f52a9bd20c31b9e7f7ba7212157d47cda641fcfa64800ac0765908d5824455869bad8986dc44081b0b320029131bf44493979aaa22398ccdfa711f9812e74a043b1a2b9c67a0023e7b59550967f81d9eb2cfd99ad91714ed8133bcc80e02649838ee45e44b98fb20c02beb8a371b81d1df18fad1b76019ff73a5c2f04f420a282ce201ca2b17e981e4805b2587d392a36b62a2108e44f1e8af28d52ddc3e0aa70578fa3c44ed9642291938769f410636f731223c12f50ac18eb317f583ac8d1ee4445a87fb2642aed67a3bde4080728be0c9c471c7fdc6fb1ffa7adf75ddb40bea7360e3c3d39285081318d47e93ee7fcaa2904928d4547d1a88fbbaaf958865e30f9c517dafafd797c6f952bbb8ba1fa322ca714abb18f052576a7410676c0ea8d366730754e308058f775a6b3a198315fa3392c8753d8c81ec01f1df6a51a4ab8962e910f258ff290862fc0397843e88ffdd0416e892db70bc9599b29f4ef8718e86fa020125a6f4aa4ff3052c257857474f98a6b46738586da4470be91936a7badf3b00051f071da562ee416cc1d31b0a9323baabc704c743804de692684abe1fdaba3daf3493d5c252d8e23449ad6e194edada6a48d37e5c50029db4dafd6c20669acb0b2894a58f649e79bf767041d045d2a00513ed0716261af1d20dc2455a89b3b588725e064e1742d560313937a3069ad89883e6fc0016d7ef187eafa6d65871ec10b94cdadfe44e03866d7877f83d91f556490b8dce26876291180d33fc9df99522f21b5e1b3b6407b55da2d0b02a09fd49f26b8338ed264a5f1d50795fdabf6d7c423fc8f3c683db59affa1bc42ae915258470a2c7d30475ab25c203d11a32c62d5e97b742fac3e7c67185605e5fe49f3b8479eaf46b6be7a9f081280503e796d1b2047729ac629a84cc2734fe61e26eff14ca29b8412fffc1a317c1ef431b897f6c05d107e82f215b427e44c07c4dfec4be906f4e6a3eee4a672a5728e41ffc1ef6aa7b1b33a2da544b444808b7f44a44845c9698cccad373880deac43c54b0fd39e479f617b4dcd22a456ac18b092cfbe960cd28114777ce12ed9882faa82e18390efdf31a00bfbb53725a1791e1bc01af358ef60ed914beab2631e92330b19692c946620870ac98282cac781243704fbb204c1f3232ada689ee6812886a50b884bd34640e7cdd460f7ca5a04cbeef91a90356d4d3dfac460b44ca2b52089cd60e5b98b9a0974b105a0e81f8041ae3b41b5ffa6024f27012202affdbeab345c36b325504b64c66c3451625e03e513b6c818fba03dae4b03b3d0b4b5921952164f8aa385aaea7840d7463406b4d88d6c7606d73c56984958ee8d8fbf850006ba28003f7480f6f61f02e1c9f7ecb7c8d43c427a3719c20a289e797b35279a9b4359ad164211412d689035c3aa641d65d0d6e3a5514ea8bc0c92bc16bd9ef21c6429dbca6fe280aaf39084455aa9aea7f71fb91ac89d41b7ea5e9c9addbb3128ec92422b50db833f30d39f2c6ce0104df1201782a44ff396484e67d4c373825d04877ca7b5737df071b986187824a322c0e1580e1831a9d691fa93d8299260c4b212a782a984a5d147073794e4b12090744ad3c4591d8b94d46b3e23e20f942679a7bbc55de91976a3a73ddc3f41acb00a65ad9a51f00a2e889af874a3fc67a30ccb6a054566555d2218f3034659b1d3b678ca8bcd76100413088df42702b9d96737c605cee880a83febe4bfd217adc7c8ece3d5b453c58b3670a2719254df066b9de8500eb68807ef7d1d7e223aa06b5e214049e4c8c3718869e0d51517066cc11fc3a3bf6b0e2554288663fc85b88b5fb8f013bee41466a5327edd813ce41444850a2898716a111a96fa804931026e1121b37d7399efcbf00870544c99610f8f88ea0e35d0da8847de3173274e3d927ffce48115f5972cab7e3b7b9b74d9301d485bfbc89a20fd467fb24d60dbb39457b8e43192794fae331baa615495b31b9133c5538dc5c417bca801e50c13c51a3fd4a74e6d770d05e7f79615ac1814f5785e5695139e82ff745cccf643029b7521f31d30f788fb3da32774e9f03996319aeac5f454811e65f7b443eca35144a40ba36a703c148000249000000001ca68d8edc08e2b92c4606daba54d80a640231a683a0437f7d20295776587bb4aef8f5d04039bcd99e86fe7d5bedbcb9210b6971025b08adc4f08607942faade3a8b4f63f5632fab2d04399b590d7cb86ecc7db3e458d3b65af93fbaebd600005b303245e7ee8cb67c4e6ad273cef035398a356071345f69f6958ec500a71bee229b0e21cf6cd2f2472470568115b5a2a4af93afe5ca042d3c08a395f2f77e44622bdcf692111822143c0bebf3dbe8c8f50ba9667b7aaf5061c2a14d376cb88012443a9ee2a60fb76afe85ac6c199682c59cd1b692f3a7a6e96afed48a146efeb51849761e3d2c9fdb1597bfa584c7075a5a61f9bc730059f9fad379439709b7a01c2be28a319fd6d5a477624cbc10953ea86cd21fdb26557a0a50aa450040790507d0695859bb6bc1cb575b8796d13765a8b47a7c25695be707154cf5e13decf6fbbc8d780e9e16ad825977dafd94d2963d3f0ad419403ef7e03aee74d80de153a53765f026299292c6eee613c35b76a061dee23a4de754ea839b34b640c39bea2a9b4c092e66308ed7e72978410b4485b19dcd0612e91fe6aa85d863b3a53cb82e304f4b57367f461628c6dde62a9c69e577577f7cc50bc2369e514f1fbbbe5c2a48079a3976e21a23b8ed264a4ee92e5245844c4aae7f89eb1a8c28407d46d61339b2b838b9a36ed9366f8f5161712a4723e588d480cab0d276f7b0f08775db438283c348cdeff5bfc7d30d248f9d1bdd8c1158b114b8614ec2a7401f7fc1f4cc6e0487780c2f8ff648d32f1cc732c3cb2f95150cdd1c10d6970bd4e65ec600aff608982fa8b6fa3fae7fd97f09b7525eb52cec747147a6629a2c0fcc3e2be0e239fb55c41828bfc08ecab2c42c399344172fb360baee797e24d762f013be513903acaf8a66a0b236a9527580bffa085fec65696cdaf0021c8766c71252b6ddc20683689d6a896ff843a3dc78aca3f6fce052b31a8d0ce692a592611bd5124367dd65ada18bb48727a6fb8e3b136597ce6b2595f3fd88d6381928b06bcfdb5c37dd25a96e7e2683f558b0e75ffd030bb97448ce12b54b6775b1bc01605d45483d3393e843b8717528ac40d96471e92cb3955d5c95c88016a5f5ab805fb2dc66696ac3a6679376825c820b545cd4e68f2092f68b39733d3f6114167467b0f2ca98a5dfa3ca66d30ffb2d865f8089b9b25aad0358f11a7fa0ae5ebff029350ef8b303b2d03ab0062f9905658a5369cf582ff36842d62f078b219c52d4ceb21407a6ad12013597ee04888ca8f612e22516978e1f375d726b14b067c57769e351703026afb6c8de01747b8ae65b6212b26cefae8655bd2fa4a342db325356bd5a6b210a31f4f056dee83cd21a47b83e2a492621c0dd526d60673d069888f826f52a1c0bec301ccbcf41143170a04dde7697adb6a898f3aaba4be1c9e41d236d57dbe92c16ee497eb925caa1c7c2a96a8c1790c9865b6e9749bc028c558cddd5d1861d891f88d995e003bdfd760ea8302e46e8f412f0e6d771abf7530414165346adbcaa0df6a9c4baeee81fe56e79178763000551b6fa6be0c8816a8ae02a66e7ddca487ccf0ad2313b3d24ae3992008e50f58d4b1604f785a8072c2c1eda22e3500e6d42d28ba453f9d38144d6ae2cdd9b1f824970ed966c3bf784dfff3a35c710edb4e1208fbc725298dda38962b51e863c3993439b21fde8904f554f955ea03b5fcf39b0d34ae4ceb195e62f369d018ead00d514ee9640981489b23c8bef71f8624ad7f5aa5e1d422e29c5065688e52b9956bb5f078ffa9af4312d91f37fe3eed1243590c1ad0c9a8e1760febbafbade1219adfabb162cbfe4086e8967d923bd7435aed9a1d9f1f5cf5aa6ac428df86d465fb1bc1c10ed8cc619d4340aafa92f720c033c69616ef9ab2250808ab68737895730dd033a15f1949c786ea9488635c6bcc3fc5a69cb3dd5be0b02f89b1cc859e43852998a78169e331c6232007f66ca62a6f5ebcbc8db7021ee337a727d0c3c99cea76bd021c24fb6db5b7bbdcf6e8d60b3e8881d5957ed21c8a8cfe6ff7578787fca4336a240c1c476db3658ab3e90bc4ced22c97988722bf8610c345208dfd9daaffc8af369de96aec53372506e1f868c5b449999491e9c3798cb781e2c3435da49a1ace62137dd324d12315b11b8823d52f15a292693475b2725c79bcf4dbef7901c509f3fe3834347af88258757cabada36f46c7db14ba90d188b353254260ccafc407324c77321a92e2e769beb186b41631f409542136241362e7be6868a8f448624b4884392f031e1b026dbb870630913e2fd5de845bfd49c832db0b2a0213f0c29283c8efa9836dc7e16793eec61e054fdd56c895ec7134bfb4d6f89e06c07174c2161cbbdb70aedb06dc7329e5b122db5666867e69a01297a67d618e778f0e8d29b12f51b41183c5b202373059020b3d630402f05e05d78d916a7fe0456fdbcef1ca355736cf57cb4ccf7e49a5394d64b664725ba0c463db7792dec3c561cd65a1d2697715813c4f3f43f3dc017a83750e6c0fad22ed14e4ce2ec157320bd64db0a119a20947a6114866d9b2c6eb92fd3d499d7f5f0eae762d7aa9fa7c71b745219524d3e54029405949a3f5c472034b3dc7c4338d97687b6ef5fe2a43e113669af458a64ab39dcafe870fc89280d213dbc39dcb6c58eeadd4a5359e72ce0f0371f330ed534c4701c995c2bcc139058b223fb2746e5333a5a8d5ff9df2e3940c271ab96c354bdb49e51a23d4c460e5e12f82d6fe61178e74a3339a081fea18b4e57a790845df773a82c9d8297fa5f01e48cd2e4419faf33dcd90e20670644145445753dcbd5fec27cc9f541cc8779b3a51e3767f2c41e5afcc51f60b4c35e93c142be0afeb57d7da24f2743479d2dc70863e42f43f513bd3ab6ea323f4676dcef2e07be12a8e7cdef647f8d6561357def58393be37bcdd69bf9b7f1afb5bd291cde340b62ba86445f1a769e0ab82dd61d4d2310ecc90702def1d0da0e8ca60c35f61a4f98aff5e59af72e9918bcc2635f4869c1c0901a91acc38b65874ed096f6eacb4181b421cda40cf1435be9324d2270d31cdd701e1b8d3bd3f0b83bf78ebb6b7c3c05bef719b18b660b86287654d2537f679485d98df4e3fa145ae0ee66c59235fd01fe01f57642d73e5fcac0a039c14edde373351030e64c9c2fca4c3fdb5f51652aee318390b996daa19f9b4eff19531426df8b1e57d00a3be5bbbf628cac239946e340426b9ce6acfb9e495ac13333c9a0fef7f430d08f289ea17ec5458d0547c062f42f28a27ee521159a8ccdc815d57e4e29c1e9619a3d43d720698e9a6d84fb91981617e15f1490aaf62ef8f76d2a4e446cf4dd581f8f935aab417582019644988818ebe9e2bb86932657e6aeaf797637c3d9d43ca8e1f392e8a1bc60bb9ea6c7752af60b03570a2dca2ed835ae4d86a11b56b10b83a16c3c97ae3b10370b0bfda10554d4b3409af8f76487fd0908c74a0fa715ac4d75bb18e2ae2ff9b2534e8b7fec189bc6d142bb678ff4c31e680d40fc5999116c034469cc901bb281a5127f478e33648554aa011944c4a4cbf7d92d58f264013c633717a4146a465f4b23a70532a63d24797c6ae4b1f08e9376cc013ef1ecd4945a2ee7ed30ece8f17b8eb22a3eb4e1e12362931f952335450b301031a433a2481208feb658f69a733b7e462faf4857dde8c8d39315e41ca75bfcec974061b4df72952318747c98b3aedda12b7639e5fc2e20bab2cd106029231f5b7063609aa67cd83726c686185e14127eb7411d95469ee14133661867001e0a2fc5f96b8c10105a1ef933997bd3f992995d4d443f55f9fce7c854a382e4bd0320163a227be43aaa1a5f3d7410f3bfaaf5057794049fba733362a57a511fcf03b307cc36716c7c75e054d040b2908f7d9d08465b1123a812ea2852f9f59dc7f874ead0e3a77aa38881b2ef6842a03cd23df62139d656481370e1964b2190da992ae4d920f0b37271bcce6cc0c34b61b5b191839dc16f846e2b7b475644721c6951f84f28fd89dd46b2078ab7da135dc97a6396e629ec73a4403b0d8c74b4b8d05e33bb2eff407cfd16a3a5feae32cd114e00aad29bf2586e74372f8dd4a8139de84103940bc50858ca3588931063310fc5ff34942c837aa668014811a50eb56426c0c5b701079957585d07d86ad572e6ac394107c3839c30b72c93859b045c0729565e942efce71e0db5ee88b2167f8624261a7a733b4c6ff4375e7ba2e69df1fd1e367b765970135d18a42718189f9a22dda4a64207512555acab493720d60d8aeac9bb01843d6f485e9ac685300bd234275b09c633240cfb749a6554bf544dc3f032be83128576ee3c1993327ebe55e68687795e5d9260b1534e72ab6597fc07c1fb8c12300bf0774f4bd311e01140faab70ab1eb995fa3c21d4138d881252664809499973eb3628f7f06df66b9890241352c1cd14c117511b17f5fdcf21b94d7c5adfaae91fef1261a85fa5b34b5a6502d91333cf39916ee93253e7ee208b81edad3f3221b653194b4979a9db038a27d05b4b62bceec782106dbcc7d8defc9de231a40892fe6b1139acc74f7c8914f09c0ae0061285ae049e4c1243a81a04af68534043dfe0d15b339e665a58e39a43f67b294032abba222b2496b979343a5895693ed47d00244bb67e950296e8e8a1490f0d19cb63ac635036e0f25c1386f0f6854cc03db7f7e41bc09bbaa395c21ea10c93fd1a5d45635f4c73e30a90375201f8fd08828ddb19bffff0837c59883603bf7581f2c2b630293d37025601da9d298782d5bbeb3805829d5f385c00098fe49de0981c4fccf41d2288604a01bbb56c4c28cc7500752f8077c70594171c841cd67e756b098f12f1ed399db8aebc4101006e8677d732fd31315a78854ab9528206f218d7fecab03896a74c245b876409333c77ad36363d6d4cb94262b0912ae16157070d8582b0a6bbfadf080483a09bcf2ab708596908302376ab724ae58ecc6d1e9b3bb0a6f374b483fcc231b45dab156d555506a248387e1462cf9dd0155538fa0f90cea30dc72dfe634c048bdbd902b98a7ee4799fa8cece7dc5e6c0e6438abe82f3ba2c3a44c474d359f2c53360e93da8a2fd1461af429e78c896223b5c07e746a598cc5cc83914665c4040144913f910a23f607f0e1a34dbe1c23da97a08afeda29be043321ed7145393ba3c96f91dbdba1fa795d5dd04edbffaca79604be43a98db4815a17e74f6e7f96825bd7700f1d64f65c512f6bfcbb7be34427c3fcd85e264fdec8853b25b60176ce8c615d68010e126f40633b046cbbf5e93c052f95e284255c6b5f79c156d55fc9f535fcff11f9ae2f19ce5cac46d2442981ac1d5124f3552edbec2396add22111c16227a2c1bbd7fa8a922969123af28008b12afec3d09d902fc6924d05ee4f1ef6ff53e3a208235085af24d1bbe66f99b71b773a5020293a9fde04a1814885da212a017f79b8089168aaed04d6d5cb4fddab07e3124a20899c8d40f4fa1d71ace9485dd3bbfc1da0d53b7406157756b516e9a20422360ea5dbfbbd6b620321256d0ce2e6e321b74590b6487636186e2d1088bdeab701d8cb315089b44b0e23fe7d61ac78d9a7235eedc9659d09e56430fe6ec0bdb4a998a80579fb941b019c353785017218fc38f0a208d62c37f6e0e51b8fec6b2b63ba5c6c9433b5f061f9447f78fa8881ffd29b648935def4ecbe6a608056452f683535b1e6076f978a1acc86528d7f4cce3bc811b487a83876ba16e843cdddbe529765d7b714416ca996f6220f7167f26d67c4b4b397f6bc8aba96e0717f09bc6b4d7169f53180d57f5ef458b813306d8b5d3fb4b5d9533d34a19b41075d0f5455cb0040bf0876b649bdc01516c83ece17112e0d8a8244ed25a00121e34019b6e2f926b80b42116301d383ed3d3a584b83d5d231f826d8384a00993908bed07c50eb52ef04dba060462b614f09900f89f7bf48bbfa79d669635d42ee0193fec688e666c8b988d6db2023ae94d44d5d550982c0b03a28e0e9c06ac17d5fca5af85b45b444e0e201224367429013e82ae53c38d16821ace36227abae3a427a527f524b4a5de9624bd63f56c9ac69239cf48991ff14b7f2554e3e10d468d7a28c373852c6b339c4f8bb450b6e627e6157ab809d99cb57bd19b018d3a2e5810c197fb4b74f678ace08294407021055d2b681dfc3deba0cd51bcd643adaaff5154bcaa50e80b462d42a902ba4f8a9228a96ea33498fb35afa683e30bbccdb7f5f6b8b24c28c2f165eddeeff5bf16b1c190cb444541693b918f460610792947a1003a02da0a0523930d4fef3e4c9311130ee2432ff31cf82d64af009313bf20c73a3001b6a58b1bdbf3cc593a7e2160f48c5cb38e457593058033796019047791ce8c84055b6701d0413bafa185b5699b642cd2aa2a9d6af5cb870ff24f32ede144ecc29579d014cf4526326d581887f2bd09e3211520b8e4d8403b943b9821ea594161cc18cfe557e0086d4ef9ad2a0c271fe0267fd55d46b7e81900c6e3d8e2f56a0beaa542d177c08c2b6bf12051b6b183d6deca62530d31b3d2c8f4352bc8aa654299c076b87f2bedb5ed4f9353195ce009371025709085c787eb5d563d0cb797787932e86a86d9ee3aeacd3a6c4cc27955b7d0fcd4280352ecaede8658282e8e7e093d0060259ee73294931a5d58a0a94a952edf166ba5bdf91fb9183369be8e8a1c67269b26181cc7f6bd23a9da5c85fd0532c950edd67e76c01f3cfa11e35eb705dd65b57a0e2c9c218bcf64eff7d8d699ce65b57363c5c9eb1d0c2781491fbba150e388b9dacdeafbe73e25ae5d42ad6305c921811905e629bea3c4450d88a7376365a69700e80fd4345f520333775bf3851b6e0b8b5aaa6200529d2b23b6e8754b69a2774b32657f0126cecf93004344c0a0e07ce2313c5b0eff49d793c3e9721747f3315aafa3c5b777332dd6e884262d93f385e63e057e8adb6a4d47073a807e113266866688355b79d17ae19f4708c2b338655233edf3cf6f6fd24ef95695c72b1346c52699d7234139fd1ece6d3df07a72e3aa4c793001cf15a5d1dbe7d95bde9ca7e5cb8d7d70fb249784277247999c0585946d7b702791debc66d7db9ffddd41274cef141e2d87e628c927db3c0bb261776bb253f488afa23e403c5a141f245b5739e6b957722be3fabf5a916960abcc4476f597f75fc6ea90f6ef870c94960fefee4c62cc623f5de7270a6b8bfeb2699bf73ac5ed83ff51f4a7f5a293b44e7f130125cc07ac1d65a8f086b6cddbe37ed55eb007f66e53a4db1316afc12f6e2fbaa38ce366fa1eb052c8c1769b938d222b6eb1610b0f38093150e346076dde61a5d6dc2841631231f1e98d052d5243477a13bd819bc16c4ffea7330e1a761444fb692f60eafad4ea3cbd0899727db2855f188dfba2f078e02b7669f5ce2717c718d22b64e85d9ffc1d467e48c0ceae770ac807cb63cef217a192f36c74378ca638f198425e0b9a97ed20b87b4d013203cc3f629602ce46b7ea2b1e049920c055b9a91a9f1bbf7c1c4b1c1dbcb6d438a4cb9b0d1c34180180542c25c2615b4daeba8d9804f34e3592624e22494000000000000000079a80a01012e6cefadf5212b8f6c3a317a73274353a9061d55d732a14048469b3d062eca79233ba7d9a0781b823b652497636a8e7286e60b99b961a928df80e6d10a1c50d9812dc6b0254dc20a60ad9c594c9eb9268243cc3c5906cfa1aa7deb75fcc0dcc3c08a1847e03fc55e997ffe8057e04b520934b9b2871ff965d11b3c4ec16cd1452207d43f202853a6f38e09b5d9f1cc3a8be3c45f071a492498de0041432a4e7624be4a1265042c42191f805452a19e57e57ca2d175df573972299aece394df4b196f7598675d5c8ee4aa1b164ba78a78cf7efee5dd8c57bfa1c79ffd8fecdf6fa94f8d5b62b1c10d628f68bfda79fbf3d93ebc0f6257bb874c63a82ae6c3c7a38c34dfef87044dc6f1f5c3b7d3bf42e7472ec76a6517b6746555c6328166fef8ebfca77a38cb86b9ae34fe54ddb44fb23c82ec190902627c838b619b6a1c88a0cfdaae046877bc55e406296b90b4b7514b56df9e03a5b8b120bf312a7dc37d0a4fac3a6b92f6ff2559648ab5c103f075d7ae25b41d57877029017c6496ad6f64084784abf0f87231f4af20e903f8b4c34001b1d045032a2216af2e18974dfec283204d38463220ca0402d99f7f9438eaefb3f2c596c40c309bfe00899473836911ad1492dc0e9a38cd5c798562190048bf4319c307125184943af90ea6b367cbb1f6dbdcb5b77e1f926f4c242bb3ae5804d5ede45e8b402f4fe4fb9ad0c79ffcbb95d5809ae32d91f1adeaf82487e2a4f27764bc45dc8dcae8f96eade5be4d75aee092a7a55ce70242cd8e16e78bac1d8eee88dc882af9abbdcaa5cbc3c4a686e76f60d58fb7000dc2858e0a1d78fa85b64036682c90f854acb7656880082283d277ca05bd3edb95a5589b9bf52ebccc70ba75413235a1f027c7652f2466149c8aa4bdcb7d806e1aa42a3ae673e4d9d22c4c2a127edd455c015855e4adc8fb77736765f4e7f4fed6fe091b925869080c1d3f1853542ac3aea59ae0ee052743feea563680b682630c6f59480590d96ec9fa5b24d17b260cc7dada8253f2f9f7ba613d9ea7224e1796dd6d54aca881856059f2ae315e5419d4a982b0966bfbd9f3881289d2df852993c783b5f16b67aec0f69fcad1329d2b7144678e26af4544fa1dd58a6dd31cb316a965573b4d59a0e2c00d61dd2d1fd18aa75cd26c56fa33b37f12248f76e9f8da508f0ac74bdd2e32861df31c13b8d6d50172a4b3e15e8e9f08bf008afb4474e379804e51ea159963db2b51715a0194fb5c8fd82f08ebb2bcb2046c7443865d42cfb03fefd7f13a8fb6ef4edba54b99da3f12e443c9330541fcfb88122a4bbc127d7f3460acc39c3bfa3e60385f8b5588e59e42df257a2eecc91dffc1f4d0ca836d3fa9b434153ef1da6e0cf090360653436eeb3f231e258448eb22ff966ebeaa4c539cf5d5180df65a23c0eecb79ecaf896fc20345d8e931565af429c00cd0731252533afd7a3f2a98becb4e8585d129f9f264bf7dd40457951422b4caada42f437a59449bd4128d0a57f871061b9a68b261afb50d5310cd1224f24ff20b886375edb6077897e98d7e27236e1ea1f3249aad23953d52ea08a554b2163e7fa1977720109c37f644dee5e7b31971feee588000000000000001e81a04b235d685b9a60f2d43e571ecff614d9744b156b5acb9a6a61dd56f55d3e8ea071ea56d622bb2c2eea975071d90ab1d12868605d25fa548df0888d46982adf3beb6e5357cfbdfbf4cb6f80002a156f72ccb5720ad97a8eb614642597f40a87ab08a6d7157bcf2ac1a4bd551f5bc9d126d898924a6ca0aedf60aa3468d0ea2e35c3e04bdcf311a0a770432780a05092b8ca7d203d374e5ba0c48de92a337992ad7a9b0ffb761aaf7e08712f0ba1168f6453a08ee4dd431fd2bca45ebaede5f5f5565f64a0c8619d55161f8cf53c72df1fa18dbffacd0c6c17a1ba78e912fc73e58329a2034bbce93110227653aad8223d64a5ccd27fe25b2f05721d58ac03fe32e9a9ca519e176b8044d23e7aae63ee951095c50f01831299d7487fb8f5fa7469c178bca8a665b0fba176824f1625d539a47e75398f3070619b14a7d75fe80fec9844c068c8b206d0f263ce4a0898380c72b3b5a757b7115ce9dec34d88743408b46438796a3ec111073137cc399349b6105c411eec8a5455d4fc7d6bfeefa44f2b9a19a1e957f942b582db83a96bfa4751b13356a275029384c8bdf03fd7cd16614337a5bc86a1fc4980c38d2ed96706cbe39cb0b75c8e19b2bc44b028f21df043e2f493384daa8f5e5d1bd45aea0e6cf4255498bff7f14573d659a0b48b00b3d40cb699251d472b3d4971b6ce7517545b7b95e3a2a7f2fddd2c9ec645f3ad78c440c0392419bc0bc9298f153315dc7ad06ce6bd973ac47992d0d03341ec2f114c9860f4c93eaffe0352f2ab06470a5bc4f8ae248e888df33836d44af76f6e3a42c263ea92bb153e237028159ef51fe5aca9ef83e8422f32724092701896738000291ffb0fec209f84100592bae9cd29af5b83443c066eedbd97aa2ebfd35c4e4e3696a23de77fd708277e1f7cf5af5b9eaecd7968fba45287ffcccfcfb8e3697a18b1938d53e1715c0671c87adbf0b2bbe5c0690e86f8f6a21803e57c9dc47cfab1570e69b28b21277b5be58c581d00225c90287780468b3281cff07aa7b5c651bdcfde3a5a2f2ab3c2e51e06f921e6770175f0fe71938f0bd2feb3d607e90f614ca8beea07d4676f38c89909fabfdfa43c7ffeffebea0dfeb881da3fb7312026c2bf1d35274076bfa9f079ca2dbba9bdd91db37615a0154a17f6b5216f7fd6eda6e959c9db5752ad2f8ebbf7b79f0633a83d26d2054852a274ca8f00c080e4e9cf055cae2c75c3003ace5ad3f9614d3d204c204a9ff701cc5fc347727cba307828b0647c38466497f3b896b91993593db77d8f79fb3f57fa0fa16a0af48b96fbcf8b26e01c3afd5e7162c19d9da4b1058d6b6c077a52359447577c58bd04f072ed81b44d85157774f102e95b1d05167ecd4fd23cd74e57fbec09757f9de41ae466055e278d226303f7808f7b12b98758fa6af9a796d2c2b31029b65d7a52b7979fac129e35c189f78ee1ec2eb2ff998b6d7b7ec8af25c31b7462f7aa3a01e80b747ffa921be9e613d16da3d6beb740aef7597593648dc24f70d98c637b0bcadae04869b2433ba94646c2eeb5c670b7e343939557656d6a0324a434e6e9d9e9a2bd60a565788ca2c8ca5e6769948a16282b3da0098df14cc8b7721665f1f3679c1c0d9aeb3e4e03bc2dd0a0f9608c0f35d65564ac7b60c8ab207488221dbc52c0473852befc925bcc2752ab4b4e1198d85955e5e8b65d72e3237d95edd7de300528669964083d72826f00b9dc7bf36967226cbbe107b418d9049657bd96e0215d69381823958485ffd38baeb8d297a894cc860da4587264b9cc29f175149c248077d83fc3c893f8866c2e98a9ec3a30ceb960baaf99e8150d706c6fcf75b9b0256c4a4924f93443e300ecda2e93272734d2bb290fdb298832dce1afd2d032bd104a6ea2f39a1786e13c200e3dee3d78b26fa54a260ab91f41935976734d94754d51c7e59611f4cc1b90db33aa43903dc4ac3a7ecb470bc7fccd20a66721327848a60018367624915d471e2c4edf746756d3b81995c0fab74294e094914e866d61abaf95c993ea7e75247c3ebd0ddd2f92f8723587d86ce9a5a1917520f1f702ebcba6394e384fa255601bc8192d8baf42bf464b9a1157d25bf9310ca6e2893d4652d9d9d20932ab717c0aed40dd65899456be44bdc90de9b864e1990a894c39d965fce89ebf889f004df0c6ed5977d07081e127cf7585f424b84c9c7f9f0a16efce6fbab86f4674a0ab778dc953722e241a7bfffc8a40fd95a5e487cbc908523f3a0f46d333f73a88bd57999535b3330187346979963fc495ee0a085c64bf1bf7af959463d7f9fdd871b934dc043c8c4f4d187cdf419485ddcb5c7146e80abf5d0fc891023ee361303d893d6ab09e1147c18de93e5ace4377166251f8da17dc48eb4deb4fdeae80656f7c549d19d3468de4a38530bf5d5977a3d53c8b52ebe9653e0bd57226116e404912e21997710eae45ba53379d33805457e63f323be69aa9d0528ea606c7158ed5c2c13d6d80fd317912444f759e01a18b453571c1938544d440e3f4c6c978edff0d9bb0ded4bce5a7ccb3cead55d5b18584fbef1e58a17f3f1d139200bb575a52f5150f050e6be171419f9324c3ddfc7fec2f59ba5360e96de2a18c4a90e47215586bb301b134483f4cac4d5336289dc19249096c2e6d9c666a8cf0d8ceab06a5a91a259bd9ae53ef57cdee0c8aab14adecf17989d04df767bd24b0dc3f74386c2c5703608ca35b1eda9ff0de263218a669b20da258cf0884373c3a04eb7decb7ba1883e1c5397a9a91463a986fb05903d42d4b67ee819879eb3de77cc49bda6439fe7731fcc6e7f80063298850421e5fa4e4c2324ab51148cbe848c0e65ff6c5d7b197221bfe5f7fc3484c55b9ad87394eb264303814d27b466a5b2c19c3603918ae9ee4d58f40ecede327b25cff8aa1746156bf2352f563e0b4078001aec9c7885850616cfce4c2943cf94f10df192b09a71af3ac63954a6a1e20dc5053339b25232e528744be9be3f6b42ca71cdeaa8fc2f5d8fe8178a975d54dede808cdbeb6d84e4a2f5949af533bfec7c4801ecf748820b68a5f180a7b499892297a759890fd1aa138f65b33aa1427e89d93b7630a115dc75a00a84646392437418ff80ccf26489bf46b266811b9f8629e8840c88462add4d0cfc9b1ecd79c532f2a10331036996a51f8802019ee0515a32974ca6849eb96a552a62cf6366a4dea486104f68799254d023d0f705d9f20f1a93c4ef5aef4cb2620bca14982ab78b003001684a710757546cd43f772ea4fa4517bccb224d28826a6ff5d8ce132965fb179298fad31e88af5a1812fc31e39218322ba52a5ad01c33e60b71dd388ac143780a48804a185144bf8c3e91024c1b79030949296a8448ffb306210dc9209bdff001ba8f13f4cb8bff96f5f4fcde2ff883181c4cf4d16779a617f9c49fee2e8bc75abe78519389283b89185667200e25d7ce746566e9fec7c6c11c1165720657e9f22ea5b68b29b0028c8222f7443b26de3ebb3445fa61454fce55f6faa68d10a0175be6751ba9605b901f55b63dc1bf53c18186ddaec56665f5cb85d3b98d13dbf860b667bc1892f90ff2c1b4bceb1c2fe86b6d85a0266aaa061d33a7fbc439a2b7485e3067f5298754768180b5cf60c764780cf3d04cd9649100300913457bd690f4fcc3b775f0029fc83c7170d01c00000	admin	2026-10-07 03:19:18.345524	\N	\N	f	\N
3	image/png	279	\\x89504e470d0a1a0a0000000d4948445200000040000000400806000000aa6971de000000cc494441547801ec95b10d84401003112dd1354d42eec4b288b0e7a50f4eb7011ecfc2795fd7b3fc3f8ff11f00c60538300003c609b002e302f012640558817102acc0b8007b5f012d9c1550226b670c586b5cf3628012593b63c05ae39a170394c8da1903d61ad7bc18a044d6ce18d0deb8cb87018e50fb3d06b437ecf2618023d47e8f01ed0dbb7c18e008b5df63407bc32e1f063842edf718d0d6709a070352626df318d0d6689a070352626df318d0d6689a070352626df318d0d6689a070352626df318f0f746bf3eff0b0000ffff4854379a000000064944415403002bef84413ddbf8850000000049454e44ae426082	admin	2026-10-07 03:33:05.828305	\N	\N	t	admin
4	image/png	279	\\x89504e470d0a1a0a0000000d4948445200000040000000400806000000aa6971de000000cc494441547801ec95b10d84401003112dd1354d42eec4b288b0e7a50f4eb7011ecfc279ddd7b3fc3f8ff11f00c60538300003c609b002e302f012640558817102acc0b8007b5f012d9c1550226b670c586b5cf3628012593b63c05ae39a170394c8da1903d61ad7bc18a044d6ce18d0deb8cb87018e50fb3d06b437ecf2618023d47e8f01ed0dbb7c18e008b5df63407bc32e1f063842edf718d0d6709a070352626df318d0d6689a070352626df318d0d6689a070352626df318d0d6689a070352626df318f0f746bf3eff0b0000ffff5448f0a3000000064944415403000e2f84413b9831c90000000049454e44ae426082	admin	2026-10-07 03:33:06.122341	\N	\N	t	admin
\.


--
-- Data for Name: incidents; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.incidents (id, order_id, handled_by, type, severity, reason, description, resolution, status, reported_at, resolved_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: interactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.interactions (id, customer_id, channel, content, note, status, remind_at, closed_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: loyalty_point_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.loyalty_point_history (id, customer_id, order_id, points_change, reason, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: material_batches; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.material_batches (id, material_id, import_quantity, remaining_quantity, unit_cost, expiry_date, status, created_by, created_at, updated_by, updated_at, del_flag, del_user, ready_at, expires_at) FROM stdin;
3	3	6000.00	6000.00	0.06	2027-02-03	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	\N
4	4	8000.00	8000.00	0.04	2026-10-11	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	\N
7	7	60.00	60.00	3500.00	2026-10-16	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	\N
8	8	800.00	800.00	0.09	2026-11-05	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	\N
9	9	30.00	30.00	9000.00	2026-10-13	AVAILABLE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N	\N
2	2	5000.00	4500.00	0.45	2027-01-04	AVAILABLE	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:19:08.815974	f	\N	\N	\N
11	11	1950.00	1950.00	0.12	2026-10-14	AVAILABLE	admin	2026-10-07 07:19:08.617035	admin	2026-10-07 07:22:45.246371	f	\N	2026-10-07 07:22:45.064931	2026-10-14 07:22:45.064931
1	1	8000.00	7500.00	0.25	2026-12-05	AVAILABLE	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:58.884596	f	\N	\N	\N
5	5	5000.00	4000.00	0.03	2027-10-06	AVAILABLE	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:49:00.216669	f	\N	\N	\N
13	12	1300.00	1300.00	0.02	2026-10-21	AVAILABLE	admin	2026-10-07 07:48:59.987861	admin	2026-10-07 07:49:00.926195	f	\N	2026-10-07 07:49:00.784137	2026-10-21 07:49:00.784137
12	16	980.00	0.00	0.13	2026-10-08	EXPIRED	admin	2026-10-07 07:48:58.53492	system	2026-10-08 07:50:31.045195	f	\N	2026-10-07 07:48:59.440582	2026-10-08 07:48:59.440582
10	10	30000.00	0.00	0.00	2026-10-08	EXPIRED	system	2026-10-06 06:37:15.880553	system	2026-10-08 17:00:30.964269	f	\N	\N	\N
6	6	1500.00	0.00	0.12	2026-10-10	EXPIRED	system	2026-10-06 06:37:15.880553	system	2026-10-10 17:00:33.172836	f	\N	\N	\N
\.


--
-- Data for Name: material_components; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.material_components (id, material_id, component_id, quantity, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	11	2	250.00	admin	2026-10-07 07:18:41.248415	\N	\N	f	\N
2	12	5	1000.00	admin	2026-10-07 07:18:41.555102	\N	\N	f	\N
3	16	1	250.00	admin	2026-10-07 07:36:28.9776	\N	\N	f	\N
4	17	13	200.00	admin	2026-10-07 07:36:29.339423	\N	\N	f	\N
5	17	5	500.00	admin	2026-10-07 07:36:29.377113	\N	\N	f	\N
6	6	14	500.00	admin	2026-10-07 07:36:29.846411	\N	\N	f	\N
7	6	3	120.00	admin	2026-10-07 07:36:29.885293	\N	\N	f	\N
8	6	4	60.00	admin	2026-10-07 07:36:29.923731	\N	\N	f	\N
9	6	15	5.00	admin	2026-10-07 07:36:29.96213	\N	\N	f	\N
\.


--
-- Data for Name: material_transactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.material_transactions (id, batch_id, staff_id, order_item_id, type, quantity, note, created_by, created_at, updated_by, updated_at, del_flag, del_user, produced_batch_id) FROM stdin;
1	1	1	\N	IMPORT	8000.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
2	2	1	\N	IMPORT	5000.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
3	3	1	\N	IMPORT	6000.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
4	4	1	\N	IMPORT	8000.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
5	5	1	\N	IMPORT	5000.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
6	6	1	\N	IMPORT	1500.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
7	7	1	\N	IMPORT	60.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
8	8	1	\N	IMPORT	800.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
9	9	1	\N	IMPORT	30.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
10	10	1	\N	IMPORT	30000.00	Nhập kho ban đầu	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	\N
11	2	1	\N	PRODUCTION_USE	-500.00	Mẻ Cốt cold brew #11 · Mẻ đầu tiên	admin	2026-10-07 07:19:08.764386	\N	\N	f	\N	11
12	11	1	\N	PRODUCE	1950.00	Hoàn tất mẻ	admin	2026-10-07 07:22:45.162781	\N	\N	f	\N	\N
13	1	1	\N	PRODUCTION_USE	-500.00	Chế biến Cốt cà phê phin lô #12 · Lô cốt phin đầu ca	admin	2026-10-07 07:48:58.826522	\N	\N	f	\N	12
14	12	1	\N	PRODUCE	980.00	Hoàn tất lô chế biến	admin	2026-10-07 07:48:59.533451	\N	\N	f	\N	\N
15	5	1	\N	PRODUCTION_USE	-1000.00	Chế biến Nước đường lô #13 · Lô nước đường đầu tiên	admin	2026-10-07 07:49:00.154624	\N	\N	f	\N	13
16	13	1	\N	PRODUCE	1300.00	Hoàn tất lô chế biến	admin	2026-10-07 07:49:00.879111	\N	\N	f	\N	\N
17	12	\N	\N	ADJUSTMENT	-980.00	Hủy lô hết hạn	system	2026-10-08 07:50:30.832976	\N	\N	f	\N	\N
18	10	\N	\N	ADJUSTMENT	-30000.00	Hủy lô hết hạn	system	2026-10-08 17:00:30.823211	\N	\N	f	\N	\N
19	6	\N	\N	ADJUSTMENT	-1500.00	Hủy lô hết hạn	system	2026-10-10 17:00:33.013676	\N	\N	f	\N	\N
\.


--
-- Data for Name: materials; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.materials (id, name, unit, stock_quantity, min_stock, status, created_by, created_at, updated_by, updated_at, del_flag, del_user, kind, yield_quantity, prep_minutes, shelf_life_minutes, instructions) FROM stdin;
9	Cam vàng	quả	30.00	10.00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	RAW	\N	\N	\N	\N
3	Sữa đặc	ml	6000.00	2000.00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	RAW	\N	\N	\N	\N
10	Đá viên	g	0.00	5000.00	ACTIVE	system	2026-10-06 06:37:15.880553	system	2026-10-08 17:00:30.964269	f	\N	RAW	\N	\N	\N	\N
4	Sữa tươi	ml	8000.00	3000.00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	RAW	\N	\N	\N	\N
6	Kem muối	ml	0.00	500.00	ACTIVE	system	2026-10-06 06:37:15.880553	system	2026-10-10 17:00:33.170837	f	\N	PREPARED	900.00	15	2880	1. Để whipping cream, âu và cây đánh trứng trong ngăn mát ít nhất 15 phút (càng lạnh càng dễ bông).\n2. Cho 500 ml whipping cream, 120 ml sữa đặc, 60 ml sữa tươi và 5 g muối vào âu.\n3. Đánh tốc độ vừa 3–5 phút đến khi kem sánh, chảy thành dải mềm (bông mềm) — không đánh cứng, sẽ không nổi đều trên ly.\n4. Nếm thử: vị béo, mặn nhẹ cuối lưỡi; thiếu mặn thêm 1 nhúm muối.\n5. Cho vào hộp kín, dán nhãn lô + hạn dùng, bảo quản ngăn mát 2–4 °C, dùng trong 2 ngày.\n6. Trước khi dùng khuấy nhẹ lại vài vòng nếu kem bị tách.
13	Sả cây	g	0.00	500.00	ACTIVE	admin	2026-10-07 07:36:27.289855	\N	\N	f	\N	RAW	\N	\N	\N	\N
14	Whipping cream	ml	0.00	1000.00	ACTIVE	admin	2026-10-07 07:36:27.59897	\N	\N	f	\N	RAW	\N	\N	\N	\N
8	Cốt dừa	ml	800.00	1000.00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	RAW	\N	\N	\N	\N
15	Muối	g	0.00	200.00	ACTIVE	admin	2026-10-07 07:36:27.820766	\N	\N	f	\N	RAW	\N	\N	\N	\N
7	Trứng gà	quả	60.00	20.00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N	RAW	\N	\N	\N	\N
2	Hạt Arabica	g	4500.00	1500.00	ACTIVE	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:19:08.815974	f	\N	RAW	\N	\N	\N	\N
1	Cà phê Robusta rang xay	g	7500.00	2000.00	ACTIVE	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:58.884596	f	\N	RAW	\N	\N	\N	\N
11	Cốt cold brew	ml	1950.00	1000.00	ACTIVE	admin	2026-10-07 07:18:41.204322	admin	2026-10-07 07:22:45.255809	f	\N	PREPARED	1000.00	1080	10080	1. Cân 250 g hạt Arabica, xay cỡ thô (hạt như muối hột) ngay trước khi ủ.\n2. Cho bột vào túi lọc vải, đặt trong bình ủ sạch có nắp.\n3. Rót 1.500 ml nước lọc ở nhiệt độ phòng, khuấy nhẹ 10–15 giây để bột ngấm đều, không khuấy mạnh.\n4. Đậy kín, ủ ngăn mát 2–4 °C trong 16–18 giờ.\n5. Nhấc túi lọc ra, để ráo tự nhiên 5 phút, không vắt (vắt sẽ làm cốt đắng và đục).\n6. Lọc lại qua giấy lọc, đóng chai sạch.\n7. Dán nhãn số lô + hạn dùng, bảo quản ngăn mát 2–4 °C, dùng trong 7 ngày.\nTỉ lệ cốt đậm 1:6 — bã giữ lại khoảng 2 ml nước/g nên thu khoảng 1.000 ml.
17	Siro sả	ml	0.00	300.00	ACTIVE	admin	2026-10-07 07:36:29.301537	\N	\N	f	\N	PREPARED	800.00	60	10080	1. Rửa 200 g sả (khoảng 6–8 cây), bỏ lá già ngoài, cắt khúc 3 cm, đập dập.\n2. Cho sả, 500 g đường và 500 ml nước vào nồi, đun sôi.\n3. Hạ lửa nhỏ, đun liu riu 10 phút, khuấy thỉnh thoảng.\n4. Tắt bếp, ngâm sả trong siro đến khi nguội hẳn (khoảng 40 phút) để ra mùi.\n5. Lọc bỏ bã, rót vào chai sạch có nắp, dán nhãn lô + hạn dùng.\n6. Bảo quản ngăn mát, dùng trong 7 ngày; bỏ đi nếu siro đục.
5	Đường	g	4000.00	1000.00	ACTIVE	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:49:00.216669	f	\N	RAW	\N	\N	\N	\N
12	Nước đường	ml	1300.00	500.00	ACTIVE	admin	2026-10-07 07:18:41.516683	admin	2026-10-07 07:49:00.926195	f	\N	PREPARED	1300.00	60	20160	1. Cho 1.000 g đường cát trắng và 700 ml nước lọc vào nồi inox.\n2. Đun lửa vừa, khuấy đều đến khi đường tan hết và nước trong (khoảng 5–7 phút), không để sôi lâu kẻo bị keo.\n3. Tắt bếp, để nguội hoàn toàn ở nhiệt độ phòng (khoảng 1 giờ).\n4. Rót vào chai sạch có nắp, dán nhãn số lô + hạn dùng.\n5. Bảo quản ngăn mát, dùng trong 14 ngày; bỏ đi nếu đục hoặc có mùi chua.\nTỉ lệ 1 đường : 0,7 nước thu khoảng 1.300 ml.
16	Cốt cà phê phin	ml	0.00	500.00	ACTIVE	admin	2026-10-07 07:36:28.936776	system	2026-10-08 07:50:31.040189	f	\N	PREPARED	500.00	30	1440	1. Tráng phin lớn / bình pha bằng nước sôi cho nóng đều.\n2. Cho 250 g bột Robusta, lắc phẳng mặt, nén nhẹ.\n3. Rót khoảng 100 ml nước 92–96 °C, ủ 30–45 giây cho bột nở.\n4. Rót tiếp 700 ml nước, đậy nắp, để nhỏ giọt hết (khoảng 20–25 phút).\n5. Để nguội bớt, rót vào bình thủy tinh sạch có nắp, dán nhãn lô + giờ pha.\n6. Bảo quản ngăn mát, dùng trong 24 giờ; lắc nhẹ trước khi rót.\nTỉ lệ khoảng 1:3,2 — bã giữ khoảng 1,2 ml nước/g nên thu khoảng 500 ml cốt.
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.order_items (id, order_id, coffee_id, quantity, unit_price, line_total, note, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.orders (id, order_code, customer_id, staff_id, table_id, cart_id, promotion_id, order_type, channel, status, subtotal, discount_amount, total_amount, note, cancel_reason, pickup_time, ordered_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payments (id, order_id, method, amount, status, transaction_code, paid_at, refund_amount, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.permissions (id, code, name, path, module, description, sort_order, active, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	dashboard	Tổng quan	/admin	core	\N	10	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	orders	Điều phối đơn hàng	/admin/orders	core	Xác nhận, từ chối, hủy, pha chế, hoàn tất, thu tiền	20	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	orders.refund	Hoàn tiền	\N	core	Hoàn tiền khoản đã thanh toán	21	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	pos	Order tại quầy	/admin/pos	core	\N	30	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
5	shipments	Giao hàng	/admin/shipments	core	Đặt xe, cập nhật trạng thái giao	40	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
6	incidents	Sự cố đơn hàng	/admin/incidents	core	Xem và ghi nhận sự cố	50	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
7	incidents.resolve	Xử lý sự cố & đền bù voucher	\N	core	\N	51	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
8	menu	Cà phê & công thức	/admin/coffees	core	Xem thực đơn, công thức; bật/tắt bán, còn/hết món	60	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
9	menu.edit	Sửa thực đơn & công thức	\N	core	Tạo/sửa cà phê, giá, danh mục, công thức	61	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
10	categories	Danh mục	/admin/categories	core	\N	62	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
11	materials	Nguyên liệu & lô	/admin/materials	core	\N	70	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
12	materials.edit	Sửa nguyên liệu & kiểm kê	\N	core	Tạo/sửa nguyên liệu, mức tồn tối thiểu, kiểm kê	71	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
13	stock	Nhập / xuất kho	/admin/stock	core	\N	72	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
14	stock-history	Lịch sử kho	/admin/stock-history	core	\N	73	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
15	tables	Bàn & mã QR	/admin/tables	core	\N	80	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
16	tables.edit	Sửa bàn & tạo lại QR	\N	core	\N	81	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
17	crm.customers	Hồ sơ khách hàng	/admin/crm/customers	crm	\N	100	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
18	crm.interactions	Tương tác & chăm sóc	/admin/crm/interactions	crm	\N	101	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
19	crm.feedbacks	Phản hồi & khiếu nại	/admin/crm/feedbacks	crm	\N	102	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
20	crm.groups	Nhóm khách hàng	/admin/crm/groups	crm	\N	103	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
21	crm.insights	Phân tích khách hàng	/admin/crm/insights	crm	\N	104	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
22	promotions	Đợt giảm giá	/admin/promotions	promotions	\N	200	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
23	vouchers	Voucher	/admin/vouchers	promotions	\N	201	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
24	promotions.analytics	Hiệu quả khuyến mãi	/admin/promotions/analytics	promotions	\N	202	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
25	stats	Thống kê doanh thu	/admin/stats	stats	\N	300	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
26	staff	Nhân viên	/admin/staff	system	\N	400	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
27	users	Tài khoản	/admin/users	system	\N	401	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
28	roles	Phân quyền	/admin/roles	system	Gán quyền cho 3 role	402	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
29	account	Tài khoản của tôi	/account	customer	Đơn hàng, điểm, voucher của khách	500	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: product_bundle_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.product_bundle_items (id, bundle_id, coffee_id, quantity, optional, display_order, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: product_bundles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.product_bundles (id, name, description, bundle_price, starts_at, ends_at, status, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: product_placement_rules; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.product_placement_rules (id, coffee_id, section, title, display_order, starts_at, ends_at, active, publication_status, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: promotion_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.promotion_items (id, promotion_id, coffee_id, custom_discount, sale_price, max_quantity, sold_quantity, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	2	8	\N	\N	100	0	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	2	9	\N	36000.00	100	0	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: promotions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.promotions (id, name, description, apply_scope, discount_percent, min_order_amount, start_date, end_date, status, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	Giảm 10% đơn từ 150K	Áp dụng cho mọi đơn từ 150.000đ	ORDER	10.00	150000.00	2026-10-05 00:00:00	2026-12-05 00:00:00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	Giờ vàng Cold Brew	Cold Brew giảm giá, số lượng có hạn	COFFEE	20.00	\N	2026-10-05 00:00:00	2026-11-05 00:00:00	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: recipe_materials; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recipe_materials (id, recipe_id, material_id, quantity, note, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
11	4	2	18.00	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
12	4	4	180.00	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
13	5	2	18.00	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
14	5	4	120.00	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
17	7	2	18.00	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
15	6	2	18.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 01:49:52.920861	t	admin
16	6	10	100.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 01:49:52.920964	t	admin
32	6	2	18.00	\N	admin	2026-10-07 01:49:53.222605	admin	2026-10-07 01:52:51.180158	t	admin
33	6	10	100.00	\N	admin	2026-10-07 01:49:53.919844	admin	2026-10-07 01:52:51.180252	t	admin
34	6	2	18.00	\N	admin	2026-10-07 01:52:51.219007	admin	2026-10-07 02:03:54.221254	t	admin
35	6	10	100.00	\N	admin	2026-10-07 01:52:51.221519	admin	2026-10-07 02:03:54.221321	t	admin
36	6	2	18.00	\N	admin	2026-10-07 02:03:54.314389	\N	\N	f	\N
37	6	10	100.00	\N	admin	2026-10-07 02:03:54.317091	\N	\N	f	\N
21	9	2	30.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:18:55.959439	t	admin
22	9	10	150.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:18:55.962948	t	admin
38	9	11	80.00	\N	admin	2026-10-07 07:18:56.17341	\N	\N	f	\N
39	9	10	150.00	\N	admin	2026-10-07 07:18:56.211947	\N	\N	f	\N
18	8	2	30.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:18:57.178288	t	admin
19	8	10	150.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:18:57.178288	t	admin
20	8	9	0.50	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:18:57.178288	t	admin
8	3	1	25.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:15.632456	t	admin
9	3	10	150.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:15.637156	t	admin
10	3	5	10.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:15.637156	t	admin
43	3	16	50.00	\N	admin	2026-10-07 07:48:15.979748	\N	\N	f	\N
44	3	12	15.00	\N	admin	2026-10-07 07:48:16.038994	\N	\N	f	\N
45	3	10	150.00	\N	admin	2026-10-07 07:48:16.082497	\N	\N	f	\N
5	2	1	25.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.118424	t	admin
6	2	10	150.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.118424	t	admin
7	2	3	30.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.118424	t	admin
46	2	16	50.00	\N	admin	2026-10-07 07:48:17.501532	\N	\N	f	\N
47	2	3	30.00	\N	admin	2026-10-07 07:48:17.548473	\N	\N	f	\N
48	2	10	150.00	\N	admin	2026-10-07 07:48:17.586077	\N	\N	f	\N
1	1	1	12.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:18.631796	t	admin
2	1	10	150.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:18.631796	t	admin
3	1	4	60.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:18.631796	t	admin
4	1	3	30.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:18.631796	t	admin
49	1	16	25.00	\N	admin	2026-10-07 07:48:18.984665	\N	\N	f	\N
50	1	3	30.00	\N	admin	2026-10-07 07:48:19.019766	\N	\N	f	\N
51	1	4	60.00	\N	admin	2026-10-07 07:48:19.062218	\N	\N	f	\N
52	1	10	150.00	\N	admin	2026-10-07 07:48:19.100269	\N	\N	f	\N
29	12	1	25.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:20.141682	t	admin
30	12	6	40.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:20.141682	t	admin
31	12	3	20.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:20.141682	t	admin
53	12	16	50.00	\N	admin	2026-10-07 07:48:20.647508	\N	\N	f	\N
54	12	3	20.00	\N	admin	2026-10-07 07:48:20.694605	\N	\N	f	\N
55	12	6	40.00	\N	admin	2026-10-07 07:48:20.734399	\N	\N	f	\N
56	12	10	120.00	\N	admin	2026-10-07 07:48:20.784125	\N	\N	f	\N
26	11	1	20.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:21.833886	t	admin
27	11	7	1.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:21.833886	t	admin
28	11	3	25.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:21.833886	t	admin
57	11	16	40.00	\N	admin	2026-10-07 07:48:22.148926	\N	\N	f	\N
58	11	7	1.00	trứng tiệt trùng	admin	2026-10-07 07:48:22.202324	\N	\N	f	\N
59	11	3	25.00	\N	admin	2026-10-07 07:48:22.249711	\N	\N	f	\N
23	10	8	60.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:23.28122	t	admin
24	10	1	20.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:23.28122	t	admin
25	10	10	200.00	\N	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:23.28122	t	admin
60	10	16	40.00	\N	admin	2026-10-07 07:48:23.654824	\N	\N	f	\N
61	10	8	60.00	\N	admin	2026-10-07 07:48:23.701079	\N	\N	f	\N
62	10	3	20.00	\N	admin	2026-10-07 07:48:23.749447	\N	\N	f	\N
63	10	10	200.00	\N	admin	2026-10-07 07:48:23.985736	\N	\N	f	\N
40	8	11	70.00	\N	admin	2026-10-07 07:18:57.440496	admin	2026-10-07 07:48:25.066624	t	admin
41	8	9	0.50	\N	admin	2026-10-07 07:18:57.479134	admin	2026-10-07 07:48:25.066624	t	admin
42	8	10	150.00	\N	admin	2026-10-07 07:18:57.517046	admin	2026-10-07 07:48:25.066624	t	admin
64	8	11	70.00	\N	admin	2026-10-07 07:48:25.616598	\N	\N	f	\N
65	8	17	15.00	\N	admin	2026-10-07 07:48:25.650621	\N	\N	f	\N
66	8	9	0.50	\N	admin	2026-10-07 07:48:25.70051	\N	\N	f	\N
67	8	10	150.00	\N	admin	2026-10-07 07:48:25.750395	\N	\N	f	\N
\.


--
-- Data for Name: recipe_steps; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recipe_steps (id, recipe_id, step_no, instruction, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
4	4	1	Chiết xuất 1 shot espresso 18g / 36ml trong 28 giây	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
5	4	2	Đánh nóng 180ml sữa tươi tới 60°C	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
6	4	3	Rót sữa vào espresso, tạo latte art	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
10	6	1	Chuẩn bị ly vừa với 100 đá	admin	2026-10-07 01:52:51.22327	admin	2026-10-07 02:03:54.221374	t	admin
11	6	2	Chiết xuất cà phê từ máy trong 30s	admin	2026-10-07 01:52:51.230842	admin	2026-10-07 02:03:54.221411	t	admin
12	6	3	Rót trực tiếp vào đá rồi thêm 100ml nước lạnh	admin	2026-10-07 01:52:51.232663	admin	2026-10-07 02:03:54.221456	t	admin
13	6	1	Xay pha mịn 18g hạt arabica, sau đó chiết xuất từ máy trong 30s	admin	2026-10-07 02:03:54.319117	\N	\N	f	\N
14	6	2	Chuẩn bị ly vừa với 100g đá	admin	2026-10-07 02:03:54.320869	\N	\N	f	\N
15	6	3	Rót trực tiếp vào đá rồi thêm 100ml nước lạnh	admin	2026-10-07 02:03:54.32281	\N	\N	f	\N
16	9	1	Lắc nhẹ chai cốt cold brew, kiểm tra hạn dùng trên nhãn lô	admin	2026-10-07 07:18:56.249217	\N	\N	f	\N
17	9	2	Cho 150 g đá vào ly 400 ml	admin	2026-10-07 07:18:56.286555	\N	\N	f	\N
18	9	3	Rót 80 ml cốt cold brew lên đá	admin	2026-10-07 07:18:56.323929	\N	\N	f	\N
19	9	4	Thêm 80 ml nước lọc lạnh, khuấy nhẹ 3–4 vòng	admin	2026-10-07 07:18:56.360624	\N	\N	f	\N
20	9	5	Hỏi khách có cần thêm nước đường / sữa không, phục vụ ngay	admin	2026-10-07 07:18:56.39929	\N	\N	f	\N
25	3	1	Lắc nhẹ bình cốt phin, kiểm tra giờ pha trên nhãn (dùng trong 24 giờ)	admin	2026-10-07 07:48:16.13157	\N	\N	f	\N
26	3	2	Rót 15 ml nước đường vào ly (khách dặn ít ngọt thì giảm còn 8 ml)	admin	2026-10-07 07:48:16.179207	\N	\N	f	\N
27	3	3	Thêm 50 ml cốt phin, khuấy đều	admin	2026-10-07 07:48:16.215769	\N	\N	f	\N
28	3	4	Cho 150 g đá, khuấy nhẹ 2–3 vòng rồi phục vụ	admin	2026-10-07 07:48:16.268093	\N	\N	f	\N
1	2	1	Tráng phin bằng nước sôi, cho 25g cà phê, nén nhẹ	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.118424	t	admin
2	2	2	Rót 20ml nước sôi ủ 30 giây, sau đó rót thêm 60ml	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.118424	t	admin
3	2	3	Cho 30ml sữa đặc vào ly, đổ cà phê, khuấy đều và thêm đá	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.118424	t	admin
29	2	1	Lắc nhẹ bình cốt phin, kiểm tra giờ pha trên nhãn	admin	2026-10-07 07:48:17.617525	\N	\N	f	\N
30	2	2	Rót 30 ml sữa đặc vào ly	admin	2026-10-07 07:48:17.66549	\N	\N	f	\N
31	2	3	Thêm 50 ml cốt phin, khuấy đến khi sữa tan đều, màu nâu sữa	admin	2026-10-07 07:48:17.700505	\N	\N	f	\N
32	2	4	Cho 150 g đá, khuấy nhẹ rồi phục vụ	admin	2026-10-07 07:48:17.733373	\N	\N	f	\N
33	1	1	Rót 30 ml sữa đặc và 60 ml sữa tươi vào ly, khuấy đều	admin	2026-10-07 07:48:19.138519	\N	\N	f	\N
34	1	2	Cho 150 g đá	admin	2026-10-07 07:48:19.176723	\N	\N	f	\N
35	1	3	Rót từ từ 25 ml cốt phin lên mặt đá để tạo lớp nâu trên nền trắng	admin	2026-10-07 07:48:19.216052	\N	\N	f	\N
36	1	4	Phục vụ kèm muỗng, dặn khách khuấy trước khi uống	admin	2026-10-07 07:48:19.254128	\N	\N	f	\N
7	12	1	Pha phin 25g cà phê với 60ml nước	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:20.141682	t	admin
8	12	2	Cho sữa đặc và đá vào ly, rót cà phê	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:20.141682	t	admin
9	12	3	Phủ 40ml kem muối lên trên, không khuấy	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:20.141682	t	admin
37	12	1	Rót 20 ml sữa đặc vào ly, cho 120 g đá	admin	2026-10-07 07:48:20.831229	\N	\N	f	\N
38	12	2	Thêm 50 ml cốt phin	admin	2026-10-07 07:48:20.865218	\N	\N	f	\N
39	12	3	Khuấy nhẹ hộp kem muối cho đều (kem để ngăn mát, dùng trong 2 ngày)	admin	2026-10-07 07:48:20.914649	\N	\N	f	\N
40	12	4	Rót 40 ml kem muối phủ kín mặt ly, không khuấy; phục vụ ngay để giữ lớp kem	admin	2026-10-07 07:48:20.968497	\N	\N	f	\N
41	11	1	Hâm nóng 40 ml cốt phin tới khoảng 70 °C, rót vào tách; đặt tách trong bát nước nóng để giữ nhiệt	admin	2026-10-07 07:48:22.283811	\N	\N	f	\N
42	11	2	Tách lấy lòng đỏ 1 quả trứng tiệt trùng, thêm 25 ml sữa đặc	admin	2026-10-07 07:48:22.340768	\N	\N	f	\N
43	11	3	Đánh bằng máy đánh trứng 3–4 phút đến khi kem bông mịn, màu vàng nhạt, nhấc lên chảy thành dải	admin	2026-10-07 07:48:22.385291	\N	\N	f	\N
44	11	4	Rót kem trứng phủ lên mặt cà phê, phục vụ ngay kèm muỗng	admin	2026-10-07 07:48:22.429623	\N	\N	f	\N
45	10	1	Cho 60 ml cốt dừa, 20 ml sữa đặc và 200 g đá vào máy xay	admin	2026-10-07 07:48:24.03761	\N	\N	f	\N
46	10	2	Xay 20–30 giây đến khi sánh mịn như tuyết	admin	2026-10-07 07:48:24.083555	\N	\N	f	\N
47	10	3	Đổ hỗn hợp dừa vào ly	admin	2026-10-07 07:48:24.128307	\N	\N	f	\N
21	8	1	Đập dập 1 nhánh sả tươi, cho vào ly 400 ml cùng 150 g đá	admin	2026-10-07 07:18:57.554818	admin	2026-10-07 07:48:25.066624	t	admin
22	8	2	Vắt ½ quả cam vàng, lọc hạt, rót nước cam vào ly	admin	2026-10-07 07:18:57.592088	admin	2026-10-07 07:48:25.066624	t	admin
23	8	3	Rót chậm 70 ml cốt cold brew lên trên để tạo 2 lớp màu	admin	2026-10-07 07:18:57.630657	admin	2026-10-07 07:48:25.066624	t	admin
24	8	4	Trang trí 1 lát cam, cắm ống hút, dặn khách khuấy đều trước khi uống	admin	2026-10-07 07:18:57.667651	admin	2026-10-07 07:48:25.066624	t	admin
48	10	4	Rót 40 ml cốt phin lên trên, phục vụ kèm muỗng	admin	2026-10-07 07:48:24.17597	\N	\N	f	\N
49	8	1	Rót 15 ml siro sả vào ly 400 ml	admin	2026-10-07 07:48:25.804587	\N	\N	f	\N
50	8	2	Vắt ½ quả cam vàng, lọc hạt, rót nước cam vào ly	admin	2026-10-07 07:48:25.850873	\N	\N	f	\N
51	8	3	Cho 150 g đá	admin	2026-10-07 07:48:25.895811	\N	\N	f	\N
52	8	4	Rót chậm 70 ml cốt cold brew lên trên để tạo 2 lớp màu	admin	2026-10-07 07:48:25.941396	\N	\N	f	\N
53	8	5	Trang trí 1 lát cam, dặn khách khuấy đều trước khi uống	admin	2026-10-07 07:48:25.984757	\N	\N	f	\N
\.


--
-- Data for Name: recipes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recipes (id, coffee_id, brew_method, version, description, brew_time_min, is_active, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
4	4	MACHINE	1	\N	4	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
5	5	MACHINE	1	\N	4	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
7	7	MACHINE	1	\N	2	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
6	6	MACHINE	1	cơ bản	3	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 02:03:54.325926	f	\N
9	9	COLD_BREW	1	\N	2	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:18:56.441312	f	\N
3	3	PHIN	1	\N	1	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:16.315602	f	\N
2	2	PHIN	1	\N	1	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:17.7826	f	\N
1	1	PHIN	1	\N	2	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:19.292682	f	\N
12	12	PHIN	1	\N	2	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:21.001855	f	\N
11	11	PHIN	1	\N	5	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:22.479417	f	\N
10	10	PHIN	1	\N	3	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:24.22082	f	\N
8	8	COLD_BREW	1	\N	2	t	system	2026-10-06 06:37:15.880553	admin	2026-10-07 07:48:26.033386	f	\N
\.


--
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.role_permissions (id, role_id, permission_id, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	1	1	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	1	2	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	1	3	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	1	4	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
5	1	5	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
6	1	6	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
7	1	7	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
8	1	8	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
9	1	9	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
10	1	10	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
11	1	11	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
12	1	12	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
13	1	13	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
14	1	14	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
15	1	15	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
16	1	16	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
17	1	17	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
18	1	18	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
19	1	19	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
20	1	20	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
21	1	21	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
22	1	22	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
23	1	23	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
24	1	24	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
25	1	25	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
26	1	26	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
27	1	27	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
28	1	28	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
29	2	1	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
30	2	2	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
31	2	4	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
32	2	5	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
33	2	6	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
34	2	8	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
35	2	10	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
36	2	11	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
37	2	13	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
38	2	14	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
39	2	15	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
40	2	17	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
41	2	18	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
42	2	19	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
43	3	29	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.roles (id, code, name, description, active, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	ADMIN	Quản lý	Toàn quyền hệ thống quản trị	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	STAFF	Nhân viên	Bán hàng, pha chế, giao nhận, kho, chăm sóc khách	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	CUSTOMER	Khách hàng	Tài khoản khách: đơn hàng, điểm tích lũy, voucher của mình	t	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: shipments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.shipments (id, order_id, address_id, carrier, tracking_code, shipping_fee, status, booked_at, driver_accepted_at, delivered_at, note, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
\.


--
-- Data for Name: staff; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.staff (id, full_name, "position", phone, email, hire_date, work_status, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
2	Trần Quốc Bảo	Barista	0901000002	bao.tq@coffeeholic.vn	2024-03-01	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	Lê Thu Hà	Barista	0901000003	ha.lt@coffeeholic.vn	2024-06-10	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	Phạm Gia Huy	Thu ngân	0901000004	huy.pg@coffeeholic.vn	2025-02-20	ACTIVE	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
1	Hồ Tiến Việt	Quản lý cửa hàng	0901000001	anh.nm@coffeeholic.vn	2024-01-15	ACTIVE	system	2026-10-06 06:37:15.880553	admin	2026-10-06 07:14:51.11823	f	\N
5	Võ Thanh Tâm	Phục vụ	0901000005	\N	2025-05-05	ACTIVE	system	2026-10-06 06:37:15.880553	admin	2026-10-06 07:15:04.322755	f	\N
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_roles (id, user_id, role_id, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	1	1	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	3	2	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	2	2	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	4	3	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, username, password_hash, full_name, email, phone, active, staff_id, customer_id, last_login_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
2	staff1	$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW	Trần Quốc Bảo	bao.tq@coffeeholic.vn	0901000002	t	2	\N	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
3	staff2	$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW	Phạm Gia Huy	huy.pg@coffeeholic.vn	0901000004	t	4	\N	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
4	0912345678	$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW	Đặng Hoài Nam	nam.dh@gmail.com	0912345678	t	\N	1	\N	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
1	admin	$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW	Hồ Tiến Việt	\N	0901000001	t	1	\N	2026-10-07 12:57:14.957463	system	2026-10-06 06:37:15.880553	admin	2026-10-06 07:14:34.014203	f	\N
\.


--
-- Data for Name: vouchers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vouchers (id, code, customer_id, order_id, incident_id, discount_value, min_order_value, status, issued_at, expires_at, created_by, created_at, updated_by, updated_at, del_flag, del_user) FROM stdin;
1	WELCOME20K	\N	\N	\N	20000.00	80000.00	ISSUED	2026-10-06 06:37:15.880553	2027-01-04 06:37:15.880553	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
2	NAM50K	1	\N	\N	50000.00	100000.00	ISSUED	2026-10-06 06:37:15.880553	2026-11-05 06:37:15.880553	system	2026-10-06 06:37:15.880553	\N	\N	f	\N
\.


--
-- Name: categories_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.categories_id_seq', 3, true);


--
-- Name: daily_revenue_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.daily_revenue_id_seq', 1, false);


--
-- Name: material_transactions_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.material_transactions_id_seq', 1, false);


--
-- Name: materials_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.materials_id_seq', 5, true);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.order_items_id_seq', 1, false);


--
-- Name: orders_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.orders_id_seq', 1, false);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.payments_id_seq', 1, false);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.products_id_seq', 4, true);


--
-- Name: shipments_id_seq; Type: SEQUENCE SET; Schema: legacy_backup; Owner: -
--

SELECT pg_catalog.setval('legacy_backup.shipments_id_seq', 1, false);


--
-- Name: background_service_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.background_service_logs_id_seq', 1166, true);


--
-- Name: cart_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cart_items_id_seq', 3, true);


--
-- Name: carts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.carts_id_seq', 1, true);


--
-- Name: categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.categories_id_seq', 4, true);


--
-- Name: coffees_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.coffees_id_seq', 13, true);


--
-- Name: customer_addresses_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customer_addresses_id_seq', 1, false);


--
-- Name: customer_group_members_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customer_group_members_id_seq', 1, false);


--
-- Name: customer_groups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customer_groups_id_seq', 1, false);


--
-- Name: customers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customers_id_seq', 2, true);


--
-- Name: dining_tables_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.dining_tables_id_seq', 6, true);


--
-- Name: feedbacks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.feedbacks_id_seq', 1, false);


--
-- Name: images_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.images_id_seq', 4, true);


--
-- Name: incidents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.incidents_id_seq', 1, false);


--
-- Name: interactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.interactions_id_seq', 1, false);


--
-- Name: loyalty_point_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.loyalty_point_history_id_seq', 1, false);


--
-- Name: material_batches_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.material_batches_id_seq', 13, true);


--
-- Name: material_components_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.material_components_id_seq', 9, true);


--
-- Name: material_transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.material_transactions_id_seq', 19, true);


--
-- Name: materials_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.materials_id_seq', 17, true);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.order_items_id_seq', 1, false);


--
-- Name: orders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.orders_id_seq', 1, false);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payments_id_seq', 1, false);


--
-- Name: permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.permissions_id_seq', 29, true);


--
-- Name: product_bundle_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.product_bundle_items_id_seq', 1, false);


--
-- Name: product_bundles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.product_bundles_id_seq', 1, false);


--
-- Name: product_placement_rules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.product_placement_rules_id_seq', 1, false);


--
-- Name: promotion_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.promotion_items_id_seq', 2, true);


--
-- Name: promotions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.promotions_id_seq', 2, true);


--
-- Name: recipe_materials_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recipe_materials_id_seq', 67, true);


--
-- Name: recipe_steps_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recipe_steps_id_seq', 53, true);


--
-- Name: recipes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recipes_id_seq', 12, true);


--
-- Name: role_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.role_permissions_id_seq', 43, true);


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.roles_id_seq', 3, true);


--
-- Name: shipments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.shipments_id_seq', 1, false);


--
-- Name: staff_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.staff_id_seq', 5, true);


--
-- Name: user_roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_roles_id_seq', 4, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 4, true);


--
-- Name: vouchers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vouchers_id_seq', 2, true);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: daily_revenue daily_revenue_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.daily_revenue
    ADD CONSTRAINT daily_revenue_pkey PRIMARY KEY (id);


--
-- Name: daily_revenue daily_revenue_revenue_date_key; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.daily_revenue
    ADD CONSTRAINT daily_revenue_revenue_date_key UNIQUE (revenue_date);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: material_transactions material_transactions_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.material_transactions
    ADD CONSTRAINT material_transactions_pkey PRIMARY KEY (id);


--
-- Name: materials materials_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.materials
    ADD CONSTRAINT materials_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_order_code_key; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.orders
    ADD CONSTRAINT orders_order_code_key UNIQUE (order_code);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: product_materials product_materials_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.product_materials
    ADD CONSTRAINT product_materials_pkey PRIMARY KEY (product_id, material_id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: shipments shipments_pkey; Type: CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.shipments
    ADD CONSTRAINT shipments_pkey PRIMARY KEY (id);


--
-- Name: background_service_logs background_service_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_service_logs
    ADD CONSTRAINT background_service_logs_pkey PRIMARY KEY (id);


--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);


--
-- Name: carts carts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);


--
-- Name: carts carts_session_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_session_code_key UNIQUE (session_code);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: coffees coffees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coffees
    ADD CONSTRAINT coffees_pkey PRIMARY KEY (id);


--
-- Name: customer_addresses customer_addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT customer_addresses_pkey PRIMARY KEY (id);


--
-- Name: customer_group_members customer_group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_group_members
    ADD CONSTRAINT customer_group_members_pkey PRIMARY KEY (id);


--
-- Name: customer_groups customer_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_groups
    ADD CONSTRAINT customer_groups_pkey PRIMARY KEY (id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: dining_tables dining_tables_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dining_tables
    ADD CONSTRAINT dining_tables_pkey PRIMARY KEY (id);


--
-- Name: feedbacks feedbacks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedbacks
    ADD CONSTRAINT feedbacks_pkey PRIMARY KEY (id);


--
-- Name: images images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT images_pkey PRIMARY KEY (id);


--
-- Name: incidents incidents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_pkey PRIMARY KEY (id);


--
-- Name: interactions interactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interactions
    ADD CONSTRAINT interactions_pkey PRIMARY KEY (id);


--
-- Name: loyalty_point_history loyalty_point_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_point_history
    ADD CONSTRAINT loyalty_point_history_pkey PRIMARY KEY (id);


--
-- Name: material_batches material_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_batches
    ADD CONSTRAINT material_batches_pkey PRIMARY KEY (id);


--
-- Name: material_components material_components_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_components
    ADD CONSTRAINT material_components_pkey PRIMARY KEY (id);


--
-- Name: material_transactions material_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT material_transactions_pkey PRIMARY KEY (id);


--
-- Name: materials materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materials
    ADD CONSTRAINT materials_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_cart_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_cart_id_key UNIQUE (cart_id);


--
-- Name: orders orders_order_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_code_key UNIQUE (order_code);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: payments payments_transaction_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_transaction_code_key UNIQUE (transaction_code);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: product_bundle_items product_bundle_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_bundle_items
    ADD CONSTRAINT product_bundle_items_pkey PRIMARY KEY (id);


--
-- Name: product_bundles product_bundles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_bundles
    ADD CONSTRAINT product_bundles_pkey PRIMARY KEY (id);


--
-- Name: product_placement_rules product_placement_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_placement_rules
    ADD CONSTRAINT product_placement_rules_pkey PRIMARY KEY (id);


--
-- Name: promotion_items promotion_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_items
    ADD CONSTRAINT promotion_items_pkey PRIMARY KEY (id);


--
-- Name: promotions promotions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotions
    ADD CONSTRAINT promotions_pkey PRIMARY KEY (id);


--
-- Name: recipe_materials recipe_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_materials
    ADD CONSTRAINT recipe_materials_pkey PRIMARY KEY (id);


--
-- Name: recipe_steps recipe_steps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT recipe_steps_pkey PRIMARY KEY (id);


--
-- Name: recipes recipes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_pkey PRIMARY KEY (id);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: shipments shipments_order_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT shipments_order_id_key UNIQUE (order_id);


--
-- Name: shipments shipments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT shipments_pkey PRIMARY KEY (id);


--
-- Name: staff staff_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff
    ADD CONSTRAINT staff_pkey PRIMARY KEY (id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vouchers vouchers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT vouchers_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON legacy_backup.flyway_schema_history USING btree (success);


--
-- Name: idx_material_transactions_material; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_material_transactions_material ON legacy_backup.material_transactions USING btree (material_id);


--
-- Name: idx_order_items_order; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_order_items_order ON legacy_backup.order_items USING btree (order_id);


--
-- Name: idx_order_items_product; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_order_items_product ON legacy_backup.order_items USING btree (product_id);


--
-- Name: idx_orders_created_at; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_orders_created_at ON legacy_backup.orders USING btree (created_at);


--
-- Name: idx_orders_status; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_orders_status ON legacy_backup.orders USING btree (status);


--
-- Name: idx_payments_order; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_payments_order ON legacy_backup.payments USING btree (order_id);


--
-- Name: idx_products_category; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_products_category ON legacy_backup.products USING btree (category_id);


--
-- Name: idx_shipments_order; Type: INDEX; Schema: legacy_backup; Owner: -
--

CREATE INDEX idx_shipments_order ON legacy_backup.shipments USING btree (order_id);


--
-- Name: idx_background_service_logs; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_background_service_logs ON public.background_service_logs USING btree (service, check_time DESC);


--
-- Name: idx_cart_items_cart; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cart_items_cart ON public.cart_items USING btree (cart_id);


--
-- Name: idx_cart_items_coffee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cart_items_coffee ON public.cart_items USING btree (coffee_id);


--
-- Name: idx_carts_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_carts_customer ON public.carts USING btree (customer_id);


--
-- Name: idx_carts_table; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_carts_table ON public.carts USING btree (table_id);


--
-- Name: idx_cgm_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cgm_customer ON public.customer_group_members USING btree (customer_id);


--
-- Name: idx_coffees_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_coffees_category ON public.coffees USING btree (category_id);


--
-- Name: idx_coffees_image; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_coffees_image ON public.coffees USING btree (image_id);


--
-- Name: idx_customer_addresses_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_addresses_customer ON public.customer_addresses USING btree (customer_id);


--
-- Name: idx_feedbacks_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedbacks_customer ON public.feedbacks USING btree (customer_id);


--
-- Name: idx_incidents_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_order ON public.incidents USING btree (order_id);


--
-- Name: idx_interactions_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interactions_customer ON public.interactions USING btree (customer_id);


--
-- Name: idx_interactions_remind; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interactions_remind ON public.interactions USING btree (remind_at) WHERE ((closed_at IS NULL) AND (NOT del_flag));


--
-- Name: idx_lph_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lph_customer ON public.loyalty_point_history USING btree (customer_id);


--
-- Name: idx_lph_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lph_order ON public.loyalty_point_history USING btree (order_id);


--
-- Name: idx_material_batches_material; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_batches_material ON public.material_batches USING btree (material_id);


--
-- Name: idx_material_components_component; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_components_component ON public.material_components USING btree (component_id);


--
-- Name: idx_material_tx_batch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_tx_batch ON public.material_transactions USING btree (batch_id);


--
-- Name: idx_material_tx_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_tx_created ON public.material_transactions USING btree (created_at);


--
-- Name: idx_material_tx_order_item; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_tx_order_item ON public.material_transactions USING btree (order_item_id);


--
-- Name: idx_material_tx_produced_batch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_tx_produced_batch ON public.material_transactions USING btree (produced_batch_id);


--
-- Name: idx_material_tx_staff; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_material_tx_staff ON public.material_transactions USING btree (staff_id);


--
-- Name: idx_order_items_coffee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_items_coffee ON public.order_items USING btree (coffee_id);


--
-- Name: idx_order_items_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_items_order ON public.order_items USING btree (order_id);


--
-- Name: idx_orders_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_customer ON public.orders USING btree (customer_id);


--
-- Name: idx_orders_ordered_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_ordered_at ON public.orders USING btree (ordered_at);


--
-- Name: idx_orders_promotion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_promotion ON public.orders USING btree (promotion_id);


--
-- Name: idx_orders_staff; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_staff ON public.orders USING btree (staff_id);


--
-- Name: idx_orders_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_status ON public.orders USING btree (status);


--
-- Name: idx_orders_table; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_table ON public.orders USING btree (table_id);


--
-- Name: idx_payments_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_order ON public.payments USING btree (order_id);


--
-- Name: idx_product_bundle_items_coffee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_bundle_items_coffee ON public.product_bundle_items USING btree (coffee_id) WHERE (NOT del_flag);


--
-- Name: idx_product_bundles_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_bundles_status ON public.product_bundles USING btree (status, starts_at, ends_at) WHERE (NOT del_flag);


--
-- Name: idx_product_placement_section; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_placement_section ON public.product_placement_rules USING btree (section, publication_status, active, display_order) WHERE (NOT del_flag);


--
-- Name: idx_promotion_items_coffee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotion_items_coffee ON public.promotion_items USING btree (coffee_id);


--
-- Name: idx_promotions_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promotions_period ON public.promotions USING btree (start_date, end_date);


--
-- Name: idx_recipe_materials_material; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_materials_material ON public.recipe_materials USING btree (material_id);


--
-- Name: idx_role_permissions_permission; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_permissions_permission ON public.role_permissions USING btree (permission_id);


--
-- Name: idx_shipments_address; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_shipments_address ON public.shipments USING btree (address_id);


--
-- Name: idx_user_roles_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_role ON public.user_roles USING btree (role_id);


--
-- Name: idx_vouchers_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vouchers_customer ON public.vouchers USING btree (customer_id);


--
-- Name: idx_vouchers_incident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vouchers_incident ON public.vouchers USING btree (incident_id);


--
-- Name: idx_vouchers_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vouchers_order ON public.vouchers USING btree (order_id);


--
-- Name: uq_categories_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_categories_name ON public.categories USING btree (lower((name)::text)) WHERE (NOT del_flag);


--
-- Name: uq_customer_addresses_default; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customer_addresses_default ON public.customer_addresses USING btree (customer_id) WHERE (is_default AND (NOT del_flag));


--
-- Name: uq_customer_group_members; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customer_group_members ON public.customer_group_members USING btree (group_id, customer_id) WHERE (NOT del_flag);


--
-- Name: uq_customer_groups_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customer_groups_name ON public.customer_groups USING btree (lower((name)::text)) WHERE (NOT del_flag);


--
-- Name: uq_customers_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_customers_phone ON public.customers USING btree (phone) WHERE (NOT del_flag);


--
-- Name: uq_dining_tables_no; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_dining_tables_no ON public.dining_tables USING btree (table_no) WHERE (NOT del_flag);


--
-- Name: uq_dining_tables_qr; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_dining_tables_qr ON public.dining_tables USING btree (qr_code) WHERE (NOT del_flag);


--
-- Name: uq_material_components; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_material_components ON public.material_components USING btree (material_id, component_id) WHERE (NOT del_flag);


--
-- Name: uq_materials_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_materials_name ON public.materials USING btree (lower((name)::text)) WHERE (NOT del_flag);


--
-- Name: uq_permissions_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_permissions_code ON public.permissions USING btree (code) WHERE (NOT del_flag);


--
-- Name: uq_product_bundle_items; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_product_bundle_items ON public.product_bundle_items USING btree (bundle_id, coffee_id) WHERE (NOT del_flag);


--
-- Name: uq_product_bundles_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_product_bundles_name ON public.product_bundles USING btree (lower((name)::text)) WHERE (NOT del_flag);


--
-- Name: uq_product_placement_rule; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_product_placement_rule ON public.product_placement_rules USING btree (coffee_id, section) WHERE (NOT del_flag);


--
-- Name: uq_promotion_items; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_promotion_items ON public.promotion_items USING btree (promotion_id, coffee_id) WHERE (NOT del_flag);


--
-- Name: uq_recipe_materials; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_recipe_materials ON public.recipe_materials USING btree (recipe_id, material_id) WHERE (NOT del_flag);


--
-- Name: uq_recipe_steps; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_recipe_steps ON public.recipe_steps USING btree (recipe_id, step_no) WHERE (NOT del_flag);


--
-- Name: uq_recipes_active_coffee; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_recipes_active_coffee ON public.recipes USING btree (coffee_id) WHERE (is_active AND (NOT del_flag));


--
-- Name: uq_recipes_version; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_recipes_version ON public.recipes USING btree (coffee_id, version) WHERE (NOT del_flag);


--
-- Name: uq_role_permissions; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_role_permissions ON public.role_permissions USING btree (role_id, permission_id) WHERE (NOT del_flag);


--
-- Name: uq_roles_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_roles_code ON public.roles USING btree (code) WHERE (NOT del_flag);


--
-- Name: uq_staff_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_staff_email ON public.staff USING btree (email) WHERE (NOT del_flag);


--
-- Name: uq_staff_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_staff_phone ON public.staff USING btree (phone) WHERE (NOT del_flag);


--
-- Name: uq_user_roles; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_user_roles ON public.user_roles USING btree (user_id, role_id) WHERE (NOT del_flag);


--
-- Name: uq_users_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_users_customer ON public.users USING btree (customer_id) WHERE ((NOT del_flag) AND (customer_id IS NOT NULL));


--
-- Name: uq_users_staff; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_users_staff ON public.users USING btree (staff_id) WHERE ((NOT del_flag) AND (staff_id IS NOT NULL));


--
-- Name: uq_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_users_username ON public.users USING btree (username) WHERE (NOT del_flag);


--
-- Name: uq_vouchers_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_vouchers_code ON public.vouchers USING btree (upper((code)::text)) WHERE (NOT del_flag);


--
-- Name: material_transactions material_transactions_material_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.material_transactions
    ADD CONSTRAINT material_transactions_material_id_fkey FOREIGN KEY (material_id) REFERENCES legacy_backup.materials(id);


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES legacy_backup.orders(id) ON DELETE CASCADE;


--
-- Name: order_items order_items_product_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.order_items
    ADD CONSTRAINT order_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES legacy_backup.products(id);


--
-- Name: payments payments_order_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.payments
    ADD CONSTRAINT payments_order_id_fkey FOREIGN KEY (order_id) REFERENCES legacy_backup.orders(id) ON DELETE CASCADE;


--
-- Name: product_materials product_materials_material_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.product_materials
    ADD CONSTRAINT product_materials_material_id_fkey FOREIGN KEY (material_id) REFERENCES legacy_backup.materials(id) ON DELETE CASCADE;


--
-- Name: product_materials product_materials_product_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.product_materials
    ADD CONSTRAINT product_materials_product_id_fkey FOREIGN KEY (product_id) REFERENCES legacy_backup.products(id) ON DELETE CASCADE;


--
-- Name: products products_category_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.products
    ADD CONSTRAINT products_category_id_fkey FOREIGN KEY (category_id) REFERENCES legacy_backup.categories(id);


--
-- Name: shipments shipments_order_id_fkey; Type: FK CONSTRAINT; Schema: legacy_backup; Owner: -
--

ALTER TABLE ONLY legacy_backup.shipments
    ADD CONSTRAINT shipments_order_id_fkey FOREIGN KEY (order_id) REFERENCES legacy_backup.orders(id) ON DELETE CASCADE;


--
-- Name: cart_items cart_items_cart_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES public.carts(id);


--
-- Name: cart_items cart_items_coffee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id);


--
-- Name: carts carts_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: carts carts_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.dining_tables(id);


--
-- Name: coffees coffees_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coffees
    ADD CONSTRAINT coffees_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id);


--
-- Name: coffees coffees_image_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coffees
    ADD CONSTRAINT coffees_image_id_fkey FOREIGN KEY (image_id) REFERENCES public.images(id);


--
-- Name: customer_addresses customer_addresses_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT customer_addresses_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: customer_group_members customer_group_members_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_group_members
    ADD CONSTRAINT customer_group_members_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: customer_group_members customer_group_members_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_group_members
    ADD CONSTRAINT customer_group_members_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.customer_groups(id);


--
-- Name: feedbacks feedbacks_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedbacks
    ADD CONSTRAINT feedbacks_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: incidents incidents_handled_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_handled_by_fkey FOREIGN KEY (handled_by) REFERENCES public.staff(id);


--
-- Name: incidents incidents_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: interactions interactions_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interactions
    ADD CONSTRAINT interactions_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: loyalty_point_history loyalty_point_history_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_point_history
    ADD CONSTRAINT loyalty_point_history_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: loyalty_point_history loyalty_point_history_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_point_history
    ADD CONSTRAINT loyalty_point_history_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: material_batches material_batches_material_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_batches
    ADD CONSTRAINT material_batches_material_id_fkey FOREIGN KEY (material_id) REFERENCES public.materials(id);


--
-- Name: material_components material_components_component_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_components
    ADD CONSTRAINT material_components_component_id_fkey FOREIGN KEY (component_id) REFERENCES public.materials(id);


--
-- Name: material_components material_components_material_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_components
    ADD CONSTRAINT material_components_material_id_fkey FOREIGN KEY (material_id) REFERENCES public.materials(id);


--
-- Name: material_transactions material_transactions_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT material_transactions_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.material_batches(id);


--
-- Name: material_transactions material_transactions_order_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT material_transactions_order_item_id_fkey FOREIGN KEY (order_item_id) REFERENCES public.order_items(id);


--
-- Name: material_transactions material_transactions_produced_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT material_transactions_produced_batch_id_fkey FOREIGN KEY (produced_batch_id) REFERENCES public.material_batches(id);


--
-- Name: material_transactions material_transactions_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT material_transactions_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(id);


--
-- Name: order_items order_items_coffee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id);


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: orders orders_cart_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES public.carts(id);


--
-- Name: orders orders_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: orders orders_promotion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES public.promotions(id);


--
-- Name: orders orders_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(id);


--
-- Name: orders orders_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_table_id_fkey FOREIGN KEY (table_id) REFERENCES public.dining_tables(id);


--
-- Name: payments payments_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: product_bundle_items product_bundle_items_bundle_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_bundle_items
    ADD CONSTRAINT product_bundle_items_bundle_id_fkey FOREIGN KEY (bundle_id) REFERENCES public.product_bundles(id);


--
-- Name: product_bundle_items product_bundle_items_coffee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_bundle_items
    ADD CONSTRAINT product_bundle_items_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id);


--
-- Name: product_placement_rules product_placement_rules_coffee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_placement_rules
    ADD CONSTRAINT product_placement_rules_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id);


--
-- Name: promotion_items promotion_items_coffee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_items
    ADD CONSTRAINT promotion_items_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id);


--
-- Name: promotion_items promotion_items_promotion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promotion_items
    ADD CONSTRAINT promotion_items_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES public.promotions(id);


--
-- Name: recipe_materials recipe_materials_material_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_materials
    ADD CONSTRAINT recipe_materials_material_id_fkey FOREIGN KEY (material_id) REFERENCES public.materials(id);


--
-- Name: recipe_materials recipe_materials_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_materials
    ADD CONSTRAINT recipe_materials_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(id);


--
-- Name: recipe_steps recipe_steps_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT recipe_steps_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(id);


--
-- Name: recipes recipes_coffee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_coffee_id_fkey FOREIGN KEY (coffee_id) REFERENCES public.coffees(id);


--
-- Name: role_permissions role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- Name: role_permissions role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: shipments shipments_address_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT shipments_address_id_fkey FOREIGN KEY (address_id) REFERENCES public.customer_addresses(id);


--
-- Name: shipments shipments_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipments
    ADD CONSTRAINT shipments_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: users users_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: users users_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(id);


--
-- Name: vouchers vouchers_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT vouchers_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: vouchers vouchers_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT vouchers_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id);


--
-- Name: vouchers vouchers_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT vouchers_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- PostgreSQL database dump complete
--

\unrestrict iqB58SmG5gGgs1U7b6vtLFXE7eLi2xxLn5DsnvcmsL0eZC0pFmsvs9KrCHZkXXA

