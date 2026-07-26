#!/bin/sh
set -eu

source_mdf=/source/StackOverflow2010.mdf
source_ldf=/source/StackOverflow2010_log.ldf
target_mdf=/var/opt/mssql/data/StackOverflow2010.mdf
target_ldf=/var/opt/mssql/data/StackOverflow2010_log.ldf

if [ ! -f "$target_mdf" ] || [ "$(stat -c %s "$target_mdf")" != "$(stat -c %s "$source_mdf")" ]; then
  cp "$source_mdf" "$target_mdf"
fi

if [ ! -f "$target_ldf" ] || [ "$(stat -c %s "$target_ldf")" != "$(stat -c %s "$source_ldf")" ]; then
  cp "$source_ldf" "$target_ldf"
fi

chown 10001:10001 "$target_mdf" "$target_ldf"
chmod 660 "$target_mdf" "$target_ldf"

