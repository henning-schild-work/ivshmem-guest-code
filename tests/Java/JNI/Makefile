CC=gcc
JAVA_HOME=/usr/lib/jvm/jdk1.6.0_14/

#gcc -G -I/usr/local/java/include -I/usr/local/java/include/solaris 
#	nativetest.c -o nativetest.so

all:	
	$(CC) -fPIC -shared -o libnativelib.so nativelib.c -lc

MemAccess:
	javac MemAccess.java
	javah -jni MemAccess

memlib:
	g++ -fPIC -shared -o libMemAccess.so MemAccess.cpp 
	

#	$(CC) -fPIC -o libnativelib.so -shared -Wl,-soname,libnative.so \
     -I$(JAVA_HOME)/include \
     -I$(JAVA_HOME)/include/linux nativelib.c  \
     -static -lc


