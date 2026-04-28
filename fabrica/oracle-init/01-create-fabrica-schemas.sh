#!/bin/bash
set -euo pipefail

create_user() {
  local user_name="$1"
  local user_pass="$2"

  sqlplus -s "system/${ORACLE_PASSWORD}@localhost:1521/XEPDB1" <<SQL
WHENEVER SQLERROR EXIT SQL.SQLCODE;
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM dba_users WHERE username = UPPER('${user_name}');
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER ${user_name} IDENTIFIED BY "${user_pass}"';
  END IF;
END;
/
ALTER USER ${user_name} IDENTIFIED BY "${user_pass}";
GRANT CONNECT, RESOURCE TO ${user_name};
ALTER USER ${user_name} QUOTA UNLIMITED ON USERS;
EXIT;
SQL
}

create_user "FABRICA2" "${ORACLE_F2_APP_PASSWORD:-123}"
create_user "FABRICA3" "${ORACLE_F3_APP_PASSWORD:-123}"
create_user "FABRICA4" "${ORACLE_F4_APP_PASSWORD:-123}"
create_user "FABRICA5" "${ORACLE_F5_APP_PASSWORD:-123}"
