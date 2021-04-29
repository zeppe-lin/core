#!/bin/sh
#
# core/LINT.SH: Find missing dependencies for core pkgsrc collection.
#

find . -name Pkgfile -exec dirname {} \; | sed 's/.\///g' | \
while read p; do
	pkgman  --root=/dev/null \
		--no-std-config \
		--config-set="pkgsrcdir $PWD" \
		dep --all --recursive $p | \
		awk -v p=$p '/not found/{print p":"$1}'
done

# End of file.
