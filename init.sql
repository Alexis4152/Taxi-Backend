--
-- PostgreSQL database dump
--

-- Dumped from database version 18.6
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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: driver_locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.driver_locations (
    id bigint NOT NULL,
    driver_id bigint NOT NULL,
    lat double precision NOT NULL,
    lng double precision NOT NULL,
    heading double precision,
    online boolean DEFAULT false NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: driver_locations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.driver_locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: driver_locations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.driver_locations_id_seq OWNED BY public.driver_locations.id;


--
-- Name: driver_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.driver_profiles (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    photo_url character varying(300),
    bank_account character varying(50),
    rating_avg numeric(3,2) DEFAULT 0 NOT NULL,
    rating_count integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    address character varying(200),
    taxi_id bigint
);


--
-- Name: driver_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.driver_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: driver_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.driver_profiles_id_seq OWNED BY public.driver_profiles.id;


--
-- Name: organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organizations (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    contact_phone character varying(20),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    logo_url character varying(300),
    is_independent_pool boolean DEFAULT false NOT NULL
);


--
-- Name: organizations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.organizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: organizations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.organizations_id_seq OWNED BY public.organizations.id;


--
-- Name: passenger_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.passenger_profiles (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    rating_avg numeric(3,2) DEFAULT 0 NOT NULL,
    rating_count integer DEFAULT 0 NOT NULL,
    photo_url character varying(300)
);


--
-- Name: passenger_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.passenger_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: passenger_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.passenger_profiles_id_seq OWNED BY public.passenger_profiles.id;


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(200) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: ratings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ratings (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    from_user_id bigint NOT NULL,
    to_user_id bigint NOT NULL,
    direction character varying(20) NOT NULL,
    score smallint NOT NULL,
    comment character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT ratings_direction_check CHECK (((direction)::text = ANY ((ARRAY['PASSENGER_TO_DRIVER'::character varying, 'DRIVER_TO_PASSENGER'::character varying])::text[]))),
    CONSTRAINT ratings_score_check CHECK (((score >= 1) AND (score <= 5)))
);


--
-- Name: ratings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ratings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ratings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ratings_id_seq OWNED BY public.ratings.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(200) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: shift_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shift_templates (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    name character varying(60) NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: shift_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.shift_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shift_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shift_templates_id_seq OWNED BY public.shift_templates.id;


--
-- Name: shifts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shifts (
    id bigint NOT NULL,
    taxi_id bigint NOT NULL,
    driver_id bigint NOT NULL,
    start_at timestamp without time zone NOT NULL,
    end_at timestamp without time zone,
    temporary boolean DEFAULT false NOT NULL,
    revoked boolean DEFAULT false CONSTRAINT shifts_active_not_null NOT NULL,
    created_by_user_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    shift_template_id bigint
);


--
-- Name: shifts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.shifts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shifts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shifts_id_seq OWNED BY public.shifts.id;


--
-- Name: tariff_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tariff_rules (
    id bigint NOT NULL,
    organization_id bigint,
    base_fare numeric(10,2) NOT NULL,
    per_km numeric(10,2) NOT NULL,
    min_fare numeric(10,2) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: tariff_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tariff_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tariff_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tariff_rules_id_seq OWNED BY public.tariff_rules.id;


--
-- Name: taxi_change_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.taxi_change_requests (
    id bigint NOT NULL,
    driver_id bigint NOT NULL,
    requested_unit_number character varying(20),
    requested_plates character varying(15) NOT NULL,
    requested_brand character varying(60),
    requested_model character varying(60),
    reason character varying(300),
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    resolved_at timestamp without time zone,
    resolved_by_user_id bigint
);


--
-- Name: taxi_change_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.taxi_change_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: taxi_change_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.taxi_change_requests_id_seq OWNED BY public.taxi_change_requests.id;


--
-- Name: taxis; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.taxis (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    unit_number character varying(20) NOT NULL,
    plates character varying(15) NOT NULL,
    photo_url character varying(300),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    brand character varying(60),
    model character varying(60)
);


--
-- Name: taxis_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.taxis_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: taxis_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.taxis_id_seq OWNED BY public.taxis.id;


--
-- Name: trip_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trip_messages (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    sender_user_id bigint NOT NULL,
    body character varying(1000) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: trip_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trip_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trip_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trip_messages_id_seq OWNED BY public.trip_messages.id;


--
-- Name: trip_offers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trip_offers (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    driver_id bigint NOT NULL,
    status character varying(15) DEFAULT 'SENT'::character varying NOT NULL,
    sent_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    responded_at timestamp without time zone
);


--
-- Name: trip_offers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trip_offers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trip_offers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trip_offers_id_seq OWNED BY public.trip_offers.id;


--
-- Name: trips; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trips (
    id bigint NOT NULL,
    passenger_id bigint NOT NULL,
    organization_id bigint,
    driver_id bigint,
    taxi_id bigint,
    origin_lat double precision NOT NULL,
    origin_lng double precision NOT NULL,
    origin_address character varying(300) NOT NULL,
    destination_lat double precision NOT NULL,
    destination_lng double precision NOT NULL,
    destination_address character varying(300) NOT NULL,
    status character varying(25) NOT NULL,
    distance_km numeric(8,2),
    duration_min numeric(8,2),
    estimated_fare numeric(10,2),
    payment_method character varying(15) NOT NULL,
    payment_confirmed boolean DEFAULT false NOT NULL,
    requested_at timestamp without time zone DEFAULT now() NOT NULL,
    accepted_at timestamp without time zone,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    cancelled_at timestamp without time zone,
    cancel_reason character varying(300),
    cancelled_by_role character varying(10),
    scheduled_at timestamp without time zone,
    route_geometry text,
    share_token character varying(40),
    tip_amount numeric(10,2) DEFAULT 0 NOT NULL,
    baby_seat boolean DEFAULT false NOT NULL,
    more_than_four_passengers boolean DEFAULT false NOT NULL,
    has_pet boolean DEFAULT false NOT NULL,
    special_comments character varying(300),
    dispatch_attempts integer DEFAULT 0 NOT NULL
);


--
-- Name: trips_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.trips_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: trips_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.trips_id_seq OWNED BY public.trips.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    phone character varying(20) NOT NULL,
    password_hash character varying(100) NOT NULL,
    name character varying(150) NOT NULL,
    email character varying(150),
    role character varying(20) NOT NULL,
    organization_id bigint,
    active boolean DEFAULT true NOT NULL,
    must_change_password boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    terms_accepted_at timestamp without time zone,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['SUPER_ADMIN'::character varying, 'ADMIN'::character varying, 'DRIVER'::character varying, 'PASSENGER'::character varying])::text[])))
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
-- Name: driver_locations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_locations ALTER COLUMN id SET DEFAULT nextval('public.driver_locations_id_seq'::regclass);


