CC=gcc
JAVA_HOME=/usr/lib/jvm/jdk1.6.0_14
CFLAGS=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -fPIC -shared -Wall -D_GNU_SOURCE

all: libMemAccess.so FTPRecv.class FTPSend.class

%.class: %.java
	javac $<

MemAccess.h: MemAccess.class
	javah -jni MemAccess

libMemAccess.so: MemAccess.h MemAccess.c ivshmem.c ivshmem.h
	$(CC) $(CFLAGS) -o libMemAccess.so MemAccess.c ivshmem.c

clean:
	-rm -f *.so *.class MemAccess.h
