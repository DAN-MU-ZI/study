#!/bin/sh
set -eu

pgloader \
  --dynamic-space-size 4096 \
  "mssql://sa:${MSSQL_SA_PASSWORD}@sqlserver:1433/StackOverflow2010" \
  "postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres:5432/stackoverflow_base"