--
-- Name: driver_profiles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_profiles ALTER COLUMN id SET DEFAULT nextval('public.driver_profiles_id_seq'::regclass);


--
-- Name: organizations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations ALTER COLUMN id SET DEFAULT nextval('public.organizations_id_seq'::regclass);


--
-- Name: passenger_profiles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passenger_profiles ALTER COLUMN id SET DEFAULT nextval('public.passenger_profiles_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: ratings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings ALTER COLUMN id SET DEFAULT nextval('public.ratings_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: shift_templates id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shift_templates ALTER COLUMN id SET DEFAULT nextval('public.shift_templates_id_seq'::regclass);


--
-- Name: shifts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shifts ALTER COLUMN id SET DEFAULT nextval('public.shifts_id_seq'::regclass);


--
-- Name: tariff_rules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariff_rules ALTER COLUMN id SET DEFAULT nextval('public.tariff_rules_id_seq'::regclass);


--
-- Name: taxi_change_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxi_change_requests ALTER COLUMN id SET DEFAULT nextval('public.taxi_change_requests_id_seq'::regclass);


--
-- Name: taxis id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxis ALTER COLUMN id SET DEFAULT nextval('public.taxis_id_seq'::regclass);


--
-- Name: trip_messages id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_messages ALTER COLUMN id SET DEFAULT nextval('public.trip_messages_id_seq'::regclass);


--
-- Name: trip_offers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_offers ALTER COLUMN id SET DEFAULT nextval('public.trip_offers_id_seq'::regclass);


--
-- Name: trips id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trips ALTER COLUMN id SET DEFAULT nextval('public.trips_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: driver_locations driver_locations_driver_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_locations
    ADD CONSTRAINT driver_locations_driver_id_key UNIQUE (driver_id);


--
-- Name: driver_locations driver_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_locations
    ADD CONSTRAINT driver_locations_pkey PRIMARY KEY (id);


--
-- Name: driver_profiles driver_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_profiles
    ADD CONSTRAINT driver_profiles_pkey PRIMARY KEY (id);


--
-- Name: driver_profiles driver_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_profiles
    ADD CONSTRAINT driver_profiles_user_id_key UNIQUE (user_id);


--
-- Name: organizations organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);


--
-- Name: passenger_profiles passenger_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passenger_profiles
    ADD CONSTRAINT passenger_profiles_pkey PRIMARY KEY (id);


--
-- Name: passenger_profiles passenger_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passenger_profiles
    ADD CONSTRAINT passenger_profiles_user_id_key UNIQUE (user_id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token);


--
-- Name: ratings ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_pkey PRIMARY KEY (id);


--
-- Name: ratings ratings_trip_id_direction_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_trip_id_direction_key UNIQUE (trip_id, direction);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);


