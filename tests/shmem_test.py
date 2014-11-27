#!/usr/bin/python

import mmap
import contextlib

with open('/dev/uio0', 'r+b') as f:
    with contextlib.closing(mmap.mmap(f.fileno(), 4096, offset=4096)) as m:
        print('Shmem content: ' + m.read(30))
