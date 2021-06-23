#!/bin/sh
#
# /etc/rc.boot: system boot script
#

echo "The system is coming up.  Please wait."

# Load configuration
. /etc/rc.conf

# Soft reboot on ctrl-alt-del
/sbin/ctrlaltdel -s

# Start udev
/bin/mountpoint -q /proc || /sbin/mount -t proc  none /proc
/bin/mountpoint -q /sys  || /sbin/mount -t sysfs none /sys
/sbin/start_udev

# Create device-mapper device nodes and scan for LVM volume groups
if [ -x /sbin/lvm ]; then
	/sbin/vgscan --mknodes --ignorelockingfailure
	/sbin/vgchange --sysinit -a y
fi

# Mount root read-only
/sbin/mount -o remount,ro /

if [ -f /forcefsck ]; then
FORCEFSCK="-f"
fi

# Check filesystems
/sbin/fsck $FORCEFSCK -A -T -C -a
if [ $? -gt 1 ]; then
	echo
	echo "***************  FILESYSTEM CHECK FAILED  ******************"
	echo "*                                                          *"
	echo "*  Please repair manually and reboot. Note that the root   *"
	echo "*  file system is currently mounted read-only. To remount  *"
	echo "*  it read-write type: mount -n -o remount,rw /            *"
	echo "*  When you exit the maintainance shell the system will    *"
	echo "*  reboot automatically.                                   *"
	echo "*                                                          *"
	echo "************************************************************"
	echo
	/bin/sh
	echo "Automatic reboot in progress..."
	/sbin/umount -a -l
	/sbin/mount -o remount,ro /
	/sbin/halt -r
	exit 0
fi

# Remount root filesystem read-write
/sbin/mount -o remount,rw /

# Mount local filesystems
/sbin/mount -a # XXX -t nonfs

# Activate swap
/sbin/swapon -a

# Clean up misc files
: > /var/run/utmp
/bin/rm -rf /forcefsck /fastboot /etc/nologin /etc/shutdownpid
(cd /var/run  && /usr/bin/find . -name "*.pid" -delete)
(cd /var/lock && /usr/bin/find . ! -type d -delete)
(cd /tmp      && /usr/bin/find . ! -name . -delete)
/bin/mkdir -m 1777 /tmp/.ICE-unix

# Set kernel variables
/sbin/sysctl -p /etc/sysctl.conf > /dev/null

# Update shared library links
/sbin/ldconfig

# Configure host name
if [ "$HOSTNAME" ]; then
	echo "hostname: $HOSTNAME"
	/bin/hostname $HOSTNAME
fi

# Load random seed
/bin/cat /var/lib/urandom/seed > /dev/urandom

# Configure system clock
if [ "$TIMEZONE" ] && [ -f /usr/share/zoneinfo/$TIMEZONE ]; then
	/bin/ln -sf /usr/share/zoneinfo/$TIMEZONE /etc/localtime
fi
# Set the system time from the hardware clock
/sbin/hwclock -s

# Load console font
if [ "$FONT" ] && [ -x /usr/bin/setfont ]; then
	echo "font: $FONT"
	/usr/bin/setfont $FONT
fi

# Load console keymap
if [ "$KEYMAP" ]; then
	echo "keyboard: $KEYMAP"
	/usr/bin/loadkeys -q $KEYMAP
fi

# Screen blanks after 15 minutes idle time
if [ -x /usr/bin/setterm ]; then
	/usr/bin/setterm -blank 15
fi

# Run module initialization script
if [ -x /etc/rc.modules ]; then
	/etc/rc.modules
fi

# Save boot messages
/bin/dmesg > /var/log/boot
if [ -f /proc/sys/kernel/dmesg_restrict ]; then
	if [ "$(cat /proc/sys/kernel/dmesg_restrict)" = 1 ]; then
		/bin/chmod 0600 /var/log/boot
	else
		/bin/chmod 0644 /var/log/boot
	fi
fi

# Boot single- or multi-user mode
case "$(cat /proc/cmdline)" in
*\ single\ *) [ -x /etc/rc.single ] && /etc/rc.single ;;
*)            [ -x /etc/rc.multi  ] && /etc/rc.multi  ;;
esac

# End of file