--
-- Name: shift_templates shift_templates_organization_id_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shift_templates
    ADD CONSTRAINT shift_templates_organization_id_name_key UNIQUE (organization_id, name);


--
-- Name: shift_templates shift_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shift_templates
    ADD CONSTRAINT shift_templates_pkey PRIMARY KEY (id);


--
-- Name: shifts shifts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shifts
    ADD CONSTRAINT shifts_pkey PRIMARY KEY (id);


--
-- Name: tariff_rules tariff_rules_organization_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariff_rules
    ADD CONSTRAINT tariff_rules_organization_id_key UNIQUE NULLS NOT DISTINCT (organization_id);


--
-- Name: tariff_rules tariff_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariff_rules
    ADD CONSTRAINT tariff_rules_pkey PRIMARY KEY (id);


--
-- Name: taxi_change_requests taxi_change_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxi_change_requests
    ADD CONSTRAINT taxi_change_requests_pkey PRIMARY KEY (id);


--
-- Name: taxis taxis_organization_id_unit_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxis
    ADD CONSTRAINT taxis_organization_id_unit_number_key UNIQUE (organization_id, unit_number);


--
-- Name: taxis taxis_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxis
    ADD CONSTRAINT taxis_pkey PRIMARY KEY (id);


--
-- Name: taxis taxis_plates_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxis
    ADD CONSTRAINT taxis_plates_key UNIQUE (plates);


--
-- Name: trip_messages trip_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_messages
    ADD CONSTRAINT trip_messages_pkey PRIMARY KEY (id);


--
-- Name: trip_offers trip_offers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_offers
    ADD CONSTRAINT trip_offers_pkey PRIMARY KEY (id);


--
-- Name: trip_offers trip_offers_trip_id_driver_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_offers
    ADD CONSTRAINT trip_offers_trip_id_driver_id_key UNIQUE (trip_id, driver_id);


--
-- Name: trips trips_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT trips_pkey PRIMARY KEY (id);


--
-- Name: users users_phone_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_key UNIQUE (phone);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_driver_locations_online; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_driver_locations_online ON public.driver_locations USING btree (online);


--
-- Name: idx_shifts_driver_revoked; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_shifts_driver_revoked ON public.shifts USING btree (driver_id) WHERE (revoked = false);


--
-- Name: idx_shifts_taxi_revoked; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_shifts_taxi_revoked ON public.shifts USING btree (taxi_id) WHERE (revoked = false);


--
-- Name: idx_trip_messages_trip; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_trip_messages_trip ON public.trip_messages USING btree (trip_id);


