CREATE UNIQUE INDEX uq_customer_keycloak_user_id
    ON customer (keycloak_user_id)
    WHERE keycloak_user_id IS NOT NULL;
