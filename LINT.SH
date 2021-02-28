#!/bin/bash
# Find missing dependencies for core pkgsrc collection.

PKGS=$(find . -name Pkgfile -exec dirname {} \; | sed 's/.\///g')
for p in $PKGS; do
	pkgman \
	--root=/dev/null \
	--no-std-config \
	--config-set="pkgsrcdir $PWD" \
	dep --all --recursive $p | \
	awk -v p=$p '/not found/{print p":"$1}'
done

# End of file.