--
-- Name: idx_trip_offers_driver_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_trip_offers_driver_status ON public.trip_offers USING btree (driver_id, status);


--
-- Name: idx_trips_driver; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_trips_driver ON public.trips USING btree (driver_id);


--
-- Name: idx_trips_passenger; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_trips_passenger ON public.trips USING btree (passenger_id);


--
-- Name: idx_trips_share_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_trips_share_token ON public.trips USING btree (share_token);


--
-- Name: idx_trips_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_trips_status ON public.trips USING btree (status);


--
-- Name: driver_locations driver_locations_driver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_locations
    ADD CONSTRAINT driver_locations_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES public.driver_profiles(id);


--
-- Name: driver_profiles driver_profiles_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_profiles
    ADD CONSTRAINT driver_profiles_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- Name: driver_profiles driver_profiles_taxi_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_profiles
    ADD CONSTRAINT driver_profiles_taxi_id_fkey FOREIGN KEY (taxi_id) REFERENCES public.taxis(id);


--
-- Name: driver_profiles driver_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_profiles
    ADD CONSTRAINT driver_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: passenger_profiles passenger_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passenger_profiles
    ADD CONSTRAINT passenger_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: password_reset_tokens password_reset_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: ratings ratings_from_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_from_user_id_fkey FOREIGN KEY (from_user_id) REFERENCES public.users(id);


--
-- Name: ratings ratings_to_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_to_user_id_fkey FOREIGN KEY (to_user_id) REFERENCES public.users(id);


--
-- Name: ratings ratings_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trips(id);


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: shift_templates shift_templates_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shift_templates
    ADD CONSTRAINT shift_templates_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- Name: shifts shifts_created_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shifts
    ADD CONSTRAINT shifts_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);


--
-- Name: shifts shifts_driver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shifts
    ADD CONSTRAINT shifts_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES public.driver_profiles(id);


--
-- Name: shifts shifts_shift_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shifts
    ADD CONSTRAINT shifts_shift_template_id_fkey FOREIGN KEY (shift_template_id) REFERENCES public.shift_templates(id);


--
-- Name: shifts shifts_taxi_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shifts
    ADD CONSTRAINT shifts_taxi_id_fkey FOREIGN KEY (taxi_id) REFERENCES public.taxis(id);


--
-- Name: tariff_rules tariff_rules_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariff_rules
    ADD CONSTRAINT tariff_rules_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- Name: taxi_change_requests taxi_change_requests_driver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxi_change_requests
    ADD CONSTRAINT taxi_change_requests_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES public.driver_profiles(id);


--
-- Name: taxi_change_requests taxi_change_requests_resolved_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxi_change_requests
    ADD CONSTRAINT taxi_change_requests_resolved_by_user_id_fkey FOREIGN KEY (resolved_by_user_id) REFERENCES public.users(id);


--
-- Name: taxis taxis_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxis
    ADD CONSTRAINT taxis_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- Name: trip_messages trip_messages_sender_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_messages
    ADD CONSTRAINT trip_messages_sender_user_id_fkey FOREIGN KEY (sender_user_id) REFERENCES public.users(id);


--
-- Name: trip_messages trip_messages_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_messages
    ADD CONSTRAINT trip_messages_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trips(id);


--
-- Name: trip_offers trip_offers_driver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_offers
    ADD CONSTRAINT trip_offers_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES public.driver_profiles(id);


--
-- Name: trip_offers trip_offers_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trip_offers
    ADD CONSTRAINT trip_offers_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trips(id);


--
-- Name: trips trips_driver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT trips_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES public.driver_profiles(id);


--
-- Name: trips trips_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT trips_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- Name: trips trips_passenger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT trips_passenger_id_fkey FOREIGN KEY (passenger_id) REFERENCES public.users(id);


--
-- Name: trips trips_taxi_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trips
    ADD CONSTRAINT trips_taxi_id_fkey FOREIGN KEY (taxi_id) REFERENCES public.taxis(id);


--
-- Name: users users_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- PostgreSQL database dump complete
--

